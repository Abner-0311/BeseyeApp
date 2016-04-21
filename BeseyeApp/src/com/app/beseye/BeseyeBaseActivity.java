package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.CrashManagerListener;
import net.hockeyapp.android.UpdateManager;
import net.hockeyapp.android.UpdateManagerListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeApplication.BeseyeAppStateChangeListener;
import com.app.beseye.adapter.MarketAppAdapter;
import com.app.beseye.adapter.MarketWebAdapter;
import com.app.beseye.adapter.MarketWebAdapter.MarketWebInfo;
import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.httptask.BeseyeUpdateBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.ISessionUpdateCallback;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.httptask.SessionMgr.SessionData;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.pairing.SoundPairingNamingActivity;
import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.setting.HWSettingsActivity;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeCamInfoSyncMgr.OnCamInfoChangedListener;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeFeatureConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeNewFeatureMgr;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;


public abstract class BeseyeBaseActivity extends ActionBarActivity implements OnClickListener, 
																			  OnHttpTaskCallback, 
																			  ISessionUpdateCallback,
																			  BeseyeAppStateChangeListener,
																			  OnCamInfoChangedListener,
																			  //OnCamUpdateVersionCheckListener, 
																			  OnNetworkChangeCallback{
	static public final String KEY_FROM_ACTIVITY					= "KEY_FROM_ACTIVITY";
	
	protected boolean mbFirstResume = true;
	protected boolean mActivityDestroy = false;
	protected boolean mActivityResume = false;
	protected boolean mbIgnoreSessionCheck = false;
	protected boolean mbIgnoreCamVerCheck = false;
	private boolean mbHaveCheckAppVer = false;
	
	protected String mStrVCamID = null;
	protected String mStrVCamName = null;
	protected JSONObject mCam_obj = null;
	
	static private long slLastTimeToCheckSession = -1;
	static private int siActiveActivityCount = 0;
	
	static public int getActiveActivityCount(){
		return siActiveActivityCount;
	}
	
	private static ExecutorService FULL_TASK_EXECUTOR; 
	static {  
        FULL_TASK_EXECUTOR = (ExecutorService) Executors.newCachedThreadPool();  
        siActiveActivityCount = 0;
    };  
	private Handler mHandler = new Handler();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		siActiveActivityCount++;
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
		
		NetworkMgr.getInstance().registerNetworkChangeCallback(this);
		
		checkForCrashes();
	    checkForUpdates();
	    BeseyeApplication.increVisibleCount(this);
	    
		//if(! mbIgnoreSessionCheck && checkSession())
	    if( mbIgnoreSessionCheck || (-1 != slLastTimeToCheckSession && (System.currentTimeMillis() - slLastTimeToCheckSession) < 300000) || checkSession())
			invokeSessionComplete();
	    
	    checkOnResumeUpdateCamInfoRunnable();
		mActivityResume = true;
	}
	
	@Override
	protected void onPause() {
		BeseyeApplication.decreVisibleCount(this);
		mActivityResume = false;
		
		NetworkMgr.getInstance().unregisterNetworkChangeCallback(this);

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
		siActiveActivityCount--;
		mActivityDestroy = true;
		Log.e(TAG, "onDestroy(),"+getClass().getName());
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
	
//	private AsyncTask<String, Double, List<JSONObject>> mGetCamListTask = null; 
//	
//	private void getCamListAndCheckCamUpdateVersions(){
//		//Set<String> setVcamList = null;
//		if(!BeseyeFeatureConfig.CAM_SW_UPDATE_CHK || SessionMgr.getInstance().getIsCamSWUpdateSuspended() || !SessionMgr.getInstance().getIsTrustDev()){
//			return;
//		}
//		
//		if((0 != slLastGetCamListTs && (System.currentTimeMillis() - slLastGetCamListTs) < 300000L) && !mbIsNetworkDisconnectedWhenCamUpdating){//Not check update within 5 mins
//			Log.e(TAG, "getCamListAndCheckCamUpdateVersions(), within checked duration");
//			return;
//		}
//		
//		if(false == this instanceof CameraListActivity /*|| (null == ( setVcamList = BeseyeCamInfoSyncMgr.getInstance().getVCamIdList()) || 0 == setVcamList.size())*/){
//			if(null != mGetCamListTask && false == mGetCamListTask.isCancelled()){
//				mGetCamListTask.cancel(true);
//			}
//			monitorAsyncTask(mGetCamListTask = new BeseyeAccountTask.GetVCamListTask(this, true).setDialogId(-1), true);
//		}
//	}
	
	private boolean checkSession(){
		if(SessionMgr.getInstance().isTokenValid()){
			monitorAsyncTask(new BeseyeAccountTask.CheckAccountTask(this).setDialogId(mbFirstResume?DIALOG_ID_LOADING:-1), true, SessionMgr.getInstance().getAuthToken());
			//invokeSessionComplete();
			return false;
		}	
		else{
			Log.e(TAG, "checkSession(), need to get new session");
			onSessionInvalid(false);
			//monitorAsyncTask(new iKalaAddrTask.GetSessionTask(this), true);
		}
		return false;
	}
	
	private void invokeSessionComplete(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(mbFirstResume)
					onSessionComplete();
				mbFirstResume = false;
			}}, 0);

	}
	
	protected void onSessionComplete(){
		checkOnResumeUpdateCamInfoRunnable();
	}
	
	private void checkForCrashes() {
	    CrashManager.register(this, HOCKEY_APP_ID,  new CrashManagerListener(){

			@Override
			public String getContact() {
				// TODO Auto-generated method stub
				return super.getContact();
			}

			@Override
			public String getDescription() {
				// TODO Auto-generated method stub
				return super.getDescription();
			}

			@Override
			public String getUserID() {
				// TODO Auto-generated method stub
				return super.getUserID();
			}

			@Override
			public boolean ignoreDefaultHandler() {
				// TODO Auto-generated method stub
				return super.ignoreDefaultHandler();
			}

			@Override
			public boolean includeDeviceData() {
				// TODO Auto-generated method stub
				return super.includeDeviceData();
			}

			@Override
			public void onNewCrashesFound() {
				// TODO Auto-generated method stub
				super.onNewCrashesFound();
			}
	    	
	    });
	}
	
