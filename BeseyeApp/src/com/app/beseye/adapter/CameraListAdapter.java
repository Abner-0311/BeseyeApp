package com.app.beseye.adapter;

import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONArray;
import org.json.JSONObject;

import com.app.beseye.R;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteImageView;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class CameraListAdapter extends BeseyeJSONAdapter {
	private OnSwitchBtnStateChangedListener mOnSwitchBtnStateChangedListener;
	
	public CameraListAdapter(Context context, JSONArray list, int iLayoutId,
			OnClickListener itemOnClickListener, OnSwitchBtnStateChangedListener onSwitchBtnStateChangedListener) {
		super(context, list, iLayoutId, itemOnClickListener);
		mOnSwitchBtnStateChangedListener = onSwitchBtnStateChangedListener;
	}

	static public class CameraListItmHolder{
		public ViewGroup mVgCamOff;
		public ViewGroup mVgCamDisconnected;
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
				CameraListItmHolder holder = new CameraListItmHolder();
				holder.mTxtCamName = (TextView)convertView.findViewById(R.id.tv_camera_name);
				
				holder.mSbCamOnOff = (BeseyeSwitchBtn)convertView.findViewById(R.id.sb_camera_switch);
				if(null != holder.mSbCamOnOff){
					holder.mSbCamOnOff.setOnSwitchBtnStateChangedListener(mOnSwitchBtnStateChangedListener);
				}
				
				holder.mImgThumbnail = (RemoteImageView)convertView.findViewById(R.id.iv_cameralist_thumbnail);
				
				holder.mVgCamOff = (ViewGroup)convertView.findViewById(R.id.rl_cameralist_no_video);
				holder.mVgCamDisconnected = (ViewGroup)convertView.findViewById(R.id.rl_camera_disconnected_solid);
				
				convertView.setOnClickListener(mItemOnClickListener);
				convertView.setTag(holder);
			}
		}
		return convertView;
	}

	@Override
	protected void setupItem(int iPosition, View convertView, ViewGroup parent, JSONObject obj) {
		if(null != convertView){
			CameraListItmHolder holder = (CameraListItmHolder)convertView.getTag();
			if(null != holder){
				if(null != holder.mTxtCamName){
					holder.mTxtCamName.setText(BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_NAME));
				}
				
				CAM_CONN_STATUS connState = CAM_CONN_STATUS.toCamConnStatus(BeseyeJSONUtil.getJSONInt(obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, -1));
				
				BeseyeUtils.setVisibility(holder.mImgThumbnail, connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED)?View.INVISIBLE:View.VISIBLE);
				BeseyeUtils.setVisibility(holder.mVgCamOff, connState.equals(CAM_CONN_STATUS.CAM_OFF)?View.VISIBLE:View.GONE);
				BeseyeUtils.setVisibility(holder.mVgCamDisconnected, connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED)?View.VISIBLE:View.GONE);
				
				if(null != holder.mSbCamOnOff){
					holder.mSbCamOnOff.setTag(holder);
					holder.mSbCamOnOff.setSwitchState(connState.equals(CAM_CONN_STATUS.CAM_ON)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF);
					holder.mSbCamOnOff.setEnabled(connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED)?false:true);
				}
				
				if(null != holder.mImgThumbnail){
					//Log.i(TAG, "setupItem(), name"+BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_NAME)+", path="+BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_VCAM_THUMB));
					holder.mImgThumbnail.setURI(BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_VCAM_THUMB), R.drawable.cameralist_thumbnail);
					holder.mImgThumbnail.loadImage();
				}
				
				convertView.setEnabled((BeseyeConfig.COMPUTEX_DEMO && connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED))?false:true);
				
				holder.mObjCam = obj;
				convertView.setTag(holder);
			}
		}
	}

}
