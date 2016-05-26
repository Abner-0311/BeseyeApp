package com.app.beseye.httptask;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.*;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.app.beseye.BeseyeApplication;
import com.app.beseye.BeseyeNewsActivity.BeseyeNewsHistoryMgr;
import com.app.beseye.test.BeseyeComputexModeActivity;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

//SessionMgr is responsible for storing back-end URL, token, user Userid in storage/memory
public class SessionMgr {
	static public enum SERVER_MODE{
		MODE_DEV,
		MODE_DEV2,
		MODE_PRODUCTION,
		MODE_STAGING,
		MODE_CHINA_STAGE,
		MODE_TYPES_CNT;
		
		static public SERVER_MODE translateToMode(int iMode){
			SERVER_MODE mode = MODE_DEV;
			switch(iMode){
				case 1:{
					mode = MODE_DEV2;
					break;
				} 
				case 3:{
					mode = MODE_STAGING;
					break;
				} 
				case 2:{
					mode = MODE_PRODUCTION;
					break;
				} 
				case 4:{
					mode = MODE_CHINA_STAGE;
					break;
				} 
			}
			return mode;
		}
	}
	
	static private final String ACCOUNT_URL_FORMAT = "%s/be_acc/v1/";
	
	static private final String[] ACC_VPC_BE_URL = {"http://oregon-p2-dev-api-1.beseye.com/acc",
													"https://acc-dev.beseye.com", 
													"http://oregon-p1-stage-api-1.beseye.com/acc",
													"http://oregon-p2-stage-api-1.beseye.com/acc",
													"http://oregon-p1-stage-api-1.beseye.com/acc"}; 
	
	static private final String[] ACCOUNT_BE_URL = {"https://oregon-p2-dev-api-1.beseye.com/acc",
													"https://acc-dev.beseye.com", 
													"https://oregon-p1-stage-api-%d.beseye.com/acc",
													"https://oregon-p2-stage-api-%d.beseye.com/acc",
													"https://bj-p2-stage-api-%d.beseye.cn/acc"}; 
	
	static private final String[] MM_BE_URL = { "https://oregon-p2-dev-api-1.beseye.com/mm/",
												"https://mm-dev.beseye.com/",
												"https://oregon-p1-stage-api-%d.beseye.com/mm/",
												"https://oregon-p2-stage-api-%d.beseye.com/mm/",
												"https://bj-p2-stage-api-%d.beseye.cn/mm/"}; 
	
	static private final String[] IMPMM_BE_URL = {  "https://oregon-p2-dev-api-1.beseye.com/impmm/",
													"https://mm-dev.beseye.com/",
													"https://oregon-p1-stage-api-%d.beseye.com/impmm/",
													"https://oregon-p2-stage-api-%d.beseye.com/impmm/",
													"https://bj-p2-stage-api-%d.beseye.cn/impmm/"}; 

	static private final String[] CAM_BE_URL = { "https://oregon-p2-dev-api-1.beseye.com/cam/",
												 "https://ns-dev.beseye.com/",
												 "https://oregon-p1-stage-api-%d.beseye.com/cam/",
												 "https://oregon-p2-stage-api-%d.beseye.com/cam/",
												 "https://bj-p2-stage-api-%d.beseye.cn/cam/"}; 
	
	static private final String[] NS_BE_URL = { "https://oregon-p2-dev-api-1.beseye.com/ns/",
												"https://ns-dev.beseye.com/",
												"https://oregon-p1-stage-api-%d.beseye.com/ns/",
												"https://oregon-p2-stage-api-%d.beseye.com/ns/",
												"https://bj-p2-stage-api-%d.beseye.cn/ns/"}; 
	
	static private final String[] CAM_WS_BE_URL = { "https://oregon-p2-dev-api-1.beseye.com/ws/",
													"https://ns-dev.beseye.com/",
													"https://oregon-p1-stage-api-%d.beseye.com/ws/",
													"https://oregon-p2-stage-api-%d.beseye.com/ws/",
													"https://bj-p2-stage-api-%d.beseye.cn/ws/"}; 
	
	static private final String[] WS_BE_URL = { "https://oregon-p2-dev-ws-1.beseye.com/",
												"https://ws-dev.beseye.com/", 
												"https://oregon-p1-stage-ws-%d.beseye.com/",
												"https://oregon-p2-stage-ws-%d.beseye.com/",
												"https://bj-p2-stage-ws-%d.beseye.cn/"}; 
	
	static private final String[] WSA_BE_URL = { "https://oregon-p2-dev-wsa-1.beseye.com/",
												 "https://wsa-dev.beseye.com/", 
												 "https://oregon-p1-stage-wsa-%d.beseye.com/",
												 "https://oregon-p2-stage-wsa-%d.beseye.com/",
												 "https://bj-p2-stage-wsa-%d.beseye.cn/"}; 
	
