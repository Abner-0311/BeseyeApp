package com.app.beseye.httptask;

import java.util.List;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.util.BeseyeUtils;

import static com.app.beseye.util.BeseyeJSONUtil.*;

public class BeseyeNotificationBEHttpTask  {
	static private final String URL_AUDIO_WS_SERVER  = "websocket_audio/get_avaliable_server";
	static private final String URL_REQUEST_CAM_CONN  = "websocket_audio/request_cam_connect";
	
	
	public static class GetAudioWSServerTask extends BeseyeHttpTask{
		public GetAudioWSServerTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(HOST_ADDR+URL_AUDIO_WS_SERVER);
		}
	}
	
	public static class RequestAudioWSOnCamTask extends BeseyeHttpTask{
		public RequestAudioWSOnCamTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(CAM_UUID, strParams[0]);
				obj.put(DEV_ID, BeseyeUtils.getAndroidUUid());
				obj.put(SES_TOKEN, SessionMgr.getInstance().getAuthToken());
				return super.doInBackground(HOST_ADDR+URL_REQUEST_CAM_CONN, obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
