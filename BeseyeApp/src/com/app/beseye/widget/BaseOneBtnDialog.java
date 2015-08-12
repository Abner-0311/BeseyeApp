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
 * Have one title, one text body and one button
 * 
 * How to use:
 * 		new BaseOneBtnDialog(Context context);
 * 		#need to setOnOneBtnClickListener#
 * 
 * Example:
 * 		BaseOneBtnDialog d = new BaseOneBtnDialog(this);
 *
 * 
 * How to setOnOneBtnClickListener:
 * 		setOnOneBtnClickListener(new OnOneBtnClickListener(){
 * 			@Override
 *			public void onBtnClick() {
 *				//do something
 *			}});
 *
 * Setting:
 * 		setBodyText(int resid);
 *		setTitleText(int resid);
 *  	setPositiveBtnText(int resid)	//default: "ok"
 *		setCancelable(boolean b);		//default: true
 *
 * NOTE:
 *		need to import
 *			com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;
 */


public class BaseOneBtnDialog extends Dialog implements OnClickListener {
	
	private ViewGroup m_vgViewDialog;
	private Button mBtnYes;
	private TextView m_tvBody;
	private TextView m_tvTitle;
	
	public BaseOneBtnDialog(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		
		getWindow().setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);		
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgViewDialog = (ViewGroup)inflater.inflate(R.layout.layout_one_btn_dialog, null);	
			
			m_tvBody = (TextView)m_vgViewDialog.findViewById(R.id.txt_dialog_body);
			m_tvTitle = (TextView)m_vgViewDialog.findViewById(R.id.txt_dialog_title);
	
			if(null != m_vgViewDialog) {
				
				mBtnYes = (Button)m_vgViewDialog.findViewById(R.id.btn_yes);
				if(null != mBtnYes){
					mBtnYes.setOnClickListener(this);
				}
				
				setContentView(m_vgViewDialog);
			}
		}	
	}
	
	public void setBodyText(int resid){
		m_tvBody.setText(resid);
	}
	
	public void setBodyText(String bodyString){
		m_tvBody.setText(bodyString);	
	}
	
	public void setTitleText(int resid){
		m_tvTitle.setText(resid);
	}
	
	public void setTitleText(String titleString){
		m_tvTitle.setText(titleString);
	}
	
	public void setPositiveBtnText(int resid){
		mBtnYes.setText(resid);
	}
	
	public void setPositiveBtnText(String BtnString){
		mBtnYes.setText(BtnString);
	}
	
	static public interface OnOneBtnClickListener{
		void onBtnClick();
	}
	
	private OnOneBtnClickListener mOnOneBtnClickListener = null;
	
	public void setOnOneBtnClickListener(OnOneBtnClickListener onOneBtnClickListener){
		mOnOneBtnClickListener = onOneBtnClickListener;
	}
	
	@Override
	public void onClick(View view) {	
		hide();
		if(null != mOnOneBtnClickListener){
			mOnOneBtnClickListener.onBtnClick();
		}
	}
}