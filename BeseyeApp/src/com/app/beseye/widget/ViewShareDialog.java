package com.app.beseye.widget;

import com.app.beseye.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

/*
 * How to use Dialog:
 * 		new ViewShareDialog(Activity activity, String content); 
 * 		#need to setOnShareClickListener#
 * 
 * Parameters:
 * 		activity	just a activity
 * 		content		(String) image file path 
 * 
 * Sample:
 * 		ViewShareDialog d = new ViewShareDialog(this, "/storage/emulated/0/DCIM/Beseye/Beseye_Pro_04_24_2015_09_59_05_上午.jpg");
 * 	
 * How to setOnShareClickListener:
 * 		setOnShareClickListener(new OnShareClickListener(){
 * 			@Override
 *			public void onBtnShareClick() {
 *				//do something, like call ShageMgr.java 
 *			}
 *			@Override
 *			public void onBtnCloseClick() {
 *			}} );
 *
 * NOTE:
 * 		don't forget to call show();
 */

public class ViewShareDialog extends Dialog implements OnClickListener {
	
	private ViewGroup m_vgViewShare;
	private ImageView mSnapshot;
	private Button mBtnShare, mBtnClose;
	
	public ViewShareDialog(Context context, String imageContext) {
		super(context);
		init(context, imageContext);
	}

	private void init(Context context, String imageContext) {
		
		getWindow().setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);		
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgViewShare = (ViewGroup)inflater.inflate(R.layout.layout_view_share, null);	
			
			if(null != m_vgViewShare) {
				
				mSnapshot = (ImageView)m_vgViewShare.findViewById(R.id.iv_snapshot);
				Bitmap bMap = BitmapFactory.decodeFile(imageContext);
				mSnapshot.setImageBitmap(bMap);
		        
				mBtnShare = (Button)m_vgViewShare.findViewById(R.id.btn_share);
				if(null != mBtnShare){
					mBtnShare.setOnClickListener(this);
				}
				mBtnClose = (Button)m_vgViewShare.findViewById(R.id.btn_close);
				if(null != mBtnClose){
					mBtnClose.setOnClickListener(this);
				}
				
				setContentView(m_vgViewShare);
			}
		}	
	}
	
	static public interface OnShareClickListener{
		void onBtnShareClick();
		void onBtnCloseClick();
	}
	
	private OnShareClickListener mOnShareClickListener = null;
	
	public void setOnShareClickListener(OnShareClickListener l){
		mOnShareClickListener = l;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_close:{
				hide();
				if(null != mOnShareClickListener){
					mOnShareClickListener.onBtnCloseClick();
				}
				break;
			}
			case R.id.btn_share:{
				if(null != mOnShareClickListener){
					mOnShareClickListener.onBtnShareClick();
				}
				break;
			}
		}
	}
}