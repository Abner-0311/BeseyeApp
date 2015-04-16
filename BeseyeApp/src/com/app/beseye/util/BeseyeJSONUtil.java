package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.text.Html;
import android.util.Log;

public class BeseyeJSONUtil {
	//Http & Session
	public static final String HOST 					= "Host"; 
	public static final String SESSION_MDID 			= "Mdid"; 
	
	//Generic
	public static final String RET_CODE 				= "ReturnCode"; 
	public static final String RET_CODE_HEX 			= "HexReturnCode"; 
	public static final String RET_CODE_FIN_TS 			= "FinTimestamp"; 
	
	public static final String RET_CODE_CAMBE 			= "Code"; 
	public static final String OBJ_CNT 					= "ObjCount";
	public static final String OBJ_LST 					= "Objs";
	public static final String OBJ_LK_KEY 				= "LinkKey";
	public static final String OBJ_NXT_PAGE 			= "NextPage";
	
	public static final String OBJ_DATA 				= "Data";
	public static final String OBJ_UNREAD 				= "UnreadObjCount";
	public static final String OBJ_UNREAD_SOCIAL 		= "UnreadObjCountSocial";
	public static final String OBJ_UNREAD_CHORUS 		= "UnreadObjCountChorus";
	public static final String OBJ_UNREAD_MESSAGE 		= "UnreadObjCountMessage";
	public static final String OBJ_UNREAD_NOTIFY 		= "UnreadObjCountNotification";
	public static final String OBJ_RECORDS 				= "Records";
	public static final String OBJ_START_TIME 			= "StartTime";
	public static final String OBJ_END_TIME 			= "EndTime";
	
	public static final String HITS_NUM 				= "Hits";
	public static final String LIKES_NUM 				= "Likes";
	public static final String FANS_NUM 				= "Fans";
	public static final String WORKS_NUM 				= "Works";
	
	public static final String FROM 					= "From";
	public static final String USER_ID 					= "UserID";
	public static final String USER_NAME 				= "UserName";
	public static final String USER_ICON 				= "UserIcon";
	
	public static final String CREATE_TIME 				= "CreateTime";
	public static final String UPDATE_TIME 				= "UpdateTime";
	public static final String INDEX 					= "Index";
	public static final String UNREAD 					= "Unread";
	
	public final static String REF_LABEL 				= "REF_LABEL";
	public final static String REF_INTENT 				= "REF_INTENT";
	
	public static final String OBJ_TIMESTAMP 			= "Timestamp";
	
	//Cam BE service
	public static final String CAM_STATUS 				= "CamStatus";
	public static final String LED_STATUS 				= "LEDStatus";
	public static final String STATUS 					= "Status";
	public static final String TYPE 					= "Type";
	
	public static final String SPEAKER_STATUS 			= "SpeakerStatus";
	public static final String SPEAKER_VOLUME 			= "SpeakerVolume";
	public static final String MIC_STATUS 				= "MicStatus";
	public static final String MIC_GAIN 				= "MicGainPercent";
	public static final String IRCUT_STATUS 			= "IRStatus";
	public static final String VIDEO_RES 				= "VideoResolution";
	public static final String MAC_ADDR 				= "CamMacAddr";
	
	
	public static final String NOTIFY_OBJ 				= "Notify";
	public static final String NOTIFY_PEOPLE 			= "People";
	public static final String NOTIFY_MOTION 			= "Motion";
	public static final String NOTIFY_FIRE 				= "Fire";
	public static final String NOTIFY_SOUND 			= "Sound";
	public static final String NOTIFY_OFFLINE 			= "Offline";
	
	public static final String IMG_OBJ 					= "Image";
	public static final String IMG_FLIP 				= "Flip";
	public static final String IMG_MIRROR 				= "Mirror";
	public static final String IMG_BRIGHTNESS 			= "Brightness";
	public static final String IMG_CONTRAST 			= "Contrast";
	public static final String IMG_HUE 					= "Hue";
	public static final String IMG_SATURATION 			= "Saturation";
	public static final String IMG_SHARPNESS 			= "Sharpness";
	public static final String IMG_FPS 					= "FPS";
	
	public static final String WIFI_SSID 				= "SSID";
	public static final String WIFI_KEY 				= "Key";
	public static final String WIFI_SECU 				= "Security";
	
