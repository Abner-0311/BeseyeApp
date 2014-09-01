package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.*;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_TS;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;

import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class NotificationEventsSettingActivity extends BeseyeBaseActivity
											   implements OnSwitchBtnStateChangedListener{
	static private enum NOTIFY_TYPE{
		NOTIFY_PEOPLE,
		NOTIFY_MOTION,
		NOTIFY_FIRE,
		NOTIFY_SOUND,
		NOTIFY_OFFLINE,
		NOTIFY_TYPE_COUNT;
	};
	
	static private final int s_NotifyTypeNum = NOTIFY_TYPE.NOTIFY_TYPE_COUNT.ordinal();
	
	private BeseyeSwitchBtn mNotifyMeSwitchBtn;
	private ViewGroup mVgNotifyType[];
	private ImageView mIvNotifyTypeCheck[], mIvNotifyTypeCheckBg[];
	private TextView mTxtSchedDays[];
	private JSONObject mSched_obj;
	
	private String[] mStrObjKey = {BeseyeJSONUtil.NOTIFY_PEOPLE, 
								   BeseyeJSONUtil.NOTIFY_MOTION,
								   BeseyeJSONUtil.NOTIFY_FIRE,
								   BeseyeJSONUtil.NOTIFY_SOUND,
								   BeseyeJSONUtil.NOTIFY_OFFLINE};
	
	private View mVwNavBar, mVNotifyMe;
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
				txtTitle.setText(R.string.cam_setting_title_notification_event);
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
			
//			mSched_obj = new JSONObject(getIntent().getStringExtra(PowerScheduleEditActivity.KEY_SCHED_OBJ)); 
		} catch (JSONException e1) {
			Log.e(TAG, "PowerScheduleDayPickerActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mVNotifyMe = findViewById(R.id.vg_notify_me);
		
		mNotifyMeSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_notify_me_switch);
		if(null != mNotifyMeSwitchBtn){
			mNotifyMeSwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
			mNotifyMeSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mVgNotifyType = new ViewGroup[s_NotifyTypeNum];
		mIvNotifyTypeCheck = new ImageView[s_NotifyTypeNum];
		mIvNotifyTypeCheckBg = new ImageView[s_NotifyTypeNum];
		mTxtSchedDays = new TextView[s_NotifyTypeNum];
		
		int iVgIds[] = {R.id.vg_people_detect, R.id.vg_motion_detect, R.id.vg_fire_detect, R.id.vg_sound_detect, R.id.vg_offline_detect};
		int iStrIds[] = {R.string.cam_setting_title_notification_event_people_detect,
						 R.string.cam_setting_title_notification_event_motion_detect, 
						 R.string.cam_setting_title_notification_event_fire_detect, 
						 R.string.cam_setting_title_notification_event_sound_detect,  
						 R.string.cam_setting_title_notification_event_offline_detect};
		
		
		for(int idx = 0; idx < s_NotifyTypeNum;idx++){
			mVgNotifyType[idx] = (ViewGroup)findViewById(iVgIds[idx]);
			if(null != mVgNotifyType[idx]){
				mIvNotifyTypeCheck[idx] = (ImageView)mVgNotifyType[idx].findViewById(R.id.iv_day_check);
				//BeseyeUtils.setVisibility(mIvNotifyTypeCheck[idx], View.VISIBLE);
				
				mIvNotifyTypeCheckBg[idx] = (ImageView)mVgNotifyType[idx].findViewById(R.id.iv_day_check_bg);
				if(null != mIvNotifyTypeCheckBg[idx]){
					mIvNotifyTypeCheckBg[idx].setTag(idx);
					//mIvNotifyTypeCheckBg[idx].setOnClickListener(this);
				}
				
				mTxtSchedDays[idx] = (TextView)mVgNotifyType[idx].findViewById(R.id.txt_day_title);
				if(null != mTxtSchedDays[idx]){
					mTxtSchedDays[idx].setText(iStrIds[idx]);
				}
				mVgNotifyType[idx].setOnClickListener(this);
			}
		}
		//Disable fire and sound detection
		mVgNotifyType[2].setVisibility(View.GONE);
		mVgNotifyType[3].setVisibility(View.GONE);
		
//		JSONArray arrDays = BeseyeJSONUtil.getJSONArray(mSched_obj, SCHED_DAYS);
//		int iSize = (null != arrDays)?arrDays.length():0;
//		for(int idx = 0;idx < iSize;idx++){
//			try {
//				BeseyeUtils.setVisibility(mIvNotifyTypeCheck[arrDays.getInt(idx)], View.VISIBLE);
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA)){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
		}else{
			updateNotificationTypeState();
		}
	}
	
	private void updateNotificationTypeState(){
		boolean bIsCamDisconnected = BeseyeJSONUtil.isCamPowerDisconnected(mCam_obj);
		JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ);
		if(null != mNotifyMeSwitchBtn){
			boolean bMotifyMe = false;
			if(null != notify_obj){
				bMotifyMe = BeseyeJSONUtil.getJSONBoolean(notify_obj, STATUS);
			}
			
			mNotifyMeSwitchBtn.setEnabled(!bIsCamDisconnected);
			mNotifyMeSwitchBtn.setSwitchState(((!bIsCamDisconnected && bMotifyMe)?SwitchState.SWITCH_ON:(bIsCamDisconnected?SwitchState.SWITCH_DISABLED:SwitchState.SWITCH_OFF)));
			BeseyeUtils.setEnabled(mVNotifyMe, !bIsCamDisconnected);
		}
		
		JSONObject type_obj =  BeseyeJSONUtil.getJSONObject(notify_obj, TYPE);
		for(int idx = 0; idx < s_NotifyTypeNum;idx++){
			BeseyeUtils.setEnabled(mVgNotifyType[idx], !bIsCamDisconnected);
			BeseyeUtils.setVisibility(mIvNotifyTypeCheck[idx], BeseyeJSONUtil.getJSONBoolean(type_obj, mStrObjKey[idx])?View.VISIBLE:View.INVISIBLE);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_notification_events;
	}
	
	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		setNotifySetting();
	}

	@Override
	public void onClick(View view) {
		int idx = findIdxByView(view);
		if(0 <= idx){
			if(null != mIvNotifyTypeCheck[idx]){
				if(View.VISIBLE != mIvNotifyTypeCheck[idx].getVisibility()){
					mIvNotifyTypeCheck[idx].setVisibility(View.VISIBLE);
				}else{
					mIvNotifyTypeCheck[idx].setVisibility(View.INVISIBLE);
				}	
				
				setNotifySetting();
			}
		}else if(R.id.iv_nav_left_btn == view.getId()){
			finish();
		}else{
			super.onClick(view);
		}
	}
	
	private int findIdxByView(View view){
		int iRet = -1;
		for(int idx = 0; idx < s_NotifyTypeNum;idx++){
			if(view == mVgNotifyType[idx]){
				iRet = idx;
				break;
			}
		}
		
		return iRet;
	}
	
