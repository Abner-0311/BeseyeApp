package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Region.Op;
import android.graphics.Xfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import com.app.beseye.R;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;

public class RemoteImageView extends ImageView {
	final protected static int EMPTY_DEFAULT_IMAGE = -1;
	final protected static int PHOTO_THUMB_SAMPLE_MEM_THRESHHOLD = 48;//if mem class is greater than 48 MB, use sample rate 2, or use smaple rate 4
	
	protected String mCachePath;
	protected String mURI;
	protected Float mRatio = (float) 1.0; // default image's ratio
	protected Float mDestRatio = (float) 1.0; // dest image's ratio
	protected int miDesireWidth = -1, miDesireHeight = -1;
	protected RemoteImageCallback mCallback;
	protected int mDefaultImage = EMPTY_DEFAULT_IMAGE;
	protected Handler mHandler = new Handler();
	protected static ExecutorService sExecutor = Executors.newFixedThreadPool(10);
	protected Future<?> mFuture;
	protected boolean mIsPreload;
	protected boolean mbMatchWidth = false;
	
	protected boolean mbIsPhoto = false;
	protected boolean mbIsPhotoViewMode = false;
	protected boolean mbIsLoaded = false;
	
	protected String mStrVCamId = null;
	private String mStrVCamIdLoad = null;
	
	static public final String CACHE_POSTFIX_SAMPLE_1 = "_s1";//set sample as 1
	static public final String CACHE_POSTFIX_SAMPLE_2 = "_s2";//set sample as 2
	static public final String CACHE_POSTFIX_HIGH_RES = "_hs";//set as high resolution
	
	//For Shadow feature
	protected static final float SHADOW_WIDTH = 3.0f;
	protected boolean mbEnableShadow = false;
	protected float mShadowWidth = SHADOW_WIDTH;
	

	public interface RemoteImageCallback {
		public void imageLoaded(boolean success);
	}
	
	public RemoteImageView(Context context) {
		this(context, null, 0);
		setShadowWidth(context);
	}

