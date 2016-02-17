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
import com.app.beseye.util.BeseyeCamInfoSyncMgr.OnCamInfoChangedListener;

public class BeseyeCamSWVersionMgr implements OnHttpTaskCallback{
	static private BeseyeCamSWVersionMgr sBeseyeCamSWVersionMgr;
	
	static public BeseyeCamSWVersionMgr getInstance(){
		if(null == sBeseyeCamSWVersionMgr){
			sBeseyeCamSWVersionMgr = new BeseyeCamSWVersionMgr();
		}
		return sBeseyeCamSWVersionMgr;
	}
	
	private List<WeakReference<OnCamUpdateVersionCheckListener>> mLstOnCamUpdateVersionCheckListener;
	
	private BeseyeCamSWVersionMgr() {
		mLstOnCamUpdateVersionCheckListener = new ArrayList<WeakReference<OnCamUpdateVersionCheckListener>>();
	}
	
	private Map<String, CamSwUpdateRecord> mMapCamSwUpdateRecord = new HashMap<String, CamSwUpdateRecord>();
	
	public class CamSwUpdateRecord{
		public String strVCamId;
		public CAM_UPDATE_STATUS eUpdateStatus;
		public CAM_UPDATE_ERROR eUpdateErrType;
		public int iErrCode;
		public int iUpdatePercentage;
		public long lBeginUpdateTs;
		public long lLastUpdateTs;
		public long lLastUserFeedbackTs;
		
		public CamSwUpdateRecord(){
			strVCamId = null;
			eUpdateStatus = CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_INIT;
			eUpdateErrType = CAM_UPDATE_ERROR.CAM_UPDATE_ERROR_NONE;
			iErrCode = 0;
			iUpdatePercentage = -1;
			lBeginUpdateTs = lLastUpdateTs = lLastUserFeedbackTs;
		}
	}
	
	public enum CAM_UPDATE_STATUS{
		CAM_UPDATE_STATUS_INIT,
		CAM_UPDATE_STATUS_VER_CHECKING,
		CAM_UPDATE_STATUS_UPDATE_START,
		CAM_UPDATE_STATUS_UPDATE_END,
		CAM_UPDATE_STATUS_UPDATE_ERR,
		CAM_UPDATE_STATUS_COUNT
	}
	
	public enum CAM_UPDATE_ERROR{
		CAM_UPDATE_ERROR_NONE,
		CAM_UPDATE_ERROR_GET_VCAM_LST_FALED,
		CAM_UPDATE_ERROR_VER_CHECK_FAILED,
		CAM_UPDATE_ERROR_TRIGGER_UPDATE_FAILED,
		CAM_UPDATE_ERROR_NO_RESPONSE,
		CAM_UPDATE_ERROR_COUNT
	}
		
	static public interface OnCamUpdateVersionCheckListener{
		public void onCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec);
		public void onCamUpdateProgress(String strVcamId, int iPercetage);
	}
	
	synchronized public void registerOnCamUpdateVersionCheckListener(OnCamUpdateVersionCheckListener listener){
		if(null != listener && null == findOnCamUpdateVersionCheckListener(listener)){
			//Log.i(TAG, "registerOnCamUpdateVersionCheckListener(), listener add OK");
			mLstOnCamUpdateVersionCheckListener.add(new WeakReference<OnCamUpdateVersionCheckListener>(listener));
		}
	}
	
	synchronized public void unregisterOnCamUpdateVersionCheckListener(OnCamUpdateVersionCheckListener listener){
		WeakReference<OnCamUpdateVersionCheckListener> ret = null;
		if(null != listener && null != (ret = findOnCamUpdateVersionCheckListener(listener))){
			mLstOnCamUpdateVersionCheckListener.remove(ret);
		}
	}
	
	synchronized private WeakReference<OnCamUpdateVersionCheckListener> findOnCamUpdateVersionCheckListener(OnCamUpdateVersionCheckListener listener){
		WeakReference<OnCamUpdateVersionCheckListener> ret = null;
		if(null != listener){
			for(WeakReference<OnCamUpdateVersionCheckListener> listenerChk : mLstOnCamUpdateVersionCheckListener){
				if(null != listenerChk && listener.equals(listenerChk.get())){
					ret = listenerChk;
					break;
				}
			}
		}	
		//Log.i(TAG, "findOnCamUpdateVersionCheckListener(), ret : "+((null != ret)?ret.get().toString():""));
		return ret;
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
//				OnCamUpdateVersionCheckListener listener = (null != mOnCamUpdateVersionCheckListener)?mOnCamUpdateVersionCheckListener.get():null;
//				if(null != mLstUpdateCandidate && 0 < mLstUpdateCandidate.length() && null != listener){
//					listener.onCamUpdateList(mLstUpdateCandidate);
//				}
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
