package com.app.beseye.ota;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
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
		public void onCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec);
		public void onCamUpdateProgress(String strVcamId, int iPercetage);
		public void onSessionInvalid(AsyncTask<String, Double, List<JSONObject>> task, int iInvalidReason);
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
	
	private boolean[] mbNeedPeriodCheckUpdateStatus = null;
	private Runnable[] mPeriodCheckUpdateStatusRunnable = null;
	static private final long PERIOD_TO_CHECK = 5000L;
	
	class PeriodCheckUpdateStatusRunnable implements Runnable{
		public CAM_UPDATE_GROUP meUpdateGroup;
		
		public PeriodCheckUpdateStatusRunnable(CAM_UPDATE_GROUP group){
			meUpdateGroup = group;
		}
		
		@Override
		public void run() {
			if(mbNeedPeriodCheckUpdateStatus[meUpdateGroup.ordinal()]){
				checkGroupCamUpdateStatus(meUpdateGroup);
				BeseyeUtils.postRunnable(mPeriodCheckUpdateStatusRunnable[meUpdateGroup.ordinal()], PERIOD_TO_CHECK);
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
		mbNeedPeriodCheckUpdateStatus = new boolean[CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal()];
		mPeriodCheckUpdateStatusRunnable = new Runnable[CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal()];
		for(int i = 0; i < CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_COUNT.ordinal();i++){
			mLstMapCamSwUpdateRecord.add(new HashMap<String, CamSwUpdateRecord>());
			mbNeedPeriodCheckUpdateStatus[i] = false;
		}
	}
	
	public void setNeedPeriodCheckUpdateStatus(CAM_UPDATE_GROUP group, boolean bEnabled){
		if(bEnabled){
			mbNeedPeriodCheckUpdateStatus[group.ordinal()] = true;
			if(null == mPeriodCheckUpdateStatusRunnable){
				mPeriodCheckUpdateStatusRunnable[group.ordinal()] = new PeriodCheckUpdateStatusRunnable(group);
				BeseyeUtils.postRunnable(mPeriodCheckUpdateStatusRunnable[group.ordinal()], 0);
			}
		}else{
			mbNeedPeriodCheckUpdateStatus[group.ordinal()] = false;
			BeseyeUtils.removeRunnable(mPeriodCheckUpdateStatusRunnable[group.ordinal()]);
			mPeriodCheckUpdateStatusRunnable[group.ordinal()] =null;
		}
	}
	
	public class CamSwUpdateRecord{
		@Override
		public String toString() {
			return "CamSwUpdateRecord [mStrVCamId=" + mStrVCamId
					+ ", mStrCamName=" + mStrCamName + ", meUpdateStatus="
					+ meUpdateStatus + ", meUpdateErrType=" + meUpdateErrType
					+ ", meUpdateGroup=" + meUpdateGroup
					+ ", meVerCheckStatus=" + meVerCheckStatus
					+ ", mbUpdateTriggerred=" + mbUpdateTriggerred
					+ ", miErrCode=" + miErrCode + ", miUpdatePercentage="
					+ miUpdatePercentage + ", mlBeginUpdateTs="
					+ mlBeginUpdateTs + ", mlLastUpdateTs=" + mlLastUpdateTs
					+ ", mlLastOTAErrorTs=" + mlLastOTAErrorTs
					+ ", mlLastUserFeedbackTs=" + mlLastUserFeedbackTs
					+ ", mUpdateCamSWTask=" + mUpdateCamSWTask
					+ ", mGetCamUpdateStatusTask="
					+ mGetCamUpdateStatusTask + "]";
		}

		public String mStrVCamId;
		public String mStrCamName;
		public CAM_UPDATE_STATUS meUpdateStatus;
		public CAM_UPDATE_ERROR meUpdateErrType;
		public CAM_UPDATE_GROUP meUpdateGroup;
		public CAM_UPDATE_VER_CHECK_STATUS meVerCheckStatus;
		
		public boolean mbUpdateTriggerred;
		
		public int miErrCode;
		public int miUpdatePercentage;
		public long mlBeginUpdateTs;
		public long mlLastUpdateTs;
		public long mlLastOTAErrorTs;
		public long mlLastUserFeedbackTs;
		public BeseyeCamBEHttpTask.UpdateCamSWTask mUpdateCamSWTask;
		public BeseyeCamBEHttpTask.GetCamUpdateStatusTask mGetCamUpdateStatusTask;
		
		public CamSwUpdateRecord(String strVCamId, CAM_UPDATE_GROUP eUpdateGroup, String strCamName){
			this.mStrVCamId = strVCamId;
			this.mStrCamName = strCamName;
			this.meUpdateGroup = eUpdateGroup;
			init();
		}
		
		private void init(){
			this.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_INIT;
			this.meUpdateErrType = CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE;
			this.meVerCheckStatus = CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_INIT;
			this.mbUpdateTriggerred = false;
			this.miErrCode = 0;
			this.miUpdatePercentage = -1;
			this.mlBeginUpdateTs = this.mlLastUpdateTs = this.mlLastUserFeedbackTs = this.mlLastOTAErrorTs = -1;
			this.mUpdateCamSWTask = null;
			this.mGetCamUpdateStatusTask = null;
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
		if(null != listener && null != (ret = findOnCamGroupUpdateVersionCheckListener(listener))){
			mLstOnCamGroupUpdateVersionCheckListener.remove(ret);
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
	
	synchronized private void broadcastOnCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec){
		
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
	
	synchronized public void registerOnCamUpdateStatusChangedListener(OnCamUpdateStatusChangedListener listener){
		if(null != listener && null == findOnCamUpdateStatusChangedListener(listener)){
			//Log.i(TAG, "registerOnCamUpdateStatusChangedListener(), listener add OK");
			mLstOnCamUpdateStatusChangedListener.add(new WeakReference<OnCamUpdateStatusChangedListener>(listener));
		}
	}
	
	synchronized public void unregisterOnCamUpdateStatusChangedListener(OnCamUpdateStatusChangedListener listener){
		WeakReference<OnCamUpdateStatusChangedListener> ret = null;
		if(null != listener && null != (ret = findOnCamUpdateStatusChangedListener(listener))){
			mLstOnCamUpdateStatusChangedListener.remove(ret);
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
    
    synchronized public void performCamGroupUpdateCheck(CAM_UPDATE_GROUP eUpdateGroup){
		if(null == mGetVCamListTasks[eUpdateGroup.ordinal()]){
			mGetVCamListTasks[eUpdateGroup.ordinal()] = new BeseyeAccountTask.GetVCamListTask(this);
			executeAsyncTask(mGetVCamListTasks[eUpdateGroup.ordinal()]);
			Log.i(TAG, "performCamGroupUpdateCheck(), mGetVCamListTasks["+eUpdateGroup.ordinal()+"] is launching...");
		}else{
			if(DEBUG)
				Log.i(TAG, "performCamGroupUpdateCheck(), mGetVCamListTasks["+eUpdateGroup.ordinal()+"] is ongoing");
		}
	}
    
    synchronized public void performCamGroupUpdate(CAM_UPDATE_GROUP eUpdateGroup){
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	if(0 < mapVcamList.size()){
    		for(CamSwUpdateRecord camRec : mapVcamList.values()){
    			if(null != camRec && camRec.meVerCheckStatus.equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE)){
    				CAM_UPDATE_STATUS prevStatus = camRec.meUpdateStatus;
					camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_REQUEST;
					broadcastOnCamUpdateStatusChanged(camRec.mStrVCamId, camRec.meUpdateStatus, prevStatus, camRec);
    				performCamUpdate(camRec);
    			}
    		}
    	}
	}
    
    synchronized public void performCamUpdate(CamSwUpdateRecord camRec){
    	if(null != camRec){
    		if(null == camRec.mUpdateCamSWTask){
    			camRec.mUpdateCamSWTask = new BeseyeCamBEHttpTask.UpdateCamSWTask(this);
    			camRec.mUpdateCamSWTask.setCusObj(camRec);
            	executeAsyncTask(camRec.mUpdateCamSWTask, camRec.mStrVCamId);
    		}else{
    			if(DEBUG)
    				Log.i(TAG, "performCamUpdate(), mUpdateCamSWTask["+camRec.mStrVCamId+"] is ongoing");
    		}
    	}else{
			Log.e(TAG, "performCamUpdate(), camRec is null ");
    	}
    }
    
    synchronized public void checkGroupCamUpdateStatus(CAM_UPDATE_GROUP eUpdateGroup){
    	Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(eUpdateGroup.ordinal());
    	if(0 < mapVcamList.size()){
    		for(CamSwUpdateRecord camRec : mapVcamList.values()){
    			if(null != camRec && camRec.meUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING)){
    				checkCamUpdateStatus(camRec);
    			}
    		}
    	}
    }
    
    synchronized public void checkCamUpdateStatus(CamSwUpdateRecord camRec){
    	if(null != camRec){
    		if(null == camRec.mGetCamUpdateStatusTask){
    			camRec.mGetCamUpdateStatusTask = new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this);
    			camRec.mGetCamUpdateStatusTask.setCusObj(camRec);

            	executeAsyncTask(camRec.mGetCamUpdateStatusTask, camRec.mStrVCamId);
    		}else{
    			if(DEBUG)
    				Log.i(TAG, "checkCamUpdateStatus(), mGetCamUpdateStatusTask["+camRec.mStrVCamId+"] is ongoing");
    		}
    	}else{
			Log.e(TAG, "mGetCamUpdateStatusTask(), camRec is null ");
    	}
    }
    
    private void executeAsyncTask(BeseyeHttpTask task, String... params){
    	if(null != task){
    		task.setDialogId(-1);
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
    
    private void fillUpdateVCamList(JSONObject objVCamList, CAM_UPDATE_GROUP updateGroup){		
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
					JSONObject camObj = VcamList.getJSONObject(i);
					if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
						String strVcamId = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID);
						String strCamName = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_NAME);
						mapVcamList.put(strVcamId, new CamSwUpdateRecord(strVcamId, updateGroup, strCamName));
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			Set<String> setVcamIdToCheck = mapVcamList.keySet();
			for(String vcamId : setVcamIdToCheck){
				BeseyeCamBEHttpTask.CheckCamUpdateStatusTask checkCamListVersionTask = new BeseyeCamBEHttpTask.CheckCamUpdateStatusTask(this);
				if(null != checkCamListVersionTask){
					CamSwUpdateRecord camRec = mapVcamList.get(vcamId);
					checkCamListVersionTask.setDialogId(-1);
					checkCamListVersionTask.setCusObj(camRec);
					executeAsyncTask(checkCamListVersionTask, vcamId);
					
					CAM_UPDATE_STATUS prevStatus = camRec.meUpdateStatus;
					camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_VER_CHECKING;
					broadcastOnCamUpdateStatusChanged(camRec.mStrVCamId, camRec.meUpdateStatus, prevStatus, camRec);
				}
				
			}
			
		}else{
			Log.i(TAG, "fillUpdateVCamList(), empty for gourp "+updateGroup);
			broadcastOnCamGroupUpdateVersionCheck(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_VCAM_LST_EMPTY, 
														  updateGroup, 
														  CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE, 
														  null);
		}
	}
    
    private void CheckVCamListVerChkStatus(CAM_UPDATE_GROUP updateGroup){		
		Map<String, CamSwUpdateRecord> mapVcamList =  mLstMapCamSwUpdateRecord.get(updateGroup.ordinal());
		
		List<String> lstVCamIdToUpdate = new ArrayList<String>();
		
		boolean bGroupVerCheckFinish = true;
		for(CamSwUpdateRecord rec : mapVcamList.values()){
			if(null != rec ){
				if(rec.meVerCheckStatus.equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_INIT)){
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
		if(task instanceof BeseyeCamBEHttpTask.CheckCamUpdateStatusTask){
			CamSwUpdateRecord camRec = (CamSwUpdateRecord)((BeseyeCamBEHttpTask.CheckCamUpdateStatusTask)task).getCusObj();
			
			if(null != camRec){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					
					boolean bCanBeUpdate = BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.UPDATE_CAN_GO);
					camRec.meVerCheckStatus = bCanBeUpdate?CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE
														  :CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_UPDATED;
				}else if(Integer.MIN_VALUE == iRetCode){
					camRec.meVerCheckStatus = CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_TIMEOUT;
				}else{
					camRec.meVerCheckStatus = CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_ERR;
					camRec.miErrCode = iRetCode;
				}
				
				CheckVCamListVerChkStatus(camRec.meUpdateGroup);
			}
		}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
			CAM_UPDATE_GROUP updateGroup = findCamUpdateGroupByTask((BeseyeAccountTask.GetVCamListTask)task);
			if(null == updateGroup){
				Log.e(TAG, "onPostExecute(), find invalid update group");
			}else{
				if(0 == iRetCode){
					JSONObject objVCamList = result.get(0);
					fillUpdateVCamList(objVCamList, updateGroup);
				}else{
					broadcastOnCamGroupUpdateVersionCheck(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_ERROR, 
																  updateGroup, 
																  CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_GET_VCAM_LST_FALED, 
																  null);
				}
				mGetVCamListTasks[updateGroup.ordinal()] = null;
			}
		}else if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
			final CamSwUpdateRecord camRec = (CamSwUpdateRecord)((BeseyeCamBEHttpTask.UpdateCamSWTask)task).getCusObj();
			final String strVcamId = ((BeseyeCamBEHttpTask.UpdateCamSWTask)task).getVcamId();
			if(0 == iRetCode){
				if(DEBUG)
					Log.i(TAG, "onPostExecute(), "+result.toString()+", strVcamId:"+strVcamId);
				
				if(null != camRec){
					CAM_UPDATE_STATUS prevStatus = camRec.meUpdateStatus;
					camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING;
					broadcastOnCamUpdateStatusChanged(strVcamId, camRec.meUpdateStatus, prevStatus, camRec);
					
					checkCamUpdateStatus(camRec);
				}
			}else{
				if(iRetCode == BeseyeError.E_OTA_SW_UPDATING){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), strVcamId["+strVcamId+"] is updating");
					if(null != camRec){
						camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING;
						
						CAM_UPDATE_STATUS prevStatus = camRec.meUpdateStatus;
						camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING;
						broadcastOnCamUpdateStatusChanged(strVcamId, camRec.meUpdateStatus, prevStatus, camRec);
						
						checkCamUpdateStatus(camRec);
					}
				}else{
					CAM_UPDATE_STATUS prevStatus = camRec.meUpdateStatus;
					camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_ERR;
					camRec.miErrCode = iRetCode;
					broadcastOnCamUpdateStatusChanged(strVcamId, camRec.meUpdateStatus, prevStatus, camRec);
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
		}else if(task instanceof BeseyeCamBEHttpTask.GetCamUpdateStatusTask){
			final CamSwUpdateRecord camRec = (CamSwUpdateRecord)((BeseyeCamBEHttpTask.GetCamUpdateStatusTask)task).getCusObj();
			final String strVcamId = ((BeseyeCamBEHttpTask.GetCamUpdateStatusTask)task).getVcamId();
			if(0 == iRetCode){					
				JSONObject objCamUpdateStatus = result.get(0);
				if(null != objCamUpdateStatus && null != camRec){
					int iFinalStatus = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_FINAL_STAUS, -1);
					
					if(DEBUG)
						Log.i(TAG, "updateCamUpdateProgress(), "+objCamUpdateStatus+", strVcamId:"+camRec.mStrVCamId+", iFinalStatus="+iFinalStatus);
					
					if(-1 ==  iFinalStatus){//updating
						camRec.miUpdatePercentage = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_PROGRESS, 0);
						camRec.mlLastUpdateTs = BeseyeJSONUtil.getJSONLong(objCamUpdateStatus, BeseyeJSONUtil.LAST_CAM_UPDATE_TS);
						broadcastOnCamUpdateProgressChanged(strVcamId, camRec.miUpdatePercentage);
					}else{
						CAM_UPDATE_STATUS prevStatus = camRec.meUpdateStatus;

						if(0 == iFinalStatus || 1 == iFinalStatus){//Update Done
							camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_FINISH;
							camRec.miUpdatePercentage = 100;//BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_PROGRESS, 0);
						}else{//Update Failed							
							camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_ERR;
							camRec.miErrCode = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_DETAIL_STAUS, -1);
						}
						broadcastOnCamUpdateStatusChanged(strVcamId, camRec.meUpdateStatus, prevStatus, camRec);
					}
				}
			}else{
				//If task timeout, keep tracking 
				if(Integer.MIN_VALUE != iRetCode){
					CAM_UPDATE_STATUS prevStatus = camRec.meUpdateStatus;
					camRec.meUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_ERR;
					camRec.miErrCode = iRetCode;
					broadcastOnCamUpdateStatusChanged(strVcamId, camRec.meUpdateStatus, prevStatus, camRec);
				}
			}
		}
	}
	
	@Override
	public void onShowDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId, int iTitleRes,
			int iMsgRes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDismissDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onToastShow(AsyncTask<String, Double, List<JSONObject>> task, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionInvalid(AsyncTask<String, Double, List<JSONObject>> task, int iInvalidReason) {
		
	}
}
