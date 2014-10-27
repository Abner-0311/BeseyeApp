package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.*;

import java.util.List;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.WifiControlBaseActivity;
import com.app.beseye.WifiListActivity;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

public class HWSettingsActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener{
	private BeseyeSwitchBtn mHDQualitySwitchBtn, mMicSenSwitchBtn, mStatusLightSwitchBtn;
	private TextView mTxtTimezoneDesc, mTxtNightVision;
	private ImageView mIvViewUpDownCheck, mIvViewUpDownCheckBg, mIvMicNoVol, mIvMicMaxVol;
	private ViewGroup mVgHDQuality, mVgMicSen, mVgStatusLight, mVgWifiSetting, mVgTimezone, mVgNightVision, mVgViewUpDown;
	private SeekBar mSbMicSensitivity;
	
	private int miIRCutStatus = 0;//0:auto, 1:on, 2:off
	private int miBeginPosOfMicGain = -1;
	private TimeZone mTimeZone;
	private TimeZone mTimeZoneCandidate;
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "HWSettingsActivity::onCreate()");
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
						
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.cam_hardware_setting_title);
				txtTitle.setOnClickListener(this);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				//mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
				mTimeZone = TimeZone.getTimeZone(BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA), BeseyeJSONUtil.CAM_TZ, TimeZone.getDefault().getID()));
			}
		} catch (JSONException e1) {
			Log.e(TAG, "HWSettingsActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}
		
		mIvMicNoVol = (ImageView)findViewById(R.id.img_mic_vol_min);
		mIvMicMaxVol = (ImageView)findViewById(R.id.img_mic_vol_max);
		mSbMicSensitivity = (SeekBar)findViewById(R.id.sb_mic_sensitivity);
		if(null != mSbMicSensitivity){			
			mSbMicSensitivity.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
				int iStartPos = -1;
				@Override
				public void onProgressChanged(SeekBar view, int iProg,boolean bFromUser) {
					
				}

				@Override
				public void onStartTrackingTouch(SeekBar view) {
					iStartPos = view.getProgress();
				}

				@Override
				public void onStopTrackingTouch(SeekBar view) {
					int iEndPos = view.getProgress();
					if(iEndPos != iStartPos){
						miBeginPosOfMicGain = iStartPos;
						monitorAsyncTask(new BeseyeCamBEHttpTask.SetMicGainTask(HWSettingsActivity.this), true, mStrVCamID,""+(iEndPos));
					}
				}});
		}
		
		mVgHDQuality = (ViewGroup)findViewById(R.id.vg_hd_quality);
		BeseyeUtils.setVisibility(mVgHDQuality, View.GONE);
		
		mHDQualitySwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_hd_switch);
		if(null != mHDQualitySwitchBtn){
			mHDQualitySwitchBtn.setOnSwitchBtnStateChangedListener(this);
			//mHDQualitySwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
		}
		
		mVgMicSen = (ViewGroup)findViewById(R.id.vg_mic_sensitivity);
		
		mMicSenSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_mic_sensitivity_switch);
		if(null != mMicSenSwitchBtn){
			mMicSenSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mVgStatusLight = (ViewGroup)findViewById(R.id.vg_status_light);
		
		mStatusLightSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_status_light_switch);
		if(null != mStatusLightSwitchBtn){
			mStatusLightSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mTxtNightVision = (TextView)findViewById(R.id.txt_night_vision_mode);
		
		mTxtTimezoneDesc = (TextView)findViewById(R.id.txt_timezone_desc);
		if(null != mTxtTimezoneDesc){
			mTxtTimezoneDesc.setText(BeseyeUtils.getGMTString(mTimeZone));
		}
		
		mVgViewUpDown = (ViewGroup)findViewById(R.id.vg_video_upside_down);
		if(null != mVgViewUpDown){
			mVgViewUpDown.setOnClickListener(this);
			if(BeseyeUtils.isHiddenFeature()){
				BeseyeUtils.setVisibility(mVgViewUpDown, View.GONE);
			}
		}
		
		mIvViewUpDownCheck = (ImageView)findViewById(R.id.iv_video_upside_down_check);
		if(null != mIvViewUpDownCheck){
			mIvViewUpDownCheck.setVisibility(View.INVISIBLE);
			//mIvViewUpDownCheck.setOnClickListener(this);
		}
		
		mIvViewUpDownCheckBg = (ImageView)findViewById(R.id.iv_video_upside_down_check_bg);
		if(null != mIvViewUpDownCheckBg){
			mIvViewUpDownCheckBg.setOnClickListener(this);
		}
		
		mVgWifiSetting = (ViewGroup)findViewById(R.id.vg_wifi_setting);
		if(null != mVgWifiSetting){
			mVgWifiSetting.setOnClickListener(this);
		}
		
		mVgTimezone = (ViewGroup)findViewById(R.id.vg_timezone);
		if(null != mVgTimezone){
			mVgTimezone.setOnClickListener(this);
		}
		
		mVgNightVision = (ViewGroup)findViewById(R.id.vg_night_vision);
		if(null != mVgNightVision){
			mVgNightVision.setOnClickListener(this);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_hardware_setting_page;
	}
	
	@Override
	protected void onResume() {
		Log.i(TAG, "CameraSettingActivity::onResume()");
		super.onResume();
		if(!mbFirstResume || null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA)){
			updateHWSettingState();
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA)){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
		}else{
			updateHWSettingState();
		}
	}
	
	private int miUnmaskHitCount = 0;
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.vg_video_upside_down:
			case R.id.iv_video_upside_down_check_bg:{
				if(null != mIvViewUpDownCheck){
					mIvViewUpDownCheck.setVisibility((View.VISIBLE == mIvViewUpDownCheck.getVisibility())?View.INVISIBLE:View.VISIBLE);
					monitorAsyncTask(new BeseyeCamBEHttpTask.SetVideoUpsideDownTask(this), true, mStrVCamID,new Boolean((View.VISIBLE == mIvViewUpDownCheck.getVisibility())).toString());
					
//					BeseyeJSONUtil.setJSONInt(mCam_obj, CameraListActivity.KEY_VCAM_UPSIDEDOWN, (View.VISIBLE == mIvViewUpDownCheck.getVisibility())?1:0);
//					setActivityResultWithCamObj();
				}
				break;
			}
			case R.id.vg_wifi_setting:{
				Bundle bundle = new Bundle();
				bundle.putBoolean(WifiControlBaseActivity.KEY_CHANGE_WIFI_ONLY, true);
				bundle.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(WifiListActivity.class.getName(), bundle, REQUEST_WIFI_SETTING_CHANGED);
				break;
			}
			case R.id.vg_timezone:{
				Bundle bundle = new Bundle();
				bundle.putString(TimezoneListActivity.KEY_TZ, mTimeZone.getID());
				launchActivityForResultByClassName(TimezoneListActivity.class.getName(), bundle, REQUEST_TIMEZONE_CHANGED);
				break;
			}
			case R.id.vg_night_vision:{
				//showMyDialog(DIALOG_ID_CAM_NIGHT_VISION);
				Bundle bundle = new Bundle();
				bundle.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(NightVisionActivity.class.getName(), bundle, REQUEST_NIGHT_VISION_CHANGED);
				break;
			}
			case R.id.txt_nav_title:{
				if(5 == ++miUnmaskHitCount){
					BeseyeUtils.setVisibility(mVgViewUpDown, View.VISIBLE);
				}
				break;
			}
			default:
				super.onClick(view);
		}
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		switch(view.getId()){
			case R.id.sb_hd_switch:{
				monitorAsyncTask(new BeseyeCamBEHttpTask.SetVideoResTask(this), true, mStrVCamID,SwitchState.SWITCH_ON.equals(state)?"1":"0");
				break;
			}
			case R.id.sb_mic_sensitivity_switch:{
				monitorAsyncTask(new BeseyeCamBEHttpTask.SetMicStatusTask(this), true, mStrVCamID,SwitchState.SWITCH_ON.equals(state)?"1":"0");
				break;
			}
			case R.id.sb_status_light_switch:{
				monitorAsyncTask(new BeseyeCamBEHttpTask.SetLEDStatusTask(this), true, mStrVCamID,SwitchState.SWITCH_ON.equals(state)?"1":"0");
				break;
			}
			default:{
				
			}
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "WifiListActivity::onCreateDialog()");
		Dialog dialog;
		switch(id){
			case DIALOG_ID_CAM_NIGHT_VISION:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createNightVisionDialog());
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							removeMyDialog(DIALOG_ID_CAM_NIGHT_VISION);
						}});
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							
						}});
				}
            	break;
			}case DIALOG_ID_WIFI_AP_APPLY:{
				dialog = ProgressDialog.show(this, "", getString(R.string.cam_setting_wifi_setting_apply), true, true);
				dialog.setCancelable(false);
				BeseyeUtils.postRunnable(mCountDownWiFiChangeRunnable = new Runnable(){
					@Override
					public void run() {
						removeMyDialog(DIALOG_ID_WIFI_AP_APPLY);
						Toast.makeText(HWSettingsActivity.this, getString(R.string.cam_setting_fail_to_apply_wifi_setting), Toast.LENGTH_LONG).show();
					}}, 60*1000);
				break;
			}
			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
	
	static private final int NUM_NIGHT_VISION_MODE = 3;
	
	private static int[] sStridNightVisionMode = {R.string.cam_setting_hw_night_vision_auto, 
												  R.string.cam_setting_hw_night_vision_on,
												  R.string.cam_setting_hw_night_vision_off}; 
	
	private View createNightVisionDialog(){
		View viewRet = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			viewRet = (View)inflater.inflate(R.layout.night_vision_mode_dialog, null);
			if(null != viewRet){
				ImageView[] ivKeyIdx = new ImageView[NUM_NIGHT_VISION_MODE]; 
				if(null != ivKeyIdx){
					ivKeyIdx[0] = (ImageView)viewRet.findViewById(R.id.iv_key_auto_check);
					ivKeyIdx[1] = (ImageView)viewRet.findViewById(R.id.iv_key_on_check);
					ivKeyIdx[2] = (ImageView)viewRet.findViewById(R.id.iv_key_off_check);
				}
				
				final ViewGroup[] vgKeyIdx = new ViewGroup[NUM_NIGHT_VISION_MODE];  
				if(null != vgKeyIdx){
					vgKeyIdx[0] = (ViewGroup)viewRet.findViewById(R.id.vg_nv_holder_auto);
					vgKeyIdx[1] = (ViewGroup)viewRet.findViewById(R.id.vg_nv_holder_on);
					vgKeyIdx[2] = (ViewGroup)viewRet.findViewById(R.id.vg_nv_holder_off);
				}
				
				OnClickListener keyIdxClick = new OnClickListener(){
					@Override
					public void onClick(View view) {
						removeMyDialog(DIALOG_ID_CAM_NIGHT_VISION);
						for(int idx = 0; idx < NUM_NIGHT_VISION_MODE;idx++){
							if(vgKeyIdx[idx] == view){
								monitorAsyncTask(new BeseyeCamBEHttpTask.SetIRCutStatusTask(HWSettingsActivity.this), true, mStrVCamID, ""+idx);
								break;
							}
						}
					}};
				
				for(int idx = 0; idx < NUM_NIGHT_VISION_MODE;idx++){
					ivKeyIdx[idx].setVisibility((idx == miIRCutStatus)?View.VISIBLE:View.INVISIBLE);
					vgKeyIdx[idx].setOnClickListener(keyIdxClick);
				}
			}
		}
		return viewRet;
	}
	
	static public final String TIME_ZONE_INFO = "TIME_ZONE_INFO";
	static public final int REQUEST_TIMEZONE_CHANGED 		= 10001;
	static public final int REQUEST_WIFI_SETTING_CHANGED 	= 10002;
	static public final int REQUEST_NIGHT_VISION_CHANGED 	= 10003;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_TIMEZONE_CHANGED == requestCode){
			if(resultCode == RESULT_OK){
				String strTimeZoneId = intent.getStringExtra(TIME_ZONE_INFO);
				mTimeZoneCandidate = TimeZone.getTimeZone(strTimeZoneId);
				if(!mTimeZoneCandidate.equals(mTimeZone)){
					monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamTimezoneTask(this), true, mStrVCamID, mTimeZoneCandidate.getID());
				}	
			}
		}else if(REQUEST_WIFI_SETTING_CHANGED== requestCode){
			if(resultCode == RESULT_OK){
				WifiAPInfo chosenWifiAPInfo = intent.getParcelableExtra(SoundPairingActivity.KEY_WIFI_INFO);
				if(null != chosenWifiAPInfo){
					monitorAsyncTask(new BeseyeCamBEHttpTask.SetWiFiConfigTask(this), true, mStrVCamID, chosenWifiAPInfo.SSID, chosenWifiAPInfo.password, ""+chosenWifiAPInfo.iCipherIdx);
				}
				//mbIsWifiSettingChanged = true;
			}
		}else if(REQUEST_NIGHT_VISION_CHANGED== requestCode){
			if(resultCode == RESULT_OK){
				updateHWSettingState();
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
	
	private void updateMicSenItmStatus(){
		SwitchState state = null != mMicSenSwitchBtn?mMicSenSwitchBtn.getSwitchState():SwitchState.SWITCH_DISABLED;
		boolean bIsCamDisconnected = BeseyeJSONUtil.isCamPowerDisconnected(mCam_obj);
		if(null != mIvMicNoVol){
			mIvMicNoVol.setEnabled(!bIsCamDisconnected && SwitchState.SWITCH_ON.equals(state));
		}
		
		if(null != mIvMicMaxVol){
			mIvMicMaxVol.setEnabled(!bIsCamDisconnected && SwitchState.SWITCH_ON.equals(state));
		}
		
		if(null != mSbMicSensitivity){
			mSbMicSensitivity.setEnabled(!bIsCamDisconnected && SwitchState.SWITCH_ON.equals(state));
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			showErrorDialog(R.string.cam_setting_fail_to_get_cam_info, true);
		}else if(task instanceof BeseyeCamBEHttpTask.SetLEDStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					showErrorDialog(R.string.cam_setting_fail_to_update_led_status, false);
					if(null != mStatusLightSwitchBtn){
						mStatusLightSwitchBtn.setSwitchState((mStatusLightSwitchBtn.getSwitchState()==SwitchState.SWITCH_ON)?SwitchState.SWITCH_OFF:SwitchState.SWITCH_ON);
					}
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetMicStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					showErrorDialog(R.string.cam_setting_fail_to_update_mic_status, false);
					if(null != mMicSenSwitchBtn){
						mMicSenSwitchBtn.setSwitchState((mMicSenSwitchBtn.getSwitchState()==SwitchState.SWITCH_ON)?SwitchState.SWITCH_OFF:SwitchState.SWITCH_ON);
					}
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetIRCutStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					showErrorDialog(R.string.cam_setting_fail_to_update_night_vision, false);
					if(null != mMicSenSwitchBtn){
						mMicSenSwitchBtn.setSwitchState((mMicSenSwitchBtn.getSwitchState()==SwitchState.SWITCH_ON)?SwitchState.SWITCH_OFF:SwitchState.SWITCH_ON);
					}
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetWiFiConfigTask){
			showErrorDialog(R.string.cam_setting_fail_to_update_wifi_setting, false);
		}else if(task instanceof BeseyeCamBEHttpTask.SetVideoResTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					showErrorDialog(R.string.cam_setting_fail_to_update_hd_quality, false);
					if(null != mHDQualitySwitchBtn){
						mHDQualitySwitchBtn.setSwitchState((mHDQualitySwitchBtn.getSwitchState()==SwitchState.SWITCH_ON)?SwitchState.SWITCH_OFF:SwitchState.SWITCH_ON);
					}
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetMicGainTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					showErrorDialog(R.string.cam_setting_fail_to_update_mic_sensitivity, false);
					if(null != mSbMicSensitivity){			
						mSbMicSensitivity.setProgress(miBeginPosOfMicGain);
						miBeginPosOfMicGain = -1;
					}
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetVideoUpsideDownTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					showErrorDialog(R.string.cam_setting_fail_to_update_video_upside_down, false);
					if(null != mIvViewUpDownCheck){
						mIvViewUpDownCheck.setVisibility((View.VISIBLE == mIvViewUpDownCheck.getVisibility())?View.INVISIBLE:View.VISIBLE);
					}
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetCamTimezoneTask){
			showErrorDialog(R.string.cam_setting_fail_to_update_timezone, false);
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
	
	private void updateHWSettingState(){
		if(null != mCam_obj){
			JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
			if(null != dataObj){
				boolean bIsCamDisconnected = BeseyeJSONUtil.isCamPowerDisconnected(mCam_obj);
				
				if(null != mVgStatusLight){
					mVgStatusLight.setEnabled(!bIsCamDisconnected);
				}
				
				int iLEDStatus = getJSONInt(dataObj, LED_STATUS, 0);
				if(null != mStatusLightSwitchBtn){
					mStatusLightSwitchBtn.setEnabled(!bIsCamDisconnected);
					mStatusLightSwitchBtn.setSwitchState((!bIsCamDisconnected && iLEDStatus>0?SwitchState.SWITCH_ON:(bIsCamDisconnected?SwitchState.SWITCH_DISABLED:SwitchState.SWITCH_OFF)));
				}
				
				if(null != mVgMicSen){
					mVgMicSen.setEnabled(!bIsCamDisconnected);
				}
				
				int iMicGain = getJSONInt(dataObj, MIC_GAIN, 0);
				if(null != mSbMicSensitivity){
					mSbMicSensitivity.setProgress(iMicGain);
					mSbMicSensitivity.setEnabled(!bIsCamDisconnected);
				}
				
				int iMicStatus = getJSONInt(dataObj, MIC_STATUS, 0);
				if(null != mMicSenSwitchBtn){
					mMicSenSwitchBtn.setEnabled(!bIsCamDisconnected);
					mMicSenSwitchBtn.setSwitchState((!bIsCamDisconnected && iMicStatus>0?SwitchState.SWITCH_ON:(bIsCamDisconnected?SwitchState.SWITCH_DISABLED:SwitchState.SWITCH_OFF)));
				}
				
				if(null != mVgHDQuality){
					mVgHDQuality.setEnabled(!bIsCamDisconnected);
				}
				
				int iVIdeoRes = getJSONInt(dataObj, VIDEO_RES, 0);
				if(null != mHDQualitySwitchBtn){
					mHDQualitySwitchBtn.setEnabled(!bIsCamDisconnected);
					mHDQualitySwitchBtn.setSwitchState((!bIsCamDisconnected && iVIdeoRes>0?SwitchState.SWITCH_ON:(bIsCamDisconnected?SwitchState.SWITCH_DISABLED:SwitchState.SWITCH_OFF)));
				}
				
				updateMicSenItmStatus();
				
				miIRCutStatus = getJSONInt(dataObj, IRCUT_STATUS, 0);
				if(null != this.mTxtNightVision){
					mTxtNightVision.setText(sStridNightVisionMode[miIRCutStatus]);
					mTxtNightVision.setEnabled(!bIsCamDisconnected);
				}
				
				if(null != mVgWifiSetting){
					mVgWifiSetting.setEnabled(!bIsCamDisconnected);
				}
				
				if(null != mVgNightVision){
					mVgNightVision.setEnabled(!bIsCamDisconnected);
				}
				
				if(null != mVgViewUpDown){
					mVgViewUpDown.setEnabled(!bIsCamDisconnected);
				}
				
				if(null != mIvViewUpDownCheckBg){
					mIvViewUpDownCheckBg.setEnabled(!bIsCamDisconnected);
				}
				
				if(null != mIvViewUpDownCheck){
					mIvViewUpDownCheck.setEnabled(!bIsCamDisconnected);
					JSONObject imageObj = BeseyeJSONUtil.getJSONObject(dataObj, IMG_OBJ);
					if(null != imageObj){
						mIvViewUpDownCheck.setVisibility((getJSONBoolean(imageObj, CAM_UPSIDE_DOWN, false))?View.VISIBLE:View.INVISIBLE);
					}
				}
				
				if(null != mVgTimezone){
					mVgTimezone.setEnabled(!bIsCamDisconnected);
				}
			}
		}
	}
	
	@Override
	protected void updateUICallback(){
		updateHWSettingState();
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					//Special handling
					super.onPostExecute(task, result, iRetCode);
					updateHWSettingState();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetLEDStatusTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					BeseyeJSONUtil.setJSONInt(getJSONObject(mCam_obj, ACC_DATA), LED_STATUS, BeseyeJSONUtil.getJSONInt(result.get(0), LED_STATUS));
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetVideoResTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());				
					BeseyeJSONUtil.setJSONInt(getJSONObject(mCam_obj, ACC_DATA), VIDEO_RES, BeseyeJSONUtil.getJSONInt(result.get(0), VIDEO_RES));
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetMicStatusTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					BeseyeJSONUtil.setJSONInt(getJSONObject(mCam_obj, ACC_DATA), MIC_STATUS, BeseyeJSONUtil.getJSONInt(result.get(0), MIC_STATUS));
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();
					updateMicSenItmStatus();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetMicGainTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					BeseyeJSONUtil.setJSONInt(getJSONObject(mCam_obj, ACC_DATA), MIC_GAIN, BeseyeJSONUtil.getJSONInt(result.get(0), MIC_GAIN));
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();	
					miBeginPosOfMicGain = -1;
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetIRCutStatusTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					miIRCutStatus = BeseyeJSONUtil.getJSONInt(result.get(0), IRCUT_STATUS);
					BeseyeJSONUtil.setJSONInt(getJSONObject(mCam_obj, ACC_DATA), IRCUT_STATUS, miIRCutStatus);
					if(null != mTxtNightVision){
						mTxtNightVision.setText(sStridNightVisionMode[miIRCutStatus]);
					}
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetCamTimezoneTask){
				if(0 == iRetCode){
					mTimeZone = mTimeZoneCandidate;
					mTimeZoneCandidate = null;
					if(null != mTxtTimezoneDesc){
						mTxtTimezoneDesc.setText(BeseyeUtils.getGMTString(mTimeZone));
					}
					BeseyeJSONUtil.setJSONString(getJSONObject(mCam_obj, ACC_DATA), CAM_TZ, mTimeZone.getID());
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetVideoUpsideDownTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
					if(null != dataObj){
						JSONObject imageObj = BeseyeJSONUtil.getJSONObject(dataObj, IMG_OBJ);
						if(null != imageObj){
							BeseyeJSONUtil.setJSONBoolean(imageObj, CAM_UPSIDE_DOWN, BeseyeJSONUtil.getJSONBoolean(result.get(0), CAM_UPSIDE_DOWN));
						}
					}
					
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();	
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetImageSettingTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.GetImageSettingTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.SetWiFiConfigTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					showMyDialog(DIALOG_ID_WIFI_AP_APPLY);
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
}
