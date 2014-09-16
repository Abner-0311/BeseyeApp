package com.app.beseye.adapter;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.widget.RemoteImageView;

public abstract class BeseyeJSONAdapter extends BaseAdapter {
	protected Context mContext = null;
	protected LayoutInflater mInflater;
	protected JSONArray mArrList = null;
	protected OnClickListener mItemOnClickListener = null;
	protected int miLayoutId;
	
	public BeseyeJSONAdapter(Context context, JSONArray list, int iLayoutId,  OnClickListener itemOnClickListener){
		mContext = context;
		if(null != mContext){
			mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
		
		mArrList = list;
		mItemOnClickListener = itemOnClickListener;
		miLayoutId = iLayoutId;
	}
	
	public void updateResultList(JSONArray list){
		mArrList = list;
	}
	
	public JSONArray getJSONList(){
		return mArrList;
	}
	
	@Override
	public int getCount() {
		return (null != mArrList)?mArrList.length():0;
	}

	@Override
	public Object getItem(int arg0) {
		try {
			return (null != mArrList)?mArrList.get(arg0):null;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return false;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = inflateItem(position, convertView, parent);
		}
		
		if(null != convertView){
			JSONObject obj = mArrList.optJSONObject(position);
			setupItem(position, convertView, parent,obj);
		}
		
		return convertView;
	}
	
	abstract protected View inflateItem(int iPosition, View convertView, ViewGroup parent);
	
	abstract protected void setupItem(int iPosition, View convertView, ViewGroup parent, JSONObject obj);
	
	protected void setTag(Map<String, Object> holder, String strLabel){
		if(null != holder && null != strLabel){
			View view = (View)holder.get(strLabel);
			if(null != view)
				view.setTag(holder);
		}
	}
	
	protected TextView setNameText(Map<String, Object> holder, String strLabel, String strValue){
		TextView txtView = (TextView)holder.get(strLabel);
		if(null != txtView){
			txtView.setText(strValue);
		}
		return txtView;
	}
	
	protected TextView setAttrText(Map<String, Object> holder, String strLabel, String strValue){
		TextView txtView = (TextView)holder.get(strLabel);
		if(null != txtView){
//			if(txtView instanceof iKalaImageTextView){
//				((iKalaImageTextView)txtView).setImageText(strValue, false);
//			}else
				txtView.setText(strValue);
			//txtView.setVisibility(View.VISIBLE);
		}
		return txtView;
	}
	
	protected void setAttrVisibility(Map<String, Object> holder, String strLabel, int iVisibility){
		View view = (View)holder.get(strLabel);
		if(null != view){
			view.setVisibility(iVisibility);
		}
	}
	
	protected RemoteImageView setImage(Map<String, Object> holder, String strLabel, String strUrl){
		return setImage(holder, strLabel, strUrl, R.drawable.common_app_icon, true);
	}
	
	protected RemoteImageView setImage(Map<String, Object> holder, String strLabel, String strUrl, int iDefRes, boolean bNeedAttachHost){
		RemoteImageView img = (RemoteImageView)holder.get(strLabel);
		if(null != img && null != strUrl){
//			if(WORK_AUDIO_TYPE.equals(strUrl)){
//				img.setURI("", R.drawable.video_no_default_bg);
//			}else{
//				img.setURI((bNeedAttachHost?SessionMgr.getInstance().getStorageHostUrl():"")+iKalaUtil.photoUrlTransform(strUrl), iDefRes, img.getWidth(), img.getHeight());
//			}
			img.loadImage();
		}
		return img;
	}
}
