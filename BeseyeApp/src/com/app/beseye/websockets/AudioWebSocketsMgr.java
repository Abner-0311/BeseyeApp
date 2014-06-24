package com.app.beseye.websockets;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeNotificationBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.BeseyeHttpTask.OnHttpTaskCallback;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback;
import com.koushikdutta.async.http.WebSocket.StringCallback;
import com.koushikdutta.async.http.libcore.RequestHeaders;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServer.WebSocketRequestCallback;

public class AudioWebSocketsMgr extends WebsocketsMgr implements OnHttpTaskCallback {
	static final boolean ENABLE_INTERNAL_SERVER = true;
	static final boolean AUDIO_REC_FILE = false;
	
	String mSWID = "3eb1ef7b06673a2562aa";
	
	static private String AUDIO_WS_ADDR = "ws://192.168.2.4:1314";
	static private final String AUDIO_WS_ADDR_INTERNAL = "http://localhost:5566/audiowebsocket";//
	static private AudioWebSocketsMgr sWebsocketsMgr = null;
	
	AsyncHttpServer httpServer = null;
	byte[] b =  new byte[1024];
	
	private AudioWebSocketsMgr(){
		if(null == httpServer && ENABLE_INTERNAL_SERVER){
			Log.i(TAG, "Launch httpServer...");
			httpServer = new AsyncHttpServer();
	        httpServer.setErrorCallback(new CompletedCallback() {
	            @Override
	            public void onCompleted(Exception ex) {
	                //fail();
	            }
	        });
	        httpServer.listen(AsyncServer.getDefault(), 5566);
	        
	        httpServer.websocket("/audiowebsocket", new WebSocketRequestCallback() {
	            @Override
	            public void onConnected(final WebSocket webSocket, RequestHeaders headers) {
	            	Log.i(TAG, "httpServer::onConnected()...");
	            	//webSocket.send("[\"client_connected\", {\"data\":{\"connection_id\":\"e2f3e226bdaba71e0a45\"}]");
	            	
	            	webSocket.send("Test2".getBytes());
	                webSocket.setStringCallback(new StringCallback() {
	                    @Override
	                    public void onStringAvailable(String s) {
	                        webSocket.send(s);
	                    }
	                });
	                
	                webSocket.setDataCallback(new DataCallback(){
						@Override
						public void onDataAvailable(DataEmitter arg0, ByteBufferList arg1) {
							Log.i(TAG, "httpServer::onDataAvailable()...");
							webSocket.send(arg1.readString());
						}});
	            }
	        });
		}
	}
	
	static public AudioWebSocketsMgr getInstance(){
		if(null == sWebsocketsMgr){
			sWebsocketsMgr = new AudioWebSocketsMgr();
		}
		return sWebsocketsMgr;
	}
	
	private String mStrVCamId;
	
	public void setVCamId(String id){
		mStrVCamId = id;
	}
	
	public void setAudioWSServerIP(String ip){
		AUDIO_WS_ADDR = String.format("http://%s:80/websocket", ip);//"54.238.255.56");
	}
	
	protected WebSocketConnectCallback getWebSocketConnectCallback(){
		return mWebSocketConnectCallback;
	}
	
	protected String getWSPath(){
		return ENABLE_INTERNAL_SERVER?AUDIO_WS_ADDR_INTERNAL:AUDIO_WS_ADDR;
	}
	
	protected String getWSProtocol(){
		return null;//ENABLE_INTERNAL_SERVER?null:"beseye-audio-protocol";
	}
	
