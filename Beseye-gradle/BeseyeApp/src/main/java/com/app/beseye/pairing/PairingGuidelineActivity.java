package com.app.beseye.pairing;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.OpeningPage;
import com.app.beseye.R;
import com.app.beseye.WifiListActivity;
import com.app.beseye.util.BeseyeUtils;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class PairingGuidelineActivity extends BeseyeNavBarBaseActivity {
	private Button mBtnTryAgain;
	private WebView mWvGuideline;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		//getSupportActionBar().hide();
		
		mWvGuideline = (WebView)findViewById(R.id.wv_guideline);
		if(null != mWvGuideline){
			String strUrl = null;
			try {
				strUrl = "https://www.beseye.com/signup_faq_mobile?lang="+URLEncoder.encode(BeseyeUtils.getLocaleString(), "utf-8");				 
				mWvGuideline.getSettings().setJavaScriptEnabled(true); 
				mWvGuideline.getSettings().setDomStorageEnabled(true);
				mWvGuideline.loadUrl(strUrl);
				if(DEBUG){
					Log.i(TAG, "PairingGuidelineActivity::onCreate(), strUrl:"+strUrl);
				}
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_try_again_description_3);
		}
		
		if(null != mIvBack){
			mIvBack.setImageResource(R.drawable.sl_event_list_cancel);
		}
		
		mBtnTryAgain = (Button)findViewById(R.id.button_continue);
		if(null != mBtnTryAgain){
			mBtnTryAgain.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_guideline;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_continue:
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
