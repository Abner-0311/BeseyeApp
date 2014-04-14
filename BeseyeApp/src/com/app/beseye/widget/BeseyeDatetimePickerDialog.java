package com.app.beseye.widget;

import net.simonvt.numberpicker.NumberPicker;
import net.simonvt.numberpicker.NumberPicker.OnValueChangeListener;

import com.app.beseye.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;

public class BeseyeDatetimePickerDialog extends Dialog {

	private ViewGroup m_vgPickerHolder;
	private NumberPicker mNpMonth, mNpDay, mNpHour;
	
	public BeseyeDatetimePickerDialog(Context context) {
		super(context);
		init(context);
	}

	public BeseyeDatetimePickerDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		init(context);
	}

	public BeseyeDatetimePickerDialog(Context context, int theme) {
		super(context, theme);
		init(context);
	}

	private void init(Context context){
		getWindow().setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgPickerHolder = (ViewGroup)inflater.inflate(R.layout.beseye_datetime_picker_dialog, null);
			if(null != m_vgPickerHolder){
				mNpMonth = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerMonth);
				if(null != mNpMonth){
					mNpMonth.setOnValueChangedListener(mOnValueChangedListener);
					mNpMonth.setMinValue(1);
					mNpMonth.setMaxValue(12);
					mNpMonth.setFocusable(false);
					mNpMonth.setFocusableInTouchMode(false);
					
				}
				mNpDay = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerDay);
				if(null != mNpDay){
					mNpDay.setOnValueChangedListener(mOnValueChangedListener);
					mNpDay.setMinValue(30);
					mNpDay.setMaxValue(31);
				}
				mNpHour = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerHour);
				if(null != mNpHour){
					mNpHour.setOnValueChangedListener(mOnValueChangedListener);
					mNpHour.setMinValue(0);
					mNpHour.setMaxValue(23);
				}
				
				setContentView(m_vgPickerHolder);
			}
		}
	}
	
	private OnValueChangeListener mOnValueChangedListener = new OnValueChangeListener(){
		@Override
		public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
			// TODO Auto-generated method stub
			
		}};
}
