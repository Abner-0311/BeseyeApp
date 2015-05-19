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
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
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
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.MotionZoneEditView;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;
import com.app.beseye.widget.BaseTwoBtnDialog;
import com.app.beseye.widget.BaseTwoBtnDialog.OnTwoBtnClickListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteImageView;

public class MotionZoneEditActivity extends BeseyeBaseActivity 
									implements OnClickListener, OnGlobalLayoutListener{
	
	private Button mBtnOk, mBtnFull, mBtnCancel;
	private MotionZoneEditView mMotionZoneEditView;
	private double[] mRatios = {-1.0, -1.0, -1.0, -1.0};
	private RemoteImageView mImgThumbnail;
	private String mThumbnailPath;
	private ProgressBar mPbLoadingCursor;
	
	private final double minZoneRatio = 0.2;
    private final double confidenceV = 0.95;		//for different device may have different double 

    private String[] mStrObjKey = {BeseyeJSONUtil.MOTION_ZONE_LEFT, 
			   BeseyeJSONUtil.MOTION_ZONE_TOP,
			   BeseyeJSONUtil.MOTION_ZONE_RIGHT,
			   BeseyeJSONUtil.MOTION_ZONE_BOTTOM
			   };
	
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
			ViewTreeObserver vto = mMotionZoneEditView.getViewTreeObserver();
			vto.addOnGlobalLayoutListener(this);

			BeseyeUtils.setVisibility(mMotionZoneEditView, View.GONE);
		}
		
		mRatios = getIntent().getDoubleArrayExtra("MotionZoneRatio");
		if(-1 == mRatios[0]){
			mRatios = getRatioFromServer();
		}
		monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(-1), true, mStrVCamID);	
	}

	//for get size of view
	@SuppressWarnings("deprecation")
	@Override
	public void onGlobalLayout() {
		if(null != mMotionZoneEditView) {
			if (mMotionZoneEditView.getWidth() > 0 && mMotionZoneEditView.getHeight() > 0) {
				ViewTreeObserver obs = mMotionZoneEditView.getViewTreeObserver();
				
				if (Build.VERSION.SDK_INT < 16) {
			        obs.removeGlobalOnLayoutListener(this);
			    }else {
			        obs.removeOnGlobalLayoutListener(this);
			    }
				
				if(-1 == mRatios[0]) {
					mRatios = getRatioFromServer();
				}
				
//				for(int i=0; i<mRatios.length; i++){
//					if(0 > mRatios[i]) {
//						mRatios[i] = 0;
//					} 
//					if(1 < mRatios[i]){
//						mRatios[i] = 1;
//					}
//				}
				
				boolean isRatioVailate = true;
				//check 0~1
				for(int i=0; i<mRatios.length; i++){
					if(0 > mRatios[i] || 1 < mRatios[i]){
						isRatioVailate = false;
						break;
					}
				}
				
				// check minL 
				if(isRatioVailate){
					if( (mRatios[3]-mRatios[1]) < minZoneRatio*confidenceV || (mRatios[2]-mRatios[0]) < minZoneRatio/16.0*9*confidenceV){
						isRatioVailate = false;
					}
				}
				
				if(!isRatioVailate){
					mRatios[0] = 0;
					mRatios[1] = 0;
					mRatios[2] = 1;
					mRatios[3] = 1;
				}
				
				mMotionZoneEditView.Init(mMotionZoneEditView.getWidth(), mMotionZoneEditView.getHeight(), mRatios);	
			}
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
	
					finish();
				} else {
					Log.e(TAG, "MotionZoneEditActivity SetMotionZoneTask Error");
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	private void setThumbnail(){
		if(null != mImgThumbnail){
			int miThumbnailWidth = BeseyeUtils.getDeviceWidth(this);
			BeseyeUtils.setThumbnailRatio(mImgThumbnail, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
			
			mThumbnailPath = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_THUMB);
			
			mImgThumbnail.setURI(mThumbnailPath, 
								R.drawable.cameralist_s_view_noview_bg, 
								BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID),
								new RemoteImageView.RemoteImageCallback(){
									@Override
									public void imageLoaded(boolean success) {
										if(true == success){
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
	
	private double[] getRatioFromServer(){
		double[] r = {-1.0, -1.0, -1.0, -1.0};
		JSONArray motion_zone_array =  BeseyeJSONUtil.getJSONArray(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), MOTION_ZONE);

		JSONObject motion_zone_obj = null;
		try {
			motion_zone_obj = (JSONObject) motion_zone_array.get(0);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if(null != motion_zone_obj){
			for(int idx = 0; idx < mStrObjKey.length; idx++){
				r[idx] = BeseyeJSONUtil.getJSONDouble(motion_zone_obj, mStrObjKey[idx]);
			}
		}
		
		return r;
	}
	
	private int setRatio(double[] newRatios){
		JSONObject obj =  new JSONObject();
		
		JSONObject motion_zone_obj =  new JSONObject();
		if(null != motion_zone_obj){
			for(int idx = 0; idx < mStrObjKey.length; idx++){
				BeseyeJSONUtil.setJSONFloat(motion_zone_obj, mStrObjKey[idx], (float)newRatios[idx]);
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
				
				Bundle b = new Bundle();
				b.putDoubleArray("MotionZoneRatio", newRatios);
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
			    Intent resultIntent = new Intent();
				resultIntent.putExtras(b);
				setResult(RESULT_OK, resultIntent);	
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
		BaseOneBtnDialog d = new BaseOneBtnDialog(this);
		d.setBodyText(R.string.thumbnail_error_body);
		d.setTitleText(R.string.thumbnail_error_title);
		d.setCancelable(false);
		
		d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
		
			@Override
			public void onBtnClick() {
				finish();			
			}});
		d.show();
	}
}
