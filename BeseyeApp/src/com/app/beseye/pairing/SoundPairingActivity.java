package com.app.beseye.pairing;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.media.AudioManager;
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
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.WifiControlBaseActivity;
import com.app.beseye.audio.AudioChannelMgr;
import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeFeatureConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;
import com.app.beseye.widget.GifMovieView;

public class SoundPairingActivity extends BeseyeBaseActivity {	
	static public final String KEY_WIFI_INFO = "KEY_WIFI_INFO";
	static public final String KEY_ORIGINAL_VCAM_CNT = "KEY_ORIGINAL_VCAM_CNT";
	static public final String KEY_ORIGINAL_VCAM_ARR = "KEY_ORIGINAL_VCAM_ARR";
	
	static public final String KEY_FAKE_PROCESS = "KEY_FAKE_PROCESS";
	static public final String KEY_CHANGE_WIFI_BEBEBE = "KEY_CHANGE_WIFI_BEBEBE";
	static public final String KEY_CHANGE_WIFI_VCAM = "KEY_CHANGE_WIFI_VCAM";
	
	static private AudioManager sAudioManager;
	static private int siOriginalVolume;
	
	private WifiAPInfo mChosenWifiAPInfo;
	private ViewGroup mVgCamNameHolder;
	private EditText mEtCamName;
	private TextView mTxtProgress;
	private String mStrCamName;
	private PairingCounter mPairingCounter;
	private static boolean sbFinishToPlay = false;
	private boolean mbGetPairingToken = false;
	
	private int miOriginalVCamCnt = -1;
	private JSONArray mArrOriginalVCam = null;
	
	private boolean mbFindNewCam = false;
	
	private boolean mbFakeProcess = false;
	
	static private String sStrCamNameCandidate = null;
	
	private String mStrChangeWiFiVCamId = null;
	
	private boolean mbGetPairingresult = false;
	
	private static int siPairingFailedTimes = 0;
	
	private BeseyeAccountTask.GetPairingStatusHttpTask mGetPairingStatusHttpTask;
	
	//For Soundpairing feature
	private native static boolean nativeClassInit();
	private native boolean playCode(String strCode, boolean bNeedEncode);
	private native int playPairingCode(String strMac, String strKey, int iSecType, short sUserToken);
	private native int playPairingCodeWithPurpose(String strMac, String strKey, int iSecType, short sUserToken, char cPurpose);
	private native int playSSIDPairingCodeWithPurpose(String strSSID, String strKey, int iSecType, short sUserToken, char cPurpose);
	private native int playSSIDPairingCodeWithPurposeAndRegion(String strSSID, String strKey, int iSecType, short sUserToken, char cPurpose, int cRegId, boolean bIsChinaReg);
//	private native int playSSIDHashPairingCodeWithPurpose(String strSSID, String strKey, int iSecType, short sUserToken, char cPurpose);
//	private native int playSSIDHashPairingCodeWithPurposeAndRegion(String strSSID, String strKey, int iSecType, short sUserToken, char cPurpose, int cRegId);
	private native String getSSIDHashValue(String strSSID);
	
	private native long getSoundPairingDuration(String strSSID, String strKey, boolean bSSIDHash);
	
	private native void finishPlayCode();
	private native void swTest();
	
    public static boolean isSPDebugMode() {
        return BeseyeConfig.DEBUG;
    }
	
    static {
    	//System.loadLibrary("websockets");
    	System.loadLibrary("soundpairing");
    	if (!nativeClassInit())
			throw new RuntimeException("Native Init Failed");
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		
		//miOriginalVCamCnt = getIntent().getIntExtra(KEY_ORIGINAL_VCAM_CNT, -1);
		try {
			String strVCamList = getIntent().getStringExtra(KEY_ORIGINAL_VCAM_ARR);
			mArrOriginalVCam = new JSONArray(null!=strVCamList?strVCamList:"");
			miOriginalVCamCnt = mArrOriginalVCam.length();
		} catch (JSONException e) {
			Log.e(TAG, "onCreate(), fail to create mArrOriginalVCam");
			e.printStackTrace();
			miOriginalVCamCnt = 0;
		}
		if(DEBUG)
			Log.e(TAG, "onCreate(), miOriginalVCamCnt is "+miOriginalVCamCnt);
		
		mbFakeProcess = getIntent().getBooleanExtra(KEY_FAKE_PROCESS, false);
		
		mStrChangeWiFiVCamId = getIntent().getStringExtra(KEY_CHANGE_WIFI_VCAM);
		
		if(DEBUG)
			Log.e(TAG, "onCreate(), KEY_CHANGE_WIFI_BEBEBE is "+getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false)+", mStrChangeWiFiVCamId:"+mStrChangeWiFiVCamId+", mbFakeProcess:"+mbFakeProcess);
		
