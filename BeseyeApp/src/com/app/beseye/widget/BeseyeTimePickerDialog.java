package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.*;
import static com.app.beseye.util.BeseyeUtils.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import net.simonvt.numberpicker.NumberPicker;
import net.simonvt.numberpicker.NumberPicker.Formatter;
import net.simonvt.numberpicker.NumberPicker.OnValueChangeListener;
import android.util.Log;
import android.view.View.OnClickListener;

import com.app.beseye.R;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class BeseyeTimePickerDialog extends Dialog implements OnClickListener{
	private ViewGroup m_vgPickerHolder;
	private NumberPicker mNpHour, mNpMinute;
	private TextView mTxtTimeSelected;
	private Button mBtnOK, mBtnCancel;
	private Date mSelectedDate;
	private Calendar mShowDate; 
	private TimeFormatter mTimeFormatter;

	
	public BeseyeTimePickerDialog(Context context) {
		super(context);
		init(context, new Date(), "");
	}
	
	public BeseyeTimePickerDialog(Context context, Date selectTime, String strTitle) {
		super(context);
		init(context, selectTime, strTitle);
	}
	
	static private class TimeFormatter implements Formatter{
		@Override
		public String format(int value) {
			return String.format("%02d", value);
		}
	}

	private void init(Context context, Date selectTime, String strTitle){
		getWindow().setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		mTimeFormatter = new TimeFormatter();
		mSelectedDate = selectTime;
		mShowDate = Calendar.getInstance();
		mShowDate.setTime(mSelectedDate);
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgPickerHolder = (ViewGroup)inflater.inflate(R.layout.beseye_time_picker_dialog, null);
			if(null != m_vgPickerHolder){
				mTxtTimeSelected = (TextView)m_vgPickerHolder.findViewById(R.id.txt_picker_title);
				if(null != mTxtTimeSelected){
					mTxtTimeSelected.setText(strTitle);
				}
				
				mNpHour = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerHour);
				if(null != mNpHour){
					mNpHour.setMinValue(0);
					mNpHour.setMaxValue(23);
					mNpHour.setFormatter(mTimeFormatter);
					mNpHour.setOnValueChangedListener(mOnValueChangedListener);
				}
				
				mNpMinute = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerMinute);
				if(null != mNpMinute){
					mNpMinute.setMinValue(0);
					mNpMinute.setMaxValue(59);
					mNpMinute.setFormatter(mTimeFormatter);
					mNpMinute.setOnValueChangedListener(mOnValueChangedListener);
				}
				
				mBtnOK = (Button)m_vgPickerHolder.findViewById(R.id.btn_ok);
				if(null != mBtnOK){
					mBtnOK.setOnClickListener(this);
				}
				mBtnCancel = (Button)m_vgPickerHolder.findViewById(R.id.btn_cancel);
				if(null != mBtnCancel){
					mBtnCancel.setOnClickListener(this);
				}
				updatePicker();
				setContentView(m_vgPickerHolder);
			}
		}
	}
	
	private OnValueChangeListener mOnValueChangedListener = new OnValueChangeListener(){
		@Override
		public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
			if(picker == mNpMinute){
				mShowDate.add(Calendar.MINUTE, newVal-oldVal);
			}else if(picker == mNpHour){
				mShowDate.add(Calendar.HOUR_OF_DAY, newVal-oldVal);
			}
		}
	};
	
	public Calendar getPickedTime(){
		return mShowDate;
	}

	public void setDate(Date dateSelected){
		if(dateSelected != null){
			mSelectedDate = dateSelected;
			mShowDate.setTime(mSelectedDate);
		}
		updatePicker();
	}
	
	private void updatePicker(){		
		if(null != mNpHour){
			mNpHour.setValue(mShowDate.get(Calendar.HOUR_OF_DAY));
		}
		
		if(null != mNpMinute){
			mNpMinute.setValue(mShowDate.get(Calendar.MINUTE));
		}
	}
	
	static public interface OnDatetimePickerClickListener{
		void onBtnOKClick(Calendar pickDate);
		void onBtnCancelClick();
	}
	
	private OnDatetimePickerClickListener mOnDatetimePickerClickListener = null;
	
	public void setOnDatetimePickerClickListener(OnDatetimePickerClickListener listener){
		mOnDatetimePickerClickListener = listener;
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()){
			case R.id.btn_cancel:{
				mShowDate.setTime(new Date());
				hide();
				if(null != mOnDatetimePickerClickListener){
					mOnDatetimePickerClickListener.onBtnCancelClick();
				}
				break;
			}
			case R.id.btn_ok:{
				if(null != mOnDatetimePickerClickListener){
					mOnDatetimePickerClickListener.onBtnOKClick(mShowDate);
				}
				hide();
				break;
			}
		}
	}
}