//	private boolean mbGetUpdateRetFromHockeyApp = false;
//	private boolean mbGetUpdateRetFromMiSDK = false;
//	
//	static final private long TIME_TO_CHECK_UPDATE_VIA_MI = 6000L;//Avoid no update from HockeyApp SDK
//	static final private long TIME_TO_CHECK_FINAL_UPDATE_RET    = 10000L;//Avoid no update from both SDK
//	
//	private Runnable mCheckUpdateRetFromMiSDKRunnable = new Runnable(){
//		@Override
//		public void run() {
//			if(false == mbGetUpdateRetFromHockeyApp){
//				Log.i(TAG, "run(), try to get update status from mi");
//				XiaomiUpdateAgent.setUpdateAutoPopup(false);
//				XiaomiUpdateAgent.setUpdateListener(new XiaomiUpdateListener() {
//					@Override
//					public void onUpdateReturned(int updateStatus, UpdateResponse reponse) {
//				        switch (updateStatus) {
//				            case UpdateStatus.STATUS_UPDATE:
//				    			 Log.i(TAG, "onUpdateReturned(), for UpdateStatus.STATUS_UPDATE, reponse:"+reponse.toString());
//				    			 launchUpdateApp();
//				                 break;
//				            case UpdateStatus.STATUS_NO_UPDATE:
//				    			 Log.i(TAG, "onUpdateReturned(), for UpdateStatus.STATUS_NO_UPDATE");
//				    			 onAppUpdateNotAvailable();
//				                 break;
//				            case UpdateStatus.STATUS_NO_WIFI:
//				    			 Log.i(TAG, "onUpdateReturned(), for UpdateStatus.STATUS_NO_WIFI");
//				                 break;
//				            case UpdateStatus.STATUS_NO_NET:
//				    			 Log.i(TAG, "onUpdateReturned(), for UpdateStatus.STATUS_NO_NET");
//				                break;
//				            case UpdateStatus.STATUS_FAILED:
//				    			 Log.i(TAG, "onUpdateReturned(), for UpdateStatus.STATUS_FAILED");
//				    			 onAppUpdateNotAvailable();
//				                 break;
//				            case UpdateStatus.STATUS_LOCAL_APP_FAILED:
//				    			 Log.i(TAG, "onUpdateReturned(), for UpdateStatus.STATUS_LOCAL_APP_FAILED");
//				    			 onAppUpdateNotAvailable();
//				                 break;
//				            default:{
//				    			 Log.i(TAG, "onUpdateReturned(), unknown updateStatus:"+updateStatus);
//				    			 onAppUpdateNotAvailable();
//				            }   
//				            mbGetUpdateRetFromMiSDK = true;
//				        }
//					}
//				});
//				BeseyeUtils.postRunnable(mCheckFinalAppUpdateRet, TIME_TO_CHECK_FINAL_UPDATE_RET);
//				mbGetUpdateRetFromMiSDK = false;
//				XiaomiUpdateAgent.update(BeseyeBaseActivity.this);
//			}else{
//				Log.i(TAG, "run(), already get update status from hockeyApp");
//			}
//		}};
//	
//		
//	private Runnable mCheckFinalAppUpdateRet = new Runnable(){
//		@Override
//		public void run() {
//			Log.i(TAG, "mCheckFinalAppUpdateRet::run(), "+mbGetUpdateRetFromMiSDK+", "+mbGetUpdateRetFromHockeyApp);
//
//			if(false == mbGetUpdateRetFromMiSDK && false ==mbGetUpdateRetFromHockeyApp){
//				onAppUpdateNotAvailable();
//			}
//		}};	
	
	private void checkForUpdates() {
		if(SessionMgr.getInstance().getEnableBeseyeAppVerControl() || BeseyeUtils.isProductionVersion()){
			monitorAsyncTask(new BeseyeUpdateBEHttpTask.GetLatestAndroidAppVersionTask(this).setDialogId(-1), true, getPackageName());
		}else  if(BeseyeUtils.canUpdateFromHockeyApp()){
			UpdateManager.register(this, HOCKEY_APP_ID, mUpdateManagerListener, true);
		}else if(BeseyeConfig.DEBUG){
			onAppUpdateNotAvailable();
		}
		
//		if(false == mbIgnoreCamVerCheck){
//			BeseyeCamInfoSyncMgr.getInstance().registerOnCamUpdateVersionCheckListener(this);
//		}
//		
//		mbHaveCheckAppVer = false;
//		mbGetUpdateRetFromHockeyApp = false;
//		mbGetUpdateRetFromMiSDK = false;
//		BeseyeUtils.removeRunnable(mCheckFinalAppUpdateRet);
//		BeseyeUtils.removeRunnable(mCheckUpdateRetFromMiSDKRunnable);
//		
//		if(BeseyeUtils.canUpdateFromHockeyApp()){
//			UpdateManager.register(this, HOCKEY_APP_ID, mUpdateManagerListener, true);
//		}else if(BeseyeUtils.isProductionVersion()){
//			Log.i(TAG, "checkForUpdates(), for production:"+((HOCKEY_APP_ID.length() > 7)?HOCKEY_APP_ID.substring(0, 6):""));
//			UpdateManager.register(this, HOCKEY_APP_ID, mUpdateManagerListenerForProduction, false);
//		}
//
//		BeseyeUtils.postRunnable(mCheckUpdateRetFromMiSDKRunnable, TIME_TO_CHECK_UPDATE_VIA_MI);
	}
	
	//For Alpha update
	private UpdateManagerListener mUpdateManagerListener = new UpdateManagerListener(){
		@Override
		public void onNoUpdateAvailable() {
			super.onNoUpdateAvailable();
//			mbGetUpdateRetFromHockeyApp = true;
//			if(false == mbGetUpdateRetFromMiSDK){
				onAppUpdateNotAvailable();
//			}else{
//				Log.i(TAG, "onNoUpdateAvailable(), have get update ret from mi");
//			}
		}
	}; 
//	
//	//For production version app update
//	private UpdateManagerListener mUpdateManagerListenerForProduction = new UpdateManagerListener(){
//		@Override
//		public void onUpdateAvailable() {
//			super.onUpdateAvailable();
//			mbGetUpdateRetFromHockeyApp = true;
//			BeseyeUtils.removeRunnable(mCheckUpdateRetFromMiSDKRunnable);
//			UpdateManager.unregister();
//			
//			if(false == mbGetUpdateRetFromMiSDK){
//				launchUpdateApp();
//			}else{
//				Log.i(TAG, "onUpdateAvailable(), have get update ret from mi");
//			}
//		}
//
//		@Override
//		public void onNoUpdateAvailable() {
//			super.onNoUpdateAvailable();
//			mbGetUpdateRetFromHockeyApp = true;
//			BeseyeUtils.removeRunnable(mCheckUpdateRetFromMiSDKRunnable);
//			
//			if(false == mbGetUpdateRetFromMiSDK){
//				onAppUpdateNotAvailable();
//			}else{
//				Log.i(TAG, "onNoUpdateAvailable(), have get update ret from mi");
//			}
//		}
//	}; 
	
	private void launchUpdateApp(){
		final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
		if(BeseyeUtils.isProductionVersion()){
			Log.i(TAG, "onUpdateAvailable(), for production, appPackageName:"+appPackageName);
		}
		
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName));
		final PackageManager pm = getPackageManager();
	    final List<ResolveInfo> matches = pm.queryIntentActivities(intent, 0);
	    if(!matches.isEmpty()){
	    	ResolveInfo riGooglePlay = null;
		    for (final ResolveInfo info : matches){
				Log.e(TAG, "info:"+info.toString());
				if (info.activityInfo.packageName.toLowerCase().equals("com.android.vending")) {
					riGooglePlay = info;
					break;
				}
		    }
		    
		    //If Google Play exist, show it
		    if(null != riGooglePlay){
		    	intent.setClassName(riGooglePlay.activityInfo.packageName, riGooglePlay.activityInfo.name);
		    	startActivity(intent);
		    	
//		    	dismissMarketAppList();
//		    	dismissMarketWebList();
		    }else{
		    	//[Abner 20160127] Disable 360 & mi update because we don't support China from now
//		    	List<ResolveInfo> lstOtherMarket = new ArrayList<ResolveInfo>();
//		    	for (final ResolveInfo info : matches){
//					if (info.activityInfo.packageName.toLowerCase().equals("com.qihoo.appstore") || 
//						info.activityInfo.packageName.toLowerCase().equals("com.xiaomi.market")	) {
//						lstOtherMarket.add(info);
//					}
//			    }
//		    	
//		    	if(false == lstOtherMarket.isEmpty()){
//		    		if(1 == lstOtherMarket.size()){
//		    			intent.setClassName(lstOtherMarket.get(0).activityInfo.packageName, lstOtherMarket.get(0).activityInfo.name);
//				    	startActivity(intent);
//				    	dismissMarketAppList();
//				    	dismissMarketWebList();
//		    		}else{
//	    				mMarketAppAdapter = new MarketAppAdapter(this, lstOtherMarket);
//		    			dismissMarketWebList();
//		    			showMarketAppList();
//		    		}
//		    	}else{
//		    		onNoMarketApp();
//		    	}
		    }
	    }else{
	    	//[Abner 20160127] Disable 360 & mi update because we don't support China from now
	    	//onNoMarketApp();
	    }
//	    
//		try {
//			getMarketAppIntent(this, appPackageName);
//		    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
//		} catch (android.content.ActivityNotFoundException anfe) {
//			Log.i(TAG, "onUpdateAvailable(), ActivityNotFoundException:"+anfe.toString());
//
//			if(SessionMgr.getInstance().getServerMode().equals(SessionMgr.SERVER_MODE.MODE_CHINA_STAGE)){
//				Log.i(TAG, "onUpdateAvailable(), launch 360 web page");
//			    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://m.app.haosou.com/detail/index?pname=" + appPackageName+"&id=2972261")));
//			    //"http://m.app.mi.com/detail/index?id=95502"
//			}else{
//				Log.i(TAG, "onUpdateAvailable(), launch google play web page");
//			    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + appPackageName)));
//			}
//		}
	}
	
