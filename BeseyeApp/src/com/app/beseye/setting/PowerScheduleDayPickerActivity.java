package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_TS;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;

import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class PowerScheduleDayPickerActivity extends BeseyeBaseActivity{
	static private final int DAY_OF_WEEK = 7;
	
	private ImageView mIvDayOfWeekCheck[], mIvDayOfWeekCheckBg[];
	private TextView mTxtSchedDays[];
	private JSONObject mSched_obj;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "PowerScheduleDayPickerActivity::onCreate()");
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
				txtTitle.setText(R.string.cam_setting_schedule_picker_title);
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
			
			mSched_obj = new JSONObject(getIntent().getStringExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ)); 
		} catch (JSONException e1) {
			Log.e(TAG, "PowerScheduleDayPickerActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mIvDayOfWeekCheck = new ImageView[DAY_OF_WEEK];
		mIvDayOfWeekCheckBg = new ImageView[DAY_OF_WEEK];
		mTxtSchedDays = new TextView[DAY_OF_WEEK];
		
		int iVgIds[] = {R.id.vg_sunday, R.id.vg_monday, R.id.vg_tuesday, R.id.vg_wednesday, R.id.vg_thursday, R.id.vg_friday, R.id.vg_saturday};
		for(int idx = 0; idx < DAY_OF_WEEK;idx++){
			ViewGroup vgDay = (ViewGroup)findViewById(iVgIds[idx]);
			if(null != vgDay){
				mIvDayOfWeekCheck[idx] = (ImageView)vgDay.findViewById(R.id.iv_day_check);
				BeseyeUtils.setVisibility(mIvDayOfWeekCheck[idx], View.INVISIBLE);
				
				mIvDayOfWeekCheckBg[idx] = (ImageView)vgDay.findViewById(R.id.iv_day_check_bg);
				if(null != mIvDayOfWeekCheckBg[idx]){
					mIvDayOfWeekCheckBg[idx].setTag(idx);
					mIvDayOfWeekCheckBg[idx].setOnClickListener(this);
				}
				
				mTxtSchedDays[idx] = (TextView)vgDay.findViewById(R.id.txt_day_title);
				if(null != mTxtSchedDays[idx]){
					mTxtSchedDays[idx].setText(BeseyeUtils.getSchdelDay(idx));
				}
			}
		}
		
		JSONArray arrDays = BeseyeJSONUtil.getJSONArray(mSched_obj, SCHED_DAYS);
		int iSize = (null != arrDays)?arrDays.length():0;
		for(int idx = 0;idx < iSize;idx++){
			try {
				BeseyeUtils.setVisibility(mIvDayOfWeekCheck[arrDays.getInt(idx)], View.VISIBLE);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		//monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_power_schdule_select_days;
	}

	@Override
	public void onClick(View view) {
		
		int idx = findIdxByView(view);
		if(0 <= idx){
			if(null != mIvDayOfWeekCheck[idx]){
				if(View.VISIBLE != mIvDayOfWeekCheck[idx].getVisibility()){
					mIvDayOfWeekCheck[idx].setVisibility(View.VISIBLE);
				}else if(1 < getNumOfChecked()){
					mIvDayOfWeekCheck[idx].setVisibility(View.INVISIBLE);
				}		
			}
		}else if(R.id.iv_nav_left_btn == view.getId()){
			setPickResult();
			finish();
		}else{
			super.onClick(view);
		}
	}
	
	private int getNumOfChecked(){
		int iRet = 0;
		for(int idx = 0; idx < DAY_OF_WEEK;idx++){
			if(View.VISIBLE == mIvDayOfWeekCheck[idx].getVisibility()){
				iRet++;
			}
		}
		return iRet;
	}
	
	private int findIdxByView(View view){
		int iRet = -1;
		for(int idx = 0; idx < DAY_OF_WEEK;idx++){
			if(view == mIvDayOfWeekCheckBg[idx]){
				iRet = idx;
				break;
			}
		}
		
		return iRet;
	}
	
	private void setPickResult(){
		JSONArray arrRet = new JSONArray();
		for(int idx = 0; idx < DAY_OF_WEEK;idx++){
			if(View.VISIBLE == mIvDayOfWeekCheck[idx].getVisibility()){
				arrRet.put(idx);
			}
		}
		
		if(null != mSched_obj){
			try {
				mSched_obj.put(SCHED_DAYS, arrRet);
				Intent intent = new Intent();
				intent.putExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ, mSched_obj.toString());
				setResult(RESULT_OK, intent);
			} catch (JSONException e1) {
				Log.e(TAG, "PowerScheduleDayPickerActivity::setPickResult(), failed to parse, e1:"+e1.toString());
			}
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			setPickResult();
		}
		
		return super.onKeyUp(keyCode, event);
	}
}
