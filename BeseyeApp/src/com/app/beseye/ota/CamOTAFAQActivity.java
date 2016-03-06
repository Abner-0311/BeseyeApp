package com.app.beseye.ota;

import android.content.Intent;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;

import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.util.BeseyeUtils;


public class CamOTAFAQActivity extends BeseyeNavBarBaseActivity {
	private Button mBtnContactUs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		mBtnContactUs = (Button)findViewById(R.id.button_contact_us);
		if(null != mBtnContactUs){
			mBtnContactUs.setOnClickListener(this);
		}
		
		BeseyeUtils.setText(mTxtNavTitle, getString(R.string.title_faq_page));
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_cam_update_faq;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_contact_us:{
				Bundle b = getIntent().getExtras();
				//b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityForResultByClassName(CamOTAFeedbackActivity.class.getName(), b, REQUEST_OTA_FEEDBACK);
				break;
			}
			default:
				super.onClick(view);
		}		
	}
	
	static public final int REQUEST_OTA_FEEDBACK = 10001;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_OTA_FEEDBACK == requestCode && resultCode == RESULT_OK){
			setResult(RESULT_OK, intent);
			finish();
		}else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}
}