	private WebSocketConnectCallback mWebSocketConnectCallback = new WebSocketConnectCallback() {
        @Override
        public void onCompleted(Exception ex, final WebSocket webSocket) {
        	Log.i(TAG, "AudioWebSocketsMgr::onCompleted()...AUDIO_WS_ADDR:"+AUDIO_WS_ADDR);
        	if(null == webSocket){
        		Log.i(TAG, "AudioWebSocketsMgr::onCompleted(), connect to "+AUDIO_WS_ADDR+"failed, ex:"+ex.toString());
        		return;
        	}
        	
        	OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
			if(null != listener){
				listener.onChannelConnected();
			}
			if(ENABLE_INTERNAL_SERVER)
				new GetSessionTask(AudioWebSocketsMgr.this).execute();
			
			//webSocket.send("Test".getBytes());
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
										//
										if(FAKE_AUDIO_RECEIVER){
											String strWsId = mSWID;//BeseyeJSONUtil.getJSONString(dataObj, WSA_WS_ID);
											//Log.i(TAG, "onStringAvailable(), binConnObj="+dataObj.toString()+", strWsId:"+strWsId);
											JSONObject binConnObj = BeseyeWebsocketsUtil.genBinaryConnMsg(strWsId);
											if(null != binConnObj){
												webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_BIN_CONN, binConnObj.toString()));
												Log.i(TAG, "onStringAvailable(), binConnObj="+binConnObj.toString());
												mStrAudioConnJobId = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(binConnObj, WS_ATTR_DATA),WS_ATTR_JOB_ID); 
												Log.i(TAG, "onStringAvailable(), mStrAudioConnJobId="+mStrAudioConnJobId);
											}
										}else{
											new BeseyeNotificationBEHttpTask.RequestAudioWSOnCamTask(AudioWebSocketsMgr.this).execute(mStrVCamId);
										}
										
									}else if(null != mStrAudioConnJobId && mStrAudioConnJobId.equals(strJobID) && 0 == iRetCode){
										Log.i(TAG, "onStringAvailable(), Audio Conn OK -----------------------");
										transferAudioBuf();
									}
								}
							}else if(WS_CB_REMOTE_BIN_CONN.equals(strCmd)){
								JSONObject dataObj = BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.newJSONObject(strBody),WS_ATTR_DATA);
								if(null != dataObj){
									String strWsId = BeseyeJSONUtil.getJSONString(dataObj, WSA_WS_ID);
									//Log.i(TAG, "onStringAvailable(), binConnObj="+dataObj.toString()+", strWsId:"+strWsId);
									JSONObject binConnObj = BeseyeWebsocketsUtil.genBinaryConnMsg(strWsId);
									if(null != binConnObj){
										webSocket.send(String.format(WS_CMD_FORMAT, WS_FUNC_BIN_CONN, binConnObj.toString()));
										Log.i(TAG, "onStringAvailable(), binConnObj="+binConnObj.toString());
										mStrAudioConnJobId = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(binConnObj, WS_ATTR_DATA),WS_ATTR_JOB_ID); 
										Log.i(TAG, "onStringAvailable(), mStrAudioConnJobId="+mStrAudioConnJobId);
									}
								}
							}else if("wss_binary_transfer".equals(strCmd)){
								Log.w(TAG, "onStringAvailable(), wss_binary_transfer at="+System.currentTimeMillis());
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
					webSocket.send(bb.getAllByteArray());
				}});
            
            webSocket.setClosedCallback(new CompletedCallback(){

				@Override
				public void onCompleted(Exception ex) {
					Log.i(TAG, "onCompleted(), from close cb, ex="+((null != ex)?ex.toString():""));	
					synchronized(AudioWebSocketsMgr.this){
						mFNotifyWSChannel = null;
					}
					OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
					if(null != listener){
						listener.onChannelClosed();
					}
				}});
            
            webSocket.setEndCallback(new CompletedCallback(){

				@Override
				public void onCompleted(Exception ex) {
					Log.i(TAG, "onCompleted(), from End cb, ex="+((null != ex)?ex.toString():""));			
					synchronized(AudioWebSocketsMgr.this){
						mFNotifyWSChannel = null;
					}
					OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
					if(null != listener){
						listener.onChannelClosed();
					}
				}});
            
            webSocket.setWriteableCallback(new WritableCallback(){
				@Override
				public void onWriteable() {
					Log.i(TAG, "onWriteable()...");	
				}});
           
        }
    };
    
    static public class GetSessionTask extends BeseyeHttpTask {	 
		public GetSessionTask(OnHttpTaskCallback cb) {
			super(cb);
			setHttpMethod(HttpPost.METHOD_NAME);
		}
 
		@Override
		protected List<JSONObject> doInBackground(String... strParams) {
			JSONObject obj = new JSONObject();
			try {
				obj.put("username", "admin");
				obj.put("password", "password");
				return super.doInBackground("http://192.168.2.209/sray/login.cgi", obj.toString());
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
    
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private static int sampleRateInHz = 8000;
    private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSizeInBytes = 0;

    private AudioRecord audioRecord;
    private AudioSendThread audioSendThread = null;
    
    private void transferAudioBuf(){
    	bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
		channelConfig, audioFormat);
    	bufferSizeInBytes = 6400;
    	if(null == audioRecord){
			audioRecord = new AudioRecord(audioSource, sampleRateInHz,
			channelConfig, audioFormat, bufferSizeInBytes);
			audioRecord.startRecording();
			
			audioSendThread = new AudioSendThread();
			audioSendThread.start();
    	}
    }
    
    static public final String WS_CMD_FORMAT_AUDIO 			= "[\"%s\", \"data\":%s]";
    private int iRefCount = 0;
    private static final int COUNT_TO_CHECK = 3;//0.1 second 
    class AudioSendThread extends Thread {
    	@Override
    	public void run(){
    		iRefCount = 0;
		    OutputStream os = null;
		    Socket socket = null;
		    FileOutputStream fos = null;
		    
		    if(ENABLE_INTERNAL_SERVER){
		    	try{
				    socket = new Socket("192.168.2.209", 80);
				    os = socket.getOutputStream();
				    //String header = "POST /cgi/audio/transmit.cgi?session="+session+"&httptype=singlepart HTTP/1.1\r\nHost:192.168.2.4\r\n\r\n";
				    
				    String header = "POST /cgi/audio/transmit.cgi?session="+session+"&httptype=singlepart HTTP/1.0\r\n" 
				    + "Host:192.168.2.4\r\n"
				    + "Content-Type: audio/basic\r\n"
				    + "Cache-Control: no-cache\r\n"
				    //+ "User-Agent: Mozilla/4.0 (compatible; )\r\n"
				    + "Content-Length:9999999\r\n"
				    + "Connection: Keep-Alive\r\n"
				    //+ "Cookie: NetworkCamera_Volume=100\r\n"
				    //+ "Authorization: Basic YWRtaW46cGFzc3dvcmQ=\r\n" 
				    + "\r\n\r\n";
				    os.write(header.getBytes());
				    os.flush();
				}catch (Exception e1) {
				    e1.printStackTrace();
				    return;
				}
		    }
		    
		    if(AUDIO_REC_FILE){
			    File file = new File("/data/data/com.app.beseye/sample_8k.ulaw");
			    try{
			    	fos = new FileOutputStream(file);
			    }catch (FileNotFoundException e) {
			    	Log.i(TAG, "run(), FileNotFoundException");	
			    }
		    }

		    Log.i(TAG, "run(), socket connected");	
		    bufferSizeInBytes = 640;
		    byte[] audiodata = new byte[bufferSizeInBytes];
		    int readsize = 0;

		    while (AudioWebSocketsMgr.getInstance().isNotifyWSChannelAlive() == true){
		    	readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
		    	//Log.i(TAG, "run(), readsize="+readsize);
		    	if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
		    		if(0 == (iRefCount++)%COUNT_TO_CHECK){
	    				int iMaxVal = 0;
	    				for(int idx = 0; idx < readsize/2;idx++){
	    					//Log.i(TAG, "run(), audiodata[2*idx]:"+audiodata[2*idx]+", audiodata[2*idx+1]:"+ audiodata[2*idx+1]);
	    					int iVal = Math.abs(audiodata[2*idx+1]<<8 | audiodata[2*idx]);
	    					if(iMaxVal < iVal){
	    						iMaxVal = iVal;
	    					}
	    				}
	    				
	    				//Log.i(TAG, "run(), iMaxVal:"+iMaxVal);
	    				
	    				OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
		    			if(null != listener){
		    				listener.onAudioAmplitudeUpdate((iMaxVal)/32767.0f);
		    			}
	    			}
		    		
		    		InputStream is = new ByteArrayInputStream(audiodata);
		    		UlawEncoderInputStream uis=null;
		    		try {
					    uis = new UlawEncoderInputStream(is,0);
					    byte buff[] = new byte[320];
					    int len = uis.read(buff);
					    while (len > 0) {
					    	
					    	if(AUDIO_REC_FILE){
					    		fos.write(buff, 0, len);
					    		fos.flush();
					    	}else{
					    		if(ENABLE_INTERNAL_SERVER){
						    		os.write(buff, 0, len);
								    os.flush();
					    		}else{
					    			
					    			//mFNotifyWSChannel.get().send(buff);
					    			JSONObject data_obj = new JSONObject();
					    			JSONArray arr = new JSONArray();
					    			if(null != arr){
					    				for(int i = 0;i< len;i++)
					    					arr.put(buff[i]+128);
					    			}
					    			//String data = arr.toString().substring(1, arr.toString().length()-2);
				    				data_obj.put(WS_ATTR_DATA, arr);//new String(buff,0, len, "UTF-8"));
				    				String strSent = String.format(WS_CMD_FORMAT, WS_FUNC_BIN_TRANSFER, data_obj.toString());
					    			mFNotifyWSChannel.get().send(strSent);	
					    			//Log.i(TAG, "run(), len="+len+", strSent=\n"+strSent);
					    		}
					    	}
						    len = uis.read(buff);
					    }
		    		} catch (Exception e) {
		    			Log.i(TAG, "run(), Exception:"+e.toString());	
		    		} finally{
		    			try {uis.close();} catch (Exception e) {}
				    }
		    	}
		    }
		    
		    Log.i(TAG, "run(), socket disconnected");	
		
		    try {
		    	if(null != os)
		    		os.close();
		    	if(null != socket)
		    		socket.close();
		    	if(null != fos){
		    		fos.close();
		    	}
		    	if(null != audioRecord){
		    		audioRecord.stop();
			    	audioRecord = null;
		    	}
		    } catch (IOException e) {
		    }
		}
    }
    
    public final static class UlawEncoderInputStream extends InputStream {
        private final static String TAG = "UlawEncoderInputStream";
        
        private final static int MAX_ULAW = 8192;
        private final static int SCALE_BITS = 16;
        
        private InputStream mIn;
        
        private int mMax = 0;
        
        private final byte[] mBuf = new byte[1024];
        private int mBufCount = 0; // should be 0 or 1
        
        private final byte[] mOneByte = new byte[1];
        
        public static void encode(byte[] pcmBuf, int pcmOffset,
                byte[] ulawBuf, int ulawOffset, int length, int max) {
            
            // from  'ulaw' in wikipedia
            // +8191 to +8159                          0x80
            // +8158 to +4063 in 16 intervals of 256   0x80 + interval number
            // +4062 to +2015 in 16 intervals of 128   0x90 + interval number
            // +2014 to  +991 in 16 intervals of  64   0xA0 + interval number
            //  +990 to  +479 in 16 intervals of  32   0xB0 + interval number
            //  +478 to  +223 in 16 intervals of  16   0xC0 + interval number
            //  +222 to   +95 in 16 intervals of   8   0xD0 + interval number
            //   +94 to   +31 in 16 intervals of   4   0xE0 + interval number
            //   +30 to    +1 in 15 intervals of   2   0xF0 + interval number
            //     0                                   0xFF
            
            //    -1                                   0x7F
            //   -31 to    -2 in 15 intervals of   2   0x70 + interval number
            //   -95 to   -32 in 16 intervals of   4   0x60 + interval number
            //  -223 to   -96 in 16 intervals of   8   0x50 + interval number
            //  -479 to  -224 in 16 intervals of  16   0x40 + interval number
            //  -991 to  -480 in 16 intervals of  32   0x30 + interval number
            // -2015 to  -992 in 16 intervals of  64   0x20 + interval number
            // -4063 to -2016 in 16 intervals of 128   0x10 + interval number
            // -8159 to -4064 in 16 intervals of 256   0x00 + interval number
            // -8192 to -8160                          0x00
            
            // set scale factors
            if (max <= 0) max = MAX_ULAW;
            
            int coef = MAX_ULAW * (1 << SCALE_BITS) / max;
            
            for (int i = 0; i < length; i++) {
                int pcm = (0xff & pcmBuf[pcmOffset++]) + (pcmBuf[pcmOffset++] << 8);
                pcm = (pcm * coef) >> SCALE_BITS;
                
                int ulaw;
                if (pcm >= 0) {
                    ulaw = pcm <= 0 ? 0xff :
                            pcm <=   30 ? 0xf0 + ((  30 - pcm) >> 1) :
                            pcm <=   94 ? 0xe0 + ((  94 - pcm) >> 2) :
                            pcm <=  222 ? 0xd0 + (( 222 - pcm) >> 3) :
                            pcm <=  478 ? 0xc0 + (( 478 - pcm) >> 4) :
                            pcm <=  990 ? 0xb0 + (( 990 - pcm) >> 5) :
                            pcm <= 2014 ? 0xa0 + ((2014 - pcm) >> 6) :
                            pcm <= 4062 ? 0x90 + ((4062 - pcm) >> 7) :
                            pcm <= 8158 ? 0x80 + ((8158 - pcm) >> 8) :
                            0x80;
                } else {
                    ulaw = -1 <= pcm ? 0x7f :
                              -31 <= pcm ? 0x70 + ((pcm -   -31) >> 1) :
                              -95 <= pcm ? 0x60 + ((pcm -   -95) >> 2) :
                             -223 <= pcm ? 0x50 + ((pcm -  -223) >> 3) :
                             -479 <= pcm ? 0x40 + ((pcm -  -479) >> 4) :
                             -991 <= pcm ? 0x30 + ((pcm -  -991) >> 5) :
                            -2015 <= pcm ? 0x20 + ((pcm - -2015) >> 6) :
                            -4063 <= pcm ? 0x10 + ((pcm - -4063) >> 7) :
                            -8159 <= pcm ? 0x00 + ((pcm - -8159) >> 8) :
                            0x00;
                }
                ulawBuf[ulawOffset++] = (byte)ulaw;
            }
        }
        
        /**
         * Compute the maximum of the absolute value of the pcm samples.
         * The return value can be used to set ulaw encoder scaling.
         * @param pcmBuf array containing 16 bit pcm data.
         * @param offset offset of start of 16 bit pcm data.
         * @param length number of pcm samples (not number of input bytes)
         * @return maximum abs of pcm data values
         */
        public static int maxAbsPcm(byte[] pcmBuf, int offset, int length) {
            int max = 0;
            for (int i = 0; i < length; i++) {
                int pcm = (0xff & pcmBuf[offset++]) + (pcmBuf[offset++] << 8);
                if (pcm < 0) pcm = -pcm;
                if (pcm > max) max = pcm;
            }
            return max;
        }


        /**
         * Create an InputStream which takes 16 bit pcm data and produces ulaw data.
         * @param in InputStream containing 16 bit pcm data.
         * @param max pcm value corresponding to maximum ulaw value.
         */
        public UlawEncoderInputStream(InputStream in, int max) {
            mIn = in;
            mMax = max;
        }
        
        @Override
        public int read(byte[] buf, int offset, int length) throws IOException {
            if (mIn == null) throw new IllegalStateException("not open");


            // return at least one byte, but try to fill 'length'
            while (mBufCount < 2) {
                int n = mIn.read(mBuf, mBufCount, Math.min(length * 2, mBuf.length - mBufCount));
                if (n == -1) return -1;
                mBufCount += n;
            }
            
            // compand data
            int n = Math.min(mBufCount / 2, length);
            encode(mBuf, 0, buf, offset, n, mMax);
            
            // move data to bottom of mBuf
            mBufCount -= n * 2;
            for (int i = 0; i < mBufCount; i++) mBuf[i] = mBuf[i + n * 2];
            
            return n;
        }
        
        @Override
        public int read(byte[] buf) throws IOException {
            return read(buf, 0, buf.length);
        }
        
        @Override
        public int read() throws IOException {
            int n = read(mOneByte, 0, 1);
            if (n == -1) return -1;
            return 0xff & (int)mOneByte[0];
        }
        
        @Override
        public void close() throws IOException {
            if (mIn != null) {
                InputStream in = mIn;
                mIn = null;
                in.close();
            }
        }
        
        @Override
        public int available() throws IOException {
            return (mIn.available() + mBufCount) / 2;
        }
    }

	@Override
	public void onShowDialog(AsyncTask task, int iDialogId, int iTitleRes,
			int iMsgRes) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDismissDialog(AsyncTask task, int iDialogId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		// TODO Auto-generated method stub
		
	}

	private String session;
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(task instanceof GetSessionTask){
			//Log.i(TAG, "onPostExecute(), result = "+result.toString());
			//if(0 == iRetCode){
				JSONObject obj = result.get(0);
				Log.i(TAG, "onPostExecute(), obj = "+obj.toString());
				session = BeseyeJSONUtil.getJSONString(obj, "session");
				transferAudioBuf();
			//}
		}else if(task instanceof BeseyeNotificationBEHttpTask.RequestAudioWSOnCamTask){
			if(0 == iRetCode){
				JSONObject obj = result.get(0);
				Log.i(TAG, "onPostExecute(), obj = "+obj.toString());
				
			}
		}
	}

	@Override
	public void onToastShow(AsyncTask task, String strMsg) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason) {
		// TODO Auto-generated method stub
		
	}
}