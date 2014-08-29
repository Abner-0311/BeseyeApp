package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_TS;
import static com.app.beseye.setting.CamSettingMgr.*;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.OpeningPage;
import com.app.beseye.R;
import com.app.beseye.WifiListActivity;
import com.app.beseye.WifiSetupGuideActivity;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class CameraSettingActivity extends BeseyeBaseActivity 
								   implements OnSwitchBtnStateChangedListener{
	
	private BeseyeSwitchBtn mCamSwitchBtn;
	private TextView mTxtPowerDesc,  mTxtPowerTitle, mTxtViewUpDownTitle;
	private ImageView mIvViewUpDownCheck, mIvViewUpDownCheckBg;
	private ViewGroup mVgNotificationType, mVgFamilyRecognition, mVgCamInfo, mVgPowerSchedule, mVgLocationAware, mVgHWSettings, mVgSiren;
	
	private String mStrVCamSN = null;
	private String mStrVCamMacAddr = null;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "CameraSettingActivity::onCreate()");
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
				txtTitle.setText(R.string.cam_setting_title);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			//mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
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
		
//		mStrVCamID = getIntent().getStringExtra(CameraListActivity.KEY_VCAM_ID);
//		mStrVCamName = getIntent().getStringExtra(CameraListActivity.KEY_VCAM_NAME);
		mCamSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_camera_switch);
		if(null != mCamSwitchBtn){
			mCamSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mTxtPowerTitle = (TextView)findViewById(R.id.txt_power_title);
		
		mTxtPowerDesc = (TextView)findViewById(R.id.txt_power_desc);
		
		mVgCamInfo = (ViewGroup)findViewById(R.id.vg_cam_info);
		if(null != mVgCamInfo){
			mVgCamInfo.setOnClickListener(this);
		}
		
		mVgPowerSchedule = (ViewGroup)findViewById(R.id.vg_power_schedule);
		if(null != mVgPowerSchedule){
			mVgPowerSchedule.setOnClickListener(this);
		}
		
		mVgLocationAware = (ViewGroup)findViewById(R.id.vg_location_aware);
		if(null != mVgLocationAware){
			mVgLocationAware.setOnClickListener(this);
			TextView txtLoc = (TextView)mVgLocationAware.findViewById(R.id.txt_loc_aware);
			if(null != txtLoc){
				txtLoc.setText("Reboot cam !!!(Dev)");
			}
			
			if(SessionMgr.getInstance().getServerMode().ordinal() >= SERVER_MODE.MODE_STAGING.ordinal()){
				mVgLocationAware.setVisibility(View.GONE);
			}
		}
		
		mVgHWSettings = (ViewGroup)findViewById(R.id.vg_hw_settings);
		if(null != mVgHWSettings){
			mVgHWSettings.setOnClickListener(this);
		}
		
		mVgSiren = (ViewGroup)findViewById(R.id.vg_siren);
		if(null != mVgSiren){
			mVgSiren.setOnClickListener(this);
//			if(SessionMgr.getInstance().getServerMode().ordinal() >= SERVER_MODE.MODE_STAGING.ordinal()){
//				mVgSiren.setVisibility(View.GONE);
//			}
		}
		
		TextView txtSiren = (TextView)findViewById(R.id.txt_setting_emergency_siren);
		if(null != txtSiren){
			txtSiren.setText("Detach cam !!!(Dev)");
		}
		
		mVgNotificationType = (ViewGroup)findViewById(R.id.vg_notification_type);
		if(null != mVgNotificationType){
			mVgNotificationType.setOnClickListener(this);
		}
		
		mVgFamilyRecognition = (ViewGroup)findViewById(R.id.vg_family_recognition);
		if(null != mVgFamilyRecognition){
			mVgFamilyRecognition.setOnClickListener(this);
			mVgFamilyRecognition.setVisibility(View.GONE);
		}
	}
	
	@Override
	protected void onResume() {
		Log.i(TAG, "CameraSettingActivity::onResume()");
		super.onResume();
		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamStatusTask(this), true, mStrVCamID,"1");
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamStatusTask(this), true, mStrVCamID);
		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetLEDStatusTask(this), true, mStrVCamID,"0");
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetLEDStatusTask(this), true, mStrVCamID);
		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetSpeakerStatusTask(this), true, mStrVCamID,"1");
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetSpeakerStatusTask(this), true, mStrVCamID);
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetSpeakerVolumeTask(this), true, mStrVCamID,"60");
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetSpeakerVolumeTask(this), true, mStrVCamID);
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetMicStatusTask(this), true, mStrVCamID,"1");
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetMicStatusTask(this), true, mStrVCamID);
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetMicGainTask(this), true, mStrVCamID,"30");
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetMicGainTask(this), true, mStrVCamID);
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetIRCutStatusTask(this), true, mStrVCamID,"1");
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetIRCutStatusTask(this), true, mStrVCamID);
//		
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetImageSettingTask(this), true, mStrVCamID);
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.RestartCamTask(this), true, mStrVCamID);
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.ReconnectMMTask(this), true, mStrVCamID);
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.SetWiFiConfigTask(this), true, mStrVCamID, "beseye", "0630BesEye", "3");
//		
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA)){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
		}else{
			updateSettingState();
		}
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetWiFiConfigTask(this), true, mStrVCamID);
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetWiFiSSIDListTask(this), true, mStrVCamID);
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetDateTimeTask(this), true, mStrVCamID);
//		monitorAsyncTask(new BeseyeCamBEHttpTask.GetSystemInfoTask(this), true, mStrVCamID);
		
		//monitorAsyncTask(new BeseyeCamBEHttpTask.SetImageSettingTask(this), true, mStrVCamID,"0","0","32","32","32","32","32","15");
	}
	
	private void updateSettingState(){
		BeseyeJSONUtil.CAM_CONN_STATUS iCamState = BeseyeJSONUtil.getVCamConnStatus(mCam_obj);// BeseyeJSONUtil.CAM_CONN_STATUS.toCamConnStatus(BeseyeJSONUtil.getJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, -1));
		if(null != mCamSwitchBtn){
			if(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED == iCamState){
				mCamSwitchBtn.setEnabled(false);
				mTxtPowerTitle.setEnabled(false);
			}else{
				mCamSwitchBtn.setEnabled(true);
				mTxtPowerTitle.setEnabled(true);
				mCamSwitchBtn.setSwitchState((BeseyeJSONUtil.CAM_CONN_STATUS.CAM_ON == iCamState)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
		}
		if(null != mIvViewUpDownCheck){
			if(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED == iCamState){
				mTxtViewUpDownTitle.setEnabled(false);
				mIvViewUpDownCheckBg.setEnabled(false);
				mIvViewUpDownCheck.setVisibility(View.INVISIBLE);
			}else{
				mTxtViewUpDownTitle.setEnabled(true);
				mIvViewUpDownCheckBg.setEnabled(true);
				mIvViewUpDownCheck.setVisibility((0 == BeseyeJSONUtil.getJSONInt(mCam_obj, CameraListActivity.KEY_VCAM_UPSIDEDOWN, 0))?View.INVISIBLE:View.VISIBLE);
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
		return R.layout.layout_setting;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamStatusTask(this), true, mStrVCamID,SwitchState.SWITCH_ON.equals(state)?"1":"0");
		updatePowerDesc(state);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.iv_video_upside_down_check_bg:{
				if(null != mIvViewUpDownCheck){
					mIvViewUpDownCheck.setVisibility((View.VISIBLE == mIvViewUpDownCheck.getVisibility())?View.INVISIBLE:View.VISIBLE);
					//CamSettingMgr.getInstance().setVideoUpsideDown(TMP_CAM_ID, (View.VISIBLE == mIvViewUpDownCheck.getVisibility())?1:0);
					BeseyeJSONUtil.setJSONInt(mCam_obj, CameraListActivity.KEY_VCAM_UPSIDEDOWN, (View.VISIBLE == mIvViewUpDownCheck.getVisibility())?1:0);
					setActivityResultWithCamObj();
				}
				break;
			}
			case R.id.vg_cam_info:{
//				if(null == mStrVCamSN)
//					monitorAsyncTask(new BeseyeCamBEHttpTask.GetSystemInfoTask(this), true, mStrVCamID);
//				else{
//					showDialog(DIALOG_ID_CAM_INFO);
//				}
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(CameraInfoActivity.class.getName(),b, REQUEST_CAM_INFO_CHANGED);
				break;
			}
			case R.id.btn_ok:{
				removeMyDialog(DIALOG_ID_CAM_INFO);
				break;
			}
			case R.id.vg_power_schedule:{
				//Toast.makeText(this, "Power", Toast.LENGTH_SHORT).show();
				//monitorAsyncTask(new BeseyeCamBEHttpTask.RestartCamTask(this), true, mStrVCamID);
				//monitorAsyncTask(new BeseyeCamBEHttpTask.SetWiFiConfigTask(this), true, mStrVCamID, "beseye", "0630BesEye", "3");
				//showMyDialog(DIALOG_ID_CAM_INFO);
				//showMyDialog(DIALOG_ID_CAM_REBOOT_CONFIRM);
				
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityByClassName(PowerScheduleActivity.class.getName(),b);
				break;
			}
			case R.id.vg_location_aware:{
				//launchActivityByClassName(TimezoneListActivity.class.getName());
				showMyDialog(DIALOG_ID_CAM_REBOOT_CONFIRM);
				break;
			}
			case R.id.vg_hw_settings:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityByClassName(HWSettingsActivity.class.getName(),b);
				break;
			}
			case R.id.vg_siren:{
				showMyDialog(DIALOG_ID_CAM_DETTACH_CONFIRM);
				break;
			}
			case R.id.vg_notification_type:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityByClassName(NotificationEventsSettingActivity.class.getName(),b);
				break;
			}
			default:
				super.onClick(view);
				//Log.d(TAG, "CameraSettingActivity::onClick(), unhandled event by view:"+view);
		}
	}
	
	private String mstrNameCandidate ;
	private boolean mbTriggerDetachAfterReboot = false;
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
								mstrNameCandidate = etCamName.getText().toString();
								if(null != mstrNameCandidate && 0 < mstrNameCandidate.length()){
									if(ASSIGN_ST_PATH){
										STREAM_PATH_LIST.set(0, mstrNameCandidate);
									}else if(null == mStrVCamName || !mStrVCamName.equals(mstrNameCandidate)){
										monitorAsyncTask(new BeseyeAccountTask.SetCamAttrTask(CameraSettingActivity.this), true, mStrVCamID, etCamName.getText().toString());
									}
								}else{
									Bundle b = new Bundle();
									b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_empty_name_warning));
									showMyDialog(DIALOG_ID_WARNING, b);
								}
								//CamSettingMgr.getInstance().setCamName(TMP_CAM_ID, etCamName.getText().toString());
							}
						}});
				}
            	break;
			}
			case DIALOG_ID_CAM_DETTACH_CONFIRM:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(String.format(getString(R.string.dialog_dettach_cam),mStrVCamName));
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_CAM_DETTACH_CONFIRM);
				    	monitorAsyncTask(new BeseyeAccountTask.CamDettachTask(CameraSettingActivity.this), true, mStrVCamID);	
				    	//mbTriggerDetachAfterReboot = true;
				    	//monitorAsyncTask(new BeseyeCamBEHttpTask.RestartCamTask(CameraSettingActivity.this).setDialogId(-1), true, mStrVCamID);
