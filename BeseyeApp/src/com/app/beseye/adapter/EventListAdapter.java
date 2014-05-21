package com.app.beseye.adapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteImageView;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class EventListAdapter extends BeseyeJSONAdapter {
	private int miSelectedImt = 0;
	
	public EventListAdapter(Context context, JSONArray list, int iLayoutId,
			OnClickListener itemOnClickListener) {
		super(context, list, iLayoutId, itemOnClickListener);
	}

	static public class EventListItmHolder{
		public TextView mTxtEventType;
		public RemoteImageView mImgThumbnail;
		public ImageView mImgDot;
		public ImageView mImgFace;
		public ImageView mImgFire;
		public ImageView mImgSound;
		public ImageView mImgMotion;
		public JSONObject mObjEvent;
	}
	
	public boolean setSelectedItm(int iItm){
		boolean bRet = miSelectedImt!=iItm;
		miSelectedImt = iItm;
		return bRet;
	}
	
	@Override
	protected View inflateItem(int iPosition, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = mInflater.inflate(miLayoutId, null);
			if(null != convertView){
				EventListItmHolder holder = new EventListItmHolder();
				holder.mTxtEventType = (TextView)convertView.findViewById(R.id.tv_eventlist_event_name);
				
				holder.mImgThumbnail = (RemoteImageView)convertView.findViewById(R.id.iv_timeline_video_thumbnail);
				
				holder.mImgDot = (ImageView)convertView.findViewById(R.id.iv_timeline_dot_greenblue);
				holder.mImgFace = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_face);
				holder.mImgFire = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_fire);
				holder.mImgSound = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_sound);
				holder.mImgMotion = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_motion);
				
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
				genDetectionType(holder, BeseyeJSONUtil.getJSONArray(obj, BeseyeJSONUtil.MM_TYPE_IDS));
				convertView.setBackgroundResource((iPosition%2 == 0)?R.drawable.cl_event_itm_bg_gray_color:R.drawable.cl_event_itm_bg_white_color);
				if(null != holder.mImgDot){
					holder.mImgDot.setImageResource(0 == iPosition?R.drawable.eventlist_timeline_point_bluegreen:R.drawable.eventlist_timeline_point_gray);
				}
				
				if(0 == iPosition){
					if(null != holder.mTxtEventType){
						holder.mTxtEventType.setText(R.string.event_itm_live);
					}
				}
				holder.mObjEvent = obj;
				convertView.setTag(holder);
			}
		}
	}
	
	private String genDetectionType(EventListItmHolder holder, JSONArray typeArr){
		String strRet = "";
		BeseyeUtils.setVisibility(holder.mImgFace, View.INVISIBLE);
		BeseyeUtils.setVisibility(holder.mImgFire, View.INVISIBLE);
		BeseyeUtils.setVisibility(holder.mImgSound, View.INVISIBLE);
		BeseyeUtils.setVisibility(holder.mImgMotion, View.INVISIBLE);
		if(null != typeArr){
			int iCount = typeArr.length();
			for(int i = 0;i< iCount;i++){
				int iType;
				try {
					String strType = null;
					iType = typeArr.getInt(i);
					if(1 == iType){
						strType = "Family Dectection";
						BeseyeUtils.setVisibility(holder.mImgFace, View.VISIBLE);
					}else if(2 == iType){
						strType = "Fire Dectection";
						BeseyeUtils.setVisibility(holder.mImgFire, View.VISIBLE);
					}else if(3 == iType){
						strType = "Sound Dectection";
						BeseyeUtils.setVisibility(holder.mImgSound, View.VISIBLE);
					}else if(4 == iType){
						strType = "Motion Dectection";
						BeseyeUtils.setVisibility(holder.mImgMotion, View.VISIBLE);
					}
					
					strRet += (i == 0)?strType:(" & "+strType);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			if(null != holder.mTxtEventType){
				holder.mTxtEventType.setText(strRet);
			}
		}
		
		return strRet;
	}
}
