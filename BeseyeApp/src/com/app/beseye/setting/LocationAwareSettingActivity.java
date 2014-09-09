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
import com.app.beseye.WifiListActivity;
import com.app.beseye.WifiSetupGuideActivity;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
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
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class LocationAwareSettingActivity extends BeseyeBaseActivity 
								          implements OnSwitchBtnStateChangedListener{

	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
	}
	
	@Override
	protected void onSessionComplete(){
		Log.i(TAG, "onSessionComplete()");	
		super.onSessionComplete();
	}
	
	
	@Override
	protected void onResume() {
		Log.i(TAG, "LocationAwareSettingActivity::onResume()");
		super.onResume();
		if(!mbFirstResume){

		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_power_schdule;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		//monitorAsyncTask(new BeseyeCamBEHttpTask.SetScheduleStatusTask(this), true, mStrVCamID,(SwitchState.SWITCH_ON.equals(state)?Boolean.TRUE:Boolean.FALSE).toString());
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){

			default:{
				super.onClick(view);
				//Log.d(TAG, "CameraSettingActivity::onClick(), unhandled event by view:"+view);
			}
		}
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg) {
		super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
	

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
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
	
}
