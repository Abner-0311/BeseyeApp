package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeUtils.*;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.R;
import com.app.beseye.TouchSurfaceView.OnTouchSurfaceCallback;
import com.app.beseye.audio.AudioChannelMgr;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.httptask.BeseyeNotificationBEHttpTask;
import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.websockets.AudioWebSocketsMgr;
import com.app.beseye.websockets.WebsocketsMgr.OnWSChannelStateChangeListener;
import com.app.beseye.widget.CameraViewControlAnimator;

import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;

public class CameraViewActivity extends BeseyeBaseActivity implements OnTouchSurfaceCallback,
																	  OnNetworkChangeCallback,
																	  OnWSChannelStateChangeListener{
	static public final String KEY_PAIRING_DONE 	= "KEY_PAIRING_DONE";
	static public final String KEY_DVR_STREAM_MODE  = "KEY_DVR_STREAM_MODE";
	static public final String KEY_DVR_STREAM_TS    = "KEY_DVR_STREAM_TS";
	static public final String DVR_REQ_TIME         = "60000";
	
	private TouchSurfaceView mStreamingView;
	private TextView mTxtDate, mTxtCamName, mTxtTime, mTxtEvent, mTxtGoLive, mTxtPowerState;
	
	private ImageView mIvStreamType;
	private ImageButton mIbTalk, mIbRewind, mIbPlayPause, mIbFastForward, mIbSetting;
	
	private RelativeLayout mVgHeader, mVgToolbar;
	private CameraViewControlAnimator mCameraViewControlAnimator;
	private ProgressBar mPbLoadingCursor;
	private ViewGroup mVgPowerState, mVgCamInvalidState, mVgPairingDone;
	private Button mBtnPairingDoneOK; 
	private ImageButton mIbOpenCam;
	private String mStrVCamID = "Bes0001";
	private String mStrVCamName = null;
	
	private boolean mbIsLiveMode = true;//false means VOD
	private String mstrLiveStreamServer;
	private String mstrLiveStreamPath;
	private List<JSONObject> mstrDVRStreamPathList;
	private List<JSONObject> mstrPendingStreamPathList;
	private long mlDVRStartTs;
	
	
	private boolean mbIsFirstLaunch = true;
	private boolean mbIsPauseWhenPlaying = false;
	private boolean mbIsCamSettingChanged = false;
	private boolean mbIsWifiSettingChanged = false;
	
	//CameraView internal status
    enum CameraView_Internal_Status{
    	CV_STATUS_UNINIT,
    	CV_STREAM_INIT,
    	CV_STREAM_CONNECTING,
    	CV_STREAM_CONNECTED,
    	CV_STREAM_PLAYING,
    	CV_STREAM_WAITING_PAUSE,
    	CV_STREAM_PAUSING,
    	CV_STREAM_PAUSED,
    	CV_STREAM_EOF,
    	CV_STREAM_WAITING_CLOSE,
    	CV_STREAM_CLOSE
    }
    
    private CameraView_Internal_Status mCamViewStatus = CameraView_Internal_Status.CV_STATUS_UNINIT;
    
    private CameraView_Internal_Status getCamViewStatus(){
    	return mCamViewStatus;	
    }
    
    private boolean isCamViewStatus(CameraView_Internal_Status status){
    	return mCamViewStatus == status;
    }
    
    private void setCamViewStatus(final CameraView_Internal_Status status){
       	if(null != sHandler){
    		sHandler.post(new Runnable(){
				@Override
				public void run() {
					Log.e(TAG, "setCamViewStatus(), status:" + status);
			    	CameraView_Internal_Status precCamViewStatus = mCamViewStatus;
			    	
			    	switch(status){
			    		case CV_STATUS_UNINIT:{
			    			stopUpdateTime();
			    			
			    			setEnabled(mIbTalk, false);
			    			setEnabled(mIbRewind, false);
			    			setEnabled(mIbPlayPause, isCamPowerOn() && NetworkMgr.getInstance().isNetworkConnected());
			    			setEnabled(mIbFastForward, false);
			    			setVisibility(mPbLoadingCursor, View.GONE);
			    			
			    			setImageRes(mIbPlayPause, R.drawable.sl_liveview_play_btn);
			    			hideInvalidStateMask();
			    			break;
			    		}
			    		case CV_STREAM_INIT:{
			    			initDateTime();
			    			aquireWakelock();
			    			setEnabled(mIbPlayPause, false);
			    			setVisibility(mPbLoadingCursor, View.VISIBLE);
			    			startCheckVideoConn();
			    			break;
			    		}
			    		case CV_STREAM_CONNECTING:{
			    			break;
			    		}
			    		case CV_STREAM_CONNECTED:{
			    			CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_ON);
			    			hideInvalidStateMask();
			    			break;
			    		}
			    		case CV_STREAM_PLAYING:{
			    			setEnabled(mIbTalk, mbIsLiveMode);
			    			//setEnabled(mIbRewind, true);
			    			cancelCheckVideoConn();
			    			setEnabled(mIbPlayPause, true);
			    			//setEnabled(mIbFastForward, !mbIsLiveMode);
			    			setVisibility(mPbLoadingCursor, View.GONE);
			    			
			    			startUpdateTime();
			    			
			    			setImageRes(mIbPlayPause, R.drawable.sl_liveview_pause_btn);
			    			break;
			    		}
			    		case CV_STREAM_WAITING_PAUSE:{
			    			setVisibility(mPbLoadingCursor, View.VISIBLE);
			    			break;
			    		}
			    		case CV_STREAM_PAUSED:{
			    			setEnabled(mIbRewind, false);
			    			setEnabled(mIbPlayPause, true);
			    			setEnabled(mIbFastForward, false);
			    			setVisibility(mPbLoadingCursor, View.GONE);
			    			stopUpdateTime();
			    			setImageRes(mIbPlayPause, R.drawable.sl_liveview_play_btn);
			    			break;
			    		}
			    		case CV_STREAM_EOF:{
			    			setVisibility(mPbLoadingCursor, View.VISIBLE);
			    			stopUpdateTime();
			    			break;
			    		}
			    		case CV_STREAM_WAITING_CLOSE:{
			    			setVisibility(mPbLoadingCursor, View.VISIBLE);
			    			break;
			    		}
			    		case CV_STREAM_CLOSE:{
			    			setCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT);
			    			releaseWakelock();
			    			cancelCheckVideoConn();
			    			break;
			    		}
			    		default:{
			    			Log.e(TAG, "setCamViewStatus(), invalid type" + mCamViewStatus);
			    			return;
			    		}
			    	}
			    	mCamViewStatus = status;
				}});
    	}
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "CameraViewActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		getSupportActionBar().hide();
		
		mbIsLiveMode = !getIntent().getBooleanExtra(KEY_DVR_STREAM_MODE, false);
		mStrVCamID = getIntent().getStringExtra(CameraListActivity.KEY_VCAM_ID);
		
		mStrVCamName = getIntent().getStringExtra(CameraListActivity.KEY_VCAM_NAME);
		
		mstrDVRStreamPathList = new ArrayList<JSONObject>();
		mstrPendingStreamPathList = new ArrayList<JSONObject>();
		
		mlDVRStartTs = System.currentTimeMillis() - 60*60*1000; //getIntent().getLongExtra(KEY_DVR_STREAM_TS, 0);
		Log.i(TAG, "CameraViewActivity::onCreate(), mlDVRStartTs="+mlDVRStartTs);
		
		mStreamingView = (TouchSurfaceView)findViewById(R.id.surface_streaming_view);
		if(null != mStreamingView){
			mStreamingView.registerSingleTapCallback(this);
		}
		
		mTxtDate = (TextView)findViewById(R.id.txt_streaming_date);
		mTxtTime = (TextView)findViewById(R.id.txt_streaming_time);
		mTxtCamName = (TextView)findViewById(R.id.txt_cam_name);
		if(!ASSIGN_ST_PATH && null != mTxtCamName){
			mTxtCamName.setOnClickListener(this);
		}
		
		mVgPowerState = (ViewGroup)findViewById(R.id.vg_cam_power_state);
		if(null != mVgPowerState){
			mTxtPowerState = (TextView)mVgPowerState.findViewById(R.id.txt_cam_power_state);
			mIbOpenCam = (ImageButton)mVgPowerState.findViewById(R.id.ib_open_cam);
			if(null != mIbOpenCam){
				mIbOpenCam.setOnClickListener(this);
			}
		}
		
		if(getIntent().getBooleanExtra(KEY_PAIRING_DONE, false)){
			mVgPairingDone = (ViewGroup)findViewById(R.id.vg_pairing_done);
			if(null != mVgPairingDone){
				BeseyeUtils.setVisibility(mVgPairingDone, View.VISIBLE);
				if(null != mVgPairingDone){
					mVgPairingDone.setOnClickListener(this);
					mBtnPairingDoneOK = (Button)mVgPairingDone.findViewById(R.id.button_start);
					if(null != mBtnPairingDoneOK){
						mBtnPairingDoneOK.setOnClickListener(this);
					}
					//worksround
					CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_ON);
				}
			}	
		}
		
		mVgCamInvalidState = (ViewGroup)findViewById(R.id.vg_cam_invald_statement);
		
		mUpdateDateTimeRunnable = new UpdateDateTimeRunnable(mTxtDate, mTxtTime);
		
		mTxtEvent = (TextView)findViewById(R.id.txt_events);
		if(null != mTxtEvent){
			mTxtEvent.setOnClickListener(this);
			//mTxtEvent.setEnabled(false);//not implement
		}
		
		mTxtGoLive = (TextView)findViewById(R.id.txt_go_live);
		if(null != mTxtGoLive){
			mTxtGoLive.setOnClickListener(this);
			mTxtGoLive.setEnabled(false);
		}
		
		mIvStreamType = (ImageView)findViewById(R.id.iv_streaming_type);
		if(null != mIvStreamType){
			mIvStreamType.setOnClickListener(this);
		}
		
		mIbTalk = (ImageButton)findViewById(R.id.ib_talk);
		if(null != mIbTalk){
			mIbTalk.setOnClickListener(this);
			//mIbTalk.setEnabled(false);//not implement
		}
		
		mIbRewind = (ImageButton)findViewById(R.id.ib_rewind);
		if(null != mIbRewind){
			mIbRewind.setOnClickListener(this);
			mIbRewind.setEnabled(false);//not implement
		}
		
		mIbPlayPause = (ImageButton)findViewById(R.id.ib_play_pause);
		if(null != mIbPlayPause){
			mIbPlayPause.setOnClickListener(this);
		}
		
		mIbFastForward = (ImageButton)findViewById(R.id.ib_fast_forward);
		if(null != mIbFastForward){
			mIbFastForward.setOnClickListener(this);
			mIbFastForward.setEnabled(false);//not implement
		}
		
		mIbSetting = (ImageButton)findViewById(R.id.ib_settings);
		if(null != mIbSetting){
			mIbSetting.setOnClickListener(this);
			//mIbSetting.setEnabled(false);//not implement
		}
		
		mPbLoadingCursor = (ProgressBar)findViewById(R.id.pb_loadingCursor);
		
		mVgHeader = (RelativeLayout)findViewById(R.id.vg_streaming_view_header);
		if(null != mVgHeader){
			mVgHeader.setOnClickListener(this);
		}
		mVgToolbar = (RelativeLayout)findViewById(R.id.vg_streaming_view_footer);
		if(null != mVgToolbar){
			mVgToolbar.setOnClickListener(this);
		}
		mCameraViewControlAnimator = new CameraViewControlAnimator(this, mVgHeader, mVgToolbar);
		
		mSingleton = this;
		
		setCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT);
		
		AudioWebSocketsMgr.getInstance().registerOnWSChannelStateChangeListener(this);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "CameraViewActivity::onResume()");
		super.onResume();
		if(null != mPauseCameraViewRunnable){
			BeseyeUtils.removeRunnable(mPauseCameraViewRunnable);
			mPauseCameraViewRunnable = null;
		}
		
		NetworkMgr.getInstance().registerNetworkChangeCallback(this);
		
		if(null != mCameraViewControlAnimator){
			mCameraViewControlAnimator.showControl();
		}
		
		checkAndExtendHideHeader();
		