		if(null == sAudioManager){
			sAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
			setVolumeControlStream(AudioManager.STREAM_MUSIC);  
		}
		
		getSupportActionBar().hide();
		
		mVgCamNameHolder = (ViewGroup)findViewById(R.id.vg_name_cam);
		if(null != mVgCamNameHolder){
			mEtCamName = (EditText)mVgCamNameHolder.findViewById(R.id.editText_name_camera);
			if(null != mEtCamName){
				updateProgress(0);
				mEtCamName.setOnEditorActionListener(mOnEditorActionListener);
			}
		}
		
		GifMovieView gifTop = (GifMovieView)findViewById(R.id.iv_paring_cam_sonar);
		if(null != gifTop){
			gifTop.setMovieResource(R.drawable.signup_voice_sonar_top);
		}
		
		GifMovieView gifBottom = (GifMovieView)findViewById(R.id.iv_paring_phone_sonar);
		if(null != gifBottom){
			gifBottom.setMovieResource(R.drawable.signup_voice_sonar_bottom);
		}
		
		mTxtProgress = (TextView)findViewById(R.id.tv_percentage_label);
		
		mChosenWifiAPInfo = getIntent().getParcelableExtra(KEY_WIFI_INFO);
		if(null == mChosenWifiAPInfo){
			Log.e(TAG, "onCreate(), mChosenWifiAPInfo is nul");
		}	
	}
	
	private boolean isSSIDPiaringCase(){
		return true;
		//return null == mChosenWifiAPInfo.BSSID || 0 == mChosenWifiAPInfo.BSSID.length() || mChosenWifiAPInfo.bIsHiddenSSID;// || !BeseyeUtils.isPureAscii(mChosenWifiAPInfo.SSID);
	}
	
	@Override
	protected void onSessionComplete() {
		super.onSessionComplete();
		if(isSSIDPiaringCase()){
			if(false == mbGetPairingToken){
				monitorAsyncTask(new BeseyeAccountTask.StartCamPairingTask(this), true, (getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false))?BeseyeJSONUtil.ACC_PAIRING_TYPE_VALIDATE+"":BeseyeJSONUtil.ACC_PAIRING_TYPE_ATTACH+"", mChosenWifiAPInfo.SSID);
			}
		}else{
			String strSSIDHash = getSSIDHashValue(mChosenWifiAPInfo.SSID);
			if(DEBUG)
				Log.w(TAG, "onSessionComplete(),strSSIDHash:"+strSSIDHash);
			monitorAsyncTask(new BeseyeAccountTask.StartCamPairingTask(this), true, (getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false))?BeseyeJSONUtil.ACC_PAIRING_TYPE_VALIDATE+"":BeseyeJSONUtil.ACC_PAIRING_TYPE_ATTACH+"", strSSIDHash);
		}
	}
	
	@Override
	protected void onPause() {
		finishPlayCode();
		if(null != sAudioManager){
			sAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, siOriginalVolume, AudioManager.FLAG_PLAY_SOUND); 
		}
		
		if(false == sbFinishToPlay){
			//siPairingFailedTimes++;
			onPairingFailed(null, null);
		}
		
		super.onPause();
		if(DEBUG)
			Log.w(TAG, "onPause()-");
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_paring;
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
				sbFinishToPlay = false;
				if(DEBUG)
					Log.i(TAG, "onStartGen(), strCode:["+strCode+"]");
		    	//mBtnPLayTone.setEnabled(false);
			}}, 0);
	}

    public static void onStopGen(final String strCode){
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				sbFinishToPlay = true;
				if(DEBUG)
					Log.i(TAG, "onStopGen(), strCode:["+strCode+"]");
		    	//mBtnPLayTone.setEnabled(true);
			}}, 0);
	}

    public static void onCurFreqChanged(final double dFreq){
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(DEBUG)
					Log.i(TAG, "onCurFreqChanged(), dFreq:["+dFreq+"]");
			}}, 0);
    	
	}

    public static void onErrCorrectionCode(String strCode, String strEC, String strEncodeMark){
    	if(DEBUG)
    		Log.i(TAG, "onErrCorrectionCode(), strCode:["+strCode+"]\n, strEC:["+strEC+"]\n, strEncodeMark:["+strEncodeMark+"]\n");
	}
    
    TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener(){
		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			//Log.i(TAG, "onEditorAction(), actionId:["+actionId+"]");
			if (view.equals(mEtCamName) && actionId == EditorInfo.IME_ACTION_DONE) { 
				if(0 < mEtCamName.getText().length()){
					sStrCamNameCandidate = mStrCamName = mEtCamName.getText().toString();
					//Log.i(TAG, "onEditorAction(), mStrCamName:["+mStrCamName+"]");
					BeseyeUtils.hideSoftKeyboard(SoundPairingActivity.this, mEtCamName);
					BeseyeUtils.setVisibility(mVgCamNameHolder, View.GONE);
					checkPairingStatus();
				}else 
					Toast.makeText(SoundPairingActivity.this, R.string.toast_pairing_enter_cam_name, Toast.LENGTH_SHORT).show();
				return true;
			}			
			return false;
		}
	};
	
	private void beginToPlayPairingTone(final int sUserTmpId, final char cPurpose){
		if(DEBUG)
			Log.i(TAG, "beginToPlayPairingTone(), sUserTmpId is "+sUserTmpId);
		if(null != mChosenWifiAPInfo){
			if(DEBUG)
				Log.w(TAG, "beginToPlayPairingTone(), mChosenWifiAPInfo:"+mChosenWifiAPInfo.toString());
			siOriginalVolume = sAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			sAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(sAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), AudioManager.FLAG_PLAY_SOUND); 
			
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					int iRet = 0;
					//if(isSSIDPiaringCase()){
						iRet = playSSIDPairingCodeWithPurposeAndRegion(mChosenWifiAPInfo.SSID, mChosenWifiAPInfo.password,mChosenWifiAPInfo.iCipherIdx,(short) sUserTmpId, cPurpose, SessionMgr.getInstance().getVPCNumber(), SessionMgr.getInstance().getServerMode().equals(SessionMgr.SERVER_MODE.MODE_CHINA_STAGE));
					//}else{
					//	iRet = playSSIDHashPairingCodeWithPurposeAndRegion(mChosenWifiAPInfo.SSID, mChosenWifiAPInfo.password,mChosenWifiAPInfo.iCipherIdx,(short) sUserTmpId, cPurpose,SessionMgr.getInstance().getVPCNumber());
					//}
					
					if(iRet != 0){
						Toast.makeText(SoundPairingActivity.this, "Failed to play pairing code, errCode:"+iRet, Toast.LENGTH_SHORT).show();
						onPairingFailed(null, null);
					}else{
						sbFinishToPlay = false;
						estimatePairingTime();
					}
				}}, 500);			
		}else{
			Log.e(TAG, "beginToPlayPairingTone(), mChosenWifiAPInfo is null");
		}			
	}
		
	private void estimatePairingTime(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				long lTimeToWait = getSoundPairingDuration(mChosenWifiAPInfo.SSID, mChosenWifiAPInfo.password, !isSSIDPiaringCase());
				mPairingCounter = new PairingCounter(lTimeToWait, SoundPairingActivity.this);
				if(null != mPairingCounter){
					mPairingCounter.start();
					extendWSConnection(lTimeToWait);
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
		if(null != sAudioManager){
			sAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, siOriginalVolume, AudioManager.FLAG_PLAY_SOUND); 
		}
		
		if(null != mPairingCounter && mPairingCounter.isFinished() && sbFinishToPlay){
			//sStrCamNameCandidate = mStrCamName = mEtCamName.getText().toString();
			if(!BeseyeFeatureConfig.PTOKEN_QUERY_TEST){
				monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
			}else{
				if(false == mbGetPairingresult){
					launchGetPairingStatusHttpTask(true);
				}
			}
		}
	}
		
	static private class PairingCounter extends CountDownTimer {
       private boolean mbFinished = false;
       private long mlTotolTime = 0;
       private WeakReference<SoundPairingActivity> mAct;
       private boolean mbPrecheckStart = false;
       
	   public PairingCounter(long millisInFuture, SoundPairingActivity act) {
		   super(millisInFuture, 1000);
		   mlTotolTime = millisInFuture;
		   mAct = new WeakReference<SoundPairingActivity>(act);
		   mbFinished = false;
		   mbPrecheckStart = false;
	   }
	   
	   public boolean isFinished(){
		   return mbFinished;
	   }
	   
	   @Override
	    public void onFinish() {
		   if(DEBUG)
			   Log.d(TAG, "onFinish()");
		   
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
			   long lPercent = lUsedTime*100/mlTotolTime;
			   act.updateProgress(lPercent);
			   if(act.mbFakeProcess && 60 < lPercent){
				   cancel();
				   mbFinished = true;
				   act.onPairingTimeout();
			   }
			   
			   if(!mbPrecheckStart && 50 < lPercent){
				   act.launchGetPairingStatusHttpTask(false);
				   mbPrecheckStart = true;
			   }
		   }		   
	   } 
	}
	
	private void launchGetPairingStatusHttpTask(boolean bShowDialog){
		if(isPairingTokenValid()){
			if(null != mGetPairingStatusHttpTask){
				mGetPairingStatusHttpTask.cancel(true);
			}
			monitorAsyncTask((mGetPairingStatusHttpTask = new BeseyeAccountTask.GetPairingStatusHttpTask(this)).setDialogId(bShowDialog?DIALOG_ID_LOADING:-1), true, ((getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false))?BeseyeJSONUtil.ACC_PAIRING_TYPE_VALIDATE:BeseyeJSONUtil.ACC_PAIRING_TYPE_ATTACH)+"", SessionMgr.getInstance().getPairToken());
		}else{
			Log.e(TAG, "launchGetPairingStatusHttpTask(), invalid PairToken");
		}
	}
	
	private int miFailTry = 0;
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,String strMsg) {	
		Log.e(TAG, "onErrorReport(), "+task.getClass().getSimpleName()+", iErrType="+iErrType);	
		if(task instanceof BeseyeAccountTask.StartCamPairingTask){
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), BeseyeUtils.appendErrorCode(this, R.string.msg_pairing_error, iErrType));
		}else if(task instanceof BeseyeAccountTask.GetCamInfoTask){
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), BeseyeUtils.appendErrorCode(this, R.string.msg_pairing_error, iErrType));
		}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
			if(3 > miFailTry++){
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(SoundPairingActivity.this), true);
					}}, 500);
			}else{
				// if pairing failed
				onPairingFailed(null, null);
			}
		}/*else if(task instanceof BeseyeAccountTask.GetPairingStatusHttpTask){
			if(3 > miFailTry++){
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						if(isPairingTokenValid()){
							monitorAsyncTask(new BeseyeAccountTask.GetPairingStatusHttpTask(SoundPairingActivity.this), true, ((getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false))?BeseyeJSONUtil.ACC_PAIRING_TYPE_VALIDATE:BeseyeJSONUtil.ACC_PAIRING_TYPE_ATTACH)+"", SessionMgr.getInstance().getPairToken());
						}else{
							Log.e(TAG, "onErrorReport(), invalid PairToken");
						}
					}}, 500);
			}else{
				// if pairing failed
				onPairingFailed();
			}
		}*/else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
	
	private void startPairingNamePage(JSONObject objVcam){
		SessionMgr.getInstance().setIsCertificated(true);
		Bundle b = new Bundle();
		b.putString(CameraListActivity.KEY_VCAM_OBJ, objVcam.toString());
		launchDelegateActivity(SoundPairingNamingActivity.class.getName(), b);
		WifiControlBaseActivity.updateWiFiPasswordHistory("");
		siPairingFailedTimes = 0;
		SessionMgr.getInstance().setPairToken("");
		
		if(null != mPairingCounter){
			mPairingCounter.cancel();
			mPairingCounter = null;
		}
		
		mbGetPairingresult = true;
	}
	
	private boolean isPairingTokenValid(){
		return null != SessionMgr.getInstance().getPairToken() && 0 < SessionMgr.getInstance().getPairToken().length();
	}
	
	private boolean isSamePairingToken(String strToken){
		return null != SessionMgr.getInstance().getPairToken() && SessionMgr.getInstance().getPairToken().equals(strToken);
	}
	
