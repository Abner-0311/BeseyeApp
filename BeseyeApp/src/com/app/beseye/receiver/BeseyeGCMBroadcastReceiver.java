package com.app.beseye.receiver;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import android.content.Context;
import android.util.Log;

import com.app.beseye.GCMIntentService;
import com.app.beseye.util.BeseyeConfig;
import com.google.android.gcm.GCMBroadcastReceiver;

public class BeseyeGCMBroadcastReceiver extends GCMBroadcastReceiver {
	
	public BeseyeGCMBroadcastReceiver() {
        super();
        if(DEBUG)
        	Log.e(BeseyeConfig.TAG, "BeseyeGCMBroadcastReceiver::BeseyeGCMBroadcastReceiver()");
    }
	
	@Override
	protected String getGCMIntentServiceClassName(Context context){
		if(DEBUG)
			Log.e(BeseyeConfig.TAG, "BeseyeGCMBroadcastReceiver::getGCMIntentServiceClassName(), GCMIntentService.class.getName():"+GCMIntentService.class.getName());
		return GCMIntentService.class.getName();
	}
}
