package com.app.beseye.adapter;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeIMPMMBEHttpTask;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteImageView;
import com.app.beseye.widget.RemoteImageView.RemoteImageCallback;

public class HumanDetectTrainPicAdapter extends BeseyeJSONAdapter{
	static final public int NUM_OF_SUB_ITM = 4;
	private int miThumbnailWidth;
	//private int miBlockWidth;
	private int miMarginWidth;
	private String mStrVCamId = null;
	private RemoteImageCallback mRemoteImageCallback = null;
	
	public HumanDetectTrainPicAdapter(Context context, JSONArray list, int iLayoutId, OnClickListener itemOnClickListener, RemoteImageCallback remoteImageCallback) {
		super(context, list, iLayoutId, itemOnClickListener);
		if(DEBUG){
			Log.i(TAG, "context.getResources().getDimension(R.dimen.human_detect_pic_margin):"+context.getResources().getDimension(R.dimen.human_detect_pic_margin));
		}
		miMarginWidth = (int) context.getResources().getDimension(R.dimen.human_detect_pic_margin);
		//miBlockWidth = (int)  ((BeseyeUtils.getDeviceWidth((Activity)context)) - miMarginWidth) / NUM_OF_SUB_ITM;
		miThumbnailWidth = (int) ((BeseyeUtils.getDeviceWidth((Activity)context) - miMarginWidth*(NUM_OF_SUB_ITM+1)))/NUM_OF_SUB_ITM; 
		mRemoteImageCallback = remoteImageCallback;
		
		if(DEBUG)
			Log.i(TAG, "miThumbnailWidth:"+miThumbnailWidth);
	}
	
	static public class HumanDetectTrainItmHolder{
		public ViewGroup mVgHumanDetectTrainSubItm;
		public ImageView mIvHumanDetectTrainSubItmMask;
		public TextView mTxtNoHuman;
		public RemoteImageView mImgTrainPic;
		public ImageView mImgPicBorder;
		public JSONObject mObjCam;
	}
	
	public void setVCamid(String id){
		mStrVCamId = id;
	}
	
	@Override
	public int getCount() {
		return (null != mArrList)?(mArrList.length()+NUM_OF_SUB_ITM -1)/NUM_OF_SUB_ITM:0;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = inflateItem(position, convertView, parent);
		}
		
		if(null != convertView){
			int iStartIdx = position*NUM_OF_SUB_ITM;
			final int iCount = (getCount()-1 == position)?mArrList.length()-iStartIdx:NUM_OF_SUB_ITM;
			JSONObject[] objs = new JSONObject[iCount];
			
			Log.i(TAG, "getView(), position:"+position+", iStartIdx:"+iStartIdx+", iCount:"+iCount);

			for(int idx = 0; idx < iCount ;idx++){
				objs[idx] = mArrList.optJSONObject(iStartIdx+idx);
			}
			setupItem(position, convertView, parent, objs);
		}
		
