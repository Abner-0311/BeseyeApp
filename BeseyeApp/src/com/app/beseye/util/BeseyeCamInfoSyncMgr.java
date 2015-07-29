package com.app.beseye.util;

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
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;

public class BeseyeCamInfoSyncMgr implements OnHttpTaskCallback{
	static private BeseyeCamInfoSyncMgr sBeseyeCamInfoSyncMgr;
	
	static public BeseyeCamInfoSyncMgr getInstance(){
		if(null == sBeseyeCamInfoSyncMgr){
			sBeseyeCamInfoSyncMgr = new BeseyeCamInfoSyncMgr();
		}
		return sBeseyeCamInfoSyncMgr;
	}
	
	private List<WeakReference<OnCamInfoChangedListener>> mLstOnCamInfoChangedListener;
	private Map<String, JSONObject> mMapCamInfo;
	
	private BeseyeCamInfoSyncMgr() {
		mLstOnCamInfoChangedListener = new ArrayList<WeakReference<OnCamInfoChangedListener>>();
		mMapCamInfo = new HashMap<String, JSONObject>();
	}
	
	static public interface OnCamInfoChangedListener{
		public void onCamSetupChanged(String strVcamId, long lTs, JSONObject objCamSetup);
	}
	
	synchronized public void registerOnCamInfoChangedListener(OnCamInfoChangedListener listener){
		//Log.i(TAG, "registerOnCamInfoChangedListener(), listener:"+listener);
		if(null != listener && null == findOnCamInfoChangedListener(listener)){
			//Log.i(TAG, "registerOnCamInfoChangedListener(), listener add OK");
			mLstOnCamInfoChangedListener.add(new WeakReference<OnCamInfoChangedListener>(listener));
		}
	}
	
	synchronized public void unregisterOnCamInfoChangedListener(OnCamInfoChangedListener listener){
		WeakReference<OnCamInfoChangedListener> ret = null;
		if(null != listener && null != (ret = findOnCamInfoChangedListener(listener))){
			mLstOnCamInfoChangedListener.remove(ret);
		}
	}
	
	synchronized private WeakReference<OnCamInfoChangedListener> findOnCamInfoChangedListener(OnCamInfoChangedListener listener){
		WeakReference<OnCamInfoChangedListener> ret = null;
		if(null != listener){
			for(WeakReference<OnCamInfoChangedListener> listenerChk : mLstOnCamInfoChangedListener){
				if(null != listenerChk && listener.equals(listenerChk.get())){
					ret = listenerChk;
					break;
				}
			}
		}	
		//Log.i(TAG, "findOnCamInfoChangedListener(), ret : "+((null != ret)?ret.get().toString():""));
		return ret;
	} 
	
	private WeakReference<OnCamUpdateVersionCheckListener> mOnCamUpdateVersionCheckListener;
	
	static public interface OnCamUpdateVersionCheckListener{
		public void onCamUpdateList(JSONArray arrUpdateCandidate);
	}
	
	synchronized public void registerOnCamUpdateVersionCheckListener(OnCamUpdateVersionCheckListener listener){
		mOnCamUpdateVersionCheckListener = new WeakReference<OnCamUpdateVersionCheckListener>(listener);
	}
	
	synchronized public void unregisterOnCamUpdateVersionCheckListener(OnCamUpdateVersionCheckListener listener){
		mOnCamUpdateVersionCheckListener = null;
	}
	
	
	synchronized public JSONObject getCamInfoByVCamId(String strVcamId){
		JSONObject objRet = null;
		if(null != strVcamId && 0 < strVcamId.length() && null != mMapCamInfo){
			objRet= mMapCamInfo.get(strVcamId);
		}
		return objRet;
	}
	
	synchronized public void removeCamInfoByVCamId(String strVcamId){
		if(null != strVcamId && 0 < strVcamId.length() && null != mMapCamInfo){
			mMapCamInfo.remove(strVcamId);
		}
	}
	
	synchronized public Set<String> getVCamIdList(){
		return (null != mMapCamInfo)?mMapCamInfo.keySet():null;
	}
	
