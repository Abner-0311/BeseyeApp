package com.app.beseye.httptask;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class BeseyeHttpTask extends AsyncTask<String, Double, List<JSONObject>> {
	
public static final boolean LINK_PRODUCTION_SERVER = true;
	
	//public static String HOST_ADDR = "http://54.199.158.71:80/"; //= /*"http://210.64.138.164:5080/";*/"http://song81.corp.ikala.tv:5080/"; /*"http://192.168.0.61:3000/";//*///Internal;
	
	static{
		checkHostAddr();
	}
	
	static public void checkHostAddr(){
//		if(LINK_PRODUCTION_SERVER && SessionMgr.getInstance().getIsProductionMode()){
//			HOST_ADDR = "http://ap.mobile.sbf.ikala.tv:80/";
//			//HOST_ADDR = "http://210.64.138.160:80/";
//		}else{
//			HOST_ADDR = "http://song81.corp.ikala.tv:5080/";
////			HOST_ADDR = "http://210.64.138.161:5080/";
//		}
//		
//		iKalaAddrTask.checkHostAddr();
//		iKalaPushServiceTask.checkHostAddr();
	}
	
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
	private static final int CONNECTION_TIMEOUT = 10000; /* 10 seconds */
	// the timeout for waiting for data
	private static final int SOCKET_TIMEOUT = 10000; /* 10 seconds */
	
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
	
	public BeseyeHttpTask(OnHttpTaskCallback cb){
		mOnHttpTaskCallback = new WeakReference<OnHttpTaskCallback>(cb);
	}
	
	public BeseyeHttpTask(OnHttpTaskCallback cb, int iDialogId){
		this(cb);
		mDialogId = iDialogId;
	}
	public void enableHttps(){
		mbViaHttps = true;
	}
	
	public boolean isFromCache(){
		return mbFromCache;
	}
	
	public boolean needToForceUpdate(){
		return mbNeedForceUpdate;
	}
	
	public BeseyeHttpTask ignoreDialogShow(boolean bIngore){
		if(bIngore)
			mDialogId = -1;
		return this;
	}
	
	public BeseyeHttpTask setDialogId(int iDialogId){
		mDialogId = iDialogId;
		return this;
	}
	
	public int getDialogId(){
		return mDialogId;
	}
	
	public String getHttpMethod(){
		return mHttpMethod;
	}
	
	public BeseyeHttpTask setConnectionTimeout(int iConTimeout){
		miConTimeout = iConTimeout;
		return this;
	}
	
	public BeseyeHttpTask setSocketTimeout(int iSocketTimeout){
		miSocketTimeout = iSocketTimeout;
		return this;
	}
	
	public BeseyeHttpTask setDialogResId(int iTitleRes, int iMsgRes){
		this.miTitleRes = iTitleRes;
		this.miMsgRes = iMsgRes;
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
	
	protected int getRetCode(){
		return miRetCode;
	}
	
	protected int getErrType(){
		return miErrType;
	}
	
	protected void setRetryCount(int iCount){
		mRetryCount = (iCount>=1)?iCount:1;
	}
	
	protected void showToast(String strMsg){
		if(null != mOnHttpTaskCallback.get())
			mOnHttpTaskCallback.get().onToastShow(this, strMsg);
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
	
	@Override
	protected List<JSONObject> doInBackground(String... strUrlFile) {
		long lStartTime = System.currentTimeMillis();
		if(null != strUrlFile && 0 < strUrlFile.length){
			addResult(getJSONfromURL(strUrlFile));
			
			if(BeseyeConfig.DEBUG)
				Log.i(TAG, "doInBackground(), method: "+mHttpMethod+" takes "+(System.currentTimeMillis()-lStartTime)+" ms for "+filterPrivacyData(strUrlFile[0]));
		}
		return mResults;
	}
	
	protected void addResult(JSONObject ret){
		if(null != mResults)
			mResults.add(ret);
	}

	protected String getAppendSessionURL(String url){
		return url;//SessionMgr.appendSession(url);
	}
	
	//For hide the user privacy data
	static public String filterPrivacyData(String input){
		String strRet = input;
		if(null != input && 0 < input.length()){
			if(input.contains("user/sign_in")){
				strRet = BeseyeAccountTask.URL_LOGIN;
			}else if(input.contains("user/sign_up")){
				strRet = BeseyeAccountTask.URL_REGISTER;
			}
		}
		return strRet;
	}
	
	protected String mStrVCamIdForPerm = null;
	protected void setVCamIdForPerm(String strVcamId){
		mStrVCamIdForPerm = strVcamId;
	}
	
	//customize interface of AsycTask End
	
	/**Do real http task within this method
	   strParams[0] is URL, strParams[1] is JSON body
	**/
	protected HttpEntity doHttpRequest(String... strParams)throws Exception{
		HttpEntity entity = null;
		//http post
		String strUrl = getAppendSessionURL(strParams[0]);	
    	
		HttpClient httpclient = null;
//		if(BeseyeConfig.DEBUG)
//			Log.e(TAG, "doHttpRequest(),strUrl "+strUrl);
		if(strUrl.startsWith("https")){
			 httpclient = getNewHttpClient();
		}else
			httpclient = new DefaultHttpClient();
    	//HttpClient httpclient = mbViaHttps?getNewHttpClient():new DefaultHttpClient();
		HttpRequestBase httpRequest = null;
		try  {  
	    	httpRequest = getHttpRequestByType(strUrl);
	    	if(null == httpRequest){
	    		Log.e(TAG, "null httpRequest");
	    		return null;
	    	}
	
//	    	if(this instanceof BeseyeAccountTask.CamAttchTask || this instanceof BeseyeAccountTask.CamValidateTask || this instanceof BeseyeAccountTask.CamBeeValidateTask){
//	    		if(this instanceof BeseyeAccountTask.CamValidateTask || this instanceof BeseyeAccountTask.CamBeeValidateTask){
//	    			httpRequest.setHeader("Bes-Dev-Session", strParams[2]);
//	    		}
//	    	}else{
	    		if(null != mStrVCamIdForPerm && 0 < mStrVCamIdForPerm.length()){
	    			httpRequest.addHeader("Bes-VcamPermission-VcamUid", mStrVCamIdForPerm);
	    		}
	    		
	    		if(SessionMgr.getInstance().isTokenValid()){
	    			httpRequest.addHeader("Bes-User-Session", SessionMgr.getInstance().getAuthToken());
	    		}
	    		
	    		httpRequest.addHeader("Bes-Client-Devudid", BeseyeUtils.getAndroidUUid());
	    		httpRequest.addHeader("Bes-User-Agent", BeseyeUtils.getUserAgent());
	    		
	    		httpRequest.addHeader("Content-Type", "application/json");
	    		httpRequest.addHeader("Accept", "application/json");
	    		httpRequest.addHeader("User-Agent", BeseyeUtils.getUserAgent());
//	    	}
	    	
	    	if(httpRequest instanceof HttpEntityEnclosingRequestBase && 1 < strParams.length){
	    		HttpEntityEnclosingRequestBase request = (HttpEntityEnclosingRequestBase)httpRequest;
	    		request.setEntity(new StringEntity(strParams[1], HTTP.UTF_8));
	    	}
	    	Log.i(TAG, "Send Http Request:"+filterPrivacyData(strUrl)+", ["+(null != mStrVCamIdForPerm && 0 < mStrVCamIdForPerm.length())+", "+(SessionMgr.getInstance().isTokenValid())+"]");
	    	
	    	long startTime = System.currentTimeMillis();
	        HttpResponse response = httpclient.execute(httpRequest);
	        StatusLine statusLine = response.getStatusLine();
	        int statusCode = statusLine.getStatusCode();
	        if (statusCode == 200) {
	        	 entity = response.getEntity();
	             Log.i(TAG, "Receive Http Request:"+(System.currentTimeMillis()-startTime)+"ms");
	             //is = entity.getContent();
	        }
	        else{
	        	Log.e(TAG, "statusCode:"+statusCode+", url = "+filterPrivacyData(strUrl));
	        	 if(null != httpRequest){
	             	httpRequest.abort();
	             }
	        }     
        }catch(Exception e){
        	if(null != httpRequest){
             	httpRequest.abort();
            }
        	throw e;
        }
		return entity;
	}
	
	protected JSONObject getJSONfromURL(String... strParams){
		HttpEntity entity = null;
        // HTTP get JSON
		int retryCount = 0;
		while (true) {
			try{
				if ((entity = doHttpRequest(strParams)) != null) {
					break;
				}
			}catch (UnknownHostException e){
				miErrType = ERR_TYPE_NO_CONNECTION;
			    e.printStackTrace();
			}catch (org.apache.http.conn.HttpHostConnectException e){
				miErrType = ERR_TYPE_NO_CONNECTION;
			    e.printStackTrace();
			}catch(SocketTimeoutException e){
				miErrType = ERR_TYPE_CONNECTION_TIMEOUT;
			    e.printStackTrace();
			}catch (ConnectTimeoutException e){
				miErrType = ERR_TYPE_CONNECTION_TIMEOUT;
			    e.printStackTrace();
			}catch (Exception e){
				miErrType = ERR_TYPE_GENERAL_ERR;
				/* ZH: for debug */
//				miErrType = ERR_TYPE_CONNECTION_TIMEOUT;
				Log.e(TAG, "Error in http connection "+e.toString());
			}
			
			if (++retryCount >= mRetryCount
					|| Thread.currentThread()
							.isInterrupted()) {
				break;
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {}
		}
        return checkEntity(entity, strParams);
    }
	
	protected JSONObject parseJSON(String strRet){
		long startTime=System.currentTimeMillis();
		JSONObject jsonRet = null;
		 try{
        	if(null != strRet && 0 < strRet.length()){
        		jsonRet = new JSONObject(strRet); 
        	}
        }catch(JSONException e){
            Log.e(TAG, "Error parsing data ("+strRet+")"+e.toString());
        }
		 
		if(BeseyeConfig.DEBUG)
		    Log.i(TAG, "Parse JSONObj Time:"+(System.currentTimeMillis()-startTime)+"ms");
		        
	    if(null != jsonRet){
	    	if(jsonRet.has(BeseyeJSONUtil.RET_CODE_CAMBE)){
	    		miRetCode = BeseyeJSONUtil.getJSONInt(jsonRet, BeseyeJSONUtil.RET_CODE_CAMBE);
	    	}else{
	    		String strRetCode = BeseyeJSONUtil.getJSONString(jsonRet, BeseyeJSONUtil.RET_CODE, null);
		    	if(null != strRetCode){
		    		if(strRetCode.startsWith("0x")){
		    			strRetCode = strRetCode.substring(2);
		    		}
		    		miRetCode = Integer.parseInt(strRetCode);//Integer.valueOf(strRetCode, 16).intValue();
		    	}
	    	}
	    	
			//miRetCode = BeseyeJSONUtil.getJSONInt(jsonRet, BeseyeJSONUtil.RET_CODE, Integer.MIN_VALUE);
	    	
		}else
			miErrType = ERR_TYPE_INVALID_DATA;
	    
		return jsonRet;
	}
	
	protected JSONObject checkEntity(HttpEntity entity, String... strParams){
		String result = "";
		JSONObject jsonRet = null;
		if(ERR_TYPE_NO_ERR == miErrType){
			try {
		    	if(null != entity){
		    		result = EntityUtils.toString(entity);
		    	}
			} catch (ParseException e1) {
				miErrType = ERR_TYPE_INVALID_DATA;
				e1.printStackTrace();
			} catch (IOException e1) {
				miErrType = ERR_TYPE_GENERAL_ERR;
				e1.printStackTrace();
			}  
		
		    jsonRet = parseJSON(result);
		}
	
		checkError(jsonRet, strParams);
		return jsonRet;
	}
	
	protected void checkError(JSONObject jsonRet, String... strParams){
		if(0 == miRetCode /*&& SessionMgr.getInstance().isMdidValid()*/){
			int iSessionMdid = BeseyeJSONUtil.getJSONInt(jsonRet, BeseyeJSONUtil.SESSION_MDID, -1);
			if(0 == iSessionMdid /*|| this instanceof iKalaChannelTask.LoadAboutMeInfoTask*/){//invalid session from social BE
				Log.e(TAG, "getJSONfromURL(), invalid seesion from social BE");
				miRetCode = ERR_TYPE_SESSION_INVALID;
				//miRetCode = -1;
			}
		}
		
		if(0 != miRetCode || miErrType != ERR_TYPE_NO_ERR){
	       	if(miErrType == ERR_TYPE_NO_ERR){
	       		if(/*SessionMgr.getInstance().isMdidValid() && */-1 == miRetCode){//invalid seesion from login BE
	       			Log.e(TAG, "getJSONfromURL(), invalid seesion from login BE");
	       			//if(this instanceof iKalaAccountTask.LogoutHttpTask){
	       			//	miRetCode = 0;//Let it pass
	       			//}else
	       			miErrType = ERR_TYPE_SESSION_INVALID;
	       		}else{
	       			miErrType = ERR_TYPE_REQUEST_RET_ERR;
	       		}
	       	}
	       	
	       	if(null != mOnHttpTaskCallback.get()){
	       		
	       		StringBuilder sb = new StringBuilder ();
	       		sb.append("miRetCode = "+String.format("%x", miRetCode)+", ");
	       	    for (int i = 0; i < strParams.length; i++){
	       	    	if(i == 0){
	       	    		sb.append ("[ ");
	       	    		//sb.append (SessionMgr.appendSession(filterPrivacyData(strParams [i])));
	       	    	}else
	       	    		sb.append (filterPrivacyData(strParams [i]));
	       	    	
	       	        if(i == strParams.length-1)
	       	    		sb.append (" ]");
	       	        else 
	       	        	sb.append (", ");
	       	    }
	       	    if(null != jsonRet){
	       	    	sb.append (", body: "+jsonRet.toString());
	       	    }
	       	    
	       	    if(ERR_TYPE_SESSION_INVALID == miErrType){
	       			mOnHttpTaskCallback.get().onSessionInvalid(this, 0);
	       		}else{
	       			mOnHttpTaskCallback.get().onErrorReport(this, miRetCode, "", sb.toString());
	       		}
	   			
	   			if(BeseyeConfig.DEBUG)
	   				Log.e(TAG, "getJSONfromURL(), err: "+sb.toString());
	       	}
       }
	}
	
	public static String encodeUrl(Bundle parameters) {
        if (parameters == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : parameters.keySet()) {
            Object parameter = parameters.get(key);
            if (!(parameter instanceof String)) {
                continue;
            }

            if (first) first = false; else sb.append("&");
//            sb.append(URLEncoder.encode(key) + "=" +
//                      URLEncoder.encode(parameters.getString(key)));
            sb.append(key + "=" +parameters.getString(key));
        }
        return sb.toString();
    }
	
	public static Bundle parseUrl(String url) {
        // hack to prevent MalformedURLException
        url = url.replace("iKalaconnect", "http");
        try {
            URL u = new URL(url);
            Bundle b = decodeUrl(u.getQuery());
            b.putAll(decodeUrl(u.getRef()));
            return b;
        } catch (MalformedURLException e) {
            return new Bundle();
        }
    }
 
	public static Bundle decodeUrl(String s) {
        Bundle params = new Bundle();
        if (s != null) {
            String array[] = s.split("&");
            for (String parameter : array) {
                String v[] = parameter.split("=");
                if (v.length == 2) {
                    params.putString(URLDecoder.decode(v[0]),
                                     URLDecoder.decode(v[1]));
                }
            }
        }
        return params;
    }
	
	public static JSONObject parseUrlInJSON(String url) {
        return decodeUrl(null, url);
    }
 
	public static JSONObject decodeUrl(JSONObject obj, String s) {
		if(null == obj){
			obj = new JSONObject();
		}
        if (s != null) {
            String array[] = s.split("&");
            for (String parameter : array) {
                String v[] = parameter.split("=");
                if (v.length == 2) {
                	try {
						obj.put(URLDecoder.decode(v[0]), URLDecoder.decode(v[1]));
					} catch (JSONException e) {
						e.printStackTrace();
					}
                }
            }
        }
        return obj;
    }
	
	public Object clone() throws CloneNotSupportedException {
		 return super.clone();
	}
	
	public static HttpClient getNewHttpClient() {
         try {
        	 //Log.e(TAG, "getNewHttpClient(), https enabled");
             KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
             trustStore.load(null, null);

             SSLSocketFactory sf = new EasySSLSocketFactory(trustStore);
             sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

             HttpParams params = new BasicHttpParams();
             HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
             HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
             //ConnManagerParams.setMaxTotalConnections(params, MAX_TOTAL_CONNECTIONS);
             

             SchemeRegistry registry = new SchemeRegistry();
             registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
             registry.register(new Scheme("https", sf, 443));

             ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

             return new DefaultHttpClient(ccm, params);
         } catch (Exception e) {
             return new DefaultHttpClient();
         }
     }	
	
	static private String generateChecksum(String algorithm, String stringToHash){
		 try {
          MessageDigest digest = MessageDigest.getInstance(algorithm);
          digest.reset();
          byte[] out = digest.digest(stringToHash.getBytes("UTF-8"));
          return convertToHex(out);//android.util.Base64.encodeToString(out, android.util.Base64.NO_WRAP);
	     } catch (Exception e) {
	        e.printStackTrace();
	     }
	     return null;
	}
	
	static private String generateSHA1Checksum(String stringToHash){
	     return generateChecksum("SHA-1", stringToHash);
	}
	
	static public String generateMD5Checksum(String stringToHash){
	     return generateChecksum("MD5", stringToHash);
	}
	
	static private final String IKALA_SHA1_TOKEN = "ikala53342456";
	static public String getSHA1Checksum(){
		String stringToHash = android.os.Build.MANUFACTURER+android.os.Build.MODEL+SessionMgr.getInstance().getAuthToken()+IKALA_SHA1_TOKEN;
		String checkSum = generateSHA1Checksum(stringToHash);
//	    if(BeseyeConfig.DEBUG)
//			Log.e(TAG, "getSHA1Checksum(), err: "+checkSum);
	    
	    return checkSum;
	}
	
	private static String convertToHex(byte[] data) { 
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) { 
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do { 
                if ((0 <= halfbyte) && (halfbyte <= 9)) 
                    buf.append((char) ('0' + halfbyte));
                else 
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        } 
        return buf.toString();
    } 
	
	static public String getLocalIpAddress(){
	    try {
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
	                InetAddress inetAddress = enumIpAddr.nextElement();
	                if (!inetAddress.isLoopbackAddress()) {
	                	String ip = Formatter.formatIpAddress(inetAddress.hashCode());
	                    return ip;
	                }
	             }
	         }
	     } catch (SocketException ex) {
	         Log.e(TAG, ex.toString());
	     }
	     return null;
	}

	static public class EasySSLSocketFactory extends SSLSocketFactory {
	        SSLContext sslContext = SSLContext.getInstance("TLS");

	        public EasySSLSocketFactory(KeyStore truststore)
	                        throws NoSuchAlgorithmException, KeyManagementException,
	                        KeyStoreException, UnrecoverableKeyException {
	                super(truststore);

	                TrustManager tm = new X509TrustManager() {
	                        public void checkClientTrusted(X509Certificate[] chain,
	                                        String authType) throws CertificateException {
	                        }

	                        public void checkServerTrusted(X509Certificate[] chain,
	                                        String authType) throws CertificateException {
	                        }

	                        public X509Certificate[] getAcceptedIssuers() {
	                                return null;
	                        }
	                };

	                sslContext.init(null, new TrustManager[] { tm }, null);
	        }

	        @Override
	        public Socket createSocket(Socket socket, String host, int port,
	                        boolean autoClose) throws IOException, UnknownHostException {
	                return sslContext.getSocketFactory().createSocket(socket, host, port,
	                                autoClose);
	        }

	        @Override
	        public Socket createSocket() throws IOException {
	                return sslContext.getSocketFactory().createSocket();
	        }

	}
}