//	private MarketAppAdapter mMarketAppAdapter = null;
//	private MarketWebAdapter mMarketWebAdapter = null;
//	private Dialog mMarketAppListDialog = null;
//	private Dialog mMarketWebListDialog = null;
//	
//	private void onNoMarketApp(){
//		List<MarketWebInfo> lstMarket = new ArrayList<MarketWebInfo>();
//		lstMarket.add(new MarketWebInfo(getString(R.string.download_app_web_360), Uri.parse("http://m.app.haosou.com/detail/index?&id=2972261")));
//		lstMarket.add(new MarketWebInfo(getString(R.string.download_app_web_mi), Uri.parse("http://m.app.mi.com/detail/index?id=95502")));
//		mMarketWebAdapter = new MarketWebAdapter(this, lstMarket);
//		
//		dismissMarketAppList();
//		showMarketWebList();
//	}
//	
//	private void showMarketAppList(){
//		if(null == mMarketAppListDialog){
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//	        builder.setAdapter(mMarketAppAdapter, new DialogInterface.OnClickListener() {
//	            @Override
//	            public void onClick(DialogInterface dialog, int which) {
//	                Object objInfo = mMarketAppAdapter.getItem(which);
//	            	if(objInfo instanceof ResolveInfo){
//	            		ResolveInfo info = (ResolveInfo)objInfo;
//	            		Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
//	  	               	marketIntent.setClassName(info.activityInfo.packageName, info.activityInfo.name);
//	  	               	startActivity(marketIntent);
//	            	}else{
//	            		Log.e(TAG, "not ResolveInfo object");
//	            	}
//	            }
//	        }).setCancelable(true); 
//	        mMarketAppListDialog = setMarketDialog(builder, getResources().getString(R.string.dialog_title_download_app_via));
//	        mMarketAppListDialog.setCancelable(false);
//	        mMarketAppListDialog.setOnDismissListener(new OnDismissListener(){
//
//				@Override
//				public void onDismiss(DialogInterface dialog) {
//					mMarketAppListDialog = null;
//				}});
//		}
//	}
//	
//	private void showMarketWebList(){
//		if(null == mMarketWebListDialog){
//			AlertDialog.Builder builder = new AlertDialog.Builder(this);
//	        builder.setAdapter(mMarketWebAdapter, new DialogInterface.OnClickListener() {
//	            @Override
//	            public void onClick(DialogInterface dialog, int which) {
//	            	Object objInfo = mMarketWebAdapter.getItem(which);
//	            	if(objInfo instanceof MarketWebInfo){
//	            		MarketWebInfo info = (MarketWebInfo) objInfo;
//	            		Intent marketIntent = new Intent(Intent.ACTION_VIEW, info.mMarketURL);
//	                    startActivity(marketIntent);
//	            	}else{
//	            		Log.e(TAG, "not MarketWebInfo object");
//	            	}
//	            }
//	        }).setCancelable(true); 
//	        mMarketWebListDialog = setMarketDialog(builder, this.getResources().getString(R.string.dialog_title_download_app_via_web));
//	        mMarketWebListDialog.setCancelable(false);
//	        mMarketWebListDialog.setOnDismissListener(new OnDismissListener(){
//
//				@Override
//				public void onDismiss(DialogInterface dialog) {
//					mMarketWebListDialog = null;
//				}});
//		}
//	}
//	
//	private void dismissMarketAppList(){
//		if(null != mMarketAppListDialog){
//			mMarketAppListDialog.dismiss();
//			mMarketAppListDialog = null;
//		}
//	}
//	
//	private void dismissMarketWebList(){
//		if(null != mMarketWebListDialog){
//			mMarketWebListDialog.dismiss();
//			mMarketWebListDialog = null;
//		}
//	}
//	
//	private Dialog setMarketDialog(AlertDialog.Builder builder, String strTitle){
//		TextView title = new TextView(this);
//        title.setText(strTitle);
//        title.setPadding(this.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_left),
//        		this.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_top),
//        		this.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_top),
//        		this.getResources().getDimensionPixelSize(R.dimen.alertdialog_padding_top));
//        
//        title.setTextColor(this.getResources().getColor(R.color.wifi_info_dialog_title_font_color));
//        float scaledDensity = this.getResources().getDisplayMetrics().scaledDensity;
//        title.setTextSize(this.getResources().getDimension(R.dimen.wifi_ap_info_dialog_title_font_size)/scaledDensity);        
//        builder.setCustomTitle(title);
//        
//        //It is a hack!
//        //http://stackoverflow.com/questions/14439538/how-can-i-change-the-color-of-alertdialog-title-and-the-color-of-the-line-under
//        Dialog d = builder.show();
//        int dividerId = d.getContext().getResources().getIdentifier("android:id/titleDivider", null, null);
//        View divider = d.findViewById(dividerId);
//        divider.setBackgroundColor(this.getResources().getColor(R.color.wifi_info_dialog_title_font_color));
//        
//        d.getWindow().setFlags(
//        	    WindowManager.LayoutParams.FLAG_FULLSCREEN, 
//        	    WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        
//        return d;
//	}
	
	private void onAppUpdateNotAvailable(){
//		if(true == mbIgnoreCamVerCheck){
//			return;
//		}
//		
//		if(this instanceof CameraListActivity /*&& !checkWithinCamUpdatePeriod()*/ && !isCamUpdatingInCurrentPage() && null != mObjVCamList){
//			getCamUpdateCandidateList(mObjVCamList);
//		}
//		
//		mbHaveCheckAppVer = true;
//		if((false == this instanceof CameraListActivity && false == mbIgnoreCamVerCheck /*&& !checkWithinCamUpdatePeriod()*/ && !isCamUpdatingInCurrentPage()) || mbIsNetworkDisconnectedWhenCamUpdating)
//			getCamListAndCheckCamUpdateVersions();
	}
	
	protected boolean isAppVersionChecked(){
		return mbHaveCheckAppVer;
	}
	
	static public final String KEY_WARNING_TITLE = "KEY_WARNING_TITLE";
	static public final String KEY_WARNING_TEXT  = "KEY_WARNING_TEXT";
	static public final String KEY_INFO_TITLE 	 = "KEY_INFO_TITLE";
	static public final String KEY_INFO_TEXT  	 = "KEY_INFO_TEXT";
	static public final String KEY_WARNING_CLOSE = "KEY_WARNING_CLOSE";
	
	static public final int DIALOG_ID_LOADING 	= 1; 
	static public final int DIALOG_ID_WARNING 	= 2;
	static public final int DIALOG_ID_SYNCING 	= 3; 
	static public final int DIALOG_ID_INFO 	  	= 4;
	static public final int DIALOG_ID_SETTING 	= 5;
	static public final int DIALOG_ID_LOGIN   	= 6; 
	static public final int DIALOG_ID_SIGNUP  	= 7; 
	static public final int DIALOG_ID_NO_NETWORK= 8; 
	static public final int DIALOG_ID_DETACH_CAM= 9; 
	
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
	static public final int DIALOG_ID_PLAYER_CAPTURE		= DIALOG_ID_WIFI_BASE+20; 
	static public final int DIALOG_ID_UPDATE_VIA_MARKET		= DIALOG_ID_WIFI_BASE+21; 
	static public final int DIALOG_ID_UPDATE_VIA_WEB		= DIALOG_ID_WIFI_BASE+22; 
	static public final int DIALOG_ID_DELETE_TRUST_DEV		= DIALOG_ID_WIFI_BASE+23; 
	static public final int DIALOG_ID_PIN_VERIFY_FAIL		= DIALOG_ID_WIFI_BASE+24; 
	static public final int DIALOG_ID_PIN_VERIFY_FAIL_3_TIME= DIALOG_ID_WIFI_BASE+25; 
	static public final int DIALOG_ID_PIN_VERIFY_FAIL_EXPIRED	= DIALOG_ID_WIFI_BASE+26; 
	static public final int DIALOG_ID_PIN_AUTH_REQUEST		= DIALOG_ID_WIFI_BASE+27; 
	static public final int DIALOG_ID_RESET_HUMAN_DETECT	= DIALOG_ID_WIFI_BASE+28; 
	static public final int DIALOG_ID_OTA_FORCE_UPDATE		= DIALOG_ID_WIFI_BASE+29; 
	static public final int DIALOG_ID_OTA_WS_DISCONN		= DIALOG_ID_WIFI_BASE+30; 
	static public final int DIALOG_ID_OTA_FORCE_CAM_LST		= DIALOG_ID_WIFI_BASE+31; 

	
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		Dialog dialog;
		switch(id){
		case DIALOG_ID_WARNING:{
			BaseOneBtnDialog d = new BaseOneBtnDialog(this);
			d.setBodyText(bundle.getString(KEY_WARNING_TEXT));
			d.setTitleText(bundle.getString(KEY_WARNING_TITLE, getString(R.string.dialog_title_warning)));

			d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
				@Override
				public void onBtnClick() {
					removeMyDialog(DIALOG_ID_WARNING);	
					if(bundle.getBoolean(KEY_WARNING_CLOSE, false)){
						finish();
					}
				}});
			dialog = d;
			break;
		}
		case DIALOG_ID_NO_NETWORK:{
			BaseOneBtnDialog d = new BaseOneBtnDialog(this);
			d.setBodyText(BeseyeUtils.appendErrorCode(this, R.string.streaming_error_no_network, 0));
			d.setTitleText(getString(R.string.dialog_title_warning));

			d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
			
				@Override
				public void onBtnClick() {
					removeMyDialog(DIALOG_ID_NO_NETWORK);		
				}});
			dialog = d;
			break;
		}
		case DIALOG_ID_INFO:{
			BaseOneBtnDialog d = new BaseOneBtnDialog(this);
			d.setBodyText(bundle.getString(KEY_INFO_TEXT));
			d.setTitleText(bundle.getString(KEY_INFO_TITLE, getString(R.string.dialog_title_info)));

			d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
			
				@Override
				public void onBtnClick() {
					removeMyDialog(DIALOG_ID_INFO);	
				}});
			dialog = d;
			break;
		}
		case DIALOG_ID_PIN_AUTH_REQUEST:{
			BaseOneBtnDialog d = new BaseOneBtnDialog(this);
			d.setBodyText(bundle.getString(KEY_INFO_TEXT));
			d.setTitleText(getString(R.string.dialog_title_info));
			d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
			
				@Override
				public void onBtnClick() {
					removeMyDialog(DIALOG_ID_PIN_AUTH_REQUEST);	
				}});
			dialog = d;
			
			break;
		}
		case DIALOG_ID_OTA_FORCE_UPDATE:{
			BaseOneBtnDialog d = new BaseOneBtnDialog(this);
			d.setBodyText(getString(R.string.desc_dialog_cam_force_update_remind));
			d.setTitleText(getString(R.string.dialog_title_attention));
			d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
				@Override
				public void onBtnClick() {
					removeMyDialog(DIALOG_ID_OTA_FORCE_UPDATE);	
				}});
			dialog = d;
			break;
		}
		case DIALOG_ID_OTA_WS_DISCONN:{
			BaseOneBtnDialog d = new BaseOneBtnDialog(this);
			d.setBodyText(getString(R.string.desc_dialog_cam_offline_during_ota));
			d.setTitleText(getString(R.string.dialog_title_warning));
			d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
				@Override
				public void onBtnClick() {
					removeMyDialog(DIALOG_ID_OTA_WS_DISCONN);	
				}});
			dialog = d;
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
			case DIALOG_ID_SETTING:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_setting), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_LOGIN:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_msg_login), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_SIGNUP:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_msg_signup), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_PLAYER_CAPTURE:{
				dialog = ProgressDialog.show(this, "", getString(R.string.player_screenshot_capturing), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_DETACH_CAM:{
				dialog = ProgressDialog.show(this, "", getString(R.string.cam_setting_title_detach_cam), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
//			case DIALOG_ID_CAM_UPDATE:{
//				dialog = new Dialog(this);
//				if(null != dialog){
//					dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.transparent)));
//					dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
//					dialog.setContentView(inflateCamUpdateView());
//					dialog.setCancelable(false);
//				}
//				break;
//			}
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
				if(dialog instanceof android.app.AlertDialog){
					if(0 < strMsgRes.length())
						((android.app.AlertDialog) dialog).setMessage(strMsgRes);
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
				if(DEBUG)
					Log.d(TAG, "removeDialog(), iDialogId="+miLastDialog);
				removeDialog(miLastDialog);
			}
			mbCompleted = true;
		}
	}
	
	public boolean showMyDialog(int iDialogId, Bundle bundle){
		if(false == mActivityDestroy && 0 <= iDialogId){
			if(null != mRemoveDialogRunnable && false == mRemoveDialogRunnable.mbCompleted && mRemoveDialogRunnable.miLastDialog == iDialogId){
				if(DEBUG)
					Log.d(TAG, "showMyDialog(), remove mRemoveDialogRunnable, iDialogId="+iDialogId);
				mHandler.removeCallbacks(mRemoveDialogRunnable);
				mRemoveDialogRunnable = null;
				return true;
			}
			if(DEBUG)
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
    private Map<AsyncTask<String, Double, List<JSONObject>>, AsyncTaskParams> mMapCurAsyncTasks;
    private AsyncTask<String, Double, List<JSONObject>> mLastAsyncTask = null;
    //private AsyncTaskParams mLastTaskParams = null;
    
    static class AsyncTaskParams{
    	AsyncTaskParams(boolean bCancelWhenDestroy, String... strArgs){
    		this.bCancelWhenDestroy = bCancelWhenDestroy;
    		this.strArgs = strArgs;
    	}
    	boolean bCancelWhenDestroy= false;
    	String[] strArgs;
    }
    
    public void monitorAsyncTask(AsyncTask<String, Double, List<JSONObject>> task, boolean bCancelWhenDestroy, String... strArgs){
    	monitorAsyncTask(task, bCancelWhenDestroy, FULL_TASK_EXECUTOR, strArgs);
    }
    
    protected void showNoNetworkDialog(){
		showMyDialog(DIALOG_ID_NO_NETWORK);
	}
    
    protected void hideNoNetworkDialog(){
  		removeMyDialog(DIALOG_ID_NO_NETWORK);
  		removeMyDialog(DIALOG_ID_WARNING);
  	}
    
    public void onConnectivityChanged(boolean bNetworkConnected){
    	if(bNetworkConnected){
    		hideNoNetworkDialog();
    		onSessionComplete();
    		checkForUpdates();
    	}else{
    		showNoNetworkDialog();
//    		if(isCamUpdatingInCurrentPage()){
//    			mbIsNetworkDisconnectedWhenCamUpdating = true;
//    		}
    	}
    }
    
    public void monitorAsyncTask(AsyncTask<String, Double, List<JSONObject>> task, boolean bCancelWhenDestroy, ExecutorService executor , String... strArgs){
    	if(null != task){
        	//Log.i(TAG, "Check network status");
        	if(NetworkMgr.getInstance().isNetworkConnected()){
        		if(null != mMapCurAsyncTasks){
            		mMapCurAsyncTasks.put(task, new AsyncTaskParams(bCancelWhenDestroy, strArgs));
            	}
        		
        		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1){
        			task.execute(strArgs);
        		}else{
        			task.executeOnExecutor(executor, strArgs);
        		}
        	}else{
        		Log.e(TAG, "Network disconnected");
        		showNoNetworkDialog();
        		onErrorReport(task, BeseyeHttpTask.ERR_TYPE_NO_CONNECTION, "", "");
        	}
    	}
    }
    
    protected void cancelRunningTasks(){
    	if(null != mMapCurAsyncTasks){
    		for(AsyncTask<String, Double, List<JSONObject>> task:mMapCurAsyncTasks.keySet()){
    			AsyncTaskParams params = mMapCurAsyncTasks.get(task);
    			if((null == params || true == params.bCancelWhenDestroy) && AsyncTask.Status.FINISHED != task.getStatus())
    				task.cancel(true);
    		}
    		mMapCurAsyncTasks.clear();
    	} 
    }
    
    protected void recordLastAsyncTask(AsyncTask<String, Double, List<JSONObject>> task){
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
    	//mLastTaskParams = null;
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
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeAccountTask.CheckAccountTask){
			//onSessionInvalid();
		}else if(task instanceof BeseyeAccountTask.LogoutHttpTask){
			//SessionMgr.getInstance().cleanSession();
			onSessionInvalid(true);
		}else if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
			//onToastShow(task, "failed to update sw");
		}else if(task instanceof BeseyeCamBEHttpTask.GetCamUpdateStatusTask){
			//removeMyDialog(DIALOG_ID_CAM_UPDATE);
			//onToastShow(task, "failed to update status");
		}/*else{
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					onServerError();
				}}, 0);	
		}*/
		
		if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal()){
//			if(null != strMsg && 0 < strMsg.length())
//				onToastShow(task, strMsg);
			Log.e(TAG, "onErrorReport(), task:["+task.getClass().getSimpleName()+"], iErrType:"+iErrType+", strTitle:"+strTitle+", strMsg:"+strMsg);
		}
	}
	
	protected void onServerError(int iErrCode){
		showErrorDialog(R.string.server_error, false, iErrCode);
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, final List<JSONObject> result, final int iRetCode) {
		if(DEBUG)
			Log.i(TAG, "BeseyeBaseActivity::onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.CheckAccountTask){
				if(0 == iRetCode){
					slLastTimeToCheckSession = System.currentTimeMillis();
					if(false == SessionMgr.getInstance().getIsTrustDev()){
						SessionMgr.getInstance().setIsTrustDev(true);
					}
					invokeSessionComplete();
				}/*else if(BeseyeError.E_BE_ACC_USER_SESSION_EXPIRED == iRetCode  || BeseyeError.E_BE_ACC_USER_SESSION_NOT_FOUND_BY_TOKEN == iRetCode){
					Toast.makeText(this, getString(R.string.toast_session_invalid), Toast.LENGTH_SHORT).show();
					onSessionInvalid(false);
				}else if(BeseyeError.E_BE_ACC_USER_SESSION_CLIENT_IS_NOT_TRUSTED == iRetCode){
					launchDelegateActivity(BeseyeTrustDevAuthActivity.class.getName());
				}*/else if(BeseyeError.E_BE_ACC_USER_SESSION_EXPIRED != iRetCode && 
						   BeseyeError.E_BE_ACC_USER_SESSION_NOT_FOUND_BY_TOKEN != iRetCode && 
						   BeseyeError.E_BE_ACC_USER_SESSION_CLIENT_IS_NOT_TRUSTED != iRetCode){
					onServerError(iRetCode);
				}
			}else if(task instanceof BeseyeAccountTask.GetCamInfoTask){
				if(0 == iRetCode){
					JSONObject cam_obj = BeseyeJSONUtil.getJSONObject(result.get(0), BeseyeJSONUtil.ACC_VCAM);
					if(null != cam_obj){
						if(task == mGetNewCamTask){
							//workaround
							SessionMgr.getInstance().setIsCertificated(true);						
							Bundle b = new Bundle();
							b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
							launchDelegateActivity(SoundPairingNamingActivity.class.getName(), b);
							
							mGetNewCamTask = null;
						}else{
							String strVcamId = BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID);
							if(false == BeseyeJSONUtil.getJSONBoolean(cam_obj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
								BeseyeStorageAgent.doDeleteCacheByFolder(this, strVcamId);
								if(null != strVcamId && strVcamId.equals(mStrVCamID)){
									if(mActivityResume){
					    				Toast.makeText(this, String.format(getString(R.string.toast_cam_deactivated), BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME)), Toast.LENGTH_SHORT).show();
									}
									finish();
								}
							}
							
							if(null != mCam_obj){
								BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME));
								BeseyeJSONUtil.setJSONBoolean(mCam_obj, BeseyeJSONUtil.ACC_VCAM_ATTACHED, BeseyeJSONUtil.getJSONBoolean(cam_obj, BeseyeJSONUtil.ACC_VCAM_ATTACHED));
								BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_PLAN, BeseyeJSONUtil.getJSONInt(cam_obj, BeseyeJSONUtil.ACC_VCAM_PLAN));
							}else{
								if(DEBUG)
									Log.i(TAG, getClass().getSimpleName()+":: GetCamInfoTask,  mCam_obj is replaced");
								mCam_obj = cam_obj;
							}
							
							BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(strVcamId, mCam_obj);
							
							if(true == ((BeseyeAccountTask.GetCamInfoTask)task).getNeedToLoadCamSetup() && null != mStrVCamID){
								monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
							}
						}
					}
				}
			}else if(task instanceof BeseyeAccountTask.LogoutHttpTask){
				if(0 == iRetCode){
					//Log.i(TAG, "onPostExecute(), "+result.toString());
					if(BeseyeConfig.PRODUCTION_VER)
						SessionMgr.getInstance().setAccount("");
					
					onSessionInvalid(true);
				}
			}/*else if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(task == mGetCamListTask){
					JSONObject objVCamList = result.get(0);
					getCamUpdateCandidateList(objVCamList);
				}
			}*/else if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, getClass().getSimpleName()+"::onPostExecute(), "+result.toString());
					
					JSONObject obj = result.get(0);
					if(null != obj){
						JSONObject dataObj = BeseyeJSONUtil.getJSONObject(obj, ACC_DATA);
						if(null != dataObj){
							BeseyeJSONUtil.setJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(obj, BeseyeJSONUtil.OBJ_TIMESTAMP));
							BeseyeJSONUtil.setJSONObject(mCam_obj, ACC_DATA, dataObj);
							BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
						}
					}
					
					mOnResumeUpdateCamInfoRunnable = null;
				}
			}/*else if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
				final String strVcamId = ((BeseyeCamBEHttpTask.UpdateCamSWTask)task).getVcamId();
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString()+", strVcamId:"+strVcamId);
					
					if(null != mLstUpdateCandidate){
						if(mLstUpdateCandidate.contains(strVcamId)){
							BeseyeJSONUtil.setJSONObject(mUpdateVcamList, strVcamId, new JSONObject());
						}
					}
				}else{
					if(null != mUpdateVcamList){
						if(iRetCode != BeseyeError.E_OTA_SW_UPDATING){
							mLstUpdateCandidate.remove(strVcamId);
							miCheckUpdateCamIdx--;
						}else{
							if(DEBUG)
								Log.i(TAG, "onPostExecute(), strVcamId["+strVcamId+"] is updating");
						}
						
						if(iRetCode != BeseyeError.E_OTA_SW_ALRADY_LATEST &&
						   iRetCode != BeseyeError.E_OTA_SW_UPDATING &&	
						   iRetCode != BeseyeError.E_WEBSOCKET_CONN_NOT_EXIST && 
						   iRetCode != BeseyeError.E_WEBSOCKET_OPERATION_FAIL && 
						   !((BeseyeHttpTask)task).isNetworkTimeoutErr() && 
						   !BeseyeUtils.isProductionVersion()){
							BeseyeUtils.postRunnable(new Runnable(){
								@Override
								public void run() {
									if(BeseyeBaseActivity.this.mActivityResume){
										String strMsg = (0 < result.size())?BeseyeJSONUtil.getJSONString(result.get(0), "exceptionMessage"):"";
										Bundle b = new Bundle();
			        					b.putString(KEY_INFO_TITLE, "Cam update failed");
			        					b.putString(KEY_INFO_TEXT, String.format("Msg:[%s]\nerrCode:[0x%x]\nCamName:[%s]\nVcam_id:[%s]", strMsg, iRetCode, findCamNameFromVcamUpdateList(strVcamId), strVcamId));
			        					showMyDialog(DIALOG_ID_INFO, b);
									}
								}}, 200);
						}
					}
				}
				
				if(DEBUG)
					Log.i(TAG, "miCheckUpdateCamIdx:"+miCheckUpdateCamIdx+", mLstUpdateCandidate.size():"+mLstUpdateCandidate.size());
				
				if(miCheckUpdateCamIdx == mLstUpdateCandidate.size()){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), Check point..., size:"+mLstUpdateCandidate.size());	
					
					if(0 < mLstUpdateCandidate.size()){
						miUpdateCamNum = mLstUpdateCandidate.size();
						miCurUpdateCamStatusIdx = 0;
						
						showMyDialog(DIALOG_ID_CAM_UPDATE);
						SessionMgr.getInstance().setCamUpdateTimestamp(System.currentTimeMillis());
						SessionMgr.getInstance().setCamUpdateList(arrayToString(mLstUpdateCandidate));
						
						monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++));
					}else{
						if(!mbSilentUpdate){
							BeseyeUtils.postRunnable(new Runnable(){
								@Override
								public void run() {
									Bundle b = new Bundle();
									b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_update_no_valid_cam));
									showMyDialog(DIALOG_ID_WARNING, b);
								}}, 0);
						}	
						initUpdateItems(false);
						Log.e(TAG, "onPostExecute(), there is no valid camera to update");	
					}
					
				}else{
					monitorAsyncTask(new BeseyeCamBEHttpTask.UpdateCamSWTask(this).setDialogId(mbSilentUpdate?-1:DIALOG_ID_LOADING), true, mLstUpdateCandidate.get(miCheckUpdateCamIdx++));
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetCamUpdateStatusTask){
				String strVcamId = ((BeseyeCamBEHttpTask.GetCamUpdateStatusTask)task).getVcamId();
				if(0 == iRetCode){					
					BeseyeJSONUtil.setJSONObject(mUpdateVcamList, strVcamId, result.get(0));
					updateCamUpdateProgress();
				}else{
					//If task timeout, keep tracking 
					if(Integer.MIN_VALUE != iRetCode){
						mLstUpdateCandidate.remove(strVcamId);
						miUpdateCamNum-=1;
					}
				}
				
				if(isCamUpdatingCompleted() || 0 == miUpdateCamNum){
					removeMyDialog(DIALOG_ID_CAM_UPDATE);
					initUpdateItems(false);
					Log.i(TAG, "onPostExecute(), Camera update finish...");
				}else{
					if(miCurUpdateCamStatusIdx >= miUpdateCamNum){
						miCurUpdateCamStatusIdx = 0;
						BeseyeUtils.postRunnable(new Runnable(){
							@Override
							public void run() {
								if(checkWithinCamUpdatePeriod()){
									monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(BeseyeBaseActivity.this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++%miUpdateCamNum));
								}else{
									Bundle b = new Bundle();
									b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(BeseyeBaseActivity.this, R.string.cam_update_timeout, BeseyeError.E_FE_AND_OTA_TIMEOUT));
									showMyDialog(DIALOG_ID_WARNING, b);
									removeMyDialog(DIALOG_ID_CAM_UPDATE);
								}
							}}, 5000);
					}else{
						monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(BeseyeBaseActivity.this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++%miUpdateCamNum));
					}
				} 
			}*/else if(task instanceof BeseyeUpdateBEHttpTask.GetLatestAndroidAppVersionTask){
				if(0 == iRetCode){		
					String strPkg = BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.UPDATE_PKG_NAME);
					//String strName = BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.UPDATE_VER_NAME);
					
					int strVerCode = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.UPDATE_VER_CODE);
					
					if(getPackageName().equals(strPkg) && BeseyeUtils.getPackageVersionCode() < strVerCode){
						Log.i(TAG, "onPostExecute(), BeseyeUpdateBEHttpTask.GetLatestAndroidAppVersionTask has new version: "+strVerCode);
						launchUpdateApp();
						//Toast.makeText(BeseyeBaseActivity.this, "Update for "+strPkg+" to version "+strVerCode, Toast.LENGTH_LONG).show();
					}else{
						Log.i(TAG, "onPostExecute(), BeseyeUpdateBEHttpTask.GetLatestAndroidAppVersionTask latest version: "+strVerCode);
						//Log.i(TAG, "onPostExecute(), BeseyeUpdateBEHttpTask.GetLatestAndroidAppVersionTask error: "+iRetCode);
						onAppUpdateNotAvailable();
						//Toast.makeText(BeseyeBaseActivity.this, "Not Update for "+strPkg+" to version "+strVerCode, Toast.LENGTH_LONG).show();
					}
				}else{
					onAppUpdateNotAvailable();
					Log.e(TAG, "onPostExecute(), BeseyeUpdateBEHttpTask.GetLatestAndroidAppVersionTask error: "+Integer.toHexString(iRetCode));
					//Toast.makeText(BeseyeBaseActivity.this, "Update app error "+Integer.toHexString(iRetCode), Toast.LENGTH_LONG).show();
				}
			}
		}
		
