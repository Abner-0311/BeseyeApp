package com.app.beseye;

import com.app.beseye.util.BeseyeJSONUtil;

import android.os.Bundle;
import android.widget.TextView;

public class ForgetPWConfirmActivity extends BeseyeAccountBaseActivity {
	private TextView mEtUserEmail;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(R.string.forget_password_check_title);
		}
		
		mEtUserEmail= (TextView)findViewById(R.id.tv_chech_mail_address);
		if(null != mEtUserEmail){
			mEtUserEmail.setText(getIntent().getStringExtra(BeseyeJSONUtil.ACC_EMAIL));
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_forget_pw_check_mail;
	}
}
