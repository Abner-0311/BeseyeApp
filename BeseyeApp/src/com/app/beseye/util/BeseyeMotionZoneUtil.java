package com.app.beseye.util;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class BeseyeMotionZoneUtil {
    
	public final static double sdMinZoneRatio = 0.2;    
    public final static double sdConfidenceV = 0.95;		//for different device may have different double 
    public final static int siRatioMinV = 0;
    public final static int siRatioMaxV = 1;
    public final static String MOTION_ZONE_RATIO = "MOTION_ZONE_RATIO";
    public static final int siMaskAlpha = 153;	//255*0.6
    public static final int REQUEST_MOTION_ZONE_EDIT = 1001;
    
    public final static String[] ssStrObjKey = {BeseyeJSONUtil.MOTION_ZONE_LEFT, 
			   BeseyeJSONUtil.MOTION_ZONE_TOP,
			   BeseyeJSONUtil.MOTION_ZONE_RIGHT,
			   BeseyeJSONUtil.MOTION_ZONE_BOTTOM
			   };
    
	static public final double[] getMotionZoneFromServer(JSONObject cam_obj, String[] strObjKey){
		double[] r = {-1.0, -1.0, -1.0, -1.0};
		
		JSONArray motion_zone_array =  BeseyeJSONUtil.getJSONArray(BeseyeJSONUtil.getJSONObject(cam_obj, ACC_DATA), BeseyeJSONUtil.MOTION_ZONE);
		if(motion_zone_array == null){
			Log.e(TAG, "motion_zone_array is null");
		} else{
			JSONObject motion_zone_obj = null;
			try {
				motion_zone_obj = (JSONObject) motion_zone_array.get(0);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			if(null != motion_zone_obj){
				for(int idx = 0; idx < strObjKey.length; idx++){
					r[idx] = BeseyeJSONUtil.getJSONDouble(motion_zone_obj, strObjKey[idx]);
				}
			}
		}
		
		return r;
	}
	
	static public final boolean isMotionZoneRangeValiate(double[] r, int minV, int maxV, double minL, double confidenceV){
		boolean isValiate = true;

		for(int i=0; i<r.length; i++){
			if(minV > r[i] || maxV < r[i]){
				isValiate = false;
				break;
			}
		}
		if(isValiate){
			if( (r[3]-r[1]) < minL*confidenceV || (r[2]-r[0]) < minL*BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16*confidenceV){
				isValiate = false;
			}
		}
		
		return isValiate;
	}
	
	static public final double[] setDefaultRatio(double[] r){
		r[0] = siRatioMinV;
		r[1] = siRatioMinV;
		r[2] = siRatioMaxV;
		r[2] = siRatioMaxV;
		
		return r;
	}
}
