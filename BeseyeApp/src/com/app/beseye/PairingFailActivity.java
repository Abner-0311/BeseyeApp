package com.app.beseye;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PairingFailActivity extends BeseyeAccountBaseActivity {
	private Button mBtnTryAgain;
	private TextView mTxtSetupGuide;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//getSupportActionBar().hide();
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_title_cam_pair_fail);
		}
		
		mBtnTryAgain = (Button)findViewById(R.id.button_continue);
		if(null != mBtnTryAgain){
			mBtnTryAgain.setOnClickListener(this);
		}
		
		mTxtSetupGuide = (TextView)findViewById(R.id.tv_try_again_description_3);
		if(null != mTxtSetupGuide){
			mTxtSetupGuide.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_try_again;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_continue:
			case R.id.iv_nav_left_btn:{
				backToWifiSetupGuidePage();
				break;
			}
			case R.id.tv_try_again_description_3:{
				Toast.makeText(this, "Go to setup guide", Toast.LENGTH_SHORT).show();
				break;
			}
			default:
				super.onClick(view);
		}
	}
	
	private void backToWifiSetupGuidePage(){
		Intent intent = new Intent();
		intent.setClassName(this, PairingWatchOutActivity.class.getName());
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
