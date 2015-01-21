package com.app.beseye;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PairingPlugPowerActivity extends BeseyeAccountBaseActivity {
	private Button mBtnStart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_plug_in_title);
		}
		
		mBtnStart = (Button)findViewById(R.id.button_continue);
		if(null != mBtnStart){
			mBtnStart.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_power_plug;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_continue:{
				launchActivityByClassName(SignupActivity.class.getName(), getIntent().getExtras());
				break;
			}
			default:
				super.onClick(view);
		}		
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(BeseyeEntryActivity.REQUEST_SIGNUP == requestCode){
			if(resultCode == RESULT_OK){
				finish();
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
}
