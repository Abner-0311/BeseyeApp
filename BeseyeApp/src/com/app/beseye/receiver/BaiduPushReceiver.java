package com.app.beseye.receiver;

import static com.app.beseye.util.BeseyeConfig.DEBUG;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.app.beseye.GCMIntentService;
import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.baidu.android.pushservice.PushMessageReceiver;

public class BaiduPushReceiver extends PushMessageReceiver{

	public static final String FORWARD_ACTION_TYPE_BAIDU_REG 	  	= "onBaiduRegistered";
	public static final String FORWARD_ACTION_TYPE_BAIDU_UNREG 		= "onBaiduUnregistered";
	public static final String FORWARD_ACTION_TYPE_BAIDU_MSG 		= "onBaiduMessage";
	public static final String BAIDU_USER_ID						= "BaiduUserID";
	public static final String BAIDU_CHANNEL_ID						= "BaiduChannelId";
	
	@Override
	public void onBind(Context context, int errorCode, String appid, String userId, String channelId, String requestId) {
		String responseString = "errorCode=" + errorCode + " appid=" + appid + " userId=" + userId + " channelId=" + channelId + " requestId=" + requestId;
		if(DEBUG)
			Log.i(BeseyeConfig.TAG, "Baidu onBind " + responseString);
       
		Log.d(BeseyeConfig.TAG, "Kelly Baidu onBind errorCode " + errorCode);
		
        if (errorCode == 0) {
        	forwardBaiduMessage(context, FORWARD_ACTION_TYPE_BAIDU_REG, userId, channelId);
        }
	}


	@Override
	public void onMessage(Context context, String message, String customContentString) {
        String messageString = "message=\"" + message + "\" customContentString=" + customContentString;
        if(DEBUG)
			Log.i(BeseyeConfig.TAG, "Baidu onMessage " + messageString);
        
        JSONObject obj;
        String rdata = "", cdata = "";
		try {
			obj = new JSONObject(message);
			rdata = obj.getString(BeseyeJSONUtil.PS_REGULAR_DATA);
			cdata = obj.getString(BeseyeJSONUtil.PS_CUSTOM_DATA);
		} catch (JSONException e) {
			e.printStackTrace();
		}	
		
		Log.d(BeseyeConfig.TAG, "Kelly Baidu onMessage");
		
        forwardBaiduMessage(context, FORWARD_ACTION_TYPE_BAIDU_MSG, rdata, cdata);
    }

	@Override
	   public void onUnbind(Context context, int errorCode, String requestId) {
        String responseString = "errorCode=" + errorCode + " requestId = " + requestId;
        if(DEBUG)
			Log.i(BeseyeConfig.TAG, "Baidu onUnbind " + responseString);
    
        Log.d(BeseyeConfig.TAG, "Kelly Baidu onUnbind");
        
        if (errorCode == 0) {
        	forwardBaiduMessage(context, FORWARD_ACTION_TYPE_BAIDU_UNREG, "", "");
        }
    }

	static void forwardBaiduMessage(Context context, String type, String strValue, String strValue2) {
		if(DEBUG)
			Log.i(BeseyeConfig.TAG, "Baidu forwardBaiduMessage(), type "+type+", strValue = "+strValue);
		
	    Intent intent = new Intent(GCMIntentService.FORWARD_PUSH_MSG_ACTION);
        intent.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, type);
        
        if(FORWARD_ACTION_TYPE_BAIDU_UNREG.equals(type)) {
        	intent.putExtra(BeseyeNotificationService.PUSH_SERVICE_REG_ID, strValue);
        } else if(FORWARD_ACTION_TYPE_BAIDU_REG.equals(type)){
        	Bundle b = new Bundle();
            b.putString(BAIDU_USER_ID, strValue);
            b.putString(BAIDU_CHANNEL_ID, strValue2);
            
        	intent.putExtras(b);
        } else if(FORWARD_ACTION_TYPE_BAIDU_MSG.equals(type)){
        	Bundle b = new Bundle();
            b.putString(BeseyeJSONUtil.PS_REGULAR_DATA, strValue);
            b.putString(BeseyeJSONUtil.PS_CUSTOM_DATA, strValue2);
            
        	intent.putExtras(b);
        }
        context.sendBroadcast(intent);
    }

	

	@Override
	public void onDelTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
	
	}


	@Override
	public void onListTags(Context arg0, int arg1, List<String> arg2, String arg3) {
		
	}


	@Override
	public void onNotificationArrived(Context arg0, String arg1, String arg2, String arg3) {
		
	}


	@Override
	public void onNotificationClicked(Context arg0, String arg1, String arg2, String arg3) {
		
	}


	@Override
	public void onSetTags(Context arg0, int arg1, List<String> arg2, List<String> arg3, String arg4) {
		
	}
}
