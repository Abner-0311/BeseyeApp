package com.app.beseye.util;

import com.app.beseye.R;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ShareAdapter extends BaseAdapter {
Object[] items;
private LayoutInflater mInflater;
Context context;

public ShareAdapter(Context context, Object[] items) {
    this.mInflater = LayoutInflater.from(context);
    this.items = items;
    this.context = context;
}

public int getCount() {
    return items.length;
}

public Object getItem(int position) {
    return items[position];
}

public long getItemId(int position) {
    return position;
}

public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder holder;
    if (convertView == null) {
        convertView = mInflater.inflate(R.layout.layout_single_item, null);
        holder = new ViewHolder();
        holder.name = (TextView) convertView.findViewById(R.id.textView1);
        holder.logo = (ImageView) convertView.findViewById(R.id.imageView1);
        convertView.setTag(holder);
    } else {
        holder = (ViewHolder) convertView.getTag();
    }
    
    holder.name
            .setText(((ResolveInfo) items[position]).loadLabel(context.getPackageManager()).toString());

    holder.logo
            .setImageDrawable(((ResolveInfo) items[position]).loadIcon(context.getPackageManager()));

    return convertView;
}

static class ViewHolder {

    TextView name;
    ImageView logo;
}}