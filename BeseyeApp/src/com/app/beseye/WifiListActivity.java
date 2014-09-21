package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.IRCUT_STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.LED_STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.MIC_GAIN;
import static com.app.beseye.util.BeseyeJSONUtil.MIC_STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.VIDEO_RES;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONInt;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.adapter.CameraListAdapter;
import com.app.beseye.adapter.WifiInfoAdapter;
import com.app.beseye.adapter.WifiInfoAdapter.WifoInfoHolder;
import com.app.beseye.delegator.WifiAPSetupDelegator;
import com.app.beseye.delegator.WifiAPSetupDelegator.OnWifiApSetupCallback;
import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_ERROR;
import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_STATE;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeJSONUtil.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class WifiListActivity extends WifiControlBaseActivity 
							  implements OnSwitchBtnStateChangedListener{
	
	private PullToRefreshListView mlvWifiList;
	private WifiInfoAdapter mWifiInfoAdapter;
	private View mSwWifi;
	private ActionBar.LayoutParams mSwWifiViewLayoutParams;
	private BeseyeSwitchBtn mWifiSwitchBtn;
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	static private final String KEY_MAYBE_WRONG_PW = "KEY_MAYBE_WRONG_PW";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "WifiListActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		if(mbChangeWifi){
			try {
				mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
				if(null != mCam_obj){
					mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				}
			} catch (JSONException e1) {
				Log.e(TAG, "WifiListActivity::onCreate(), failed to parse, e1:"+e1.toString());
			}
		}
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_wifilist_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			mWifiSwitchBtn = (BeseyeSwitchBtn)mVwNavBar.findViewById(R.id.sb_wifi_btn);
			if(null != mWifiSwitchBtn){
				mWifiSwitchBtn.setOnSwitchBtnStateChangedListener(this);
				if(mbChangeWifi){
					mWifiSwitchBtn.setVisibility(View.GONE);
				}
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		mlvWifiList = (PullToRefreshListView) findViewById(R.id.lst_wifi_list);
		
		if(null != mlvWifiList){
			mlvWifiList.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				Log.i(TAG, "onRefresh()");	
    				if(mbChangeWifi){
    					loadWifiSSIDListFromCam();
    				}else
    					scanWifi(true);
    			}

				@Override
				public void onRefreshCancel() {

				}
    		});
			
			mlvWifiList.setMode(LvExtendedMode.PULL_DOWN_TO_REFRESH);
        	
			mWifiInfoAdapter = new WifiInfoAdapter(this, mlstScanResult, R.layout.wifi_list_item, this);
			if(null != mlvWifiList){
				mlvWifiList.setAdapter(mWifiInfoAdapter);
				if(mbChangeWifi){
					mWifiInfoAdapter.setIsCamWifiList(true);
				}
			}
		}

	}
	
	@Override
	protected boolean onCameraOffline(JSONObject msgObj){
    	Log.i(TAG, getClass().getSimpleName()+"::onCameraOffline(),  msgObj = "+msgObj);
		if(mbChangeWifi && null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != mStrVCamID && mStrVCamID.equals(strCamUID)){
    				finish();
    				return true;
    			}
    		}
		}
		return false;
	 }
	
    @Override
	protected void onResume() {
    	Log.d(TAG, "WifiListActivity::onResume()");
		super.onResume();
		updateUIByWifiStatus(NetworkMgr.getInstance().getWifiStatus());
	}
    
    @Override
	protected void onSessionComplete() {
		super.onSessionComplete();
		if(false == mbChangeWifi)
			monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "WifiListActivity::onCreateDialog()");
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WIFI_AP_KEYINDEX:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createKeyIdxDialog());
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							removeMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
						}});
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							
						}});
				}
            	break;
			}
			case DIALOG_ID_WIFI_AP_SECU_PICKER:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createSecuValDialog());
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							removeMyDialog(DIALOG_ID_WIFI_AP_SECU_PICKER);
						}});
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							
						}});
				}
            	break;
			}

			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WIFI_AP_INCORRECT_PW:{
				dialog = super.onCreateDialog(id, bundle);
//				if(REDDOT_DEMO){
//					mlstScanResult.clear();
//					setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
//				}else 
				if(null != mChosenWifiAPInfo){
					boolean bMaybeWrongPW = bundle.getBoolean(KEY_MAYBE_WRONG_PW);	
					dialog = new Dialog(this);
					dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
					dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
					dialog.setContentView(createWifiAPInfoView(true, bMaybeWrongPW?R.string.dialog_wifi_incorrect_password_wep:R.string.dialog_wifi_incorrect_password));
					
					if(null != dialog){
						dialog.setCanceledOnTouchOutside(true);
						dialog.setOnCancelListener(new OnCancelListener(){
							@Override
							public void onCancel(DialogInterface arg0) {
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}});
						
						dialog.setOnDismissListener(new OnDismissListener(){
							@Override
							public void onDismiss(DialogInterface arg0) {
								mWifiApPassword = null;
							}});
					}
	            	break;
				}
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		
		return dialog;
	}

	private View createKeyIdxDialog(){
		View viewRet = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			viewRet = (View)inflater.inflate(R.layout.wifi_ap_key_index_dialog, null);
			if(null != viewRet){
				ImageView[] ivKeyIdx = new ImageView[NetworkMgr.NUM_WEP_KEY_IDX]; 
				if(null != ivKeyIdx){
					ivKeyIdx[0] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_1_check);
					ivKeyIdx[1] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_2_check);
					ivKeyIdx[2] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_3_check);
					ivKeyIdx[3] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_4_check);
				}
				
				final ViewGroup[] vgKeyIdx = new ViewGroup[NetworkMgr.NUM_WEP_KEY_IDX];  
				if(null != vgKeyIdx){
					vgKeyIdx[0] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_1_holder);
					vgKeyIdx[1] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_2_holder);
					vgKeyIdx[2] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_3_holder);
					vgKeyIdx[3] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_4_holder);
				}
				
				OnClickListener SecuValClick = new OnClickListener(){
					@Override
					public void onClick(View view) {
						removeMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
						for(int idx = 0; idx < NetworkMgr.NUM_WEP_KEY_IDX;idx++){
							if(vgKeyIdx[idx] == view){
								mChosenWifiAPInfo.wepkeyIdx = idx;
								if(null != mtxtKeyIndex){
									mtxtKeyIndex.setText(String.valueOf(mChosenWifiAPInfo.wepkeyIdx+1));
								}
								break;
							}
						}
					}};
				
				for(int idx = 0; idx < NetworkMgr.NUM_WEP_KEY_IDX;idx++){
					ivKeyIdx[idx].setVisibility((idx == (mChosenWifiAPInfo.wepkeyIdx))?View.VISIBLE:View.INVISIBLE);
					vgKeyIdx[idx].setOnClickListener(SecuValClick);
				}
			}
		}
		return viewRet;
	}
	
	private View createSecuValDialog(){
		View viewRet = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			viewRet = (View)inflater.inflate(R.layout.wifi_ap_key_index_dialog, null);
			if(null != viewRet){
				ImageView[] ivSecuIdx = new ImageView[NetworkMgr.NUM_WIFI_SECU_TYPE]; 
				if(null != ivSecuIdx){
					ivSecuIdx[0] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_1_check);
					ivSecuIdx[1] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_2_check);
					ivSecuIdx[2] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_3_check);
					ivSecuIdx[3] = (ImageView)viewRet.findViewById(R.id.iv_key_idx_4_check);
				}
				
				final ViewGroup[] vgSecuIdx = new ViewGroup[NetworkMgr.NUM_WIFI_SECU_TYPE];  
				if(null != vgSecuIdx){
					vgSecuIdx[0] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_1_holder);
					vgSecuIdx[1] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_2_holder);
					vgSecuIdx[2] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_3_holder);
					vgSecuIdx[3] = (ViewGroup)viewRet.findViewById(R.id.vg_key_idx_4_holder);
				}
				
				final TextView[] txtSecuIdx = new TextView[NetworkMgr.NUM_WIFI_SECU_TYPE];  
				if(null != txtSecuIdx){
					txtSecuIdx[0] = (TextView)viewRet.findViewById(R.id.txt_key_idx_1);
					BeseyeUtils.setText(txtSecuIdx[0] , NetworkMgr.translateCipherTypeToDesc(WifiListActivity.this, 0));
					txtSecuIdx[1] = (TextView)viewRet.findViewById(R.id.txt_key_idx_2);
					BeseyeUtils.setText(txtSecuIdx[1] , NetworkMgr.translateCipherTypeToDesc(WifiListActivity.this, 1));
					txtSecuIdx[2] = (TextView)viewRet.findViewById(R.id.txt_key_idx_3);
					BeseyeUtils.setText(txtSecuIdx[2] , NetworkMgr.translateCipherTypeToDesc(WifiListActivity.this, 2));
					txtSecuIdx[3] = (TextView)viewRet.findViewById(R.id.txt_key_idx_4);
					BeseyeUtils.setText(txtSecuIdx[3] , NetworkMgr.translateCipherTypeToDesc(WifiListActivity.this, 3));
				}
				
				OnClickListener SecuValClick = new OnClickListener(){
					@Override
					public void onClick(View view) {
						removeMyDialog(DIALOG_ID_WIFI_AP_SECU_PICKER);
						for(int idx = 0; idx < NetworkMgr.NUM_WIFI_SECU_TYPE;idx++){
							if(vgSecuIdx[idx] == view){
								mChosenWifiAPInfo.iCipherIdx = idx;
								if(null != mtxtSecurityVal){
									mtxtSecurityVal.setText(NetworkMgr.translateCipherTypeToDesc(WifiListActivity.this, idx));
								}
								updateDialogByWifiInfo();
								break;
							}
						}
					}};
				
				for(int idx = 0; idx < NetworkMgr.NUM_WEP_KEY_IDX;idx++){
					ivSecuIdx[idx].setVisibility((idx == mChosenWifiAPInfo.iCipherIdx)?View.VISIBLE:View.INVISIBLE);
					vgSecuIdx[idx].setOnClickListener(SecuValClick);
				}
			}
		}
		return viewRet;
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_wifi_list;
	}

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof WifoInfoHolder){
			WifoInfoHolder info = (WifoInfoHolder)view.getTag();
			if(null != info){
				mChosenWifiAPInfo = (WifiAPInfo)info.mUserData;
				
				if(false ==mChosenWifiAPInfo.bIsOther)
					showMyDialog(DIALOG_ID_WIFI_AP_INFO);
				else
					showMyDialog(DIALOG_ID_WIFI_AP_INFO_ADD);
			}
		}else{
			super.onClick(view);
		}
	}
	
	protected void updateUIByWifiStatus(int iWifiState){
		super.updateUIByWifiStatus(iWifiState);
		if(mbChangeWifi){
			loadWifiSSIDListFromCam();
		}else{
			if(null != mWifiSwitchBtn){
				mWifiSwitchBtn.setEnabled(true);
				if(iWifiState == WifiManager.WIFI_STATE_ENABLED){
					mWifiSwitchBtn.setSwitchState(SwitchState.SWITCH_ON);
				}else if(iWifiState == WifiManager.WIFI_STATE_DISABLED){
					mWifiSwitchBtn.setSwitchState(SwitchState.SWITCH_OFF);
				}else{
					mWifiSwitchBtn.setEnabled(false);
				}
			}
		}
	}
	private BeseyeCamBEHttpTask.GetWiFiSSIDListTask mGetWiFiSSIDListTask;
	
	private void loadWifiSSIDListFromCam(){
		if(null == mGetWiFiSSIDListTask)
			monitorAsyncTask(mGetWiFiSSIDListTask = new BeseyeCamBEHttpTask.GetWiFiSSIDListTask(this), true, mStrVCamID);
	}
	
	protected void onWiFiScanComplete(){
		refreshListView();	
	}
	
	private void refreshListView(){
		if(mlvWifiList != null)
			mlvWifiList.onRefreshComplete();
		
		if(null != mWifiInfoAdapter)
			mWifiInfoAdapter.notifyDataSetChanged();
	}
	
	protected void setWifiSettingState(WIFI_SETTING_STATE state){
		Log.i(TAG, "WifiListActivity::setWifiSettingState(), state:"+state);
		WIFI_SETTING_STATE prevState = mWifiSettingState;
		//mWifiSettingState = state;
		switch(state){
			case STATE_WIFI_AP_PICKING:{
				break;
			}
			case STATE_WIFI_AP_SETTING:{
				showMyDialog(DIALOG_ID_WIFI_SETTING);
				break;
			}
			case STATE_WIFI_AP_SET_DONE:{
				if(prevState == WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING){
					removeMyDialog(DIALOG_ID_WIFI_SETTING);
					if(getIntent().getBooleanExtra(WifiControlBaseActivity.KEY_CHANGE_WIFI_ONLY, false)){
						
						finish();
					}else{
						Intent intent = new Intent();
						intent.setClass(this, CameraViewActivity.class);
						startActivity(intent);
					}
					setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCAN_DONE);
					mWifiAPSetupDelegator = null;
				}else{
					Log.i(TAG, "setWifiSettingState(), invalid prestate "+prevState);
				}
				
				break;
			}
			default:
				super.setWifiSettingState(state);
		}
	}

	@Override
	public void onWifiApSetupStateChanged(WIFI_AP_SETUP_STATE curState, WIFI_AP_SETUP_STATE prevState) {
		Log.i(TAG, "onWifiApSetupStateChanged(), curState:"+curState+", prevState:"+prevState);
		
		if(curState.equals(WIFI_AP_SETUP_STATE.SETUP_DONE)){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SET_DONE);
		}else if(curState.equals(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTED)){
			mChosenWifiAPInfo.BSSID = NetworkMgr.getInstance().getActiveWifiBSSID();
			Intent intent = new Intent();
			intent.putExtra(SoundPairingActivity.KEY_WIFI_INFO, mChosenWifiAPInfo);
			intent.setClass(this, SoundPairingActivity.class);
			intent.putExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_CNT, miOriginalVcamCnt);
			intent.putExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_ARR, miOriginalVcamArr);
			startActivity(intent);
			setResult(RESULT_OK);		
		}
	}

	@Override
	public void onWifiApSetupError(WIFI_AP_SETUP_STATE curState, WIFI_AP_SETUP_ERROR error, Object userData) {
		Log.i(TAG, "onWifiApSetupError(), error:"+error);
		removeMyDialog(DIALOG_ID_WIFI_SETTING);
		switch(error){
			case ERROR_WIFI_DISABLED:{
				NetworkMgr.getInstance().turnOffWifi();
				break;
			}
			case ERROR_TARGET_AP_NOT_FOUND:
			case ERROR_TARGET_AP_INVALID:{
				WifiAPInfo info = (WifiAPInfo)userData;
				Bundle b = new Bundle();
				b.putString(KEY_WARNING_TEXT, String.format(getResources().getString(R.string.dialog_wifi_fail_to_connect), null!= info?info.SSID:""));
				showMyDialog(DIALOG_ID_WARNING, b);
				break;
			}
			case ERROR_TARGET_AP_CONNECT_ISSUE:
			case ERROR_TARGET_AP_MAYBE_PW_ERROR:{
				Bundle b = new Bundle();
				b.putBoolean(KEY_MAYBE_WRONG_PW, true);
				showMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW, b);
				break;
			}
			case ERROR_TARGET_AP_PW_ERROR:{
				Bundle b = new Bundle();
				b.putBoolean(KEY_MAYBE_WRONG_PW, false);
				showMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW, b);
				break;
			}
			
			case ERROR_TARGET_AP_CONNECTIVITY_ERROR:
			case ERROR_RELAY_AP_CONNECTIVITY_ERROR:
			case ERROR_RELAY_AP_SETUP_FAILED:
			case ERROR_RELAY_AP_CONNECT_ISSUE:{
				WifiAPInfo info = (WifiAPInfo)userData;
				Bundle b = new Bundle();
				b.putString(KEY_WARNING_TEXT, String.format(getResources().getString(R.string.dialog_wifi_invalid_connectivity), null!= info?info.SSID:""));
				showMyDialog(DIALOG_ID_WARNING, b);
				
				break;
			}
			case ERROR_RELAY_AP_NOT_FOUND:
			case ERROR_RELAY_AP_INVALID:{
				Bundle b = new Bundle();
				b.putString(KEY_WARNING_TEXT, String.format(getResources().getString(R.string.dialog_wifi_fail_to_connect), RELAY_AP_SSID));
				showMyDialog(DIALOG_ID_WARNING, b);
				break;
			}
			default:{
				Log.w(TAG, "onWifiApSetupError(), unhandled error:"+error);
			}
		}
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		if(state.equals(SwitchState.SWITCH_OFF)){
			NetworkMgr.getInstance().turnOffWifi();
		}else if(state.equals(SwitchState.SWITCH_ON)){
			turnOnWifi();
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeCamBEHttpTask.GetWiFiSSIDListTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_get_wifi_list));
					b.putBoolean(KEY_WARNING_CLOSE, true);
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
			
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.GetWiFiSSIDListTask){
				if(0 == iRetCode){
					Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject obj = result.get(0);
					if(null != obj){
						JSONObject dataObj = BeseyeJSONUtil.getJSONObject(obj, ACC_DATA);
						if(null != dataObj){
							Log.i(TAG, "onPostExecute(), "+dataObj.toString());
							//JSONArray iLEDStatus = getJSONInt(dataObj, LED_STATUS, 0);
							JSONArray ssidList = BeseyeJSONUtil.getJSONArray(dataObj, BeseyeJSONUtil.WIFI_SSIDLST);
							NetworkMgr.getInstance().filterWifiAPInfo(mlstScanResult, ssidList, BeseyeJSONUtil.getJSONString(dataObj, BeseyeJSONUtil.WIFI_SSIDLST_USED));
							onWiFiScanComplete();
						}
					}
				}
			}else if(task instanceof BeseyeAccountTask.GetVCamListTask){
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
						miOriginalVcamArr = arrCamList.toString();
						miOriginalVcamCnt = arrCamList.length();
					}
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", miOriginalVcamCnt="+miOriginalVcamCnt);
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
		
		if(task == mGetWiFiSSIDListTask){
			mGetWiFiSSIDListTask = null;
		}
	}
}
