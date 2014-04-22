package com.app.beseye.websockets;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.app.beseye.util.BeseyeJSONUtil;
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

public class WebsocketsMgr {
	static private final String WS_NOTIFY_ADDR = "http://54.238.255.56:80/websocket";
	static private WebsocketsMgr sWebsocketsMgr = null;
	
	static public WebsocketsMgr getInstance(){
		if(null == sWebsocketsMgr){
			sWebsocketsMgr = new WebsocketsMgr();
		}
		return sWebsocketsMgr;
	}
	
	private Future<WebSocket> mFNotifyWSChannel = null;
	private String mStrAuthJobId = null;
	private boolean mBAuth = false;
	private boolean mBConstructingNotifyWSChannel = false;
	private WeakReference<OnWSChannelStateChangeListener> mOnWSChannelStateChangeListener = null;
	
	private WebsocketsMgr(){
		
	}
	
	static public interface OnWSChannelStateChangeListener{
		public void onChannelConnecting();
		public void onChannelConnected();
		public void onMessageReceived(String msg);
		public void onChannelCloased();
	}
	
	public void registerOnWSChannelStateChangeListener(OnWSChannelStateChangeListener listener){
		if(null != listener){
			mOnWSChannelStateChangeListener = new WeakReference<OnWSChannelStateChangeListener>(listener);
		}
	}
	
	public void unregisterOnWSChannelStateChangeListener(){
		mOnWSChannelStateChangeListener = null;
	}
	
	private synchronized void printNotifyWSChannelState(){
		if(null == mFNotifyWSChannel){
			Log.i(TAG, "printNotifyWSChannelState(), mFNotifyWSChannel is null");
		}else{
			Log.i(TAG, "printNotifyWSChannelState(), mFNotifyWSChannel:[ "+mFNotifyWSChannel.isCancelled() +", "+mFNotifyWSChannel.isDone()+"], mBConstructingNotifyWSChannel="+mBConstructingNotifyWSChannel);
		}
	}
	
