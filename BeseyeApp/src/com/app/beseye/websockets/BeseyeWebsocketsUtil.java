package com.app.beseye.websockets;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Build;
import android.util.Log;

import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;

public class BeseyeWebsocketsUtil {
    static public final String WS_CMD_FORMAT 			= "[\"%s\", %s]";
    
    static public final String WS_CB_CLIENT_CONNECTION 	= "client_connected";
    static public final String WS_CB_KEEP_ALIVE  		= "wss_keep_alive";
    static public final String WS_CB_ACK  				= "wss_ack";
    static public final String WS_CB_EVT  				= "wss_evt";
    
    
    static public final String WS_FUNC_CONNECTED 		= "wsc_connected";
    static public final String WS_FUNC_AUTH 			= "wsc_auth";
    static public final String WS_FUNC_KEEP_ALIVE 		= "wsc_keep_alive";
    static public final String WS_FUNC_LOOPBACK 		= "wsc_loopback";
    static public final String WS_FUNC_RAILS_PING 		= "websocket_rails.ping";

    static public final String WS_ATTR_DATA 			= "data";

    static public final String WS_ATTR_TYPE 			= "Type";
    static public final int WS_ATTR_TYPE_GET 			= 0;
    static public final int WS_ATTR_TYPE_SET 			= 1;

    static public final String WS_ATTR_COMM 			= "Command";
    static public final int WS_ATTR_COMM_AUTH 			= 0x0100;

    static public final String WS_ATTR_JOB_ID 			= "JobID";
    static public final String WS_ATTR_CODE 			= "Code";
    static public final String WS_ATTR_MSG 				= "Msg";

    static public final String WS_ATTR_INTERNAL_DATA 	= "Data";
    static public final String WS_ATTR_CONN_ID 			= "connection_id";

    static public final String WS_ATTR_USER_ID 			= "user_id";
    static public final String WS_ATTR_DEV_TYPE 		= "device_type";
    static public final String WS_ATTR_DEV_NAME 		= "device_name";
    static public final String WS_ATTR_DEV_ID 			= "device_id";
    static public final String WS_ATTR_DEV_MAC 			= "device_macaddr";
    static public final String WS_ATTR_SES_TOKEN 		= "session_token";
    
    static private int job_id_seed = 0;

    static public synchronized JSONObject getJsonObjWithJobID(){
    	JSONObject ret_obj = new JSONObject();
    	if(null != ret_obj){
    		try {
				ret_obj.put(WS_ATTR_JOB_ID, String.format("job%08x", ++job_id_seed));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	return ret_obj;
    }

    static public JSONObject wrapWSBaseMsg(){
    	JSONObject ret_obj = new JSONObject();
    	if(null != ret_obj){
    		JSONObject job_obj = getJsonObjWithJobID();
    		if(null != job_obj){
    			try {
					ret_obj.put(WS_ATTR_DATA, job_obj);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}else{
    			Log.e(TAG,  "job_obj is null\n");
    		}
    	}
    	else{
    		Log.e( TAG,"ret_obj is null\n");
    	}
    	return ret_obj;
    }
    
    static public JSONObject genAuthMsg(){
    	JSONObject ret_obj = new JSONObject();
    	if(null != ret_obj){
    		JSONObject job_obj = getJsonObjWithJobID();
    		if(null != job_obj){
    			try {
    				JSONObject data_obj = new JSONObject();
    				data_obj.put(WS_ATTR_USER_ID, SessionMgr.getInstance().getMdid()+"");
    				data_obj.put(WS_ATTR_DEV_TYPE, 1);//1 means Mobile
    				data_obj.put(WS_ATTR_DEV_NAME, Build.MODEL);
    				data_obj.put(WS_ATTR_DEV_ID, BeseyeUtils.getAndroidUUid());
    				data_obj.put(WS_ATTR_DEV_MAC, NetworkMgr.getInstance().getMacAddress());
    				data_obj.put(WS_ATTR_SES_TOKEN, SessionMgr.getInstance().getAuthToken());
    				
    				job_obj.put(WS_ATTR_TYPE, WS_ATTR_TYPE_SET);
    				job_obj.put(WS_ATTR_COMM, WS_ATTR_COMM_AUTH);
    				job_obj.put(WS_ATTR_INTERNAL_DATA, data_obj);
    				
    				ret_obj.put(WS_ATTR_DATA, job_obj);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		}else{
    			Log.e(TAG,  "job_obj is null\n");
    		}
    	}
    	else{
    		Log.e( TAG,"ret_obj is null\n");
    	}
    	return ret_obj;
    }
}