	public RemoteImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		setShadowWidth(context);
	}

	public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setShadowWidth(context);
	}
	
	private void setShadowWidth(Context context){
		mShadowWidth = 0;//context.getResources().getDimension(com.ikala.app.R.dimen.iChannelConvPicShadowWidth);
	}
	
	public void setShadowWidth(float fWidth){
		mShadowWidth = (0 < fWidth)?fWidth:0.0f;
	}
	
	public void enableShadow(boolean bEnable){
		if(mbEnableShadow != bEnable){
			mbEnableShadow = bEnable;
			invalidate();
		}
	}
	
	public void setMatchWidth(boolean bMatch){
		mbMatchWidth = bMatch;
	}

	public void setCallback(RemoteImageCallback callback) {
		mCallback = callback;
	}

	public void setURI(String uri) {
		setURI(uri, EMPTY_DEFAULT_IMAGE);
		mbIsLoaded = false;
	}

	public void setURI(String uri, int defaultImage) {
		mURI = uri;
		mDefaultImage = defaultImage;
		mCachePath = buildCachePath(getContext(), uri);
		mbIsPhoto = false;
		mbIsPhotoViewMode = false;
		mbIsLoaded = false;
		mStrVCamId = null;
		
//		if(DEBUG)
//			Log.i(iKalaUtil.IKALA_APP_TAG, "setURI(), uri:"+uri); 
	}
	
	public void setURI(String uri, int defaultImage, String strVCamId) {
		setURI(uri, defaultImage);
		mStrVCamId = strVCamId;
	}

	public void setURI(String uri, int defaultImage, float ratio, float fDestRatio) {
		setURI(uri, defaultImage);
		mRatio = ratio;
		mDestRatio = fDestRatio;
	}
	
	public void setURI(String uri, int defaultImage, int iDesireWidth, int iDesireHeight) {
		setURI(uri, defaultImage);
		miDesireWidth = iDesireWidth;
		miDesireHeight = iDesireHeight;
		if(0 < miDesireWidth && 0 < miDesireHeight){
			mCachePath = String.format("%s_%s-%s", mCachePath, miDesireWidth, miDesireHeight);
		}
	}

	public void setImage(String file) {
		setImage(file, EMPTY_DEFAULT_IMAGE);
	}

	public void setImage(String path, int defaultImage) {
		mCachePath = path;
		mDefaultImage = defaultImage;
	}

	public String getCachePath() {
		return mCachePath;
	}

	public String getURI() {
		return mURI;
	}
	
	public boolean isLoaded(){
		return mbIsLoaded;
	}

	public void setIsPhoto(boolean bIsPhoto){
		mbIsPhoto = bIsPhoto;
	}
	
	public void setIsPhotoViewMode(boolean bIsPhotoViewMode){
		mbIsPhoto = mbIsPhotoViewMode = bIsPhotoViewMode;
	}
	
	static public boolean cacheExists(Context context, String cacheName) {
		String cachePath = buildCachePath(context, cacheName);
		if (cachePath != null && new File(cachePath).exists()) {
			return true;
		}
		return false;
	}
	
	private static Map<String, String> sMapCachePath = new HashMap<String, String>();

	static public String buildCachePath(Context context, String cacheName) {
		
		long lStartTime = System.currentTimeMillis();
		if (cacheName == null || cacheName.length() == 0) {
			return null;
		}

		File picDir = BeseyeStorageAgent.getCacheDir(context);
		if(null != picDir){
			picDir.mkdir();
		}
		
		String strEncode = null;
		
		if(sMapCachePath.containsKey(cacheName)){
			strEncode = sMapCachePath.get(cacheName);
		}else{
			strEncode = cacheName;//URLEncoder.encode(cacheName);
			int iFrom = (strEncode.length() > 64)?(strEncode.length()-64):0;
			strEncode = strEncode.substring(iFrom);
			sMapCachePath.put(cacheName, strEncode);
		}
		
//		int iIdx = strEncode.indexOf(".jpg");
//		if(-1 < iIdx){
//			int iFrom = (strEncode.length() > 32)?(strEncode.length()-32):0;
//			strEncode = strEncode.substring(0, iIdx+4);
//		}else{
//			int iFrom = (strEncode.length() > 32)?(strEncode.length()-32):0;
//			strEncode = strEncode.substring(iFrom);
//		}
		if(DEBUG)
			Log.d(TAG, "buildCachePath(), takes "+(System.currentTimeMillis() - lStartTime)+" ms");

		return String.format("%s%s", picDir.getAbsolutePath()+ "/", strEncode);
	}
	
	static public String[] getCachePaths(Context context, String[] cacheNames){
		String[] strRet = null;
		if(null != context && null != cacheNames && 0 < cacheNames.length){
			strRet = new String[cacheNames.length];
			for(int idx = 0; idx < cacheNames.length;idx++){
				strRet[idx] = buildCachePath(context, cacheNames[idx]);
			}
		}
		return strRet;
	} 

	static Hashtable<Integer, SoftReference<Bitmap>> mDefaultImageHolder = new Hashtable<Integer, SoftReference<Bitmap>>();

	public void loadDefaultImage() {
		Bitmap bitmap = getDefaultImage();
		if (bitmap != null) {
			// add stretch method
			if (!mRatio.equals((float) -1.0)) {
				Matrix matrix = new Matrix();
				matrix.postScale(mRatio, mRatio);

				Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
				setImageBitmap(resizedBitmap);
			} else {
				setImageBitmap(bitmap);
			}
		}else{
			//setImageBitmap(null);
			setImageResource(R.color.transparent);
		}
	}

	public Bitmap getDefaultImage() {
		if (mDefaultImage == EMPTY_DEFAULT_IMAGE) {
			return null;
		}
		
		return BeseyeMemCache.getBmpByResId(getContext(), mDefaultImage, 0, 0);
	}
	
	private File getDirByVCamid(String strVcamid){
		File vcamidDir = null;
		if(null != strVcamid){
			File picDir = BeseyeStorageAgent.getCacheDir(getContext());
			if(null != picDir){
				vcamidDir = new File(picDir.getAbsolutePath()+"/"+strVcamid);
				if(null != vcamidDir){
					if(!vcamidDir.isDirectory()){
						vcamidDir.delete();
					}
					if(!vcamidDir.exists()){
						vcamidDir.mkdir();
					}
				}
			}
		}
		
		return vcamidDir;
	}
	
	private String findLastPhotoByVCamid(String strVcamid){
		String strRet = null;
		File vcamidDir = getDirByVCamid(strVcamid);
		if(null != vcamidDir && vcamidDir.isDirectory() && vcamidDir.exists()){
			File[] files = vcamidDir.listFiles();
			if(null != files && 0 < files.length){
				strRet = files[0].getAbsolutePath();
			}
		}
		return strRet;
	}

	public void loadImage() {
		// load image from cache
		Bitmap cBmp = null;	
		if(null != mCachePath){
			if(null != mStrVCamId){
				String cacheFileName = mCachePath.substring(mCachePath.lastIndexOf("/")+1);
				if(DEBUG)
					Log.i(TAG, "cacheFileName:["+cacheFileName+"]:"+mStrVCamId);
				File vcamidDir = getDirByVCamid(mStrVCamId);
				String fileToCache = vcamidDir.getAbsoluteFile()+"/"+cacheFileName;
				if(DEBUG)
					Log.i(TAG, "fileToCache:["+fileToCache+"]:"+mStrVCamId);
				cBmp = BeseyeMemCache.getBitmapFromMemCache(fileToCache);
			}else{
				cBmp = BeseyeMemCache.getBitmapFromMemCache(mCachePath);
			}
		}
		
		if (cBmp != null) {
			if(DEBUG)
				Log.d(TAG, "loadImage(), use mem cache , mCachePath:["+mCachePath+"]");
			
			setImageBitmap(cBmp);
			//We don't cache high quality pic in memory
			if(mbIsPhotoViewMode){
				loadRemoteImage();
			}
			imageLoaded(true);
			return;
		} else {
			String fileCache = null;
			if(null != mStrVCamId){
				fileCache = findLastPhotoByVCamid(mStrVCamId);
			}else{
				fileCache = mCachePath+(mbIsPhoto?CACHE_POSTFIX_SAMPLE_1:CACHE_POSTFIX_SAMPLE_2);
			}
			
			if(fileExist(fileCache)){
				if(DEBUG)
					Log.d(TAG, "loadImage(), have file cache , fileCache:["+fileCache+"]");
				Bitmap cBmpFile = BeseyeMemCache.getBitmapFromMemCache(fileCache);
				if (cBmpFile != null) {
					if(DEBUG)
						Log.i(TAG, "loadImage(), have file cache in mem");
					setImageBitmap(cBmpFile);
				}else if(null == mStrVCamIdLoad || !mStrVCamIdLoad.equals(mStrVCamId)){
					loadDefaultImage();
				}
			}else{
				if(DEBUG)
					Log.d(TAG, "loadImage(), load default , no fileCache:["+fileCache+"]");
				loadDefaultImage();
			}
		}
		loadRemoteImage();
	}
	
	public void setImageBitmap(Bitmap bm, String strVcamId) {
		mStrVCamIdLoad = strVcamId;
		if(DEBUG)
			Log.d(TAG, "setImageBitmap(), mStrVCamIdLoad:["+mStrVCamIdLoad+"], id:"+this.getId());
		
		//setImageBitmap(bm);
		
		Drawable[] layers = new Drawable[2];
		layers[0] = this.getDrawable();
		layers[1] = new BitmapDrawable(bm);

		TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
		setImageDrawable(transitionDrawable);
		transitionDrawable.startTransition(300);
	}

