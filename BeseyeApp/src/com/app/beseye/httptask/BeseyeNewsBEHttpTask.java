package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

import android.util.Log;

import com.app.beseye.util.BeseyeJSONUtil;

public class BeseyeNewsBEHttpTask  {
	static private final String URL_NEWS_COUNT  			= "news/get_counts";
	static private final String URL_NEWS_LIST  				= "news/get_list_detail";
	static private final String URL_NEWS_LATEST  			= "news/get_latest?lang=%s";
	
	
	public static class GetNewsCountTask extends BeseyeHttpTask{
		public GetNewsCountTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNewsHostUrl()+URL_NEWS_COUNT);
		}
	}
	
	public static class GetNewsListTask extends BeseyeHttpTask{
		public GetNewsListTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			BeseyeJSONUtil.setJSONInt(obj, BeseyeJSONUtil.NEWS_START_IDX, Integer.parseInt(strParams[0]));
			BeseyeJSONUtil.setJSONInt(obj, BeseyeJSONUtil.NEWS_NUM, Integer.parseInt(strParams[1]));
			BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.NEWS_LANG, strParams[2]);
			Log.e(TAG, "doInBackground(), obj="+obj.toString());
			return super.doInBackground(SessionMgr.getInstance().getNewsHostUrl()+URL_NEWS_LIST, obj.toString());
		}
	}
	
	public static class GetLatestNewsTask extends BeseyeHttpTask{
		public GetLatestNewsTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNewsHostUrl()+String.format(URL_NEWS_LATEST, strParams[0]));
		}
	}
}
