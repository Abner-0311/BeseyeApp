package com.app.beseye.setting;



import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.Arrays;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.Button;


import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseDialog;
import com.app.beseye.widget.RemoteImageView;

import com.app.beseye.widget.BaseDialog;
import com.app.beseye.widget.BaseDialog.OnDialogClickListener;

public class MotionZoneEditActivity extends BeseyeBaseActivity implements OnClickListener, OnGlobalLayoutListener{
	private Button mBtnOk, mBtnFull, mBtnCancel;
	private MotionZoneEditView mMotionZoneEditView;
	private double[] ratios = new double[4];
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WindowManager.LayoutParams attributes = getWindow().getAttributes();
		attributes.flags =  WindowManager.LayoutParams.FLAG_FULLSCREEN;
		getWindow().setAttributes(attributes);

		getSupportActionBar().hide();
		
		ratios = getIntent().getDoubleArrayExtra("MotionZoneRatio");
		if(null == ratios){
			ratios = getRatioFromServer();
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "MotionZoneEditActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}
		
		//TODO: handle when BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_VCAM_THUMB) is null; AsyncTask
		final RemoteImageView mImgThumbnail;
		int miThumbnailWidth = BeseyeUtils.getDeviceWidth(this);
		JSONObject obj = mCam_obj;
		
		mImgThumbnail = (RemoteImageView)findViewById(R.id.iv_motion_zone_thumbnail);
		if(null != mImgThumbnail){
			BeseyeUtils.setThumbnailRatio(mImgThumbnail, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
			mImgThumbnail.setURI(BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_VCAM_THUMB), R.drawable.cameralist_s_view_noview_bg, BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_ID));
			mImgThumbnail.loadImage();
		}
		//TODO: if some error happen on thumbnail, show dialog
		
		mBtnCancel = (Button)findViewById(R.id.btn_cancel);
		if(null != mBtnCancel){
			mBtnCancel.setOnClickListener(this);
		}
		mBtnFull = (Button)findViewById(R.id.btn_full);
		if(null != mBtnFull){
			mBtnFull.setOnClickListener(this);
		}
		mBtnOk = (Button)findViewById(R.id.btn_ok);
		if(null != mBtnOk){
			mBtnOk.setOnClickListener(this);
		}
		mMotionZoneEditView = (MotionZoneEditView)findViewById(R.id.iv_motion_zone_edit);
		
		ViewTreeObserver vto = mMotionZoneEditView.getViewTreeObserver();
		vto.addOnGlobalLayoutListener(this);
		
		
//		int ThumbnailHeight = mImgThumbnail.getLayoutParams().height;
//		int ThumbnailWidth = mImgThumbnail.getLayoutParams().width;
//		
//		int EditViewHeight = mMotionZoneEditView.getLayoutParams().height;
//		int EditViewWidth = mMotionZoneEditView.getLayoutParams().width;
//		
//		Log.v(TAG, "Kelly Edit Size" + EditViewHeight + " " + EditViewWidth);
//		Log.v(TAG, "Kelly View Size" + ThumbnailHeight + " " + ThumbnailWidth);
//	
//		mMotionZoneEditView.onDraw(null);
//		mMotionZoneEditView.setImageBoundary(ThumbnailWidth);
	}

	
	
	//for get size of view
	@SuppressWarnings("deprecation")
	@Override
	public void onGlobalLayout() {
		if (mMotionZoneEditView.getWidth() != 0 && mMotionZoneEditView.getHeight() != 0) {
			ViewTreeObserver obs = mMotionZoneEditView.getViewTreeObserver();
			
			if (Build.VERSION.SDK_INT < 16) {
		        obs.removeGlobalOnLayoutListener(this);
		    }else {
		        obs.removeOnGlobalLayoutListener(this);
		    }
			//obs.removeGlobalOnLayoutListener(this);
			//Log.v(TAG, "Kelly "+mMotionZoneEditView.getWidth()+" "+mMotionZoneEditView.getHeight());
			if(null == ratios){
				ratios = getRatioFromServer();
			}
			mMotionZoneEditView.Init(mMotionZoneEditView.getWidth(), mMotionZoneEditView.getHeight(), ratios);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_motion_zone_edit;
	}
	
	private double[] getRatioFromServer(){
		double[] r = new double[4];
		// TODO: call API
		r[0] = 0.0;
		r[1] = 0.0;
		r[2] = 1.0;
		r[3] = 1.0;
		return r;
	}
	
	private int setRatio(double[] newRatios){
		// TODO: call API
		return 0;
	}
	
	@Override
	public void onClick(View view){
		switch(view.getId()){
			case R.id.btn_cancel:{
				double[] newRatios = mMotionZoneEditView.getNewRatio();
				if(Arrays.equals(newRatios,ratios)){
					finish();
				} else {
					BaseDialog d = new BaseDialog(this); 
					
		    		d.setOnDialogClickListener(new OnDialogClickListener(){
		    			@Override
		    			public void onBtnYesClick() {
		    				finish();
		    			}

		    			@Override
		    			public void onBtnNoClick() {
		    			}} );
		    		d.show();
				}
				break;
			}
			case R.id.btn_full:{
				mMotionZoneEditView.fullscreen();
				break;
			}
			case R.id.btn_ok:{
				double[] newRatios = mMotionZoneEditView.getNewRatio();
				Log.v(TAG, "Kelly ratios " + newRatios[0] + " " + newRatios[1] + " " + newRatios[2] + " " + newRatios[3] );

				//TODO: call API and then return view and ratios
				setRatio(newRatios);
				
				Bundle b = new Bundle();
				b.putDoubleArray("MotionZoneRatio", newRatios);
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());

			    Intent resultIntent = new Intent();
				resultIntent.putExtras(b);
				setResult(RESULT_OK, resultIntent);	
				finish();
				break;
			}
			default:
				super.onClick(view);	
		}
	}
	@Override
	public void onBackPressed() {
		double[] newRatios = mMotionZoneEditView.getNewRatio();
		if(Arrays.equals(newRatios,ratios)){
			finish();
		} else {
			BaseDialog d = new BaseDialog(this); 
			
    		d.setOnDialogClickListener(new OnDialogClickListener(){
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
}
