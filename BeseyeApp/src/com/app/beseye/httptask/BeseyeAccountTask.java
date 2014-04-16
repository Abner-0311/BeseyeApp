package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.util.BeseyeConfig.*;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.app.beseye.R;
import com.app.beseye.util.DeviceUuidFactory;

public class BeseyeAccountTask {
	
	public static final String URL_LOGIN			="user/sign_in";
	public static final String URL_LOGOUT			="user/sign_out";
	public static final String URL_ACCOUNT_CHECK	="user/validate_session";
	
	public static final String URL_REGISTER			="user/sign_up";
	public static final String URL_PAIRING			="user/pairing";
	
	public static final String URL_CAM_ATTACH		="cam/attach";
	
	static public class LoginHttpTask extends BeseyeHttpTask {	 
		public LoginHttpTask(OnHttpTaskCallback cb) {
			super(cb);
			setDialogResId(0, R.string.dialog_msg_login);
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
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, "Android_"+DeviceUuidFactory.getDeviceUuid());
				objClient.put(ACC_CLIENT_UA, "Android");
				objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_LOGIN, obj.toString());
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
				obj.put(ACC_SES_TOKEN, strParams[0]);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_LOGOUT, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
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
				obj.put(ACC_SES_TOKEN, strParams[0]);
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, "Android_"+DeviceUuidFactory.getDeviceUuid());
				objClient.put(ACC_CLIENT_UA, "Android");
				objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_ACCOUNT_CHECK, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class RegisterTask extends BeseyeHttpTask {	 	
		public RegisterTask(OnHttpTaskCallback cb) {
			super(cb);
			setDialogResId(0, R.string.dialog_msg_signup);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_EMAIL, strParams[0]);
				obj.put(ACC_PASSWORD, strParams[1]);
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, "Android_"+DeviceUuidFactory.getDeviceUuid());
				objClient.put(ACC_CLIENT_UA, "Android");
				objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_REGISTER, obj.toString());
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
				obj.put(ACC_SES_TOKEN, strParams[0]);
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, "Android_"+DeviceUuidFactory.getDeviceUuid());
				objClient.put(ACC_CLIENT_UA, "Android");
				objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_PAIRING, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
	
	static public class CamAttchTask extends BeseyeHttpTask {	 	
		public CamAttchTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			//enableHttps();
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(ACC_USER_ID, Integer.parseInt(strParams[0]));
				JSONObject objClient = new JSONObject();
				objClient.put(ACC_CLIENT_UDID, "SN0000003");
				objClient.put(ACC_CLIENT_UA, "BeseyeCam");
				objClient.put(ACC_CLIENT_LOC, "Taiwan");
				obj.put(ACC_CLIENT, objClient);
				Log.e(TAG, "obj:"+obj.toString());
				return super.doInBackground(SessionMgr.getInstance().getHostUrl()+URL_CAM_ATTACH, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;	
		}
	}
}