	synchronized public void updateCamInfo(final String strVcamId, final JSONObject objCamSetup){
		final List<WeakReference<OnCamInfoChangedListener> > copy = new ArrayList<WeakReference<OnCamInfoChangedListener>>(mLstOnCamInfoChangedListener);
		if(null != mMapCamInfo){
			mMapCamInfo.put(strVcamId, objCamSetup);
		}
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				for(WeakReference<OnCamInfoChangedListener> listenerChk : copy){
					OnCamInfoChangedListener listener = (null != listenerChk)?listenerChk.get():null;
					if(null != listener){
						listener.onCamSetupChanged(strVcamId, BeseyeJSONUtil.getJSONLong(objCamSetup, BeseyeJSONUtil.OBJ_TIMESTAMP), objCamSetup);
					}
				}
				
				if(false == BeseyeJSONUtil.getJSONBoolean(objCamSetup, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
					if(null != mMapCamInfo){
						mMapCamInfo.remove(strVcamId);
					}
				}
			}}, 0);
	}
	
	private static ExecutorService SINGLE_TASK_EXECUTOR; 
	static {  
		SINGLE_TASK_EXECUTOR = (ExecutorService) Executors.newSingleThreadExecutor();  
    }; 
    
	private AsyncTask<String, Double, List<JSONObject>> mCheckCamListVersionTask = null; 
	private JSONArray mArrVcamList = null;
	private int miCurCheckIdx = 0;
	private JSONArray mLstUpdateCandidate = new JSONArray();
	
	public void queryCamUpdateVersions(JSONArray arrVcamIdList){
		if(!BeseyeFeatureConfig.CAM_SW_UPDATE_CHK || SessionMgr.getInstance().getIsCamSWUpdateSuspended()){
			return;
		}
		
		if(null != mCheckCamListVersionTask && false == mCheckCamListVersionTask.isCancelled()){
			if(DEBUG)
				Log.i(TAG, "queryCamUpdateVersions(), mCheckCamListVersionTask is ongoing");
			return;
		}
		
		mArrVcamList = arrVcamIdList;
		miCurCheckIdx = 0;
		mLstUpdateCandidate = new JSONArray();
		checkVersionByIdx();
	}
	
	private void checkVersionByIdx(){
		if(null != mArrVcamList){
			if(miCurCheckIdx == mArrVcamList.length()){
				//report
				if(DEBUG)
					Log.i(TAG, "checkVersionByIdx(), mLstUpdateCandidate="+mLstUpdateCandidate.toString());
				OnCamUpdateVersionCheckListener listener = (null != mOnCamUpdateVersionCheckListener)?mOnCamUpdateVersionCheckListener.get():null;
				if(null != mLstUpdateCandidate && 0 < mLstUpdateCandidate.length() && null != listener){
					listener.onCamUpdateList(mLstUpdateCandidate);
				}
			}else{
				mCheckCamListVersionTask = new BeseyeCamBEHttpTask.CheckCamUpdateStatusTask(this).setDialogId(-1);
				if(null != mCheckCamListVersionTask){
					JSONObject camObj = mArrVcamList.optJSONObject(miCurCheckIdx++);
					((BeseyeCamBEHttpTask.CheckCamUpdateStatusTask)mCheckCamListVersionTask).setCamObj(camObj);
					
					if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1){
						mCheckCamListVersionTask.execute(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID));
	        		}else{
						mCheckCamListVersionTask.executeOnExecutor(SINGLE_TASK_EXECUTOR, BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID));
	        		}
				}
			}
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(task instanceof BeseyeCamBEHttpTask.CheckCamUpdateStatusTask){
			//final String strVcamId = ((BeseyeCamBEHttpTask.CheckCamUpdateStatusTask)task).getVcamId();
			if(0 == iRetCode){
				if(DEBUG)
					Log.i(TAG, "onPostExecute(), "+result.toString());
				
				if(BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.UPDATE_CAN_GO)){
					if(null != mLstUpdateCandidate){
						mLstUpdateCandidate.put(((BeseyeCamBEHttpTask.CheckCamUpdateStatusTask)task).getCamObj());
					}
				}			
			}
			
			checkVersionByIdx();
		}
		
		mCheckCamListVersionTask = null;
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
		// TODO Auto-generated method stub
		
	}
}
