package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.NOTIFY_OBJ;
import static com.app.beseye.util.BeseyeJSONUtil.OBJ_TIMESTAMP;
import static com.app.beseye.util.BeseyeJSONUtil.STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONObject;

import java.util.List;

import org.json.JSONArray;
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
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.RemoteImageView;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;



public class MotionNotificationSettingActivity extends BeseyeBaseActivity 
												implements OnSwitchBtnStateChangedListener{

	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;	
	private ViewGroup mVgMotionZoneEdit;
	private ImageView mIvDrawingImageView;
	private ImageView mIvImageMask;
	private double[] mRatios = {-1.0, -1.0, -1.0, -1.0};
	private BeseyeSwitchBtn mNotifyMeSwitchBtn;
	private boolean mbModified = false;
	private RemoteImageView mImgThumbnail;
	private int mImageWidth, mImageHeight;
	private boolean mbChangeBySelf = false;
	
	private Canvas canvas;
	private Paint linePaint, rectPaint;
	private int strokeWidth;

	private final double minZoneRatio = 0.2;
	private final double confidenceV = 0.95;		//for different device may have different double
    private final int maskAlpha = 153;				//255*0.6
	
	private String[] mStrObjKey = {BeseyeJSONUtil.MOTION_ZONE_LEFT, 
			   BeseyeJSONUtil.MOTION_ZONE_TOP,
			   BeseyeJSONUtil.MOTION_ZONE_RIGHT,
			   BeseyeJSONUtil.MOTION_ZONE_BOTTOM
			   };
	
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
		
		mNotifyMeSwitchBtn = (BeseyeSwitchBtn)findViewById(R.id.sb_motion_notify_switch);
		if(null != mNotifyMeSwitchBtn){
			mNotifyMeSwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
			mNotifyMeSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		
		mVgMotionZoneEdit = (ViewGroup)findViewById(R.id.vg_motion_zone);
		if(null != mVgMotionZoneEdit){
			mVgMotionZoneEdit.setOnClickListener(this);
		}
		
    	monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(-1), true, mStrVCamID);
    			
    	mImageWidth = BeseyeUtils.getDeviceWidth(this);
		mImageHeight = (int) ((double)mImageWidth/16.0*9);
		
		
		mImgThumbnail = (RemoteImageView)findViewById(R.id.iv_motion_zone_thumbnail);
		if(null != mImgThumbnail) {
			Bitmap defaultImage = BitmapFactory.decodeResource(getResources(), R.drawable.cameralist_s_view_noview_bg);
			mImgThumbnail.setImageBitmap(Bitmap.createScaledBitmap(defaultImage, mImageWidth, mImageHeight, false));
		}
		
	    mIvImageMask = (ImageView) this.findViewById(R.id.iv_motion_zone_mask);
	    if(null != mIvImageMask){
		    Bitmap bitmapMask = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
		    mIvImageMask.setImageBitmap(bitmapMask);
		    mIvImageMask.setBackgroundColor(this.getResources().getColor(R.color.camera_list_video_mask));
		}
	    
	    Bitmap bitmap = Bitmap.createBitmap(mImageWidth, mImageHeight, Bitmap.Config.ARGB_8888);
		mIvDrawingImageView = (ImageView) this.findViewById(R.id.iv_motion_zone_setting);
		if(null != mIvDrawingImageView) {
			mIvDrawingImageView.setImageBitmap(bitmap);
		}
		canvas = new Canvas(bitmap);
		strokeWidth = getResources().getDimensionPixelSize(R.dimen.motion_zone_strokewidth);
		
	    linePaint = new Paint();	    
        linePaint.setAntiAlias(true);
        linePaint.setDither(true);			
        linePaint.setStrokeJoin(Paint.Join.MITER);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(getResources().getColor(R.color.beseye_color_normal));
        linePaint.setStrokeWidth(strokeWidth);

//	    rectPaint = new Paint();
//	    rectPaint.setAntiAlias(true);
//        rectPaint.setDither(true);
//        rectPaint.setStrokeJoin(Paint.Join.MITER);
//        rectPaint.setStyle(Paint.Style.FILL_AND_STROKE);
//        rectPaint.setColor(getResources().getColor(R.color.mask_black));
//        rectPaint.setAlpha(maskAlpha);
//        rectPaint.setStrokeWidth(0);

        mRatios = getRatioFromServer();
        drawLineRect();
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
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
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
					finish();
				}
				updateNotificationTypeState();
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	protected void onSessionComplete(){
		super.onSessionComplete();
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this), true, mStrVCamID);
		}else{
			updateNotificationTypeState();
		}
	}
	
	private void setThumbnail(){
		if(null != mImgThumbnail){
			BeseyeUtils.setThumbnailRatio(mImgThumbnail, mImageWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
			mImgThumbnail.setURI(BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_THUMB), R.drawable.cameralist_s_view_noview_bg, BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID));
			mImgThumbnail.loadImage();
		}
	}
	
	@Override
	public void onClick(View view){
		switch(view.getId()){
			case R.id.vg_motion_zone:{
				Bundle b = new Bundle();
				b.putDoubleArray("MotionZoneRatio", mRatios);
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(MotionZoneEditActivity.class.getName(), b, REQUEST_MOTION_ZONE_EDIT);
				break;
			} 
			case R.id.iv_nav_left_btn:{
				setNotifySetting();
			}
			default:
				super.onClick(view);	
		}
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			setNotifySetting();
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
	}

	private void setNotifySetting(){
		if(checkDiff()){
			JSONObject obj =  new JSONObject();
			if(null != obj){
				BeseyeJSONUtil.setJSONBoolean(obj, BeseyeJSONUtil.STATUS, (null != mNotifyMeSwitchBtn && mNotifyMeSwitchBtn.getSwitchState() == SwitchState.SWITCH_ON));
				monitorAsyncTask(new BeseyeCamBEHttpTask.SetNotifySettingTask(this), true, mStrVCamID, obj.toString());
			}
		}else{
			finish();
		}
	}
	
	private boolean checkDiff(){
		boolean bRet = false;
		JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ);
		if(null != notify_obj){
			boolean bNotifyMe = BeseyeJSONUtil.getJSONBoolean(notify_obj, STATUS);
			if(!mNotifyMeSwitchBtn.getSwitchState().equals(bNotifyMe?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF)){
				bRet = true;
			}
		}	
		return bRet;
	}
	
	@Override
	protected void updateUICallback(){
		if(!mbChangeBySelf) {
			mRatios = getRatioFromServer();
		}
		drawLineRect();
		mbChangeBySelf = false;
	}
	
	private void drawLineRect(){
		Paint p = new Paint();
        p.setXfermode(new PorterDuffXfermode(Mode.CLEAR));
        canvas.drawPaint(p);
        p.setXfermode(new PorterDuffXfermode(Mode.SRC));   
        
		float left = (float) (mRatios[0]*mImageWidth);
	    float top = (float) (mRatios[1]*mImageHeight);
	    float right = (float) (mRatios[2]*mImageWidth);
	    float bottom = (float) (mRatios[3]*mImageHeight);

	    canvas.drawRect(left+strokeWidth/2, top+strokeWidth/2, right-strokeWidth/2, bottom-strokeWidth/2, linePaint);
//	      canvas.drawRect(0, 0, mImageWidth, top, rectPaint); 
//        canvas.drawRect(0, top, left, bottom, rectPaint);	
//        canvas.drawRect(right, top, mImageWidth, bottom, rectPaint);
//        canvas.drawRect(0, bottom, mImageWidth, mImageHeight, rectPaint);
        
	    Rect rHole = new Rect((int)left, (int)top, (int)right, (int)bottom);
	    canvas.clipRect(rHole,  Region.Op.DIFFERENCE);
	    canvas.drawARGB(maskAlpha, 0, 0, 0);
	    canvas.clipRect(new Rect(0, 0, mImageWidth, mImageHeight), Region.Op.REPLACE);
	    
        mIvDrawingImageView.invalidate();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_MOTION_ZONE_EDIT == requestCode && resultCode == RESULT_OK){
			mbChangeBySelf = true;
			
			try {
				mCam_obj = new JSONObject(intent.getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		    setThumbnail();
		     
	        mRatios = intent.getDoubleArrayExtra("MotionZoneRatio");
	        drawLineRect();
		}
		super.onActivityResult(requestCode, resultCode, intent);
	}
	
	private double[] getRatioFromServer(){
		double[] r = {-1.0, -1.0, -1.0, -1.0};
		
		JSONArray motion_zone_array =  BeseyeJSONUtil.getJSONArray(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), BeseyeJSONUtil.MOTION_ZONE);
		if(motion_zone_array == null){
			Log.e(TAG, "motion_zone_array is null");
		} else{
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
		}
			
		return checkRatioRange(r);
	}
	
	private double[] checkRatioRange(double[] r){
		boolean isRatioVailate = true;
		
		// check 0~1
		for(int i=0; i<r.length; i++){
			if(0 > r[i] || 1 < r[i]){
				isRatioVailate = false;
				break;
			}
		}
		
		// check minL 
		if(isRatioVailate){
			if( (r[3]-r[1]) < minZoneRatio*confidenceV || (r[2]-r[0]) < minZoneRatio/16.0*9*confidenceV){
				isRatioVailate = false;
			}
		}
		
		if(!isRatioVailate){
			r[0] = 0;
			r[1] = 0;
			r[2] = 1;
			r[3] = 1;
		}
		
		return r;
	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "MotionNotificationSettingActivity::onResume()");	
		super.onResume();
		
		if(!mbFirstResume){
			updateNotificationTypeState();
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_motion_notification;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		mbModified = true;
		updateNotificationTypeState();	
	}
	
	private void updateNotificationTypeState(){
		JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ);
		if(false == mbModified){
			boolean bNotifyMe = false;
			if(null != notify_obj){
				bNotifyMe = BeseyeJSONUtil.getJSONBoolean(notify_obj, STATUS);
			}
			
			if(null != mNotifyMeSwitchBtn){
				mNotifyMeSwitchBtn.setSwitchState((bNotifyMe)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
			}
			BeseyeUtils.setEnabled(mVgMotionZoneEdit, bNotifyMe);
			BeseyeUtils.setVisibility(mIvImageMask, bNotifyMe?View.GONE:View.VISIBLE);
		}else{
			boolean bNotifyMe = false;
			if(null != mNotifyMeSwitchBtn){
				bNotifyMe = mNotifyMeSwitchBtn.getSwitchState().equals(SwitchState.SWITCH_ON);
			}
			BeseyeUtils.setEnabled(mVgMotionZoneEdit, bNotifyMe);
			BeseyeUtils.setVisibility(mIvImageMask, bNotifyMe?View.GONE:View.VISIBLE);
		}
	}
}