	public boolean constructNotifyWSChannel(){
		Log.i(TAG, "constructNotifyWSChannel(), ++");
		boolean bRet = false;
		try {
			//printNotifyWSChannelState();
			if(isNotifyWSChannelAlive()){
				bRet = false;
			}else{
				synchronized(this){
					mBConstructingNotifyWSChannel = true;
				}
				OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
				if(null != listener){
					listener.onChannelConnecting();
				}
				
				mFNotifyWSChannel = AsyncHttpClient.getDefaultInstance().websocket(WS_NOTIFY_ADDR, null, new WebSocketConnectCallback() {
		            @Override
		            public void onCompleted(Exception ex, final WebSocket webSocket) {
		            	Log.i(TAG, "onCompleted()...");
		            	OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
						if(null != listener){
							listener.onChannelConnected();
						}

		                webSocket.setStringCallback(new StringCallback() {
		                    @Override
		                    public void onStringAvailable(String s) {
		                    	
		                    	JSONArray arrPkt = BeseyeJSONUtil.newgetJSONArray(s);
		                    	if(null != arrPkt){
		                    		try {
		                    			JSONArray arrNew = arrPkt.getJSONArray(0);
		                    			String strCmd = arrNew.getString(0);
		                    			String strBody = arrNew.getString(1);
										Log.i(TAG, "onStringAvailable(), strCmd=["+strCmd+"], strBody"+strBody);
										if(WS_CB_CLIENT_CONNECTION.equals(strCmd)){
											webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_CONNECTED, wrapWSBaseMsg().toString()));
											JSONObject authObj = BeseyeWebsocketsUtil.genAuthMsg();
											if(null != authObj){
												webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_AUTH, authObj.toString()));
												//Log.i(TAG, "onStringAvailable(), authObj="+authObj.toString());
												mStrAuthJobId = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(authObj, WS_ATTR_DATA),WS_ATTR_JOB_ID); 
												Log.i(TAG, "onStringAvailable(), strAuthJobId="+mStrAuthJobId);
											}
											webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_KEEP_ALIVE, wrapWSBaseMsg().toString()));
										}else if(WS_CB_KEEP_ALIVE.equals(strCmd)){
											webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_KEEP_ALIVE, wrapWSBaseMsg().toString()));
										}else if(WS_CB_ACK.equals(strCmd)){
											JSONObject dataObj = BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.newJSONObject(strBody),WS_ATTR_DATA);
											if(null != dataObj){
												String strJobID = BeseyeJSONUtil.getJSONString(dataObj, WS_ATTR_JOB_ID);
												int iRetCode = BeseyeJSONUtil.getJSONInt(dataObj, WS_ATTR_CODE, -1);
												if(null != mStrAuthJobId && mStrAuthJobId.equals(strJobID) && 0 == iRetCode){
													Log.i(TAG, "onStringAvailable(), Auth OK -----------------------");
													mStrAuthJobId = null;
													mBAuth = true;
												}
											}
										}else{
											Log.w(TAG, "onStringAvailable(), not handle cmd="+strCmd);
											OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
											if(null != listener){
												listener.onMessageReceived(s);
											}
										}
										
									} catch (JSONException e) {
										e.printStackTrace();
										Log.i(TAG, "JSONException(), e="+e.toString());
									}
		                    	}
		                    }
		                });
		                
		                webSocket.setDataCallback( new DataCallback(){

							@Override
							public void onDataAvailable(DataEmitter emitter,
									ByteBufferList bb) {
								Log.i(TAG, "onDataAvailable()...");						
							}});
		                
		                webSocket.setClosedCallback(new CompletedCallback(){

							@Override
							public void onCompleted(Exception ex) {
								Log.i(TAG, "onCompleted(), from close cb");	
								synchronized(WebsocketsMgr.this){
									mFNotifyWSChannel = null;
								}
								OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
								if(null != listener){
									listener.onChannelCloased();
								}
							}});
		                
		                webSocket.setEndCallback(new CompletedCallback(){

							@Override
							public void onCompleted(Exception ex) {
								Log.i(TAG, "onCompleted(), from End cb");	
								synchronized(WebsocketsMgr.this){
									mFNotifyWSChannel = null;
								}
								OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
								if(null != listener){
									listener.onChannelCloased();
								}
							}});
		                
		                webSocket.setWriteableCallback(new WritableCallback(){
							@Override
							public void onWriteable() {
								Log.i(TAG, "onWriteable()...");	
							}});
		               
		            }
		        });
				
				
				
				WebSocket ws = mFNotifyWSChannel.get();
				Log.i(TAG, "constructNotifyWSChannel(), ws is null =>"+(null == ws));
				if(null == ws){
					mFNotifyWSChannel = null;
				}
			}
			
			synchronized(this){
				mBConstructingNotifyWSChannel = false;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Log.i(TAG, "InterruptedException(), e="+e.toString());
		} catch (ExecutionException e) {
			e.printStackTrace();
			Log.i(TAG, "ExecutionException(), e="+e.toString());
		}
		return bRet;
	}
	
	public boolean isNotifyWSChannelAlive(){
		Log.i(TAG, "isNotifyWSChannelAlive(), ++");
		boolean bRet = false;
		printNotifyWSChannelState();
		synchronized(this){
			if(mBConstructingNotifyWSChannel){
				bRet = true;
			}else if(null != mFNotifyWSChannel && false == mFNotifyWSChannel.isCancelled() && true == mFNotifyWSChannel.isDone()){
				bRet = true;
			}
		}
	
		Log.i(TAG, "isNotifyWSChannelAlive()--, bRet="+bRet);
		return bRet;
	}
	
	public boolean destroyNotifyWSChannel(){
		Log.i(TAG, "destroyNotifyWSChannel(), ++");
		boolean bRet = false;
		printNotifyWSChannelState();
		
		synchronized(this){
			try {
				WebSocket ws = null;
				if(null != mFNotifyWSChannel){
					if(mBConstructingNotifyWSChannel){
						if(false == mFNotifyWSChannel.isCancelled()){
							 mFNotifyWSChannel.cancel(true);
						}
					}else if(true == mFNotifyWSChannel.isDone() && null != (ws = (WebSocket) mFNotifyWSChannel.get())){
						ws.close();
						bRet = true;
						Log.i(TAG, "destroyNotifyWSChannel(), call close");
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				Log.i(TAG, "InterruptedException(), e="+e.toString());
			} catch (ExecutionException e) {
				e.printStackTrace();
				Log.i(TAG, "ExecutionException(), e="+e.toString());
			}
		}
		return bRet;
	}
}
