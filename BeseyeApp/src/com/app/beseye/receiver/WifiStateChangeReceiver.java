package com.app.beseye.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.NetworkMgr;

public class WifiStateChangeReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		if(BeseyeConfig.DEBUG)
			Log.d(BeseyeConfig.TAG, "WifiStateChangeReceiver::onReceive(), action:"+intent.getAction());
		if(null != intent){
    		if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
    			NetworkMgr.getInstance().notifyWifiScanResultAvailable();
    		}else if(intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)){
        		NetworkMgr.getInstance().notifyWifiStatusChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,WifiManager.WIFI_STATE_UNKNOWN));
    		}else if(intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){
    			DetailedState state=((NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
        		NetworkMgr.getInstance().notifyWifiNetworkStatusChanged(state);
    		}else if(intent.getAction().equals(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)){
    			SupplicantState supl_state=((SupplicantState)intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
    			NetworkMgr.getInstance().notifyOnSupplicantStatusChangeCallback(supl_state);
    			int supl_error=intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                if(supl_error == WifiManager.ERROR_AUTHENTICATING){
                    NetworkMgr.getInstance().notifyOnSuppicantAuthenticationError();
                }
    		}
    	}
	}
}
