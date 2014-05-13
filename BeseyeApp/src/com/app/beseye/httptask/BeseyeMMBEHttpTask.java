package com.app.beseye.httptask;

import java.util.List;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;
import static com.app.beseye.util.BeseyeJSONUtil.*;

public class BeseyeMMBEHttpTask  {
	static private final String MM_HOST 				= "http://54.178.141.19:3001/";
	
	static private final String URL_LIVE_STREAM_INFO 	= "live-stream/downstream_info/%s?narrowBW=%s";
	static private final String URL_DVR_STREAM_INFO 	= "dvr/dvr_playlist/%s?startTime=%s&duration=%s&transc=aac";
	
	public static class GetLiveStreamTask extends BeseyeHttpTask{
		public GetLiveStreamTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(MM_HOST+String.format(URL_LIVE_STREAM_INFO, strParams[0], strParams[1]));
		}
	}
	
	public static class GetDVRStreamTask extends BeseyeHttpTask{
		public GetDVRStreamTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(MM_HOST+String.format(URL_DVR_STREAM_INFO, strParams[0], strParams[1], strParams[2]));
		}
	}
}
	
	
