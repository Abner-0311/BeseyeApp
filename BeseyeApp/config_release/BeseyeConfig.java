package com.app.beseye.util;

import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeConfig;

import android.util.Log;

public class BeseyeConfig {
	static public final String TAG = "BesEye";
	
	public static final String HOCKEY_APP_ID = "3f241ef5b7fb32623b970d296f90fcb4"; 
	
	static public final boolean DEBUG = false;
	static public boolean ALPHA_VER = false;
	static public boolean BETA_VER = false;
	static public boolean PRODUCTION_VER = true;
	static public final SERVER_MODE DEFAULT_SERVER_MODE = SERVER_MODE.MODE_PRODUCTION;
	
	static public final long TIME_TO_CHECK_WIFI_SETUP = 10000L;
	static public final boolean FAKE_AUDIO_RECEIVER = false;
	static public final String TEST_ACC 	= "privatecam@beseye.com";
	static public final String RELAY_AP_SSID = "raylios WiFi";
	static public final String RELAY_AP_PW = "whoisyourdaddy";

	static public final boolean SHOW_THUMBNAIL_LOG = false;
	static public final boolean PRODUCTION_FAKE_APP_CHECK = false;


	static{
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.DEBUG:"+ BeseyeConfig.DEBUG);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.ALPHA_VER:"+ BeseyeConfig.ALPHA_VER);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.BETA_VER:"+ BeseyeConfig.BETA_VER);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.PRODUCTION_VER:"+ BeseyeConfig.PRODUCTION_VER);
	}
}
