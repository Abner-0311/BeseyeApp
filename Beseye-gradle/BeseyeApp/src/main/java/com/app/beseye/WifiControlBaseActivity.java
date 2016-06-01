package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

//import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


import com.app.beseye.delegator.WifiAPSetupDelegator;
import com.app.beseye.delegator.WifiAPSetupDelegator.OnWifiApSetupCallback;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.pairing.PairingRemindActivity;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.util.BeseyeLocationMgr;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.ApManager;
import com.app.beseye.util.NetworkMgr.OnNetworkChangeCallback;
import com.app.beseye.util.NetworkMgr.OnWifiScanResultAvailableCallback;
import com.app.beseye.util.NetworkMgr.OnWifiStatusChangeCallback;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import android.app.Activity;
import android.app.Dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.drawable.ColorDrawable;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.PermissionChecker;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public abstract class WifiControlBaseActivity extends BeseyeBaseActivity 
							  				  implements OnWifiScanResultAvailableCallback, 
							  				  			 OnNetworkChangeCallback, 
							  				  			 OnWifiStatusChangeCallback,
							  				  			 OnWifiApSetupCallback,
														 GoogleApiClient.ConnectionCallbacks,
														 GoogleApiClient.OnConnectionFailedListener{
	
	protected WIFI_SETTING_STATE mWifiSettingState = WIFI_SETTING_STATE.STATE_UNINIT; 
	protected List<WifiAPInfo> mlstScanResult;
	protected TextView mtxtKeyIndex;
	
	protected TextView mtxtSecurityVal;
	protected RelativeLayout mVgPassordHolder;
	protected RelativeLayout mVgKeyIndexHolder;
	protected Button mbtnConnect;
	protected EditText mEtSSID;
	
	protected WifiAPSetupDelegator mWifiAPSetupDelegator = null;
	protected String mWifiApPassword = null;
	protected WifiAPInfo mChosenWifiAPInfo;
	protected boolean mbChangeWifi = false;
	
	//workaround
	protected int miOriginalVcamCnt = -1;
	protected String miOriginalVcamArr = null;
	static public final String KEY_CHANGE_WIFI_ONLY = "KEY_CHANGE_WIFI_ONLY";

	final static protected long MAX_TIME_TO_WAIT_WIFI_RET = 60000L;
	protected long mlBeginScanWiFiTimestamp = -1;
	
	//static private String sWiFiPasswordHistory = "";
	
	static public void updateWiFiPasswordHistory(String strPW){
		if(DEBUG)
			Log.w(TAG, "updateWiFiPasswordHistory(), strPW:"+strPW);
		//sWiFiPasswordHistory = strPW;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG)
			Log.d(TAG, "WifiControlBaseActivity::onCreate()");
		super.onCreate(savedInstanceState);
		
		mbChangeWifi = getIntent().getBooleanExtra(KEY_CHANGE_WIFI_ONLY, false);
		mlstScanResult = new ArrayList<WifiAPInfo>();
//		if(false == mbChangeWifi)
//			setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
		
		miOriginalVcamCnt = getIntent().getIntExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_CNT, 0);
		if(DEBUG)
			Log.i(TAG, "WifiControlBaseActivity::onCreate(), miOriginalVcamCnt=>"+miOriginalVcamCnt);
	}
	
    @Override
	protected void onResume() {
    	if(DEBUG)
    		Log.d(TAG, "WifiControlBaseActivity::onResume()");
		super.onResume();
		if(false == mbChangeWifi){
			NetworkMgr.getInstance().registerNetworkChangeCallback(this);
			NetworkMgr.getInstance().registerWifiStatusChangeCallback(this);
			if(BeseyeLocationMgr.getInstance().isLocationPermissionGranted()){
				if(getWifiSettingState().ordinal() <= WIFI_SETTING_STATE.STATE_WIFI_SCAN_DONE.ordinal()){
					setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
				}
				BeseyeLocationMgr.getInstance().requestGoogleApiClient(this, REQUEST_LOCATION);
			}else {
				BeseyeLocationMgr.getInstance().requestLocationPermission(this, REQUEST_LOCATION_PERM);
			}
		}	
	}
    
	@Override
	protected void onPause() {
		if(DEBUG)
			Log.d(TAG, "WifiControlBaseActivity::onPause()");
		if(false == mbChangeWifi){
			if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING))
				cancelScanWifi();
			NetworkMgr.getInstance().unregisterNetworkChangeCallback(this);
			NetworkMgr.getInstance().unregisterWifiStatusChangeCallback(this);
		}
		super.onPause();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		if(DEBUG)
			Log.d(TAG, "WifiControlBaseActivity::onCreateDialog()");
		Dialog dialog = null;
		switch(id){
			case DIALOG_ID_WIFI_AP_INFO:{
				if(null != mChosenWifiAPInfo){
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
				}
		    	break;
			}
			case DIALOG_ID_WIFI_AP_INFO_ADD:{
				if(null != mChosenWifiAPInfo){
					dialog = new Dialog(this);
					dialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
					dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
					dialog.setContentView(createWifiAPInfoAddView(false, 0));
					
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
				}
				break;
			}
			case DIALOG_ID_WIFI_TURN_ON_FAILED:{
				dialog = new BaseOneBtnDialog(this);
				BaseOneBtnDialog d = (BaseOneBtnDialog)dialog;
				d.setBodyText(getString(R.string.dialog_wifi_fail_on));
				d.setTitleText(getString(R.string.dialog_title_warning));

				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
				
					@Override
					public void onBtnClick() {
						removeMyDialog(DIALOG_ID_WIFI_TURN_ON_FAILED);		
					}});
				
				d.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						//finish();
						dialog.dismiss();
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
					}});
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
				}
            	break;
            }
			case DIALOG_ID_WIFI_SCAN_FAILED:{				
				dialog = new BaseOneBtnDialog(this);
				BaseOneBtnDialog d = (BaseOneBtnDialog)dialog;
				d.setBodyText(getString( R.string.dialog_wifi_fail_scan));
				d.setTitleText(getString(R.string.dialog_title_warning));

				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
				
					@Override
					public void onBtnClick() {
						removeMyDialog(DIALOG_ID_WIFI_SCAN_FAILED);		
					}});
				
				d.setOnCancelListener(new OnCancelListener(){
					@Override
					public void onCancel(DialogInterface dialog) {
						//finish();
						dialog.dismiss();
						setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
					}});
				
				if(null != dialog){
					dialog.setCanceledOnTouchOutside(true);
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
			case DIALOG_ID_WARNING:{
				dialog = super.onCreateDialog(id, bundle);
				if(null != dialog){
					dialog.setOnDismissListener(new OnDismissListener(){
						@Override
						public void onDismiss(DialogInterface arg0) {
							if(false == mbChangeWifi){
								clearScanResult();
								setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
							}
						}});
				}
				
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		
		return dialog;
	}
	
	protected void clearScanResult(){
		if(null != mlstScanResult)
			mlstScanResult.clear();
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
		
		updateUIByWifiStatus(iWifiState);
	}
	
	protected void updateUIByWifiStatus(int iWifiState){}
	
	@Override
	public void onWifiNetworkStateChanged(DetailedState iWifiNetworkState, DetailedState iPrevWifiNetworkState) {
		loadWifiAPList();
		if(false == mbChangeWifi && iWifiNetworkState == DetailedState.DISCONNECTED){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
//			if(inWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING))
//				setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SET_DONE);
		}
	}

	@Override
	public void onConnectivityChanged(boolean onConnectivityChanged) {
		if(DEBUG)
			Log.i(TAG, "onConnectivityChanged(), onConnectivityChanged "+onConnectivityChanged);
	}
	
	protected void loadWifiAPList(){
		if(DEBUG)
			Log.i(TAG, "loadWifiAPList()");
		
		if(null != mlstScanResult){
			NetworkMgr.getInstance().filterWifiAPInfo(mlstScanResult, NetworkMgr.getInstance().getWifiScanList());
			onWiFiScanComplete();
			if(null != mlstScanResult && 1 == mlstScanResult.size()){
				if(this.MAX_TIME_TO_WAIT_WIFI_RET > (System.currentTimeMillis() - mlBeginScanWiFiTimestamp)) {
					BeseyeUtils.postRunnable(new Runnable() {
						@Override
						public void run() {
							scanWifi(false);
						}
					}, 1000);
				}else{
                    onWiFiScanFailed();
				}
			}
		}
	}

	protected void onWiFiScanFailed(){BeseyeLocationMgr.getInstance().requestGoogleApiClient(this, REQUEST_LOCATION);}

	protected void onWiFiScanComplete(){}
	
	protected boolean scanWifi(boolean bForceShowDialog){
		boolean bRet = false;
		if(bRet = NetworkMgr.getInstance().scanWifiList(this)){
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_SCANNING);
			if(bForceShowDialog || (null != mlstScanResult && 1 >= mlstScanResult.size())){
				showMyDialog(DIALOG_ID_WIFI_SCANNING);
			}else{
				removeMyDialog(DIALOG_ID_WIFI_SCANNING);
			}
		}else{
			showMyDialog(DIALOG_ID_WIFI_SCAN_FAILED);
		}
		
		return bRet;
	}
	
	protected boolean cancelScanWifi(){
		return NetworkMgr.getInstance().cancelScanWifiList(this);
	}
	
	protected enum WIFI_SETTING_STATE{
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

	protected WIFI_SETTING_STATE getWifiSettingState(){
		return mWifiSettingState;
	}
	
	protected boolean inWifiSettingState(WIFI_SETTING_STATE state){
		return mWifiSettingState == state;
	}
	
	protected void setWifiSettingState(WIFI_SETTING_STATE state){
		if(DEBUG)
			Log.i(TAG, "WifiControlBaseActivity::setWifiSettingState(), state:"+state);
		
		//WIFI_SETTING_STATE prevState = mWifiSettingState;
		mWifiSettingState = state;
		switch(mWifiSettingState){
			case STATE_INIT:{
				if(NetworkMgr.getInstance().getWifiStatus() != WifiManager.WIFI_STATE_ENABLED){
					if(DEBUG)
						Log.d(TAG, "WifiControlBaseActivity::setWifiSettingState(), wifi is not enabled");
					
					if(ApManager.isApOn(this)){
						ApManager.changeApState(this, false);
					}
					
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
				//Avoid blocking on this state
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						if(NetworkMgr.getInstance().getWifiStatus() != WifiManager.WIFI_STATE_ENABLED){
							setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
						}
					}}, 10000);
				break;
			}
			case STATE_WIFI_ON:{
				if(NetworkMgr.getInstance().getWifiStatus() == WifiManager.WIFI_STATE_ENABLED){
					removeMyDialog(DIALOG_ID_TURN_ON_WIFI);
					scanWifi(false);
					mlBeginScanWiFiTimestamp = System.currentTimeMillis();
				}else{
					if(DEBUG)
						Log.i(TAG, "WifiControlBaseActivity::setWifiSettingState(), can't scan due to wifi off");
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
//				if(REDDOT_DEMO){
//					mChosenWifiAPInfo = null;
//					for(WifiAPInfo info : mlstScanResult){
//						if(null != info && RELAY_AP_SSID.equals(BeseyeUtils.removeDoubleQuote(info.SSID))){
//							mChosenWifiAPInfo = info;
//							mChosenWifiAPInfo.password = RELAY_AP_PW;
//					    	if(null == mWifiAPSetupDelegator){
//					    		mWifiAPSetupDelegator = new WifiAPSetupDelegator(mChosenWifiAPInfo, WifiControlBaseActivity.this);
//					    	}else{
//					    		mWifiAPSetupDelegator.updateTargetAPInfo(mChosenWifiAPInfo);
//					    	}
//					    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING);
//					    	break;
//						}
//					}
//					
//					if(null == mChosenWifiAPInfo){
//						Bundle b = new Bundle();
//						b.putString(KEY_WARNING_TEXT, String.format(getResources().getString(R.string.dialog_wifi_fail_to_connect), RELAY_AP_SSID));
//						showMyDialog(DIALOG_ID_WARNING, b);
//					}
//				}
				break;
			}
			default:
				Log.i(TAG, "setWifiSettingState(), invalid state "+state);
		}
	}
	
	protected void turnOnWifi(){
		if(ApManager.isApOn(this)){
			ApManager.changeApState(this, false);
		}
		
		if(NetworkMgr.getInstance().turnOnWifi()){
			//mLoadWifiListRunnable = new LoadWifiListRunnable(this);
			setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_TURNING_ON);
			showMyDialog(DIALOG_ID_TURN_ON_WIFI);
		}else{
			showMyDialog(DIALOG_ID_WIFI_TURN_ON_FAILED);
		}
	}
	
	private boolean isValidWiFiPWLength(int iSecType, int iPwLen){
		boolean bRet = false;
		if(iSecType == WifiAPInfo.AUTHNICATION_KEY_NONE){
			bRet = true;
		}else if(iSecType == WifiAPInfo.AUTHNICATION_KEY_WEP){
			bRet = (5 == iPwLen) || (13 == iPwLen) || (10 == iPwLen) || (26 == iPwLen);
		}else if(iSecType == WifiAPInfo.AUTHNICATION_KEY_WPA || iSecType == WifiAPInfo.AUTHNICATION_KEY_WPA2){
			bRet = (8 <= iPwLen) && (iPwLen <= 63);
		}
		return bRet;
	}
	
	protected View createWifiAPInfoView(final boolean bPasswordOnly, int iWrongPWId){
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
						txtSecurity.setText((mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_NONE)?getResources().getString(R.string.dialog_wifi_ap_security_none):mChosenWifiAPInfo.getCipher());
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
				
//				mtxtKeyIndex = (TextView)vgApInfo.findViewById(R.id.txt_keyindex_value);
//				ImageView ivSpinner = (ImageView)vgApInfo.findViewById(R.id.iv_spinner);
//				if(null != ivSpinner){
//					ivSpinner.setOnClickListener(new OnClickListener(){
//						@Override
//						public void onClick(View arg0) {
//							showMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
//						}});
//				}

				final Button btnConnect = (Button)vgApInfo.findViewById(R.id.btn_connect);
				if(null != btnConnect){
					if(getIntent().getBooleanExtra(WifiControlBaseActivity.KEY_CHANGE_WIFI_ONLY, false)){
						btnConnect.setText(R.string.select);
					}
					
					btnConnect.setEnabled((null != mChosenWifiAPInfo && WifiAPInfo.AUTHNICATION_KEY_NONE == mChosenWifiAPInfo.iCipherIdx));
					
					btnConnect.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							if(bPasswordOnly){
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}else{
								removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
							}
							
//							if(mChosenWifiAPInfo.iCipherIdx != WifiAPInfo.AUTHNICATION_KEY_NONE && (null ==mWifiApPassword || 0 == mWifiApPassword.length())){
//								
//							}
							mChosenWifiAPInfo.password = mWifiApPassword;
//							if(WifiAPInfo.AUTHNICATION_KEY_WEP == mChosenWifiAPInfo.iCipherIdx && (10 == mChosenWifiAPInfo.password.length() || 26 == mChosenWifiAPInfo.password.length())){
//								byte[] utf8;
//								try {
//									utf8 = BeseyeUtils.hexToASCII(mChosenWifiAPInfo.password.toLowerCase()).getBytes("UTF-8");
//									mChosenWifiAPInfo.password = new String(utf8, "UTF-8");
//								} catch (UnsupportedEncodingException e) {
//									// TODO Auto-generated catch block
//									e.printStackTrace();
//								}
//								
//								Log.i(TAG, "WifiControlBaseActivity::onClick(), mChosenWifiAPInfo.password=["+mChosenWifiAPInfo.password+"]"+mChosenWifiAPInfo.password.getBytes().length);
//								Log.i(TAG, "WifiControlBaseActivity::onClick(),"+mChosenWifiAPInfo.password.getBytes()[0]);
//
//							}
							
					    	//mChosenWifiAPInfo.wepkeyIdx = Integer.parseInt(String.valueOf(mtxtKeyIndex.getText())) -1;
					    	
							Intent intent = new Intent();
							
							if(getIntent().getBooleanExtra(WifiControlBaseActivity.KEY_CHANGE_WIFI_ONLY, false)){
								intent.putExtra(SoundPairingActivity.KEY_WIFI_INFO, mChosenWifiAPInfo);
								setResult(RESULT_OK, intent);
								finish();
							}else{
								//intent.setClass(WifiControlBaseActivity.this, SoundPairingActivity.class);
								intent.setClass(WifiControlBaseActivity.this, PairingRemindActivity.class);
								intent.putExtras(getIntent().getExtras());
								intent.putExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_CNT, miOriginalVcamCnt);
								intent.putExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_ARR, miOriginalVcamArr);
								intent.putExtra(SoundPairingActivity.KEY_WIFI_INFO, mChosenWifiAPInfo);
								
								startActivity(intent);
								setResult(RESULT_OK);
								
								if(bPasswordOnly){
									removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
								}else{
									removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
								}							}
							if(DEBUG)
								Log.i(TAG, "WifiControlBaseActivity::onClick(), miOriginalVcamCnt=>"+miOriginalVcamCnt);
						}});
				}

				
				RelativeLayout vgPassord = (RelativeLayout)vgApInfo.findViewById(R.id.vg_password_holder);
				if(null != vgPassord){
					if(mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_NONE){
						mWifiApPassword = "";
						vgPassord.setVisibility(View.GONE);
					}else{
						final int iMinPasswordLength = 0;//(mChosenWifiAPInfo.iCipherIdx > WifiAPInfo.AUTHNICATION_KEY_WEP)?8:13;
						final EditText etPassword = (EditText)vgPassord.findViewById(R.id.et_password_value);
						if(null != etPassword){
//							if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal()){
//								etPassword.setText(mChosenWifiAPInfo.iCipherIdx > WifiAPInfo.AUTHNICATION_KEY_WEP?(mChosenWifiAPInfo.SSID.equals("beseye")?"":"12345678"):"");
//								btnConnect.setEnabled(true);
//							}else 
//							if(false == getIntent().getBooleanExtra(WifiControlBaseActivity.KEY_CHANGE_WIFI_ONLY, false) && null != sWiFiPasswordHistory && 0 < sWiFiPasswordHistory.length()){
//								etPassword.setText(sWiFiPasswordHistory);
//								btnConnect.setEnabled(sWiFiPasswordHistory.length() >=iMinPasswordLength);
//							}
							
							mWifiApPassword = etPassword.getText().toString();
							etPassword.addTextChangedListener(new TextWatcher(){
								@Override
								public void afterTextChanged(Editable editable) {
									btnConnect.setEnabled(isValidWiFiPWLength(mChosenWifiAPInfo.iCipherIdx, editable.length())/*editable.length() >= iMinPasswordLength*/);
									mWifiApPassword = etPassword.getText().toString();
									//Log.i(TAG, "afterTextChanged(), mWifiApPassword=>"+mWifiApPassword);

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
				
//				RelativeLayout vgKeyIndex = (RelativeLayout)vgApInfo.findViewById(R.id.vg_keyindex_holder);
//				if(null != vgKeyIndex){
//					vgKeyIndex.setVisibility(mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_WEP?View.VISIBLE:View.GONE);
//				}
			}
		}
		return vgApInfo;
	}
	
	protected View createWifiAPInfoAddView(final boolean bPasswordOnly, int iWrongPWId){
		View vgApInfo = null;
		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			vgApInfo = (View)inflater.inflate(R.layout.wifi_ap_add_dialog, null);
			if(null != vgApInfo){
				final int iMinPasswordLength = (mChosenWifiAPInfo.iCipherIdx > WifiAPInfo.AUTHNICATION_KEY_WEP)?8:(mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_WEP)?13:0;
				Button btnCancel= (Button)vgApInfo.findViewById(R.id.btn_cancel);
				if(null != btnCancel){
					btnCancel.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							if(bPasswordOnly){
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}else{
								removeMyDialog(DIALOG_ID_WIFI_AP_INFO_ADD);
							}
						}});
				}

				mbtnConnect = (Button)vgApInfo.findViewById(R.id.btn_connect);
				if(null != mbtnConnect){
					if(getIntent().getBooleanExtra(WifiControlBaseActivity.KEY_CHANGE_WIFI_ONLY, false)){
						mbtnConnect.setText(R.string.select);
					}
					
					mbtnConnect.setEnabled(false);
					
					mbtnConnect.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							if(bPasswordOnly){
								removeMyDialog(DIALOG_ID_WIFI_AP_INCORRECT_PW);
							}else{
								removeMyDialog(DIALOG_ID_WIFI_AP_INFO);
							}
							mChosenWifiAPInfo.SSID = mEtSSID.getText().toString();
							mChosenWifiAPInfo.password = mWifiApPassword;
//							if(WifiAPInfo.AUTHNICATION_KEY_WEP == mChosenWifiAPInfo.iCipherIdx && (10 == mChosenWifiAPInfo.password.length() || 26 == mChosenWifiAPInfo.password.length())){
//								mChosenWifiAPInfo.password = BeseyeUtils.hexToASCII(mChosenWifiAPInfo.password.toLowerCase());
//							}
					    	//mChosenWifiAPInfo.wepkeyIdx = Integer.parseInt(String.valueOf(mtxtKeyIndex.getText())) -1;
					    	
							Intent intent = new Intent();
							
							intent.putExtra(SoundPairingActivity.KEY_WIFI_INFO, mChosenWifiAPInfo);
							if(getIntent().getBooleanExtra(WifiControlBaseActivity.KEY_CHANGE_WIFI_ONLY, false)){
								setResult(RESULT_OK, intent);
								finish();
							}else{
								
//						    	if(null == mWifiAPSetupDelegator){
//						    		mWifiAPSetupDelegator = new WifiAPSetupDelegator(mChosenWifiAPInfo, WifiControlBaseActivity.this);
//						    	}else{
//						    		mWifiAPSetupDelegator.updateTargetAPInfo(mChosenWifiAPInfo);
//						    	}
//						    	setWifiSettingState(WIFI_SETTING_STATE.STATE_WIFI_AP_SETTING);
								
								//intent.setClass(WifiControlBaseActivity.this, SoundPairingActivity.class);
								intent.setClass(WifiControlBaseActivity.this, PairingRemindActivity.class);
								intent.putExtra(SoundPairingActivity.KEY_CHANGE_WIFI_BEBEBE, getIntent().getBooleanExtra(SoundPairingActivity.KEY_CHANGE_WIFI_BEBEBE, false));
								intent.putExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_CNT, miOriginalVcamCnt);
								intent.putExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_ARR, miOriginalVcamArr);
								startActivity(intent);
								setResult(RESULT_OK);	
							}
							if(DEBUG)
								Log.i(TAG, "WifiControlBaseActivity::onClick(), miOriginalVcamCnt=>"+miOriginalVcamCnt);
						}});
				}
				
				mEtSSID = (EditText)vgApInfo.findViewById(R.id.et_ssid_value);
				if(null != mEtSSID){
					mEtSSID.setText(mChosenWifiAPInfo.SSID);
					if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal()){
						mEtSSID.setText("beseye2G");
					}
					mEtSSID.addTextChangedListener(new TextWatcher(){
						@Override
						public void afterTextChanged(Editable editable) {
							mbtnConnect.setEnabled(mEtSSID.length() > 0 && (null != mWifiApPassword && isValidWiFiPWLength(mChosenWifiAPInfo.iCipherIdx, mWifiApPassword.length())/*mWifiApPassword.length() >= iMinPasswordLength*/));
						}

						@Override
						public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

						@Override
						public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
							
						}});
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
						txtSecurity.setText(mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_NONE?getResources().getString(R.string.dialog_wifi_ap_security_none):mChosenWifiAPInfo.getCipher());
					}
				}

