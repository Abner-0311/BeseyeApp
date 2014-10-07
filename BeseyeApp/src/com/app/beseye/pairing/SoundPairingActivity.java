package com.app.beseye.pairing;

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
import com.app.beseye.CameraViewActivity;
import com.app.beseye.PairingFailActivity;
import com.app.beseye.R;
import com.app.beseye.WifiControlBaseActivity;
import com.app.beseye.BeseyeBaseActivity.OnResumeUpdateCamInfoRunnable;
import com.app.beseye.audio.AudioChannelMgr;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.setting.CameraSettingActivity;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;
import com.app.beseye.widget.GifMovieView;

public class SoundPairingActivity extends BeseyeBaseActivity {	
	static public final String KEY_WIFI_INFO = "KEY_WIFI_INFO";
	static public final String KEY_ORIGINAL_VCAM_CNT = "KEY_ORIGINAL_VCAM_CNT";
	static public final String KEY_ORIGINAL_VCAM_ARR = "KEY_ORIGINAL_VCAM_ARR";
	
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
	
	private int miOriginalVCamCnt = -1;
	private JSONArray mArrOriginalVCam = null;
	
	private boolean mbFindNewCam = false;
	
	static private String sStrCamNameCandidate = null;
	
	private String mStrChangeWiFiVCamId = null;
	
	//For Soundpairing feature
	private native static boolean nativeClassInit();
	private native boolean playCode(String strCode, boolean bNeedEncode);
	private native int playPairingCode(String strMac, String strKey, int iSecType, short sUserToken);
	private native int playPairingCodeWithPurpose(String strMac, String strKey, int iSecType, short sUserToken, char cPurpose);
	
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
		Log.e(TAG, "onCreate(), miOriginalVCamCnt is "+miOriginalVCamCnt);
		
		mStrChangeWiFiVCamId = getIntent().getStringExtra(KEY_CHANGE_WIFI_VCAM);
		
