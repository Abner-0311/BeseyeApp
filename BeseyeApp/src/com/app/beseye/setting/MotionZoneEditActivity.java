package com.app.beseye.setting;



import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.RemoteImageView;

public class MotionZoneEditActivity extends BeseyeBaseActivity implements OnClickListener, OnGlobalLayoutListener{

//	private MotionZoneEditView freeDraw;
	private Button mBtnOk, mBtnFull, mBtnCancel;
	private MotionZoneEditView mMotionZoneEditView;
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		WindowManager.LayoutParams attributes = getWindow().getAttributes();
		//attributes.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
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
		
		
//		WindowManager wm = getWindowManager();
//		Display display = wm.getDefaultDisplay();
//		@SuppressWarnings("deprecation")
//		int screenWidth = display.getWidth();
//		@SuppressWarnings("deprecation")
//		int screenHeight = display.getHeight();

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
//		freeDraw = new MotionZoneEditView(this);
//		freeDraw.setBackgroundColor(Color.TRANSPARENT);
//		
//		LinearLayout freeDrawLayout = (LinearLayout) findViewById(R.id.draw_layout);
//		freeDraw = new MotionZoneEditView(this);
//        freeDrawLayout.addView(freeDraw);   
		
//		setContentView(freeDraw);
		
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
		int ThumbnailWidth = mImgThumbnail.getLayoutParams().width;
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

	
	@Override
	public void onGlobalLayout() {
	if (mMotionZoneEditView.getWidth() != 0 && mMotionZoneEditView.getHeight() != 0) {
	ViewTreeObserver obs = mMotionZoneEditView.getViewTreeObserver();
	obs.removeGlobalOnLayoutListener(this);
	//do things after get size
		Log.v(TAG, "Kelly "+mMotionZoneEditView.getWidth()+" "+mMotionZoneEditView.getHeight());
		mMotionZoneEditView.setImageBoundary(mMotionZoneEditView.getWidth(), mMotionZoneEditView.getHeight());
		mMotionZoneEditView.fullscreen();
	}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_motion_zone_edit;
	}
	
	@Override
	public void onClick(View view){
		switch(view.getId()){
			case R.id.btn_cancel:{
				break;
			}
			case R.id.btn_full:{
				mMotionZoneEditView.fullscreen();
				break;
			}
			case R.id.btn_ok:{
				break;
			}
			default:
				super.onClick(view);	
		}
	}
}
