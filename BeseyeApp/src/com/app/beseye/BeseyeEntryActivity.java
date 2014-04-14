package com.app.beseye;

import com.app.beseye.widget.BeseyeDatetimePickerDialog;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class BeseyeEntryActivity extends BeseyeBaseActivity {

	private TextView mTvSetupAndSignup, mTvLearnMore, mTvLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		getSupportActionBar().hide();
		
		mTvSetupAndSignup = (TextView)findViewById(R.id.button_signup);
		if(null != mTvSetupAndSignup){
			mTvSetupAndSignup.setOnClickListener(this);
		}
		
		mTvLearnMore = (TextView)findViewById(R.id.tv_bottom_beseye);
		if(null != mTvLearnMore){
			mTvLearnMore.setOnClickListener(this);
		}
		
		mTvLogin = (TextView)findViewById(R.id.button_signin);
		if(null != mTvLogin){
			mTvLogin.setOnClickListener(this);
		}
		
		//new BeseyeDatetimePickerDialog(this).show(); 
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_firstpage;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_signup:{
				launchActivityByClassName(PairingRemindActivity.class.getName());
				break;
			}
			case R.id.button_signin:{
				launchActivityByClassName(LoginActivity.class.getName());
				break;
			}
			case R.id.tv_bottom_beseye:{
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