//				    	BeseyeUtils.postRunnable(new Runnable(){
//							@Override
//							public void run() {
//															
//							}}, 1000);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_CAM_DETTACH_CONFIRM);
					}});
				
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
				break;
			}
			case DIALOG_ID_CAM_REBOOT_CONFIRM:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(String.format(getString(R.string.dialog_reboot_cam),mStrVCamName));
				builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_CAM_REBOOT_CONFIRM);
				    	monitorAsyncTask(new BeseyeCamBEHttpTask.RestartCamTask(CameraSettingActivity.this), true, mStrVCamID);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_CAM_REBOOT_CONFIRM);
					}});
				
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
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
					etCamName.setText(null == mStrVCamName?CamSettingMgr.getInstance().getCamName(TMP_CAM_ID):mStrVCamName);
//					if(!ASSIGN_ST_PATH)
//						etCamName.setFocusable(false);
				}
				
				TextView txtSN = (TextView)viewRet.findViewById(R.id.txt_sn_value);
				if(null != txtSN){
					txtSN.setText(mStrVCamSN);
				}
				
				TextView txtMAC = (TextView)viewRet.findViewById(R.id.txt_mac_value);
				if(null != txtMAC){
					txtMAC.setText(mStrVCamMacAddr);
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

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
			onToastShow(task, "Notify SW Update Failed.");
		}else if(task instanceof BeseyeCamBEHttpTask.GetSystemInfoTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_get_cam_info));
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
			
		}else if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_get_cam_info));
					showMyDialog(DIALOG_ID_WARNING, b);
					
					BeseyeJSONUtil.setVCamConnStatus(mCam_obj, CAM_CONN_STATUS.CAM_DISCONNECTED);
					
					//BeseyeJSONUtil.setJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED.getValue());
					updatePowerDesc(SwitchState.SWITCH_OFF);
					updateSettingState();
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetCamStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_update_cam_status));
					showMyDialog(DIALOG_ID_WARNING, b);
					
					if(null != mCamSwitchBtn){
						mCamSwitchBtn.setSwitchState((mCamSwitchBtn.getSwitchState()==SwitchState.SWITCH_ON)?SwitchState.SWITCH_OFF:SwitchState.SWITCH_ON);
					}
				}}, 0);
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					super.onPostExecute(task, result, iRetCode);
					updatePowerDesc((BeseyeJSONUtil.getVCamConnStatus(mCam_obj).equals(CAM_CONN_STATUS.CAM_ON))?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
					updateSettingState();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetSystemInfoTask){
				if(0 == iRetCode){
					JSONObject obj = result.get(0);
					if(null != obj){
						Log.i(TAG, "onPostExecute(), "+obj.toString());
						JSONObject dataObj = BeseyeJSONUtil.getJSONObject(obj, ACC_DATA);
						if(null != dataObj){
							mStrVCamSN = BeseyeJSONUtil.getJSONString(dataObj, BeseyeJSONUtil.CAM_SN);
							mStrVCamMacAddr = BeseyeJSONUtil.getJSONString(dataObj, BeseyeJSONUtil.CAM_MAC_ADDR);
							showMyDialog(DIALOG_ID_CAM_INFO);
						}
					}
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetCamStatusTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject obj = result.get(0);
					if(null != obj){
						BeseyeJSONUtil.setVCamConnStatus(mCam_obj, (BeseyeJSONUtil.getJSONInt(obj, BeseyeJSONUtil.CAM_STATUS)==1)?CAM_CONN_STATUS.CAM_ON:CAM_CONN_STATUS.CAM_OFF);
						BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
						BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
						setActivityResultWithCamObj();	
						setResult(RESULT_OK);
					}
				}
			}else if(task instanceof BeseyeCamBEHttpTask.UpdateCamSWTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					onToastShow(task, "Notify SW Update Successfully.");
				}
			}else if(task instanceof BeseyeAccountTask.CamDettachTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					onToastShow(task, "Detach  Successfully.");
					if(BeseyeConfig.COMPUTEX_PAIRING){
						//invokeLogout();
						launchDelegateActivity(CameraListActivity.class.getName(), null);
					}else{
						Bundle b = new Bundle();
						b.putBoolean(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
						launchDelegateActivity(WifiListActivity.class.getName(), b);
					}
				}
			}else if(task instanceof BeseyeAccountTask.SetCamAttrTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					onToastShow(task, "Change cam name Successfully.");
					mStrVCamName = mstrNameCandidate;
					BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME, mstrNameCandidate);
					setActivityResultWithCamObj();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetImageSettingTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.GetImageSettingTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else if(task instanceof BeseyeCamBEHttpTask.RestartCamTask){
				if(0 == iRetCode){
					onToastShow(task, "Reboot cam Successfully.");
					Log.i(TAG, "onPostExecute(), "+result.toString());
				}else{
					onToastShow(task, "Reboot cam failed.");
				}
				
				if(mbTriggerDetachAfterReboot){
					monitorAsyncTask(new BeseyeAccountTask.CamDettachTask(CameraSettingActivity.this), true, mStrVCamID);
					//mbTriggerDetachAfterReboot = false;
				}
				
			}else if(task instanceof BeseyeCamBEHttpTask.ReconnectMMTask){
				if(0 == iRetCode)
					Log.i(TAG, "onPostExecute(), "+result.toString());
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.i(TAG, "onConfigurationChanged(), "+newConfig.toString());
		super.onConfigurationChanged(newConfig);
		
		// Checks the orientation of the screen
	    if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
	       // Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
	    } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
	       // Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
	    }
	}
	
	static public final int REQUEST_CAM_INFO_CHANGED = 2001;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.w(TAG, "CameraSettingActivity::onActivityResult(), requestCode:"+requestCode+", resultCode:"+resultCode);
		if(REQUEST_CAM_INFO_CHANGED == requestCode && resultCode == RESULT_OK){
			try {
				mCam_obj = new JSONObject(intent.getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
				setActivityResultWithCamObj();
				setResult(RESULT_OK);
			} catch (JSONException e) {
				Log.e(TAG, "onActivityResult(), e:"+e.toString());
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
}
