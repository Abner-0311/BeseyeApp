package com.app.beseye.adapter;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.setting.TimezoneListActivity.BeseyeTimeZone;
import com.app.beseye.util.BeseyeUtils;

public class TimezoneInfoAdapter extends BaseAdapter {
	protected WeakReference<Context> mContext = null;
	protected LayoutInflater mInflater;
	protected OnClickListener mItemOnClickListener = null;
	protected int miLayoutId;
	
	private String mStrCurTZ;
	
	private List<BeseyeTimeZone> mlstTimezoneResult;
	
	public TimezoneInfoAdapter(Context context, List<BeseyeTimeZone> lstScanResult, int iLayoutId, OnClickListener itemOnClickListener){
		mContext = new WeakReference<Context>(context);
		mlstTimezoneResult = lstScanResult;
		miLayoutId = iLayoutId;
		mItemOnClickListener = itemOnClickListener;
		
		final TimeZoneComparator comparator = new TimeZoneComparator(true);
        Collections.sort(mlstTimezoneResult, comparator);
        
		if(null != mContext){
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
	}
	
	public void setCurrentTZ(String strTZ){
		mStrCurTZ = strTZ;
	}
	
	public void setSortByTimezone(boolean bSortByTimezone){
		final TimeZoneComparator comparator = new TimeZoneComparator(bSortByTimezone);
        Collections.sort(mlstTimezoneResult, comparator);
	}
	
	@Override
	public int getCount() {
		return (null != mlstTimezoneResult)?mlstTimezoneResult.size():0;
	}

	@Override
	public Object getItem(int iIndex) {
		return (getCount() > iIndex)?mlstTimezoneResult.get(iIndex):null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}
	
	static public class TimezoneInfoHolder{
		TextView mtxtName;
		public TextView mtxtZoneInfo;
		public ImageView mivSelected;
		public BeseyeTimeZone mTimeZone;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		convertView = inflateItem(position, convertView, parent);
		setupItem(position, convertView, parent);
		return convertView;
	}
	
	protected View inflateItem(int iPosition, View convertView, ViewGroup parent){
		if(null == convertView){
			if(null != mInflater){
				convertView = mInflater.inflate(miLayoutId, null);
				if(null != convertView){
					convertView.setOnClickListener(mItemOnClickListener);
					
					TimezoneInfoHolder holder = new TimezoneInfoHolder();
					if(null != holder){						
						holder.mtxtName = (TextView) convertView.findViewById(R.id.txt_zone_name);
						holder.mtxtZoneInfo = (TextView) convertView.findViewById(R.id.txt_zone_time);
						holder.mivSelected = (ImageView) convertView.findViewById(R.id.iv_check);
						convertView.setTag(holder);
					}
				}
			}
		}
		return convertView;
	}
	
	protected void setupItem(int iPosition, View convertView, ViewGroup parent){
		if(null != convertView){
			TimezoneInfoHolder holder = (TimezoneInfoHolder)convertView.getTag();
			if(null != holder){
				BeseyeTimeZone sRet = (BeseyeTimeZone)getItem(iPosition);
				if(null != sRet){
					
					if(null != holder.mtxtName)
						holder.mtxtName.setText(sRet.getDisplayName());
					
					//Log.i(BeseyeConfig.TAG, "Name:"+sRet.getDisplayName()+", sRet:"+sRet.tz);
					
					if(null != holder.mtxtZoneInfo){
						holder.mtxtZoneInfo.setText(BeseyeUtils.getGMTString(sRet.tz));
					}
					
					if(null != holder.mivSelected){
						holder.mivSelected.setVisibility((null != mStrCurTZ && mStrCurTZ.equals(sRet.tz.getID()))?View.VISIBLE:View.GONE);
					}
					
					holder.mTimeZone = sRet;
					convertView.setTag(holder);
					convertView.setOnClickListener(mItemOnClickListener);
				}
			}
		}
	}
	
	private static class TimeZoneComparator implements Comparator<BeseyeTimeZone> {
		private boolean mbSortByTimezone = true;
		
		public TimeZoneComparator(boolean bSortByTimezone){
			mbSortByTimezone = bSortByTimezone;
		}
		
        public int compare(BeseyeTimeZone zone1, BeseyeTimeZone zone2) {
        	if(mbSortByTimezone){
        		if(zone1.tz.getRawOffset() == zone2.tz.getRawOffset()){
            		return zone1.getDisplayName().compareTo(zone2.getDisplayName());
            	}else if(zone1.tz.getRawOffset() < zone2.tz.getRawOffset()){
            		return -1;
            	}else{
            		return 1;
            	}
        	}else{
        		return zone1.getDisplayName().compareTo(zone2.getDisplayName()); 
        	}
        }
    }
}