//		AsyncHttpClient.getDefaultInstance().websocket("ws://192.168.2.151:5432","beseye-soundpair-protocol", new WebSocketConnectCallback(){
//
//			@Override
//			public void onCompleted(Exception ex, WebSocket webSocket) {
//				Log.i(TAG, "onCompleted()..., ex="+((null == ex)?"":ex.toString()));
//				if(null == ex)
//					webSocket.send("Welcome");
//			}});
	}
	
	protected void onSessionComplete(){
		Log.d(TAG, "CameraViewActivity::onSessionComplete(), mbIsFirstLaunch:"+mbIsFirstLaunch+", mbIsPauseWhenPlaying:"+mbIsPauseWhenPlaying+", mbIsCamSettingChanged:"+mbIsCamSettingChanged+", mbIsWifiSettingChanged:"+mbIsWifiSettingChanged);
		if(false == handleReddotNetwork(false)){
			if(null != mTxtCamName){
				mTxtCamName.setText((null == mStrVCamName)?CamSettingMgr.getInstance().getCamName(TMP_CAM_ID):mStrVCamName);
			}
			
			if(null != mStreamingView)
				mStreamingView.setUpsideDown(CamSettingMgr.getInstance().getVideoUpsideDown(TMP_CAM_ID) == 1);
		}
		
		if(null != mVgCamInvalidState){
			mVgCamInvalidState.setVisibility(View.GONE);
			if(CAM_CONN_STATUS.CAM_DISCONNECTED == CamSettingMgr.getInstance().getCamPowerState(TMP_CAM_ID)){
				CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_ON);
				mbIsCamSettingChanged = true;
				Log.d(TAG, "CameraViewActivity::onPostResume(), make mbIsCamSettingChanged: true.............");
			}
		}
		
		checkPlayState();
		initDateTime();
	}
		
	@Override
	protected void onPostResume() {
		//Log.d(TAG, "CameraViewActivity::onPostResume(), mbIsFirstLaunch:"+mbIsFirstLaunch+", mbIsPauseWhenPlaying:"+mbIsPauseWhenPlaying+", mbIsCamSettingChanged:"+mbIsCamSettingChanged+", mbIsWifiSettingChanged:"+mbIsWifiSettingChanged);
		super.onPostResume();
//		if(false == handleReddotNetwork(false)){
//			if(null != mTxtCamName){
//				mTxtCamName.setText(CamSettingMgr.getInstance().getCamName(TMP_CAM_ID));
//			}
//			
//			if(null != mStreamingView)
//				mStreamingView.setUpsideDown(CamSettingMgr.getInstance().getVideoUpsideDown(TMP_CAM_ID) == 1);
//		}
//		
//		if(null != mVgCamInvalidState){
//			mVgCamInvalidState.setVisibility(View.GONE);
//			if(CAM_CONN_STATUS.CAM_DISCONNECTED == CamSettingMgr.getInstance().getCamPowerState(TMP_CAM_ID)){
//				CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_ON);
//				mbIsCamSettingChanged = true;
//				Log.d(TAG, "CameraViewActivity::onPostResume(), make mbIsCamSettingChanged: true.............");
//
//			}
//		}
//		
//		checkPlayState();
//		initDateTime();
	}
	
	private boolean handleReddotNetwork(boolean bForceShow){
		boolean bRet = false;
		Log.i(TAG, "CameraViewActivity::handleReddotNetwork(), isWifiEnabled:"+NetworkMgr.getInstance().isWifiEnabled()+
															  ",getActiveWifiSSID:"+NetworkMgr.getInstance().getActiveWifiSSID()+
															  ",bForceShow:"+bForceShow);
		if(REDDOT_DEMO && NetworkMgr.getInstance().isWifiEnabled() && RELAY_AP_SSID.equals(NetworkMgr.getInstance().getActiveWifiSSID())){
			if(bForceShow){
				Log.i(TAG, "CameraViewActivity::handleReddotNetwork(), cannot contact AP --------------");
				showInvalidStateMask();
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						beginLiveView();
					}}, 600L);
			}
		}else if(REDDOT_DEMO && (bForceShow ||(!NetworkMgr.getInstance().isWifiEnabled() || !RELAY_AP_SSID.equals(NetworkMgr.getInstance().getActiveWifiSSID())))){
			Log.i(TAG, "CameraViewActivity::handleReddotNetwork(), launch wifi list --------------");
			Intent intent = new Intent();
			intent.setClass(this, WifiListActivity.class);
			intent.putExtra(WifiListActivity.KEY_CHANGE_WIFI_ONLY, true);
			startActivityForResult(intent, REQUEST_WIFI_SETTING_CHANGED);
			bRet = true;
		}
		return bRet;
	}
	
	private static class PauseCameraViewRunnable implements Runnable{
		WeakReference<CameraViewActivity> mCameraViewActivity;
		
		PauseCameraViewRunnable(CameraViewActivity act){
			mCameraViewActivity = new WeakReference<CameraViewActivity>(act);
		}
		@Override
		public void run() {
			CameraViewActivity act = mCameraViewActivity.get();
			if(null != act){
				if(act.isCamViewStatus(CameraView_Internal_Status.CV_STREAM_PLAYING)){
					Log.d(TAG, "CameraViewActivity::onPause()->run(), pause when playing");
					act.mbIsPauseWhenPlaying = true;
				}
				//Close it because pause not implement
				act.closeStreaming();
				act.stopUpdateTime();
				act.releaseWakelock();
				NetworkMgr.getInstance().unregisterNetworkChangeCallback(act);
				act.mCameraViewControlAnimator.cancelHideControl();
				
				if(AudioWebSocketsMgr.getInstance().isNotifyWSChannelAlive()){
					AudioWebSocketsMgr.getInstance().destroyNotifyWSChannel();
				}
			}
		}
	}
	
	private static final long TIME_TO_CONFIRM_PAUSE = 500L;
	private PauseCameraViewRunnable mPauseCameraViewRunnable;

	@Override
	protected void onPause() {
		Log.d(TAG, "CameraViewActivity::onPause()");
		if(null == mPauseCameraViewRunnable){
			mPauseCameraViewRunnable = new PauseCameraViewRunnable(this);
		}
		BeseyeUtils.removeRunnable(mPauseCameraViewRunnable);
		BeseyeUtils.postRunnable(mPauseCameraViewRunnable, TIME_TO_CONFIRM_PAUSE);
		
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "CameraViewActivity::onDestroy()");
		super.onDestroy();
		// Now wait for the SDL thread to quit
        if (TouchSurfaceView.mSDLThread != null) {
            try {
            	TouchSurfaceView.mSDLThread.join();
            } catch(Exception e) {
                Log.v(TAG, "Problem stopping thread: " + e);
            }
            TouchSurfaceView.mSDLThread = null;

            //Log.v(TAG, "Finished waiting for SDL thread");
        }
	}
	
	@Override
	public void onConnectivityChanged(boolean bNetworkConnected) {
		Log.d(TAG, "CameraViewActivity::onConnectivityChanged(), bNetworkConnected="+bNetworkConnected+", state="+getCamViewStatus());
		if(false == bNetworkConnected){
			if(!isCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT)){
				closeStreaming();
				showNoNetworkDialog();
			}
		}else{
			if(isCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT)){
				checkPlayState();
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WARNING:{
				dialog = super.onCreateDialog(id, bundle);
				if(null != dialog){
					Log.w(TAG, "CameraViewActivity::onDismiss()");
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							Log.w(TAG, "CameraViewActivity::onDismiss(), mbNeedToCheckReddotNetwork = "+mbNeedToCheckReddotNetwork);
							if(mbNeedToCheckReddotNetwork)
								handleReddotNetwork(true);
							mbNeedToCheckReddotNetwork = false;
							
							if(null != mCameraViewControlAnimator){
								mCameraViewControlAnimator.showControl();
							}
							checkAndExtendHideHeader();
						}});
				}
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		
		return dialog;
	}

	private void showNoNetworkDialog(){
		Bundle b = new Bundle();
		b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.streaming_error_no_network));
		showMyDialog(DIALOG_ID_WARNING, b);
		showInvalidStateMask();
	}
	
	private boolean mbNeedToCheckReddotNetwork = false;
	private void showInvalidStateMask(){
		mbNeedToCheckReddotNetwork = true;
		sHandler.post(new Runnable(){
			@Override
			public void run() {
				if(null != mVgCamInvalidState){
					mVgCamInvalidState.setVisibility(View.VISIBLE);
					CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_DISCONNECTED);
				}
			}});
	}
	
	private void hideInvalidStateMask(){
		//mbNeedToCheckReddotNetwork = false;
		sHandler.post(new Runnable(){
			@Override
			public void run() {
				if(null != mVgCamInvalidState){
					mVgCamInvalidState.setVisibility(View.GONE);
//					if(-1 == CamSettingMgr.getInstance().getCamPowerState(TMP_CAM_ID))
//						CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, 1);
				}
			}});
	}
	
	private void checkPlayState(){
		boolean bPowerOn = isCamPowerOn();
		boolean bNetworkConnected = NetworkMgr.getInstance().isNetworkConnected();
		
		Log.i(TAG, "CameraViewActivity::checkPlayState(), bPowerOn:"+bPowerOn+", bNetworkConnected:"+bNetworkConnected);
		
		if(bNetworkConnected && bPowerOn && (mbIsFirstLaunch || mbIsPauseWhenPlaying || mbIsCamSettingChanged || mbIsWifiSettingChanged)){
			//beginLiveView();
			//monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
			getStreamingInfo();
		}/*else{
			setCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT);
		}*/
		
		setEnabled(mIbPlayPause, bPowerOn && bNetworkConnected);
		
		if(!bNetworkConnected){
			showNoNetworkDialog();
		}
		
		updatePowerState();
		mbIsFirstLaunch = mbIsPauseWhenPlaying = mbIsCamSettingChanged = false;
		
		if(null != mStreamingView){
			mStreamingView.drawDefaultBackground();
		}
	}
	
	private BeseyeHttpTask mLiveStreamTask = null;
	private BeseyeHttpTask mDVRStreamTask = null;
	
	private void getStreamingInfo(){
		if(mbIsLiveMode){
			if(null == mLiveStreamTask)
				monitorAsyncTask(mLiveStreamTask = new BeseyeMMBEHttpTask.GetLiveStreamTask(this), true, (null != mStrVCamID)?mStrVCamID:TMP_MM_VCAM_ID, "false");
		}else{
			if(null == mDVRStreamTask)
				monitorAsyncTask(mDVRStreamTask = new BeseyeMMBEHttpTask.GetDVRStreamTask(this), true, (null != mStrVCamID)?mStrVCamID:TMP_MM_VCAM_ID, mlDVRStartTs+"", DVR_REQ_TIME);
		}
	}
	
	private boolean isCamPowerOn(){
		return (CamSettingMgr.getInstance().getCamPowerState(TMP_CAM_ID) == CAM_CONN_STATUS.CAM_ON) ;
	}
	
	private boolean isCamPowerOff(){
		return (CamSettingMgr.getInstance().getCamPowerState(TMP_CAM_ID) == CAM_CONN_STATUS.CAM_OFF) ;
	}
	
	private boolean isCamPowerDisconnected(){
		return (CamSettingMgr.getInstance().getCamPowerState(TMP_CAM_ID) == CAM_CONN_STATUS.CAM_DISCONNECTED) ;
	}
	
	private void updatePowerState(){
		if(null != mVgPowerState){
			if(isCamPowerOff()){
				mVgPowerState.setVisibility(View.VISIBLE);
			}else{
				mVgPowerState.setVisibility(View.GONE);
			}			
		}
	}
	
	private PowerManager.WakeLock mWakelock;
	private void aquireWakelock(){
		if(null == mWakelock){
			PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
			mWakelock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, getPackageName());
			if(null != mWakelock){
				mWakelock.acquire();
				Log.d(TAG, "CameraViewActivity::aquireWakelock(), acquire a wakelock");
			}
		}else{
			Log.w(TAG, "CameraViewActivity::aquireWakelock(), wakelock was already acquired");
		}
	}
	
	private void releaseWakelock(){
		if(null != mWakelock){
			mWakelock.release();
			mWakelock = null;
			Log.d(TAG, "CameraViewActivity::releaseWakelock(), release a wakelock");
		}else{
			Log.w(TAG, "CameraViewActivity::releaseWakelock(), wakelock wasn't acquired yet");
		}
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeNotificationBEHttpTask.GetAudioWSServerTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
					JSONArray arr = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.OBJ_DATA);
					try {
						AudioWebSocketsMgr.getInstance().setAudioWSServerIP(arr.getString(0));
						AudioWebSocketsMgr.getInstance().constructNotifyWSChannel();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}else if(task instanceof BeseyeMMBEHttpTask.GetLiveStreamTask){
				if(0 == iRetCode){
					//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
					
					JSONObject streamInfo = result.get(0);
					if(null != streamInfo){
						mstrLiveStreamServer = BeseyeJSONUtil.getJSONString(streamInfo, BeseyeJSONUtil.MM_SERVER);
						mstrLiveStreamPath = BeseyeJSONUtil.getJSONString(streamInfo, BeseyeJSONUtil.MM_STREAM);
						beginLiveView();
					}
				}else if(ASSIGN_ST_PATH){
					//Workaround
					beginLiveView();
				}else{
					//Workaround
					mstrLiveStreamServer = "rtmp://54.238.191.39:1935/live-edge/_definst_";
					mstrLiveStreamPath = "{o}54.250.149.50/live-origin-record/_definst_/1001_aac";
//					mstrLiveStreamServer = "rtmp://54.250.149.50/vods3/_definst_";//rtmp://54.238.191.39:1935/live-edge/_definst_";
//					mstrLiveStreamPath = "mp4:amazons3/wowza2.s3.tokyo/liveorigin/sample.mp4";//{o}54.250.149.50/live-origin-record/_definst_/1001_aac";
					beginLiveView();
				}
			}else if(task instanceof BeseyeMMBEHttpTask.GetDVRStreamTask){
				if(0 == iRetCode){
					//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
					JSONObject streamInfo = result.get(0);
					if(null != streamInfo){
						JSONArray streamList = BeseyeJSONUtil.getJSONArray(streamInfo, BeseyeJSONUtil.MM_PLAYLIST);
						if(null != streamList){
							appendStreamList(streamList);
							beginLiveView();
						}
					}
				}
			}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					int iVcamCnt = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_VCAM_CNT);
					if(0 < iVcamCnt){
						JSONArray VcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_VCAM_LST);
						if(null != VcamList){
							try {
								JSONObject vcam = VcamList.getJSONObject(0);
								if(null != vcam){
									mStrVCamID = BeseyeJSONUtil.getJSONString(vcam, BeseyeJSONUtil.ACC_ID);
									mStrVCamName = BeseyeJSONUtil.getJSONString(vcam, BeseyeJSONUtil.ACC_NAME);
									Log.e(TAG, "onPostExecute(), mStrVCamID:"+mStrVCamID);
									getStreamingInfo();
								}
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}else{
						onToastShow(task, "no Vcam attached.");
						Bundle b = new Bundle();
						b.putBoolean(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
						launchDelegateActivity(WifiSetupGuideActivity.class.getName(), b);
					}
				}
			}else{
				Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
		
		if(task == mLiveStreamTask){
			mLiveStreamTask = null;
		}else if(task == mDVRStreamTask){
			mDVRStreamTask = null;
		}
	}

	protected int getLayoutId(){
		return R.layout.layout_camera_view;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.txt_cam_name:{
//				Intent intent = new Intent();
//				intent.setClass(CameraViewActivity.this, SoundPairingActivity.class);
//				startActivity(intent);
				//invokeLogout();
				break;
			}
			case R.id.txt_events:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_ID, mStrVCamID);
				b.putString(CameraListActivity.KEY_VCAM_NAME, mStrVCamName);
				launchActivityByClassName(EventListActivity.class.getName(), b);
				break;
			}
			case R.id.txt_go_live:{
				
				break;
			}
			case R.id.ib_talk:{
				if(AudioWebSocketsMgr.getInstance().isNotifyWSChannelAlive()){
					AudioWebSocketsMgr.getInstance().destroyNotifyWSChannel();
				}else{
					monitorAsyncTask(new BeseyeNotificationBEHttpTask.GetAudioWSServerTask(this), true);
				}
				break;
			}
			case R.id.ib_rewind:{
				break;
			}
			case R.id.vg_pairing_done:{
				break;
			}
			case R.id.button_start:{
				BeseyeUtils.setVisibility(mVgPairingDone, View.GONE);
				break;
			}
			case R.id.ib_play_pause:{
				if(isCamViewStatus(CameraView_Internal_Status.CV_STREAM_PLAYING))
					closeStreaming();
				else{
					//beginLiveView();
					getStreamingInfo();
				}
				break;
			}
			case R.id.ib_fast_forward:{
				break;
			}
			case R.id.ib_settings:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_ID, mStrVCamID);
				b.putString(CameraListActivity.KEY_VCAM_NAME, mStrVCamName);
				launchActivityForResultByClassName(CameraSettingActivity.class.getName(), b, REQUEST_CAM_SETTING_CHANGED);
