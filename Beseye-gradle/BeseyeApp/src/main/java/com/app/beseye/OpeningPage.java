package com.app.beseye;


import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.ota.BeseyeCamSWVersionMgr;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_GROUP_VER_CHK_RET;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_ERROR;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.OnCamGroupUpdateVersionCheckListener;
import com.app.beseye.ota.CamOTAInstructionActivity;
import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.facebook.appevents.AppEventsLogger;

public class OpeningPage extends Activity implements OnHttpTaskCallback, 
													 OnCamGroupUpdateVersionCheckListener{
	public static final String ACTION_BRING_FRONT 		= "ACTION_BRING_FRONT";
	public static final String KEY_HAVE_HANDLED 		= "KEY_HAVE_HANDLED";
	public static final String KEY_DELEGATE_INTENT 		= "KEY_DELEGATE_INTENT";
	public static final String KEY_EVENT_FLAG 			= "KEY_EVENT_FLAG";
	public static final String KEY_EVENT_RELAUNCH_FLAG 	= "KEY_EVENT_RELAUNCH_FLAG";
	public static final String KEY_EVENT_INTENT 		= "KEY_EVENT_INTENT";
	public static final String KEY_IGNORE_ACTIVATED_FLAG= "KEY_IGNORE_ACTIVATED_FLAG";
	public static final String KEY_PINCODE_AUTH			= "KEY_PINCODE_AUTH";
	public static final String KEY_APP_TYPE				= "KEY_APP_TYPE";
	public static final String FIRST_PAGE 				= CameraListActivity.class.getName();//BeseyeTrustDevAuthActivity.class.getName();
	
	private static boolean sbFirstLaunch = true;
	private static final long TIME_TO_CLOSE_OPENING_PAGE = 3000L;
	
	private boolean m_bLaunchForDelegate = false;
	private Intent intentRelaunch = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onCreate()");
		
		//if(sbFirstLaunch)
		if(getIntent().getBooleanExtra(KEY_HAVE_HANDLED, false)){
			if(DEBUG)
				Log.i(TAG, "OpeningPage::onCreate(), call finish for KEY_HAVE_HANDLED is true ");
			finish();
			return;
		}
		
		if(null != savedInstanceState && savedInstanceState.getBoolean(KEY_HAVE_HANDLED, false)){
			if(DEBUG)
				Log.i(TAG, "OpeningPage::onCreate(), call finish for KEY_HAVE_HANDLED of savedInstanceState is true ");
			finish();
			return;
		}
		
		setContentView(R.layout.layout_opening);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		if(handleBringFrontAction(getIntent())){
			return;
		}
		
		launchActivityByIntent(getIntent());
		
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(KEY_HAVE_HANDLED, true);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onNewIntent(), intent "+intent.getDataString()+", this = "+this);
		
		super.onNewIntent(intent);
		if(null != intent && null == intent.getParcelableExtra(KEY_DELEGATE_INTENT)){
			String strCls = intent.getStringExtra("ClassName");
			if(null == strCls){
				if(DEBUG)
					Log.i(TAG, "OpeningPage::onNewIntent(), null == strCls ");
				finish();
				return;
			}
			
			if(handleBringFrontAction(intent)){
				return;
			}
		}
		
		launchActivityByIntent(intent);
	}
	
	private boolean handleBringFrontAction(Intent intent){
		boolean bRet = false;
		if(intent.getBooleanExtra(ACTION_BRING_FRONT, false)){
			if(DEBUG)
				Log.i(TAG, "OpeningPage::handleBringFrontAction(), ACTION_BRING_FRONT call finish");
			bRet = true;
			if(0 == BeseyeBaseActivity.getActiveActivityCount()){
				if(DEBUG)
					Log.i(TAG, "OpeningPage::handleBringFrontAction(), relaunch");
				intentRelaunch = new Intent();
				intentRelaunch.setClassName(this, OpeningPage.class.getName());
				intentRelaunch.setAction("android.intent.action.MAIN"); 
				intentRelaunch.addCategory("android.intent.category.LAUNCHER"); 
				intentRelaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP/* | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET*/);
			}
			finish();
		}
		return bRet;
	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onResume()");
		super.onResume();
		if(false == m_bLaunchForDelegate && null == mGetUserInfoTask){
			if(DEBUG)
				Log.i(TAG, "OpeningPage::onResume(), call finish");
			finish();
		}
		m_bLaunchForDelegate = false;
		sbFirstLaunch = false;
		
		//facebook
		// Logs 'install' and 'app activate' App Events.
		AppEventsLogger.activateApp(this);
	}
	
	@Override
	protected void onPause() {
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onPause()");
		if(null != mGetUserInfoTask){
			mGetUserInfoTask.cancel(true);
		}
		
		super.onPause();
		
		//Facebook
		// Logs 'app deactivate' App Event.
		AppEventsLogger.deactivateApp(this);
		  
//		Log.i(TAG, "OpeningPage::onPause(), call finish");
//		finish();
	}

	@Override
	protected void onDestroy() {
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onDestroy()");

		super.onDestroy();
		if(intentRelaunch != null){
			if(DEBUG)
				Log.i(TAG, "OpeningPage::onDestroy(), invoke intentRelaunch");
			startActivity(intentRelaunch);
		}
	}

	private BeseyeAccountTask.GetUserInfoTask mGetUserInfoTask;
	private Intent mintentLanuchRunnable;
	private void launchActivityByIntent(Intent intent){		
		Intent intentLanuch = null;
		//Normal launch case
		if(null == (intentLanuch = intent.getParcelableExtra(KEY_DELEGATE_INTENT))){
			intentLanuch = new Intent();
			String strCls = null;
			if(null != intent){
				strCls = intent.getStringExtra("ClassName");
			}
			
			if(null == strCls){
				strCls = FIRST_PAGE;
			}
			
			if(!SessionMgr.getInstance().isTokenValid()){//If Dev token exists? 
				strCls = BeseyeEntryActivity.class.getName();
			}else if(!SessionMgr.getInstance().getIsCertificated() && !intent.getBooleanExtra(KEY_IGNORE_ACTIVATED_FLAG, false)){//User has login but not pairing successfully
				mGetUserInfoTask = new BeseyeAccountTask.GetUserInfoTask(this);
				if(null != mGetUserInfoTask){
					mGetUserInfoTask.execute();
				}
				return;
			}
			
			if(null != intent.getExtras())
				intentLanuch.putExtras(intent.getExtras());
			
			if(intent.getBooleanExtra(OpeningPage.KEY_PINCODE_AUTH, false)){
				//Abner: special handle for launch activity from notification
	        	if(false == intent.getBooleanExtra(KEY_EVENT_RELAUNCH_FLAG, false) && BeseyeBaseActivity.getActiveActivityCount() == 0){
	        		if(DEBUG)
	        			Log.i(TAG, "OpeningPage::launchActivityByIntent(), relaunch for pin");
	        		intentRelaunch = new Intent();
	        		intentRelaunch.setClassName(this, OpeningPage.class.getName());
	        		intentRelaunch.setAction("android.intent.action.MAIN"); 
	        		intentRelaunch.addCategory("android.intent.category.LAUNCHER"); 
	        		intentRelaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP/* | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET*/);
	        		intentRelaunch.putExtra(KEY_EVENT_RELAUNCH_FLAG, true);
	        		intentRelaunch.putExtras(intent.getExtras());
	        		
	        		finish();
	        		return;
	        	}else /*if(intent.getBooleanExtra(KEY_EVENT_RELAUNCH_FLAG, false))*/{
	        		if(DEBUG)
	        			Log.i(TAG, "OpeningPage::launchActivityByIntent(), reach relaunch case for pin");
	        		
//					//Try to show pin code dialog from service
					Intent intentBroadcast = new Intent(BeseyeNotificationService.ACTION_FORWARD_PINCODE_CLICKED);
					intentBroadcast.putExtras(intent.getExtras());
					intentBroadcast.putExtra(KEY_APP_TYPE, BeseyeApplication.getAppMark());
					
			        sendBroadcast(intentBroadcast);
			        if(BeseyeBaseActivity.getActiveActivityCount() == 0){
			        	intentLanuch.setClassName(this, strCls);
			        }else{
			        	finish();
		        		return;
			        }
	        	}
			}else{
				//Check cam SW here  
				intentLanuch.setClassName(this, strCls);
			}
		}else{
//			//Try to close push dialog when launch from status bar
//			Intent intentBroadcast = new Intent(GCMIntentService.FORWARD_PUSH_MSG_ACTION);
//			intentBroadcast.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, GCMIntentService.FORWARD_ACTION_TYPE_CHECK_DIALOG);
//	        sendBroadcast(intentBroadcast);
	        
	        if(intent.getBooleanExtra(KEY_EVENT_FLAG, false) ){
	        	//Abner: special handle for launch activity from notification
	        	if(false == intent.getBooleanExtra(KEY_EVENT_RELAUNCH_FLAG, false) && BeseyeBaseActivity.getActiveActivityCount() == 0){
	        		if(DEBUG)
	        			Log.i(TAG, "OpeningPage::launchActivityByIntent(), relaunch");
	        		intentRelaunch = new Intent();
	        		intentRelaunch.setClassName(this, OpeningPage.class.getName());
	        		intentRelaunch.setAction("android.intent.action.MAIN"); 
	        		intentRelaunch.addCategory("android.intent.category.LAUNCHER"); 
	        		intentRelaunch.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP/* | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET*/);
	        		intentRelaunch.putExtra(KEY_EVENT_FLAG, true);
	        		intentRelaunch.putExtra(KEY_EVENT_RELAUNCH_FLAG, true);
	        		intentRelaunch.putExtra(KEY_DELEGATE_INTENT, intentLanuch);
	        		
	        		finish();
	        		return;
	        	}else if(intent.getBooleanExtra(KEY_EVENT_RELAUNCH_FLAG, false)){
	        		if(DEBUG)
	        			Log.i(TAG, "OpeningPage::launchActivityByIntent(), reach relaunch case");
	        		
	        		Intent intentCameraList = new Intent();
		        	intentCameraList.setClassName(this, FIRST_PAGE);
		        	intentCameraList.putExtra(KEY_EVENT_FLAG, true);
		        	intentCameraList.putExtra(KEY_EVENT_INTENT, intentLanuch);
		        	intentLanuch = intentCameraList;
	        	}
	        	
	        }else{
	        	//intentLanuch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        }
		}
		
		String strTsInfo = intentLanuch.getStringExtra(CameraViewActivity.KEY_TIMELINE_INFO);
		if(DEBUG)
			Log.i(TAG, "OpeningPage::launchActivityByIntent(), strTsInfo:"+strTsInfo);
		
		//intentLanuch.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		if(sbFirstLaunch || (!SessionMgr.getInstance().getIsCertificated() && !intent.getBooleanExtra(KEY_IGNORE_ACTIVATED_FLAG, false))){
			mintentLanuchRunnable =intentLanuch;
			if(sbFirstLaunch && SessionMgr.getInstance().isTokenValid() && SessionMgr.getInstance().getIsCertificated() && !SessionMgr.getInstance().getIsCamSWUpdateSuspended()){
				BeseyeCamSWVersionMgr.getInstance().registerOnCamGroupUpdateVersionCheckListener(this);
				BeseyeCamSWVersionMgr.getInstance().performCamGroupOTAVerCheck(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL);
			}else{
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						if(!SessionMgr.getInstance().isTokenValid() && !SessionMgr.getInstance().getIsCertificated()){
							mintentLanuchRunnable.setClassName(OpeningPage.this, BeseyeEntryActivity.class.getName());
						}
						startActivity(mintentLanuchRunnable);
						finish();
					}
				}, sbFirstLaunch?TIME_TO_CLOSE_OPENING_PAGE:0);
			}
		}else{
			startActivity(intentLanuch);
		}

		m_bLaunchForDelegate = true;
		
		getIntent().putExtra(KEY_HAVE_HANDLED, true);
	}
	
	@Override
	public void onDismissDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,
			String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result,
			int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.GetUserInfoTask){
				Intent intentLanuch = new Intent();
				intentLanuch.setClassName(this, BeseyeEntryActivity.class.getName());
				if(0 == iRetCode){
					JSONObject obj = result.get(0);
					if(null != obj){
						if(DEBUG)
							Log.i(TAG, "OpeningPage::onPostExecute(), obj "+obj);
						JSONObject objUser = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_USER);
						if(null != objUser){
							SessionMgr.getInstance().setIsCertificated(BeseyeJSONUtil.getJSONBoolean(objUser, BeseyeJSONUtil.ACC_ACTIVATED));
							if(SessionMgr.getInstance().getIsCertificated()){
								if(!SessionMgr.getInstance().getIsCamSWUpdateSuspended()){
									//intentLanuch.setClassName(this, FIRST_PAGE);
									BeseyeCamSWVersionMgr.getInstance().performCamGroupOTAVerCheck(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL);
									return;
								}
							}else{
								SessionMgr.getInstance().cleanSession();
							}
						}
					}
				}else{
					SessionMgr.getInstance().cleanSession();
				}
				
				startActivity(intentLanuch);
				if(DEBUG)
					Log.i(TAG, "OpeningPage::onPostExecute(), call finish");
				finish();
			}
		}
		
		if(task == mGetUserInfoTask){
			mGetUserInfoTask = null;
		}
	}

	@Override
	public void onToastShow(AsyncTask<String, Double, List<JSONObject>> task, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionInvalid(AsyncTask<String, Double, List<JSONObject>> task, int iInvalidReason) {
		SessionMgr.getInstance().cleanSession();
		
		Intent intentLanuch = new Intent();
		intentLanuch.setClassName(this, BeseyeEntryActivity.class.getName());
		startActivity(intentLanuch);
		
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onSessionInvalid(), call finish");
		finish();
	}

	@Override
	public void onShowDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId, int iTitleRes,
			int iMsgRes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onCamUpdateVersionCheckAllCallback(
			CAM_GROUP_VER_CHK_RET chkRet, CAM_UPDATE_GROUP chkGroup,
			CAM_UPDATE_ERROR chkErr, List<String> lstVcamIds) {
		Intent intentLanuch = (null != mintentLanuchRunnable)?mintentLanuchRunnable:new Intent();
		if(chkRet.equals(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_ALL_OUT_OF_UPDATE)){
			intentLanuch.setClassName(this, CamOTAInstructionActivity.class.getName());
			intentLanuch.putExtra(CamOTAInstructionActivity.CAM_OTA_TYPE, CamOTAInstructionActivity.CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ALL.ordinal());
		}else{
			if((null == mintentLanuchRunnable)){
				intentLanuch.setClassName(this, FIRST_PAGE);
			}
		}
		
		startActivity(intentLanuch);
		finish();
		if(DEBUG)
			Log.i(TAG, "OpeningPage::onCamUpdateVersionCheckAllCallback(), call finish");
		BeseyeCamSWVersionMgr.getInstance().unregisterOnCamGroupUpdateVersionCheckListener(this);
	}
}
