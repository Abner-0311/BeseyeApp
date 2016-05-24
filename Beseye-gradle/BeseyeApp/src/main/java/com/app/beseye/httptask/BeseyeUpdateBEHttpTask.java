package com.app.beseye.httptask;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

public class BeseyeUpdateBEHttpTask  {
	static private final String URL_APP_VERSION_CHECK	= "software/android?pkgName=%s";
	static private final String URL_UPLOAD_OTA_FEEDBACK	= "ota/record_user_feedback";

	public static class GetLatestAndroidAppVersionTask extends BeseyeHttpTask{
		public GetLatestAndroidAppVersionTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getUpdateHostUrl()+String.format(URL_APP_VERSION_CHECK, strParams[0]));
		}
	}
	
	public static class UploadUserOTAFeedbackTask extends BeseyeHttpTask{
		public UploadUserOTAFeedbackTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			return super.doInBackground(SessionMgr.getInstance().getUpdateHostUrl()+URL_UPLOAD_OTA_FEEDBACK, strParams[1]);
		}
	}
}
