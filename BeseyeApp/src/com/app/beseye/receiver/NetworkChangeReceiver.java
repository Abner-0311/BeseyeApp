package com.app.beseye.receiver;

import com.app.beseye.util.NetworkMgr;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class NetworkChangeReceiver extends BroadcastReceiver {
	@Override
	public void onReceive( Context context, Intent intent) {
		if(null != intent){
    		if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)){
    			NetworkMgr.getInstance().notifyNetworkStatusChanged();
    		}
    	}
	}
}
