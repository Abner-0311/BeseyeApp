package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_TS;
import static com.app.beseye.setting.CamSettingMgr.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.OpeningPage;
import com.app.beseye.R;
import com.app.beseye.TimezoneListActivity;
import com.app.beseye.WifiListActivity;
import com.app.beseye.WifiSetupGuideActivity;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeTimePickerDialog;
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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PowerScheduleEditActivity extends BeseyeBaseActivity{
	
	public static final String KEY_SCHED_OBJ = "KEY_SCHED_OBJ";
	public static final String KEY_SCHED_EDIT_MODE = "KEY_SCHED_EDIT_MODE";
	
	private ImageView mIvTurnoffAllDayCheck, mIvTurnoffAllDayCheckBg;
	private ViewGroup mVgPickDays, mVgFromTime, mVgToTime;
	private Button mBtnRemove;
	private TextView mTxtTimeFrom, mTxtTimeTo, mTxtSchedDays;
	private String mStrVCamID = "Bes0001";
	private JSONObject mCam_obj, mSched_obj, mSched_obj_edit;
	private boolean mbEditMode = false;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "PowerScheduleEditActivity::onCreate()");
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mbEditMode = getIntent().getBooleanExtra(KEY_SCHED_EDIT_MODE, false);
		
		mBtnRemove = (Button)this.findViewById(R.id.button_remove);
		if(null != mBtnRemove){
			mBtnRemove.setOnClickListener(this);
			mBtnRemove.setVisibility(mbEditMode?View.VISIBLE:View.GONE);
		}
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_cam_list_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_menu_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
				mIvBack.setImageResource(R.drawable.sl_event_list_cancel);
			}
			
			ImageView mIvOK = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_add_cam_btn);
			if(null != mIvOK){
				mIvOK.setOnClickListener(this);
				mIvOK.setImageResource(R.drawable.sl_nav_ok_btn);
			}
			
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.cam_setting_schedule_edit_title);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				//mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
			
			if(mbEditMode){
				mSched_obj = new JSONObject(getIntent().getStringExtra(KEY_SCHED_OBJ)); 
				mSched_obj_edit = new JSONObject(getIntent().getStringExtra(KEY_SCHED_OBJ)); 
			}else{
				mSched_obj_edit = new JSONObject();
				JSONArray arrDays = new JSONArray();
				arrDays.put(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY);
				mSched_obj_edit.put(SCHED_DAYS, arrDays);
				mSched_obj_edit.put(SCHED_FROM, BeseyeUtils.DEFAULT_FROM_TIME);
				mSched_obj_edit.put(SCHED_TO, BeseyeUtils.DEFAULT_TO_TIME);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "PowerScheduleEditActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mTxtSchedDays = (TextView)findViewById(R.id.txt_turnoff_picker_desc);
		setScheduleDays();
		
		mVgPickDays = (ViewGroup)findViewById(R.id.vg_turnoff_picker);
		if(null != mVgPickDays){
			mVgPickDays.setOnClickListener(this);
		}
		
		int iFromTime = BeseyeJSONUtil.getJSONInt(mSched_obj_edit, SCHED_FROM);
		int iToTime = BeseyeJSONUtil.getJSONInt(mSched_obj_edit, SCHED_TO);
		boolean bAllDay = (BeseyeUtils.DAY_IN_SECONDS == (iToTime-iFromTime));
		
		mVgFromTime = (ViewGroup)findViewById(R.id.vg_turnoff_from);
		if(null != mVgFromTime){
			mVgFromTime.setOnClickListener(this);
			mTxtTimeFrom = (TextView)mVgFromTime.findViewById(R.id.txt_time_from);
			if(null != mTxtTimeFrom)
				mTxtTimeFrom.setText(BeseyeUtils.getTimeBySeconds(iFromTime));
		}
		
		mVgToTime = (ViewGroup)findViewById(R.id.vg_turnoff_to);
		if(null != mVgToTime){
			mVgToTime.setOnClickListener(this);
			mTxtTimeTo = (TextView)mVgToTime.findViewById(R.id.txt_time_to);
			if(null != mTxtTimeTo)
				mTxtTimeTo.setText(BeseyeUtils.getTimeBySeconds(iToTime));
		}
		
		mIvTurnoffAllDayCheck = (ImageView)findViewById(R.id.iv_turnoff_all_day_check);
		if(null != mIvTurnoffAllDayCheck){
			mIvTurnoffAllDayCheck.setVisibility(View.INVISIBLE);
		}
		
		mIvTurnoffAllDayCheckBg = (ImageView)findViewById(R.id.iv_turnoff_all_day_check_bg);
		if(null != mIvTurnoffAllDayCheckBg){
			mIvTurnoffAllDayCheckBg.setOnClickListener(this);
		}
		
		if(bAllDay)
			toggleTurnoffAllday();
	}
	
	private void setScheduleDays(){
		if(null != mTxtSchedDays){
			String strDays = BeseyeUtils.getSchdelDaysInShort(BeseyeJSONUtil.getJSONArray(mSched_obj_edit, SCHED_DAYS));
			if(null == strDays || 0 == strDays.length()){
				mTxtSchedDays.setVisibility(View.GONE);
			}else{
				mTxtSchedDays.setText(strDays);
			}
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_power_schdule_edit;
	}
	
	private void toggleTurnoffAllday(){
		if(null != mIvTurnoffAllDayCheck){
			boolean bToBeVisible = View.VISIBLE != mIvTurnoffAllDayCheck.getVisibility();
			mIvTurnoffAllDayCheck.setVisibility(bToBeVisible?View.VISIBLE:View.INVISIBLE);
			BeseyeUtils.setEnabled(mVgFromTime, !bToBeVisible);
			BeseyeUtils.setEnabled(mVgToTime, !bToBeVisible);
			
			if(null != mTxtTimeFrom)
				mTxtTimeFrom.setText(BeseyeUtils.getTimeBySeconds(bToBeVisible?0:BeseyeJSONUtil.getJSONInt(mSched_obj_edit, SCHED_FROM)));
			
			if(null != mTxtTimeTo)
				mTxtTimeTo.setText(BeseyeUtils.getTimeBySeconds(bToBeVisible?0:BeseyeJSONUtil.getJSONInt(mSched_obj_edit, SCHED_TO)));
		}
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.vg_turnoff_picker:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				b.putString(PowerScheduleEditActivity.KEY_SCHED_OBJ, mSched_obj_edit.toString());
				launchActivityForResultByClassName(PowerScheduleDayPickerActivity.class.getName(),b, REQUEST_DAY_PICK_CHANGED);
				break;
			}
			case R.id.iv_turnoff_all_day_check_bg:{
				toggleTurnoffAllday();
				break;
			}
			case R.id.vg_turnoff_from:{
				showTimePicker(mTxtTimeFrom, SCHED_FROM, getString(R.string.cam_setting_schedule_turnoff_from));
				break;
			}
			case R.id.vg_turnoff_to:{
				showTimePicker(mTxtTimeTo, SCHED_TO, getString(R.string.cam_setting_schedule_turnoff_to));
				break;
			}
			case R.id.button_remove:{
				showMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
				break;
			}
			case R.id.iv_nav_menu_btn:{
				showMyDialog(DIALOG_ID_CAM_SCHED_ABORT);
				break;
			}
			case R.id.iv_nav_add_cam_btn:{
				checkScheduleResult();
				break;
			}
			default:{
				super.onClick(view);
				//Log.d(TAG, "CameraSettingActivity::onClick(), unhandled event by view:"+view);
			}
		}
	}
	
	static public final int REQUEST_DAY_PICK_CHANGED = 1001;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_DAY_PICK_CHANGED == requestCode && resultCode == RESULT_OK){
			try {
				mSched_obj_edit = new JSONObject(intent.getStringExtra(KEY_SCHED_OBJ));
				setScheduleDays();
			} catch (JSONException e) {
				Log.e(TAG, "onActivityResult(), e:"+e.toString());
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_get_cam_info));
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
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
							int iCamStatus = getJSONInt(dataObj, CAM_STATUS, 0);
	
						}
					}
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	private void showTimePicker(final TextView viewToModify, final String strFieldTUpdate, String strTitle){
		
		BeseyeTimePickerDialog d = new BeseyeTimePickerDialog(this, BeseyeUtils.getTimeObjBySeconds(BeseyeJSONUtil.getJSONInt(mSched_obj_edit, strFieldTUpdate)), strTitle); 
		d.setOnDatetimePickerClickListener(new BeseyeTimePickerDialog.OnDatetimePickerClickListener(){
			@Override
			public void onBtnOKClick(Calendar pickDate) {
				if(!COMPUTEX_DEMO)
					Toast.makeText(PowerScheduleEditActivity.this, "onBtnOKClick(),pickDate="+pickDate.getTime().toLocaleString(), Toast.LENGTH_SHORT).show();
				
				BeseyeJSONUtil.setJSONInt(mSched_obj_edit, strFieldTUpdate, (pickDate.get(Calendar.HOUR_OF_DAY)*60+pickDate.get(Calendar.MINUTE))*60);
				
				if(null != viewToModify)
					viewToModify.setText(BeseyeUtils.getTimeBySeconds(BeseyeJSONUtil.getJSONInt(mSched_obj_edit, strFieldTUpdate)));
			}

			@Override
			public void onBtnCancelClick() {
				if(!COMPUTEX_DEMO)
					Toast.makeText(PowerScheduleEditActivity.this, "onBtnCancelClick(),", Toast.LENGTH_SHORT).show();
			}});
		
		d.show();
	}
	
	private boolean checkScheduleResult(){
		boolean bRet = false;
		boolean bTurnOffAllDay = (null != mIvTurnoffAllDayCheck && View.VISIBLE == mIvTurnoffAllDayCheck.getVisibility());

		int iFromTime = BeseyeJSONUtil.getJSONInt(mSched_obj_edit, SCHED_FROM);
		int iToTime = BeseyeJSONUtil.getJSONInt(mSched_obj_edit, SCHED_TO);
		if(!bTurnOffAllDay && iFromTime == iToTime){
			Bundle b = new Bundle();
			b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_schedule_error_same_time));
			showMyDialog(DIALOG_ID_WARNING, b);
		}else{
			if(bTurnOffAllDay){
				iFromTime = 0;
				iToTime = BeseyeUtils.DAY_IN_SECONDS;
			}
			else if(iFromTime >= iToTime){
				iToTime+=BeseyeUtils.DAY_IN_SECONDS;
			}
			Toast.makeText(PowerScheduleEditActivity.this, (mbEditMode?"Save":"Add")+" schedule from "+iFromTime+" to "+iToTime, Toast.LENGTH_SHORT).show();
			JSONObject toSave;
			try {
				toSave = new JSONObject(mSched_obj_edit.toString());
				toSave.put(SCHED_FROM, iFromTime);
				toSave.put(SCHED_TO, iToTime);
				
				Intent intent = new Intent();
				intent.putExtra(KEY_SCHED_OBJ, toSave.toString());
				setResult(RESULT_OK, intent);
				finish();
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return bRet;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			showMyDialog(DIALOG_ID_CAM_SCHED_ABORT);
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_CAM_SCHED_DELETE:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(getString(R.string.cam_setting_schedule_delete_confirm));
				builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
				    	//do delete
				    }
				});
				builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
					}});
				
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
				break;
			}
			case DIALOG_ID_CAM_SCHED_ABORT:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(getString(R.string.cam_setting_schedule_abort_confirm));
				builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
				    	finish();
				    }
				});
				builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	removeMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
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
	
	protected void onCamSettingChangedCallback(JSONObject DataObj){
    	super.onCamSettingChangedCallback(DataObj);
    	if(null != DataObj){
			String strCamUID = BeseyeJSONUtil.getJSONString(DataObj, WS_ATTR_CAM_UID);
			long lTs = BeseyeJSONUtil.getJSONLong(DataObj, WS_ATTR_TS);
			if(mStrVCamID.equals(strCamUID)){
				if(!mActivityDestroy){
		    		if(!mActivityResume){
		    			setOnResumeRunnable(new Runnable(){
							@Override
							public void run() {
								monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(PowerScheduleEditActivity.this), true, mStrVCamID);
							}});
		    		}else{
		    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
		    		}
		    	}
			}
		}
    }
}