//				mtxtKeyIndex = (TextView)vgApInfo.findViewById(R.id.txt_keyindex_value);				
//				ImageView ivSpinner = (ImageView)vgApInfo.findViewById(R.id.iv_spinner);
//				if(null != ivSpinner){
//					ivSpinner.setOnClickListener(new OnClickListener(){
//						@Override
//						public void onClick(View arg0) {
//							showMyDialog(DIALOG_ID_WIFI_AP_KEYINDEX);
//						}});
//				}
				
				mtxtSecurityVal = (TextView)vgApInfo.findViewById(R.id.txt_security_value);
				ImageView ivSecuSpinner = (ImageView)vgApInfo.findViewById(R.id.iv_secu_spinner);
				if(null != ivSecuSpinner){
					ivSecuSpinner.setOnClickListener(new OnClickListener(){
						@Override
						public void onClick(View arg0) {
							showMyDialog(DIALOG_ID_WIFI_AP_SECU_PICKER);
						}});
				}
				
				mVgPassordHolder = (RelativeLayout)vgApInfo.findViewById(R.id.vg_password_holder);
				if(null != mVgPassordHolder){
					final EditText etPassword = (EditText)mVgPassordHolder.findViewById(R.id.et_password_value);
					if(null != etPassword){
						if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal()){
							etPassword.setText(mChosenWifiAPInfo.iCipherIdx > WifiAPInfo.AUTHNICATION_KEY_WEP?(mChosenWifiAPInfo.SSID.equals("beseye")?"":"asdfghjkl!@#$%^7*()"):"");
						}
						mWifiApPassword = etPassword.getText().toString();
						etPassword.addTextChangedListener(new TextWatcher(){
							@Override
							public void afterTextChanged(Editable editable) {
								mbtnConnect.setEnabled(mEtSSID.length() > 0 && isValidWiFiPWLength(mChosenWifiAPInfo.iCipherIdx, editable.length())/*editable.length() >= iMinPasswordLength*/);
								mWifiApPassword = etPassword.getText().toString();
								//password.matches("[0-9A-Fa-f]*")
							}

							@Override
							public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

							@Override
							public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
								
							}});
					}
					
					CheckBox cbShowPW = (CheckBox)mVgPassordHolder.findViewById(R.id.cb_show_password);
					if(null != cbShowPW){
						cbShowPW.setOnCheckedChangeListener(new OnCheckedChangeListener(){
							@Override
							public void onCheckedChanged(CompoundButton view,boolean bChecked) {
								if(null != etPassword){
									etPassword.setInputType(InputType.TYPE_CLASS_TEXT|(bChecked?InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD:InputType.TYPE_TEXT_VARIATION_PASSWORD));
								}
							}});
					}
					
					if(mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_NONE){
						mWifiApPassword = "";
						mVgPassordHolder.setVisibility(View.GONE);
					}
				}
