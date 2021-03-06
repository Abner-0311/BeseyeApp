package com.app.beseye;


import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.HOCKEY_APP_ID;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.acra.ACRA;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderFactory;
import org.acra.config.ACRAConfiguration;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import android.app.Application;
//import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.httptask.SessionMgr.SessionData;
import com.app.beseye.receiver.UBTEventBroadcastReciever;
import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeFeatureConfig;
import com.app.beseye.util.BeseyeLocationMgr;
import com.app.beseye.util.BeseyeNewFeatureMgr;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.DeviceUuidFactory;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.widget.BeseyeMemCache;
import com.facebook.FacebookSdk;

@ReportsCrashes(logcatArguments = { "-t", "2500", "-v", "long", "BesEye:W", "*:S" },
				mode = ReportingInteractionMode.TOAST,
				alsoReportToAndroidFramework = false,
				resToastText = R.string.crash_toast_text,
				reportSenderFactoryClasses ={com.app.beseye.BeseyeApplication.HockeySenderFactory.class},
				customReportContent = { ReportField.PACKAGE_NAME, ReportField.APP_VERSION_CODE, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.BUILD, ReportField.TOTAL_MEM_SIZE,
										ReportField.AVAILABLE_MEM_SIZE, ReportField.STACK_TRACE, ReportField.THREAD_DETAILS, ReportField.LOGCAT, ReportField.EVENTSLOG, ReportField.DUMPSYS_MEMINFO})

public class BeseyeApplication extends Application {

	private static Application sApplication;
	/* Used for flurry trackers identification across multi-process 
	   -- Begin */
	final static public String BESEYE_MAIN_PROCESS = "com.app.beseye";
	static private String sCurProcessName = null;
	static private String sStrAppMark = "";
	
	@Override
	public void onCreate() {
		super.onCreate();
		sApplication = this;
		sCurProcessName = BeseyeUtils.getProcessName(this, android.os.Process.myPid());
		
		BeseyeUtils.init();
		SessionMgr.createInstance(getApplicationContext());
		BeseyeNewFeatureMgr.createInstance(getApplicationContext());
		BeseyeUtils.setPackageVersion(getApplicationContext());
		//facebook
		FacebookSdk.sdkInitialize(getApplicationContext());
		
		if(BeseyeConfig.DEBUG){
			Log.i(TAG, "*****************BeseyeApplication::onCreate(), sCurProcessName = \""+sCurProcessName+"\" HOCKEY_APP_ID:"+HOCKEY_APP_ID+", can update:"+BeseyeUtils.canUpdateFromHockeyApp()+", Build.VERSION.RELEASE:"+Build.VERSION.RELEASE);
			Log.i(TAG, "CAM_SW_UPDATE_CHK:"+BeseyeFeatureConfig.CAM_SW_UPDATE_CHK+", ADV_TWO_WAY_tALK:"+BeseyeFeatureConfig.ADV_TWO_WAY_TALK+", VPC_NUM_QUERY:"+BeseyeFeatureConfig.VPC_NUM_QUERY);

		}else{ 
			Log.i(TAG, "*****************BeseyeApplication::onCreate(), can update:"+BeseyeUtils.canUpdateFromHockeyApp()+", CAM_SW_UPDATE_CHK:"+BeseyeFeatureConfig.CAM_SW_UPDATE_CHK);
		}


		//ACRA.init(this);
		//ACRA.getErrorReporter().setReportSender(new HockeySender());
		
		NetworkMgr.createInstance(getApplicationContext());
		CamSettingMgr.createInstance(getApplicationContext());
		BeseyeLocationMgr.createInstance(getApplicationContext());
		
		//Log.i(TAG, "*****************BeseyeApplication::onCreate(), Hotspot name:"+NetworkMgr.getInstance().getHotspotName());

		
		checkServerMode();
		
		DeviceUuidFactory.getInstance(getApplicationContext());
			
		startService(new Intent(this,BeseyeNotificationService.class));
		BeseyeMemCache.init(this);
			
		if(null != s_checkBackgroundRunnable){
			s_checkBackgroundRunnable.updateContext(this);
		}    
		
		sStrAppMark = (BeseyeConfig.ALPHA_VER?" (alpha)":(BeseyeConfig.BETA_VER?" (beta)":(BeseyeConfig.DEBUG?" (dev)":"")));
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);

