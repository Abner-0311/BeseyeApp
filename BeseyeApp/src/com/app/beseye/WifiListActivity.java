package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.util.ArrayList;
import java.util.List;

import com.app.beseye.adapter.WifiInfoAdapter;
import com.app.beseye.adapter.WifiInfoAdapter.WifoInfoHolder;
import com.app.beseye.delegator.WifiAPSetupDelegator;
import com.app.beseye.delegator.WifiAPSetupDelegator.OnWifiApSetupCallback;
import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_ERROR;
import com.app.beseye.delegator.WifiAPSetupDelegator.WIFI_AP_SETUP_STATE;
import com.app.beseye.setting.CamSettingMgr;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.util.NetworkMgr.OnWifiScanResultAvailableCallback;
import com.app.beseye.util.NetworkMgr.OnWifiStatusChangeCallback;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;

import android.app.Dialog;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class WifiListActivity extends BeseyeBaseActivity 
							  implements OnClickListener, 
							  			 OnWifiScanResultAvailableCallback, 
							  			 OnNetworkChangeCallback, 
							  			 OnWifiStatusChangeCallback,
							  			 OnWifiApSetupCallback,
							  			 OnSwitchBtnStateChangedListener{
	private ListView mlvWifiList;
	private WifiInfoAdapter mWifiInfoAdapter;
	private List<WifiAPInfo> mlstScanResult;
	private WIFI_SETTING_STATE mWifiSettingState = WIFI_SETTING_STATE.STATE_UNINIT; 
	private WifiAPSetupDelegator mWifiAPSetupDelegator = null;
	private View mSwWifi;
	private ActionBar.LayoutParams mSwWifiViewLayoutParams;
	private BeseyeSwitchBtn mWifiSwitchBtn;
	
	static public final String KEY_CHANGE_WIFI_ONLY = "KEY_CHANGE_WIFI_ONLY";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "WifiListActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.wifisetup_wifi_title_bg));
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE, ActionBar.DISPLAY_SHOW_TITLE); 
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		
		mSwWifi = getLayoutInflater().inflate(R.layout.layout_wifi_switcher, null);
		mWifiSwitchBtn = (BeseyeSwitchBtn)mSwWifi.findViewById(R.id.sb_wifi_btn);
		if(null != mWifiSwitchBtn){
			mWifiSwitchBtn.setOnSwitchBtnStateChangedListener(this);
		}
		mSwWifiViewLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT);
		mSwWifiViewLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
        getSupportActionBar().setCustomView(mSwWifi, mSwWifiViewLayoutParams);
		
		mlvWifiList = (ListView)findViewById(R.id.lst_wifi_list);
		mlstScanResult = new ArrayList<WifiAPInfo>();
		
		mWifiInfoAdapter = new WifiInfoAdapter(this, mlstScanResult, R.layout.wifi_list_item, this);
		if(null != mlvWifiList){
			mlvWifiList.setAdapter(mWifiInfoAdapter);
		}
		setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
	}
	
    @Override
	protected void onResume() {
    	Log.d(TAG, "WifiListActivity::onResume()");
		super.onResume();
		NetworkMgr.getInstance().registerNetworkChangeCallback(this);
		NetworkMgr.getInstance().registerWifiStatusChangeCallback(this);
		
		if(getWifiSettingState().ordinal() <= WIFI_SETTING_STATE.STATE_WIFI_SCAN_DONE.ordinal()){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
		}
		
		updateWifiBtn(NetworkMgr.getInstance().getWifiStatus());
	}
    
	@Override
	protected void onPause() {
		Log.d(TAG, "WifiListActivity::onPause()");
		if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING))
			cancelScanWifi();
		NetworkMgr.getInstance().unregisterNetworkChangeCallback(this);
		NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "WifiListActivity::onDestroy()");
		super.onDestroy();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Log.d(TAG, "WifiListActivity::onCreateDialog()");
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WIFI_TURN_ON_FAILED:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(getString(R.string.dialog_wifi_fail_on));
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	dialog.dismiss();
				    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
				    }
				});
				
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						//finish();
						dialog.dismiss();
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
					}});
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
            	break;
            }
			case DIALOG_ID_WIFI_SCAN_FAILED:{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setIcon(android.R.drawable.ic_dialog_alert);
            	builder.setTitle(getString(R.string.dialog_title_warning));
            	builder.setMessage(getString(R.string.dialog_wifi_fail_scan));
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int item) {
				    	dialog.dismiss();
				    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
				    }
				});
				builder.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						//finish();
						dialog.dismiss();
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
					}});
				dialog = builder.create();
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
            	break;
            }
			case DIALOG_ID_WIFI_AP_INFO:{
				dialog = new Dialog(this);
				dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
				dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(createWifiAPInfoView(false, 0));
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
					dialog.setOnCancelListener(new OnCancelListener(){
						@Override
						public void onCancel(DialogInterface arg0) {
							removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
						}});
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							mWifiApPassword = null;
						}});
				}
            	break;
			}
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
	
	static private final String KEY_MAYBE_WRONG_PW = "KEY_MAYBE_WRONG_PW";
	
	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_WIFI_AP_INCORRECT_PW:{
				dialog = super.onCreateDialog(id, bundle);
				if(REDDOT_DEMO){
					mlstScanResult.clear();
					setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
				}else if(null != mChosenWifiAPInfo){
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
			case DIALOG_ID_WARNING:{
				dialog = super.onCreateDialog(id, bundle);
				if(REDDOT_DEMO && null != dialog){
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							mlstScanResult.clear();
							setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
						}});
				}
				
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		
		return dialog;
	}

	private TextView mtxtKeyIndex;
	private String mWifiApPassword = null;
	private View createWifiAPInfoView(final boolean bPasswordOnly, int iWrongPWId){
		View vgApInfo = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			vgApInfo = (View)inflater.inflate(R.layout.wifi_ap_info_dialog, null);
			if(null != vgApInfo){
				TextView txtSSID = (TextView)vgApInfo.findViewById(R.id.txt_ap_name);
				if(null != txtSSID){
					txtSSID.setText(mChosenWifiAPInfo.SSID);
				}
				
				ViewGroup vgAPBasicInfo = (ViewGroup)vgApInfo.findViewById(R.id.vg_ap_basic_info_holder);
				if(bPasswordOnly){
					vgAPBasicInfo.setVisibility(View.GONE);
					RelativeLayout vgWrongPW = (RelativeLayout)vgApInfo.findViewById(R.id.vg_incorrect_password_holder);
					if(null != vgWrongPW){
						vgWrongPW.setVisibility(View.VISIBLE);
						TextView txtWrongPW = (TextView)vgWrongPW.findViewById(R.id.txt_incorrect_password);
						if(null != txtWrongPW){
							txtWrongPW.setText(iWrongPWId);
						}
					}
				}else{
					TextView txtSignal = (TextView)vgAPBasicInfo.findViewById(R.id.txt_signal_value);
					if(null != txtSignal){
						txtSignal.setText(NetworkMgr.getInstance().getSignalStrengthTermId(mChosenWifiAPInfo.signalLevel));
					}
					
					TextView txtSecurity = (TextView)vgAPBasicInfo.findViewById(R.id.txt_security_value);
					if(null != txtSecurity){
						txtSecurity.setText((WifiAPInfo.AUTHNICATION_NONE.equals(mChosenWifiAPInfo.cipher))?getResources().getString(R.string.dialog_wifi_ap_security_none):mChosenWifiAPInfo.cipher);
					}
				}
				
				Button btnCancel= (Button)vgApInfo.findViewById(R.id.btn_cancel);
				if(null != btnCancel){
					btnCancel.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							if(bPasswordOnly){
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}else{
								removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
							}
						}});
				}
				
				mtxtKeyIndex = (TextView)vgApInfo.findViewById(R.id.txt_keyindex_value);
				if(null != mtxtKeyIndex){
					WifiConfiguration config = NetworkMgr.getInstance().getWifiConfigurationBySSID(mChosenWifiAPInfo.SSID);
					if(null != config){
						mChosenWifiAPInfo.wepkeyIdx = config.wepTxKeyIndex;
					}else{
						
					}
					mtxtKeyIndex.setText(String.valueOf(mChosenWifiAPInfo.wepkeyIdx+1));
					mtxtKeyIndex.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							showMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
						}});
				}
				
				ImageView ivSpinner = (ImageView)vgApInfo.findViewById(R.id.iv_spinner);
				if(null != ivSpinner){
					ivSpinner.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							showMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
						}});
				}

				final Button btnConnect = (Button)vgApInfo.findViewById(R.id.btn_connect);
				if(null != btnConnect){
					btnConnect.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							if(bPasswordOnly){
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}else{
								removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
							}
							
							mChosenWifiAPInfo.password = mWifiApPassword;
					    	mChosenWifiAPInfo.wepkeyIdx = Integer.parseInt(String.valueOf(mtxtKeyIndex.getText())) -1;
					    	if(null == mWifiAPSetupDelegator){
					    		mWifiAPSetupDelegator = new WifiAPSetupDelegator(mChosenWifiAPInfo, WifiListActivity.this);
					    	}else{
					    		mWifiAPSetupDelegator.updateTargetAPInfo(mChosenWifiAPInfo);
					    	}
					    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING);
						}});
				}

				
				RelativeLayout vgPassord = (RelativeLayout)vgApInfo.findViewById(R.id.vg_password_holder);
				if(null != vgPassord){
					if(WifiAPInfo.AUTHNICATION_NONE.equals(mChosenWifiAPInfo.cipher)){
						mWifiApPassword = "";
						vgPassord.setVisibility(View.GONE);
					}else{
						final int iMinPasswordLength = mChosenWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WPA)?8:13;
						final EditText etPassword = (EditText)vgPassord.findViewById(R.id.et_password_value);
						if(null != etPassword){
							if(DEBUG){
								etPassword.setText(mChosenWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WPA)?"0630BesEye":"0630BesEye123");
							}
							mWifiApPassword = etPassword.getText().toString();
							etPassword.addTextChangedListener(new TextWatcher(){
								@Override
								public void afterTextChanged(Editable editable) {
									btnConnect.setEnabled(editable.length() >= iMinPasswordLength);
									mWifiApPassword = etPassword.getText().toString();
									//password.matches("[0-9A-Fa-f]*")
								}

								@Override
								public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

								@Override
								public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
									
								}});
						}
						
						CheckBox cbShowPW = (CheckBox)vgPassord.findViewById(R.id.cb_show_password);
						if(null != cbShowPW){
							cbShowPW.setOnCheckedChangeListener(new OnCheckedChangeListener(){
								@Override
								public void onCheckedChanged(CompoundButton view,boolean bChecked) {
									if(null != etPassword){
										etPassword.setInputType(InputType.TYPE_CLASS_TEXT|(bChecked?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));
									}
								}});
						}
					}
				}
				
				RelativeLayout vgKeyIndex = (RelativeLayout)vgApInfo.findViewById(R.id.vg_keyindex_holder);
				if(null != vgKeyIndex){
					vgKeyIndex.setVisibility(mChosenWifiAPInfo.cipher.contains(WifiAPInfo.AUTHNICATION_WEP)?View.VISIBLE:View.GONE);
				}
			}
		}
		return vgApInfo;
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
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_wifilist, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.menu_wifi_scan){
        	scanWifi(true);
        }
        return super.onOptionsItemSelected(item);
    }
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_wifi_list;
	}

	private WifiAPInfo mChosenWifiAPInfo;
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

	@Override
	public void onWifiScanResultAvailable() {
		//Toast.makeText(this, "onWifiScanResultAvailable()", Toast.LENGTH_SHORT).show();
		if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING)){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCAN_DONE);
		}
	}

	@Override
	public void onWifiStateChanged(int iWifiState, int iPrevWifiState) {
		Log.i(TAG, "onWifiStateChanged(), iWifiState from "+iPrevWifiState+" to "+iWifiState);
		if(iWifiState == WifiManager.WIFI_STATE_ENABLED){
			if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON))
				setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
		}
		
		updateWifiBtn(iWifiState);
	}
	
	private void updateWifiBtn(int iWifiState){
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
	
	@Override
	public void onWifiNetworkStateChanged(DetailedState iWifiNetworkState, DetailedState iPrevWifiNetworkState) {
		loadWifiAPList();
//		if(iWifiNetworkState == DetailedState.CONNECTED){
//			if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING))
//				setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SET_DONE);
//		}
	}

	@Override
	public void onConnectivityChanged(boolean onConnectivityChanged) {
		Log.i(TAG, "onConnectivityChanged(), onConnectivityChanged "+onConnectivityChanged);
	}
	
	private void loadWifiAPList(){
		Log.i(TAG, "loadWifiAPList()");
		if(null != mlstScanResult){
			NetworkMgr.getInstance().filterWifiAPInfo(mlstScanResult, NetworkMgr.getInstance().getWifiScanList());
			refreshListView();	
		}
	}
	
	private void refreshListView(){
		if(null != mWifiInfoAdapter)
			mWifiInfoAdapter.notifyDataSetChanged();
	}
	
	private boolean scanWifi(boolean bForceShowDialog){
		boolean bRet = false;
		if(bRet = NetworkMgr.getInstance().scanWifiList(this)){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING);
			if(bForceShowDialog || (null != mlstScanResult && 0 == mlstScanResult.size()))
				showMyDialog(DIALOG_ID_WIFI_SCANNING);
		}else{
			showMyDialog(DIALOG_ID_WIFI_SCAN_FAILED);
		}
		
		return bRet;
	}
	
	private boolean cancelScanWifi(){
		return NetworkMgr.getInstance().cancelScanWifiList(this);
	}
	
	private enum WIFI_SETTING_STATE{
		STATE_UNINIT,
		STATE_INIT,
		STATE_WIFI_TURNING_ON,
		STATE_WIFI_ON,
		STATE_WIFI_SCANNING,
		STATE_WIFI_SCAN_DONE,
		STATE_WIFI_AP_PICKING,
		STATE_WIFI_AP_SETTING,
		STATE_WIFI_AP_SET_DONE
	}

	private WIFI_SETTING_STATE getWifiSettingState(){
		return mWifiSettingState;
	}
	
	private boolean inWifiSettingState(WIFI_SETTING_STATE state){
		return mWifiSettingState == state;
	}
	
	private void setWifiSettingState(WIFI_SETTING_STATE state){
		Log.i(TAG, "WifiListActivity::setWifiSettingState(), state:"+state);
		WIFI_SETTING_STATE prevState = mWifiSettingState;
		mWifiSettingState = state;
		switch(mWifiSettingState){
			case STATE_INIT:{
				if(NetworkMgr.getInstance().getWifiStatus() != WifiManager.WIFI_STATE_ENABLED){
					Log.d(TAG, "WifiListActivity::setWifiSettingState(), wifi is not enabled");
					if(NetworkMgr.getInstance().turnOnWifi()){
						//mLoadWifiListRunnable = new LoadWifiListRunnable(this);
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
						showMyDialog(DIALOG_ID_TURN_ON_WIFI);
					}else{
						showMyDialog(DIALOG_ID_WIFI_TURN_ON_FAILED);
					}
				}else{
					setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_ON);
				}
				break;
			}
			case STATE_WIFI_TURNING_ON:{
//				if(null != mLoadWifiListRunnable){
//					removeMyDialog(DIALOG_ID_TURN_ON_WIFI);
//					mLoadWifiListRunnable.run();
//					mLoadWifiListRunnable = null;
//				}
				break;
			}
			case STATE_WIFI_ON:{
				if(NetworkMgr.getInstance().getWifiStatus() == WifiManager.WIFI_STATE_ENABLED){
					removeMyDialog(DIALOG_ID_TURN_ON_WIFI);
					scanWifi(false);
				}else{
					Log.i(TAG, "WifiListActivity::setWifiSettingState(), can't scan due to wifi off");
					setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
				}
				break;
			}
			case STATE_WIFI_SCANNING:{
				break;
			}
			case STATE_WIFI_SCAN_DONE:{
				removeMyDialog(DIALOG_ID_WIFI_SCANNING);
				loadWifiAPList();
				if(REDDOT_DEMO){
					mChosenWifiAPInfo = null;
					for(WifiAPInfo info : mlstScanResult){
						if(null != info && RELAY_AP_SSID.equals(BeseyeUtils.removeDoubleQuote(info.SSID))){
							mChosenWifiAPInfo = info;
							mChosenWifiAPInfo.password = RELAY_AP_PW;
					    	if(null == mWifiAPSetupDelegator){
					    		mWifiAPSetupDelegator = new WifiAPSetupDelegator(mChosenWifiAPInfo, WifiListActivity.this);
					    	}else{
					    		mWifiAPSetupDelegator.updateTargetAPInfo(mChosenWifiAPInfo);
					    	}
					    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING);
					    	break;
						}
					}
					
					if(null == mChosenWifiAPInfo){
						Bundle b = new Bundle();
						b.putString(KEY_WARNING_TEXT, String.format(getResources().getString(R.string.dialog_wifi_fail_to_connect), RELAY_AP_SSID));
						showMyDialog(DIALOG_ID_WARNING, b);
					}
				}
				break;
			}
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
				Log.i(TAG, "setWifiSettingState(), invalid state "+state);
		}
	}
	
	private void turnOnWifi(){
		if(NetworkMgr.getInstance().turnOnWifi()){
			//mLoadWifiListRunnable = new LoadWifiListRunnable(this);
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
			showMyDialog(DIALOG_ID_TURN_ON_WIFI);
		}else{
			showMyDialog(DIALOG_ID_WIFI_TURN_ON_FAILED);
		}
	}

	@Override
	public void onWifiApSetupStateChanged(WIFI_AP_SETUP_STATE curState, WIFI_AP_SETUP_STATE prevState) {
		Log.i(TAG, "onWifiApSetupStateChanged(), curState:"+curState+", prevState:"+prevState);
		
		if(curState.equals(WIFI_AP_SETUP_STATE.SETUP_DONE)){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SET_DONE);
		}else if(REDDOT_DEMO && curState.equals(WIFI_AP_SETUP_STATE.TARGET_AP_CONNECTED)){
			setResult(RESULT_OK);
			CamSettingMgr.getInstance().setCamPowerState(TMP_CAM_ID, CAM_CONN_STATUS.CAM_ON);
			finish();
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
	public void onSwitchBtnStateChanged(SwitchState state) {
		if(state.equals(SwitchState.SWITCH_OFF)){
			NetworkMgr.getInstance().turnOffWifi();
		}else if(state.equals(SwitchState.SWITCH_ON)){
			turnOnWifi();
		}
	}
}
