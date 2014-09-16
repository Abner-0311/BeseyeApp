package com.app.beseye.websockets;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CODE;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_DATA;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_JOB_ID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_CB_ACK;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_CB_CLIENT_CONNECTION;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_CB_EVT;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_CB_KEEP_ALIVE;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_CMD_FORMAT;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_FUNC_AUTH;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_FUNC_CONNECTED;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_FUNC_KEEP_ALIVE;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_FUNC_RAILS_PING;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_FUNC_RAILS_PONG;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.wrapWSBaseMsg;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutionException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.WebSocket.StringCallback;

public class WebsocketsMgr {
	static private String NOTIFY_WS_ADDR = null;/*"ws://192.168.2.4:1314";*///"http://54.238.255.56:80/websocket";
	static private WebsocketsMgr sWebsocketsMgr = null;
	
	static public WebsocketsMgr getInstance(){
		if(null == sWebsocketsMgr){
			sWebsocketsMgr = new WebsocketsMgr();
		}
		return sWebsocketsMgr;
	}
	
	protected Future<WebSocket> mFNotifyWSChannel = null;
	protected String mStrAuthJobId = null;
	protected String mStrAudioConnJobId = null;
	protected boolean mBAuth = false;
	protected boolean mBConstructingNotifyWSChannel = false;
	protected WeakReference<OnWSChannelStateChangeListener> mOnWSChannelStateChangeListener = null;
	protected long mlLastTimeToGetKeepAlive = -1;
	
	protected WebsocketsMgr(){
		
	}
	
