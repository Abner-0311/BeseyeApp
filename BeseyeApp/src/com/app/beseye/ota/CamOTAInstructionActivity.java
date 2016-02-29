package com.app.beseye.ota;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.util.BeseyeUtils;

public class CamOTAInstructionActivity extends BeseyeNavBarBaseActivity {
	public static final String CAM_OTA_TYPE = "CAM_OTA_TYPE";
	
	public enum CAM_OTA_INSTR_TYPE{
		TYPE_UPDATE_ALL,
		TYPE_UPDATE_ONE,
		TYPE_UPDATE_BY_OTHER,
		TYPE_UPDATE_COUNT
	}
	
	static public CAM_OTA_INSTR_TYPE translateToOTAType(int iTypeNum){
		CAM_OTA_INSTR_TYPE ret = CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ALL;
		switch(iTypeNum){
			case 1:{
				ret = CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE;
				break;
			}
			case 2:{
				ret = CAM_OTA_INSTR_TYPE.TYPE_UPDATE_BY_OTHER;
				break;
			}
		}
		return ret;
	}
	
	private CAM_OTA_INSTR_TYPE mCamOTAType = CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE;
	private Button mBtnAction;
	private TextView mTvOTAMainDesc, mTvOTASubDesc;
	private ImageView mIvBackground;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mCamOTAType = translateToOTAType(getIntent().getIntExtra(CAM_OTA_TYPE, CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE.ordinal()));
		
		mBtnAction = (Button)findViewById(R.id.button_action);
		if(null != mBtnAction){
			mBtnAction.setOnClickListener(this);
		}
		mTvOTAMainDesc = (TextView)findViewById(R.id.txt_update_title);
		mTvOTASubDesc = (TextView)findViewById(R.id.txt_update_instruction);
		mIvBackground = (ImageView)findViewById(R.id.iv_ota_image);

		BeseyeUtils.setVisibility(mIvBack, View.INVISIBLE);

		if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ALL.equals(mCamOTAType)){
			BeseyeUtils.setText(mTxtNavTitle, getString(R.string.title_cam_update_hint));
			BeseyeUtils.setText(mBtnAction, getString(R.string.next_step));
			BeseyeUtils.setText(mTvOTAMainDesc, getString(R.string.desc_cam_new_ver_avaiable));
			BeseyeUtils.setText(mTvOTASubDesc, getString(R.string.desc_cam_update_instruction));
			BeseyeUtils.setImageRes(mIvBackground, R.drawable.cam_ota_image_loading_one_guy_android);
		}else if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_BY_OTHER.equals(mCamOTAType)){
			BeseyeUtils.setText(mTxtNavTitle, getString(R.string.title_cam_update_hint));
			BeseyeUtils.setText(mBtnAction, getString(R.string.sure));
			BeseyeUtils.setText(mTvOTAMainDesc, getString(R.string.desc_cam_ota_available_update_by_other));
			BeseyeUtils.setText(mTvOTASubDesc, String.format(getString(R.string.desc_cam_update_by_other), mStrVCamName));
			BeseyeUtils.setImageRes(mIvBackground, R.drawable.cam_ota_image_loading_two_guys_android);

		}else{
			BeseyeUtils.setText(mTxtNavTitle, getString(R.string.title_cam_update_hint));
			BeseyeUtils.setText(mBtnAction, getString(R.string.next_step));
			BeseyeUtils.setText(mTvOTAMainDesc, getString(R.string.desc_cam_new_ver_avaiable));
			BeseyeUtils.setText(mTvOTASubDesc, getString(R.string.desc_cam_update_short_intro));
			BeseyeUtils.setImageRes(mIvBackground, R.drawable.cam_ota_image_loading_two_guys_android);

		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_cam_update_instruction;
	}
	
	private void backToCamLstAndCheckUpdateProgress(){
		BeseyeCamSWVersionMgr.getInstance().resetOTAStatusByVcamId(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL, this.mStrVCamID);
		launchDelegateActivity(CameraListActivity.class.getName());
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_BY_OTHER.equals(mCamOTAType)){
				backToCamLstAndCheckUpdateProgress();
			}else{
				showMyDialog(DIALOG_ID_OTA_FORCE_UPDATE);
			}
			return true;
		}else
			return super.onKeyUp(keyCode, event);
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_action:{
				if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_BY_OTHER.equals(mCamOTAType)){
					backToCamLstAndCheckUpdateProgress();
				}else{
					Bundle bundle = new Bundle();
					bundle.putInt(CAM_OTA_TYPE, mCamOTAType.ordinal());
					if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE.equals(mCamOTAType) && null != mCam_obj){
						bundle.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
					}
					launchActivityByClassName(CamOTARemindActivity.class.getName(), bundle);
				}
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