	static private final String[] NEWS_BE_URL = { "https://oregon-p2-dev-api-1.beseye.com/news/",
												  "https://news-dev.beseye.com/", 
												  "https://oregon-p1-stage-api-%d.beseye.com/news/",
												  "https://oregon-p2-stage-api-%d.beseye.com/news/",
												  "https://bj-p2-stage-api-%d.beseye.cn/news/"}; 
	
	static private final String[] UPDATE_BE_URL = { "https://oregon-p2-dev-api-1.beseye.com/ub/",
												  "https://news-dev.beseye.com/", 
												  "https://oregon-p1-stage-api-%d.beseye.com/ub/",
												  "https://oregon-p2-stage-api-%d.beseye.com/ub/",
												  "https://bj-p2-stage-api-%d.beseye.cn/ub/"}; 
	
	
	
	static private final String SESSION_PREF 				= "beseye_ses";
	static private final String SESSION_PREF_SEC 			= "beseye_ses_sec";
	
	static private final String SESSION_TOKEN 				= "beseye_token";
	static private final String SESSION_DOMAIN 				= "beseye_domain";
	static private final String SESSION_USERID				= "beseye_userid";
	static private final String SESSION_ACCOUNT				= "beseye_account";
	static private final String SESSION_ACC_CERTIFICATED	= "beseye_certificated";
	static private final String SESSION_ACC_TRUSTED			= "beseye_trust_dev";
	static private final String SESSION_OWNER_INFO			= "beseye_owner_data";
	static private final String SESSION_OWNER_VPC_NUM		= "beseye_owner_vpc_no";
	static private final String SESSION_PAIR_TOKEN	    	= "beseye_pair_token";
	
//	static private final String SESSION_UPDATE_TS			= "beseye_cam_update_ts";
//	static private final String SESSION_UPDATE_CAMS			= "beseye_cam_update_list";
	static private final String SESSION_UPDATE_SUSPEND		= "beseye_cam_update_suspend";
	
	static private final String SESSION_SERVER_MODE	    	= "beseye_server_mode";
	static private final String SESSION_DETACH_HW_ID	    = "beseye_detach_hw_id";//Computex
	static private final String SESSION_SIGNUP_EMAIL	    = "beseye_signup_email";//Computex
	
	static private final String SESSION_NEWS_HISTORY	    = "beseye_news_history";
	static private final String SESSION_NEWS_LAST_MAX	    = "beseye_news_last_max";
	static private final String SESSION_NEWS_IND_SHOW	    = "beseye_news_show_ind";
	
	static private final String SESSION_TRANFER_TO_SEC	    = "beseye_transfer_to_sec";
	static private final String SESSION_TRANFER_TO_ATT_EVENT= "beseye_transfer_to_att_event_hw_id";
	//Below items are after security mode
	
	static private final String SESSION_DEBUG_SHOW_NOTIFY	= "beseye_debug_show_notify";
	static private final String SESSION_HD_INTRO_SHOWN		= "beseye_human_detect_intro_shown";
	//static private final String SESSION_SHOW_HD_INTRO_ONCE	= "beseye_show_human_detect_intro_once";//deprecated
	static private final String SESSION_SHOW_HD_INTRO_ALWAYS= "beseye_show_human_detect_intro_always";
	static private final String SESSION_ENABLE_BES_APP_CONTROL= "beseye_enable_beseye_app_ver_control";

	static private final String SESSION_DEBUG_STREAM_PATH	= "beseye_debug_stream_path";
	//static private final String SESSION_SCREENSHOT_FEATURE	= "beseye_screen_feature";
	
	static private final String SESSION_DEBUG_FAKE_APP_VER	= "beseye_fake_app_ver";
	
	static private SessionMgr sSessionMgr;
	
	static public void createInstance(Context context){
		if(null == sSessionMgr)
			sSessionMgr = new SessionMgr(context);
	}
	
	static public SessionMgr getInstance(){
		return sSessionMgr;
	}
	
	private SharedPreferences mPref, mSecuredPref;
	//private String mStrHostUrl, mStrMMHostUrl;
	private SessionData mSessionData;
	private boolean mbShowPirvateCam  = false;
	//private ChannelInfo mOwnerChannelInfo;
	
