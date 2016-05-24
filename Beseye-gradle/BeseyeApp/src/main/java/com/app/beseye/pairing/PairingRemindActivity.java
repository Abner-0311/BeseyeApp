package com.app.beseye.pairing;

import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeUtils;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class PairingRemindActivity extends BeseyeNavBarBaseActivity {
	static public final String KEY_ADD_CAM_FROM_LIST = "KEY_ADD_CAM_FROM_LIST";
	private Button mBtnStart;
	private ImageView mIvSetupHint;
	//private boolean mbFirstPic = true;
	private int miPicSequence = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_confirm_led_title);
		}
		
		mBtnStart = (Button)findViewById(R.id.button_start);
		if(null != mBtnStart){
			mBtnStart.setOnClickListener(this);
		}
		
		mIvSetupHint = (ImageView)findViewById(R.id.iv_before_setup_top_img);
		
		TextView tvDesc = (TextView)findViewById(R.id.tv_before_setup_description);
		if(null != tvDesc){
			String strGLight = getString(R.string.signup_confirm_led_twinkling_green_light);
			String strDesc = String.format(getString(R.string.signup_confirm_led_desc), strGLight);
			//tvDesc.setText(strDesc);
			Spannable wordtoSpan = new SpannableString(strDesc);          

			//Spannable str = (Spannable) tvDesc.getEditableText();
		    int i = strDesc.indexOf(strGLight);
		    if(i >=0){
			    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strGLight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    }
		    tvDesc.setText(wordtoSpan);
		}
		
//		GifMovieView gifTop = (GifMovieView)findViewById(R.id.iv_before_setup_top_img);
//		if(null != gifTop){
//			int iDeviceWidth = BeseyeUtils.getDeviceWidth(this);
//			int iGifRedId = R.drawable.signup_blinking_green_720;
//			if(iDeviceWidth >= 1080){
//				iGifRedId = R.drawable.signup_blinking_green_1080;
//			}
//			gifTop.setMovieResource(iGifRedId);
//		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		BeseyeUtils.removeRunnable(mPlayAnimationRunnable);
		BeseyeUtils.postRunnable(mPlayAnimationRunnable, ANI_PLAY_INTERVAL);
	}

	@Override
	protected void onPause() {
		BeseyeUtils.removeRunnable(mPlayAnimationRunnable);
		super.onPause();
	}

	static private long ANI_PLAY_INTERVAL = 450L;
	private Runnable mPlayAnimationRunnable = new Runnable(){
		@Override
		public void run() {
			if(null != mIvSetupHint){
				mIvSetupHint.setImageResource((0 == miPicSequence%3)?R.drawable.signup_blinking_green_01:((1 == miPicSequence%3)?R.drawable.signup_blinking_green_02:R.drawable.signup_blinking_green_03));
				//mbFirstPic=!mbFirstPic;
				miPicSequence++;
			}
			BeseyeUtils.postRunnable(this, ANI_PLAY_INTERVAL);
		}};
	
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
					//launchActivityByClassName(SignupActivity.class.getName());
					launchActivityByClassName(PairingWatchOutActivity.class.getName(), getIntent().getExtras());
				}
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