//		if(task == mGetCamListTask){
//			mGetCamListTask = null;
//		}
		
		if(null != mMapCurAsyncTasks){
			mMapCurAsyncTasks.remove(task);
		}
	}
	
//	protected void getCamUpdateCandidateList(JSONObject objVCamList){
//		//Log.i(TAG, "mGetCamListTask(), objVCamList="+objVCamList.toString());
//		
//		if(!BeseyeFeatureConfig.CAM_SW_UPDATE_CHK || SessionMgr.getInstance().getIsCamSWUpdateSuspended()){
//			return;
//		}
////		
////		if(checkCamUpdateValid()){
////			return ;
////		}
//		
//		JSONArray arrVcamIdList = new JSONArray();
//		int iVcamCnt = BeseyeJSONUtil.getJSONInt(objVCamList, BeseyeJSONUtil.ACC_VCAM_CNT);
//		if(0 < iVcamCnt){
//			JSONArray VcamList = BeseyeJSONUtil.getJSONArray(objVCamList, BeseyeJSONUtil.ACC_VCAM_LST);
//			for(int i = 0;i< iVcamCnt;i++){
//				try {
//					JSONObject camObj = VcamList.getJSONObject(i);
//					if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
//						arrVcamIdList.put(camObj);
//					}
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			
//			if(0 < arrVcamIdList.length()){
//				if(checkWithinCamUpdatePeriod()){
//					resumeCamUpdate(arrVcamIdList);
//				}else{
//					BeseyeCamInfoSyncMgr.getInstance().queryCamUpdateVersions(arrVcamIdList);
//				}
//			}
//		}
//		
//		slLastGetCamListTs = System.currentTimeMillis();
//	}

	@Override
	public void onToastShow(AsyncTask task,final String strMsg) {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
					Toast.makeText(BeseyeBaseActivity.this, strMsg, Toast.LENGTH_LONG).show();
			}});
	}
	
	private Runnable mLogoutRunnable = null;
	static final private long TIME_TO_WAIT_DEL_PUSH = 10000L;
	
	protected void invokeLogout(){
		if(null != mLogoutRunnable){
			BeseyeUtils.removeRunnable(mLogoutRunnable);
		}
		
		mLogoutRunnable = new Runnable(){
			@Override
			public void run() {
				monitorAsyncTask(new BeseyeAccountTask.LogoutHttpTask(BeseyeBaseActivity.this), true, SessionMgr.getInstance().getAuthToken());
				mLogoutRunnable = null;
			}};
			
		//Wait for unregister push service in background process
		BeseyeUtils.postRunnable(mLogoutRunnable, TIME_TO_WAIT_DEL_PUSH);
		if(null != mNotifyService){
			try {
				if(DEBUG)
					Log.i(TAG, "invokeLogout(), send MSG_REQUEST_DEL_PUSH");
				mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_REQUEST_DEL_PUSH));
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		Bundle b  = new Bundle();
		b.putString(KEY_WARNING_TEXT, getString(R.string.dialog_msg_logiout));	
		showMyDialog(DIALOG_ID_LOADING, b);
	}

	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason) {
		Log.i(TAG, "onSessionInvalid(), iInvalidReason:"+iInvalidReason+", mbIgnoreSessionCheck:"+mbIgnoreSessionCheck);
		if(!mbIgnoreSessionCheck){
			if(iInvalidReason == BeseyeHttpTask.ERR_TYPE_SESSION_NOT_TRUST){
				SessionMgr.getInstance().setIsTrustDev(false);
				launchDelegateActivity(BeseyeTrustDevAuthActivity.class.getName());
			}else{
				onSessionInvalid(false);
			}
		}
	}
	
	protected void invalidDevSession(){
		SessionMgr.getInstance().cleanSession();
		BeseyeNewFeatureMgr.getInstance().reset();
		launchDelegateActivity(BeseyeEntryActivity.class.getName());
	}
	
	protected void onSessionInvalid(boolean bIsLogoutCase){
		if(SessionMgr.getInstance().isTokenValid()){
			if(false == bIsLogoutCase){
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						Toast.makeText(BeseyeBaseActivity.this, getString(R.string.toast_session_invalid), Toast.LENGTH_SHORT).show();
					}}, 0L);
			}
			invalidDevSession();
		}else{
			Log.i(TAG, "onSessionInvalid(), token is invalid");
		}
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
		intent.setPackage(getPackageName());
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
    	intent.setAction("android.intent.action.MAIN"); 
    	intent.addCategory("android.intent.category.LAUNCHER"); 
		intent.putExtra("ClassName", strCls);
		intent.setPackage(getPackageName());
		intent.setClass(this, OpeningPage.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK );
		//intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
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
	
	final public void notifyServicePincodeNotifyClick(String strPincodeInfo){
		if(null != mNotifyService){
			try {
				mNotifyService.send(Message.obtain(null, BeseyeNotificationService.MSG_PIN_CODE_NOTIFY_CLICKED));
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
	
	//TO notify the latest session data to service
	public void onUpdateFocusVCamId(String strVCamId){
		if(null != mNotifyService){
			try {
				Message msg = Message.obtain(null, BeseyeNotificationService.MSG_UPDATE_PLAYER_VCAM);
				if(null != msg){
					msg.getData().putString("VCAMID", strVCamId);
					mNotifyService.send(msg);
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	//TO extend ws connection to avoid mssing cam activate event in bg
	protected void extendWSConnection(long lTimeToExtend){
		if(null != mNotifyService){
			try {
				Message msg = Message.obtain(null, BeseyeNotificationService.MSG_EXTEND_WS_CONN);
				if(null != msg){
					msg.getData().putLong(BeseyeNotificationService.MSG_WS_EXTEND, lTimeToExtend);
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
                case BeseyeNotificationService.MSG_CAM_ACTIVATE:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraActivated(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_CAM_DEACTIVATE:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraDeactivated(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_CAM_ONLINE:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraOnline(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;	
                }
                case BeseyeNotificationService.MSG_CAM_OFFLINE:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraOffline(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;	
                }
                case BeseyeNotificationService.MSG_CAM_OTA_START:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraOTAStart(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;	
                }
                case BeseyeNotificationService.MSG_CAM_OTA_END:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraOTAEnd(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;	
                }
                case BeseyeNotificationService.MSG_CAM_EVENT_FACE:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraPeopleEvent(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_CAM_EVENT_HUMAN:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraHumanDetectEvent(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_CAM_EVENT_MOTION:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraMotionEvent(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_CAM_EVENT_OFFLINE:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCameraOfflineEvent(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_USER_PW_CHANGED:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onPasswordChanged(dataObj);
						} catch (JSONException e) {
							Log.i(TAG, "handleMessage(), e:"+e.toString());
						}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_RESPOND_DEL_PUSH:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
	                	if(null != act.mLogoutRunnable){
	                		BeseyeUtils.removeRunnable(act.mLogoutRunnable);
	                		act.mLogoutRunnable.run();
	                	}
                	}
                	break;
                }
                case BeseyeNotificationService.MSG_CAM_STATUS_CHANGED_FOR_EVT:{
                	BeseyeBaseActivity act = mActivity.get();
                	if(null != act){
                		JSONObject dataObj;
						try {
							Bundle b = msg.getData();
							dataObj = new JSONObject(b.getString(BeseyeNotificationService.MSG_REF_JSON_OBJ));
							act.onCamStatusChangedForEvt(dataObj);
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
        bindService(new Intent(BeseyeBaseActivity.this, BeseyeNotificationService.class), mConnection, Context.BIND_AUTO_CREATE);
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
			if(null != mStrVCamID){
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(BeseyeBaseActivity.this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, mStrVCamID);
			}
		}
    }
    
    protected OnResumeUpdateCamInfoRunnable mOnResumeUpdateCamInfoRunnable = null;
    protected void setOnResumeUpdateCamInfoRunnable(OnResumeUpdateCamInfoRunnable run){
    	if(DEBUG)
    		Log.i(TAG, getClass().getSimpleName()+"::setOnResumeUpdateCamInfoRunnable()");
    	mOnResumeUpdateCamInfoRunnable = run;
    }
    
    protected boolean isSameIdOnResumeUpdateCamInfoRunnable(String strVCamId){
    	return (null != mOnResumeUpdateCamInfoRunnable && mOnResumeUpdateCamInfoRunnable.isSameVCamId(strVCamId));
    }
    
    private void checkOnResumeUpdateCamInfoRunnable(){
    	if(null != mOnResumeUpdateCamInfoRunnable){
    		if(DEBUG)
    			Log.i(TAG, "checkOnResumeUpdateCamInfoRunnable(), trigger...");
    		BeseyeUtils.postRunnable(mOnResumeUpdateCamInfoRunnable, 0);
    		mOnResumeUpdateCamInfoRunnable = null;
    	}
    }
    
    protected void onCamSettingChangedCallback(JSONObject DataObj){
    	if(null != mStrVCamID){
    		if(null != DataObj){
    			String strCamUID = BeseyeJSONUtil.getJSONString(DataObj, WS_ATTR_CAM_UID);
    			//long lTs = BeseyeJSONUtil.getJSONLong(DataObj, WS_ATTR_TS);
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
    
    protected AsyncTask<String, Double, List<JSONObject>> mGetNewCamTask;
    
    protected boolean onCameraActivated(JSONObject msgObj){
    	if(! (this instanceof SoundPairingActivity) && mActivityResume){
    		Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(),  msgObj = "+msgObj);
    		if(null != msgObj){
        		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
        		if(null != objCus){
        			String strPairToken = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_PAIR_TOKEN);
        			if(null != strPairToken && strPairToken.equals(SessionMgr.getInstance().getPairToken())){
        				Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(), find match strPairToken = "+strPairToken);
        				String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.MM_VCAM_UUID);
        				monitorAsyncTask(mGetNewCamTask = new BeseyeAccountTask.GetCamInfoTask(this), false, strCamUID);
        				SessionMgr.getInstance().setPairToken("");
        				Toast.makeText(this, getString(R.string.toast_new_cam_activated), Toast.LENGTH_SHORT).show();
        			}
        			return true;
        		}
    		}
    	}
    	return false;
    }
    
    protected boolean onCameraDeactivated(JSONObject msgObj){
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraDeactivated(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != strCamUID && strCamUID.equals(mStrVCamID)){
    				launchDelegateActivity(CameraListActivity.class.getName());
    				Toast.makeText(this, String.format(getString(R.string.toast_cam_deactivated), BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_NAME)), Toast.LENGTH_SHORT).show();
    			}
    			BeseyeCamInfoSyncMgr.getInstance().removeCamInfoByVCamId(strCamUID);
    			return true;
    		}
		}
    	return false;
    }
    
    protected boolean onCameraOnline(JSONObject msgObj){
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraOnline(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != mStrVCamID && mStrVCamID.equals(strCamUID)){
    				if(!mActivityDestroy){
    		    		if(!mActivityResume){
    		    			setOnResumeUpdateCamInfoRunnable(new OnResumeUpdateCamInfoRunnable(mStrVCamID));
    		    		}else{
    		    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, mStrVCamID);
    		    		}
    		    	}
    			}
    			return true;
    		}
		}
    	return false;
    }
    
    protected boolean onCameraOffline(JSONObject msgObj){
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraOffline(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != mStrVCamID && mStrVCamID.equals(strCamUID)){
    				if(!mActivityDestroy){
    		    		if(!mActivityResume){
    		    			setOnResumeUpdateCamInfoRunnable(new OnResumeUpdateCamInfoRunnable(mStrVCamID));
    		    		}else{
    		    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, mStrVCamID);
    		    		}
    		    	}
    			}
    			return true;
    		}
		}
    	return false;
    }
    
    protected boolean onCameraOTAStart(JSONObject msgObj){
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraOTAStart(),  msgObj = "+msgObj);
    	return false;
    }
    
    protected boolean onCameraOTAEnd(JSONObject msgObj){
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraOTAEnd(),  msgObj = "+msgObj);
		if(mActivityResume && null != msgObj){
    		final JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			BeseyeUtils.postRunnable(new Runnable(){
    				@Override
    				public void run() {
    					Toast.makeText(BeseyeBaseActivity.this, String.format(getString(R.string.desc_cam_update_finish), BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_NAME)), Toast.LENGTH_SHORT).show();
    				}}, 0);
    			return true;
    		}
		}
    	return false;
    }
    
    protected boolean onCameraMotionEvent(JSONObject msgObj){return false;}
    protected boolean onCameraPeopleEvent(JSONObject msgObj){return false;}
    protected boolean onCameraHumanDetectEvent(JSONObject msgObj){return false;}
    protected boolean onCameraOfflineEvent(JSONObject msgObj){return false;}
    
    public boolean onCamStatusChangedForEvt(JSONObject msgObj){
//Only for ATT event
//   	Log.i(TAG, getClass().getSimpleName()+"::onCamStatusChangedForEvt(),  msgObj = "+msgObj);
//    	if(mActivityResume){
//    		if(null != msgObj){
//        		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
//        		if(null != objCus){
//        			JSONObject objCamChgData = BeseyeJSONUtil.getJSONObject(objCus, CAM_CHANGE_DATA);
//        			boolean bCamStatusOn = (BeseyeJSONUtil.getJSONInt(objCamChgData, BeseyeJSONUtil.CAM_STATUS) == 1);
//					String strNotifyMsg = getString(bCamStatusOn?R.string.att_event_cam_status_on:R.string.att_event_cam_status_off);
//    				Toast.makeText(this, strNotifyMsg, Toast.LENGTH_SHORT).show();
//        			return true;
//        		}
//    		}
//    	}
    	return false;
    }
    
    protected boolean onPasswordChanged(JSONObject msgObj){
    	Toast.makeText(this, getString(R.string.toast_password_changed), Toast.LENGTH_SHORT).show();
    	onSessionInvalid(false);
    	return true;
    }
    
    protected long mlCamSetupObjUpdateTs = -1;
    
    public void onCamSetupChanged(String strVcamId, long lTs, JSONObject objCamSetup){
		if(null != strVcamId && strVcamId.equals(mStrVCamID)){
			long lTsOldTs = BeseyeJSONUtil.getJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP);
			if(DEBUG)
				Log.i(TAG, getClass().getSimpleName()+"::onCamSetupChanged(),  lTs = "+lTs+", lTsOldTs="+lTsOldTs);
			if(0 < lTs && lTs >= lTsOldTs){
				if(DEBUG)
					Log.i(TAG, getClass().getSimpleName()+"::onCamSetupChanged(),  mCam_obj is replaced");
				
				mCam_obj = objCamSetup;
				mOnResumeUpdateCamInfoRunnable = null;
				updateUICallback();
			}
		}
    }
    
    public void onCamUpdateList(JSONArray arrUpdateCandidate){
//    	triggerCamUpdate(arrUpdateCandidate, true);
    }
    
    protected void updateUICallback(){}
    
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
	private JSONArray mVcamUpdateList = null;
	protected JSONObject mObjVCamList = null; //for cam list
	static private long slLastGetCamListTs = 0; 
	private boolean mbIsNetworkDisconnectedWhenCamUpdating = false;
	
	private String findCamNameFromVcamUpdateList(String strVCamId){
		String strRet = "";
		int iVcamNum = (null != mVcamUpdateList)?mVcamUpdateList.length():0;
		for(int idx = 0; idx < iVcamNum;idx++){
			try {
				String strVCamIdChk = BeseyeJSONUtil.getJSONString(mVcamUpdateList.getJSONObject(idx), BeseyeJSONUtil.ACC_ID);
				if(strVCamId.equals(strVCamIdChk)){
					strRet = BeseyeJSONUtil.getJSONString(mVcamUpdateList.getJSONObject(idx), BeseyeJSONUtil.ACC_NAME);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return strRet;
	}
	
//	private void initUpdateItems(boolean bResumeCase){
//		Log.i(TAG, "initUpdateItems()+++");
//
//		mbSilentUpdate = false;
//		mVcamUpdateList = null;
//		miCheckUpdateCamIdx = 0;
//		mUpdateVcamList = new JSONObject();
//		mLstUpdateCandidate = new ArrayList<String>();
//		miCurUpdateCamStatusIdx = 0;
//		if(false == bResumeCase){
//			SessionMgr.getInstance().setCamUpdateTimestamp(0);
//		}
//	}
//	
//	protected boolean mbSilentUpdate = true;
//	
//	protected void triggerCamUpdate(JSONArray VcamList, boolean bSilent){
//		if(isCamUpdatingInCurrentPage()){
//			if(DEBUG)
//				Log.i(TAG, "triggerCamUpdate(), isCamUpdating... return");
//			return;
//		}
//		
//		if(false == mActivityResume){
//			if(DEBUG)
//				Log.i(TAG, "triggerCamUpdate(), mActivityResume is false... return");
//			return;
//		}
//		
//		int iVcamNum = (null != VcamList)?VcamList.length():0;
//		if(0 < iVcamNum){
//			initUpdateItems(false);
//			mbSilentUpdate = bSilent;
//			mVcamUpdateList = VcamList;
//			//mMapUpdateStatus = new LinkedHashMap<String, JSONObject>();
//			for(int idx = 0; idx < iVcamNum;idx++){
//				try {
//					String strVCamId = BeseyeJSONUtil.getJSONString(VcamList.getJSONObject(idx), BeseyeJSONUtil.ACC_ID);
//					mLstUpdateCandidate.add(strVCamId);
//					if(DEBUG)
//						Log.i(TAG, "triggerCamUpdate(), strVcamId:"+strVCamId);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			}
//			
//			monitorAsyncTask(new BeseyeCamBEHttpTask.UpdateCamSWTask(this).setDialogId(bSilent?-1:DIALOG_ID_LOADING), true, mLstUpdateCandidate.get(miCheckUpdateCamIdx++));
//		}else{
//			if(!mbSilentUpdate){
//				BeseyeUtils.postRunnable(new Runnable(){
//					@Override
//					public void run() {
//						Bundle b = new Bundle();
//						b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_update_no_valid_cam));
//						showMyDialog(DIALOG_ID_WARNING, b);
//					}}, 0);
//			}
//			initUpdateItems(false);
//			Log.i(TAG, "triggerCamUpdate(), there is no valid camera to update");
//		}
//		
////		mUpdateVcamList = new JSONObject();
////		mLstUpdateCandidate = new ArrayList<String>();
////		miCurUpdateCamStatusIdx=0;
////		mLstUpdateCandidate.add("01d0f4f7b68e48a583e28449eacf99b2");
////		mLstUpdateCandidate.add("9ee1316073f54242b6c4cfe6aa2a0eda");
////		miUpdateCamNum = 2;
////		showMyDialog(DIALOG_ID_CAM_UPDATE);
////		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++));
//	}
//	
//	protected void resumeCamUpdate(JSONArray VcamList){
//		if(isCamUpdatingInCurrentPage() && false == mbIsNetworkDisconnectedWhenCamUpdating){
//			if(DEBUG)
//				Log.i(TAG, "resumeCamUpdate(), isCamUpdating... return");
//			return;
//		}
//		
//		mbIsNetworkDisconnectedWhenCamUpdating = false;
//		
//		String[] strCamUpdate = SessionMgr.getInstance().getCamUpdateList().split(";");
//		if(null != strCamUpdate){
//			int iNum = strCamUpdate.length;
//			int iVcamNum = (null != VcamList)?VcamList.length():0;
//			if(0 < iVcamNum){
//				initUpdateItems(true);
//				for(int idx = 0; idx < iNum; idx++){
//					for(int idx2 = 0; idx2 < iVcamNum;idx2++){
//						try {
//							if(strCamUpdate[idx].equals(BeseyeJSONUtil.getJSONString(VcamList.getJSONObject(idx2), BeseyeJSONUtil.ACC_ID))){
//								mLstUpdateCandidate.add(strCamUpdate[idx]);
//								if(DEBUG)
//									Log.i(TAG, "resumeCamUpdate(), strCamUpdate[idx]:"+strCamUpdate[idx]);
//								break;
//							}
//							
//						} catch (JSONException e) {
//							e.printStackTrace();
//						}
//					}
//				}
//				
//				miUpdateCamNum = mLstUpdateCandidate.size();
//				if(0 < miUpdateCamNum){
//					showMyDialog(DIALOG_ID_CAM_UPDATE);
//					monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this).setDialogId(-1), true, mLstUpdateCandidate.get(miCurUpdateCamStatusIdx++));
//				}else{
//					Log.i(TAG, "resumeCamUpdate(), there is no cam to update");
//					initUpdateItems(false);
//				}
//			}
//		}
//	}
//	
//	protected View inflateCamUpdateView(){
//		View vCamUpdate = getLayoutInflater().inflate(R.layout.layout_camera_update_progress, null);
//		if(null != vCamUpdate){
//			mProgressBarCamUpdate = (ProgressBar)vCamUpdate.findViewById(R.id.sb_update_progress);
//			mTxtCamUpdateStatus = (TextView)vCamUpdate.findViewById(R.id.txt_update_progress);
//		}
//		
//		setCamUpdateProgress(0);
//		return vCamUpdate;
//	}
//	
//	private void setCamUpdateProgress(int iProgress){
//		if(null != mProgressBarCamUpdate)
//			mProgressBarCamUpdate.setProgress(iProgress);
//			
//		if(null != mTxtCamUpdateStatus)
//			mTxtCamUpdateStatus.setText(String.format(getString(R.string.cam_update_progress), iProgress+"%"));
//	}
//	
//	private void updateCamUpdateProgress(){
//		int iProgress = 0;
//		int iCompleteNum = 0;
//		int iNumOfDone = 0;
//		int iNumOfFail = 0;
//		for(int idx = 0; idx < miUpdateCamNum && idx < mLstUpdateCandidate.size(); idx++){
//			String strVCamId = mLstUpdateCandidate.get(idx);
//			JSONObject objCamUpdateStatus = BeseyeJSONUtil.getJSONObject(mUpdateVcamList, strVCamId);
//			if(null != objCamUpdateStatus){
//				int iFinalStatus = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_FINAL_STAUS, -1);
//				
//				if(DEBUG)
//					Log.i(TAG, "updateCamUpdateProgress(), "+objCamUpdateStatus+", strVcamId:"+strVCamId+", iFinalStatus="+iFinalStatus);
//				
//				if(-1 !=  iFinalStatus){
//					iCompleteNum++;
//					iProgress+=(100.0/miUpdateCamNum);
//				}else{
//					if(0 == iFinalStatus || 1 == iFinalStatus){
//						iNumOfDone++;
//					}else{
//						iNumOfFail++;
//					}
//					int iProgressIdv = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_PROGRESS, 0);
//					iProgress+=((100.0/miUpdateCamNum))*(iProgressIdv/100.0);
//				}
//			}
//		}
//		
//		if(iCompleteNum < miUpdateCamNum){
//			setCamUpdateProgress(iProgress);
//		}else{
//			removeMyDialog(DIALOG_ID_CAM_UPDATE);
//			Bundle b = new Bundle();
//			String strRet = "";
//			if(0 == iNumOfFail){
//				strRet = getResources().getString(R.string.cam_update_success);
//			}else if(0 == iNumOfDone){
//				strRet = BeseyeUtils.appendErrorCode(BeseyeBaseActivity.this, R.string.cam_update_failed, BeseyeError.E_FE_AND_OTA_TIMEOUT);
//			}else{
//				strRet = String.format(getResources().getString(R.string.cam_update_result), iNumOfDone, iNumOfFail);
//			}
//			
//			b.putString(KEY_INFO_TEXT, strRet);
//			showMyDialog(DIALOG_ID_INFO, b);
//			initUpdateItems(false);
//			Log.i(TAG, "updateCamUpdateProgress(), Update SW successfully");
//		}	
//	}
//	
//	protected boolean isCamUpdatingInCurrentPage(){
//		return (null != mLstUpdateCandidate && 0 < mLstUpdateCandidate.size());
//	}
//	
//	private boolean isCamUpdatingCompleted(){
//		boolean bRet = true;
//		for(int idx = 0; idx < miUpdateCamNum && idx < mLstUpdateCandidate.size(); idx++){
//			String strVCamId = mLstUpdateCandidate.get(idx);
//			JSONObject objCamUpdateStatus = BeseyeJSONUtil.getJSONObject(mUpdateVcamList, strVCamId);
//			if(null != objCamUpdateStatus){
//				int iFinalStatus = BeseyeJSONUtil.getJSONInt(objCamUpdateStatus, BeseyeJSONUtil.UPDATE_FINAL_STAUS, -1);
//				if(DEBUG)
//					Log.i(TAG, "isCamUpdateFinish(), "+objCamUpdateStatus+", strVcamId:"+strVCamId+", iFinalStatus="+iFinalStatus);
//				if(-1 == iFinalStatus){//if final status is -1, means updating is ongoing
//					bRet = false;
//					break;
//				}
//			}
//		}
//		
//		return bRet;
//	}
//	
//	//If timestamp is 0, means update is done
//	protected boolean checkCamUpdateDone(){
//		return SessionMgr.getInstance().getCamUpdateTimestamp() == 0;
//	}
//	
//	//Wait cam for 10 mins at most
//	protected boolean checkWithinCamUpdatePeriod(){
//		boolean  bRet = true;
//		long lDelta = System.currentTimeMillis() - SessionMgr.getInstance().getCamUpdateTimestamp();
//		if(lDelta > 10*60*1000){//timeout after 10 mis
//			SessionMgr.getInstance().setCamUpdateTimestamp(0);
//			bRet = false;
//		}
//		if(DEBUG)
//			Log.i(TAG, "checkCamUpdateValid(), lDelta:"+lDelta+", bRet:"+bRet);
//		return bRet;
//	}
//	
//	//Save update cam list in pref file for future checking
//	protected String arrayToString(List<String> lstUpdateCandidate){
//		String strRet = "";
//		int iNum = (null != lstUpdateCandidate)?lstUpdateCandidate.size():0;
//		for(int idx = 0; idx < iNum; idx++){
//			strRet += ((0 == idx)?"":";")+lstUpdateCandidate.get(idx);
//		}
//		if(DEBUG)
//			Log.i(TAG, "arrayToString(), strRet:"+strRet);
//		return strRet;
//	}
//	
//	//Camera update end
	
	protected void showErrorDialog(final int iMsgId, final boolean bCloseSelf, final int iErrCode){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				Bundle b = new Bundle();
				b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(BeseyeBaseActivity.this, iMsgId, iErrCode));
				if(bCloseSelf)
					b.putBoolean(KEY_WARNING_CLOSE, true);
				showMyDialog(DIALOG_ID_WARNING, b);
			}}, 0);
	}
}
