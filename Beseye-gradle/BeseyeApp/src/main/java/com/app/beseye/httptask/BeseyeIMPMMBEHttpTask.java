package com.app.beseye.httptask;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;


public class BeseyeIMPMMBEHttpTask  {
	static private final String URL_GET_REFINE_LST 			= "human/refine/list?num=%s";
	static private final String URL_GET_REFINE_IMG 			= "human/refine/img?file=%s";
	static private final String URL_POST_REFINE_LABEL 		= "human/refine/set_label";
	static private final String URL_POST_RESET 				= "human/refine/reset";
	static private final String URL_GET_PROGRESS			= "human/refine/progress";
	
	public static class GetHumanDetectRefineListTask extends BeseyeHttpTask{
		public GetHumanDetectRefineListTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			return super.doInBackground(SessionMgr.getInstance().getIMPMMBEHostUrl()+String.format(URL_GET_REFINE_LST,strParams[1]));
		}
	}
	
	public static String getRefineImgPath(String strFileName){
		return SessionMgr.getInstance().getIMPMMBEHostUrl()+String.format(URL_GET_REFINE_IMG,strFileName);
	}
	
	public static class SetHumanDetectRefineLabelTask extends BeseyeHttpTask{
		public SetHumanDetectRefineLabelTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			return super.doInBackground(SessionMgr.getInstance().getIMPMMBEHostUrl()+URL_POST_REFINE_LABEL,strParams[1]);
		}
	}
	
	public static class SetHumanDetectResetTask extends BeseyeHttpTask{
		public SetHumanDetectResetTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			return super.doInBackground(SessionMgr.getInstance().getIMPMMBEHostUrl()+URL_POST_RESET);
		}
	}
	
	public static class GetHumanDetectProgressTask extends BeseyeHttpTask{
		public GetHumanDetectProgressTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			setVCamIdForPerm(strParams[0]);
			return super.doInBackground(SessionMgr.getInstance().getIMPMMBEHostUrl()+URL_GET_PROGRESS);
		}
	}
}
	
	
