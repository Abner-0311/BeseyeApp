package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.*;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeFeatureConfig;
import com.app.beseye.util.BeseyeUtils;

public class BeseyeAccountTask {
	
	public static final String URL_LOGIN			="user/sign_in";
	public static final String URL_LOGOUT			="user/sign_out";
	public static final String URL_ACCOUNT_CHECK	="user/validate";
	
	public static final String URL_REGISTER			="user/sign_up";
	public static final String URL_PAIRING			="user/vcam/pairing";
	public static final String URL_SET_ATTR			="user/vcam/set_attr";
	public static final String URL_GET_VCAM_LST		="user/vcam/list";
	public static final String URL_QUERY_VCAM		="user/vcam/query";
	public static final String URL_GET_INFO			="user/info";
	public static final String URL_GET_VCAM_INFO	="user/vcam/info";
	public static final String URL_FORGET_PW		="user/password_forget";
	public static final String URL_VPC_QUERY		="user/query";
	public static final String URL_PTOKEN_QUERY		="user/vcam/pairing_status";
	
	public static final String URL_CAM_ATTACH		="vcam/attach";
	public static final String URL_CAM_DEATTACH		="user/vcam/detach";
	public static final String URL_CAM_VALIDATE		="vcam/validate";
	public static final String URL_CAM_VALIDATE_BEE	="vcam/bee_validate";
	
