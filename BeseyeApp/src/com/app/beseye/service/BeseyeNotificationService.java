package com.app.beseye.service;
import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.app.beseye.CameraListActivity;
import com.app.beseye.CameraViewActivity;
import com.app.beseye.GCMIntentService;
import com.app.beseye.OpeningPage;
import com.app.beseye.R;
import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.httptask.BeseyeNotificationBEHttpTask;
import com.app.beseye.httptask.BeseyePushServiceTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.httptask.SessionMgr.SessionData;
import com.app.beseye.receiver.BaiduPushReceiver;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeSharedPreferenceUtil;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.websockets.WebsocketsMgr;
import com.app.beseye.websockets.WebsocketsMgr.OnWSChannelStateChangeListener;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class BeseyeNotificationService extends Service implements com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback,
																  OnWSChannelStateChangeListener,
																  OnNetworkChangeCallback{
	
	/** For showing and hiding our notification. */
	private NotificationManager mNotificationManager;
	private static final int NOTIFICATION_TYPE_BASE = 0x0;
	private static final int NOTIFICATION_TYPE_INFO = NOTIFICATION_TYPE_BASE+1;
	private static final int NOTIFICATION_TYPE_MSG  = NOTIFICATION_TYPE_INFO;//NOTIFICATION_TYPE_BASE+2;
	private static final int NOTIFICATION_TYPE_CAM  = NOTIFICATION_TYPE_BASE+2;
	
	
	public static final String MSG_REF_JSON_OBJ 	= "MSG_REF_JSON_OBJ";
	public static final String MSG_DEL_PUSH_RET 	= "MSG_DEL_PUSH_RET";
	
	public static final String MSG_WS_EXTEND 		= "MSG_WS_EXTEND";
	
	/** Keeps track of all current registered clients. */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    public static final int MSG_REGISTER_CLIENT = 1;
    
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    public static final int MSG_UNREGISTER_CLIENT 			= 2;
    public static final int MSG_CHECK_NOTIFY_NUM 			= 3;
    public static final int MSG_SET_NEWS_NUM 				= 4;
    public static final int MSG_QUERY_NEWS_NUM 				= 5;
    public static final int MSG_SHOW_NOTIFICATION 			= 6;
    public static final int MSG_CHECK_CACHE_STATE 			= 7;
    public static final int MSG_CHECK_UNREAD_NEWS_NUM		= 8;
    public static final int MSG_SET_UNREAD_NEWS_NUM 		= 9;
    public static final int MSG_APP_TO_FOREGROUND 			= 10;
    public static final int MSG_APP_TO_BACKGROUND 			= 11;
    
    public static final int MSG_UPDATE_SESSION_DATA 		= 12;
    public static final int MSG_UPDATE_PREF_DATA 			= 13;
    
    public static final int MSG_GCM_REGISTER 				= 14;
    public static final int MSG_GCM_UNREGISTER 				= 15;
    public static final int MSG_GCM_MSG 					= 16;
    public static final int MSG_GCM_ERR 					= 17;
    public static final int MSG_CHECK_DIALOG 			    = 18;
    
    public static final int MSG_CAM_SETTING_UPDATED 		= 19;
    public static final int MSG_CAM_WIFI_CONFIG_CHANGED 	= 20;
    
    public static final int MSG_CAM_ATTACH 					= 21;
    public static final int MSG_CAM_DETACH 					= 22;
    public static final int MSG_CAM_PLAN_CHANGED 			= 23;
    
    public static final int MSG_ACCOUNT_TOKEN_EXPIRED 		= 24;
    public static final int MSG_ACCOUNT_PW_CHANGED 			= 25;
    
    public static final int MSG_EVENT_DETECTION 			= 26;
    public static final int MSG_CAM_ACTIVATE 				= 27;
    
    public static final int MSG_EXTEND_WS_CONN 				= 28;//extend for receive cam activate ws event
    
    public static final int MSG_CAM_DEACTIVATE 				= 29;
    public static final int MSG_CAM_ONLINE 					= 30;
    public static final int MSG_CAM_OFFLINE 				= 31;
    
    public static final int MSG_CAM_EVENT_PEOPLE 			= 32;
    public static final int MSG_CAM_EVENT_MOTION 			= 33;
    public static final int MSG_CAM_EVENT_OFFLINE 			= 34;
    
    public static final int MSG_UPDATE_PLAYER_VCAM 			= 35;
    public static final int MSG_USER_PW_CHANGED 			= 36;
    
    public static final int MSG_REQUEST_DEL_PUSH 			= 37;
    public static final int MSG_RESPOND_DEL_PUSH 			= 38;
    
    public static final int MSG_BAIDU_REGISTER 				= 39;
    public static final int MSG_BAIDU_UNREGISTER 			= 40;
    public static final int MSG_BAIDU_MSG 					= 41;
    
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
//        	if(BeseyeConfig.DEBUG)
//        		Log.i(TAG, "BG service detects "+msg.toString());
        	
        	switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_CAM_WIFI_CONFIG_CHANGED:{
                	JSONObject msgObj;
					try {
						msgObj = new JSONObject((String)msg.obj);
						JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
	            		if(null != objCus){
	            			if(DEBUG)
	            				Log.i(TAG, "MSG_CAM_SETTING_UPDATED,  objCus = "+objCus.toString());
	            			int iConfigReport = BeseyeJSONUtil.getJSONInt(objCus, BeseyeJSONUtil.PS_WIFI_CONFIG_REPORT);
	            			Toast.makeText(BeseyeNotificationService.this, getString(0 == iConfigReport?R.string.cam_setting_succeed_to_apply_wifi_setting:R.string.cam_setting_fail_to_apply_wifi_setting), Toast.LENGTH_LONG).show();
	            		}	
					} catch (JSONException e) {
						Log.i(TAG, "MSG_CAM_SETTING_UPDATED,  e = "+e.toString());
					}
                }
                case MSG_CAM_SETTING_UPDATED:
                case MSG_CAM_ACTIVATE:
                case MSG_CAM_DEACTIVATE:
                case MSG_CAM_ONLINE:
                case MSG_CAM_OFFLINE:
                case MSG_CAM_EVENT_PEOPLE:
                case MSG_CAM_EVENT_MOTION:
                case MSG_CAM_EVENT_OFFLINE:{
                	for (int i=mClients.size()-1; i>=0; i--) {
                		try {
                			Bundle b = new Bundle();
                			b.putString(MSG_REF_JSON_OBJ, (String)msg.obj);
                			Message msgToSend = Message.obtain(null, msg.what);
                			msgToSend.setData(b);
                			mClients.get(i).send(msgToSend);
                		} catch (RemoteException e) {
                			// The client is dead.  Remove it from the list;
                			// we are going through the list from back to front
                			// so this is safe to do inside the loop.
                			mClients.remove(i);
                		}
                	}
                	break;
                }
                case MSG_USER_PW_CHANGED:{
                	for (int i=mClients.size()-1; i>=0; i--) {
                		try {
                			Bundle b = new Bundle();
                			b.putString(MSG_REF_JSON_OBJ, (String)msg.obj);
                			Message msgToSend = Message.obtain(null, msg.what);
                			msgToSend.setData(b);
                			mClients.get(i).send(msgToSend);
                			break;
                		} catch (RemoteException e) {
                			// The client is dead.  Remove it from the list;
                			// we are going through the list from back to front
                			// so this is safe to do inside the loop.
                			mClients.remove(i);
                		}
                	}
                	break;
                }
                case MSG_CHECK_CACHE_STATE:{
                	if(-1 != msg.arg1){
                		if(BeseyeStorageAgent.doCheckCacheSize(BeseyeNotificationService.this.getApplicationContext())){
                			sendMessageDelayed(Message.obtain(null,MSG_CHECK_CACHE_STATE,0,0), getTimeToCheck());
                		}else{
                			sendMessageDelayed(Message.obtain(null,MSG_CHECK_CACHE_STATE,0,0), 1000*60*60);//if false, check it 1 hr later
                		}
                	}
                	break;
                }
                case MSG_APP_TO_FOREGROUND:{
                	if(mbAppInBackground){
                		if(DEBUG)
                			Log.i(TAG, "BG service detects MSG_APP_TO_FOREGROUND()");
                		mbAppInBackground = false;
                		beginToCheckWebSocketState();
                		finishToCheckPushMsgState();
                	}
                	
//                	if(shouldPullMsg())
//            			sendMessageDelayed(Message.obtain(null,MSG_CHECK_UNREAD_MSG_NUM,0,0), 1*1000L);
                	mbAppInBackground = false;
                	
                	checkUserLoginState();
                	break;
                }
                case MSG_APP_TO_BACKGROUND:{
                	if(!mbAppInBackground){
                		if(DEBUG)
                			Log.i(TAG, "BG service detects MSG_APP_TO_BACKGROUND()");
                		finishToCheckWebSocketState();
                		beginToCheckPushMsgState();
                	}
                	mbAppInBackground = true;
                	checkUserLoginState();
                	BeseyeStorageAgent.doCheckCacheSize(BeseyeNotificationService.this.getApplicationContext());
                	break;
                }
                case MSG_UPDATE_SESSION_DATA:{
                	final Bundle bundle = msg.getData();
                	bundle.setClassLoader(getClassLoader());
                	boolean bLoginBefore = SessionMgr.getInstance().isUseridValid();
                	
                	SessionData sessionData = (SessionData) bundle.getParcelable("SessionData");
                	if(null != sessionData){
                		SessionMgr.getInstance().setSessionData(sessionData);
                		//If logout
                		if(bLoginBefore && (!SessionMgr.getInstance().isUseridValid() || null == SessionMgr.getInstance().getAccount() || SessionMgr.getInstance().getAccount().length() == 0)){
                			if(DEBUG)
                				Log.i(TAG, "BG service detects MSG_UPDATE_SESSION_DATA() and reset");
                			
                			sendMessage(Message.obtain(null,MSG_SET_NEWS_NUM,0,0));
                			sendMessage(Message.obtain(null,MSG_SET_UNREAD_NEWS_NUM,0,0));
                			
                			if(SessionMgr.getInstance().getServerMode() == SERVER_MODE.MODE_CHINA_STAGE) {        	
            					unregisterBaiduService();
            				} else {
            					unregisterGCMServer();
            				}

                			cancelNotification();
                			
                			BeseyeStorageAgent.doDeleteCache(BeseyeNotificationService.this.getApplicationContext());
                		}//If Login
                		else if(!bLoginBefore && SessionMgr.getInstance().isUseridValid()){
                			if(null == mGetVCamListTask){
                				(mGetVCamListTask = new BeseyeAccountTask.GetVCamListTask(BeseyeNotificationService.this)).execute();
                			}
                			
                			initPushVarialbes();
                			
                			if(SessionMgr.getInstance().getServerMode() == SERVER_MODE.MODE_CHINA_STAGE) {        	
                				checkBaiduService();
                	        } else {
                	        	checkGCMService();
                	        }
                		}
                		checkUserLoginState();
                	}
                	break;
                }
                case MSG_EXTEND_WS_CONN:{
                	final Bundle bundle = msg.getData();
                	long lTimeToExtend = bundle.getLong(MSG_WS_EXTEND);
                	if(lTimeToExtend > 0){
                		if(DEBUG)
                			Log.i(TAG, "MSG_EXTEND_WS_CONN, lTimeToExtend : "+lTimeToExtend);
                		
                		mlTimeToCloseWs = System.currentTimeMillis() + lTimeToExtend;
                		postToCloseWs(lTimeToExtend);
                	}
                	break;
                }
                case MSG_UPDATE_PREF_DATA:{
//                	final Bundle bundle = msg.getData();
//                	bundle.setClassLoader(getClassLoader());
//                	iKalaSettingsMgr.SettingData settingData = (iKalaSettingsMgr.SettingData) bundle.getParcelable("SettingData");
//                	iKalaSettingsMgr.getInstance().setSettingData(settingData);
                	break;
                }
                case MSG_GCM_REGISTER:{
                	mbRegisterLocalPushSerivce = true;
                	final Bundle bundle = msg.getData();
                	storeRegistrationId(BeseyeNotificationService.this, bundle.getString(PUSH_SERVICE_REG_ID));
                	registerPushServer();
                	break;
                }
                case MSG_GCM_UNREGISTER:{
                	mbRegisterLocalPushSerivce = false;
            		if(mbRegisterReceiver){
                		mbRegisterReceiver = false;
                		unregisterReceiver(mHandleGCMMessageReceiver);
                	}
                	unregisterPushServer();
                	break;
                }
                case MSG_BAIDU_REGISTER:{
                	
                	if(false == mbRegisterLocalPushSerivce) {
	                	
	                	final Bundle bundle = msg.getData();
	                	String BaiduUserID = bundle.getString(BaiduPushReceiver.BAIDU_USER_ID);
	                	String BaiduChannelID = bundle.getString(BaiduPushReceiver.BAIDU_CHANNEL_ID);
	                	if(DEBUG)
	                		Log.i(TAG, "MSG_BAIDU_REGISTER BaiduUserID " + BaiduUserID + ", BaiduChannelID " + BaiduChannelID);
	         
	                	if(null != BaiduUserID && null != BaiduChannelID){
	                		mStrBaiduChannelId = BaiduChannelID;
	                		mStrBaiduUserId = BaiduUserID;
	                		mbRegisterLocalPushSerivce = true;
	                		registerBaiDuPushServer();
	                	} else{
	                		if(DEBUG)
	        	    			Log.e(TAG, "registerPushServer(), invalid BaiduUserID " +  BaiduUserID + " or BaiduChannelID " + BaiduChannelID);
	                	}
                	}
                	break;
                }
                case MSG_BAIDU_UNREGISTER:{
                	if(DEBUG)
                		Log.i(TAG, "MSG_BAIDU_UNREGISTER");
                	
            		mbRegisterLocalPushSerivce = false;
            		mbBaiduApiKey = false;
            		if(mbRegisterReceiver){
                		mbRegisterReceiver = false;
                		unregisterReceiver(mHandleGCMMessageReceiver);
                	}
                	unregisterBaiduPushServer();
                	break;
                }
                case MSG_BAIDU_MSG:
                case MSG_GCM_MSG:{
                	final Bundle bundle = msg.getData();
                	String data = bundle.getString(PS_REGULAR_DATA);
                	String dataCus = bundle.getString(PS_CUSTOM_DATA);
                	if(DEBUG)
                		Log.d(TAG, "MSG_GSM/BAIDU_MSG, data : "+data+", dataCus : "+dataCus);

                	handleGCMEvents(data, dataCus);
                	break;
                }
                case MSG_GCM_ERR:{
                	break;
                }
                case MSG_CHECK_DIALOG:{
                	//checkAndCloseDialog();
                }
                case MSG_UPDATE_PLAYER_VCAM:{
                	final Bundle bundle = msg.getData();
                	mStrFocusVCamId = bundle.getString("VCAMID");
                	if(DEBUG)
                		Log.i(TAG, "mStrFocusVCamId : "+mStrFocusVCamId);
                	break;
                }
                case MSG_REQUEST_DEL_PUSH:{
                	if(DEBUG)
    					Log.i(TAG, "receive MSG_REQUEST_DEL_PUSH");
                	
        			if(SessionMgr.getInstance().getServerMode() == SERVER_MODE.MODE_CHINA_STAGE) {        	
        				unregisterBaiduPushServer();
        			} else { 
        				unregisterPushServer();
        			}
                	break;
                }
                case MSG_RESPOND_DEL_PUSH:{
                	for (int i=mClients.size()-1; i>=0; i--) {
                		try {
                			Bundle b = new Bundle();
                			b.putBoolean(MSG_DEL_PUSH_RET, (Boolean)msg.obj);
                			Message msgToSend = Message.obtain(null, msg.what);
                			msgToSend.setData(b);
                			mClients.get(i).send(msgToSend);
                		} catch (RemoteException e) {
                			// The client is dead.  Remove it from the list;
                			// we are going through the list from back to front
                			// so this is safe to do inside the loop.
                			mClients.remove(i);
                		}
                	}
                	break;
                }
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    static private final int CACHE_CHECK_TIME = 4;//AM 4:00
    private long getTimeToCheck(){
    	long lRet = 0;
    	Date now = new Date();
    	if(null != now){
    		//Log.d(TAG, "getTimeToCheck(), now : "+now);
    		Calendar calendar = Calendar.getInstance();
    		calendar.setTimeInMillis (now.getTime());
    		int iHour = calendar.get(Calendar.HOUR_OF_DAY);
    		int iMin = calendar.get(Calendar.MINUTE);
    		int iSec = calendar.get(Calendar.SECOND);
    		if(iHour < CACHE_CHECK_TIME){
    			lRet = Math.abs(((60*60*(CACHE_CHECK_TIME - iHour))-(60*iMin+iSec))*1000L);
    		}else if(iHour == CACHE_CHECK_TIME){
    			if(0 == iMin && 0 == iSec){
    				lRet = 0;
    			}else{
    				lRet = Math.abs(((60*60*24)-(60*iMin+iSec))*1000L);
    			}
    		}else{
    			lRet = Math.abs(((60*60*(24+CACHE_CHECK_TIME-iHour))-(60*iMin+iSec))*1000L);
    		}
    	}
    	if(DEBUG)
    		Log.i(TAG, "getTimeToCheck(), lRet : "+lRet);
    	return lRet;
    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
	
	//static private final String PUSH_SERVICE_PREF 				= "beseye_push_service";
	static public  final String PUSH_SERVICE_REG_ID 			= "beseye_push_service_reg_id";
	static public  final String PUSH_SERVICE_CHANNEL_ID			= "beseye_push_channel_id";  
	
	private String mStrFocusVCamId = null;//current vcam id on player 
	
	private boolean mbRegisterLocalPushSerivce = false;
	private boolean mbRegisterPushServer = false;
	private boolean mbBaiduApiKey = false;
	private String mStrBaiduChannelId = null;
	private String mStrBaiduUserId = null;
	private String mStrBaiduApiKey = null;
	
	//private SharedPreferences mPref = null;
	private boolean mbRegisterReceiver = false;
	private BeseyePushServiceTask.GetProjectIDTask mGetProjectIDTask = null;
	private BeseyePushServiceTask.GetBaiduApiKeyTask mGetBaiduApiKeyTask = null;
	private long mlTimeToCloseWs = -1;
	private Runnable mCloseWsRunnable = null;
	private boolean mbAppInBackground = true;
	
    @Override
    public void onCreate() {
    	if(DEBUG)
    		Log.i(TAG, "###########################  BeseyeNotificationService::onCreate(), this:"+this);
 	
    	if(null == SessionMgr.getInstance()){
    		SessionMgr.createInstance(getApplicationContext());
    	}    		
    	mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        
        try {
			mMessenger.send(Message.obtain(null,MSG_CHECK_CACHE_STATE,0,0));
		} catch (RemoteException e) {
			e.printStackTrace();
		}
        
        if(SessionMgr.getInstance().getServerMode() == SERVER_MODE.MODE_CHINA_STAGE) {        	
        	checkBaiduService();
        } else {
        	mGCMInstance = GoogleCloudMessaging.getInstance(this);
        	checkGCMService();
        }

        WebsocketsMgr.getInstance().registerOnWSChannelStateChangeListener(this);
        checkUserLoginState();
        beginToCheckPushMsgState();
    }
    
    private void initPushVarialbes(){
		Log.i(TAG, "BeseyeNotificationService::initPushVarialbes()");
    	mbRegisterLocalPushSerivce = false;
    	mbRegisterPushServer = false;
    	mbBaiduApiKey = false;
    	mStrBaiduChannelId = null;
    	mStrBaiduUserId = null;
    	mStrBaiduApiKey = null;
    }
    
    //Periodical check to avoid specific fail case 
	//Step 1: get SenderId/api-key
	//Step 2: register local push service and get reg id/channel id
	//Step 3: register Push server via reg id/channel id
    private void checkBaiduService(){	
    	if(SessionMgr.getInstance().isTokenValid()){
    		if(false == mbBaiduApiKey){
    			if(null == mGetBaiduApiKeyTask){
    				(mGetBaiduApiKeyTask = new BeseyePushServiceTask.GetBaiduApiKeyTask(this)).execute();
    			}
    		}else if(false == mbRegisterLocalPushSerivce){
    			if(null != mStrBaiduApiKey) {
    				PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY, mStrBaiduApiKey);
    			}else {
    				mbBaiduApiKey = false;
    			}
    		}else if(false == mbRegisterPushServer){
    			registerBaiDuPushServer();
    		}
    	}
    }
    
    private void checkGCMService(){  
        if(SessionMgr.getInstance().isTokenValid()){
     	   if(false == mbRegisterLocalPushSerivce){
 			   registerGCMServer();
 		   }else if(false == mbRegisterPushServer){
 			   registerPushServer();
 		   }
        }
     }
    
    private BeseyeMMBEHttpTask.GetIMPEventListTask mGetIMPEventListTask;
    
    private void checkUserLoginState(){
    	if(DEBUG)
    		Log.i(TAG, "checkUserLoginState(), ["+mbAppInBackground+", "+SessionMgr.getInstance().isTokenValid()+", "+WebsocketsMgr.getInstance().isWSChannelAlive()+", "+NetworkMgr.getInstance().isNetworkConnected()+"]");
    	
    	if(false == mbAppInBackground && SessionMgr.getInstance().isTokenValid() /*&& SessionMgr.getInstance().getIsCertificated()*/){
    		if(NetworkMgr.getInstance().isNetworkConnected()){
    			if(false == WebsocketsMgr.getInstance().isWSChannelAlive()){
    				if("".equals(SessionMgr.getInstance().getWSHostUrl())){
    					new BeseyeNotificationBEHttpTask.GetWSServerTask(this).execute();
    				}else{
    					WebsocketsMgr.getInstance().setWSServerIP(SessionMgr.getInstance().getWSHostUrl());
    					WebsocketsMgr.getInstance().destroyWSChannel();
        				WebsocketsMgr.getInstance().constructWSChannel();
    				}
    			}else if(true ==  WebsocketsMgr.getInstance().checkLastTimeToGetKeepAlive()){
    				Log.e(TAG, "Too long to receive keepalive");
    		
    				WebsocketsMgr.getInstance().destroyWSChannel();
    			}
    		}
    	}else{
    		postToCloseWs((-1 != mlTimeToCloseWs && mlTimeToCloseWs >= System.currentTimeMillis())?(mlTimeToCloseWs - System.currentTimeMillis()):0);
    	}
    	
    	if(SessionMgr.getInstance().getServerMode() == SERVER_MODE.MODE_CHINA_STAGE) {        	
        	checkBaiduService();
        } else {
        	checkGCMService();
        }
    }
    
    private void postToCloseWs(final long lTimeToClose){
    	if(DEBUG)
    		Log.i(TAG, "postToCloseWs(), lTimeToClose= "+lTimeToClose);

    	if(null != mCloseWsRunnable){
    		BeseyeUtils.removeRunnable(mCloseWsRunnable);
    		mCloseWsRunnable = null;
    	}
    	
    	mCloseWsRunnable = new Runnable(){
			@Override
			public void run() {
				if(WebsocketsMgr.getInstance().isWSChannelAlive())
					WebsocketsMgr.getInstance().destroyWSChannel();
				mCloseWsRunnable = null;
				mlTimeToCloseWs = -1;
			}};
		BeseyeUtils.postRunnable(mCloseWsRunnable, (0 < lTimeToClose)?lTimeToClose:0);
    }
    
    private void beginToCheckWebSocketState(){
    	BeseyeUtils.removeRunnable(mCheckWebsocketAliveRunnable);
		BeseyeUtils.postRunnable(mCheckWebsocketAliveRunnable, mbAppInBackground?60*1000:10*1000);
    }
    
    private void finishToCheckWebSocketState(){
    	BeseyeUtils.removeRunnable(mCheckWebsocketAliveRunnable);
    }
    
	private Runnable mCheckWebsocketAliveRunnable = new Runnable(){
		@Override
		public void run() {
			checkUserLoginState();
			beginToCheckWebSocketState();
		}};
    
	private void beginToCheckPushMsgState(){
    	BeseyeUtils.removeRunnable(mCheckPushMsgRunnable);
		BeseyeUtils.postRunnable(mCheckPushMsgRunnable, 60*1000);
    }
    
    private void finishToCheckPushMsgState(){
    	BeseyeUtils.removeRunnable(mCheckPushMsgRunnable);
    }
    
	private Runnable mCheckPushMsgRunnable = new Runnable(){
		@Override
		public void run() {
			if(DEBUG)
	    		Log.i(TAG, "mCheckPushMsgRunnable::run(), ["+mbAppInBackground+", "+SessionMgr.getInstance().isTokenValid()+", "+mbRegisterPushServer+", "+mbRegisterLocalPushSerivce+"]");
	    	
			beginToCheckPushMsgState();
			if(mbAppInBackground && SessionMgr.getInstance().isTokenValid()){
				if(SessionMgr.getInstance().getServerMode() == SERVER_MODE.MODE_CHINA_STAGE) {        	
					if(false == mbBaiduApiKey){
		    			if(null == mGetBaiduApiKeyTask){
		    				(mGetBaiduApiKeyTask = new BeseyePushServiceTask.GetBaiduApiKeyTask(BeseyeNotificationService.this)).execute();
		    			}
		    		}else if(false == mbRegisterLocalPushSerivce){
		    			if(null != mStrBaiduApiKey) {
		    				PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY, mStrBaiduApiKey);
		    			}else {
		    				mbBaiduApiKey = false;
		    			}
		    		}else if(false == mbRegisterPushServer){
		    			registerBaiDuPushServer();
		    		}else{
		 		   		finishToCheckPushMsgState();
		 		   	}
		        } else {
		        	if(false == mbRegisterLocalPushSerivce){
		 			   	registerGCMServer();
		 		   	}else if(false == mbRegisterPushServer){
		 		   		registerPushServer();
		 		   	}else{
		 		   		finishToCheckPushMsgState();
		 		   	}
		        }
			}else{
				finishToCheckPushMsgState();
			}
			
		}};
		
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
    	// We want this service to
    	// continue running until it is explicitly
    	// stopped, so return sticky.

		return START_STICKY;
	}
    
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
            	try {
					GooglePlayServicesUtil.getErrorPendingIntent(resultCode, this,
					        PLAY_SERVICES_RESOLUTION_REQUEST).send();
				} catch (CanceledException e) {
					e.printStackTrace();
					Log.e(TAG, "This device is not supported.e:"+e.toString());
				}
