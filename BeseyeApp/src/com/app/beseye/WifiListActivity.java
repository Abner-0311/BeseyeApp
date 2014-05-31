package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import com.app.beseye.adapter.CameraListAdapter;
import com.app.beseye.adapter.WifiInfoAdapter;
import com.app.beseye.adapter.WifiInfoAdapter.WifoInfoHolder;
import com.app.beseye.delegator.WifiAPSetupDelegator;
import com.app.beseye.delegator.WifiAPSetupDelegator.OnWifiApSetupCallback;
import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_ERROR;
import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_STATE;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
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
							  implements OnWifiApSetupCallback,
							  			 OnSwitchBtnStateChangedListener{
	
	private PullToRefreshListView mlvWifiList;
	private WifiInfoAdapter mWifiInfoAdapter;
	private WifiAPSetupDelegator mWifiAPSetupDelegator = null;
	private View mSwWifi;
	private ActionBar.LayoutParams mSwWifiViewLayoutParams;
	private BeseyeSwitchBtn mWifiSwitchBtn;
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	static public final String KEY_CHANGE_WIFI_ONLY = "KEY_CHANGE_WIFI_ONLY";
	static private final String KEY_MAYBE_WRONG_PW = "KEY_MAYBE_WRONG_PW";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "WifiListActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		//getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.wifisetup_wifi_title_bg));
		getSupportActionBar().setDisplayOptions(0);
		//getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE); 
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
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
//		mSwWifi = getLayoutInflater().inflate(R.layout.layout_wifi_switcher, null);
//		mWifiSwitchBtn = (BeseyeSwitchBtn)mSwWifi.findViewById(R.id.sb_wifi_btn);
//		if(null != mWifiSwitchBtn){
//			mWifiSwitchBtn.setOnSwitchBtnStateChangedListener(this);
//		}
//		mSwWifiViewLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
//		mSwWifiViewLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
//        getSupportActionBar().setCustomView(mSwWifi, mSwWifiViewLayoutParams);
		
		mlvWifiList = (PullToRefreshListView) findViewById(R.id.lst_wifi_list);
		
		if(null != mlvWifiList){
			mlvWifiList.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				Log.i(TAG, "onRefresh()");	
    				//monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(WifiListActivity.this), true);
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
			}
		}

	}
	
    @Override
	protected void onResume() {
    	Log.d(TAG, "WifiListActivity::onResume()");
		super.onResume();
		updateUIByWifiStatus(NetworkMgr.getInstance().getWifiStatus());
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
				
				OnClickListener keyIdxClick = new OnClickListener(){
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
					vgKeyIdx[idx].setOnClickListener(keyIdxClick);
				}
			}
		}
		return viewRet;
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.menu_wifilist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if(item.getItemId() == R.id.menu_wifi_scan){
//        	scanWifi(true);
//        }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_wifi_list;
	}

	@Override
	public void onClick(View view) {
		WifoInfoHolder info = (WifoInfoHolder)view.getTag();
		if(null != info){
			mChosenWifiAPInfo = (WifiAPInfo)info.mUserData;
			
//			if(mChosenWifiAPInfo.SSID.equals("Tenda1")){
//				BeseyeHttpTask task = new BeseyeHttpTask();
//				task.execute();
//			}else
				showMyDialog(DIALOG_ID_WIFI_AP_INFO);
		}
	}
	
	protected void updateUIByWifiStatus(int iWifiState){
		super.updateUIByWifiStatus(iWifiState);
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
					if(getIntent().getBooleanExtra(KEY_CHANGE_WIFI_ONLY, false)){
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
		}
//		else if(REDDOT_DEMO && curState.equals(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTED)){
//			setResult(RESULT_OK);
//			CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_ON);
//			finish();
//		}
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
}
