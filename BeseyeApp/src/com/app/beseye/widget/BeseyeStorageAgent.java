package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.*;

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
	static private boolean sExternalStorageAvailable = false;
	static private boolean sExternalStorageWriteable = false;
	
	static private final int MIN_STORAGE_VOLUME = 1024*1024*10;//10MB
	static private final int MAX_CACHE_VOLUME = 1024*1024*6;//6MB
	static private final String CACHE_FOLDER = "Cache";
	
	static private OnSDCardNotifyListener sSDCardListener = null;
	
	static public interface OnSDCardNotifyListener
	{
		void notifySDCardStatusChanged(String strAction);
	}
	
	static MediaBroadcastReceiver sEventListener;
	
	static public class MediaBroadcastReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	        Log.d(TAG, "MediaBroadcastReceiver.onReceive() = " + intent);
	    	Uri uriData = null;
	        if (null == intent)
	        {
		        Log.e(TAG, "mSDReceiver.onReceive, invalid intent");
		        return;
	        }
	        
	        String strAction = intent.getAction();
	        
			if (null == (uriData = intent.getData()))
		    {
			    Log.e(TAG, "mSDReceiver.onReceive, invalid intent data");
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
				Log.d(TAG, "install an intent filter to receive SD card related events.");
				IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_EJECT);
				if(null != intentFilter){
					intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
					intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
					intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
					intentFilter.addDataScheme("file");
					context.registerReceiver(sEventListener, intentFilter);
				}
			}else
				Log.e(TAG, "onResume(), failed to new MediaBroadcastReceiver");
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
	                    context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();

	    return new File(cachePath + File.separator + uniqueName);
	}
	
	public static boolean checkInternalSpaceEnough(Context context){
		return checkEnoughSpaceInFolder(context.getCacheDir().getPath());
	}
	
	public static boolean checkExternalSpaceEnough(Context context){
		return canUseExternalStorage() && checkEnoughSpaceInFolder(context.getExternalCacheDir().getPath());
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
			Log.e(TAG, "isLowMemorry(), invalid path");
			return bRes;
		}
		
		Log.d(TAG, "isLowMemorry() +");
		StatFs stat = new StatFs(path);
		long block = stat.getAvailableBlocks();
		int blockSize = stat.getBlockSize();
		double AvailableSize = block*blockSize;		
		Log.d(TAG, "isLowMemorry(), stat AvailableBlocks : "+block+ " / Block : "+blockSize);
		
		if(AvailableSize <= iRequiredByte){	// in byte{
			bRes = true;
		}
		return bRes;
	}
	
	public static boolean doCheckCacheSize(Context context){
		try{
			if(null == sCheckCacheTask){
				sCheckCacheTask = new CacheSizeCheckTask(context).execute();
				return false;
			}
		}catch(RejectedExecutionException ex){
			Log.e(TAG, "doCheckCacheSize(), "+ex.toString());
		}
		return true;
	}
	
	static private AsyncTask sCheckCacheTask;
	
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
		Log.d(TAG, "checkCacheSize() +");
		checkInternalCache(context);
		if(canUseExternalStorage()){
			checkExternalCache(context);
		}
		Log.d(TAG, "checkCacheSize() -");
	}
	
	private static void checkInternalCache(Context context){
		try {
			File cacheFolder = new File(context.getCacheDir().getPath()+ File.separator + CACHE_FOLDER);
			if(null != cacheFolder && cacheFolder.exists()){
				File[] files = cacheFolder.listFiles();
				if(0 ==  files.length)
					return;
				
				if(canUseExternalStorage()){
					//try to migrate the internal data to external 
					Log.d(TAG, "checkInternalCache(), try to migrate the internal data to external");
					File cacheExtenalFolder = new File(context.getExternalCacheDir().getPath()+ File.separator + CACHE_FOLDER);
					if(null != cacheExtenalFolder){
						cacheExtenalFolder.mkdir();
						for(File file:files){
							if(null == file)
								continue;
							
							File fileExternal = new File(context.getExternalCacheDir().getPath()+ File.separator +file.getName());
							if(null != fileExternal && (!fileExternal.exists() || (file.lastModified() > fileExternal.lastModified()))){
								copyFile(file, fileExternal);
							}
							file.delete();
						}
					}
				}else{
					Log.d(TAG, "checkInternalCache(), Check if exceed the max cache size");
					//Check if exceed the max cache size
					Arrays.sort(files, fileLastModifiedComparator);
					
					long lTotalSize = 0;
					for(File file:files){
						if(MAX_CACHE_VOLUME < (lTotalSize + file.length())){
							file.delete();
						}else{
							lTotalSize+=file.length();
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void checkExternalCache(Context context){
		File cacheExtenalFolder = new File(context.getExternalCacheDir().getPath()+ File.separator + CACHE_FOLDER);
		if(null != cacheExtenalFolder && cacheExtenalFolder.exists()){
			File[] files = cacheExtenalFolder.listFiles();
			if(0 ==  files.length)
				return;
			
			Log.d(TAG, "checkExternalCache(), Check if exceed the max cache size");
			//Check if exceed the max cache size
			Arrays.sort(files, fileLastModifiedComparator);
			
			long lTotalSize = 0;
			for(File file:files){
				if(MAX_CACHE_VOLUME < (lTotalSize + file.length())){
					file.delete();
				}else{
					lTotalSize+=file.length();
				}
			}
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
}
