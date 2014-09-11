package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeUtils.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.R;
import com.app.beseye.BeseyeBaseActivity.OnResumeUpdateCamInfoRunnable;
import com.app.beseye.TouchSurfaceView.CameraStatusCallback;
import com.app.beseye.TouchSurfaceView.OnTouchSurfaceCallback;
import com.app.beseye.audio.AudioChannelMgr;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.httptask.BeseyeNotificationBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.BeseyeNotificationBEHttpTask.GetAudioWSServerTask;
import com.app.beseye.setting.CameraSettingActivity;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeJSONUtil.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.websockets.AudioWebSocketsMgr;
import com.app.beseye.websockets.AudioWebSocketsMgr.OnAudioAmplitudeUpdateListener;
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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;

public class CameraViewActivity extends BeseyeBaseActivity implements OnTouchSurfaceCallback,
																	  OnNetworkChangeCallback,
																	  OnWSChannelStateChangeListener,
																	  OnAudioAmplitudeUpdateListener,
																	  CameraStatusCallback{
	static public final String KEY_PAIRING_DONE 	= "KEY_PAIRING_DONE";
	static public final String KEY_TIMELINE_INFO    = "KEY_TIMELINE_INFO";
	static public final String KEY_DVR_STREAM_MODE  = "KEY_DVR_STREAM_MODE";
	static public final String KEY_DVR_STREAM_TS    = "KEY_DVR_STREAM_TS";
	static public final String KEY_P2P_STREAM    	= "KEY_P2P_STREAM";
	static public final String KEY_P2P_STREAM_NAME  = "KEY_P2P_STREAM_NAME";
	static public final String DVR_REQ_TIME         = "60000";
	
	private TouchSurfaceView mStreamingView;
	private TextView mTxtPowerState;
	
	private RelativeLayout mVgHeader, mVgToolbar;
	private CameraViewControlAnimator mCameraViewControlAnimator;
	private ProgressBar mPbLoadingCursor;
	private ViewGroup mVgPowerState, mVgCamInvalidState, mVgPairingDone;
	private Button mBtnPairingDoneOK; 
	private ImageButton mIbOpenCam;
	private boolean mbVCamAdmin = true;
	
	private boolean mbIsLiveMode = true;//false means VOD
	private String mstrLiveP2P = null;
	private String mstrLiveStreamServer = null;
	private String mstrLiveStreamPath = null;
	private List<JSONObject> mstrDVRStreamPathList;
	private List<JSONObject> mstrPendingStreamPathList;
	private long mlDVRStartTs;
	private long mlDVRFirstSegmentStartTs = -1;
	private long mlRetryConnectBeginTs = -1;
	private TimeZone mTimeZone;
	
	
	private boolean mbIsDemoCam = false;
	private boolean mbIsFirstLaunch = true;
	private boolean mbIsRetryAtNextResume = false;
	private boolean mbIsPauseWhenPlaying = false;
	private boolean mbIsCamStatusChanged = false;
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
    	CV_STREAM_WAITING_UNPAUSE,
    	CV_STREAM_EOF,
    	CV_STREAM_WAITING_CLOSE,
    	CV_STREAM_CLOSE
    }
    
    private CameraView_Internal_Status mCamViewStatus = CameraView_Internal_Status.CV_STATUS_UNINIT;
    
    private CameraView_Internal_Status getCamViewStatus(){
    	return mCamViewStatus;	
    }
    
    private boolean isCamViewStatus(CameraView_Internal_Status status){
    	Log.i(TAG, "isCamViewStatus(), status:" + status+", mCamViewStatus:"+mCamViewStatus);
    	return mCamViewStatus == status;
    }
    
    private boolean isBetweenCamViewStatus(CameraView_Internal_Status statusFrom, CameraView_Internal_Status statusTo){
    	Log.i(TAG, "isBetweenCamViewStatus(), statusFrom:" + statusFrom+"-> statusTo:" + statusTo+", mCamViewStatus:"+mCamViewStatus);
    	return (statusFrom.ordinal() <= mCamViewStatus.ordinal() && mCamViewStatus.ordinal() <= statusTo.ordinal());
    }
    
    private void setCamViewStatus(final CameraView_Internal_Status status){
       	if(null != sHandler){
    		sHandler.post(new Runnable(){
				@Override
				public void run() {
					Log.i(TAG, "setCamViewStatus(), status:" + status);
			    	CameraView_Internal_Status precCamViewStatus = mCamViewStatus;
			    	
			    	switch(status){
			    		case CV_STATUS_UNINIT:{
			    			stopUpdateTime();
			    			if(null != mCameraViewControlAnimator){
				    			setEnabled(mCameraViewControlAnimator.getTalkView(), false);
				    			setEnabled(mCameraViewControlAnimator.getRewindView(), false);
				    			setEnabled(mCameraViewControlAnimator.getFastForwardView(), false);
				    			
				    			updatePlayPauseBtnEnabledStatus();
				    			updatePlayPauseBtnByStatus(status);
			    			}
			    			setVisibility(mPbLoadingCursor, View.GONE);
			    			hideInvalidStateMask();
			    			break;
			    		}
			    		case CV_STREAM_INIT:{
			    			initDateTime();
			    			aquireWakelock();
			    			if(null != mCameraViewControlAnimator){
			    				setEnabled(mCameraViewControlAnimator.getPlayPauseView(), false);
			    			}
			    			setVisibility(mPbLoadingCursor, View.VISIBLE);
			    			startCheckVideoConn();
			    			break;
			    		}
			    		case CV_STREAM_CONNECTING:{
			    			break;
			    		}
			    		case CV_STREAM_CONNECTED:{
			    			//BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, CAM_CONN_STATUS.CAM_ON.getValue());
			    			//setVisibility(mPbLoadingCursor, View.GONE);
			    			cancelCheckVideoConn();
			    			if(null != mCameraViewControlAnimator){
				    			setEnabled(mCameraViewControlAnimator.getTalkView(), mbIsLiveMode && !isInP2PMode() && !mbIsDemoCam);
				    			//setEnabled(mIbRewind, true);
				    			setEnabled(mCameraViewControlAnimator.getPlayPauseView(), true);
				    			//setEnabled(mIbFastForward, !mbIsLiveMode);
				    			//setVisibility(mPbLoadingCursor, View.GONE);
				    			
				    			updatePlayPauseBtnByStatus(status);
			    			}
			    			
			    			if(mbIsLiveMode)
			    				startUpdateTime();
			    			
			    			hideInvalidStateMask();
			    			
			    			if(!mActivityResume){
			    				Log.w(TAG, "mActivityResume is false when connected");
			    				closeStreaming();
			    			}
			    			
			    			if(null != mVgPairingDone && mVgPairingDone.getVisibility() == View.GONE){
			    				mVgPairingDone = null;
			    			}
			    			break;
			    		}
			    		case CV_STREAM_PLAYING:{
			    			if(!mActivityResume){
			    				Log.w(TAG, "mActivityResume is false when playing");
			    				closeStreaming();
			    			}else{
			    				//if(mCamViewStatus.equals(CameraView_Internal_Status.CV_STREAM_PAUSED)){
			    				
			    				updatePlayPauseBtnByStatus(status);
			    				
			    				if(null != mCameraViewControlAnimator){
				    				setEnabled(mCameraViewControlAnimator.getPlayPauseView(), true);
			    				}
			    				if(mbIsLiveMode){
			    					if(null == mCheckVideoBlockRunnable){
		    		        			mCheckVideoBlockRunnable = new CheckVideoBlockRunnable(CameraViewActivity.this);
		    		        		}
		    		        		BeseyeUtils.removeRunnable(mCheckVideoBlockRunnable);
		    		        		BeseyeUtils.postRunnable(mCheckVideoBlockRunnable, CheckVideoBlockRunnable.EXPIRE_VIDEO_NOT_START);
			    		    	}
			    			//}
			    			}
			    			
			    			break;
			    		}
			    		case CV_STREAM_WAITING_PAUSE:{
			    			setVisibility(mPbLoadingCursor, View.VISIBLE);
			    			break;
			    		}
			    		case CV_STREAM_PAUSED:{
			    			if(null != mCameraViewControlAnimator){
				    			setEnabled(mCameraViewControlAnimator.getRewindView(), false);
				    			setEnabled(mCameraViewControlAnimator.getPlayPauseView(), true);
				    			setEnabled(mCameraViewControlAnimator.getFastForwardView(), false);
				    			
				    			updatePlayPauseBtnByStatus(status);
			    			}
			    			setVisibility(mPbLoadingCursor, View.GONE);
			    			stopUpdateTime();
			    			
			    			if(!mActivityResume){
			    				Log.w(TAG, "mActivityResume is false when paused");
			    				closeStreaming();
			    			}
			    			break;
			    		}
			    		case CV_STREAM_WAITING_UNPAUSE:{
			    			setVisibility(mPbLoadingCursor, View.VISIBLE);
			    			if(null != mCameraViewControlAnimator){
			    				setEnabled(mCameraViewControlAnimator.getPlayPauseView(), false);
			    			}
			    			break;
			    		}
			    		case CV_STREAM_EOF:{
			    			if(!precCamViewStatus.equals(CameraView_Internal_Status.CV_STREAM_EOF)){
			    				setVisibility(mPbLoadingCursor, View.VISIBLE);
				    			stopUpdateTime();
				    			if(mbIsLiveMode)
					    			BeseyeUtils.postRunnable(new Runnable(){
										@Override
										public void run() {
											tryToReconnect();
										}}, 0);
			    			}
			    			
			    			if(!mActivityResume){
			    				Log.w(TAG, "mActivityResume is false when eof");
			    				closeStreaming();
			    			}
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
			    			
			    			if(null != mCameraViewControlAnimator && mCameraViewControlAnimator.isInHoldToTalkMode()){
								mCameraViewControlAnimator.terminateTalkMode();
							}
			    			
			    			if(null != mReStartRunnable){
								BeseyeUtils.postRunnable(mReStartRunnable, 600L);
								mReStartRunnable = null;
							}
			    			break;
			    		}
			    		default:{
			    			Log.e(TAG, "setCamViewStatus(), invalid type:" + mCamViewStatus);
			    			return;
			    		}
			    	}
			    	mCamViewStatus = status;
				}});
    	}
    }
    
    private void updatePlayPauseBtnByStatus(CameraView_Internal_Status status){
    	Configuration config = getResources().getConfiguration();	
    	if(null != mCameraViewControlAnimator){
    		if((CameraView_Internal_Status.CV_STATUS_UNINIT.ordinal() <= status.ordinal() && status.ordinal() <= CameraView_Internal_Status.CV_STREAM_CONNECTING.ordinal()) || 
    		   CameraView_Internal_Status.CV_STREAM_PAUSED == status ){
    			setImageRes(mCameraViewControlAnimator.getPlayPauseView(), Configuration.ORIENTATION_LANDSCAPE == config.orientation?R.drawable.sl_liveview_play_btn:R.drawable.sl_liveview_play_btn_round);
    			setImageRes(mCameraViewControlAnimator.getPlayPauseViewOrient(), Configuration.ORIENTATION_PORTRAIT == config.orientation?R.drawable.sl_liveview_play_btn:R.drawable.sl_liveview_play_btn_round);
    		}else{
    			setImageRes(mCameraViewControlAnimator.getPlayPauseView(), Configuration.ORIENTATION_LANDSCAPE == config.orientation?R.drawable.sl_liveview_pause_btn:R.drawable.sl_liveview_pause_btn_round);
    			setImageRes(mCameraViewControlAnimator.getPlayPauseViewOrient(), Configuration.ORIENTATION_PORTRAIT == config.orientation?R.drawable.sl_liveview_pause_btn:R.drawable.sl_liveview_pause_btn_round);
    		}
		}
    }
    
    private void tryToReconnect(){
    	Log.i(TAG, "CameraViewActivity::tryToReconnect(), mActivityResume:"+mActivityResume+", mActivityDestroy:"+mActivityDestroy);
    	//if(!isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_INIT, CameraView_Internal_Status.CV_STREAM_PAUSED)){
    		int iReOpenDelay = 0;
    		if(!isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_WAITING_CLOSE, CameraView_Internal_Status.CV_STREAM_CLOSE) || 
    		   !isCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT)){
    			closeStreaming();
    			iReOpenDelay = 500;
    		}
    		
    		if(mActivityResume){
    			BeseyeUtils.postRunnable(new Runnable(){
    				@Override
    				public void run() {
    					beginLiveView();
    				}}, iReOpenDelay);
    		}else if(!mActivityDestroy){
    			mbIsPauseWhenPlaying = true;
    		}
    		
    		if(null != mVgPairingDone){
    			setVisibility(mPbLoadingCursor, View.VISIBLE);
    		}
    	//}
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "CameraViewActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
//		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		WindowManager.LayoutParams attributes = getWindow().getAttributes();
		attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
		getWindow().setAttributes(attributes);
		
		getSupportActionBar().hide();
		
		updateAttrByIntent(getIntent());
		
		mVgHeader = (RelativeLayout)findViewById(R.id.vg_streaming_view_header);
		if(null != mVgHeader){
			mVgHeader.setOnClickListener(this);
		}
		
		mVgToolbar = (RelativeLayout)findViewById(R.id.vg_streaming_view_footer);
		if(null != mVgToolbar){
			mVgToolbar.setOnClickListener(this);
		}
		
		mStreamingView = (TouchSurfaceView)findViewById(R.id.surface_streaming_view);
		if(null != mStreamingView){
			mStreamingView.registerSingleTapCallback(this);
			mStreamingView.registerCameraStatusCallback(this);
		}
		
		mVgPowerState = (ViewGroup)findViewById(R.id.vg_cam_power_state);
		if(null != mVgPowerState){
			mTxtPowerState = (TextView)mVgPowerState.findViewById(R.id.txt_cam_power_state);
			mIbOpenCam = (ImageButton)mVgPowerState.findViewById(R.id.ib_open_cam);
			if(null != mIbOpenCam){
				mIbOpenCam.setOnClickListener(this);
			}
		}
		
		mVgCamInvalidState = (ViewGroup)findViewById(R.id.vg_cam_invald_statement);
		if(null != mVgCamInvalidState && isInP2PMode()){
			mVgCamInvalidState.setVisibility(View.GONE);
		}
		
		mPbLoadingCursor = (ProgressBar)findViewById(R.id.pb_loadingCursor);
		
		mCameraViewControlAnimator = new CameraViewControlAnimator(this, mVgHeader, mVgToolbar,(ViewGroup)findViewById(R.id.vg_hold_to_talk), getStatusBarHeight(this));
		if(null != mCameraViewControlAnimator){
			mCameraViewControlAnimator.setP2PMode(isInP2PMode());
			
			Configuration config = getResources().getConfiguration();		
			setMarginByOrientation(config.orientation);
			
			ImageView ivStreamType =  mCameraViewControlAnimator.getStremTypeView();
			if(null != ivStreamType){
				ivStreamType.setImageResource((mbIsLiveMode || this.isInP2PMode())?R.drawable.liveview_h_display_icon:R.drawable.liveview_xhdpi_h_event_icon);
			}
			
			TextView txtGoLive = mCameraViewControlAnimator.getGoLiveView();
			if(null != txtGoLive){
				txtGoLive.setEnabled(!mbIsLiveMode);
			}
		}
		
		applyCamAttr();
		
		mSingleton = this;
		
		setCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT);
		
		AudioWebSocketsMgr.getInstance().registerOnWSChannelStateChangeListener(this);
		AudioWebSocketsMgr.getInstance().registerOnAudioAmplitudeUpdateListener(this);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		Log.i(TAG, "CameraViewActivity::onNewIntent()");
		super.onNewIntent(intent);
		updateAttrByIntent(intent);
	}
	
	private boolean isInP2PMode(){
		return null != mstrLiveP2P && 0 < mstrLiveP2P.length();
	}
	
	private void updateAttrByCamObj(){
		if(null != mCam_obj){
			Log.i(TAG, "CameraViewActivity::updateAttrByCamObj(), mCam_obj:"+mCam_obj.toString());
			mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			mTimeZone = TimeZone.getTimeZone(BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA), BeseyeJSONUtil.CAM_TZ, TimeZone.getDefault().getID()));
			if(null != mUpdateDateTimeRunnable){
				mUpdateDateTimeRunnable.updateTimeZone(mTimeZone);
			}
			mbVCamAdmin = BeseyeJSONUtil.getJSONBoolean(mCam_obj, BeseyeJSONUtil.ACC_SUBSC_ADMIN, true);
			
			if(null != mCameraViewControlAnimator){
				TextView txtCamName = mCameraViewControlAnimator.getCamNameView();
				if(null != txtCamName){
					txtCamName.setText(mStrVCamName);
				}
			}
		}
	}
	
	private void updateAttrByIntent(Intent intent){
		if(null != intent){
			mstrLiveP2P = intent.getStringExtra(KEY_P2P_STREAM);
			if(null != mstrLiveP2P && 0 < mstrLiveP2P.length()){
				mbIsLiveMode = true;
				//mStrVCamName = intent.getStringExtra(KEY_P2P_STREAM_NAME);
				try {
					mCam_obj = new JSONObject(intent.getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
					if(null != mCam_obj){
						mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
						mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
						mTimeZone = TimeZone.getTimeZone(BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA), BeseyeJSONUtil.CAM_TZ, TimeZone.getDefault().getID()));
						if(null != mUpdateDateTimeRunnable){
							mUpdateDateTimeRunnable.updateTimeZone(mTimeZone);
						}
						//BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, CAM_CONN_STATUS.CAM_ON.getValue());
					}
				} catch (JSONException e1) {
					Log.e(TAG, "CameraViewActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
				}
				
				Log.i(TAG, "CameraViewActivity::updateAttrByIntent(), enter p2p mode");
			}else{
				
				try {
					mCam_obj = new JSONObject(intent.getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
					updateAttrByCamObj();
				} catch (JSONException e1) {
					Log.e(TAG, "CameraViewActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
				}
				
				mbIsDemoCam = intent.getBooleanExtra(CameraListActivity.KEY_DEMO_CAM_MODE, false);
				mbIsLiveMode = !intent.getBooleanExtra(KEY_DVR_STREAM_MODE, false);
				mstrDVRStreamPathList = new ArrayList<JSONObject>();
				mstrPendingStreamPathList = new ArrayList<JSONObject>();
				
				Log.e(TAG, "CameraViewActivity::updateAttrByIntent(), mbIsDemoCam:"+mbIsDemoCam);
				
				String strTsInfo = intent.getStringExtra(KEY_TIMELINE_INFO);
				if(null != strTsInfo && 0 < strTsInfo.length()){
					try {
						JSONObject tsInfo = new JSONObject(strTsInfo);
						Log.i(TAG, "CameraViewActivity::updateAttrByIntent(), tsInfo:"+tsInfo.toString());
						if(null != tsInfo && false == BeseyeJSONUtil.getJSONBoolean(tsInfo, BeseyeJSONUtil.MM_IS_LIVE, false)){
							mbIsLiveMode = false;
							mlDVRStartTs = BeseyeJSONUtil.getJSONLong(tsInfo, BeseyeJSONUtil.MM_START_TIME);
							Log.i(TAG, "CameraViewActivity::onCreate(), mlDVRStartTs="+mlDVRStartTs);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				if(intent.getBooleanExtra(KEY_PAIRING_DONE, false)){
					mlRetryConnectBeginTs = System.currentTimeMillis();
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
							//BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, CAM_CONN_STATUS.CAM_ON.getValue());
							setVisibility(mPbLoadingCursor, View.VISIBLE);
						}
					}	
					getIntent().putExtra(CameraViewActivity.KEY_PAIRING_DONE, false);
				}
			}
		}
	}
	
	private void goToLiveMode(){
//		Intent intent = getIntent();
//		if(null != intent){
//			intent.putExtra(KEY_TIMELINE_INFO, "");
//		}
//		
//		mbIsLiveMode = true;
//		mlDVRStartTs = 0;
//		
//		updateUIByMode();
//		
//		getStreamingInfo(true);
		
		Bundle b = new Bundle();
		b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
		b.putBoolean(CameraListActivity.KEY_DEMO_CAM_MODE, mbIsDemoCam);
		launchActivityByClassName(CameraViewActivity.class.getName(), b);
	}
	
	private void updateUIByMode(){		
		if(null != mCameraViewControlAnimator){
			BeseyeUtils.setEnabled(mCameraViewControlAnimator.getGoLiveView(), !mbIsLiveMode);
			BeseyeUtils.setEnabled(mCameraViewControlAnimator.getEventsView(), !isInP2PMode());
			BeseyeUtils.setVisibility(mCameraViewControlAnimator.getSettingView(), ((!COMPUTEX_DEMO || BeseyeConfig.COMPUTEX_PAIRING) && !isInP2PMode() && mbVCamAdmin && !mbIsDemoCam)?View.VISIBLE:View.INVISIBLE);
		}
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
			Configuration config = getResources().getConfiguration();		
			setMarginByOrientation(config.orientation);
		}
		
		checkAndExtendHideHeader();
		
		if(!mbFirstResume || mbIsRetryAtNextResume){
			triggerPlay();
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA)){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
		}
		triggerPlay();
	}
	
	private void triggerPlay(){
		//Workaround for can not get down stream after switching ss
		Log.i(TAG, "CameraViewActivity::triggerPlay(), mbIsFirstLaunch:"+mbIsFirstLaunch+", mbIsPauseWhenPlaying:"+mbIsPauseWhenPlaying+", mbIsCamSettingChanged:"+mbIsCamSettingChanged+", mbIsWifiSettingChanged:"+mbIsWifiSettingChanged);
		mlRetryConnectBeginTs = System.currentTimeMillis();
		
		//if(false == handleReddotNetwork(false)){
			if(null != mStreamingView)
				mStreamingView.setUpsideDown(BeseyeJSONUtil.getJSONInt(mCam_obj, CameraListActivity.KEY_VCAM_UPSIDEDOWN, 0) == 1);
		//}
		
		if(null != mVgCamInvalidState){
			mVgCamInvalidState.setVisibility(View.GONE);
			if(isCamPowerDisconnected()){
				//BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, CAM_CONN_STATUS.CAM_ON.getValue());
				mbIsCamSettingChanged = true;
				Log.d(TAG, "CameraViewActivity::triggerPlay(), make mbIsCamSettingChanged: true.............");
			}
		}
		
		checkPlayState();
		initDateTime();
	}
	
//	private boolean handleReddotNetwork(boolean bForceShow){
//		boolean bRet = false;
//		Log.i(TAG, "CameraViewActivity::handleReddotNetwork(), isWifiEnabled:"+NetworkMgr.getInstance().isWifiEnabled()+
//															  ",getActiveWifiSSID:"+NetworkMgr.getInstance().getActiveWifiSSID()+
//															  ",bForceShow:"+bForceShow);
//		if(REDDOT_DEMO && NetworkMgr.getInstance().isWifiEnabled() && RELAY_AP_SSID.equals(NetworkMgr.getInstance().getActiveWifiSSID())){
//			if(bForceShow){
//				Log.i(TAG, "CameraViewActivity::handleReddotNetwork(), cannot contact AP --------------");
//				showInvalidStateMask();
//				BeseyeUtils.postRunnable(new Runnable(){
//					@Override
//					public void run() {
//						beginLiveView();
//					}}, 600L);
//			}
//		}else if(REDDOT_DEMO && (bForceShow ||(!NetworkMgr.getInstance().isWifiEnabled() || !RELAY_AP_SSID.equals(NetworkMgr.getInstance().getActiveWifiSSID())))){
//			Log.i(TAG, "CameraViewActivity::handleReddotNetwork(), launch wifi list --------------");
//			Intent intent = new Intent();
//			intent.setClass(this, WifiListActivity.class);
//			intent.putExtra(WifiListActivity.KEY_CHANGE_WIFI_ONLY, true);
//			startActivityForResult(intent, REQUEST_WIFI_SETTING_CHANGED);
//			bRet = true;
//		}
//		return bRet;
//	}
	
	private static class PauseCameraViewRunnable implements Runnable{
		WeakReference<CameraViewActivity> mCameraViewActivity;
		
		PauseCameraViewRunnable(CameraViewActivity act){
			mCameraViewActivity = new WeakReference<CameraViewActivity>(act);
		}
		@Override
		public void run() {
			CameraViewActivity act = mCameraViewActivity.get();
			if(null != act){
				if(act.isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING, CameraView_Internal_Status.CV_STREAM_PLAYING)){
					Log.d(TAG, "CameraViewActivity::onPause()->run(), pause when playing");
					act.mbIsPauseWhenPlaying = true;
				}
				//Close it because pause not implement
				act.closeStreaming();
				act.stopUpdateTime();
				act.releaseWakelock();
				NetworkMgr.getInstance().unregisterNetworkChangeCallback(act);
				act.mCameraViewControlAnimator.cancelHideControl();
				
				if(AudioWebSocketsMgr.getInstance().isWSChannelAlive()){
					AudioWebSocketsMgr.getInstance().destroyWSChannel();
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
		mlStartLogoutTs = -1;
		
		closeAudioChannel(true);
		
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "CameraViewActivity::onDestroy()");
		
		AudioWebSocketsMgr.getInstance().unregisterOnAudioAmplitudeUpdateListener();
		AudioWebSocketsMgr.getInstance().unregisterOnWSChannelStateChangeListener();
		if(null != mStreamingView){
			mStreamingView.unregisterCameraStatusCallback();
		}
		
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
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i(TAG, "onConfigurationChanged " + newConfig);
		super.onConfigurationChanged(newConfig);
		
		setMarginByOrientation(newConfig.orientation);
		
		if(null != mStreamingView){
			mStreamingView.onOrientationChanged();
		}
	}
	
	private void setMarginByOrientation(int iOrient){
		if(null != mStreamingView){
			mStreamingView.drawDefaultBackground();
		}
		
		if(null != mCameraViewControlAnimator){
			mCameraViewControlAnimator.setOrientation(iOrient);
			updatePlayPauseBtnByStatus(mCamViewStatus);
			if(null != mUpdateDateTimeRunnable)
				mUpdateDateTimeRunnable.updateDateTimeView(mCameraViewControlAnimator.getDateView(), mCameraViewControlAnimator.getTimeView());
			else
				mUpdateDateTimeRunnable = new UpdateDateTimeRunnable(mCameraViewControlAnimator.getDateView(), mCameraViewControlAnimator.getTimeView(), mTimeZone);
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
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_CAM_TALK_INIT:{
				dialog = ProgressDialog.show(this, "", getString(R.string.dialog_init_cam), true, true);
				dialog.setCancelable(false);
				//TODO: avoid this dialog infinite showing
				break;
			}
			case DIALOG_ID_WARNING:{
				dialog = super.onCreateDialog(id, bundle);
				if(null != dialog){
					Log.w(TAG, "CameraViewActivity::onDismiss()");
//					if(bundle.getBoolean(KEY_WARNING_CLOSE, false)){
//						((android.app.AlertDialog)dialog).setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.ok), new DialogInterface.OnClickListener() {
//					    public void onClick(DialogInterface dialog, int item) {
//					    	removeMyDialog(DIALOG_ID_WARNING);
//					    	finish();
//					    }
//					});
//					}
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							Log.w(TAG, "CameraViewActivity::onDismiss(), mbNeedToCheckReddotNetwork = "+mbNeedToCheckReddotNetwork);
//							if(mbNeedToCheckReddotNetwork)
//								handleReddotNetwork(true);
							mbNeedToCheckReddotNetwork = false;
							
							if(null != mCameraViewControlAnimator){
								mCameraViewControlAnimator.showControl();
							}
							checkAndExtendHideHeader();
							
//							if(BeseyeConfig.COMPUTEX_DEMO && bundle.getBoolean(KEY_WARNING_CLOSE, false)){
//								finish();
//							}
							tryToReconnect();
							
						}});
					BeseyeUtils.postRunnable(new Runnable(){

						@Override
						public void run() {
							tryToReconnect();
						}}, 200);
					
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
				Log.i(TAG, "CameraViewActivity::showInvalidStateMask()");
				if(null != mVgCamInvalidState && isCamPowerDisconnected()){
					mVgCamInvalidState.setVisibility(View.VISIBLE);
				}
			}});
	}
	
	private void hideInvalidStateMask(){
		//mbNeedToCheckReddotNetwork = false;
		Log.i(TAG, "CameraViewActivity::hideInvalidStateMask()");
		sHandler.post(new Runnable(){
			@Override
			public void run() {
				if(null != mVgCamInvalidState && !isCamPowerDisconnected()){
					mVgCamInvalidState.setVisibility(View.GONE);
				}
			}});
	}
	
	private void checkPlayState(){
		boolean bPowerOn = isCamPowerOn();
		boolean bNetworkConnected = NetworkMgr.getInstance().isNetworkConnected();
		
		Log.i(TAG, "CameraViewActivity::checkPlayState(), bPowerOn:"+bPowerOn+", bNetworkConnected:"+bNetworkConnected);
		
		if(bNetworkConnected && bPowerOn && (mbIsFirstLaunch || mbIsPauseWhenPlaying || mbIsCamSettingChanged || mbIsWifiSettingChanged || mbIsRetryAtNextResume)){
			mbIsRetryAtNextResume = false;
			getStreamingInfo(true);
		}/*else{
			setCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT);
		}*/
		
		updatePlayPauseBtnEnabledStatus();
		
		if(!bNetworkConnected){
			showNoNetworkDialog();
		}
		
		updatePowerState();
		mbIsFirstLaunch = mbIsPauseWhenPlaying = false;
		
		if(null != mStreamingView){
			mStreamingView.drawDefaultBackground();
		}
	}
	
	private void applyCamAttr(){		
		if(null != mCameraViewControlAnimator){
			ImageButton ibSetting = mCameraViewControlAnimator.getSettingView();
			if(null != ibSetting)
				ibSetting.setVisibility(((!COMPUTEX_DEMO || BeseyeConfig.COMPUTEX_PAIRING) && !isInP2PMode() && mbVCamAdmin && !mbIsDemoCam)?View.VISIBLE:View.INVISIBLE);
			
			TextView txtCamName = mCameraViewControlAnimator.getCamNameView();
			if(null != txtCamName){
				txtCamName.setText(mStrVCamName);
			}
		}
	}
	
	private BeseyeHttpTask mLiveStreamTask = null;
	private BeseyeHttpTask mDVRStreamTask = null;
	private BeseyeHttpTask mDVRStreamAppendTask = null;
	
	private void getStreamingInfo(boolean bShowDialog){
		if(false == isInP2PMode()){
			if(mbIsLiveMode){
				if(null == mLiveStreamTask){
					monitorAsyncTask(mLiveStreamTask = new BeseyeMMBEHttpTask.GetLiveStreamTask(this, -1).setDialogId(bShowDialog?DIALOG_ID_LOADING:-1), true, (null != mStrVCamID)?mStrVCamID:TMP_MM_VCAM_ID, "false");
					setVisibility(mPbLoadingCursor, View.VISIBLE);
				}
			}else{
				if(null == mDVRStreamTask){
					monitorAsyncTask(mDVRStreamTask = new BeseyeMMBEHttpTask.GetDVRStreamTask(this).setDialogId(DIALOG_ID_LOADING), true, (null != mStrVCamID)?mStrVCamID:TMP_MM_VCAM_ID, mlDVRStartTs+"", DVR_REQ_TIME);
					mlDVRFirstSegmentStartTs = -1;
					setVisibility(mPbLoadingCursor, View.VISIBLE);
				}
			}
		}else{
			Log.d(TAG, "CameraViewActivity::getStreamingInfo(), play p2p stream:"+mstrLiveP2P);
			updateUIByMode();
			beginLiveView();
		}
	}
	
	private void updatePlayPauseBtnEnabledStatus(){
		if(null != mCameraViewControlAnimator){
			setEnabled(mCameraViewControlAnimator.getPlayPauseView(), (null == mLiveStreamTask && null == mDVRStreamTask) && isCamPowerOn() && NetworkMgr.getInstance().isNetworkConnected());
		}
		
		if(null != mCameraViewControlAnimator){
			setEnabled(mCameraViewControlAnimator.getTalkView(), isCamPowerOn() && NetworkMgr.getInstance().isNetworkConnected() && isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTED, CameraView_Internal_Status.CV_STREAM_PAUSED));
		}
	}
	
	private boolean isCamPowerOn(){
		return !mbIsLiveMode || BeseyeJSONUtil.isCamPowerOn(mCam_obj);
	}
	
	private boolean isCamPowerOff(){
		return mbIsLiveMode && BeseyeJSONUtil.isCamPowerOff(mCam_obj);
	}
	
	private boolean isCamPowerDisconnected(){
		return mbIsLiveMode && BeseyeJSONUtil.isCamPowerDisconnected(mCam_obj);
	}
	
	private void updatePowerState(){
		if(null != mVgPowerState){
			if(isCamPowerDisconnected()){
				showInvalidStateMask();
			}else{
				hideInvalidStateMask();
				if(isCamPowerOff()){
					mVgPowerState.setVisibility(View.VISIBLE);
				}else{
					mVgPowerState.setVisibility(View.GONE);
				}
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
						AudioWebSocketsMgr.getInstance().setVCamId(mStrVCamID);
						AudioWebSocketsMgr.getInstance().setAudioWSServerIP("http://"+arr.getString(arr.length()-1));
						AudioWebSocketsMgr.getInstance().setSienceFlag(false);
						AudioWebSocketsMgr.getInstance().constructWSChannel();
					} catch (JSONException e) {
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
						mbIsCamStatusChanged = false;
						mbIsCamSettingChanged = false;
					}
				}else if(ASSIGN_ST_PATH){
					//Workaround
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
							
							if(0 < mstrPendingStreamPathList.size()){
								new Thread(new Runnable(){
									@Override
									public void run() {
										if(isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING, CameraView_Internal_Status.CV_STREAM_WAITING_CLOSE)){
			            					Log.i(TAG, "append list mstrDVRStreamPathList.size():"+mstrDVRStreamPathList.size());
			            					synchronized(CameraViewActivity.this){
				   	                     		while(0 < mstrPendingStreamPathList.size()){
				   	                     			String stream =  BeseyeJSONUtil.getJSONString(mstrPendingStreamPathList.get(0), BeseyeJSONUtil.MM_STREAM);
				   	                     			if(0 > addStreamingPath(0,stream)){
				   	                     				Log.i(TAG, "failed to append stream:"+stream);
				   	                     				mstrPendingStreamPathList.clear();
				   	                     				break;
				   	                     			}
				   	                     			mstrDVRStreamPathList.add(mstrPendingStreamPathList.remove(0));
				   	                     		}
			                        		}
			            				}else
				            				beginLiveView();
									}}).start();
								
	            			}else{
	            				if(isCamViewStatus(CameraView_Internal_Status.CV_STATUS_UNINIT)){
		            				Bundle b = new Bundle();
		        					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.streaming_invalid_dvr));
		        					b.putBoolean(KEY_WARNING_CLOSE, true);
		        					showMyDialog(DIALOG_ID_WARNING, b);
	            				}
	            			}
						}
					}
					mbIsCamStatusChanged = false;
					mbIsCamSettingChanged = false;
				}
			}else if(task instanceof BeseyeAccountTask.GetCamInfoTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					JSONObject vcamObj = BeseyeJSONUtil.getJSONObject(result.get(0), BeseyeJSONUtil.ACC_VCAM);
					if(null != vcamObj){
						mStrVCamName = BeseyeJSONUtil.getJSONString(vcamObj, BeseyeJSONUtil.ACC_NAME);
						//mbVCamAdmin  = BeseyeJSONUtil.getJSONBoolean(vcamObj, BeseyeJSONUtil.ACC_SUBSC_ADMIN);
						applyCamAttr();
						Log.e(TAG, "onPostExecute(), mStrVCamID:"+mStrVCamID);
					}
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetCamStatusTask){
				if(0 == iRetCode){
					BeseyeJSONUtil.setVCamConnStatus(mCam_obj, (BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.CAM_STATUS)==1)?CAM_CONN_STATUS.CAM_ON:CAM_CONN_STATUS.CAM_OFF);
					BeseyeJSONUtil.setJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					//BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, BeseyeJSONUtil.CAM_CONN_STATUS.CAM_ON.getValue());
					//setActivityResultWithCamObj();
					
					mbIsCamSettingChanged = true;
					mbIsCamStatusChanged = true;
					mlRetryConnectBeginTs = System.currentTimeMillis();
					checkPlayState();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					super.onPostExecute(task, result, iRetCode);
					if(isCamPowerOn() && isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING, CameraView_Internal_Status.CV_STREAM_PAUSED)){
						closeStreaming();
					}
					mbIsCamSettingChanged = true;
					updateAttrByCamObj();
					checkPlayState();
				}
			}else{
				//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
		
		if(task == mLiveStreamTask){
			mLiveStreamTask = null;
		}else if(task == mDVRStreamTask){
			mDVRStreamTask = null;
		}else if(task == mDVRStreamAppendTask){
			mDVRStreamAppendTask = null;
		}else if(task == mGetAudioWSServerTask){
			mGetAudioWSServerTask = null;
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeMMBEHttpTask.GetLiveStreamTask){
			if(mActivityDestroy){
				return;
			}
			
			if(!mActivityResume){
				mbIsRetryAtNextResume = true;
				return;
			}
			
			if(/*(null != mVgPairingDone || mbIsCamStatusChanged || mbIsCamSettingChanged) &&*/ (System.currentTimeMillis() - mlRetryConnectBeginTs < 60*1000) && mActivityResume){
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						getStreamingInfo(false);
					}}, 1000);
			}else{
				mbIsRetryAtNextResume = true;
				mbIsCamStatusChanged = false;
				mbIsCamSettingChanged = false;
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						if(isCamPowerOn()){
							Bundle b = new Bundle();
							b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.streaming_playing_error));
							b.putBoolean(KEY_WARNING_CLOSE, true);
							showMyDialog(DIALOG_ID_WARNING, b);
							updatePlayPauseBtnEnabledStatus();
							setVisibility(mPbLoadingCursor, View.GONE);
						}
					}}, 0);
			}
			return;
		}else if(task instanceof BeseyeMMBEHttpTask.GetDVRStreamTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.streaming_invalid_dvr));
					b.putBoolean(KEY_WARNING_CLOSE, true);
					showMyDialog(DIALOG_ID_WARNING, b);
					updatePlayPauseBtnEnabledStatus();
					setVisibility(mPbLoadingCursor, View.GONE);
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetCamStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_update_cam_status));
					showMyDialog(DIALOG_ID_WARNING, b);

				}}, 0);
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	protected int getLayoutId(){
		return R.layout.layout_camera_view;
	}
	
	private long mlStartLogoutTs = -1;
	private int mlStartLogoutClickCount= 0;

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
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				b.putBoolean(CameraListActivity.KEY_DEMO_CAM_MODE, mbIsDemoCam);
				launchActivityByClassName(EventListActivity.class.getName(), b);
				break;
			}
			case R.id.txt_go_live:{
				if(isCamViewStatus(CameraView_Internal_Status.CV_STREAM_PLAYING))
					closeStreaming();
				goToLiveMode();
				break;
			}
			case R.id.iv_back:{
				finish();
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
				if(mbIsLiveMode){
					if(isCamViewStatus(CameraView_Internal_Status.CV_STREAM_PLAYING) || isCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTED)){
						closeStreaming();
					}else{
						//beginLiveView();
						getStreamingInfo(true);
					}
				}else{
					if(isCamViewStatus(CameraView_Internal_Status.CV_STREAM_PLAYING) || isCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTED)){
						if(0 <= miStreamIdx){
							pauseStreaming(miStreamIdx);
						}
					}else if(isCamViewStatus(CameraView_Internal_Status.CV_STREAM_PAUSED)){
						if(0 <= miStreamIdx){
							if(0 <=resumeStreaming(miStreamIdx))
								setCamViewStatus(CameraView_Internal_Status.CV_STREAM_WAITING_UNPAUSE);
						}
					}else{
						//beginLiveView();
						getStreamingInfo(true);
					}
				}
				
				break;
			}
			case R.id.ib_fast_forward:{
				break;
			}
			case R.id.ib_settings:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
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
				if(isInP2PMode()){
					if(-1 == mlStartLogoutTs){
						mlStartLogoutTs = System.currentTimeMillis();
						mlStartLogoutClickCount = 0;
					}
					
					if(++mlStartLogoutClickCount >=5 && (System.currentTimeMillis() - mlStartLogoutTs) <= 7*1000){
						invokeLogout();
					}
				}
				break;
			}
			case R.id.ib_open_cam:{
				monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamStatusTask(this), true, mStrVCamID, "1");
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
		Log.w(TAG, "CameraViewActivity::onActivityResult(), requestCode:"+requestCode+", resultCode:"+resultCode);
		if(REQUEST_CAM_SETTING_CHANGED == requestCode){
			mbIsCamSettingChanged = (resultCode == RESULT_OK);
			if(resultCode == RESULT_OK){
				//monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this), true, mStrVCamID);
				updateAttrByIntent(intent);
				setResult(RESULT_OK, intent);
			}
		}else if(REQUEST_WIFI_SETTING_CHANGED== requestCode){
			if(resultCode == RESULT_OK){
				mbIsWifiSettingChanged = true;
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void onSingleTapConfirm() {
		if(null != mCameraViewControlAnimator && false == mCameraViewControlAnimator.isInAnimation()){
			if(View.VISIBLE != mCameraViewControlAnimator.getVisibility()){
				removePrepareToHideStatusBar();
			}
			mCameraViewControlAnimator.performControlAnimation();
		}
	}
	
	@Override
	public void onDoubleTapConfirm() {
		if(null != mCameraViewControlAnimator)
			mCameraViewControlAnimator.hideControl();
	}
	
	@Override
	public void onZoomBeginConfirm(){
		if(null != mCameraViewControlAnimator)
			mCameraViewControlAnimator.hideControl();
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
	private int miStreamIdx = -1;
	private Thread mPlayStreamThread = null;
	private void beginLiveView(){
    	if(null == mStreamingView || !isCamPowerOn()){
    		Log.w(TAG, "beginLiveView(), mStreamingView is null or isCamPowerOn() is "+isCamPowerOn());
    		return;
    	}
    	
    	if(null == mPlayStreamThread){
    		mIsStop = false;
       	 	mPlayStreamThread = new Thread(){
            	public void run(){ 
            		if (TouchSurfaceView.mSDLThread != null) {
                       try {
                       	TouchSurfaceView.mSDLThread.join();
                       } catch(Exception e) {
                           Log.v(TAG, "Problem stopping thread: " + e);
                           mPlayStreamThread = null;
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
	   	         			String streamFullPath = null;
	   	         			if(null != mstrLiveP2P && 0 < mstrLiveP2P.length()){
	   	         				streamFullPath = mstrLiveP2P;
	   	         			}else if(null != mstrLiveStreamServer && null != mstrLiveStreamPath){
	   	             			streamFullPath = mstrLiveStreamServer+"/"+mstrLiveStreamPath;
	   	             		}/*else{
	   	             			streamFullPath = STREAM_PATH_LIST.get(CUR_STREAMING_PATH_IDX%STREAM_PATH_LIST.size());
	   	             		}*/
	   	         			
	   	         			if(null == streamFullPath){
	   	         				return;
	   	         			}
	   	             		
	   	         			miStreamIdx = 0;
	   	         			int iTrial = 0;
	   	         			int iRetCreateStreaming = 0;
	   	         			do{
	   	         				Log.i(TAG, "CameraViewActivity::beginLiveView(), mActivityResume:"+mActivityResume+", mActivityDestroy:"+mActivityDestroy);
	   	         				if(mActivityDestroy){
	   	         					break;
	   	         				}else if(!mActivityResume){
	   	         					mbIsPauseWhenPlaying = true;
	   	         					break;
	   	         				}else if(isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING, CameraView_Internal_Status.CV_STREAM_PAUSED)){
   		         					break;
   		         				}
	   	         				
	   	         				if(0 > iRetCreateStreaming){
		   	         				if(-2 ==iRetCreateStreaming){
	   	         						closeStreaming();
	   	         					}
	   	         					iTrial++;
	   	         					try {
	   									Thread.sleep(1000);
	   									Log.i(TAG, "open stream failed due to "+iRetCreateStreaming+", sleep one sec");
	   								} catch (InterruptedException e) {
	   									e.printStackTrace();
	   								}
	   	         				}
	   	         				Log.i(TAG, "open stream for idx:"+miStreamIdx);
	   	         				iRetCreateStreaming = openStreaming(miStreamIdx, getNativeSurface(), streamFullPath, 0);
	   	         			}while(iRetCreateStreaming < 0 && iTrial < 8);
	   	         			
	   	         			if(miStreamIdx >= 10){
	   	         				miStreamIdx = -1;
	   	         			}
	   	         			
	   	         			if(0 <= iRetCreateStreaming){
	   	             			setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE);
	   	                 		mCurCheckCount = 0;
	   	             		}
            			}
            		}else{
            			if(0 < mstrPendingStreamPathList.size()){
            				if(!isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING, CameraView_Internal_Status.CV_STREAM_WAITING_CLOSE)){
            					String[] streamList = null;
                				String strHost=null;
                				long lOffset = 0;
	       		         		
                				synchronized(CameraViewActivity.this){
                					int iLen = mstrPendingStreamPathList.size();
                					Log.i(TAG, "mstrPendingStreamPathList.size():"+mstrPendingStreamPathList.size());
                					if(0 < iLen){
                						streamList = new String[mstrPendingStreamPathList.size()];
                    					strHost = BeseyeJSONUtil.getJSONString(mstrPendingStreamPathList.get(0), BeseyeJSONUtil.MM_SERVER)+"/";
                    					mlDVRFirstSegmentStartTs = BeseyeJSONUtil.getJSONLong(mstrPendingStreamPathList.get(0), BeseyeJSONUtil.MM_START_TIME);
                    					if(mlDVRFirstSegmentStartTs > mlDVRStartTs){
                    						mlDVRStartTs = mlDVRFirstSegmentStartTs;
                    					}else{
                    						lOffset = mlDVRStartTs-mlDVRFirstSegmentStartTs;
                    					}
                    					
                    					Log.i(TAG, "mlDVRFirstSegmentStartTs:"+mlDVRFirstSegmentStartTs+" > mlDVRStartTs:"+mlDVRStartTs+", lOffset:"+lOffset);
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
	       							
	//       							if(0 <= openStreamingList(0, getNativeSurface(), strHost, streamList, 0)){
	//       								//Log.i(TAG, "openStreamingList out");
	//       	                 			setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE);
	//       	                     		mCurCheckCount = 0;
	////       	                     		//roll back
	////       	                     		synchronized(CameraViewActivity.this){
	////       		                     		while(0 < mstrDVRStreamPathList.size()){
	////       		                     			mstrPendingStreamPathList.add(mstrDVRStreamPathList.remove(0));
	////       		                     		}
	////       		                     		//Log.i(TAG, "mstrDVRStreamPathList.size():"+mstrDVRStreamPathList.size());
	////       	                     		}
	//       							}
	       							
	       							miStreamIdx = 0;
	       		         			int iTrial = 0;
	       		         			int iRetCreateStreaming = 0;
	       		         			
	       		         			do{
	       		         				Log.i(TAG, "CameraViewActivity::beginLiveView(), mActivityResume:"+mActivityResume+", mActivityDestroy:"+mActivityDestroy);
	       		         				if(mActivityDestroy){
	       		         					break;
	       		         				}else if(!mActivityResume){
	       		         					mbIsPauseWhenPlaying = true;
	       		         					break;
	       		         				}else if(isBetweenCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING, CameraView_Internal_Status.CV_STREAM_PAUSED)){
	       		         					break;
	       		         				}
	       		         				
	       		         				
	       		         				if(0 > iRetCreateStreaming){
	       		         					iTrial++;
	       		         					try {
	       										Thread.sleep(1000);
	       										Log.i(TAG, "open stream failed due to "+iRetCreateStreaming+", sleep one sec");
	       									} catch (InterruptedException e) {
	       										e.printStackTrace();
	       									}
	       		         				}
	       		         				Log.i(TAG, "open stream for idx"+miStreamIdx);
	       		         				iRetCreateStreaming = openStreamingList(miStreamIdx, getNativeSurface(), strHost, streamList, /*(int)lOffset8*/0);
	       		         			}while(iRetCreateStreaming < 0 && iTrial < 8);
	       		         			
	       		         			if(miStreamIdx >= 10){
	       		         				miStreamIdx = -1;
	       		         			}
	       		         			
	       		         			if(0 <= iRetCreateStreaming){
	       		             			setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE);
	       		                 		mCurCheckCount = 0;
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
            		mPlayStreamThread = null;
            		//if(isCamViewStatus(CameraView_Internal_Status.CV_STREAM_CLOSE))
            	}
            };
            mPlayStreamThread.start();
    	}else{
    		Log.e(TAG, "mPlayStreamThread is alive");
    	}
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
		if(null != mCameraViewControlAnimator){
			TextView txtTime = mCameraViewControlAnimator.getTimeView();
			if(null != txtTime){
				txtTime.setText(R.string.camerview_init_time);
			}
			
			TextView txtDate = mCameraViewControlAnimator.getDateView();
			if(null != txtDate){
				txtDate.setText(R.string.camerview_init_date);
			}
		}
	}
	
	static class UpdateDateTimeRunnable implements Runnable{
		private WeakReference<TextView> mTxtDate, mTxtTime;
		private TimeZone mTimeZone = TimeZone.getDefault();
		
		static private final SimpleDateFormat sDateFormat = new SimpleDateFormat("MM.dd.yyyy");
		static private final SimpleDateFormat sTimeFormat = new SimpleDateFormat("hh:mm:ss a");
		
		public UpdateDateTimeRunnable(TextView txtDate, TextView txtTime){
			mTxtDate = new WeakReference<TextView>(txtDate);
			mTxtTime = new WeakReference<TextView>(txtTime);
		}
		
		public UpdateDateTimeRunnable(TextView txtDate, TextView txtTime, TimeZone tz){
			mTxtDate = new WeakReference<TextView>(txtDate);
			mTxtTime = new WeakReference<TextView>(txtTime);
			updateTimeZone(tz);
			//Log.e(TAG, "UpdateDateTimeRunnable(), mTimeZone:"+mTimeZone.toString());
		}
		
		public void updateDateTimeView(TextView txtDate, TextView txtTime){
			mTxtDate = new WeakReference<TextView>(txtDate);
			mTxtTime = new WeakReference<TextView>(txtTime);
		}
		
		public void updateTimeZone(TimeZone tz){
			 mTimeZone = tz;
			 sDateFormat.setTimeZone(mTimeZone);
			 sTimeFormat.setTimeZone(mTimeZone);
			 Log.e(TAG, "UpdateDateTimeRunnable(), mTimeZone:"+mTimeZone.toString());
		}
		
		@Override
		public void run() {
			Date now = Calendar.getInstance(mTimeZone).getTime();
			updateDateTime(now);
			sHandler.postDelayed(this, 1000L);
		}
		
		public void updateDateTime(Date date){
			TextView txtDate = mTxtDate.get();
			if(null != txtDate){
				txtDate.setText(sDateFormat.format(date));
			}
			
			TextView txtTime = mTxtTime.get();
			if(null != txtTime){
				txtTime.setText(sTimeFormat.format(date));
			}
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
    	static private final long EXPIRE_VIDEO_BLOCK = 10000L;
    	static private final long EXPIRE_VIDEO_NOT_START = 5000L;
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
				act.mReStartRunnable =  new Runnable(){
					@Override
					public void run() {
						act.beginLiveView();
					}};
			}
		}
	} 
    
    private Runnable mReStartRunnable= null;
    
    final private static int COUNT_TO_START_CHECK_EXPIRE =30; 
    private int mCurCheckCount = 0;
    private CheckVideoBlockRunnable mCheckVideoBlockRunnable;
    
    public void drawStreamBitmap(){
    	if(mPbLoadingCursor.getVisibility()!=View.GONE){
    		setVisibility(mPbLoadingCursor, View.GONE);
    	}
    	
    	if(null != mStreamingView && isCamPowerOn())
    		mStreamingView.drawStreamBitmap();
    	
    	if(mbIsLiveMode){
    		mCurCheckCount++;
        	if(mCurCheckCount > COUNT_TO_START_CHECK_EXPIRE){
        		if(null == mCheckVideoBlockRunnable){
        			mCheckVideoBlockRunnable = new CheckVideoBlockRunnable(this);
        		}
        		BeseyeUtils.removeRunnable(mCheckVideoBlockRunnable);
        		BeseyeUtils.postRunnable(mCheckVideoBlockRunnable, CheckVideoBlockRunnable.EXPIRE_VIDEO_BLOCK);
        	}
    	}
    }
    
    private void cancelCheckVideoBlock(){
    	if(null != mCheckVideoBlockRunnable)
    		BeseyeUtils.removeRunnable(mCheckVideoBlockRunnable);
    }
    
    private void closeStreaming(){
    	Log.e(TAG, "closeStreaming() ++");
    	cancelCheckVideoBlock();
//    	new Thread(){
//    		public void run(){ 
    			if(0 <= miStreamIdx)
    	    		closeStreaming(miStreamIdx);
//    		}}.start();
    	mCurCheckCount = 0;
    	Log.e(TAG, "closeStreaming() --");
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
    
    //refer to beseye_rtmp_observer.h => iMajorType of ERROR_CB
    enum Player_Major_Error {
		INTERNAL_STREAM_ERR,
		NO_NETWORK_ERR,
		NOMEM_CB,
		UNKNOWN_ERR //retry
	}
    
    //refer to Stream_Error in rtmp.h => iMinorType, if iMajorType == INTERNAL_STREAM_ERR, 
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
 
    //Sync with rtmp.h
	enum Stream_Status{
		STREAM_UNINIT,
		STREAM_INIT,
		STREAM_CONNECTING,
		STREAM_CONNECTED,
		STREAM_PLAYING,
		STREAM_PAUSING,
		STREAM_PAUSED,
		STREAM_UNPAUSING,
		STREAM_EOF,
		STREAM_INTERNAL_CLOSE,
		STREAM_CLOSE
	};

	
    
    public void updateRTMPStatus(int iType, final String msg){
    	Log.w(TAG, "updateRTMPStatus(), iType:"+iType);
    	if(iType == Stream_Status.STREAM_INIT.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_INIT);
    	}else if(iType == Stream_Status.STREAM_CONNECTING.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTING);
    	}else if(iType == Stream_Status.STREAM_CONNECTED.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_CONNECTED);
    	}else if(iType == Stream_Status.STREAM_PLAYING.ordinal()){
    		setCamViewStatus(CameraView_Internal_Status.CV_STREAM_PLAYING);
    		BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					updateStreamList(msg);
				}}, 0);
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
    	if(null != strCurPlaying && null != mstrDVRStreamPathList){
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
					monitorAsyncTask(mDVRStreamAppendTask = new BeseyeMMBEHttpTask.GetDVRStreamTask(this).setDialogId(-1), true, (null != mStrVCamID)?mStrVCamID:TMP_MM_VCAM_ID, startTime+"", DVR_REQ_TIME);
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
    			lRet = lStartTime + lDuration;
    			Log.i(TAG, "findLastTimeFromList(), lStartTime: "+lStartTime+", lDuration:"+lDuration+", traget:"+target.toString()+", time:"+(new Date(lRet).toString()));
    		}else{
    			Log.e(TAG, "findLastTimeFromList(), can not find target");
    		}
    	}
    	return lRet;
    }
    
    private void appendStreamList(JSONArray streamList){
    	int iCount = streamList.length();
		Log.i(TAG, "appendStreamList(), iCount: "+iCount);

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
					//Log.e(TAG, "appendStreamList(), add itm:"+check.toString());	
					mstrPendingStreamPathList.add(check);
					//Log.e(TAG, "onPostExecute(), mstrLiveStreamServer:"+mstrLiveStreamServer+", mstrLiveStreamPathList[i]="+mstrDVRStreamPathList[i]);	
				} catch (JSONException e) {
					e.printStackTrace();
					Log.e(TAG, "appendStreamList(), e:"+e.toString());	
				}
			}
		}
    }
   
    public void updateRTMPClockCallback(final int iTimeInSec){
    	BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				//Log.w(TAG, "updateRTMPClockCallback(), iTimeInSec:"+iTimeInSec);
		    	if(false == mbIsLiveMode)
		    		mUpdateDateTimeRunnable.updateDateTime(new Date(mlDVRStartTs+1000*iTimeInSec));
			}}, 0);
    }
    
    public void updateRTMPErrorCallback(final int iMajorType, final int iMinorType, final String msg){
    	if(null != sHandler){
    		sHandler.post(new Runnable(){
				@Override
				public void run() {
					mbNeedToCheckReddotNetwork = false;
					int iErrStrId = R.string.streaming_error_unknown;
					if(!isInP2PMode())
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
						iErrStrId = R.string.streaming_error_unknown;
					}
					
					if(iMinorType == Stream_Error.SERVER_REQUEST_CLOSE_ERROR.ordinal() && !isCamPowerOn()){
						Log.w(TAG, "updateRTMPErrorCallback(), unPublish case");
						return;
					}
					//workaround
					if(!ASSIGN_ST_PATH && !isInP2PMode()){
						if(iErrStrId == R.string.streaming_error_unknown){
							tryToReconnect();
//							if(mActivityResume)
//								Toast.makeText(getApplicationContext(), getString(R.string.streaming_error_unknown), Toast.LENGTH_SHORT).show();
						}else{
							Bundle b = new Bundle();
							b.putString(KEY_WARNING_TEXT, getResources().getString(iErrStrId));
							showMyDialog(DIALOG_ID_WARNING, b);
					    	closeStreaming();
						}
					}else if(isInP2PMode() && iErrStrId == R.string.streaming_error_unknown){
						BeseyeUtils.postRunnable(new Runnable(){
							@Override
							public void run() {
								tryToReconnect();
							}}, 1000);
//						if(mActivityResume)
//							Toast.makeText(getApplicationContext(), getString(R.string.streaming_error_unknown), Toast.LENGTH_SHORT).show();
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
    	if(false == isInHoldToTalkMode())
    		AudioChannelMgr.audioWriteShortBuffer(buffer, buffer.length);
    }
    
    public static void audioWriteByteBuffer(byte[] buffer) {
    	if(false == isInHoldToTalkMode())
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
    
    private static final long TIME_TO_TERMINATE_AUDIO = 20*1000;
    
    private Runnable mTerminateAudioChannelRunnable = new Runnable(){
		@Override
		public void run() {
			if(AudioWebSocketsMgr.getInstance().isWSChannelAlive()){
				AudioWebSocketsMgr.getInstance().destroyWSChannel();
			}else if(null != mGetAudioWSServerTask){
				mGetAudioWSServerTask.cancel(true);
			}
		}};
    
    private BeseyeNotificationBEHttpTask.GetAudioWSServerTask mGetAudioWSServerTask = null;
    public void openAudioChannel(){
    	BeseyeUtils.removeRunnable(mTerminateAudioChannelRunnable);
    	if(!AudioWebSocketsMgr.getInstance().isWSChannelAlive()){
    		if(null == mGetAudioWSServerTask)
    			if("".equals(SessionMgr.getInstance().getWSAHostUrl())){
    				monitorAsyncTask(mGetAudioWSServerTask = (GetAudioWSServerTask) new BeseyeNotificationBEHttpTask.GetAudioWSServerTask(this).setDialogId(-1), true);
    			}else{
    				AudioWebSocketsMgr.getInstance().setVCamId(mStrVCamID);
					AudioWebSocketsMgr.getInstance().setAudioWSServerIP(SessionMgr.getInstance().getWSAHostUrl());
					AudioWebSocketsMgr.getInstance().setSienceFlag(false);
					AudioWebSocketsMgr.getInstance().constructWSChannel();
    			}
    			
    		else
    			Log.i(TAG, "openAudioChannel(), mGetAudioWSServerTask isn't null");
		}else{
			//Toast.makeText(CameraViewActivity.this, "Talk now", Toast.LENGTH_SHORT).show();
			AudioWebSocketsMgr.getInstance().setSienceFlag(false);
		}
    	
    	setInHoldToTalkMode(true);
    }
    
    public void closeAudioChannel(boolean bImmediateClose){
    	if(AudioWebSocketsMgr.getInstance().isWSChannelAlive()){
    		AudioWebSocketsMgr.getInstance().setSienceFlag(true);
    		BeseyeUtils.removeRunnable(mTerminateAudioChannelRunnable);
    		BeseyeUtils.postRunnable(mTerminateAudioChannelRunnable, bImmediateClose?0:TIME_TO_TERMINATE_AUDIO);
		}else if(null != mGetAudioWSServerTask){
			mGetAudioWSServerTask.cancel(true);
		}
    	
    	setInHoldToTalkMode(false);
    	
    	if(null != mCameraViewControlAnimator){
			mCameraViewControlAnimator.setAmplitudeRatio(0.0f);
		}
    }

	@Override
	public void onChannelConnecting() {
		Log.i(TAG, "onChannelConnecting()---");
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				checkHoldToTalkMode();
			}}, 0);
	}

	@Override
	public void onAuthfailed() {
		Log.i(TAG, "onAuthfailed()---");
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(null != mCameraViewControlAnimator && mCameraViewControlAnimator.isInHoldToTalkMode()){
					mCameraViewControlAnimator.terminateTalkMode();
				}
			}}, 0);
		
	}

	@Override
	public void onChannelConnected() {
		Log.i(TAG, "onChannelConnected()---");
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				checkHoldToTalkMode();
//				if(AudioWebSocketsMgr.getInstance().isWSChannelAlive()){
//					Toast.makeText(CameraViewActivity.this, "Talk now", Toast.LENGTH_SHORT).show();
//				}
			}}, 0);
	}

	@Override
	public void onMessageReceived(String msg) {
		Log.i(TAG, "onMessageReceived()---");
	}

	@Override
	public void onChannelClosed() {
		Log.i(TAG, "onChannelClosed()---");
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				//Toast.makeText(CameraViewActivity.this, "Talk terminated", Toast.LENGTH_SHORT).show();
			}}, 0);
		
	}
	
	private void checkHoldToTalkMode(){
		if(null != mCameraViewControlAnimator && false == mCameraViewControlAnimator.isInHoldToTalkMode()){
			closeAudioChannel(false);
		}
	}
	
	private static boolean sbInHoldToTalkMode = false;
	
	static private boolean isInHoldToTalkMode(){
		return sbInHoldToTalkMode;
	}
	
	static private void setInHoldToTalkMode(boolean bInHoldToTalkMode){
		sbInHoldToTalkMode = bInHoldToTalkMode;
	}
	
	@Override
	public void onAudioAmplitudeUpdate(final float fRatio){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(null != mCameraViewControlAnimator){
					mCameraViewControlAnimator.setAmplitudeRatio(fRatio);
				}
			}}, 0);
	}

	@Override
	public boolean isCameraStatusOn() {
		return isCamPowerOn();
	}
	
	@Override
	public void onCamSetupChanged(String strVcamId, long lTs, JSONObject objCamSetup){
		boolean bIsCamOn = isCamPowerOn();
		super.onCamSetupChanged(strVcamId, lTs, objCamSetup);
		if(null != strVcamId && strVcamId.equals(mStrVCamID)){
			mbIsCamSettingChanged = true;
			updateAttrByCamObj();
			if(mActivityResume){
//				if(false == bIsCamDisconnect && isCamPowerDisconnected()){
//					Toast.makeText(getApplicationContext(), getString(R.string.notify_offline_detect_player), Toast.LENGTH_SHORT).show();
//				}else 
				if(bIsCamOn && isCamPowerOff()){
					Toast.makeText(getApplicationContext(), getString(R.string.notify_cam_off_detect_player), Toast.LENGTH_SHORT).show();
				}
			}
		}
    }
	
	private boolean mbShowStatusForEvent = false;
	
	public boolean isShowStatusForEvent(){return mbShowStatusForEvent;}
	
	private void showStatusBar(){
		mbShowStatusForEvent = true;
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
	}
	
	private void prepareToHideStatusBar(){
		BeseyeUtils.removeRunnable(mHideStatusBarRunnable);
		BeseyeUtils.postRunnable(mHideStatusBarRunnable, 6000);
	}
	
	private void removePrepareToHideStatusBar(){
		BeseyeUtils.removeRunnable(mHideStatusBarRunnable);
		mbShowStatusForEvent = false;
	}
	
	private Runnable mHideStatusBarRunnable = new Runnable(){
		@Override
		public void run() {
			Configuration config = getResources().getConfiguration();	
			if(null != mCameraViewControlAnimator && !(mCameraViewControlAnimator.getVisibility() == View.VISIBLE && config.orientation == Configuration.ORIENTATION_PORTRAIT)){
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
			mbShowStatusForEvent = false;
		}};
	
	private boolean checkEventById(JSONObject msgObj){
		Log.i(TAG, getClass().getSimpleName()+"::checkEventById(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != mStrVCamID && !mStrVCamID.equals(strCamUID)){
    				if(mActivityResume){
		    			showStatusBar();
		    			prepareToHideStatusBar();
		    		}
    			}
    			return true;
    		}
		}
    	return false;
	}
	
	protected boolean onCameraMotionEvent(JSONObject msgObj){
		return checkEventById(msgObj);
	}
	
    protected boolean onCameraPeopleEvent(JSONObject msgObj){
    	return checkEventById(msgObj);
    }
    
    @Override
	protected boolean onCameraOffline(JSONObject msgObj){
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraOffline(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(mActivityResume){
    				if(null != strCamUID && strCamUID.equals(mStrVCamID)){
    	    			Toast.makeText(getApplicationContext(), getString(R.string.notify_offline_detect_player), Toast.LENGTH_SHORT).show();
    	    		}else{
    	    			showStatusBar();
    	    			prepareToHideStatusBar();
    	    		}
    			}
    			return true;
    		}
		}
    	return false;
    }
}
