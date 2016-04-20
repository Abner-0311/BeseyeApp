package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.NOTIFY_OBJ;
import static com.app.beseye.util.BeseyeJSONUtil.OBJ_TIMESTAMP;
import static com.app.beseye.util.BeseyeJSONUtil.STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONObject;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
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
import android.graphics.Region.Op;
import android.graphics.Region;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeMotionZoneUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.RemoteImageView;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;



public class NotificationSettingActivity extends BeseyeBaseActivity 
												implements OnSwitchBtnStateChangedListener{

	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;	
	private BeseyeSwitchBtn mNotifyMeMotionSwitchBtn, mNotifyMeHumanSwitchBtn, mNotifyMeAllSwitchBtn;
	//private boolean mbModified = false;
	private ViewGroup mVgMotionNotify, mVgHumanDetectNotify; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG)
			Log.i(TAG, "NotificationSettingActivity::onCreate()");
		
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
				txtTitle.setText(R.string.cam_setting_title_notification_setting);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "NotificationSettingActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}
	
		mNotifyMeAllSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_all_notify_switch);
		if(null != mNotifyMeAllSwitchBtn){
			mNotifyMeAllSwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
			mNotifyMeAllSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mNotifyMeMotionSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_motion_notify_switch);
		if(null != mNotifyMeMotionSwitchBtn){
			mNotifyMeMotionSwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
			mNotifyMeMotionSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mNotifyMeHumanSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_human_detect_notify_switch);
		if(null != mNotifyMeHumanSwitchBtn){
			mNotifyMeHumanSwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
			mNotifyMeHumanSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mVgMotionNotify = (ViewGroup)findViewById(R.id.vg_motion_notify);
		mVgHumanDetectNotify = (ViewGroup)findViewById(R.id.vg_human_detect_notify);	
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(BeseyeConfig.DEBUG)
			Log.d(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					super.onPostExecute(task, result, iRetCode);
					updateNotificationTypeState();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
				if(0 == iRetCode){
					if(DEBUG)
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
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
			showErrorDialog(R.string.cam_setting_fail_to_get_cam_info, true, iErrType);
		}else if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
			showErrorDialog(R.string.cam_setting_fail_to_update_notify_setting, true, iErrType);
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
	
	@Override
	protected void onSessionComplete(){
		super.onSessionComplete();
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}else{
			updateNotificationTypeState();
	    }
	}
	
//	private void setNotifySetting(){
//		if(checkDiff()){
//			JSONObject obj =  new JSONObject();
//			if(null != obj){
//				BeseyeJSONUtil.setJSONBoolean(obj, BeseyeJSONUtil.STATUS, (null != mNotifyMeSwitchBtn && mNotifyMeSwitchBtn.getSwitchState() == SwitchState.SWITCH_ON));
//				monitorAsyncTask(new BeseyeCamBEHttpTask.SetNotifySettingTask(this), true, mStrVCamID, obj.toString());
//			}
//		}else{
//			finish();
//		}
//	}
	
//	private boolean checkDiff(){
//		boolean bRet = false;
//		JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ);
//		if(null != notify_obj){
//			boolean bNotifyMe = BeseyeJSONUtil.getJSONBoolean(notify_obj, STATUS);
//			if(!mNotifyMeSwitchBtn.getSwitchState().equals(bNotifyMe?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF)){
//				bRet = true;
//			}
//		}	
//		return bRet;
//	}
	
	@Override
	protected void updateUICallback(){
		updateNotificationTypeState();
		
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "NotificationSettingActivity::onResume()");	
		super.onResume();
		
		if(!mbFirstResume){
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	@Override
	protected void onPause() {
		if(DEBUG)
			Log.d(TAG, "NotificationSettingActivity::onPause()");
		super.onPause();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_notification_setting;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		boolean bMotionNotifyMe = false, bHumanNotifyMe = false;
		
		JSONObject notify_obj_now =  BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ);
		if(null != notify_obj_now){
			JSONObject type_obj_now = BeseyeJSONUtil.getJSONObject(notify_obj_now, BeseyeJSONUtil.TYPE);
			if(null != type_obj_now){
				bMotionNotifyMe = BeseyeJSONUtil.getJSONBoolean(type_obj_now, BeseyeJSONUtil.NOTIFY_MOTION);
				bHumanNotifyMe = BeseyeJSONUtil.getJSONBoolean(type_obj_now, BeseyeJSONUtil.NOTIFY_HUMAN);
			}
		}
		
		JSONObject obj =  new JSONObject();
		if(null != obj){
			switch(view.getId()){
			case R.id.sb_all_notify_switch:{
				boolean bAllTurnOn = (null != mNotifyMeAllSwitchBtn && mNotifyMeAllSwitchBtn.getSwitchState() == SwitchState.SWITCH_ON);
				
				BeseyeJSONUtil.setJSONBoolean(obj, BeseyeJSONUtil.STATUS, bAllTurnOn);
				if(null != mVgMotionNotify){
					BeseyeUtils.setEnabled(mVgMotionNotify, bAllTurnOn);
				}
				if(null != mVgHumanDetectNotify){
					BeseyeUtils.setEnabled(mVgHumanDetectNotify, bAllTurnOn);
				}
				if(null != mNotifyMeMotionSwitchBtn){
					mNotifyMeMotionSwitchBtn.setEnabled(bAllTurnOn);
				}
				if(null != mNotifyMeHumanSwitchBtn){
					mNotifyMeHumanSwitchBtn.setEnabled(bAllTurnOn);
				}
				
				if(true == bAllTurnOn){						
					if(null != mNotifyMeMotionSwitchBtn){
						mNotifyMeMotionSwitchBtn.setSwitchState((bMotionNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
					}
					if(null != mNotifyMeHumanSwitchBtn){
						mNotifyMeHumanSwitchBtn.setSwitchState((bHumanNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
					}
				}else{
					if(null != mNotifyMeMotionSwitchBtn){
						mNotifyMeMotionSwitchBtn.setSwitchState((bAllTurnOn)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
					}
					if(null != mNotifyMeHumanSwitchBtn){
						mNotifyMeHumanSwitchBtn.setSwitchState((bAllTurnOn)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
					}
				}
				break;
			}
			case R.id.sb_motion_notify_switch:{
				bMotionNotifyMe = (null != mNotifyMeMotionSwitchBtn && mNotifyMeMotionSwitchBtn.getSwitchState() == SwitchState.SWITCH_ON);
				BeseyeJSONUtil.setJSONBoolean(obj, BeseyeJSONUtil.STATUS, true);
				break;
			}
			case R.id.sb_human_detect_notify_switch:{
				bHumanNotifyMe = (null != mNotifyMeHumanSwitchBtn && mNotifyMeHumanSwitchBtn.getSwitchState() == SwitchState.SWITCH_ON);
				BeseyeJSONUtil.setJSONBoolean(obj, BeseyeJSONUtil.STATUS, true);
				break;
			}
			}
			JSONObject type_obj =  new JSONObject();
			if(null != type_obj){
				BeseyeJSONUtil.setJSONBoolean(type_obj, BeseyeJSONUtil.NOTIFY_MOTION, bMotionNotifyMe);	
				BeseyeJSONUtil.setJSONBoolean(type_obj, BeseyeJSONUtil.NOTIFY_HUMAN, bHumanNotifyMe);	
				BeseyeJSONUtil.setJSONObject(obj, BeseyeJSONUtil.TYPE, type_obj);
			}
			
			monitorAsyncTask(new BeseyeCamBEHttpTask.SetNotifySettingTask(this), true, mStrVCamID, obj.toString());
		}
	}
	
	private void updateNotificationTypeState(){
		JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ);
		
		boolean bAllNotifyMe = false;
		if(null != notify_obj){
			bAllNotifyMe = BeseyeJSONUtil.getJSONBoolean(notify_obj, STATUS);
		}
		
		if(null != mVgMotionNotify){
			BeseyeUtils.setEnabled(mVgMotionNotify, bAllNotifyMe);
		}
		if(null != mVgHumanDetectNotify){
			BeseyeUtils.setEnabled(mVgHumanDetectNotify, bAllNotifyMe);
		}
		if(null != mNotifyMeAllSwitchBtn){
			mNotifyMeAllSwitchBtn.setSwitchState((bAllNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
		}
		if(null != mNotifyMeMotionSwitchBtn){
			mNotifyMeMotionSwitchBtn.setEnabled(bAllNotifyMe);
		}
		if(null != mNotifyMeHumanSwitchBtn){
			mNotifyMeHumanSwitchBtn.setEnabled(bAllNotifyMe);
		}
		
		if(true == bAllNotifyMe){		
			boolean bMotionNotifyMe = false, bHumanNotifyMe = false;
			
			JSONObject type_obj = BeseyeJSONUtil.getJSONObject(notify_obj, BeseyeJSONUtil.TYPE);	
			if(null != type_obj){
				bMotionNotifyMe = BeseyeJSONUtil.getJSONBoolean(type_obj, BeseyeJSONUtil.NOTIFY_MOTION);
				bHumanNotifyMe = BeseyeJSONUtil.getJSONBoolean(type_obj, BeseyeJSONUtil.NOTIFY_HUMAN);
			}
			
			if(null != mNotifyMeMotionSwitchBtn){
				mNotifyMeMotionSwitchBtn.setSwitchState((bMotionNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
			if(null != mNotifyMeHumanSwitchBtn){
				mNotifyMeHumanSwitchBtn.setSwitchState((bHumanNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
		}else{
			if(null != mNotifyMeMotionSwitchBtn){
				mNotifyMeMotionSwitchBtn.setSwitchState((bAllNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
			if(null != mNotifyMeHumanSwitchBtn){
				mNotifyMeHumanSwitchBtn.setSwitchState((bAllNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
		}
		
	}
}