	private SessionMgr(Context context){
		if(null != context){
			//mPref = getSharedPreferences(context, SESSION_PREF);
			mPref = getSecuredSharedPreferences(context, SESSION_PREF_SEC);
			//setPrefBooleanValue(mPref, SESSION_TRANFER_TO_SEC, false);
			if(false == getPrefBooleanValue(mPref, SESSION_TRANFER_TO_SEC, false) && BeseyeApplication.isInMainProcess()){
				transferToSecuMode(context);
			}
			
			mSessionData = new SessionData();
			if(null != mSessionData){
				mSessionData.setUserid(getPrefStringValue(mPref, SESSION_USERID));
				mSessionData.setAccount(getPrefStringValue(mPref, SESSION_ACCOUNT));
				mSessionData.setDomain(getPrefStringValue(mPref, SESSION_DOMAIN));
				mSessionData.setAuthToken(getPrefStringValue(mPref, SESSION_TOKEN));
				mSessionData.setVPCNumber(getPrefIntValue(mPref, SESSION_OWNER_VPC_NUM, 1));
				mSessionData.setPairToken(getPrefStringValue(mPref, SESSION_PAIR_TOKEN));
				mSessionData.setIsCertificated(0 <getPrefIntValue(mPref, SESSION_ACC_CERTIFICATED));
				mSessionData.setOwnerInfo(getPrefStringValue(mPref, SESSION_OWNER_INFO));
				mSessionData.setIsTrustDev(getPrefBooleanValue(mPref, SESSION_ACC_TRUSTED, true));
				
//				mSessionData.setCamUpdateTimestamp(getPrefLongValue(mPref, SESSION_UPDATE_TS));
//				mSessionData.setCamUpdateList(getPrefStringValue(mPref, SESSION_UPDATE_CAMS));
				mSessionData.setIsCamSWUpdateSuspended(getPrefBooleanValue(mPref, SESSION_UPDATE_SUSPEND, false));
				
				mSessionData.setServerMode(SERVER_MODE.translateToMode(getPrefIntValue(mPref, SESSION_SERVER_MODE, BeseyeConfig.DEFAULT_SERVER_MODE.ordinal())));
				mSessionData.setDetachHWID(getPrefStringValue(mPref, SESSION_DETACH_HW_ID));
				mSessionData.setSignupEmail(getPrefStringValue(mPref, SESSION_SIGNUP_EMAIL));
				
				mSessionData.setIsShowNotificationFromToast(getPrefBooleanValue(mPref, SESSION_DEBUG_SHOW_NOTIFY, false));
				
				mSessionData.setDebugStreamPath(getPrefStringValue(mPref, SESSION_DEBUG_STREAM_PATH));
				
			}
			
			if(false == getPrefBooleanValue(mPref, SESSION_TRANFER_TO_ATT_EVENT, false) && BeseyeApplication.isInMainProcess()){
				String strHWIds = "";
				if(null != BeseyeComputexModeActivity.hwids_prod){
					for(int idx = 0; idx <  BeseyeComputexModeActivity.hwids_prod.length; idx ++){
						if(strHWIds.equals("")){
							strHWIds =  BeseyeComputexModeActivity.hwids_prod[idx];
						}else{
							strHWIds += (","+BeseyeComputexModeActivity.hwids_prod[idx]);
						}
					}
					Log.e(TAG, "transferToAttEvent() ++");
					setDetachHWID(strHWIds);
				}
				setPrefBooleanValue(mPref, SESSION_TRANFER_TO_ATT_EVENT, true);
			}
		}
	}
	