	static public interface OnWSChannelStateChangeListener{
		public void onChannelConnecting();
		public void onAuthfailed();
		public void onChannelConnected();
		public void onMessageReceived(String msg);
		public void onChannelClosed();
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
			Log.i(TAG, "printNotifyWSChannelState(), mFNotifyWSChannel:["+mFNotifyWSChannel.isCancelled() +", "+mFNotifyWSChannel.isDone()+"], mBConstructingNotifyWSChannel="+mBConstructingNotifyWSChannel);
		}
	}
	
	public String getWSServerIP(){
		return NOTIFY_WS_ADDR;
	}
	
	public void setWSServerIP(String ip){
		Log.i(TAG, "setWSServerIP(), ip="+ip);
		NOTIFY_WS_ADDR = String.format("%s/websocket", ip);//"54.238.255.56");
	}
	
	public boolean constructWSChannel(){
		Log.i(TAG, "constructWSChannel(), ++");
		boolean bRet = false;
		try {
			//printNotifyWSChannelState();
			if(isWSChannelAlive() || null == getWSPath()){
				bRet = false;
			}else{
				synchronized(this){
					mBConstructingNotifyWSChannel = true;
				}
				OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
				if(null != listener){
					listener.onChannelConnecting();
				}
				
				//Log.i(TAG, "constructWSChannel()");
				mFNotifyWSChannel = AsyncHttpClient.getDefaultInstance().websocket(getWSPath(), getWSProtocol(), getWebSocketConnectCallback());
				Log.i(TAG, "constructWSChannel(), path =>"+getWSPath());
				WebSocket ws = mFNotifyWSChannel.get();
				Log.i(TAG, "constructWSChannel(), ws is null =>"+(null == ws));
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
	
	public boolean isWSChannelAlive(){
		//Log.i(TAG, "isNotifyWSChannelAlive(), ++");
		boolean bRet = false;
		//printNotifyWSChannelState();
		synchronized(this){
			if(mBConstructingNotifyWSChannel){
				bRet = true;
			}else if(null != mFNotifyWSChannel && false == mFNotifyWSChannel.isCancelled() && true == mFNotifyWSChannel.isDone()){
				bRet = true;
			}
		}
	
		//Log.i(TAG, "isWSChannelAlive()--, bRet="+bRet);
		return bRet;
	}
	
	public boolean destroyWSChannel(){
		Log.i(TAG, "destroyWSChannel(), ++");
		boolean bRet = false;
		printNotifyWSChannelState();
		
		synchronized(this){
			try {
				Log.i(TAG, "destroyWSChannel(), check");
				WebSocket ws = null;
				if(null != mFNotifyWSChannel){
					if(mBConstructingNotifyWSChannel){
						if(false == mFNotifyWSChannel.isCancelled()){
							 mFNotifyWSChannel.cancel(true);
						}
					}else if(true == mFNotifyWSChannel.isDone() && null != (ws = (WebSocket) mFNotifyWSChannel.get())){
						ws.close();
						bRet = true;
						Log.i(TAG, "destroyWSChannel(), call close");
						
						mFNotifyWSChannel = null;//workaround 0702
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
	
	protected WebSocketConnectCallback getWebSocketConnectCallback(){
		return mWebSocketConnectCallback;
	}
	
	protected String getWSPath(){
		return NOTIFY_WS_ADDR;
	}
	
	protected String getWSProtocol(){
		return null;
	}
	
	private void updateLastTimeToGetKeepAlive(){
		mlLastTimeToGetKeepAlive = System.currentTimeMillis();
		BeseyeUtils.removeRunnable(mCheckConnectionRunnable);
		BeseyeUtils.postRunnable(mCheckConnectionRunnable, 15*1000);
	}
	
	protected Runnable mCheckConnectionRunnable = new Runnable(){
		@Override
		public void run() {
			if(-1 != mlLastTimeToGetKeepAlive){
				Log.i(TAG, "Need to reconnect webSocket !!!!!!!!!!!!!!!!!!!!!");
				if(WebsocketsMgr.getInstance().isWSChannelAlive()){
					WebsocketsMgr.getInstance().destroyWSChannel();
					mlLastTimeToGetKeepAlive = -1;
				}
			}
		}};
	
	private WebSocketConnectCallback mWebSocketConnectCallback = new WebSocketConnectCallback() {
        @Override
        public void onCompleted(Exception ex, final WebSocket webSocket) {
        	Log.i(TAG, "onCompleted()...");
        	
        	if(null == webSocket){
        		Log.e(TAG, "onCompleted(), webSocket is null...");
        		return;
			}

//        	webSocket.send("Welcone");
//        	webSocket.send("Welcone".getBytes());
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
							if(WS_CB_CLIENT_CONNECTION.equals(strCmd)){
								Log.i(TAG, "onStringAvailable(), strCmd=["+strCmd+"], strBody"+strBody);
								webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_CONNECTED, wrapWSBaseMsg().toString()));
								JSONObject authObj = BeseyeWebsocketsUtil.genAuthMsg();
								if(null != authObj){
									webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_AUTH, authObj.toString()));
									Log.i(TAG, "onStringAvailable(), authObj="+authObj.toString());
									mStrAuthJobId = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(authObj, WS_ATTR_DATA),WS_ATTR_JOB_ID); 
									Log.i(TAG, "onStringAvailable(), strAuthJobId="+mStrAuthJobId);
								}
								webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_KEEP_ALIVE, wrapWSBaseMsg().toString()));
								updateLastTimeToGetKeepAlive();
							}else if(WS_FUNC_RAILS_PING.equals(strCmd)){
								webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_RAILS_PONG, wrapWSBaseMsg().toString()));
								//mlLastTimeToGetKeepAlive = System.currentTimeMillis();
							}else if(WS_CB_KEEP_ALIVE.equals(strCmd)){
								webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_KEEP_ALIVE, wrapWSBaseMsg().toString()));
								updateLastTimeToGetKeepAlive();
							}else if(WS_CB_ACK.equals(strCmd)){
								//Log.i(TAG, "onStringAvailable(), strCmd=["+strCmd+"], strBody"+strBody);
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
							}else if(WS_CB_EVT.equals(strCmd)){
								Log.i(TAG, "onStringAvailable(), strCmd=["+strCmd+"], strBody"+strBody);
								JSONObject dataObj = BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.newJSONObject(strBody),WS_ATTR_DATA),WS_ATTR_DATA);
								OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
								if(null != listener){
									listener.onMessageReceived(dataObj.toString());
								}
							}else{
								Log.w(TAG, "onStringAvailable(), not handle cmd=["+strCmd+"], strBody"+strBody);
//								OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
//								if(null != listener){
//									listener.onMessageReceived(s);
//								}
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
						listener.onChannelClosed();
					}
					mlLastTimeToGetKeepAlive = -1;
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
						listener.onChannelClosed();
					}
					mlLastTimeToGetKeepAlive = -1;
				}});
            
            webSocket.setWriteableCallback(new WritableCallback(){
				@Override
				public void onWriteable() {
					Log.i(TAG, "onWriteable()...");	
				}});
           
        }
    };
}
