package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeConfig.*;

import java.lang.ref.WeakReference;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.util.BeseyeJSONUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import static com.app.beseye.util.BeseyeSharedPreferenceUtil.*;

//SessionMgr is responsible for storing back-end URL, token, user Userid in storage/memory
public class SessionMgr {
	static private final String SESSION_PREF 				= "beseye_ses";
	static private final String SESSION_TOKEN 				= "beseye_token";
	static private final String SESSION_DOMAIN 				= "beseye_domain";
	static private final String SESSION_USERID				= "beseye_userid";
	static private final String SESSION_ACCOUNT				= "beseye_account";
	static private final String SESSION_ACC_CERTIFICATED	= "beseye_certificated";
	static private final String SESSION_OWNER_INFO			= "beseye_owner_data";
	
	static private final String SESSION_UPDATE_TS			= "beseye_cam_update_ts";
	static private final String SESSION_UPDATE_CAMS			= "beseye_cam_update_list";
	
	static private final String SESSION_PRODUCTION_MODE	    = "beseye_server_mode";
	
	static private SessionMgr sSessionMgr;
	
	static public void createInstance(Context context){
		if(null == sSessionMgr)
			sSessionMgr = new SessionMgr(context);
	}
	
	static public SessionMgr getInstance(){
		return sSessionMgr;
	}
	
	private SharedPreferences mPref, mSecuredPref;
	//private String mStrHostUrl, mStrStorageHostUrl;
	private SessionData mSessionData;
	//private ChannelInfo mOwnerChannelInfo;
	
	private SessionMgr(Context context){
		if(null != context){
			mPref = getSharedPreferences(context, SESSION_PREF);
			mSecuredPref = getSecuredSharedPreferences(context, SESSION_PREF);
			mSessionData = new SessionData();
			if(null != mSessionData){
				mSessionData.setUserid(getPrefStringValue(mPref, SESSION_USERID));
				mSessionData.setAccount(getPrefStringValue(mPref, SESSION_ACCOUNT));
				mSessionData.setDomain(getPrefStringValue(mPref, SESSION_DOMAIN));
				mSessionData.setAuthToken(getPrefStringValue(mPref, SESSION_TOKEN));
				mSessionData.setIsCertificated(0 <getPrefIntValue(mPref, SESSION_ACC_CERTIFICATED));
				mSessionData.setIsProductionMode(0 <getPrefIntValue(mPref, SESSION_PRODUCTION_MODE, 0));
				mSessionData.setOwnerInfo(getPrefStringValue(mPref, SESSION_OWNER_INFO));
				
				mSessionData.setCamUpdateTimestamp(getPrefLongValue(mPref, SESSION_UPDATE_TS));
				mSessionData.setCamUpdateList(getPrefStringValue(mPref, SESSION_UPDATE_CAMS));
			}
		}
	}
	
	public void cleanSession(){
		Log.e(TAG, "cleanSession()");
		Thread.dumpStack();
		//clearSharedPreferences(mPref);
		//clearSharedPreferences(mSecuredPref);
		setAuthToken("");
		setUserid("");
		setAccount("");
		setOwnerInfo("");
		setIsCertificated(false);
		//setOwnerChannelInfo(null);
		notifySessionUpdate();
	}
	
	public void cleanHostUrls(){
		setHostUrl("");
		setStorageHostUrl("");
		setUploadHostUrl("");
		setArtistsHostUrl("");
	}
	
	public String getHostUrl(){
		return mSessionData.getHostUrl();
	}
	
