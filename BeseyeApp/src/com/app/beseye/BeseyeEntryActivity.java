package com.app.beseye;

import java.util.Calendar;

import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.widget.BeseyeDatetimePickerDialog;
import com.app.beseye.widget.BeseyeDatetimePickerDialog.OnDatetimePickerClickListener;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class BeseyeEntryActivity extends BeseyeBaseActivity {

	private TextView mTvSetupAndSignup, mTvLearnMore, mTvLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		getSupportActionBar().hide();
		findViewById(R.id.iv_signup_top_logo).setOnClickListener(this);
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
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_firstpage;
	}
	
	private int miDemoCount=0;

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_signup:{
				launchActivityForResultByClassName(PairingRemindActivity.class.getName(), null, REQUEST_SIGNUP);
				break;
			}
			case R.id.button_signin:{
				launchActivityForResultByClassName(LoginActivity.class.getName(), null, REQUEST_SIGNUP);
				break;
			}
			case R.id.tv_bottom_beseye:{
				break;
			}
			case R.id.iv_signup_top_logo:{
				if(BeseyeConfig.DEBUG){
					miDemoCount++;
					if(miDemoCount >=5){
						launchActivityByClassName(BeseyeComputexModeActivity.class.getName());
						miDemoCount =0;
					}
				}
				break;
			}
			default:
				super.onClick(view);
		}		
	}
	
	static public final int REQUEST_SIGNUP = 1;
	static public final int REQUEST_SIGNIN = 2;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_SIGNUP == requestCode || REQUEST_SIGNIN == requestCode){
			if(resultCode == RESULT_OK){
				finish();
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
}
