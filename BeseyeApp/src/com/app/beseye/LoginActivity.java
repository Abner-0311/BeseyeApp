package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeConfig.TEST_ACC;

import java.util.List;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeAccountFilter;
import com.app.beseye.util.BeseyeFeatureConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class LoginActivity extends BeseyeAccountBaseActivity {
	private EditText mEtUserName, mEtPassword;
	private TextView mTvForgetPassword, mTvCreateAcc, mTvLogin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.signup_bottom_login);
		}
		
		mEtUserName = (EditText)findViewById(R.id.editText_username);
		if(null != mEtUserName){
			mEtUserName.addTextChangedListener(mTextWatcher);
			if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal())
				mEtUserName.setText(TEST_ACC);
		}
		
		mEtPassword = (EditText)findViewById(R.id.editText_password);
		if(null != mEtPassword){
			mEtPassword.addTextChangedListener(mTextWatcher);
			mEtPassword.setOnEditorActionListener(mOnEditorActionListener);
			if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal())
				mEtPassword.setText("beseye1234");
		}
		
		mTvForgetPassword = (TextView)findViewById(R.id.tv_forgetpw);
		if(null != mTvForgetPassword){
			mTvForgetPassword.setOnClickListener(this);
		}
		
		mTvCreateAcc = (TextView)findViewById(R.id.tv_create_account);
		if(null != mTvCreateAcc){
			mTvCreateAcc.setOnClickListener(this);
		}
		
		mTvLogin = (TextView)findViewById(R.id.button_login);
		if(null != mTvLogin){
			mTvLogin.setOnClickListener(this);
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		checkEditTextStates();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_login;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.tv_forgetpw:{
				launchActivityByClassName(ForgetPasswordActivity.class.getName());
				break;
			}
			case R.id.tv_create_account:{
				launchActivityByClassName(PairingRemindActivity.class.getName());
				break;
			}
			case R.id.button_login:{
				checkLoginInfo();
				break;
			}
			default:
				super.onClick(view);
		}
	}
	
	private TextWatcher mTextWatcher = new TextWatcher(){

		@Override
		public void afterTextChanged(Editable arg0) {
			checkEditTextStates();
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {}
	};
	
	
	private void checkEditTextStates(){
		if(null != mTvLogin){
			mTvLogin.setEnabled(BeseyeUtils.haveText(mEtUserName) && BeseyeUtils.haveText(mEtPassword));
		}
	}
	
	private void checkLoginInfo(){
		if(null != mEtUserName){
			mEtUserName.setText((new BeseyeAccountFilter()).filter(mEtUserName.getText(), 0, mEtUserName.length(), null, 0, 0));
			
			String strAccount = mEtUserName.getText().toString();
			if(!BeseyeUtils.validEmail(strAccount)){
				onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_invalid_account_format));
				return;
			}
			
			String strPw 		= (null != mEtPassword)?mEtPassword.getText().toString():null;
			if(null == strPw || 6 > strPw.length() || 32 < strPw.length()){
			//if(!BeseyeUtils.validPassword(strPw)){
				onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_pw_length_error));
				return;
			}
			
			if(BeseyeFeatureConfig.VPC_NUM_QUERY){
				monitorAsyncTask(new BeseyeAccountTask.GetVPCNoHttpTask(this).setDialogId(DIALOG_ID_LOGIN), true, mEtUserName.getText().toString());
			}else{
				monitorAsyncTask(new BeseyeAccountTask.LoginHttpTask(this).setDialogId(DIALOG_ID_LOGIN), true, mEtUserName.getText().toString(), mEtPassword.getText().toString());
			}
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,String strMsg) {	
		if(task instanceof BeseyeAccountTask.LoginHttpTask){
			int iErrMsg = R.string.msg_login_error;
			if(BeseyeError.E_BE_ACC_USER_EMAIL_FORMAT_INVALID == iErrType){
				iErrMsg = R.string.msg_invalid_account_format;
			}else if(BeseyeError.E_BE_ACC_USER_PASSWORD_INCORRET == iErrType){
				iErrMsg = R.string.msg_login_wrong_password;
			}
			onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(iErrMsg));
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(DEBUG)
			Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.LoginHttpTask){
				if(0 == iRetCode){
					//Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject obj = result.get(0);
					if(null != obj){
						JSONObject objSes = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_SES);
						if(null != objSes){
							SessionMgr.getInstance().setAuthToken(BeseyeJSONUtil.getJSONString(objSes, BeseyeJSONUtil.ACC_SES_TOKEN));
						}
						
						JSONObject objUser = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_USER);
						if(null != objUser){
							SessionMgr.getInstance().setUserid(BeseyeJSONUtil.getJSONString(objUser, BeseyeJSONUtil.ACC_ID));
							SessionMgr.getInstance().setAccount(BeseyeJSONUtil.getJSONString(objUser, BeseyeJSONUtil.ACC_EMAIL));
							SessionMgr.getInstance().setIsCertificated(BeseyeJSONUtil.getJSONBoolean(objUser, BeseyeJSONUtil.ACC_ACTIVATED));
						}
						
						if(false == SessionMgr.getInstance().getIsCertificated()){
							//Bundle b = new Bundle();
							//b.putBoolean(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
							//launchActivityByClassName(PairingRemindActivity.class.getName(), b);
							//setResult(RESULT_OK);
							SessionMgr.getInstance().cleanSession();
							onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_account_not_found));
						}else{
							launchDelegateActivity(CameraListActivity.class.getName());
							setResult(RESULT_OK);
							finish();
						}		
					}
				}
			}else if(task instanceof BeseyeAccountTask.GetVPCNoHttpTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONObject obj = result.get(0);
					if(null != obj){
						SessionMgr.getInstance().setVPCNumber(BeseyeJSONUtil.getJSONInt(obj, BeseyeJSONUtil.ACC_VPC_NO));
						BeseyeUtils.postRunnable(new Runnable(){
							@Override
							public void run() {
								monitorAsyncTask(new BeseyeAccountTask.LoginHttpTask(LoginActivity.this).setDialogId(DIALOG_ID_LOGIN), true, mEtUserName.getText().toString(), mEtPassword.getText().toString());
							}}, 100);
						
					}
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener(){

		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) { 
				if(view.equals(mEtPassword)){
					checkLoginInfo();
					BeseyeUtils.hideSoftKeyboard(LoginActivity.this, mEtPassword);
					
					return true;
				}
			}			
			return false;
		}};
}
