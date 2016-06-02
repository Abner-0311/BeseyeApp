package com.app.beseye;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class CameraUpdateActivity extends BeseyeBaseActivity {
	static public final String KEY_UPDATE_INFO = "KEY_UPDATE_INFO";
	private Button mBtnUpdate;
	protected View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	protected ImageView mIvBack;
	protected TextView mTxtNavTitle;
	private JSONArray mArrCamList;
	private JSONObject mNewsObj;
	
	private TextView mTxtUpdateTitle, mTxtUpdateDesc;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		try {
			mArrCamList = new JSONArray(getIntent().getStringExtra(CameraListActivity.KEY_VALID_CAM_INFO));
			mNewsObj = new JSONObject(getIntent().getStringExtra(KEY_UPDATE_INFO));
			//Log.e(BeseyeConfig.TAG, "onCreate(), mNewsObj:"+mNewsObj.toString());
		} catch (JSONException e) {
			Log.e(BeseyeConfig.TAG, "onCreate(), failed to parse cam list");
		}
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_signup_nav, null);
		if(null != mVwNavBar){
			mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			mTxtNavTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != mTxtNavTitle){
				mTxtNavTitle.setText(R.string.update_title);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
			BeseyeUtils.setToolbarPadding(mVwNavBar, 0);
		}
		
		mTxtUpdateTitle = (TextView)findViewById(R.id.txt_update_title);
		if(null != mTxtUpdateTitle){
			String strVer = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(mNewsObj,BeseyeJSONUtil.NEWS_CONTENT), BeseyeJSONUtil.NEWS_OTHER), BeseyeJSONUtil.NEWS_FW_VER);
			//Log.e(BeseyeConfig.TAG, "onCreate(), strVer:"+strVer);
			mTxtUpdateTitle.setText(String.format(getString(R.string.cam_update_title), strVer));
		}
		
		mTxtUpdateDesc = (TextView)findViewById(R.id.txt_update_desc);
		if(null != mTxtUpdateDesc){
			mTxtUpdateDesc.setText(BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(mNewsObj,BeseyeJSONUtil.NEWS_CONTENT), BeseyeJSONUtil.NEWS_DESC));
		}
		
		mBtnUpdate = (Button)this.findViewById(R.id.button_update);
		if(null != mBtnUpdate){
			mBtnUpdate.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_cam_update;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_update:{
				//triggerCamUpdate(mArrCamList, false);
				//showMyDialog(DIALOG_ID_CAM_UPDATE);
				//monitorAsyncTask(new BeseyeCamBEHttpTask.UpdateCamSWTask(this), true, "9ee1316073f54242b6c4cfe6aa2a0eda");
				//showMyDialog(DIALOG_ID_CAM_UPDATE);
				//monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamUpdateStatusTask(this).setDialogId(-1), true, "9ee1316073f54242b6c4cfe6aa2a0eda");
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
