package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import java.util.List;

import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.pairing.PairingPlugPowerActivity;
import com.app.beseye.test.BeseyeAppVerConfigActivity;
import com.app.beseye.test.BeseyeComputexModeActivity;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeUtils;

public class BeseyeEntryActivity extends BeseyeBaseActivity {

	private TextView mTvSetupAndSignup, mTvLearnMore, mTvLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		mbIgnoreCamVerCheck = true;
		getSupportActionBar().hide();
		View logo = findViewById(R.id.iv_signup_top_logo);
		if(null != logo){
			logo.setOnClickListener(this);
		}
		
		View sun = findViewById(R.id.iv_signup_sun);
		if(null != sun){
			sun.setOnClickListener(this);
		}
		
		mTvSetupAndSignup = (TextView)findViewById(R.id.button_signup);
		if(null != mTvSetupAndSignup){
			mTvSetupAndSignup.setOnClickListener(this);
		}
		
		mTvLearnMore = (TextView)findViewById(R.id.tv_bottom_beseye);
		if(null != mTvLearnMore){
			mTvLearnMore.setOnClickListener(this);
		}
		
		mTvLogin = (TextView)findViewById(R.id.button_signin);
		if(null != mTvLogin){
			mTvLogin.setOnClickListener(this);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_signup_firstpage;
	}
	
	private int miDemoCount=0;
	//private int miDetachCount=0;
	private int miAppVerCount = 0;
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_signup:{
				launchActivityForResultByClassName(PairingPlugPowerActivity.class.getName(), null, REQUEST_SIGNUP);
				//launchActivityForResultByClassName(PairingRemindActivity.class.getName(), null, REQUEST_SIGNUP);
				break;
			}
			case R.id.button_signin:{
				launchActivityForResultByClassName(LoginActivity.class.getName(), null, REQUEST_SIGNUP);
				break;
			}
			case R.id.tv_bottom_beseye:{
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.beseye.com"));
				startActivity(browserIntent);
				break;
			}
			case R.id.iv_signup_top_logo:{
				if(BeseyeConfig.DEBUG){
					miDemoCount++;
					if(miDemoCount >=5){
						launchActivityByClassName(BeseyeComputexModeActivity.class.getName());
						miDemoCount =0;
					}
				}
				break;
			}
			case R.id.iv_signup_sun:{
//				if(BeseyeConfig.DEBUG){
//					miDetachCount++;
//					if(miDetachCount >=2){
//						//Toast.makeText(this, "Detach by HW ID", Toast.LENGTH_SHORT).show();
//						miDetachCount =0;
//						monitorAsyncTask(new BeseyeAccountTask.CamDettachByHWIDTask(this).setDialogId(DIALOG_ID_DETACH_CAM), false, SessionMgr.getInstance().getDetachHWID());
//					}
//				}else 
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
	
	static public final int REQUEST_SIGNUP = 1;
	static public final int REQUEST_SIGNIN = 2;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_SIGNUP == requestCode || REQUEST_SIGNIN == requestCode){
			if(resultCode == RESULT_OK){
				finish();
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}
}
