package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getPrefBooleanValue;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getSecuredSharedPreferences;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.getSharedPreferences;
import static com.app.beseye.util.BeseyeSharedPreferenceUtil.setPrefBooleanValue;

import java.util.Date;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class BeseyeNewFeatureMgr {
	static private final String FEATURE_PREF 				= "beseye_feature";
	static private BeseyeNewFeatureMgr mBeseyeNewFeatureMgr;
	
	
	static public void createInstance(Context context){
		mBeseyeNewFeatureMgr = new BeseyeNewFeatureMgr(context);
	}
	
	static public BeseyeNewFeatureMgr getInstance(){
		return mBeseyeNewFeatureMgr;
	}
	
	private SharedPreferences mPref, mSecuredPref;
	private NewFeatureConfig mNewFeatureScreenshot;
	
	private BeseyeNewFeatureMgr(Context context){
		if(null != context){
			mPref = getSharedPreferences(context, FEATURE_PREF);
			mSecuredPref = getSecuredSharedPreferences(context, FEATURE_PREF);
			initValues();
		}
	}
	
	private void initValues(){
		mNewFeatureScreenshot = new NewFeatureConfig(mPref, "beseye_screen_feature", BeseyeUtils.stringToDate("2015-05-13-23-59-59","yyyy-MM-dd-HH-mm-ss"));
	}
	
	public void reset(){
		mPref.edit().clear().commit();
		mNewFeatureScreenshot = null;
		initValues();
	}
	
	public boolean isScreenshotFeatureClicked(){
		return mNewFeatureScreenshot.isUsed();
	}
	
	public void setScreenshotFeatureClicked(boolean bClicked){
		mNewFeatureScreenshot.setIsUsed(bClicked);
	}
	
	static public class NewFeatureConfig{
		private SharedPreferences mPref;
		private String mStrPrefKey;
		private Date mDateExpire = new Date();
		private boolean mbUsed = false;
		
		public NewFeatureConfig (SharedPreferences pref, String strPrefKey, Date dateExpire){
			mPref = pref;
			mStrPrefKey = strPrefKey;
			mDateExpire = dateExpire;
			mbUsed = getPrefBooleanValue(mPref, mStrPrefKey, false);
			if(BeseyeConfig.DEBUG){
				Log.e(TAG, "NewFeatureConfig(), "+toString());
			}
			checkExpired();
		}
		
		public String getPrefKey(){
			return mStrPrefKey;
		}
		
		public Date getExpiteDate(){
			return mDateExpire;
		}
		
		public boolean isUsed(){
			checkExpired();
			return mbUsed;
		}
		
		public void setIsUsed(boolean bUsed){
			mbUsed = bUsed;
			setPrefBooleanValue(mPref, mStrPrefKey, bUsed);
		}
		
		private void checkExpired(){
			if(null != mDateExpire && 0 > mDateExpire.compareTo(new Date())){
				setIsUsed(true);
				if(BeseyeConfig.DEBUG){
					Log.e(TAG, "checkExpired(), mStrPrefKey is expired. "+toString());
				}
			}
		}
		
		public String toString(){
			return "Key:"+mStrPrefKey+", Date:"+mDateExpire.toLocaleString()+", mbUsed:"+mbUsed;
		}
	}
}