	public static final String WIFI_SSIDLST 			= "SSIDList";
	public static final String WIFI_SSIDLST_ID 			= "ssid";
	public static final String WIFI_SSIDLST_BSSID 		= "bssid";
	public static final String WIFI_SSIDLST_SGL 		= "signal";
	public static final String WIFI_SSIDLST_SEC 		= "sec";
	public static final String WIFI_SSIDLST_USED 		= "CurrentUsed";
	public static final String WIFI_SSIDLST_USED_BSSID  = "CurrentUsedBSSID";
	
	public static final String CAM_SN 					= "SerialNumber";
	public static final String CAM_MAC_ADDR 			= "MACAddr";
	public static final String CAM_SOFTWARE 			= "Firmware";
	public static final String CAM_TZ 					= "TimeZone";
	public static final String CAM_UPSIDE_DOWN 			= "UpsideDown";
	
	
	public static final String CAM_STATUS_LST 			= "StatusList";
	public static final String CAM_WS_STATUS 			= "wsStatus";//1 => on-line, 0=> off-line
	
	
	public static final String SCHED_OBJ 				= "Schedule";
	public static final String SCHED_OBJ_IDX 			= "ScheduleIdx";
	public static final String SCHED_STATUS 			= "Status";
	public static final String SCHED_LIST 				= "List";
	public static final String SCHEDULE_STATUS 			= "ScheduleStatus";
	public static final String SCHED_FROM 				= "StartTime";
	public static final String SCHED_TO 				= "EndTime";
	public static final String SCHED_DAYS 				= "WorkDay";
	public static final String SCHED_PERIOD 			= "Peroid";
	public static final String SCHED_ENABLE 			= "Enable";
	
	public static final String LOCATION_OBJ 			= "Locale";
	public static final String LOCATION_LAT 			= "Latitude";
	public static final String LOCATION_LONG 			= "Longitude";
	
	
	public static final String UPDATE_PROGRESS 			= "progress";
	public static final String UPDATE_FINAL_STAUS 		= "finalStatus";
	public static final String UPDATE_DETAIL_STAUS 		= "detailStatus";
	public static final String UPDATE_CAN_GO 			= "update";
	public static final String UPDATE_VERSION_LATEST 	= "latestVersion";
		
	//WS BE service
	public static final String CAM_UUID 				= "camUuid";
	public static final String DEV_ID 					= "deviceId";
	public static final String SES_TOKEN 				= "SessionToken";
	
	//News BE service
	public static final String NEWS_START_IDX 			= "start";
	public static final String NEWS_NUM 				= "num";
	public static final String NEWS_LANG 				= "lang";
	
	public static final String NEWS_COUNT 				= "newsCounts";
	
	public static final String NEWS_LIST 				= "newsLists";
	public static final String NEWS_ID 					= "id";
	public static final String NEWS_TITLE 				= "title";
	
	public static final String NEWS_CONTENT 			= "content";
	public static final String NEWS_DESC 				= "description";
	public static final String NEWS_OTHER 				= "otherInfo";
	public static final String NEWS_URL 				= "newsUrl";
	public static final String NEWS_FW_VER 				= "firmwareVer";
	
	public static final String NEWS_ABSTRACT 			= "abstract";
	public static final String NEWS_REL_TIME 			= "releaseTime";
	
	public static final String NEWS_NEWS_ID 			= "newsId";
	public static final String NEWS_DETAIL 				= "detail";
	
	public static final String NEWS_TYPE 				= "type";
	public static final int NEWS_TYPE_ANNOUNCE 			= 0;
	public static final int NEWS_TYPE_CAM_UPDATE 		= 1;
	public static final int NEWS_TYPE_UNKNOWN 			= -1;
	
	//Account BE service
	public static final String ACC_EMAIL 				= "Email";
	public static final String ACC_PASSWORD 			= "Password";
	public static final String ACC_CLIENT 				= "UserClient";
	public static final String ACC_CLIENT_UDID 			= "ClientDevUdid";
	public static final String ACC_CLIENT_UA 			= "ClientUserAgent";
	public static final String ACC_CLIENT_LOC 			= "ClientLocation";
	public static final String ACC_CLIENT_IP 			= "ClientIp";
	public static final String ACC_VPC_NO 				= "Number";
	
	public static final String ACC_USER 				= "User";
	public static final String ACC_ID 					= "Uid";
	public static final String ACC_NAME 				= "Name";
	public static final String ACC_LOC 					= "Location";
	public static final String ACC_ACTIVATED 			= "IsActivated";
	public static final String ACC_VCAM 				= "Vcam";
	public static final String ACC_BESEYE_ACCOUNT 		= "IsBeseyeAccount";
	
