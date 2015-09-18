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



public class HumanDetectNotificationSettingActivity extends BeseyeBaseActivity 
												implements OnSwitchBtnStateChangedListener{

	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;	
	private ViewGroup mVgHumanDetectTraining, mVgHumanDetectReset;

	private BeseyeSwitchBtn mNotifyMeSwitchBtn;
	private boolean mbModified = false;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG)
			Log.i(TAG, "MotionNotificationSettingActivity::onCreate()");
		
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
				txtTitle.setText(R.string.cam_setting_title_human_detect);
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
			Log.e(TAG, "MotionNotificationSettingActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}
		
		mNotifyMeSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_human_detect_notify_switch);
		if(null != mNotifyMeSwitchBtn){
			mNotifyMeSwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
			mNotifyMeSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mVgHumanDetectTraining = (ViewGroup)findViewById(R.id.vg_human_detect_zone_text);
		if(null != mVgHumanDetectTraining){
			mVgHumanDetectTraining.setOnClickListener(this);
		}
		
		mVgHumanDetectReset = (ViewGroup)findViewById(R.id.vg_human_detect_reset);
		if(null != mVgHumanDetectReset){
			mVgHumanDetectReset.setOnClickListener(this);
		}
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(BeseyeConfig.DEBUG)
			Log.d(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
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
		// GetLatestThumbnailTask don't need to have onErrorReport because it has default image
		if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
			showErrorDialog(R.string.cam_setting_fail_to_update_notify_setting, true);
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
	
	@Override
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(-1), true, mStrVCamID);
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}else{
			updateNotificationTypeState();
		}
	}
	
	@Override
	public void onClick(View view){
		switch(view.getId()){
			case R.id.vg_human_detect_zone_text:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityByClassName(HumanDetectTrainActivity.class.getName(),b);
				break;
			} 
			default:
				super.onClick(view);	
		}
	}

	@Override
	protected void updateUICallback(){
		updateNotificationTypeState();
	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "MotionNotificationSettingActivity::onResume()");	
		super.onResume();
		
		if(!mbFirstResume){
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_human_detect_notification;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		mbModified = true;
		JSONObject obj =  new JSONObject();
		if(null != obj){
			boolean bTurnOn = (null != mNotifyMeSwitchBtn && mNotifyMeSwitchBtn.getSwitchState() == SwitchState.SWITCH_ON);
			
			if(bTurnOn){
				BeseyeJSONUtil.setJSONBoolean(obj, BeseyeJSONUtil.STATUS, bTurnOn);
			}
			
			JSONObject type_obj =  new JSONObject();
			if(null != type_obj){
				BeseyeJSONUtil.setJSONBoolean(type_obj, BeseyeJSONUtil.NOTIFY_PEOPLE, bTurnOn);	
				BeseyeJSONUtil.setJSONObject(obj, BeseyeJSONUtil.TYPE, type_obj);
			}
			
			monitorAsyncTask(new BeseyeCamBEHttpTask.SetNotifySettingTask(this), true, mStrVCamID, obj.toString());
		}
	}

	private void updateNotificationTypeState(){
		JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ);
		if(false == mbModified){
			boolean bNotifyMe = false;
			if(null != notify_obj){
				bNotifyMe = BeseyeJSONUtil.getJSONBoolean(notify_obj, STATUS);
			}
			
			if(bNotifyMe){
				JSONObject type_obj = BeseyeJSONUtil.getJSONObject(notify_obj, BeseyeJSONUtil.TYPE);
				bNotifyMe = BeseyeJSONUtil.getJSONBoolean(type_obj, BeseyeJSONUtil.NOTIFY_PEOPLE);
			}
			
			if(null != mNotifyMeSwitchBtn){
				mNotifyMeSwitchBtn.setSwitchState((bNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
		}else{
			boolean bNotifyMe = false;
			if(null != mNotifyMeSwitchBtn){
				bNotifyMe = mNotifyMeSwitchBtn.getSwitchState().equals(SwitchState.SWITCH_ON);
			}
		}
	}
}