	public static final String URL_CAM_DEATTACH_SALE="vcam/detach_by_sales?UidList=%s&SalesDemoToken=skHGU1TsnbfHIqKWyKFlapJ8BVMV0UeZM1I2Bm41NQBil9JepgOLV4Agf1h84a82";
	
	
	static private void appendDevInfo(JSONObject obj){
		if(null != obj){
			JSONObject objClient = new JSONObject();
			try{
				objClient.put(ACC_CLIENT_UDID, BeseyeUtils.getAndroidUUid());
				objClient.put(ACC_CLIENT_UA, BeseyeUtils.getUserAgent());
				obj.put(ACC_CLIENT, objClient);
			}catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	static public class LoginHttpTask extends BeseyeHttpTask {	 
		public LoginHttpTask(OnHttpTaskCallback cb) {
			super(cb);
			//setDialogResId(0, R.string.dialog_msg_login);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_EMAIL, strParams[0]);
				obj.put(ACC_PASSWORD, strParams[1]);
				obj.put(ACC_REMEM_ME, true);
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_LOGIN, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static public class GetVPCNoHttpTask extends BeseyeHttpTask {	 
		public GetVPCNoHttpTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_EMAIL, strParams[0]);
				if(BeseyeFeatureConfig.VPC_2_TEST){
					obj.put(ACC_VPC_NO, 2);
				}
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getVPCAccountBEHostUrl()+URL_VPC_QUERY, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static public class GetPairingStatusHttpTask extends BeseyeHttpTask {	 
		public GetPairingStatusHttpTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_PAIRING_TYPE, Integer.parseInt(strParams[0]));
				obj.put(ACC_PAIRING_TOKEN, strParams[1]);
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_PTOKEN_QUERY, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	static public class LogoutHttpTask extends BeseyeHttpTask {	 	
		public LogoutHttpTask(OnHttpTaskCallback cb) {
			super(cb);
			setDialogResId(0, R.string.dialog_msg_logiout);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_LOGOUT, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} 
			return null;
		}
	}
	
	static public class CheckAccountTask extends BeseyeHttpTask {	 	
		public CheckAccountTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_ACCOUNT_CHECK, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class RegisterTask extends BeseyeHttpTask {	 	
		public RegisterTask(OnHttpTaskCallback cb) {
			super(cb);
			//setDialogResId(0, R.string.dialog_msg_signup);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_EMAIL, strParams[0]);
				obj.put(ACC_PASSWORD, strParams[1]);
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_REGISTER, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;		
		}
	}
	
	static public class StartCamPairingTask extends BeseyeHttpTask {	 	
		public StartCamPairingTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_PAIRING_TYPE, Integer.parseInt(strParams[0]));
				obj.put(ACC_PAIRING_COUNT, 1);
				obj.put(ACC_PAIRING_AP_MAC, strParams[1]/*.replaceAll(":", "")*/);
				obj.put(ACC_PAIRING_META_DATA, strParams[1]/*.replaceAll(":", "")*/);
				
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_PAIRING, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class SetCamAttrTask extends BeseyeHttpTask {	 	
		public SetCamAttrTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				setVCamIdForPerm(strParams[0]);
				obj.put(ACC_VCAM_ID, strParams[0]);
				JSONObject attrObj = new JSONObject();
				attrObj.put(ACC_NAME, strParams[1]);
				obj.put(ACC_VCAM_ATTR, attrObj);
				
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_SET_ATTR, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class SendForgetPWTask extends BeseyeHttpTask {	 	
		public SendForgetPWTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_EMAIL, strParams[0]);				
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_FORGET_PW, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class GetUserInfoTask extends BeseyeHttpTask {	 	
		public GetUserInfoTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {				
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_GET_INFO, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class GetVCamListTask extends BeseyeHttpTask {
		private String mstrVCamIdCheck= null;
		private JSONObject mMsgObj = null;
		public GetVCamListTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		public GetVCamListTask(OnHttpTaskCallback cb, String strVCamIdCheck, JSONObject msgObj) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			mstrVCamIdCheck = strVCamIdCheck;
			mMsgObj = msgObj;
		}
		
		public String getVCamIdCheck(){
			return mstrVCamIdCheck;
		}
		
		public JSONObject getMsgObj(){
			return mMsgObj;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {		
//				if(0 < strParams.length){
//					setVCamIdForPerm(strParams[0]);
//					
//				}
				obj.put(ACC_REGION, BeseyeUtils.getLocaleString());
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_GET_VCAM_LST, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} 
			catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
//	static public class QueryVCamListTask extends BeseyeHttpTask {	 	
//		public QueryVCamListTask(OnHttpTaskCallback cb) {
//			super(cb);
//			setHttpMethod(HttpPost.METHOD_NAME);
//			//enableHttps();
//		}
// 
//		@Override
//		protected List<JSONObject> doInBackground(String... strParams) {
//			JSONObject obj = new JSONObject();
//			try {		
//				if(0 < strParams.length){
//					setVCamIdForPerm(strParams[0]);
//					obj.put(ACC_VCAM_ID, strParams[0]);
//				}
//				appendDevInfo(obj);
//				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_QUERY_VCAM, obj.toString());
//			} catch (NumberFormatException e) {
//				e.printStackTrace();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//			return null;	
//		}
//	}
	
	static public class GetCamInfoTask extends BeseyeHttpTask {	 	
		private boolean mbNeedToLoadCamSetup = true;
		public GetCamInfoTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
		
		public GetCamInfoTask(OnHttpTaskCallback cb, boolean bNeedToLoadCamSetup) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			mbNeedToLoadCamSetup = bNeedToLoadCamSetup;
		}
		
		public boolean getNeedToLoadCamSetup(){return mbNeedToLoadCamSetup;}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {	
				setVCamIdForPerm(strParams[0]);
				obj.put(ACC_VCAM_ID, strParams[0]);
				appendDevInfo(obj);
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_GET_VCAM_INFO, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class CamDettachTask extends BeseyeHttpTask {	 	
		public CamDettachTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				setVCamIdForPerm(strParams[0]);
				obj.put(ACC_VCAM_ID, strParams[0]);
				appendDevInfo(obj);
				if(BeseyeConfig.DEBUG)
					Log.e(TAG, "obj:"+obj.toString());
				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_CAM_DEATTACH, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class CamDettachByHWIDTask extends BeseyeHttpTask {	 	
		public CamDettachByHWIDTask(OnHttpTaskCallback cb) {
			super(cb);
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+String.format(URL_CAM_DEATTACH_SALE, strParams[0]));
		}
	}
	
	//Test cam behaviors
//	static public class CamAttchTask extends BeseyeHttpTask {	 	
//		public CamAttchTask(OnHttpTaskCallback cb) {
//			super(cb);
//			setHttpMethod(HttpPost.METHOD_NAME);
//			//enableHttps();
//		}
// 
//		@Override
//		protected List<JSONObject> doInBackground(String... strParams) {
//			JSONObject obj = new JSONObject();
//			try {
//				obj.put(ACC_PAIRING_TOKEN, strParams[0]);
//				obj.put(ACC_PAIRING_AP_MAC, strParams[1].replaceAll(":", ""));
//				JSONObject objClient = new JSONObject();
//				objClient.put(ACC_CLIENT_UDID, TMP_VCAM_ID);
//				objClient.put(ACC_CLIENT_UA, "BeseyeCam");
//				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
//				obj.put(ACC_VCAM_CLIENT, objClient);
//				Log.e(TAG, "obj:"+obj.toString());
//				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_CAM_ATTACH, obj.toString());
//			} catch (NumberFormatException e) {
//				e.printStackTrace();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//			return null;	
//		}
//	}
	
//	static public class CamBeeValidateTask extends BeseyeHttpTask {	 	
//		public CamBeeValidateTask(OnHttpTaskCallback cb) {
//			super(cb);
//			setHttpMethod(HttpPost.METHOD_NAME);
//			//enableHttps();
//		}
// 
//		@Override
//		protected List<JSONObject> doInBackground(String... strParams) {
//			JSONObject obj = new JSONObject();
//			try {
//				obj.put(ACC_PAIRING_TOKEN, strParams[0]);
//				obj.put(ACC_PAIRING_AP_MAC, strParams[1].replaceAll(":", ""));
//				JSONObject objClient = new JSONObject();
//				objClient.put(ACC_VCAM_ID, strParams[2]);
//				objClient.put(ACC_CLIENT_UDID, TMP_VCAM_ID);
//				objClient.put(ACC_CLIENT_UA, "BeseyeCam");
//				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
//				obj.put(ACC_VCAM_CLIENT, objClient);
//				Log.e(TAG, "obj:"+obj.toString());
//				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_CAM_VALIDATE_BEE, obj.toString(), strParams[3]);
//			} catch (NumberFormatException e) {
//				e.printStackTrace();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//			return null;	
//		}
//	}
	
//	static public class CamValidateTask extends BeseyeHttpTask {	 	
//		public CamValidateTask(OnHttpTaskCallback cb) {
//			super(cb);
//			setHttpMethod(HttpPost.METHOD_NAME);
//			//enableHttps();
//		}
// 
//		@Override
//		protected List<JSONObject> doInBackground(String... strParams) {
//			JSONObject obj = new JSONObject();
//			try {
//				JSONObject objClient = new JSONObject();
//				objClient.put(ACC_VCAM_ID, strParams[0]);
//				objClient.put(ACC_CLIENT_UDID, TMP_VCAM_ID);
//				objClient.put(ACC_CLIENT_UA, "BeseyeCam");
//				//objClient.put(ACC_CLIENT_LOC, "Taiwan");
//				obj.put(ACC_VCAM_CLIENT, objClient);
//				Log.e(TAG, "obj:"+obj.toString());
//				return super.doInBackground(SessionMgr.getInstance().getAccountBEHostUrl()+URL_CAM_VALIDATE, obj.toString(), strParams[1]);
//			} catch (NumberFormatException e) {
//				e.printStackTrace();
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//			return null;	
//		}
//	}
}
