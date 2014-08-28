package com.app.beseye;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PairingWatchOutActivity extends BeseyeAccountBaseActivity {
	private Button mBtnStart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.watch_out_sound);
		}
		
		mBtnStart = (Button)findViewById(R.id.button_continue);
		if(null != mBtnStart){
			mBtnStart.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_watch_out_sound;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_continue:{
				launchActivityByClassName(WifiListActivity.class.getName());
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
