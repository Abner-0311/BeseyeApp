package com.app.beseye;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.app.beseye.service.BeseyeNotificationService;
import com.app.beseye.util.BeseyeJSONUtil;
import com.baidu.android.pushservice.PushMessageReceiver;

public class BaiduPushReceiver extends PushMessageReceiver{

	@Override
	public void onBind(Context context, int errorCode, String appid, String userId, String channelId, String requestId) {
		String responseString = "onBind errorCode=" + errorCode + " appid="
				+ appid + " userId=" + userId + " channelId=" + channelId
                + " requestId=" + requestId;
        Log.d(TAG, "Kelly responseString" + responseString);

        if (errorCode == 0) {
        	Log.d(TAG, "Kelly reg succ!");
            // 绑定成功
        	//TODO: to Chris
        }
        
        Intent intent = new Intent(GCMIntentService.FORWARD_GCM_MSG_ACTION);
        intent.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, GCMIntentService.FORWARD_ACTION_TYPE_MSG);
     
        Bundle b = new Bundle();
        b.putString(BeseyeJSONUtil.PS_REGULAR_DATA, responseString);
        b.putString(BeseyeJSONUtil.PS_CUSTOM_DATA, (errorCode==0? "1" : "0"));
        intent.putExtras(b);
        context.sendBroadcast(intent);	
        
        // Demo更新界面展示代码，应用请在这里加入自己的处理逻辑
        // updateContent(context, responseString);
	}


	@Override
	public void onMessage(Context context, String message, String customContentString) {
        String messageString = "透传消息 message=\"" + message + "\" customContentString=" + customContentString;
        Log.d(TAG, "Kelly messageString" + messageString);

        // 自定义内容获取方式，mykey和myvalue对应透传消息推送时自定义内容中设置的键和值
        if (!TextUtils.isEmpty(customContentString)) {
            JSONObject customJson = null;
            try {
                customJson = new JSONObject(customContentString);
                String myvalue = null;
                if (!customJson.isNull("mykey")) {
                    myvalue = customJson.getString("mykey");
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        Intent intent = new Intent(GCMIntentService.FORWARD_GCM_MSG_ACTION);
        intent.putExtra(GCMIntentService.FORWARD_ACTION_TYPE, GCMIntentService.FORWARD_ACTION_TYPE_MSG);
     
        Bundle b = new Bundle();
        b.putString(BeseyeJSONUtil.PS_REGULAR_DATA, message);
        b.putString(BeseyeJSONUtil.PS_CUSTOM_DATA, "Kelly");
        intent.putExtras(b);
        context.sendBroadcast(intent);	
    }

	@Override
	public void onUnbind(Context arg0, int arg1, String arg2) {
		// TODO Auto-generated method stub	
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
