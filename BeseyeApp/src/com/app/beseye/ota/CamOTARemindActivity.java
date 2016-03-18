package com.app.beseye.ota;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONObject;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.BeseyeBaseActivity.OnResumeUpdateCamInfoRunnable;
import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_VER_CHECK_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.OnCamUpdateStatusChangedListener;
import com.app.beseye.ota.CamOTAInstructionActivity.CAM_OTA_INSTR_TYPE;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;

public class CamOTARemindActivity extends BeseyeNavBarBaseActivity implements OnCamUpdateStatusChangedListener{

	private CAM_OTA_INSTR_TYPE mCamOTAType = CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE;
	private Button mBtnUpdate;
	private ImageView mIvBackground;
	private boolean mbHaveClickOk = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mCamOTAType = CamOTAInstructionActivity.translateToOTAType(getIntent().getIntExtra(CamOTAInstructionActivity.CAM_OTA_TYPE, CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE.ordinal()));
		
		mBtnUpdate = (Button)findViewById(R.id.button_update);
		if(null != mBtnUpdate){
			mBtnUpdate.setOnClickListener(this);
			if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ALL.equals(mCamOTAType)){
				BeseyeUtils.setText(mBtnUpdate, getString(R.string.action_cam_update_all));
			}else{		
				BeseyeUtils.setText(mBtnUpdate, getString(R.string.update_now));
			}
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
				if(NetworkMgr.getInstance().isNetworkConnected()){
					if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ALL.equals(mCamOTAType)){
						BeseyeCamSWVersionMgr.getInstance().performCamGroupUpdate(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL);
						launchDelegateActivity(CameraListActivity.class.getName());
					}else if(CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE.equals(mCamOTAType)){
						BeseyeCamSWVersionMgr.getInstance().registerOnCamUpdateStatusChangedListener(this);
						BeseyeCamSWVersionMgr.getInstance().performCamUpdate(BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL, mStrVCamID), true);
					}
				}else{
					showNoNetworkDialog();
				}
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
	
	@Override
	protected void onDestroy() {
		BeseyeCamSWVersionMgr.getInstance().unregisterOnCamUpdateStatusChangedListener(this);
		super.onDestroy();
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
		
	@Override
	public void onCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec) {
		if(strVcamId.equals(this.mStrVCamID)){
			int iErrCode = objUpdateRec.getErrCode();
			if(prevStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_REQUEST) && curStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING) && 0 == iErrCode){
				launchDelegateActivity(CameraListActivity.class.getName());
			}else if(curStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_ERR)){
				if(iErrCode == BeseyeError.E_WEBSOCKET_CONN_NOT_EXIST || iErrCode == BeseyeError.E_WEBSOCKET_OPERATION_FAIL){
					showMyDialog(DIALOG_ID_OTA_WS_DISCONN);
				}else if(iErrCode == BeseyeError.E_OTA_SW_ALRADY_LATEST){
					launchDelegateActivity(CameraListActivity.class.getName());
				}
			}
		}
	}

	@Override
	public void onCamUpdateProgress(String strVcamId, int iPercetage) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_OTA_WS_DISCONN:{
				BaseOneBtnDialog d = new BaseOneBtnDialog(this);
				d.setBodyText(getString(R.string.desc_dialog_cam_offline_during_ota));
				d.setTitleText(getString(R.string.dialog_title_warning));
				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
					@Override
					public void onBtnClick() {
						mbHaveClickOk = true;
						submitCamUpdate();
					}});
				dialog = d;
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		return dialog;
	}
	
	private void submitCamUpdate(){
		removeMyDialog(DIALOG_ID_OTA_WS_DISCONN);	
		BeseyeCamSWVersionMgr.getInstance().registerOnCamUpdateStatusChangedListener(this);
		BeseyeCamSWVersionMgr.getInstance().performCamUpdate(BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL, mStrVCamID), false);
	}
	
	@Override
	protected boolean onCameraOnline(JSONObject msgObj){
		Log.i(TAG, getClass().getSimpleName()+"::onCameraOnline(),  msgObj = "+msgObj);
		if(null != msgObj){
			JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
			if(null != objCus){
				String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
				if(null != mStrVCamID && mStrVCamID.equals(strCamUID)){
					if(!mActivityDestroy && mbHaveClickOk){
						submitCamUpdate();
			    	}
				}
				return true;
			}
		}
		return false;
	}


	@Override
	public void onCamUpdateVerChkStatusChanged(String strVcamId,
			CAM_UPDATE_VER_CHECK_STATUS curStatus,
			CAM_UPDATE_VER_CHECK_STATUS prevStatus,
			CamSwUpdateRecord objUpdateRec) {
		// TODO Auto-generated method stub
		
	}
}
