package com.app.beseye.httptask;

import java.util.List;
import org.json.JSONObject;

public class BeseyeUpdateBEHttpTask  {
	static private final String URL_APP_VERSION_CHECK	= "software/android?pkgName=%s";

	public static class GetLatestAndroidAppVersionTask extends BeseyeHttpTask{
		public GetLatestAndroidAppVersionTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getUpdateHostUrl()+String.format(URL_APP_VERSION_CHECK, strParams[0]));
		}
	}
}
