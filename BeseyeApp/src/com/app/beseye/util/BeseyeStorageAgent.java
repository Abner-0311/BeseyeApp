package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.DEBUG;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.RejectedExecutionException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class BeseyeStorageAgent {
	private static final String LOG_TAG = BeseyeConfig.TAG;
	static private boolean sExternalStorageAvailable = false;
	static private boolean sExternalStorageWriteable = false;
	
	static private final int MIN_STORAGE_VOLUME = 1024*1024*10;//10MB
	static private final int MAX_CACHE_VOLUME = 1024*1024*10;//10MB
	static private final String CACHE_FOLDER = "cache";
	
	static private OnSDCardNotifyListener sSDCardListener = null;
	
	static public interface OnSDCardNotifyListener
	{
		void notifySDCardStatusChanged(String strAction);
	}
	
	static MediaBroadcastReceiver sEventListener;
	
	static public class MediaBroadcastReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if(DEBUG)
	    		Log.d(LOG_TAG, "MediaBroadcastReceiver.onReceive() = " + intent);
	    	Uri uriData = null;
	    	if (null == intent)
	        {
		        Log.e(LOG_TAG, "mSDReceiver.onReceive, invalid intent");
		        return;
	        }
	        
	        String strAction = intent.getAction();
	        
			if (null == (uriData = intent.getData()))
		    {
			    Log.e(LOG_TAG, "mSDReceiver.onReceive, invalid intent data");
			    return;
		    }
			
	        String strPath = uriData.getPath();
	        
	        if ((null != strPath) && (true == strPath.contains("sdcard")))
	        {
	        	notifySDCardStatusChanged(strAction);
	        }
	    }
	};
	
	static public void registerSDCardStatusChangedListener(Context context, OnSDCardNotifyListener listner){
		sSDCardListener = listner;
		if(null != sSDCardListener){
			//Exception handling
			if(null != sEventListener){
				context.unregisterReceiver(sEventListener);
				sEventListener = null;
			}
			
			sEventListener = new MediaBroadcastReceiver();
			if (sEventListener != null) {
				if(DEBUG)
					Log.d(LOG_TAG, "install an intent filter to receive SD card related events.");
				IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
				if(null != intentFilter){
					intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
					intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
					intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
					intentFilter.addDataScheme("file");
					context.registerReceiver(sEventListener, intentFilter);
				}
			}else
				Log.e(LOG_TAG, "onResume(), failed to new MediaBroadcastReceiver");
		}
	}
	
	static public void unregisterSDCardStatusChangedListener(Context context){
		if(null != sSDCardListener){
			context.unregisterReceiver(sEventListener);
			sEventListener = null;
		}
		sSDCardListener = null;
	}
	
	static void notifySDCardStatusChanged(String strAction){
		if(null != sSDCardListener){
			sSDCardListener.notifySDCardStatusChanged(strAction);
		}
	}

    static private void updateExternalStorageState() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            sExternalStorageAvailable = sExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            sExternalStorageAvailable = true;
            sExternalStorageWriteable = false;
        } else {
            sExternalStorageAvailable = sExternalStorageWriteable = false;
        }
    }
    
    //Check if we can use external storage
    static public boolean canUseExternalStorage(){
    	updateExternalStorageState();
    	return sExternalStorageAvailable && sExternalStorageWriteable;
    }
    
    static public File getFileInPicDir(String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
	    final String cachePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath()+ File.separator +"Camera";

	    return new File(cachePath + File.separator + uniqueName);
	}
	
	// Creates a unique subdirectory of the designated app cache directory. Tries to use external
	// but if not mounted, falls back on internal storage.
	static public File getCacheDir(Context context) {
	    return canUseExternalStorage()?context.getExternalCacheDir() : context.getCacheDir();
	}
	
	static public File getFolderInCacheDir(Context context, String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
	    final String cachePath = canUseExternalStorage()//Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
	            /*|| !Environment.isExternalStorageRemovable()*/ ?
	                    context.getExternalCacheDir().getAbsolutePath() : context.getCacheDir().getAbsolutePath();

	    return new File(cachePath + File.separator + uniqueName);
	}
	
	public static boolean checkInternalSpaceEnough(Context context){
		return checkEnoughSpaceInFolder(context.getCacheDir().getAbsolutePath());
	}
	
	public static boolean checkExternalSpaceEnough(Context context){
		return canUseExternalStorage() && checkEnoughSpaceInFolder(context.getExternalCacheDir().getAbsolutePath());
	}
	
	public static boolean checkEnoughSpaceInFolder(String Folder){
		boolean bRes = true;
		
		if(null == Folder || Folder.isEmpty()){
			throw new IllegalArgumentException("isLowMemorry(), empty path");
		}

		int pos = Folder.lastIndexOf(File.separator);
		
		if(-1 == pos){
			throw new IllegalArgumentException("isLowMemorry(), can't find dir seperator of path : "+Folder);		
		}
		
		String folder = Folder.substring(0, pos);		
		File directory = new File(folder);
		
		if(false == directory.isDirectory()){
			throw new IllegalArgumentException("isLowMemorry(), is not a directory "+folder);
		}
		
		bRes = isLowMemorry(folder, MIN_STORAGE_VOLUME);
		
		return !bRes;
	}
	
	public static boolean isLowMemorry(String path, int iRequiredByte){
		boolean bRes = false;
		if(null == path || path.isEmpty()){
			Log.e(LOG_TAG, "isLowMemorry(), invalid path");
			return bRes;
		}
		
		if(DEBUG)
			Log.d(LOG_TAG, "isLowMemorry() +");
		StatFs stat = new StatFs(path);
		long block = stat.getAvailableBlocks();
		int blockSize = stat.getBlockSize();
		double AvailableSize = block*blockSize;	
		if(DEBUG)
			Log.d(LOG_TAG, "isLowMemorry(), stat AvailableBlocks : "+block+ " / Block : "+blockSize);
		
		if(AvailableSize <= iRequiredByte){	// in byte{
			bRes = true;
		}
		return bRes;
	}
	static private AsyncTask sCheckCacheTask;
	
	public static boolean doCheckCacheSize(Context context){
		try{
			if(null == sCheckCacheTask){
				sCheckCacheTask = new CacheSizeCheckTask(context).execute();
				return false;
			}
		}catch(RejectedExecutionException ex){
			Log.e(LOG_TAG, "doCheckCacheSize(), "+ex.toString());
		}
		return true;
	}
	
	static private class CacheSizeCheckTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onCancelled() {
			super.onCancelled();
			sCheckCacheTask = null;
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			sCheckCacheTask = null;
		}
		private Context mContext;
		
		CacheSizeCheckTask(Context context){
			mContext = context;
		}
		@Override
		protected Void doInBackground(Void... params) {
			checkCacheSize(mContext);
			return null;
		}
	}
	
	public static void checkCacheSize(Context context){
		if(DEBUG)
			Log.d(LOG_TAG, "checkCacheSize() +");
		checkInternalCache(context);
		if(canUseExternalStorage()){
			checkExternalCache(context);
		}
		if(DEBUG)
			Log.d(LOG_TAG, "checkCacheSize() -");
	}
	
	static private AsyncTask sDeleteCacheTask;
	
	public static boolean doDeleteCache(Context context){
		try{
			if(null == sDeleteCacheTask){
				sDeleteCacheTask = new CacheDeleteTask(context).execute();
				return false;
			}
		}catch(RejectedExecutionException ex){
			Log.e(LOG_TAG, "doDeleteCacheSize(), "+ex.toString());
		}
		return true;
	}
	
	static private class CacheDeleteTask extends AsyncTask<Void, Void, Void> {
		@Override
		protected void onCancelled() {
			super.onCancelled();
			sDeleteCacheTask = null;
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			sDeleteCacheTask = null;
		}
		private Context mContext;
		
		CacheDeleteTask(Context context){
			mContext = context;
		}
		@Override
		protected Void doInBackground(Void... params) {
			deleteCache(mContext);
			return null;
		}
	}
	
	public static void deleteCache(Context context){
		if(DEBUG)
			Log.d(LOG_TAG, "deleteCache() +");
		//File cacheFolder = new File(context.getCacheDir().getAbsolutePath()+ File.separator + CACHE_FOLDER);
		File cacheFolder = context.getCacheDir();
		if(null != cacheFolder && cacheFolder.exists()){
			deleteDir(cacheFolder);
		}
		
		if(canUseExternalStorage()){
			//File cacheExtenalFolder = new File(context.getExternalCacheDir().getAbsolutePath()+ File.separator + CACHE_FOLDER);
			File cacheExtenalFolder = context.getExternalCacheDir();
			if(null != cacheExtenalFolder){
				deleteDir(cacheExtenalFolder);
			}
		}
		if(DEBUG)
			Log.d(LOG_TAG, "deleteCache() -");
	}
	
	static private AsyncTask sDeleteCacheByFolderTask;
	
	public static boolean doDeleteCacheByFolder(Context context, String strFolder){
		try{
			if(null == sDeleteCacheTask){
				sDeleteCacheTask = new DeleteCacheByFolderTask(context, strFolder).execute();
				return false;
			}
		}catch(RejectedExecutionException ex){
			Log.e(LOG_TAG, "doDeleteCacheSize(), "+ex.toString());
		}
		return true;
	}
	
	static private class DeleteCacheByFolderTask extends AsyncTask<Void, Void, Void> {
		private Context mContext;
		private String mStrFolder;
		
		DeleteCacheByFolderTask(Context context, String strFolder){
			mContext = context;
			mStrFolder = strFolder;
		}
		
		@Override
		protected void onCancelled() {
			super.onCancelled();
			sDeleteCacheByFolderTask = null;
		}
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			sDeleteCacheByFolderTask = null;
		}

		@Override
		protected Void doInBackground(Void... params) {
			deleteCacheByFolder(mContext, mStrFolder);
			return null;
		}
	}
	
	public static void deleteCacheByFolder(Context context, String strFolder){
		if(DEBUG)
			Log.d(LOG_TAG, "deleteCacheByFolder() +, strFolder:"+strFolder);
		if(null != strFolder && 0 < strFolder.length()){
			File cacheFolder = new File(context.getCacheDir().getAbsolutePath()+ File.separator + strFolder);
			if(null != cacheFolder && cacheFolder.exists()){
				deleteDir(cacheFolder);
			}
			
			if(canUseExternalStorage()){
				File cacheExtenalFolder = new File(context.getExternalCacheDir().getAbsolutePath()+ File.separator + strFolder);
				if(null != cacheExtenalFolder){
					deleteDir(cacheExtenalFolder);
				}
			}
		}
		
		if(DEBUG)
			Log.d(LOG_TAG, "deleteCacheByFolder() -");
	}
	
	private static void checkInternalCache(Context context){
		try {
			//File cacheFolder = new File(context.getCacheDir().getAbsolutePath()+ File.separator + CACHE_FOLDER);
			File cacheFolder = context.getCacheDir();
			if(null != cacheFolder && cacheFolder.exists()){
				File[] files = cacheFolder.listFiles();
				if(0 ==  files.length)
					return;
				
				if(canUseExternalStorage()){
					//try to migrate the internal data to external 
					if(DEBUG)
						Log.d(LOG_TAG, "checkInternalCache(), try to migrate the internal data to external");
					//File cacheExtenalFolder = new File(context.getExternalCacheDir().getAbsolutePath()+ File.separator + CACHE_FOLDER);
					File cacheExtenalFolder = context.getExternalCacheDir();
					if(null != cacheExtenalFolder){
						cacheExtenalFolder.mkdir();
						for(File file:files){
							if(null == file)
								continue;
							
							File fileExternal = new File(cacheExtenalFolder.getAbsolutePath()+ File.separator +file.getName());
							if(null != fileExternal && (!fileExternal.exists() || (file.lastModified() > fileExternal.lastModified()))){
								copyFile(file, fileExternal);
							}
							file.delete();
						}
					}
				}else{
					if(DEBUG)
						Log.d(LOG_TAG, "checkInternalCache(), Check if exceed the max cache size");
					//Check if exceed the max cache size
					Arrays.sort(files, fileLastModifiedComparator);
					
					long lTotalSize = 0;
					long lTotalDeleteSize = 0;
					for(File file:files){
						if(MAX_CACHE_VOLUME < (lTotalSize + file.length())){
							lTotalDeleteSize+=file.length();
							file.delete();
						}else{
							lTotalSize+=file.length();
						}
					}
					
					if(DEBUG)
						Log.d(LOG_TAG, "checkInternalCache(), lTotalSize:"+lTotalSize+", lTotalDeleteSize:"+lTotalDeleteSize);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void checkExternalCache(Context context){
		//File cacheExtenalFolder = new File(context.getExternalCacheDir().getAbsolutePath()+ File.separator + CACHE_FOLDER);
		//Log.d(LOG_TAG, "checkExternalCache(), cacheExtenalFolder is "+cacheExtenalFolder.getAbsolutePath());
		File cacheExtenalFolder = context.getExternalCacheDir();
		if(null != cacheExtenalFolder && cacheExtenalFolder.exists()){
			File[] files = cacheExtenalFolder.listFiles();
			if(0 ==  files.length)
				return;
			
			if(DEBUG)
				Log.d(LOG_TAG, "checkExternalCache(), Check if exceed the max cache size");
			//Check if exceed the max cache size
			Arrays.sort(files, fileLastModifiedComparator);
			
			long lTotalSize = 0;
			long lTotalDeleteSize = 0;
			for(File file:files){
				if(MAX_CACHE_VOLUME < (lTotalSize + file.length())){
					lTotalDeleteSize+=file.length();
					file.delete();
				}else{
					lTotalSize+=file.length();
				}
			}
			
			if(DEBUG)
				Log.d(LOG_TAG, "checkExternalCache(), lTotalSize:"+lTotalSize+", lTotalDeleteSize:"+lTotalDeleteSize);
		}
	}
	
	static Comparator<File> fileLastModifiedComparator = new Comparator<File>(){
	    public int compare(File f1, File f2){
	        return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
	    } 
	};
	
	private static void copyFile(File src, File dst) throws IOException{
	    FileChannel inChannel = new FileInputStream(src).getChannel();
	    FileChannel outChannel = new FileOutputStream(dst).getChannel();
	    try{
	        inChannel.transferTo(0, inChannel.size(), outChannel);
	    }
	    finally{
	        if (inChannel != null)
	            inChannel.close();
	        if (outChannel != null)
	            outChannel.close();
	    }
	}
	
	private static boolean deleteDir(File dir) {
	    if (dir != null && dir.isDirectory()) {
	       String[] children = dir.list();
	       for (int i = 0; i < children.length; i++) {
	          boolean success = deleteDir(new File(dir, children[i]));
	          if (!success) {
	        	  Log.e(LOG_TAG, "deleteDir(), fail to delete dir :"+children[i]);
	             return false;
	          }
	       }
	    }
	    // The directory is now empty so delete it
	    return dir.delete();
	}
}
