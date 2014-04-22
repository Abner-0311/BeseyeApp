package com.app.beseye.websockets;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.app.beseye.util.BeseyeJSONUtil;
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

public class AudioWebSocketsMgr extends WebsocketsMgr {
	static private final String AUDIO_WS_ADDR = "http://localhost:5566/audiowebsocket";
	static private AudioWebSocketsMgr sWebsocketsMgr = null;
	
	AsyncHttpServer httpServer = null;
	byte[] b =  new byte[1024];
	
	private AudioWebSocketsMgr(){
		if(null == httpServer){
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
	
	protected WebSocketConnectCallback getWebSocketConnectCallback(){
		return mWebSocketConnectCallback;
	}
	
	protected String getWSPath(){
		return AUDIO_WS_ADDR;
	}
	
	private WebSocketConnectCallback mWebSocketConnectCallback = new WebSocketConnectCallback() {
        @Override
        public void onCompleted(Exception ex, final WebSocket webSocket) {
        	Log.i(TAG, "AudioWebSocketsMgr::onCompleted()...");
        	OnWSChannelStateChangeListener listener = (null != mOnWSChannelStateChangeListener)?mOnWSChannelStateChangeListener.get():null;
			if(null != listener){
				listener.onChannelConnected();
			}
			
			transferAudioBuf();

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
					webSocket.send(bb.getAllByteArray());
				}});
            
            webSocket.setClosedCallback(new CompletedCallback(){

				@Override
				public void onCompleted(Exception ex) {
					Log.i(TAG, "onCompleted(), from close cb, ex="+ex.toString());	
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
					Log.i(TAG, "onCompleted(), from End cb, ex="+ex.toString());		
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
    
    private int audioSource = MediaRecorder.AudioSource.MIC;
    private static int sampleRateInHz = 16000;
    private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSizeInBytes = 0;

    private AudioRecord audioRecord;
    private AudioSendThread audioSendThread;
    
    private void transferAudioBuf(){
    	bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
		channelConfig, audioFormat);
		audioRecord = new AudioRecord(audioSource, sampleRateInHz,
		channelConfig, audioFormat, bufferSizeInBytes);
		audioRecord.startRecording();
		
		audioSendThread = new AudioSendThread();
		audioSendThread.start();
    }
    
    class AudioSendThread extends Thread {
    	@Override
    	public void run(){
		    OutputStream os = null;
		    Socket socket = null;
		    try{
			    socket = new Socket("192.168.2.4", 80);
			    os = socket.getOutputStream();
			    String header = "POST /cgi/audio/transmit.cgi HTTP/1.1\r\n"
			    + "Content-Type: audio/basic\r\n"
			    + "Cache-Control: no-cache\r\n"
			    + "User-Agent: Mozilla/4.0 (compatible; )\r\n"
			    + "Content-Length:30000000\r\n"
			    + "Connection: Keep-Alive\r\n"
			    + "Cookie: NetworkCamera_Volume=100\r\n"
			    + "Authorization: Basic YWRtaW46YWRtaW4=\r\n" + "\r\n\r\n";
			    os.write(header.getBytes());
			    os.flush();
			}catch (Exception e1) {
			    e1.printStackTrace();
			    return;
			}


		    byte[] audiodata = new byte[bufferSizeInBytes];
		    int readsize = 0;

		    while (AudioWebSocketsMgr.getInstance().isNotifyWSChannelAlive() == true){
		    	readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
		    	if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
		    		InputStream is = new ByteArrayInputStream(audiodata);
		    		UlawEncoderInputStream uis=null;
		    		try {
					    uis = new UlawEncoderInputStream(is,0);
					    byte buff[] = new byte[1024];
					    int len = uis.read(buff);
					    while (len > 0) {
						    os.write(buff, 0, len);
						    os.flush();
						    len = uis.read(buff);
					    }
		    		} catch (Exception e) {
		    		} finally{
		    			try {uis.close();} catch (Exception e) {}
				    }
		    	}
		    }
		
		    try {
		    	os.close();
		    	socket.close();
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
}