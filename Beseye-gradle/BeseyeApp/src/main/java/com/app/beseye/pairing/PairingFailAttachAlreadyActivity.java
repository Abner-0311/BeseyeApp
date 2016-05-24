package com.app.beseye.pairing;

import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.OpeningPage;
import com.app.beseye.R;
import com.app.beseye.WifiListActivity;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PairingFailAttachAlreadyActivity extends BeseyeNavBarBaseActivity {
	final public static String KEY_ATTACHED_CAM = "KEY_ATTACHED_CAM";
	final public static String KEY_ATTACHED_CAM_HW_ID = "KEY_ATTACHED_CAM_HW_ID";
	
	private Button mBtnDetach;
	private TextView mTxtDesc;
	private JSONObject mObjAttachedCam = null;
	private String mStrAttachedHWID = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.title_cam_attached_already);
		}
		
		if(null != mIvBack){
			mIvBack.setImageResource(R.drawable.sl_event_list_cancel);
		}
		
		try {
			if(null != getIntent().getStringExtra(KEY_ATTACHED_CAM)){
				mObjAttachedCam = new JSONObject(getIntent().getStringExtra(KEY_ATTACHED_CAM));
			}
		} catch (JSONException e) {
			Log.e(TAG, "Cannot parse KEY_ATTACHED_CAM");
		} finally{
			if(null == mObjAttachedCam){
				mStrAttachedHWID = getIntent().getStringExtra(KEY_ATTACHED_CAM_HW_ID);
			}
		}
		
		mTxtDesc = (TextView)findViewById(R.id.tv_attach_already_description);
		if(null != mTxtDesc){
			//ACC BE was already handle re-pairing issue
			/*if(null != mObjAttachedCam){
				String strPairConflictName = BeseyeJSONUtil.getJSONString(mObjAttachedCam, BeseyeJSONUtil.ACC_NAME);
				mTxtDesc.setText(String.format(getResources().getString(R.string.cam_attach_under_your_account_error), strPairConflictName));
			}else*/{
				mTxtDesc.setText(String.format(getResources().getString(R.string.cam_attach_under_other_account_error), mStrAttachedHWID));
			}
		}
		
		mBtnDetach = (Button)findViewById(R.id.button_detach);
		if(null != mBtnDetach){
			mBtnDetach.setOnClickListener(this);
			//mBtnDetach.setText(null != mObjAttachedCam?R.string.btn_detach_cam_now:R.string.btn_contact_cs);
			mBtnDetach.setText(R.string.signup_try_again_btn);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_attach_already;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_detach:{
				break;
			}
			case R.id.iv_nav_left_btn:{
				backToWifiSetupGuidePage();
				break;
			}
			default:
				super.onClick(view);
		}
	}
	
	private void backToWifiSetupGuidePage(){
		Intent intent = new Intent();
		//intent.setClassName(this, PairingWatchOutActivity.class.getName());
		intent.setClassName(this, WifiListActivity.class.getName());
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtras(getIntent().getExtras());
		intent.putExtra(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
		launchActivityByIntent(intent);
		finish();
	}
	
    @Override
   	public boolean onKeyDown(int keyCode, KeyEvent event) {
       	if ((keyCode == KeyEvent.KEYCODE_BACK)){
			backToWifiSetupGuidePage();
			return true;
       	}
   		return super.onKeyDown(keyCode, event);
   	}
}
