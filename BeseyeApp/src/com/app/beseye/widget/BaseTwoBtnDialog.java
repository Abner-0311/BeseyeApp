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
 * 
 * How to use:
 * 		new BaseTwoBtnDialog(Context context);
 * 		#need to setOnTwoBtnClickListener#
 * 
 * Example:
 * 		BaseTwoBtnDialog d = new BaseTwoBtnDialog(this);
 * 
 * How to setOnTwoBtnClickListener:
 *		setOnTwoBtnClickListener(new OnTwoBtnClickListener(){
 *    			@Override
 *   			public void onBtnYesClick() {
 *   				//do something
 *   			}
 *
 *   			@Override
 *   			public void onBtnNoClick() {
 *   				//do something
 *   			}} );
 *
 * Setting:
 * 		setBodyText(int resid);
 *		setTitleText(int resid);
 *  	setPositiveBtnText(int resid)	//default: "ok"
 *  	setNegativeBtnText(int resid)	//default: "cancel"
 *		setCancelable(boolean b);		//default: true
 *
 * NOTE:
 *		need to import
 *			com.app.beseye.widget.BaseTwoBtnDialog.OnTwoBtnClickListener;
 */


public class BaseTwoBtnDialog extends Dialog implements OnClickListener {
	
	private ViewGroup m_vgViewDialog;
	private Button mBtnYes, mBtnNo;
	private TextView m_tvBody;
	private TextView m_tvTitle;
	
	public BaseTwoBtnDialog(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		
		getWindow().setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);		
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgViewDialog = (ViewGroup)inflater.inflate(R.layout.layout_two_btn_dialog, null);	
			
			m_tvBody = (TextView)m_vgViewDialog.findViewById(R.id.txt_dialog_body);
			m_tvTitle = (TextView)m_vgViewDialog.findViewById(R.id.txt_dialog_title);
			
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
	public void setPositiveBtnText(String BtnPString){
		mBtnYes.setText(BtnPString);
	}
	
	public void setNegativeBtnText(int resid){
		mBtnNo.setText(resid);
	}
	public void setNegativeBtnText(String BtnNString){
		mBtnNo.setText(BtnNString);
	}
	
	static public interface OnTwoBtnClickListener{
		void onBtnYesClick();
		void onBtnNoClick();
	}
	
	private OnTwoBtnClickListener mOnTwoBtnClickListener = null;
	
	public void setOnTwoBtnClickListener(OnTwoBtnClickListener l){
		mOnTwoBtnClickListener = l;
	}
	
	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_no:{
				hide();
				if(null != mOnTwoBtnClickListener){
					mOnTwoBtnClickListener.onBtnNoClick();
				}
				break;
			}
			case R.id.btn_yes:{
				hide();
				if(null != mOnTwoBtnClickListener){
					mOnTwoBtnClickListener.onBtnYesClick();
				}
				break;
			}
		}
	}
}