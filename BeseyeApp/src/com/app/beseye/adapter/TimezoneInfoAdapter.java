package com.app.beseye.adapter;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;

import com.app.beseye.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TimezoneInfoAdapter extends BaseAdapter {
	protected WeakReference<Context> mContext = null;
	protected LayoutInflater mInflater;
	protected OnClickListener mItemOnClickListener = null;
	protected int miLayoutId;
	
	private List<TimeZone> mlstTimezoneResult;
	
	public TimezoneInfoAdapter(Context context, List<TimeZone> lstScanResult, int iLayoutId, OnClickListener itemOnClickListener){
		mContext = new WeakReference<Context>(context);
		mlstTimezoneResult = lstScanResult;
		miLayoutId = iLayoutId;
		mItemOnClickListener = itemOnClickListener;
		
		final TimeZoneComparator comparator = new TimeZoneComparator();
        //final List<HashMap<String, Object>> sortedList = getZones(context);
        Collections.sort(mlstTimezoneResult, comparator);
        
        for(int idx = 0; idx < mlstTimezoneResult.size();){
        	TimeZone tzCur = mlstTimezoneResult.get(idx);
        	if(tzCur.getID().toLowerCase().startsWith("etc/")){
        		mlstTimezoneResult.remove(idx);
        		continue;
        	}
        	
        	if(idx > 0){
        		TimeZone tzPre = mlstTimezoneResult.get(idx-1);
            	if(tzCur.getDisplayName().equals(tzPre.getDisplayName())){
            		mlstTimezoneResult.remove(idx);
            		continue;
            	}
        	}
        	idx++;
        }
        
		if(null != mContext){
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}
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
		TextView mtxtZoneInfo;
		public TimeZone mTimeZone;
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
				TimeZone sRet = (TimeZone)getItem(iPosition);
				if(null != sRet){
					if(null != holder.mtxtName)
						holder.mtxtName.setText(sRet.getDisplayName());
					
					if(null != holder.mtxtZoneInfo){
						  //String region = ids[i].replaceAll(".*/", "").replaceAll("_", " ");
						int hours = Math.abs(sRet.getRawOffset()) / 3600000;
						int minutes = Math.abs(sRet.getRawOffset() / 60000) % 60;
						String sign = sRet.getRawOffset() >= 0 ? "+" : "-";
						
						String timeZonePretty = String.format("GMT %s%02d:%02d", sign, hours, minutes);
						holder.mtxtZoneInfo.setText(timeZonePretty);
					}
					
					holder.mTimeZone = sRet;
					convertView.setTag(holder);
					convertView.setOnClickListener(mItemOnClickListener);
				}
			}
		}
	}
	
	private static class TimeZoneComparator implements Comparator<TimeZone> {

        public int compare(TimeZone zone1, TimeZone zone2) {
        	if(zone1.getRawOffset() == zone2.getRawOffset()){
        		return zone1.getDisplayName().compareTo(zone2.getDisplayName());
        	}else if(zone1.getRawOffset() < zone2.getRawOffset()){
        		return -1;
        	}else{
        		return 1;
        	}
//        	if(!isComparable(zone1)) {
//                return isComparable(zone2) ? 1 : 0;
//            } else if (!isComparable(zone2)) {
//                return -1;
//            }
//            return ((Comparable) zone1).compareTo(zone2);
        }
//        
//        private boolean isComparable(Object value) {
//            return (value != null) && (value instanceof Comparable); 
//        }
    }
	
	

}