		return convertView;
	}
	
	private void inflateSubItm(View convertView, HumanDetectTrainItmHolder holder, int iVgId){
		if(null != holder){
			holder.mVgHumanDetectTrainSubItm = (ViewGroup)convertView.findViewById(iVgId);
			if(null != holder.mVgHumanDetectTrainSubItm){
				int iThumbnailHeight = -1;
				holder.mIvHumanDetectTrainSubItmMask = (ImageView)holder.mVgHumanDetectTrainSubItm.findViewById(R.id.iv_human_pic_mask);
				if(null != holder.mIvHumanDetectTrainSubItmMask){
					iThumbnailHeight = BeseyeUtils.setThumbnailRatio(holder.mIvHumanDetectTrainSubItmMask, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_2_1);
				}
				
				holder.mImgTrainPic = (RemoteImageView)holder.mVgHumanDetectTrainSubItm.findViewById(R.id.iv_training_pic);
				if(null != holder.mImgTrainPic){
					//BeseyeUtils.setThumbnailRatio(holder.mImgTrainPic, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_2_1);
					BeseyeUtils.setWidthAndHeight(holder.mImgTrainPic, miThumbnailWidth, iThumbnailHeight);
				}
				
				holder.mImgPicBorder = (ImageView)holder.mVgHumanDetectTrainSubItm.findViewById(R.id.iv_human_pic_border);
				if(null != holder.mImgPicBorder){
					//BeseyeUtils.setThumbnailRatio(holder.mImgPicBorder, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_2_1);
					BeseyeUtils.setWidthAndHeight(holder.mImgPicBorder, miThumbnailWidth, iThumbnailHeight);
				}
				
				holder.mTxtNoHuman = (TextView)holder.mVgHumanDetectTrainSubItm.findViewById(R.id.txt_human_pic_no_human);
				if(null != holder.mTxtNoHuman){
					BeseyeUtils.setWidthAndHeight(holder.mTxtNoHuman, miThumbnailWidth, -1);
				}
				
				if(null != holder.mVgHumanDetectTrainSubItm){
					BeseyeUtils.setWidthAndHeight(holder.mVgHumanDetectTrainSubItm, miThumbnailWidth+miMarginWidth, iThumbnailHeight+miMarginWidth);
				}
				holder.mVgHumanDetectTrainSubItm.setOnClickListener(mItemOnClickListener);
				holder.mVgHumanDetectTrainSubItm.setTag(holder);
			}
		}
	}
	
	@Override
	protected View inflateItem(int iPosition, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = mInflater.inflate(miLayoutId, null);
			if(null != convertView){
				HumanDetectTrainItmHolder[] holder = new HumanDetectTrainItmHolder[NUM_OF_SUB_ITM];
				holder[0] = new HumanDetectTrainItmHolder();
				inflateSubItm(convertView, holder[0] , R.id.vg_train_pic_1);
				
				holder[1] = new HumanDetectTrainItmHolder();
				inflateSubItm(convertView, holder[1] , R.id.vg_train_pic_2);
				
				holder[2] = new HumanDetectTrainItmHolder();
				inflateSubItm(convertView, holder[2] , R.id.vg_train_pic_3);
				
				holder[3] = new HumanDetectTrainItmHolder();
				inflateSubItm(convertView, holder[3] , R.id.vg_train_pic_4);
				
				convertView.setTag(holder);
			}
		}
		return convertView;
	}
	
	private void setupSubItm(HumanDetectTrainItmHolder holder, JSONObject obj){
		if(null != holder){
			if(null != holder.mVgHumanDetectTrainSubItm){
				final String strPath = BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.MM_HD_IMG_PATH);
				boolean bImgPreLoaded = BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.MM_HD_IMG_PRELOAD_LOADED, false);
				boolean bImgLoaded = BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.MM_HD_IMG_LOADED, false);
				boolean bImgDeleted = null != obj && BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.MM_HD_IMG_DELETE, false);
				boolean bImgFailed = BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.MM_HD_IMG_LOAD_FAILED, false);
				if(null != holder.mImgTrainPic && null != strPath && 0 < strPath.length()){
					if(bImgFailed){
						holder.mImgTrainPic.cancelRemoteImageLoad();
						holder.mImgTrainPic.setImageResource(R.drawable.h_detection_fail_loading_image);
					}else if(bImgPreLoaded){
						holder.mImgTrainPic.setURI(BeseyeIMPMMBEHttpTask.getRefineImgPath(strPath), R.drawable.h_detection_loading_image, mStrVCamId, mRemoteImageCallback);
						holder.mImgTrainPic.disableLoadLastImgByVCamId();
						holder.mImgTrainPic.disablebBmpTransitionEffect();
						holder.mImgTrainPic.loadImage();
					}else{
						holder.mImgTrainPic.cancelRemoteImageLoad();
						holder.mImgTrainPic.setImageResource(R.drawable.h_detection_loading_image);
					}					
				}
				holder.mObjCam = obj;
				holder.mVgHumanDetectTrainSubItm.setTag(holder);
				
				BeseyeUtils.setVisibility(holder.mImgPicBorder, 				((bImgPreLoaded || bImgLoaded) && !bImgDeleted)?View.VISIBLE:View.INVISIBLE);
				BeseyeUtils.setVisibility(holder.mIvHumanDetectTrainSubItmMask, (bImgLoaded && bImgDeleted)?View.VISIBLE:View.INVISIBLE);
				BeseyeUtils.setVisibility(holder.mTxtNoHuman, 					(bImgLoaded && bImgDeleted)?View.VISIBLE:View.INVISIBLE);				
				BeseyeUtils.setVisibility(holder.mVgHumanDetectTrainSubItm, 	(null != obj)?View.VISIBLE:View.INVISIBLE);
				
				if(BeseyeConfig.DEBUG)
					Log.i(TAG, "setupSubItm(), strPath:"+strPath+", ("+bImgPreLoaded+","+bImgLoaded+","+bImgDeleted+",:"+bImgFailed+")");
			}
		}
	}

	protected void setupItem(int iPosition, View convertView, ViewGroup parent, JSONObject[] objs) {
		if(null != convertView){
			HumanDetectTrainItmHolder[] holder = (HumanDetectTrainItmHolder[])convertView.getTag();
			if(null != holder){
				for(int idx = 0; idx < NUM_OF_SUB_ITM;idx++){
					setupSubItm(holder[idx], (objs.length > idx)?objs[idx]:null);
				}
				convertView.setTag(holder);
			}
		}
	}

	@Override
	protected void setupItem(int iPosition, View convertView, ViewGroup parent,
			JSONObject obj) {
	}
}
