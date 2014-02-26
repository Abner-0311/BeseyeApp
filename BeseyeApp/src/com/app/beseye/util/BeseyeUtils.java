package com.app.beseye.util;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
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
}
