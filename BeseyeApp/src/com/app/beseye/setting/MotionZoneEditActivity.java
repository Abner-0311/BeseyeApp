package com.app.beseye.setting;



import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.RemoteImageView;

public class MotionZoneEditActivity extends BeseyeBaseActivity{
	
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
		
		MotionZoneEditView mEditView = new MotionZoneEditView(this);

	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_motion_zone_edit;
	}
}
