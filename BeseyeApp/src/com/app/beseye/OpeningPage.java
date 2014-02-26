package com.app.beseye;

import com.app.beseye.util.BeseyeUtils;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class OpeningPage extends BeseyeBaseActivity {
	private static boolean sbFirstLaunch = true;
	private static final long TIME_TO_CLOSE_OPENING_PAGE = 3000L;
	private ImageView[] mIvHeartList = new ImageView[22];
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		getSupportActionBar().hide();
		if(!sbFirstLaunch){
			launchNextPage();
		}
//		if(SPECIAL_MODE){
//			mIvHeartList[0] = (ImageView)findViewById(R.id.iv_heart1);
//			mIvHeartList[1] = (ImageView)findViewById(R.id.iv_heart2_1);
//			mIvHeartList[2] = (ImageView)findViewById(R.id.iv_heart3_1);
//			mIvHeartList[3] = (ImageView)findViewById(R.id.iv_heart4_1);
//			mIvHeartList[4] = (ImageView)findViewById(R.id.iv_heart5_1);
//			mIvHeartList[5] = (ImageView)findViewById(R.id.iv_heart6_1);
//			mIvHeartList[6] = (ImageView)findViewById(R.id.iv_heart7_1);
//			mIvHeartList[7] = (ImageView)findViewById(R.id.iv_heart8_1);
//			mIvHeartList[8] = (ImageView)findViewById(R.id.iv_heart9_1);
//			mIvHeartList[9] = (ImageView)findViewById(R.id.iv_heart10_1);
//			mIvHeartList[10] = (ImageView)findViewById(R.id.iv_heart11_1);
//			mIvHeartList[11] = (ImageView)findViewById(R.id.iv_heart12_1);
//			mIvHeartList[12] = (ImageView)findViewById(R.id.iv_heart11_2);
//			mIvHeartList[13] = (ImageView)findViewById(R.id.iv_heart10_2);
//			mIvHeartList[14] = (ImageView)findViewById(R.id.iv_heart9_2);
//			mIvHeartList[15] = (ImageView)findViewById(R.id.iv_heart8_2);
//			mIvHeartList[16] = (ImageView)findViewById(R.id.iv_heart7_2);
//			mIvHeartList[17] = (ImageView)findViewById(R.id.iv_heart6_2);
//			mIvHeartList[18] = (ImageView)findViewById(R.id.iv_heart5_2);
//			mIvHeartList[19] = (ImageView)findViewById(R.id.iv_heart4_2);
//			mIvHeartList[20] = (ImageView)findViewById(R.id.iv_heart3_2);
//			mIvHeartList[21] = (ImageView)findViewById(R.id.iv_heart2_2);
//			for(int i = 0; i< mIvHeartList.length;i++){
//				mIvHeartList[i].setVisibility(View.INVISIBLE);
//			}	
//		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_opening;
		//return !SPECIAL_MODE?R.layout.layout_opening:R.layout.layout_opening_heart;
	}

	@Override
	protected void onResume() {
		super.onResume();
//		if(SPECIAL_MODE){
//			mHeartRunnable = new HeartRunnable();
//			mIvHeartList[0].postDelayed(mHeartRunnable, 2000);
//		}else{
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					launchNextPage();
				}
			}, TIME_TO_CLOSE_OPENING_PAGE);
			sbFirstLaunch = false;
//		}
	}
	
	@Override
	protected void onPause() {
//		if(mHeartRunnable!= null){
//			mIvHeartList[0].removeCallbacks(mHeartRunnable);
//			mHeartRunnable = null;
//			for(int i = 0; i< mIvHeartList.length;i++){
//				mIvHeartList[i].setVisibility(View.INVISIBLE);
//			}
//		}
		super.onPause();
	}
	
//	//private static boolean SPECIAL_MODE = false;
//	private class HeartRunnable implements Runnable{
//		int idx = 0;
//		int iExtraStep = 0;
//		boolean bFirstRound = true;
//		@Override
//		public void run() {
//			if(bFirstRound){
//				if(idx > mIvHeartList.length/2){
//					if(0 == iExtraStep || 2 == iExtraStep || 4 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						}
//						iExtraStep++;
//					}else if(1 == iExtraStep || 3 == iExtraStep ){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						iExtraStep++;
//						
//					}else if(5 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						idx = 0;
//						iExtraStep = 0;
//						bFirstRound = false;
//						mIvHeartList[0].postDelayed(this, 2000);
//						return;
//					}
//					mIvHeartList[0].postDelayed(this, 500);
//				}else{
//					if(idx == 0){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						}
//					}
//					if(idx == 0)
//						mIvHeartList[idx].setVisibility(View.VISIBLE);
//					else{
//						mIvHeartList[idx].setVisibility(View.VISIBLE);
//						mIvHeartList[mIvHeartList.length-idx].setVisibility(View.VISIBLE);
//					}
//					mIvHeartList[0].postDelayed(this, idx == mIvHeartList.length/2?500:100);
//					idx+=1;
//				}
//			}else{
//				if(idx > mIvHeartList.length/2){
//					if(0 == iExtraStep || 2 == iExtraStep || 4 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						}
//						iExtraStep++;
//					}else if(1 == iExtraStep || 3 == iExtraStep ){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						iExtraStep++;
//						
//					}else if(5 == iExtraStep){
//						for(int i = 0; i< mIvHeartList.length;i++){
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//						idx = 0;
//						iExtraStep = 0;
//						bFirstRound = true;
//						mIvHeartList[0].postDelayed(this, 2000);
//						return;
//					}
//					mIvHeartList[0].postDelayed(this, 500);
//				}else{
//					for(int i = 0; i< mIvHeartList.length;i++){
//						if((idx == i || (mIvHeartList.length - idx ) == i) && i != mIvHeartList.length/2)
//							mIvHeartList[i].setVisibility(View.INVISIBLE);
//						else{
//							mIvHeartList[i].setVisibility(View.VISIBLE);
//						}
//					}
//					mIvHeartList[0].postDelayed(this, idx == mIvHeartList.length/2?500:100);
//					idx+=1;
//				}
//			}
//		}
//	}
//	
//	private HeartRunnable mHeartRunnable = null;
	private void launchNextPage(){
		Intent intent = new Intent();
		intent.setClass(OpeningPage.this, CameraViewActivity.class);//WifiListActivity.class);
		startActivity(intent);
		finish();
	}
}
