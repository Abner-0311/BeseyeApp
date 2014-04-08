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

//SessionMgr is responsible for storing back-end URL, token, user mdid in storage/memory
public class SessionMgr {
	static private final String SESSION_PREF 				= "ikala_ses";
	static private final String SESSION_TOKEN 				= "ikala_token";
	static private final String SESSION_DOMAIN 				= "ikala_domain";
	static private final String SESSION_MDID				= "ikala_mdid";
	static private final String SESSION_ACCOUNT				= "ikala_account";
	static private final String SESSION_READ_AGREEMENT		= "ikala_read_agreement";
	static private final String SESSION_OPEN_ID				= "ikala_open_id";
	static private final String SESSION_OPEN_ID_TYPE		= "ikala_open_id_type";
	static private final String SESSION_ACC_CERTIFICATED	= "ikala_certificated";
	static private final String SESSION_OWNER_INFO			= "ikala_owner_data";
	static private final String SESSION_ACCOUNT_CREATE_DATE	= "ikala_account_register";
	
	static private final String SESSION_PRODUCTION_MODE	    = "ikala_server_mode";
	
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
				mSessionData.setMdid(getPrefStringValue(mPref, SESSION_MDID));
				mSessionData.setAccount(getPrefStringValue(mPref, SESSION_ACCOUNT));
				mSessionData.setDomain(getPrefStringValue(mPref, SESSION_DOMAIN));
				mSessionData.setAuthToken(getPrefStringValue(mPref, SESSION_TOKEN));
				mSessionData.setIsOpenId(0 <getPrefIntValue(mPref, SESSION_OPEN_ID));
				mSessionData.setOpenIdType(getPrefStringValue(mPref, SESSION_OPEN_ID_TYPE));
				mSessionData.setIsCertificated(0 <getPrefIntValue(mPref, SESSION_ACC_CERTIFICATED));
				mSessionData.setIsProductionMode(0 <getPrefIntValue(mPref, SESSION_PRODUCTION_MODE, 1));
				mSessionData.setOwnerInfo(getPrefStringValue(mPref, SESSION_OWNER_INFO));
				mSessionData.setRegisterDate(getPrefLongValue(mPref, SESSION_ACCOUNT_CREATE_DATE));
			}
		}
	}
	
	public void cleanSession(){
		Log.e(TAG, "cleanSession()");
		Thread.dumpStack();
		//clearSharedPreferences(mPref);
		//clearSharedPreferences(mSecuredPref);
		setMdid("");
		setAccount("");
		setIsOpenId(false);
		setOpenIdType("");
		setOwnerInfo("");
		setIsCertificated(false);
		setIsReadAgreement(false);
		setRegisterDate(0);
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
	
	public String getMdid(){
		return mSessionData.getMdid();
	}
	
	synchronized public void setMdid(String strId){
		setPrefStringValue(mPref, SESSION_MDID, strId);
		mSessionData.setMdid(strId);
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
	
	public boolean isMdidValid(){
		String id = getMdid();
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
	
//	public ChannelInfo getOwnerChannelInfo(){
//		return mOwnerChannelInfo;
//	}
//	
//	public void setOwnerChannelInfo(ChannelInfo info){
//		mOwnerChannelInfo = info;
//	}
	
	public String getOpenIdType(){
		return mSessionData.getOpenIdType();
	}
	
	synchronized public void setOpenIdType(String strOpenIdType){
		setPrefStringValue(mPref, SESSION_OPEN_ID_TYPE, strOpenIdType);
		mSessionData.setOpenIdType(strOpenIdType);
		notifySessionUpdate();
	}
	
	public boolean getIsOpenId(){
		return mSessionData.getIsOpenId();
	}
	
	public void setIsOpenId(boolean bIsOpenId){
		setPrefIntValue(mPref, SESSION_OPEN_ID, bIsOpenId?1:0);
		mSessionData.setIsOpenId(bIsOpenId);
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
	
	public boolean getIsReadAgreement(){
		return mSessionData.getIsReadAgreement();
	}
	
	public void setIsReadAgreement(boolean bIsReadAgreement){
		setPrefIntValue(mPref, SESSION_READ_AGREEMENT, bIsReadAgreement?1:0);
		mSessionData.setIsReadAgreement(bIsReadAgreement);
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
	
	public long getRegisterDate(){
		return mSessionData.getRegisterDate();
	}
	
	synchronized public void setRegisterDate(long lRegisterDate){
		setPrefLongValue(mPref, SESSION_ACCOUNT_CREATE_DATE, lRegisterDate);
		mSessionData.setRegisterDate(lRegisterDate);
		notifySessionUpdate();
	}
	
	public SessionData getSessionData(){
		return mSessionData;
	}
	
	public void setSessionData(SessionData data){
		mSessionData = data;
	}
	
//	static public boolean isVip(){
//		if(sSessionMgr!=null){
//			try {
//				JSONObject obj = new JSONObject(sSessionMgr.getOwnerInfo());
//				String isVipString = BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.USER_IS_VIP);
//				if(isVipString.compareTo("1")==0)
//					return true;
////					return false;
//				else
//					return false;
//			} catch (JSONException e1) {
//				e1.printStackTrace();
//				return false;
//			}
//		}
//		return false;
//	}
	
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
		private String mStrHostUrl, mStrStorageHostUrl, mStrUploadHostUrl, mStrArtistsHostUrl, mStrMdid, mStrAccount, mStrDomain, mStrToken, mStrOpenIdType, mStrOwnerInfo;
		private boolean mbIsOpenId, mbIsCertificated, mbReadAgreement, mbProductionMode;
		private long mlRegisterDate;
		
		public SessionData() {
			mStrHostUrl = "";
			mStrStorageHostUrl = "";
			mStrUploadHostUrl = "";
			mStrArtistsHostUrl = "";
			mStrMdid = "";
			mStrAccount = "";
			mStrDomain = "";
			mStrToken = "";
			mStrOpenIdType = "";
			mStrOwnerInfo = "";
			mbIsOpenId = false;
			mbIsCertificated = false;
			mbReadAgreement = false;
			mlRegisterDate= 0;
			mbProductionMode = true;
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

		public String getMdid(){
			return mStrMdid;
		}
		
		synchronized public void setMdid(String strId){
			mStrMdid = strId;
		}
		
		public String getAccount(){
			return mStrAccount;
		}
		
		synchronized public void setAccount(String strAccount){
			mStrAccount = strAccount;
		}
		
		public boolean isMdidValid(){
			String id = getMdid();
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
		
		public String getOpenIdType(){
			return mStrOpenIdType;
		}
		
		synchronized public void setOpenIdType(String strOpenIdType){
			mStrOpenIdType = strOpenIdType;
		}
		
		public boolean getIsOpenId(){
			return mbIsOpenId;
		}
		
		public void setIsOpenId(boolean bIsOpenId){
			mbIsOpenId = bIsOpenId;
		}
		
		public boolean getIsCertificated(){
			return mbIsCertificated;
		}
		
		public void setIsCertificated(boolean bIsCertificated){
			mbIsCertificated = bIsCertificated;
		}
		
		public boolean getIsReadAgreement(){
			return mbReadAgreement;
		}
		
		public void setIsReadAgreement(boolean bIsReadAgreement){
			mbReadAgreement = bIsReadAgreement;
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
		
		public long getRegisterDate(){
			return mlRegisterDate;
		}
		
		synchronized public void setRegisterDate(long lRegisterDate){
			mlRegisterDate = lRegisterDate;
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
			dest.writeString(mStrMdid);
			dest.writeString(mStrAccount);
			dest.writeString(mStrDomain);
			dest.writeString(mStrToken);
			dest.writeString(mStrOpenIdType);
			dest.writeString(mStrOwnerInfo);
			
			dest.writeInt(mbIsOpenId?1:0);
			dest.writeInt(mbIsCertificated?1:0);
			dest.writeInt(mbReadAgreement?1:0);
			dest.writeInt(mbProductionMode?1:0);
		
			dest.writeLong(mlRegisterDate);
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
			mStrMdid = in.readString();
			mStrAccount = in.readString();
			mStrDomain = in.readString();
			mStrToken = in.readString();
			mStrOpenIdType = in.readString();
			mStrOwnerInfo = in.readString();
			
			mbIsOpenId = in.readInt()>0?true:false;
			mbIsCertificated = in.readInt()>0?true:false;
			mbReadAgreement = in.readInt()>0?true:false;
			mbProductionMode = in.readInt()>0?true:false;
			
			mlRegisterDate = in.readLong();
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
