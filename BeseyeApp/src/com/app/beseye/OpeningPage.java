package com.app.beseye;


import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;

import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class OpeningPage extends Activity implements OnHttpTaskCallback{
	public static final String ACTION_BRING_FRONT 		= "ACTION_BRING_FRONT";
	public static final String KEY_HAVE_HANDLED 		= "KEY_HAVE_HANDLED";
	public static final String KEY_DELEGATE_INTENT 		= "KEY_DELEGATE_INTENT";
	public static final String KEY_EVENT_FLAG 			= "KEY_EVENT_FLAG";
	public static final String KEY_EVENT_RELAUNCH_FLAG 	= "KEY_EVENT_RELAUNCH_FLAG";
	public static final String KEY_EVENT_INTENT 		= "KEY_EVENT_INTENT";
	public static final String KEY_IGNORE_ACTIVATED_FLAG= "KEY_IGNORE_ACTIVATED_FLAG";
	public static final String FIRST_PAGE 				= CameraListActivity.class.getName();
	
	private static boolean sbFirstLaunch = true;
	private static final long TIME_TO_CLOSE_OPENING_PAGE = 3000L;
	
	private boolean m_bLaunchForDelegate = false;
	private Intent intentRelaunch = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, "OpeningPage::onCreate()");
		//if(sbFirstLaunch)
		if(getIntent().getBooleanExtra(KEY_HAVE_HANDLED, false)){
			Log.i(TAG, "OpeningPage::onCreate(), call finish for KEY_HAVE_HANDLED is true ");
			finish();
			return;
		}
		
		if(null != savedInstanceState && savedInstanceState.getBoolean(KEY_HAVE_HANDLED, false)){
			Log.i(TAG, "OpeningPage::onCreate(), call finish for KEY_HAVE_HANDLED of savedInstanceState is true ");
			finish();
			return;
		}
		
		setContentView(R.layout.layout_opening);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		if(getIntent().getBooleanExtra(ACTION_BRING_FRONT, false)){
			Log.i(TAG, "OpeningPage::onCreate(), call finish for ACTION_BRING_FRONT");
			finish();
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

			if(intent.getBooleanExtra(ACTION_BRING_FRONT, false)){
				if(DEBUG)
					Log.i(TAG, "OpeningPage::onNewIntent(), ACTION_BRING_FRONT ");
				finish();
				return;
			}
		}
		
		launchActivityByIntent(intent);
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "OpeningPage::onResume()");
		super.onResume();
		if(false == m_bLaunchForDelegate && null == mGetUserInfoTask){
			Log.i(TAG, "OpeningPage::onResume(), call finish");
			finish();
		}
		m_bLaunchForDelegate = false;
		sbFirstLaunch = false;
	}
	
	@Override
	protected void onPause() {
		Log.i(TAG, "OpeningPage::onPause()");
		if(null != mGetUserInfoTask){
			mGetUserInfoTask.cancel(true);
		}
		
		if(null != mGetVCamListTask){
			mGetVCamListTask.cancel(true);
		}
		super.onPause();
//		Log.i(TAG, "OpeningPage::onPause(), call finish");
//		finish();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "OpeningPage::onDestroy()");
		super.onDestroy();
		if(intentRelaunch != null){
			Log.i(TAG, "OpeningPage::onDestroy(), invoke intentRelaunch");
			startActivity(intentRelaunch);
		}
	}

	private BeseyeAccountTask.GetUserInfoTask mGetUserInfoTask;
	private BeseyeHttpTask mGetVCamListTask;
	
	private void launchActivityByIntent(Intent intent){
//		if(SessionMgr.getInstance().isTokenValid()){
//			File p2pFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_p2p");
//			String strP2P = null;
//			String strName = null;
//			if(null != p2pFile && p2pFile.exists()){
//				try {
//					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(p2pFile)));
//					try {
//						strP2P = (null != reader)?reader.readLine():null;
//						strName = (null != reader)?reader.readLine():null;
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				} catch (FileNotFoundException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			Log.i(TAG, "OpeningPage::launchActivityByIntent(), strP2P :"+strP2P+", p2pFile:"+p2pFile.getAbsolutePath());
//			
//			if(null != strP2P && 0 < strP2P.length()){
//				COMPUTEX_P2P = true;
//				Intent intentLanuch = new Intent();
//				intentLanuch.setClassName(this, CameraViewActivity.class.getName());
//				JSONObject mCam_obj = new JSONObject();
//				//BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_ID, mStrVCamID);
//		        BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME, strName);
//		        intentLanuch.putExtra(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
//				intentLanuch.putExtra(CameraViewActivity.KEY_P2P_STREAM, strP2P);
//				intentLanuch.putExtra(CameraViewActivity.KEY_P2P_STREAM_NAME, strName);
//				startActivity(intentLanuch);
//				return;
//			}else{
//				COMPUTEX_P2P = false;
//			}
//		}
//		
//		BeseyeApplication.checkPairingMode();
		
		Intent intentLanuch = null;
		if(null == (intentLanuch = intent.getParcelableExtra(KEY_DELEGATE_INTENT))){
			intentLanuch = new Intent();
			String strCls = null;
			if(null != intent){
				strCls = intent.getStringExtra("ClassName");
			}
			
			if(null == strCls){
				strCls = FIRST_PAGE;
			}
			
			if(!SessionMgr.getInstance().isTokenValid()){
				strCls = BeseyeEntryActivity.class.getName();
			}else if(!SessionMgr.getInstance().getIsCertificated() && !intent.getBooleanExtra(KEY_IGNORE_ACTIVATED_FLAG, false)){
				mGetUserInfoTask = new BeseyeAccountTask.GetUserInfoTask(this);
				if(null != mGetUserInfoTask){
					mGetUserInfoTask.execute();
				}
				return;
			}
			
			if(null != intent.getExtras())
				intentLanuch.putExtras(intent.getExtras());
			
			intentLanuch.setClassName(this, strCls);
		}else{
			//Try to close push dialog when launch from status bar
			Intent intentBroadcast = new Intent(GCMIntentService.FORWARD_GCM_MSG_ACTION);
			intentBroadcast.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, GCMIntentService.FORWARD_ACTION_TYPE_CHECK_DIALOG);
	        sendBroadcast(intentBroadcast);
	        
	        if(intent.getBooleanExtra(KEY_EVENT_FLAG, false) ){
	        	if(false == intent.getBooleanExtra(KEY_EVENT_RELAUNCH_FLAG, false) && BeseyeBaseActivity.getActiveActivityCount() == 0){
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
	        		Log.i(TAG, "OpeningPage::launchActivityByIntent(), reach relaunch case");
	        		Intent intentCameraList = new Intent();
		        	intentCameraList.setClassName(this, FIRST_PAGE);
		        	intentCameraList.putExtra(KEY_EVENT_FLAG, true);
		        	intentCameraList.putExtra(KEY_EVENT_INTENT, intentLanuch);
		        	intentLanuch = intentCameraList;
		        	finish();
	        	}
	        	
	        }else{
	        	//intentLanuch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        }
		}
		
		String strTsInfo = intentLanuch.getStringExtra(CameraViewActivity.KEY_TIMELINE_INFO);
		Log.i(TAG, "OpeningPage::launchActivityByIntent(), strTsInfo:"+strTsInfo);
		
		//intentLanuch.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		if(sbFirstLaunch || (!SessionMgr.getInstance().getIsCertificated() && !intent.getBooleanExtra(KEY_IGNORE_ACTIVATED_FLAG, false))){
			final Intent intentLanuchRunnable =intentLanuch;
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					if(SessionMgr.getInstance().isTokenValid() && !SessionMgr.getInstance().getIsCertificated()){
						intentLanuchRunnable.setClassName(OpeningPage.this, BeseyeEntryActivity.class.getName());
					}
					startActivity(intentLanuchRunnable);
				}
			}, TIME_TO_CLOSE_OPENING_PAGE);
		}else{
			startActivity(intentLanuch);
		}

		m_bLaunchForDelegate = true;
		
		getIntent().putExtra(KEY_HAVE_HANDLED, true);
	}
	
	@Override
	public void onDismissDialog(AsyncTask task, int iDialogId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result,
			int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.GetUserInfoTask){
				Intent intentLanuch = new Intent();
				intentLanuch.setClassName(this, BeseyeEntryActivity.class.getName());
				if(0 == iRetCode){
					JSONObject obj = result.get(0);
					if(null != obj){
						Log.i(TAG, "OpeningPage::onPostExecute(), obj "+obj);
						JSONObject objUser = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_USER);
						if(null != objUser){
							SessionMgr.getInstance().setIsCertificated(BeseyeJSONUtil.getJSONBoolean(objUser, BeseyeJSONUtil.ACC_ACTIVATED));
							if(SessionMgr.getInstance().getIsCertificated()){
								intentLanuch.setClassName(this, FIRST_PAGE);
							}else{
								SessionMgr.getInstance().cleanSession();
							}
						}
					}
				}else{
					SessionMgr.getInstance().cleanSession();
				}
				startActivity(intentLanuch);
				Log.i(TAG, "OpeningPage::onPostExecute(), call finish");
				finish();
			}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					int iVcamCnt = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_VCAM_CNT);
					if(0 == iVcamCnt){
						SessionMgr.getInstance().setIsCertificated(false);
					}
				}
			}
		}
		
		if(task == mGetUserInfoTask){
			mGetUserInfoTask = null;
		}else if(task == mGetVCamListTask){
			mGetVCamListTask = null; 
		}
	}

	@Override
	public void onToastShow(AsyncTask task, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onShowDialog(AsyncTask task, int iDialogId, int iTitleRes,
			int iMsgRes) {
		// TODO Auto-generated method stub
		
	}
}