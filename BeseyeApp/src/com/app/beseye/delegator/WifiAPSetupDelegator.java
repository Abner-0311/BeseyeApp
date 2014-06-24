package com.app.beseye.delegator;

import static com.app.beseye.util.BeseyeConfig.*;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONObject;

import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.app.beseye.httptask.BeseyeHttpWifiTask;
import com.app.beseye.httptask.BeseyeHttpWifiTask.OnHttpTaskCallback;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.NetPingTask;
import com.app.beseye.util.NetworkMgr.OnPingCallback;
import com.app.beseye.util.NetworkMgr.OnSupplicantStatusChangeCallback;
import com.app.beseye.util.NetworkMgr.OnWifiScanResultAvailableCallback;
import com.app.beseye.util.NetworkMgr.OnWifiStatusChangeCallback;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

public class WifiAPSetupDelegator implements OnSupplicantStatusChangeCallback, OnWifiStatusChangeCallback, OnHttpTaskCallback, OnPingCallback{
	static private final int COUNT_TO_CHECK_REBOOT = 5;//5 times
	static private final long TIME_TO_CHECK_REBOOT = 10000L;//10 sec
	static private final long MAX_TIME_TO_CHECK_REBOOT = 60000L;//60 sec
	
	private static final long TIME_TO_EXPIRE_CONNECT = 30000L;;//30 sec
	private static final int MAX_RETRY_CONNECT = 3;
	private int miRetryCount = 0;
	private boolean mbMaybeAuthenticationError = false;
	
	private CheckWifiConnectExpireRunnable mTargetAPConnectExpire = null;
	private CheckWifiConnectExpireRunnable mRelayAPConnectExpire = null;
	
	static class CheckWifiConnectExpireRunnable implements Runnable{
		private WeakReference<WifiAPSetupDelegator> mWifiAPSetupDelegator;
		
		CheckWifiConnectExpireRunnable(WifiAPSetupDelegator delegator){
			mWifiAPSetupDelegator = new WeakReference<WifiAPSetupDelegator>(delegator);
		}
		
		@Override
		public void run() {
			WifiAPSetupDelegator delegator = mWifiAPSetupDelegator.get();
			if(null != delegator){
				delegator.connectExpireCallback(this);
			}
		}	
	}
	
	private void connectExpireCallback(CheckWifiConnectExpireRunnable r){
		Log.i(TAG, "connectExpireCallback(), r="+r);
		if(r == mTargetAPConnectExpire){
			setError(WIFI_AP_SETUP_ERROR.ERROR_TARGET_AP_CONNECT_ISSUE, mTargetWifiAPInfo);
			mTargetAPConnectExpire = null;
		}else if(r == mRelayAPConnectExpire){
			setError(WIFI_AP_SETUP_ERROR.ERROR_RELAY_AP_CONNECT_ISSUE, null);
			mRelayAPConnectExpire = null;
		}
	}
	
	private CheckRealyAPRebootRunnable mCheckRealyAPRebootRunnable=null;
	
	static class CheckRealyAPRebootRunnable implements Runnable, OnWifiScanResultAvailableCallback{
		private WeakReference<WifiAPSetupDelegator> mWifiAPSetupDelegator;
		private int miTrials = 0;
		private boolean mbAPFound = false;
		private long mlInitTime = 0;
		private long mlLastCheckTime = 0;
		private boolean mbCancel = false;
		
		CheckRealyAPRebootRunnable(WifiAPSetupDelegator delegator){
			mWifiAPSetupDelegator = new WeakReference<WifiAPSetupDelegator>(delegator);
			mlInitTime = System.currentTimeMillis();
			BeseyeUtils.postRunnable(this, TIME_TO_CHECK_REBOOT);
		}
		
		public void cancel(){
			mbCancel = true;
		}
		