	private void transferToSecuMode(Context context){
		Log.e(TAG, "transferToSecuMode() ++");
		SharedPreferences mPrefOld = getSharedPreferences(context, SESSION_PREF);
		//SharedPreferences mPrefOld = getSecuredSharedPreferences(context, SESSION_PREF_SEC);
		if(null != mPrefOld){
			setPrefStringValue(mPref, SESSION_USERID, getPrefStringValue(mPrefOld, SESSION_USERID));
			setPrefStringValue(mPref, SESSION_ACCOUNT, getPrefStringValue(mPrefOld, SESSION_ACCOUNT));
			setPrefStringValue(mPref, SESSION_DOMAIN, getPrefStringValue(mPrefOld, SESSION_DOMAIN));
			setPrefStringValue(mPref, SESSION_TOKEN, getPrefStringValue(mPrefOld, SESSION_TOKEN));
			setPrefIntValue(mPref, SESSION_OWNER_VPC_NUM, getPrefIntValue(mPrefOld, SESSION_OWNER_VPC_NUM));
			setPrefIntValue(mPref, SESSION_ACC_CERTIFICATED, getPrefIntValue(mPrefOld, SESSION_ACC_CERTIFICATED));
			setPrefBooleanValue(mPref, SESSION_UPDATE_SUSPEND, getPrefBooleanValue(mPrefOld, SESSION_UPDATE_SUSPEND, false));
			setPrefStringValue(mPref, SESSION_PAIR_TOKEN, getPrefStringValue(mPrefOld, SESSION_PAIR_TOKEN));
			setPrefBooleanValue(mPref, SESSION_ACC_TRUSTED, getPrefBooleanValue(mPrefOld, SESSION_ACC_TRUSTED, false));

			setPrefStringValue(mPref, SESSION_OWNER_INFO, getPrefStringValue(mPrefOld, SESSION_OWNER_INFO));
//			setPrefLongValue(mPref, SESSION_UPDATE_TS, getPrefLongValue(mPrefOld, SESSION_UPDATE_TS));
//			setPrefStringValue(mPref, SESSION_UPDATE_CAMS, getPrefStringValue(mPrefOld, SESSION_UPDATE_CAMS));
			
			setPrefIntValue(mPref, SESSION_SERVER_MODE, getPrefIntValue(mPrefOld, SESSION_SERVER_MODE, BeseyeConfig.DEFAULT_SERVER_MODE.ordinal()));
			setPrefStringValue(mPref, SESSION_DETACH_HW_ID, getPrefStringValue(mPrefOld, SESSION_DETACH_HW_ID));
			setPrefStringValue(mPref, SESSION_SIGNUP_EMAIL, getPrefStringValue(mPrefOld, SESSION_SIGNUP_EMAIL));
			
			setPrefStringValue(mPref, SESSION_NEWS_HISTORY, getPrefStringValue(mPrefOld, SESSION_NEWS_HISTORY));
			setPrefIntValue(mPref, SESSION_NEWS_LAST_MAX, getPrefIntValue(mPrefOld, SESSION_NEWS_LAST_MAX, 0));
			setPrefIntValue(mPref, SESSION_NEWS_IND_SHOW, getPrefIntValue(mPrefOld, SESSION_NEWS_IND_SHOW));
			clearSharedPreferences(mPrefOld);
		}
		setPrefBooleanValue(mPref, SESSION_TRANFER_TO_SEC, true);
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				notifySessionUpdate();
			}}, 5000L);
		
		Log.e(TAG, "transferToSecuMode() --");
	}
	
	public void cleanSession(){
		Log.e(TAG, "cleanSession()");
		Thread.dumpStack();
		//clearSharedPreferences(mPref);
		//clearSharedPreferences(mSecuredPref);
		setAuthToken("");
		setUserid("");
		//setAccount("");
		setOwnerInfo("");
		setIsCertificated(false);
		setIsTrustDev(false);
		setVPCNumber(1);
		setNewsHistory("");
		setHumanDetectIntroShown(false);
		BeseyeNewsHistoryMgr.deinit();
		//setOwnerChannelInfo(null);
		mbShowPirvateCam = false;
		notifySessionUpdate();
	}
	
	public void cleanHostUrls(){
		setAccountBEHostUrl("");
		setVPCAccountBEHostUrl("");
		setMMBEHostUrl("");
		setIMPMMBEHostUrl("");
		setCamBEHostUrl("");
		setNSBEHostUrl("");
		setCamWSBEHostUrl("");
		setWSHostUrl("");
		setWSAHostUrl("");
		setNewsHostUrl("");
	}
	
	public int getFakeAppVer(){
		return getPrefIntValue(mPref, SESSION_DEBUG_FAKE_APP_VER, 0);
	}
	
	public void setFakeAppVer(int iFakeAppVer){
		setPrefIntValue(mPref, SESSION_DEBUG_FAKE_APP_VER, iFakeAppVer);
	}
	
	public boolean isFakeAppVerEnabled(){
		return 0 < getFakeAppVer();
	}
	
	public void setShowPirvateCam(boolean bFlag){
		mbShowPirvateCam = bFlag;
	}
	
	public boolean getShowPirvateCam(){
		return mbShowPirvateCam;
	}
	
	public String getNewsHistory(){
		return getPrefStringValue(mPref, SESSION_NEWS_HISTORY);
	}
	
	public void setNewsHistory(String strHistory){
		setPrefStringValue(mPref, SESSION_NEWS_HISTORY, strHistory);
	}
	
	public int getNewsLastMax(){
		return getPrefIntValue(mPref, SESSION_NEWS_LAST_MAX, -1);
	}
	
	public void setNewsLastMax(int iValue){
		setPrefIntValue(mPref, SESSION_NEWS_LAST_MAX, iValue);
	}
	
	public int getNewsShowInd(){
		return getPrefIntValue(mPref, SESSION_NEWS_IND_SHOW, -1);
	}
	
	public void setNewsShowInd(int iValue){
		setPrefIntValue(mPref, SESSION_NEWS_IND_SHOW, iValue);
	}
	
	public boolean getHumanDetectIntroShown(){
		return getPrefBooleanValue(mPref, SESSION_HD_INTRO_SHOWN, false);
	}
	
	public void setHumanDetectIntroShown(boolean bValue){
		setPrefBooleanValue(mPref, SESSION_HD_INTRO_SHOWN, bValue);
	}
	
