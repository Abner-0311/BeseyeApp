package com.app.beseye.adapter;

import java.util.List;

import com.app.beseye.R;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MarketWebAdapter extends BaseAdapter {
	private List<MarketWebInfo> mLstItems;
	private LayoutInflater mInflater;
	private Context context;

	public MarketWebAdapter(Context context, List<MarketWebInfo> items) {
	    this.mInflater = LayoutInflater.from(context);
	    this.mLstItems = items;
	    this.context = context;
	}
	
	public int getCount() {
	    return null != mLstItems?mLstItems.size():0;
	}
	
	public Object getItem(int position) {
	    return null != mLstItems?mLstItems.get(position):null;
	}
	
	
	public long getItemId(int position) {
	    return position;
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
	    ViewHolder holder;
	    if (convertView == null) {
	        convertView = mInflater.inflate(R.layout.layout_market_web_item, null);
	        holder = new ViewHolder();
	        holder.name = (TextView) convertView.findViewById(R.id.txt_market_app_name);
	        convertView.setTag(holder);
	    } else {
	        holder = (ViewHolder) convertView.getTag();
	    }
	    
	    holder.name.setText(mLstItems.get(position).mStrMarketName);
	
	    return convertView;
	}
	
	static public class MarketWebInfo{
		public MarketWebInfo(String mStrMarketName, Uri mMarketURL) {
			super();
			this.mStrMarketName = mStrMarketName;
			this.mMarketURL = mMarketURL;
		}
		public String mStrMarketName;
		public Uri mMarketURL;
	}
	
	static class ViewHolder {
	    TextView name;
	}
}