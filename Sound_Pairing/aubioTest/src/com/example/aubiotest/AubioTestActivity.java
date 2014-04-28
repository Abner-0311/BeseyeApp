package com.example.aubiotest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.example.aubiotest.AubioTestConfig.*;

import com.example.aubiotest.AudioBufferMgr.BufRecord;
import com.example.aubiotest.AutoTestResultSaver.MATCH_RESULTS;
import com.example.aubiotest.FreqAnalyzer.FreqData;
import com.example.aubiotest.FreqAnalyzer.IFreqAnalyzeResultCB;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.AudioRecord.OnRecordPositionUpdateListener;
//import android.media.audiofx.NoiseSuppressor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AubioTestActivity extends Activity implements IFreqAnalyzeResultCB{
	public final static String TAG = "SoundPairing";
	
	int bufferRecSize, bufferShortSize;
	short[] shortsRec;
	private Button btnAnalyzeAubio, btnAnalyzeDywa, btnAnalyzeBoth, btnStop, btnSave, mBtnAutoTest, mBtnDelRec, mBtnShoot;
	private EditText etFile, etDecode, etDigital, etSenderOffset, etDistance, etShootDigit, etMsgLen, etTimer;
	private CheckBox ckbGenCode, mckbSender, ckbReceiver, ckbRandom, ckbMaxVolume;
	private TextView mTxtFreq, mTxtFreq2, mTxtFreqSend, mTxtTimeElapse;
	private Spinner mSpTestType, mSpVolumes;
	//private CheckBox mckbSender;
	
    private ListView mFreqListView;
    
    private List<FreqData> mFreqDataList= new ArrayList<FreqData>();
    private FreqDataAdapter mFreqDataAdapter;
      
	private WakeLock mWakeLock;
	private AudioManager am; 

	boolean mIsPause = true;
	boolean mIsStop = false;
	
	private native static boolean nativeClassInit();
	private native void startRecord(int bufferSize, int iSampleRate, int iFrameSize, int iNSIndex, float fAGCLevel, boolean bDeverb, float fDeverbDecay, float dDeverbLevel);
	//Check by aubio lib
	private native float recordAudio(short[] bytes, int bufSize, boolean bNeedToResetFFT);
	private native float analyzeAudio(short[] bytes, int bufSize, boolean bNeedToResetFFT);//For additional checking
	
	//Check by audacity lib
	private native float analyzeAudioViaAudacity(short[] bytes, int bufSize, boolean bNeedToResetFFT, int iLastDetectTone, int[] iFFTValues);
	private native float analyzeAudioViaAudacityAC(short[] bytes, int bufSize, boolean bNeedToResetFFT, int iLastDetectTone, int[] iFFTValues);
	
	//Check by Dywa lib
	private native float analyzeAudioViaDywa(short[] bytes, int bufSize);
	
	private native void endRecord();
	
	private native void runAudioPreprocess(short[] bytes, boolean bNeedToResetFFT);
	private native void runAudioPreprocessAC(short[] bytes, boolean bNeedToResetFFT);
	
	public static native void encodeRS(int[] data, int iCount, int iNumErr);
	public static native void decodeRS(int[] data, int iCount, int iNumErr);
	
	public void decodeRSCode(int[] data, int iCount, int iNumErr){
		//decodeRS(data, iCount, iNumErr);
	} 
	
	//FreqGenerator
