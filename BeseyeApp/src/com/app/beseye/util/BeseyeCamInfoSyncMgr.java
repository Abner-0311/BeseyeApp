package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import android.util.Log;

public class BeseyeCamInfoSyncMgr {
	static private BeseyeCamInfoSyncMgr sBeseyeCamInfoSyncMgr;
	
	static public BeseyeCamInfoSyncMgr getInstance(){
		if(null == sBeseyeCamInfoSyncMgr){
			sBeseyeCamInfoSyncMgr = new BeseyeCamInfoSyncMgr();
		}
		return sBeseyeCamInfoSyncMgr;
	}
	
	private List<WeakReference<OnCamInfoChangedListener>> mLstOnCamInfoChangedListener;
	
	private BeseyeCamInfoSyncMgr() {
		mLstOnCamInfoChangedListener = new ArrayList<WeakReference<OnCamInfoChangedListener>>();
	}
	
	static public interface OnCamInfoChangedListener{
		public void onCamSetupChanged(String strVcamId, long lTs, JSONObject objCamSetup);
	}
	
	synchronized public void registerOnCamInfoChangedListener(OnCamInfoChangedListener listener){
		//Log.i(TAG, "registerOnCamInfoChangedListener(), listener:"+listener);
		if(null != listener && null == findOnCamInfoChangedListener(listener)){
			//Log.i(TAG, "registerOnCamInfoChangedListener(), listener add OK");
			mLstOnCamInfoChangedListener.add(new WeakReference<OnCamInfoChangedListener>(listener));
		}
	}
	
	synchronized public void unregisterOnCamInfoChangedListener(OnCamInfoChangedListener listener){
		WeakReference<OnCamInfoChangedListener> ret = null;
		if(null != listener && null != (ret = findOnCamInfoChangedListener(listener))){
			mLstOnCamInfoChangedListener.remove(ret);
		}
	}
	
	synchronized private WeakReference<OnCamInfoChangedListener> findOnCamInfoChangedListener(OnCamInfoChangedListener listener){
		WeakReference<OnCamInfoChangedListener> ret = null;
		if(null != listener){
			for(WeakReference<OnCamInfoChangedListener> listenerChk : mLstOnCamInfoChangedListener){
				if(null != listenerChk && listener.equals(listenerChk.get())){
					ret = listenerChk;
					break;
				}
			}
		}	
		//Log.i(TAG, "findOnCamInfoChangedListener(), ret : "+((null != ret)?ret.get().toString():""));
		return ret;
	} 
	
	synchronized public void updateCamInfo(final String strVcamId, final JSONObject objCamSetup){
		final List<WeakReference<OnCamInfoChangedListener> > copy = new ArrayList<WeakReference<OnCamInfoChangedListener>>(mLstOnCamInfoChangedListener);
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				for(WeakReference<OnCamInfoChangedListener> listenerChk : copy){
					OnCamInfoChangedListener listener = (null != listenerChk)?listenerChk.get():null;
					if(null != listener){
						listener.onCamSetupChanged(strVcamId, BeseyeJSONUtil.getJSONLong(objCamSetup, BeseyeJSONUtil.OBJ_TIMESTAMP), objCamSetup);
					}
				}
			}}, 0);
	}
}
