package com.app.beseye.widget;

import com.app.beseye.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/*
 * This dialog is only for simple Beseye style dialog!
 * Have one title, some text body and two button
 */


public class BaseDialog extends Dialog implements OnClickListener {
	
	private ViewGroup m_vgViewDialog;
	private Button mBtnYes, mBtnNo;
	
	public BaseDialog(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		
		getWindow().setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);		
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgViewDialog = (ViewGroup)inflater.inflate(R.layout.layout_motion_zone_cancel_dialog, null);	
			
			if(null != m_vgViewDialog) {
				
				mBtnYes = (Button)m_vgViewDialog.findViewById(R.id.btn_yes);
				if(null != mBtnYes){
					mBtnYes.setOnClickListener(this);
				}
				mBtnNo = (Button)m_vgViewDialog.findViewById(R.id.btn_no);
				if(null != mBtnNo){
					mBtnNo.setOnClickListener(this);
				}
				
				setContentView(m_vgViewDialog);
			}
		}	
	}
	
	static public interface OnDialogClickListener{
		void onBtnYesClick();
		void onBtnNoClick();
	}
	
	private OnDialogClickListener mOnDialogClickListener = null;
	
	public void setOnDialogClickListener(OnDialogClickListener l){
		mOnDialogClickListener = l;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_no:{
				hide();
				if(null != mOnDialogClickListener){
					mOnDialogClickListener.onBtnNoClick();
				}
				break;
			}
			case R.id.btn_yes:{
				hide();
				if(null != mOnDialogClickListener){
					mOnDialogClickListener.onBtnYesClick();
				}
				break;
			}
		}
	}
}