package com.app.beseye.ota;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.BeseyeNavBarBaseActivity;
import com.app.beseye.httptask.BeseyeUpdateBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class CamOTAFAQActivity extends BeseyeNavBarBaseActivity {
	private Button mBtnSubmit;
	private TextView mTvCamSN;
	private EditText mEtName, mEtEmail, mEtPhone, mEtQuestion;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);		
		mBtnSubmit = (Button)findViewById(R.id.button_send);
		if(null != mBtnSubmit){
			mBtnSubmit.setOnClickListener(this);
		}
		
		mTvCamSN = (TextView)findViewById(R.id.tv_faq_cam_id);
		if(null != mTvCamSN){
			BeseyeUtils.setText(mTvCamSN, getString(R.string.cam_info_sn)+":"+BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_HW_ID));
		}
		
		mEtName = (EditText)findViewById(R.id.et_faq_username);
		setTextWatcher(mEtName);
		
		mEtEmail = (EditText)findViewById(R.id.et_faq_useremail);
		setTextWatcher(mEtEmail);

		mEtPhone = (EditText)findViewById(R.id.et_faq_userphone);
		setTextWatcher(mEtPhone);

		mEtQuestion = (EditText)findViewById(R.id.et_faq_userquestion);
		setTextWatcher(mEtQuestion);

		BeseyeUtils.setVisibility(mIvBack, View.INVISIBLE);
	}
	
	private void setTextWatcher(EditText et){
		if(null != et){
			et.addTextChangedListener(mTextWatcher);
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_cam_update_faq;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.button_send:{
				JSONObject obj = new JSONObject();
				BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.UPDATE_OTA_USER_ACC, SessionMgr.getInstance().getAccount());
				BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.UPDATE_OTA_CAM_SN, BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_HW_ID));
				BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.UPDATE_OTA_USER_NAME, BeseyeUtils.getTrimText(mEtName));
				BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.UPDATE_OTA_USER_EMAIL, BeseyeUtils.getTrimText(mEtEmail));
				BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.UPDATE_OTA_USER_PHONE, BeseyeUtils.getTrimText(mEtPhone));
				BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.UPDATE_OTA_USER_DESC, BeseyeUtils.getTrimText(mEtQuestion));
				monitorAsyncTask(new BeseyeUpdateBEHttpTask.UploadUserOTAFeedbackTask(this), false, mStrVCamID, obj.toString());
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
		if(null != mBtnSubmit){
			mBtnSubmit.setEnabled(BeseyeUtils.haveText(mEtName) && 
								  BeseyeUtils.validEmail(null != mEtEmail?mEtEmail.getText().toString():null) && 
								  BeseyeUtils.validPhone(null != mEtPhone?mEtPhone.getText().toString():null) && 
								  BeseyeUtils.haveText(mEtQuestion));
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, final int iErrType, String strTitle,String strMsg) {	
		if(task instanceof BeseyeUpdateBEHttpTask.UploadUserOTAFeedbackTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(CamOTAFAQActivity.this, R.string.dialog_no_connectivity, iErrType));
					showMyDialog(DIALOG_ID_WARNING, b);
				}}, 0);
			
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(DEBUG)
			Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeUpdateBEHttpTask.UploadUserOTAFeedbackTask){
				if(0 == iRetCode){
					Intent intent = new Intent();
					intent.putExtra(CameraListActivity.KEY_VCAM_ID, mStrVCamID);
					setResult(RESULT_OK, intent);
					finish();
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
}