//                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
//                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
               // finish();
            }
            return false;
        }
        return true;
    }

	private void registerGCMServer(){
		try {
	    	if(null != SessionMgr.getInstance() && SessionMgr.getInstance().isUseridValid()){
	    		if(checkPlayServices()){  			
					String sSenderID = GCMIntentService.getSenderId();
					if(DEBUG)
						Log.i(TAG, "registerGCMServer(), sSenderID "+sSenderID);
					
					if(null == sSenderID || 0 == sSenderID.length()){
						if(null == mGetProjectIDTask)
							(mGetProjectIDTask = new BeseyePushServiceTask.GetProjectIDTask(this)).execute();
					}else{
						registerGCMService(sSenderID);
					}
	            }
	    	}
		}catch (UnsupportedOperationException e) {
    		Log.i(TAG, "registerGCMServer(), e: "+e.toString());
		}
    }
    
	private void unregisterBaiduService(){
		if(DEBUG)
			Log.i(TAG, "unregisterBaiduServer()");
		if(mbRegisterLocalPushSerivce){
    		mbBaiduApiKey = false;
    		PushManager.stopWork(getApplicationContext());
		}
	}
	
    private void unregisterGCMServer(){
    	if(mbRegisterReceiver){
    		mbRegisterReceiver = false;
    		unregisterReceiver(mHandleGCMMessageReceiver);
    	}
    	
    	if(mbRegisterLocalPushSerivce){
    		mbRegisterLocalPushSerivce = false;
    	    new AsyncTask<Void, Integer, String>() {
    	        @Override
    	        protected String doInBackground(Void... params) {
    	            String msg = "";
    	            try {
    	    			//GCMRegistrar.unregister(getApplicationContext());
    	    			try {
    						mGCMInstance.unregister();
    					} catch (IOException e) {
    						Log.i(TAG, "unregisterGCMServer(), e: "+e.toString());
    					}
    	    		}catch (UnsupportedOperationException e) {
    	        		Log.i(TAG, "unregisterGCMServer(), e: "+e.toString());
    	            }
    	            return msg;
    	        }

    	        @Override
    	        protected void onPostExecute(String msg) {
    	            //mDisplay.append(msg + "\n");
    	        }
    	    }.execute(null, null, null);
    	}
    }

	@Override
	public void onDestroy() {
		if(DEBUG)
			Log.d(TAG, "###########################  BeseyeNotificationService::onDestroy(), this:"+this);
		if (mRegisterPushServerTask != null) {
			mRegisterPushServerTask.cancel(true);
        }
		
		WebsocketsMgr.getInstance().unregisterOnWSChannelStateChangeListener();
		if(SessionMgr.getInstance().getServerMode() == SERVER_MODE.MODE_CHINA_STAGE) {        	
			unregisterBaiduService();
		} else {
			unregisterGCMServer();
		}
		super.onDestroy();
	}
	
	@Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
	
	public static final String PROPERTY_REG_ID = "registration_id";
	//private static final String PROPERTY_APP_VERSION = "appVersion";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	    
	private BeseyeHttpTask mRegisterPushServerTask, mUnRegisterPushServerTask;
	private GoogleCloudMessaging mGCMInstance;
	private String mStrRegistrationId = "";
	
