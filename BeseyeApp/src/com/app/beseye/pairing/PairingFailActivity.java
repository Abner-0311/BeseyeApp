package com.app.beseye.pairing;

import com.app.beseye.BeseyeAccountBaseActivity;
import com.app.beseye.OpeningPage;
import com.app.beseye.R;
import com.app.beseye.WifiListActivity;
import com.app.beseye.R.drawable;
import com.app.beseye.R.id;
import com.app.beseye.R.layout;
import com.app.beseye.R.string;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class PairingFailActivity extends BeseyeAccountBaseActivity {
	private Button mBtnTryAgain;
	//private TextView mTxtSetupGuide;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		//getSupportActionBar().hide();
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_title_cam_pair_fail);
		}
		
		if(null != mIvBack){
			mIvBack.setImageResource(R.drawable.sl_event_list_cancel);
		}
		
		mBtnTryAgain = (Button)findViewById(R.id.button_continue);
		if(null != mBtnTryAgain){
			mBtnTryAgain.setOnClickListener(this);
		}
		
//		TextView tvDesc = (TextView)findViewById(R.id.tv_try_again_description_1);
//		if(null != tvDesc){
//			String strGLight = getString(R.string.signup_confirm_led_twinkling_green_light);
//			String strDesc = String.format(getString(R.string.signup_pairing_fail_desc), strGLight);
//			//tvDesc.setText(strDesc);
//			Spannable wordtoSpan = new SpannableString(strDesc);          
//
//			//Spannable str = (Spannable) tvDesc.getEditableText();
//		    int i = strDesc.indexOf(strGLight);
//		    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.cl_link_font_color)), i, i+strGLight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//		    tvDesc.setText(wordtoSpan);
//		}
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
//			case R.id.tv_try_again_description_3:{
//				Toast.makeText(this, "Go to setup guide", Toast.LENGTH_SHORT).show();
//				break;
//			}
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
