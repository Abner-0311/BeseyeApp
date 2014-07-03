package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.TextView;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.TimezoneListActivity;
import com.app.beseye.WifiListActivity;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

public class HWSettingsActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener{
	private BeseyeSwitchBtn mHDQualitySwitchBtn, mMicSenSwitchBtn, mStatusLightSwitchBtn;
	private TextView mTxtTimezoneDesc, mTxtNightVision;
	private ImageView mIvViewUpDownCheck, mIvViewUpDownCheckBg;
	private ViewGroup mVgWifiSetting, mVgTimezone, mVgNightVision;
	private SeekBar mSbMicSensitivity;
	private String mStrVCamID = "Bes0001";
	private String mStrVCamName = null;
	private String mStrOldVCamName = null;
	private JSONObject mCam_obj;
	
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
				mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "CameraViewActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mSbMicSensitivity = (SeekBar)findViewById(R.id.sb_mic_sensitivity);
		if(null != mSbMicSensitivity){
			//mSbMicSensitivity.setBackgroundResource(R.drawable.setting_voice_control_bar);
			
			//mSbMicSensitivity.setProgressDrawable(getResources().getDrawable(R.drawable.seekbar_progress_normal));
		}
		
		mHDQualitySwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_hd_switch);
		if(null != mHDQualitySwitchBtn){
			mHDQualitySwitchBtn.setOnSwitchBtnStateChangedListener(this);
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
			mIvViewUpDownCheck.setOnClickListener(this);
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
				bundle.putBoolean(WifiListActivity.KEY_CHANGE_WIFI_ONLY, true);
				launchActivityByClassName(WifiListActivity.class.getName(), bundle);
				break;
			}
			case R.id.vg_timezone:{
				launchActivityForResultByClassName(TimezoneListActivity.class.getName(), null, REQUEST_TIMEZONE_CHANGED);
				break;
			}
			case R.id.vg_night_vision:{
				//launchActivityForResultByClassName(TimezoneListActivity.class.getName(), null, REQUEST_TIMEZONE_CHANGED);
				this.showDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
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
				monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamStatusTask(this), true, mStrVCamID,SwitchState.SWITCH_ON.equals(state)?"1":"0");
				break;
			}
			case R.id.sb_mic_sensitivity_switch:{
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
			case DIALOG_ID_WIFI_AP_KEYINDEX:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createKeyIdxDialog());
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							removeMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
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
	
	private View createKeyIdxDialog(){
		View viewRet = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			viewRet = (View)inflater.inflate(R.layout.wifi_ap_key_index_dialog, null);
			if(null != viewRet){
				ImageView[] ivKeyIdx = new ImageView[NetworkMgr.NUM_WEP_KEY_IDX]; 
				if(null != ivKeyIdx){
					ivKeyIdx[0] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_1_check);
					ivKeyIdx[1] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_2_check);
					ivKeyIdx[2] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_3_check);
					ivKeyIdx[3] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_4_check);
				}
				
				final ViewGroup[] vgKeyIdx = new ViewGroup[NetworkMgr.NUM_WEP_KEY_IDX];  
				if(null != vgKeyIdx){
					vgKeyIdx[0] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_1_holder);
					vgKeyIdx[1] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_2_holder);
					vgKeyIdx[2] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_3_holder);
					vgKeyIdx[3] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_4_holder);
				}
				
				OnClickListener keyIdxClick = new OnClickListener(){
					@Override
					public void onClick(View view) {
						removeMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
						for(int idx = 0; idx < NetworkMgr.NUM_WEP_KEY_IDX;idx++){
							if(vgKeyIdx[idx] == view){
//								mChosenWifiAPInfo.wepkeyIdx = idx;
//								if(null != mtxtKeyIndex){
//									mtxtKeyIndex.setText(String.valueOf(mChosenWifiAPInfo.wepkeyIdx+1));
//								}
								break;
							}
						}
					}};
				
				for(int idx = 0; idx < NetworkMgr.NUM_WEP_KEY_IDX;idx++){
					//ivKeyIdx[idx].setVisibility((idx == (mChosenWifiAPInfo.wepkeyIdx))?View.VISIBLE:View.INVISIBLE);
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
				//mbIsWifiSettingChanged = true;
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}

}
