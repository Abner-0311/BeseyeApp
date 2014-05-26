package com.app.beseye.httptask;

import java.util.List;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;
import static com.app.beseye.util.BeseyeJSONUtil.*;

public class BeseyeMMBEHttpTask  {
	static private final String MM_HOST 				= "http://54.178.141.19/";
	static private final String MM_HOST_PRODCTION 	    = "http://54.178.141.19/";
	static private final String MM_HOST_EVENT 	    	= "http://ec2-54-178-141-19.ap-northeast-1.compute.amazonaws.com/";
	
	 
	
	static private final String URL_LIVE_STREAM_INFO 	= "live-stream/downstream_info/%s?narrowBW=%s";
	static private final String URL_DVR_STREAM_INFO 	= "dvr/dvr_playlist/%s?startTime=%s&duration=%s&transc=aac";
	
	static private final String URL_GET_EVENT_LIST 	    = "events/%s?startTime=%s&duration=%s&order=desc";
	static private final String URL_GET_EVENT_LIST_CNT 	= "events/count/%s?startTime=%s&duration=%s";
	
	static private final String URL_GET_LATEST_THUMB 	= "thumbnail/get_latest?vcamUuid=%s";
	static private final String URL_GET_THUMB_BY_EVENT 	= "thumbnail/get_by_event_list";
	
	static public final long SEVEN_DAYS_IN_MS = 7*24*60*60*1000;
	
	public static class GetLiveStreamTask extends BeseyeHttpTask{
		private String strVcamId = null;
		public GetLiveStreamTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		public String getVcamId(){
			return strVcamId;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			strVcamId = strParams[0];
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
	
	public static class GetEventListTask extends BeseyeHttpTask{
		public GetEventListTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(MM_HOST_EVENT+String.format(URL_GET_EVENT_LIST, strParams[0], strParams[1], strParams[2]));
		}
	}
	
	public static class GetEventListCountTask extends BeseyeHttpTask{
		public GetEventListCountTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(MM_HOST_EVENT+String.format(URL_GET_EVENT_LIST_CNT, strParams[0], strParams[1], strParams[2]));
		}
	}
	
	public static class GetLatestThumbnailTask extends BeseyeHttpTask{
		private String strVcamId = null;
		public GetLatestThumbnailTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		public String getVcamId(){
			return strVcamId;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			strVcamId = strParams[0];
			return super.doInBackground(MM_HOST_PRODCTION+String.format(URL_GET_LATEST_THUMB, strParams[0]));
		}
	}
	
	public static class GetThumbnailByEventListTask extends BeseyeHttpTask{
		public GetThumbnailByEventListTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(MM_HOST_PRODCTION+URL_GET_THUMB_BY_EVENT, strParams[0]);
		}
	}
}
	
	
