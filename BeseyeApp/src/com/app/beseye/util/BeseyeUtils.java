package com.app.beseye.util;

import java.util.Calendar;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

public class BeseyeUtils {
	static private Handler sHandler = new Handler();
	
	static public int getDeviceWidth(Activity act){
		if(null != act){
			return act.getWindowManager().getDefaultDisplay().getWidth();
		}
		return 0;
	}
	
	static public int getDeviceHeight(Activity act){
		if(null != act){
			return act.getWindowManager().getDefaultDisplay().getHeight();
		}
		return 0;
	}
	
	static public void setEnabled(final View view, final boolean bEnabled){
		if(null != view){
			view.post(new Runnable(){
				@Override
				public void run() {
					view.setEnabled(bEnabled);
				}});
			
		}
	}
	
	static public void setVisibility(final View view, final int iVisibility){
		if(null != view){
			view.post(new Runnable(){
				@Override
				public void run() {
					view.setVisibility(iVisibility);
				}});
		}
	}
	
	static public boolean haveText(EditText et){
		return null != et && (0 < et.length() || View.GONE==et.getVisibility());
	}
	
	static public void setImageRes(final ImageView view, final int iResId){
		if(null != view){
			view.post(new Runnable(){
				@Override
				public void run() {
					view.setImageResource(iResId);
				}});
		}
	}
	
	static public void postRunnable(Runnable run, long lDelay){
		if(null != sHandler){
			if(0 < lDelay){
				sHandler.postDelayed(run, lDelay);
			}else{
				sHandler.post(run);
			}
		}
	}
	
	static public void removeRunnable(Runnable run){
		if(null != sHandler){
			sHandler.removeCallbacks(run);
		}
	}
	
	static public String removeDoubleQuote(String input){
		String strRet = input;
		if(null != input && 2 <= input.length() && input.startsWith("\"") && input.endsWith("\"")){
			strRet = input.substring(1, input.length()-1);
		}
		return strRet;
	}
	
	//Format validation
	static public boolean validEmail(String email) {
		if(null == email || 0 == email.length())
			return true;
	    Pattern pattern = Patterns.EMAIL_ADDRESS;
	    return pattern.matcher(email).matches();
	}
	
	static public boolean validPhone(String phone) {
		if(null == phone || 0 == phone.length())
			return true;
	    Pattern pattern = Patterns.PHONE;
	    return pattern.matcher(phone).matches() && 10 <= phone.length() && phone.startsWith("09");
	}
	
	//IME related 
	static public void hideSoftKeyboard (Context context, View view) {
		if(null != context && null != view){
			InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
			  imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}
	
	static public void showSoftKeyboard (Context context, View view) {
		if(null != context && null != view){
		  InputMethodManager imm = (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
		  imm.showSoftInput(view, 0);
		}
	}
	
	static public boolean isSameDay(Calendar date1, Calendar date2){
		boolean bRet = false;
		if(null != date1 && null != date2){
			if(date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) && date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR)){
				bRet = true;
			}
		}
		return bRet;
	}
}
