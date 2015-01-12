package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;
import android.util.Log;

public class BeseyeMemCache {
	static private LruCache<String, Bitmap> sMemoryCache;
	static private int sMemClass;
	
	static public void init(Context context){
		// Get memory class of this device, exceeding this amount will throw an
	    // OutOfMemory exception.
		sMemClass = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

	    // Use 1/8th of the available memory for this memory cache.
	    final int cacheSize = 1024 * 1024 * sMemClass / 6;
	    if(DEBUG)
	    	Log.i(TAG, "BeseyeMemCache::init(), cacheSize, = "+cacheSize);
	    	
	    sMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            // The cache size will be measured in bytes rather than number of items.
	            return bitmap.getRowBytes()*bitmap.getHeight();
	        }
	    };
	    
//	    //Workaround
//	    Byte[] arrByte = new Byte[cacheSize];
//	    if(null != arrByte)
//	    	arrByte = null;
	}
	
	static public int getMemClass(){return sMemClass;}
	
	static public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
	    if (null != key && getBitmapFromMemCache(key) == null) {
	    	sMemoryCache.put(key, bitmap);
//	    	if(BeseyeConfig.DEBUG)
//	    		Log.i(TAG, "BeseyeMemCache::addBitmapToMemoryCache(), size, = "+(bitmap.getRowBytes()*bitmap.getHeight())/1024+", for ["+key+"]");
	    }
	}

	static public Bitmap getBitmapFromMemCache(String key) {
	    return (null != key)?sMemoryCache.get(key):null;
	}
	
	static private final String RES_PREFIX = "res_drawable_";
	static public Bitmap getBmpByResId(Context context, int iResId, int iDeisreWidth, int iDesireHeight){
		Bitmap bmp = sMemoryCache.get(RES_PREFIX+iResId);
		if(null == bmp){
			BitmapFactory.Options option= new BitmapFactory.Options();
			if(null != option){
				if(0 < iDeisreWidth && 0 < iDesireHeight){
					option.inJustDecodeBounds = true;
					BitmapFactory.decodeResource(context.getResources(), iResId, option);
					if(option.outWidth > iDeisreWidth &&  option.outHeight > iDesireHeight){
						if(DEBUG)
							Log.i(TAG, "getBmpByResId(), downDample, ("+option.outWidth+", "+option.outHeight+"), desire:("+iDeisreWidth+", "+iDesireHeight+")");
						option.inSampleSize = 2;
					}
					option.inJustDecodeBounds = false;
				}
				bmp = BitmapFactory.decodeResource(context.getResources(), iResId, option);
				if(null != bmp){
					addBitmapToMemoryCache(RES_PREFIX+iResId, bmp);
				}
			}
		}
	    return bmp;
	}
}
