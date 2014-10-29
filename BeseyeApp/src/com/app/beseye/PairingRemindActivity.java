package com.app.beseye;

import com.app.beseye.util.BeseyeUtils;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class PairingRemindActivity extends BeseyeAccountBaseActivity {
	static public final String KEY_ADD_CAM_FROM_LIST = "KEY_ADD_CAM_FROM_LIST";
	private Button mBtnStart;
	private ImageView mIvSetupHint;
	private boolean mbFirstPic = true;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		mBtnStart = (Button)findViewById(R.id.button_start);
		if(null != mBtnStart){
			mBtnStart.setOnClickListener(this);
		}
		
		mIvSetupHint = (ImageView)findViewById(R.id.iv_before_setup_top_img);

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

	static private long ANI_PLAY_INTERVAL = 200L;
	private Runnable mPlayAnimationRunnable = new Runnable(){
		@Override
		public void run() {
			if(null != mIvSetupHint){
				mIvSetupHint.setImageResource(mbFirstPic?R.drawable.signup_blinking_green_02:R.drawable.signup_blinking_green_01);
				mbFirstPic=!mbFirstPic;
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
					launchActivityByClassName(SignupActivity.class.getName());
				}
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
