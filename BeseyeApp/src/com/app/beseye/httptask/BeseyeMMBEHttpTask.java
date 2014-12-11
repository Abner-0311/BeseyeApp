package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.app.beseye.EventFilterActivity;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeStorageAgent;

public class BeseyeMMBEHttpTask  {
//	static private final String MM_HOST 				= "http://mm01-forext-dev.beseye.com/";
//	static private final String MM_HOST_PRODCTION 	    = "http://54.178.141.19/";
//	static private final String MM_HOST_EVENT 	    	= "http://ec2-54-178-141-19.ap-northeast-1.compute.amazonaws.com/";

	//static private final String MM_HOST_EVENT 	    	= "http://beseye-mm00-forext-stage.no-ip.org/";
	
	static private final String URL_LIVE_STREAM_INFO 		= "live-stream/downstream_info/%s?narrowBW=%s";
	static private final String URL_DVR_STREAM_INFO 		= "dvr/dvr_playlist/%s?startTime=%s&duration=%s&transc=aac";
	
	static private final String URL_GET_EVENT_LIST 	    	= "events/%s?startTime=%s&duration=%s&order=desc&count=%s";
	static private final String URL_GET_EVENT_LIST_FILT 	= "events/%s?startTime=%s&duration=%s&order=desc&count=%s&typeFilter=%s";
	static private final String URL_GET_IMP_EVENT_LIST 		= "imp-events/%s?startTime=%s&duration=%s&order=desc&typeFilter=2";
	static private final String URL_GET_EVENT_LIST_CNT		= "events/count/%s?startTime=%s&duration=%s";
	static private final String URL_GET_EVENT_LIST_CNT_FILT = "events/count/%s?startTime=%s&duration=%s&typeFilter=%s";
	
	static private final String URL_GET_LATEST_THUMB 		= "thumbnail/get_latest?vcamUuid=%s";
	static private final String URL_GET_THUMB_BY_EVENT 		= "thumbnail/get_by_event_list";
	
	static public final int EVENT_FILTER_MOTION = 0x1;
	static public final int EVENT_FILTER_PEOPLE = 0x2;
	static public final int EVENT_FILTER_SOUND 	= 0x4;
	static public final int EVENT_FILTER_FIRE	= 0x8;
	
	static public final long THIRTY_DAYS_IN_MS = 30*24*60*60*1000;
	static public final long SEVEN_DAYS_IN_MS = 7*24*60*60*1000;
	static public final long ONE_DAY_IN_MS = 24*60*60*1000;
	static public final long ONE_HOUR_IN_MS = 60*60*1000;
	
	public static class GetLiveStreamTask extends BeseyeHttpTask{
		private String strVcamId = null;
		private int iTaskSeed = 0;
		public GetLiveStreamTask(OnHttpTaskCallback cb, int seed) {
			super(cb);
			iTaskSeed = seed;
		}
		
		public String getVcamId(){
			return strVcamId;
		}
		