		// The following line triggers the initialization of ACRA
		ACRA.init(this);
	}

	static public class HockeySenderFactory implements ReportSenderFactory {
		@Override
		public ReportSender create(Context context, ACRAConfiguration config) {
			return new HockeySender();
		}
	}
	
	static synchronized public Application getApplication(){
		return sApplication;
	}
	
	static public String getAppMark(){
		return sStrAppMark;
	}
	
//	public static void broadcastEventToMainProcess(Context context, UBT_Event event, int iSes, boolean bIsBeginEvent){
//    	Intent intent = new Intent();
//    	if(null != context && null != intent && null != event){
//    		intent.setAction(UBTEventBroadcastReciever.ACTION_UBT_EVENT);
//    		intent.putExtra(UBTEventBroadcastReciever.UBT_EVENT_OBJ, event);
//    		intent.putExtra(UBTEventBroadcastReciever.UBT_EVENT_SESSION, iSes);
//    		if(bIsBeginEvent)
//    			intent.putExtra(UBTEventBroadcastReciever.UBT_EVENT_BEGIN, true);
//    		else
//    			intent.putExtra(UBTEventBroadcastReciever.UBT_EVENT_END, true);
//    		context.sendBroadcast(intent);
//    	}
//    }
	
	public static String getProcessName(){
		return sCurProcessName;
	}
	
	public static boolean isInMainProcess(){
		return null != sCurProcessName && sCurProcessName.startsWith(BESEYE_MAIN_PROCESS) && !sCurProcessName.endsWith(":remote");///BESEYE_MAIN_PROCESS.equals(sCurProcessName);
	}
	/* Used for flurry trackers identification across multi-process 
	   -- End */

	//private static Handler s_handler = null; 
	private static long s_lLastPauseTs = 0;
	private static volatile int s_iVisibleCount = 0; 
	private static WeakReference<BeseyeAppStateChangeListener> sLastBeseyeAppStateChangeListener;
	private static final long FG_CHECK_LATENCY = 1000L;//1s
	private static final long BG_CHECK_LATENCY = 3000L;//3s
	
	static class CheckBackgroundRunnable implements Runnable{
		private WeakReference<Context> mContext; 
		@Override
		public void run() {
			if(0 == s_iVisibleCount){
				Context c = (null != mContext)?mContext.get():null;
				notifyAppEnterBackground(c);
			}
		}
		
		public void updateContext(Context context){
			mContext = new WeakReference<Context>(context);
		}
	}
		
	private static CheckBackgroundRunnable s_checkBackgroundRunnable = new CheckBackgroundRunnable();
	
	synchronized public static void increVisibleCount(Context context){
		if(isInMainProcess()){
			s_iVisibleCount++;
			BeseyeUtils.removeRunnable(s_checkBackgroundRunnable);
			if(1 == s_iVisibleCount && FG_CHECK_LATENCY < (System.currentTimeMillis() - s_lLastPauseTs))
				notifyAppEnterForeground(context);
		}else{
			Intent intent = new Intent();
	    	if(null != context && null != intent){
	    		intent.setAction(UBTEventBroadcastReciever.ACTION_UBT_EVENT);
	    		intent.putExtra(UBTEventBroadcastReciever.UBT_EVENT_VISIBLE_ADD, true);
	    		context.sendBroadcast(intent);
	    	}
		}	
	}
	
	synchronized public static void decreVisibleCount(Context context){
		if(isInMainProcess()){
			s_iVisibleCount--;
			s_lLastPauseTs = System.currentTimeMillis();
			if(0 == s_iVisibleCount)
				BeseyeUtils.postRunnable(s_checkBackgroundRunnable, BG_CHECK_LATENCY);
		}else{
			Intent intent = new Intent();
	    	if(null != context && null != intent){
	    		intent.setAction(UBTEventBroadcastReciever.ACTION_UBT_EVENT);
	    		intent.putExtra(UBTEventBroadcastReciever.UBT_EVENT_VISIBLE_DEC, true);
	    		context.sendBroadcast(intent);
	    	}
		}
	}
	
	public static interface BeseyeAppStateChangeListener{
		void onAppEnterForeground();
		void onAppEnterBackground();
		void notifyServiceAppForeground();
		void notifyServiceAppBackground();
	}
	
	private static Set<BeseyeAppStateChangeListener> s_listeners = new HashSet<BeseyeAppStateChangeListener>();
	synchronized public static void registerAppStateChangeListener(BeseyeAppStateChangeListener listener){
		if(null != s_listeners && null != listener)
			s_listeners.add(listener);
	}
	
	synchronized public static void unregisterAppStateChangeListener(BeseyeAppStateChangeListener listener){
		if(null != s_listeners && null != listener){
			s_listeners.remove(listener);
			if(0 == s_listeners.size()){
				sLastBeseyeAppStateChangeListener = new WeakReference<BeseyeAppStateChangeListener>(listener);
			}
		}
	}
	
	static public boolean s_bSeesionBegun = false;
	
	synchronized private static void notifyAppEnterForeground(Context context){
		Log.i(TAG, "notifyAppEnterForeground()");
		
		//Event 27 = >App launch event
		//UBT_Instance.getInstance().SessionBegin(context, 0);
//		if(false == s_bSeesionBegun){
//			UBT_Instance.getInstance().SessionBegin(context, 0);
//			s_bSeesionBegun = true;
//		}
//		
//		UBT_Instance.getInstance().EventBegin(new UBT_Event("App_Enter_Foreground", "",  UBT_Event.UBT_EVENT_DEFAULT), 0);
		//UBT_Instance.getInstance().SessionEnd(context, 0);
		if(null != s_listeners){
			boolean bNotifyService = false;
			for(BeseyeAppStateChangeListener listener: s_listeners){
				listener.onAppEnterForeground();
				if(!bNotifyService){
					listener.notifyServiceAppForeground();
					bNotifyService = true;
				}
			}
		}
	}
	
	synchronized private static void notifyAppEnterBackground(Context context){
		Log.i(TAG, "notifyAppEnterBackground()");
		
//		if(s_bSeesionBegun){
//			if(null != context)
//				UBT_Instance.getInstance().SessionEnd(context, 0);
//			s_bSeesionBegun = false;
//		}
		
		if(null != s_listeners){
			boolean bNotifyService = false;
			if(0 < s_listeners.size()){
				for(BeseyeAppStateChangeListener listener: s_listeners){
					listener.onAppEnterBackground();
					if(!bNotifyService){
						listener.notifyServiceAppBackground();
						bNotifyService = true;
					}
				}
			}else if(null != sLastBeseyeAppStateChangeListener && null != sLastBeseyeAppStateChangeListener.get()){
				sLastBeseyeAppStateChangeListener.get().notifyServiceAppBackground();
				sLastBeseyeAppStateChangeListener = null;
			}
		}
	}
	
	static public class DataUpdateBroadcastReciever extends BroadcastReceiver {
		static final public String ACTION_DATA_UPDATE_EVENT  	= "com.app.beseye.DATA_UPDATE_EVENT";
		static final public String SESSION_DATA_OBJ 	 		= "SESSION_DATA_OBJ";
		static final public String SETTING_DATA_OBJ 			= "SETTING_DATA_OBJ";
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if (ACTION_DATA_UPDATE_EVENT.equals(intent.getAction())) {  
				
				if(intent.hasExtra(SESSION_DATA_OBJ)){
					SessionData sessionData = (SessionData) intent.getExtras().getParcelable(SESSION_DATA_OBJ);
					if(null != sessionData){
	            		SessionMgr.getInstance().setSessionData(sessionData);
	            		if(BeseyeConfig.DEBUG)
	            			Log.i(TAG, "onReceive(), setSessionData invoked, process:"+getProcessName());
					}
				}
//				else if(intent.hasExtra(SETTING_DATA_OBJ)){
//					iKalaSettingsMgr.SettingData settingData = (iKalaSettingsMgr.SettingData) intent.getExtras().getParcelable(SETTING_DATA_OBJ);
//					if(null != settingData){
//						iKalaSettingsMgr.getInstance().setSettingData(settingData);
//						if(BeseyeConfig.DEBUG)
//							Log.i(TAG, "onReceive(), setSettingData invoked, process:"+Application.getProcessName());
//					}
//				}
			}  
		}
	}
	
//	static public boolean checkPairingMode(){
//		File pairingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_pairing");
//		SERVER_MODE mode = SessionMgr.getInstance().getServerMode();
//		COMPUTEX_PAIRING = ((null != pairingFile)&&pairingFile.exists()) || (mode.ordinal() >= SERVER_MODE.MODE_STAGING.ordinal());
//		
//		Log.i(TAG, "checkPairingMode(), COMPUTEX_PAIRING :"+COMPUTEX_PAIRING);
//		return COMPUTEX_PAIRING;
//	}
	
	static public void checkServerMode(){
		SERVER_MODE mode = SessionMgr.getInstance().getServerMode();
		SessionMgr.getInstance().setBEHostUrl(mode);
		if(DEBUG)
			Log.i(TAG, "checkServerMode(), mode :"+mode);
	}
}

