package com.app.beseye.adapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteImageView;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class EventListAdapter extends BeseyeJSONAdapter {
	private OnSwitchBtnStateChangedListener mOnSwitchBtnStateChangedListener;
	
	public EventListAdapter(Context context, JSONArray list, int iLayoutId,
			OnClickListener itemOnClickListener, OnSwitchBtnStateChangedListener onSwitchBtnStateChangedListener) {
		super(context, list, iLayoutId, itemOnClickListener);
		mOnSwitchBtnStateChangedListener = onSwitchBtnStateChangedListener;
	}

	static public class EventListItmHolder{
		public TextView mTxtCamName;
		public RemoteImageView mImgThumbnail;
		public BeseyeSwitchBtn mSbCamOnOff;
		public JSONObject mObjCam;
	}
	
	@Override
	protected View inflateItem(int iPosition, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = mInflater.inflate(miLayoutId, null);
			if(null != convertView){
				EventListItmHolder holder = new EventListItmHolder();
				holder.mTxtCamName = (TextView)convertView.findViewById(R.id.tv_camera_name);
				
				holder.mSbCamOnOff = (BeseyeSwitchBtn)convertView.findViewById(R.id.sb_camera_switch);
				if(null != holder.mSbCamOnOff){
					holder.mSbCamOnOff.setOnSwitchBtnStateChangedListener(mOnSwitchBtnStateChangedListener);
				}
				
				holder.mImgThumbnail = (RemoteImageView)convertView.findViewById(R.id.iv_cameralist_thumbnail);
				
				convertView.setOnClickListener(mItemOnClickListener);
				convertView.setTag(holder);
			}
		}
		return convertView;
	}

	@Override
	protected void setupItem(int iPosition, View convertView, ViewGroup parent, JSONObject obj) {
		if(null != convertView){
			EventListItmHolder holder = (EventListItmHolder)convertView.getTag();
			if(null != holder){
				if(null != holder.mTxtCamName){
					holder.mTxtCamName.setText(genDetectionType(BeseyeJSONUtil.getJSONArray(obj, BeseyeJSONUtil.MM_TYPE_IDS)));
				}
				
				if(null != holder.mSbCamOnOff){
					holder.mSbCamOnOff.setTag(holder);
					holder.mSbCamOnOff.setSwitchState(SwitchState.SWITCH_ON);
				}
				
				holder.mObjCam = obj;
				convertView.setTag(holder);
			}
		}
	}
	
	private String genDetectionType(JSONArray typeArr){
		String strRet = "";
		int iCount = typeArr.length();
		for(int i = 0;i< iCount;i++){
			int iType;
			try {
				String strType = null;
				iType = typeArr.getInt(i);
				if(1 == iType){
					strType = "Family Dectection";
				}else if(2 == iType){
					strType = "Fire Dectection";
				}else if(3 == iType){
					strType = "Sound Dectection";
				}else if(4 == iType){
					strType = "Motion Dectection";
				}
				
				strRet += (i == 0)?strType:(" & "+strType);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return strRet;
	}
}
