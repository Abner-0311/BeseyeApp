package com.app.beseye.adapter;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;

public class TrustDevListAdapter extends BeseyeJSONAdapter {
	private boolean mbIsDeleteMode = false;
	
	public TrustDevListAdapter(Context context, JSONArray list, int iLayoutId, OnClickListener itemOnClickListener) {
		super(context, list, iLayoutId, itemOnClickListener);
	}

	static public class TrustDevItmHolder{
		public View mVTrustDevItm;
		public TextView mTxtTrustDevName;
		public ImageView mImgDeleteCheck;
		public ImageView mImgDeleteCheckBg;
		public JSONObject mObjTrustDev;
	}
	
	public void setDeleteMode(boolean bIsDeleteMode){
		mbIsDeleteMode = bIsDeleteMode;
	}
	
	@Override
	protected View inflateItem(int iPosition, View convertView, ViewGroup parent) {
		if(null == convertView){
			convertView = mInflater.inflate(miLayoutId, null);
			if(null != convertView){
				TrustDevItmHolder holder = new TrustDevItmHolder();
				holder.mVTrustDevItm = convertView;
				holder.mTxtTrustDevName = (TextView)convertView.findViewById(R.id.txt_dev_name);
				holder.mImgDeleteCheck = (ImageView)convertView.findViewById(R.id.iv_trust_delete_check);
				holder.mImgDeleteCheckBg = (ImageView)convertView.findViewById(R.id.iv_trust_delete_check_bg);
				convertView.setOnClickListener(mItemOnClickListener);
				convertView.setTag(holder);
			}
		}
		return convertView;
	}

	@Override
	protected void setupItem(int iPosition, View convertView, ViewGroup parent, JSONObject obj) {
		if(null != convertView){
			TrustDevItmHolder holder = (TrustDevItmHolder)convertView.getTag();
			if(null != holder){
				if(null != holder.mTxtTrustDevName){
					holder.mTxtTrustDevName.setText(BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_TRUST_DEV_NAME));
				}
				
				boolean bIsHost = BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.ACC_TRUST_DEV_HOST);
				convertView.setEnabled(!bIsHost);
				BeseyeUtils.setVisibility(holder.mImgDeleteCheck, (mbIsDeleteMode && BeseyeJSONUtil.getJSONBoolean(obj, BeseyeJSONUtil.ACC_TRUST_DEV_CHECK, false) && !bIsHost)?View.VISIBLE:View.GONE);
				BeseyeUtils.setVisibility(holder.mImgDeleteCheckBg, (mbIsDeleteMode && !bIsHost)?View.VISIBLE:View.GONE);	
				
				holder.mObjTrustDev = obj;
				convertView.setTag(holder);
			}
		}
	}

}