//				if(!receiveAudioBufThreadRunning()){
//					receiveAudioBufFromCam("");
//				}else{
//					stopReceiveAudioBufThread();
//				}
				break;
			}
			case R.id.iv_streaming_type:{
//				if(1 < STREAM_PATH_LIST.size()){
//					CUR_STREAMING_PATH_IDX++;
//					Toast.makeText(this, "Streaming "+STREAM_PATH_LIST.get(CUR_STREAMING_PATH_IDX%STREAM_PATH_LIST.size())+" is going to play", Toast.LENGTH_LONG).show();
//					if(!isCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT)){
//						closeStreaming();
//						BeseyeUtils.postRunnable(new Runnable(){
//							@Override
//							public void run() {
//								beginLiveView();
//							}}, 600L);
//					}else{
//						beginLiveView();
//					}
//				}
//				Bundle b = new Bundle();
//				b.putBoolean(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
//				launchDelegateActivity(WifiSetupGuideActivity.class.getName(), b);
				break;
			}
			case R.id.ib_open_cam:{
				CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_ON);
				mbIsCamSettingChanged = true;
				checkPlayState();
				break;
			}
			default:
				Log.w(TAG, "onClick(), not handle view id:"+view.getId());
		}
	}
	
	static public final int REQUEST_CAM_SETTING_CHANGED = 1;
	static public final int REQUEST_WIFI_SETTING_CHANGED = 2;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_CAM_SETTING_CHANGED == requestCode){
			mbIsCamSettingChanged = (resultCode == RESULT_OK);
		}else if(REQUEST_WIFI_SETTING_CHANGED== requestCode){
			if(resultCode == RESULT_OK){
				mbIsWifiSettingChanged = true;
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void onSingleTapConfirm() {
		if(null != mCameraViewControlAnimator && false == mCameraViewControlAnimator.isInAnimation())
			mCameraViewControlAnimator.performControlAnimation();
	}
	
	private void checkAndExtendHideHeader(){
		if(null != mCameraViewControlAnimator && View.VISIBLE == mCameraViewControlAnimator.getVisibility())
			mCameraViewControlAnimator.extendHideControl();
	}

	@Override
	public void onTouch() {
		checkAndExtendHideHeader();
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(null != mVgPairingDone && View.VISIBLE == mVgPairingDone.getVisibility()){
				BeseyeUtils.setVisibility(mVgPairingDone, View.GONE);
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	boolean mIsPause = true;
	boolean mIsStop = false;
	int idx =1;
	
	private void beginLiveView(){
    	if(null == mStreamingView){
    		Log.w(TAG, "beginLiveView(), mStreamingView is null");
    		return;
    	}
    	
    	 mIsStop = false;
         new Thread(){
         	public void run(){ 
         		if (TouchSurfaceView.mSDLThread != null) {
                    try {
                    	TouchSurfaceView.mSDLThread.join();
                    } catch(Exception e) {
                        Log.v(TAG, "Problem stopping thread: " + e);
                    }
                }
         		idx = 1;    
         		
         		if(mbIsLiveMode){
         			if(ASSIGN_ST_PATH){
	         			if(0 <= openStreaming(0, getNativeSurface(), STREAM_PATH_LIST.get(CUR_STREAMING_PATH_IDX%STREAM_PATH_LIST.size()), 0)){
	             			setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE);
	                 		mCurCheckCount = 0;
	             		}
         			}else{
	         			String streamFullPath;
	             		if(null != mstrLiveStreamServer && null != mstrLiveStreamPath){
	             			streamFullPath = mstrLiveStreamServer+"/"+mstrLiveStreamPath;
	             		}else{
	             			streamFullPath = STREAM_PATH_LIST.get(CUR_STREAMING_PATH_IDX%STREAM_PATH_LIST.size());
	             		}
	             		
	         			if(0 <= openStreaming(0, getNativeSurface(), streamFullPath, 0)){
	             			setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE);
	                 		mCurCheckCount = 0;
	             		}
         			}
         		}else{
         			if(0 < mstrPendingStreamPathList.size()){
         				if(CameraView_Internal_Status.CV_STREAM_CONNECTED.ordinal() <=  mCamViewStatus.ordinal() && 
         				   mCamViewStatus.ordinal() < CameraView_Internal_Status.CV_STREAM_WAITING_CLOSE.ordinal()){
         					Log.i(TAG, "append list mstrDVRStreamPathList.size():"+mstrDVRStreamPathList.size());
         					synchronized(CameraViewActivity.this){
         						
	                     		while(0 < mstrPendingStreamPathList.size()){
	                     			String stream =  BeseyeJSONUtil.getJSONString(mstrPendingStreamPathList.get(0), BeseyeJSONUtil.MM_STREAM);
	                     			if(0 > addStreamingPath(0,stream)){
	                     				Log.i(TAG, "failed to append stream"+stream);
	                     				mstrPendingStreamPathList.clear();
	                     				break;
	                     			}
	                     			mstrDVRStreamPathList.add(mstrPendingStreamPathList.remove(0));
	                     		}
                     		}
         				}else{
         					String[] streamList = null;
             				String strHost=null;
             				synchronized(CameraViewActivity.this){
             					int iLen = mstrPendingStreamPathList.size();
             					if(0 < iLen){
             						streamList = new String[mstrPendingStreamPathList.size()];
                 					strHost = BeseyeJSONUtil.getJSONString(mstrPendingStreamPathList.get(0), BeseyeJSONUtil.MM_SERVER)+"/";
             					}
             					for(int i = 0;i< iLen;i++){
             						streamList[i] = BeseyeJSONUtil.getJSONString(mstrPendingStreamPathList.get(i), BeseyeJSONUtil.MM_STREAM);
             					}
             				}
             			
    						if(strHost != null && null != streamList){
    							synchronized(CameraViewActivity.this){
    	                     		while(0 < mstrPendingStreamPathList.size()){
    	                     			mstrDVRStreamPathList.add(mstrPendingStreamPathList.remove(0));
    	                     		}
    	                     		Log.i(TAG, "mstrDVRStreamPathList.size():"+mstrDVRStreamPathList.size());
                         		}
    							
    							if(0 <= openStreamingList(0, getNativeSurface(), strHost, streamList, 0)){
    								//Log.i(TAG, "openStreamingList out");
    	                 			setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE);
    	                     		mCurCheckCount = 0;
//    	                     		//roll back
//    	                     		synchronized(CameraViewActivity.this){
//    		                     		while(0 < mstrDVRStreamPathList.size()){
//    		                     			mstrPendingStreamPathList.add(mstrDVRStreamPathList.remove(0));
//    		                     		}
//    		                     		//Log.i(TAG, "mstrDVRStreamPathList.size():"+mstrDVRStreamPathList.size());
//    	                     		}
    							}
    						}
         				}
         			}else{
         				Log.e(TAG, "mstrDVRStreamServer is null");
         				BeseyeUtils.postRunnable(new Runnable(){
							@Override
							public void run() {
								Bundle b = new Bundle();
								b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.streaming_invalid_dvr));
								showMyDialog(DIALOG_ID_WARNING, b);
							}}, 0);
         				
         			}
         		}
         		
         		//if(isCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE))
         	}
         }.start();
         
    }
	
	private void startUpdateTime(){
		if(null != mUpdateDateTimeRunnable){
			sHandler.removeCallbacks(mUpdateDateTimeRunnable);
			sHandler.post(mUpdateDateTimeRunnable);
		}
	}
	
	private void stopUpdateTime(){
		if(null != mUpdateDateTimeRunnable){
			sHandler.removeCallbacks(mUpdateDateTimeRunnable);
		}
	}
	
	private UpdateDateTimeRunnable mUpdateDateTimeRunnable;
	private static Handler sHandler = new Handler();
	
	private void initDateTime(){
		if(null != mTxtDate)
			mTxtDate.setText(R.string.camerview_init_date);
		if(null != mTxtTime)
			mTxtTime.setText(R.string.camerview_init_time);
	}
	
	static class UpdateDateTimeRunnable implements Runnable{
		private WeakReference<TextView> mTxtDate, mTxtTime;
		static private final SimpleDateFormat sDateFormat = new SimpleDateFormat("MMM.dd.yyyy");
		static private final SimpleDateFormat sTimeFormat = new SimpleDateFormat("hh:mm:ss a");
		
		public UpdateDateTimeRunnable(TextView txtDate, TextView txtTime){
			mTxtDate = new WeakReference<TextView>(txtDate);
			mTxtTime = new WeakReference<TextView>(txtTime);
		}
		
		@Override
		public void run() {
			Date now = new Date();
			TextView txtDate = mTxtDate.get();
			if(null != txtDate){
				txtDate.setText(sDateFormat.format(now));
			}
			
			TextView txtTime = mTxtTime.get();
			if(null != txtTime){
				txtTime.setText(sTimeFormat.format(now));
			}
			
			sHandler.postDelayed(this, 1000L);
		}
	} 
	
	//For Streaming feature
	private native static boolean nativeClassInit();
	
	private native int openStreaming(int iDx,Surface s, String path, int iSeekOffset);
	private native int openStreamingList(int iDx,Surface s, String host, String[] streamList, int iSeekOffset);
	
	private native int addStreamingPath(int iDx, String path);
	private native int updateSurface(int iDx, Surface s);
	
	private native int pauseStreaming(int iDx);
	private native int resumeStreaming(int iDx);
	private native int closeStreaming(int iDx);
	
	private native int startRecord(int fd);
	private native int isRecording();
	private native void recordAudio(byte[] bytes, int bufSize);
	private native void endRecord();
	
	private native boolean receiveAudioBufFromCam(String strHost);
	private native boolean receiveAudioBufThreadRunning();
	private native boolean stopReceiveAudioBufThread();
	
    static {
    	System.loadLibrary("ffmpegutils");
    	if (!nativeClassInit())
			throw new RuntimeException("Native Init Failed");
    }
	
    public Bitmap getBitmapBySize(int iWidth, int iHeight){
    	return (null != mStreamingView)?mStreamingView.getBitmapBySize(iWidth, iHeight):null;
    } 
    
    static class CheckVideoBlockRunnable implements Runnable{
    	static private final long EXPIRE_VIDEO_BLOCK = 2000L;
		private WeakReference<CameraViewActivity> mAct;
		
		public CheckVideoBlockRunnable(CameraViewActivity act){
			mAct = new WeakReference<CameraViewActivity>(act);
		}
		
		@Override
		public void run() {
			final CameraViewActivity act = mAct.get();
			if(null != act && !act.isCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT)){
				Log.e(TAG, "CheckVideoBlockRunnable::run(), time to reconnect..............");
				act.closeStreaming();
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						act.beginLiveView();
					}}, 600L);
			}
		}
	} 
    
    final private static int COUNT_TO_START_CHECK_EXPIRE =30; 
    private int mCurCheckCount = 0;
    private CheckVideoBlockRunnable mCheckVideoBlockRunnable;
    
    public void drawStreamBitmap(){
    	if(null != mStreamingView)
    		mStreamingView.drawStreamBitmap();
    	mCurCheckCount++;
    	if(mCurCheckCount > COUNT_TO_START_CHECK_EXPIRE){
    		if(null == mCheckVideoBlockRunnable){
    			mCheckVideoBlockRunnable = new CheckVideoBlockRunnable(this);
    		}
    		BeseyeUtils.removeRunnable(mCheckVideoBlockRunnable);
    		BeseyeUtils.postRunnable(mCheckVideoBlockRunnable, CheckVideoBlockRunnable.EXPIRE_VIDEO_BLOCK);
    	}
    }
    
    private void cancelCheckVideoBlock(){
    	if(null != mCheckVideoBlockRunnable)
    		BeseyeUtils.removeRunnable(mCheckVideoBlockRunnable);
    }
    
    private void closeStreaming(){
    	cancelCheckVideoBlock();
    	closeStreaming(0);
    	mCurCheckCount = 0;
    }
    
    static class CheckVideoConnectionRunnable implements Runnable{
    	static private final long EXPIRE_VIDEO_CONN = 30000L;
		private WeakReference<CameraViewActivity> mAct;
		
		public CheckVideoConnectionRunnable(CameraViewActivity act){
			mAct = new WeakReference<CameraViewActivity>(act);
		}
		
		@Override
		public void run() {
			final CameraViewActivity act = mAct.get();
			if(null != act && act.getCamViewStatus().ordinal() < CameraView_Internal_Status.CV_STREAM_PLAYING.ordinal()){
				Log.e(TAG, "CheckVideoConnectionRunnable::run(), act.getCamViewStatus().ordinal():"+act.getCamViewStatus().ordinal());
				act.closeStreaming();
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						act.beginLiveView();
					}}, 1000L);
			}
		}
	} 
    
    private CheckVideoConnectionRunnable mCheckVideoConnectionRunnable;
    
    private void startCheckVideoConn(){
    	if(null == mCheckVideoConnectionRunnable)
    		mCheckVideoConnectionRunnable = new CheckVideoConnectionRunnable(this);
    	Log.i(TAG, "startCheckVideoConn()");
    	BeseyeUtils.postRunnable(mCheckVideoConnectionRunnable, CheckVideoConnectionRunnable.EXPIRE_VIDEO_CONN);
    }
    
    private void cancelCheckVideoConn(){
    	Log.i(TAG, "cancelCheckVideoConn()");
    	if(null != mCheckVideoConnectionRunnable)
    		BeseyeUtils.removeRunnable(mCheckVideoConnectionRunnable);
    }
    
 
    //Sync with rtmp.h
	enum Stream_Status{
		STREAM_UNINIT,
		STREAM_INIT,
		STREAM_CONNECTING,
		STREAM_CONNECTED,
		STREAM_PLAYING,
		STREAM_PAUSING,
		STREAM_PAUSED,
		STREAM_EOF,
		STREAM_CLOSE
	};
	
	enum Player_Major_Error {
		INTERNAL_STREAM_ERR,//refer to Stream_Error in rtmp.h
		NO_NETWORK_ERR,
		NOMEM_CB,
		UNKNOWN_ERR
	}

	enum Stream_Error{
		INVALID_APP_ERROR,
		INVALID_PATH_ERROR,
		INVALID_STREAM_ERROR,
		OPEN_STREAM_ERROR,
		STREAM_PLAY_ERROR,
		STREAM_CONNECTION_ERROR,
		SERVER_REQUEST_CLOSE_ERROR,
		NETWORK_CONNECTION_ERROR,
		UNKOWN_NETWORK_ERROR,
		UNKOWN_ERROR
	};
    
    public void updateRTMPStatus(int iType, String msg){
    	Log.w(TAG, "updateRTMPStatus(), iType:"+iType);
    	if(iType == Stream_Status.STREAM_INIT.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_INIT);
    	}else if(iType == Stream_Status.STREAM_CONNECTING.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING);
    	}else if(iType == Stream_Status.STREAM_CONNECTED.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTED);
    	}else if(iType == Stream_Status.STREAM_PLAYING.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_PLAYING);
    		updateStreamList(msg);
    	}else if(iType == Stream_Status.STREAM_PAUSING.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_PAUSING);
    	}else if(iType == Stream_Status.STREAM_PAUSED.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_PAUSED);
    	}else if(iType == Stream_Status.STREAM_EOF.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_EOF);
    	}else if(iType == Stream_Status.STREAM_CLOSE.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE);
    	}
    }
    
    private void updateStreamList(String strCurPlaying){
    	Log.w(TAG, "updateStreamList(), strCurPlaying="+strCurPlaying);
    	if(null != strCurPlaying){
    		synchronized(CameraViewActivity.this){
        		int iPos = 0;
        		//Log.e(TAG, "updateStreamList(), mstrDVRStreamPathList.size():"+mstrDVRStreamPathList.size());
        		for(; iPos < mstrDVRStreamPathList.size();iPos++){
        			String strStream = BeseyeJSONUtil.getJSONString(mstrDVRStreamPathList.get(iPos), BeseyeJSONUtil.MM_STREAM).replace("\\/", "/");
        			if(0 <= strStream.indexOf(strCurPlaying)){
        				Log.w(TAG, "updateStreamList(), find pos at "+iPos+", "+strStream);
        				break;
        			}
        			
        			//Log.w(TAG, "updateStreamList(), compare:\n["+strStream+"]\n["+strCurPlaying+"]");
        		}
        		
        		if(iPos == mstrDVRStreamPathList.size()){
        			Log.e(TAG, "updateStreamList(), can not find "+strCurPlaying);
        		}else{
        			for(int i = 0; i < iPos;i++){
        				mstrDVRStreamPathList.remove(0);
        			}
        		}
        	}
        	
        	if(3 >= mstrDVRStreamPathList.size()){
        		long startTime = findLastTimeFromList();
        		if(0 < startTime){
        			monitorAsyncTask(new BeseyeMMBEHttpTask.GetDVRStreamTask(this).setDialogId(-1), true, TMP_MM_VCAM_ID, startTime+"", DVR_REQ_TIME);
        		}else{
        			Log.e(TAG, "updateStreamList(), failed to get start time ");
        		}
        	}
    	}
    	
    }
    
    private long findLastTimeFromList(){
    	long lRet = 0;
    	synchronized(CameraViewActivity.this){
    		JSONObject target = null;
    		if(0 < mstrPendingStreamPathList.size()){
    			target = mstrPendingStreamPathList.get(mstrPendingStreamPathList.size()-1);
    		}else if(0 < mstrDVRStreamPathList.size()){
    			target = mstrDVRStreamPathList.get(mstrDVRStreamPathList.size()-1);
    		}
    		
    		if(null != target){
    			long lStartTime = BeseyeJSONUtil.getJSONLong(target, BeseyeJSONUtil.MM_START_TIME);
    			long lDuration = BeseyeJSONUtil.getJSONLong(target, BeseyeJSONUtil.MM_DURATION);
    			Log.i(TAG, "findLastTimeFromList(), lStartTime: "+lStartTime+", lDuration:"+lDuration+", traget:"+target.toString());
    			lRet = lStartTime + lDuration;
    		}else{
    			Log.e(TAG, "findLastTimeFromList(), can not find target");
    		}
    	}
    	return lRet;
    }
    
    private void appendStreamList(JSONArray streamList){
    	int iCount = streamList.length();
		synchronized(CameraViewActivity.this){
			JSONObject target = null;
    		if(0 < mstrPendingStreamPathList.size()){
    			target = mstrPendingStreamPathList.get(mstrPendingStreamPathList.size()-1);
    		}else if(0 < mstrDVRStreamPathList.size()){
    			target = mstrDVRStreamPathList.get(mstrDVRStreamPathList.size()-1);
    		}
    		
    		long lStartTime = 0, lEndTime = 0; 
    		String strStream= null;
    		if(null != target){
    			lStartTime = BeseyeJSONUtil.getJSONLong(target, BeseyeJSONUtil.MM_START_TIME);
    			lEndTime = lStartTime+BeseyeJSONUtil.getJSONLong(target, BeseyeJSONUtil.MM_DURATION);
    			strStream = BeseyeJSONUtil.getJSONString(target, BeseyeJSONUtil.MM_STREAM);
    			Log.i(TAG, "appendStreamList(), lStartTime: "+lStartTime+", lEndTime:"+lEndTime+", traget:"+target.toString());
    		}
    		
			for(int i = 0;i< iCount;i++){
				try {
					JSONObject check = streamList.getJSONObject(i);
					if(null != target){
						long lStartTimeCk = BeseyeJSONUtil.getJSONLong(check, BeseyeJSONUtil.MM_START_TIME);
		    			long lEndTimeCk = lStartTimeCk+BeseyeJSONUtil.getJSONLong(check, BeseyeJSONUtil.MM_DURATION);
		    			
		    			if(lStartTime == lStartTimeCk && lEndTime == lEndTimeCk && 
		    			   strStream.equals(BeseyeJSONUtil.getJSONString(check, BeseyeJSONUtil.MM_STREAM))){
		    				Log.w(TAG, "appendStreamList(), find duplicate item,  check:"+check.toString());
		    				continue;
		    			}
					}
					
					mstrPendingStreamPathList.add(check);
					//Log.e(TAG, "onPostExecute(), mstrLiveStreamServer:"+mstrLiveStreamServer+", mstrLiveStreamPathList[i]="+mstrDVRStreamPathList[i]);	
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
    }
   
    public void updateRTMPErrorCallback(final int iMajorType, final int iMinorType, final String msg){
    	if(null != sHandler){
    		sHandler.post(new Runnable(){
				@Override
				public void run() {
					mbNeedToCheckReddotNetwork = false;
					int iErrStrId = R.string.streaming_error_unknown;
					showInvalidStateMask();
					Log.w(TAG, "updateRTMPErrorCallback(), iMajorType:"+iMajorType+", iMinorType="+iMinorType+", msg="+msg);
					if(Player_Major_Error.INTERNAL_STREAM_ERR.ordinal() == iMajorType){
						if(Stream_Error.INVALID_APP_ERROR.ordinal() <= iMinorType && iMinorType <= Stream_Error.SERVER_REQUEST_CLOSE_ERROR.ordinal()){
							iErrStrId = R.string.streaming_playing_error;
							//showInvalidStateMask();
						}else if(Stream_Error.NETWORK_CONNECTION_ERROR.ordinal() <= iMinorType && iMinorType <= Stream_Error.UNKOWN_NETWORK_ERROR.ordinal()){
							iErrStrId = R.string.streaming_error_no_network;
						}
					}else if(Player_Major_Error.NO_NETWORK_ERR.ordinal() == iMajorType){
						Log.w(TAG, "updateRTMPErrorCallback(), NO_NETWORK_ERR");
						iErrStrId = R.string.streaming_error_no_network;
						//showInvalidStateMask();
					}else if(Player_Major_Error.NOMEM_CB.ordinal() == iMajorType){
						iErrStrId = R.string.streaming_error_low_mem;
					}else if(Player_Major_Error.UNKNOWN_ERR.ordinal() == iMajorType){
						iErrStrId = R.string.streaming_error_no_network;//workaround
					}
					
					if(!ASSIGN_ST_PATH){
						Bundle b = new Bundle();
						b.putString(KEY_WARNING_TEXT, getResources().getString(iErrStrId));
						showMyDialog(DIALOG_ID_WARNING, b);
				    	closeStreaming();
					}
					
				}});
    	}
    }
    
	//for SDL begin
	
    // Main components
    protected static CameraViewActivity mSingleton;
	
	 // C functions we call
    public static native void nativeInit();
    public static native void nativeLowMemory();
    public static native void nativeQuit();
    public static native void nativePause();
    public static native void nativeResume();
    public static native void onNativeResize(int x, int y, int format);
    public static native void onNativeKeyDown(int keycode);
    public static native void onNativeKeyUp(int keycode);
    public static native void onNativeKeyboardFocusLost();
    public static native void onNativeTouch(int touchDevId, int pointerFingerId,
                                            int action, float x, 
                                            float y, float p);
    public static native void onNativeAccel(float x, float y, float z);
    public static native void onNativeSurfaceChanged();
    public static native void onNativeSurfaceDestroyed();
    public static native void nativeFlipBuffers();

    public static void flipBuffers() {
        //SDLActivity.nativeFlipBuffers();
    }

    public static boolean setActivityTitle(String title) {
        // Called from SDLMain() thread and can't directly affect the view
        return false;//mSingleton.sendCommand(COMMAND_CHANGE_TITLE, title);
    }

    public static boolean sendMessage(int command, int param) {
        return false;//mSingleton.sendCommand(command, Integer.valueOf(param));
    }

    public static Context getContext() {
        return mSingleton;
    }
	
    public static boolean showTextInput(int x, int y, int w, int h) {
        // Transfer the task to the main thread as a Runnable
        return false;//mSingleton.commandHandler.post(new ShowTextInputTask(x, y, w, h));
    }
            
    public static Surface getNativeSurface() {
        return mSingleton.mStreamingView.getNativeSurface();
    }

    // Audio
    public static int audioInit(int sampleRate, boolean is16Bit, boolean isStereo, int desiredFrames) {
       return AudioChannelMgr.audioInit(sampleRate, is16Bit, isStereo, desiredFrames);
    }
    
    public static int getAudioBufSize(int iSampleRate){
    	return AudioChannelMgr.getAudioBufSize(iSampleRate);
    }
    
    public static void audioWriteShortBuffer(short[] buffer) {
    	AudioChannelMgr.audioWriteShortBuffer(buffer, buffer.length);
    }
    
    public static void audioWriteByteBuffer(byte[] buffer) {
    	AudioChannelMgr.audioWriteByteBuffer(buffer);
    }

    public static void audioQuit() {
    	AudioChannelMgr.audioQuit();
    }

    // Input

    /**
     * @return an array which may be empty but is never null.
     */
    public static int[] inputGetInputDeviceIds(int sources) {
        int[] ids = InputDevice.getDeviceIds();
        int[] filtered = new int[ids.length];
        int used = 0;
        for (int i = 0; i < ids.length; ++i) {
            InputDevice device = InputDevice.getDevice(ids[i]);
            if ((device != null) && ((device.getSources() & sources) != 0)) {
                filtered[used++] = device.getId();
            }
        }
        return Arrays.copyOf(filtered, used);
    }
	//for SDL end

	@Override
	public void onChannelConnecting() {
		Log.i(TAG, "onChannelConnecting()---");
	}

	@Override
	public void onAuthfailed() {
		Log.i(TAG, "onAuthfailed()---");
	}

	@Override
	public void onChannelConnected() {
		Log.i(TAG, "onChannelConnected()---");
	}

	@Override
	public void onMessageReceived(String msg) {
		Log.i(TAG, "onMessageReceived()---");
	}

	@Override
	public void onChannelClosed() {
		Log.i(TAG, "onChannelCloased()---");
	}
}