		Log.e(TAG, "onCreate(), KEY_CHANGE_WIFI_BEBEBE is "+getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false)+", mStrChangeWiFiVCamId:"+mStrChangeWiFiVCamId);
		
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
		
		//monitorAsyncTask(new BeseyeAccountTask.CamDeattchTask(this), true);
		monitorAsyncTask(new BeseyeAccountTask.StartCamPairingTask(this), true, (getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false))?BeseyeJSONUtil.ACC_PAIRING_TYPE_VALIDATE+"":BeseyeJSONUtil.ACC_PAIRING_TYPE_ATTACH+"", mChosenWifiAPInfo.BSSID);
	}
	@Override
	protected void onPause() {
		finishPlayCode();
		if(null != sAudioManager){
			sAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, siOriginalVolume, AudioManager.FLAG_PLAY_SOUND); 
		}
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
				sbFinishToPlay = false;
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
			//Log.i(TAG, "onEditorAction(), actionId:["+actionId+"]");
			if (view.equals(mEtCamName) && actionId == EditorInfo.IME_ACTION_DONE) { 
				if(0 < mEtCamName.getText().length()){
					sStrCamNameCandidate = mStrCamName = mEtCamName.getText().toString();
					Log.i(TAG, "onEditorAction(), mStrCamName:["+mStrCamName+"]");
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
		Log.i(TAG, "beginToPlayPairingTone(), sUserTmpId is "+sUserTmpId);
		if(null != mChosenWifiAPInfo){
			Log.w(TAG, "beginToPlayPairingTone(), mChosenWifiAPInfo:"+mChosenWifiAPInfo.toString());
			siOriginalVolume = sAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			sAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int)(sAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), AudioManager.FLAG_PLAY_SOUND); 
			
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					int iRet = playPairingCodeWithPurpose(mChosenWifiAPInfo.BSSID.replace(":", ""), mChosenWifiAPInfo.password,mChosenWifiAPInfo.iCipherIdx,(short) sUserTmpId, cPurpose);
					if(iRet != 0)
						Toast.makeText(SoundPairingActivity.this, "ret:"+iRet, Toast.LENGTH_SHORT).show();
					else{
						sbFinishToPlay = false;
						estimatePairingTime();
						/*if(false == BeseyeConfig.COMPUTEX_DEMO)*/{
//							BeseyeUtils.postRunnable(new Runnable(){
//								@Override
//								public void run() {
//									mStrCamName = null;
//									if(null != sStrCamNameCandidate){
//										mEtCamName.setText(sStrCamNameCandidate);
//										mStrCamName = sStrCamNameCandidate;
//										sStrCamNameCandidate = null;
//									}
//									BeseyeUtils.setVisibility(mVgCamNameHolder, View.VISIBLE);	
//									mEtCamName.requestFocus();
//									BeseyeUtils.showSoftKeyboard(SoundPairingActivity.this, mEtCamName);
//								}}, 2000);
						}
					}
				}}, 500);			
		}else{
			Log.e(TAG, "beginToPlayPairingTone(), mChosenWifiAPInfo is null");
		}			
	}
	
	static private final long MIN_TIME_TO_WAIT_ATTACH = 60000; //1 min 
		
	private void estimatePairingTime(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				int iNumWords = mChosenWifiAPInfo.BSSID.replace(":", "").length()+
						mChosenWifiAPInfo.password.length()*2+
						4+//user temp id
						2+//purpose
						6;//prefix+postfix+divider
		
				long lTimeToWait = iNumWords*6*100 + 24*1000;
				if(MIN_TIME_TO_WAIT_ATTACH > lTimeToWait){
					lTimeToWait = MIN_TIME_TO_WAIT_ATTACH;
				}
				
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
		//launchActivityByClassName(PairingFailActivity.class.getName());
	}
	
	private void checkPairingStatus(){
		//Check pairing result
		if(null != sAudioManager){
			sAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, siOriginalVolume, AudioManager.FLAG_PLAY_SOUND); 
		}
		
		//if(null != mStrCamName){
		if(null != mPairingCounter && mPairingCounter.isFinished() && sbFinishToPlay){
			//sStrCamNameCandidate = mStrCamName = mEtCamName.getText().toString();
			monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
		}
		//}
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
	
	private int miFailTry = 0;
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,String strMsg) {	
		Log.e(TAG, "onErrorReport(), "+task.getClass().getSimpleName()+", iErrType="+iErrType);	
		if(task instanceof BeseyeAccountTask.StartCamPairingTask){
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_pairing_error));
		}else if(task instanceof BeseyeAccountTask.GetCamInfoTask){
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_pairing_error));
		}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
			if(3 > miFailTry++){
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(SoundPairingActivity.this), true);
					}}, 500);
			}else{
				//showErrorDialog(iMsgId, true);
				// if pairing failed
				WifiControlBaseActivity.updateWiFiPasswordHistory(null != mChosenWifiAPInfo?mChosenWifiAPInfo.password:"");
				launchActivityByClassName(PairingFailActivity.class.getName(), getIntent().getExtras());
			}
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
	
	String vcam_id;
	String dev_token;
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.StartCamPairingTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					String strPairToken = BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.ACC_PAIRING_TOKEN);
					SessionMgr.getInstance().setPairToken(strPairToken);
					beginToPlayPairingTone(Integer.parseInt(strPairToken, 16), (char) ((getIntent().getBooleanExtra(KEY_CHANGE_WIFI_BEBEBE, false))?BeseyeJSONUtil.ACC_PAIRING_TYPE_VALIDATE:BeseyeJSONUtil.ACC_PAIRING_TYPE_ATTACH));
					updateProgress(0);
					//monitorAsyncTask(new BeseyeAccountTask.CamBeeValidateTask(this), true, BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.ACC_PAIRING_TOKEN), mChosenWifiAPInfo.BSSID, "1a1b1005c6574d0aa406da3c981bef5f", "82e13cea4c75f7dd7556a99d96f533a09418feaec6bfeb6be02bc22c14b3207469c3104ca51aa261479a60bacfe2b084b94190f2a19c55bf01709a0257826926");
					//Log.i(TAG, "onPostExecute(), "+result.toString());
					//monitorAsyncTask(new BeseyeAccountTask.CamAttchTask(this), true, BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.ACC_PAIRING_TOKEN), mChosenWifiAPInfo.BSSID);
				}
			}/*else if(task instanceof BeseyeAccountTask.CamAttchTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					vcam_id = BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.ACC_VCAM_ID);
					dev_token = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(result.get(0),BeseyeJSONUtil.ACC_SES_DEV), BeseyeJSONUtil.ACC_SES_TOKEN);
					monitorAsyncTask(new BeseyeAccountTask.CamValidateTask(this), true, vcam_id, dev_token);
					monitorAsyncTask(new BeseyeAccountTask.GetUserInfoTask(this), true);
				}
			}else if(task instanceof BeseyeAccountTask.CamValidateTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					monitorAsyncTask(new BeseyeAccountTask.SetCamAttrTask(this), true, vcam_id, "My Test Cam");
				}
			}*/else if(task instanceof BeseyeAccountTask.SetCamAttrTask){
				if(0 == iRetCode){
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
						
						Log.i(TAG, "miOriginalVCamCnt:"+miOriginalVCamCnt+", iNetVcamCnt = "+iNetVcamCnt);
						if(miOriginalVCamCnt < iNetVcamCnt){
							//JSONArray VcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_VCAM_LST);
							if(null != arrCamList){
								JSONObject cam_obj = null;
								try {
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
												Log.i(TAG, "find new cam = "+cam_obj);
												mbFindNewCam = true;
												break;
											}
										}
									}
									//cam_obj = arrCamList.getJSONObject(iNetVcamCnt-1);
									if(null != cam_obj){
//										String strVcamId = BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID);
//										String strCamName = BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME);
										
//										if(BeseyeConfig.COMPUTEX_DEMO)
//											mStrCamName = "New Camera";
										
//										if(null != mStrCamName && 0 < mStrCamName.length()){
//											strCamName = mStrCamName;
//											BeseyeJSONUtil.setJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME, strCamName);
//											Log.i(TAG, "update cam name, cam_obj:"+cam_obj.toString());
//											monitorAsyncTask(new BeseyeAccountTask.SetCamAttrTask(this).setDialogId(-1), false, strVcamId, strCamName);
//										}
										
										//workaround
										SessionMgr.getInstance().setIsCertificated(true);
//										Bundle b = new Bundle();
//										b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
//										b.putBoolean(CameraViewActivity.KEY_PAIRING_DONE, true);
//										launchDelegateActivity(CameraListActivity.class.getName(), b);
										
										Bundle b = new Bundle();
										b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
										launchDelegateActivity(SoundPairingNamingActivity.class.getName(), b);
										WifiControlBaseActivity.updateWiFiPasswordHistory("");
									}
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							sStrCamNameCandidate = null;
						}else{
							// if pairing failed
							WifiControlBaseActivity.updateWiFiPasswordHistory(null != mChosenWifiAPInfo?mChosenWifiAPInfo.password:"");
							launchActivityByClassName(PairingFailActivity.class.getName(), getIntent().getExtras());
						}
					}
				}
			}else{
				Log.i(TAG, "onPostExecute(), "+result.toString());
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	protected boolean onCameraActivated(JSONObject msgObj){
		//Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(),  msgObj = "+msgObj);
    	if(null != msgObj){
    		Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(),  msgObj = "+msgObj);
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strPairToken = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_PAIR_TOKEN);
    			if(null != strPairToken && strPairToken.equals(SessionMgr.getInstance().getPairToken())){
    				Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(), find match strPairToken = "+strPairToken);
    				if(false == mbFindNewCam){
	    				String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
	    				monitorAsyncTask(mGetNewCamTask = new BeseyeAccountTask.GetCamInfoTask(this), false, strCamUID);
	    				mbFindNewCam = true;
	    				WifiControlBaseActivity.updateWiFiPasswordHistory("");
    				}
    				SessionMgr.getInstance().setPairToken("");
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
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraOnline(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != mStrChangeWiFiVCamId && mStrChangeWiFiVCamId.equals(strCamUID)){
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

