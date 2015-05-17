package com.app.beseye.util;

import com.app.beseye.httptask.SessionMgr.SERVER_MODE;

import android.util.Log;

public class BeseyeConfig {
	static public final String TAG = "BesEye";	
	public static final String HOCKEY_APP_ID = "f90a325fb2364345a826c493888913b8"; 
	
	static public final boolean DEBUG = false;
	static public boolean ALPHA_VER = false;
	static public boolean BETA_VER = true;
	static public boolean PRODUCTION_VER = false;
	static public final SERVER_MODE DEFAULT_SERVER_MODE = SERVER_MODE.MODE_PRODUCTION;
	
	static public final long TIME_TO_CHECK_WIFI_SETUP = 10000L;
	static public final boolean FAKE_AUDIO_RECEIVER = false;
	static public final String TEST_ACC 	= "privatecam@beseye.com";
	static public final String RELAY_AP_SSID = "raylios WiFi";
	static public final String RELAY_AP_PW = "whoisyourdaddy";

	static{
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.DEBUG:"+ BeseyeConfig.DEBUG);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.ALPHA_VER:"+ BeseyeConfig.ALPHA_VER);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.BETA_VER:"+ BeseyeConfig.BETA_VER);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.PRODUCTION_VER:"+ BeseyeConfig.PRODUCTION_VER);
	}
}
