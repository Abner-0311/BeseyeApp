package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONObject;

import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class BeseyeTrustDevAuthActivity extends BeseyeBaseActivity {
	
	private static class PinCodeViewHolder{
		private ImageView mImgFillIn;
		private ImageView mImgError;
		
		public PinCodeViewHolder(View vParent){
			if(null != vParent){
				mImgFillIn = (ImageView)vParent.findViewById(R.id.iv_pincode_fill);
				mImgError = (ImageView)vParent.findViewById(R.id.iv_pincode_red);
			}
		}
		
		public void setEmpty(){
			BeseyeUtils.setVisibility(mImgFillIn, View.INVISIBLE);
			BeseyeUtils.setVisibility(mImgError, View.INVISIBLE);
		}
		
		public void setFillIn(){
			BeseyeUtils.setVisibility(mImgFillIn, View.VISIBLE);
			BeseyeUtils.setVisibility(mImgError, View.INVISIBLE);
		}
		
		public void setError(){
			BeseyeUtils.setVisibility(mImgFillIn, View.INVISIBLE);
			BeseyeUtils.setVisibility(mImgError, View.VISIBLE);
		}
	}
	
	static private final int NUM_OF_PINCODE = 6;
	static private final int NUM_OF_NUMBER = 10;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private TextView mTxtNavTitle;
	private TextView mTxtResendPincode;
	private ImageView mIvBack;
	private Button mBtnClear;
	private Button mBtnDone;
	private Button[] mBtnNum;
	private PinCodeViewHolder[] mVPincode;
	
	private String mStrPincode="";
	private boolean mbPincodeCheckError = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;//We do session check by this page 

		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			mTxtNavTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != mTxtNavTitle){
				mTxtNavTitle.setText(R.string.pincode_verify_title);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
			BeseyeUtils.setToolbarPadding(mVwNavBar, 0);
		}
		
		mBtnClear = (Button)findViewById(R.id.btn_pincode_num_clear);
		if(null != mBtnClear){
			mBtnClear.setOnClickListener(this);
		}
		
		mBtnDone = (Button)findViewById(R.id.btn_pincode_num_done);
		if(null != mBtnDone){
			mBtnDone.setOnClickListener(this);
			mBtnDone.setEnabled(false);
		}
		
		mBtnNum = new Button[NUM_OF_NUMBER];
		mBtnNum[0] = (Button)findViewById(R.id.btn_pincode_num_0);
		mBtnNum[1] = (Button)findViewById(R.id.btn_pincode_num_1);
		mBtnNum[2] = (Button)findViewById(R.id.btn_pincode_num_2);
		mBtnNum[3] = (Button)findViewById(R.id.btn_pincode_num_3);
		mBtnNum[4] = (Button)findViewById(R.id.btn_pincode_num_4);
		mBtnNum[5] = (Button)findViewById(R.id.btn_pincode_num_5);
		mBtnNum[6] = (Button)findViewById(R.id.btn_pincode_num_6);
		mBtnNum[7] = (Button)findViewById(R.id.btn_pincode_num_7);
		mBtnNum[8] = (Button)findViewById(R.id.btn_pincode_num_8);
		mBtnNum[9] = (Button)findViewById(R.id.btn_pincode_num_9);
		
		for(int idx = 0;idx < NUM_OF_NUMBER;idx++){
			if(null != mBtnNum[idx]){
				mBtnNum[idx].setOnClickListener(this);
			}
		}
		
		mVPincode = new PinCodeViewHolder[NUM_OF_PINCODE];
		mVPincode[0] = new PinCodeViewHolder(findViewById(R.id.vg_pincode_1));
		mVPincode[1] = new PinCodeViewHolder(findViewById(R.id.vg_pincode_2));
		mVPincode[2] = new PinCodeViewHolder(findViewById(R.id.vg_pincode_3));
		mVPincode[3] = new PinCodeViewHolder(findViewById(R.id.vg_pincode_4));
		mVPincode[4] = new PinCodeViewHolder(findViewById(R.id.vg_pincode_5));
		mVPincode[5] = new PinCodeViewHolder(findViewById(R.id.vg_pincode_6));
		
		mTxtResendPincode = (TextView)findViewById(R.id.tv_resend_pincode);
		if(null != mTxtResendPincode){
			mTxtResendPincode.setOnClickListener(this);
			mTxtResendPincode.setPaintFlags(mTxtResendPincode.getPaintFlags() |   Paint.UNDERLINE_TEXT_FLAG);
		}
		
		clearPincode();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		monitorAsyncTask(new BeseyeAccountTask.CheckAccountTask(BeseyeTrustDevAuthActivity.this), true, SessionMgr.getInstance().getAuthToken());
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_trust_dev_verify;
	}
	
	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.CheckAccountTask){
				if(0 == iRetCode){
					SessionMgr.getInstance().setIsTrustDev(true);
					launchDelegateActivity(CameraListActivity.class.getName());
				}/*else if(BeseyeError.E_BE_ACC_USER_SESSION_EXPIRED == iRetCode  || BeseyeError.E_BE_ACC_USER_SESSION_NOT_FOUND_BY_TOKEN == iRetCode){
					Toast.makeText(this, getString(R.string.toast_session_invalid), Toast.LENGTH_SHORT).show();
					onSessionInvalid(false);
				}*/else if(BeseyeError.E_BE_ACC_USER_SESSION_EXPIRED != iRetCode  && BeseyeError.E_BE_ACC_USER_SESSION_NOT_FOUND_BY_TOKEN != iRetCode){
					//BeseyeError.E_BE_ACC_USER_SESSION_CLIENT_IS_NOT_TRUSTED == iRetCode
					BeseyeUtils.postRunnable(new Runnable(){
						@Override
						public void run() {
							if(mActivityResume){
								monitorAsyncTask(new BeseyeAccountTask.CheckAccountTask(BeseyeTrustDevAuthActivity.this).setDialogId(-1), true, SessionMgr.getInstance().getAuthToken());
							}
						}}, 10000L);
				}
			}else if(task instanceof BeseyeAccountTask.PinCodeRenewTask){
				if(0 == iRetCode){
					Toast.makeText(getApplication(), getString(R.string.msg_pincode_verify_resend_done), Toast.LENGTH_SHORT).show();
					clearPincode();
				}
			}else if(task instanceof BeseyeAccountTask.PinCodeVerifyTask){
				if(0 == iRetCode){
					SessionMgr.getInstance().setIsTrustDev(true);
					launchDelegateActivity(CameraListActivity.class.getName());
				}else if(BeseyeError.E_BE_ACC_TT_PINCODE_VERIFY_FAILED == iRetCode){
					showMyDialog(DIALOG_ID_PIN_VERIFY_FAIL);
					onPincodeVerifyFailed();
				}else if(BeseyeError.E_BE_ACC_TT_PINCODE_VERIFY_FAILED_TOO_MANY_TIMES == iRetCode){
					showMyDialog(DIALOG_ID_PIN_VERIFY_FAIL_3_TIME);
				}else if(BeseyeError.E_BE_ACC_TT_PINCODE_IS_EXPIRED == iRetCode || BeseyeError.E_BE_ACC_TT_PINCODE_NOT_FOUND == iRetCode){
					showMyDialog(DIALOG_ID_PIN_VERIFY_FAIL_EXPIRED);
				}else{
					onServerError(iRetCode);
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, final int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeAccountTask.PinCodeRenewTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(BeseyeTrustDevAuthActivity.this, R.string.msg_pincode_verify_resend_fail, iErrType));
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);	
			
		}else if(task instanceof BeseyeAccountTask.PinCodeVerifyTask){
			//Do nothing because handling in onPostExecute
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
	
	@Override
	public void onSessionInvalid(AsyncTask task, int iInvalidReason) {
		if(task instanceof BeseyeAccountTask.CheckAccountTask && iInvalidReason != BeseyeHttpTask.ERR_TYPE_SESSION_NOT_TRUST){
			onSessionInvalid(false);
		}
	}

	private void appendPincode(int iNum){
		if(null != mStrPincode && NUM_OF_PINCODE > mStrPincode.length()){
			mStrPincode += iNum;
		}
		
		if(1 == mStrPincode.length()){
			mbPincodeCheckError = false;
		}
		
		updatePincodeUIState();
		
		if(NUM_OF_PINCODE == mStrPincode.length()){
			checkPincode();
		}
	}
	
	private void onPincodeVerifyFailed(){
		mStrPincode = "";
		mbPincodeCheckError = true;
		updatePincodeUIState();
	}
	
	private void clearPincode(){
		mStrPincode = "";
		updatePincodeUIState();
	}
	
	private void checkPincode(){
		monitorAsyncTask(new BeseyeAccountTask.PinCodeVerifyTask(this), false, mStrPincode);
	}
	
	private void resendPincode(){
		monitorAsyncTask(new BeseyeAccountTask.PinCodeRenewTask(this), false, mStrPincode);
	}
	
	private void updatePincodeUIState(){
		int iLenPincodeEntered = (null != mStrPincode)?mStrPincode.length():0;
		for(int idx = 0; idx < NUM_OF_PINCODE;idx++){
			if(null != mVPincode[idx]){
				if(mbPincodeCheckError){
					mVPincode[idx].setError();
				}else if(iLenPincodeEntered > idx){
					mVPincode[idx].setFillIn();
				}else{
					mVPincode[idx].setEmpty();
				}
			}
		}
		
		BeseyeUtils.setEnabled(mBtnDone, iLenPincodeEntered == NUM_OF_PINCODE);
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_pincode_num_0:{
				appendPincode(0);
				break;
			}
			case R.id.btn_pincode_num_1:{
				appendPincode(1);
				break;
			}
			case R.id.btn_pincode_num_2:{
				appendPincode(2);
				break;
			}
			case R.id.btn_pincode_num_3:{
				appendPincode(3);
				break;
			}
			case R.id.btn_pincode_num_4:{
				appendPincode(4);
				break;
			}
			case R.id.btn_pincode_num_5:{
				appendPincode(5);
				break;
			}
			case R.id.btn_pincode_num_6:{
				appendPincode(6);
				break;
			}
			case R.id.btn_pincode_num_7:{
				appendPincode(7);
				break;
			}
			case R.id.btn_pincode_num_8:{
				appendPincode(8);
				break;
			}
			case R.id.btn_pincode_num_9:{
				appendPincode(9);
				break;
			}
			case R.id.btn_pincode_num_clear:{
				clearPincode();
				break;
			}
			case R.id.btn_pincode_num_done:{
				checkPincode();
				break;
			}
			case R.id.tv_resend_pincode:{
				resendPincode();
				break;
			}
			case R.id.iv_nav_left_btn:{
				invalidDevSession();
				break;
			}
			default:
				super.onClick(view);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		switch(id){
			case DIALOG_ID_PIN_VERIFY_FAIL:{
				BaseOneBtnDialog d = new BaseOneBtnDialog(this);
				d.setBodyText(getString(R.string.msg_pincode_verify_fail));
				d.setTitleText(getString(R.string.dialog_title_warning));
				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
					@Override
					public void onBtnClick() {
						removeMyDialog(DIALOG_ID_PIN_VERIFY_FAIL);	
					}});
				dialog = d;
				break;
			}
			case DIALOG_ID_PIN_VERIFY_FAIL_EXPIRED:{
				BaseOneBtnDialog d = new BaseOneBtnDialog(this);
				d.setBodyText(getString(R.string.msg_pincode_verify_expired));
				d.setTitleText(getString(R.string.dialog_title_warning));
				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
					@Override
					public void onBtnClick() {
						removeMyDialog(DIALOG_ID_PIN_VERIFY_FAIL_EXPIRED);	
					}});
				dialog = d;
				break;
			}
			case DIALOG_ID_PIN_VERIFY_FAIL_3_TIME:{
				BaseOneBtnDialog d = new BaseOneBtnDialog(this);
				d.setBodyText(getString(R.string.msg_pincode_verify_fail_three_times));
				d.setTitleText(getString(R.string.dialog_title_warning));
				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
					@Override
					public void onBtnClick() {
						removeMyDialog(DIALOG_ID_PIN_VERIFY_FAIL_3_TIME);	
					}});
				dialog = d;
				dialog.setOnDismissListener(new OnDismissListener(){
					@Override
					public void onDismiss(DialogInterface dialog) {
						onSessionInvalid(true);
					}});
				break;
			}
			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		invalidDevSession();
	}
}