//	@Override
//	public void setImageBitmap(Bitmap bm) {
////		if(mbMatchWidth){
////			
////		}
//		
//		super.setImageBitmap(bm);
//	}

	public void loadRemoteImage() {
		if (null != mFuture) {
			mFuture.cancel(true);
			mFuture = null;
		}
		
		if(null != mURI && 0 < mURI.length()){
			mFuture = sExecutor.submit(new LoadImageRunnable(mCachePath, mURI, mIsPreload, mbIsPhoto, mbIsPhotoViewMode, mStrVCamId));
		}
	}

	private void imageLoaded(final boolean success) {
		post(new Runnable(){

			@Override
			public void run() {
				mbIsLoaded = success;
				if (mCallback != null) {
					mCallback.imageLoaded(success);
				}
			}});
		
	}

	class LoadImageRunnable implements Runnable {		
		private String mLocal;
		private String mLocalSample, mLocalSampleHQ;
		private String mRemote;
		private String mStrVCamId = null;
		private boolean mIsPreload, mbIsPhoto, mbIsPhotoViewMode;

		public LoadImageRunnable(String local, String remote, boolean isPreload, boolean bIsPhoto, boolean bIsPhotoViewMode, String strVCamId) {
			mLocal = local;
			mRemote = remote;
			mIsPreload = isPreload;
			mbIsPhoto = bIsPhoto;
			mbIsPhotoViewMode = bIsPhotoViewMode;
			mStrVCamId = strVCamId;
			
			mLocalSample = mLocal+(mbIsPhoto?CACHE_POSTFIX_SAMPLE_1:CACHE_POSTFIX_SAMPLE_2);
			mLocalSampleHQ = mbIsPhotoViewMode?(mLocalSample+CACHE_POSTFIX_HIGH_RES):null;
		}

		class SetImageRunnable implements Runnable {
			Bitmap mBitmap;
			String mStrVcamId=null;

			public SetImageRunnable(Bitmap b) {
				mBitmap = b;
			}
			
			public SetImageRunnable(Bitmap b, String strVcamId) {
				mBitmap = b;
				mStrVcamId = strVcamId;
			}

			@Override
			public void run() {
				if (mBitmap == null || !equalsObj(mLocal, mCachePath)) {
					return;
				}
				setImageBitmap(mBitmap, mStrVcamId);
			}
		}

		private void setImage(Bitmap b) {
			if (mHandler != null && b != null) {
				mHandler.post(new SetImageRunnable(b));
			}
		}
		
		private void setImage(Bitmap b, String strVcamId) {
			if (mHandler != null && b != null) {
				mHandler.post(new SetImageRunnable(b, strVcamId));
			}
		}

		@Override
		public void run() {
			if (mLocal == null) {
				return;
			}
			boolean loaded = false;
			Bitmap bitmap = BeseyeMemCache.getBitmapFromMemCache(mLocal);
			if(mbIsPhotoViewMode){
				// set low quality image first
				if(DEBUG){
					Log.i(TAG, "use low quality first");
				}
				setImage(bitmap, mStrVCamId);
				bitmap = null;
			}
			
			File vcamidDir = null;
			boolean bSameFileForVCamid = false;
			String cacheFileName = null;
			try {
				if(null == bitmap){
//					if (fileExist(mLocal)) {						
//						if(DEBUG){
//							Log.w(TAG, "transfer cache file to sample 2, mLocal : " +mLocal);
//						}
//						//Version control, In order to transfer all cache file to sample 2						
//						deleteFile(mLocal);
//					}
					String strCachePath = mbIsPhotoViewMode?(mLocalSampleHQ):mLocalSample;
					cacheFileName = strCachePath.substring(strCachePath.lastIndexOf("/")+1);
					
					if(null != mStrVCamId){
						if(DEBUG)
							Log.i(TAG, "cacheFileName:["+cacheFileName+"]");
						vcamidDir = getDirByVCamid(mStrVCamId);
						String strLastPhoto = findLastPhotoByVCamid(mStrVCamId);
						if(DEBUG)
							Log.i(TAG, "strLastPhoto:["+strLastPhoto+"]");
						Bitmap cBmpFile = BeseyeMemCache.getBitmapFromMemCache(strLastPhoto);
						if(null == cBmpFile && fileExist(strLastPhoto)){
							bitmap = BitmapFactory.decodeFile(strLastPhoto);
							if(null != bitmap){
								setImage(bitmap, mStrVCamId);
								
								// write low quality image to memory cache
								BeseyeMemCache.addBitmapToMemoryCache(strLastPhoto, bitmap);
								
								//bitmap = null;
								
								if(DEBUG)
									Log.i(TAG, "use file cache first");
							}
						}
						bSameFileForVCamid = (null != strLastPhoto)?strLastPhoto.endsWith(cacheFileName):false;
					}
					
					if(mbIsPhotoViewMode && !fileExist(mLocalSampleHQ) && fileExist(mLocalSample)){
						bitmap = BitmapFactory.decodeFile(mLocalSample);
						if(null != bitmap){
							setImage(bitmap, mStrVCamId);
							
							// write low quality image to memory cache
							BeseyeMemCache.addBitmapToMemoryCache(mLocal, bitmap);
							
							bitmap = null;
							if(DEBUG){
								Log.i(TAG, "decode low quality first");
							}
						}
					}
					
					
					if(!bSameFileForVCamid){
						if(fileExist(mbIsPhotoViewMode?(mLocalSampleHQ):mLocalSample)){
							bitmap = BitmapFactory.decodeFile(mbIsPhotoViewMode?(mLocalSampleHQ):mLocalSample);
							if(mbIsPhotoViewMode && DEBUG){
								Log.i(TAG, "decode file use high quality");
							}
						}else {						
							if (mRemote == null) {
								return;
							}
							
							Bitmap downloadBitmap = null;
							try {
								if (mIsPreload) {
									downloadBitmap = BitmapFactory.decodeFile(mRemote);
								} else {
									// HTTP get image
									int retryCount = 0;
									while (true) {
										if ((downloadBitmap = imageHTTPTask(mRemote, 1, mStrVCamId/*mbIsPhotoViewMode?1:(mbIsPhoto && (PHOTO_THUMB_SAMPLE_MEM_THRESHHOLD >= BeseyeMemCache.getMemClass())?4:2)*/)) != null) {
											break;
										}
										if (++retryCount >= 3
												|| Thread.currentThread()
														.isInterrupted()) {
											return;
										}
										try {
											Thread.sleep(500);
										} catch (InterruptedException e) {
										}
									}
								}
								if(DEBUG){
									Log.w(TAG, "downloadBitmap width :"+downloadBitmap.getWidth()+", height :"+downloadBitmap.getHeight());
									Log.w(TAG, "mDestRatio :"+mDestRatio+", miDesireWidth :"+miDesireWidth+", miDesireHeight :"+miDesireHeight);
									
									if(mbIsPhotoViewMode){
										Log.w(TAG, "download file use high quality");
									}
								}
								
								if ((0 > Float.compare(mDestRatio, 1.0f) || (0 < miDesireWidth || 0 < miDesireHeight)) && downloadBitmap != null) {
									float fRatio = 1.0f;
									if(0 > Float.compare(mDestRatio, 1.0f)){
										fRatio = Math.abs(mDestRatio);
									}else{
										float fWidthRatio = Math.abs(miDesireWidth/(float)downloadBitmap.getWidth());
										float fHeightRatio = Math.abs(miDesireHeight/(float)downloadBitmap.getHeight());
										fRatio = (fWidthRatio > fHeightRatio)?((fHeightRatio)>1.0f?1.0f:Math.abs(fHeightRatio))
												                             :((fWidthRatio)>1.0f?1.0f:Math.abs(fWidthRatio));
									}
									
									if(0 > Float.compare(fRatio, 1.0f)){
										if(DEBUG)
											Log.w(TAG, "scale fRatio:"+fRatio+", mLocal = "+mLocal);
										// compress image to decrease memory usage
										bitmap = Bitmap.createScaledBitmap(downloadBitmap,
												Math.abs((int)(fRatio*downloadBitmap.getWidth())), Math.abs((int)(fRatio*downloadBitmap.getHeight())), true);
									}else{
										bitmap = downloadBitmap;
									}
									
								}else{
									bitmap = downloadBitmap;
								}
							} finally {
								if(bitmap != downloadBitmap)
									recycleBitmap(downloadBitmap);
							}
						}
					}
				}
				

				if (bitmap == null) {
					Log.w(TAG, "Get image fail");
					return;
				}
				
				if(null != vcamidDir){
					if(!bSameFileForVCamid){
						String fileToCache = vcamidDir.getAbsoluteFile()+"/"+cacheFileName;
						if(!fileExist(fileToCache))
							deleteFilesUnderDir(vcamidDir);
						
						compressFile(bitmap, fileToCache, Bitmap.CompressFormat.JPEG, 90);
						BeseyeMemCache.addBitmapToMemoryCache(fileToCache, bitmap);
					}
				}else if (!fileExist(mbIsPhotoViewMode?(mLocalSampleHQ):mLocalSample)) {// write image to file cache
					createParentDir(mbIsPhotoViewMode?(mLocalSampleHQ):mLocalSample);
					compressFile(bitmap, mbIsPhotoViewMode?(mLocalSampleHQ):mLocalSample, Bitmap.CompressFormat.JPEG, 90);
					if(mbIsPhotoViewMode && DEBUG){
						Log.w(TAG, "save file use high quality");
					}
				}
				
				// set image
				setImage(bitmap, mStrVCamId);

				if(false == mbIsPhotoViewMode){
					// write image to memory cache
					BeseyeMemCache.addBitmapToMemoryCache(mLocal, bitmap);
				}
				
				loaded = true;
			} finally {
				imageLoaded(loaded);
			}
		}
	}

	static public Bitmap imageHTTPTask(String uri, int iSample, String strVcamId) {
//		if(DEBUG)
//			Log.i(TAG, "imageHTTPTask(), iSample: " + iSample);
		
		if (uri == null) {
			return null;
		}
		InputStream inputStream = null;
		Bitmap bitmap = null;
		long lStartTs = System.currentTimeMillis();
		try {
			if(uri.startsWith("http")){
//				URL url = new URL(uri);
//				URLConnection conn = url.openConnection();
//				HttpURLConnection httpConn = (HttpURLConnection) conn;
//				httpConn.setRequestProperty("Bes-User-Session", SessionMgr.getInstance().getAuthToken());
//				httpConn.setRequestProperty("Bes-Client-Devudid", BeseyeUtils.getAndroidUUid());
//				httpConn.setRequestProperty("Bes-User-Agent", BeseyeUtils.getUserAgent());
//				httpConn.setRequestProperty("User-Agent", BeseyeUtils.getUserAgent());
//				if(null != strVcamId)
//					httpConn.setRequestProperty("Bes-VcamPermission-VcamUid", strVcamId);
//				httpConn.setRequestMethod("GET");
//				httpConn.connect();
//				if (httpConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//					inputStream = httpConn.getInputStream();
//					if (inputStream != null) {
//						BitmapFactory.Options options = new BitmapFactory.Options();
//						options.inSampleSize = iSample;
//						bitmap = BitmapFactory.decodeStream(inputStream, null, options);
//					}else{
//						Log.w(TAG, "inputStream is null");
//					}
//				}
				
				final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
				HttpGet getRequest = new HttpGet(convertURL(uri));
				try{
					if(null != getRequest){
						getRequest.addHeader("Bes-User-Session", SessionMgr.getInstance().getAuthToken());
						getRequest.addHeader("Bes-Client-Devudid", BeseyeUtils.getAndroidUUid());
						getRequest.addHeader("Bes-User-Agent", BeseyeUtils.getUserAgent());
						getRequest.addHeader("User-Agent", BeseyeUtils.getUserAgent());
						getRequest.addHeader("Bes-App-Ver", BeseyeUtils.getPackageVersion());
						getRequest.addHeader("Bes-Android-Ver", Build.VERSION.RELEASE);
						if(null != strVcamId){
							getRequest.addHeader("Bes-VcamPermission-VcamUid", strVcamId);
					      	HttpResponse response = client.execute(getRequest);
					      	final int statusCode = response.getStatusLine().getStatusCode();
					      	if(statusCode == HttpStatus.SC_OK){
					      		final HttpEntity entity = response.getEntity();
					      		if(entity != null){
					      			inputStream = entity.getContent();
					      			if (inputStream != null) {
										BitmapFactory.Options options = new BitmapFactory.Options();
										options.inSampleSize = iSample;
										bitmap = BitmapFactory.decodeStream(inputStream, null, options);
									}else{
										Log.w(TAG, "inputStream is null");
									}
					      			entity.consumeContent();
					      		}
					      	}
					    }
					}
				}catch(Exception e){
				      // Could provide a more explicit error message for IOException or
				      // IllegalStateException
				    	Log.w(TAG, "Http Get image fail: " + e);
				    	if(null != getRequest){
				    		getRequest.abort();
				    	}
				 }finally{
				      if(client != null){
				    	  client.close();
				      }
				 }
			}
//			else if(uri.startsWith(S3_FILE_PREFIX)){
//				int iBucketPos = S3_FILE_PREFIX.length();
//				int iFilePos = uri.indexOf("/", iBucketPos);
//				if(iFilePos > iBucketPos){
//					String strBucket = uri.substring(iBucketPos, iFilePos);
//					String strPath = uri.substring(iFilePos+1);
//					AmazonS3Client s3Client = new AmazonS3Client(myCredentials);
//					S3Object object = s3Client.getObject(new GetObjectRequest(strBucket, strPath));
//					//S3Object object = s3Client.getObject(new GetObjectRequest("2e26ea2bccb34937a65dfa02488e58dc-ap-northeast-1-beseyeuser", "thumbnail/400x225/2014/05-22/09/{sEnd}1400751551309_{dur}10426_{r}1400750317346_{th}1400751550883.jpg"));
//					inputStream = new BufferedInputStream(object.getObjectContent()); //
//					if (inputStream != null) {
//						BitmapFactory.Options options = new BitmapFactory.Options();
//						options.inSampleSize = iSample;
//						bitmap = BitmapFactory.decodeStream(inputStream, null, options);
//						
//						Log.w(TAG, "image decode "+uri+", bitmap="+(null == bitmap?"null":"not null"));
//					}
//				}
//			}
		} catch (Exception e) {
			Log.w(TAG, "Http Get image fail: " + e);
		} finally {
			closeStream(inputStream);
		}
		if(BeseyeConfig.DEBUG)
			Log.w(TAG, "imageHTTPTask(), take "+(System.currentTimeMillis()- lStartTs));
		return bitmap;
	}


	static public void createParentDir(String path) {
		if (path == null) {
			return;
		}
		File file = new File(path);
		File parent = null;
		if (!file.exists() && (parent = file.getParentFile()) != null
				&& !parent.exists()) {
			parent.mkdirs();
		}
	}
	
	static public void deleteFilesUnderDir(File vcamidDir){
		if(null != vcamidDir && vcamidDir.isDirectory()){
			File[] files = vcamidDir.listFiles();
			for(File file : files){
				if(null != file && file.exists()){
					file.delete();
				}
			}
		}
	}

	static public boolean fileExist(String path) {
		File f;
		return (path != null && (f = new File(path)).exists() && f.length() > 0);
	}
	