//	private SharedPreferences getGCMPreferences(Context context) {
//	    // This sample app persists the registration ID in shared preferences, but
//	    // how you store the regID in your app is up to you.
//	    return getSharedPreferences(BeseyeNotificationService.class.getSimpleName(), Context.MODE_PRIVATE);
//	}
	
	private String getRegistrationId(Context context) {
//	    final SharedPreferences prefs = getGCMPreferences(context);
//	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
//	    if (registrationId.isEmpty()) {
//	        //Log.d(TAG, "Registration not found.");
//	        return "";
//	    }
//	    // Check if app was updated; if so, it must clear the registration ID
//	    // since the existing regID is not guaranteed to work with the new
//	    // app version.
//	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
//	    int currentVersion = getAppVersion(context);
//	    if (registeredVersion != currentVersion) {
//	        Log.d(TAG, "App version changed.");
//	        storeRegistrationId(this,"");
//	        return "";
//	    }
	    return mStrRegistrationId;
	}
	
//	private static int getAppVersion(Context context) {
//	    try {
//	        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
//	        return packageInfo.versionCode;
//	    } catch (NameNotFoundException e) {
//	        // should never happen
//	        throw new RuntimeException("Could not get package name: " + e);
//	    }
//	}
	
	private void storeRegistrationId(Context context, String regId) {
//	    final SharedPreferences prefs = getGCMPreferences(context);
//	    int appVersion = getAppVersion(context);
//	    if(DEBUG)
//	    	Log.d(TAG, "Saving regId on app version " + appVersion);
//	    SharedPreferences.Editor editor = prefs.edit();
//	    editor.putString(PROPERTY_REG_ID, regId);
//	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
//	    editor.commit();
		mStrRegistrationId = regId;
	}
	
    private void registerGCMService(String strSenderId){
    	if(SessionMgr.getInstance().isTokenValid()){
    		GCMIntentService.updateSenderId(strSenderId);
        	try{
    	    	final String regId = getRegistrationId(BeseyeNotificationService.this);//GCMRegistrar.getRegistrationId(getApplicationContext());
    	    	if(DEBUG)
    	    		Log.d(TAG, "registerGCMService(), regId: "+regId);
    	        
    	    	if(false == mbRegisterReceiver){
	    			registerReceiver(mHandleGCMMessageReceiver,new IntentFilter(GCMIntentService.FORWARD_PUSH_MSG_ACTION));
	    			mbRegisterReceiver = true;
	    		}
    	    	
    	    	if (regId.equals("")) {
    	        	// Log.i(TAG, "registerGCM(), strSenderId "+strSenderId);
    	            // Automatically registers application on startup.
    	        	registerGCMInBackground(strSenderId);
    	        } else {
    	        	registerPushServer();
    	        }
    	    }catch (UnsupportedOperationException e) {
    			Log.e(TAG, "registerGCMService(), e: "+e.toString());
    	    }
    	}
    }
    
    private void registerBaiDuPushServer(){
    	try{
    		if(false == mbRegisterPushServer && null == mRegisterPushServerTask){
    			if(null != mStrBaiduUserId && null != mStrBaiduChannelId) {
        			JSONObject obj = new JSONObject();
			        try {
						obj.put(PS_REG_DEV_NAME, Build.MODEL);
						obj.put(PS_CHANNEL_ID, mStrBaiduChannelId);
						obj.put(PS_USER_ID, mStrBaiduUserId);
					} catch (JSONException e) {
						e.printStackTrace();
					}
			        mRegisterPushServerTask = (BeseyeHttpTask) new BeseyePushServiceTask.AddBaiduIDTask(this).execute(obj.toString());
			        if(DEBUG)
			        	Log.d(TAG, "registerBaiDuPushServer(), userID: "+mStrBaiduUserId+", channelID:"+mStrBaiduChannelId);
    			} else {
    				mbRegisterLocalPushSerivce = false;
    			}
    		}
	    }catch (UnsupportedOperationException e) {
			Log.i(TAG, "registerBaiDuPushServer(), e: "+e.toString());
	    }

    }

	private void registerGCMInBackground(final String strSenderId) {
	    new AsyncTask<Void, Integer, String>() {
	        @Override
	        protected String doInBackground(Void... params) {
	            String msg = "";
	            try {
	                if (mGCMInstance == null) {
	                    mGCMInstance = GoogleCloudMessaging.getInstance(BeseyeNotificationService.this);
	                }
	                String regId = mGCMInstance.register(strSenderId);
	                msg = "Device registered, registration ID=" + regId;
	                
	                storeRegistrationId(BeseyeNotificationService.this, regId);
	                mbRegisterLocalPushSerivce = true;

	                registerPushServer();
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            if(DEBUG)
	    			Log.d(TAG, "msg:"+msg);
	            return msg;
	        }

	        @Override
	        protected void onPostExecute(String msg) {
	            //mDisplay.append(msg + "\n");
	        }
	    }.execute(null, null, null);
	    
	}
    
    private void registerPushServer(){
    	try{
	    	final String regId = getRegistrationId(this);

	    	if(null != regId && 0 < regId.length() /*&& SessionMgr.getInstance().isUseridValid()*/){
	            if (false == mbRegisterPushServer && null == mRegisterPushServerTask) {
	                // Try to register again, but not in the UI thread.
	                // It's also necessary to cancel the thread onDestroy(),
	                // hence the use of AsyncTask instead of a raw thread.
	            	JSONObject obj = new JSONObject();
	            	try {
						obj.put(PS_REG_DEV_UUID, BeseyeUtils.getAndroidUUid());
						obj.put(PS_REG_DEV_NAME, Build.MODEL);
						obj.put(PS_REG_ID, regId);
					} catch (JSONException e) {
						e.printStackTrace();
					}
	            	mRegisterPushServerTask = (BeseyeHttpTask) new BeseyePushServiceTask.AddRegisterIDTask(this).execute(obj.toString());
	            	//showRegIdNotification();
	            	if(DEBUG)
	            		Log.d(TAG, "registerPushServer(), regId: "+regId+", obj:"+obj.toString());
	            }
	    	}else{
	    		if(DEBUG)
	    			Log.e(TAG, "registerPushServer(), invalid regId or mdid "+SessionMgr.getInstance().getUserid());
	    	}
	    }catch (UnsupportedOperationException e) {
			Log.i(TAG, "registerPushServer(), e: "+e.toString());
	    }
    }
    
    private void unregisterBaiduPushServer(){
    	mbBaiduApiKey = false;
    	if(null != mStrBaiduChannelId && 0 < mStrBaiduChannelId.length() && null == mUnRegisterPushServerTask){
    		if(DEBUG)
        		Log.i(TAG, "unregisterBaiduPushServer() ChannelId: " + mStrBaiduChannelId);
      
    		JSONObject obj = new JSONObject();
        	try {
        		JSONArray arrRegIds = new JSONArray();
        		arrRegIds.put(mStrBaiduChannelId);
				obj.put(PS_CHANNEL_IDS, arrRegIds);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        	mUnRegisterPushServerTask = (BeseyeHttpTask) new BeseyePushServiceTask.DelBaiduIDTask(this).execute(obj.toString());
    	}else{
    		Log.e(TAG, "unregisterBaiduPushServer(), invalid channelId "+mStrBaiduChannelId);
    	}
    }
    
    private void unregisterPushServer(){
    	final String regId = getRegistrationId(this);
    	if(null != regId && 0 < regId.length() && null == mUnRegisterPushServerTask){
    		JSONObject obj = new JSONObject();
        	try {
        		JSONArray arrRegIds = new JSONArray();
        		arrRegIds.put(regId);
				obj.put(PS_REG_IDS, arrRegIds);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        	
        	mUnRegisterPushServerTask = (BeseyeHttpTask) new BeseyePushServiceTask.DelRegisterIDTask(this).execute(obj.toString());
    	}else{
    		Log.e(TAG, "unregisterPushServer(), invalid regId "+regId);
    	}
    }
    
    private final BroadcastReceiver mHandleGCMMessageReceiver = new BroadcastReceiver() {
    	@Override
        public void onReceive(Context context, Intent intent) {
    				 
            String action = intent.getExtras().getString(GCMIntentService.FORWARD_ACTION_TYPE);
            Message msg = null;
            if(DEBUG)
            	Log.i(TAG, "onReceive(), action "+action+", data:"+intent);
          
            if(GCMIntentService.FORWARD_ACTION_TYPE_REG.equals(action)){	
            	msg = Message.obtain(null,MSG_GCM_REGISTER,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_UNREG.equals(action)){
            	msg = Message.obtain(null,MSG_GCM_UNREGISTER,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_MSG.equals(action)){
            	msg = Message.obtain(null,MSG_GCM_MSG,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_ERR.equals(action)){
            	msg = Message.obtain(null,MSG_GCM_ERR,0,0);
            }else if(GCMIntentService.FORWARD_ACTION_TYPE_CHECK_DIALOG.equals(action)){
            	msg = Message.obtain(null,MSG_CHECK_DIALOG,0,0);
            }else if(BaiduPushReceiver.FORWARD_ACTION_TYPE_BAIDU_REG.equals(action)){
            	msg = Message.obtain(null,MSG_BAIDU_REGISTER,0,0);
            }else if(BaiduPushReceiver.FORWARD_ACTION_TYPE_BAIDU_UNREG.equals(action)){
            	msg = Message.obtain(null,MSG_BAIDU_UNREGISTER,0,0);
            }else if(BaiduPushReceiver.FORWARD_ACTION_TYPE_BAIDU_MSG.equals(action)){
            	msg = Message.obtain(null,MSG_BAIDU_MSG,0,0);	
            }else{
            	Log.e(TAG, "onReceive(), invalid action "+action);
            }
            
            try {
            	if(null != mMessenger && null != msg){
            		msg.setData(intent.getExtras());
            		mMessenger.send(msg);
            	}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
        }
    };
    
	private static int siRequestCode = (int) (System.currentTimeMillis()%100000);
	
	private String getAppName(){
		return (BeseyeConfig.ALPHA_VER?getText(R.string.app_name_alpha):
			   (BeseyeConfig.BETA_VER?getText(R.string.app_name_beta):
		       (BeseyeConfig.DEBUG?getText(R.string.app_name_dev):
		        getText(R.string.app_name)))).toString();
	}
	
	private void showNotification(int iNotifyId, Intent intent, CharSequence charSequence, CharSequence text, long lTs) {
		PendingIntent contentIntent = PendingIntent.getActivity(this, siRequestCode++ , intent, 0);
		
		//Log.i(TAG, "showNotification(), mCam_obj:"+intent.getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
		if(null != contentIntent){	         
			NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(BeseyeNotificationService.this)
			        .setContentTitle((null == charSequence)?getAppName():charSequence)  
			        .setContentText(text)
			        .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
			        .setWhen(lTs)
			        .setContentIntent(contentIntent)
			        .setDefaults(Notification.DEFAULT_VIBRATE)
			        .setAutoCancel(true)
					.setSmallIcon(R.drawable.common_app_icon)
					.setTicker(text);
			
			final Notification notification = mBuilder.build();
			mNotificationManager.cancel(iNotifyId);
	        mNotificationManager.notify(iNotifyId, notification);
		}
	}
	private void cancelNotification(){
		if(null != mNotificationManager){
			mNotificationManager.cancel(NOTIFICATION_TYPE_INFO);
			mNotificationManager.cancel(NOTIFICATION_TYPE_MSG);
			mNotificationManager.cancel(NOTIFICATION_TYPE_CAM);
			if(0 < mMapNotificationId.size()){
				for(String strVCamId : mMapNotificationId.keySet()){
					if(null != mNotificationManager){
						mNotificationManager.cancel(mMapNotificationId.get(strVCamId));
					}
				}
				mMapNotificationId.clear();
			}
			
			if(0 < mMapNCode.size()){
				mMapNCode.clear();
			}
		}
	}
	
	@Override
	public void onShowDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId, int iTitleRes, int iMsgRes) {}

	@Override
	public void onDismissDialog(AsyncTask<String, Double, List<JSONObject>> task, int iDialogId) {}

	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyePushServiceTask.GetProjectIDTask){
			Log.e(TAG, "BeseyeNotificationService::onPostExecute(), GetProjectIDTask, iErrType = "+iErrType);
		}else if(task instanceof BeseyePushServiceTask.GetBaiduApiKeyTask){
			Log.e(TAG, "BeseyeNotificationService::onPostExecute(), GetBaiduApiKeyTask, iErrType = "+iErrType);
		}else if(task instanceof BeseyePushServiceTask.AddRegisterIDTask){
			Log.e(TAG, "BeseyeNotificationService::onPostExecute(), AddRegisterIDTask, iErrType = "+iErrType);
		}else if(task instanceof BeseyePushServiceTask.AddBaiduIDTask){
			Log.e(TAG, "BeseyeNotificationService::onPostExecute(), AddBaiduIDTask, iErrType = "+iErrType);
		}else if(task instanceof BeseyePushServiceTask.DelRegisterIDTask){
			Log.e(TAG, "BeseyeNotificationService::onPostExecute(), DelRegisterIDTask, iErrType = "+iErrType);
		}else if(task instanceof BeseyePushServiceTask.DelBaiduIDTask){
			Log.e(TAG, "BeseyeNotificationService::onPostExecute(), DelBaiduIDTask, iErrType = "+iErrType + " msg = " + strMsg);
		}
		Log.e(TAG, "Service::onErrorReport(), "+task.getClass().getSimpleName()+", params:"+strMsg+", iErrType: "+iErrType);
	}
	
	@Override
	public void onSessionInvalid(AsyncTask<String, Double, List<JSONObject>> task, int iInvalidReason){
		
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(false == task.isCancelled()){
			if(task instanceof BeseyePushServiceTask.GetProjectIDTask){
				if(0 == iRetCode && null != result && 0 < result.size()){
					String senderId = BeseyeJSONUtil.getJSONString(result.get(0), PS_PORJ_NUM);
					
					if(DEBUG)
						Log.i(TAG, "BeseyeNotificationService::onPostExecute(), senderId "+senderId);
					
					if(0 < senderId.length()){
						registerGCMService(senderId);
					}else{
						Log.e(TAG, "BeseyeNotificationService::onPostExecute(), GetProjectIDTask, invalid senderId ");
					}
				}else if(BeseyeError.E_BE_ACC_SESSION_NOT_FOUND == iRetCode){
					Log.i(TAG, "BeseyeNotificationService::onPostExecute(), E_BE_ACC_SESSION_NOT_FOUND ");
					SessionMgr.getInstance().cleanSession();
				}
			}else if(task instanceof BeseyePushServiceTask.GetBaiduApiKeyTask){
				if(0 == iRetCode && null != result && 0 < result.size()){
					String apiKey = BeseyeJSONUtil.getJSONString(result.get(0), PS_BAIDU_API_KEY);
					if(DEBUG)
						Log.i(TAG, "BeseyeNotificationService::onPostExecute(), apiKey "+apiKey);
					if(null != apiKey && 0 < apiKey.length()){
						mStrBaiduApiKey = apiKey;
						PushManager.startWork(getApplicationContext(), PushConstants.LOGIN_TYPE_API_KEY, mStrBaiduApiKey);
						if(false == mbRegisterReceiver){
			    			registerReceiver(mHandleGCMMessageReceiver,new IntentFilter(GCMIntentService.FORWARD_PUSH_MSG_ACTION));
			    			mbRegisterReceiver = true;
			    		}
			        	mbBaiduApiKey = true;
					}else{
						Log.e(TAG, "BeseyeNotificationService::onPostExecute(), GetBaiduApiKeyTask, invalid apiKey "+ apiKey);
					}
				}else if(BeseyeError.E_BE_ACC_SESSION_NOT_FOUND == iRetCode){
					Log.i(TAG, "BeseyeNotificationService::onPostExecute(), E_BE_ACC_SESSION_NOT_FOUND ");
					SessionMgr.getInstance().cleanSession();
				}
			}else if(task instanceof BeseyePushServiceTask.AddRegisterIDTask){
				if(0 == iRetCode || 2 == iRetCode && null != result && 0 < result.size()){
					if(DEBUG)
						Log.i(TAG, "BeseyeNotificationService::onPostExecute(), AddRegisterIDTask OK");
					mbRegisterPushServer = true;
				}else if(BeseyeError.E_BE_PUSH_ALREADY_REGISTER == iRetCode){
					Log.i(TAG, "BeseyeNotificationService::onPostExecute(), E_BE_PUSH_ALREADY_REGISTER");
					mbRegisterPushServer = true;
				}
			}else if(task instanceof BeseyePushServiceTask.AddBaiduIDTask){
				if(0 == iRetCode || 2 == iRetCode && null != result && 0 < result.size()){
					if(DEBUG)
						Log.i(TAG, "BeseyeNotificationService::onPostExecute(), AddBaiduIDTask OK");
					mbRegisterPushServer = true;
				}else if(BeseyeError.E_BE_PUSH_ALREADY_REGISTER == iRetCode){
					Log.i(TAG, "BeseyeNotificationService::onPostExecute(), E_BE_PUSH_ALREADY_REGISTER");
					mbRegisterPushServer = true;
				}
			}else if(task instanceof BeseyePushServiceTask.DelRegisterIDTask){
				if(0 == iRetCode && null != result && 0 < result.size()){
					if(DEBUG)
						Log.i(TAG, "BeseyeNotificationService::onPostExecute(), DelRegisterIDTask OK");
					
					storeRegistrationId(this, "");
					mbRegisterPushServer = false;
			
					//Broadcast to Main process
					if(null != mMessenger){
						try {
							mMessenger.send(Message.obtain(null, MSG_RESPOND_DEL_PUSH, 0 == iRetCode?Boolean.TRUE:Boolean.FALSE));
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				}
			}else if(task instanceof BeseyePushServiceTask.DelBaiduIDTask){
				if(0 == iRetCode && null != result && 0 < result.size()){
					if(DEBUG)
						Log.i(TAG, "BeseyeNotificationService::onPostExecute(), DelBaiduIDTask OK");
					mbBaiduApiKey = false;
					mbRegisterPushServer = false;
					mStrBaiduChannelId = null;
				}
				//Broadcast to Main process
				if(null != mMessenger){
					try {
						mMessenger.send(Message.obtain(null, MSG_RESPOND_DEL_PUSH, 0 == iRetCode?Boolean.TRUE:Boolean.FALSE));
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				}
			}else if(task instanceof BeseyeNotificationBEHttpTask.GetWSServerTask){
				if(0 == iRetCode && null != result && 0 < result.size()){
					JSONArray arr = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.OBJ_DATA);
					try {
						WebsocketsMgr.getInstance().setWSServerIP("http://"+arr.getString(arr.length()-1));
						if(NetworkMgr.getInstance().isNetworkConnected()){
							WebsocketsMgr.getInstance().constructWSChannel();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					JSONObject objVCamList = result.get(0);
					fillVCamList(objVCamList);
					BeseyeAccountTask.GetVCamListTask accTask = (BeseyeAccountTask.GetVCamListTask)task;
					String strCheckId = accTask.getVCamIdCheck();
					if(null != strCheckId){
						if(false == mMapNotificationId.containsKey(strCheckId)){
							mListBlackVCamId.add(strCheckId);
							if(DEBUG)
								Log.i(TAG, "BeseyeNotificationService::onPostExecute(), add "+strCheckId+" into mListBlackVCamId");
						}else{
							handleNotificationEvent(accTask.getMsgObj(), accTask.getFromPsuh());
						}
					}
				}
			}
		}
		if(task == mRegisterPushServerTask){
			mRegisterPushServerTask = null;
		}else if(task == mGetProjectIDTask){
			mGetProjectIDTask = null;
		}else if(task == mGetVCamListTask){
			mGetVCamListTask = null;
		}else if(task == mUnRegisterPushServerTask){
			mUnRegisterPushServerTask = null;
		}else if(task == mGetBaiduApiKeyTask){
			mGetBaiduApiKeyTask = null;
		}
	}
	
	//Handle multiple beseye apps in same device and login via different accounts
	private Map<String, Integer> mMapNotificationId = new HashMap<String, Integer>();
	private Map<String, Integer> mMapNCode = new HashMap<String, Integer>();
	private List<String> mListBlackVCamId = new ArrayList<String>();//vcamid list that doesn't belong to current account
	private BeseyeHttpTask mGetVCamListTask = null;
	
	private void fillVCamList(JSONObject objVCamList){
		int iVcamCnt = BeseyeJSONUtil.getJSONInt(objVCamList, BeseyeJSONUtil.ACC_VCAM_CNT);		
		if(0 < iVcamCnt){
			JSONArray VcamList = BeseyeJSONUtil.getJSONArray(objVCamList, BeseyeJSONUtil.ACC_VCAM_LST);
			for(int i = 0;i< iVcamCnt;i++){
				try {
					JSONObject camObj = VcamList.getJSONObject(i);
					if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
						String strVCamId = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID);
						int iNotifyType = NOTIFICATION_TYPE_INFO+((null != strVCamId)?strVCamId.hashCode():0);
						if(null != strVCamId && false == mMapNotificationId.containsKey(strVCamId)){
							mMapNotificationId.put(strVCamId, iNotifyType);
						}
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void onToastShow(AsyncTask<String, Double, List<JSONObject>> task, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	static final int MAX_WS_RETRY_TIME = 100;
	private int miWSDisconnectRetry = 0;
	
	@Override
	public void onChannelConnecting() {
		if(DEBUG)
			Log.i(TAG, "ws onChannelConnecting()---");
	}
	
	@Override
	public void onAuthfailed(){
		if(DEBUG)
			Log.w(TAG, "ws onAuthfailed()---");
	}

	@Override
	public void onAuthComplete() {
		if(DEBUG)
			Log.e(TAG, "ws onAuthComplete()---");		
	}
	@Override
	public void onChannelConnected() {
		if(DEBUG)
			Log.i(TAG, "ws onChannelConnected()---");
		miWSDisconnectRetry = 0;
	}

	@Override
	public void onMessageReceived(String msg) {
		try {
			JSONObject dataObj = new JSONObject(msg);
			handleWSEvent(dataObj);
		} catch (JSONException e) {
			Log.e(TAG, "ws onMessageReceived(), failed to parse : "+msg);
		}
	}

	@Override
	public void onChannelClosed() {
		if(DEBUG)
			Log.i(TAG, "ws onChannelClosed()---!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		
		if(/*miWSDisconnectRetry < MAX_WS_RETRY_TIME && */false == mbAppInBackground && SessionMgr.getInstance().isTokenValid() /*&& NetworkMgr.getInstance().isNetworkConnected()*/){
			Log.e(TAG, "ws onChannelClosed(), abnormal close, retry-----");
			long lTimeToWait = (miWSDisconnectRetry++)*1000;
			if(lTimeToWait > 10000){
				lTimeToWait = 10000;
			}
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					if(NetworkMgr.getInstance().isNetworkConnected()){
						WebsocketsMgr.getInstance().constructWSChannel();
					}else{
						long lTimeToWait = (miWSDisconnectRetry++)*1000;
						if(lTimeToWait > 10000){
							lTimeToWait = 10000;
						}
						BeseyeUtils.postRunnable(this, lTimeToWait);
					}
				}}, lTimeToWait);
    	}
	}

	@Override
	public void onConnectivityChanged(boolean bNetworkConnected) {
		checkUserLoginState();
	}
	
	private boolean handleNotificationEvent(JSONObject msgObj, boolean bFromGCM){
		if(!SessionMgr.getInstance().isTokenValid()){
			return true;
		}
		if(DEBUG)
			Log.i(TAG, "handleNotificationEvent(), msgObj="+msgObj.toString() + ", bFromGCM=" + bFromGCM);	
		
		boolean bRet = true;
		JSONObject objReg = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_REGULAR_DATA);
		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
	
		if(null != objReg){
			int iNCode = BeseyeJSONUtil.getJSONInt(objReg, BeseyeJSONUtil.PS_NCODE);
			if(DEBUG)
				Log.i(TAG, "handleNotificationEvent(),iNCode="+iNCode);	
			
			int iMsgType = -1;
			String strNotifyMsg = null;
			String strNotifyTitle = null;
			int iNotifyType = -1;
			Intent intent = new Intent();
			long lTs = System.currentTimeMillis();
			switch(iNCode){
				case NCODE_CAM_ACTIVATE:{
					iMsgType = MSG_CAM_ACTIVATE;
					if(bFromGCM){
						strNotifyMsg = getString(R.string.toast_new_cam_activated);
						iNotifyType = NOTIFICATION_TYPE_CAM;
						
						intent.setClassName(this, OpeningPage.class.getName());
						
						Intent delegateIntent = new Intent();
						delegateIntent.setClassName(this, CameraListActivity.class.getName());
						
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(OpeningPage.KEY_DELEGATE_INTENT, delegateIntent);
						
						lTs = BeseyeJSONUtil.getJSONLong(objReg, BeseyeJSONUtil.PS_TS);
					}
					break;
				}
				case NCODE_CAM_DEACTIVATE:{
					iMsgType = MSG_CAM_DEACTIVATE;
					if(bFromGCM){
						strNotifyMsg = getString(R.string.toast_cam_deactivated);
						iNotifyType = NOTIFICATION_TYPE_CAM;
						
						intent.setClassName(this, OpeningPage.class.getName());
						
						Intent delegateIntent = new Intent();
						delegateIntent.setClassName(this, CameraListActivity.class.getName());
						
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra(OpeningPage.KEY_DELEGATE_INTENT, delegateIntent);
						
						lTs = BeseyeJSONUtil.getJSONLong(objReg, BeseyeJSONUtil.PS_TS);
					}
					
					String strVCamId = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
					if(null != strVCamId && true == mMapNotificationId.containsKey(strVCamId)){
						if(null != mNotificationManager){
							mNotificationManager.cancel(mMapNotificationId.get(strVCamId));
						}
						mMapNotificationId.remove(strVCamId);
						mMapNCode.remove(strVCamId);
					}
					
					BeseyeStorageAgent.doDeleteCacheByFolder(BeseyeNotificationService.this.getApplicationContext(), strVCamId);
					break;
				}
				case NCODE_WIFI_CHANGED:{
					iMsgType = MSG_CAM_WIFI_CONFIG_CHANGED;
					break;
				}
				case NCODE_CAM_ONLINE:{
					iMsgType = MSG_CAM_ONLINE;
					break;
				}
				case NCODE_CAM_OFFLINE:{
					iMsgType = MSG_CAM_OFFLINE;
					break;
				}
				case NCODE_PW_CHANGE:{
					iMsgType = MSG_USER_PW_CHANGED;
					break;
				}
				case NCODE_PEOPLE_DETECT:
				case NCODE_MOTION_DETECT:
				case NCODE_OFFLINE_DETECT:{
					String strVCamId = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
					if(null != strVCamId && false == mMapNotificationId.containsKey(strVCamId)){
						if(false == mListBlackVCamId.contains(strVCamId)){
							if(null != mGetVCamListTask && false == mGetVCamListTask.isCancelled()){
								mGetVCamListTask.cancel(true);
                			}
							if(DEBUG)
								Log.i(TAG, "handleNotificationEvent(),strVCamId="+strVCamId+" not in mListBlackVCamId nor mMapNotificationId");	
							(mGetVCamListTask = new BeseyeAccountTask.GetVCamListTask(BeseyeNotificationService.this, strVCamId, msgObj, bFromGCM)).execute();
						}
					}else{
						String strCamName = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_NAME);
						if(NCODE_PEOPLE_DETECT == iNCode)
							iMsgType = MSG_CAM_EVENT_PEOPLE;
						else if(NCODE_MOTION_DETECT == iNCode)
							iMsgType = MSG_CAM_EVENT_MOTION;
						else 
							iMsgType = MSG_CAM_EVENT_OFFLINE;
						
						//if(bFromGCM){
							if(NCODE_PEOPLE_DETECT == iNCode){
								strNotifyMsg = String.format(getString(R.string.notify_people_detect), strCamName);
								//strNotifyTitle = getString(R.string.cam_setting_title_notification_event_people_detect);
							}else if(NCODE_MOTION_DETECT == iNCode){
								strNotifyMsg = String.format(getString(R.string.notify_motion_detect), strCamName);
								//strNotifyTitle = getString(R.string.cam_setting_title_notification_event_motion_detect);
							}else{ 
								strNotifyMsg = String.format(getString(R.string.notify_offline_detect), strCamName);
								//strNotifyTitle = getString(R.string.cam_setting_title_notification_event_offline_detect);
							}
							
							iNotifyType = NOTIFICATION_TYPE_INFO+((null != strVCamId)?strVCamId.hashCode():0);
							if(null != strVCamId && false == mMapNotificationId.containsKey(strVCamId)){
								mMapNotificationId.put(strVCamId, iNotifyType);
								mMapNCode.put(strVCamId, iNCode);
							}
							
							if(NCODE_OFFLINE_DETECT != iNCode){
								if(null != strVCamId && !strVCamId.equals(mStrFocusVCamId)){
									JSONObject cam_obj = new JSONObject();
									BeseyeJSONUtil.setJSONString(cam_obj, BeseyeJSONUtil.ACC_ID, strVCamId);
								    BeseyeJSONUtil.setJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME, strCamName);
								        
								   // iNotifyType = NOTIFICATION_TYPE_INFO+((null != strVCamId)?strVCamId.hashCode():0);
									
									lTs = BeseyeJSONUtil.getJSONLong(objCus, BeseyeJSONUtil.PS_EVT_TS);
									
									Intent delegateIntent = new Intent();
									delegateIntent.setClassName(this, CameraViewActivity.class.getName());
									delegateIntent.putExtra(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
									delegateIntent.putExtra(CameraViewActivity.KEY_DVR_STREAM_MODE, true);
									
									JSONObject tsInfo = new JSONObject();
									BeseyeJSONUtil.setJSONLong(tsInfo, BeseyeJSONUtil.MM_START_TIME, lTs);
									delegateIntent.putExtra(CameraViewActivity.KEY_TIMELINE_INFO, tsInfo.toString());
									
									intent.setClassName(this, OpeningPage.class.getName());
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//									intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
									intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
									intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
									intent.putExtra(OpeningPage.KEY_DELEGATE_INTENT, delegateIntent);	
									intent.putExtra(OpeningPage.KEY_EVENT_FLAG, true);
								}else{
									strNotifyMsg = null;
									if(DEBUG)
										Log.i(TAG, "handleNotificationEvent(),match mStrFocusVCamId="+mStrFocusVCamId);	
								}
							}
						//}
					}
					
					break;
				}
				default:{
					bRet = false;
					Log.i(TAG, "handleNotificationEvent(), not handled type "+Integer.toHexString(iNCode));	
				}
			}
			
			try {
				if(0 <= iMsgType && null != mMessenger){
					mMessenger.send(Message.obtain(null, iMsgType, msgObj.toString()));
				}
			} catch (RemoteException e) {
				Log.e(TAG, "handleNotificationEvent(), RemoteException, e="+e.toString());	
			}
			
			if(null != strNotifyMsg){
				showNotification(iNotifyType, intent, strNotifyTitle, strNotifyMsg, lTs);
			}
		}
		return bRet;
	}
	
	private void handleWSEvent(JSONObject dataObj){
		if(!SessionMgr.getInstance().isTokenValid()){
			return;
		}
    	if(null != dataObj){
			final int iCmd = BeseyeJSONUtil.getJSONInt(dataObj, WS_ATTR_COMM);
			if(DEBUG)
				Log.i(TAG, "handleWSEvent(),iCmd="+iCmd);	
			final JSONObject DataObj = BeseyeJSONUtil.getJSONObject(dataObj, WS_ATTR_INTERNAL_DATA);
			
			int iMsgType = -1;
			switch(iCmd){
				case WS_ATTR_COMM_CAM_SETTING_CHANGED:{
					iMsgType = MSG_CAM_SETTING_UPDATED;
					break;
				}
				case WS_ATTR_COMM_DETECTION_NOTIFY:{
					handleNotificationEvent(DataObj, false);
					break;
				}
				case WS_ATTR_COMM_SYS_INFO:{
					break;
				}
				case WS_ATTR_COMM_KEEP_ALIVE:{
					break;
				}
				case WS_ATTR_COMM_WS_RECONNECT:{
					break;
				}
				default:{
					Log.w(TAG, "handleWSEvent(), not handled evt, iCmd="+iCmd);	
				}
			}
			
			try {
				if(0 <= iMsgType && null != mMessenger)
					mMessenger.send(Message.obtain(null, iMsgType, DataObj.toString()));
			} catch (RemoteException e) {
				Log.e(TAG, "handleWSEvent(), RemoteException, e="+e.toString());	
			}
		}else{
			Log.e(TAG, "handleWSEvent(), dataObj is null");	
		}
    }
	
	private void handleGCMEvents(String strRegularData, String strCusData){
		if(!SessionMgr.getInstance().isTokenValid()){
			return;
		}
		
		if(null != strRegularData){
			try {
				JSONObject objReg = new JSONObject(strRegularData);
				if(null != objReg){
					JSONObject msgObj = new JSONObject();
					BeseyeJSONUtil.setJSONObject(msgObj, BeseyeJSONUtil.PS_REGULAR_DATA, objReg);
					try {
						if(null != strCusData){
							BeseyeJSONUtil.setJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA, new JSONObject(strCusData));
						}
					} catch (JSONException e) {
						Log.e(TAG, "handleGCMEvents(), e:"+e.toString());	
					}
					
					handleNotificationEvent(msgObj, true);
				}
			} catch (JSONException e) {
				Log.e(TAG, "handleGCMEvents(), e:"+e.toString());	
			}
		}else{
			Log.e(TAG, "handleGCMEvents(), strRegularData is null");	
		}
	}
	
	//Notification NCode definitions
	private static final int NCODE_CAM_NAME_CHANGE 				= 0x0100; 
	private static final int NCODE_CAM_DEACTIVATE 				= 0x0101;
	private static final int NCODE_WIFI_CHANGED 				= 0x0102; 
	
	private static final int NCODE_SHARE_BROWSE_PERM 			= 0x0200; 
	private static final int NCODE_PLAN_EXPIRED 				= 0x0201; 
	private static final int NCODE_PLAN_CHANGE 					= 0x0202; 
	private static final int NCODE_PW_CHANGE 					= 0x0203;
	private static final int NCODE_UNSUBSCRIBE 					= 0x0204;
	
	private static final int NCODE_MOTION_DETECT 				= 0x0300;
	private static final int NCODE_PEOPLE_DETECT 				= 0x0301;
	private static final int NCODE_OFFLINE_DETECT 				= 0x0302;
	
	private static final int NCODE_TALK_CHANNEL_USED 			= 0x0400;
	private static final int NCODE_TALK_CHANNEL_RELE 			= 0x0401;
	private static final int NCODE_CAM_NEW_VER 					= 0x0402;
	
	private static final int NCODE_CAM_ACTIVATE 				= 0X0500;
	
	private static final int NCODE_CAM_ONLINE 					= 0X1000;
	private static final int NCODE_CAM_OFFLINE 					= 0X1001;
	
	private static final int NCODE_CAM_SETTING_UPDATE 			= 0X2000;
	
	
	
//	private void showNotification(int iNotifyId, Intent intent, CharSequence text, long lTs) {
//		showNotification(iNotifyId, intent, getAppName(), text, lTs);
//	}
	
//	private void showNotification(int iNotifyId, Intent intent, CharSequence text, JSONObject eventObj) {
//		long lTs = BeseyeJSONUtil.getJSONLong(eventObj, BeseyeJSONUtil.MM_START_TIME);
//		showNotification(iNotifyId, intent, text, lTs);
//    }
	
//	private void showRegIdNotification(){
//		Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
//		intent.setType("text/plain");
//		intent.putExtra(Intent.EXTRA_SUBJECT, "Beseye User RegId");
//		intent.putExtra(Intent.EXTRA_TEXT, GCMRegistrar.getRegistrationId(getApplicationContext()));
//		intent.setData(Uri.parse("mailto:")); // or just "mailto:" for blank
//		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
//		/*if(null != contentIntent)*/{
////			 final Notification notification = new Notification(
////				        				R.drawable.common_app_icon,       // the icon for the status bar
////				        				"RegId got",                        // the text to display in the ticker
////				        				/*System.currentTimeMillis()*/mLastNotifyUpdateTime); // the timestamp for the notification
////
////			 if(null != notification){
////				notification.setLatestEventInfo(
////						 this,                        // the context to use
////						 "Beseye User RegId",
////						                              // the title for the notification
////						 GCMRegistrar.getRegistrationId(getApplicationContext()),                        // the details to display in the notification
////						 contentIntent);              // the contentIntent (see above)
////
////				notification.defaults = Notification.DEFAULT_LIGHTS;
////				notification.flags = Notification.FLAG_AUTO_CANCEL;
////				
////				mNotificationManager.notify(
////				999, // we use a string id because it is a unique
////				// number.  we use it later to cancel the notification
////				notification);
////			 }
//			
////			final String path = "https://beseye-thumbnail.s3-ap-northeast-1.amazonaws.com/clothing-store.jpg?AWSAccessKeyId=AKIAI4TMTBQZA45VAMUQ&Expires=1406781831&Signature=iJ4SFLbXK7NhNuEF3r3VH3KwHAE%3D";
//////			Bitmap bitmap = null;
//////			try {
//////				bitmap = BitmapFactory.decodeStream(
//////			                (InputStream) new URL(path).getContent());
//////			} catch (IOException e) {
//////			        e.printStackTrace();
//////			}			
////			
////			
////			
////			new AsyncTask<String, Integer, Bitmap>(){
////				@Override
////				protected Bitmap doInBackground(String... arg0) {
////					Bitmap bitmap = null;
////					try {
////						URL url;
////						url = new URL(path);
////						URLConnection conn;
////						conn = url.openConnection();
////
////						HttpURLConnection httpConn = (HttpURLConnection) conn;
////						httpConn.setRequestMethod("GET");
////						httpConn.connect();
////						if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
////							InputStream inputStream = httpConn.getInputStream();
////							if (inputStream != null) {
////								BitmapFactory.Options options = new BitmapFactory.Options();
////								options.inSampleSize = 2;
////								bitmap = BitmapFactory.decodeStream(inputStream, null, options);
////							}else{
////								Log.w(TAG, "inputStream is null");
////							}
////						}
////					} catch (MalformedURLException e) {
////						// TODO Auto-generated catch block
////						e.printStackTrace();
////					}catch (IOException e) {
////						// TODO Auto-generated catch block
////						e.printStackTrace();
////					}
////					return bitmap;
////				}
////
////				@Override
////				protected void onPostExecute(Bitmap bitmap) {
////					super.onPostExecute(bitmap);
////					NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
////					
////					style.bigPicture(bitmap);
////					style.setBigContentTitle("Event Detected (Expand)");
////					style.setSummaryText("Description (Expand)");
////					style.bigLargeIcon(bitmap);
////					
////			         
////					NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(  
////							BeseyeNotificationService.this).setSmallIcon(R.drawable.common_app_icon)  
////					        .setContentTitle("Event Detected (Normal)")  
////					        .setContentText("Description (Normal)")
////					        .setStyle(style)
////					        .setWhen(new Date().getTime())  	
////							.setSmallIcon(R.drawable.common_app_icon)
////							.setLargeIcon(bitmap);
////					         mNotificationManager.notify(
////										888, // we use a string id because it is a unique
////										// number.  we use it later to cancel the notification
////										mBuilder.build());
////					
////				
////				}}.execute();
////			
////			
////			
////			
//			//NotificationCompat.BigPictureStyle picStyle = new NotificationCompat.BigPictureStyle();  
//			//Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cameralist_thumbnail);  
//			//picStyle.bigPicture(bitmap);  
//			//mBuilder.setStyle(picStyle); 
//			
//			
//		}
//		testMsgGot();
//	}
	
//	private void testMsgGot(){
//		new IncomingHandler().postDelayed(new Runnable(){
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				Intent intent = new Intent(GCMIntentService.FORWARD_GCM_MSG_ACTION);
//		        intent.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, GCMIntentService.FORWARD_ACTION_TYPE_MSG);
//		        intent.putExtra("info", "This is a beseye test message.");
//		        sendBroadcast(intent);
//			}}, 1000);
//		
//	}
	
//	private void setLastNotifyItem(JSONObject notifyObj){
//	if(null != notifyObj){
//		//mLastNotifyId = BeseyeJSONUtil.getJSONString(notifyObj, NOTIFY_ID);
//		BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_LAST_NOTIFY_ID, mLastNotifyId);
//		
//		mLastNotifyUpdateTime = BeseyeJSONUtil.getJSONLong(notifyObj, UPDATE_TIME);
//		BeseyeSharedPreferenceUtil.setPrefLongValue(mPref, PUSH_SERVICE_LAST_NOTIFY_TIME, mLastNotifyUpdateTime);
//	}
//}
//
//private boolean isNewEvent(JSONObject eventObj){
//	boolean bRet = false;
//	if(null != eventObj){
//		if(null != mLastEventObj){
//			if(DEBUG){
//				Log.i(TAG, "isNewEvent(), mLastEventObj: ("+mLastEventObj+")\n" +
//						"	              eventObj: ("+eventObj+")");
//			}
//			
//			long lLastEventStartTime = BeseyeJSONUtil.getJSONLong(mLastEventObj, BeseyeJSONUtil.MM_START_TIME);
//			long lNewEventStartTime = BeseyeJSONUtil.getJSONLong(eventObj, BeseyeJSONUtil.MM_START_TIME);
//			if(lLastEventStartTime < lNewEventStartTime){
//				if(DEBUG)
//					Log.d(TAG, "isNewEvent(), lLastEventStartTime("+lLastEventStartTime+") < lNewEventStartTime ("+lNewEventStartTime+")");
//				return true;
//			}/*else if(lLastEventStartTime == lNewEventStartTime){
//				
//				int typeArrOld = BeseyeJSONUtil.getJSONInt(mLastEventObj, BeseyeJSONUtil.MM_TYPE_IDS);
//				int typeArrNew = BeseyeJSONUtil.getJSONInt(eventObj, BeseyeJSONUtil.MM_TYPE_IDS);
//				
//				if(0 < (BeseyeJSONUtil.MM_TYPE_ID_FACE & typeArrNew)){
//					boolean bFoundNew = true;
//					if((0 < (BeseyeJSONUtil.MM_TYPE_ID_FACE & typeArrOld) || 0 < (BeseyeJSONUtil.MM_TYPE_ID_MOTION & typeArrOld)) && typeArrOld <= typeArrNew){
//						bFoundNew = false;
//					}
//					if(bFoundNew){
//						Log.i(TAG, "isNewEvent(), found new event");
//						bRet = true;
//					}
//				}
//			}*/
//		}else{
//			bRet =  true;
//		}
//	}
//	return bRet;
//}
//
//private void setLastEventItem(JSONObject eventObj){
//	if(null != eventObj){
//		if(DEBUG)
//			Log.i(TAG, "setLastEventItem(), eventObj:"+eventObj.toString());
//		
//		BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_LAST_EVENT, eventObj.toString());
//		mLastEventObj = eventObj;
//	}else{
//		Log.d(TAG, "setLastEventItem(), eventObj:"+null);
//		mLastEventObj = null;
//		BeseyeSharedPreferenceUtil.setPrefStringValue(mPref, PUSH_SERVICE_LAST_EVENT, "");
//	}
//}

	
}
