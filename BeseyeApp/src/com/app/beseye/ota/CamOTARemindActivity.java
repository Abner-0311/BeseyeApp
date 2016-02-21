package com.app.beseye.ota;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.ota.CamOTAInstructionActivity.CAM_OTA_INSTR_TYPE;
import com.app.beseye.util.BeseyeUtils;

public class CamOTARemindActivity extends BeseyeNavBarBaseActivity {

	private CAM_OTA_INSTR_TYPE mCamOTAType = CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE;
	private Button mBtnUpdate;
	private ImageView mIvBackground;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mCamOTAType = CamOTAInstructionActivity.translateToOTAType(getIntent().getIntExtra(CamOTAInstructionActivity.CAM_OTA_TYPE, CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE.ordinal()));
		
		mBtnUpdate = (Button)findViewById(R.id.button_update);
		if(null != mBtnUpdate){
			mBtnUpdate.setOnClickListener(this);
		}
		
		mIvBackground = (ImageView)findViewById(R.id.iv_ota_image);
		BeseyeUtils.setText(mTxtNavTitle, getString(R.string.signup_watch_out_title));		
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_cam_update_warning;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_update:{
				if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ALL.equals(mCamOTAType)){
					BeseyeCamSWVersionMgr.getInstance().performCamGroupUpdate(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL);
				}else if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE.equals(mCamOTAType)){
					//BeseyeCamSWVersionMgr.getInstance().performCamUpdate(camRec);
				}
				launchDelegateActivity(CameraListActivity.class.getName());

				break;
			}
			default:
				super.onClick(view);
		}		
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

	private int miPicSequence = 0;
	static private long ANI_PLAY_INTERVAL = 450L;
	private Runnable mPlayAnimationRunnable = new Runnable(){
		@Override
		public void run() {
			if(null != mIvBackground){
				mIvBackground.setImageResource((0 == miPicSequence%3)?R.drawable.cam_ota_image_1_android:((1 == miPicSequence%3)?R.drawable.cam_ota_image_2_android:R.drawable.cam_ota_image_3_android));
				//mbFirstPic=!mbFirstPic;
				miPicSequence++;
			}
			BeseyeUtils.postRunnable(this, ANI_PLAY_INTERVAL);
		}};
}
