package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_TS;
import static com.app.beseye.setting.CamSettingMgr.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class PowerScheduleActivity extends BeseyeBaseActivity 
								   implements OnSwitchBtnStateChangedListener{
	
	private BeseyeSwitchBtn mScheduleSwitchBtn;
	private ViewGroup mVgAddPowerSchedule, mVgPowerScheduleContainer;
	private List<ViewGroup> mArrVgSchedules;

	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "PowerScheduleActivity::onCreate()");
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mArrVgSchedules = new ArrayList<ViewGroup>();
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
						
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.cam_setting_title_power_time);
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
			Log.e(TAG, "PowerScheduleActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mScheduleSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_schedule_switch);
		if(null != mScheduleSwitchBtn){
			mScheduleSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mVgAddPowerSchedule = (ViewGroup)findViewById(R.id.vg_schedule_add);
		if(null != mVgAddPowerSchedule){
			mVgAddPowerSchedule.setOnClickListener(this);
		}
		
		mVgPowerScheduleContainer = (ViewGroup)findViewById(R.id.vg_power_schedule_itm_container);
		try {
			addScheduleItm(new JSONObject("{\""+SCHED_FROM+"\":3600, \""+SCHED_TO+"\":36000, \""+SCHED_DAYS+"\":[0,1,3,5,6]}"));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void addScheduleItm(JSONObject objSchdl){
		if(null != objSchdl){
			if(null != mVgPowerScheduleContainer){
				if(null != mArrVgSchedules){
					ViewGroup vgScheduleItm = (ViewGroup) getLayoutInflater().inflate(R.layout.layout_power_schdule_itm, null);
					if(null != vgScheduleItm){
						ScheduleItmHolder holder = new ScheduleItmHolder();
						holder.objSchdl = objSchdl;
						holder.txtSchdlDays = (TextView)vgScheduleItm.findViewById(R.id.txt_schedule_days);
						if(null != holder.txtSchdlDays){
							holder.txtSchdlDays.setText(BeseyeUtils.getSchdelDaysInShort(BeseyeJSONUtil.getJSONArray(holder.objSchdl, SCHED_DAYS)));
						}
						holder.txtSchdlPeriod = (TextView)vgScheduleItm.findViewById(R.id.txt_schedule_period);
						if(null != holder.txtSchdlPeriod){
							int iFromTime = BeseyeJSONUtil.getJSONInt(holder.objSchdl, SCHED_FROM);
							int iToTime = BeseyeJSONUtil.getJSONInt(holder.objSchdl, SCHED_TO);
							boolean bAllDay = (BeseyeUtils.DAY_IN_SECONDS == (iToTime-iFromTime));
							if(bAllDay){
								holder.txtSchdlPeriod.setText(getString(R.string.cam_setting_all_day_indicator));
							}else{
								holder.txtSchdlPeriod.setText(String.format(getString(R.string.cam_setting_desc_turnoff_during), 
										BeseyeUtils.getTimeBySeconds(iFromTime),
										BeseyeUtils.getTimeBySeconds(iToTime))
										+(BeseyeUtils.DAY_IN_SECONDS<=iToTime?getString(R.string.cam_setting_next_day_indicator):""));
							}
							
						}
						
						vgScheduleItm.setTag(holder);
						vgScheduleItm.setOnClickListener(this);
						
						mVgPowerScheduleContainer.addView(vgScheduleItm, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
						mArrVgSchedules.add(vgScheduleItm);
					}
				}
			}
		}
	}
	
	static class ScheduleItmHolder{
		TextView txtSchdlDays;
		TextView txtSchdlPeriod;
		JSONObject objSchdl;
	} 
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
	}
	
	private void updateSheduleState(){
		CAM_CONN_STATUS iCamState =  CAM_CONN_STATUS.toCamConnStatus(BeseyeJSONUtil.getJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, -1));
		if(null != mScheduleSwitchBtn){
			if(CAM_CONN_STATUS.CAM_DISCONNECTED == iCamState){
				mScheduleSwitchBtn.setEnabled(false);
			}else{
				mScheduleSwitchBtn.setEnabled(true);
				mScheduleSwitchBtn.setSwitchState((CAM_CONN_STATUS.CAM_ON == iCamState)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_power_schdule;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {

		//monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamStatusTask(this), true, mStrVCamID,SwitchState.SWITCH_ON.equals(state)?"1":"0");
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.vg_schedule_add:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(PowerScheduleEditActivity.class.getName(),b, REQUEST_SCHEDULE_ADD);
				break;
			}
			default:{
				if(view.getTag() instanceof ScheduleItmHolder){
					ScheduleItmHolder holder = (ScheduleItmHolder)view.getTag();
					if(null != holder){
						Bundle b = new Bundle();
						b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
						b.putString(PowerScheduleEditActivity.KEY_SCHED_OBJ, holder.objSchdl.toString());
						b.putBoolean(PowerScheduleEditActivity.KEY_SCHED_EDIT_MODE, true);
						launchActivityForResultByClassName(PowerScheduleEditActivity.class.getName(),b, REQUEST_SCHEDULE_CHANGED);
					}
				}else 
					super.onClick(view);
				//Log.d(TAG, "CameraSettingActivity::onClick(), unhandled event by view:"+view);
			}
		}
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
					updateSheduleState();
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
							updateSheduleState();
						}
					}
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	static public final int REQUEST_SCHEDULE_CHANGED = 101;
	static public final int REQUEST_SCHEDULE_ADD 	 = 102;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_SCHEDULE_CHANGED == requestCode && resultCode == RESULT_OK){
			try {
				JSONObject sched_obj_edit = new JSONObject(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ));
				//setScheduleDays();
			} catch (JSONException e) {
				Log.e(TAG, "onActivityResult(), e:"+e.toString());
			}
		}else if(REQUEST_SCHEDULE_ADD == requestCode && resultCode == RESULT_OK){
			JSONObject sched_obj_edit;
			try {
				sched_obj_edit = new JSONObject(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ));
				addScheduleItm(sched_obj_edit);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
}
