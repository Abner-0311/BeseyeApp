package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.PS_CUSTOM_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.PS_REGULAR_DATA;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.app.beseye.httptask.BeseyePushServiceTask;
import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.google.android.gcm.GCMBaseIntentService;

public class GCMIntentService extends GCMBaseIntentService{
	public static final String FORWARD_GCM_MSG_ACTION 			= "com.app.beseye.FORWARD_GCM_MSG_ACTION";
	public static final String FORWARD_ACTION_TYPE 	  			= "FORWARD_ACTION_TYPE";
	public static final String FORWARD_ACTION_TYPE_REG 	  		= "onRegistered";
	public static final String FORWARD_ACTION_TYPE_UNREG 		= "onUnregistered";
	public static final String FORWARD_ACTION_TYPE_MSG 			= "onMessage";
	public static final String FORWARD_ACTION_TYPE_ERR 			= "onError";
	public static final String FORWARD_ACTION_TYPE_ERR_MSG  	= "ErrorMsg";
	public static final String FORWARD_ACTION_TYPE_CHECK_DIALOG = "CheckDialog";
	
	static public String SENDER_ID = "309705516409";
	
	public GCMIntentService(){
		super(SENDER_ID);
	}
	
	static public void updateSenderId(String id){
		SENDER_ID = id;
	}
	
	@Override
	protected void onRegistered(Context context, String registrationId) {
		forwardGCMMessage(context, FORWARD_ACTION_TYPE_REG, registrationId, null);
	}

	@Override
	protected void onUnregistered(Context context, String registrationId) {
		forwardGCMMessage(context, FORWARD_ACTION_TYPE_UNREG, registrationId, null);
	}
	
	@Override
	protected void onMessage(Context context, Intent intent) {
		forwardGCMMessage(context, FORWARD_ACTION_TYPE_MSG, "", intent);
	}
	
	@Override
	protected void onError(Context context, String arg1) {
		forwardGCMMessage(context, FORWARD_ACTION_TYPE_ERR, arg1, null);
	}
	
//	@Override
//	protected String[] getSenderIds(Context context) {
//		return new String[]{SENDER_ID};
//	}


	@Override
	protected void onDeletedMessages(Context context, int total) {
		super.onDeletedMessages(context, total);
	}

	@Override
	protected boolean onRecoverableError(Context context, String errorId) {
		return super.onRecoverableError(context, errorId);
	}
	
	static void forwardGCMMessage(Context context, String type, String strValue, Intent msg) {
		Log.i(BeseyeConfig.TAG, "forwardGCMMessage(), type "+type+", strValue = "+strValue);
		
//		final String data = msg.getExtras().getString(PS_REGULAR_DATA);
//    	final String dataCus = msg.getExtras().getString(PS_CUSTOM_DATA);
//    	BeseyeUtils.postRunnable(new Runnable(){
//
//			@Override
//			public void run() {
//				Toast.makeText(BeseyeApplication.getApplication(), "Got message from Beseye server, data = "+data+", dataCus : "+dataCus, Toast.LENGTH_LONG ).show();
//			}}, 0);
//    	
//		
        Intent intent = new Intent(FORWARD_GCM_MSG_ACTION);
        intent.putExtra(FORWARD_ACTION_TYPE, type);
        if(FORWARD_ACTION_TYPE_REG.equals(type) || FORWARD_ACTION_TYPE_UNREG.equals(type))
        	intent.putExtra(BeseyeNotificationService.PUSH_SERVICE_REG_ID, strValue);
        else if(FORWARD_ACTION_TYPE_MSG.equals(type)){
        	Bundle b = msg.getExtras();
        	intent.putExtras(b);
//        	intent.putExtra(BeseyeJSONUtil.PS_REGULAR_DATA, msg.getStringExtra(BeseyeJSONUtil.PS_REGULAR_DATA));
//        	intent.putExtra(BeseyeJSONUtil.PS_CUSTOM_DATA, msg.getStringExtra(BeseyeJSONUtil.PS_CUSTOM_DATA));
        }else{
        	intent.putExtra(FORWARD_ACTION_TYPE_ERR_MSG, strValue);
        }
        context.sendBroadcast(intent);
    }

}
