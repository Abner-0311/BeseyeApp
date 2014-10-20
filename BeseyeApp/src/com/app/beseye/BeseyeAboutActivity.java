package com.app.beseye;

import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeUtils;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class BeseyeAboutActivity extends BeseyeBaseActivity {
	protected View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	protected ImageView mIvBack;
	protected TextView mTxtNavTitle;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
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
				mTxtNavTitle.setText(R.string.about_title);
				mTxtNavTitle.setOnClickListener(this);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		TextView txtVer = (TextView)findViewById(R.id.txt_app_ver);
		if(null != txtVer){
			try {
				PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
				txtVer.setText(String.format(getString(R.string.about_ver), packageInfo.versionName));
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		TextView txtLink = (TextView)findViewById(R.id.txt_app_link);
		if(null != txtLink){
			txtLink.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_about_page;
	}

	private int miSendLog = 0;
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.txt_app_link:{
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.beseye.com"));
				startActivity(browserIntent);
				break;
			}
			case R.id.txt_nav_title:{
				//if(BeseyeConfig.DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_STAGING_TOKYO.ordinal()){
					if(++miSendLog == 5){
						BeseyeUtils.saveLogToFile(this);
					}
				//}
				break;
			}
			default:
				super.onClick(view);
		}		
	}
}
