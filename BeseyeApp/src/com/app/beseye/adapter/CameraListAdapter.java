package com.app.beseye.adapter;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteImageView;
import com.app.beseye.widget.RemoteImageView.RemoteImageCallback;

public class CameraListAdapter extends BeseyeJSONAdapter {
	private OnSwitchBtnStateChangedListener mOnSwitchBtnStateChangedListener;
	private int miThumbnailWidth;
	private boolean mbIsDemoCamList = false;
	private boolean mbShowMore = false;
	
	public CameraListAdapter(Context context, JSONArray list, int iLayoutId,
			OnClickListener itemOnClickListener, OnSwitchBtnStateChangedListener onSwitchBtnStateChangedListener) {
		super(context, list, iLayoutId, itemOnClickListener);
		mOnSwitchBtnStateChangedListener = onSwitchBtnStateChangedListener;
		if(DEBUG){
			Log.i(TAG, "context.getResources().getDimension(R.dimen.cameralist_videoblock_margin):"+context.getResources().getDimension(R.dimen.cameralist_videoblock_margin));
			Log.i(TAG, "context.getResources().getDimension(cameralist_videoblock_thunmbnail_padding):"+context.getResources().getDimension(R.dimen.cameralist_videoblock_thunmbnail_padding));
		}
		miThumbnailWidth = (int) (BeseyeUtils.getDeviceWidth((Activity)context) - (context.getResources().getDimension(R.dimen.cameralist_videoblock_margin)+context.getResources().getDimension(R.dimen.cameralist_videoblock_thunmbnail_padding))*2); 
		if(DEBUG)
			Log.i(TAG, "miThumbnailWidth:"+miThumbnailWidth);
	}

	static public class CameraListItmHolder{
		public ViewGroup mVgCamOff;
		public ViewGroup mVgCamDisconnected;
		public ViewGroup mVgCamDisconnectedContent;
		public ViewGroup mVgCamOTAState;
		public ViewGroup mVgCamOTAFailed;
		public TextView mTxtCamName;
		public TextView mTxtMore;
		public RemoteImageView mImgThumbnail;
		public BeseyeSwitchBtn mSbCamOnOff;
		public Button mBtnOTAUpdate;
		public Button mBtnOTASupport;
		public JSONObject mObjCam;
	}
	
	public void setIsDemoCamList(boolean bIsDemoCamList){
		mbIsDemoCamList = bIsDemoCamList;
	}
	
	public void setShowMore(boolean bShowMore){
		mbShowMore = bShowMore;
	}
	
