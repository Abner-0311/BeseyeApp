package com.app.beseye.adapter;

import java.util.List;

import com.app.beseye.R;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MarketAppAdapter extends BaseAdapter {
	private List<ResolveInfo> mLstItems;
	private LayoutInflater mInflater;
	private Context context;
	
	public MarketAppAdapter(Context context, List<ResolveInfo> items) {
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
	        convertView = mInflater.inflate(R.layout.layout_single_item, null);
	        holder = new ViewHolder();
	        holder.name = (TextView) convertView.findViewById(R.id.txt_share_app_name);
	        holder.logo = (ImageView) convertView.findViewById(R.id.img_share_app_icon);
	        convertView.setTag(holder);
	    } else {
	        holder = (ViewHolder) convertView.getTag();
	    }
	    
	    holder.name
	            .setText((mLstItems.get(position)).loadLabel(context.getPackageManager()).toString());
	
	    holder.logo
	            .setImageDrawable((mLstItems.get(position)).loadIcon(context.getPackageManager()));
	
	    return convertView;
	}
	
	static class ViewHolder {
	    TextView name;
	    ImageView logo;
	}
}