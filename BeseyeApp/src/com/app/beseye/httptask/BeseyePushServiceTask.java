package com.app.beseye.httptask;

import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONObject;

public class BeseyePushServiceTask {
	private static String PUSH_SERVER_URL;
	
	static{
		checkHostAddr();
	}
	
	static public void checkHostAddr(){
//		if(iKalaHttpTask.LINK_PRODUCTION_SERVER  && SessionMgr.getInstance().getIsProductionMode()){
//			PUSH_SERVER_URL = "http://ap.mobile.sbf.ikala.tv/";
//		}else{
//			PUSH_SERVER_URL = "http://192.168.0.61:7000/";
//		}
	}
	
	public static final String FORMAT_GET_PROJ_ID 		= "ps/android/get_project_id";
	public static final String FORMAT_GET_REG_ID 	  	= "ps/android/get_reg_ids/%s";
	public static final String FORMAT_ADD_REG_ID	   	= "ps/android/reg_id_add";
	public static final String FORMAT_DEL_REG_ID	   	= "ps/android/reg_id_delete";
	public static final String FORMAT_UPDATE_REG_ID	   	= "ps/android/reg_id_update";
	
	static public class GetProjectIDTask extends BeseyeHttpTask{
		public GetProjectIDTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {			
			return super.doInBackground(PUSH_SERVER_URL+FORMAT_GET_PROJ_ID);
		}
	}
	
	static public class GetRegisterIDTask extends BeseyeHttpTask{
		public GetRegisterIDTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {			
			return super.doInBackground(PUSH_SERVER_URL+String.format(FORMAT_GET_REG_ID, strParams[0]));
		}
	}
	
	static public class AddRegisterIDTask extends BeseyeHttpTask{
		public AddRegisterIDTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {			
			return super.doInBackground(PUSH_SERVER_URL+FORMAT_ADD_REG_ID, strParams[0]);
		}
	}
	
	static public class DelRegisterIDTask extends BeseyeHttpTask{
		public DelRegisterIDTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {			
			return super.doInBackground(PUSH_SERVER_URL+FORMAT_DEL_REG_ID, strParams[0]);
		}
	}
	
	static public class UpdateRegisterIDTask extends BeseyeHttpTask{
		public UpdateRegisterIDTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {			
			return super.doInBackground(PUSH_SERVER_URL+FORMAT_UPDATE_REG_ID, strParams[0]);
		}
	}
}
