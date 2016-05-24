package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.util.BeseyeJSONUtil;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public abstract class BeseyeNavBarBaseActivity extends BeseyeBaseActivity {
	protected View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	protected ImageView mIvBack;
	protected TextView mTxtNavTitle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG)
			Log.d(TAG, "BeseyeNavBarBaseActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		try {
			String strCamObj = getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ);
			if(null != strCamObj && 0 < strCamObj.length()){
				mCam_obj = new JSONObject(strCamObj);
				if(null != mCam_obj){
					mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
					mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
				}
			}
		} catch (JSONException e1) {
			Log.e(TAG, "BeseyeNavBarBaseActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_signup_nav, null);
		if(null != mVwNavBar){
			mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			mTxtNavTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
	}
}
