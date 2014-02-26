package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.acra.ACRA;
import org.acra.collector.CrashReportData;
import org.acra.ReportField;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.util.Log;

public class HockeySender implements ReportSender {
	  private static String BASE_URL = "https://rink.hockeyapp.net/api/2/apps/";
	  private static String CRASHES_PATH = "/crashes";

	  @Override
	  public void send(CrashReportData report) throws ReportSenderException {
	    String log = createCrashLog(report);
	    String url = BASE_URL + ACRA.getConfig().formKey() + CRASHES_PATH;
	    //String url = BASE_URL + Configuration.HOCKEY_APP_ID + CRASHES_PATH;
	    
	    Log.e(TAG, "***STACK_TRACE***\n"+report.get(ReportField.STACK_TRACE));

	    try {
	      DefaultHttpClient httpClient = new DefaultHttpClient(); 
	      HttpPost httpPost = new HttpPost(url);

	      List<NameValuePair> parameters = new ArrayList<NameValuePair>(); 
	      parameters.add(new BasicNameValuePair("raw", log));
//	      parameters.add(new BasicNameValuePair("userID", (null != SessionMgr.getInstance().getOwnerChannelInfo())?SessionMgr.getInstance().getOwnerChannelInfo().getOwnerId():"ikala_guest"));
//	      parameters.add(new BasicNameValuePair("contact", report.get(ReportField.USER_EMAIL)));
//	      parameters.add(new BasicNameValuePair("description", report.get(ReportField.USER_COMMENT)));
	      
	      httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));

	      httpClient.execute(httpPost);   
	    }
	    catch (Exception e) {
	      e.printStackTrace();
	    } 
	  }

	  private String createCrashLog(CrashReportData report) {
	    Date now = new Date();
	    StringBuilder log = new StringBuilder();
	    
	    log.append("Package: " + report.get(ReportField.PACKAGE_NAME) + "\n");
	    log.append("Version: " + report.get(ReportField.APP_VERSION_CODE) + "\n");
	    log.append("Android: " + report.get(ReportField.ANDROID_VERSION) + "\n");
	    log.append("Manufacturer: " + android.os.Build.MANUFACTURER + "\n");
	    log.append("Model: " + report.get(ReportField.PHONE_MODEL) + "\n");
	    log.append("BUILD: <--\n" + report.get(ReportField.BUILD) + "-->\n");
	    log.append("TOTAL_MEM_SIZE: " + report.get(ReportField.TOTAL_MEM_SIZE) + "\n");
	    log.append("AVAILABLE_MEM_SIZE: " + report.get(ReportField.AVAILABLE_MEM_SIZE) + "\n");
	    log.append("Date: " + now + "\n");
	    log.append("***STACK_TRACE***\n");
	    log.append(report.get(ReportField.STACK_TRACE));
	    log.append("***LOGCAT***\n");
	    log.append(report.get(ReportField.LOGCAT));
	    log.append("***THREAD_DETAILS***\n");
	    log.append(report.get(ReportField.THREAD_DETAILS));
	    log.append("***EVENTSLOG***\n");
	    log.append(report.get(ReportField.EVENTSLOG));
	    log.append("***DUMPSYS_MEMINFO***\n");
	    log.append(report.get(ReportField.DUMPSYS_MEMINFO));

	    return log.toString();
	  }
}