//	public native int launchFreqGenerator(String strCurCode, int iDigitalTest);
//	public native int stopFreqGenerator();
	
	//Native auto test
	public native int startAutoTest(String strCurCode, int iDigitalTest);
	public native int stopAutoTest();
	
	public native void setTestMode(boolean bIsSenderMode, boolean bIsReceiverMode);
	public native void playCode(String strCode, boolean bNeedEncode);
	
	//http cgi
	private native boolean receiveAudioBufFromCam(String strHost);
	private native boolean receiveAudioBufThreadRunning();
	private native boolean stopReceiveAudioBufThread();
	
	//ws test client
	private native void setCamWSServerInfo(String strHost, int iPort);
	private native int connectCamWSServer(String strHost, int iPort);
	private native int disconnectCamWSServer();
	private native boolean isCamWSServerConnected();
	
	// Audio
    protected static AudioTrack mAudioTrack;
    public static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
        int channelConfig = isStereo ? AudioFormat.CHANNEL_CONFIGURATION_STEREO : AudioFormat.CHANNEL_CONFIGURATION_MONO;
        int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int frameSize = (isStereo ? 2 : 1) * (is16Bit ? 2 : 1);
        
        Log.e(TAG, "audioInit(), SDL audio: wanted " + (isStereo ? "stereo" : "mono") + " " + (is16Bit ? "16-bit" : "8-bit") + " " + (sampleRate / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        desiredFrames = Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);
        
        if (mAudioTrack == null) {
            mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                    channelConfig, audioFormat, desiredFrames * frameSize, AudioTrack.MODE_STREAM);
            
            // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
            // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
            // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()
            
            if (mAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                Log.e(TAG, "Failed during initialization of Audio Track");
                mAudioTrack = null;
                return -1;
            }
            
            mAudioTrack.play();
        }
       
        Log.e(TAG, "SDL audio: got " + ((mAudioTrack.getChannelCount() >= 2) ? "stereo" : "mono") + " " + ((mAudioTrack.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + (mAudioTrack.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        return 0;
    }
    
    public static int getAudioBufSize(int iSampleRate){
    	int iMinBufSize = AudioTrack.getMinBufferSize(  iSampleRate,
														AudioFormat.CHANNEL_CONFIGURATION_MONO,
														AudioFormat.ENCODING_PCM_16BIT);
    	return iMinBufSize;
    }
    
    public static void audioWriteShortBuffer(short[] buffer) {
//        for (int i = 0; i < buffer.length; ) {
//            int result = mAudioTrack.write(buffer, i, buffer.length - i);
//            if (result > 0) {
//                i += result;
//            } else if (result == 0) {
//                try {
//                    Thread.sleep(1);
//                } catch(InterruptedException e) {
//                    // Nom nom
//                }
//            } else {
//                Log.w(TAG, "SDL audio: error return from write(short)");
//                return;
//            }
//        }
        audioWriteShortBuffer(buffer, buffer.length);
    }
    
    public static void audioWriteShortBuffer(short[] buffer, int iLen) {
    	//Log.w(TAG, "audioWriteShortBuffer(), iLen="+iLen);
        for (int i = 0; i < iLen; ) {
            int result = mAudioTrack.write(buffer, i, iLen - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(short)");
                return;
            }
        }
    }
    
    public static void audioWriteByteBuffer(byte[] buffer) {
        for (int i = 0; i < buffer.length; ) {
            int result = mAudioTrack.write(buffer, i, buffer.length - i);
            if (result > 0) {
                i += result;
            } else if (result == 0) {
                try {
                    Thread.sleep(1);
                } catch(InterruptedException e) {
                    // Nom nom
                }
            } else {
                Log.w(TAG, "SDL audio: error return from write(byte)");
                return;
            }
        }
    }

    public static void audioQuit() {
        if (mAudioTrack != null) {
            mAudioTrack.stop();
            mAudioTrack = null;
        }
    }
    
    protected static AudioRecord sAudioRecorder;
    public static int audioRecordInit(int sampleRate, boolean is16Bit) {
        //int audioFormat = is16Bit ? AudioFormat.ENCODING_PCM_16BIT : AudioFormat.ENCODING_PCM_8BIT;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;//AudioFormat.ENCODING_PCM_8BIT;
        //int frameSize = 2;
        
        Log.e(TAG, "audioRecordInit(), SDL audio: wanted  " + (is16Bit ? "16-bit" : "8-bit") + " " + (sampleRate / 1000f) + "kHz,  frames buffer");
        
        // Let the user pick a larger buffer if they really want -- but ye
        // gods they probably shouldn't, the minimums are horrifyingly high
        // latency already
        int desiredFrames = getAudioRecordBufSize(sampleRate);//Math.max(desiredFrames, (AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat) + frameSize - 1) / frameSize);
        
        if (sAudioRecorder == null) {
        	sAudioRecorder = 
        			new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, audioFormat, desiredFrames);
      
            
            // Instantiating AudioTrack can "succeed" without an exception and the track may still be invalid
            // Ref: https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/media/java/android/media/AudioTrack.java
            // Ref: http://developer.android.com/reference/android/media/AudioTrack.html#getState()
            
            if (sAudioRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Failed during initialization of sAudioRecorder");
                sAudioRecorder = null;
                return -1;
            }
            
            sAudioRecorder.startRecording();
        }
       
        Log.e(TAG, "audioRecordInit(),SDL audio: got  " + ((sAudioRecorder.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT) ? "16-bit" : "8-bit") + " " + (sAudioRecorder.getSampleRate() / 1000f) + "kHz, " + desiredFrames + " frames buffer");
        
        return 0;
    }
    
    public static int getAudioRecordBufSize(int iSampleRate){
    	int iMinBufSize = AudioRecord.getMinBufferSize(  iSampleRate,
														AudioFormat.CHANNEL_CONFIGURATION_MONO,
														AudioFormat.ENCODING_PCM_16BIT);
    	return iMinBufSize;
    }
    
    public static int getAudioRecordBuf(short[] buf, int iLen){
    	if(null != sAudioRecorder){
    		int samplesRead = sAudioRecorder.read(buf, 0, iLen);
			//Log.i(TAG, "record, samplesRead:"+samplesRead);
			if(samplesRead == AudioRecord.ERROR_INVALID_OPERATION){
				Log.w(TAG, "getAudioRecordBuf(), AudioRecord.ERROR_INVALID_OPERATION");
			}else{
				//Log.w(TAG, "getAudioRecordBuf(), buf[99]="+buf[99]);
				return samplesRead;
			}
    	}
    	return 0; 
    }
    
    static short[] sBuf = null;
    public static short[]  getAudioRecordBuf(int iLen){
    	if(null != sAudioRecorder){
    		if(null == sBuf || sBuf.length != iLen){
    			sBuf = new short[iLen];
    		}
    		int samplesRead = sAudioRecorder.read(sBuf, 0, iLen);
			//Log.i(TAG, "record, samplesRead:"+samplesRead);
			if(samplesRead == AudioRecord.ERROR_INVALID_OPERATION){
				Log.w(TAG, "getAudioRecordBuf(), AudioRecord.ERROR_INVALID_OPERATION");
			}else{
				//Log.w(TAG, "getAudioRecordBuf(), sBuf[99]="+sBuf[99]);
				return sBuf;
			}
    	}
    	return null; 
    }
    
    public static void audioRecordDeinit(){
    	if(null != sAudioRecorder)
    		sAudioRecorder.stop();
    	sAudioRecorder=null;
    }
    
    public void feedbackMatchRet(final String strCode, final String strECCode, final String strEncodeMark, final String strDecode, final String strDecodeUnmark, final String strDecodeMark, final int iMatchDesc, final boolean bFromAutoCorrection){
//    	Log.e(TAG, "feedbackMatchRet(), Case "+iMatchDesc+" ===>>>  bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
//				"curCode          = ["+curCode+"], \n" +
//				"curECCode        = ["+curECCode+"], \n" +
//				"curEncodeMark    = ["+curEncodeMark+"], \n" +
//				"strDecodeMark    = ["+strDecodeMark+"]\n"+
//				"strDecodeUnmark  = ["+strDecodeUnmark+"] \n"+
//				"strCode          = ["+strCode+"]\n");
    	handler.post(new Runnable() {
            public void run() {
            	if(null != saver){
            		
            		saver.addRecord(strCode, strECCode, strEncodeMark, strCode, strDecodeUnmark, strDecodeMark, MATCH_RESULTS.values()[iMatchDesc], bFromAutoCorrection);
            		showMsg(String.format("%s\n%s\n%s\n%s\n%s", strCode, strECCode, strCode, strDecodeMark, MATCH_RESULTS.values()[iMatchDesc]+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
            	}
            	
            	if(false == checkTimer()){
					enterStopMode();
				}
            }
        });
    }
    
    public void onTestRoundBegin(){
    	if(mckbSender.isChecked()){
    		if(false == checkTimer()){
				enterStopMode();
			}
    	}
    }
    
    public void onTestRoundEnd(final String strMatchRet, final String strStatistics){
    	Log.i(TAG, "onTestRoundEnd(), strMatchRet = "+strMatchRet+", strStatistics="+strStatistics);
    	handler.post(new Runnable(){
			@Override
			public void run() {
				if(null != saver){
					saver.addRecord(strMatchRet);
				}
				etDecode.setText(strMatchRet);
            	etShootDigit.setText(strStatistics);
			}});
    }
    
    public void onWSClientConnecting(final String strHost){
    	Log.i(TAG, "onWSClientConnecting(), strHost = "+strHost);
    	handler.post(new Runnable(){
			@Override
			public void run() {
				setTitle("Connecting to "+strHost);
			}});
    	
    }
    
    public void onWSClientConnected(final String strHost){
    	Log.i(TAG, "onWSClientConnected(), strHost = "+strHost);
    	handler.post(new Runnable(){

			@Override
			public void run() {
				setTitle("Connected to "+strHost);
			}});
    }

    public void onWSClientClosed(final String strHost){
    	Log.i(TAG, "onWSClientClosed(), strHost = "+strHost);
    	if(mckbSender.isChecked()){
    		handler.postDelayed(new Runnable(){
				@Override
				public void run() {
					setTitle("Connection closed to "+strHost);
					connectToCamViaWS();
				}}, 2000);
    	}
    }
    
    private int miRetryWSServer = 0;
    private void connectToCamViaWS(){
    	if(!isCamWSServerConnected()){
    		if(miRetryWSServer++ < 5){
    			connectCamWSServer("192.168.2.4", 5432);
    		}else{
    			enterStopMode();
    		}
		}else{
			Log.w(TAG, "connectToCamViaWS(), isCamWSServerConnected = true");
		}
    }
 // Audio End
    
    
	String curCode = null;
	String curECCode = null;
	String curEncodeMark;
	String decodeRet = null;
	
	private synchronized void setDecodeRet(String strRet){
		decodeRet = strRet ;
	}
	
	private synchronized String getDecodeRet(){
		return decodeRet ;
	}
	
	final Object objSync = new Object();
	
	final Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
//			String strCode = (String) msg.obj;
//			if(strCode.length() > 0){
//				Log.i(TAG, "AutoTest(), timeout , strCode = "+strCode);
//				FreqAnalyzer.getInstance().reset();
//				synchronized(objSync){
//					objSync.notifyAll();
//				}
//			}
			super.handleMessage(msg);
		}};
		
	private void showMsg(final String msg, final String typeCount){
		handler.post(new Runnable() {
            public void run() {
            	Log.e(TAG, "showMsg(), ---- "+typeCount);
            	etDecode.setText(msg);
            	etShootDigit.setText(typeCount);
            }
        });
	}
	
	static {
	    System.loadLibrary("soundpairing");
	    if (!nativeClassInit())
			throw new RuntimeException("Native Init Failed");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_aubio_test);
		am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);  
		iOriginalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);   
		
		 mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	        // If the adapter is null, then Bluetooth is not supported
	        if (mBluetoothAdapter == null) {
	            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
	            finish();
	            return;
	        }
		
		FreqAnalyzer.getInstance().setIFreqAnalyzeResultCB(this);
		
		mFreqDataAdapter = new FreqDataAdapter(this, mFreqDataList);
		mFreqListView = (ListView)findViewById(R.id.lv_data);
		if(null != mFreqListView){
			mFreqListView.setAdapter(mFreqDataAdapter);
		}
		
		btnAnalyzeAubio = (Button)findViewById(R.id.btn_analyze);
		if(null != btnAnalyzeAubio){
			btnAnalyzeAubio.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					
//					startGen();
//					FreqAnalyzer.getInstance().reset();
//					mIsStop = false;
//					btnAnalyzeAubio.setEnabled(false);
//					btnAnalyzeDywa.setEnabled(false);
//					btnAnalyzeBoth.setEnabled(false);
//					mBtnAutoTest.setEnabled(false);
//					btnStop.setEnabled(true);
//					resetData();
//					beginToAnalyze();
					}});
		}
		
		btnAnalyzeDywa = (Button)findViewById(R.id.btn_analyze2);
		if(null != btnAnalyzeDywa){
			btnAnalyzeDywa.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
//					startGen();
//					mIsStop = false;
//					FreqAnalyzer.getInstance().reset();
//					btnAnalyzeAubio.setEnabled(false);
//					btnAnalyzeDywa.setEnabled(false);
//					btnAnalyzeBoth.setEnabled(false);
//					mBtnAutoTest.setEnabled(false);
//					btnStop.setEnabled(true);
//					resetData();
//					beginToAnalyzeDywa();
				}});
		}
		
		btnStop = (Button)findViewById(R.id.btn_stop);
		if(null != btnStop){
			btnStop.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
					enterStopMode();
					//stopFreqGenerator();
				}});
		}
		
		btnAnalyzeBoth = (Button)findViewById(R.id.btn_analyzeBoth);
		if(null != btnAnalyzeBoth){
			btnAnalyzeBoth.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View arg0) {
//					startGen();
//					mIsStop = false;
//					FreqAnalyzer.getInstance().reset();
//					btnAnalyzeAubio.setEnabled(false);
//					btnAnalyzeDywa.setEnabled(false);
//					btnAnalyzeBoth.setEnabled(false);
//					mBtnAutoTest.setEnabled(false);
//					btnStop.setEnabled(true);
//					resetData();
//					beginToAnalyzeBoth();
				}});
		}
		
		btnSave = (Button)findViewById(R.id.btn_save);
		if(null != btnSave){
			btnSave.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					String file = etFile.getText().toString().trim();
					if(".txt".equals(file) || 0 == file.length()){
						file = System.currentTimeMillis()+".txt";
					}
					
					saveDataToFile(file);
				}});
		} 
		
		mBtnAutoTest = (Button)findViewById(R.id.btn_test);
        if(null != mBtnAutoTest){
        	mBtnAutoTest.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					mIsStop = false;
					FreqAnalyzer.getInstance().reset();
					btnAnalyzeAubio.setEnabled(false);
					btnAnalyzeDywa.setEnabled(false);
					btnAnalyzeBoth.setEnabled(false);
					mBtnAutoTest.setEnabled(false);
					btnStop.setEnabled(true);
//					if(SELF_TEST)
//						beginToSelfAnalyze();
//					else
//						beginToAnalyze();
					resetBuffer();
					runAutoTest();
				}
        	});
        }
        
