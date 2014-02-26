package com.app.beseye.adapter;

import java.lang.ref.WeakReference;
import java.util.List;

import com.app.beseye.R;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.util.NetworkMgr.WifiAPInfo;

import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class WifiInfoAdapter extends BaseAdapter {
	protected WeakReference<Context> mContext = null;
	protected LayoutInflater mInflater;
	protected OnClickListener mItemOnClickListener = null;
	protected int miLayoutId;
	protected String strSecure;
	
	private List<WifiAPInfo> mlstScanResult;
	
	public WifiInfoAdapter(Context context, List<WifiAPInfo> lstScanResult, int iLayoutId, OnClickListener itemOnClickListener){
		mContext = new WeakReference<Context>(context);
		mlstScanResult = lstScanResult;
		miLayoutId = iLayoutId;
		mItemOnClickListener = itemOnClickListener;
		
		if(null != mContext){
			mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			strSecure = context.getResources().getString(R.string.network_secure_with);
		}
	}
	
	@Override
	public int getCount() {
		return (null != mlstScanResult)?mlstScanResult.size():0;
	}

	@Override
	public Object getItem(int iIndex) {
		return (getCount() > iIndex)?mlstScanResult.get(iIndex):null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}
	
	static public class WifoInfoHolder{
		TextView mtxtSSID;
		TextView mtxtSecure;
		ImageView mivSignalLevel;
		ImageView mivSecure;
		public Object mUserData;
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
				convertView = mInflater.inflate(R.layout.wifi_list_item, null);
				if(null != convertView){
					convertView.setOnClickListener(mItemOnClickListener);
					
					WifoInfoHolder holder = new WifoInfoHolder();
					if(null != holder){						
						holder.mtxtSSID = (TextView) convertView.findViewById(R.id.txt_ssid);
						holder.mtxtSecure = (TextView) convertView.findViewById(R.id.txt_secure);
						holder.mivSignalLevel = (ImageView) convertView.findViewById(R.id.iv_signal_strength);
						holder.mivSecure = (ImageView) convertView.findViewById(R.id.iv_wifi_secure);
						convertView.setTag(holder);
					}
				}
			}
		}
		return convertView;
	}
	
	protected void setupItem(int iPosition, View convertView, ViewGroup parent){
		if(null != convertView){
			WifoInfoHolder holder = (WifoInfoHolder)convertView.getTag();
			if(null != holder){
				WifiAPInfo sRet = (WifiAPInfo)getItem(iPosition);
				if(null != sRet){
					if(null != holder.mtxtSSID)
						holder.mtxtSSID.setText(sRet.SSID/*+", "+sRet.BSSID+", "+sRet.frequency*/);
					
					if(null != holder.mtxtSecure){
						if(sRet.bActiveConn){
							holder.mtxtSecure.setVisibility(View.VISIBLE);
							holder.mtxtSecure.setText(NetworkMgr.getInstance().getActiveConnStateId());
						}else{
							if(WifiAPInfo.AUTHNICATION_NONE.equals(sRet.cipher)){
								holder.mtxtSecure.setVisibility(View.GONE);
							}else{
								holder.mtxtSecure.setVisibility(View.VISIBLE);
								holder.mtxtSecure.setText(String.format(strSecure, sRet.cipher));
							}
						}
					}
					
					if(null != holder.mivSignalLevel){
						holder.mivSignalLevel.setVisibility(View.VISIBLE);
						holder.mivSignalLevel.setImageResource(NetworkMgr.getInstance().getSignalLevelDrawableId(sRet.signalLevel));
					}
					
					if(null != holder.mivSecure)
						holder.mivSecure.setVisibility((WifiAPInfo.AUTHNICATION_NONE.equals(sRet.cipher))?View.INVISIBLE:View.VISIBLE);
					
					holder.mUserData = sRet;
					convertView.setOnClickListener(mItemOnClickListener);
				}
			}
		}
	}
}
