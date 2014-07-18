package com.app.beseye.adapter;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.R;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class NewsListAdapter extends BeseyeJSONAdapter {
	
	public NewsListAdapter(Context context, JSONArray list, int iLayoutId,
			OnClickListener itemOnClickListener) {
		super(context, list, iLayoutId, itemOnClickListener);
	}

	static public class NewsListItmHolder{
		public View mVgNews;
		public TextView mTxtNewsTime;
		public TextView mTxtTitle;
		public TextView mTxtContent;
		public JSONObject mObjEvent;
	}
	
	@Override
	protected View inflateItem(int iPosition, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = mInflater.inflate(miLayoutId, null);
			if(null != convertView){
				NewsListItmHolder holder = new NewsListItmHolder();
				holder.mVgNews = convertView;
				holder.mTxtNewsTime = (TextView)convertView.findViewById(R.id.tv_news_release_date);				
				holder.mTxtTitle = (TextView)convertView.findViewById(R.id.tv_news_title);
				holder.mTxtContent = (TextView)convertView.findViewById(R.id.tv_news_description);
				convertView.setOnClickListener(mItemOnClickListener);
				convertView.setTag(holder);
			}
		}
		return convertView;
	}

	@Override
	protected void setupItem(int iPosition, View convertView, ViewGroup parent, JSONObject obj) {
		if(null != convertView){
			NewsListItmHolder holder = (NewsListItmHolder)convertView.getTag();
			if(null != holder){
				if(null != holder.mVgNews){
					holder.mVgNews.setBackgroundResource((0 == iPosition%2)?R.drawable.cl_news_list_bg_color_1:R.drawable.cl_news_list_bg_color_2);
					if(0 == iPosition){
						holder.mVgNews.setBackgroundResource(R.drawable.cl_news_list_bg_color_unread);
					}
					//holder.mVgNews.setOnClickListener(mItemOnClickListener);
				}
				
				holder.mObjEvent = obj;
				convertView.setTag(holder);
			}
		}
	}
}