//				
//				mVgKeyIndexHolder = (RelativeLayout)vgApInfo.findViewById(R.id.vg_keyindex_holder);
//				if(null != mVgKeyIndexHolder){
//					mVgKeyIndexHolder.setVisibility(View.GONE/*mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_WEP?View.VISIBLE:View.GONE*/);
//				}
			}
		}
		return vgApInfo;
	}
	
	protected void updateDialogByWifiInfo(){
		if(null != mVgPassordHolder){
			mVgPassordHolder.setVisibility(mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_NONE?View.GONE:View.VISIBLE);
			
			final int iMinPasswordLength = (mChosenWifiAPInfo.iCipherIdx > WifiAPInfo.AUTHNICATION_KEY_WEP)?8:(mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_WEP)?13:0;
			
			final EditText etPassword = (EditText)mVgPassordHolder.findViewById(R.id.et_password_value);
			if(null != etPassword){
				
				if(null != mbtnConnect){
					mbtnConnect.setEnabled(mEtSSID.length() > 0 && isValidWiFiPWLength(mChosenWifiAPInfo.iCipherIdx, etPassword.length())/*etPassword.length() >= iMinPasswordLength*/);
				}
				
				if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal()){
					etPassword.setText(mChosenWifiAPInfo.iCipherIdx > WifiAPInfo.AUTHNICATION_KEY_WEP?(mChosenWifiAPInfo.SSID.equals("beseye")?"":"asdfghjkl!@#$%^&*()"):"");
				}
				
				mWifiApPassword = etPassword.getText().toString();
				etPassword.addTextChangedListener(new TextWatcher(){
					@Override
					public void afterTextChanged(Editable editable) {
						mbtnConnect.setEnabled(isValidWiFiPWLength(mChosenWifiAPInfo.iCipherIdx, editable.length())/*editable.length() >= iMinPasswordLength*/);
						mWifiApPassword = etPassword.getText().toString();
						//password.matches("[0-9A-Fa-f]*")
					}

					@Override
					public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

					@Override
					public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
						
					}});
			}
		}
		
		if(null != mVgKeyIndexHolder){
			mVgKeyIndexHolder.setVisibility(View.GONE/*mChosenWifiAPInfo.iCipherIdx == WifiAPInfo.AUTHNICATION_KEY_WEP?View.VISIBLE:View.GONE*/);
		}
	}

	final static int REQUEST_LOCATION_PERM = 124;

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
			case REQUEST_LOCATION_PERM:
				//if (grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED && grantResults[1] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
				if(BeseyeLocationMgr.getInstance().isLocationPermissionGranted()){
					// Permission Granted
					Log.i(TAG, getClass().getSimpleName()+"::onRequestPermissionsResult(),PERMISSION_GRANTED ");
					if(getWifiSettingState().ordinal() <= WIFI_SETTING_STATE.STATE_WIFI_SCAN_DONE.ordinal()){
						setWifiSettingState(WIFI_SETTING_STATE.STATE_INIT);
					}
				} else {
					// Permission Denied
					Log.i(TAG, getClass().getSimpleName()+"::onRequestPermissionsResult(),not PERMISSION_GRANTED ");
					//Need Dialog
				}
				break;
			default:
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}


	final static int REQUEST_LOCATION = 199;
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d("onActivityResult()", Integer.toString(resultCode));

		//final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
		switch (requestCode) {
			case REQUEST_LOCATION :{
				switch (resultCode) {
					case Activity.RESULT_OK: {
						// All required changes were successfully made
						//Toast.makeText(this, "Location enabled by user!", Toast.LENGTH_LONG).show();
						break;
					}
					case Activity.RESULT_CANCELED: {
						// The user was asked to change settings, but chose not to
						//Toast.makeText(this, "Location not enabled, user cancelled.", Toast.LENGTH_LONG).show();
						Log.i(TAG, "Location not enabled, user cancelled.");
						//Need Dialog
						break;
					}
					default: {
						break;
					}
				}
				break;
			}
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.i(TAG, "onConnectionFailed(), "+((null != connectionResult)?connectionResult.toString():""));
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.i(TAG, "onConnected(), "+((null != bundle)?bundle.toString():"null"));
		BeseyeLocationMgr.getInstance().checkLocationService(this, REQUEST_LOCATION);
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		Log.i(TAG, "onConnectionSuspended(), "+arg0);

	}
}
