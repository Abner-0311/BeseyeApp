package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.RELAY_AP_SSID;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.app.beseye.R;
import com.app.beseye.receiver.NetworkChangeReceiver;
import com.app.beseye.receiver.WifiStateChangeReceiver;

public class NetworkMgr {
	public static final int NUM_WEP_KEY_IDX = 4;
	public static final int NUM_WIFI_SECU_TYPE = 4;
	
	static public interface OnNetworkChangeCallback{
		public void onConnectivityChanged(boolean bNetworkConnected);
	}
	
	static public interface OnWifiStatusChangeCallback{
		public void onWifiStateChanged(int iWifiState, int iPrevWifiState);
		public void onWifiNetworkStateChanged(DetailedState iWifiNetworkState, DetailedState iPrevWifiNetworkState);
	}
	
	static public interface OnSupplicantStatusChangeCallback{
		public void onSupplicantStateChanged(SupplicantState iWifiState, SupplicantState iPrevWifiState);
		public void onAuthenticationError(String BSSID);
	}
	
	static public interface OnWifiScanResultAvailableCallback{
		public void onWifiScanResultAvailable();
	}
	
	private WeakReference<Context> mWrContext;
	private ConnectivityManager mConnectivityManager;
	private NetworkChangeReceiver mNetworkChangeReceiver;
	private WifiManager mWifiManager;
	private WifiStateChangeReceiver mWifiReceiver;

	private boolean mbIsNetworkConnected = false;
	private int miWifiState ;
	private DetailedState miWifiNetworkState ;
	private SupplicantState mSupplicantState;
	private String mPreviousActiveSSID;
	
	private List<WeakReference<OnNetworkChangeCallback>> mOnNetworkChangeCallbackListeners;
	private List<WeakReference<OnWifiStatusChangeCallback>> mOnWifiStatusChangeCallbackListeners;
	private WeakReference<OnSupplicantStatusChangeCallback> mOnSupplicantStatusChangeCallbackListeners;
	private WeakReference<OnWifiScanResultAvailableCallback> mOnWifiScanResultAvailableCallbackListener;
	
	static private NetworkMgr sNetworkMgr;
	//static private Handler sHandler = new Handler();
	
	static public NetworkMgr createInstance(Context context){
		if(null == sNetworkMgr){
			sNetworkMgr = new NetworkMgr(context);
		}
		return sNetworkMgr;
	}
	
	static public NetworkMgr getInstance(){		
		return sNetworkMgr;
	}
	
