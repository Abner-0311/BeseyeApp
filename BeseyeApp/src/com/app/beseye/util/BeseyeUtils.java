package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;

import com.app.beseye.BeseyeApplication;
import com.app.beseye.R;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
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
import android.widget.Toast;

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
            "((?=.*\\d)(?=.*[a-zA-Z])(\\S*).{6,32})";
	  
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
	
	static public boolean canUpdateFromHockeyApp(){
		String strProcessName = BeseyeApplication.getProcessName();
		return (null != strProcessName && 0 < strProcessName.length() && strProcessName.startsWith("com.app.beseye.") && !isProductionVersion()) || (DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_STAGING_TOKYO.ordinal());
	}
	
	static public boolean isProductionVersion(){
		String strProcessName = BeseyeApplication.getProcessName();
		return (null != strProcessName && 0 < strProcessName.length() && strProcessName.equals("com.app.beseye.production"));
	}
	
	static public String getAndroidUUid(){
		return "{Mobile}_{Android}_{"+DeviceUuidFactory.getDeviceUuid()+(BeseyeConfig.PRODUCTION_VER?"":(BeseyeConfig.ALPHA_VER?"-alpha":(BeseyeConfig.BETA_VER?"-beta":"-debug")))+"}";
	}
	
	static public String getUserAgent(){
		return ("{"+Build.MANUFACTURER+"}_{"+Build.MODEL+"}");
	}
	
	static public String getStreamSecInfo(){
		try {
			return String.format("?ua=%s&se=%s&dd=%s", URLEncoder.encode(getUserAgent(), "utf-8"), SessionMgr.getInstance().getAuthToken(), getAndroidUUid());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
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
	
	static public boolean isHiddenFeature(){
		return BeseyeConfig.PRODUCTION_VER || BeseyeConfig.BETA_VER || BeseyeConfig.ALPHA_VER;
	}
	
	static public String getLog(){
		String description = "";

	    try {
	      Process process = Runtime.getRuntime().exec("logcat -d BesEye:D SoundPairing:I Debug:W *:S");
	      BufferedReader bufferedReader = 
	        new BufferedReader(new InputStreamReader(process.getInputStream()));

	      StringBuilder log = new StringBuilder();
	      String line;
	      while ((line = bufferedReader.readLine()) != null) {
	        log.append(line);
	        log.append(System.getProperty("line.separator"));
	      }
	      bufferedReader.close();

	      description = log.toString();
	    } 
	    catch (IOException e) {
	    	
	    }

	    return description;
	}
	
	static public void saveLogToFile(Context context){
		File cacheDir = BeseyeStorageAgent.getCacheDir(context);
		if(null != cacheDir){
			cacheDir.mkdir();
			File logDir = new File(cacheDir.getAbsolutePath()+"/log");
			if(null != logDir){
				logDir.mkdir();
				File[] oldLogs = logDir.listFiles();
				for(File log:oldLogs){
					if(log.exists() && log.isFile()){
						log.delete();
					}
				}
				
				Toast.makeText(context, "Dumping log, please wait...", Toast.LENGTH_LONG).show();
				
				File logFile = new File(logDir.getAbsolutePath()+"/"+SessionMgr.getInstance().getAccount()+"_"+getDateString(new Date(), "yyyy-MM-dd_hh:mm:ss")+".log");
				if(null != logFile){
					Writer writer = null;
					try {
						writer = new BufferedWriter(new FileWriter(logFile));
						if(null != writer){
							writer.write(getLog());
							writer.close();
						}
						
						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType("text/plain");
						
						final PackageManager pm = context.getPackageManager();
					    final List<ResolveInfo> matches = pm.queryIntentActivities(intent, 0);
					    ResolveInfo best = null;
					    for (final ResolveInfo info : matches)
					      if (info.activityInfo.packageName.endsWith(".gm") ||
					          info.activityInfo.name.toLowerCase().contains("gmail")) best = info;
					    if (best != null)
					      intent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
					    
						intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"abner.huang@beseye.com"});
						intent.putExtra(Intent.EXTRA_SUBJECT, "[Android Log]"+logFile.getName());
						intent.putExtra(Intent.EXTRA_TEXT, "This is log from "+SessionMgr.getInstance().getAccount()+"\nIssue occurred on [Cam name]");
						
						Uri uri = Uri.parse("file://" + logFile);
						intent.putExtra(Intent.EXTRA_STREAM, uri);
						context.startActivity(Intent.createChooser(intent, "Send email..."));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
	}
	
	static CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder(); // or "ISO-8859-1" for ISO Latin 1
	public static boolean isPureAscii(String v) {
		return null != v && asciiEncoder.canEncode(v);
	}
	
	static public String hexToASCII(String strHex){
		StringBuilder output = new StringBuilder();
	    for (int i = 0; i < strHex.length(); i+=2) {
	        String str = strHex.substring(i, i+2);
	        output.append((char)Integer.parseInt(str, 16));
	    }
	    
	    return output.toString();
	}
	
	static private String sPkgVersion = null;
	
	static public void setPackageVersion(Context context){
		if(null == sPkgVersion){
			try {
				PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
				sPkgVersion = packageInfo.versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	static public String getPackageVersion(){		
		return sPkgVersion;
	}
}
