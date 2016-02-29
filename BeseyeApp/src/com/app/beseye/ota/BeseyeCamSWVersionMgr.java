package com.app.beseye.ota;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.R;
import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeFeatureConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.BeseyeCamInfoSyncMgr.OnCamInfoChangedListener;

public class BeseyeCamSWVersionMgr implements OnHttpTaskCallback{
	
	static public enum CAM_UPDATE_STATUS{
		CAM_UPDATE_STATUS_INIT,
		CAM_UPDATE_STATUS_VER_CHECKING,
		CAM_UPDATE_STATUS_UPDATE_REQUEST,
		CAM_UPDATE_STATUS_UPDATING,
		CAM_UPDATE_STATUS_UPDATE_FINISH,
		CAM_UPDATE_STATUS_UPDATE_ERR,
		CAM_UPDATE_STATUS_COUNT
	}
	
	static public enum CAM_UPDATE_ERROR{
		CAM_UPDATE_ERROR_NONE,
		CAM_UPDATE_ERROR_GET_VCAM_LST_FALED,
		CAM_UPDATE_ERROR_VER_CHECK_FAILED,
		CAM_UPDATE_ERROR_TRIGGER_UPDATE_FAILED,
		CAM_UPDATE_ERROR_UPDATE_FAILED,
		CAM_UPDATE_ERROR_NO_RESPONSE,
		CAM_UPDATE_ERROR_COUNT
	}
	
	static public enum CAM_UPDATE_VER_CHECK_STATUS{
		CAM_UPDATE_VER_CHECK_INIT,
		CAM_UPDATE_VER_CHECK_OUT_OF_DATE,
		CAM_UPDATE_VER_CHECK_UPDATED,
		CAM_UPDATE_VER_CHECK_TIMEOUT,
		CAM_UPDATE_VER_CHECK_ERR,
		CAM_UPDATE_VER_CHECK_COUNT
	}
	
	static public enum CAM_GROUP_VER_CHK_RET{
		CAM_GROUP_VER_CHK_ALL_UPDATED,
		CAM_GROUP_VER_CHK_PARTIAL_UPDATED,
		CAM_GROUP_VER_CHK_ALL_OUT_OF_UPDATE,
		CAM_GROUP_VER_CHK_VCAM_LST_EMPTY,
		CAM_GROUP_VER_CHK_ONGOING,
		CAM_GROUP_VER_CHK_TOO_CLOSE,
		CAM_GROUP_VER_CHK_ERROR,
		CAM_GROUP_VER_CHK_RET_COUNT
	}
	
	static public enum CAM_UPDATE_GROUP{
		CAM_UPDATE_GROUP_PERONSAL,
		CAM_UPDATE_GROUP_DEMO,
		CAM_UPDATE_GROUP_PRIVATE,
		CAM_UPDATE_GROUP_COUNT
	}
	
	static public interface OnCamGroupUpdateVersionCheckListener{
		public void onCamUpdateVersionCheckAllCallback(CAM_GROUP_VER_CHK_RET chkRet, CAM_UPDATE_GROUP chkGroup, CAM_UPDATE_ERROR chkErr, List<String> lstVcamIds);
		public void onSessionInvalid(AsyncTask<String, Double, List<JSONObject>> task, int iInvalidReason);
	}
		