//        mBtnShoot = (Button)findViewById(R.id.btn_shoot);
//        if(null != mBtnShoot){
//        	mBtnShoot.setOnClickListener(new OnClickListener(){
//        		boolean bFirstTime = true;
//        		Thread threadShoot = null;
//        		
//        		Object objShoot = new Object();
//				@Override
//				public void onClick(View arg0) {
//					mIsStop = false;
//					FreqAnalyzer.getInstance().reset();
//					btnAnalyzeAubio.setEnabled(false);
//					btnAnalyzeDywa.setEnabled(false);
//					btnAnalyzeBoth.setEnabled(false);
//					mBtnAutoTest.setEnabled(false);
//					btnStop.setEnabled(true);
//					if(bFirstTime){
//						bFirstTime = false;
//						beginToAnalyze();
//					}
//					
//					if(null == threadShoot){
//						threadShoot = new Thread(){
//							public void run(){
//								while(!mIsStop){
//									sendBTMsg(BT_MSG_PURE+etShootDigit.getText().toString());
//									synchronized(objShoot){
//										try {
//											objShoot.wait();
//										} catch (InterruptedException e) {
//											// TODO Auto-generated catch block
//											e.printStackTrace();
//										}
//									}
//								}
//								threadShoot = null;
//							}
//						};
//						threadShoot.start();
//					}
//					synchronized(objShoot){
//						objShoot.notify();
//					}
//				}
//        	});
//        }
        
        mBtnDelRec = (Button)findViewById(R.id.btn_delete_rec);
        if(null != mBtnDelRec){
        	mBtnDelRec.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View arg0) {
					
					if(!receiveAudioBufThreadRunning()){
						receiveAudioBufFromCam("");
					}else{
						stopReceiveAudioBufThread();
					}
					
					setTestMode(true, true);
//					//BT Testing
//					int iDistance = etDistance.getText().length() == 0 ? 0 : Integer.parseInt(etDistance.getText().toString());
//					StringBuilder tmp = new StringBuilder();
//					for(int i = 0; i< iDistance; i++){
//						tmp.append(AubioTestConfig.sCodeTable.get(i%16));
//					}
//					sendBTMsg(tmp.toString());
//					final int iDigitalToTest = Integer.parseInt(etMsgLen.getText().toString());
//			    	
//			    	final boolean bIsSenderMode = isSenderMode();
//			    	final boolean bIsReceiverMode = isReceiverMode();
//			    	final boolean bIsAutoTestMode = !bIsSenderMode && !bIsReceiverMode;
//			    	int iDistance = etDistance.getText().length() == 0 ? 0 : Integer.parseInt(etDistance.getText().toString());
//			    	int iOriginalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
//			    	final int iMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//			    	final int iVolumeChoosen = Integer.parseInt(mSpVolumes.getSelectedItem().toString());
//			    	
//					final String strFileName = AutoTestResultSaver.genFileName(iDigitalToTest, bIsAutoTestMode?0:iDistance, iVolumeChoosen, ckbRandom.isChecked(), !bIsAutoTestMode);
//					
//					AlertDialog.Builder builder = new AlertDialog.Builder(AubioTestActivity.this);
//					builder.setIcon(android.R.drawable.ic_dialog_alert);
//					builder.setTitle("Warning");
//					builder.setMessage("Do you wanna to delete file "+strFileName+" ?");
//					builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//					    public void onClick(DialogInterface dialog, int item) {
//					    	dialog.dismiss();
//					    	Toast.makeText(AubioTestActivity.this, AutoTestResultSaver.deleteAutoTestRec(strFileName), Toast.LENGTH_LONG).show();
//					    }
//					});
//					builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
//					    public void onClick(DialogInterface dialog, int item) {
//					    	dialog.dismiss();
//					    }
//					});
//
//					Dialog dialog = builder.create();
//					if(null != dialog){
//						dialog.setCanceledOnTouchOutside(true);
//						dialog.show();
//					}
				}
        	});
        }
		
		etFile = (EditText)findViewById(R.id.et_file);
		etDecode = (EditText)findViewById(R.id.et_decode);
		etDigital = (EditText)findViewById(R.id.et_digital);
		etSenderOffset = (EditText)findViewById(R.id.et_sender_offset);
		etDistance = (EditText)findViewById(R.id.et_dist);
		etMsgLen = (EditText)findViewById(R.id.et_msglen);
		etTimer = (EditText)findViewById(R.id.et_timer);
		etShootDigit = (EditText)findViewById(R.id.et_digit_shoot);
		
		mTxtFreq= (TextView)findViewById(R.id.txtFreq);
		mTxtFreq2= (TextView)findViewById(R.id.txtFreq2);
		
		mTxtFreqSend = (TextView)findViewById(R.id.txtFreqSend);
		mTxtTimeElapse = (TextView)findViewById(R.id.txt_timer_elapse);
		
		ckbGenCode = (CheckBox)findViewById(R.id.ckb_gen);
		mckbSender = (CheckBox)findViewById(R.id.ckb_at_sender);
		if(null != mckbSender){
			mckbSender.setOnCheckedChangeListener(new OnCheckedChangeListener(){

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
					if(arg1){
						ckbReceiver.setChecked(false);
						
//						// Check that we're actually connected before trying anything
//				        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//				        	BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(BT_BINDING_MAC);
//				            // Attempt to connect to the device
//				            mChatService.connect(device, true);
//				        }
						setTestMode(true, false);
						connectToCamViaWS();
						miRetryWSServer=0;
					}
				}});
		}
		
		ckbReceiver = (CheckBox)findViewById(R.id.ckb_at_receiver);
		if(null != ckbReceiver){
			ckbReceiver.setOnCheckedChangeListener(new OnCheckedChangeListener(){

				@Override
				public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
					if(arg1){
						mckbSender.setChecked(false);
//						// Check that we're actually connected before trying anything
//				        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
//				        	BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(BT_BINDING_MAC_SENDER);
//				            // Attempt to connect to the device
//				            mChatService.connect(device, true);
//				        }
						setTestMode(false, true);
					}
				}});
		}
		
		ckbRandom = (CheckBox)findViewById(R.id.ckb_random);
		ckbMaxVolume = (CheckBox)findViewById(R.id.ckb_max_volume);
		
		mSpTestType = (Spinner)findViewById(R.id.spinner_type);
		if(null != mSpTestType){
			List<String> list = new ArrayList<String>();
			list.add(FINITE_FIELD_TYPE.FFT_D16.toString());
			//list.add(FINITE_FIELD_TYPE.FFT_D_8.toString());
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpTestType.setAdapter(dataAdapter);
			
			mSpTestType.setOnItemSelectedListener(new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}});
		}
		
		mSpVolumes = (Spinner)findViewById(R.id.spin_volume);
		if(null != mSpVolumes){
			List<String> list = new ArrayList<String>();
			for(int i = 0; i <= 100; i+=10){
				list.add(""+i);			
			}
			
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
			dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mSpVolumes.setAdapter(dataAdapter);
			mSpVolumes.setSelection(5);//60%
			
			mSpVolumes.setOnItemSelectedListener(new OnItemSelectedListener(){
				@Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
					
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub
					
				}});
			
		}
		
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "FGActivity");
	}
	
	private void enterStopMode(){
    	handler.post(new Runnable() {
            public void run() {
            	stopAutoTest();
            	am.setStreamVolume(AudioManager.STREAM_MUSIC, iOriginalVolume, AudioManager.FLAG_PLAY_SOUND);
            	mbStop = true;
            	mIsStop = true;
        		btnAnalyzeAubio.setEnabled(true);
        		btnAnalyzeDywa.setEnabled(true);
        		btnAnalyzeBoth.setEnabled(true);
        		mBtnAutoTest.setEnabled(true);
        		btnSave.setEnabled(true);
        		btnStop.setEnabled(false);
        		
        		if(null != mMyCounter)
    				mMyCounter.cancel();
        		
        		synchronized(objSync){
        			objSync.notifyAll();
        		}
            }
        });
	}

    @Override
	protected void onPause() {
    	mIsPause = true;
    	mbStop = true;
    	super.onPause();
	}
    
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		mIsPause = false;
		mbStop = false;
		
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
	}
	
	private void saveDataToFile(String strFile){	
		Log.i(TAG, "saveDataToFile(), strFile:"+strFile);
		File root = Environment.getExternalStorageDirectory();
	    File outDir = new File(root.getAbsolutePath() + File.separator + "AudioTest");
	    if (!outDir.isDirectory()) {
	      outDir.mkdir();
	    }
	    
	    try {
	      if (!outDir.isDirectory()) {
	        throw new IOException(
	            "Unable to create directory EZ_time_tracker. Maybe the SD card is mounted?");
	      }
	      File outputFile = new File(outDir, strFile);
	      BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
	      
	      long lStart = -1;
	      int iSize = mFreqDataList.size();
	      for(int index = iSize -1; index >= 0; index--){
	    	  FreqData data = mFreqDataList.get(index);
	    	  if(null != data){
		    	  if(-1 == lStart){
					lStart = data.mTime;
				  }
				  String strContent = String.format("%d\t%2f\n", (data.mTime- lStart), data.mdFreq);
				  Log.i(TAG, "saveDataToFile(), strContent:"+strContent);
				  writer.write(strContent);
	    	  }
	      }
		  writer.flush();
	      writer.close();
	    } catch (IOException e) {
	    	Log.e(TAG, "saveDataToFile(), e:"+e.toString());
	    }
	}
	
	private float duration = 1.0f; // seconds
    private final int sampleRate = 44100;
    private int numSamples = (int) (duration * sampleRate);
    private double sample[] ;
    private final double freqOfTone = 440; // hz

    private byte generatedSnd[];
    
    private boolean mbStop = true;
    
    private boolean isSenderMode(){
    	return (null != mckbSender && mckbSender.isChecked()) && (null != ckbReceiver && false == ckbReceiver.isChecked());
    }
    
    private boolean isReceiverMode(){
    	return (null != ckbReceiver && ckbReceiver.isChecked()) && (null != mckbSender && false == mckbSender.isChecked());
    }
    
    public static class MatchRetSet{
    	@Override
		public String toString() {
			return "MatchRetSet [prevMatchRetType=" + prevMatchRetType
					+ ", strDecodeMark=" + strDecodeMark + ", strDecodeUnmark="
					+ strDecodeUnmark + ", strCode=" + strCode + "]";
		}
    	
		public MatchRetSet(MATCH_RESULTS prevMatchRet, String strDecodeMark,
				String strDecodeUnmark, String strCode) {
			super();
			this.prevMatchRetType = prevMatchRet;
			this.strDecodeMark = strDecodeMark;
			this.strDecodeUnmark = strDecodeUnmark;
			this.strCode = strCode;
		}
    	
		public MATCH_RESULTS prevMatchRetType;
    	public String strDecodeMark;
    	public String strDecodeUnmark;
    	public String strCode;
    }
    
    private void registerAnalysisCallback(final StringBuilder tmpRet, final AutoTestResultSaver saver, final boolean isSenderMode){
		IFreqAnalyzeResultCB sIFreqAnalyzeResultCBListener = new IFreqAnalyzeResultCB(){	
			
			@Override
			public void onAppendResult(String strCode) {
				Log.i(TAG, "onAppendResult(), strCode:"+strCode);
				tmpRet.append(strCode);
			}

			@Override
			public void onSetResult(String strCode, String strDecodeMark, String strDecodeUnmark, boolean bFromAutoCorrection, MatchRetSet prevMatchRet) {
				Log.i(TAG, "onSetResult(), strCode:"+strCode+", strDecodeMark = "+strDecodeMark);
				if(strCode.length() > 0 || strDecodeMark.length() >0){
					//int iNumEc = FreqGenerator.getNumOfEcBytes(curCode.length());
					setDecodeRet(strCode);
					if(false == isSenderMode){
						if(null != strCode && strCode.equals(curCode)){
							if(null != curCode && strDecodeUnmark.startsWith(curCode)){
								Log.e(TAG, "runAutoTest(), Case 1 ===>>> Detection match before error correction, bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
												"curCode          = ["+curCode+"], \n" +
												"curECCode        = ["+curECCode+"], \n" +
												"curEncodeMark    = ["+curEncodeMark+"], \n" +
												"strDecodeMark    = ["+strDecodeMark+"]\n"+
												"strDecodeUnmark  = ["+strDecodeUnmark+"] \n"+
												"strCode          = ["+strCode+"]\n");
								saver.addRecord(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, MATCH_RESULTS.DESC_MATCH, bFromAutoCorrection);
								showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, strCode, strDecodeMark, MATCH_RESULTS.DESC_MATCH+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
							}else{
								Log.e(TAG, "runAutoTest(), Case 2 ===>>> Detection match after error correction, bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
											"curCode          = ["+curCode+"], \n" +
											"curECCode        = ["+curECCode+"], \n" +
											"curEncodeMark    = ["+curEncodeMark+"], \n" +
											"strDecodeMark    = ["+strDecodeMark+"]\n"+
											"Difference       = ["+findDifference(curEncodeMark, strDecodeMark)+"]\n"+
											"strDecodeUnmark  = ["+strDecodeUnmark+"] \n"+
											"strCode          = ["+strCode+"]\n");
								
								if(bFromAutoCorrection){
									if(null != prevMatchRet && prevMatchRet.prevMatchRetType.ordinal() <= MATCH_RESULTS.DESC_MATCH_EC.ordinal()){
										adaptPrevMatchRet(prevMatchRet);
									}else{
										saver.addRecord(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, MATCH_RESULTS.DESC_MATCH_EC, bFromAutoCorrection);
										showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, strCode, strDecodeMark, MATCH_RESULTS.DESC_MATCH_EC+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
									}
								}else{
									MatchRetSet matchRet = new MatchRetSet(MATCH_RESULTS.DESC_MATCH_EC, strDecodeMark, strDecodeUnmark, strCode);
									tmpRet.delete(0, tmpRet.length());
									FreqAnalyzer.getInstance().performAutoCorrection(matchRet);
									return;
								}
							}
						}else{
							if(null != curCode && strDecodeUnmark.startsWith(curCode)){
								Log.e(TAG, "runAutoTest(), Case 3 ===>>> Detection mismatch but msg matched, bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
										"curCode          = ["+curCode+"], \n" +
										"curECCode        = ["+curECCode+"], \n" +
										"curEncodeMark    = ["+curEncodeMark+"], \n" +
										"strDecodeMark    = ["+strDecodeMark+"]\n"+
										"Difference       = ["+findDifference(curEncodeMark, strDecodeMark)+"]\n"+
										"strDecodeUnmark  = ["+strDecodeUnmark+"] \n"+
										"strCode          = ["+strCode+"]\n");
								if(bFromAutoCorrection){
									if(null != prevMatchRet && prevMatchRet.prevMatchRetType.ordinal() <= MATCH_RESULTS.DESC_MATCH_EC.ordinal()){
										adaptPrevMatchRet(prevMatchRet);
									}else{
										saver.addRecord(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, MATCH_RESULTS.DESC_MATCH_MSG, bFromAutoCorrection);
										showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, strCode, strDecodeMark, MATCH_RESULTS.DESC_MATCH_MSG+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
									}
								}else{
									MatchRetSet matchRet = new MatchRetSet(MATCH_RESULTS.DESC_MATCH_MSG, strDecodeMark, strDecodeUnmark, strCode);
									tmpRet.delete(0, tmpRet.length());
									FreqAnalyzer.getInstance().performAutoCorrection(matchRet);
									return;
								}
							}else{
								Log.e(TAG, "runAutoTest(), Case 4 ===>>> Detection mismatch, bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
										"curCode          = ["+curCode+"], \n" +
										"curECCode        = ["+curECCode+"], \n" +
										"curEncodeMark    = ["+curEncodeMark+"], \n" +
										"strDecodeMark    = ["+strDecodeMark+"]\n"+
										"Difference       = ["+findDifference(curEncodeMark, strDecodeMark)+"]\n"+
										"strDecodeUnmark  = ["+strDecodeUnmark+"] \n"+
										"strCode          = ["+strCode+"]\n");
								if(bFromAutoCorrection){
									if(null != prevMatchRet && prevMatchRet.prevMatchRetType.ordinal() <= MATCH_RESULTS.DESC_MATCH_MSG.ordinal()){
										adaptPrevMatchRet(prevMatchRet);
									}else{
										saver.addRecord(curCode, curECCode, curEncodeMark, strCode, strDecodeUnmark, strDecodeMark, MATCH_RESULTS.DESC_MISMATCH, bFromAutoCorrection);
										showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, strCode, strDecodeMark, MATCH_RESULTS.DESC_MISMATCH+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
									}
								}else{
									MatchRetSet matchRet = new MatchRetSet(MATCH_RESULTS.DESC_MISMATCH, strDecodeMark, strDecodeUnmark, strCode);
									tmpRet.delete(0, tmpRet.length());
									FreqAnalyzer.getInstance().performAutoCorrection(matchRet);
									return;
								}
							}
						}
					}
					tmpRet.delete(0, tmpRet.length());
					FreqAnalyzer.getInstance().reset();
					FreqAnalyzer.getInstance().endToTrace();
					resetBuffer();
					synchronized(objSync){
						objSync.notifyAll();
					}
				}
			}
			
			@Override
			public void onTimeout(FreqAnalyzer freqAnalyzer, boolean bFromAutoCorrection, MatchRetSet prevMatchRet){
				Log.e(TAG, "onTimeout(), bFromAutoCorrection:"+bFromAutoCorrection);
				/*if(null == getDecodeRet())*/{
					if(false == freqAnalyzer.checkEndPoint()){
						String strDecodeUnmark = AubioTestConfig.decodeConsecutiveDigits(tmpRet.toString());
						if(null != strDecodeUnmark && strDecodeUnmark.startsWith(curCode)){
							if(strDecodeUnmark.startsWith(curCode+curECCode)){
								Log.e(TAG, "!!!!!!!!!!!!!!!!!!!runAutoTest(), Case 7 ===>>> detection timeout but msg+errCode matched, bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
										"curCode          = ["+curCode+"], \n" +
										"curECCode        = ["+curECCode+"], \n" +
										"curEncodeMark    = ["+curEncodeMark+"], \n" +
										"tmpRet           = ["+tmpRet+"]\n" +
										"strDecodeUnmark  = ["+strDecodeUnmark+"]");
								if(bFromAutoCorrection){
									if(null != prevMatchRet && prevMatchRet.prevMatchRetType.ordinal() <= MATCH_RESULTS.DESC_MATCH_MSG.ordinal()){
										adaptPrevMatchRet(prevMatchRet);
									}else{
										saver.addRecord(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.toString(), MATCH_RESULTS.DESC_TIMEOUT_MSG_EC, bFromAutoCorrection);
										showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, tmpRet.toString(), "", MATCH_RESULTS.DESC_TIMEOUT_MSG_EC+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
									}
								}else{
									MatchRetSet matchRet = new MatchRetSet(MATCH_RESULTS.DESC_TIMEOUT_MSG_EC, tmpRet.toString(), strDecodeUnmark, "XXX");
									tmpRet.delete(0, tmpRet.length());
									freqAnalyzer.performAutoCorrection(matchRet);
									return;
								}
							}else{
								Log.e(TAG, "!!!!!!!!!!!!!!!!!!!runAutoTest(), Case 6 ===>>> detection timeout but msg matched, bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
										"curCode          = ["+curCode+"], \n" +
										"curECCode        = ["+curECCode+"], \n" +
										"curEncodeMark    = ["+curEncodeMark+"], \n" +
										"tmpRet           = ["+tmpRet+"]\n" +
										"strDecodeUnmark  = ["+strDecodeUnmark+"]");
								if(bFromAutoCorrection){
									if(null != prevMatchRet && prevMatchRet.prevMatchRetType.ordinal() <= MATCH_RESULTS.DESC_MATCH_MSG.ordinal()){
										adaptPrevMatchRet(prevMatchRet);
									}else{
										saver.addRecord(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.toString(), MATCH_RESULTS.DESC_TIMEOUT_MSG, bFromAutoCorrection);
										showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, tmpRet.toString(), "", MATCH_RESULTS.DESC_TIMEOUT_MSG+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
									}
								}else{
									MatchRetSet matchRet = new MatchRetSet(MATCH_RESULTS.DESC_TIMEOUT_MSG, tmpRet.toString(), strDecodeUnmark, "XXX");
									tmpRet.delete(0, tmpRet.length());
									freqAnalyzer.performAutoCorrection(matchRet);
									return;
								}
							}
						}else{
							Log.e(TAG, "!!!!!!!!!!!!!!!!!!!runAutoTest(), Case 5 ===>>> detection timeout and msg mismatched, bFromAutoCorrection:"+bFromAutoCorrection+"\n" +
									"curCode          = ["+curCode+"], \n" +
									"curECCode        = ["+curECCode+"], \n" +
									"curEncodeMark    = ["+curEncodeMark+"], \n" +
									"tmpRet           = ["+tmpRet+"]\n" +
									"strDecodeUnmark  = ["+strDecodeUnmark+"]");
							if(bFromAutoCorrection){
								if(null != prevMatchRet && prevMatchRet.prevMatchRetType.ordinal() <= MATCH_RESULTS.DESC_MATCH_MSG.ordinal()){
									adaptPrevMatchRet(prevMatchRet);
								}else{
									saver.addRecord(curCode, curECCode, curEncodeMark, "XXX", strDecodeUnmark, tmpRet.toString(), MATCH_RESULTS.DESC_TIMEOUT, bFromAutoCorrection);
									showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, tmpRet.toString(), "", MATCH_RESULTS.DESC_TIMEOUT+(bFromAutoCorrection?"_AC":"")), saver.getTypeCountValue());
								}
							}else{
								MatchRetSet matchRet = new MatchRetSet(MATCH_RESULTS.DESC_TIMEOUT, tmpRet.toString(), strDecodeUnmark, "XXX");
								tmpRet.delete(0, tmpRet.length());
								freqAnalyzer.performAutoCorrection(matchRet);
								return;
							}
						}
					}else{
						//if end point is detected, the path will be redirect to onSetResult() callback
						Log.e(TAG, "onTimeout(), checkEndPoint is true, bFromAutoCorrection:"+bFromAutoCorrection);
						return;
					}
				}
				
				tmpRet.delete(0, tmpRet.length());
				FreqAnalyzer.getInstance().reset();
				FreqAnalyzer.getInstance().endToTrace();
				resetBuffer();
				synchronized(objSync){
					objSync.notifyAll();
				}
			}
			
			private void adaptPrevMatchRet(MatchRetSet prevMatchRet){
				Log.i(TAG, "adaptPrevMatchRet(), previous result is better,\n prevMatchRet = "+prevMatchRet);
				saver.addRecord(curCode, curECCode, curEncodeMark, prevMatchRet.strCode, prevMatchRet.strDecodeUnmark, prevMatchRet.strDecodeMark, prevMatchRet.prevMatchRetType, false);
				showMsg(String.format("%s\n%s\n%s\n%s\n%s", curCode, curECCode, prevMatchRet.strCode, prevMatchRet.strDecodeMark, prevMatchRet.prevMatchRetType), saver.getTypeCountValue());
			}

			@Override
			public void onDetectStart() {
				resetData();
			}
			
			//RealDoubleFFT transformerInner;
			//final double[] toTransform = new double[FRAME_SIZE_REC];
			@Override
			public float onBufCheck(short[] buf, long lBufTs, boolean bResetFFT, int[] iFFTValues){
				//Log.e(TAG, "onBufCheck(), buf[99] = "+buf[99]+", bResetFFT = ["+bResetFFT+"]");
//				if(null == transformerInner || bResetFFT)
//					transformerInner = new RealDoubleFFT(FRAME_SIZE_REC);
//				return analyzeFreqByFFTJ(transformerInner, buf, buf.length, toTransform);
				//analyzeAudio(buf, buf.length, true);
				//return analyzeAudio(buf, buf.length, false);
				
				if(AUBIO_FFT){
					analyzeAudio(buf, buf.length, true);
					return analyzeAudio(buf, buf.length, false);
				}else{
					analyzeAudioViaAudacityAC(buf, buf.length, bResetFFT, 0, null);
					return analyzeAudioViaAudacityAC(buf, buf.length, bResetFFT, FreqAnalyzer.getInstance().getLastDetectedToneIdx(lBufTs), iFFTValues);//addtionalFFTCheck(buf, bResetFFT);
				}
			}

			@Override
			public void decodeRSCode(int[] data, int iCount, int iNumErr) {
				// TODO Auto-generated method stub
				
			}
		};
			
		
		FreqAnalyzer.getInstance().setSenderMode(isSenderMode);
		FreqAnalyzer.getInstance().setIFreqAnalyzeResultCB(sIFreqAnalyzeResultCBListener);
    }
    
	static private String findDifference(String strSrc, String strDecode){
		StringBuilder strRet = new StringBuilder(strDecode);
		int iLenSrc = (null != strSrc)?strSrc.length():0;
		for(int i =0; i < iLenSrc; i++){
			if(i >= strDecode.length())
				break;
			if(!strSrc.substring(i, i+1).equals(strDecode.substring(i, i+1))){
				strRet.replace(i, i+1, "#");
				//break;
			}
		}
		return strRet.toString();
	}
    
    private Object mBTSenderLock = new Object();
    private Object mBTReceiverLock = new Object();
    
    private String mstrCurTransferCode = null;
    private String mstrCurTransferTs = null;
    private boolean mbSenderAcked = false;
    
    private synchronized void resetBTParams(){
    	mstrCurTransferCode = null;
    	mstrCurTransferTs = null;
    	mbSenderAcked = false;
    }
    
    private void onBTMsgReceived(String strMsg){
    	final boolean bIsSenderMode = isSenderMode();
    	final boolean bIsReceiverMode = isReceiverMode();
		
    	Log.i(TAG, "onBTMsgReceived(), bIsReceiverMode = "+bIsReceiverMode+", mstrCurTransferTs = ["+mstrCurTransferTs+"], \n msg = ["+strMsg+"]");
    	
    	/*if(bIsReceiverMode){
    		
    	}else */if(bIsSenderMode){
    		if(null != strMsg){
    			if(strMsg.startsWith(BT_MSG_SET_VOLUME)){
    				int iEndIdx = strMsg.indexOf(BT_MSG_SET_VOLUME_END);
    				final String strVol = strMsg.substring(BT_MSG_SET_VOLUME.length(), iEndIdx);
    				
    				try{
	    				final int iVolumeChoosen = Integer.parseInt(strVol);
	    				final int iMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	    				am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) ((iVolumeChoosen/100.f)*iMaxVolume), AudioManager.FLAG_PLAY_SOUND); 
	    				Log.i(TAG, "onBTMsgReceived(), set volume of sender as "+iVolumeChoosen);
	    				if(null != mSpVolumes){
	    					mSpVolumes.post(new Runnable(){
								@Override
								public void run() {
									mSpVolumes.setSelection(iVolumeChoosen/10);
								}});
	    				}
    				}catch(NumberFormatException e){
    					Log.i(TAG, "onBTMsgReceived(), can not parse "+strVol+" to integer");
    				}finally{
    					if(-1 < iEndIdx){
    						strMsg = strMsg.substring(iEndIdx+BT_MSG_SET_VOLUME_END.length());
    					}
    				}
    			}
    			
    			if(null != strMsg){
    				String[] msg = strMsg.split(BT_MSG_DIVIDER);
        			if(2 == msg.length){
        				Log.i(TAG, "onBTMsgReceived(), bIsSenderMode, msg[0] = ["+msg[0]+"], msg[1] = ["+msg[1]+"]");
        				if(msg[0].equals(BT_MSG_ACK) && mstrCurTransferTs.equals(msg[1])){
        					try {
    							Thread.sleep(1000L);
    						} catch (InterruptedException e) {
    							// TODO Auto-generated catch block
    							e.printStackTrace();
    						}
        					if(mstrCurTransferCode.startsWith(BT_MSG_PURE)){
        						//FreqGenerator.getInstance().playCode2(mstrCurTransferCode.substring(BT_MSG_PURE.length()), false);
        						playCode(mstrCurTransferCode.substring(BT_MSG_PURE.length()), false);
        					}else{
        						//FreqGenerator.getInstance().playCode2(mstrCurTransferCode, true);
        						playCode(mstrCurTransferCode, true);
        					}
        					
        					resetBTParams();
        				}else{
        					mstrCurTransferTs = msg[0];
        					mstrCurTransferCode = msg[1];
        					sendBTMsg(String.format(BT_MSG_FORMAT, BT_MSG_ACK, mstrCurTransferTs));
        				}
        			}
    			}
    		}
    	}else{
    		if(null != strMsg){
    			String[] msg = strMsg.split(BT_MSG_DIVIDER);
    			Log.i(TAG, "onBTMsgReceived(), bIsReceiverMode, msg[0] = ["+msg[0]+"], msg[1] = ["+msg[1]+"]");
    			if(2 == msg.length && null != mstrCurTransferTs){
    				if(msg[0].equals(BT_MSG_ACK) && mstrCurTransferTs.equals(msg[1])){
    					sendBTMsg(String.format(BT_MSG_FORMAT, BT_MSG_ACK, mstrCurTransferTs));
    					
    					synchronized(mBTReceiverLock){
    						Log.i(TAG, "onBTMsgReceived(), notify receiver, mstrCurTransferCode = ["+mstrCurTransferCode+"]");
    						mbSenderAcked = true;
	    					mBTReceiverLock.notify();
    	        		}
    					
    					resetBTParams();
    				}
    			}
    		}
    	}
	}
	
	public void sendBTMsg(String strMsg){
		final boolean bIsSenderMode = isSenderMode();
    	//final boolean bIsReceiverMode = isReceiverMode();
    	
    	/*if(bIsReceiverMode){
    		
    		
    	}else */if(bIsSenderMode){
    		sendMessage(strMsg);
    	}else{
    		if(null == mstrCurTransferCode){
    			mbSenderAcked = false;
        		do{
        			mstrCurTransferCode = strMsg;
        			mstrCurTransferTs = String.valueOf(System.currentTimeMillis());
            		sendMessage(String.format(BT_MSG_FORMAT, mstrCurTransferTs, mstrCurTransferCode));
            		
            		synchronized(mBTReceiverLock){
            			try {
            				Log.i(TAG, "sendBTMsg(), begin wait sender signal, mstrCurTransferCode = ["+mstrCurTransferCode+"]");
        					mBTReceiverLock.wait(5000L);
        					Log.i(TAG, "sendBTMsg(), exit wait sender signal, mbSenderAcked = "+mbSenderAcked);
        					if(!mbSenderAcked){
        						AudioBufferMgr.getInstance().recycleAllBuffer();
        					}
        				} catch (InterruptedException e) {
        					e.printStackTrace();
        				}
            		}
        		}while(!mbSenderAcked && (mChatService.getState() == BluetoothChatService.STATE_CONNECTED));
    		}else{
    			sendMessage(strMsg);
    		}
    	}
	}
	
	private void sendPureBTMsg(String strMsg){
		sendMessage(strMsg);
	}
	
	private long mlTimerEnd = 0;
	private MyCounter mMyCounter = null;
	static final long ONE_HOUR = 60*60*1000L;
	static final long ONE_MIN  = 60*1000L;
	static final long ONE_SEC  = 1000L;
	
    void triggerTimer(){
    	if(null != etTimer){
    		
    		String strTimer = etTimer.getText().toString();
    		if(null != strTimer && 0 < strTimer.length()){
    			int iTimerHour =  Integer.parseInt(strTimer);
    			if(0 < iTimerHour){
    				//mlTimerEnd = System.currentTimeMillis() + iTimerHour*60*60*1000;
    				mMyCounter = new MyCounter(iTimerHour*ONE_HOUR, 1000);
    				mMyCounter.start();
    			}
    		}else{
    			//mlTimerEnd = 0;
    			mMyCounter = null;
    			mTxtTimeElapse.post(new Runnable(){
					@Override
					public void run() {
						mTxtTimeElapse.setText("Unlimited");
					}});
    		}
    	}
    }
    
    boolean checkTimer(){
//    	boolean bRet = true;
//    	if(0 < mlTimerEnd){
//    		final long lDelta = mlTimerEnd - System.currentTimeMillis();
//    		bRet = lDelta >0;
//    		if(bRet){
//    			
//    		}
//    	}
//    	return bRet;//true means not timeout 
    	return (null == mMyCounter) || false == mMyCounter.isbFinished();
    }
    
    
    
    public class MyCounter extends CountDownTimer {
       private boolean mbFinished = false;
       
	   public MyCounter(long millisInFuture, long countDownInterval) {
		   super(millisInFuture, countDownInterval);
		   mbFinished = false;
	   }
	   
	   public boolean isbFinished(){
		   return mbFinished;
	   }
	   
	   @Override
	    public void onFinish() {
		   mTxtTimeElapse.setText("0 sec");
		   mbFinished = true;
	   }
	   
	   @Override
	   public void onTick(long millisUntilFinished) {
		   long iHour = millisUntilFinished/ONE_HOUR;
		   if(100 <= iHour){
			   mTxtTimeElapse.setText("longer than 100 hours");
		   }else{
			   long iMins = (millisUntilFinished%ONE_HOUR)/ONE_MIN;
			   long iSecs = (millisUntilFinished%ONE_MIN)/ONE_SEC;
			   String strElapse = "";
			   if(iHour >= 10){
				   strElapse += iHour+":";
			   }else if(iHour >0){
				   strElapse += "0"+iHour+":";
			   }else{
				   strElapse += "00:";
			   }
			   
			   if(iMins >= 10){
				   strElapse += iMins+":";
			   }else if(iMins >0){
				   strElapse += "0"+iMins+":";
			   }else{
				   strElapse += "00:";
			   }
			   
			   if(iSecs >= 10){
				   strElapse += iSecs;
			   }else if(iSecs >0){
				   strElapse += "0"+iSecs;
			   }else{
				   strElapse += "00";
			   }
			   
			   mTxtTimeElapse.setText(strElapse);
		   }
		   
	   } 
	}  // end of MyCount
    
    int iOriginalVolume;//
    AutoTestResultSaver saver;
    private void runAutoTest(){
    	final int iDigitalToTest = Integer.parseInt(etMsgLen.getText().toString());
    	
    	final boolean bIsSenderMode = isSenderMode();
    	final boolean bIsReceiverMode = isReceiverMode();
    	final boolean bIsAutoTestMode = !bIsSenderMode && !bIsReceiverMode;
    	int iDistance = etDistance.getText().length() == 0 ? 0 : Integer.parseInt(etDistance.getText().toString());
    	int iOriginalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    	final int iMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    	final int iVolumeChoosen = Integer.parseInt(mSpVolumes.getSelectedItem().toString());
    	
    	triggerTimer();
    	
//    	saver = (bIsReceiverMode || bIsAutoTestMode)?
//    			new AutoTestResultSaver(iDigitalToTest, bIsAutoTestMode?0:iDistance, /*(ckbMaxVolume.isChecked()?100:(iOriginalVolume*100/iMaxVolume))*/iVolumeChoosen, ckbRandom.isChecked(), !bIsAutoTestMode):
//    			null;
    			
		saver = new AutoTestResultSaver(iDigitalToTest, bIsAutoTestMode?0:iDistance, /*(ckbMaxVolume.isChecked()?100:(iOriginalVolume*100/iMaxVolume))*/iVolumeChoosen, ckbRandom.isChecked(), !bIsAutoTestMode);
    	
    	Log.e(TAG, "runAutoTest(), config:"+AubioTestConfig.configuration());
    	
    	curCode = (0 == etSenderOffset.getText().length() && null != saver && !ckbRandom.isChecked())?saver.getLastRecord():etSenderOffset.getText().toString();
    	iOriginalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    	am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) ((iVolumeChoosen/100.f)*iMaxVolume), AudioManager.FLAG_PLAY_SOUND); 
    	
    	new Thread(){
			public void run(){	
				setTestMode(bIsSenderMode, bIsReceiverMode);
				startAutoTest(curCode, iDigitalToTest);
//				if(!bIsAutoTestMode){
//					sendPureBTMsg(BT_MSG_SET_VOLUME+iVolumeChoosen+BT_MSG_SET_VOLUME_END);
//				}
			}
    	}.start();
    	
