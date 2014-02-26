package com.app.beseye.httptask;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

import android.os.AsyncTask;
import android.util.Log;

public class BeseyeHttpTask extends AsyncTask<String, Double, List<JSONObject>> {
	public static final String LIST_NUM_LOAD = "24";
	public static final int HTTP_RETRY_COUNT = 3;
	
	protected int miErrType = ERR_TYPE_NO_ERR;
	/*
	 *Define the error types in http tasks 
	 * */
	public static final int ERR_TYPE_NO_ERR 				= 0;//init value
	public static final int ERR_TYPE_GENERAL_ERR  			= 1;//no idea about the err
	public static final int ERR_TYPE_INVALID_DATA   		= 2;//err occurred from the invalid format/parse failed
	public static final int ERR_TYPE_NO_CONNECTION  		= 3;//there is no network connection
	public static final int ERR_TYPE_CONNECTION_TIMEOUT  	= 4;//network connection timeout
	public static final int ERR_TYPE_REQUEST_RET_ERR  	    = 5;//when ret code is not equal to 0
	public static final int ERR_TYPE_SESSION_INVALID  	    = 6;//when seesion is invalid
	
	public static final int ERR_TYPE_COUNT				  	= 7;
	//
	
	protected WeakReference<OnHttpTaskCallback> mOnHttpTaskCallback;
	protected int miRetCode = Integer.MIN_VALUE, miTitleRes = 0, miMsgRes = 0;
	private String mHttpMethod = HttpGet.METHOD_NAME;
	private int mDialogId = BeseyeBaseActivity.DIALOG_ID_LOADING;// mDialogId < 0 =>no dialog
	private List<JSONObject> mResults;
	protected int mRetryCount = HTTP_RETRY_COUNT;
	protected boolean mbViaHttps = false;
	protected boolean mbFromCache = false;
	protected boolean mbNeedForceUpdate = false;
	private int miConTimeout = CONNECTION_TIMEOUT;
	private int miSocketTimeout = SOCKET_TIMEOUT;
	
	// the timeout until a connection is established
	private static final int CONNECTION_TIMEOUT = 60000; /* 10 seconds */
	// the timeout for waiting for data
	private static final int SOCKET_TIMEOUT = 60000; /* 10 seconds */
	
	protected static void setTimeouts(HttpParams params, int iConTimeout, int iSocketTimeout) {
	    params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
	    params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SOCKET_TIMEOUT);
	}
	
	static public interface OnHttpTaskCallback{
		void onShowDialog(AsyncTask task, int iDialogId, int iTitleRes, int iMsgRes);
		void onDismissDialog(AsyncTask task, int iDialogId);
		void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg);
		void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode);
		void onToastShow(AsyncTask task, String strMsg);
		void onSessionInvalid(AsyncTask task, int iInvalidReason);
	} 
	
	public BeseyeHttpTask setHttpMethod(String method){
		mHttpMethod = method;
		return this;
	}
	
	protected HttpRequestBase getHttpRequestByType(String url){
		HttpRequestBase request = new HttpGet(url);
		if(mHttpMethod.equals(HttpPut.METHOD_NAME)){
			request = new HttpPut(url);
		}else if(mHttpMethod.equals(HttpPost.METHOD_NAME)){
			request = new HttpPost(url);
		}else if(mHttpMethod.equals(HttpDelete.METHOD_NAME)){
			request = new HttpDelete(url);
		}
		
		if(null != request)
			setTimeouts(request.getParams(), miConTimeout, miSocketTimeout);
		return request;
	}
	
	public BeseyeHttpTask(OnHttpTaskCallback cb){
		mOnHttpTaskCallback = new WeakReference<OnHttpTaskCallback>(cb);
	}
	
	public BeseyeHttpTask(OnHttpTaskCallback cb, int iDialogId){
		this(cb);
		mDialogId = iDialogId;
	}
	
	@Override
	protected List<JSONObject> doInBackground(String... params) {
		Log.e(TAG, "doInBackground(), ==================params:"+params[5]);
		String url = "http://192.168.6.1/goform/WizardHandle";
		HttpEntity entity = null;
		DefaultHttpClient httpclient = null;
		httpclient = new DefaultHttpClient();
		
		Credentials creds = new UsernamePasswordCredentials("admin", "admin");
		httpclient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), creds);
		
		HttpPost httpRequest =  new HttpPost(url);
		
		HttpEntityEnclosingRequestBase request = (HttpEntityEnclosingRequestBase)httpRequest;
		
		request.addHeader("Content-Type", "application/x-www-form-urlencoded");
		request.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		request.addHeader("Accept-Encoding", "gzip,deflate,sdch");
		request.addHeader("Accept-Language", "zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4");
		//request.addHeader("Authorization", "Basic YWRtaW46YWRtaW4=");

		request.addHeader("Cache-Control", "max-age=0");
		request.addHeader("Connection", "keep-alive");
		request.addHeader("Host", "192.168.6.1");
		request.addHeader("Origin", "http://192.168.6.1");
		request.addHeader("Referer", "http://192.168.6.1/wizard3.asp");