	public static final String ACC_SES 					= "UserSession";
	public static final String ACC_SES_TOKEN 			= "SessionToken";
	public static final String ACC_DATA 				= "Data";
	public static final String ACC_CREATE 				= "ExpiredAt";
	public static final String ACC_EXPIRE 				= "CreatedAt";

	public static final String ACC_REMEM_ME 			= "RememberMe";
	
	public static final String ACC_PAIRING_TYPE 		= "PairingType";
	public static final int ACC_PAIRING_TYPE_ATTACH 	= 0;
	public static final int ACC_PAIRING_TYPE_VALIDATE 	= 1;
	
	public static final String ACC_PAIRING_COUNT 		= "PairingCnt";
	public static final String ACC_PAIRING_AP_MAC 		= "PairingWifiRouterMac";
	public static final String ACC_PAIRING_META_DATA 	= "PairingMetaData";
	public static final String ACC_PAIRING_TOKEN 		= "PairingToken";
	
	
	public static final String ACC_SES_DEV 				= "DevSession";
	public static final String ACC_VCAM_CLIENT 			= "VcamClient";
	public static final String ACC_USER_ID 				= "user_id";
	public static final String ACC_VCAM_ID 				= "VcamUid";
	public static final String ACC_VCAM_ATTR 			= "VcamAttr";
	public static final String ACC_VCAM_ATTACHED 		= "IsAttached";
	public static final String ACC_VCAM_CONN_STATE 		= "VcamConnState";
	public static final String ACC_VCAM_THUMB 			= "VcamThumbnail";
	
	public static final String ACC_VCAM_CNT 			= "VcamsCnt";
	public static final String ACC_VCAM_LST 			= "Vcams";
	public static final String ACC_DEMO_VCAM_CNT 		= "DemoVcamsCnt";
	public static final String ACC_DEMO_VCAM_LST 		= "DemoVcams";
	public static final String ACC_PRIVATE_VCAM_CNT 	= "PrivateVcamsCnt";
	public static final String ACC_PRIVATE_VCAM_LST 	= "PrivateVcams";
	public static final String ACC_VCAM_PLAN 			= "Plan";
	public static final String ACC_VCAM_HW_ID 			= "PhyCamHwSn";
	
	public static final String ACC_SUBSC_CNT 			= "SubscribersCnt";
	public static final String ACC_SUBSC_LST 			= "Subscribers";
	
	public static final String ACC_SUBSC_ID 			= "Id";
	public static final String ACC_SUBSC_TYPE 			= "Type";
	public static final String ACC_SUBSC_NAME 			= "UserName";
	public static final String ACC_SUBSC_EMAIL 			= "UserEmail";
	public static final String ACC_SUBSC_ADMIN 			= "IsAdmin";
	
	//MM BE service
	public static final String MM_SERVER 				= "server";
	public static final String MM_STREAM 				= "stream";
	public static final String MM_START_TIME 			= "startTime";
	public static final String MM_END_TIME 				= "endTime";
	public static final String MM_DURATION 				= "duration";
	public static final String MM_PLAYLIST 				= "playList";
	
	public static final String MM_OBJ_LST 				= "objs";
	public static final String MM_OBJ_CNT 				= "objCount";
	public static final String MM_CNT 					= "count";
	
	public static final String MM_TYPE_IDS				= "typeIds";
	public static final int MM_TYPE_ID_MOTION			= 1;
	public static final int MM_TYPE_ID_FACE				= 2;
	
	public static final String MM_FACE_IDS 				= "faceIds";
	
	static public class FACE_LIST{
		public int miId;
		public String mstrName;
		public String miPhototId;
		
		public FACE_LIST(int miId, String mstrName, String miPhototId) {
			super();
			this.miId = miId;
			this.mstrName = mstrName;
			this.miPhototId = miPhototId;
		}
	}
	
	public static enum CAM_CONN_STATUS{
		CAM_INIT(-2),
		CAM_DISCONNECTED(-1),
		CAM_OFF(0),
		CAM_ON(1);
		
		private int iValue;
		CAM_CONN_STATUS(int iVal){
			iValue = iVal;
		}
		
		public int getValue(){
			return iValue;
		}
		
