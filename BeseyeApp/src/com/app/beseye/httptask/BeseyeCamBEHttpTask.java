package com.app.beseye.httptask;

import java.util.List;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;

import static com.app.beseye.util.BeseyeJSONUtil.*;

public class BeseyeCamBEHttpTask  {
	static private final String URL_CAM_SETUP 			= "cam/%s/setup";
	static private final String URL_CAM_STATUS 			= "cam/%s/camonoff";
	static private final String URL_CAM_LED_STATUS 		= "cam/%s/ledonoff";
	static private final String URL_CAM_SPK_STATUS 		= "cam/%s/speaker/onoff";
	static private final String URL_CAM_SPK_VOLUME 		= "cam/%s/speaker/volume";
	static private final String URL_CAM_MIC_STATUS 		= "cam/%s/mic/onoff";
	static private final String URL_CAM_MIC_GAIN 		= "cam/%s/mic/gain";
	static private final String URL_CAM_VIDEO_RES		= "cam/%s/videores";
	
	
	static private final String URL_CAM_IRCUT_STATUS 	= "cam/%s/ir/setup";
	static private final String URL_CAM_IMG_SETTING 	= "cam/%s/image/setting";
	
	static private final String URL_CAM_RESTART 		= "cam/%s/restart";
	static private final String URL_CAM_RECONN_MM 		= "cam/%s/reconnectmm";
	
	static private final String URL_CAM_WIFI_CONFIG 	= "cam/%s/wifi/config";
	static private final String URL_CAM_WIFI_SSIDLST 	= "cam/%s/wifi/ssidlist";
	
	static private final String URL_CAM_SYS_INFO 		= "cam/%s/sysinfo";
	static private final String URL_CAM_DATETIME 		= "cam/%s/datetime";
	static private final String URL_CAM_TIMEZONE 		= "cam/%s/time_zone";
	
	
	static private final String URL_CAM_DATETIME_CONFIG = "cam/%s/datetime/config";
	static private final String URL_CAM_DATETIME_NTP_CONFIG = "cam/%s/datetime/ntpconfig";
	
	static private final String URL_CAM_SW_UPDATE 		= "cam/%s/software/update";
	static private final String URL_CAM_SW_UPDATE_STATUS= "cam/%s/software/update_status";
	
	static private final String URL_CAM_SCHEDULE_STATUS = "cam/%s/schedule/onoff";
	static private final String URL_CAM_SCHEDULE  		= "cam/%s/schedule";
	static private final String URL_CAM_SCHEDULE_IDX  	= "cam/%s/schedule/%s";
	
	static private final String URL_NOTIFY_SETTING  	= "cam/%s/notify_setting";
	
	public static class GetCamSetupTask extends BeseyeHttpTask{
		private String strVcamId = null;
		private int iTaskSeed = 0;
		
		public GetCamSetupTask(OnHttpTaskCallback cb) {
			this(cb,-1);
		}
		
