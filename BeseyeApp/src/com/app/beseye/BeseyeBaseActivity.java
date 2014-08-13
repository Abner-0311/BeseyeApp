package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.CAM_STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONInt;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_TS;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.BeseyeApplication.BeseyeAppStateChangeListener;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.ISessionUpdateCallback;
import com.app.beseye.httptask.SessionMgr.SessionData;
import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.setting.CameraSettingActivity;
import com.app.beseye.setting.HWSettingsActivity;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeCamInfoSyncMgr.OnCamInfoChangedListener;
import com.app.beseye.util.BeseyeJSONUtil.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public abstract class BeseyeBaseActivity extends ActionBarActivity implements OnClickListener, 
																			  OnHttpTaskCallback, 
																			  ISessionUpdateCallback,
																			  BeseyeAppStateChangeListener,
																			  OnCamInfoChangedListener{
	static public final String KEY_FROM_ACTIVITY					= "KEY_FROM_ACTIVITY";
	
	protected boolean mbFirstResume = true;
	protected boolean mActivityDestroy = false;
	protected boolean mActivityResume = false;
	protected boolean mbIgnoreSessionCheck = false;
	
	protected String mStrVCamID = null;
	protected String mStrVCamName = null;
	protected JSONObject mCam_obj;
	
	private Handler mHandler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutId());
		BeseyeApplication.registerAppStateChangeListener(this);
		SessionMgr.getInstance().registerSessionUpdateCallback(this);
		doBindService();
		
		BeseyeCamInfoSyncMgr.getInstance().registerOnCamInfoChangedListener(this);
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkForCrashes();
	    checkForUpdates();
	    BeseyeApplication.increVisibleCount(this);
	    
		//if(! mbIgnoreSessionCheck && checkSession())
	    if( mbIgnoreSessionCheck || checkSession())
			invokeSessionComplete();
	    
	    checkOnResumeUpdateCamInfoRunnable();
		mActivityResume = true;
	}
	
	@Override
	protected void onPause() {
		BeseyeApplication.decreVisibleCount(this);
		mActivityResume = false;
		super.onPause();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		BeseyeCamInfoSyncMgr.getInstance().unregisterOnCamInfoChangedListener(this);
		clearLastAsyncTask();
		cancelRunningTasks();
		doUnbindService();
		BeseyeApplication.unregisterAppStateChangeListener(this);
		super.onDestroy();
		
		mActivityDestroy = true;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}

	@Override
	protected void onActivityResult(int arg0, int arg1, Intent arg2) {
		super.onActivityResult(arg0, arg1, arg2);
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}
	
	private boolean checkSession(){
		if(SessionMgr.getInstance().isTokenValid()){
			monitorAsyncTask(new BeseyeAccountTask.CheckAccountTask(this).setDialogId(mbFirstResume?DIALOG_ID_LOADING:-1), true, SessionMgr.getInstance().getAuthToken());
			//invokeSessionComplete();
			return false;
		}	
		else{
			Log.e(TAG, "checkSession(), need to get new session");
			onSessionInvalid();
			//monitorAsyncTask(new iKalaAddrTask.GetSessionTask(this), true);
		}
		return false;
	}
	
	private void invokeSessionComplete(){
		if(mbFirstResume)
			onSessionComplete();
		mbFirstResume = false;
	}
	
	protected void onSessionComplete(){
		checkOnResumeUpdateCamInfoRunnable();
	}
	
	private void checkForCrashes() {
	    CrashManager.register(this, HOCKEY_APP_ID);
	}
	
	private void checkForUpdates() {
	    // Remove this for store builds!
		if(DEBUG)
			UpdateManager.register(this, HOCKEY_APP_ID);
	}
	
	static public final String KEY_WARNING_TITLE = "KEY_WARNING_TITLE";
	static public final String KEY_WARNING_TEXT  = "KEY_WARNING_TEXT";
	static public final String KEY_INFO_TITLE 	 = "KEY_INFO_TITLE";
	static public final String KEY_INFO_TEXT  	 = "KEY_INFO_TEXT";
	static public final String KEY_WARNING_CLOSE = "KEY_WARNING_CLOSE";
	
	static public final int DIALOG_ID_LOADING = 1; 
	static public final int DIALOG_ID_WARNING = 2;
	static public final int DIALOG_ID_SYNCING = 3; 
	static public final int DIALOG_ID_INFO 	  = 4;
	
	static public final int DIALOG_ID_WIFI_BASE 			= 0x1000; 
	static public final int DIALOG_ID_TURN_ON_WIFI 			= DIALOG_ID_WIFI_BASE+1; 
	static public final int DIALOG_ID_WIFI_SCANNING 		= DIALOG_ID_WIFI_BASE+2; 
	static public final int DIALOG_ID_WIFI_SETTING 			= DIALOG_ID_WIFI_BASE+3; 
	static public final int DIALOG_ID_WIFI_TURN_ON_FAILED 	= DIALOG_ID_WIFI_BASE+4; 
	static public final int DIALOG_ID_WIFI_SCAN_FAILED 		= DIALOG_ID_WIFI_BASE+5; 
	static public final int DIALOG_ID_WIFI_AP_INFO 			= DIALOG_ID_WIFI_BASE+6; 
	static public final int DIALOG_ID_WIFI_AP_INCORRECT_PW 	= DIALOG_ID_WIFI_BASE+7; 
	static public final int DIALOG_ID_WIFI_AP_KEYINDEX		= DIALOG_ID_WIFI_BASE+8; 
	static public final int DIALOG_ID_CAM_INFO				= DIALOG_ID_WIFI_BASE+9; 
	static public final int DIALOG_ID_CAM_DETTACH_CONFIRM	= DIALOG_ID_WIFI_BASE+10; 
	static public final int DIALOG_ID_CAM_REBOOT_CONFIRM	= DIALOG_ID_WIFI_BASE+11; 
	static public final int DIALOG_ID_CAM_TALK_INIT			= DIALOG_ID_WIFI_BASE+12; 
	static public final int DIALOG_ID_CAM_NIGHT_VISION		= DIALOG_ID_WIFI_BASE+13; 
	static public final int DIALOG_ID_CAM_SCHED_DELETE		= DIALOG_ID_WIFI_BASE+14; 
	static public final int DIALOG_ID_CAM_SCHED_ABORT		= DIALOG_ID_WIFI_BASE+15; 
	static public final int DIALOG_ID_CAM_UPDATE			= DIALOG_ID_WIFI_BASE+16; 
	static public final int DIALOG_ID_WIFI_AP_INFO_ADD 		= DIALOG_ID_WIFI_BASE+17;
	static public final int DIALOG_ID_WIFI_AP_SECU_PICKER	= DIALOG_ID_WIFI_BASE+18; 
	static public final int DIALOG_ID_WIFI_AP_APPLY			= DIALOG_ID_WIFI_BASE+19; 
	
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WARNING:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(bundle.getString(KEY_WARNING_TITLE, getString(R.string.dialog_title_warning)));
            	builder.setMessage(bundle.getString(KEY_WARNING_TEXT));
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_WARNING);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_WARNING);
						if(bundle.getBoolean(KEY_WARNING_CLOSE, false)){
							finish();
						}
					}});
				
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					if(bundle.getBoolean(KEY_WARNING_CLOSE, false)){
						((android.app.AlertDialog)dialog).setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
						    public void onClick(DialogInterface dialog, int item) {
						    	removeMyDialog(DIALOG_ID_WARNING);
						    	finish();
						    }
						});
					}
				}
				break;
			}
			case DIALOG_ID_INFO:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(bundle.getString(KEY_INFO_TITLE, getString(R.string.dialog_title_info)));
            	builder.setMessage(bundle.getString(KEY_INFO_TEXT));
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_INFO);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_INFO);
					}});
				
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		return dialog;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_LOADING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_loading), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_TURN_ON_WIFI:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_wifi_turning_on), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_WIFI_SCANNING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_wifi_scanning), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_WIFI_SETTING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_wifi_setting), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_SYNCING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_syncing), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_CAM_UPDATE:{
				dialog = new Dialog(this);
				if(null != dialog){
					dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
					dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
					dialog.setContentView(inflateCamUpdateView());
					dialog.setCancelable(false);
				}
				break;
			}
			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
		switch (id) {
			case DIALOG_ID_LOADING:{
				String strMsgRes = "";
				if(null != args){
					strMsgRes = args.getString(KEY_WARNING_TEXT);
				}
				if(dialog instanceof AlertDialog){
					if(0 < strMsgRes.length())
						((AlertDialog) dialog).setMessage(strMsgRes);
				}
			}
	        case DIALOG_ID_WARNING:{
				String strTitleRes = "", strMsgRes = "";
				if(null != args){
					strTitleRes = args.getString(KEY_WARNING_TITLE);
					strMsgRes = args.getString(KEY_WARNING_TEXT);
				}
				if(dialog instanceof AlertDialog){
					((AlertDialog) dialog).setIcon(R.drawable.common_app_icon);
					((AlertDialog) dialog).setTitle((strTitleRes == null || 0 == strTitleRes.length())?getString(R.string.dialog_title_warning):strTitleRes);
					if(0 < strMsgRes.length())
						((AlertDialog) dialog).setMessage(strMsgRes);
				}
				
				break;
			}
	        case DIALOG_ID_INFO:{
				String strTitleRes = "", strMsgRes = "";
				if(null != args){
					strTitleRes = args.getString(KEY_INFO_TITLE);
					strMsgRes = args.getString(KEY_INFO_TEXT);
				}
				if(dialog instanceof AlertDialog){
					((AlertDialog) dialog).setIcon(R.drawable.common_app_icon);
					((AlertDialog) dialog).setTitle((strTitleRes == null || 0 == strTitleRes.length())?getString(R.string.dialog_title_info):strTitleRes);
					if(0 < strMsgRes.length())
						((AlertDialog) dialog).setMessage(strMsgRes);
				}
				
				break;
			}
	        default:
	        	super.onPrepareDialog(id, dialog, args);
	    }
	}
	
	public boolean showMyDialog(int iDialogId){
		return showMyDialog(iDialogId, null);
	}
	
	/*Workaround: Avoid the dialog isn't showed if we invoke it continuously. */
	private RemoveDialogRunnable mRemoveDialogRunnable = null;
	static private final long TIME_TO_REMOVE_DIALOG = 500L;
	
	private class RemoveDialogRunnable implements Runnable{
		public int miLastDialog = -1;
		public boolean mbCompleted = false;
		
		public RemoveDialogRunnable(int iLastDialog){
			miLastDialog = iLastDialog;
		}
		
		@Override
		public void run() {
			if(false == mActivityDestroy && 0 <= miLastDialog){
				Log.d(TAG, "removeDialog(), iDialogId="+miLastDialog);
				removeDialog(miLastDialog);
			}
			mbCompleted = true;
		}
	}
	
	public boolean showMyDialog(int iDialogId, Bundle bundle){
		if(false == mActivityDestroy && 0 <= iDialogId){
			if(null != mRemoveDialogRunnable && false == mRemoveDialogRunnable.mbCompleted && mRemoveDialogRunnable.miLastDialog == iDialogId){
				Log.d(TAG, "showMyDialog(), remove mRemoveDialogRunnable, iDialogId="+iDialogId);
				mHandler.removeCallbacks(mRemoveDialogRunnable);
				return true;
			}
			Log.d(TAG, "showMyDialog(), iDialogId="+iDialogId);
			return showDialog(iDialogId, bundle);
		}
		return false;
	}
	
	/*
	 *we will check if activity is destroyed before handle the dialog 
	 */
	public void dismissMyDialog(int iDialogId){
		if(false == mActivityDestroy && 0 <= iDialogId){
			mRemoveDialogRunnable = new RemoveDialogRunnable(iDialogId);
			mHandler.postDelayed(mRemoveDialogRunnable, TIME_TO_REMOVE_DIALOG);
		}
	}
	
	public void removeMyDialog(int iDialogId){
		if(false == mActivityDestroy && 0 <= iDialogId){
			mRemoveDialogRunnable = new RemoveDialogRunnable(iDialogId);
			mHandler.postDelayed(mRemoveDialogRunnable, TIME_TO_REMOVE_DIALOG);
		}
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.iv_nav_left_btn:{
				finish();
				break;
			}
			default:{
				Log.w(TAG, "BeseyeBaseActivity::onClick(), unhandled event by view:"+view);
			}
		}
	}
	
	/*
	 * Well manage the async tasks
	 * Cancel it if we don't need to run it after finishing this page
	 */
    private Map<AsyncTask, AsyncTaskParams> mMapCurAsyncTasks;
    private AsyncTask mLastAsyncTask = null;
    private AsyncTaskParams mLastTaskParams = null;
    
    static class AsyncTaskParams{
    	AsyncTaskParams(boolean bCancelWhenDestroy, String... strArgs){
    		this.bCancelWhenDestroy = bCancelWhenDestroy;
    		this.strArgs = strArgs;
    	}
    	boolean bCancelWhenDestroy= false;
    	String[] strArgs;
    }
    
    public void monitorAsyncTask(AsyncTask task, boolean bCancelWhenDestroy, String... strArgs){
    	if(null != task){
    		if(null != mMapCurAsyncTasks){
        		mMapCurAsyncTasks.put(task, new AsyncTaskParams(bCancelWhenDestroy, strArgs));
        	}
    		task.execute(strArgs);
    	}
    }
    
    protected void cancelRunningTasks(){
    	if(null != mMapCurAsyncTasks){
    		for(AsyncTask task:mMapCurAsyncTasks.keySet()){
    			AsyncTaskParams params = mMapCurAsyncTasks.get(task);
    			if((null == params || true == params.bCancelWhenDestroy) && AsyncTask.Status.FINISHED != task.getStatus())
    				task.cancel(true);
    		}
    		mMapCurAsyncTasks.clear();
    	} 
    }
    
    protected void recordLastAsyncTask(AsyncTask task){
    	//the task is still executed after clone
//    	try {
//    		if(null != task){
//    			mLastAsyncTask = (AsyncTask) ((iKalaHttpTask)task).clone();
//    			mLastTaskParams = mMapCurAsyncTasks.get(task);
//    		}
//		} catch (CloneNotSupportedException e) {
//			e.printStackTrace();
//		}
    }
    
    protected void clearLastAsyncTask(){
    	mLastAsyncTask = null;
    	mLastTaskParams = null;
    }
    
    protected void onRetryHttpTask(){
    	if(null != mLastAsyncTask){
    		//TODO: how to clone a AsyncTask
//    		if(null != mLastTaskParams)
//    			monitorAsyncTask(mLastAsyncTask, mLastTaskParams.bCancelWhenDestroy, mLastTaskParams.strArgs);
//    		else
//    			monitorAsyncTask(mLastAsyncTask, true);
    	}else{
    		//The basic handle: simulate the scenario of page begin
//		if(checkHost())
//			onSessionComplete();
    	}
    }
    
    protected void onRetryLaterHttpTask(){
    	//The basic handle: close current page
    	finish();
    }
    
	public void onShowDialog(AsyncTask task, final int iDialogId, final int iTitleRes, final int iMsgRes){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Bundle b = null;
				if(0 < iTitleRes || 0 < iMsgRes){
					b = new Bundle();
					if(iTitleRes > 0)
						b.putString(KEY_WARNING_TITLE, getString(iTitleRes));
					if(iMsgRes > 0)
						b.putString(KEY_WARNING_TEXT, getString(iMsgRes));		
				}
				showMyDialog(iDialogId, b);
			}});
	}
	
	public void onShowDialog(AsyncTask task, final int iDialogId, final String strTitleRes, final String strMsgRes){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Bundle b = null;
				b = new Bundle();
				b.putString(KEY_WARNING_TITLE, strTitleRes);
				b.putString(KEY_WARNING_TEXT, strMsgRes);	
				showMyDialog(iDialogId, b);
			}});
	}

	@Override
	public void onDismissDialog(AsyncTask task, final int iDialogId) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				dismissMyDialog(iDialogId);
			}});
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeAccountTask.CheckAccountTask){
			onSessionInvalid();
		}else if(task instanceof BeseyeAccountTask.LogoutHttpTask){
			SessionMgr.getInstance().cleanSession();
			onSessionInvalid();
		}else if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
			onToastShow(task, "failed to update sw");
		}else if(task instanceof BeseyeCamBEHttpTask.GetCamUpdateStatusTask){
			//removeMyDialog(DIALOG_ID_CAM_UPDATE);
			//onToastShow(task, "failed to update status");
		}
		
		if(DEBUG){
			onToastShow(task, strMsg);
			Log.e(TAG, "onErrorReport(), task:["+task.getClass().getSimpleName()+"], iErrType:"+iErrType+", strTitle:"+strTitle+", strMsg:"+strMsg);
		}
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "BeseyeBaseActivity::onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.CheckAccountTask){
				if(0 == iRetCode){
					invokeSessionComplete();
				}
			}else if(task instanceof BeseyeAccountTask.LogoutHttpTask){
				if(0 == iRetCode){
					//Log.i(TAG, "onPostExecute(), "+result.toString());
					onSessionInvalid();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject obj = result.get(0);
					if(null != obj){
						JSONObject dataObj = BeseyeJSONUtil.getJSONObject(obj, ACC_DATA);
						if(null != dataObj){
							//int iCamStatus = getJSONInt(dataObj, CAM_STATUS, 0);
							//BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, BeseyeJSONUtil.CAM_CONN_STATUS.toCamConnStatus(iCamStatus).getValue());
							BeseyeJSONUtil.setJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(obj, BeseyeJSONUtil.OBJ_TIMESTAMP));
							BeseyeJSONUtil.setJSONObject(mCam_obj, ACC_DATA, dataObj);
							BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
						}
					}
					
					mOnResumeUpdateCamInfoRunnable = null;
				}
			}else if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
				String strVcamId = ((BeseyeCamBEHttpTask.UpdateCamSWTask)task).getVcamId();
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					
					if(null != mLstUpdateCandidate){
						if(mLstUpdateCandidate.contains(strVcamId)){
							BeseyeJSONUtil.setJSONObject(mUpdateVcamList, strVcamId, new JSONObject());
						}
					}
				}else{
					if(null != mUpdateVcamList){
						mLstUpdateCandidate.remove(strVcamId);
						miCheckUpdateCamIdx--;
					}
				}
				
				if(miCheckUpdateCamIdx == mLstUpdateCandidate.size()){
					if(0 < mLstUpdateCandidate.size()){
						miUpdateCamNum = mLstUpdateCandidate.size();
						miCurUpdateCamStatusIdx = 0;
						
						showMyDialog(DIALOG_ID_CAM_UPDATE);
						SessionMgr.getInstance().setCamUpdateTimestamp(System.currentTimeMillis());
						SessionMgr.getInstance().setCamUpdateList(arrayToString(mLstUpdateCandidate));
						
						monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++));
					}else{
						BeseyeUtils.postRunnable(new Runnable(){
							@Override
							public void run() {
								Bundle b = new Bundle();
								b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_update_no_valid_cam));
								showMyDialog(DIALOG_ID_WARNING, b);
							}}, 0);
						SessionMgr.getInstance().setCamUpdateTimestamp(0);
						Log.e(TAG, "onPostExecute(), there is no valid camera to update");
					}
					
				}else{
					
					monitorAsyncTask(new BeseyeCamBEHttpTask.UpdateCamSWTask(this), true, mLstUpdateCandidate.get(miCheckUpdateCamIdx++));
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetCamUpdateStatusTask){
				String strVcamId = ((BeseyeCamBEHttpTask.GetCamUpdateStatusTask)task).getVcamId();
				if(0 == iRetCode){					
					BeseyeJSONUtil.setJSONObject(mUpdateVcamList, strVcamId, result.get(0));
					updateCamUpdateProgress();
				}else{
					mLstUpdateCandidate.remove(strVcamId);
					miUpdateCamNum-=1;		
				}
				
				if(isCamUpdateFinish() || 0 == miUpdateCamNum){
					removeMyDialog(DIALOG_ID_CAM_UPDATE);
					SessionMgr.getInstance().setCamUpdateTimestamp(0);
					Log.e(TAG, "onPostExecute(), Camera update finish...");
				}else{
					if(miCurUpdateCamStatusIdx >= miUpdateCamNum){
						miCurUpdateCamStatusIdx = 0;
						BeseyeUtils.postRunnable(new Runnable(){
							@Override
							public void run() {
								if(checkCamUpdateFlag()){
									monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(BeseyeBaseActivity.this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++%miUpdateCamNum));
								}else{
									Bundle b = new Bundle();
									b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_update_timeout));
									showMyDialog(DIALOG_ID_WARNING, b);
									removeMyDialog(DIALOG_ID_CAM_UPDATE);
								}
								
							}}, 5000);
					}else{
						monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(BeseyeBaseActivity.this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++%miUpdateCamNum));
					}
				} 
			}
		}
		
		if(null != mMapCurAsyncTasks){
			mMapCurAsyncTasks.remove(task);
		}
	}

	@Override
	public void onToastShow(AsyncTask task,final String strMsg) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
					//Toast.makeText(BeseyeBaseActivity.this, strMsg, Toast.LENGTH_LONG).show();
			}});
	}
	
	protected void invokeLogout(){
		monitorAsyncTask(new BeseyeAccountTask.LogoutHttpTask(this), true, SessionMgr.getInstance().getAuthToken());
	}

	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason) {
		onSessionInvalid();
	}
	
	protected void onSessionInvalid(){
		SessionMgr.getInstance().cleanSession();
		launchDelegateActivity(BeseyeEntryActivity.class.getName());
	}
	
	public void launchActivityByIntent(Intent intent){
		intent.putExtra(KEY_FROM_ACTIVITY, getClass().getSimpleName());
		startActivity(intent);
	}
    
    protected void launchActivityByClassName(String strClass){
    	launchActivityByClassName(strClass, new Bundle());
	}
    
    public void launchActivityByClassName(String strClass, Bundle bundle){
		Intent intent = new Intent();
		intent.setClassName(this, strClass);
		if(null != bundle)
			intent.putExtras(bundle);
		
		launchActivityByIntent(intent);
	}
    
    public void launchActivityForResultByClassName(String strClass, Bundle bundle, int iRequestCode){
		Intent intent = new Intent();
		intent.setClassName(this, strClass);
		intent.putExtra(KEY_FROM_ACTIVITY, getClass().getSimpleName());
		if(null != bundle)
			intent.putExtras(bundle);
		startActivityForResult(intent, iRequestCode);
	}
    
    protected void launchDelegateActivity(String strCls){
		launchDelegateActivity(strCls, null);
    }
	
	protected void launchDelegateActivity(String strCls, Bundle bundle){
    	Intent intent = new Intent();
		intent.putExtra("ClassName", strCls);
		intent.setClass(this, OpeningPage.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if(null != bundle)
			intent.putExtras(bundle);
		startActivity(intent);
    }
	
	public void onAppEnterForeground(){}
	public void onAppEnterBackground(){}
	
	private boolean mbNeedToNotifyWhenServiceConnected = false;
	final public void notifyServiceAppForeground(){
		if(null != mNotifyService){
			try {
				mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_APP_TO_FOREGROUND));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}else{
			mbNeedToNotifyWhenServiceConnected = true;
		}
	}
	
	final public void notifyServiceAppBackground(){
		if(null != mNotifyService){
			try {
				mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_APP_TO_BACKGROUND));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	//TO notify the latest session data to service
	public void onSessionUpdate(SessionData data){
		if(null != mNotifyService){
			try {
				Message msg = Message.obtain(null, BeseyeNotificationService.MSG_UPDATE_SESSION_DATA);
				if(null != msg){
					msg.getData().putParcelable("SessionData", data);
					mNotifyService.send(msg);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	private Messenger mMessenger = new Messenger(new NotificationHandler(this));
	protected Messenger mNotifyService = null;
	protected Messenger mUploadWorksService = null;
	 /** Flag indicating whether we have called bind on the service. */
	protected  boolean mIsBound;
	
	/**
     * Handler of incoming messages from service.
     */
    static class NotificationHandler extends Handler {
    	private final WeakReference<BeseyeBaseActivity> mActivity; 
    	
    	NotificationHandler(BeseyeBaseActivity act){
    		mActivity = new WeakReference<BeseyeBaseActivity>(act);
    	}
    	
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            	  
                case BeseyeNotificationService.MSG_CAM_SETTING_UPDATED:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCamSettingChangedCallback(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                    break;
                }
                case BeseyeNotificationService.MSG_CAM_WIFI_CONFIG_CHANGED:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onWifiSettingChangedCallback(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                    break;
                }
//                case BeseyeNotificationService.MSG_SET_UNREAD_MSG_NUM:{
//                	BeseyeBaseActivity act = mActivity.get();
//                	if(null != act)
//                		act.onUnReadMsgCallback(msg.arg1);
//                    break;
//                }
                default:
                    super.handleMessage(msg);
            }
        }
    }
	
	/**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
        	mNotifyService = new Messenger(service);
          

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
                Message msg = Message.obtain(null,
                		BeseyeNotificationService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mNotifyService.send(msg);
//                mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_QUERY_NOTIFY_NUM));
                if(mbNeedToNotifyWhenServiceConnected){
                	mbNeedToNotifyWhenServiceConnected = false;
                	mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_APP_TO_FOREGROUND));
                }
//                //To stop the pulling in case of app crash
//                mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_STOP_TO_PULL_MSG));
            } catch (RemoteException e) {
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            
            // As part of the sample, tell the user what happened.
//            Toast.makeText(BeseyeBaseActivity.this, "onServiceConnected",
//                    Toast.LENGTH_SHORT).show();
            notifyServiceConnected();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
        	mNotifyService = null;

            // As part of the sample, tell the user what happened.
//            Toast.makeText(BeseyeBaseActivity.this, "onServiceDisconnected",
//                    Toast.LENGTH_SHORT).show();
        	notifyServiceDisconnected();
        }
    };
    
//    private ServiceConnection mUploadConnection = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className,
//                IBinder service) {
//            // This is called when the connection with the service has been
//            // established, giving us the service object we can use to
//            // interact with the service.  We are communicating with our
//            // service through an IDL interface, so get a client-side
//            // representation of that from the raw service object.
//        	mUploadWorksService = new Messenger(service);
//          
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            // This is called when the connection with the service has been
//            // unexpectedly disconnected -- that is, its process crashed.
//        	mUploadWorksService = null;
//            // As part of the sample, tell the user what happened.
//        }
//    };
    
    protected void notifyServiceConnected(){}
    protected void notifyServiceDisconnected(){}
    
    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because there is no reason to be able to let other
        // applications replace our component.
        bindService(new Intent(BeseyeBaseActivity.this, 
        		BeseyeNotificationService.class), mConnection, Context.BIND_AUTO_CREATE);
//        bindService(new Intent(BeseyeBaseActivity.this, 
//        		iKalaUploadWorksService.class), mUploadConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mNotifyService != null) {
                try {
                    Message msg = Message.obtain(null,
                    		BeseyeNotificationService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mNotifyService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service
                    // has crashed.
                }
            }
            
            // Detach our existing connection.
            unbindService(mConnection);
            //unbindService(mUploadConnection);
            mIsBound = false;
        }
    }
   
    public class OnResumeUpdateCamInfoRunnable implements Runnable{
    	String mStrVCamId;
    	OnResumeUpdateCamInfoRunnable(String strVCamId){
    		mStrVCamId = strVCamId;
    	}
    	
    	boolean isSameVCamId(String strVCamId){
    		return null != mStrVCamId && mStrVCamId.equals(strVCamId);
    	}
    	
		@Override
		public void run() {
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(BeseyeBaseActivity.this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, mStrVCamID);
		}
    	
    }
    
    protected OnResumeUpdateCamInfoRunnable mOnResumeUpdateCamInfoRunnable = null;
    protected void setOnResumeUpdateCamInfoRunnable(OnResumeUpdateCamInfoRunnable run){
    	Log.i(TAG, "setOnResumeUpdateCamInfoRunnable()");
    	mOnResumeUpdateCamInfoRunnable = run;
    }
    
    protected boolean isSameIdOnResumeUpdateCamInfoRunnable(String strVCamId){
    	return (null != mOnResumeUpdateCamInfoRunnable && mOnResumeUpdateCamInfoRunnable.isSameVCamId(strVCamId));
    }
    
    private void checkOnResumeUpdateCamInfoRunnable(){
    	if(null != mOnResumeUpdateCamInfoRunnable){
    		Log.i(TAG, "checkOnResumeUpdateCamInfoRunnable(), trigger...");
    		BeseyeUtils.postRunnable(mOnResumeUpdateCamInfoRunnable, 0);
    		mOnResumeUpdateCamInfoRunnable = null;
    	}
    }
    
    protected void onCamSettingChangedCallback(JSONObject DataObj){
    	if(null != mStrVCamID){
    		if(null != DataObj){
    			String strCamUID = BeseyeJSONUtil.getJSONString(DataObj, WS_ATTR_CAM_UID);
    			long lTs = BeseyeJSONUtil.getJSONLong(DataObj, WS_ATTR_TS);
    			if(mStrVCamID.equals(strCamUID)){
    				if(!mActivityDestroy){
    		    		if(!mActivityResume){
    		    			setOnResumeUpdateCamInfoRunnable(new OnResumeUpdateCamInfoRunnable(mStrVCamID));
    		    		}else{
    		    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, mStrVCamID);
    		    		}
    		    	}
    			}
    		}
    	}
    }
    
    protected Runnable mCountDownWiFiChangeRunnable;
    protected boolean onWifiSettingChangedCallback(JSONObject msgObj){
    	if(this instanceof HWSettingsActivity){
    		if(null != mCountDownWiFiChangeRunnable){
        		BeseyeUtils.removeRunnable(mCountDownWiFiChangeRunnable);
        		mCountDownWiFiChangeRunnable = null;
        	}
        	
        	Log.i(TAG, getClass().getSimpleName()+"::onWifiSettingChangedCallback(),  msgObj = "+msgObj);
        	if(null != msgObj){
        		JSONObject objReg = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_REGULAR_DATA);
        		if(null != objReg){
        			//String strCamUID = BeseyeJSONUtil.getJSONString(objReg, WS_ATTR_CAM_UID);
        			removeMyDialog(DIALOG_ID_WIFI_AP_APPLY);
        			return true;
        		}
    		}
    	}
    	return false;
    }
    
    protected long mlCamSetupObjUpdateTs = -1;
    
    public void onCamSetupChanged(String strVcamId, long lTs, JSONObject objCamSetup){
		if(null != strVcamId && strVcamId.equals(mStrVCamID)){
			long lTsOldTs = BeseyeJSONUtil.getJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP);
			Log.i(TAG, getClass().getSimpleName()+"::onCamSetupChanged(),  lTs = "+lTs+", lTsOldTs="+lTsOldTs);
			if(lTs > lTsOldTs){
				mCam_obj = objCamSetup;
				mOnResumeUpdateCamInfoRunnable = null;
			}
		}
    }
    
    protected void setActivityResultWithCamObj(){
		Intent resultIntent = new Intent();
		resultIntent.putExtra(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
		setResult(RESULT_OK, resultIntent);	
	}

	protected abstract int getLayoutId();
	
	//Camera update begin
	private ProgressBar mProgressBarCamUpdate;
	private TextView mTxtCamUpdateStatus;
	private int miCheckUpdateCamIdx = 0;
	private List<String> mLstUpdateCandidate;
	private JSONObject mUpdateVcamList;
	private int miUpdateCamNum = 0;
	private int miCurUpdateCamStatusIdx = 0;
	
	protected void triggerCamUpdate(JSONArray VcamList){
		int iVcamNum = (null != VcamList)?VcamList.length():0;
		if(0 < iVcamNum){
			miCheckUpdateCamIdx = 0;
			mUpdateVcamList = new JSONObject();
			mLstUpdateCandidate = new ArrayList<String>();
			//mMapUpdateStatus = new LinkedHashMap<String, JSONObject>();
			for(int idx = 0; idx < iVcamNum;idx++){
				try {
					String strVCamId = BeseyeJSONUtil.getJSONString(VcamList.getJSONObject(idx), BeseyeJSONUtil.ACC_ID);
					mLstUpdateCandidate.add(strVCamId);
					Log.i(TAG, "triggerCamUpdate(), strVcamId:"+strVCamId);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			monitorAsyncTask(new BeseyeCamBEHttpTask.UpdateCamSWTask(this), true, mLstUpdateCandidate.get(miCheckUpdateCamIdx++));
		}else{
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_update_no_valid_cam));
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
			
			Log.e(TAG, "triggerCamUpdate(), there is no valid camera to update");
		}
		
//		mUpdateVcamList = new JSONObject();
//		mLstUpdateCandidate = new ArrayList<String>();
//		miCurUpdateCamStatusIdx=0;
//		mLstUpdateCandidate.add("01d0f4f7b68e48a583e28449eacf99b2");
//		mLstUpdateCandidate.add("9ee1316073f54242b6c4cfe6aa2a0eda");
//		miUpdateCamNum = 2;
//		showMyDialog(DIALOG_ID_CAM_UPDATE);
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++));
	}
	
	protected void resumeCamUpdate(JSONArray VcamList){
		String[] strCamUpdate = SessionMgr.getInstance().getCamUpdateList().split(";");
		if(null != strCamUpdate){
			int iNum = strCamUpdate.length;
			int iVcamNum = (null != VcamList)?VcamList.length():0;
			if(0 < iVcamNum){
				mUpdateVcamList = new JSONObject();
				mLstUpdateCandidate = new ArrayList<String>();
				miCurUpdateCamStatusIdx=0;
				for(int idx = 0; idx < iNum; idx++){
					for(int idx2 = 0; idx2 < iVcamNum;idx2++){
						try {
							if(strCamUpdate[idx].equals(BeseyeJSONUtil.getJSONString(VcamList.getJSONObject(idx2), BeseyeJSONUtil.ACC_ID))){
								mLstUpdateCandidate.add(strCamUpdate[idx]);
								Log.i(TAG, "resumeCamUpdate(), strCamUpdate[idx]:"+strCamUpdate[idx]);
								break;
							}
							
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				
				miUpdateCamNum = mLstUpdateCandidate.size();
				if(0 < miUpdateCamNum){
					showMyDialog(DIALOG_ID_CAM_UPDATE);
					monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++));
				}else{
					Log.i(TAG, "resumeCamUpdate(), there is no cam to update");
				}
			}
		}
	}
	
	protected View inflateCamUpdateView(){
		View vCamUpdate = getLayoutInflater().inflate(R.layout.layout_camera_update_progress, null);
		if(null != vCamUpdate){
			mProgressBarCamUpdate = (ProgressBar)vCamUpdate.findViewById(R.id.sb_update_progress);
			mTxtCamUpdateStatus = (TextView)vCamUpdate.findViewById(R.id.txt_update_progress);
		}
		
		setCamUpdateProgress(0);
		return vCamUpdate;
	}
	
	private void setCamUpdateProgress(int iProgress){
		if(null != mProgressBarCamUpdate)
			mProgressBarCamUpdate.setProgress(iProgress);
			
		if(null != mTxtCamUpdateStatus)
			mTxtCamUpdateStatus.setText(String.format(getString(R.string.cam_update_progress), iProgress+"%"));
	}
	
	private void updateCamUpdateProgress(){
		int iProgress = 0;
		int iCompleteNum = 0;
		int iNumOfDone = 0;
		int iNumOfFail = 0;
		for(int idx = 0; idx < miUpdateCamNum; idx++){
			String strVCamId = mLstUpdateCandidate.get(idx);
			JSONObject objCamUpdateStatus = BeseyeJSONUtil.getJSONObject(mUpdateVcamList, strVCamId);
			if(null != objCamUpdateStatus){
				int iFinalStatus = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_FINAL_STAUS, -1);
				
				Log.i(TAG, "updateCamUpdateProgress(), "+objCamUpdateStatus+", strVcamId:"+strVCamId+", iFinalStatus="+iFinalStatus);
				
				if(-1 !=  iFinalStatus){
					iCompleteNum++;
					iProgress+=(100.0/miUpdateCamNum);
				}else{
					if(0 == iFinalStatus || 1 == iFinalStatus){
						iNumOfDone++;
					}else{
						iNumOfFail++;
					}
					int iProgressIdv = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_PROGRESS, 0);
					iProgress+=((100.0/miUpdateCamNum))*(iProgressIdv/100.0);
				}
			}
		}
		
		if(iCompleteNum < miUpdateCamNum){
			setCamUpdateProgress(iProgress);
		}else{
			removeMyDialog(DIALOG_ID_CAM_UPDATE);
			Bundle b = new Bundle();
			String strRet = "";
			if(0 == iNumOfFail){
				strRet = getResources().getString(R.string.cam_update_success);
			}else if(0 == iNumOfDone){
				strRet = getResources().getString(R.string.cam_update_failed);
			}else{
				strRet = String.format(getResources().getString(R.string.cam_update_result), iNumOfDone, iNumOfFail);
			}
			
			b.putString(KEY_INFO_TEXT, strRet);
			showMyDialog(DIALOG_ID_INFO, b);
			Log.i(TAG, "updateCamUpdateProgress(), Update SW successfully");
		}	
	}
	
	private boolean isCamUpdateFinish(){
		boolean bRet = true;
		
		for(int idx = 0; idx < miUpdateCamNum; idx++){
			String strVCamId = mLstUpdateCandidate.get(idx);
			JSONObject objCamUpdateStatus = BeseyeJSONUtil.getJSONObject(mUpdateVcamList, strVCamId);
			if(null != objCamUpdateStatus){
				int iFinalStatus = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_FINAL_STAUS, -1);
				
				Log.i(TAG, "isCamUpdateFinish(), "+objCamUpdateStatus+", strVcamId:"+strVCamId+", iFinalStatus="+iFinalStatus);
				if(-1 == iFinalStatus){
					bRet = false;
					break;
				}
			}
		}
		
		return bRet;
	}
	
	protected boolean checkCamUpdateFlag(){
		boolean  bRet = true;
		long lDelta = System.currentTimeMillis() - SessionMgr.getInstance().getCamUpdateTimestamp();
		if(lDelta > 30*60*1000){
			SessionMgr.getInstance().setCamUpdateTimestamp(0);
			bRet = false;
		}
		Log.i(TAG, "checkCamUpdateFlag(), lDelta:"+lDelta+", bRet:"+bRet);
		return bRet;
	}
	
	protected String arrayToString(List<String> lstUpdateCandidate){
		String strRet = "";
		int iNum = (null != lstUpdateCandidate)?lstUpdateCandidate.size():0;
		for(int idx = 0; idx < iNum; idx++){
			strRet += ((0 == idx)?"":";")+lstUpdateCandidate.get(idx);
		}
		Log.i(TAG, "arrayToString(), strRet:"+strRet);
		return strRet;
	}
	
	//Camera update end
}
