package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONObject;

import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.test.BeseyeAppVerConfigActivity;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeUtils;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class BeseyeAboutActivity extends BeseyeBaseActivity {
	protected View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	protected ImageView mIvBack, mIvLogo;
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
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
			BeseyeUtils.setToolbarPadding(mVwNavBar, 0);
		}
		
		TextView txtVer = (TextView)findViewById(R.id.txt_app_ver);
		if(null != txtVer){
			try {
				PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

				String strBranch = BeseyeConfig.ALPHA_VER?getString(R.string.alpha):
														 (BeseyeConfig.BETA_VER?getString(R.string.beta):
															 					(BeseyeConfig.DEBUG?"(dev)":
															 										""));
				
				txtVer.setText(String.format(getString(R.string.about_ver), strBranch+" "+packageInfo.versionName)+" ("+packageInfo.versionCode+")");
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		TextView txtLink = (TextView)findViewById(R.id.txt_app_link);
		if(null != txtLink){
			txtLink.setOnClickListener(this);
		}
		
		mIvLogo = (ImageView)findViewById(R.id.img_about_logo);
		if(null != mIvLogo){
			mIvLogo.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_about_page;
	}

	private int miSendLog = 0;
	//private int miDetachCount = 0;
	private int miAppVerCount = 0;
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.txt_app_link:{
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.beseye.com/home"));
				startActivity(browserIntent);
				break;
			}
			case R.id.txt_nav_title:{
				//if(BeseyeConfig.DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_STAGING_TOKYO.ordinal()){
					if(!BeseyeConfig.PRODUCTION_VER && ++miSendLog == 5){
						BeseyeUtils.saveLogToFile(this);
					}
				//}
					
				break;
			}
			case R.id.img_about_logo:{
//				if(BeseyeConfig.DEBUG){
//					miDetachCount++;
//					if(miDetachCount >=2){
//						//Toast.makeText(this, "Detach by HW ID", Toast.LENGTH_SHORT).show();
//						miDetachCount =0;
//						monitorAsyncTask(new BeseyeAccountTask.CamDettachByHWIDTask(this).setDialogId(DIALOG_ID_DETACH_CAM), false, SessionMgr.getInstance().getDetachHWID());
//					}
//				}
				
				if((BeseyeUtils.isProductionVersion() || BeseyeConfig.PRODUCTION_FAKE_APP_CHECK) && BeseyeAppVerConfigActivity.isInAppVerTestAllowList()){
					miAppVerCount++;
					if(miAppVerCount >= 2){
						miAppVerCount = 0;
						launchActivityByClassName(BeseyeAppVerConfigActivity.class.getName());
					}
				}
				break;
			}
			default:
				super.onClick(view);
		}		
	}
	
	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.CamDettachByHWIDTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					onToastShow(task, "Detach Cam (HW_ID is "+SessionMgr.getInstance().getDetachHWID()+") successfully.");
				}else{
					onToastShow(task, "Detach Cam (HW_ID is "+SessionMgr.getInstance().getDetachHWID()+") falsed.");
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
}
