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
		if(SessionMgr.getInstance().getIsProductionMode()){
			PUSH_SERVER_URL = "";
		}else{
			PUSH_SERVER_URL = BeseyeHttpTask.HOST_ADDR;
		}
	}
	
	public static final String FORMAT_GET_PROJ_ID 		= "notify/android/get_project_info";
	public static final String FORMAT_GET_REG_ID 	  	= "notify/android/get_reg_ids";
	public static final String FORMAT_ADD_REG_ID	   	= "notify/android/add_reg_id";
	public static final String FORMAT_DEL_REG_ID	   	= "notify/android/del_reg_ids";
	public static final String FORMAT_UPDATE_REG_ID	   	= "notify/android/update_reg_id";
	
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
