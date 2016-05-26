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
	
	public enum BESEYE_NEW_FEATURE{
		FEATURE_SCN_SHOT,
		FEATURE_FACE_RECG,
		FEATURE_SCHEDULE,
		FEATURE_COUNT
	}
	
	private SharedPreferences mPref;
	private NewFeatureConfig[] mNewFeatures;
//	private NewFeatureConfig mNewFeatureScreenshot;
//	private NewFeatureConfig mNewFeatureFaceRecognition;
//	private NewFeatureConfig mNewFeatureTriggerZone;
//	private NewFeatureConfig mNewFeatureHumanDetect;
	
	private BeseyeNewFeatureMgr(Context context){
		if(null != context){
			mPref = getSharedPreferences(context, FEATURE_PREF);
			initValues();
		}
	}
	
	private void initValues(){
		mNewFeatures = new NewFeatureConfig[BESEYE_NEW_FEATURE.FEATURE_COUNT.ordinal()];
		mNewFeatures[BESEYE_NEW_FEATURE.FEATURE_SCN_SHOT.ordinal()] = new NewFeatureConfig(mPref, "beseye_screen_feature", BeseyeUtils.stringToDate("2015-05-13-23-59-59","yyyy-MM-dd-HH-mm-ss"));
		mNewFeatures[BESEYE_NEW_FEATURE.FEATURE_FACE_RECG.ordinal()] = new NewFeatureConfig(mPref, "beseye_feature_face_recog", BeseyeUtils.stringToDate("2029-12-32-23-59-59","yyyy-MM-dd-HH-mm-ss"));
		mNewFeatures[BESEYE_NEW_FEATURE.FEATURE_SCHEDULE.ordinal()] = new NewFeatureConfig(mPref, "beseye_feature_cam_schedule", BeseyeUtils.stringToDate("2015-11-30-23-59-59","yyyy-MM-dd-HH-mm-ss"));
	}
	
	public void reset(){
		mPref.edit().clear().commit();
//		mNewFeatureScreenshot = null;
//		mNewFeatureFaceRecognition = null;
//		mNewFeatureTriggerZone = null;
//		mNewFeatureHumanDetect = null;
		for(int idx = 0; idx < BESEYE_NEW_FEATURE.FEATURE_COUNT.ordinal();idx++){
			mNewFeatures[idx] = null;
		}
		initValues();
	}
	
	public boolean isScreenshotFeatureClicked(){
		return mNewFeatures[BESEYE_NEW_FEATURE.FEATURE_SCN_SHOT.ordinal()].isUsed();
	}
	
	public void setScreenshotFeatureClicked(boolean bClicked){
		mNewFeatures[BESEYE_NEW_FEATURE.FEATURE_SCN_SHOT.ordinal()].setIsUsed(bClicked);
	}
	
	public boolean isFaceRecognitionOn(){
		return mNewFeatures[BESEYE_NEW_FEATURE.FEATURE_FACE_RECG.ordinal()].isUsed();
	}
	
	public void setFaceRecognitionOn(boolean bIsOn){
		mNewFeatures[BESEYE_NEW_FEATURE.FEATURE_FACE_RECG.ordinal()].setIsUsed(bIsOn);
	}
		
	public boolean isFeatureClicked(BESEYE_NEW_FEATURE feature){
		return mNewFeatures[feature.ordinal()].isUsed();
	}
	
	public void setFeatureClicked(BESEYE_NEW_FEATURE feature, boolean bClicked){
		mNewFeatures[feature.ordinal()].setIsUsed(bClicked);
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
