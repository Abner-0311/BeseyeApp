package com.app.beseye;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.util.BeseyeConfig;
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
		
        Intent intent = new Intent(FORWARD_GCM_MSG_ACTION);
        intent.putExtra(FORWARD_ACTION_TYPE, type);
        if(FORWARD_ACTION_TYPE_REG.equals(type) || FORWARD_ACTION_TYPE_UNREG.equals(type))
        	intent.putExtra(BeseyeNotificationService.PUSH_SERVICE_REG_ID, strValue);
        else if(FORWARD_ACTION_TYPE_MSG.equals(type)){
        	intent.putExtras(msg.getExtras());
        }else{
        	intent.putExtra(FORWARD_ACTION_TYPE_ERR_MSG, strValue);
        }
        context.sendBroadcast(intent);
    }

}
