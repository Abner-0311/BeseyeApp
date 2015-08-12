/*
 * Communication between PowerScheduleEditActivity.java and PowerScheduleDayPickerActivity.java
 * 
 * JSON format:
 * 	sched_local: {
 * 		Day: [<int> ...]
 * 		From: <int>
 * 		To: <int>
 * 	}
 * 
 * Convert to communication format by serverToLocalFormat();
 * Use Communication format for the rest of this file
 * 
 * Convert to Server BE JSON format when save bottom is clicked.
 * 
 */



package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_DAYS;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_ENABLE;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_PERIOD;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_ACTION;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_LOCAL_DAY;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_LOCAL_FROM;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_LOCAL_TO;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_INDEX;


import java.util.Calendar;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseTwoBtnDialog;
import com.app.beseye.widget.BaseTwoBtnDialog.OnTwoBtnClickListener;
import com.app.beseye.widget.BeseyeTimePickerDialog;

public class PowerScheduleEditActivity extends BeseyeBaseActivity{
	
	public static final String KEY_SCHED_IDX 		= "KEY_SCHED_IDX";
	public static final String KEY_SCHED_OBJ 		= "KEY_SCHED_OBJ";
	public static final String KEY_SCHED_TS			= "KEY_SCHED_TS";
	public static final String KEY_SCHED_OBJ_DEL 	= "KEY_SCHED_OBJ_DEL";
	public static final String KEY_SCHED_EDIT_MODE 	= "KEY_SCHED_EDIT_MODE";
	public static final String KEY_SCHED_CONFLICT  	= "KEY_SCHED_CONFLICT";
	public static final String KEY_SCHED_CONFLICT_WILL_BE  = "KEY_SCHED_CONFLICT_WILL_BE";
	
	
	private ImageView mIvTurnoffAllDayCheck;//, mIvTurnoffAllDayCheckBg;
	private ViewGroup mVgPickDays, mVgFromTime, mVgToTime, mVgTurnOffAllDay;
	private Button mBtnRemove;
	private TextView mTxtTimeFrom, mTxtTimeTo, mTxtSchedDays;
	private String mStrSchedIdx = null;
	private JSONObject mSched_obj_edit, mSched_local, mSched_local_old;
	private boolean mbEditMode = false;
	private BeseyeCamBEHttpTask.ModifyScheduleTask mAddTask, mUpdateTask, mDeleteTask;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			}
			
			if(mbEditMode){
				mStrSchedIdx = getIntent().getStringExtra(KEY_SCHED_IDX);
				mSched_obj_edit = new JSONObject(getIntent().getStringExtra(KEY_SCHED_OBJ)); 
			}else{
				mSched_obj_edit = new JSONObject();
				mSched_obj_edit.put(SCHED_ACTION, BeseyeJSONUtil.SCHED_ACTION_ADD);

				//[Abner Review 0812]allArrDays is not a suitable name. Maybe arrSched
				JSONArray allArrDays = new JSONArray();
				//[Abner Review 0812]arrDays is not a suitable name. Maybe arrSubSched
				JSONArray arrDays = new JSONArray();
				arrDays.put(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY);
				arrDays.put(BeseyeUtils.DEFAULT_FROM_TIME);
				arrDays.put(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY + 1);
				arrDays.put(BeseyeUtils.DEFAULT_TO_TIME);	
				
				allArrDays.put(arrDays);
				mSched_obj_edit.put(SCHED_DAYS, allArrDays);

				mSched_obj_edit.put(SCHED_PERIOD, BeseyeJSONUtil.SCHED_DEFAULT_PEROID);
				mSched_obj_edit.put(SCHED_ENABLE, true);
			}
			mSched_local = new JSONObject();
			//Convert to communication format
			serverToLocalFormat();
			mSched_local_old  = new JSONObject(mSched_local.toString());
		} catch (JSONException e1) {
			Log.e(TAG, "PowerScheduleEditActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mTxtSchedDays = (TextView)findViewById(R.id.txt_turnoff_picker_desc);
		setScheduleDays();
		
		mVgPickDays = (ViewGroup)findViewById(R.id.vg_turnoff_picker);
		if(null != mVgPickDays){
			mVgPickDays.setOnClickListener(this);
		}
		
		int iFromTime = BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_FROM);
		int iToTime = BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_TO);
		boolean bAllDay = (iToTime == 0 && iFromTime == 0);
	
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
		
		mVgTurnOffAllDay = (ViewGroup)findViewById(R.id.vg_turnoff_all_day);
		if(null != mVgTurnOffAllDay){
			mVgTurnOffAllDay.setOnClickListener(this);
		}
		
		mIvTurnoffAllDayCheck = (ImageView)findViewById(R.id.iv_turnoff_all_day_check);
		if(null != mIvTurnoffAllDayCheck){
			mIvTurnoffAllDayCheck.setVisibility(View.INVISIBLE);
		}
				
		if(bAllDay)
			toggleTurnoffAllday();
	}
	
	private void serverToLocalFormat(){
		JSONArray workDays = BeseyeJSONUtil.getJSONArray(mSched_obj_edit, SCHED_DAYS);
		try {
			JSONArray arrDays = new JSONArray();
			for(int idx=0; idx<workDays.length(); idx++){
				if(0 == idx){
					//[Abner Review 0812]Null pointer check after getJSONArray
					mSched_local.put(SCHED_LOCAL_FROM, workDays.getJSONArray(idx).getInt(1));
					mSched_local.put(SCHED_LOCAL_TO, workDays.getJSONArray(idx).getInt(3));
				}
				arrDays.put(workDays.getJSONArray(idx).getInt(0));
			}
			mSched_local.put(SCHED_LOCAL_DAY, arrDays);
		} catch (JSONException e1) {
			Log.e(TAG, "PowerScheduleEditActivity::serverToLocalFormat(), e1:"+e1.toString());
		}
	}
	
	private void setScheduleDays(){
		if(null != mTxtSchedDays){
			String strDays = BeseyeUtils.getSchdelLocalDaysInShort(BeseyeJSONUtil.getJSONArray(mSched_local, SCHED_LOCAL_DAY));
			if(null == strDays || 0 == strDays.length()){
				mTxtSchedDays.setVisibility(View.GONE);
			}else{
				mTxtSchedDays.setText(strDays);
			}
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
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
				mTxtTimeFrom.setText(BeseyeUtils.getTimeBySeconds(bToBeVisible?0:BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_FROM)));
			
			if(null != mTxtTimeTo)
				mTxtTimeTo.setText(BeseyeUtils.getTimeBySeconds(bToBeVisible?0:BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_TO)));
		}
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.vg_turnoff_picker:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				b.putString(PowerScheduleEditActivity.KEY_SCHED_OBJ, mSched_local.toString());
				launchActivityForResultByClassName(PowerScheduleDayPickerActivity.class.getName(),b, REQUEST_DAY_PICK_CHANGED);
				break;
			}
			case R.id.vg_turnoff_all_day:{
				toggleTurnoffAllday();
				break;
			}
			case R.id.vg_turnoff_from:{
				showTimePicker(mTxtTimeFrom, SCHED_LOCAL_FROM, getString(R.string.cam_setting_schedule_turnoff_from));
				break;
			}
			case R.id.vg_turnoff_to:{
				showTimePicker(mTxtTimeTo, SCHED_LOCAL_TO, getString(R.string.cam_setting_schedule_turnoff_to));
				break;
			}
			case R.id.button_remove:{
				showMyDialog(DIALOG_ID_CAM_SCHED_DELETE);
				break;
			}
			case R.id.iv_nav_menu_btn:{
				if(!checkDIfference()){
					finish();
				}
				break;
			}
			case R.id.iv_nav_add_cam_btn:{
				checkScheduleResult();
				break;
			}
			default:{
				super.onClick(view);
			}
		}
	}
	
	static public final int REQUEST_DAY_PICK_CHANGED = 1001;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_DAY_PICK_CHANGED == requestCode && resultCode == RESULT_OK){
			try {
				mSched_local = new JSONObject(intent.getStringExtra(KEY_SCHED_OBJ));
				setScheduleDays();
			} catch (JSONException e) {
				Log.e(TAG, "onActivityResult(), e:"+e.toString());
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_get_cam_info));
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.ModifyScheduleTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					//[Abner review 0812] Better to show different warning msg for add/update/delete action
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_modify_schdule));
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.ModifyScheduleTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());

					if(task == mAddTask || task == mUpdateTask) {
						JSONObject toSave; 
						try {
							toSave = new JSONObject(mSched_obj_edit.toString());
							
							Intent intent = new Intent();
							intent.putExtra(KEY_SCHED_OBJ, toSave.toString());
							intent.putExtra(KEY_SCHED_IDX, String.valueOf(BeseyeJSONUtil.getJSONString(result.get(0), BeseyeJSONUtil.SCHED_INDEX)));
							intent.putExtra(KEY_SCHED_TS, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
							intent.putExtra(KEY_SCHED_CONFLICT, BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.SCHED_CONFLICT));
							intent.putExtra(KEY_SCHED_CONFLICT_WILL_BE, BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.SCHED_CONFLICT_WILL_BE));
							setResult(RESULT_OK, intent);
							finish();
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}else {
						Intent intent = new Intent();
						intent.putExtra(KEY_SCHED_OBJ_DEL, true);
						intent.putExtra(KEY_SCHED_IDX, mStrSchedIdx);
						intent.putExtra(KEY_SCHED_TS, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
						intent.putExtra(KEY_SCHED_CONFLICT, BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.SCHED_CONFLICT));
						intent.putExtra(KEY_SCHED_CONFLICT_WILL_BE, BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.SCHED_CONFLICT_WILL_BE));
						setResult(RESULT_OK, intent);
						finish();
					}	
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	private void showTimePicker(final TextView viewToModify, final String strFieldTUpdate, String strTitle){
		BeseyeTimePickerDialog d = new BeseyeTimePickerDialog(this, BeseyeUtils.getTimeObjBySeconds(BeseyeJSONUtil.getJSONInt(mSched_local, strFieldTUpdate)), strTitle); 
		d.setOnDatetimePickerClickListener(new BeseyeTimePickerDialog.OnDatetimePickerClickListener(){
			@Override
			public void onBtnOKClick(Calendar pickDate) {
//				if(!COMPUTEX_DEMO)
//					Toast.makeText(PowerScheduleEditActivity.this, "onBtnOKClick(),pickDate="+pickDate.getTime().toLocaleString(), Toast.LENGTH_SHORT).show();
				
				BeseyeJSONUtil.setJSONInt(mSched_local, strFieldTUpdate, (pickDate.get(Calendar.HOUR_OF_DAY)*60+pickDate.get(Calendar.MINUTE))*60);
				
				if(null != viewToModify)
					viewToModify.setText(BeseyeUtils.getTimeBySeconds(BeseyeJSONUtil.getJSONInt(mSched_local, strFieldTUpdate)));
			}

			@Override
			public void onBtnCancelClick() {
//				if(!COMPUTEX_DEMO)
//					Toast.makeText(PowerScheduleEditActivity.this, "onBtnCancelClick(),", Toast.LENGTH_SHORT).show();
			}});
		
		d.show();
	}
	
	private boolean checkScheduleResult(){
		boolean bRet = false;
		boolean bTurnOffAllDay = (null != mIvTurnoffAllDayCheck && View.VISIBLE == mIvTurnoffAllDayCheck.getVisibility());
		boolean bNextDay = false;
		
		int iFromTime = BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_FROM);
		int iToTime = BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_TO);
		JSONArray arrWorkDays = BeseyeJSONUtil.getJSONArray(mSched_local, SCHED_LOCAL_DAY);
		
		if(!bTurnOffAllDay && iFromTime == iToTime){
			Bundle b = new Bundle();
			b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_schedule_error_same_time));
			showMyDialog(DIALOG_ID_WARNING, b);
		}else{	
			//Convert to Server BE JSON format
			try {
				if(bTurnOffAllDay){
					iFromTime = 0;
					iToTime = 0;
				}
				if(iFromTime >= iToTime) {
					bNextDay = true;
				}
				JSONArray allArrDays = new JSONArray();
				for(int i=0; i<arrWorkDays.length(); i++) {
					JSONArray arrDays = new JSONArray();
					arrDays.put(arrWorkDays.getInt(i));
					arrDays.put(iFromTime);
					arrDays.put(bNextDay?arrWorkDays.getInt(i)+1:arrWorkDays.getInt(i));
					arrDays.put(iToTime);	
					allArrDays.put(arrDays);
				}	
				mSched_obj_edit.put(SCHED_DAYS, allArrDays);
				
				if(false == mbEditMode){
					// Action: Add
					mAddTask = new BeseyeCamBEHttpTask.ModifyScheduleTask(this);
					monitorAsyncTask(mAddTask, true, mStrVCamID, mSched_obj_edit.toString());
				}else{
					// Action: Update
					mSched_obj_edit.put(SCHED_ACTION, BeseyeJSONUtil.SCHED_ACTION_UPDATE);
					mSched_obj_edit.put(SCHED_INDEX, mStrSchedIdx);
					mUpdateTask = new BeseyeCamBEHttpTask.ModifyScheduleTask(this);
					monitorAsyncTask(mUpdateTask, true, mStrVCamID, mSched_obj_edit.toString());
				}	
				
			} catch (JSONException e1) {
				Log.e(TAG, "PowerScheduleEditActivity::checkScheduleResult(), e1:"+e1.toString());
			}
		}
		return bRet;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(!checkDIfference())
				finish();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}
	
	private boolean checkDIfference(){
		boolean bRet = true;
		if(mbEditMode){
			if(null != mSched_local_old){
				boolean bTurnOffAllDay = (null != mIvTurnoffAllDayCheck && View.VISIBLE == mIvTurnoffAllDayCheck.getVisibility());

				int iFromTime = BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_FROM);
				int iToTime = BeseyeJSONUtil.getJSONInt(mSched_local, SCHED_LOCAL_TO);
				if(bTurnOffAllDay){
					iFromTime = 0;
					iToTime = 0;
				}
				
				if((BeseyeJSONUtil.getJSONInt(mSched_local_old, SCHED_LOCAL_FROM) == iFromTime) && 
				   (BeseyeJSONUtil.getJSONInt(mSched_local_old, SCHED_LOCAL_TO) == iToTime) && 
				   (BeseyeJSONUtil.getJSONArray(mSched_local_old, SCHED_LOCAL_DAY).equals(BeseyeJSONUtil.getJSONArray(mSched_local, SCHED_LOCAL_DAY)))){
					bRet = false;
				}
			}
		}
		
		if(bRet)
			showMyDialog(DIALOG_ID_CAM_SCHED_ABORT);
		
		return bRet;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_CAM_SCHED_DELETE:{		
				BaseTwoBtnDialog d = new BaseTwoBtnDialog(this);
				d.setBodyText(getString(R.string.cam_setting_schedule_delete_confirm));
				d.setTitleText(getString(R.string.dialog_title_warning));

				d.setOnTwoBtnClickListener(new OnTwoBtnClickListener(){
					@Override
					public void onBtnYesClick() {
						try {
							mSched_obj_edit.put(SCHED_INDEX, mStrSchedIdx);
							mSched_obj_edit.put(SCHED_ACTION, BeseyeJSONUtil.SCHED_ACTION_DELETE);
						} catch (JSONException e1) {
							Log.e(TAG, "PowerScheduleEditActivity DIALOG_ID_CAM_SCHED_DELETE, e1:"+e1.toString());
						}
						mDeleteTask = new BeseyeCamBEHttpTask.ModifyScheduleTask(PowerScheduleEditActivity.this);
						monitorAsyncTask(mDeleteTask, true, mStrVCamID, mSched_obj_edit.toString());
					}
					@Override
					public void onBtnNoClick() {
					}} );
				dialog = d;
				break;
			}
			case DIALOG_ID_CAM_SCHED_ABORT:{
				BaseTwoBtnDialog d = new BaseTwoBtnDialog(this);
				d.setBodyText(getString(R.string.cam_setting_schedule_abort_confirm));
				d.setTitleText(getString(R.string.dialog_title_warning));

				d.setOnTwoBtnClickListener(new OnTwoBtnClickListener(){
					@Override
					public void onBtnYesClick() {
						finish();
					}
					@Override
					public void onBtnNoClick() {
					}} );
				dialog = d;
				break;
			}
			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
}