//	@Override
//	public boolean onKeyUp(int keyCode, KeyEvent event) {
//		if(keyCode == KeyEvent.KEYCODE_BACK){
//			setPickResult();
//		}
//		
//		return super.onKeyUp(keyCode, event);
//	}
	
	private void setNotifySetting(){
		JSONObject obj =  new JSONObject();
		if(null != obj){
			BeseyeJSONUtil.setJSONBoolean(obj, BeseyeJSONUtil.STATUS, (null != mNotifyMeSwitchBtn && mNotifyMeSwitchBtn.getSwitchState() == SwitchState.SWITCH_ON));
			JSONObject type_obj =  new JSONObject();
			if(null != type_obj){
				for(int idx = 0; idx < s_NotifyTypeNum;idx++){
					BeseyeJSONUtil.setJSONBoolean(type_obj, mStrObjKey[idx], (null != mIvNotifyTypeCheck[idx] && mIvNotifyTypeCheck[idx].getVisibility() == View.VISIBLE));	
				}
				BeseyeJSONUtil.setJSONObject(obj, BeseyeJSONUtil.TYPE, type_obj);
			}
			
			monitorAsyncTask(new BeseyeCamBEHttpTask.SetNotifySettingTask(this), true, mStrVCamID, obj.toString());
		}
	}
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					//Special handling
					super.onPostExecute(task, result, iRetCode);
					updateNotificationTypeState();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(result.get(0), NOTIFY_OBJ);
					BeseyeJSONUtil.setJSONObject(getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ, notify_obj);
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
				}
				updateNotificationTypeState();
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			showErrorDialog(R.string.cam_setting_fail_to_get_cam_info, true);
		}else if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
			showErrorDialog(R.string.cam_setting_fail_to_update_notify_setting, true);
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
}