	static public interface OnCamUpdateStatusChangedListener{
		public void onCamUpdateVerChkStatusChanged(String strVcamId, CAM_UPDATE_VER_CHECK_STATUS curStatus, CAM_UPDATE_VER_CHECK_STATUS prevStatus, CamSwUpdateRecord objUpdateRec);
		public void onCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec);
		public void onCamUpdateProgress(String strVcamId, int iPercetage);
		public void onSessionInvalid(AsyncTask<String, Double, List<JSONObject>> task, int iInvalidReason);
		public void onShowDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId, int iTitleRes, int iMsgRes);
		public void onDismissDialog(AsyncTask task, final int iDialogId);
	}
	
	static private BeseyeCamSWVersionMgr sBeseyeCamSWVersionMgr;
	
	static public BeseyeCamSWVersionMgr getInstance(){
		if(null == sBeseyeCamSWVersionMgr){
			sBeseyeCamSWVersionMgr = new BeseyeCamSWVersionMgr();
		}
		return sBeseyeCamSWVersionMgr;
	}
	
	private List<WeakReference<OnCamGroupUpdateVersionCheckListener>> mLstOnCamGroupUpdateVersionCheckListener;
	private List<WeakReference<OnCamUpdateStatusChangedListener>> mLstOnCamUpdateStatusChangedListener;

	private List<Map<String, CamSwUpdateRecord>> mLstMapCamSwUpdateRecord = null;

	private BeseyeAccountTask.GetVCamListTask[] mGetVCamListTasks = null;
	private long mlLastGroupVerCheckTs[] = null;
	private boolean[] mbNeedPeriodCheckUpdateStatus = null;
	private Runnable[] mPeriodCheckUpdateStatusRunnable = null;
	
	static private final long PERIOD_TO_CHECK_GROUP_VER = 60*1000L;
	static private final long PERIOD_TO_CHECK_UPDATE_STATUS = 5000L;
	
	//Period check ota status
	class PeriodCheckUpdateStatusRunnable implements Runnable{
		public CAM_UPDATE_GROUP meUpdateGroup;
		
		public PeriodCheckUpdateStatusRunnable(CAM_UPDATE_GROUP group){
			meUpdateGroup = group;
		}
		
		@Override
		public void run() {
			if(mbNeedPeriodCheckUpdateStatus[meUpdateGroup.ordinal()]){
				checkGroupCamUpdateStatus(meUpdateGroup, false);
				BeseyeUtils.postRunnable(mPeriodCheckUpdateStatusRunnable[meUpdateGroup.ordinal()], PERIOD_TO_CHECK_UPDATE_STATUS);
			}else{
				mPeriodCheckUpdateStatusRunnable[meUpdateGroup.ordinal()] = null;
			}
		}
	} 

	private BeseyeCamSWVersionMgr() {
		mLstOnCamGroupUpdateVersionCheckListener = new ArrayList<WeakReference<OnCamGroupUpdateVersionCheckListener>>();
		mLstOnCamUpdateStatusChangedListener = new ArrayList<WeakReference<OnCamUpdateStatusChangedListener>>();
		mLstMapCamSwUpdateRecord = new ArrayList<Map<String, CamSwUpdateRecord>>();
		mGetVCamListTasks = new BeseyeAccountTask.GetVCamListTask[CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal()];
		mlLastGroupVerCheckTs = new long[CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal()];
		mbNeedPeriodCheckUpdateStatus = new boolean[CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal()];
		mPeriodCheckUpdateStatusRunnable = new Runnable[CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal()];
		for(int i = 0; i < CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal();i++){
			mLstMapCamSwUpdateRecord.add(new HashMap<String, CamSwUpdateRecord>());
			mbNeedPeriodCheckUpdateStatus[i] = false;
			mlLastGroupVerCheckTs[i] = -1;
		}
	}
	
	//set enabled when need to monitor ota status in specific page ex. camema list
	synchronized public void setNeedPeriodCheckUpdateStatus(CAM_UPDATE_GROUP group, boolean bEnabled){
		if(bEnabled){
			mbNeedPeriodCheckUpdateStatus[group.ordinal()] = true;
			if(null == mPeriodCheckUpdateStatusRunnable[group.ordinal()]){
				mPeriodCheckUpdateStatusRunnable[group.ordinal()] = new PeriodCheckUpdateStatusRunnable(group);
				BeseyeUtils.postRunnable(mPeriodCheckUpdateStatusRunnable[group.ordinal()], 0);
			}
		}else{
			mbNeedPeriodCheckUpdateStatus[group.ordinal()] = false;
			BeseyeUtils.removeRunnable(mPeriodCheckUpdateStatusRunnable[group.ordinal()]);
			mPeriodCheckUpdateStatusRunnable[group.ordinal()] =null;
		}
	}
	
	synchronized public void registerOnCamGroupUpdateVersionCheckListener(OnCamGroupUpdateVersionCheckListener listener){
		if(null != listener && null == findOnCamGroupUpdateVersionCheckListener(listener)){
			//Log.i(TAG, "registerOnCamGroupUpdateVersionCheckListener(), listener add OK");
			mLstOnCamGroupUpdateVersionCheckListener.add(new WeakReference<OnCamGroupUpdateVersionCheckListener>(listener));
		}
	}
	
	synchronized public void unregisterOnCamGroupUpdateVersionCheckListener(OnCamGroupUpdateVersionCheckListener listener){
		WeakReference<OnCamGroupUpdateVersionCheckListener> ret = null;
//		if(null != listener && null != (ret = findOnCamGroupUpdateVersionCheckListener(listener))){
//			mLstOnCamGroupUpdateVersionCheckListener.remove(ret);
//		}
		
		for( Iterator<WeakReference<OnCamGroupUpdateVersionCheckListener>> it = mLstOnCamGroupUpdateVersionCheckListener.iterator(); it.hasNext() ; ){
			ret = it.next();
			if(null != ret && listener.equals(ret.get())){
				it.remove();
			}
	    }
	}
	
	synchronized private WeakReference<OnCamGroupUpdateVersionCheckListener> findOnCamGroupUpdateVersionCheckListener(OnCamGroupUpdateVersionCheckListener listener){
		WeakReference<OnCamGroupUpdateVersionCheckListener> ret = null;
		if(null != listener){
			for(WeakReference<OnCamGroupUpdateVersionCheckListener> listenerChk : mLstOnCamGroupUpdateVersionCheckListener){
				if(null != listenerChk && listener.equals(listenerChk.get())){
					ret = listenerChk;
					break;
				}
			}
		}	
		//Log.i(TAG, "findOnCamGroupUpdateVersionCheckListener(), ret : "+((null != ret)?ret.get().toString():""));
		return ret;
	} 
	
	synchronized private void broadcastOnCamGroupUpdateVersionCheck(CAM_GROUP_VER_CHK_RET chkRet, CAM_UPDATE_GROUP chkGroup, CAM_UPDATE_ERROR chkErr, List<String> lstVcamIds){
		
		Log.i(TAG, "broadcastOnCamGroupUpdateVersionCheck(), ["+chkRet+", "+chkGroup+", "+chkErr+"]");

		OnCamGroupUpdateVersionCheckListener listener = null;
		for(WeakReference<OnCamGroupUpdateVersionCheckListener> listenerChk : mLstOnCamGroupUpdateVersionCheckListener){
			if(null != listenerChk && null != (listener = listenerChk.get())){
				listener.onCamUpdateVersionCheckAllCallback(chkRet, chkGroup, chkErr, lstVcamIds);
			}
		}
	} 
	
	synchronized protected void broadcastOnCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec){
		
		Log.i(TAG, "broadcastOnCamUpdateStatusChanged(), ["+prevStatus+"->"+curStatus+", "+strVcamId+"]");

		OnCamUpdateStatusChangedListener listener = null;
		for(WeakReference<OnCamUpdateStatusChangedListener> listenerChk : mLstOnCamUpdateStatusChangedListener){
			if(null != listenerChk && null != (listener = listenerChk.get())){
				listener.onCamUpdateStatusChanged(strVcamId, curStatus, prevStatus, objUpdateRec);
			}
		}
	} 
	
	synchronized private void broadcastOnCamUpdateProgressChanged(String strVcamId, int iPercetage){
		
		Log.i(TAG, "broadcastOnCamUpdateProgressChanged(), ["+strVcamId+", "+iPercetage+"%]");

		OnCamUpdateStatusChangedListener listener = null;
		for(WeakReference<OnCamUpdateStatusChangedListener> listenerChk : mLstOnCamUpdateStatusChangedListener){
			if(null != listenerChk && null != (listener = listenerChk.get())){
				listener.onCamUpdateProgress(strVcamId, iPercetage);
			}
		}
	} 
	
	synchronized protected void broadcastOnCamUpdateVerChkStatusChanged(String strVcamId, CAM_UPDATE_VER_CHECK_STATUS curStatus, CAM_UPDATE_VER_CHECK_STATUS prevStatus, CamSwUpdateRecord objUpdateRec){
		Log.i(TAG, "broadcastOnCamUpdateVerChkStatusChanged(), ["+prevStatus+"->"+curStatus+", "+strVcamId+"]");

		OnCamUpdateStatusChangedListener listener = null;
		for(WeakReference<OnCamUpdateStatusChangedListener> listenerChk : mLstOnCamUpdateStatusChangedListener){
			if(null != listenerChk && null != (listener = listenerChk.get())){
				listener.onCamUpdateVerChkStatusChanged(strVcamId, curStatus, prevStatus, objUpdateRec);
			}
		}
	} 
	
	synchronized public void registerOnCamUpdateStatusChangedListener(OnCamUpdateStatusChangedListener listener){
		if(null != listener && null == findOnCamUpdateStatusChangedListener(listener)){
			//Log.i(TAG, "registerOnCamUpdateStatusChangedListener(), listener add OK");
			mLstOnCamUpdateStatusChangedListener.add(new WeakReference<OnCamUpdateStatusChangedListener>(listener));
		}
	}
	
	synchronized public void unregisterOnCamUpdateStatusChangedListener(OnCamUpdateStatusChangedListener listener){
		WeakReference<OnCamUpdateStatusChangedListener> ret = null;
//		if(null != listener && null != (ret = findOnCamUpdateStatusChangedListener(listener))){
//			mLstOnCamUpdateStatusChangedListener.remove(ret);
//		}
		
		for( Iterator<WeakReference<OnCamUpdateStatusChangedListener>> it = mLstOnCamUpdateStatusChangedListener.iterator(); it.hasNext() ; ){
			ret = it.next();
			if(null != ret && listener.equals(ret.get())){
				it.remove();
			}
	    }
	}
	
	synchronized private WeakReference<OnCamUpdateStatusChangedListener> findOnCamUpdateStatusChangedListener(OnCamUpdateStatusChangedListener listener){
		WeakReference<OnCamUpdateStatusChangedListener> ret = null;
		if(null != listener){
			for(WeakReference<OnCamUpdateStatusChangedListener> listenerChk : mLstOnCamUpdateStatusChangedListener){
				if(null != listenerChk && listener.equals(listenerChk.get())){
					ret = listenerChk;
					break;
				}
			}
		}	
		//Log.i(TAG, "findOnCamUpdateStatusChangedListener(), ret : "+((null != ret)?ret.get().toString():""));
		return ret;
	} 
	
	private static ExecutorService CACHE_TASK_EXECUTOR; 
	static {  
		CACHE_TASK_EXECUTOR = (ExecutorService) Executors.newCachedThreadPool();  
    }; 
    
    synchronized public CamSwUpdateRecord findCamSwUpdateRecord(CAM_UPDATE_GROUP eUpdateGroup, String strVCamId){
    	CamSwUpdateRecord ret = null;
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	if(0 < mapVcamList.size() && null != strVCamId){
    		for(CamSwUpdateRecord camRec : mapVcamList.values()){
    			if(null != camRec && strVCamId.equals(camRec.mStrVCamId)){
    				ret = camRec;
    				break;
    			}
    		}
    	}
    	return ret;
	}
    
    synchronized public void performCamGroupOTAVerCheck(CAM_UPDATE_GROUP eUpdateGroup){
		if(null == mGetVCamListTasks[eUpdateGroup.ordinal()]){
			if(-1 == mlLastGroupVerCheckTs[eUpdateGroup.ordinal()] || (System.currentTimeMillis() - mlLastGroupVerCheckTs[eUpdateGroup.ordinal()] > PERIOD_TO_CHECK_GROUP_VER)){
				mGetVCamListTasks[eUpdateGroup.ordinal()] = new BeseyeAccountTask.GetVCamListTask(this);
				mGetVCamListTasks[eUpdateGroup.ordinal()].setDialogId(-1);
				executeAsyncTask(mGetVCamListTasks[eUpdateGroup.ordinal()]);
				//mlLastGroupVerCheckTs[eUpdateGroup.ordinal()] = System.currentTimeMillis();
				Log.i(TAG, "performCamGroupOTAVerCheck(), mGetVCamListTasks["+eUpdateGroup.ordinal()+"] is launching...");
			}else{
				Log.i(TAG, "performCamGroupOTAVerCheck(), mGetVCamListTasks["+eUpdateGroup.ordinal()+"] is within PERIOD_TO_CHECK_GROUP_VER");
				broadcastOnCamGroupUpdateVersionCheck(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_TOO_CLOSE, 
													  eUpdateGroup, 
													  CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE, 
													  null);
			}
		}else{
			if(DEBUG)
				Log.i(TAG, "performCamGroupUpdateCheck(), mGetVCamListTasks["+eUpdateGroup.ordinal()+"] is ongoing");
			broadcastOnCamGroupUpdateVersionCheck(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_ONGOING, 
												  eUpdateGroup, 
												  CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE, 
												  null);
		}
	}
    
    synchronized public void performCamGroupUpdate(CAM_UPDATE_GROUP eUpdateGroup){
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	if(0 < mapVcamList.size()){
    		for(CamSwUpdateRecord camRec : mapVcamList.values()){
    			if(null != camRec && camRec.meVerCheckStatus.equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE)){
    				performCamUpdate(camRec, false);
    			}
    		}
    	}
	}

    synchronized public void performCamUpdate(CamSwUpdateRecord camRec, boolean bShowDialog){
    	if(null != camRec){
    		if(null == camRec.mUpdateCamSWTask){
    			if(null != camRec.mGetCamUpdateStatusTask){
    				camRec.mGetCamUpdateStatusTask.cancel(true);
    			}
				camRec.changeUpdateStatus(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_REQUEST);
    			camRec.resetErrorInfo();
    			camRec.mUpdateCamSWTask = new BeseyeCamBEHttpTask.UpdateCamSWTask(this);
    			camRec.mUpdateCamSWTask.setCusObj(camRec);
    			if(!bShowDialog){
    				camRec.mUpdateCamSWTask.setDialogId(-1);
    			}
            	executeAsyncTask(camRec.mUpdateCamSWTask, camRec.mStrVCamId);
    		}else{
    			if(DEBUG)
    				Log.i(TAG, "performCamUpdate(), mUpdateCamSWTask["+camRec.mStrVCamId+"] is ongoing");
    		}
    	}else{
			Log.e(TAG, "performCamUpdate(), camRec is null ");
    	}
    }
    
    synchronized public void checkGroupCamUpdateStatus(CAM_UPDATE_GROUP eUpdateGroup, boolean bForceCheck){
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	if(0 < mapVcamList.size()){
    		for(CamSwUpdateRecord camRec : mapVcamList.values()){
    			if(null != camRec && (camRec.mbUpdateTriggerred) || (bForceCheck || camRec.meUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING))){
    				checkCamUpdateStatus(camRec, false);
    			}
    		}
    	}
    }
    
    synchronized public void resetPoorNetworkError(CAM_UPDATE_GROUP eUpdateGroup){
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	if(0 < mapVcamList.size()){
    		for(CamSwUpdateRecord camRec : mapVcamList.values()){
    			if(null != camRec && camRec.isPoorNetworkErrWhenOTA()){
    				camRec.resetErrorInfo();
    			}
    		}
    	}
	}
    
    synchronized public boolean setOTAFeedbackTsByVcamId(CAM_UPDATE_GROUP eUpdateGroup, String strVcamId){
    	boolean bRet = false;
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	CamSwUpdateRecord camRec = mapVcamList.get(strVcamId);
		if(null != camRec){
			camRec.setOTAFeedbackSent();
			bRet = true;
		}
		return bRet;
    }
    
    synchronized public boolean resetOTAStatusByVcamId(CAM_UPDATE_GROUP eUpdateGroup, String strVcamId){
    	boolean bRet = false;
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	CamSwUpdateRecord camRec = mapVcamList.get(strVcamId);
		if(null != camRec){
			camRec.resetCamOTAInfo();
			bRet = true;
		}
		return bRet;
    }

    
    synchronized public void checkCamUpdateStatus(CAM_UPDATE_GROUP eUpdateGroup, String strVcamId){
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	CamSwUpdateRecord camRec = mapVcamList.get(strVcamId);
		if(null != camRec){
			checkCamUpdateStatus(camRec, true);
		}
    }
    
    synchronized public void checkCamUpdateStatus(CamSwUpdateRecord camRec, boolean bForceCheck){
    	if(null != camRec){
    		if(!bForceCheck && camRec.getUpdateStatus().equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_VER_CHECKING) && camRec.getVerCheckStatus().equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE)){
				Log.i(TAG, "checkCamUpdateStatus(), mGetCamUpdateStatusTask["+camRec.mStrVCamId+"], wait to be triggerred OTA");
    		}else{
    			if(null == camRec.mGetCamUpdateStatusTask){
        			camRec.mGetCamUpdateStatusTask = new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this);
        			camRec.mGetCamUpdateStatusTask.setCusObj(camRec);
        			camRec.mGetCamUpdateStatusTask.setDialogId(-1);
                	executeAsyncTask(camRec.mGetCamUpdateStatusTask, camRec.mStrVCamId);
        		}else{
        			if(DEBUG)
        				Log.i(TAG, "checkCamUpdateStatus(), mGetCamUpdateStatusTask["+camRec.mStrVCamId+"] is ongoing");
        		}
    		}
    	}else{
			Log.e(TAG, "mGetCamUpdateStatusTask(), camRec is null ");
    	}
    }
    
    private void executeAsyncTask(BeseyeHttpTask task, String... params){
    	if(null != task){
    		//task.setDialogId(-1);
    		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1){
    			task.execute(params);
    		}else{
    			task.executeOnExecutor(CACHE_TASK_EXECUTOR, params);
    		}
    	}
    }
    
    private CAM_UPDATE_GROUP findCamUpdateGroupByTask(BeseyeAccountTask.GetVCamListTask task){
    	CAM_UPDATE_GROUP ret = null;
    	for(CAM_UPDATE_GROUP group : CAM_UPDATE_GROUP.values()){
			if(task ==  mGetVCamListTasks[group.ordinal()]){
				ret = group;
				break;
			}
		}
    	return ret;
    }
    
    synchronized public void updateGroupCamList(CAM_UPDATE_GROUP eUpdateGroup, JSONArray arrCamLst){
    	CamSwUpdateRecord ret = null;
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	Map<String, CamSwUpdateRecord> mapVcamListNew = new HashMap<String, CamSwUpdateRecord>();
    	for(int i = 0;i< arrCamLst.length();i++){
			try {
				JSONObject camObj = arrCamLst.getJSONObject(i);
				if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
					String strVcamId = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID);
					String strCamName = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_NAME);
					CamSwUpdateRecord camRec = (null != mapVcamList)?mapVcamList.get(strVcamId):null;
					mapVcamListNew.put(strVcamId, (null != camRec)?camRec:new CamSwUpdateRecord(strVcamId, eUpdateGroup, strCamName));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
    	
    	mLstMapCamSwUpdateRecord.set(eUpdateGroup.ordinal(), mapVcamListNew);
	}
    
    synchronized public void AddUpdateVCam(JSONObject objVCam, CAM_UPDATE_GROUP updateGroup){		
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(updateGroup.ordinal());
    	if(null != mapVcamList && null != objVCam){
    		if(BeseyeJSONUtil.getJSONBoolean(objVCam, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
				String strVcamId = BeseyeJSONUtil.getJSONString(objVCam, BeseyeJSONUtil.ACC_ID);
				String strCamName = BeseyeJSONUtil.getJSONString(objVCam, BeseyeJSONUtil.ACC_NAME);
				mapVcamList.put(strVcamId, new CamSwUpdateRecord(strVcamId, updateGroup, strCamName));
			}
    	}
    }
    
    synchronized private void fillUpdateVCamList(JSONObject objVCamList, CAM_UPDATE_GROUP updateGroup){		
		String strIDVcamCnt= (updateGroup.equals(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL)?BeseyeJSONUtil.ACC_VCAM_CNT:(updateGroup.equals(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_DEMO)?BeseyeJSONUtil.ACC_DEMO_VCAM_CNT:BeseyeJSONUtil.ACC_PRIVATE_VCAM_CNT));
		String strIDVcamLst= (updateGroup.equals(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL)?BeseyeJSONUtil.ACC_VCAM_LST:(updateGroup.equals(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_DEMO)?BeseyeJSONUtil.ACC_DEMO_VCAM_LST:BeseyeJSONUtil.ACC_PRIVATE_VCAM_LST));
		
		Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(updateGroup.ordinal());
		if(null != mapVcamList){
			mapVcamList.clear();
		}
		
		int iVcamCnt = BeseyeJSONUtil.getJSONInt(objVCamList, strIDVcamCnt);
		if(0 < iVcamCnt){
			JSONArray VcamList = BeseyeJSONUtil.getJSONArray(objVCamList, strIDVcamLst);
			for(int i = 0;i< iVcamCnt;i++){
				try {
					AddUpdateVCam(VcamList.getJSONObject(i), updateGroup);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		
		checkGroupOTAVer(updateGroup);
	}
    
    synchronized public void checkGroupOTAVer(CAM_UPDATE_GROUP updateGroup){		
		Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(updateGroup.ordinal());
		int iVcamCnt = mapVcamList.keySet().size();
		if(0 < iVcamCnt){
			mlLastGroupVerCheckTs[updateGroup.ordinal()] = System.currentTimeMillis();
			Log.i(TAG, "checkGroupOTAVer(), mlLastGroupVerCheckTs:"+mlLastGroupVerCheckTs[updateGroup.ordinal()]+" for gourp "+updateGroup);
			
			Set<String> setVcamIdToCheck = mapVcamList.keySet();
			for(String vcamId : setVcamIdToCheck){
				CamSwUpdateRecord camRec = mapVcamList.get(vcamId);
				checkCamOTAVer(camRec, false);
			}
		}else{
			Log.i(TAG, "checkGroupOTAVer(), empty for gourp "+updateGroup);
			broadcastOnCamGroupUpdateVersionCheck(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_VCAM_LST_EMPTY, 
														  updateGroup, 
														  CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE, 
														  null);
		}
	}
    
    synchronized public void checkCamOTAVer(CamSwUpdateRecord camRec, boolean bForceCheck){		
		if(null != camRec ){
			if(camRec.isOTATriggerredByThisDev() && camRec.getUpdateStatus().equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_FINISH) && camRec.getBeginUpdateTs() < camRec.getLastCamReporteTs()){
				camRec.resetCamOTAInfo();
			}
			
			if(!camRec.isOTATriggerredByThisDev() || bForceCheck){
				BeseyeCamBEHttpTask.CheckCamOTAVersionTask checkCamOTAVersionTask = new BeseyeCamBEHttpTask.CheckCamOTAVersionTask(this);
				if(null != checkCamOTAVersionTask){
					checkCamOTAVersionTask.setDialogId(-1);
					checkCamOTAVersionTask.setCusObj(camRec);
					executeAsyncTask(checkCamOTAVersionTask, camRec.getVCamId());
					camRec.changeUpdateStatus(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_VER_CHECKING);
				}
			}else{
				Log.e(TAG, "checkCamOTAVer(),isOTATriggerredByThisDev is true");
			}	
		}else{
			Log.e(TAG, "checkCamOTAVer(),null camRec ");
		}
	}
    
    synchronized public void checkCamOTAVer(CAM_UPDATE_GROUP eUpdateGroup, String strVcamId, boolean bShowDialog){	
    	CamSwUpdateRecord camRec = findCamSwUpdateRecord(eUpdateGroup, strVcamId);
		if(null != camRec){
			BeseyeCamBEHttpTask.CheckCamOTAVersionTask checkCamOTAVersionTask = new BeseyeCamBEHttpTask.CheckCamOTAVersionTask(this);
			if(null != checkCamOTAVersionTask){
				if(null != camRec.mGetCamUpdateStatusTask){
    				camRec.mGetCamUpdateStatusTask.cancel(true);
    			}
				if(!bShowDialog){
					checkCamOTAVersionTask.setDialogId(-1);
				}
				checkCamOTAVersionTask.setCusObj(camRec);
				executeAsyncTask(checkCamOTAVersionTask, camRec.getVCamId());
				camRec.changeUpdateStatus(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_VER_CHECKING);
			}
		}
	}
  
    synchronized private void CheckVCamListVerChkStatus(CAM_UPDATE_GROUP updateGroup){		
		Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(updateGroup.ordinal());
		
		List<String> lstVCamIdToUpdate = new ArrayList<String>();
		
		boolean bGroupVerCheckFinish = true;
		for(CamSwUpdateRecord rec : mapVcamList.values()){
			if(null != rec ){
				if(rec.meVerCheckStatus.equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_INIT) || mlLastGroupVerCheckTs[updateGroup.ordinal()] > rec.getVerCheckTs()){
					bGroupVerCheckFinish = false;
					break;
				}else if(rec.meVerCheckStatus.equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE)){
					lstVCamIdToUpdate.add(rec.mStrVCamId);
				}
			}
		}
		
		if(bGroupVerCheckFinish){
			int iGroupCnt = mapVcamList.keySet().size() ;
			int iUpdateCnt = lstVCamIdToUpdate.size();
			
			CAM_GROUP_VER_CHK_RET ret = (0 == iUpdateCnt)?CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_ALL_UPDATED:
													 (iGroupCnt == iUpdateCnt)?CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_ALL_OUT_OF_UPDATE:
												     CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_PARTIAL_UPDATED;
			
			Log.i(TAG, "CheckVCamListVerChkStatus(), gourp version check finish :"+updateGroup);
			
			broadcastOnCamGroupUpdateVersionCheck(ret, 
														  updateGroup, 
														  CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE, 
														  lstVCamIdToUpdate);
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, final List<JSONObject> result,final int iRetCode) {
		if(task instanceof BeseyeCamBEHttpTask.CheckCamOTAVersionTask){
			CamSwUpdateRecord camRec = (CamSwUpdateRecord)((BeseyeCamBEHttpTask.CheckCamOTAVersionTask)task).getCusObj();
			
			if(null != camRec){
				CAM_UPDATE_VER_CHECK_STATUS prevStatus = camRec.getVerCheckStatus();
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					
					boolean bCanBeUpdate = BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.UPDATE_CAN_GO);
					camRec.mlVerCheckTs = System.currentTimeMillis();
					camRec.meVerCheckStatus = bCanBeUpdate?CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE
														  :CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_UPDATED;
					
				}else if(Integer.MIN_VALUE == iRetCode){
					camRec.meVerCheckStatus = CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_TIMEOUT;
				}else{
					camRec.meVerCheckStatus = CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_ERR;
					camRec.miErrCode = iRetCode;
				}	
				
				broadcastOnCamUpdateVerChkStatusChanged(camRec.mStrVCamId, camRec.meVerCheckStatus , prevStatus, camRec);
				
				CheckVCamListVerChkStatus(camRec.meUpdateGroup);
			}
		}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
			CAM_UPDATE_GROUP updateGroup = findCamUpdateGroupByTask((BeseyeAccountTask.GetVCamListTask)task);
			if(null == updateGroup){
				Log.e(TAG, "onPostExecute(), find invalid update group");
			}else{
				if(!task.isCancelled()){
					if(0 == iRetCode){
						JSONObject objVCamList = result.get(0);
						fillUpdateVCamList(objVCamList, updateGroup);
					}else{
						broadcastOnCamGroupUpdateVersionCheck(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_ERROR, 
																	  updateGroup, 
																	  CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_GET_VCAM_LST_FALED, 
																	  null);
					}
				}
				mGetVCamListTasks[updateGroup.ordinal()] = null;
			}
		}else if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
			final CamSwUpdateRecord camRec = (CamSwUpdateRecord)((BeseyeCamBEHttpTask.UpdateCamSWTask)task).getCusObj();
			final String strVcamId = ((BeseyeCamBEHttpTask.UpdateCamSWTask)task).getVcamId();
			if(!task.isCancelled()){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString()+", strVcamId:"+strVcamId);
					
					if(null != camRec){
						camRec.miErrCode = 0;
						camRec.mbUpdateTriggerred = true;
						camRec.mlBeginUpdateTs = System.currentTimeMillis();
						camRec.changeUpdateStatus(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING);
						
						checkCamUpdateStatus(camRec, false);
					}
				}else{
					if(iRetCode == BeseyeError.E_OTA_SW_UPDATING){
						if(DEBUG)
							Log.i(TAG, "onPostExecute(), strVcamId["+strVcamId+"] is updating");
						if(null != camRec){
							camRec.changeUpdateStatus( CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING);
							checkCamUpdateStatus(camRec, false);
						}
					}else{
						camRec.miErrCode = iRetCode;
						camRec.changeUpdateStatus( CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_ERR);
					}
					
					if(iRetCode != BeseyeError.E_OTA_SW_ALRADY_LATEST &&
					   iRetCode != BeseyeError.E_OTA_SW_UPDATING &&	
					   iRetCode != BeseyeError.E_WEBSOCKET_CONN_NOT_EXIST && 
					   iRetCode != BeseyeError.E_WEBSOCKET_OPERATION_FAIL && 
					   !((BeseyeHttpTask)task).isNetworkTimeoutErr() && 
					   !BeseyeUtils.isProductionVersion()){
						
						String strMsg = (0 < result.size())?BeseyeJSONUtil.getJSONString(result.get(0), "exceptionMessage"):"";
						Log.i(TAG, "onPostExecute(), "+String.format("Msg:[%s]\nerrCode:[0x%x]\ncamRec:%s", strMsg, iRetCode, (null != camRec)?camRec.toString():""));
						
	//					BeseyeUtils.postRunnable(new Runnable(){
	//						@Override
	//						public void run() {
	//							
	//
	////							if(BeseyeBaseActivity.this.mActivityResume){
	////								String strMsg = (0 < result.size())?BeseyeJSONUtil.getJSONString(result.get(0), "exceptionMessage"):"";
	////								Bundle b = new Bundle();
	////	        					b.putString(KEY_INFO_TITLE, "Cam update failed");
	////	        					b.putString(KEY_INFO_TEXT, String.format("Msg:[%s]\nerrCode:[0x%x]\nCamName:[%s]\nVcam_id:[%s]", strMsg, iRetCode, findCamNameFromVcamUpdateList(strVcamId), strVcamId));
	////	        					showMyDialog(DIALOG_ID_INFO, b);
	////							}
	//						}}, 200);
					}
				}
			}
			
			camRec.mUpdateCamSWTask = null;
		}else if(task instanceof BeseyeCamBEHttpTask.GetCamUpdateStatusTask){
			final CamSwUpdateRecord camRec = (CamSwUpdateRecord)((BeseyeCamBEHttpTask.GetCamUpdateStatusTask)task).getCusObj();
			final String strVcamId = ((BeseyeCamBEHttpTask.GetCamUpdateStatusTask)task).getVcamId();
			if(!task.isCancelled()){
				if(0 == iRetCode){					
					JSONObject objCamUpdateStatus = result.get(0);
					if(null != objCamUpdateStatus && null != camRec){
						final int iFinalStatus = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_FINAL_STAUS, -1);
						final int iDetailStatus = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_DETAIL_STAUS, 0);
	
						camRec.mlLastCamReportTs = BeseyeJSONUtil.getJSONLong(objCamUpdateStatus, BeseyeJSONUtil.LAST_CAM_UPDATE_TS);
	
						if(DEBUG)
							Log.i(TAG, "updateCamUpdateProgress(), strVcamId:"+camRec.mStrVCamId+", iFinalStatus="+iFinalStatus+", iDetailStatus="+iDetailStatus+", camRec.mlLastCamReporteTs="+camRec.mlLastCamReportTs);
						
	
						if(-1 ==  iFinalStatus){//updating
							//For demo/private cam handling
							if(camRec.meUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_INIT) || 	
							  (camRec.meUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_VER_CHECKING)/* && camRec.mlVerCheckTs < camRec.mlLastCamReporteTs*/)){
								camRec.setCamOnlineAfterOTATs(-1);
								camRec.changeUpdateStatus( CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING);
							}
							camRec.miErrCode = 0;
							camRec.miUpdatePercentage = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_PROGRESS, 0);
							broadcastOnCamUpdateProgressChanged(strVcamId, camRec.miUpdatePercentage);
							this.checkCamUpdateStatus(camRec, false);
						}else{
							if(0 == iFinalStatus || 1 == iFinalStatus){//Update Done
								if(camRec.mlVerCheckTs < camRec.mlLastCamReportTs){
									camRec.miErrCode = 0;
									camRec.miUpdatePercentage = 100;//BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_PROGRESS, 0);
									camRec.changeUpdateStatus( CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_FINISH);
								}
							}else{//Update Failed							
								camRec.mlLastOTAErrorTs = camRec.mlLastCamReportTs;
								camRec.miErrCode = BeseyeError.E_NOT_ENOUGH_SPACE;//0 == iDetailStatus?iFinalStatus:iDetailStatus;
								
								if(camRec.isRebootErrWhenOTAPrepare()){
									Log.i(TAG, "updateCamUpdateProgress(), meet E_REBOOT_DURING_PREPARING_STAGE, reset");
									//power off before 20%
									BeseyeUtils.postRunnable(new Runnable(){
										@Override
										public void run() {
											camRec.resetErrorInfo();
											checkCamOTAVer(camRec, true);
										}}, 0);
								}else{
									camRec.changeUpdateStatus( CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_ERR);							
								}
							}
						}
					}
				}else{
					//If task timeout, keep tracking 
					if(Integer.MIN_VALUE != iRetCode){
						camRec.miErrCode = iRetCode;
						camRec.changeUpdateStatus( CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_ERR);
					}
				}
			}
			camRec.mGetCamUpdateStatusTask = null;
		}
	}
	
	@Override
	public void onShowDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId, int iTitleRes, int iMsgRes) {
		synchronized(this){
			OnCamUpdateStatusChangedListener listener = null;
			for(WeakReference<OnCamUpdateStatusChangedListener> listenerChk : mLstOnCamUpdateStatusChangedListener){
				if(null != listenerChk && null != (listener = listenerChk.get())){
					listener.onShowDialog(task, iDialogId, iTitleRes, iMsgRes);
				}
			}
		}
	}

	@Override
	public void onDismissDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId) {
		synchronized(this){
			OnCamUpdateStatusChangedListener listener = null;
			for(WeakReference<OnCamUpdateStatusChangedListener> listenerChk : mLstOnCamUpdateStatusChangedListener){
				if(null != listenerChk && null != (listener = listenerChk.get())){
					listener.onDismissDialog(task, iDialogId);
				}
			}
		}
	}

	@Override
	public void onToastShow(AsyncTask<String, Double, List<JSONObject>> task, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionInvalid(AsyncTask<String, Double, List<JSONObject>> task, int iInvalidReason) {
		
	}
}