		public int getTaskSeed(){
			return iTaskSeed;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			strVcamId = strParams[0];
			return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_LIVE_STREAM_INFO, strParams[0], strParams[1]));
		}
	}
	
	public static class GetDVRStreamTask extends BeseyeHttpTask{
		public GetDVRStreamTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_DVR_STREAM_INFO, strParams[0], strParams[1], strParams[2]));
		}
	}
	
	public static class GetEventListTask extends BeseyeHttpTask{
		public boolean mbAppend = false;
		public int iTaskSeed = -1;
		
		public GetEventListTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		public GetEventListTask(OnHttpTaskCallback cb, int iSeed) {
			super(cb);
			iTaskSeed = iSeed;
		}
		
		public GetEventListTask(OnHttpTaskCallback cb, boolean bAppend) {
			super(cb);
			mbAppend = bAppend;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			if(strParams.length == 5 && strParams[4].equals(EventFilterActivity.DEF_EVENT_FILTER_VALUE+""))
				return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_GET_EVENT_LIST, strParams[0], strParams[1], strParams[2], strParams[3]));
			else
				return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_GET_EVENT_LIST_FILT, strParams[0], strParams[1], strParams[2], strParams[3], strParams[4]));
		}
	}
	
	public static class GetIMPEventListTask extends BeseyeHttpTask{
		public GetIMPEventListTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_GET_IMP_EVENT_LIST, strParams[0], strParams[1], strParams[2]));
		}
	}
	
	public static class GetEventListCountTask extends BeseyeHttpTask{
		public GetEventListCountTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			if(strParams.length == 4 && strParams[3].equals(EventFilterActivity.DEF_EVENT_FILTER_VALUE+""))
				return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_GET_EVENT_LIST_CNT, strParams[0], strParams[1], strParams[2]));
			else
				return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_GET_EVENT_LIST_CNT_FILT, strParams[0], strParams[1], strParams[2], strParams[3]));
		}
	}
	
	public static class GetLatestThumbnailTask extends BeseyeHttpTask{
		private String strVcamId = null;
		private int iTaskSeed = 0;
		
		public GetLatestThumbnailTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		public GetLatestThumbnailTask(OnHttpTaskCallback cb, int seed) {
			super(cb);
			iTaskSeed = seed;
		}
		
		public String getVcamId(){
			return strVcamId;
		}
		
		public int getTaskSeed(){
			return iTaskSeed;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			strVcamId = strParams[0];
			return super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+String.format(URL_GET_LATEST_THUMB, strParams[0]));
		}
	}
	
	public static class GetThumbnailByEventListTask extends BeseyeHttpTask{
		private static JSONObject sObjThbCache = null;
		private static File cacheFile;
		private String mstrThbKey = null;
		private String mstrThbPath = null;
		private int iTaskSeed = 0;
		
		public GetThumbnailByEventListTask(OnHttpTaskCallback cb, int seed) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
			iTaskSeed = seed;
		}
		
		public String getKey(){
			return mstrThbKey;
		}
		
		public String getPath(){
			return mstrThbPath;
		}
		
		public int getTaskSeed(){
			return iTaskSeed;
		}
		
		static synchronized void checkCache(Context c){
			if(null == sObjThbCache){
				File picDir = BeseyeStorageAgent.getCacheDir(c);
				if(null != picDir){
					picDir.mkdir();
				}
				
				cacheFile = new File(picDir.getAbsolutePath()+"/thbCache");
				if(null != cacheFile){
					if(cacheFile.exists()){
						String strCache = null;
						try {
							BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cacheFile)));
							try {
								strCache = (null != reader)?reader.readLine():null;
							} catch (IOException e) {
								e.printStackTrace();
							}
						} catch (FileNotFoundException e) {
							e.printStackTrace();
							Log.e(TAG, "checkCache(),e:"+e.toString());
						}
						
						if(null != strCache && 0 < strCache.length()){
							try {
								sObjThbCache = new JSONObject(strCache);
							} catch (JSONException e) {
								e.printStackTrace();
								Log.e(TAG, "checkCache(),e:"+e.toString());
							}
						}
					}
				}
				
				if(null == sObjThbCache	)
					sObjThbCache = new JSONObject();
			}
		}
		
		static synchronized String findCache(String strKey){
			String strRet = (null == strKey || 0 == strKey.length())?"":BeseyeJSONUtil.getJSONString(sObjThbCache, strKey, null);
			return strRet;
		}
		
		static synchronized void writeCache(String strKey, String strValue){
			BeseyeJSONUtil.setJSONString(sObjThbCache, strKey, strValue);
			if(null != cacheFile){
				if(cacheFile.exists())
					cacheFile.delete();
				Writer writer = null;
				try {
					writer = new BufferedWriter(new FileWriter(cacheFile));
					if(null != writer){
						writer.write(sObjThbCache.toString());
						writer.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
					Log.e(TAG, "writeCache(),e:"+e.toString());
				}
			}
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[2]);
			if(isCancelled()){
				return null;
			}
			
			checkCache((Context)mOnHttpTaskCallback.get());
			mstrThbKey = strParams[0];
			mstrThbPath = findCache(mstrThbKey);
			List<JSONObject> ret=null;
			if(null != mstrThbPath && mstrThbPath.length() >0){
				Log.e(TAG, "doInBackground(),find cache for key "+mstrThbKey);
				miRetCode = 0;
			}else{
				ret = super.doInBackground(SessionMgr.getInstance().getMMBEHostUrl()+URL_GET_THUMB_BY_EVENT, strParams[1]);
//				if(getRetCode() == 0){
//					JSONArray thumbnailList = BeseyeJSONUtil.getJSONArray(ret.get(0), BeseyeJSONUtil.MM_THUMBNAILS);
//					if(null != thumbnailList){
//						try {
//							JSONArray path = BeseyeJSONUtil.getJSONArray(thumbnailList.getJSONObject(0),BeseyeJSONUtil.MM_THUMBNAIL_PATH);
//							if(null != path){
//								writeCache(mstrThbKey, path.toString());
//							}
//						} catch (JSONException e) {
//							e.printStackTrace();
//							Log.e(TAG, "doInBackground(),e:"+e.toString());
//						}
//					}
//				}
			}
			
			return ret;
		}
	}
}
	
	
