package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeConfig.TEST_ACC;

import java.util.List;

import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.app.beseye.error.BeseyeError;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.util.BeseyeAccountFilter;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class ForgetPasswordActivity extends BeseyeAccountBaseActivity {
	private Button mBtnSubmit;
	private EditText mEtUserEmail;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		mbIgnoreCamVerCheck = true;
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.forget_password);
		}
		
		mBtnSubmit = (Button)findViewById(R.id.button_submit);
		if(null != mBtnSubmit){
			mBtnSubmit.setOnClickListener(this);
		}
		
		mEtUserEmail= (EditText)findViewById(R.id.editText_username);
		if(null != mEtUserEmail){
			mEtUserEmail.setOnClickListener(this);
			mEtUserEmail.setOnEditorActionListener(mOnEditorActionListener);
			if(DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_DEV.ordinal())
				mEtUserEmail.setText(TEST_ACC);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_forget_pw_sent_instruction;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_submit:{
				checkLoginInfo();
				break;
			}
			default:
				super.onClick(view);
		}		
	}
	
	private void checkLoginInfo(){
		if(null != mEtUserEmail){
			//mEtUserEmail.setText((new BeseyeAccountFilter()).filter(mEtUserEmail.getText(), 0, mEtUserEmail.length(), null, 0, 0));
			
			String strAccount = mEtUserEmail.getText().toString();
			if(!BeseyeUtils.validEmail(strAccount)){
				onShowDialog(null, DIALOG_ID_WARNING, getString(R.string.dialog_title_warning), getString(R.string.msg_invalid_account_format));
				return;
			}

			monitorAsyncTask(new BeseyeAccountTask.SendForgetPWTask(this), true, strAccount);
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,String strMsg) {	
		if(task instanceof BeseyeAccountTask.SendForgetPWTask){
			int iErrMsg = R.string.msg_login_error;
			if(BeseyeError.E_BE_ACC_USER_EMAIL_FORMAT_INVALID == iErrType){
				iErrMsg = R.string.msg_invalid_account_format;
			}else if(BeseyeError.E_BE_ACC_USER_NOT_FOUND_BY_EMAIL == iErrType){
				iErrMsg = R.string.msg_account_not_found;
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
			if(task instanceof BeseyeAccountTask.SendForgetPWTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					Bundle bundle= new Bundle();
					bundle.putString(BeseyeJSONUtil.ACC_EMAIL, mEtUserEmail.getText().toString());
					launchActivityByClassName(ForgetPWConfirmActivity.class.getName(), bundle);
					finish();
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
				if(view.equals(mEtUserEmail)){
					checkLoginInfo();
					BeseyeUtils.hideSoftKeyboard(ForgetPasswordActivity.this, mEtUserEmail);
					
					return true;
				}
			}			
			return false;
		}};
}
