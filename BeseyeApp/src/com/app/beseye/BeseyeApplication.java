package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.httptask.SessionMgr.SessionData;
import com.app.beseye.receiver.UBTEventBroadcastReciever;
import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.DeviceUuidFactory;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.widget.BeseyeMemCache;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

@ReportsCrashes(formKey= HOCKEY_APP_ID,
				logcatArguments = { "-t", "2500", "-v", "long", "BesEye:W", "*:S" },
				mode = ReportingInteractionMode.TOAST,
				forceCloseDialogAfterToast = false,
				resToastText = R.string.crash_toast_text,
				customReportContent = { ReportField.PACKAGE_NAME, ReportField.APP_VERSION_CODE, ReportField.ANDROID_VERSION, ReportField.PHONE_MODEL, ReportField.BUILD, ReportField.TOTAL_MEM_SIZE,
										ReportField.AVAILABLE_MEM_SIZE, ReportField.STACK_TRACE, ReportField.THREAD_DETAILS, ReportField.LOGCAT, ReportField.EVENTSLOG, ReportField.DUMPSYS_MEMINFO})

public class BeseyeApplication extends Application {

	private static Application sApplication;
	/* Used for flurry trackers identification across multi-process 
	   -- Begin */
	final static public String BESEYE_MAIN_PROCESS = "com.app.beseye";
	static private String sCurProcessName = null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		sCurProcessName = BeseyeUtils.getProcessName(this, android.os.Process.myPid());
		if(BeseyeConfig.DEBUG)
			Log.i(TAG, "*****************BeseyeApplication::onCreate(), sCurProcessName = \""+sCurProcessName+"\" "+System.currentTimeMillis());
		
		ACRA.init(this);
		ACRA.getErrorReporter().setReportSender(new HockeySender());
		
		NetworkMgr.createInstance(getApplicationContext());
		CamSettingMgr.createInstance(getApplicationContext());
		//CamSettingMgr.getInstance().addCamID(getApplicationContext(), TMP_CAM_ID);
		
		SessionMgr.createInstance(getApplicationContext());
		checkServerMode();
		
		DeviceUuidFactory.getInstance(getApplicationContext());
		startService(new Intent(this,BeseyeNotificationService.class));
		BeseyeMemCache.init(this);
		
		BeseyeApplication.checkPairingMode();
		
		sApplication = this;
	}
	
	static synchronized public Application getApplication(){
		return sApplication;
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
		return BESEYE_MAIN_PROCESS.equals(sCurProcessName);
	}
	/* Used for flurry trackers identification across multi-process 
	   -- End */

	//private static Handler s_handler = null; 
	private static long s_lLastPauseTs = 0;
	private static volatile int s_iVisibleCount = 0; 
	
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
		if(null != s_listeners && null != listener)
			s_listeners.remove(listener);
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
		
		if(null != s_listeners && 0 < s_listeners.size()){
			boolean bNotifyService = false;
			for(BeseyeAppStateChangeListener listener: s_listeners){
				listener.onAppEnterBackground();
				if(!bNotifyService){
					listener.notifyServiceAppBackground();
					bNotifyService = true;
				}
			}
		}
	}
	
	static public class DataUpdateBroadcastReciever extends BroadcastReceiver {
		static final public String ACTION_DATA_UPDATE_EVENT  	= "com.ikala.app.DATA_UPDATE_EVENT";
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
	
	static public boolean checkPairingMode(){
		File pairingFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_pairing");
		COMPUTEX_PAIRING = (null != pairingFile)&&pairingFile.exists();
		
		Log.i(TAG, "checkPairingMode(), COMPUTEX_PAIRING :"+COMPUTEX_PAIRING);
		return COMPUTEX_PAIRING;
	}
	
	static public void checkServerMode(){
		File modeFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/Download/bes_mode");
		SERVER_MODE mode = SERVER_MODE.MODE_COMPUTEX;
		if(null != modeFile && modeFile.exists()){
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(modeFile)));
				try {
					String strMode = (null != reader)?reader.readLine():null;
					if(null != strMode && 0 < strMode.length())
						mode = SERVER_MODE.translateToMode(Integer.parseInt(strMode));
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		SessionMgr.getInstance().setBEHostUrl(mode);
		Log.i(TAG, "checkServerMode(), mode :"+mode);
	}
}

