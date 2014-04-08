package com.app.beseye;

import android.os.Bundle;


public class LoginActivity extends BeseyeBaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().hide();
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_login;
	}

}
