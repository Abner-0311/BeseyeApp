package com.app.beseye.pairing;

import static com.app.beseye.util.BeseyeConfig.TAG;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.R;
import com.app.beseye.audio.AudioChannelMgr;
import com.app.beseye.util.BeseyeUtils;

public class SoundPairingActivity extends BeseyeBaseActivity {
	private static EditText mEtTonePlay;
	private static Button mBtnPLayTone;
	
	//For Soundpairing feature
	private native static boolean nativeClassInit();
	private native boolean playCode(String strCode, boolean bNeedEncode);
	private native void finishPlayCode();
	
    static {
    	System.loadLibrary("soundpairing");
    	if (!nativeClassInit())
			throw new RuntimeException("Native Init Failed");
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mEtTonePlay = (EditText)findViewById(R.id.et_playtone_value);
		mEtTonePlay.setText("raylios WiFi"+Character.toString((char) 0x1B)+Character.toString((char) 0x1B)+"whoisyourdaddy"+Character.toString((char) 0x1B)+Character.toString((char) 0x1B)+"3");
		mBtnPLayTone = (Button)findViewById(R.id.btn_playtone);
		if(null != mBtnPLayTone){
			mBtnPLayTone.setOnClickListener(this);
		}
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
				if(playCode(mEtTonePlay.getText().toString(), true)){
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
		return R.layout.layout_sound_pairing;
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
		    	mBtnPLayTone.setEnabled(false);
			}}, 0);
	}

    public static void onStopGen(final String strCode){
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				Log.i(TAG, "onStopGen(), strCode:["+strCode+"]");
		    	mBtnPLayTone.setEnabled(true);
			}}, 0);
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
