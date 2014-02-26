package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.*;
import com.app.beseye.R;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class BeseyeSwitchBtn extends LinearLayout implements View.OnClickListener{
	
	//For state control Begin
	public static enum SwitchState{
		SWITCH_ON(0),
		SWITCH_OFF(1),
		SWITCH_DISABLED(2),
		SWITCH_STATE_COUNT(3);
		
		SwitchState(int iState){}
	}
	
	private SwitchState mSwitchState = SwitchState.SWITCH_OFF;
	private boolean mbEnabled = true;
	private ViewGroup m_vgSwitchBtnHolder;
	private ImageView m_imgSwitchIcon;
	private ImageView m_imgSwitchIconBg;
	
	private int mBgDrawable[] = {R.drawable.wifisetup_wifi_btn_bg_bluegreen, R.drawable.wifisetup_wifi_btn_bg_white, R.drawable.wifisetup_wifi_btn_bg_white};
		
	public BeseyeSwitchBtn(Context context) {
		super(context);
		init(context);
	}
	
	public BeseyeSwitchBtn(Context context, AttributeSet attrs) {
		super(context,attrs);
		init(context);
	}
	
	private void init(Context context){
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgSwitchBtnHolder = (ViewGroup)inflater.inflate(R.layout.beseye_function_switch_btn, null);
			if(null != m_vgSwitchBtnHolder){
				m_vgSwitchBtnHolder.setOnClickListener(this);
				m_imgSwitchIconBg = (ImageView)m_vgSwitchBtnHolder.findViewById(R.id.imgSwitchIconBg);
				m_imgSwitchIcon = (ImageView)m_vgSwitchBtnHolder.findViewById(R.id.imgSwitchIcon);
				addView(m_vgSwitchBtnHolder, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				applyDrawableByState();
				//setWidth(context);
			}
		}
	}
	
//	private void setWidth(Context context){
//		BitmapFactory.Options option= new BitmapFactory.Options();
//		if(null != option){
//			option.inJustDecodeBounds = true;
//			BitmapFactory.decodeResource(context.getResources(), mBgDrawable[mSwitchState.ordinal()], option);
//			if(null != m_vgSwitchBtnHolder){
//				LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) m_vgSwitchBtnHolder.getLayoutParams();
//				if(null != params){
//					params.width = option.outWidth*3;
//					Log.i(TAG,"setWidth(), option.outWidth = "+option.outWidth);
//					m_vgSwitchBtnHolder.setLayoutParams(params);
//				}
//			}
//		}
//	}
	
	private void applyDrawableByState(){
		if(null != m_imgSwitchIconBg)
			m_imgSwitchIconBg.setImageResource(mBgDrawable[mSwitchState.ordinal()]);
		
		if(null != m_imgSwitchIcon){
			RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) m_imgSwitchIcon.getLayoutParams();
			if(null != params){
				if(mbEnabled){
					if(SwitchState.SWITCH_OFF.equals(mSwitchState)){	
						params.addRule(RelativeLayout.ALIGN_LEFT, m_imgSwitchIconBg.getId());
						params.addRule(RelativeLayout.ALIGN_RIGHT, 0);
						//params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
					}
					else{
						params.addRule(RelativeLayout.ALIGN_LEFT, 0);
						params.addRule(RelativeLayout.ALIGN_RIGHT, m_imgSwitchIconBg.getId());
						//params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
					}
				}else{
					params.addRule(RelativeLayout.ALIGN_LEFT, m_imgSwitchIconBg.getId());
					params.addRule(RelativeLayout.ALIGN_RIGHT, 0);
				}
				m_imgSwitchIcon.setLayoutParams(params);
			}
		}
	}

	@Override
	public void onClick(View v) {
		if(mbEnabled){
			mSwitchState = (SwitchState.SWITCH_OFF.equals(mSwitchState))?SwitchState.SWITCH_ON:SwitchState.SWITCH_OFF;
			applyDrawableByState();
			if(null != mOnSwitchBtnStateChangedListener)
				mOnSwitchBtnStateChangedListener.onSwitchBtnStateChanged(mSwitchState);
		}
	}
	
	public SwitchState getSwitchState(){
		return mSwitchState;
	}
	
	public void setSwitchState(SwitchState state){
		if(null != state && !mSwitchState.equals(state)){
			mSwitchState = state;
			applyDrawableByState();
		}
	}
	
	public void setEnabled(boolean bEnabled){
		mbEnabled = bEnabled;
		applyDrawableByState();
	}
	
	private OnSwitchBtnStateChangedListener mOnSwitchBtnStateChangedListener;
	public static interface OnSwitchBtnStateChangedListener{
		void onSwitchBtnStateChanged(SwitchState state);
	}
	
	public void setOnSwitchBtnStateChangedListener(OnSwitchBtnStateChangedListener listener){
		mOnSwitchBtnStateChangedListener = listener;
	}
}
