package com.app.beseye.receiver;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import com.app.beseye.service.BeseyeNotificationService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if(action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			if(DEBUG)
				Log.i(TAG, "*****************BootBroadcastReceiver::onReceive(), ACTION_BOOT_COMPLETED invoked");
			context.startService(new Intent(context,BeseyeNotificationService.class));
		}
	}
}