		public static CAM_CONN_STATUS toCamConnStatus(int iVal){
			switch(iVal){
				case 0:{
					return CAM_CONN_STATUS.CAM_OFF;
				}
				case 1:{
					return CAM_CONN_STATUS.CAM_ON;
				}
				case -1:{
					return CAM_CONN_STATUS.CAM_DISCONNECTED;
				}
				default:
					return CAM_CONN_STATUS.CAM_INIT;
			}
		}
	}

	static public FACE_LIST findFacebyId(int id){
		if(0 <= id && id<faceList.length)
			return faceList[id];
		else
			return null;
	}
	
	static final public FACE_LIST faceList[] =
		{
		    new FACE_LIST(1, "Abner", "Abner 1.jpg"),

		    new FACE_LIST(2, "Amos", "Amos.jpg"),

		    new FACE_LIST(3, "Carlos", "Carlos.jpg"),

		    new FACE_LIST(4, "Chris", "Chris_head.jpg"),

		    new FACE_LIST(5, "Claudia", "Claudia_20110817.jpg"),

		    new FACE_LIST(6, "Doris", "Doris.jpg"),

		    new FACE_LIST(7, "Yolux", "DSC04568.JPG"),

		    new FACE_LIST(8, "Giben", "Giben.Lin.png"),

		    new FACE_LIST(9, "Olive", "Olive.jpg"),

		    new FACE_LIST(10, "Peggy", "peggy.jpg"),

		    new FACE_LIST(11, "Selena", "selena.JPG"),

		    new FACE_LIST(12, "Shaq", "Shaq.jpg"),

		    new FACE_LIST(13, "Yehudi", "Yehudi.JPG"),

		    new FACE_LIST(14, "Zara", "zara.jpg"),

		    new FACE_LIST(15, "Cuthbert", "Cuthbert1.jpg"),   //-4

		    new FACE_LIST(16, "Cuthbert", "Cuthbert2.JPG"),   //-6

		    new FACE_LIST(17, "Peggy", "IMG_9856.JPG"),

		    new FACE_LIST(18, "Meg", "IMG_9868.JPG"),

		    new FACE_LIST(19, "Olive", "IMG_9872.JPG"),

		    new FACE_LIST(20, "Giben", "IMG_9873.JPG"),

		    new FACE_LIST(21, "Zara", "IMG_9875.JPG"),

		    new FACE_LIST(22, "Selena", "IMG_9876.JPG"),

		    new FACE_LIST(23, "Yolux", "IMG_9880.JPG"),

		    new FACE_LIST(24, "Chris", "IMG_9881.JPG"),

		    new FACE_LIST(25, "Yehudi", "IMG_9882.JPG"),

		    new FACE_LIST(26, "Carlos", "IMG_9883.JPG"),

		    new FACE_LIST(27, "Abner", "IMG_9884.JPG"),

		    new FACE_LIST(28, "Shaq", "IMG_9885.JPG"),

		    new FACE_LIST(29, "Jobbie", "IMG_9886.JPG"),

		    new FACE_LIST(30, "Karen", "IMG_9887.JPG"),

		    new FACE_LIST(31, "Doris", "IMG_9890.JPG")

		};
	
	public static final String MM_IS_LIVE 				= "isLive";
	
	public static final String MM_THUMBNAIL 			= "thumbnail";
	public static final String MM_THUMBNAIL_PATH 		= "path";
	public static final String MM_THUMBNAIL_PATH_CACHE 	= "pathCache";
	public static final String MM_VCAM_UUID 			= "vcamUuid";	
	public static final String MM_SIZE 					= "size";
	public static final String MM_MAX_NUM 				= "maxNumber";
	public static final String MM_EVT_LST 				= "eventList";
	public static final String MM_THUMBNAILS 			= "thumbnails";
	public static final String MM_TIMESTAMP 			= "timestamp";
	
	public static final String MM_THUMBNAIL_REQ 		= "thumbnailReq";
	public static final String MM_THUMBNAIL_EXPIRE 		= "thumbnailExpire";
	
	
	//For Push service
	public static final String PS_PORJ_ID 				= "projectId";
	public static final String PS_PORJ_NUM 				= "projectNumber";
	
	public static final String PS_REG_ID 				= "regId";
	public static final String PS_REG_DEV_UUID 			= "devUuid";
	public static final String PS_REG_DEV_NAME 			= "devName";
	
