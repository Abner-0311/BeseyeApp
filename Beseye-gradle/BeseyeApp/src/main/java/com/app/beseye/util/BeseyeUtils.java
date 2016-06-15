package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;

import com.app.beseye.BeseyeApplication;
import com.app.beseye.R;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.widget.Toolbar;
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
	static private Handler sHandler = null;
	
	static public void init(){
		sHandler = new Handler();
	}
	
	static public final float BESEYE_THUMBNAIL_RATIO_9_16 = 9.0f/16.0f;
	static public final float BESEYE_THUMBNAIL_RATIO_2_1 = 2.0f/1.0f;
	
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
		String strContent = (null != et)?et.getText().toString():null;
		return null != et && ((null != strContent && 0 < strContent.trim().length()) || View.GONE == et.getVisibility());
	}
	
	static public String getTrimText(EditText et){
		String strContent = (null != et)?et.getText().toString():null;
		return null != strContent?strContent.trim():"";
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
		if(null == email || 0 == email.length() || 0 == email.trim().length())
			return false;
	    Pattern pattern = Patterns.EMAIL_ADDRESS;
	    return pattern.matcher(email).matches();
	}
	
	static public boolean validPhone(String phone) {
		if(null == phone || 0 == phone.length() || 0 == phone.trim().length())
			return false;
	    Pattern pattern = Patterns.PHONE;
	    return pattern.matcher(phone).matches();// && 10 <= phone.length() && phone.startsWith("09");
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
		return (null != strProcessName && 0 < strProcessName.length() && strProcessName.startsWith("com.app.beseye.") && !isProductionVersion()) || (DEBUG && SessionMgr.getInstance().getServerMode().ordinal() != SERVER_MODE.MODE_PRODUCTION.ordinal());
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
	
	static public String getDevName(){
		String strRet = NetworkMgr.getInstance().getHotspotName();
		if(null == strRet || "".equals(strRet)){
			BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			if(null != adapter){
				strRet = adapter.getName();
			}
		}
		return toUtf8(strRet+BeseyeApplication.getAppMark());
	}
	
	public static String toUtf8(String str) {
		try {
			return new String(str.getBytes("UTF-8"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
		}
		return "";
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
	
	static public Date stringToDate(String strDate,String strFormat) {
	      if(strDate==null) return null;
	      ParsePosition pos = new ParsePosition(0);
	      SimpleDateFormat simpledateformat = new SimpleDateFormat(strFormat);
	      Date stringDate = simpledateformat.parse(strDate, pos);
	      return stringDate;            

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
	
	//JSON for the BE
	static public String getSchdelDaysInShort(JSONArray arrDays){
		String strRet = "";
		int iSize = (null != arrDays)?arrDays.length():0;
		for(int idx = 0;idx < iSize;idx++){
			if(idx > 0){
				strRet += ",";
			}
			try {
				//[Abner Review 0812]Need to add null pointer check after getJSONArray
				strRet+=getSchdelDayInShort(arrDays.getJSONArray(idx).getInt(0));
			} catch (JSONException e) {
				Log.e(TAG, "PowerScheduleEditActivity::getSchdelDaysInShort(), failed to parse, e:"+e.toString());
			}
		}
		return strRet;
	}

	//JSON for communication between PowerScheduleEditActivity and PowerScheduleDayPickerActivity
	static public String getSchdelLocalDaysInShort(JSONArray arrDays){
		String strRet = "";
		int iSize = (null != arrDays)?arrDays.length():0;
		for(int idx = 0;idx < iSize;idx++){
			if(idx > 0){
				strRet += ",";
			}
			try {
				strRet+=getSchdelDayInShort(arrDays.getInt(idx));
			} catch (JSONException e) {
				Log.e(TAG, "PowerScheduleEditActivity::getSchdelLocalDaysInShort(), failed to parse, e:"+e.toString());
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
	
	public static void setWidthAndHeight(View view, int iWidth, int iHeight){
		LayoutParams lp = view.getLayoutParams();
		if(null == lp){
			lp = new AbsListView.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		}
		else{
			lp =view.getLayoutParams();
		}
		if(0 <= iWidth)
			lp.width = iWidth;
		if(0 <= iHeight)
			lp.height = iHeight;
		view.setLayoutParams(lp);
	}
	
	static public boolean isHiddenFeature(){
		return BeseyeConfig.PRODUCTION_VER || BeseyeConfig.BETA_VER || BeseyeConfig.ALPHA_VER;
	}
	
	static public String getLog(){
		String description = "";

	    try {
	      Process process = Runtime.getRuntime().exec("logcat -v time -d BesEye:D SoundPairing:I Debug:W *:S");
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
			Log.e(TAG, "e:"+e.toString());
	    }
	    
	    if(description.length() == 0){
			Log.e(TAG, "log is empty !!!!");
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
				
				File logFile = new File(logDir.getAbsolutePath()+"/"+SessionMgr.getInstance().getAccount()+"_"+getDateString(new Date(), "MM_dd_hh_mm")+".log");
				//File logFile = new File(logDir.getAbsolutePath()+"/a.log");

				if(null != logFile){
					Writer writer = null;
					try {
						writer = new BufferedWriter(new FileWriter(logFile));
						if(null != writer){
							writer.write(getLog());
							writer.flush();
							writer.close();
						}
						
						launchEmail(context, 
									logFile, 
									"[Android Log]"+logFile.getName(), 
									"This is log from "+SessionMgr.getInstance().getAccount()+"\nIssue occurred on [Cam name]",
									SessionMgr.getInstance().getServerMode().equals(SessionMgr.SERVER_MODE.MODE_CHINA_STAGE)?"15219425820@163.com":"abner.huang@beseye.com");
					} catch (IOException e) {
						if(e instanceof FileNotFoundException){
							Log.e(TAG, "cannot find log :"+logFile.getAbsolutePath());
					    	Toast.makeText(context, "Can't save tmp log...", Toast.LENGTH_LONG).show();
						}else{
							Log.e(TAG, "e:"+e.toString());
						}
					}
				}
			}
			
		}
	}
	
	static public void launchEmail(Context context, File fAttachPath, String strSubject, String strContent, String strReceiverEmail){
		
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		
		final PackageManager pm = context.getPackageManager();
	    final List<ResolveInfo> matches = pm.queryIntentActivities(intent, 0);
	    if(matches.isEmpty()){
	    	Toast.makeText(context, "No mail related app", Toast.LENGTH_LONG).show();
	    }else{
	    	ResolveInfo best = null;
		    for (final ResolveInfo info : matches)
		      if (info.activityInfo.packageName.endsWith(".gm") ||
		          info.activityInfo.name.toLowerCase().contains("gmail")) best = info;
		    if (best != null)
		      intent.setClassName(best.activityInfo.packageName, best.activityInfo.name);
		    
		    if(null != strReceiverEmail){
		    	intent.putExtra(Intent.EXTRA_EMAIL, new String[] {strReceiverEmail});
		    }
			
		    if(null != strSubject){
		    	intent.putExtra(Intent.EXTRA_SUBJECT, strSubject);
		    }
		    
		    if(null != strContent){
		    	intent.putExtra(Intent.EXTRA_TEXT, strContent);
		    }
			
		    if(null != fAttachPath){
		    	Uri uri = Uri.parse("file://" + fAttachPath);
				intent.putExtra(Intent.EXTRA_STREAM, uri);
		    }
			
			context.startActivity(Intent.createChooser(intent, "Send email..."));
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
	static private int sPkgVersionCode = 0;
	
	static public void setPackageVersion(Context context){
		if(null == sPkgVersion){
			try {
				PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
				sPkgVersion = packageInfo.versionName;
				sPkgVersionCode = packageInfo.versionCode;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
	
	static public String getPackageVersion(){		
		return sPkgVersion;
	}
	
	static public int getPackageVersionCode(){		
		return sPkgVersionCode;
	}
	
	static public final String DEF_NEWS_LANG = "en";
	
	static public String getLocaleString(){
		String strLocale = DEF_NEWS_LANG;
		if(Locale.getDefault().equals(Locale.TRADITIONAL_CHINESE)){
			strLocale = "zh-tw";
		}else if(Locale.getDefault().equals(Locale.JAPAN) || Locale.getDefault().equals(Locale.JAPANESE) ){
			strLocale = "ja";
		}else if(Locale.getDefault().equals(Locale.SIMPLIFIED_CHINESE)){
			strLocale = "zh-cn";
		}
		Log.e(TAG, "getLocaleString(), Locale.getDefault():"+Locale.getDefault()+", strLocale:"+strLocale);

		return strLocale;
	}
	
	static public String getLocaleStringForRegion(){
		String strLocale = DEF_NEWS_LANG;
		if(Locale.getDefault().equals(Locale.TRADITIONAL_CHINESE)){
			strLocale = "zh-tw";
		}else if(Locale.getDefault().equals(Locale.JAPAN) || Locale.getDefault().equals(Locale.JAPANESE) ){
			strLocale = "ja";
		}else if(Locale.getDefault().equals(Locale.SIMPLIFIED_CHINESE)){
			strLocale = "zh-cn";
		}else{
			strLocale = Locale.getDefault().toString().replace("_", "-");
		}	
		Log.e(TAG, "getLocaleStringForRegion(), Locale.getDefault():"+Locale.getDefault()+", strLocale:"+strLocale);

		return strLocale;
	}
	
	//Begin of Gentle warning message
	static List<Integer> sLstSeriousWarningMsgIds = new ArrayList<Integer>();
	static{
		if(BeseyeFeatureConfig.TRANS_SERIOUS_WARNING){
			sLstSeriousWarningMsgIds.add(R.string.server_error);
			sLstSeriousWarningMsgIds.add(R.string.streaming_invalid_dvr);
			sLstSeriousWarningMsgIds.add(R.string.streaming_playing_error);
			sLstSeriousWarningMsgIds.add(R.string.streaming_error_unknown);
			sLstSeriousWarningMsgIds.add(R.string.streaming_error_low_mem);
			sLstSeriousWarningMsgIds.add(R.string.cam_update_timeout);
			//Append the serious warning msg string ids
		}
	} 
	
	static public String appendErrorCode(Context context, int iOriginStrId, int iErrCode){
		return appendErrorCodeByString( context, 
										context.getString((BeseyeFeatureConfig.TRANS_SERIOUS_WARNING && sLstSeriousWarningMsgIds.contains(iOriginStrId))?R.string.dialog_no_connectivity:iOriginStrId),
										iErrCode);
	}
	
	static public String appendErrorCodeByString(Context context, String strOrigin, int iErrCode){
		return strOrigin+(BeseyeFeatureConfig.APPEND_ERR_CODE?String.format(context.getString(R.string.error_code_fmt), iErrCode):"");
	}
	
	//End of Gentle warning message
	
	//Begin of exponential backoff
	final static int NUM_RETRY_INTERVAL = 7;
	final static float VAL_RETRY_INTERVAL[] = {1.0f, 2.0f, 4.0f, 8.0f, 16.0f, 32.0f, 64.0f};
	final static float RANDOM_FACTOR = 0.5f;
	
	static public long getRetrySleepTime(final int iRetryTime){
		long lRet = 1000;
		int iRealRetryTime = iRetryTime;
		
		if(iRealRetryTime < 0 ){
			iRealRetryTime = 0;
		}else if(iRealRetryTime >= NUM_RETRY_INTERVAL){
			iRealRetryTime = NUM_RETRY_INTERVAL - 1;
		}
		
		Random random = new Random(new Date().getTime());

		float fRetryIntervalBase = VAL_RETRY_INTERVAL[iRealRetryTime];
		float fRandomRatio = ((1.0f) + ((random.nextFloat()*2.0f - 1.0f)*RANDOM_FACTOR));
		float fRetryIntervalInSec = fRetryIntervalBase * fRandomRatio;
		
		lRet = (long) (fRetryIntervalInSec*1000);
		
		if(BeseyeConfig.DEBUG)
			Log.d(TAG, "getRetrySleepTime(), fRetryIntervalBase:"+fRetryIntervalBase+",fRandomRatio:"+fRandomRatio+", lRet:"+lRet);

		return lRet;
	}
	//End of exponential backoff
	
	static public boolean isServerUnavailableError(int iHttpStatusCode){
		return  HttpStatus.SC_BAD_GATEWAY == iHttpStatusCode ||
				HttpStatus.SC_NOT_FOUND == iHttpStatusCode ||
				HttpStatus.SC_REQUEST_TIMEOUT == iHttpStatusCode ||
				HttpStatus.SC_INTERNAL_SERVER_ERROR == iHttpStatusCode ||
				HttpStatus.SC_SERVICE_UNAVAILABLE == iHttpStatusCode ||
				HttpStatus.SC_GATEWAY_TIMEOUT == iHttpStatusCode ||
				429 ==  iHttpStatusCode; //HTTP_429_TOO_MANY_REQUESTS;
	}
	
	static public String getStringByResId(int iResId){
		return BeseyeApplication.getApplication().getString(iResId);
	}

	static public void setToolbarPadding(View vNaviBar, int iBgColor){
		if(null != vNaviBar){
			Toolbar parent =(Toolbar) vNaviBar.getParent();
			parent.setContentInsetsAbsolute(0,0);
			parent.setPadding(0,0,0,0);
			if(0 != iBgColor){
				parent.setBackgroundColor(iBgColor);
			}
		}
	}

	static public String getAppName(Context context){
		int iStringId = BeseyeConfig.ALPHA_VER?R.string.app_name_alpha:(BeseyeConfig.BETA_VER?R.string.app_name_beta:(BeseyeConfig.DEBUG?R.string.app_name_dev:R.string.app_name));
		return context.getString(iStringId);
	}
}