//	String vcam_id;
//	String dev_token;
	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(DEBUG)
			Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.StartCamPairingTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					mbGetPairingToken = true;
					String strPairToken = BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.ACC_PAIRING_TOKEN);
					SessionMgr.getInstance().setPairToken(strPairToken);
					beginToPlayPairingTone(Integer.parseInt(strPairToken, 16), (char) ((getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false))?BeseyeJSONUtil.ACC_PAIRING_TYPE_VALIDATE:BeseyeJSONUtil.ACC_PAIRING_TYPE_ATTACH));
					updateProgress(0);
					mbGetPairingresult = false;
				}
			}else if(task instanceof BeseyeAccountTask.GetPairingStatusHttpTask){
				
				if(DEBUG)
					Log.i(TAG, "onPostExecute(), "+((null != result)?result.toString():""));
				
				boolean bPairingFailed = true;
				boolean bNeedRequery = false;
				JSONObject objVcamConflict = null;
				String strAttachedHWID = null;
				
				if(0 == iRetCode){
					boolean bKeepPolling = BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.ACC_PAIRING_KEEP_POLLING, false);//Handle for ver 3(2015/09/10)
					if(false == bKeepPolling){
						JSONObject objVcam = BeseyeJSONUtil.getJSONObject(result.get(0), BeseyeJSONUtil.ACC_VCAM);//Handle for ver 1
						if(null != objVcam){
							if(isPairingTokenValid()){
								startPairingNamePage(objVcam);
								bPairingFailed = false;
							}
						}else{
							int iCntPairStatus = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_PAIRING_STATUS_CNT);
							JSONArray arrPairStatus = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_PAIRING_STATUS);//Handle for ver 2
							if(null != arrPairStatus){
								for(int idx = 0; idx < iCntPairStatus; idx++){
									JSONObject objPairStatus = arrPairStatus.optJSONObject(idx);
									if(null != objPairStatus && 0 == BeseyeJSONUtil.getJSONInt(objPairStatus, BeseyeJSONUtil.RET_CODE, -1)){
										JSONObject objVcamInStatus = BeseyeJSONUtil.getJSONObject(objPairStatus, BeseyeJSONUtil.ACC_VCAM);
										if(null != objVcamInStatus){
											if(isPairingTokenValid()){
												startPairingNamePage(objVcamInStatus);
												bPairingFailed = false;
												break;
											}
										}
									}
								}
							}
						}
					}else{
						if(DEBUG){
							Log.e(TAG, "onPostExecute(), find ACC_PAIRING_KEEP_POLLING");
						}
						if(null != mPairingCounter && mPairingCounter.isFinished() && sbFinishToPlay){
							bNeedRequery = false;
						}else{
							bNeedRequery = true;
						}
					}
				}else if(BeseyeError.E_BE_ACC_VCAM_CAM_HW_UID_ALREADY_USED_BY_OTHER_VCAM == iRetCode){//Handle for ver 2
					Log.e(TAG, "onPostExecute(), E_BE_ACC_VCAM_CAM_HW_UID_ALREADY_USED_BY_OTHER_VCAM");
					strAttachedHWID = "";
					JSONArray objPairStatusArr = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_PAIRING_STATUS);
					if(null != objPairStatusArr && 0 < objPairStatusArr.length()){
						JSONObject objPairStatus = objPairStatusArr.optJSONObject(0);
						if(null != objPairStatus){
							objVcamConflict = BeseyeJSONUtil.getJSONObject(objPairStatus, BeseyeJSONUtil.ACC_PAIRING_CONFLICT_VCAM);
							if(null == objVcamConflict){
								//Log.e(TAG, "onPostExecute(), null objVcamConflict");
								strAttachedHWID = BeseyeJSONUtil.getJSONString(objPairStatus, BeseyeJSONUtil.ACC_PAIRING_CAM_HWID);
							}
						}else{
							Log.e(TAG, "onPostExecute(), null objPairStatus");
						}
					}else{
						Log.e(TAG, "onPostExecute(), null objPairStatusArr");
					}
				}else if(BeseyeError.E_BE_ACC_PS_KEEP_POLLING == iRetCode || BeseyeError.E_BE_ACC_PS_VCAM_WAIT_FOR_ATTACH_CONFIRM == iRetCode ){//Handle for ver 2
					//if(3 > ++miFailTry){
					if(null != mPairingCounter && mPairingCounter.isFinished() && sbFinishToPlay){
						bNeedRequery = false;
					}else{
						bNeedRequery = true;
					}
					//}
				}
				
				
				if(bPairingFailed){
					if(bNeedRequery){
						BeseyeUtils.postRunnable(new Runnable(){
							@Override
							public void run() {
								if(mActivityResume){
									launchGetPairingStatusHttpTask(false);
								}
							}}, 3000L);
					}else{
						// if pairing failed
						siPairingFailedTimes++;
						onPairingFailed(objVcamConflict, strAttachedHWID);
					}
				}
				
			}else if(task instanceof BeseyeAccountTask.SetCamAttrTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					//monitorAsyncTask(new BeseyeAccountTask.SetCamAttrTask(this), true, vcam_id, "My Test Cam");
				}
			}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					if(false == mbFindNewCam){
						//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
						int iNetVcamCnt = 0;
						JSONArray arrCamList = new JSONArray();
						int iVcamCnt = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_VCAM_CNT);
						//miOriginalVcamCnt = iVcamCnt;
						if(0 < iVcamCnt){
							JSONArray VcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_VCAM_LST);
							for(int i = 0;i< iVcamCnt;i++){
								try {
									JSONObject camObj = VcamList.getJSONObject(i);
									if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
										arrCamList.put(camObj);
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							iNetVcamCnt = arrCamList.length();
						}
						
						if(DEBUG)
							Log.i(TAG, "miOriginalVCamCnt:"+miOriginalVCamCnt+", iNetVcamCnt = "+iNetVcamCnt);
						
						if(miOriginalVCamCnt < iNetVcamCnt || (mbFakeProcess && 0 < iNetVcamCnt)){
							//JSONArray VcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_VCAM_LST);
							if(null != arrCamList){
								JSONObject cam_obj = null;
								try {
									if(mbFakeProcess){
										cam_obj = arrCamList.getJSONObject(iNetVcamCnt-1);
										if(DEBUG)
											Log.i(TAG, "mbFakeProcess, find new cam = "+cam_obj);
										mbFindNewCam = true;
									}else{
										for(int idx = iNetVcamCnt - 1; idx >= 0; idx--){
											cam_obj = arrCamList.getJSONObject(idx);
											if(null != cam_obj){
												String strVcamId = BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID, "");
												boolean bFound = false;
												for(int idxOld = miOriginalVCamCnt - 1; idxOld >= 0; idxOld--){
													if(null != strVcamId && 0 < strVcamId.length() && strVcamId.equals(BeseyeJSONUtil.getJSONString(mArrOriginalVCam.getJSONObject(idxOld), BeseyeJSONUtil.ACC_ID, ""))){
														bFound = true;
														break;
													}
												}
												
												if(false == bFound){
													if(DEBUG)
														Log.i(TAG, "find new cam = "+cam_obj);
													mbFindNewCam = true;
													break;
												}
											}
										}
									}
									
									if(null != cam_obj){
										//workaround
										startPairingNamePage(cam_obj);
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							sStrCamNameCandidate = null;
						}else{
							// if pairing failed
							siPairingFailedTimes++;
							onPairingFailed(null, null);
						}
					}
				}
			}else{
				if(DEBUG)
					Log.i(TAG, "onPostExecute(), "+result.toString());
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	private void onPairingFailed(JSONObject objAttachedCam, String strAttachedCamHWID){
		WifiControlBaseActivity.updateWiFiPasswordHistory(null != mChosenWifiAPInfo?mChosenWifiAPInfo.password:"");
		
		if(null == objAttachedCam && null == strAttachedCamHWID){
			if(2 == siPairingFailedTimes){
				siPairingFailedTimes = 0;
				launchActivityByClassName(PairingGuidelineActivity.class.getName(), getIntent().getExtras());
			}else{
				launchActivityByClassName(PairingFailActivity.class.getName(), getIntent().getExtras());
			}
		}else{
			Bundle bundle = getIntent().getExtras();
			if(null != objAttachedCam){
				bundle.putString(PairingFailAttachAlreadyActivity.KEY_ATTACHED_CAM, objAttachedCam.toString());
			}else{
				bundle.putString(PairingFailAttachAlreadyActivity.KEY_ATTACHED_CAM_HW_ID, strAttachedCamHWID);
			}
			
			launchActivityByClassName(PairingFailAttachAlreadyActivity.class.getName(), bundle);
		}
		
		if(null != mPairingCounter){
			mPairingCounter.cancel();
			mPairingCounter = null;
		}
		
		mbGetPairingresult = true;
	}
	
	@Override
	protected boolean onCameraActivated(JSONObject msgObj){
		//Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(),  msgObj = "+msgObj);
    	if(null != msgObj){
    		if(DEBUG)
    			Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(),  msgObj = "+msgObj);
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strPairToken = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_PAIR_TOKEN);
    			if(null != strPairToken && strPairToken.equals(SessionMgr.getInstance().getPairToken())){
    				if(DEBUG)
    					Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(), find match strPairToken = "+strPairToken);
    				if(false == mbFindNewCam){
    					if(!BeseyeFeatureConfig.PTOKEN_QUERY_TEST){
    						String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    						monitorAsyncTask(mGetNewCamTask = new BeseyeAccountTask.GetCamInfoTask(this), false, strCamUID);
    					}else{
    						if(false == mbGetPairingresult){
    							launchGetPairingStatusHttpTask(true);
    						}
    					}

	    				mbFindNewCam = true;
	    				WifiControlBaseActivity.updateWiFiPasswordHistory("");
	    				siPairingFailedTimes = 0;
    				}
    				//SessionMgr.getInstance().setPairToken("");
    				if(null != mPairingCounter){
    					mPairingCounter.cancel();
    					mPairingCounter = null;
    				}
    			}
    			return true;
    		}
		}
    	return super.onCameraActivated(msgObj);
    }
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(null != mPairingCounter && !mPairingCounter.isFinished()){
				if(null != mVgCamNameHolder && View.VISIBLE == mVgCamNameHolder.getVisibility()){
					Toast.makeText(this, R.string.toast_pairing_enter_cam_name, Toast.LENGTH_SHORT).show();
					BeseyeUtils.showSoftKeyboard(SoundPairingActivity.this, mEtCamName);
				}else{
					Toast.makeText(this, R.string.toast_pairing_wait, Toast.LENGTH_SHORT).show();
				} 
				return true;
			}
			return super.onKeyUp(keyCode, event);
		}else
			return super.onKeyUp(keyCode, event);
	}
	
	protected boolean onCameraOnline(JSONObject msgObj){
		if(DEBUG)
			Log.i(TAG, getClass().getSimpleName()+"::onCameraOnline(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != mStrChangeWiFiVCamId && mStrChangeWiFiVCamId.equals(strCamUID)){
    				if(DEBUG)
    					Log.i(TAG, getClass().getSimpleName()+"::onCameraOnline(),  change wifi ok, mStrChangeWiFiVCamId = "+mStrChangeWiFiVCamId);
					Toast.makeText(getApplicationContext(), String.format(getString(R.string.toast_cam_change_wifi_done), BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_NAME)), Toast.LENGTH_SHORT).show();
    				launchDelegateActivity(CameraListActivity.class.getName());
    				WifiControlBaseActivity.updateWiFiPasswordHistory("");
    			}
    			return true;
    		}
		}
    	return false;
    }
}