	private NetworkMgr(Context context){
		if(null != context){
			mWrContext = new WeakReference<Context>(context);
			mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			mbIsNetworkConnected = isNetworkConnected();
			
			mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			miWifiState = getWifiStatus();
			miWifiNetworkState = getWifiNetworkStatus();
			mSupplicantState = getActiveSupplicantState();
			mPreviousActiveSSID = getActiveWifiBSSID();
			
			mOnNetworkChangeCallbackListeners = new ArrayList<WeakReference<OnNetworkChangeCallback>>();
			mOnWifiStatusChangeCallbackListeners = new ArrayList<WeakReference<OnWifiStatusChangeCallback>>();
			
			mNetworkChangeReceiver = new NetworkChangeReceiver();
			if(null != mNetworkChangeReceiver){
				context.registerReceiver(mNetworkChangeReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			}
			
			mWifiReceiver = new WifiStateChangeReceiver();
			if(null != mWifiReceiver){
				IntentFilter intentFilter = new IntentFilter();
				intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
				intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
				intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
				context.registerReceiver(mWifiReceiver, intentFilter);
			}
		}
	}
		
	public String getMacAddress(){
		if(null != mWifiManager){
			WifiInfo info = mWifiManager.getConnectionInfo();
			if(null != info){
				return info.getMacAddress();
			}
		}
		return null;
	}
	
	public String getHotspotName() { 
		String strRet = "";
	    try {
	        Method getConfigMethod = mWifiManager.getClass().getMethod("getWifiApConfiguration");
	        if(null != getConfigMethod){
		        WifiConfiguration wifiConfig = (WifiConfiguration) getConfigMethod.invoke(mWifiManager);
		        if(null != wifiConfig){
		        	strRet = wifiConfig.SSID;
		        }else{
		        	Log.e(TAG, "getHotspotName(), wifiConfig is null:");
		        }
	        }else{
	        	Log.e(TAG, "getHotspotName(), getConfigMethod is null:");
	        }
	    }
	    catch (Exception e) {
	        e.printStackTrace();
        	Log.e(TAG, "getHotspotName(), e:"+e.toString());

	    }
	    return strRet;
	}
	
//	//Only check Mobile/Wifi/Wimax
//	public boolean isNetworkConnected(){
//		boolean bConnected = false;
//		if(null != mConnectivityManager){
//			NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
//	        if (null != activeNetwork && activeNetwork.isConnected()) {
//	        	int iNetworkType = activeNetwork.getType();
//	            if(iNetworkType == ConnectivityManager.TYPE_MOBILE || iNetworkType == ConnectivityManager.TYPE_WIFI || iNetworkType == ConnectivityManager.TYPE_WIMAX || iNetworkType == ConnectivityManager.TYPE_ETHERNET)
//	            	bConnected = true;
//	        } 
//		}
//		return bConnected;
//	}
//	
	public boolean isNetworkConnected() {
        boolean outcome = false;
        if(null != mConnectivityManager){
            NetworkInfo[] networkInfos = mConnectivityManager.getAllNetworkInfo();
            for (NetworkInfo tempNetworkInfo : networkInfos) {
                if (null != tempNetworkInfo && tempNetworkInfo.isConnected()) {
                    outcome = true;
                    break;
                }
            }
    	}

        return outcome;
    }
	
	public String getActiveWifiSSID(){
		if(null != mWifiManager){
			WifiInfo info = mWifiManager.getConnectionInfo();
			if(null != info){
				return BeseyeUtils.removeDoubleQuote(info.getSSID());
			}
		}
		return null;
	}
	
	public String getActiveWifiBSSID(){
		if(null != mWifiManager){
			WifiInfo info = mWifiManager.getConnectionInfo();
			if(null != info){
				return BeseyeUtils.removeDoubleQuote(info.getBSSID());
			}
		}
		return null;
	}
	
	public SupplicantState getActiveSupplicantState(){
		if(null != mWifiManager){
			WifiInfo info = mWifiManager.getConnectionInfo();
			if(null != info){
				return info.getSupplicantState();
			}
		}
		return null;
	}
	
	public List<WifiConfiguration> getAllWifiConfiguration(){
		if(null != mWifiManager){
			return mWifiManager.getConfiguredNetworks();
		}
		return null;
	}
	
	public WifiConfiguration getWifiConfigurationBySSID(String SSID){
		WifiConfiguration config = null;
		List<WifiConfiguration> configs = getAllWifiConfiguration();
		if(null != configs){
			for(WifiConfiguration c : configs){
				if(null != c && c.SSID.equals("\"" + SSID + "\"")){
					config = c;
					Log.i(TAG, "getWifiConfigurationByBSSID(), SSID:<"+c.SSID+
							  ">, BSSID:<"+c.BSSID+
							  ">, hiddenSSID:<"+c.hiddenSSID+
							  ">, wepTxKeyIndex:<"+c.wepTxKeyIndex+
							  ">, wepKeys:<"+c.wepKeys[c.wepTxKeyIndex]+
							  ">, status:<"+c.status+
							  ">, allowedAuthAlgorithms:<"+c.allowedAuthAlgorithms+
							  ">, allowedGroupCiphers:<"+c.allowedGroupCiphers+
							  ">, allowedKeyManagement:<"+c.allowedKeyManagement+
							  ">, allowedPairwiseCiphers:<"+c.allowedPairwiseCiphers+
							  ">, allowedProtocols:<"+c.allowedProtocols+
							  ">, preSharedKey:<"+c.preSharedKey+">");
					break;
				}
			}
		}
		
		return config;
	}
	
	public WifiConfiguration getWifiConfigurationByBSSID(String BSSID){
		WifiConfiguration config = null;
		if(null != BSSID){
			List<WifiConfiguration> configs = getAllWifiConfiguration();
			if(null != configs){
				for(WifiConfiguration c : configs){
					if(null != c && null != c.BSSID && c.BSSID.equalsIgnoreCase("\"" + BSSID + "\"")){
						config = c;
						if(DEBUG){
							Log.i(TAG, "getWifiConfigurationByBSSID(), SSID:<"+c.SSID+
									  ">, BSSID:<"+c.BSSID+
									  ">, hiddenSSID:<"+c.hiddenSSID+
									  ">, wepTxKeyIndex:<"+c.wepTxKeyIndex+
									  ">, wepKeys:<"+c.wepKeys[c.wepTxKeyIndex]+
									  ">, status:<"+c.status+
									  ">, allowedAuthAlgorithms:<"+c.allowedAuthAlgorithms+
									  ">, allowedGroupCiphers:<"+c.allowedGroupCiphers+
									  ">, allowedKeyManagement:<"+c.allowedKeyManagement+
									  ">, allowedPairwiseCiphers:<"+c.allowedPairwiseCiphers+
									  ">, allowedProtocols:<"+c.allowedProtocols+
									  ">, preSharedKey:<"+c.preSharedKey+">");
						}
						break;
					}
				}
			}
		}
		
		return config;
	}
	
	public DetailedState getWifiNetworkStatus(){
		DetailedState dRet = DetailedState.DISCONNECTED;
		if(null != mConnectivityManager){
			NetworkInfo[] allNetworkInfo = mConnectivityManager.getAllNetworkInfo();
			for(NetworkInfo info : allNetworkInfo){
				if(null != info && info.getType() == ConnectivityManager.TYPE_WIFI){
					dRet = info.getDetailedState();
					break;
				}
			}
		}
		return dRet;
	}
	
	public int getWifiStatus(){
		return (null != mWifiManager)?mWifiManager.getWifiState():WifiManager.WIFI_STATE_UNKNOWN;
	}
	
	public boolean connectWifi(WifiAPInfo wifiApinfo, String password){
		if(DEBUG)
			Log.i(TAG, "connectWifi(), SSID:<"+wifiApinfo.SSID+">, password:<"+password+">");
		
		boolean bRet = false;
		int netId = -1;
		
		WifiConfiguration wifiConfig = getWifiConfigurationBySSID(wifiApinfo.SSID);
		if(null == wifiConfig){
			if(DEBUG)
				Log.i(TAG, "connectWifi(), can't find matched wifi config");
			wifiConfig = new WifiConfiguration();
			wifiConfig.SSID = String.format("\"%s\"", wifiApinfo.SSID);	
			if(wifiApinfo.bIsOther){
				wifiConfig.hiddenSSID = true;
			}
			
		}else{
			netId = wifiConfig.networkId;
		}
		if(DEBUG)
			Log.i(TAG, "connectWifi(), wifiConfig:"+wifiConfig.networkId);
		
		if(RELAY_AP_SSID.equals(wifiApinfo.SSID)){
			wifiConfig.priority = 1;
		}
		
		wifiConfig.status = WifiConfiguration.Status.ENABLED;
		if(/*null == wifiApinfo.cipher || */wifiApinfo.cipher.contains(WifiAPInfo.AUTHNICATION_NONE) || wifiApinfo.iCipherIdx == 0){
			wifiConfig.allowedAuthAlgorithms.clear();
			wifiConfig.allowedGroupCiphers.clear();
			wifiConfig.allowedKeyManagement.clear();
			wifiConfig.allowedPairwiseCiphers.clear();
			wifiConfig.allowedProtocols.clear();
			wifiConfig.wepKeys[0] = "";
			wifiConfig.wepTxKeyIndex = 0;
			wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
		}else if(wifiApinfo.cipher.contains(WifiAPInfo.AUTHNICATION_WPA) || wifiApinfo.iCipherIdx > 1){
			wifiConfig.preSharedKey = String.format("\"%s\"", password);
			wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			if(DEBUG)
				Log.i(TAG, "connectWifi(), wifiConfig.preSharedKey:"+wifiConfig.preSharedKey);
		}else if(wifiApinfo.cipher.contains(WifiAPInfo.AUTHNICATION_WEP) || wifiApinfo.iCipherIdx == 1){
			wifiConfig.wepTxKeyIndex = wifiApinfo.wepkeyIdx;
			for(int i = 0; i<4;i++){
				if(i == wifiConfig.wepTxKeyIndex)
					wifiConfig.wepKeys[i] = String.format("\"%s\"", password);
				else
					wifiConfig.wepKeys[i] = null;
			}
			if(DEBUG)
				Log.e(TAG, "connectWifi(), AUTHNICATION_WEP, wifiConfig.wepTxKeyIndex="+wifiConfig.wepTxKeyIndex);
			//wifiConfig.hiddenSSID = false;
			
			wifiConfig.allowedAuthAlgorithms.clear();
			wifiConfig.allowedGroupCiphers.clear();
			wifiConfig.allowedKeyManagement.clear();
			wifiConfig.allowedPairwiseCiphers.clear();
			wifiConfig.allowedProtocols.clear();
			  
			wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			//wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			//wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			wifiConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
			//wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			//wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
			wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			//wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN); 
			//wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
		}
		
		if(0 < wifiConfig.networkId){
			if(DEBUG)
				Log.e(TAG, "connectWifi(), removeNetwork wifi with netId :"+wifiConfig.networkId);
			if(!mWifiManager.removeNetwork(wifiConfig.networkId))
				if(DEBUG)
					Log.e(TAG, "connectWifi(), cannot removeNetwork wifi with netId :"+wifiConfig.networkId);
			
			if(!mWifiManager.saveConfiguration()){
				if(DEBUG)
					Log.e(TAG, "connectWifi(), cannot saveConfiguration");
			}
		}
		
		netId = mWifiManager.addNetwork(wifiConfig);
		if(DEBUG)
			Log.d(TAG, "connectWifi(), netId:"+netId);
		
		if(bRet = mWifiManager.disconnect()){
			if(bRet = mWifiManager.enableNetwork(netId, true)){
				if(false == (bRet = mWifiManager.reconnect())){
					if(DEBUG)
						Log.e(TAG, "connectWifi(), cannot reconnect wifi with netId :"+netId);
				}
			}else{
				if(DEBUG)
					Log.e(TAG, "connectWifi(), cannot enable wifi with netId :"+netId);
			}
		}else{
			Log.e(TAG, "connectWifi(), cannot disconnect wifi");
		}
		mWifiManager.saveConfiguration();
		return bRet;
	}
	
	public boolean turnOnWifi(){
		boolean bRet = false;
		if(null != mWifiManager){
			if(WifiManager.WIFI_STATE_ENABLED != mWifiManager.getWifiState() || WifiManager.WIFI_STATE_ENABLING != mWifiManager.getWifiState()){
				bRet = mWifiManager.setWifiEnabled(true);
			}
		}
		return bRet;
	}
	
	public boolean turnOffWifi(){
		boolean bRet = false;
		if(null != mWifiManager){
			if(WifiManager.WIFI_STATE_DISABLED != mWifiManager.getWifiState() || WifiManager.WIFI_STATE_DISABLING != mWifiManager.getWifiState()){
				bRet = mWifiManager.setWifiEnabled(false);
			}
		}
		return bRet;
	}
	
	public boolean isWifiEnabled(){
		boolean bRet = false;
		if(null != mWifiManager){
			bRet = WifiManager.WIFI_STATE_ENABLED == mWifiManager.getWifiState();
		}
		return bRet;
	}
	
	public void registerNetworkChangeCallback(OnNetworkChangeCallback listerner){
		if(null != mOnNetworkChangeCallbackListeners && null != listerner){
			for(WeakReference<OnNetworkChangeCallback> wrListener : mOnNetworkChangeCallbackListeners){
				OnNetworkChangeCallback checkListener = wrListener.get();
				if(checkListener == listerner){
					if(DEBUG)
						Log.i(TAG, "registerNetworkChangeCallback(), same listener detected :"+listerner.getClass().getSimpleName());
					return;
				}
			}
			
			mOnNetworkChangeCallbackListeners.add(new WeakReference<OnNetworkChangeCallback>(listerner));
		}
	}
	
	public void unregisterNetworkChangeCallback(OnNetworkChangeCallback listerner){
		if(null != mOnNetworkChangeCallbackListeners && null != listerner){
			WeakReference<OnNetworkChangeCallback> targetListener = null;
			for(WeakReference<OnNetworkChangeCallback> wrListener : mOnNetworkChangeCallbackListeners){
				OnNetworkChangeCallback checkListener = wrListener.get();
				if(checkListener == listerner){
					targetListener = wrListener;
					break;
				}
			}
			if(null != targetListener){
				mOnNetworkChangeCallbackListeners.remove(targetListener);
			}else{
				Log.i(TAG, "unregisterNetworkChangeCallback(), can't find same listener:"+listerner.getClass().getSimpleName());
			}
		}
	}
	
	public void notifyNetworkStatusChanged(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				boolean bLatestConnectedState = isNetworkConnected();
				//Mark below condition because some network handover is too soon 
				//if(mbIsNetworkConnected != bLatestConnectedState){
					if(DEBUG)
						Log.i(TAG, "notifyNetworkStatusChanged(), connectivity change to "+bLatestConnectedState);
					for(WeakReference<OnNetworkChangeCallback> wrListener : mOnNetworkChangeCallbackListeners){
						OnNetworkChangeCallback checkListener = wrListener.get();
						if(null != checkListener){
							checkListener.onConnectivityChanged(bLatestConnectedState);
						}
					}
				//}
				mbIsNetworkConnected = bLatestConnectedState;
			}},0);
	}
	
	public void registerWifiStatusChangeCallback(OnWifiStatusChangeCallback listerner){
		synchronized(mOnWifiStatusChangeCallbackListeners){
			if(null != mOnWifiStatusChangeCallbackListeners && null != listerner){
				for(WeakReference<OnWifiStatusChangeCallback> wrListener : mOnWifiStatusChangeCallbackListeners){
					OnWifiStatusChangeCallback checkListener = wrListener.get();
					if(checkListener == listerner){
						if(DEBUG)
							Log.i(TAG, "registerWifiStatusChangeCallback(), same listener detected :"+listerner.getClass().getSimpleName());
						return;
					}
				}
				if(DEBUG)
					Log.i(TAG, "registerWifiStatusChangeCallback(), listener:"+listerner.getClass().getSimpleName());
				mOnWifiStatusChangeCallbackListeners.add(new WeakReference<OnWifiStatusChangeCallback>(listerner));
			}else{
				Log.e(TAG, "registerWifiStatusChangeCallback(), invalid params, mOnWifiStatusChangeCallbackListeners="+mOnWifiStatusChangeCallbackListeners);
			}
		}
	}
	
	public void unregisterWifiStatusChangeCallback(OnWifiStatusChangeCallback listerner){
		if(null != mOnWifiStatusChangeCallbackListeners && null != listerner){
			WeakReference<OnWifiStatusChangeCallback> targetListener = null;
			synchronized(mOnWifiStatusChangeCallbackListeners){
				for(WeakReference<OnWifiStatusChangeCallback> wrListener : mOnWifiStatusChangeCallbackListeners){
					OnWifiStatusChangeCallback checkListener = wrListener.get();
					if(checkListener == listerner){
						targetListener = wrListener;
						break;
					}
				}
				
				if(null != targetListener){
					if(DEBUG)
						Log.i(TAG, "unregisterWifiStatusChangeCallback(), listener:"+listerner.getClass().getSimpleName());
					mOnWifiStatusChangeCallbackListeners.remove(targetListener);
				}else{
					Log.i(TAG, "unregisterWifiStatusChangeCallback(), can't find same listener:"+listerner.getClass().getSimpleName());
				}
			}
		}
	}
	
	public void notifyWifiStatusChanged(final int iLatestWifiStatus){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(DEBUG)
					Log.i(TAG, "notifyWifiStatusChanged(), wifi status change from "+miWifiState+" to "+iLatestWifiStatus);
				if(miWifiState != iLatestWifiStatus){
					synchronized(mOnWifiStatusChangeCallbackListeners){
						for(WeakReference<OnWifiStatusChangeCallback> wrListener : mOnWifiStatusChangeCallbackListeners){
							OnWifiStatusChangeCallback checkListener = wrListener.get();
							if(null != checkListener){
								checkListener.onWifiStateChanged(iLatestWifiStatus, miWifiState);
							}
						}
					}
				}
				miWifiState = iLatestWifiStatus;
			}}, 0);
	}
	
	public void notifyWifiNetworkStatusChanged(final DetailedState iLatestWifiStatus){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(DEBUG)
					Log.i(TAG, "notifyWifiNetworkStatusChanged(), wifi ("+getActiveWifiBSSID()+")status change from "+miWifiNetworkState+" to "+iLatestWifiStatus);
				
				//if(miWifiNetworkState != iLatestWifiStatus){
				synchronized(mOnWifiStatusChangeCallbackListeners){
					//for(WeakReference<OnWifiStatusChangeCallback> wrListener : mOnWifiStatusChangeCallbackListeners){
					if(miWifiNetworkState != iLatestWifiStatus){
						for(int i = 0; i < mOnWifiStatusChangeCallbackListeners.size();i++){
							OnWifiStatusChangeCallback checkListener = mOnWifiStatusChangeCallbackListeners.get(i).get();
							if(null != checkListener){
								if(DEBUG)
									Log.i(TAG, "notifyWifiNetworkStatusChanged(), checkListener="+checkListener.getClass().getSimpleName());
								checkListener.onWifiNetworkStateChanged(iLatestWifiStatus, miWifiNetworkState);
							}
						}
					}
				}
				//}
				miWifiNetworkState = iLatestWifiStatus;
			}}, 0);
	}
	
	public void registerOnSupplicantStatusChangeCallback(OnSupplicantStatusChangeCallback listerner){
		if(null == mOnSupplicantStatusChangeCallbackListeners && null != listerner){
			mOnSupplicantStatusChangeCallbackListeners = new WeakReference<OnSupplicantStatusChangeCallback>(listerner);
		}
	}
	
	public void unregisterOnSupplicantStatusChangeCallback(OnSupplicantStatusChangeCallback listerner){
		if(null != mOnSupplicantStatusChangeCallbackListeners && null != listerner){
			OnSupplicantStatusChangeCallback checkListener = mOnSupplicantStatusChangeCallbackListeners.get();
			if(null != checkListener && checkListener == listerner){
				mOnSupplicantStatusChangeCallbackListeners = null;
			}else{
				Log.i(TAG, "unregisterOnSupplicantStatusChangeCallback(), can't find same listener:"+listerner.getClass().getSimpleName());
			}
		}
	}
	
	public void notifyOnSupplicantStatusChangeCallback(final SupplicantState iLatestSupplicantState){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				String curSSID = getActiveWifiBSSID();
				if(DEBUG)
					Log.i(TAG, "notifyOnSupplicantStatusChangeCallback(), supplicant ("+curSSID+")state change from <"+mSupplicantState+"> to <"+iLatestSupplicantState+">");
				if(mSupplicantState != iLatestSupplicantState){
					OnSupplicantStatusChangeCallback checkListener = (null != mOnSupplicantStatusChangeCallbackListeners)?mOnSupplicantStatusChangeCallbackListeners.get():null;
					if(null != checkListener){
						checkListener.onSupplicantStateChanged(iLatestSupplicantState, mSupplicantState);
					}
				}
				mSupplicantState = iLatestSupplicantState;
				
				if(null != curSSID && !curSSID.equals(mPreviousActiveSSID)){
					if(DEBUG)
						Log.i(TAG, "notifyOnSupplicantStatusChangeCallback(), SSID change from <"+mPreviousActiveSSID+"> to <"+curSSID+">");
					mPreviousActiveSSID = curSSID;
				}
			}}, 0);
	}
	
	public void notifyOnSuppicantAuthenticationError(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				//String SSID = NetworkMgr.getInstance().getActiveWifiSSID();
				if(DEBUG)
					Log.w(TAG, "notifyOnSuppicantAuthenticationError()!!!!!!!!!!!!!, ssid:"+mPreviousActiveSSID);
				OnSupplicantStatusChangeCallback checkListener = (null != mOnSupplicantStatusChangeCallbackListeners)?mOnSupplicantStatusChangeCallbackListeners.get():null;
				if(null != checkListener){
					checkListener.onAuthenticationError(mPreviousActiveSSID);
				}
			}}, 0);
	}
	
	private String getSSIDfromBSSID(String BSSID){
		String strSSID = null;
		if(null != BSSID){
			List<ScanResult> scanRet =getWifiScanList();
			if(null != scanRet){
				for(ScanResult sRet : scanRet){
					if(null != sRet.BSSID && sRet.BSSID.equals(BSSID)){
						strSSID = sRet.SSID;
						break;
					}
				}
			}
		}else{
			Log.e(TAG, "getSSIDfromBSSID(), BSSID is null");
		}
		
		return strSSID;
	}
	public List<ScanResult> getWifiScanList(){
		return (null != mWifiManager)?mWifiManager.getScanResults():null;
	}
	
	public boolean scanWifiList(OnWifiScanResultAvailableCallback listerner){
		boolean bRet = false;
		if(null != mWifiManager){
			if(DEBUG)
				Log.i(TAG, "scanWifiList()");
			registerOnWifiScanResultAvailableCallbackListener(listerner);
			Context context = mWrContext.get();
			if(null != context)
				context.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			bRet = mWifiManager.startScan();
		}
		return bRet;
	}
	
	public boolean cancelScanWifiList(OnWifiScanResultAvailableCallback listerner){
		boolean bRet = false;
		if(null != mWifiManager){
			if(DEBUG)
				Log.i(TAG, "cancelScanWifiList()");
			unregisterOnWifiScanResultAvailableCallbackListener(listerner);
		}
		return bRet;
	}
	
	public void registerOnWifiScanResultAvailableCallbackListener(OnWifiScanResultAvailableCallback listerner){
		if(null != listerner){
			if(DEBUG)
				Log.i(TAG, "registerOnWifiScanResultAvailableCallbackListener()");
			mOnWifiScanResultAvailableCallbackListener = new WeakReference<OnWifiScanResultAvailableCallback>(listerner);
		}
	}
	
	public void unregisterOnWifiScanResultAvailableCallbackListener(OnWifiScanResultAvailableCallback listerner){
		if(null != mOnWifiScanResultAvailableCallbackListener && null != listerner){
			OnWifiScanResultAvailableCallback checkListener = mOnWifiScanResultAvailableCallbackListener.get();
			if(null != checkListener && checkListener == listerner){
				mOnWifiScanResultAvailableCallbackListener = null;
			}else{
				Log.i(TAG, "unregisterOnWifiScanResultAvailableCallbackListener(), can't find same listener:"+listerner.getClass().getSimpleName());
			}
		}
	}
	
	public void notifyWifiScanResultAvailable(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(DEBUG)
					Log.i(TAG, "notifyWifiScanResultAvailable()");
				if(null != mOnWifiScanResultAvailableCallbackListener ){
					OnWifiScanResultAvailableCallback checkListener = mOnWifiScanResultAvailableCallbackListener.get();
					if(null != checkListener){
						checkListener.onWifiScanResultAvailable();
						unregisterOnWifiScanResultAvailableCallbackListener(checkListener);
						Context context = mWrContext.get();
						if(null != context){
							if(null != mWifiReceiver){
								context.unregisterReceiver(mWifiReceiver);
								IntentFilter intentFilter = new IntentFilter();
								intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
								intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
								intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
								context.registerReceiver(mWifiReceiver, intentFilter);
							}
						}
					}
				}
			}}, 0);
	}
	
	static public class ApManager {
		//check whether wifi hotspot on or off
		public static boolean isApOn(Context context) {
		    WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);     
		    try {
		        Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
		        method.setAccessible(true);
		        return (Boolean) method.invoke(wifimanager);
		    }
		    catch (Throwable ignored) {}
		    return false;
		}

		// toggle wifi hotspot on or off
		public static boolean changeApState(Context context, boolean bTurnOn) {
		    WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
		    WifiConfiguration wificonfiguration = null;
		    try {                 
		        Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);                   
		        method.invoke(wifimanager, wificonfiguration, bTurnOn);
		        return true;
		    } 
		    catch (Exception e) {
		        e.printStackTrace();
		    }       
		    return false;
		}
	} // end of class
	
	static public class WifiAPInfo implements Parcelable{
		@Override
		public String toString() {
			return (DEBUG)?("WifiAPInfo [SSID=" + SSID + ", BSSID=" + BSSID+", bIsHiddenSSID="+bIsHiddenSSID
					+ ", cipher=" + cipher + ", password=" + password
					+ ", wepkeyIdx=" + wepkeyIdx + ", signalLevel="
					+ signalLevel + ", frequency=" + frequency
					+ ", bActiveConn=" + bActiveConn + "]"):"";
		}

		static final public int MAX_SIGNAL_LEVEL = 4;
		static final public int MAX_FREQUENCY = 3000;
		static final public String AUTHNICATION_NONE = "ESS";
		static final public String AUTHNICATION_WEP = "WEP";
		static final public String AUTHNICATION_WPA = "WPA";
		static final public String AUTHNICATION_WPA2 = "WPA2";
		static final public String AUTHNICATION_SUB_PSK = "PSK";
		
		static final public int AUTHNICATION_KEY_NONE = 0;
		static final public int AUTHNICATION_KEY_WEP = 1;
		static final public int AUTHNICATION_KEY_WPA = 2;
		static final public int AUTHNICATION_KEY_WPA2 = 3;
		
		public String SSID ="";
		public String BSSID ="";
		public String cipher ="";
		public String password ="";
		public int iCipherIdx = 0;//0:None, 1:WEP, 2:WPA, 3:WPA2
		public int wepkeyIdx;
		public int signalLevel;
		public int frequency;
		public boolean bActiveConn;
		public boolean bIsOther = false;
		public boolean bIsHiddenSSID = false;
		
		public WifiAPInfo() {
			bIsOther = false;
		}
		
		public WifiAPInfo(boolean bIsOther) {
			this.bIsOther = bIsOther;
		}
		
		public WifiAPInfo(Parcel in) {
			readFromParcel(in);
		}

		@Override
		public int describeContents() {
			return 0;
		}
		
		private void readFromParcel(Parcel in) {
			SSID = in.readString();
			BSSID = in.readString();
			cipher = in.readString();
			password = in.readString();
			
			iCipherIdx = in.readInt();
			wepkeyIdx = in.readInt();
			signalLevel = in.readInt();
			frequency = in.readInt();
			bActiveConn = in.readInt()>0?true:false;
			bIsOther = in.readInt()>0?true:false; 
			bIsHiddenSSID = in.readInt()>0?true:false; 
		}
		
		public String getCipher(){ return cipher;}
		
		@Override
		public void writeToParcel(Parcel dest, int flags) {
	 
			// We just need to write each field into the
			// parcel. When we read from parcel, they
			// will come back in the same order
			dest.writeString(SSID);
			dest.writeString(BSSID);
			dest.writeString(cipher);
			dest.writeString(password);
			
			dest.writeInt(iCipherIdx);
			dest.writeInt(wepkeyIdx);
			dest.writeInt(signalLevel);
			dest.writeInt(frequency);
			dest.writeInt(bActiveConn?1:0);
			dest.writeInt(bIsOther?1:0);
			dest.writeInt(bIsHiddenSSID?1:0);
			
		}
		
		public static final Parcelable.Creator<WifiAPInfo> CREATOR = new Parcelable.Creator<WifiAPInfo>() {
	        public WifiAPInfo createFromParcel(Parcel in) {
	            return new WifiAPInfo(in);
	        }

	        public WifiAPInfo[] newArray(int size) {
	            return new WifiAPInfo[size];
	        }
	    };
	}
	