		@Override
		public void run() {		
			Log.i(TAG, "CheckRealyAPRebootRunnable::run(), miTrials="+miTrials);
			if(mbCancel){
				return;
			}
			
			miTrials++;
			if(mbAPFound || miTrials > COUNT_TO_CHECK_REBOOT || (System.currentTimeMillis() - mlInitTime) > MAX_TIME_TO_CHECK_REBOOT){
				WifiAPSetupDelegator delegator = mWifiAPSetupDelegator.get();
				if(null != delegator){
					delegator.relayAPRebootCallback(this, mbAPFound);
				}
				return;
			}
			
			NetworkMgr.getInstance().scanWifiList(this);
			
			mlLastCheckTime = System.currentTimeMillis();
			BeseyeUtils.postRunnable(this, TIME_TO_CHECK_REBOOT);
		}

		@Override
		public void onWifiScanResultAvailable() {
			if(NetworkMgr.getInstance().findWifiAPBySSID(RELAY_AP_SSID)){
				mbAPFound = true;
				BeseyeUtils.removeRunnable(this);
				BeseyeUtils.postRunnable(this, 0);
			}else{
				long lDelta = System.currentTimeMillis() - mlLastCheckTime;
				BeseyeUtils.removeRunnable(this);
				if((lDelta) > TIME_TO_CHECK_REBOOT){
					BeseyeUtils.postRunnable(this, 0);
				}else{
					BeseyeUtils.postRunnable(this, TIME_TO_CHECK_REBOOT - lDelta);
				}
			}
		}	
	}
	
