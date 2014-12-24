package com.app.beseye.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

public class BeseyeCamInfoSyncMgr {
	static private BeseyeCamInfoSyncMgr sBeseyeCamInfoSyncMgr;
	
	static public BeseyeCamInfoSyncMgr getInstance(){
		if(null == sBeseyeCamInfoSyncMgr){
			sBeseyeCamInfoSyncMgr = new BeseyeCamInfoSyncMgr();
		}
		return sBeseyeCamInfoSyncMgr;
	}
	
	private List<WeakReference<OnCamInfoChangedListener>> mLstOnCamInfoChangedListener;
	private Map<String, JSONObject> mMapCamInfo;
	
	private BeseyeCamInfoSyncMgr() {
		mLstOnCamInfoChangedListener = new ArrayList<WeakReference<OnCamInfoChangedListener>>();
		mMapCamInfo = new HashMap<String, JSONObject>();
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
	
	synchronized public JSONObject getCamInfoByVCamId(String strVcamId){
		JSONObject objRet = null;
		if(null != strVcamId && 0 < strVcamId.length() && null != mMapCamInfo){
			objRet= mMapCamInfo.get(strVcamId);
		}
		return objRet;
	}
	
	synchronized public void removeCamInfoByVCamId(String strVcamId){
		if(null != strVcamId && 0 < strVcamId.length() && null != mMapCamInfo){
			mMapCamInfo.remove(strVcamId);
		}
	}
	
	synchronized public Set<String> getVCamIdList(){
		return (null != mMapCamInfo)?mMapCamInfo.keySet():null;
	}
	
	synchronized public void updateCamInfo(final String strVcamId, final JSONObject objCamSetup){
		final List<WeakReference<OnCamInfoChangedListener> > copy = new ArrayList<WeakReference<OnCamInfoChangedListener>>(mLstOnCamInfoChangedListener);
		if(null != mMapCamInfo){
			mMapCamInfo.put(strVcamId, objCamSetup);
		}
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				for(WeakReference<OnCamInfoChangedListener> listenerChk : copy){
					OnCamInfoChangedListener listener = (null != listenerChk)?listenerChk.get():null;
					if(null != listener){
						listener.onCamSetupChanged(strVcamId, BeseyeJSONUtil.getJSONLong(objCamSetup, BeseyeJSONUtil.OBJ_TIMESTAMP), objCamSetup);
					}
				}
				
				if(false == BeseyeJSONUtil.getJSONBoolean(objCamSetup, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
					if(null != mMapCamInfo){
						mMapCamInfo.remove(strVcamId);
					}
				}
			}}, 0);
	}
}