//	public static int translateCipherToType(String cipher){
//		int iRet = -1;
//		if(null != cipher && 0< cipher.length()){
//			if(cipher.contains(WifiAPInfo.AUTHNICATION_NONE)){
//				iRet = 0;
//			}else if(cipher.contains(WifiAPInfo.AUTHNICATION_WEP)){
//				iRet = 1;
//			}else if(cipher.contains(WifiAPInfo.AUTHNICATION_WPA2)){
//				iRet = 3;
//			}else if(cipher.contains(WifiAPInfo.AUTHNICATION_WPA)){
//				iRet = 2;
//			}
//		}
//		
//		return iRet;
//	}
	
	public static String translateCipherTypeToDesc(Context context, int iType){
		String  strRet = context.getString(R.string.dialog_wifi_ap_security_none);
		if(1 == iType){
			strRet = WifiAPInfo.AUTHNICATION_WEP;
		}else if(2 == iType){
			strRet = WifiAPInfo.AUTHNICATION_WPA;
		}else if(3 == iType){
			strRet = WifiAPInfo.AUTHNICATION_WPA2;
		}		
		return strRet;
	}
	
	private void sortWifiApInfo(List<WifiAPInfo> dest){
		if(null != dest){
			Collections.sort(dest, sWiFiInfoNameComparator);
			
			int iNewCount = dest.size();
			
			for(int idx = 1;idx < iNewCount;){
				WifiAPInfo prevInfo = dest.get(idx -1);
				WifiAPInfo curInfo = dest.get(idx);
				if(null != prevInfo && null != curInfo && curInfo.SSID.equals(prevInfo.SSID)){
					if(prevInfo.signalLevel < curInfo.signalLevel){
						if(false == prevInfo.bActiveConn){
							dest.remove(idx -1);
						}else{
							dest.remove(idx);
						}
						
					}else{
						if(false == curInfo.bActiveConn){
							dest.remove(idx);
						}else{
							dest.remove(idx-1);
						}
					}
					iNewCount--;
				}else{
					idx++;
				}
			}
			
			Collections.sort(dest, sWiFiInfoComparator);
			
			iNewCount = dest.size();
			
			for(int idx = 0;idx < iNewCount;idx++){
				WifiAPInfo curInfo = dest.get(idx);
				if(null != curInfo && curInfo.bActiveConn ){
					dest.add(0, dest.remove(idx));
					break;
				}
			}
		}
	}
	
	public void filterWifiAPInfo(List<WifiAPInfo> dest, List<ScanResult> src){
		if(null != dest){
			dest.clear();
			String strAciveBSSID = NetworkMgr.getInstance().getActiveWifiBSSID();
			if(DEBUG)
				Log.i(TAG, "filterWifiAPInfo(), strAciveBSSID = "+strAciveBSSID);
			if(null != src && 0 < src.size()){
				for(ScanResult ret : src){
					if(null != ret){
						if(ret.frequency < WifiAPInfo.MAX_FREQUENCY && 0 < ret.SSID.length()){
							WifiAPInfo info = transformFromScanResult(ret, strAciveBSSID);
							dest.add(info);
						}
					}
				}
				
				sortWifiApInfo(dest);
				
				if(null != dest && 0 < dest.size()){
					WifiAPInfo infoOther = new WifiAPInfo(true);
					dest.add(infoOther);
				}
			}else{
				WifiAPInfo infoOther = new WifiAPInfo(true);
				dest.add(infoOther);
			}
		}
	}
	
	public void filterWifiAPInfo(List<WifiAPInfo> dest, JSONArray src, String strAciveSSID){
		filterWifiAPInfo(dest, src, strAciveSSID, "");
	}
	
	public void filterWifiAPInfo(List<WifiAPInfo> dest, JSONArray src, String strAciveSSID, String strAciveBSSID){
		if(null != dest){
			dest.clear();
			//String strAciveBSSID = NetworkMgr.getInstance().getActiveWifiBSSID();
			if(DEBUG)
				Log.i(TAG, "filterWifiAPInfo(), strAciveSSID = "+strAciveSSID+", strAciveBSSID = "+strAciveBSSID);
			if(null != src){
				int iCount = src.length();
				for(int idx = 0;idx< iCount;idx++){
					JSONObject ret;
					try {
						ret = src.getJSONObject(idx);
						if(null != ret){
							WifiAPInfo info = transformFromScanResult(ret, strAciveSSID, strAciveBSSID);
							dest.add(info);
						}
					} catch (JSONException e) {
						Log.i(TAG, "filterWifiAPInfo(), e = "+e.toString());
						
					}
				}
				
				sortWifiApInfo(dest);
				
				if(null != dest && 0 < dest.size()){
					WifiAPInfo infoOther = new WifiAPInfo(true);
					dest.add(infoOther);
				}
			}
		}
	}
	
	static private WiFiInfoNameComparator sWiFiInfoNameComparator = new WiFiInfoNameComparator();
	static private WiFiInfoComparator sWiFiInfoComparator = new WiFiInfoComparator();
	
	private static class WiFiInfoNameComparator implements Comparator<WifiAPInfo> {
        public int compare(WifiAPInfo info1, WifiAPInfo info2) {
        	return info2.SSID.compareTo(info1.SSID);
        }
    }
	
	private static class WiFiInfoComparator implements Comparator<WifiAPInfo> {
        public int compare(WifiAPInfo info1, WifiAPInfo info2) {
        	return info1.signalLevel > info2.signalLevel ?-1:(info1.signalLevel == info2.signalLevel?0:1);
        }
    }
	
	public WifiAPInfo getWifiAPInfoBySSID(String SSID){
		WifiAPInfo retInfo = null;
		List<ScanResult> src = getWifiScanList();
		for(ScanResult ret : src){
			if(null != ret){
				if(ret.frequency < WifiAPInfo.MAX_FREQUENCY && ret.SSID.equals(SSID)){
					retInfo = transformFromScanResult(ret, NetworkMgr.getInstance().getActiveWifiBSSID());
					break;
				}
			}
		}
		return retInfo;
	}
	
	public boolean findWifiAPBySSID(String SSID){
		boolean bRet = false;
		List<ScanResult> src = getWifiScanList();
		for(ScanResult ret : src){
			if(null != ret){
				if(ret.frequency < WifiAPInfo.MAX_FREQUENCY && ret.SSID.equals(SSID)){
					bRet = true;
					break;
				}
			}
		}
		return bRet;
	}
	
	private WifiAPInfo transformFromScanResult(ScanResult ret, String activeSSID){
		WifiAPInfo retInfo = null;
		if(null != ret){
			retInfo = new WifiAPInfo();
			retInfo.SSID = ret.SSID;
			retInfo.BSSID = ret.BSSID;
			retInfo.bActiveConn = ret.BSSID.equals(activeSSID);
			retInfo.frequency = ret.frequency;
			WifiConfiguration wifiConfig = getWifiConfigurationBySSID(retInfo.SSID);
			if(null != wifiConfig){
				retInfo.bIsHiddenSSID = wifiConfig.hiddenSSID;
			}else{
//				if(DEBUG){
//					Log.e(TAG, "transformFromScanResult(), can not find config for ret.SSID "+retInfo.SSID);
//				}
			}
			retInfo.signalLevel = WifiManager.calculateSignalLevel(ret.level, WifiAPInfo.MAX_SIGNAL_LEVEL);
			
			retInfo.iCipherIdx = 0;
			String strCipher = WifiAPInfo.AUTHNICATION_NONE;
			if(null != ret.capabilities){
				int iWPAIdx = -1;
				int iWPA2Idx = -1;
				if(ret.capabilities.contains(WifiAPInfo.AUTHNICATION_WEP)){
					strCipher = WifiAPInfo.AUTHNICATION_WEP;
					retInfo.iCipherIdx = 1;
				}else if(-1 < (iWPAIdx = ret.capabilities.indexOf(WifiAPInfo.AUTHNICATION_WPA))){
					if(-1 < (iWPA2Idx = ret.capabilities.indexOf(WifiAPInfo.AUTHNICATION_WPA2))){
						if(iWPA2Idx != iWPAIdx)
							strCipher = WifiAPInfo.AUTHNICATION_WPA+"/"+WifiAPInfo.AUTHNICATION_WPA2+" "+WifiAPInfo.AUTHNICATION_SUB_PSK;
						else
							strCipher = WifiAPInfo.AUTHNICATION_WPA2+" "+WifiAPInfo.AUTHNICATION_SUB_PSK;
						retInfo.iCipherIdx = 3;
					}else{
						strCipher = WifiAPInfo.AUTHNICATION_WPA+" "+WifiAPInfo.AUTHNICATION_SUB_PSK;
						retInfo.iCipherIdx = 2;
					}
				}
			}
			retInfo.cipher = strCipher;
			//Log.i(TAG, "transformFromScanResult(), ret.SSID=<"+ret.SSID+">, ret.BSSID=<"+ret.BSSID+">, ret.capabilities=<"+ret.capabilities+">, strCipher=<"+strCipher+">");
		}
		return retInfo;
	}
	
	private WifiAPInfo transformFromScanResult(JSONObject ret, String activeSSID, String activeBSSID){
		WifiAPInfo retInfo = null;
		if(null != ret){
			retInfo = new WifiAPInfo();
			retInfo.SSID = BeseyeJSONUtil.getJSONString(ret, BeseyeJSONUtil.WIFI_SSIDLST_ID);
			retInfo.BSSID = BeseyeJSONUtil.getJSONString(ret, BeseyeJSONUtil.WIFI_SSIDLST_BSSID);
			retInfo.bActiveConn = retInfo.SSID.equals(activeSSID) && ((null == activeBSSID || 0 == activeBSSID.length()) || activeBSSID.equalsIgnoreCase(retInfo.BSSID));
			
			//retInfo.frequency = ret.frequency;
			retInfo.signalLevel = WifiManager.calculateSignalLevel(BeseyeJSONUtil.getJSONInt(ret, BeseyeJSONUtil.WIFI_SSIDLST_SGL), WifiAPInfo.MAX_SIGNAL_LEVEL);
			String strCipher = WifiAPInfo.AUTHNICATION_NONE;
			int iSecType = retInfo.iCipherIdx =BeseyeJSONUtil.getJSONInt(ret, BeseyeJSONUtil.WIFI_SSIDLST_SEC);
			if(1 == iSecType){
				strCipher = WifiAPInfo.AUTHNICATION_WEP;
			}else if(2 == iSecType){
				strCipher = WifiAPInfo.AUTHNICATION_WPA;
			}else if(3 == iSecType){
				strCipher = WifiAPInfo.AUTHNICATION_WPA2;
			}
			retInfo.cipher = strCipher;
			//Log.i(TAG, "transformFromScanResult(), ret.SSID=<"+ret.SSID+">, ret.BSSID=<"+ret.BSSID+">, ret.capabilities=<"+ret.capabilities+">, strCipher=<"+strCipher+">");
		}
		return retInfo;
	}
	
	public int getSignalStrengthTermId(int ilevel){
		int iId = R.string.dialog_wifi_ap_signal_Excellent;
		switch(ilevel){
			case 0:
				iId = R.string.dialog_wifi_ap_signal_Poor;
				break;
			case 1:
				iId = R.string.dialog_wifi_ap_signal_Fair;
				break;
			case 2:
				iId = R.string.dialog_wifi_ap_signal_Good;
				break;
		}
		return iId;
	}
	
	public int getActiveConnStateId(){
		DetailedState state = NetworkMgr.getInstance().getWifiNetworkStatus();
		int iId = R.string.network_connecting;
		switch(state){
			case SUSPENDED:
				iId = R.string.network_suspended;
				break;
			case FAILED:
				iId = R.string.network_failed;
				break;
			case IDLE:
				iId = R.string.network_idle;
				break;
			case SCANNING:
				iId = R.string.network_scanning;
				break;
			case AUTHENTICATING:
				iId = R.string.network_authenticating;
				break;
			case BLOCKED:
				iId = R.string.network_blocked;
				break;
			case CONNECTED:
				iId = R.string.network_connected;
				break;
			case CONNECTING:
				iId = R.string.network_connecting;
				break;
			case DISCONNECTED:
				iId = R.string.network_disconnected;
				break;
			case DISCONNECTING:
				iId = R.string.network_disconnecting;
				break;	
			case OBTAINING_IPADDR:
				iId = R.string.network_obtaining_ip;
				break;	
		}
		return iId;
	}
	
	public int getSignalLevelDrawableId(int level){
		int iId = R.drawable.sl_wifi_signal_icon_4;
		switch(level){
			case 0:
				iId = R.drawable.sl_wifi_signal_icon_1;
				break;
			case 1:
				iId = R.drawable.sl_wifi_signal_icon_2;
				break;
			case 2:
				iId = R.drawable.sl_wifi_signal_icon_3;
				break;
		}
		return iId;
	}
	
	//for wifi channel transform
	private final static ArrayList<Integer> channelsFrequency = new ArrayList<Integer>(
	        Arrays.asList(0, 2412, 2417, 2422, 2427, 2432, 2437, 2442, 2447,
	                2452, 2457, 2462, 2467, 2472, 2484));

	public static Integer getFrequencyFromChannel(int channel) {
	    return channelsFrequency.get(channel);
	}

	public static int getChannelFromFrequency(int frequency) {
	    return channelsFrequency.indexOf(Integer.valueOf(frequency));
	}
	
	static public interface OnPingCallback{
		public void onPingResultCallback(AsyncTask task, boolean bSuccess);
	}
      
    static public class NetPingTask extends AsyncTask<String, String, String> { 
    	private WeakReference<OnPingCallback> mOnPingCallback;
    	
    	public NetPingTask(OnPingCallback cb){
    		mOnPingCallback = new WeakReference<OnPingCallback>(cb);
    	}
    	
        @Override  
        protected String doInBackground(String... params) {  
            String s = "";  
            s = Ping("tw.yahoo.com");  
            if(DEBUG)
            	Log.i(TAG, "NetPing::doInBackground(), ret:"+s);  
            OnPingCallback cb = (null != mOnPingCallback)?mOnPingCallback.get():null;
            if(null != cb){
            	cb.onPingResultCallback(this, "success".equals(s));
            }
            return s;  
        }  
    }  
    
    //for ping
  	public static String Ping(String str) {  
          String resault = "";  
          Process p;  
          try {  
              p = Runtime.getRuntime().exec("ping -c 3 -w 100 " + str);  
              int status = p.waitFor();  

              InputStream input = p.getInputStream();  
              BufferedReader in = new BufferedReader(new InputStreamReader(input));  
              StringBuffer buffer = new StringBuffer();  
              String line = "";  
              while ((line = in.readLine()) != null){  
                buffer.append(line);  
              }  
              System.out.println("Return ============" + buffer.toString());  

              if (status == 0) {  
                  resault = "success";  
              } else {  
                  resault = "faild";  
              }  
          } catch (IOException e) {  
              e.printStackTrace();  
          } catch (InterruptedException e) {  
              e.printStackTrace();  
          }      
          return resault;  
      }  
}
