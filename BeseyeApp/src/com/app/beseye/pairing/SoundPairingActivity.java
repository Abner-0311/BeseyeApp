package com.app.beseye.pairing;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraViewActivity;
import com.app.beseye.PairingFailActivity;
import com.app.beseye.R;
import com.app.beseye.audio.AudioChannelMgr;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

public class SoundPairingActivity extends BeseyeBaseActivity {	
	static public final String KEY_WIFI_INFO = "KEY_WIFI_INFO";
	
	private WifiAPInfo mChosenWifiAPInfo;
	private ViewGroup mVgCamNameHolder;
	private EditText mEtCamName;
	private TextView mTxtProgress;
	private String mStrCamName;
	private PairingCounter mPairingCounter;
	private static boolean sbFinishToPlay = false;
	
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
		
		mVgCamNameHolder = (ViewGroup)findViewById(R.id.vg_name_cam);
		if(null != mVgCamNameHolder){
			mEtCamName = (EditText)mVgCamNameHolder.findViewById(R.id.editText_name_camera);
			if(null != mEtCamName){
				updateProgress(0);
				mEtCamName.setOnEditorActionListener(mOnEditorActionListener);
			}
		}
		
		mTxtProgress = (TextView)findViewById(R.id.tv_percentage_label);
		
		mChosenWifiAPInfo = getIntent().getParcelableExtra(KEY_WIFI_INFO);
		if(null == mChosenWifiAPInfo){
			Log.e(TAG, "onCreate(), mChosenWifiAPInfo is nul");
		}
		
//		new Thread(new Runnable(){
//
//			@Override
//			public void run() {
//				swTest();
//			}}).start();
	
	}
	
	@Override
	protected void onSessionComplete() {
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeAccountTask.StartCamPairingTask(this), true, SessionMgr.getInstance().getAuthToken());
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
//			case R.id.btn_playtone:{
//				//if(playCode(mEtTonePlay.getText().toString(), true)){
//				if(playPairingCode("aabbccddeeff", "BesEye0630",3,(short) 1) == 0){
//					mBtnPLayTone.setEnabled(false);
//				}
//				break;
//			}
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
				sbFinishToPlay = true;
				Log.i(TAG, "onStopGen(), strCode:["+strCode+"]");
		    	//mBtnPLayTone.setEnabled(true);
			}}, 0);
    	
