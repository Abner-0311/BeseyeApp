package com.app.beseye.util;

import android.util.Log;

public class BeseyeConfig {
	static public final String TAG = "BesEye";
	
	public static final String HOCKEY_APP_ID = "1caf00210eb940171eb56a1904a204e8"; 
	static public final long TIME_TO_CHECK_WIFI_SETUP = 10000L;
	
	//relay wifi AP info

//	static public final boolean ASSIGN_ST_PATH = false;
	static public final boolean FAKE_AUDIO_RECEIVER = false;
	
	//static public final boolean REDDOT_DEMO = false;//deprecated
	//static public final boolean COMPUTEX_DEMO = false;
	//static public boolean COMPUTEX_PAIRING = false;
	//static public boolean COMPUTEX_P2P = false;
	
	static public final boolean DEBUG = true;
	static public boolean ALPHA_VER = false;
	static public boolean PRODUCTION_VER = false;
	
	static public final String TEST_ACC 	= "privatecam@beseye.com";
	static public final String RELAY_AP_SSID = "raylios WiFi";
	static public final String RELAY_AP_PW = "whoisyourdaddy";
	
//	static public final String TMP_MM_VCAM_ID = "1001";
//	
//	
//	static public final String TMP_VCAM_ID = "SN0000011";
//	static public final String TMP_CAM_ID = "BeseyeCam001";
//	static public final String TMP_CAM_NAME = "BesEye Cam";
//	static public final String TMP_CAM_NAME_S = "rtmp://54.238.191.39:1935/live-edge-transc/_definst_/{o}54.250.149.50/live-origin-record/_definst_/1001_aac";
//	static public final String TMP_CAM_SN = "BE00000001PN";
//	static public final String TMP_CAM_MAC = "93:be:22:fa:10:88";
	
//	static public final String[] DEMO_STREAM_PATH = {//"rtsp://54.250.149.50:554/live-origin/_definst_/mystream7_aac",
//													 //"rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/mystream_0.mp4",
//													 //"rtmp://54.250.149.50/vods3/_definst_/mp4:amazons3/wowza2.s3.tokyo/liveorigin/sample.mp4",
//													 //"rtsp://admin:password@192.168.2.2/h264_2",
//													 //"rtmp://54.238.191.39:1935/live-edge/mystream5_aac"/*,
//													 //"rtmp://54.250.149.50/live-origin/mystream3_aac",
//													 //"rtmp://54.238.191.39:1935/live-edge-transc/_definst_/{o}54.250.149.50/live-origin-record/_definst_/1001_aac",
//													 "rtmp://54.238.191.39:1935/live-edge/_definst_/{o}54.250.149.50/live-origin-record/_definst_/1001_aac",
//													 "rtsp://admin:password@192.168.2.85/h264",
//													 "rtmp://192.168.2.145/proxypublish/stream1 live=1",
//													 "rtmp://54.250.149.50/livetest/test_crtmpserver",
//													 "rtsp://admin:password@192.168.12.184/h264_2"//00:0C:43:30:50:D0
//													 ,"rtsp://admin:password@192.168.12.186/h264_2"//00:0C:43:30:50:28
//													 ,"rtsp://54.250.149.50:554/live-origin/_definst_/mystream7_aac"
//													 //,"rtsp://admin:password@192.168.6.204/h264"													
//													 //"rtmp://54.238.191.39:1935/live-edge/_definst_/{o}54.250.149.50/live-origin/_definst_/mystream3_aac",
//													 //"rtsp://admin:password@192.168.2.2/h264_2"
//													 };
//	
//	static public final String[] REDDOT_STREAM_PATH = {
//													 //"rtsp://admin:password@192.168.12.184/h264"
////														"rtsp://admin:password@192.168.12.180/h264_2"
////														,"rtsp://admin:password@192.168.12.182/h264_2"
//////														,"rtsp://admin:password@192.168.12.184/h264_2"
////														,"rtsp://admin:password@192.168.12.186/h264_2"
////														 "rtsp://admin:password@192.168.12.182/h264_2"//00:0C:43:30:50:08
////														,"rtsp://admin:password@192.168.12.182/h264_2"//00:0C:43:30:50:B8
//													 //"rtsp://admin:password@192.168.12.184/h264_2"//00:0C:43:30:50:D0
//													"rtsp://admin:password@192.168.2.169/h264"//00:0C:43:30:50:28
//													 };
	
//	static public final List<String> STREAM_PATH_LIST = new ArrayList<String>();
//	static public final Map<String, String> REDDOT_STREAM_PATH_MAP = new HashMap<String, String>();
	static{
		Log.i(TAG, "BeseyeConfig init, BeseyeConfig.DEBUG:"+ BeseyeConfig.DEBUG);
		
//		if(REDDOT_DEMO){
//			for(int i = 1; i<= REDDOT_STREAM_PATH.length;i++){
//				STREAM_PATH_LIST.add(REDDOT_STREAM_PATH[i-1]);
//				REDDOT_STREAM_PATH_MAP.put(REDDOT_STREAM_PATH[i-1], "No. "+i);
//			}
//		}else{
//			if(ASSIGN_ST_PATH)
//				STREAM_PATH_LIST.add(CamSettingMgr.getInstance().getCamName(TMP_CAM_ID));
//			
//			for(int i = 0; i< DEMO_STREAM_PATH.length;i++){
//				STREAM_PATH_LIST.add(DEMO_STREAM_PATH[i]);
//			}
//		}
	}
	
	static public int CUR_STREAMING_PATH_IDX = 0;
}
