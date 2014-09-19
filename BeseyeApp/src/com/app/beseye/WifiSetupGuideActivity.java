package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_ERROR;
import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_STATE;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class WifiSetupGuideActivity extends WifiControlBaseActivity {
	protected View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	protected ImageView mIvBack;
	protected TextView mTxtNavTitle;
	private Button mBtnChooseWifiAP, mBtnUseConnected;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_signup_nav, null);
		if(null != mVwNavBar){
			mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			mTxtNavTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != mTxtNavTitle){
				mTxtNavTitle.setText(R.string.signup_title_cam_wifi_settings);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		mBtnChooseWifiAP = (Button)this.findViewById(R.id.button_choose_network);
		if(null != mBtnChooseWifiAP){
			mBtnChooseWifiAP.setOnClickListener(this);
			mBtnChooseWifiAP.setEnabled(false);
		}
		
		mBtnUseConnected = (Button)this.findViewById(R.id.button_wifi_yes);
		if(null != mBtnUseConnected){
			mBtnUseConnected.setOnClickListener(this);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		updateBtnByScanResult();
	}
	
	@Override
	protected void onSessionComplete() {
		super.onSessionComplete();
		monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.iv_nav_left_btn:{
				invokeLogout();
				break;
			}
			case R.id.button_choose_network:{
				Bundle b = new Bundle();
				b.putInt(SoundPairingActivity.KEY_ORIGINAL_VCAM_CNT, miOriginalVcamCnt);
				launchActivityForResultByClassName(WifiListActivity.class.getName(), b, REQ_CODE_PICK_WIFI);
				break;
			}
			case R.id.button_wifi_yes:{
				showMyDialog(DIALOG_ID_WIFI_AP_INFO);				
				break;
			}
			default:
				super.onClick(view);
		}		
	}
	
	static public final int REQ_CODE_PICK_WIFI = 0x1001;
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(REQ_CODE_PICK_WIFI == requestCode){
			if(RESULT_OK == resultCode){
				finish();
			}else{
				setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
			}
		}else
			super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_wifi_setting;
	}
	
	protected void updateUIByWifiStatus(int iWifiState){
		super.updateUIByWifiStatus(iWifiState);
		updateBtnByScanResult();
	}
	
	protected void onWiFiScanComplete(){
		updateBtnByScanResult();
	}

	private void updateBtnByScanResult(){
		boolean bWifiEnabled = NetworkMgr.getInstance().isWifiEnabled();
		BeseyeUtils.setEnabled(mBtnChooseWifiAP, bWifiEnabled);
		
		if(!bWifiEnabled || null == mlstScanResult || 0 == mlstScanResult.size()){
			Log.i(TAG, "updateBtnByScanResult(), no Wifi ap was scanned");	
			BeseyeUtils.setEnabled(mBtnUseConnected, false);
		}else{
			Log.i(TAG, "Scan List:"+mlstScanResult.toString());	
			mChosenWifiAPInfo = null;
			for(WifiAPInfo info : mlstScanResult){
				if(null != info && info.bActiveConn){
					mChosenWifiAPInfo = info;
					Log.i(TAG, "updateBtnByScanResult(), get connected ap:["+mChosenWifiAPInfo.toString()+"]");	
					break;
				}
			}
			BeseyeUtils.setEnabled(mBtnUseConnected, null != mChosenWifiAPInfo);
		}
	}
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result,
			int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					JSONArray arrCamList = new JSONArray();
					int iVcamCnt = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_VCAM_CNT);
					//miOriginalVcamCnt = iVcamCnt;
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", miOriginalVcamCnt="+miOriginalVcamCnt);
					if(0 < iVcamCnt){
						JSONArray VcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_VCAM_LST);
						for(int i = 0;i< iVcamCnt;i++){
							try {
								JSONObject camObj = VcamList.getJSONObject(i);
								if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
									arrCamList.put(camObj);
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						miOriginalVcamCnt = arrCamList.length();
					}
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", miOriginalVcamCnt="+miOriginalVcamCnt);
				}
			}else 
				super.onPostExecute(task, result, iRetCode);
		}
	}

	@Override
	public void onWifiApSetupStateChanged(WIFI_AP_SETUP_STATE curState,
			WIFI_AP_SETUP_STATE prevState) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWifiApSetupError(WIFI_AP_SETUP_STATE curState,
			WIFI_AP_SETUP_ERROR error, Object userData) {
		// TODO Auto-generated method stub
		
	}
}
