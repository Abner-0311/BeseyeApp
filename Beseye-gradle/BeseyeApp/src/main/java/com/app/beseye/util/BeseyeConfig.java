package com.app.beseye.util;

import android.util.Log;

import com.app.beseye.BuildConfig;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;

public class BeseyeConfig {
	static public final String TAG = "BesEye";
	public static final String HOCKEY_APP_ID = BuildConfig.HOCKEY_APP_ID;

	static public boolean DEBUG = BuildConfig.DEBUG_LVL;
	static public boolean ALPHA_VER = BuildConfig.ALPHA_VER;
	static public boolean BETA_VER = BuildConfig.BETA_VER;
	static public boolean PRODUCTION_VER = BuildConfig.PRODUCTION_VER;
	static public final SERVER_MODE DEFAULT_SERVER_MODE = SERVER_MODE.MODE_PRODUCTION;

	static public final long TIME_TO_CHECK_WIFI_SETUP = 10000L;
	static public final boolean FAKE_AUDIO_RECEIVER = false;
	static public final String TEST_ACC    = "privatecam@beseye.com";
	static public final String RELAY_AP_SSID = "raylios WiFi";
	static public final String RELAY_AP_PW = "whoisyourdaddy";

	static public final boolean SHOW_THUMBNAIL_LOG = true;

	static{
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.DEBUG:"+ BeseyeConfig.DEBUG);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.ALPHA_VER:"+ BeseyeConfig.ALPHA_VER);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.BETA_VER:"+ BeseyeConfig.BETA_VER);
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.PRODUCTION_VER:"+ BeseyeConfig.PRODUCTION_VER);
	}
}