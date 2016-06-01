package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.*;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

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

public class NightVisionActivity extends BeseyeBaseActivity {
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private ImageView[] mivKeyIdx;
	private ViewGroup[] mVgKeyIdx;
	private int miIRCutStatus = 0;
	
	static private final int NUM_NIGHT_VISION_MODE = 3;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
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
				txtTitle.setText(R.string.cam_setting_hw_night_vision_title);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
			Toolbar parent =(Toolbar) mVwNavBar.getParent();
			parent.setContentInsetsAbsolute(0,0);
			parent.setPadding(0,0,0,0);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "HWSettingsActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}

		mivKeyIdx = new ImageView[NUM_NIGHT_VISION_MODE]; 
		if(null != mivKeyIdx){
			mivKeyIdx[0] = (ImageView)findViewById(R.id.iv_key_auto_check);
			mivKeyIdx[1] = (ImageView)findViewById(R.id.iv_key_on_check);
			mivKeyIdx[2] = (ImageView)findViewById(R.id.iv_key_off_check);
		}
		
		mVgKeyIdx = new ViewGroup[NUM_NIGHT_VISION_MODE];  
		if(null != mVgKeyIdx){
			mVgKeyIdx[0] = (ViewGroup)findViewById(R.id.vg_nv_holder_auto);
			mVgKeyIdx[1] = (ViewGroup)findViewById(R.id.vg_nv_holder_on);
			mVgKeyIdx[2] = (ViewGroup)findViewById(R.id.vg_nv_holder_off);
		}
		
		updateUIByCamObj();
	}
	
	private void updateUIByCamObj(){
		if(null != mCam_obj){
			JSONObject dataObj = BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA);
			if(null != dataObj){
				boolean bIsCamDisconnected = BeseyeJSONUtil.isCamPowerDisconnected(mCam_obj);
				
				miIRCutStatus = getJSONInt(dataObj, IRCUT_STATUS, 0);
				for(int idx = 0; idx < NUM_NIGHT_VISION_MODE;idx++){
					mivKeyIdx[idx].setVisibility((idx == miIRCutStatus)?View.VISIBLE:View.INVISIBLE);
					mVgKeyIdx[idx].setEnabled(!bIsCamDisconnected);
					mVgKeyIdx[idx].setOnClickListener(this);
					mVgKeyIdx[idx].setTag("NV");
				}
			}
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_night_vision;
	}
	
	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof String && "NV".equals(view.getTag())){
			for(int idx = 0; idx < NUM_NIGHT_VISION_MODE;idx++){
				if(mVgKeyIdx[idx] == view){
					BeseyeUtils.setVisibility(mivKeyIdx[idx], View.VISIBLE);
					monitorAsyncTask(new BeseyeCamBEHttpTask.SetIRCutStatusTask(this), true, mStrVCamID, ""+idx);
				}else{
					BeseyeUtils.setVisibility(mivKeyIdx[idx], View.INVISIBLE);
				}
			}
		}else{
			super.onClick(view);
		}
	}
	
	@Override
	protected void updateUICallback(){
		updateUIByCamObj();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(!mbFirstResume){
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.SetIRCutStatusTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					miIRCutStatus = BeseyeJSONUtil.getJSONInt(result.get(0), IRCUT_STATUS);
					BeseyeJSONUtil.setJSONInt(getJSONObject(mCam_obj, ACC_DATA), IRCUT_STATUS, miIRCutStatus);
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					setActivityResultWithCamObj();
					finish();
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, final int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.SetIRCutStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					showErrorDialog(R.string.cam_setting_fail_to_update_night_vision, true, iErrType);
				}}, 0);
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
}