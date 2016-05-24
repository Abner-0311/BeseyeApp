package com.app.beseye.adapter;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.*;


import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.ota.BeseyeCamSWVersionMgr;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_VER_CHECK_STATUS;
import com.app.beseye.ota.CamSwUpdateRecord;
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
		public ViewGroup mVgCamOTAProgress;

		public ViewGroup mVgCamOTAFailed;
		public TextView mTxtCamName;
		public TextView mTxtMore;
		public RemoteImageView mImgThumbnail;
		public BeseyeSwitchBtn mSbCamOnOff;
		public Button mBtnOTAUpdate;
		public TextView mTxtCamUpdateDesc;
		
		public TextView mTxtCamUpdatePercetage;
		public ProgressBar mProgressBarCamUpdate;
		
		public TextView mTxtCamUpdateFailedDesc;
		public Button mBtnOTASupport;
		public Button mBtnOTAUpdateAgain;
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
				
				holder.mVgCamOTAProgress = (ViewGroup)convertView.findViewById(R.id.lv_update_progress_holder);
				holder.mProgressBarCamUpdate = (ProgressBar)convertView.findViewById(R.id.sb_update_progress);
				holder.mTxtCamUpdatePercetage = (TextView)convertView.findViewById(R.id.txt_update_progress);
				
				holder.mTxtCamUpdateDesc = (TextView)convertView.findViewById(R.id.txt_update_state);
				holder.mBtnOTASupport = (Button)convertView.findViewById(R.id.btn_ota_support);
				holder.mBtnOTAUpdateAgain = (Button)convertView.findViewById(R.id.btn_ota_update_again);
				holder.mTxtCamUpdateFailedDesc = (TextView)convertView.findViewById(R.id.txt_update_failed_desc);

				
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
				final String strVcamId = BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_ID);
				final CAM_CONN_STATUS connState = BeseyeJSONUtil.getVCamConnStatus(obj);//CAM_CONN_STATUS.toCamConnStatus(BeseyeJSONUtil.getJSONInt(obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, -1));
				final CamSwUpdateRecord camRec = BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL, strVcamId);
				
				CAM_UPDATE_STATUS camUpdateStatus = null != camRec?camRec.getUpdateStatus():null;
				
				//final boolean bIsOTARebootErrWhenOTAPrepareErr = null != camRec && camRec.isRebootErrWhenOTAPrepare();
				final boolean bIsOTAPoorNetworkErr = null != camRec && camRec.isPoorNetworkErrWhenOTA();

				final boolean bCanOTAUpdate = null != camUpdateStatus && camUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_VER_CHECKING) && camRec.getVerCheckStatus().equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_OUT_OF_DATE);
				final boolean bShowOTAUpdate = bCanOTAUpdate && !connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED) && !connState.equals(CAM_CONN_STATUS.CAM_INIT);
				
				final boolean bOTAError = null != camRec && 
								    //camRec.isOTATriggerredByThisDev() && 
								    ((0 != camRec.getErrCode() && camRec.getPrevUpdateStatus().equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING)/* && !camRec.isOTAFeedbackSent()*/) ||
								     (null != camUpdateStatus && camUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING) && camRec.isReachOTANoResponseTime())) ||
								     bIsOTAPoorNetworkErr;
				
				final boolean bOTAUpdating = !bOTAError && !bCanOTAUpdate && null != camUpdateStatus && camUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING);
				final boolean bOTAFinished = !bOTAError && !bOTAUpdating && null != camUpdateStatus && camUpdateStatus.equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATE_FINISH);
				
				
				if(bOTAFinished && camRec.isInOTAFinishPeriod() && !connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED)){
					camRec.setCamOnlineAfterOTATs(System.currentTimeMillis());
				}

				final boolean bItemFreezedDueToOTA =  bShowOTAUpdate || 
													  bOTAError || 
													  bOTAUpdating || 
													  (bOTAFinished && !camRec.isCamOnlineAfterOTA() && camRec.isInOTAFinishPeriod() && connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED));
				
				if(BeseyeConfig.DEBUG)
					Log.i(TAG, "CameraListAdapter::setupItem(), camRec:["+camRec+"], bCanOTAUpdate:"+bCanOTAUpdate+", bShowOTAUpdate:"+bShowOTAUpdate+", bOTAError:"+bOTAError+", bIsOTAPoorNetworkErr:"+bIsOTAPoorNetworkErr+"\n, bOTAUpdating:"+bOTAUpdating+", bOTAFinished:"+bOTAFinished+", connState:"+connState);
				
				// OTA normal section
				if(null != holder.mVgCamOTAState){
					BeseyeUtils.setVisibility(holder.mVgCamOTAState, (!bOTAError && (bShowOTAUpdate || bOTAUpdating || (bOTAFinished && camRec.isInOTAFinishPeriod() && connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED))))?View.VISIBLE:View.GONE);
				}
				
				if(null != holder.mBtnOTAUpdate){
					BeseyeUtils.setVisibility(holder.mBtnOTAUpdate, (!bOTAError && bCanOTAUpdate)?View.VISIBLE:View.GONE);
					holder.mBtnOTAUpdate.setOnClickListener(mItemOnClickListener);
					holder.mBtnOTAUpdate.setTag(holder);
				}
				
				BeseyeUtils.setVisibility(holder.mVgCamOTAProgress, (!bOTAError && (bOTAUpdating || bOTAFinished))?View.VISIBLE:View.GONE);
				BeseyeUtils.setText(holder.mTxtCamUpdateDesc, 
						            BeseyeUtils.getStringByResId(bCanOTAUpdate?R.string.desc_cam_update_keep_cam_on_before_ota:
						            	                                       (bOTAFinished?R.string.desc_cam_update_complete:
						            	                                    	             R.string.desc_cam_update_keep_cam_on_during_ota)));
				
				if(null != holder.mProgressBarCamUpdate){
					if(bOTAUpdating || bOTAFinished){
						int iPercentage = null != camRec?camRec.getUpdatePercentage():0;
						holder.mProgressBarCamUpdate.setProgress(iPercentage>=0?iPercentage:0);
						BeseyeUtils.setText(holder.mTxtCamUpdatePercetage, iPercentage+"%");
					}
				}
				
				// OTA failed section
				
				if(null != holder.mVgCamOTAFailed){
					BeseyeUtils.setVisibility(holder.mVgCamOTAFailed, bOTAError?View.VISIBLE:View.GONE);
				}
				
				BeseyeUtils.setText(holder.mTxtCamUpdateFailedDesc, 
									BeseyeUtils.getStringByResId(bIsOTAPoorNetworkErr?R.string.desc_cam_update_failed_poor_network:
																					  R.string.desc_cam_update_failed));
				
				if(null != holder.mBtnOTAUpdateAgain){
					BeseyeUtils.setVisibility(holder.mBtnOTAUpdateAgain, (bOTAError && bIsOTAPoorNetworkErr) ?View.VISIBLE:View.GONE);
					holder.mBtnOTAUpdateAgain.setOnClickListener(mItemOnClickListener);
					holder.mBtnOTAUpdateAgain.setTag(holder);
				}
				
				if(null != holder.mBtnOTASupport){
					BeseyeUtils.setVisibility(holder.mBtnOTASupport, bOTAError&& !bIsOTAPoorNetworkErr?View.VISIBLE:View.GONE);
					holder.mBtnOTASupport.setOnClickListener(mItemOnClickListener);
					holder.mBtnOTASupport.setTag(holder);
				}
				
				//Camera list general UI control
				
				
				if(null != holder.mTxtCamName){
					holder.mTxtCamName.setText(BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_NAME));
				}
				
				if(null != holder.mTxtMore){
					holder.mTxtMore.setVisibility(mbShowMore?View.VISIBLE:View.GONE);
					holder.mTxtMore.setOnClickListener(mItemOnClickListener);
					holder.mTxtMore.setTag(holder);
				}
				
				//BeseyeUtils.setVisibility(holder.mImgThumbnail, connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED)?View.INVISIBLE:View.VISIBLE);
				BeseyeUtils.setVisibility(holder.mVgCamOff, connState.equals(CAM_CONN_STATUS.CAM_OFF)  && !bItemFreezedDueToOTA?View.VISIBLE:View.GONE);
				BeseyeUtils.setVisibility(holder.mVgCamDisconnectedContent, connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED) && !bItemFreezedDueToOTA?View.VISIBLE:View.GONE);
				BeseyeUtils.setVisibility(holder.mVgCamDisconnected, (connState.equals(CAM_CONN_STATUS.CAM_INIT) || connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED)) || 
																	  bItemFreezedDueToOTA ?View.VISIBLE:View.GONE);

				if(null != holder.mSbCamOnOff){
					holder.mSbCamOnOff.setTag(holder);
					holder.mSbCamOnOff.setSwitchState(((bItemFreezedDueToOTA || connState.equals(CAM_CONN_STATUS.CAM_INIT) || connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED))?SwitchState.SWITCH_DISABLED:(connState.equals(CAM_CONN_STATUS.CAM_ON)?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF)));

					//holder.mSbCamOnOff.setSwitchState(connState.equals(CAM_CONN_STATUS.CAM_ON)?SwitchState.SWITCH_ON:(connState.equals(CAM_CONN_STATUS.CAM_OFF)?SwitchState.SWITCH_OFF:SwitchState.SWITCH_DISABLED));
					holder.mSbCamOnOff.setEnabled(bItemFreezedDueToOTA || connState.equals(CAM_CONN_STATUS.CAM_INIT) || connState.equals(CAM_CONN_STATUS.CAM_DISCONNECTED)?false:true);
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
				
				//convertView.setEnabled(connState.equals(CAM_CONN_STATUS.CAM_ON)?true:false);
//				convertView.setOnClickListener(mItemOnClickListener);
//				convertView.setClickable(true);
				
				holder.mObjCam = obj;
				convertView.setClickable(!bItemFreezedDueToOTA);
				convertView.setTag(holder);
			}
		}
	}

}
