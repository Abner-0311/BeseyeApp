package com.app.beseye.test;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeUtils;

public class BeseyeAppVerConfigActivity extends BeseyeBaseActivity {
	private TextView mTxtRealAppVer = null;
	private EditText mEtFakeAppVer = null;
	private Button mBtnClearFakeApp, mBtnApply, mBtnAppLogout; 
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		mbIgnoreCamVerCheck = true;
		mbIgnoreAppVerCheck = true;
		
		mTxtRealAppVer = (TextView)findViewById(R.id.txt_real_app_ver);
		if(null != mTxtRealAppVer){
			try {
				PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);				
				mTxtRealAppVer.setText(""+packageInfo.versionCode);
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		mEtFakeAppVer = (EditText)findViewById(R.id.et_fake_app_ver);
		if(null != mEtFakeAppVer){
			mEtFakeAppVer.setText(SessionMgr.getInstance().getFakeAppVer()>0?""+SessionMgr.getInstance().getFakeAppVer():"");
		}
		
		mBtnClearFakeApp = (Button)findViewById(R.id.btn_clear_fake_app_ver);
		if(null != mBtnClearFakeApp){
			mBtnClearFakeApp.setOnClickListener(this);
		}
		
		mBtnApply = (Button)findViewById(R.id.button_apply);
		if(null != mBtnApply){
			mBtnApply.setOnClickListener(this);
		}
		
		mBtnAppLogout = (Button)findViewById(R.id.button_apply_logout);
		if(null != mBtnAppLogout){
			mBtnAppLogout.setOnClickListener(this);
		}
	}
	
	private void applyFakeAppVer(){
		if((BeseyeUtils.isProductionVersion() || BeseyeConfig.PRODUCTION_FAKE_APP_CHECK) && BeseyeAppVerConfigActivity.isInAppVerTestAllowList()){
			int iFakeAppVer = Integer.parseInt(mEtFakeAppVer.getEditableText().toString());
			if(0 < iFakeAppVer){
				SessionMgr.getInstance().setFakeAppVer(iFakeAppVer);
			}else{
				Toast.makeText(this, "Invalid fake app ver "+iFakeAppVer, Toast.LENGTH_LONG).show();
			}
		}else{
			Toast.makeText(this, "You are not allowed to add fake app ver", Toast.LENGTH_LONG).show();
		}
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_clear_fake_app_ver:{
				SessionMgr.getInstance().setFakeAppVer(0);
				if(null != mEtFakeAppVer){
					mEtFakeAppVer.setText("");
				}
				break;
			}
			case R.id.button_apply:{
				applyFakeAppVer();
				finish();
				break;
			}
			case R.id.button_apply_logout:{
				//applyFakeAppVer();
				invokeLogout();
				break;
			}
			default:
				super.onClick(view);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_app_ver_config;
	}

	final static private String[] sArrAppVerTestAllowList = {"abner.huang@beseye.com",
															 "kelly.chen@beseye.com",
															 "leila.lin@beseye.com",
															 "xaviar.lin@beseye.com"};
	
	static public boolean isInAppVerTestAllowList(){
		boolean bRet = false;
		for(String strCheck : sArrAppVerTestAllowList){
			if(strCheck.equals(SessionMgr.getInstance().getAccount())){
				bRet = true;
				break;
			}
		}
		return bRet;
	}
}
