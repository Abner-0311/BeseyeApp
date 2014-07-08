package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.*;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.TimezoneListActivity;
import com.app.beseye.WifiControlBaseActivity;
import com.app.beseye.WifiListActivity;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

public class HWSettingsActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener{
	private BeseyeSwitchBtn mHDQualitySwitchBtn, mMicSenSwitchBtn, mStatusLightSwitchBtn;
	private TextView mTxtTimezoneDesc, mTxtNightVision;
	private ImageView mIvViewUpDownCheck, mIvViewUpDownCheckBg, mIvMicNoVol, mIvMicMaxVol;
	private ViewGroup mVgWifiSetting, mVgTimezone, mVgNightVision;
	private SeekBar mSbMicSensitivity;
	private String mStrVCamID = "";
	private JSONObject mCam_obj;
	
	private int miIRCutStatus = 0;//0:auto, 1:on, 2:off
	
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
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				//mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "HWSettingsActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}
		
		mIvMicNoVol = (ImageView)findViewById(R.id.img_mic_vol_min);
		mIvMicMaxVol = (ImageView)findViewById(R.id.img_mic_vol_max);
		mSbMicSensitivity = (SeekBar)findViewById(R.id.sb_mic_sensitivity);
		if(null != mSbMicSensitivity){
			//mSbMicSensitivity.setBackgroundResource(R.drawable.setting_voice_control_bar);
			
			//mSbMicSensitivity.setProgressDrawable(getResources().getDrawable(R.drawable.seekbar_progress_normal));
			
			mSbMicSensitivity.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

				@Override
				public void onProgressChanged(SeekBar view, int iProg,boolean bFromUser) {
					
				}

				@Override
				public void onStartTrackingTouch(SeekBar view) {
					
				}

				@Override
				public void onStopTrackingTouch(SeekBar view) {
					monitorAsyncTask(new BeseyeCamBEHttpTask.SetMicGainTask(HWSettingsActivity.this), true, mStrVCamID,""+(1+view.getProgress()));
				}});
		}
		
		mHDQualitySwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_hd_switch);
		if(null != mHDQualitySwitchBtn){
			mHDQualitySwitchBtn.setOnSwitchBtnStateChangedListener(this);
			//mHDQualitySwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
		}
		
		mMicSenSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_mic_sensitivity_switch);
		if(null != mMicSenSwitchBtn){
			mMicSenSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mStatusLightSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_status_light_switch);
		if(null != mStatusLightSwitchBtn){
			mStatusLightSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mTxtNightVision = (TextView)findViewById(R.id.txt_night_vision_mode);
		
		mTxtTimezoneDesc = (TextView)findViewById(R.id.txt_timezone_desc);
		if(null != mTxtTimezoneDesc){
			mTxtTimezoneDesc.setText("GMT +08:00");
		}
		
		mIvViewUpDownCheck = (ImageView)findViewById(R.id.iv_video_upside_down_check);
		if(null != mIvViewUpDownCheck){
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
	
	protected void onSessionComplete(){
		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.iv_video_upside_down_check_bg:{
				if(null != mIvViewUpDownCheck){
					mIvViewUpDownCheck.setVisibility((View.VISIBLE == mIvViewUpDownCheck.getVisibility())?View.INVISIBLE:View.VISIBLE);
					BeseyeJSONUtil.setJSONInt(mCam_obj, CameraListActivity.KEY_VCAM_UPSIDEDOWN, (View.VISIBLE == mIvViewUpDownCheck.getVisibility())?1:0);
					Intent resultIntent = new Intent();
					resultIntent.putExtra(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
					setResult(RESULT_OK, resultIntent);
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
				launchActivityForResultByClassName(TimezoneListActivity.class.getName(), null, REQUEST_TIMEZONE_CHANGED);
				break;
			}
			case R.id.vg_night_vision:{
				//launchActivityForResultByClassName(TimezoneListActivity.class.getName(), null, REQUEST_TIMEZONE_CHANGED);
				showDialog(DIALOG_ID_CAM_NIGHT_VISION);
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
								if(null != mTxtNightVision){
									monitorAsyncTask(new BeseyeCamBEHttpTask.SetIRCutStatusTask(HWSettingsActivity.this), true, mStrVCamID, ""+idx);
									mTxtNightVision.setText(sStridNightVisionMode[idx]);
								}
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
	
	static public final int REQUEST_TIMEZONE_CHANGED = 10001;
	static public final int REQUEST_WIFI_SETTING_CHANGED = 10002;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_TIMEZONE_CHANGED == requestCode){
			if(resultCode == RESULT_OK){
				mTxtTimezoneDesc = (TextView)findViewById(R.id.txt_timezone_desc);
				if(null != mTxtTimezoneDesc){
					mTxtTimezoneDesc.setText(intent.getStringExtra(TIME_ZONE_INFO));
				}
				setResult(RESULT_OK, intent);
			}
		}else if(REQUEST_WIFI_SETTING_CHANGED== requestCode){
			if(resultCode == RESULT_OK){
				WifiAPInfo chosenWifiAPInfo = intent.getParcelableExtra(SoundPairingActivity.KEY_WIFI_INFO);
				if(null != chosenWifiAPInfo){
					monitorAsyncTask(new BeseyeCamBEHttpTask.SetWiFiConfigTask(this), true, mStrVCamID, chosenWifiAPInfo.SSID, chosenWifiAPInfo.password, ""+NetworkMgr.translateCipherToType(chosenWifiAPInfo.cipher));
				}
				//mbIsWifiSettingChanged = true;
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
	
	private void updateMicSenItmStatus(){
		SwitchState state = null != mMicSenSwitchBtn?mMicSenSwitchBtn.getSwitchState():SwitchState.SWITCH_DISABLED;
		
		if(null != mIvMicNoVol){
			mIvMicNoVol.setEnabled(SwitchState.SWITCH_ON.equals(state));
		}
		
		if(null != mIvMicMaxVol){
			mIvMicMaxVol.setEnabled(SwitchState.SWITCH_ON.equals(state));
		}
		
		if(null != mSbMicSensitivity){
			mSbMicSensitivity.setEnabled(SwitchState.SWITCH_ON.equals(state));
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			Bundle b = new Bundle();
			b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_get_cam_info));
			b.putBoolean(KEY_WARNING_CLOSE, true);
			showMyDialog(DIALOG_ID_WARNING, b);
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject obj = result.get(0);
					if(null != obj){
						JSONObject dataObj = BeseyeJSONUtil.getJSONObject(obj, ACC_DATA);
						if(null != dataObj){
							int iLEDStatus = getJSONInt(dataObj, LED_STATUS, 0);
							if(null != mStatusLightSwitchBtn){
								mStatusLightSwitchBtn.setSwitchState(iLEDStatus>0?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
							}
							
							int iMicGain = getJSONInt(dataObj, MIC_GAIN, 0);
							if(null != mSbMicSensitivity){
								mSbMicSensitivity.setProgress(iMicGain);
							}
							
							int iMicStatus = getJSONInt(dataObj, MIC_STATUS, 0);
							if(null != mMicSenSwitchBtn){
								mMicSenSwitchBtn.setSwitchState(iMicStatus>0?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
							}
							
							int iVIdeoRes = getJSONInt(dataObj, VIDEO_RES, 0);
							if(null != mHDQualitySwitchBtn){
								mHDQualitySwitchBtn.setSwitchState(iVIdeoRes>0?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
							}
							
							updateMicSenItmStatus();
							
							miIRCutStatus = getJSONInt(dataObj, IRCUT_STATUS, 0);
							if(null != this.mTxtNightVision){
								mTxtNightVision.setText(sStridNightVisionMode[miIRCutStatus]);
							}
						}
					}
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetLEDStatusTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetVideoResTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
				}
			}/*else if(task instanceof BeseyeCamBEHttpTask.SetSpeakerStatusTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.GetSpeakerStatusTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.SetSpeakerVolumeTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.GetSpeakerVolumeTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}*/else if(task instanceof BeseyeCamBEHttpTask.SetMicStatusTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					updateMicSenItmStatus();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetMicStatusTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.SetMicGainTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetMicGainTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.SetIRCutStatusTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.GetIRCutStatusTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.SetImageSettingTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.GetImageSettingTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.ReconnectMMTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.GetWiFiConfigTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.SetWiFiConfigTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}

}