	private void relayAPRebootCallback(CheckRealyAPRebootRunnable r, boolean bFoundAfterReboot){
		Log.i(TAG, "relayAPRebootCallback(), r="+r+", bFoundAfterReboot="+bFoundAfterReboot);
		if(r == mCheckRealyAPRebootRunnable){
			if(bFoundAfterReboot){
				setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_REBOOT_FOUND);
			}else{
				setError(WIFI_AP_SETUP_ERROR.ERROR_RELAY_AP_REBOOT_FAILED, null);
			}
			mCheckRealyAPRebootRunnable=null;
		}
	}
	
	public static enum WIFI_AP_SETUP_STATE{
		UNINIT,
		INIT,
		TARGET_AP_CONNECTING,
		TARGET_AP_CONNECTED,
		TARGET_AP_CONNECTIVITY_VERIFYING,
		TARGET_AP_CONNECTIVITY_VERIFIED,
		RELAY_AP_CONNECTING,
		RELAY_AP_CONNECTED,
		RELAY_AP_REBOOT_WAITING,
		RELAY_AP_REBOOT_FOUND,
		RELAY_AP_REBOOT_CONNECTING,
		RELAY_AP_REBOOT_CONNECTED,
		RELAY_AP_CONNECTIVITY_VERIFYING,
		RELAY_AP_CONNECTIVITY_VERIFIED,
		SETUP_DONE
	}
	
	public static enum WIFI_AP_SETUP_ERROR{
		ERROR_NONE,
		ERROR_INVALID_PREV_STATE,
		ERROR_WIFI_DISABLED,
		ERROR_TARGET_AP_INVALID,
		ERROR_TARGET_AP_NOT_FOUND,
		ERROR_TARGET_AP_CONNECT_ISSUE,
		ERROR_TARGET_AP_PW_ERROR,
		ERROR_TARGET_AP_MAYBE_PW_ERROR,
		ERROR_TARGET_AP_CONNECTIVITY_ERROR,
		ERROR_RELAY_AP_INVALID,
		ERROR_RELAY_AP_NOT_FOUND,
		ERROR_RELAY_AP_CONNECT_ISSUE,
		ERROR_RELAY_AP_PW_ERROR,
		ERROR_RELAY_AP_MAYBE_PW_ERROR,
		ERROR_RELAY_AP_CONNECTIVITY_ERROR,
		ERROR_RELAY_AP_REBOOT_FAILED,
		ERROR_RELAY_AP_SETUP_FAILED,
		ERROR_UNKNOWN
	}
	
	public static interface OnWifiApSetupCallback{
		void onWifiApSetupStateChanged(WIFI_AP_SETUP_STATE curState, WIFI_AP_SETUP_STATE prevState);
		void onWifiApSetupError(WIFI_AP_SETUP_STATE curState, WIFI_AP_SETUP_ERROR error, Object userData);
	}
	
	private WeakReference<OnWifiApSetupCallback> mOnWifiApSetupCallbackListener;
	private WifiAPInfo mTargetWifiAPInfo;
	private WIFI_AP_SETUP_STATE mWifiApSetupState = WIFI_AP_SETUP_STATE.UNINIT;
	private WIFI_AP_SETUP_ERROR mWifiApSetupError = WIFI_AP_SETUP_ERROR.ERROR_NONE;
	private BeseyeHttpWifiTask mRelayAPSetupTask;
	
	public WifiAPSetupDelegator(WifiAPInfo wifiAPInfo, OnWifiApSetupCallback listener){
		mOnWifiApSetupCallbackListener = new WeakReference<OnWifiApSetupCallback>(listener);
		updateTargetAPInfo(wifiAPInfo);
	}
	
	public void updateTargetAPInfo(WifiAPInfo wifiAPInfo){
		mTargetWifiAPInfo = wifiAPInfo;
		setError(WIFI_AP_SETUP_ERROR.ERROR_NONE, null);
		setWifiApSetupState(WIFI_AP_SETUP_STATE.INIT);
	}
	
	private void init(){
		miRetryCount = 0;
		mbMaybeAuthenticationError = false;
	}
	
	public WIFI_AP_SETUP_ERROR getWifiApSetupError(){
		return mWifiApSetupError;
	}
	
	private void setError(WIFI_AP_SETUP_ERROR error, Object userData){
		mWifiApSetupError = error;
		switch(error){
			case ERROR_TARGET_AP_CONNECT_ISSUE:
			case ERROR_TARGET_AP_PW_ERROR:
			case ERROR_TARGET_AP_MAYBE_PW_ERROR:{
				NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
				NetworkMgr.getInstance().unregisterOnSupplicantStatusChangeCallback(this);
				BeseyeUtils.removeRunnable(mTargetAPConnectExpire);
				break;
			}
			
			case ERROR_RELAY_AP_CONNECT_ISSUE:
			case ERROR_RELAY_AP_PW_ERROR:
			case ERROR_RELAY_AP_MAYBE_PW_ERROR:{
				NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
				NetworkMgr.getInstance().unregisterOnSupplicantStatusChangeCallback(this);
				BeseyeUtils.removeRunnable(mRelayAPConnectExpire);
				break;
			}
			default:
				break;
		}
		
		if(null != mOnWifiApSetupCallbackListener && !mWifiApSetupError.equals(WIFI_AP_SETUP_ERROR.ERROR_NONE)){
			OnWifiApSetupCallback listener = mOnWifiApSetupCallbackListener.get();
			if(null != listener)
				listener.onWifiApSetupError(getWifiApSetupState(), error, userData);
		}
	}
	
	public WIFI_AP_SETUP_STATE getWifiApSetupState(){
		return mWifiApSetupState;
	}
	
	public boolean inWifiApSetupState(WIFI_AP_SETUP_STATE state){
		return state.equals(mWifiApSetupState);
	}
	
	public boolean setWifiApSetupState(WIFI_AP_SETUP_STATE state){
		Log.d(TAG, "setWifiApSetupState(), state:"+state);
		boolean bRet = true;
		WIFI_AP_SETUP_STATE preState = mWifiApSetupState;
		mWifiApSetupState = state;
		switch(state){
			case INIT:{
				init();
				//Check if wifi is enabled
				if(WifiManager.WIFI_STATE_ENABLED == NetworkMgr.getInstance().getWifiStatus()){
					connectToTargetAP();
				}else{
					setError(WIFI_AP_SETUP_ERROR.ERROR_WIFI_DISABLED, null);
					bRet = false;
				}
//				if(preState == WIFI_AP_SETUP_STATE.UNINIT){
//					
//				}else{
//					setError(WIFI_AP_SETUP_ERROR.ERROR_INVALID_PREV_STATE);
//					bRet = false;
//				}
				break;
			}
			case TARGET_AP_CONNECTING:{
				NetworkMgr.getInstance().registerWifiStatusChangeCallback(this);
				NetworkMgr.getInstance().registerOnSupplicantStatusChangeCallback(this);
				break;
			}
			case TARGET_AP_CONNECTED:{
				if(null != mTargetAPConnectExpire){
					BeseyeUtils.removeRunnable(mTargetAPConnectExpire);
					mTargetAPConnectExpire = null;
				}
//				if(!REDDOT_DEMO)
//					pingTargetAP();
				break;
			}
			case TARGET_AP_CONNECTIVITY_VERIFYING:{
				break;
			}
			case TARGET_AP_CONNECTIVITY_VERIFIED:{
				connectToRelayAP();
				break;
			}
			case RELAY_AP_CONNECTING:{
				NetworkMgr.getInstance().registerWifiStatusChangeCallback(this);
				NetworkMgr.getInstance().registerOnSupplicantStatusChangeCallback(this);
				break;
			}
			case RELAY_AP_CONNECTED:{
				if(null != mRelayAPConnectExpire){
					BeseyeUtils.removeRunnable(mRelayAPConnectExpire);
					mRelayAPConnectExpire = null;
				}
				setupTargetAp();
				break;
			}
			case RELAY_AP_REBOOT_WAITING:{
				checkRelayAPReboot();
				break;
			}
			case RELAY_AP_REBOOT_FOUND:{
				connectToRelayAP();
				break;
			}
			case RELAY_AP_REBOOT_CONNECTING:{
				NetworkMgr.getInstance().registerWifiStatusChangeCallback(this);
				NetworkMgr.getInstance().registerOnSupplicantStatusChangeCallback(this);
				break;
			}
			case RELAY_AP_REBOOT_CONNECTED:{
				if(null != mRelayAPConnectExpire){
					BeseyeUtils.removeRunnable(mRelayAPConnectExpire);
					mRelayAPConnectExpire = null;
				}
				pingRelayAP();
				break;
			}
			case RELAY_AP_CONNECTIVITY_VERIFYING:{
				break;
			}
			case RELAY_AP_CONNECTIVITY_VERIFIED:{
				setWifiApSetupState(WIFI_AP_SETUP_STATE.SETUP_DONE);
				break;
			}
			case SETUP_DONE:{
				
				break;
			}
			default:
				Log.e(TAG, "setWifiApSetupState(), failed");
		}
		
		if(bRet){
			if(null != mOnWifiApSetupCallbackListener){
				OnWifiApSetupCallback listener = mOnWifiApSetupCallbackListener.get();
				if(null != listener)
					listener.onWifiApSetupStateChanged(mWifiApSetupState, preState);
			}
		}
		
		return bRet;
	}
	
	private void connectToTargetAP(){
		if(null != mTargetWifiAPInfo){
			setWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTING);
			if(false == NetworkMgr.getInstance().connectWifi(mTargetWifiAPInfo, mTargetWifiAPInfo.password)){
				Log.e(TAG, "connectToTargetAP(), failed");
				setError(WIFI_AP_SETUP_ERROR.ERROR_TARGET_AP_NOT_FOUND, mTargetWifiAPInfo);
			}else{
				mTargetAPConnectExpire = new CheckWifiConnectExpireRunnable(this);
				BeseyeUtils.postRunnable(mTargetAPConnectExpire, TIME_TO_EXPIRE_CONNECT);
			}
		}else{
			Log.e(TAG, "connectToTargetAP(), invalid mTargetWifiAPInfo");
			setError(WIFI_AP_SETUP_ERROR.ERROR_TARGET_AP_INVALID, null);
		}
	}
	
	private WifiAPInfo mRelayAPInfo = null;
	private void connectToRelayAP(){
		mRelayAPInfo = NetworkMgr.getInstance().getWifiAPInfoBySSID(RELAY_AP_SSID);
		if(null != mRelayAPInfo){
			mRelayAPInfo.password = RELAY_AP_PW;
			if(inWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTIVITY_VERIFIED))
				setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_CONNECTING);
			else if(inWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_REBOOT_FOUND))
				setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_REBOOT_CONNECTING);
			
			if(false == NetworkMgr.getInstance().connectWifi(mRelayAPInfo, mRelayAPInfo.password)){
				Log.e(TAG, "connectToRelayAP(), failed");
				setError(WIFI_AP_SETUP_ERROR.ERROR_RELAY_AP_NOT_FOUND, mRelayAPInfo);
			}else{
				mRelayAPConnectExpire = new CheckWifiConnectExpireRunnable(this);
				BeseyeUtils.postRunnable(mRelayAPConnectExpire, TIME_TO_EXPIRE_CONNECT);
			}
		}else{
			Log.e(TAG, "connectToRelayAP(), can't find info from SSID="+RELAY_AP_SSID);
			setError(WIFI_AP_SETUP_ERROR.ERROR_RELAY_AP_INVALID, null);
		}
	}
	
	private void setupTargetAp(){
		if(null != mTargetWifiAPInfo){
			if(null != mRelayAPSetupTask){
				mRelayAPSetupTask.cancel(true);
			}
			mRelayAPSetupTask = new BeseyeHttpWifiTask(this);
			mRelayAPSetupTask.execute(mTargetWifiAPInfo.SSID, mTargetWifiAPInfo.BSSID, String.valueOf(NetworkMgr.getChannelFromFrequency(mTargetWifiAPInfo.frequency)), mTargetWifiAPInfo.password, mTargetWifiAPInfo.cipher, String.valueOf(mTargetWifiAPInfo.wepkeyIdx+1));
			
		}else{
			Log.e(TAG, "setupTargetAp(), invalid mTargetWifiAPInfo");
			setError(WIFI_AP_SETUP_ERROR.ERROR_TARGET_AP_INVALID, null);
		}
	}
	
	private void checkRelayAPReboot(){
		if(null != mCheckRealyAPRebootRunnable){
			mCheckRealyAPRebootRunnable.cancel();
			mCheckRealyAPRebootRunnable = null;
		}
		
		mCheckRealyAPRebootRunnable = new CheckRealyAPRebootRunnable(this);
		
	}

	@Override
	public void onWifiStateChanged(int iWifiState, int iPrevWifiState) {
		
	}

	@Override
	public void onWifiNetworkStateChanged(DetailedState iWifiNetworkState, DetailedState iPrevWifiNetworkState) {
		Log.i(TAG, "onWifiNetworkStateChanged(), iWifiNetworkState="+iWifiNetworkState);
		if(iWifiNetworkState.equals(DetailedState.CONNECTED)){
			String activeSSID = NetworkMgr.getInstance().getActiveWifiSSID();
			if(inWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTING)){
				if(mTargetWifiAPInfo.SSID.equals(activeSSID)){
					setWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTED);
					NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
					NetworkMgr.getInstance().unregisterOnSupplicantStatusChangeCallback(this);
				}else{
					Log.i(TAG, "onWifiNetworkStateChanged(), SSID mismatch("+mTargetWifiAPInfo.SSID+", "+activeSSID+")");
				}
			}else if(inWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_CONNECTING)){
				if(mRelayAPInfo.SSID.equals(activeSSID)){
					setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_CONNECTED);
					NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
					NetworkMgr.getInstance().unregisterOnSupplicantStatusChangeCallback(this);
				}else{
					Log.i(TAG, "onWifiNetworkStateChanged(), SSID mismatch("+mTargetWifiAPInfo.SSID+", "+activeSSID+")");
				}
			}else if(inWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_REBOOT_CONNECTING)){
				if(mRelayAPInfo.SSID.equals(activeSSID)){
					setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_REBOOT_CONNECTED);
					NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
					NetworkMgr.getInstance().unregisterOnSupplicantStatusChangeCallback(this);
				}else{
					Log.i(TAG, "onWifiNetworkStateChanged(), SSID mismatch("+mTargetWifiAPInfo.SSID+", "+activeSSID+")");
				}
			}else{
				Log.i(TAG, "onWifiNetworkStateChanged(), invalid state="+getWifiApSetupState());
			}
		}
	}
	
	@Override
	public void onSupplicantStateChanged(SupplicantState iWifiState, SupplicantState iPrevWifiState) {
		if(inWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTING)){
			if(mTargetWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WEP)){
				if(iPrevWifiState.equals(SupplicantState.ASSOCIATED) && iWifiState.equals(SupplicantState.DISCONNECTED)){
					mbMaybeAuthenticationError = true;
					miRetryCount++;
					Log.i(TAG, "onSupplicantStateChanged(), set mbMaybeAuthenticationError as true for WEP, miRetryCount="+miRetryCount);
				}
			}else if(mTargetWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WPA)){
				if(iPrevWifiState.equals(SupplicantState.FOUR_WAY_HANDSHAKE) && iWifiState.equals(SupplicantState.DISCONNECTED)){
					mbMaybeAuthenticationError = true;
					miRetryCount++;
					Log.i(TAG, "onSupplicantStateChanged(), set mbMaybeAuthenticationError as true, miRetryCount="+miRetryCount);
					
				}
			}
			
			if(miRetryCount >= MAX_RETRY_CONNECT){
				setError(WIFI_AP_SETUP_ERROR.ERROR_TARGET_AP_MAYBE_PW_ERROR, mTargetWifiAPInfo);
			}
		}
	}

	@Override
	public void onAuthenticationError(String BSSID) {
		Log.i(TAG, "onAuthenticationError(), BSSID="+BSSID+", mTargetWifiAPInfo.BSSID="+mTargetWifiAPInfo.BSSID);
		if(BSSID.equals(mTargetWifiAPInfo.BSSID)){
			setError(WIFI_AP_SETUP_ERROR.ERROR_TARGET_AP_PW_ERROR, mTargetWifiAPInfo);
		}
	}
	
	private NetPingTask mPingTargetAPTask = null;
	private NetPingTask mPingRelayAPTask = null;
	
	@Override
	public void onPingResultCallback(final AsyncTask task, final boolean bSuccess) {
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				Log.i(TAG, "onPingResultCallback(), bSuccess:"+bSuccess);
				if(task == mPingTargetAPTask){
					if(inWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTIVITY_VERIFYING)){
						if(bSuccess){
							setWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTIVITY_VERIFIED);
						}else{
							setError(WIFI_AP_SETUP_ERROR.ERROR_TARGET_AP_CONNECTIVITY_ERROR, mTargetWifiAPInfo);
						}
					}else{
						Log.i(TAG, "onPingResultCallback(), mPingTargetAPTask callback when wrong state:"+getWifiApSetupState());
					}
				}else if(task == mPingRelayAPTask){
					if(inWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_CONNECTIVITY_VERIFYING)){
						if(bSuccess){
							setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_CONNECTIVITY_VERIFIED);
						}else{
							setError(WIFI_AP_SETUP_ERROR.ERROR_RELAY_AP_CONNECTIVITY_ERROR, mTargetWifiAPInfo);
						}
					}else{
						Log.i(TAG, "onPingResultCallback(), mPingRelayAPTask callback when wrong state:"+getWifiApSetupState());
					}
				}
			}}, 0);
		
	}
	
	private void pingTargetAP(){
		Log.i(TAG, "pingTargetAP()");
		mPingTargetAPTask = new NetPingTask(this);
		if(null != mPingTargetAPTask){
			mPingTargetAPTask.execute();
		}
		setWifiApSetupState(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTIVITY_VERIFYING);
	}
	
	private void pingRelayAP(){
		Log.i(TAG, "pingRelayAP()");
		mPingRelayAPTask = new NetPingTask(this);
		if(null != mPingRelayAPTask){
			mPingRelayAPTask.execute();
		}
		setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_CONNECTIVITY_VERIFYING);
	}
	
	@Override
	public void onShowDialog(AsyncTask task, int iDialogId, int iTitleRes, int iMsgRes) {
	}

	@Override
	public void onDismissDialog(AsyncTask task, int iDialogId) {
		
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg) {
		
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(task == mRelayAPSetupTask){
			if(null != result)
				setWifiApSetupState(WIFI_AP_SETUP_STATE.RELAY_AP_REBOOT_WAITING);
			else{
				setError(WIFI_AP_SETUP_ERROR.ERROR_RELAY_AP_SETUP_FAILED, mTargetWifiAPInfo);
			}
		}
	}

	@Override
	public void onToastShow(AsyncTask task, String strMsg) {
		
	}

	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason) {
		
	}
}
