package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View.OnClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.MotionNotificationSettingView;
import com.app.beseye.widget.RemoteImageView;


public class MotionNotificationSettingActivity extends BeseyeBaseActivity{

	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;	
	private ViewGroup mVgMotionZoneEdit;
	ImageView drawingImageView;
	Canvas canvas;
	Paint linePaint;
	int width, height;
	int strokeWidth = 8;
	
	private static final int REQUEST_MOTION_ZONE_EDIT = 1001;
	
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
				txtTitle.setText(R.string.cam_setting_title_motion_zone);
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
		
		mVgMotionZoneEdit = (ViewGroup)findViewById(R.id.vg_motion_zone);
		if(null != mVgMotionZoneEdit){
			mVgMotionZoneEdit.setOnClickListener(this);
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
		
		
		width = (int) (mImgThumbnail.getLayoutParams().width);
		height = (int) (mImgThumbnail.getLayoutParams().height);
		
		drawingImageView = (ImageView) this.findViewById(R.id.iv_motion_zone_setting);
	    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		canvas = new Canvas(bitmap);
	    drawingImageView.setImageBitmap(bitmap);

	    linePaint = new Paint();
	    
        linePaint.setAntiAlias(true);
        linePaint.setDither(true);			
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.parseColor("#00bbb3"));
        linePaint.setStrokeWidth(strokeWidth);

        
        double[] r = getRatioFromServer();
	    float leftx = (float) (r[0]*width);
	    float topy = (float) (r[1]*height);
	    float rightx = (float) (r[2]*width);
	    float bottomy = (float) (r[3]*height);

	    canvas.drawRect(leftx+strokeWidth/2, topy+strokeWidth/2, rightx-strokeWidth/2, bottomy-strokeWidth/2, linePaint);
}

	@Override
	public void onClick(View view){
		switch(view.getId()){
			case R.id.vg_motion_zone:{
				Bundle b = new Bundle();
				//TODO: should always call getRatioFromServer() ?
				b.putDoubleArray("MotionZoneRatio", getRatioFromServer());
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(MotionZoneEditActivity.class.getName(), b, REQUEST_MOTION_ZONE_EDIT);
				break;
			}
			default:
				super.onClick(view);	
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_MOTION_ZONE_EDIT == requestCode && resultCode == RESULT_OK){
			Paint p = new Paint();
	        p.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
	        canvas.drawPaint(p);
	        p.setXfermode(new PorterDuffXfermode(Mode.SRC));
	        
			intent.getDoubleArrayExtra("MotionZoneRatio");
			
			double[] r = intent.getDoubleArrayExtra("MotionZoneRatio");
		    float leftx = (float) (r[0]*width);
		    float topy = (float) (r[1]*height);
		    float rightx = (float) (r[2]*width);
		    float bottomy = (float) (r[3]*height);
	
		    canvas.drawRect(leftx+strokeWidth/2, topy+strokeWidth/2, rightx-strokeWidth/2, bottomy-strokeWidth/2, linePaint);
		}
		super.onActivityResult(requestCode, resultCode, intent);
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
	
	@Override
	protected void onSessionComplete(){
		if(DEBUG)
			Log.i(TAG, "onSessionComplete()");	
		super.onSessionComplete();
	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "MotionNotificationSettingActivity::onResume()");
		super.onResume();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_motion_notification;
	}
}
