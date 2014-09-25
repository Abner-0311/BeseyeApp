package com.app.beseye.receiver;

import android.content.Context;
import android.util.Log;

import com.app.beseye.GCMIntentService;
import com.app.beseye.util.BeseyeConfig;
import com.google.android.gcm.GCMBroadcastReceiver;

public class BeseyeGCMBroadcastReceiver extends GCMBroadcastReceiver {
	
	public BeseyeGCMBroadcastReceiver() {
        super();
		Log.e(BeseyeConfig.TAG, "BeseyeGCMBroadcastReceiver::BeseyeGCMBroadcastReceiver()");
    }
	
	@Override
	protected String getGCMIntentServiceClassName(Context context){
		Log.e(BeseyeConfig.TAG, "BeseyeGCMBroadcastReceiver::getGCMIntentServiceClassName(), GCMIntentService.class.getName():"+GCMIntentService.class.getName());
	    return GCMIntentService.class.getName();
	}
}
