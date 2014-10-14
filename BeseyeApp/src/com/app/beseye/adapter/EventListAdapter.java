package com.app.beseye.adapter;

import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.RemoteGifImageView;
import com.app.beseye.widget.RemoteImageView;

public class EventListAdapter extends BeseyeJSONAdapter {
	
	static public interface IListViewScrollListenser{
		public boolean isLvScrolling();
	}
	
	private int miSelectedImt = 0;
	private String mStrFamilyDetectFormat, mStrPeopleDetect, mStrSoundDetect, mStrFireDetect, mStrMotionDetect, mStrEventDetect;
	private IListViewScrollListenser mIListViewScrollListenser;
	
	public EventListAdapter(Context context, JSONArray list, int iLayoutId,
			OnClickListener itemOnClickListener, IListViewScrollListenser listViewScrollListenser) {
		super(context, list, iLayoutId, itemOnClickListener);
		mIListViewScrollListenser = listViewScrollListenser;
		mStrFamilyDetectFormat = context.getResources().getString(R.string.event_list_family_detected);
		mStrPeopleDetect = context.getResources().getString(R.string.event_list_people_detected);
		mStrSoundDetect = context.getResources().getString(R.string.event_list_sound_detected);
		mStrFireDetect = context.getResources().getString(R.string.event_list_fire_detected);
		mStrMotionDetect = context.getResources().getString(R.string.event_list_motion_detected);
		mStrEventDetect = context.getResources().getString(R.string.event_list_unknown_detected);
	}

