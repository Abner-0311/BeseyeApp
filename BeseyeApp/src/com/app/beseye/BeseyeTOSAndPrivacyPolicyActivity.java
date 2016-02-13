package com.app.beseye;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.app.beseye.util.BeseyeUtils;

import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;

public class BeseyeTOSAndPrivacyPolicyActivity extends BeseyeNavBarBaseActivity {
	private WebView mWvContent;
	static public final String TOS_PAGE = "TOS_PAGE";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		boolean bIsTOSPage = getIntent().getBooleanExtra(TOS_PAGE, false);

		mWvContent = new WebView(this);
		if(null != mWvContent){
			try {
				mWvContent.loadUrl((bIsTOSPage?"https://www.beseye.com/terms_of_use":"https://www.beseye.com/privacy_policy")+"?lang="+URLEncoder.encode(BeseyeUtils.getLocaleString(), "utf-8"));
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			mWvContent.setWebViewClient(new WebViewClient(){
//				@Override
//				   public boolean shouldOverrideUrlLoading(WebView view, String url) {
//				      view.loadUrl(url);
//				      return true;
//				   }
//			});
			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			setContentView(mWvContent, params);
		}
		
		if(null != mTxtNavTitle){
			mTxtNavTitle.setText(bIsTOSPage?R.string.signup_createAccount_description_terms:R.string.signup_createAccount_description_policy);
		}
		
		if(null != mIvBack){
			mIvBack.setImageResource(R.drawable.sl_event_list_cancel);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_about_page;
	}
}
