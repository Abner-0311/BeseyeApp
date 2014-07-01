package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
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
	
	static public int getStatusBarHeight(Activity act) {
		int statusBar = 0;
		Class c = null;
		try {
			c = Class.forName("com.android.internal.R$dimen");
			Object obj = c.newInstance();
			Field field = c.getField("status_bar_height");
			int x = Integer.parseInt(field.get(obj).toString());
			statusBar= act.getResources().getDimensionPixelSize(x);
		  Log.i(TAG, "CameraListActivity::statusBar(), statusBar="+statusBar);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	  return statusBar;
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
	
	static public String getAndroidUUid(){
		return "{Mobile}_{Android}_{"+DeviceUuidFactory.getDeviceUuid()+"}";
	}
	
	static public String getUserAgent(){
		return "{"+Build.MANUFACTURER+"}_{"+Build.MODEL+"}";
	}
	
	static public String getProcessName(Context context, int pID){
	    String processName = "";
	    ActivityManager am = (ActivityManager)context.getSystemService(Activity.ACTIVITY_SERVICE);
	    List l = am.getRunningAppProcesses();
	    Iterator i = l.iterator();
	    PackageManager pm = context.getPackageManager();
	    while(i.hasNext()) {
	          ActivityManager.RunningAppProcessInfo info = (ActivityManager.RunningAppProcessInfo)(i.next());
	          try { 
	              if(info.pid == pID){
	                  CharSequence c = pm.getApplicationLabel(pm.getApplicationInfo(info.processName, PackageManager.GET_META_DATA));
	                  //Log.d("Process", "Id: "+ info.pid +" ProcessName: "+ info.processName +"  Label: "+c.toString());
	                  //processName = c.toString();
	                  processName = info.processName;
	              }
	          }
	          catch(Exception e){
	                //Log.d("Process", "Error>> :"+ e.toString());
	          }
	   }
	   return processName;
	}
	
	private static android.text.format.DateFormat s_datetimeFormat = new android.text.format.DateFormat();
	public static String getDateString(Date date, String strFormat){
		if(null != date)
			return s_datetimeFormat.format(strFormat, date).toString();
		else
			return "";
	}
	
	static public String getDateDiffString(Context context, Date updateTime){
		String strRet = null;
		if(null != updateTime && null != context){
			strRet = getDateString(updateTime, "yyyy-MM-dd a hh:mm");
		}
		
		return strRet;
	}
}