//		new Thread(){
//			public void run(){	
//				//am.setMode(AudioManager.MODE_NORMAL);   
//				int iOriginalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
////				if(ckbMaxVolume.isChecked())
////				  am.setStreamVolume(AudioManager.STREAM_MUSIC, iMaxVolume, AudioManager.FLAG_PLAY_SOUND); 
//				
//				
//				//final Message msg = new Message();
//				final StringBuilder tmpRet = new StringBuilder();
//				
//				registerAnalysisCallback(tmpRet, saver, bIsSenderMode);
//				
//				FreqGenerator.OnPlayToneCallback cb = new FreqGenerator.OnPlayToneCallback() {
//					
//					@Override
//					public void onStopGen(String strCode) {
//						Log.i(TAG, "onStopGen(), strCode:"+strCode);
//														}
//					
//					@Override
//					public void onStartGen(String strCode) {
//						Log.i(TAG, "onStartGen(), strCode:"+strCode);
//					}
//					
//					@Override
//					public void onErrCorrectionCode(String strCode, String strEC, String strEncodeMark) {
//						Log.i(TAG, "onErrCorrectionCode(), strCode:"+strCode+", strEC:"+strEC);
//						curECCode = strEC;
//						curEncodeMark = strEncodeMark;
//					}
//					
//					@Override
//					public void onCurFreqChanged(double dFreq) {
//						Log.i(TAG, "onCurFreqChanged(), dFreq:"+dFreq);
//					}
//				};
//				
//				//FreqGenerator.getInstance().setOnPlayToneCallback(cb);
//				
//				if(/*bIsSenderMode || */bIsAutoTestMode){
//					am.setStreamVolume(AudioManager.STREAM_MUSIC, (int) ((iVolumeChoosen/100.f)*iMaxVolume), AudioManager.FLAG_PLAY_SOUND); 
//					//List<String> lstTestData = FreqGenerator.getTestList(iDigitalToTest);
//					try {
//						//wait analysis thread for one second 
//						Thread.sleep(1000L);
//					} catch (InterruptedException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//					
//					curCode = (0 == etSenderOffset.getText().length() && null != saver && !ckbRandom.isChecked())?saver.getLastRecord():etSenderOffset.getText().toString();
//					launchFreqGenerator(curCode, iDigitalToTest);				
////					//for(int i = 0; i < lstTestData.size(); i++){
////					while(null != (curCode = (ckbRandom.isChecked()?FreqGenerator.genNextRandomData(iDigitalToTest):FreqGenerator.genNextTestData(curCode, iDigitalToTest)))){						
////						if(true == mIsPause || true == mIsStop){
////							Log.e(TAG, "runAutoTest(), break loop");
////							FreqGenerator.getInstance().stopPlay2();
////							break;
////						}
////						
////						if(false == checkTimer()){
////							enterStopMode();
////							break;
////						}
////						
////						if(SELF_TEST)
////							FreqGenerator.getInstance().playCode3(/*lstTestData.get(i)*/curCode, true);
////						else
////							FreqGenerator.getInstance().playCode2(/*lstTestData.get(i)*/curCode, true);
////						
////						waitForAnalyze();
////					}
//					
//				}else{ //For Receiver mode
//					//List<String> lstTestData = FreqGenerator.getTestList(iDigitalToTest);
//
//					curCode = (0 == etSenderOffset.getText().length() && null != saver && !ckbRandom.isChecked())?saver.getLastRecord():etSenderOffset.getText().toString();
//					//for(int i = 0; i < lstTestData.size(); i++){
//					sendPureBTMsg(BT_MSG_SET_VOLUME+iVolumeChoosen+BT_MSG_SET_VOLUME_END);
//					
//					while(null != (curCode = (ckbRandom.isChecked()?FreqGenerator.genNextRandomData(iDigitalToTest):FreqGenerator.genNextTestData(curCode, iDigitalToTest)))){
//						//curCode = "0123456789ABCDEF";
//						if(false == checkTimer()){
//							enterStopMode();
//							break;
//						}
//						
//						curECCode = FreqGenerator.getECCode(curCode);
//						curEncodeMark = AubioTestConfig.encodeConsecutiveDigits(curCode+curECCode);
//						if(true == mIsPause || true == mIsStop){
//							Log.e(TAG, "runAutoTest(), break loop");
//							//FreqGenerator.getInstance().stopPlay();
//							break;
//						}
//						
//						sendBTMsg(curCode); // 990 digits max
//						
//						synchronized(objSync){
//							try {
//								tmpRet.delete(0, tmpRet.length());
//								setDecodeRet(null);
//								//curCode =lstTestData.get(i);
//								FreqAnalyzer.getInstance().beginToTrace(curCode);
//								Log.i(TAG, "runAutoTest(), receiver begin waiting");
//								objSync.wait();
//								Log.i(TAG, "runAutoTest(), exit wait in Receiver mode");
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//							}
//						}
//					}
//				}
//				
//				am.setStreamVolume(AudioManager.STREAM_MUSIC, iOriginalVolume, AudioManager.FLAG_PLAY_SOUND);
//				Log.i(TAG, "runAutoTest(), exit testing ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
//			}
//		}.start();
    }
    
    public void waitForAnalyze(){
    	//if(bIsAutoTestMode){
			synchronized(objSync){
				try {
					//tmpRet.delete(0, tmpRet.length());
					setDecodeRet(null);
					//msg.obj = curCode;// =lstTestData.get(i);
					FreqAnalyzer.getInstance().beginToTrace(curCode);
					//handler.sendMessageDelayed(msg, 3000L);
					Log.i(TAG, "runAutoTest(), begin wait");
					objSync.wait();									
					Log.i(TAG, "runAutoTest(), exit wait");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		//}
    }
    
    //Handler handler = new Handler();
    AudioTrack audioTrack;
    final int bufferSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
    
    private void startGen(){
		mbStop = false;
		if(false == ckbGenCode.isChecked())
			return;
        final Thread thread = new Thread(new Runnable() {
            public void run() {
            	
            	int iStart = 1700;//Integer.parseInt(mEtStartFrq.getText().toString());
            	int iEnd = 22500;//Integer.parseInt(mEtEndFrq.getText().toString());
            	int iInterval = 10;//Integer.parseInt(mEtIntervalFrq.getText().toString());
            	int iDelta = iEnd - iStart;
            	int iTime = Math.abs((iDelta/iInterval)+(0 == iDelta%iInterval?0:1));
            	duration = 0.15f;//Float.parseFloat(mEtCurPeriod.getText().toString());
            	numSamples = (int) (duration * sampleRate);
            	if(numSamples < bufferSize){
            		Log.i(TAG, "run(), make numSamples from "+numSamples+" to "+bufferSize );
            		numSamples = bufferSize;
            	}
            	
            	sample = new double[numSamples];
            	generatedSnd = new byte[2 * numSamples];
            	
            	audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
		                sampleRate, 
		                AudioFormat.CHANNEL_CONFIGURATION_MONO,
		                AudioFormat.ENCODING_PCM_16BIT, 
		                generatedSnd.length,
		                AudioTrack.MODE_STREAM);
            	
            	if(audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
            		audioTrack.play();
            	
            	for(int i = 0 ;i < iTime;i++){
            		final double dFreq = iStart+(i*iInterval*(iDelta >=0?1:-1));
	                genTone(dFreq);
	                playSound();
	                handler.post(new Runnable() {
	                    public void run() {
	                    	mTxtFreqSend.setText(String.format("[ %2fHz ]", dFreq));
	                    }
	                });
	                try {
						Thread.sleep((long)duration*999L);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	                if(mbStop){
	                	break;
	                }
            	}
            	stopGen();
//                genTone();
//                handler.post(new Runnable() {
//
//                    public void run() {
//                        playSound();
//                    }
//                });
            }
        });
        thread.start();
    }

    private void stopGen(){
    	handler.post(new Runnable() {
            public void run() {
            	mbStop = true;
            	enterStopMode();
            }
        });
    }
	
	void genTone(double dFreq){
        // fill out the array
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate/dFreq));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
//        int idx = 0;
//        for (final double dVal : sample) {
//            // scale to maximum amplitude
//            final short val = (short) ((dVal * 32767));
//            // in 16 bit wav PCM, first byte is the low order byte
//            generatedSnd[idx++] = (byte) (val & 0x00ff);
//            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
//
//        }
        
        int idx = 0;
        int ramp = numSamples / 25 ;                                    // Amplitude ramp as a percent of sample count
        
        for(int i = 0; i< numSamples; ++i){
        	double dVal = sample[i];// Ramp up to maximum
        	
        	double dRatio = 1.d;
        	if(i< ramp){
        		dRatio = i/ramp;// Ramp amplitude up (to avoid clicks)
        	}else if(i >= (numSamples - ramp) && i< numSamples){
        		dRatio = (numSamples-i)/ramp;// Ramp down to zero
        	}
			
        	final short val = (short) ((dVal * 32767 * dRatio));
			
			// in 16 bit wav PCM, first byte is the low order byte
			generatedSnd[idx++] = (byte) (val & 0x00ff);
			generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
    }

    void playSound(){
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
    }

	private void beginToAnalyze(){
		if(null != sAudioRecorder && sAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
			return;
		}
		mWakeLock.acquire();
    	mIsStop = false;
        bufferRecSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_REC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        bufferShortSize = FRAME_SIZE_REC;//512;//1024;//(bufferRecSize/2)/2;
        AudioBufferMgr.getInstance().setBufferSize(bufferShortSize);
        
        Log.i(TAG, "beginToAnalyze(), rec bufferSize:"+bufferRecSize+", sampleRateRec = "+SAMPLE_RATE_REC);
        
        new Thread(){
			public void run(){
				startRecord(bufferRecSize, SAMPLE_RATE_REC, FRAME_SIZE_REC, NOISE_SUPPRESS_INDEX, AGC_LEVEL, ENABLE_DEVERB, DEVERB_DECAY, DEVERB_LEVEL);
				Log.i(TAG, "startRecord---- exit");
				//final long lBufTsDleta = (bufferShortSize*1000)/SAMPLE_RATE_REC;
				
				new Thread(){
		        	public void run(){
		        		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		        		//shortsRec = new short[bufferShortSize];	

		                sAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE_REC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferRecSize);
//		                recorder.setPositionNotificationPeriod(sampleRateRec);
		                //recorder.setRecordPositionUpdateListener(positionUpdater);
		                
		                while(sAudioRecorder.getState() != sAudioRecorder.STATE_INITIALIZED){
		                	try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		                }
		                
//		                NoiseSuppressor ns= null;
//		                if(NOISE_SUPPRESS_INDEX < 0){
//			                if(android.os.Build.VERSION.SDK_INT <16){
//			                	AudioManager localAudioManager = (AudioManager)AubioTestActivity.this.getSystemService("audio");
//				                if(localAudioManager != null){
//				                	Log.i(TAG, "startRecord---- localAudioManager.getParameters(\"noise_suppression\") = "+localAudioManager.getParameters("noise_suppression"));
//				                	localAudioManager.setParameters("noise_suppression=auto");
//				                	Log.i(TAG, "startRecord---- set localAudioManager.getParameters(\"noise_suppression\") = "+localAudioManager.getParameters("noise_suppression"));
//				                }
//			                }else{
//			                	ns = NoiseSuppressor.create(recorder.getAudioSessionId());
//				                if(null != ns){
//				                	Log.i(TAG, "startRecord---- ns.getEnabled() = "+ns.getEnabled());
//					                ns.setEnabled(true);
//					                Log.i(TAG, "startRecord----  2 ns.getEnabled() = "+ns.getEnabled());
//				                }else{
//				                	Log.e(TAG, "startRecord---- cann't create NoiseSuppressor");
//				                }
//			                }
//		                }
		                
		                long lTs = 0;
		        		//boolean bBeginFFmpeg = false;
		        		sAudioRecorder.startRecording();
		        		while(false == mIsPause && false == mIsStop/* && false == bEndLoop*/){
		        			final long lTs1 = (lTs+=FRAME_TS);//System.currentTimeMillis();
		        			while(null == (shortsRec = AudioBufferMgr.getInstance().getAvailableBuf())){
		        				try {
									Thread.sleep(0);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
		        			}
		        			
		        			int samplesRead = sAudioRecorder.read(shortsRec, 0, bufferShortSize);
		        			//Log.i(TAG, "record, samplesRead:"+samplesRead);
		        			if(samplesRead != AudioRecord.ERROR_INVALID_OPERATION){
		        				AudioBufferMgr.getInstance().addToDataBuf(lTs1, shortsRec, samplesRead);
		        			}else{
		        				Log.i(TAG, "record, AudioRecord.ERROR_INVALID_OPERATION");
		        			}
		        			
		        			final long lTs2 = (lTs+=FRAME_TS);;
		        			while(null == (shortsRec = AudioBufferMgr.getInstance().getAvailableBuf())){
		        				try {
									Thread.sleep(0);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
		        			}
		        			
		        			samplesRead = sAudioRecorder.read(shortsRec, 0, bufferShortSize);
		        			//Log.i(TAG, "record, samplesRead:"+samplesRead);
		        			if(samplesRead != AudioRecord.ERROR_INVALID_OPERATION){
		        				AudioBufferMgr.getInstance().addToDataBuf(lTs2, shortsRec, samplesRead);
		        			}else{
		        				Log.i(TAG, "record, AudioRecord.ERROR_INVALID_OPERATION");
		        			}
		        		}
		        		sAudioRecorder.stop();
		        		endRecord();
		        		mWakeLock.release();
//		        		if(null != ns)
//		        			ns.release();
		        		Log.i(TAG, "record end");
		        	}
		        }.start();
		        
		        if(AUBIO_FFT)
		        	forkAnalysisThread();
		        else
		        	forkFFTAnalysisThread();
			}
		}.start();
    }
	
	private void beginToSelfAnalyze(){
		if(null != sAudioRecorder && sAudioRecorder.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING){
			return;
		}
		mWakeLock.acquire();
    	mIsStop = false;
        bufferRecSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_REC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        //bufferShortSize = (bufferRecSize/2);
        bufferShortSize = FRAME_SIZE_REC;//512;//1024;//(bufferRecSize/2)/2;
        AudioBufferMgr.getInstance().setBufferSize(bufferShortSize);
        
        Log.i(TAG, "beginToSelfAnalyze(), rec bufferSize:"+bufferRecSize);
        
        new Thread(){
			public void run(){
				startRecord(bufferRecSize, SAMPLE_RATE_REC, FRAME_SIZE_REC, NOISE_SUPPRESS_INDEX, AGC_LEVEL, ENABLE_DEVERB, DEVERB_DECAY, DEVERB_LEVEL);
				Log.i(TAG, "startRecord---- exit");
				
				new Thread(){
		        	public void run(){
		        		while(false == mIsPause && false == mIsStop/* && false == bEndLoop*/){
		        			try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		        		}
		        		endRecord();
		        		mWakeLock.release();
		        		Log.i(TAG, "record end");
		        	}
		        }.start();
		        
		        if(AUBIO_FFT)
		        	forkAnalysisThread();
		        else
		        	forkFFTAnalysisThread();
			}
		}.start();
    }
	
	private volatile boolean mbNeedToResetFFT = false; 
	short[] bufSegment = new short[FRAME_SIZE_REC];
	private void forkAnalysisThread(){
		new Thread(){
        	public void run(){
        		BufRecord buf = null;
        		while(false == mIsPause && false == mIsStop/* && false == bEndLoop*/){
        			int iSessionOffset = FreqAnalyzer.getInstance().getSessionOffset();
        			
        			//final float ret = recordAudio(shortsRec, samplesRead);
        			if(mbNeedToResetFFT)
        				Log.i(TAG, "forkAnalysisThread, mbNeedToResetFFT is true**********");
        			
        			if(iSessionOffset > 0)
        				buf = getBuf((iSessionOffset/FRAME_SIZE_REC)+1);
        			else
        				buf = getBuf();
        			
        			if(buf == null){
        				Log.e(TAG, "forkAnalysisThread, can not get buffer!!!!!!!!!!!!!!!!!!!!");
        				FreqAnalyzer.getInstance().analyze(0, 0, -1, buf.miFFTValues);
        				continue;
        			}
        			
        			short[] bufShort = buf.mbBuf;
        			
        			if(0 != iSessionOffset){
        				bufShort = AudioBufferMgr.getInstance().getBufByIndex(buf.miIndex, iSessionOffset, bufSegment);
        			}
        			final float ret = recordAudio(bufShort, buf.miSampleRead, mbNeedToResetFFT);

        			//final float ret = analyzeAudioViaAudacity(bufShort, buf.miSampleRead, mbNeedToResetFFT);
        			mbNeedToResetFFT = false;
        			//final float ret = analyzeAudioViaDywa(buf.mbBuf, buf.miSampleRead);
        			final long lTs = buf.mlTs;
        			
        			FreqAnalyzer.getInstance().analyze(lTs, ret, buf.miIndex, buf.miFFTValues);
        			
    				mTxtFreq.post(new Runnable(){
    					@Override
    					public void run() {
    						mTxtFreq.setText(ret+"Hz");
    						addData(0, lTs, ret);
    					}});
    				
    				AudioBufferMgr.getInstance().addToAvailableBuf(buf);
        		}
			}
        }.start();
	}
	
	public void updateFreq(final long lTs, final float fFreq){
		mTxtFreq.post(new Runnable(){
			@Override
			public void run() {
				mTxtFreq.setText(fFreq+"Hz");
				addData(0, lTs, fFreq);
			}});
	}
	
	//RealDoubleFFT transformer;
	private void forkFFTAnalysisThread(){
		new Thread(){
        	public void run(){
        		BufRecord buf = null;
        		//transformer = new RealDoubleFFT(FRAME_SIZE_REC);
        		//final double[] toTransform = new double[FRAME_SIZE_REC];
        		while(false == mIsPause && false == mIsStop/* && false == bEndLoop*/){
        			int iSessionOffset = FreqAnalyzer.getInstance().getSessionOffset();
        			
        			if(iSessionOffset > 0)
        				buf = getBuf((iSessionOffset/FRAME_SIZE_REC)+1);
        			else
        				buf = getBuf();
        			
        			short[] bufShort = buf.mbBuf;
        			
        			if(0 != iSessionOffset){
        				bufShort = AudioBufferMgr.getInstance().getBufByIndex(buf.miIndex, iSessionOffset, bufSegment);
        			}
        			
        			//runAudioPreprocess(bufShort, false);
        				
        			//final float ret = analyzeFreqByFFTJ(transformer, bufShort, buf.miSampleRead, toTransform);
        			//int[] iFFTValues = new int[5];
        			final float ret = analyzeAudioViaAudacity(bufShort, buf.miSampleRead, mbNeedToResetFFT, FreqAnalyzer.getInstance().getLastDetectedToneIdx(buf.mlTs), buf.miFFTValues);
        			//iMax * ((SAMPLE_RATE_REC/2.0f)/FRAME_SIZE_REC);
        			//Log.i(TAG, "forkFFTAnalysisThread(), iFFTValues=["+iFFTValues[0]+","+iFFTValues[1]+","+iFFTValues[2]+","+iFFTValues[3]+","+iFFTValues[4]+"]");
        			final long lTs = buf.mlTs;
        			
        			FreqAnalyzer.getInstance().analyze(lTs, ret, buf.miIndex, buf.miFFTValues);
        			
        			updateFreq(lTs, ret);
    				
    				AudioBufferMgr.getInstance().addToAvailableBuf(buf);
        		}
			}
        }.start();
	}
	
//	static private float analyzeFreqByFFTJ(/*RealDoubleFFT transformer, */short[] buf, int iSampleRead, double[] toTransform){
//		float fRet = 0.0f;
//		if(null == toTransform)
//			toTransform = new double[FRAME_SIZE_REC];
//		
//		for (int i = 0; i < FRAME_SIZE_REC && i < iSampleRead; i++) {
//            toTransform[i] = (double) buf[i] / 32768.0; // signed 16 bit
//        }
//		
//		transformer.ft(toTransform);
//		
//		int iMax = 0;
//		double dMax = toTransform[0];
//		for (int i = 1; i < FRAME_SIZE_REC && i < iSampleRead; i++) {
//            if(toTransform[i] > dMax){
//            	iMax = i;
//            	dMax = toTransform[i] ;
//            }
//        }
//		
//		fRet= iMax * ((SAMPLE_RATE_REC/2.0f)/FRAME_SIZE_REC);
//		
//		return fRet;
//	} 
	
	private BufRecord getBuf(){
		return getBuf(0);
	}
	
	private BufRecord getBuf(int iNumToRest){
		BufRecord buf = null;
		while( null == (buf=AudioBufferMgr.getInstance().getDataBuf(iNumToRest))){
			try {
				synchronized(FreqGenerator.sSyncObj){
					FreqGenerator.sSyncObj.notify();
				}
				synchronized(AudioBufferMgr.getInstance().getSyncObject()){
					AudioBufferMgr.getInstance().getSyncObject().wait(2000);//2 seconds
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return buf;
	}
	
	private void beginToAnalyzeDywa(){
		mWakeLock.acquire();
    	mIsStop = false;
    	
        bufferRecSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_REC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        bufferShortSize = bufferRecSize/2;
        Log.i(TAG, "beginToAnalyzeDywa(), rec bufferSize:"+bufferRecSize);
        
        new Thread(){
        	public void run(){
        		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        		shortsRec = new short[bufferShortSize];	
        		OnRecordPositionUpdateListener positionUpdater = new OnRecordPositionUpdateListener()
                {
                    @Override
                    public void onPeriodicNotification(AudioRecord recorder)
                    {
                        Date d = new Date();
                        Log.d(TAG, "periodic notification " + d.toLocaleString() + " mili " + d.getTime());
                    }

                    @Override
                    public void onMarkerReached(AudioRecord recorder)
                    {
                        Log.d(TAG, "marker reached");
                    }
                };
                sAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_REC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferRecSize);
                sAudioRecorder.setPositionNotificationPeriod(SAMPLE_RATE_REC);
                sAudioRecorder.setRecordPositionUpdateListener(positionUpdater);
                
        		//boolean bBeginFFmpeg = false;
        		sAudioRecorder.startRecording();
        		while(false == mIsPause && false == mIsStop/* && false == bEndLoop*/){
        			int samplesRead = sAudioRecorder.read(shortsRec, 0, bufferShortSize);
        			//Log.i(TAG, "record, samplesRead:"+samplesRead);
        			if(samplesRead != AudioRecord.ERROR_INVALID_OPERATION){
        				final float ret = analyzeAudioViaDywa(shortsRec, samplesRead);
        				final long lTs = System.currentTimeMillis();
        				mTxtFreq.post(new Runnable(){
							@Override
							public void run() {
								mTxtFreq.setText(ret+"Hz");
								addData(1, lTs, ret);
							}});
        			}
        		}
        		sAudioRecorder.stop();
        		endRecord();
        		mWakeLock.release();
        		Log.i(TAG, "record end");
        	}
        }.start();
        
        new Thread(){
			public void run(){
				startRecord(bufferRecSize, SAMPLE_RATE_REC, FRAME_SIZE_REC, NOISE_SUPPRESS_INDEX, AGC_LEVEL, ENABLE_DEVERB, DEVERB_DECAY, DEVERB_LEVEL);
				Log.i(TAG, "startRecord---- exit");
			}
		}.start();
    }
	
	private void beginToAnalyzeBoth(){
		mWakeLock.acquire();
    	mIsStop = false;
        bufferRecSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_REC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        bufferShortSize = bufferRecSize/2;
        Log.i(TAG, "beginToAnalyzeBoth(), rec bufferSize:"+bufferRecSize);
        
        new Thread(){
        	public void run(){
        		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        		shortsRec = new short[bufferShortSize];	
        		OnRecordPositionUpdateListener positionUpdater = new OnRecordPositionUpdateListener()
                {
                    @Override
                    public void onPeriodicNotification(AudioRecord recorder)
                    {
                        Date d = new Date();
                        Log.d(TAG, "periodic notification " + d.toLocaleString() + " mili " + d.getTime());
                    }

                    @Override
                    public void onMarkerReached(AudioRecord recorder)
                    {
                        Log.d(TAG, "marker reached");
                    }
                };
                sAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_REC, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferRecSize);
                sAudioRecorder.setPositionNotificationPeriod(SAMPLE_RATE_REC);
                sAudioRecorder.setRecordPositionUpdateListener(positionUpdater);
                
        		//boolean bBeginFFmpeg = false;
        		sAudioRecorder.startRecording();
        		while(false == mIsPause && false == mIsStop/* && false == bEndLoop*/){
        			int samplesRead = sAudioRecorder.read(shortsRec, 0, bufferShortSize);
        			//Log.i(TAG, "record, samplesRead:"+samplesRead);
        			if(samplesRead != AudioRecord.ERROR_INVALID_OPERATION){
        				final float ret = recordAudio(shortsRec, samplesRead, mbNeedToResetFFT);
        				final long lTs = System.currentTimeMillis();
        				mTxtFreq.post(new Runnable(){
							@Override
							public void run() {
								mTxtFreq.setText(ret+"Hz");
								addData(0, lTs, ret);
							}});
        				
        				final float ret2 = analyzeAudioViaDywa(shortsRec, samplesRead);
        				mTxtFreq2.post(new Runnable(){
							@Override
							public void run() {
								mTxtFreq2.setText(ret2+"Hz");
								addData(1, lTs, ret2);
							}});
        			}
        		}
        		sAudioRecorder.stop();
        		endRecord();
        		mWakeLock.release();
        		Log.i(TAG, "record end");
        	}
        }.start();
        
        new Thread(){
			public void run(){
				startRecord(bufferRecSize, SAMPLE_RATE_REC, FRAME_SIZE_REC, NOISE_SUPPRESS_INDEX, AGC_LEVEL, ENABLE_DEVERB, DEVERB_DECAY, DEVERB_LEVEL);
				Log.i(TAG, "startRecord---- exit");
			}
		}.start();
    }
	
	private void addData(int iType, long lTs, double freq){
		if(ENABLE_LV_DISPLAY && null != mFreqDataAdapter && freq > 0){
			mFreqDataList.add(0, new FreqData(iType, lTs, freq));
			mFreqDataAdapter.notifyDataSetChanged(); 
		}
	}
	
	public void resetData(){
		mTxtFreq.post(new Runnable(){
			@Override
			public void run() {
				etDecode.setText("");
				mFreqDataList.clear();
			}});
	}
	
	private void resetBuffer(){
		mbNeedToResetFFT = true;
		AudioBufferMgr.getInstance().recycleAllBuffer();
	} 
	
	static class FreqDataAdapter extends BaseAdapter{
    	protected Activity mActivity = null;
    	protected List<FreqData> mArrList = null;
    	protected LayoutInflater mInflater;
    	
    	static class DataHolder{
    		TextView txtTime;
    		TextView txtFreq;
    	}
    	
		public FreqDataAdapter(Activity mActivity, List<FreqData> mArrList) {
			super();
			this.mActivity = mActivity;
			this.mArrList = mArrList;
			if(null != mActivity){
				mInflater = (LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			}
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return mArrList.size();
		}

		@Override
		public Object getItem(int arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int iPosition, View convertView, ViewGroup parent) {
			if(null == convertView){
				convertView = mInflater.inflate(R.layout.lv_itm, null);
				if(null != convertView){
					DataHolder holder = new DataHolder();
					if(null != holder){
						holder.txtTime = (TextView)convertView.findViewById(R.id.txtTime);
						holder.txtFreq = (TextView)convertView.findViewById(R.id.txtFreq);
						convertView.setTag(holder);
					}
				}
			}
			
			if(null != convertView){
				DataHolder holder =  (DataHolder)convertView.getTag();
				if(null != holder){
					FreqData data = mArrList.get(iPosition);
					if(null != data){
						holder.txtTime.setText(String.valueOf(data.mTime));
						holder.txtFreq.setText(String.format("%2f", data.mdFreq)+" Hz");
						if(0 != data.iType){
							holder.txtFreq.setBackgroundColor(Color.GREEN);
						}else{
							holder.txtFreq.setBackgroundColor(Color.YELLOW);
						}
					}
				}
			}
			return convertView;
		}
    }

	@Override
	public void onAppendResult(String strCode) {
		if(null != etDecode){
			etDecode.append(strCode);
		}
	}
	@Override
	public void onSetResult(String strCode, String strDecodeMark, String strDecodeUnmark, boolean bFromAutoCorrection, MatchRetSet prevMatchRet) {
		if(null != etDecode){
			etDecode.setText(strCode+"\n["+strDecodeMark+"]");
		}
	}
	@Override
	public void onDetectStart() {
		resetData();
	}
	
	@Override
	public void onTimeout(FreqAnalyzer freqAnalyzer, boolean bFromAutoCorrection, MatchRetSet prevMatchRet) {
		
	}
	
	@Override
	public float onBufCheck(short[] buf, long lBufTs, boolean bResetFFT, int[] iFFTValues) {
		if(AUBIO_FFT){
			analyzeAudio(buf, buf.length, true);
			return analyzeAudio(buf, buf.length, false);
		}else{
			analyzeAudioViaAudacityAC(buf, buf.length, bResetFFT, 0, null);
			return analyzeAudioViaAudacityAC(buf, buf.length, bResetFFT, FreqAnalyzer.getInstance().getLastDetectedToneIdx(lBufTs), iFFTValues);//addtionalFFTCheck(buf, bResetFFT);
		}
	}
	
//	private RealDoubleFFT transformer2;
//	final double[] toTransform2 = new double[FRAME_SIZE_REC];
//	private float addtionalFFTCheck(short[] buf, boolean bResetFFT){
//		if(null == transformer2)
//			transformer2 = new RealDoubleFFT(FRAME_SIZE_REC);
//		
//		runAudioPreprocessAC(buf, false);
//		analyzeFreqByFFTJ(transformer2, buf, buf.length, toTransform2);
//		return analyzeFreqByFFTJ(transformer2, buf, buf.length, toTransform2);
//	}
	
	//FOr BlueTooth
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;	
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

//        // Initialize the array adapter for the conversation thread
//        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
//        mConversationView = (ListView) findViewById(R.id.in);
//        mConversationView.setAdapter(mConversationArrayAdapter);
//
//        // Initialize the compose field with a listener for the return key
//        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
//        mOutEditText.setOnEditorActionListener(mWriteListener);
//
//        // Initialize the send button with a listener that for click events
//        mSendButton = (Button) findViewById(R.id.button_send);
//        mSendButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View v) {
//                // Send a message using content of the edit text widget
//                TextView view = (TextView) findViewById(R.id.edit_text_out);
//                String message = view.getText().toString();
//                sendMessage(message);
//            }
//        });

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
    
    private void postToToast(final String msg){
    	this.handler.post(new Runnable(){
			@Override
			public void run() {
				Toast.makeText(AubioTestActivity.this, msg, Toast.LENGTH_SHORT).show();
			}});
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
        	postToToast(getString(R.string.not_connected));
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);
            
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }
    
    private final void setStatus(int resId) {
    	this.setTitle(resId);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(resId);
    }

    private final void setStatus(CharSequence subTitle) {
    	setTitle(subTitle);
        //final ActionBar actionBar = getActionBar();
        //actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName)+" by BlueTooth");
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
               // mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                Toast.makeText(AubioTestActivity.this, readMessage, Toast.LENGTH_SHORT).show();
                onBTMsgReceived(readMessage);
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE_SECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, true);
            }
            break;
        case REQUEST_CONNECT_DEVICE_INSECURE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                connectDevice(data, false);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occurred
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
        case R.id.secure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            return true;
        case R.id.insecure_connect_scan:
            // Launch the DeviceListActivity to see devices and do scan
            serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
	

}