//    	BeseyeUtils.postRunnable(new Runnable(){
//			@Override
//			public void run() {
//				Intent intent = new Intent();
//				intent.setClass(BeseyeApplication.getApplication(), CameraViewActivity.class);//WifiListActivity.class);, CameraSettingActivity.class
//				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				BeseyeApplication.getApplication().startActivity(intent);
//			}}, 2000);
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
    
    TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener(){
		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			Log.i(TAG, "onEditorAction(), actionId:["+actionId+"]");
			if (actionId == EditorInfo.IME_ACTION_DONE) { 
				if(view.equals(mEtCamName) && 0 < mEtCamName.getText().length()){
					mStrCamName = mEtCamName.getText().toString();
					Log.i(TAG, "onEditorAction(), mStrCamName:["+mStrCamName+"]");
					BeseyeUtils.hideSoftKeyboard(SoundPairingActivity.this, mEtCamName);
					BeseyeUtils.setVisibility(mVgCamNameHolder, View.GONE);
					checkPairingStatus();
					return true;
				}
			}			
			return false;
		}
	};
	
	private void beginToPlayPairingTone(int sUserTmpId){
		Log.i(TAG, "beginToPlayPairingTone(), sUserTmpId is "+sUserTmpId);
		if(null != mChosenWifiAPInfo){
			Log.w(TAG, "beginToPlayPairingTone(), mChosenWifiAPInfo:"+mChosenWifiAPInfo.toString());
			int iRet = playPairingCode(mChosenWifiAPInfo.BSSID.replace(":", ""), mChosenWifiAPInfo.password,NetworkMgr.translateCipherToType(mChosenWifiAPInfo.cipher),(short) sUserTmpId);
			
			if(iRet != 0)
				Toast.makeText(SoundPairingActivity.this, "ret:"+iRet, Toast.LENGTH_SHORT).show();
			else{
				sbFinishToPlay = false;
				estimatePairingTime();
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						mStrCamName = null;
						BeseyeUtils.setVisibility(mVgCamNameHolder, View.VISIBLE);	
						mEtCamName.requestFocus();
						BeseyeUtils.showSoftKeyboard(SoundPairingActivity.this, mEtCamName);
					}}, 2000);
			}
		}else{
			Log.e(TAG, "beginToPlayPairingTone(), mChosenWifiAPInfo is null");
		}			
	}
		
	private void estimatePairingTime(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				int iNumWords = mChosenWifiAPInfo.BSSID.replace(":", "").length()+
						mChosenWifiAPInfo.password.length()+
						1+//cipher
						2+//user temp id
						4;//prefix+postfix+divider
		
				mPairingCounter = new PairingCounter(iNumWords*2*100 + 15*1000, SoundPairingActivity.this);
				if(null != mPairingCounter){
					mPairingCounter.start();
				}				
			}}, 0);		
		
	}
		
	private void updateProgress(long lPercent){
		if(null != mTxtProgress){
			mTxtProgress.setText(lPercent+"%");
		}
	}
	
	private void onPairingTimeout(){
		checkPairingStatus();
	}
	
	private void checkPairingStatus(){
		//Check pairing result
		
		if(null != mStrCamName){
			if(null != mPairingCounter && mPairingCounter.isFinished() && sbFinishToPlay){
				Log.i(TAG, "checkPairingStatus(), launch first page");
				Bundle bundle = new Bundle();
				bundle.putBoolean(CameraViewActivity.KEY_PAIRING_DONE, true);
				launchDelegateActivity(CameraViewActivity.class.getName(), bundle);
				// if pairing failed
				//launchActivityByClassName(PairingFailActivity.class.getName());
			}
		}
	}
		
	static private class PairingCounter extends CountDownTimer {
       private boolean mbFinished = false;
       private long mlTotolTime = 0;
       private WeakReference<SoundPairingActivity> mAct;
       
	   public PairingCounter(long millisInFuture, SoundPairingActivity act) {
		   super(millisInFuture, 1000);
		   mlTotolTime = millisInFuture;
		   mAct = new WeakReference<SoundPairingActivity>(act);
		   mbFinished = false;
	   }
	   
	   public boolean isFinished(){
		   return mbFinished;
	   }
	   
	   @Override
	    public void onFinish() {
		   Log.i(TAG, "onFinish()");
		   mbFinished = true;
		   SoundPairingActivity act = mAct.get();
		   if(null != act){
			   act.updateProgress(100);
			   act.onPairingTimeout();
		   }
	   }
	   
	   @Override
	   public void onTick(long millisUntilFinished) {
		   long lUsedTime =  mlTotolTime - millisUntilFinished;
		   SoundPairingActivity act = mAct.get();
		   if(null != act){
			   act.updateProgress(lUsedTime*100/mlTotolTime);
		   }		   
	   } 
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,String strMsg) {	
		Log.e(TAG, "onErrorReport(), "+task.getClass().getSimpleName()+", iErrType="+iErrType);	
		if(task instanceof BeseyeAccountTask.StartCamPairingTask){
			//beginToPlayPairingTone(Integer.parseInt(SessionMgr.getInstance().getMdid()));
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_pairing_error));
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.StartCamPairingTask){
				if(0 == iRetCode){
					beginToPlayPairingTone(Integer.parseInt(SessionMgr.getInstance().getMdid()));
					//Log.i(TAG, "onPostExecute(), "+result.toString());
					//monitorAsyncTask(new BeseyeAccountTask.CamAttchTask(this), true, SessionMgr.getInstance().getMdid());
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
}