		public GetCamSetupTask(OnHttpTaskCallback cb, int seed) {
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
			strVcamId = strParams[0];
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SETUP, strParams[0]));
		}
	}
	
	public static class GetCamStatusTask extends BeseyeHttpTask{
		public GetCamStatusTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_STATUS, strParams[0]));
		}
	}
	
	public static class SetCamStatusTask extends BeseyeHttpTask{
		private String strVcamId = null;
		public SetCamStatusTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		public String getVcamId(){
			return strVcamId;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				strVcamId = strParams[0];
				obj.put(CAM_STATUS, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_STATUS, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetLEDStatusTask extends BeseyeHttpTask{
		public GetLEDStatusTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_LED_STATUS, strParams[0]));
		}
	}
	
	public static class SetLEDStatusTask extends BeseyeHttpTask{
		public SetLEDStatusTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(LED_STATUS, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_LED_STATUS, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetSpeakerStatusTask extends BeseyeHttpTask{
		public GetSpeakerStatusTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SPK_STATUS, strParams[0]));
		}
	}
	
	public static class SetSpeakerStatusTask extends BeseyeHttpTask{
		public SetSpeakerStatusTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(SPEAKER_STATUS, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SPK_STATUS, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetSpeakerVolumeTask extends BeseyeHttpTask{
		public GetSpeakerVolumeTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SPK_VOLUME, strParams[0]));
		}
	}
	
	public static class SetSpeakerVolumeTask extends BeseyeHttpTask{
		public SetSpeakerVolumeTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(SPEAKER_VOLUME, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SPK_VOLUME, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetMicStatusTask extends BeseyeHttpTask{
		public GetMicStatusTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_MIC_STATUS, strParams[0]));
		}
	}
	
	public static class SetMicStatusTask extends BeseyeHttpTask{
		public SetMicStatusTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(MIC_STATUS, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_MIC_STATUS, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetMicGainTask extends BeseyeHttpTask{
		public GetMicGainTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_MIC_GAIN, strParams[0]));
		}
	}
	
	public static class SetMicGainTask extends BeseyeHttpTask{
		public SetMicGainTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(MIC_GAIN, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_MIC_GAIN, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	
	
	public static class GetVideoResTask extends BeseyeHttpTask{
		public GetVideoResTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_VIDEO_RES, strParams[0]));
		}
	}
	
	public static class SetVideoResTask extends BeseyeHttpTask{
		public SetVideoResTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(VIDEO_RES, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_VIDEO_RES, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetIRCutStatusTask extends BeseyeHttpTask{
		public GetIRCutStatusTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_IRCUT_STATUS, strParams[0]));
		}
	}
	
	public static class SetIRCutStatusTask extends BeseyeHttpTask{
		public SetIRCutStatusTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(IRCUT_STATUS, Integer.parseInt(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_IRCUT_STATUS, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetImageSettingTask extends BeseyeHttpTask{
		public GetImageSettingTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_IMG_SETTING, strParams[0]));
		}
	}
	
	public static class SetImageSettingTask extends BeseyeHttpTask{
		public SetImageSettingTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(IMG_FLIP, Integer.parseInt(strParams[1]));
				obj.put(IMG_MIRROR, Integer.parseInt(strParams[2]));
				obj.put(IMG_BRIGHTNESS, Integer.parseInt(strParams[3]));
				obj.put(IMG_CONTRAST, Integer.parseInt(strParams[4]));
				obj.put(IMG_HUE, Integer.parseInt(strParams[5]));
				obj.put(IMG_SATURATION, Integer.parseInt(strParams[6]));
				obj.put(IMG_SHARPNESS, Integer.parseInt(strParams[7]));
				obj.put(IMG_FPS, Integer.parseInt(strParams[8]));
				
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_IMG_SETTING, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class RestartCamTask extends BeseyeHttpTask{
		public RestartCamTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_RESTART, strParams[0]));
		}
	}
	
	public static class ReconnectMMTask extends BeseyeHttpTask{
		public ReconnectMMTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_RECONN_MM, strParams[0]));
		}
	}
	
	public static class GetWiFiConfigTask extends BeseyeHttpTask{
		public GetWiFiConfigTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_WIFI_CONFIG, strParams[0]));
		}
	}
	
	public static class SetWiFiConfigTask extends BeseyeHttpTask{
		public SetWiFiConfigTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(WIFI_SSID, strParams[1]);
				obj.put(WIFI_KEY, strParams[2]);
				obj.put(WIFI_SECU, Integer.parseInt(strParams[3]));
				
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_WIFI_CONFIG, strParams[0]), obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetWiFiSSIDListTask extends BeseyeHttpTask{
		public GetWiFiSSIDListTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_WIFI_SSIDLST, strParams[0]));
		}
	}
	
	public static class GetDateTimeTask extends BeseyeHttpTask{
		public GetDateTimeTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_DATETIME, strParams[0]));
		}
	}
	
	public static class GetSystemInfoTask extends BeseyeHttpTask{
		public GetSystemInfoTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SYS_INFO, strParams[0]));
		}
	}
	
	public static class UpdateCamSWTask extends BeseyeHttpTask{
		private String strVcamId = null;
		public UpdateCamSWTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		public String getVcamId(){
			return strVcamId;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			strVcamId = strParams[0];
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SW_UPDATE, strParams[0]));
		}
	}
	
	public static class GetCamUpdateStatusTask extends BeseyeHttpTask{
		private String strVcamId = null;
		public GetCamUpdateStatusTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		public String getVcamId(){
			return strVcamId;
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			strVcamId = strParams[0];
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SW_UPDATE_STATUS, strParams[0]));
		}
	}
	
	public static class GetCamTimezoneTask extends BeseyeHttpTask{
		public GetCamTimezoneTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_TIMEZONE, strParams[0]));
		}
	}
	
	public static class SetCamTimezoneTask extends BeseyeHttpTask{
		public SetCamTimezoneTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(CAM_TZ, strParams[1]);
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_TIMEZONE, strParams[0]), obj.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class GetScheduleStatusTask extends BeseyeHttpTask{
		public GetScheduleStatusTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SCHEDULE_STATUS, strParams[0]));
		}
	}
	
	public static class SetScheduleStatusTask extends BeseyeHttpTask{
		public SetScheduleStatusTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put(SCHEDULE_STATUS, Boolean.parseBoolean(strParams[1]));
				return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SCHEDULE_STATUS, strParams[0]), obj.toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class AddScheduleTask extends BeseyeHttpTask{
		public AddScheduleTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SCHEDULE, strParams[0]), strParams[1]);
		}
	}
	
	public static class UpdateScheduleTask extends BeseyeHttpTask{
		public UpdateScheduleTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SCHEDULE_IDX, strParams[0], strParams[1]), strParams[2]);
		}
	}
	
	public static class DeleteScheduleTask extends BeseyeHttpTask{
		public DeleteScheduleTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpDelete.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_CAM_SCHEDULE_IDX, strParams[0], strParams[1]));
		}
	}
	
	public static class GetNotifySettingTask extends BeseyeHttpTask{
		public GetNotifySettingTask(OnHttpTaskCallback cb) {
			super(cb);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_NOTIFY_SETTING, strParams[0]));
		}
	}
	
	public static class SetNotifySettingTask extends BeseyeHttpTask{
		public SetNotifySettingTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPut.METHOD_NAME);
		}
		
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			return super.doInBackground(SessionMgr.getInstance().getNSBEHostUrl()+String.format(URL_NOTIFY_SETTING, strParams[0]), strParams[1]);
		}
	}
}
