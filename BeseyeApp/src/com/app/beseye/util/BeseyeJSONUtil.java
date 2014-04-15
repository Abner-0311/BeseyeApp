package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.*;

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
	public static final String OBJ_CNT 					= "ObjCount";
	public static final String OBJ_LST 					= "Objs";
	public static final String OBJ_LK_KEY 				= "LinkKey";
	public static final String OBJ_NXT_PAGE 			= "NextPage";
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
	public static final String SPEAKER_STATUS 			= "SpeakerStatus";
	public static final String SPEAKER_VOLUME 			= "SpeakerVolume";
	public static final String MIC_STATUS 				= "MicStatus";
	public static final String MIC_GAIN 				= "MicGain";
	public static final String IRCUT_STATUS 			= "IRStatus";
	
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
	public static final String WIFI_SSIDLST_SGL 		= "signal";
	public static final String WIFI_SSIDLST_SEC 		= "sec";
	public static final String WIFI_SSIDLST_USED 		= "CurrentUsed";
	
	//Account BE service
	public static final String ACC_EMAIL 				= "email";
	public static final String ACC_PASSWORD 			= "password";
	public static final String ACC_CLIENT 				= "client";
	public static final String ACC_CLIENT_UDID 			= "client_dev_udid";
	public static final String ACC_CLIENT_UA 			= "client_user_agent";
	public static final String ACC_CLIENT_LOC 			= "client_location";
	public static final String ACC_CLIENT_IP 			= "client_ip";
	
	public static final String ACC_USER 				= "user";
	public static final String ACC_ID 					= "id";
	public static final String ACC_NAME 				= "name";
	public static final String ACC_LOC 					= "location";
	
	public static final String ACC_SES 					= "session";
	public static final String ACC_SES_TOKEN 			= "session_token";
	public static final String ACC_DATA 				= "data";
	public static final String ACC_CREATE 				= "created_at";
	public static final String ACC_EXPIRE 				= "expired_at";

	public static final String ACC_REMEM_ME 			= "remember_me";
	
	public static final String ACC_USER_ID 				= "user_id";
	public static final String ACC_VCAM_ID 				= "vcam_id";
	
	//For Push service
	public static final String PS_PORJ_ID 				= "GCMProjectID";
	public static final String PS_REG_ID 				= "RegisterID";
	public static final String PS_REG_IDS 				= "RegIDs";
	public static final String PS_REG_ID_OLD 			= "OrigRegisterID";
	public static final String PS_REG_ID_NEW 			= "NewRegisterID";
	public static final String PS_NOTIFY_TYPE 			= "notify_type";
	public static final String PS_WORK_TYPE 			= "work_type";
	public static final String PS_NOTIFY_INFO 			= "Info";
	
	//Helper functions
	static public JSONObject newJSONObject(String json){
		JSONObject objRet;
		try{
			objRet = new JSONObject(json);
		} catch (JSONException e) {
			Log.w(TAG, "newJSONObject(), can't new JSONObject by "+json);
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
				Log.w(TAG, "getJSONObject(), can't get value by "+strKey);
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
				Log.w(TAG, "getJSONArray(), can't get value by "+strKey);
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
					strRet = Html.fromHtml(strRet).toString();
					strRet = strRet.replaceAll("<br/>", "\n");
				}else{
					strRet = strDefault;
				}
				
				//workaround for backend
				if("null".equalsIgnoreCase(strRet)){
					strRet = strDefault;
				}
			} catch (JSONException e) {
				Log.w(TAG, "getJSONString(), can't get value by "+strKey);
				strRet = strDefault;
			}
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
				Log.w(TAG, "getJSONInt(), can't get value by "+strKey);
			}
		}
		return iRet;
	}
	
	static public long getJSONLong(JSONObject obj, String strKey){
		long lRet = 0;
		if(null != obj){
			try {
				lRet = obj.getLong(strKey);
			} catch (JSONException e) {
				Log.w(TAG, "getJSONLong(), can't get value by "+strKey);
				lRet = -1;
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
				Log.w(TAG, "getJSONDouble(), can't get value by "+strKey);
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
				Log.w(TAG, "getJSONBoolean(), can't get value by "+strKey);
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
				try {
					JSONArray copy = new JSONArray(arr.toString());
					if(null != copy){
						arr.put(0, obj);
						int iCount = copy.length();
						for(int i = 0 ;i< iCount;i++){
							arr.put(i+1, copy.getJSONObject(i));
						}
						dest.put(OBJ_CNT, BeseyeJSONUtil.getJSONInt(dest, OBJ_CNT)+1);
						bRet = true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
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
	
//	static public JSONObject getMsgReceiver(JSONArray members){
//		JSONObject rec = null;
//		if(null != members){
//			String ownerId = SessionMgr.getInstance().getMdid();
//			int iCount = members.length();
//			for(int iIndex = 0; iIndex < iCount;iIndex++){
//				JSONObject member = members.optJSONObject(iIndex);
//				if(null != member && !ownerId.equals(BeseyeJSONUtil.getJSONString(member, USER_ID))){
//					rec = member;
//					break;
//				}
//			}
//		}
//		return rec;
//	}
//	
//	static public String parseOwnerInfo(JSONObject obj){
//		JSONObject ret = new JSONObject();
//		if(null != obj){
//			try {
//				ret.put(USER_MDID, getJSONString(obj, USER_MDID));
//				ret.put(USER_ACCOUNT, getJSONString(obj, USER_ACCOUNT));
//				ret.put(USER_VIP_LVL, getJSONString(obj, USER_VIP_LVL));
//				ret.put(USER_IS_VIP, getJSONString(obj, USER_IS_VIP));
//				ret.put(USER_EXPIRE, getJSONString(obj, USER_EXPIRE));
//				ret.put(USER_OPENACCOUNT, getJSONString(obj, USER_OPENACCOUNT));
//				ret.put(USER_AGREE, getJSONString(obj, USER_AGREE));
//				ret.put(USER_OPEN_ID, getJSONString(obj, USER_OPEN_ID));
//				ret.put(USER_S_TOT, getJSONString(obj, USER_S_TOT));
//				ret.put(USER_S_REC, getJSONString(obj, USER_S_REC));
//				ret.put(USER_S_ALBM, getJSONString(obj, USER_S_ALBM));
//				ret.put(PAY_AUTO_MONTH, getJSONString(obj, PAY_AUTO_MONTH));
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//		return ret.toString();
//	}
}
