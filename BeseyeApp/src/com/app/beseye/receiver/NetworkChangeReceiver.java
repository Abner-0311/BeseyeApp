package com.app.beseye.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.NetworkMgr;

public class NetworkChangeReceiver extends BroadcastReceiver {
	@Override
	public void onReceive( Context context, Intent intent) {
		if(BeseyeConfig.DEBUG)
			Log.d(BeseyeConfig.TAG, "NetworkChangeReceiver::onReceive(), action:"+intent.getAction());
		
		if(null != intent){
    		if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
    			NetworkMgr.getInstance().notifyNetworkStatusChanged();
    		}
    	}
	}
}
