package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.*;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import com.app.beseye.util.BeseyeSharedPreferenceUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class CamSettingMgr {
	static private CamSettingMgr sCamSettingMgr;
	static private final String CAM_SETTINGS_PREF 				= "cam_settings_%s";
	static private final String CAM_POWER 						= "cam_power";// 0:off, 1:on, 2:disconnected 
	static private final String CAM_UPSIDE_DOWN					= "cam_upside_down";//0: normal, 1 :upside_down
	static private final String CAM_NAME 						= "cam_name";
	static private final String CAM_SN 							= "cam_sn";
	static private final String CAM_MAC 						= "cam_mac";
	
	private Map<String, SharedPreferences> mPrefMap;
	private Map<String, CamSettingData> mSettingDataMap;
	private Map<String, WeakReference<ISettingDataUpdateCallback>> mSettingDataUpdateCallbackMap;
	
	public static enum CAM_CONN_STATUS{
		CAM_DISCONNECTED(-1),
		CAM_OFF(0),
		CAM_ON(1);
		
		private int iValue;
		CAM_CONN_STATUS(int iVal){
			iValue = iVal;
		}
		
		public int getValue(){
			return iValue;
		}
		
		public static CAM_CONN_STATUS toCamConnStatus(int iVal){
			switch(iVal){
				case 0:{
					return CAM_CONN_STATUS.CAM_OFF;
				}
				case 1:{
					return CAM_CONN_STATUS.CAM_ON;
				}
				default:
					return CAM_CONN_STATUS.CAM_DISCONNECTED;
			}
		}
	}

	static public void createInstance(Context context){
		if(null == sCamSettingMgr)
			sCamSettingMgr = new CamSettingMgr(context);
	}
	
	static public CamSettingMgr getInstance(){
		return sCamSettingMgr;
	}

	private CamSettingMgr(Context context){
		if(null != context){
			mPrefMap = new HashMap<String, SharedPreferences>();
			mSettingDataMap = new HashMap<String, CamSettingData>();
			mSettingDataUpdateCallbackMap = new HashMap<String, WeakReference<ISettingDataUpdateCallback>>(); 
		}
	}
	
	public void addCamID(Context context, String id){
		if(null != mPrefMap && null != id ){
			SharedPreferences pref = null;
			if(!mPrefMap.containsKey(id)){
				pref = BeseyeSharedPreferenceUtil.getSharedPreferences(context, String.format(CAM_SETTINGS_PREF,id));
				mPrefMap.put(id, pref);
			}
			
			if(null != pref && !mSettingDataMap.containsKey(id)){
				CamSettingData settingData = new CamSettingData();
				if(null != settingData){
					settingData.setCamID(id);
					settingData.setCamPowerState(BeseyeSharedPreferenceUtil.getPrefIntValue(pref, CAM_POWER, CAM_CONN_STATUS.CAM_ON.getValue()));
					settingData.setVideoUpsideDown(BeseyeSharedPreferenceUtil.getPrefIntValue(pref, CAM_UPSIDE_DOWN, 0));
					settingData.setCamName(BeseyeSharedPreferenceUtil.getPrefStringValue(pref, CAM_NAME, TMP_CAM_NAME));
					settingData.setCamSN(BeseyeSharedPreferenceUtil.getPrefStringValue(pref, CAM_SN, TMP_CAM_SN));
					settingData.setCamMAC(BeseyeSharedPreferenceUtil.getPrefStringValue(pref, CAM_MAC, TMP_CAM_MAC));
					mSettingDataMap.put(id, settingData);
				}
			}
		}
	}
	
	public CAM_CONN_STATUS getCamPowerState(String id){
		CamSettingData data = mSettingDataMap.get(id);
		return CAM_CONN_STATUS.toCamConnStatus(null != data?data.miCamPowerState:-1);
	}
	
	public void setCamPowerState(String id, CAM_CONN_STATUS iCamPowerState){
		CamSettingData data = mSettingDataMap.get(id);
		if(null != data){
			data.setCamPowerState(iCamPowerState.getValue());
			BeseyeSharedPreferenceUtil.setPrefIntValue(mPrefMap.get(id), CAM_POWER, data.getCamPowerState());
			notifySettingUpdate(id);
		}
	}
	
	public int getVideoUpsideDown(String id){
		CamSettingData data = mSettingDataMap.get(id);
		return null != data?data.miVideoUpsideDown:-1;
	}
	
	public void setVideoUpsideDown(String id, int iVideoUpsideDown){
		Log.i(TAG, "setVideoUpsideDown(), ------------------------------------------------iVideoUpsideDown="+iVideoUpsideDown);
		CamSettingData data = mSettingDataMap.get(id);
		if(null != data){
			data.setVideoUpsideDown(iVideoUpsideDown);
			BeseyeSharedPreferenceUtil.setPrefIntValue(mPrefMap.get(id), CAM_UPSIDE_DOWN, data.getVideoUpsideDown());
			notifySettingUpdate(id);
		}
	}
	
	public String getCamName(String id){
		CamSettingData data = mSettingDataMap.get(id);
		return null != data?data.mstrCamName:null;
	}
	
	public void setCamName(String id, String name){
		CamSettingData data = mSettingDataMap.get(id);
		if(null != data){
			data.setCamName(name);
			BeseyeSharedPreferenceUtil.setPrefStringValue(mPrefMap.get(id), CAM_NAME, data.getCamName());
			notifySettingUpdate(id);
		}
	}
	
	public String getCamSN(String id){
		CamSettingData data = mSettingDataMap.get(id);
		return null != data?data.mstrCamSN:null;
	}
	
	public void setCamSN(String id, String sn){
		CamSettingData data = mSettingDataMap.get(id);
		if(null != data){
			data.setCamSN(sn);
			BeseyeSharedPreferenceUtil.setPrefStringValue(mPrefMap.get(id), CAM_SN, data.getCamSN());
			notifySettingUpdate(id);
		}
	}
	
	public String getCamMAC(String id){
		CamSettingData data = mSettingDataMap.get(id);
		return null != data?data.mstrCamMAC:null;
	}
	
	public void setCamMAC(String id, String mac){
		CamSettingData data = mSettingDataMap.get(id);
		if(null != data){
			data.setCamMAC(mac);
			BeseyeSharedPreferenceUtil.setPrefStringValue(mPrefMap.get(id), CAM_MAC, data.getCamMAC());
			notifySettingUpdate(id);
		}
	}
	
	public CamSettingData getSettingData(String id){
		return mSettingDataMap.get(id);
	}
	
	public void setSettingData(String id, CamSettingData data){
		mSettingDataMap.put(id, data);
		SharedPreferences pref = mPrefMap.get(id);
		if(null != data && null != pref){
			BeseyeSharedPreferenceUtil.setPrefIntValue(pref, CAM_POWER, data.getCamPowerState());
			BeseyeSharedPreferenceUtil.setPrefIntValue(pref, CAM_UPSIDE_DOWN, data.getVideoUpsideDown());
			BeseyeSharedPreferenceUtil.setPrefStringValue(pref, CAM_NAME, data.getCamName());
			BeseyeSharedPreferenceUtil.setPrefStringValue(pref, CAM_SN, data.getCamSN());
			BeseyeSharedPreferenceUtil.setPrefStringValue(pref, CAM_MAC, data.getCamMAC());
		}
		notifySettingUpdate(id);
	}
	
	public void registerSettingDataUpdateCallback(String id, ISettingDataUpdateCallback cb){
		mSettingDataUpdateCallbackMap.put(id, new WeakReference<ISettingDataUpdateCallback>(cb));
	}
	
	static public interface ISettingDataUpdateCallback{
		public void onSettingDataUpdate(String id, CamSettingData data);
	}
	
	private void notifySettingUpdate(String id){
		if(null != mSettingDataUpdateCallbackMap && null != mSettingDataUpdateCallbackMap.get(id)){
			ISettingDataUpdateCallback cb = mSettingDataUpdateCallbackMap.get(id).get();
			if(null != cb)
				cb.onSettingDataUpdate(id, getSettingData(id));
		}
	}
	
	static public class CamSettingData implements Parcelable{
		private int miCamPowerState;
		private int miVideoUpsideDown;
		private String mstrCamID;
		private String mstrCamName;
		private String mstrCamSN;
		private String mstrCamMAC;
		
		public CamSettingData(){}
		
		public int getCamPowerState(){
			return miCamPowerState;
		}
		
		public void setCamPowerState(int iCamPowerState){
			miCamPowerState = iCamPowerState;
		}
		
		public int getVideoUpsideDown(){
			return miVideoUpsideDown;
		}
		
		public void setVideoUpsideDown(int iVideoUpsideDown){
			miVideoUpsideDown = iVideoUpsideDown;
		}
		
		public String getCamID(){
			return mstrCamID;
		}
		
		private void setCamID(String id){
			mstrCamID = id;
		}
		
		public String getCamName(){
			return mstrCamName;
		}
		
		public void setCamName(String name){
			mstrCamName = name;
		}
		
		public String getCamSN(){
			return mstrCamSN;
		}
		
		public void setCamSN(String sn){
			mstrCamSN = sn;
		}
		
		public String getCamMAC(){
			return mstrCamMAC;
		}
		
		public void setCamMAC(String mac){
			mstrCamMAC = mac;
		}
		
		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
	 
			// We just need to write each field into the
			// parcel. When we read from parcel, they
			// will come back in the same order
			dest.writeInt(miCamPowerState);
			dest.writeInt(miVideoUpsideDown);
			dest.writeString(mstrCamID);
			dest.writeString(mstrCamName);
			dest.writeString(mstrCamSN);
			dest.writeString(mstrCamMAC);
		}
		
		private CamSettingData(Parcel in) {
			miCamPowerState = in.readInt();
			miVideoUpsideDown = in.readInt();
			mstrCamID = in.readString();
			mstrCamName = in.readString();
			mstrCamSN = in.readString();
			mstrCamMAC = in.readString();
	    }
	 
		/**
		 *
		 * Called from the constructor to create this
		 * object from a parcel.
		 *
		 * @param in parcel from which to re-create object
		 */
		private void readFromParcel(Parcel in) {
			miCamPowerState = in.readInt();
			miVideoUpsideDown = in.readInt();
			mstrCamID = in.readString();
			mstrCamName = in.readString();
			mstrCamSN = in.readString();
			mstrCamMAC = in.readString();
		}
		
		public static final Parcelable.Creator<CamSettingData> CREATOR = new Parcelable.Creator<CamSettingData>() {
	        public CamSettingData createFromParcel(Parcel in) {
	            return new CamSettingData(in);
	        }

	        public CamSettingData[] newArray(int size) {
	            return new CamSettingData[size];
	        }
	    };
	}
}