	static public class EventListItmHolder{
		public TextView mTxtEventType;
		public RemoteGifImageView mImgThumbnail;
		public ImageView mImgDot;
		public ImageView mImgFace;
		public ImageView mImgFire;
		public ImageView mImgSound;
		public ImageView mImgMotion;
		public View mVGoLiveHolder;
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
				if(null != holder.mImgThumbnail){
					convertView.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(0, MeasureSpec.AT_MOST));
					BeseyeUtils.setThumbnailRatio(holder.mImgThumbnail, holder.mImgThumbnail.getMeasuredWidth(), BeseyeUtils.BESEYE_THUMBNAIL_RATIO_9_16);
				}
				holder.mImgDot = (ImageView)convertView.findViewById(R.id.iv_timeline_dot_greenblue);
				holder.mImgFace = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_face);
				holder.mImgFire = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_fire);
				holder.mImgSound = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_sound);
				holder.mImgMotion = (ImageView)convertView.findViewById(R.id.iv_timeline_icon_motion);
				
				holder.mVGoLiveHolder = convertView.findViewById(R.id.vg_timeline_go_live);
				
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
				genDetectionType(holder, BeseyeJSONUtil.getJSONInt(obj, BeseyeJSONUtil.MM_TYPE_IDS), obj);
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
				BeseyeUtils.setVisibility(holder.mVGoLiveHolder, (0 == iPosition)?View.VISIBLE:View.GONE);
				
				if(null != holder.mImgThumbnail){
					if(0 < iPosition){
						JSONArray arr = BeseyeJSONUtil.getJSONArray(obj, BeseyeJSONUtil.MM_THUMBNAIL_PATH);
						String[] path  = null;
						String[] pathCache = null;
						if(null != arr && 0 < arr.length()){
							path = new String[arr.length()];
							
							for(int i = 0;i<arr.length();i++){
								try {
									path[i] = arr.getString(i);	
								} catch (JSONException e) {
									Log.e(TAG, "setupItem(), e:"+e.toString());	
								}
							}	
							
							JSONArray arrCache = BeseyeJSONUtil.getJSONArray(obj, BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE);
							if(null != arrCache && 0 < arrCache.length()){
								Log.d(TAG, "setupItem(), reuse MM_THUMBNAIL_PATH_CACHE");	
								pathCache = new String[arrCache.length()];
								for(int i = 0;i<arrCache.length();i++){
									try {
										pathCache[i] = arrCache.getString(i);	
									} catch (JSONException e) {
										Log.e(TAG, "setupItem(), e:"+e.toString());	
									}
								}	
							}else{
								Log.d(TAG, "setupItem(), create MM_THUMBNAIL_PATH_CACHE");	
								pathCache = RemoteImageView.getCachePaths(mContext, path);
								JSONArray arrToCache = new JSONArray();
								for(String toCache:pathCache){
									arrToCache.put(toCache);
								}
								
								try {
									obj.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE, arrToCache);
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							
							Log.i(TAG, "setupItem(), path="+((null != path)?path.length:"null")+" at "+iPosition);	
						}else{
							Log.e(TAG, "setupItem(), no thumbnail path at "+iPosition);	
						}
						
						holder.mImgThumbnail.setIListViewScrollListenser(mIListViewScrollListenser);
						holder.mImgThumbnail.setURI(path, pathCache, R.drawable.eventlist_s_eventview_noview_bg);
						holder.mImgThumbnail.loadImage(true);
//						String[] path = {"s3://2e26ea2bccb34937a65dfa02488e58dc-ap-northeast-1-beseyeuser/thumbnail/400x225/2014/05-23/15/{sEnd}1400858859902_{dur}10351_{r}1400850536594_{th}1400858859551.jpg",
//								"s3://beseye-thumbnail/taiwan_Taipei-101.jpg",
//								"s3://2e26ea2bccb34937a65dfa02488e58dc-ap-northeast-1-beseyeuser/thumbnail/400x225/2014/05-23/15/{sEnd}1400858901167_{dur}10445_{r}1400850536594_{th}1400858900722.jpg",
//								"s3://beseye-thumbnail/shanhai01.jpg",
//								"s3://2e26ea2bccb34937a65dfa02488e58dc-ap-northeast-1-beseyeuser/thumbnail/400x225/2014/05-23/15/{sEnd}1400858921935_{dur}10390_{r}1400850536594_{th}1400858921545.jpg",
//								"s3://beseye-thumbnail/taiwan_Taipei-101_2.jpg"};
						
					}else{
						holder.mImgThumbnail.setURI(new String[]{}, R.drawable.eventlist_s_eventview_noview_bg);
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
	
	private String genDetectionType(EventListItmHolder holder, int typeArr, JSONObject obj){
		//Log.e(TAG, "genDetectionType(), typeArr:"+typeArr);	
		String strRet = "";
		
		BeseyeUtils.setVisibility(holder.mImgFire, View.INVISIBLE);
		BeseyeUtils.setVisibility(holder.mImgSound, View.INVISIBLE);
		
		
		String strType = null;
		if(0 < (BeseyeJSONUtil.MM_TYPE_ID_FACE & typeArr)){
//			JSONArray faceList = BeseyeJSONUtil.getJSONArray(obj, BeseyeJSONUtil.MM_FACE_IDS);
//			if(null != faceList && 0 < faceList.length()){
//				BeseyeJSONUtil.FACE_LIST face;
//				try {
//					int iFaceId = -1;
//					for(int i = faceList.length()-1;i >=0;i--){
//						if(0 < faceList.getInt(i)){
//							iFaceId = faceList.getInt(i);
//							break;
//						}
//					}
//					face = BeseyeJSONUtil.findFacebyId(iFaceId-1);
//					if(null != face){
//						strType = String.format(mStrFamilyDetectFormat, face.mstrName);
//					}else{
//						strType = mStrPeopleDetect;
//					}
//				} catch (JSONException e) {
//					Log.e(TAG, "genDetectionType(), e:"+e.toString());	
//				}
//				
//			}else{
//				strType = mStrPeopleDetect;
//			}
			strType = mStrPeopleDetect;
			BeseyeUtils.setVisibility(holder.mImgFace, View.VISIBLE);
		}else{
			BeseyeUtils.setVisibility(holder.mImgFace, View.INVISIBLE);
		}
		
		if(0 < (BeseyeJSONUtil.MM_TYPE_ID_MOTION & typeArr)){
			strType = ((null != strType)?(strType):mStrMotionDetect );
			BeseyeUtils.setVisibility(holder.mImgMotion, View.VISIBLE);
		}else{
			BeseyeUtils.setVisibility(holder.mImgMotion, View.INVISIBLE);
		}
		
		if(null == strType){
			strType =mStrEventDetect;
		}
		
		if(null != holder.mTxtEventType){
			holder.mTxtEventType.setText(strType);
		}
		return strRet;
	}
}
