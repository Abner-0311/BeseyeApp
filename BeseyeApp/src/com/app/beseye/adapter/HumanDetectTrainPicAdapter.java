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

public class HumanDetectTrainPicAdapter extends BeseyeJSONAdapter {
	private int miThumbnailWidth;
	private int miBlockWidth;
	
	public HumanDetectTrainPicAdapter(Context context, JSONArray list, int iLayoutId, OnClickListener itemOnClickListener) {
		super(context, list, iLayoutId, itemOnClickListener);
		if(DEBUG){
			Log.i(TAG, "context.getResources().getDimension(R.dimen.cameralist_videoblock_margin):"+context.getResources().getDimension(R.dimen.cameralist_videoblock_margin));
			Log.i(TAG, "context.getResources().getDimension(cameralist_videoblock_thunmbnail_padding):"+context.getResources().getDimension(R.dimen.cameralist_videoblock_thunmbnail_padding));
		}
		miBlockWidth = (BeseyeUtils.getDeviceWidth((Activity)context)) / NUM_OF_SUB_ITM;
		miThumbnailWidth = (int) ((BeseyeUtils.getDeviceWidth((Activity)context) - (context.getResources().getDimension(R.dimen.cameralist_videoblock_margin))*2*NUM_OF_SUB_ITM))/NUM_OF_SUB_ITM; 
		if(DEBUG)
			Log.i(TAG, "miThumbnailWidth:"+miThumbnailWidth);
	}

	static final public int NUM_OF_SUB_ITM = 3;
	
	static public class HumanDetectTrainItmHolder{
		public ViewGroup mVgHumanDetectTrainSubItm;
		public ViewGroup mVgHumanDetectTrainSubItmMask;
		public RemoteImageView mImgTrainPic;
		public JSONObject mObjCam;
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
				if(null != holder.mVgHumanDetectTrainSubItm){
					BeseyeUtils.setThumbnailRatio(holder.mVgHumanDetectTrainSubItm, miBlockWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_2_1);
				}
				holder.mVgHumanDetectTrainSubItmMask = (ViewGroup)holder.mVgHumanDetectTrainSubItm.findViewById(R.id.rl_training_pic_mask);

				if(null != holder.mVgHumanDetectTrainSubItmMask){
					BeseyeUtils.setThumbnailRatio(holder.mVgHumanDetectTrainSubItmMask, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_2_1);
				}
				
				holder.mImgTrainPic = (RemoteImageView)holder.mVgHumanDetectTrainSubItm.findViewById(R.id.iv_training_pic);
				if(null != holder.mImgTrainPic){
					BeseyeUtils.setThumbnailRatio(holder.mImgTrainPic, miThumbnailWidth, BeseyeUtils.BESEYE_THUMBNAIL_RATIO_2_1);
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
				convertView.setTag(holder);
			}
		}
		return convertView;
	}
	
	private void setupSubItm(HumanDetectTrainItmHolder holder, JSONObject obj){
		if(null != holder){
			if(null != holder.mVgHumanDetectTrainSubItm){
				if(null != holder.mImgTrainPic){
					final String strPath = BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.MM_HD_IMG_PATH);
					holder.mImgTrainPic.setURI(strPath, R.drawable.cameralist_s_view_noview_bg, BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_ID));
					holder.mImgTrainPic.loadImage();
				}
				holder.mObjCam = obj;
				holder.mVgHumanDetectTrainSubItm.setTag(holder);
				
				BeseyeUtils.setVisibility(holder.mVgHumanDetectTrainSubItmMask, (null != obj && BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.MM_HD_IMG_DELETE, false))?View.VISIBLE:View.INVISIBLE);
				BeseyeUtils.setVisibility(holder.mVgHumanDetectTrainSubItm, (null != obj)?View.VISIBLE:View.INVISIBLE);
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
		// TODO Auto-generated method stub
		
	}

}