//		request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	    nameValuePairs.add(new BasicNameValuePair("WANT", "3"));
	    nameValuePairs.add(new BasicNameValuePair("GO", "wizard3.asp"));
	    nameValuePairs.add(new BasicNameValuePair("enablewirelessEx", "1"));
	    nameValuePairs.add(new BasicNameValuePair("sta_ssid", params[0]));//
	    nameValuePairs.add(new BasicNameValuePair("sta_mac", params[1]));//
	    nameValuePairs.add(new BasicNameValuePair("sta_channel", params[2]));//
	    nameValuePairs.add(new BasicNameValuePair("sz11bChannel", params[2]));
	    nameValuePairs.add(new BasicNameValuePair("sz11gChannel", params[2]));
	    
	    //nameValuePairs.add(new BasicNameValuePair("sta_security_mode", "3"));//NONE = 0, WEP=1, WPA = 2, WPA2 =3
	    //nameValuePairs.add(new BasicNameValuePair("sta_WEPSelect", "0"));//wep mode = 1
	    //nameValuePairs.add(new BasicNameValuePair("sta_cipher", "1"));//WPA/WPA2 mode  =1
	    
	    String strCipher = params[4];
	    if(strCipher.contains(WifiAPInfo.AUTHNICATION_NONE)){
	    	nameValuePairs.add(new BasicNameValuePair("sta_security_mode", "0"));
	    	
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_mode", "0"));
	    	nameValuePairs.add(new BasicNameValuePair("sta_WEPSelect", "0"));
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_default_key", "1"));//
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_key_1", ""));//
		    nameValuePairs.add(new BasicNameValuePair("sta_wep_key_2", ""));//
		    nameValuePairs.add(new BasicNameValuePair("sta_wep_key_3", ""));//
		    nameValuePairs.add(new BasicNameValuePair("sta_wep_key_4", ""));//
		    
		    nameValuePairs.add(new BasicNameValuePair("sta_cipher", "0"));
		    nameValuePairs.add(new BasicNameValuePair("sta_passphrase", ""));//
	    }else if(strCipher.contains(WifiAPInfo.AUTHNICATION_WEP)){
	    	nameValuePairs.add(new BasicNameValuePair("sta_security_mode", "1"));
	    	
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_mode", "0"));
	    	nameValuePairs.add(new BasicNameValuePair("sta_WEPSelect", "1"));
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_default_key", params[5]));//
	    	int iKeyIndex = Integer.parseInt(params[5]);
	    	for(int i = 1; i <= 4;i++){
	    		nameValuePairs.add(new BasicNameValuePair("sta_wep_key_"+i, (iKeyIndex == i)?params[3]:""));//
	    	}
		    
		    nameValuePairs.add(new BasicNameValuePair("sta_cipher", "0"));
		    nameValuePairs.add(new BasicNameValuePair("sta_passphrase", ""));//
	    }else if(strCipher.contains(WifiAPInfo.AUTHNICATION_WPA)){
	    	if(strCipher.contains(WifiAPInfo.AUTHNICATION_WPA2))
	    		nameValuePairs.add(new BasicNameValuePair("sta_security_mode", "3"));
	    	else
	    		nameValuePairs.add(new BasicNameValuePair("sta_security_mode", "2"));
	    	
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_mode", "0"));
	    	nameValuePairs.add(new BasicNameValuePair("sta_WEPSelect", "0"));
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_default_key", "1"));//
	    	nameValuePairs.add(new BasicNameValuePair("sta_wep_key_1", ""));//
		    nameValuePairs.add(new BasicNameValuePair("sta_wep_key_2", ""));//
		    nameValuePairs.add(new BasicNameValuePair("sta_wep_key_3", ""));//
		    nameValuePairs.add(new BasicNameValuePair("sta_wep_key_4", ""));//
		    
		    nameValuePairs.add(new BasicNameValuePair("sta_cipher", "1"));
		    nameValuePairs.add(new BasicNameValuePair("sta_passphrase", params[3]));//
	    }
	    
	    nameValuePairs.add(new BasicNameValuePair("wirelessmode", "9"));
	    nameValuePairs.add(new BasicNameValuePair("bssid_num", "1"));
	    nameValuePairs.add(new BasicNameValuePair("ssid", "Tenda1"));
	    nameValuePairs.add(new BasicNameValuePair("broadcastssid", "1"));
	    nameValuePairs.add(new BasicNameValuePair("n_mode", "0"));
	    nameValuePairs.add(new BasicNameValuePair("n_bandwidth", "1"));
	    nameValuePairs.add(new BasicNameValuePair("n_gi", "1"));
	    nameValuePairs.add(new BasicNameValuePair("n_mcs", "33"));
	    nameValuePairs.add(new BasicNameValuePair("n_rdg", "1"));
	    nameValuePairs.add(new BasicNameValuePair("n_extcha", "0"));
	    nameValuePairs.add(new BasicNameValuePair("n_amsdu", "0"));
	    nameValuePairs.add(new BasicNameValuePair("ssidIndex", "0"));
	    nameValuePairs.add(new BasicNameValuePair("security_mode", "WPAPSKWPA2PSK"));
	    nameValuePairs.add(new BasicNameValuePair("security_shared_mode", "WEP"));
	    nameValuePairs.add(new BasicNameValuePair("wep_default_key", "1"));
	    nameValuePairs.add(new BasicNameValuePair("wep_key_1", ""));
	    nameValuePairs.add(new BasicNameValuePair("WEP1Select", "1"));
	    nameValuePairs.add(new BasicNameValuePair("wep_key_2", ""));
	    nameValuePairs.add(new BasicNameValuePair("WEP2Select", "0"));
	    nameValuePairs.add(new BasicNameValuePair("wep_key_3", ""));
	    nameValuePairs.add(new BasicNameValuePair("WEP3Select", "0"));
	    nameValuePairs.add(new BasicNameValuePair("wep_key_4", ""));
	    nameValuePairs.add(new BasicNameValuePair("WEP4Select", "0"));
	    nameValuePairs.add(new BasicNameValuePair("cipher", "1"));
	    nameValuePairs.add(new BasicNameValuePair("passphrase", "0630BesEye"));
	    nameValuePairs.add(new BasicNameValuePair("keyRenewalInterval", "3600"));
	    nameValuePairs.add(new BasicNameValuePair("CONNT", "1"));
	    nameValuePairs.add(new BasicNameValuePair("WANIP", "0.0.0.0"));
	    nameValuePairs.add(new BasicNameValuePair("WANMSK", "0.0.0.0"));
	    nameValuePairs.add(new BasicNameValuePair("WANGW", "0.0.0.0"));
	    nameValuePairs.add(new BasicNameValuePair("PUN", "pppoe_user"));
	    nameValuePairs.add(new BasicNameValuePair("PPW", "pppoe_passwd"));
	    nameValuePairs.add(new BasicNameValuePair("DNS1", "192.168.6.1"));
	    nameValuePairs.add(new BasicNameValuePair("DNS2", "192.168.6.1"));
	    nameValuePairs.add(new BasicNameValuePair("rebootTag", "1"));

	    try {
			httpRequest.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
			Log.i(TAG, "UnsupportedEncodingException()");
		}
	    
		HttpResponse response;
		try {
			Log.e(TAG, "doInBackground(), execute===================");

			response = httpclient.execute(httpRequest);
			Log.e(TAG, "doInBackground(), getStatusLine===================");
			StatusLine statusLine = response.getStatusLine();
	        int statusCode = statusLine.getStatusCode();
	        if (statusCode == 200) {
	        	 entity = response.getEntity();
	             Log.e(TAG, "Receive Http Request ok");
	             //is = entity.getContent();
	        }
	        else{
	        	Log.e(TAG, "statusCode:"+statusCode);
	        	 if(null != httpRequest){
	             	httpRequest.abort();
	             }
	        	 mResults = null;
	        }     
		} catch (ClientProtocolException e) {
			Log.e(TAG, "ClientProtocolException(), e:"+e.toString());
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(TAG, "IOException(), e:"+e.toString());
			e.printStackTrace();
		}
		Log.e(TAG, "doInBackground()==================");
		return mResults;
	}
	
	//customize interface of AsycTask Begin
		@Override
		protected void onCancelled() {
			super.onCancelled();
			if(0 <= mDialogId && null != mOnHttpTaskCallback.get())
				mOnHttpTaskCallback.get().onDismissDialog(this, mDialogId);
		}
		
		@Override
		protected void onPreExecute() {
			miErrType = ERR_TYPE_NO_ERR;
			
			if(null == mResults)
				mResults = new ArrayList<JSONObject>();
			else
				mResults.clear();
			
			if(0 <= mDialogId && null != mOnHttpTaskCallback.get())
				mOnHttpTaskCallback.get().onShowDialog(this, mDialogId, miTitleRes, miMsgRes);
			super.onPreExecute();
		}
	
	@Override
	protected void onPostExecute(List<JSONObject> result) {
		super.onPostExecute(result);
		
		if(null != mOnHttpTaskCallback.get())
			mOnHttpTaskCallback.get().onPostExecute(this, result, miRetCode);
		
		if(0 <= mDialogId && null != mOnHttpTaskCallback.get())
			mOnHttpTaskCallback.get().onDismissDialog(this, mDialogId);
//		super.onPreExecute();
	}

}
