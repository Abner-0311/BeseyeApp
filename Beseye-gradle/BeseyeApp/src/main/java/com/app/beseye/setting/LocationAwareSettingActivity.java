package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.LOCATION_LAT;
import static com.app.beseye.util.BeseyeJSONUtil.LOCATION_LONG;
import static com.app.beseye.util.BeseyeJSONUtil.LOCATION_OBJ;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.googlemap.*;

public class LocationAwareSettingActivity extends BeseyeBaseActivity 
								          implements OnSwitchBtnStateChangedListener{

	
	
	private BeseyeSwitchBtn mLocateSwitchBtn;
	private View mVwNavBar;
	private ViewGroup mVgAddMapLocation;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG)
			Log.i(TAG, "LocationAwareSettingActivity::onCreate()");
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
				txtTitle.setText(R.string.cam_setting_title_location);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
			BeseyeUtils.setToolbarPadding(mVwNavBar, 0);
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
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				//mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "PowerScheduleActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mLocateSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_schedule_switch);
		if(null != mLocateSwitchBtn){
			mLocateSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mVgAddMapLocation = (ViewGroup)findViewById(R.id.vg_location_add);
		if(null != mVgAddMapLocation){
			mVgAddMapLocation.setOnClickListener(this);
		}
		
		
	}
	
	@Override
	protected void onSessionComplete(){
		if(DEBUG)
			Log.i(TAG, "onSessionComplete()");	
		super.onSessionComplete();
	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "LocationAwareSettingActivity::onResume()");
		super.onResume();
		if(!mbFirstResume){
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_location_aware;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		//monitorAsyncTask(new BeseyeCamBEHttpTask.SetScheduleStatusTask(this), true, mStrVCamID,(SwitchState.SWITCH_ON.equals(state)?Boolean.TRUE:Boolean.FALSE).toString());
	}

	static public final int REQUEST_TURNOFF_AROUND 	 = 102;
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.vg_location_add:{
				//Toast.makeText(this, "IT's work", Toast.LENGTH_SHORT).show();
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(LocateOnGoogleMap.class.getName(),b, REQUEST_TURNOFF_AROUND);
				break;
			}
			default:{
					super.onClick(view);
				//Log.d(TAG, "CameraSettingActivity::onClick(), unhandled event by view:"+view);
			}
		}
	}

	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle, String strMsg) {
		super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
	

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					super.onPostExecute(task, result, iRetCode);
					
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_TURNOFF_AROUND == requestCode && resultCode == RESULT_OK){
			try{
				if(DEBUG)
					Log.e("LOCATION", "catch change location action");
				JSONObject locale_obj = new JSONObject(intent.getStringExtra(LocateOnGoogleMap.KEY_LOCALE_OBJ));
				if(null != mCam_obj){
					JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
					if(null != dataObj){
						JSONObject localeObj = BeseyeJSONUtil.getJSONObject(dataObj, LOCATION_OBJ);
						if(null != localeObj){
							BeseyeJSONUtil.setJSONDouble(localeObj, LOCATION_LAT, BeseyeJSONUtil.getJSONDouble(locale_obj, LOCATION_LAT));
							BeseyeJSONUtil.setJSONDouble(localeObj, LOCATION_LONG, BeseyeJSONUtil.getJSONDouble(locale_obj, LOCATION_LONG));
							Log.e("LOCATION", "new lat,lng =" + BeseyeJSONUtil.getJSONDouble(locale_obj, LOCATION_LAT) + "," + BeseyeJSONUtil.getJSONDouble(locale_obj, LOCATION_LONG));
						}
					}
				}
			}catch (JSONException e) {
				Log.e(TAG, "onActivityResult(), e:"+e.toString());
			}
			BeseyeJSONUtil.setJSONLong(mCam_obj, BeseyeJSONUtil.OBJ_TIMESTAMP, intent.getLongExtra(LocateOnGoogleMap.KEY_LOCALE_TS, System.currentTimeMillis()));
			BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
			
		}else
			super.onActivityResult(requestCode, resultCode, intent);
		
	}
	
	
	
}