	synchronized public void setHostUrl(String strURL){
		mSessionData.setHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getStorageHostUrl(){
		return mSessionData.getStorageHostUrl();
	}
	
	synchronized public void setStorageHostUrl(String strURL){
		mSessionData.setStorageHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getUploadHostUrl(){
		return mSessionData.getUploadHostUrl();
	}
	
	synchronized public void setUploadHostUrl(String strURL){
		mSessionData.setUploadHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getArtistsHostUrl(){
		return mSessionData.getArtistsHostUrl();
	}
	
	synchronized public void setArtistsHostUrl(String strURL){
		mSessionData.setArtistsHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getUserid(){
		return mSessionData.getUserid();
	}
	
	synchronized public void setUserid(String strId){
		setPrefStringValue(mPref, SESSION_USERID, strId);
		mSessionData.setUserid(strId);
		notifySessionUpdate();
	}
	
	public String getAccount(){
		return mSessionData.getAccount();
	}
	
	synchronized public void setAccount(String strAccount){
		setPrefStringValue(mPref, SESSION_ACCOUNT, strAccount);
		mSessionData.setAccount(strAccount);
		notifySessionUpdate();
	}
	
	public boolean isUseridValid(){
		String id = getUserid();
		return (null != id) && (0 < id.length());
	}
	
	public String getDomain(){
		return mSessionData.getDomain();
	}
	
	synchronized public void setDomain(String strDomain){
		setPrefStringValue(mPref, SESSION_DOMAIN, strDomain);
		mSessionData.setDomain(strDomain);
		notifySessionUpdate();
	}
	
	public boolean isTokenValid(){
		String token = getAuthToken();
		return (null != token) && (0 < token.length());
	}
	
	public String getAuthToken(){
		return mSessionData.getAuthToken();
	}
	
	synchronized public void setAuthToken(String strToken){
		if(DEBUG)
			Log.e(TAG, "setAuthToken(), old session = "+mSessionData.getAuthToken()+", strToken = "+strToken);
		
		setPrefStringValue(mPref, SESSION_TOKEN, strToken);
		mSessionData.setAuthToken(strToken);
		notifySessionUpdate();
	}
	
	public boolean getIsCertificated(){
		return mSessionData.getIsCertificated();
	}
	
	public void setIsCertificated(boolean bIsCertificated){
		setPrefIntValue(mPref, SESSION_ACC_CERTIFICATED, bIsCertificated?1:0);
		mSessionData.setIsCertificated(bIsCertificated);
		notifySessionUpdate();
	}
	
	public boolean getIsProductionMode(){
		return mSessionData.getIsProductionMode();
	}
	
	public void setIsProductionMode(boolean bIsProductionMode){
		setPrefIntValue(mPref, SESSION_PRODUCTION_MODE, bIsProductionMode?1:0);
		mSessionData.setIsProductionMode(bIsProductionMode);
		notifySessionUpdate();
		//BeseyeHttpTask.checkHostAddr();
	}
	
	public String getOwnerInfo(){
		return mSessionData.getOwnerInfo();
	}
	
	synchronized public void setOwnerInfo(String strOwnerInfo){
		setPrefStringValue(mPref, SESSION_OWNER_INFO, strOwnerInfo);
		mSessionData.setOwnerInfo(strOwnerInfo);
		notifySessionUpdate();
	}
	
	public long getCamUpdateTimestamp(){
		return mSessionData.getCamUpdateTimestamp();
	}
	
	synchronized public void setCamUpdateTimestamp(long lCamUpdateTs){
		setPrefLongValue(mPref, SESSION_UPDATE_TS, lCamUpdateTs);
		mSessionData.setCamUpdateTimestamp(lCamUpdateTs);
		notifySessionUpdate();
	}
	
	public String getCamUpdateList(){
		return mSessionData.getCamUpdateList();
	}
	
	synchronized public void setCamUpdateList(String strCamUpdateList){
		setPrefStringValue(mPref, SESSION_UPDATE_CAMS, strCamUpdateList);
		mSessionData.setCamUpdateList(strCamUpdateList);
		notifySessionUpdate();
	}
	
	public SessionData getSessionData(){
		return mSessionData;
	}
	
	public void setSessionData(SessionData data){
		mSessionData = data;
	}
	
	private WeakReference<ISessionUpdateCallback> mSessionUpdateCallback;
	public void registerSessionUpdateCallback(ISessionUpdateCallback cb){
		mSessionUpdateCallback = new WeakReference<ISessionUpdateCallback>(cb);
	}
	
	static public interface ISessionUpdateCallback{
		public void onSessionUpdate(SessionData data);
	}
	
	private void notifySessionUpdate(){
		if(null != mSessionUpdateCallback){
			ISessionUpdateCallback cb  = mSessionUpdateCallback.get();
			if(null != cb)
				cb.onSessionUpdate(mSessionData);
		}
	}
	
	public static class SessionData implements Parcelable{
		private String mStrHostUrl, mStrStorageHostUrl, mStrUploadHostUrl, mStrArtistsHostUrl, mStrUserid, mStrAccount, mStrDomain, mStrToken, mStrOwnerInfo;
		private boolean mbIsCertificated, mbProductionMode;
		
		private long mlCamUpdateTs;
		private String mStrCamUpdateList;
		
		public SessionData() {
			mStrHostUrl = "";
			mStrStorageHostUrl = "";
			mStrUploadHostUrl = "";
			mStrArtistsHostUrl = "";
			mStrUserid = "";
			mStrAccount = "";
			mStrDomain = "";
			mStrToken = "";
			mStrOwnerInfo = "";
			mbIsCertificated = false;
			mbProductionMode = true;
			
			mlCamUpdateTs = 0;
			mStrCamUpdateList = "";
		}
		
		public String getHostUrl(){
			return mStrHostUrl;
		}
		
		synchronized public void setHostUrl(String strURL){
			mStrHostUrl = strURL;
		}
		
		public String getStorageHostUrl(){
			return mStrStorageHostUrl;
		}
		
		synchronized public void setStorageHostUrl(String strURL){
			mStrStorageHostUrl = strURL;
		}
		
		public String getUploadHostUrl(){
			return mStrUploadHostUrl;
		}
		
		synchronized public void setUploadHostUrl(String strURL){
			mStrUploadHostUrl = strURL;
		}

		public String getArtistsHostUrl(){
			return mStrArtistsHostUrl;
		}
		
		synchronized public void setArtistsHostUrl(String strURL){
			mStrArtistsHostUrl = strURL;
		}

		public String getUserid(){
			return mStrUserid;
		}
		
		synchronized public void setUserid(String strId){
			mStrUserid = strId;
		}
		
		public String getAccount(){
			return mStrAccount;
		}
		
		synchronized public void setAccount(String strAccount){
			mStrAccount = strAccount;
		}
		
		public boolean isUseridValid(){
			String id = getUserid();
			return (null != id) && (0 < id.length());
		}
		
		public String getDomain(){
			return mStrDomain;
		}
		
		synchronized public void setDomain(String strDomain){
			mStrDomain = strDomain;
		}
		
		public boolean isTokenValid(){
			String token = getAuthToken();
			return (null != token) && (0 < token.length());
		}
		
		public String getAuthToken(){
			return mStrToken;
		}
		
		synchronized public void setAuthToken(String strToken){
			mStrToken = strToken;
		}
		
		public boolean getIsCertificated(){
			return mbIsCertificated;
		}
		
		public void setIsCertificated(boolean bIsCertificated){
			mbIsCertificated = bIsCertificated;
		}
		
		public boolean getIsProductionMode(){
			return mbProductionMode;
		}
		
		public void setIsProductionMode(boolean bIsProductionMode){
			mbProductionMode = bIsProductionMode;
		}

		@Override
		public int describeContents() {
			return 0;
		}
		
		public String getOwnerInfo(){
			return mStrOwnerInfo;
		}
		
		synchronized public void setOwnerInfo(String strOwnerInfo){
			mStrOwnerInfo = strOwnerInfo;
		}
		
		public long getCamUpdateTimestamp(){
			return mlCamUpdateTs;
		}
		
		synchronized public void setCamUpdateTimestamp(long lCamUpdateTs){
			mlCamUpdateTs = lCamUpdateTs;
		}
		
		public String getCamUpdateList(){
			return mStrCamUpdateList;
		}
		
		synchronized public void setCamUpdateList(String strCamUpdateList){
			mStrCamUpdateList = strCamUpdateList;
		}
	
		@Override
		public void writeToParcel(Parcel dest, int flags) {
	 
			// We just need to write each field into the
			// parcel. When we read from parcel, they
			// will come back in the same order
			dest.writeString(mStrHostUrl);
			dest.writeString(mStrStorageHostUrl);
			dest.writeString(mStrUploadHostUrl);
			dest.writeString(mStrArtistsHostUrl);
			dest.writeString(mStrUserid);
			dest.writeString(mStrAccount);
			dest.writeString(mStrDomain);
			dest.writeString(mStrToken);
			dest.writeString(mStrOwnerInfo);
			
			dest.writeInt(mbIsCertificated?1:0);
			dest.writeInt(mbProductionMode?1:0);
			
			dest.writeLong(mlCamUpdateTs);
			dest.writeString(mStrCamUpdateList);
		}
		
		private SessionData(Parcel in) {
			readFromParcel(in);
	    }
	 
		/**
		 *
		 * Called from the constructor to create this
		 * object from a parcel.
		 *
		 * @param in parcel from which to re-create object
		 */
		private void readFromParcel(Parcel in) {
	 
			// We just need to read back each
			// field in the order that it was
			// written to the parcel
			mStrHostUrl = in.readString();
			mStrStorageHostUrl = in.readString();
			mStrUploadHostUrl = in.readString();
			mStrArtistsHostUrl = in.readString();
			mStrUserid = in.readString();
			mStrAccount = in.readString();
			mStrDomain = in.readString();
			mStrToken = in.readString();
			mStrOwnerInfo = in.readString();
			
			mbIsCertificated = in.readInt()>0?true:false;
			mbProductionMode = in.readInt()>0?true:false;
			
			mlCamUpdateTs = in.readLong();
			mStrCamUpdateList = in.readString();
		}
		
		public static final Parcelable.Creator<SessionData> CREATOR = new Parcelable.Creator<SessionData>() {
	        public SessionData createFromParcel(Parcel in) {
	            return new SessionData(in);
	        }

	        public SessionData[] newArray(int size) {
	            return new SessionData[size];
	        }
	    };
	}
	
	//Utils function
	static public String appendSession(String url){
		String strUrl = url;
		if(SessionMgr.getInstance().isTokenValid()){
         	Bundle paramters = new Bundle();
            paramters.putString("session", SessionMgr.getInstance().getAuthToken());
             
         	int iIndex = -1;
         	if(-1 < (iIndex = url.lastIndexOf("?"))){
         		if(iIndex == url.length()-1){
         			strUrl = url+encodeUrl(paramters);
         		}else{
         			strUrl = url+"&"+encodeUrl(paramters);
         		}
         	}else{
         		strUrl = url+"?"+encodeUrl(paramters);
         	}
        } 
		return strUrl;
	}
	
	static public String encodeUrl(Bundle parameters) {
        if (parameters == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : parameters.keySet()) {
            Object parameter = parameters.get(key);
            if (!(parameter instanceof String)) {
                continue;
            }

            if (first) first = false; else sb.append("&");
            sb.append(URLEncoder.encode(key) + "=" +
                      URLEncoder.encode(parameters.getString(key)));
        }
        return sb.toString();
    }
}
