package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class CameraSettingActivity extends BeseyeBaseActivity 
								   implements OnSwitchBtnStateChangedListener,
								   			  OnClickListener{
	private BeseyeSwitchBtn mCamSwitchBtn;
	private TextView mTxtPowerDesc,  mTxtPowerTitle, mTxtViewUpDownTitle;
	private ImageView mIvViewUpDownCheck, mIvViewUpDownCheckBg;
	private ViewGroup mVgWifiSetting, mVgCamInfo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "CameraSettingActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.wifisetup_wifi_title_bg));
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE); 
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setTitle(R.string.cam_setting_title);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mCamSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_camera_switch);
		if(null != mCamSwitchBtn){
			mCamSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mTxtPowerTitle = (TextView)findViewById(R.id.txt_power_title);
		
		mTxtPowerDesc = (TextView)findViewById(R.id.txt_power_desc);
		
		mTxtViewUpDownTitle = (TextView)findViewById(R.id.txt_video_upside_down_title);
		
		mIvViewUpDownCheck = (ImageView)findViewById(R.id.iv_video_upside_down_check);
		
		mIvViewUpDownCheckBg = (ImageView)findViewById(R.id.iv_video_upside_down_check_bg);
		if(null != mIvViewUpDownCheckBg){
			mIvViewUpDownCheckBg.setOnClickListener(this);
		}
		
		mVgWifiSetting = (ViewGroup)findViewById(R.id.vg_wifi_setting);
		if(null != mVgWifiSetting){
			mVgWifiSetting.setOnClickListener(this);
		}
		
		mVgCamInfo = (ViewGroup)findViewById(R.id.vg_cam_info);
		if(null != mVgCamInfo){
			mVgCamInfo.setOnClickListener(this);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateSettingState();
	}
	
	private void updateSettingState(){
		CAM_CONN_STATUS iCamState = CamSettingMgr.getInstance().getCamPowerState(TMP_CAM_ID);
		if(null != mCamSwitchBtn){
			if(CAM_CONN_STATUS.CAM_DISCONNECTED == iCamState){
				mCamSwitchBtn.setEnabled(false);
				mTxtPowerTitle.setEnabled(false);
			}else{
				mCamSwitchBtn.setEnabled(true);
				mTxtPowerTitle.setEnabled(true);
				mCamSwitchBtn.setSwitchState((CAM_CONN_STATUS.CAM_ON == iCamState)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
		}
		if(null != mIvViewUpDownCheck){
			if(CAM_CONN_STATUS.CAM_DISCONNECTED == iCamState){
				mTxtViewUpDownTitle.setEnabled(false);
				mIvViewUpDownCheckBg.setEnabled(false);
				mIvViewUpDownCheck.setVisibility(View.INVISIBLE);
			}else{
				mTxtViewUpDownTitle.setEnabled(true);
				mIvViewUpDownCheckBg.setEnabled(true);
				mIvViewUpDownCheck.setVisibility((0 == CamSettingMgr.getInstance().getVideoUpsideDown(TMP_CAM_ID))?View.INVISIBLE:View.VISIBLE);
			}
		}
		
		updatePowerDesc(mCamSwitchBtn.getSwitchState());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
			case android.R.id.home:{
				onBackPressed();
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_wifi_setting_page;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state) {
		CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.toCamConnStatus((SwitchState.SWITCH_ON.equals(state))?1:0));
		setResult(RESULT_OK);
		updatePowerDesc(state);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.iv_video_upside_down_check_bg:{
				if(null != mIvViewUpDownCheck){
					mIvViewUpDownCheck.setVisibility((View.VISIBLE == mIvViewUpDownCheck.getVisibility())?View.INVISIBLE:View.VISIBLE);
					CamSettingMgr.getInstance().setVideoUpsideDown(TMP_CAM_ID, (View.VISIBLE == mIvViewUpDownCheck.getVisibility())?1:0);
					setResult(RESULT_OK);
				}
				break;
			}
			case R.id.vg_wifi_setting:{
				Intent intent = new Intent();
				intent.setClass(this, WifiListActivity.class);
				intent.putExtra(WifiListActivity.KEY_CHANGE_WIFI_ONLY, true);
				startActivity(intent);
				break;
			}
			case R.id.vg_cam_info:{
				showMyDialog(DIALOG_ID_CAM_INFO);
				break;
			}
			case R.id.btn_ok:{
				removeMyDialog(DIALOG_ID_CAM_INFO);
				break;
			}
			default:
				Log.d(TAG, "CameraSettingActivity::onClick(), unhandled event by view:"+view);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "CameraSettingActivity::onCreateDialog()");
		Dialog dialog;
		switch(id){
			case DIALOG_ID_CAM_INFO:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createCamInfoDialog());
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							//removeMyDialog(DIALOG_ID_CAM_INFO);
						}});
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface dialog) {
							EditText etCamName = (EditText)((Dialog)dialog).findViewById(R.id.et_cam_name);
							if(null != etCamName){
								Log.i(TAG, "onDismiss(), cam name is "+etCamName.getText().toString());
								CamSettingMgr.getInstance().setCamName(TMP_CAM_ID, etCamName.getText().toString());
								if(ASSIGN_ST_PATH){
									STREAM_PATH_LIST.set(0, CamSettingMgr.getInstance().getCamName(TMP_CAM_ID));
								}
								setResult(RESULT_OK);
							}
						}});
				}
            	break;
			}

			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
	
	private View createCamInfoDialog(){
		View viewRet = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			viewRet = (View)inflater.inflate(R.layout.cam_info_dialog, null);
			if(null != viewRet){
				EditText etCamName = (EditText)viewRet.findViewById(R.id.et_cam_name);
				if(null != etCamName){
					etCamName.setText(CamSettingMgr.getInstance().getCamName(TMP_CAM_ID));
					if(!ASSIGN_ST_PATH)
						etCamName.setFocusable(false);
				}
				
				TextView txtSN = (TextView)viewRet.findViewById(R.id.txt_sn_value);
				if(null != txtSN){
					txtSN.setText(CamSettingMgr.getInstance().getCamSN(TMP_CAM_ID));
				}
				
				TextView txtMAC = (TextView)viewRet.findViewById(R.id.txt_mac_value);
				if(null != txtMAC){
					txtMAC.setText(CamSettingMgr.getInstance().getCamMAC(TMP_CAM_ID));
				}
				
				Button btnOK = (Button)viewRet.findViewById(R.id.btn_ok);
				if(null != btnOK){
					btnOK.setOnClickListener(this);
				}
			}
		}
		return viewRet;
	}

	private void updatePowerDesc(SwitchState state){
		if(null != mTxtPowerDesc){
			mTxtPowerDesc.setText(String.format(getResources().getString(R.string.cam_setting_title_power_desc), 
												getResources().getString((SwitchState.SWITCH_ON.equals(state))?R.string.cam_setting_title_power_on:R.string.cam_setting_title_power_off)));
		}
	}
}
