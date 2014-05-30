package com.app.beseye.adapter;

import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.RemoteGifImageView;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
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
		public RemoteGifImageView mImgThumbnail;
		public ImageView mImgDot;
		public ImageView mImgFace;
		public ImageView mImgFire;
		public ImageView mImgSound;
		public ImageView mImgMotion;
		public TextView mBtnGoLive;
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
				
				holder.mImgThumbnail = (RemoteGifImageView)convertView.findViewById(R.id.iv_timeline_video_thumbnail);
				
				holder.mImgDot = (ImageView)convertView.findViewById(R.id.iv_timeline_dot_greenblue);
				holder.mImgFace = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_face);
				holder.mImgFire = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_fire);
				holder.mImgSound = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_sound);
				holder.mImgMotion = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_motion);
				
				holder.mBtnGoLive = (TextView)convertView.findViewById(R.id.btn_go_live);
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
				
				BeseyeUtils.setVisibility(holder.mTxtEventType, (0 == iPosition)?View.INVISIBLE:View.VISIBLE);
				BeseyeUtils.setVisibility(holder.mBtnGoLive, (0 == iPosition)?View.VISIBLE:View.GONE);
				
				if(null != holder.mImgThumbnail){
					if(0 < iPosition){
						JSONArray arr = BeseyeJSONUtil.getJSONArray(obj, BeseyeJSONUtil.MM_THUMBNAIL_PATH);
						String[] path  = null;
						if(null != arr && 0 < arr.length()){
							path = new String[arr.length()];
							for(int i = 0;i<arr.length();i++){
								try {
									path[i] = arr.getString(i);
								} catch (JSONException e) {
									e.printStackTrace();
									Log.e(TAG, "setupItem(), e:"+e.toString());	
								}
							}
							
						}
						//Log.e(TAG, "setupItem(), path="+((null != path)?path.toString():"null")+" at "+iPosition);	

						holder.mImgThumbnail.setURI(path, R.drawable.eventlist_video_fake);
						holder.mImgThumbnail.loadImage();
//						String[] path = {"s3://2e26ea2bccb34937a65dfa02488e58dc-ap-northeast-1-beseyeuser/thumbnail/400x225/2014/05-23/15/{sEnd}1400858859902_{dur}10351_{r}1400850536594_{th}1400858859551.jpg",
//								"s3://beseye-thumbnail/taiwan_Taipei-101.jpg",
//								"s3://2e26ea2bccb34937a65dfa02488e58dc-ap-northeast-1-beseyeuser/thumbnail/400x225/2014/05-23/15/{sEnd}1400858901167_{dur}10445_{r}1400850536594_{th}1400858900722.jpg",
//								"s3://beseye-thumbnail/shanhai01.jpg",
//								"s3://2e26ea2bccb34937a65dfa02488e58dc-ap-northeast-1-beseyeuser/thumbnail/400x225/2014/05-23/15/{sEnd}1400858921935_{dur}10390_{r}1400850536594_{th}1400858921545.jpg",
//								"s3://beseye-thumbnail/taiwan_Taipei-101_2.jpg"};
						
					}else{
						holder.mImgThumbnail.setURI(new String[]{}, R.drawable.eventlist_video_fake);
						//holder.mImgThumbnail.loadDefaultImage();
						holder.mImgThumbnail.setImageBitmap(null);
						//holder.mImgThumbnail.setBackgroundColor(this.mContext.getResources().getColor(R.color.word_white));
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
					if(2 == iType){
						strType = "Family Dectection";
						BeseyeUtils.setVisibility(holder.mImgFace, View.VISIBLE);
					}else if(8 == iType){
						strType = "Fire Dectection";
						BeseyeUtils.setVisibility(holder.mImgFire, View.VISIBLE);
					}else if(4 == iType){
						strType = "Sound Dectection";
						BeseyeUtils.setVisibility(holder.mImgSound, View.VISIBLE);
					}else if(1 == iType){
						strType = "Motion Dectection";
						BeseyeUtils.setVisibility(holder.mImgMotion, View.VISIBLE);
					}
					
					strRet += (null == strType)?strType:(" & "+strType);
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
