package com.app.beseye.pairing;

import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.R;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PairingWatchOutActivity extends BeseyeNavBarBaseActivity {
	private Button mBtnStart;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_watch_out_title);
		}
		
		TextView tvDesc = (TextView)findViewById(R.id.tv_watch_out_description);
		if(null != tvDesc){
			String strBeBeSound = getString(R.string.signup_watch_out_desc_bebebe);
			String strSpeaker = getString(R.string.signup_watch_out_desc_speaker);
			String strDesc = String.format(getString(R.string.signup_watch_out_desc), strBeBeSound, strSpeaker);
			
			Spannable wordtoSpan = new SpannableString(strDesc);          

		    int i = strDesc.indexOf(strBeBeSound);
		    if(0 <= i){
			    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strBeBeSound.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    }
		    
		    i = strDesc.indexOf(strSpeaker);
		    if(0 <= i){
		    	wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strSpeaker.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    }
		    tvDesc.setText(wordtoSpan);
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
				//launchActivityByClassName(WifiListActivity.class.getName(), getIntent().getExtras());
				launchActivityByClassName(SoundPairingActivity.class.getName(), getIntent().getExtras());
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
