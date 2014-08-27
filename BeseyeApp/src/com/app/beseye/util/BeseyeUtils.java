package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class BeseyeUtils {
	static private Handler sHandler = new Handler();
	
	static public final float BESEYE_THUMBNAIL_RATIO_9_16 = 9.0f/16.0f;
	
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
	
	static public void setText(final TextView view, final String strVal){
		if(null != view){
			view.post(new Runnable(){
				@Override
				public void run() {
					view.setText(strVal);
				}});
			
		}
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
			return false;
	    Pattern pattern = Patterns.EMAIL_ADDRESS;
	    return pattern.matcher(email).matches();
	}
	
	static public boolean validPhone(String phone) {
		if(null == phone || 0 == phone.length())
			return false;
	    Pattern pattern = Patterns.PHONE;
	    return pattern.matcher(phone).matches() && 10 <= phone.length() && phone.startsWith("09");
	}
	
	static private Pattern patternPw = null;

	private static final String PASSWORD_PATTERN = 
            "((?=.*\\d)(?=.*[a-zA-Z]).{6,20})";
	  
	static public boolean validPassword(String pw) {
		if(null == patternPw){
			patternPw = Pattern.compile(PASSWORD_PATTERN);
		}
		if(null == pw || 0 == pw.length())
			return false;
	    return patternPw.matcher(pw).matches();
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
	
	public static final int DEFAULT_FROM_TIME = 19*60*60; //PM 07:00
	public static final int DEFAULT_TO_TIME   = 7*60*60;  //AM 07:00
	public static final int DAY_IN_SECONDS    = 24*60*60; //86400
	
	static public String getTimeBySeconds(int iSeconds){
		int iTotalMinutes = (iSeconds/60);
		int iMinutes = iTotalMinutes%60;
		int iHours = (iTotalMinutes/60)%24;

		return String.format("%02d:%02d", iHours, iMinutes);
	}
	
	static public Date getTimeObjBySeconds(int iSeconds){
		Calendar c = Calendar.getInstance();
		int iTotalMinutes = (iSeconds/60);
		int iMinutes = iTotalMinutes%60;
		int iHours = iTotalMinutes/60;
		
		c.set(Calendar.HOUR_OF_DAY, iHours);
		c.set(Calendar.MINUTE, iMinutes);

		return c.getTime();
	}
	
	static public String getSchdelDaysInShort(JSONArray arrDays){
		String strRet = "";
		int iSize = (null != arrDays)?arrDays.length():0;
		for(int idx = 0;idx < iSize;idx++){
			if(idx > 0){
				strRet += ",";
			}
			try {
				strRet+=getSchdelDayInShort(arrDays.getInt(idx));
			} catch (JSONException e) {
				Log.e(TAG, "PowerScheduleEditActivity::getSchdelDaysInShort(), failed to parse, e:"+e.toString());
			}
		}
		return strRet;
	}
	
	static public String getSchdelDayInShort(int iDay){
		String strRet = "";
		SimpleDateFormat dayFormat = new SimpleDateFormat("E");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY+iDay); //Sunday ~ Saturday => 0 ~ 6, Calendar.SUNDAY~Calendar.SATURDAY => 1~7
		strRet = dayFormat.format(calendar.getTime());
		return strRet;
	}
	
	static public String getSchdelDay(int iDay){
		String strRet = "";
		SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE");
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY+iDay); //Sunday ~ Saturday => 0 ~ 6, Calendar.SUNDAY~Calendar.SATURDAY => 1~7
		strRet = dayFormat.format(calendar.getTime());
		return strRet;
	}
	
	static public String getGMTString(TimeZone tz){

		return getGMTString(tz, new Date());
	}
	
	static public String getGMTString(TimeZone tz, Date date){
		String strRet = "";
		if(null != tz){
			int iOffsetInMs = tz.getRawOffset() + ((tz.useDaylightTime() && tz.inDaylightTime(date))?tz.getDSTSavings():0);
			int hours = Math.abs(iOffsetInMs) / 3600000;
			int minutes = Math.abs(iOffsetInMs / 60000) % 60;
			String sign = iOffsetInMs >= 0 ? "+" : "-";
			
			strRet =  String.format("GMT %s%02d:%02d", sign, hours, minutes);
		}
		return strRet;
	}
	
	public static int setThumbnailRatio(View view, int iWidth, float fRatio){
		if(null == view || 0 > iWidth ||  0.0f > fRatio){
			Log.e(TAG, "setThumbnailRatio(), invalid params");
			return 0;
		}
			
		LayoutParams lp = view.getLayoutParams();
		if(null == lp){
			lp = new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		}
		else{
			lp =view.getLayoutParams();
		}
		
		lp.width = iWidth;
		lp.height = (int)(Math.ceil(((float)lp.width)*fRatio));
		view.setLayoutParams(lp);
		
		return lp.height;
	}
}