	@Override
	protected View inflateItem(int iPosition, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = mInflater.inflate(miLayoutId, null);
			if(null != convertView){
				CameraListItmHolder holder = new CameraListItmHolder();
				holder.mTxtCamName = (TextView)convertView.findViewById(R.id.tv_camera_name);
				holder.mTxtMore = (TextView)convertView.findViewById(R.id.tv_camera_more);
				
				holder.mSbCamOnOff = (BeseyeSwitchBtn)convertView.findViewById(R.id.sb_camera_switch);
				if(null != holder.mSbCamOnOff){
					holder.mSbCamOnOff.setOnSwitchBtnStateChangedListener(mOnSwitchBtnStateChangedListener);
					holder.mSbCamOnOff.setVisibility((!mbIsDemoCamList)?View.VISIBLE:View.INVISIBLE);
				}
				
				holder.mImgThumbnail = (RemoteImageView)convertView.findViewById(R.id.iv_cameralist_thumbnail);
				if(null != holder.mImgThumbnail){
					BeseyeUtils.setThumbnailRatio(holder.mImgThumbnail, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
				}
				
				if(DEBUG)
					Log.i(TAG, "CameraListAdapter::inflateItem()+++++++++++++++++++++++++++, id:"+holder.mImgThumbnail);

				holder.mVgCamOff = (ViewGroup)convertView.findViewById(R.id.rl_cameralist_no_video);
				if(null != holder.mVgCamOff){
					BeseyeUtils.setThumbnailRatio(holder.mVgCamOff, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
				}
				
				holder.mVgCamDisconnected = (ViewGroup)convertView.findViewById(R.id.rl_camera_disconnected_solid);
				if(null != holder.mVgCamDisconnected){
					BeseyeUtils.setThumbnailRatio(holder.mVgCamDisconnected, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
				}
				
				holder.mVgCamDisconnectedContent= (ViewGroup)convertView.findViewById(R.id.rl_camera_disconnected_content);
				
				holder.mVgCamOTAState = (ViewGroup)convertView.findViewById(R.id.rl_cameralist_cam_ota);
				if(null != holder.mVgCamOTAState){
					BeseyeUtils.setThumbnailRatio(holder.mVgCamOTAState, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
				}
				
				holder.mBtnOTAUpdate = (Button)convertView.findViewById(R.id.btn_ota_update);
				holder.mBtnOTASupport = (Button)convertView.findViewById(R.id.btn_ota_support);
				
				holder.mVgCamOTAFailed = (ViewGroup)convertView.findViewById(R.id.rl_cameralist_cam_ota_failed);
				if(null != holder.mVgCamOTAFailed){
					BeseyeUtils.setThumbnailRatio(holder.mVgCamOTAFailed, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
				}
				
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
				
				if(null != holder.mTxtMore){
					holder.mTxtMore.setVisibility(mbShowMore?View.VISIBLE:View.GONE);
					holder.mTxtMore.setOnClickListener(mItemOnClickListener);
					holder.mTxtMore.setTag(holder);
				}
				
				if(null != holder.mBtnOTAUpdate){
					holder.mBtnOTAUpdate.setOnClickListener(mItemOnClickListener);
					holder.mBtnOTAUpdate.setTag(holder);
				}
				
				if(null != holder.mBtnOTASupport){
					holder.mBtnOTASupport.setOnClickListener(mItemOnClickListener);
					holder.mBtnOTASupport.setTag(holder);
				}
				
				BeseyeJSONUtil.CAM_CONN_STATUS connState = BeseyeJSONUtil.getVCamConnStatus(obj);//CAM_CONN_STATUS.toCamConnStatus(BeseyeJSONUtil.getJSONInt(obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, -1));
				
				//BeseyeUtils.setVisibility(holder.mImgThumbnail, connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED)?View.INVISIBLE:View.VISIBLE);
				BeseyeUtils.setVisibility(holder.mVgCamOff, connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_OFF)?View.VISIBLE:View.GONE);
				BeseyeUtils.setVisibility(holder.mVgCamDisconnectedContent, connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED)?View.VISIBLE:View.GONE);
				BeseyeUtils.setVisibility(holder.mVgCamDisconnected, (connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_INIT)||connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED))?View.VISIBLE:View.GONE);

				if(null != holder.mSbCamOnOff){
					holder.mSbCamOnOff.setTag(holder);
					holder.mSbCamOnOff.setSwitchState(connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_ON)?SwitchState.SWITCH_ON:(connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_OFF)?SwitchState.SWITCH_OFF:SwitchState.SWITCH_DISABLED));
					holder.mSbCamOnOff.setEnabled(connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_INIT)||connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_DISCONNECTED)?false:true);
					holder.mSbCamOnOff.setVisibility((!mbIsDemoCamList && BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.ACC_SUBSC_ADMIN))?View.VISIBLE:View.INVISIBLE);
				}
				
				//Log.i(TAG, "setupItem(), holder.mImgThumbnail:"+holder.mImgThumbnail.getWidth());
				if(/*!connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED) &&*/ null != holder.mImgThumbnail){
					final String strPath = BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_VCAM_THUMB);
					holder.mImgThumbnail.setURI(strPath, R.drawable.cameralist_s_view_noview_bg, BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_ID));

//					holder.mImgThumbnail.setURI(strPath, R.drawable.cameralist_s_view_noview_bg, BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_ID), new RemoteImageCallback(){
//						@Override
//						public void imageLoaded(boolean success) {
//							// TODO Auto-generated method stub
//							Log.i(TAG, "CameraListAdapter::setupItem(), strPath:"+strPath+", success:"+success);
//
//						}});
					holder.mImgThumbnail.loadImage();
				}
				
				//convertView.setEnabled(connState.equals(BeseyeJSONUtil.CAM_CONN_STATUS.CAM_ON)?true:false);
//				convertView.setOnClickListener(mItemOnClickListener);
//				convertView.setClickable(true);
				
				holder.mObjCam = obj;
				convertView.setTag(holder);
			}
		}
	}

}