//	static private boolean deleteFile(String path) {
//		File f;
//		return (path != null && (f = new File(path)).exists() && f.delete());
//	}

	static public void compressFile(Bitmap bitmap, String target,
			Bitmap.CompressFormat format, int quality) {
		if (bitmap == null || target == null) {
			return;
		}
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(target);
			bitmap.compress(format, quality, outStream);
			outStream.flush();
		} catch (final Exception e) {
			Log.w(TAG, "Exception when compress image: " + e);
		} finally {
			closeStream(outStream);
		}
	}

	static private final boolean equalsObj(final Object obj1, final Object obj2) {
		if (null == obj1 && null == obj2) {
			return true;
		} else if (null != obj1) {
			return obj1.equals(obj2);
		} else if (null != obj2) {
			return obj2.equals(obj1);
		}
		return false;
	}

	static private void closeStream(java.io.Closeable closeable) {
		try {
			if (null != closeable) {
				closeable.close();
			}
		} catch (Exception e) {
			Log.w(TAG, "Close outstream fail: " + e);
		}
	}

	static private void recycleBitmap(final Bitmap bmp) {
		try {
			if (null != bmp && !bmp.isRecycled()) {
				bmp.recycle();
			}
		} catch (Exception e) {
		}
	}
	
	private RectF rectF = new RectF();
	private Paint paintShadow = new Paint();
	
	@Override
	protected void onDraw(Canvas canvas) {
		if(mbEnableShadow){
			// Round some corners betch!
			Drawable maiDrawable = getDrawable();
			if (maiDrawable instanceof BitmapDrawable ) {
				Paint paint = ((BitmapDrawable) maiDrawable).getPaint();
		        final int color = 0xff000000;
		        //Rect bitmapBounds = maiDrawable.getBounds();
		        //final RectF rectF = new RectF(bitmapBounds);
				
				int iWidth = getWidth();
				int iHeight = getHeight();
				//final Rect rect = new Rect(0, 0, iWidth, iHeight);
				rectF.set(0, 0, iWidth, iHeight);
				
				 // Create an off-screen bitmap to the PorterDuff alpha blending to work right
				int saveCount = canvas.saveLayer(rectF, null,
	                    Canvas.MATRIX_SAVE_FLAG |
	                    Canvas.CLIP_SAVE_FLAG |
	                    Canvas.HAS_ALPHA_LAYER_SAVE_FLAG |
	                    Canvas.FULL_COLOR_LAYER_SAVE_FLAG |
	                    Canvas.CLIP_TO_LAYER_SAVE_FLAG);
				// Resize the rounded rect we'll clip by this view's current bounds
				// (super.onDraw() will do something similar with the drawable to draw)
				//getImageMatrix().mapRect(rectF);

		        paint.setAntiAlias(true);
		        canvas.drawARGB(0, 0, 0, 0);
		        paint.setColor(color);
		        if(mbEnableShadow){
		        	rectF.top+=mShadowWidth;
			        rectF.left+=mShadowWidth;
			        rectF.bottom-=mShadowWidth;
			        rectF.right-=mShadowWidth;
		        }
		        canvas.drawRect(rectF,paint);

				Xfermode oldMode = paint.getXfermode();
				// This is the paint already associated with the BitmapDrawable that super draws
		        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
		        
		        super.onDraw(canvas);
		        paint.setXfermode(oldMode);
		        canvas.restoreToCount(saveCount);
		        
		        //Draw shadow
		        if(mbEnableShadow){
		        	canvas.save();
			        rectF.top-=mShadowWidth;
			        rectF.left-=mShadowWidth;
			        rectF.bottom+=mShadowWidth;
			        rectF.right+=mShadowWidth;
			        canvas.clipRect(rectF);
			        
			        rectF.top+=mShadowWidth;
			        rectF.left+=mShadowWidth;
			        rectF.bottom-=mShadowWidth;
			        rectF.right-=mShadowWidth;
			        canvas.clipRect(rectF, Op.DIFFERENCE);

			        paintShadow.setColor(Color.TRANSPARENT);
			        paintShadow.setShadowLayer(5.0f, 0, 0, 0x66000000); 
			        canvas.drawRect(rectF, paintShadow);
			        canvas.restore();
		        }
			} else {
				super.onDraw(canvas);
			}
		}else {
			Drawable maiDrawable = getDrawable();
			if (maiDrawable instanceof BitmapDrawable ) {
				BitmapDrawable bmp = (BitmapDrawable)maiDrawable;
				if(null == bmp.getBitmap() || false == bmp.getBitmap().isRecycled()){
					super.onDraw(canvas);
				}
			}
			else
				super.onDraw(canvas);
		}
	}
	
	private static String convertURL(String str) {
        String url = null;
        try{
        url = new String(str.trim()
        	//.replace(" ", "%20").replace("&", "%26")
            //.replace(",", "%2c").replace("(", "%28").replace(")", "%29")
            //.replace("!", "%21").replace("=", "%3D").replace("<", "%3C")
            //.replace(">", "%3E").replace("#", "%23").replace("$", "%24")
            //.replace("'", "%27").replace("*", "%2A").replace("-", "%2D")
            //.replace(".", "%2E").replace("/", "%2F").replace(":", "%3A")
            //.replace(";", "%3B").replace("?", "%3F").replace("@", "%40")
            //.replace("[", "%5B").replace("\\", "%5C").replace("]", "%5D")
            //.replace("_", "%5F").replace("`", "%60")
        	.replace("{", "%7B")
            //.replace("|", "%7C")
        	.replace("}", "%7D"));
        }catch(Exception e){
            e.printStackTrace();
        }
        return url;
    } 
}