package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.OBJ_TIMESTAMP;
import static com.app.beseye.util.BeseyeJSONUtil.SCHEDULE_STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_DAYS;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_LIST;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_OBJ;
import static com.app.beseye.util.BeseyeJSONUtil.SCHED_STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONBoolean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

public class PowerScheduleActivity extends BeseyeBaseActivity 
								   implements OnSwitchBtnStateChangedListener{
	
	private BeseyeSwitchBtn mScheduleSwitchBtn;
	private ViewGroup mVgAddPowerSchedule, mVgPowerScheduleContainer;
	private List<ViewGroup> mArrVgSchedules;

	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
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
	}
	
	private void addScheduleItm(String strIdx, JSONObject objSchdl){
		if(null != objSchdl){
			if(null != mVgPowerScheduleContainer){
				if(null != mArrVgSchedules){
					ViewGroup vgScheduleItm = (ViewGroup) getLayoutInflater().inflate(R.layout.layout_power_schdule_itm, null);
					if(null != vgScheduleItm){
						ScheduleItmHolder holder = new ScheduleItmHolder();
						holder.strSchdIdx = strIdx;
						holder.objSchdl = objSchdl;
						holder.txtSchdlDays = (TextView)vgScheduleItm.findViewById(R.id.txt_schedule_days);
						if(null != holder.txtSchdlDays){
							holder.txtSchdlDays.setText(BeseyeUtils.getSchdelDaysInShort(BeseyeJSONUtil.getJSONArray(holder.objSchdl, SCHED_DAYS)));
						}
						holder.txtSchdlPeriod = (TextView)vgScheduleItm.findViewById(R.id.txt_schedule_period);
						if(null != holder.txtSchdlPeriod){
							int iFromTime = BeseyeUtils.DEFAULT_FROM_TIME;
							int iToTime = BeseyeUtils.DEFAULT_TO_TIME;
							try {
								// The Format of WorkDay is [Day, From, Day, To]
								// Use the first array to get the time 
								if(null != BeseyeJSONUtil.getJSONArray(holder.objSchdl, SCHED_DAYS)){
									JSONArray objSchdlFirstItem = BeseyeJSONUtil.getJSONArray(holder.objSchdl, SCHED_DAYS).getJSONArray(0);
									if(null != objSchdlFirstItem){
										iFromTime = objSchdlFirstItem.getInt(1);
										iToTime = objSchdlFirstItem.getInt(3);
									}
								}
							} catch (JSONException e1) {
								Log.e(TAG, "PowerScheduleActivity::addScheduleItm(), error to get time, e1:"+e1.toString());
							}
							boolean bAllDay = (iToTime == iFromTime);
							if(bAllDay){
								holder.txtSchdlPeriod.setText(getString(R.string.cam_setting_all_day_indicator));
							}else{
								holder.txtSchdlPeriod.setText(String.format(getString(R.string.cam_setting_desc_turnoff_during), 
										BeseyeUtils.getTimeBySeconds(iFromTime),
										BeseyeUtils.getTimeBySeconds(iToTime))
										+(iFromTime >= iToTime?getString(R.string.cam_setting_next_day_indicator):""));
							}
							
						}
						
						vgScheduleItm.setTag(holder);
						vgScheduleItm.setOnClickListener(this);
						
						mVgPowerScheduleContainer.addView(vgScheduleItm, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
						mArrVgSchedules.add(vgScheduleItm);
					}
				}
			}
		}
	}
	
	static class ScheduleItmHolder{
		TextView txtSchdlDays;
		TextView txtSchdlPeriod;
		String strSchdIdx;
		JSONObject objSchdl;
	} 
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(!mbFirstResume){
			updateScheduleStatus();
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				 monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_power_schdule;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		monitorAsyncTask(new BeseyeCamBEHttpTask.SetScheduleStatusTask(this), true, mStrVCamID,(SwitchState.SWITCH_ON.equals(state)?Boolean.TRUE:Boolean.FALSE).toString());
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
						b.putString(PowerScheduleEditActivity.KEY_SCHED_IDX, holder.strSchdIdx);
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
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, final int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(PowerScheduleActivity.this, R.string.cam_setting_fail_to_get_cam_info, iErrType));
					showMyDialog(DIALOG_ID_WARNING, b);
					updateScheduleStatus();
				}}, 0);
		}else if(task instanceof BeseyeCamBEHttpTask.SetScheduleStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(PowerScheduleActivity.this, R.string.cam_setting_fail_to_update_schdule_status, iErrType));
					showMyDialog(DIALOG_ID_WARNING, b);
					updateScheduleStatus();
				}}, 0);
			
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
	
	@Override
	protected void updateUICallback(){
		updateScheduleStatus();
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					super.onPostExecute(task, result, iRetCode);
					updateScheduleStatus();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetScheduleStatusTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.e(TAG, "PowerScheduleActivity::onPostExecute(), result.get(0)="+result.get(0).toString());
					
					checkConflict(BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.SCHED_CONFLICT), BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.SCHED_CONFLICT_WILL_BE));
					
					JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
					if(null != dataObj){
						JSONObject schedObj = BeseyeJSONUtil.getJSONObject(dataObj, SCHED_OBJ);
						if(null != schedObj){
							BeseyeJSONUtil.setJSONBoolean(schedObj, SCHED_STATUS, BeseyeJSONUtil.getJSONBoolean(result.get(0), SCHEDULE_STATUS));
							BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), OBJ_TIMESTAMP));
							BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
						}
					}
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	private void updateScheduleStatus(){
		if(null != mCam_obj){
			JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
			if(null != dataObj){
				JSONObject schedObj = BeseyeJSONUtil.getJSONObject(dataObj, SCHED_OBJ);
				if(null != schedObj){
					boolean bSchedStatus = getJSONBoolean(schedObj, SCHED_STATUS, false);
					if(null != mScheduleSwitchBtn){
						mScheduleSwitchBtn.setSwitchState(bSchedStatus?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
					}
					
					JSONObject schedListObj = BeseyeJSONUtil.getJSONObject(schedObj, SCHED_LIST);
					if(null != schedListObj){
						JSONArray arrScheduleIdx = schedListObj.names();
						int iLenSchedule = (null != arrScheduleIdx)?arrScheduleIdx.length():0;
						if(null != mVgPowerScheduleContainer){
							mVgPowerScheduleContainer.removeAllViews();
						}
						if(null != mArrVgSchedules){
							mArrVgSchedules.clear();
						}
						
						TreeMap<String, JSONObject> sortSchedItem = new TreeMap<String, JSONObject>();
						for(int idx = 0; idx < iLenSchedule;idx++){
							try {
								sortSchedItem.put(arrScheduleIdx.getString(idx), BeseyeJSONUtil.getJSONObject(schedListObj, arrScheduleIdx.getString(idx)));
							} catch (JSONException e1) {
								Log.e(TAG, "PowerScheduleActivity::updateScheduleStatus(), e1:"+e1.toString());
							}
						}
						
						for ( Iterator<String> iter = sortSchedItem.keySet().iterator(); iter.hasNext(); ) {
							String key = iter.next();
							addScheduleItm( key, sortSchedItem.get( key ) );
						}
					}else {			//if null == schedListObj
						if(null != mVgPowerScheduleContainer){
							mVgPowerScheduleContainer.removeAllViews();
						}
						if(null != mArrVgSchedules){
							mArrVgSchedules.clear();
						}
					}
				}
			}
		}
	}
	
	private void updateSchedueItm(String strIdx, JSONObject schedItmObj){
		if(null != strIdx && null != schedItmObj){
			if(null != mCam_obj){
				JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
				if(null != dataObj){
					JSONObject schedObj = BeseyeJSONUtil.getJSONObject(dataObj, SCHED_OBJ);
									
					if(null != schedObj){
						JSONObject schedListObj = BeseyeJSONUtil.getJSONObject(schedObj, SCHED_LIST);
						if(null != schedListObj){
							BeseyeJSONUtil.setJSONObject(schedListObj, strIdx, schedItmObj);
							updateScheduleStatus();
						}
					}
				}
			}
		}else{
			Log.e(TAG, "PowerScheduleActivity::updateSchedueItm(), invalid strIdx or schedItmObj");
		}
	}
	
	private void deleteSchedueItm(String strIdx){
		if(null != strIdx ){
			if(null != mCam_obj){
				JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
				if(null != dataObj){
					JSONObject schedObj = BeseyeJSONUtil.getJSONObject(dataObj, SCHED_OBJ);
					if(null != schedObj){
						JSONObject schedListObj = BeseyeJSONUtil.getJSONObject(schedObj, SCHED_LIST);
						if(null != schedListObj){
							schedListObj.remove(strIdx);
							updateScheduleStatus();
						}
					}
				}
			}
		}else{
			Log.e(TAG, "PowerScheduleActivity::deleteSchedueItm(), invalid strIdx = "+strIdx);
		}
	}
	
	static public final int REQUEST_SCHEDULE_CHANGED = 101;
	static public final int REQUEST_SCHEDULE_ADD 	 = 102;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_SCHEDULE_CHANGED == requestCode && resultCode == RESULT_OK){
			checkConflict(intent.getBooleanExtra(PowerScheduleEditActivity.KEY_SCHED_CONFLICT, true), intent.getBooleanExtra(PowerScheduleEditActivity.KEY_SCHED_CONFLICT_WILL_BE, false));
			
			try {
				boolean bDeleteCase =intent.getBooleanExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ_DEL, false);
				if(bDeleteCase){
					deleteSchedueItm(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_IDX));
				}else{
					JSONObject sched_obj_edit = new JSONObject(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ));
					updateSchedueItm(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_IDX), sched_obj_edit);
					updateScheduleStatus();
				}
				
				BeseyeJSONUtil.setJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP, intent.getLongExtra(PowerScheduleEditActivity.KEY_SCHED_TS, System.currentTimeMillis()));
				BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
				
			} catch (JSONException e) {
				Log.e(TAG, "onActivityResult(), e:"+e.toString());
			}
		}else if(REQUEST_SCHEDULE_ADD == requestCode && resultCode == RESULT_OK){
			checkConflict(intent.getBooleanExtra(PowerScheduleEditActivity.KEY_SCHED_CONFLICT, true), intent.getBooleanExtra(PowerScheduleEditActivity.KEY_SCHED_CONFLICT_WILL_BE, false));
			
			JSONObject sched_obj_edit;
			try {
				sched_obj_edit = new JSONObject(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ));
				if(null != mCam_obj){
					JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
					if(null != dataObj){
						JSONObject schedObj = BeseyeJSONUtil.getJSONObject(dataObj, SCHED_OBJ);
						if(null != schedObj){
							boolean bSchedStatus = getJSONBoolean(schedObj, SCHED_STATUS, false);
							if(null != mScheduleSwitchBtn){
								mScheduleSwitchBtn.setSwitchState(bSchedStatus?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
							}
							JSONObject schedListObj = BeseyeJSONUtil.getJSONObject(schedObj, SCHED_LIST);
							if(null == schedListObj){
								JSONObject schedFirstItemObj = new JSONObject();
								schedFirstItemObj.put(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_IDX), sched_obj_edit);
								schedObj.put(SCHED_LIST, schedFirstItemObj);
							}else{
								schedListObj.put(intent.getStringExtra(PowerScheduleEditActivity.KEY_SCHED_IDX), sched_obj_edit);
							}
							updateScheduleStatus();
							BeseyeJSONUtil.setJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP, intent.getLongExtra(PowerScheduleEditActivity.KEY_SCHED_TS, System.currentTimeMillis()));
							BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
						}
					}
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
	
	private void checkConflict(boolean schedConflict, boolean schedConflictWillBe){
		if(true == schedConflict){
			BaseOneBtnDialog d = new BaseOneBtnDialog(this);
			if(true == schedConflictWillBe){
				d.setBodyText(getString(R.string.cam_setting_schedule_conflict_be_on));
			}else{
				d.setBodyText(getString(R.string.cam_setting_schedule_conflict_be_off));
			}
			d.setTitleText(getString(R.string.signup_watch_out_title));
			d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
				@Override
				public void onBtnClick() {
				}});
			d.show();
		}
	}
}