//	public boolean getHumanDetectIntroShowOnce(){
//		return getPrefBooleanValue(mPref, SESSION_SHOW_HD_INTRO_ONCE, true);
//	}
//	
//	public void setHumanDetectIntroShowOnce(boolean bValue){
//		setPrefBooleanValue(mPref, SESSION_SHOW_HD_INTRO_ONCE, bValue);
//	}

	public boolean getHumanDetectIntroShowAlways(){
		return getPrefBooleanValue(mPref, SESSION_SHOW_HD_INTRO_ALWAYS, false);
	}
	
	public void setHumanDetectIntroShowAlways(boolean bValue){
		setPrefBooleanValue(mPref, SESSION_SHOW_HD_INTRO_ALWAYS, bValue);
	}
	
	public boolean getEnableBeseyeAppVerControl(){
		return getPrefBooleanValue(mPref, SESSION_ENABLE_BES_APP_CONTROL, BeseyeConfig.PRODUCTION_VER?true:false);
	}
	
	public void setEnableBeseyeAppVerControl(boolean bValue){
		setPrefBooleanValue(mPref, SESSION_ENABLE_BES_APP_CONTROL, bValue);
	}
	
	public void setBEHostUrl(SERVER_MODE mode){
		setAccountBEHostUrl(mode);
		setVPCAccountBEHostUrl(mode);
		setMMBEHostUrl(mode);
		setIMPMMBEHostUrl(mode);
		setCamBEHostUrl(mode);
		setNSBEHostUrl(mode);
		setCamWSBEHostUrl(mode);
		setWSBEHostUrl(mode);
		setWSABEHostUrl(mode);
		setNewsBEHostUrl(mode);
		setUpdateBEHostUrl(mode);
	}
	
	public int getVPCNumber(){
		return mSessionData.getVPCNumber();
	}
	
	public void setVPCNumber(int iVPCno){
		Log.i(TAG, "setVPCNumber(), iVPCno:"+iVPCno);

		mSessionData.setVPCNumber(iVPCno);
		setPrefIntValue(mPref, SESSION_OWNER_VPC_NUM, mSessionData.getVPCNumber());
		setBEHostUrl(getServerMode());
		notifySessionUpdate();
	}
	
	public void setRegionNumber(int iRegNo){
		Log.i(TAG, "setRegionNumber(), iRegNo:"+iRegNo);

		//For production mode switch between China p2-stage and Oregon p1-stage
		if(BeseyeJSONUtil.ACC_REGION_WORLDWIDE == iRegNo && SessionMgr.getInstance().getServerMode().equals(SessionMgr.SERVER_MODE.MODE_CHINA_STAGE)){
			SessionMgr.getInstance().setServerMode(SessionMgr.SERVER_MODE.MODE_PRODUCTION);
			SessionMgr.getInstance().setBEHostUrl(SessionMgr.SERVER_MODE.MODE_PRODUCTION);
		}else if(BeseyeJSONUtil.ACC_REGION_CHINA == iRegNo && SessionMgr.getInstance().getServerMode().equals(SessionMgr.SERVER_MODE.MODE_PRODUCTION)){
			SessionMgr.getInstance().setServerMode(SessionMgr.SERVER_MODE.MODE_CHINA_STAGE);
			SessionMgr.getInstance().setBEHostUrl(SessionMgr.SERVER_MODE.MODE_CHINA_STAGE);
		}
	}
	
	public String getAccountBEHostUrl(){
		return mSessionData.getAccountBEHostUrl();
	}
	
	public void setAccountBEHostUrl(SERVER_MODE mode){
		setAccountBEHostUrl(String.format(ACCOUNT_URL_FORMAT, String.format(ACCOUNT_BE_URL[mode.ordinal()], getVPCNumber())));
	}
	
	synchronized public void setAccountBEHostUrl(String strURL){
		mSessionData.setAccountBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getVPCAccountBEHostUrl(){
		return mSessionData.getVPCAccountBEHostUrl();
	}
	
	public void setVPCAccountBEHostUrl(SERVER_MODE mode){
		setVPCAccountBEHostUrl(String.format(ACCOUNT_URL_FORMAT, ACC_VPC_BE_URL[mode.ordinal()]));
	}
	
	synchronized public void setVPCAccountBEHostUrl(String strURL){
		mSessionData.setVPCAccountBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getMMBEHostUrl(){
		return mSessionData.getMMBEHostUrl();
	}
	
	public void setMMBEHostUrl(SERVER_MODE mode){
		setMMBEHostUrl(String.format(MM_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setMMBEHostUrl(String strURL){
		mSessionData.setMMBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getIMPMMBEHostUrl(){
		return mSessionData.getIMPMMBEHostUrl();
	}
	
	public void setIMPMMBEHostUrl(SERVER_MODE mode){
		setIMPMMBEHostUrl(String.format(IMPMM_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setIMPMMBEHostUrl(String strURL){
		mSessionData.setIMPMMBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getCamBEHostUrl(){
		return mSessionData.getCAMBEHostUrl();
	}
	
	public void setCamBEHostUrl(SERVER_MODE mode){
		setCamBEHostUrl(String.format(CAM_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setCamBEHostUrl(String strURL){
		mSessionData.setCAMBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getNSBEHostUrl(){
		return mSessionData.getNSBEHostUrl();
	}
	
	public void setNSBEHostUrl(SERVER_MODE mode){
		setNSBEHostUrl(String.format(NS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setNSBEHostUrl(String strURL){
		mSessionData.setNSBEHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getCamWSBEHostUrl(){
		return mSessionData.getCamWSHostUrl();
	}
	
	public void setCamWSBEHostUrl(SERVER_MODE mode){
		setCamWSBEHostUrl(String.format(CAM_WS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	synchronized public void setCamWSBEHostUrl(String strURL){
		mSessionData.setCamWSHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public String getWSHostUrl(){
		return mSessionData.getWSHostUrl();
	}
	
	synchronized public void setWSHostUrl(String strURL){
		mSessionData.setWSHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public void setWSBEHostUrl(SERVER_MODE mode){
		setWSHostUrl(String.format(WS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	public String getWSAHostUrl(){
		return mSessionData.getWSAHostUrl();
	}
	
	synchronized public void setWSAHostUrl(String strURL){
		mSessionData.setWSAHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public void setWSABEHostUrl(SERVER_MODE mode){
		setWSAHostUrl(String.format(WSA_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	public String getNewsHostUrl(){
		return mSessionData.getNewsHostUrl();
	}
	
	synchronized public void setNewsHostUrl(String strURL){
		mSessionData.setNewsHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public void setNewsBEHostUrl(SERVER_MODE mode){
		setNewsHostUrl(String.format(NEWS_BE_URL[mode.ordinal()], getVPCNumber()));
	}
	
	public String getUpdateHostUrl(){
		return mSessionData.getUpdateHostUrl();
	}
	
	synchronized public void setUpdateHostUrl(String strURL){
		mSessionData.setUpdateHostUrl(strURL);
		notifySessionUpdate();
	}
	
	public void setUpdateBEHostUrl(SERVER_MODE mode){
		setUpdateHostUrl(String.format(UPDATE_BE_URL[mode.ordinal()], getVPCNumber()));
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
	
	public String getPairToken(){
		return mSessionData.getPairToken();
	}
	
	synchronized public void setPairToken(String strToken){
		setPrefStringValue(mPref, SESSION_PAIR_TOKEN, strToken);
		mSessionData.setPairToken(strToken);
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
	
	public boolean getIsCamSWUpdateSuspended(){
		return mSessionData.getIsCamSWUpdateSuspended();
	}
	
	public void setIsCamSWUpdateSuspended(boolean bIsCamSWUpdateSuspended){
		setPrefBooleanValue(mPref, SESSION_UPDATE_SUSPEND, bIsCamSWUpdateSuspended);
		mSessionData.setIsCamSWUpdateSuspended(bIsCamSWUpdateSuspended);
		notifySessionUpdate();
	}
	
	public boolean getIsTrustDev(){
		return mSessionData.getIsTrustDev();
	}
	
	public void setIsTrustDev(boolean bIsTrustDev){
		setPrefBooleanValue(mPref, SESSION_ACC_TRUSTED, bIsTrustDev);
		mSessionData.setIsTrustDev(bIsTrustDev);
		notifySessionUpdate();
	}
	
	public boolean getIsShowNotificationFromToast(){
		return mSessionData.getIsShowNotificationFromToast();
	}
	
	public void setIsShowNotificationFromToast(boolean bIsShowNotificationFromToast){
		setPrefBooleanValue(mPref, SESSION_DEBUG_SHOW_NOTIFY, bIsShowNotificationFromToast);
		mSessionData.setIsShowNotificationFromToast(bIsShowNotificationFromToast);
		notifySessionUpdate();
	}

	public SERVER_MODE getServerMode(){
		return mSessionData.getServerMode();
	}
	
	public void setServerMode(SERVER_MODE serverMode){
		setPrefIntValue(mPref, SESSION_SERVER_MODE, serverMode.ordinal());
		mSessionData.setServerMode(serverMode);
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
	
//	public long getCamUpdateTimestamp(){
//		return mSessionData.getCamUpdateTimestamp();
//	}
//	
//	synchronized public void setCamUpdateTimestamp(long lCamUpdateTs){
//		setPrefLongValue(mPref, SESSION_UPDATE_TS, lCamUpdateTs);
//		mSessionData.setCamUpdateTimestamp(lCamUpdateTs);
//		notifySessionUpdate();
//	}
//	
//	public String getCamUpdateList(){
//		return mSessionData.getCamUpdateList();
//	}
//	
//	synchronized public void setCamUpdateList(String strCamUpdateList){
//		setPrefStringValue(mPref, SESSION_UPDATE_CAMS, strCamUpdateList);
//		mSessionData.setCamUpdateList(strCamUpdateList);
//		notifySessionUpdate();
//	}
	
	public String getDetachHWID(){
		return mSessionData.getDetachHWID();
	}
	
	synchronized public void setDetachHWID(String strDetachHWID){
		setPrefStringValue(mPref, SESSION_DETACH_HW_ID, strDetachHWID);
		mSessionData.setDetachHWID(strDetachHWID);
		notifySessionUpdate();
	}
	
	public String getSignupEmail(){
		return mSessionData.getSignupEmail();
	}
	
	synchronized public void setSignupEmail(String strSignupEmail){
		setPrefStringValue(mPref, SESSION_SIGNUP_EMAIL, strSignupEmail);
		mSessionData.setSignupEmail(strSignupEmail);
		notifySessionUpdate();
	}
	
	public String getDebugStreamPath(){
		return mSessionData.getDebugStreamPath();
	}
	
	synchronized public void setDebugStreamPath(String strDebugStreamPath){
		setPrefStringValue(mPref, SESSION_DEBUG_STREAM_PATH, strDebugStreamPath);
		mSessionData.setDebugStreamPath(strDebugStreamPath);
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
		private String mStrHostUrl, mStrVPCHostUrl, mStrMMHostUrl, mStrIMPMMHostUrl, mStrCamHostUrl, mStrNSHostUrl, mStrCamWsHostUrl, mStrWSHostUrl, mStrWSAHostUrl, mStrNewsHostUrl, mStrUpdateHostUrl;
		private String mStrUserid, mStrAccount, mStrDomain, mStrToken, mStrOwnerInfo, mStrPairToken;
		private boolean mbIsCertificated, mbIsCamSWUpdateSuspended, mbIsTrustDev, mbIsShowNotificationFromToast;
		private SERVER_MODE mServerMode;
		private long mlCamUpdateTs;
		private String mStrCamUpdateList, mStrDetachHWID, mStrSignupEmail, mStrDebugStreamPath;
		private int miVPCno;
		
		public SessionData() {
			mStrHostUrl = "";
			mStrVPCHostUrl = "";
			mStrMMHostUrl = "";
			mStrIMPMMHostUrl = "";
			mStrCamHostUrl = "";
			mStrNSHostUrl = "";
			mStrCamWsHostUrl = "";
			mStrWSHostUrl = "";
			mStrWSAHostUrl = "";
			mStrNewsHostUrl = "";
			mStrUpdateHostUrl = "";
			
			mStrUserid = "";
			mStrAccount = "";
			mStrDomain = "";
			mStrToken = "";
			mStrOwnerInfo = "";
			mStrPairToken = "";
			mbIsCertificated = false;
			mbIsCamSWUpdateSuspended = false;
			mbIsTrustDev = false;
			mbIsShowNotificationFromToast = false;
			mServerMode = BeseyeConfig.DEFAULT_SERVER_MODE;
			
			mlCamUpdateTs = 0;
			mStrCamUpdateList = "";
			mStrDetachHWID = "";
			mStrSignupEmail = "";
			mStrDebugStreamPath = "";
			
			miVPCno = 1;
		}
		
		public String getAccountBEHostUrl(){
			return mStrHostUrl;
		}
		
		synchronized public void setAccountBEHostUrl(String strURL){
			mStrHostUrl = strURL;
		}
		
		public String getVPCAccountBEHostUrl(){
			return mStrVPCHostUrl;
		}
		
		synchronized public void setVPCAccountBEHostUrl(String strURL){
			mStrVPCHostUrl = strURL;
		}
		
		public String getMMBEHostUrl(){
			return mStrMMHostUrl;
		}
		
		synchronized public void setMMBEHostUrl(String strURL){
			mStrMMHostUrl = strURL;
		}
		
		public String getIMPMMBEHostUrl(){
			return mStrIMPMMHostUrl;
		}
		
		synchronized public void setIMPMMBEHostUrl(String strURL){
			mStrIMPMMHostUrl = strURL;
		}
		
		public String getCAMBEHostUrl(){
			return mStrCamHostUrl;
		}
		
		synchronized public void setCAMBEHostUrl(String strURL){
			mStrCamHostUrl = strURL;
		}
		
		public String getNSBEHostUrl(){
			return mStrNSHostUrl;
		}
		
		synchronized public void setNSBEHostUrl(String strURL){
			mStrNSHostUrl = strURL;
		}
		
		public String getCamWSHostUrl(){
			return mStrCamWsHostUrl;
		}
		
		synchronized public void setCamWSHostUrl(String strURL){
			mStrCamWsHostUrl = strURL;
		}

		public String getWSHostUrl(){
			return mStrWSHostUrl;
		}
		
		synchronized public void setWSHostUrl(String strURL){
			mStrWSHostUrl = strURL;
		}
		
		public String getWSAHostUrl(){
			return mStrWSAHostUrl;
		}
		
		synchronized public void setWSAHostUrl(String strURL){
			mStrWSAHostUrl = strURL;
		}
		
		public String getNewsHostUrl(){
			return mStrNewsHostUrl;
		}
		
		synchronized public void setNewsHostUrl(String strURL){
			mStrNewsHostUrl = strURL;
		}
		
		public String getUpdateHostUrl(){
			return mStrUpdateHostUrl;
		}
		
		synchronized public void setUpdateHostUrl(String strURL){
			mStrUpdateHostUrl = strURL;
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
		
		public String getPairToken(){
			return mStrPairToken;
		}
		
		synchronized public void setPairToken(String strToken){
			mStrPairToken = strToken;
		}
		
		public boolean getIsCertificated(){
			return mbIsCertificated;
		}
		
		public void setIsCertificated(boolean bIsCertificated){
			mbIsCertificated = bIsCertificated;
		}
		
		public boolean getIsCamSWUpdateSuspended(){
			return mbIsCamSWUpdateSuspended;
		}
		
		public void setIsCamSWUpdateSuspended(boolean bIsCamSWUpdateSuspended){
			mbIsCamSWUpdateSuspended = bIsCamSWUpdateSuspended;
		}
		
		public boolean getIsTrustDev(){
			return mbIsTrustDev;
		}
		
		public void setIsTrustDev(boolean bIsTrustDev){
			mbIsTrustDev = bIsTrustDev;
		}
		
		public boolean getIsShowNotificationFromToast(){
			return mbIsShowNotificationFromToast;
		}
		
		public void setIsShowNotificationFromToast(boolean bIsShowNotificationFromToast){
			mbIsShowNotificationFromToast = bIsShowNotificationFromToast;
		}
		
		public SERVER_MODE getServerMode(){
			return mServerMode;
		}
		
		public void setServerMode(SERVER_MODE mode){
			mServerMode = mode;
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
		
		public String getDetachHWID(){
			return mStrDetachHWID;
		}
		
		synchronized public void setDetachHWID(String strDetachHWID){
			mStrDetachHWID = strDetachHWID;
		}
		
		public String getSignupEmail(){
			return mStrSignupEmail;
		}
		
		synchronized public void setSignupEmail(String strSignupEmail){
			mStrSignupEmail = strSignupEmail;
		}
		
		public String getDebugStreamPath(){
			return mStrDebugStreamPath;
		}
		
		synchronized public void setDebugStreamPath(String strDebugStreamPath){
			mStrDebugStreamPath = strDebugStreamPath;
		}
		
		
		public int getVPCNumber(){
			return miVPCno;
		}
		
		synchronized public void setVPCNumber(int iVPCno){
			miVPCno = (0 >= iVPCno)?1:iVPCno;
		}
	
		@Override
		public void writeToParcel(Parcel dest, int flags) {
	 
			// We just need to write each field into the
			// parcel. When we read from parcel, they
			// will come back in the same order
			dest.writeString(mStrHostUrl);
			dest.writeString(mStrVPCHostUrl);
			dest.writeString(mStrMMHostUrl);
			dest.writeString(mStrIMPMMHostUrl);
			dest.writeString(mStrCamHostUrl);
			dest.writeString(mStrNSHostUrl);
			dest.writeString(mStrCamWsHostUrl);
			dest.writeString(mStrWSHostUrl);
			dest.writeString(mStrWSAHostUrl);
			dest.writeString(mStrNewsHostUrl);
			dest.writeString(mStrUpdateHostUrl);
			
			dest.writeString(mStrUserid);
			dest.writeString(mStrAccount);
			dest.writeString(mStrDomain);
			dest.writeString(mStrToken);
			dest.writeString(mStrOwnerInfo);
			dest.writeString(mStrPairToken);
			
			dest.writeInt(mbIsCertificated?1:0);
			dest.writeInt(mbIsCamSWUpdateSuspended?1:0);
			dest.writeInt(mbIsTrustDev?1:0);
			dest.writeInt(mbIsShowNotificationFromToast?1:0);
			dest.writeInt(mServerMode.ordinal());
			
			dest.writeLong(mlCamUpdateTs);
			dest.writeString(mStrCamUpdateList);
			dest.writeString(mStrDetachHWID);
			dest.writeString(mStrSignupEmail);
			dest.writeString(mStrDebugStreamPath);
			
			dest.writeInt(miVPCno);
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
			mStrVPCHostUrl = in.readString();
			mStrMMHostUrl = in.readString();
			mStrIMPMMHostUrl = in.readString();
			mStrCamHostUrl = in.readString();
			mStrNSHostUrl = in.readString();
			mStrCamWsHostUrl = in.readString();
			mStrWSHostUrl = in.readString();
			mStrWSAHostUrl = in.readString();
			mStrNewsHostUrl = in.readString();
			mStrUpdateHostUrl = in.readString();
			
			mStrUserid = in.readString();
			mStrAccount = in.readString();
			mStrDomain = in.readString();
			mStrToken = in.readString();
			mStrOwnerInfo = in.readString();
			mStrPairToken = in.readString();
			
			mbIsCertificated = in.readInt()>0?true:false;
			mbIsCamSWUpdateSuspended = in.readInt()>0?true:false;
			mbIsTrustDev = in.readInt()>0?true:false;
			mbIsShowNotificationFromToast = in.readInt()>0?true:false;
			mServerMode = SERVER_MODE.translateToMode(in.readInt());
			
			mlCamUpdateTs = in.readLong();
			mStrCamUpdateList = in.readString();
			mStrDetachHWID = in.readString();
			mStrSignupEmail = in.readString();
			mStrDebugStreamPath = in.readString();
			
			miVPCno = in.readInt();
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
