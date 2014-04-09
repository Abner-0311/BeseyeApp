package com.app.beseye.pairing;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.framing.FrameBuilder;
import org.java_websocket.framing.Framedata;
import org.java_websocket.handshake.ServerHandshake;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.app.beseye.BeseyeApplication;
import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraViewActivity;
import com.app.beseye.OpeningPage;
import com.app.beseye.R;
import com.app.beseye.WifiListActivity;
import com.app.beseye.audio.AudioChannelMgr;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

public class SoundPairingActivity extends BeseyeBaseActivity {
	private static EditText mEtTonePlay;
	private static Button mBtnPLayTone;
	
	static public final String KEY_WIFI_INFO = "KEY_WIFI_INFO";
	
	private WifiAPInfo mChosenWifiAPInfo;
	
	//For Soundpairing feature
	private native static boolean nativeClassInit();
	private native boolean playCode(String strCode, boolean bNeedEncode);
	private native int playPairingCode(String strMac, String strKey, int iSecType, short sUserToken);
	private native void finishPlayCode();
	private native void swTest();
	
    static {
    	//System.loadLibrary("websockets");
    	System.loadLibrary("soundpairing");
    	if (!nativeClassInit())
			throw new RuntimeException("Native Init Failed");
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().hide();
		
		mChosenWifiAPInfo = getIntent().getParcelableExtra(KEY_WIFI_INFO);
		if(null != mChosenWifiAPInfo){
			Log.w(TAG, "mChosenWifiAPInfo:"+mChosenWifiAPInfo.toString());
			int iRet = playPairingCode(mChosenWifiAPInfo.BSSID.replace(":", ""), mChosenWifiAPInfo.password,NetworkMgr.translateCipherToType(mChosenWifiAPInfo.cipher),(short) 1);
			if(iRet != 0)
				Toast.makeText(this, "ret:"+iRet, Toast.LENGTH_SHORT).show();
		}
		
//		mEtTonePlay = (EditText)findViewById(R.id.et_playtone_value);
//		mEtTonePlay.setText("raylios WiFi"+Character.toString((char) 0x1B)+Character.toString((char) 0x1B)+"whoisyourdaddy"+Character.toString((char) 0x1B)+Character.toString((char) 0x1B)+"3");
//		mBtnPLayTone = (Button)findViewById(R.id.btn_playtone);
//		if(null != mBtnPLayTone){
//			mBtnPLayTone.setOnClickListener(this);
//		}
		
//		new Thread(new Runnable(){
//
//			@Override
//			public void run() {
//				swTest();
//			}}).start();
	
	}
	
	@Override
	protected void onPause() {
		finishPlayCode();
		super.onPause();
		Log.w(TAG, "onPause()-");
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_playtone:{
				//if(playCode(mEtTonePlay.getText().toString(), true)){
				if(playPairingCode("aabbccddeeff", "BesEye0630",3,(short) 1) == 0){
					mBtnPLayTone.setEnabled(false);
				}
				break;
			}
			default:
				Log.w(TAG, "onClick(), not handle view id:"+view.getId());
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_paring;//R.layout.layout_sound_pairing;
	}

    // Audio control begin
    public static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
       return AudioChannelMgr.audioInit(sampleRate, is16Bit, isStereo, desiredFrames);
    }
    
    public static int getAudioBufSize(int iSampleRate){
    	return AudioChannelMgr.getAudioBufSize(iSampleRate);
    }
    
    public static void audioWriteShortBuffer(short[] buffer, int iLen) {
    	AudioChannelMgr.audioWriteShortBuffer(buffer, iLen);
    }
    
    public static void audioWriteByteBuffer(byte[] buffer) {
    	AudioChannelMgr.audioWriteByteBuffer(buffer);
    }

    public static void audioQuit() {
    	AudioChannelMgr.audioQuit();
    }
    // Audio control end
    
    public static void onStartGen(final String strCode){
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
		    	Log.i(TAG, "onStartGen(), strCode:["+strCode+"]");
		    	//mBtnPLayTone.setEnabled(false);
			}}, 0);
	}

    public static void onStopGen(final String strCode){
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				Log.i(TAG, "onStopGen(), strCode:["+strCode+"]");
		    	//mBtnPLayTone.setEnabled(true);
			}}, 0);
    	
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				Intent intent = new Intent();
				intent.setClass(BeseyeApplication.getApplication(), CameraViewActivity.class);//WifiListActivity.class);, CameraSettingActivity.class
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				BeseyeApplication.getApplication().startActivity(intent);
			}}, 2000);
	}

    public static void onCurFreqChanged(final double dFreq){
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				Log.i(TAG, "onCurFreqChanged(), dFreq:["+dFreq+"]");
			}}, 0);
    	
	}

    public static void onErrCorrectionCode(String strCode, String strEC, String strEncodeMark){
    	Log.i(TAG, "onErrCorrectionCode(), strCode:["+strCode+"]\n, strEC:["+strEC+"]\n, strEncodeMark:["+strEncodeMark+"]\n");
	}
}

