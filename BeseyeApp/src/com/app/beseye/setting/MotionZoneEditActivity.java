package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.OBJ_TIMESTAMP;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONObject;
import static com.app.beseye.util.BeseyeJSONUtil.MOTION_ZONE;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeMotionZoneUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.MotionZoneEditView;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;
import com.app.beseye.widget.BaseTwoBtnDialog;
import com.app.beseye.widget.BaseTwoBtnDialog.OnTwoBtnClickListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteImageView;

public class MotionZoneEditActivity extends BeseyeBaseActivity 
									implements OnClickListener{
	
	private Button mBtnOk, mBtnFull, mBtnCancel;
	private MotionZoneEditView mMotionZoneEditView;
	private double[] mdRatios = {-1.0, -1.0, -1.0, -1.0};
	private RemoteImageView mImgThumbnail;
	private ProgressBar mPbLoadingCursor;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WindowManager.LayoutParams attributes = getWindow().getAttributes();
		attributes.flags =  WindowManager.LayoutParams.FLAG_FULLSCREEN;
		getWindow().setAttributes(attributes);

		getSupportActionBar().hide();
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "MotionZoneEditActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}
		
		mImgThumbnail = (RemoteImageView)findViewById(R.id.iv_motion_zone_thumbnail);
		
		mBtnCancel = (Button)findViewById(R.id.btn_cancel);
		if(null != mBtnCancel){
			mBtnCancel.setOnClickListener(this);
		}
		mBtnFull = (Button)findViewById(R.id.btn_full);
		if(null != mBtnFull){
			mBtnFull.setOnClickListener(this);
			BeseyeUtils.setEnabled(mBtnFull, false);
		}
		mBtnOk = (Button)findViewById(R.id.btn_ok);
		if(null != mBtnOk){
			mBtnOk.setOnClickListener(this);
			BeseyeUtils.setEnabled(mBtnOk, false);
		}
		
		mPbLoadingCursor = (ProgressBar)findViewById(R.id.pb_loadingCursor);
		if(null != mPbLoadingCursor) {
			BeseyeUtils.setVisibility(mPbLoadingCursor, View.VISIBLE);
		}
		mMotionZoneEditView = (MotionZoneEditView)findViewById(R.id.iv_motion_zone_edit);		
		if(null != mMotionZoneEditView){
			BeseyeUtils.setVisibility(mMotionZoneEditView, View.GONE);
		}
		
		mdRatios = getIntent().getDoubleArrayExtra(BeseyeMotionZoneUtil.MOTION_ZONE_RATIO);
	}

	@Override
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(-1), true, mStrVCamID);		
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_motion_zone_edit;
	}
	
	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(BeseyeConfig.DEBUG)
			Log.d(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeMMBEHttpTask.GetLatestThumbnailTask){
				if(0 == iRetCode){							
					try {
						mCam_obj.put(BeseyeJSONUtil.ACC_VCAM_THUMB, BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(result.get(0), BeseyeJSONUtil.MM_THUMBNAIL), "url"));
						setThumbnail();
					} catch (JSONException e) {
						e.printStackTrace();		
					}
				} else {
					showThumbnailError();
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetMotionZoneTask){
				if(0 == iRetCode){	
					JSONArray motion_zone_array =  BeseyeJSONUtil.getJSONArray(result.get(0), MOTION_ZONE);
					BeseyeJSONUtil.setJSONArray(getJSONObject(mCam_obj, ACC_DATA), MOTION_ZONE, motion_zone_array);
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
				
					Bundle b = new Bundle();
					b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
					Intent resultIntent = new Intent();
					resultIntent.putExtras(b);					
					setResult(RESULT_OK, resultIntent);
					finish();
				} else {
					Log.e(TAG, "MotionZoneEditActivity SetMotionZoneTask Error");
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,
			String strMsg) {
		// GetLatestThumbnailTask don't need to have onErrorReport because it has its callback
		if(task instanceof BeseyeCamBEHttpTask.SetMotionZoneTask){
			showSetMZToServerError();
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
	
	private void setThumbnail(){
		if(null != mImgThumbnail){
			int miThumbnailWidth = BeseyeUtils.getDeviceWidth(this);
			BeseyeUtils.setThumbnailRatio(mImgThumbnail, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
				
			mImgThumbnail.setURI(BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_THUMB), 
								R.drawable.cameralist_s_view_noview_bg, 
								BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID),
								new RemoteImageView.RemoteImageCallback(){
									@Override
									public void imageLoaded(boolean success) {
										if(true == success){
											drawMotionZoneRect();
											BeseyeUtils.setVisibility(mPbLoadingCursor, View.GONE);
											BeseyeUtils.setVisibility(mMotionZoneEditView, View.VISIBLE);
											BeseyeUtils.setEnabled(mBtnFull, true);
											BeseyeUtils.setEnabled(mBtnOk, true);			
										} else{
											showThumbnailError();
										}
									}});
								
			mImgThumbnail.loadImage();
		}
	}
	
	private void drawMotionZoneRect(){
		if(-1 == mdRatios[0]){
			mdRatios = BeseyeMotionZoneUtil.getMotionZoneFromServer(mCam_obj, BeseyeMotionZoneUtil.ssStrObjKey);
		}	
		
		if(!BeseyeMotionZoneUtil.isMotionZoneRangeValiate(mdRatios, BeseyeMotionZoneUtil.siRatioMinV, 
				BeseyeMotionZoneUtil.siRatioMaxV, BeseyeMotionZoneUtil.sdMinZoneRatio, BeseyeMotionZoneUtil.sdConfidenceV)){
			BeseyeMotionZoneUtil.setDefaultRatio(mdRatios);
		}
		
		mMotionZoneEditView.init(mImgThumbnail.getWidth(), mImgThumbnail.getHeight(), mdRatios);	
	}
	
	private int setRatio(double[] newRatios){
		JSONObject obj =  new JSONObject();
		
		JSONObject motion_zone_obj =  new JSONObject();
		if(null != motion_zone_obj){
			for(int idx = 0; idx < BeseyeMotionZoneUtil.ssStrObjKey.length; idx++){
				BeseyeJSONUtil.setJSONFloat(motion_zone_obj, BeseyeMotionZoneUtil.ssStrObjKey[idx], (float)newRatios[idx]);
			}
		}
		
		JSONArray motion_zone_array = new JSONArray();
		motion_zone_array.put(motion_zone_obj);
		BeseyeJSONUtil.setJSONArray(obj, MOTION_ZONE, motion_zone_array);
		
		monitorAsyncTask(new BeseyeCamBEHttpTask.SetMotionZoneTask(this), true, mStrVCamID, obj.toString());
		return 0;
	}
	
	@Override
	public void onClick(View view){
		switch(view.getId()){
			case R.id.btn_cancel:{
				showCancelDialog();
				break;
			}
			case R.id.btn_full:{
				mMotionZoneEditView.fullscreen();
				break;
			}
			case R.id.btn_ok:{
				double[] newRatios = mMotionZoneEditView.getNewRatio();
				setRatio(newRatios);
				break;
			}
			default:
				super.onClick(view);	
		}
	}
	@Override
	public void onBackPressed() {
		showCancelDialog();
	}
	
	private void showCancelDialog(){
		if(!mMotionZoneEditView.isChange()){
			finish();
		} else {
			BaseTwoBtnDialog d = new BaseTwoBtnDialog(this); 

			d.setBodyText(R.string.motion_zone_cancel_body);
			d.setTitleText(R.string.motion_zone_cancel_title);
			d.setPositiveBtnText(R.string.sure);
			
    		d.setOnTwoBtnClickListener(new OnTwoBtnClickListener(){
    			@Override
    			public void onBtnYesClick() {
    				finish();
    			}

    			@Override
    			public void onBtnNoClick() {
    			}} );
    		d.show();
		}
	}
	
	private void showThumbnailError(){ 
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				BaseOneBtnDialog d = new BaseOneBtnDialog(MotionZoneEditActivity.this);
				d.setBodyText(R.string.thumbnail_error_body);
				d.setTitleText(R.string.thumbnail_error_title);
				d.setCancelable(false);
				
				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){	
					@Override
					public void onBtnClick() {
						finish();			
					}});
				d.show();
			}}, 0);
	}
	
	private void showSetMZToServerError(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				BaseOneBtnDialog d = new BaseOneBtnDialog(MotionZoneEditActivity.this);
				d.setBodyText(R.string.thumbnail_error_body);
				d.setTitleText(R.string.thumbnail_error_title);
				d.setCancelable(false);
				
				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
					@Override
					public void onBtnClick() {	
						// do nothing, just stay in edit page
					}});
				d.show();
			}}, 0);
	}
}
