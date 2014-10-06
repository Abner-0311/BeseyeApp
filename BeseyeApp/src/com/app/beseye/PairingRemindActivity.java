package com.app.beseye;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PairingRemindActivity extends BeseyeAccountBaseActivity {
	static public final String KEY_ADD_CAM_FROM_LIST = "KEY_ADD_CAM_FROM_LIST";
	private Button mBtnStart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		mBtnStart = (Button)findViewById(R.id.button_start);
		if(null != mBtnStart){
			mBtnStart.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_before_setup;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_start:{
				if(getIntent().getBooleanExtra(KEY_ADD_CAM_FROM_LIST, false)){
					launchActivityByClassName(PairingWatchOutActivity.class.getName(), getIntent().getExtras());
				}else{
					launchActivityByClassName(SignupActivity.class.getName());
				}
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