	public static final String PS_REG_IDS 				= "regIds";
	public static final String PS_REG_ID_OLD 			= "from";
	public static final String PS_REG_ID_NEW 			= "to";
	
	public static final String PS_REGULAR_DATA 			= "rData";
	public static final String PS_MSG 					= "msg";
	public static final String PS_CAM_UID 				= "vcUuid";
	public static final String PS_NCODE 				= "nCode";
	public static final String PS_TS 					= "ts";
	
	public static final String PS_CUSTOM_DATA 			= "cData";
	
	public static final String PS_WIFI_CONFIG_REPORT 	= "configReport";
	public static final String PS_PAIR_TOKEN 			= "pairToken";
	public static final String PS_USER_UUID 			= "userUuid";
	public static final String PS_CAM_NAME 				= "camName";
	public static final String PS_EVT_TS 				= "evt_ts";
	
	
	//Helper functions
	static public JSONObject newJSONObject(String json){
		JSONObject objRet;
		try{
			objRet = new JSONObject(json);
		} catch (JSONException e) {
			if(BeseyeConfig.DEBUG)
				Log.d(TAG, "newJSONObject(), can't new JSONObject by "+json);
			objRet = null;
		}
		return objRet;
	}
	
	static public JSONArray newgetJSONArray(String json){
		JSONArray objRet;
		try{
			objRet = new JSONArray(json);
		} catch (JSONException e) {
			Log.d(TAG, "newgetJSONArray(), can't new JSONArray by "+json);
			objRet = null;
		}
		return objRet;
	}
	
	static public JSONObject getJSONObject(JSONObject obj, String strKey){
		JSONObject objRet = null;
		if(null != obj){
			try {
				objRet = obj.getJSONObject(strKey);
			} catch (JSONException e) {
				if(BeseyeConfig.DEBUG)
					Log.d(TAG, "getJSONObject(), can't get value by "+strKey);
				objRet = null;
			}
		}
		return objRet;
	}
	
	static public JSONArray getJSONArray(JSONObject obj, String strKey){
		JSONArray objRet = null;
		if(null != obj){
			try {
				objRet = obj.getJSONArray(strKey);
			} catch (JSONException e) {
				if(BeseyeConfig.DEBUG)
					Log.d(TAG, "getJSONArray(), can't get value by "+strKey);
				objRet = null;
			}
		}
		return objRet;
	}
	
	static public String getJSONString(JSONObject obj, String strKey){
		return getJSONString(obj, strKey, "");
	}
	
	static public String getJSONString(JSONObject obj, String strKey, String strDefault){
		String strRet = null;
		if(null != obj){
			try {
				strRet = obj.getString(strKey);
				if(null != strRet && 0 < strRet.length()){
					//strRet = Html.fromHtml(strRet).toString();
					//strRet = strRet.replaceAll("<br/>", "\n");
				}else{
					strRet = strDefault;
				}
				
//				//workaround for backend
//				if("null".equalsIgnoreCase(strRet)){
//					strRet = strDefault;
//				}
			} catch (JSONException e) {
				if(BeseyeConfig.DEBUG)
					Log.d(TAG, "getJSONString(), can't get value by "+strKey);
				strRet = strDefault;
			}
		}else{
			strRet = strDefault;
		}
		return strRet;
	}
	
	static public int getJSONInt(JSONObject obj, String strKey){
		return getJSONInt(obj, strKey, 0);
	}
	
	static public int getJSONInt(JSONObject obj, String strKey, int iDefaultValue){
		int iRet = iDefaultValue;
		if(null != obj){
			try {
				iRet = obj.getInt(strKey);
			} catch (JSONException e) {
				if(BeseyeConfig.DEBUG)
					Log.d(TAG, "getJSONInt(), can't get value by "+strKey);
			}
		}
		return iRet;
	}
	
	static public long getJSONLong(JSONObject obj, String strKey){
		return getJSONLong(obj, strKey, 0);
	}
	
	static public long getJSONLong(JSONObject obj, String strKey, long lDef){
		long lRet = 0;
		if(null != obj){
			try {
				lRet = obj.getLong(strKey);
			} catch (JSONException e) {
				if(BeseyeConfig.DEBUG)
					Log.d(TAG, "getJSONLong(), can't get value by "+strKey);
				lRet = lDef;
			}
		}
		return lRet;
	}
	
