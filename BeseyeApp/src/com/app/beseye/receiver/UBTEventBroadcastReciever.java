package com.app.beseye.receiver;


import com.app.beseye.BeseyeApplication;
import com.app.beseye.util.BeseyeConfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class UBTEventBroadcastReciever extends BroadcastReceiver {
	static final public String ACTION_UBT_EVENT  = "com.app.beseye.ubt.EVENT_BROADCAST";
	static final public String UBT_EVENT_OBJ 	 = "UBT_EVENT_OBJ";
	static final public String UBT_EVENT_SESSION = "UBT_EVENT_SESSION";
	
	static final public String UBT_EVENT_BEGIN 	 		 = "UBT_EVENT_BEGIN";
	static final public String UBT_EVENT_END 	 		 = "UBT_EVENT_END";
	static final public String UBT_EVENT_VISIBLE_ADD 	 = "UBT_EVENT_VISIBLE_ADD";
	static final public String UBT_EVENT_VISIBLE_DEC 	 = "UBT_EVENT_VISIBLE_DEC";
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (ACTION_UBT_EVENT.equals(intent.getAction())) {  
			//UBT_Event event = intent.getParcelableExtra("UBT_EVENT_OBJ");
			if(BeseyeConfig.DEBUG)
				Log.d("Flurry", "onReceive(), Extra :"+(null != intent.getExtras()?intent.getExtras():"empty"));
			
			if(intent.getBooleanExtra(UBT_EVENT_BEGIN, false)){
				//UBT_Instance.getInstance().EventBegin(event, intent.getIntExtra(UBT_EVENT_SESSION, PageMode.HomePage.ordinal()));
			}else if(intent.getBooleanExtra(UBT_EVENT_END, false)) {
				//UBT_Instance.getInstance().EventEnd(event, intent.getIntExtra(UBT_EVENT_SESSION, PageMode.HomePage.ordinal()));
			}else if(intent.getBooleanExtra(UBT_EVENT_VISIBLE_ADD, false)) {
				BeseyeApplication.increVisibleCount(context);
			}else if(intent.getBooleanExtra(UBT_EVENT_VISIBLE_DEC, false)) {
				BeseyeApplication.decreVisibleCount(context);
			}
		}  
	}
}
