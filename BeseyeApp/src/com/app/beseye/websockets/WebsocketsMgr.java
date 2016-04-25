package com.app.beseye.websockets;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
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

import com.app.beseye.error.BeseyeError;
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
	protected boolean mbIsLastErrServerUnavailable = false;
	protected int miErrServerUnavailableCnt = 0;
	protected boolean mbAuthComplete = false;
	protected boolean mBConstructingNotifyWSChannel = false;
	protected boolean mbNotifyWSChannelConstructed = false;
	protected long mlTimeConstrucNotifyWSChannel = 0;
	protected long mlTimeToRequestWSChannelAuth = 0;
	protected WeakReference<OnWSChannelStateChangeListener> mOnWSChannelStateChangeListener = null;
	protected long mlLastTimeToGetKeepAlive = -1;
	
	protected WebsocketsMgr(){
		
	}
	
	static public interface OnWSChannelStateChangeListener{
		public void onChannelConnecting();
		public void onChannelConnected();
		public void onAuthfailed();
		public void onAuthComplete();
		public void onMessageReceived(String msg);
		public void onChannelClosed();
		public void onUserSessionInvalid();
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
		if(DEBUG)
			Log.i(TAG, "setWSServerIP(), ip="+ip);
		NOTIFY_WS_ADDR = String.format("%s/websocket", ip);//"54.238.255.56");
	}
	
	public boolean isLastErrServerUnavailable(){
		return mbIsLastErrServerUnavailable;
	}
	
	public int getErrServerUnavailableCnt(){
		return miErrServerUnavailableCnt;
	}
	
	public boolean constructWSChannel(){
		if(DEBUG)
			Log.i(TAG, "constructWSChannel(), ++"+" on "+this.getClass().getName());
		boolean bRet = false;
		try {
			//printNotifyWSChannelState();
			if(isWSChannelAlive() || null == getWSPath()){
				bRet = false;
			}else{
				synchronized(this){
					mBConstructingNotifyWSChannel = true; 
					mbNotifyWSChannelConstructed = false;
					mlTimeConstrucNotifyWSChannel = System.currentTimeMillis();
					mlTimeToRequestWSChannelAuth = 0;
				}
				
				OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
				if(null != listener){
					listener.onChannelConnecting();
				}
				
				if(DEBUG)
					Log.i(TAG, "constructWSChannel()-----"+" on "+this.getClass().getName());
				mFNotifyWSChannel = AsyncHttpClient.getDefaultInstance().websocket(getWSPath(), getWSProtocol(), getWebSocketConnectCallback());
				if(DEBUG)
					Log.i(TAG, "constructWSChannel(), path =>"+getWSPath());
				WebSocket ws = mFNotifyWSChannel.get();
				Log.i(TAG, "constructWSChannel(), ws is null =>"+(null == ws)+" on "+this.getClass().getName());
				if(null == ws){
					mFNotifyWSChannel = null;
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Log.i(TAG, "InterruptedException(), e="+e.toString()+" on "+this.getClass().getName());
			notifyChannelClosed();
		} catch (ExecutionException e) {
			e.printStackTrace();
			Log.i(TAG, "ExecutionException(), e="+e.toString()+" on "+this.getClass().getName());
			notifyChannelClosed();
		} finally{
			synchronized(this){
				mbNotifyWSChannelConstructed = false;
				mBConstructingNotifyWSChannel = false;
				mlTimeConstrucNotifyWSChannel = 0;
				mlTimeToRequestWSChannelAuth =0;
			}
		}
		return bRet;
	}
	
	protected void notifyChannelClosed(){
		synchronized(WebsocketsMgr.this){
			mFNotifyWSChannel = null;
			mbAuthComplete =false;
			mlLastTimeToGetKeepAlive = -1;
		}
		
		OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
		if(null != listener){
			listener.onChannelClosed();
		}
	}
	
	public boolean isWSChannelAlive(){
		//Log.i(TAG, "isNotifyWSChannelAlive(), ++");
		boolean bRet = false;
		//printNotifyWSChannelState();
		synchronized(this){
			if(mBConstructingNotifyWSChannel){
				if(0 < mlTimeConstrucNotifyWSChannel && System.currentTimeMillis() - mlTimeConstrucNotifyWSChannel > 15*1000){
					Log.i(TAG, "isWSChannelAlive(), too long to construct ws!!!");
					bRet = false;
				}else{
					bRet = true;
				}
			}else if(null != mFNotifyWSChannel && false == mFNotifyWSChannel.isCancelled() && true == mFNotifyWSChannel.isDone()){
				if(mbAuthComplete || (0 < mlTimeToRequestWSChannelAuth && System.currentTimeMillis() - mlTimeToRequestWSChannelAuth < 20*1000)){
					bRet = true;
				}
			}
		}
	
		//Log.i(TAG, "isWSChannelAlive()--, bRet="+bRet);
		return bRet;
	}
	
	public boolean destroyWSChannel(){
		//Thread.dumpStack();
		if(DEBUG)
			Log.i(TAG, "destroyWSChannel(), ++"+" on "+this.getClass().getName());
		boolean bRet = false;
		printNotifyWSChannelState();
		
		synchronized(this){
			try {
				if(DEBUG)
					Log.i(TAG, "destroyWSChannel(), check"+" on "+this.getClass().getName());
				WebSocket ws = null;
				if(null != mFNotifyWSChannel){
					if(mBConstructingNotifyWSChannel){
						if(false == mFNotifyWSChannel.isCancelled()){
							 mFNotifyWSChannel.cancel(true);
						}
					}else if(true == mFNotifyWSChannel.isDone() && null != (ws = (WebSocket) mFNotifyWSChannel.get())){
						ws.close();
						bRet = true;
						Log.i(TAG, "destroyWSChannel(), call close"+" on "+this.getClass().getName());
						
						mFNotifyWSChannel = null;//workaround 0702
					}	
				}
				
				mbAuthComplete = false;
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
	
	public boolean checkLastTimeToGetKeepAlive(){
		return -1 != mlLastTimeToGetKeepAlive && (System.currentTimeMillis() - mlLastTimeToGetKeepAlive) > 15*1000;
	}
	
	protected Runnable mCheckConnectionRunnable = new Runnable(){
		@Override
		public void run() {
			if(-1 != mlLastTimeToGetKeepAlive){
				if(DEBUG)
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
        	if(DEBUG)
        		Log.i(TAG, "onCompleted()..."+" on "+WebsocketsMgr.this.getClass().getName());
        	
        	if(null == webSocket){
        		Log.e(TAG, "onCompleted(), webSocket is null..."+" on "+WebsocketsMgr.this.getClass().getName());
        		return;
			}
        	
        	synchronized(this){
        		mbNotifyWSChannelConstructed = true;
				mBConstructingNotifyWSChannel = false;
				mlTimeConstrucNotifyWSChannel = 0;
				mlTimeToRequestWSChannelAuth = System.currentTimeMillis();
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
								if(DEBUG)
									Log.i(TAG, "onStringAvailable(), strCmd=["+strCmd+"], strBody"+strBody);
								webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_CONNECTED, wrapWSBaseMsg().toString()));
								JSONObject authObj = BeseyeWebsocketsUtil.genAuthMsg();
								if(null != authObj){
									webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_AUTH, authObj.toString()));
									if(DEBUG)
										Log.i(TAG, "onStringAvailable(), authObj="+authObj.toString());
									mStrAuthJobId = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(authObj, WS_ATTR_DATA),WS_ATTR_JOB_ID); 
									if(DEBUG)
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
									if(null != mStrAuthJobId && mStrAuthJobId.equals(strJobID)){
										mbIsLastErrServerUnavailable = false;
										if(BeseyeError.isNoError(iRetCode)){
											Log.i(TAG, "onStringAvailable(), Auth OK -----------------------");
											mStrAuthJobId = null;
											mbAuthComplete = true;
											mlTimeToRequestWSChannelAuth = 0;
											miErrServerUnavailableCnt = 0;
										}else if(BeseyeError.isUserSessionInvalidError(iRetCode)){
											Log.i(TAG, "onStringAvailable(), Token invalid -----------------------"+iRetCode);
											OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
											if(null != listener){
												listener.onUserSessionInvalid();
											}
											destroyWSChannel();
										}else if(BeseyeError.isWSServerUnavailableError(iRetCode)){
											Log.i(TAG, "onStringAvailable(), Server temp unavailable -----------------------"+iRetCode);
											mbIsLastErrServerUnavailable = true;
											miErrServerUnavailableCnt++;
											destroyWSChannel();
										}else{
											Log.i(TAG, "onStringAvailable(), other error -----------------------"+iRetCode);
											destroyWSChannel();
										}
									}
								}
							}else if(WS_CB_EVT.equals(strCmd)){
								if(DEBUG)
									Log.i(TAG, "onStringAvailable(), strCmd=["+strCmd+"], strBody"+strBody);
								JSONObject dataObjOutside = BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.newJSONObject(strBody),WS_ATTR_DATA);
								String strIdx = BeseyeJSONUtil.getJSONString(dataObjOutside, BeseyeWebsocketsUtil.WS_ATTR_IDX);
								if(null != strIdx){
									JSONObject objRet = new JSONObject();
									JSONObject objRetData = new JSONObject();
									objRetData.put(BeseyeWebsocketsUtil.WS_ATTR_IDX, strIdx);
									objRetData.put(BeseyeWebsocketsUtil.WS_ATTR_CODE, 0);
									objRet.put(WS_ATTR_DATA, objRetData);
									webSocket.send(String.format(WS_CMD_FORMAT, BeseyeWebsocketsUtil.WS_FUNC_ACK, objRet.toString()));
									Log.i(TAG, "onStringAvailable(), objRet=["+objRet.toString()+"]");
								}
								
								JSONObject dataObj = BeseyeJSONUtil.getJSONObject(dataObjOutside, WS_ATTR_DATA);
								OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
								if(null != listener){
									listener.onMessageReceived(dataObj.toString());
								}

							}else{
								if(DEBUG)
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
					if(DEBUG)
						Log.i(TAG, "onDataAvailable()...");						
				}});
            
            webSocket.setClosedCallback(new CompletedCallback(){

				@Override
				public void onCompleted(Exception ex) {
					Log.i(TAG, "onCompleted(), from close cb");	
					notifyChannelClosed();
				}});
            
            webSocket.setEndCallback(new CompletedCallback(){

				@Override
				public void onCompleted(Exception ex) {
					Log.i(TAG, "onCompleted(), from End cb");	
					synchronized(WebsocketsMgr.this){
						mFNotifyWSChannel = null;
					}
					notifyChannelClosed();
				}});
            
            webSocket.setWriteableCallback(new WritableCallback(){
				@Override
				public void onWriteable() {
					if(DEBUG)
						Log.i(TAG, "onWriteable()...");	
				}});
           
        }
    };
}