	static public double getJSONDouble(JSONObject obj, String strKey){
		double dRet = 0.0d;
		if(null != obj){
			try {
				dRet = obj.getDouble(strKey);
			} catch (JSONException e) {
				if(BeseyeConfig.DEBUG)
					Log.d(TAG, "getJSONDouble(), can't get value by "+strKey);
				dRet = Double.NaN;
			}
		}
		return dRet;
	}
	
	static public boolean getJSONBoolean(JSONObject obj, String strKey){
		return getJSONBoolean(obj, strKey, false);
	}
	
	static public boolean getJSONBoolean(JSONObject obj, String strKey, boolean bDefault){
		boolean bRet = bDefault;
		if(null != obj){
			try {
				bRet = obj.getBoolean(strKey);
			} catch (JSONException e) {
				if(BeseyeConfig.DEBUG)
					Log.d(TAG, "getJSONBoolean(), can't get value by "+strKey);
			}
		}
		return bRet;
	}
	
	static public List<String> getListFromJSONArray(JSONArray arr){
		List<String> arrRet = new ArrayList<String>();
		if(null != arr){
			int iLen = arr.length();
			for(int iIndex = 0; iIndex < iLen; iIndex++){
				try {
					String value = arr.getString(iIndex);
					if(null != value && 0 < value.trim().length())
						arrRet.add(value);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return arrRet;
	}
	
	static public JSONObject appendMoreObj(JSONObject src, JSONObject dest){
		if(null != src){
			if(null != dest){
				try {
					dest.put(OBJ_NXT_PAGE, BeseyeJSONUtil.getJSONBoolean(src, OBJ_NXT_PAGE));
					dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(src, OBJ_CNT)+BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT));
					JSONArray arrDest = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
					JSONArray arrSrc = BeseyeJSONUtil.getJSONArray(src, OBJ_LST);
					if(null != arrSrc && null != arrDest){
						int iCount = arrSrc.length();
						for(int i = 0; i < iCount;i++){
							arrDest.put(arrSrc.get(i));
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}else
				dest = src;
		}
		
		return dest;
	}
	
	static public String getLastLinkKey(JSONObject obj){
		String strRet = null;
		if(null != obj){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(obj, OBJ_LST);
			if(null != arr && 0 < arr.length()){
				try {
					strRet = BeseyeJSONUtil.getJSONString(arr.getJSONObject(arr.length()-1), OBJ_LK_KEY);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return strRet;
	}
	
	static public boolean appendObjToArrayBegin(JSONObject dest, JSONObject obj){
		boolean bRet = false;
		if(null != dest && null != obj){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				if(appendObjToArrayBegin(arr, obj)){
					try {
						dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)+1);
						bRet = true;
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		return bRet;
	}
	
	static public boolean appendObjToArrayBegin(JSONArray arr, JSONObject obj){
		boolean bRet = false;
		if(null != arr){
			try {
				JSONArray copy = new JSONArray(arr.toString());
				if(null != copy){
					arr.put(0, obj);
					int iCount = copy.length();
					for(int i = 0 ;i< iCount;i++){
						arr.put(i+1, copy.getJSONObject(i));
					}
					bRet = true;
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return bRet;
	}
	
	static public boolean appendObjToArrayEnd(JSONObject dest, JSONObject obj){
		boolean bRet = false;
		if(null != dest && null != obj){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				arr.put(obj);
				try {
					dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)+1);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				bRet = true;
			}
		}
		return bRet;
	}
	
	static public boolean removeObjFromArrayBegin(JSONObject dest){
		boolean bRet = false;
		if(null != dest ){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				try {
					JSONArray copy = new JSONArray();
					if(null != copy){
						int iCount = arr.length();
						if(0 < iCount){
							for(int i = 1 ;i< iCount;i++){
								copy.put(i-1, arr.getJSONObject(i));
							}
							dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)-1);
							dest.put(OBJ_LST, copy);
						}
						
						bRet = true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				bRet = true;
			}
		}
		return bRet;
	}
	
	static public boolean removeObjFromArrayEnd(JSONObject dest){
		boolean bRet = false;
		if(null != dest ){
			JSONArray arr = BeseyeJSONUtil.getJSONArray(dest, OBJ_LST);
			if(null != arr){
				try {
					JSONArray copy = new JSONArray();
					if(null != copy){
						int iCount = arr.length();
						if(0 < iCount){
							for(int i = 0 ;i< iCount-1;i++){
								copy.put(i, arr.getJSONObject(i));
							}
							dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)-1);
							dest.put(OBJ_LST, copy);
						}
						bRet = true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
				bRet = true;
			}
		}
		return bRet;
	}
	
	static public boolean setJSONInt(JSONObject obj, String strKey, int iVal){
		boolean bRet = false;
		try {
			if(null != obj && null != strKey){
				obj.put(strKey, iVal);
				bRet = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bRet;
	}
	
	static public boolean setJSONLong(JSONObject obj, String strKey, long lVal){
		boolean bRet = false;
		try {
			if(null != obj && null != strKey){
				obj.put(strKey, lVal);
				bRet = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bRet;
	}
	
	static public boolean setJSONDouble(JSONObject obj, String strKey, Double lVal){
		boolean bRet = false;
		try {
			if(null != obj && null != strKey){
				obj.put(strKey, lVal);
				bRet = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bRet;
	}
	
	static public boolean setJSONBoolean(JSONObject obj, String strKey, boolean bVal){
		boolean bRet = false;
		try {
			if(null != obj && null != strKey){
				obj.put(strKey, bVal);
				bRet = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bRet;
	}
	
	static public boolean setJSONString(JSONObject obj, String strKey, String strVal){
		boolean bRet = false;
		try {
			if(null != obj &&  null != strKey && null != strVal){
				obj.put(strKey, strVal);
				bRet = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bRet;
	}
	
	static public boolean setJSONObject(JSONObject obj, String strKey, JSONObject objVal){
		boolean bRet = false;
		try {
			if(null != obj &&  null != strKey && null != objVal){
				obj.put(strKey, objVal);
				bRet = true;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return bRet;
	}
	
	static public CAM_CONN_STATUS getVCamConnStatus(JSONObject objCam){
		CAM_CONN_STATUS cRet = CAM_CONN_STATUS.CAM_INIT;
		if(null != objCam){
			JSONObject dataObj = getJSONObject(objCam, ACC_DATA);
			if(null != dataObj){
				JSONObject statusLstObj = getJSONObject(dataObj, CAM_STATUS_LST);
				
				int iWsStatus = getJSONInt((null != statusLstObj)?statusLstObj:dataObj, CAM_WS_STATUS);
				if(1 == iWsStatus){
					int iCamStatus = getJSONInt(dataObj, BeseyeJSONUtil.CAM_STATUS, -1);
					if(1==iCamStatus){
						cRet = CAM_CONN_STATUS.CAM_ON;
					}else if(0==iCamStatus){
						cRet = CAM_CONN_STATUS.CAM_OFF;
					}
				}else{
					cRet = CAM_CONN_STATUS.CAM_DISCONNECTED;
				}
				
			}else{
				if(BeseyeConfig.DEBUG)
					Log.e(TAG, "getVCamConnStatus(), can't find dataObj");
			}
		}
		//Log.e(TAG, "getVCamConnStatus(), cRet:"+cRet.toString());
		return cRet;
	}
	
	static public void setVCamConnStatus(JSONObject objCam, CAM_CONN_STATUS status){
		if(null != objCam){
			JSONObject dataObj = getJSONObject(objCam, ACC_DATA);
			if(null != dataObj){
				JSONObject statusLstObj = getJSONObject(dataObj, CAM_STATUS_LST);
				if(status == CAM_CONN_STATUS.CAM_DISCONNECTED){
					setJSONInt((null != statusLstObj)?statusLstObj:dataObj, CAM_WS_STATUS, 0);
				}else{
					setJSONInt((null != statusLstObj)?statusLstObj:dataObj, CAM_WS_STATUS, 1);
					setJSONInt(dataObj, CAM_STATUS, (status.equals(CAM_CONN_STATUS.CAM_ON))?1:0);
				}
			}else{
				Log.e(TAG, "setVCamConnStatus(), can't find dataObj");
			}
		}
	}
	
	static public boolean isCamPowerOn(JSONObject objCam){
		return (BeseyeJSONUtil.getVCamConnStatus(objCam) == BeseyeJSONUtil.CAM_CONN_STATUS.CAM_ON);
	}
	
	static public boolean isCamPowerOff(JSONObject objCam){
		return (BeseyeJSONUtil.getVCamConnStatus(objCam) == BeseyeJSONUtil.CAM_CONN_STATUS.CAM_OFF);
	}
	
	static public boolean isCamPowerDisconnected(JSONObject objCam){
		return (BeseyeJSONUtil.getVCamConnStatus(objCam) == BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED);
	}
}
