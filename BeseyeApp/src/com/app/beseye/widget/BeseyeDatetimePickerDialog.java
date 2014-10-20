package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeUtils.isSameDay;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import net.simonvt.numberpicker.NumberPicker;
import net.simonvt.numberpicker.NumberPicker.Formatter;
import net.simonvt.numberpicker.NumberPicker.OnValueChangeListener;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.app.beseye.R;

public class BeseyeDatetimePickerDialog extends Dialog implements OnClickListener{
	static final private int DEF_CHECK_PERIOD = -7;//7 days ago
	private ViewGroup m_vgPickerHolder;
	private NumberPicker mNpMonth, mNpDay, mNpHour;
	private TextView mTxtTimeSelected;
	private Button mBtnOK, mBtnCancel;
	private Date mSelectedDate;
	private Calendar mShowDate,mStartDate,mEndDate; 
	private int miPeriodInDay;
	private MonthFormatter mMonthFormatter;
	static private SimpleDateFormat sDateFormat = new SimpleDateFormat("ccc, MMM, dd, yyyy");
	
	public BeseyeDatetimePickerDialog(Context context) {
		super(context);
		init(context, new Date(), DEF_CHECK_PERIOD);
	}
	
	public BeseyeDatetimePickerDialog(Context context, Date dateSelected, int iPeriodInDay) {
		super(context);
		init(context, dateSelected, iPeriodInDay);
	}
	
	static private class MonthFormatter implements Formatter{
		static private final SimpleDateFormat sDateFormat = new SimpleDateFormat("MMM");
		static private Calendar sCalendar = Calendar.getInstance();
		@Override
		public String format(int value) {
			int iMonth = value%12 -1;
			sCalendar.set(Calendar.MONTH, iMonth);
			return sDateFormat.format(sCalendar.getTime());
		}
	}

	private void init(Context context, Date dateSelected, int iPeriodInDay){
		
//		sDateFormat = (SimpleDateFormat) android.text.format.DateFormat.getDateFormat(context);
//		
//		final String format = Settings.System.getString(context.getContentResolver(), Settings.System.DATE_FORMAT);
//		if (TextUtils.isEmpty(format)) {
//			sDateFormat = (SimpleDateFormat) android.text.format.DateFormat.getMediumDateFormat(context);
//		} else {
//			sDateFormat = new SimpleDateFormat(format);
//		}
		
		getWindow().setBackgroundDrawable(new ColorDrawable(context.getResources().getColor(android.R.color.transparent)));
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgPickerHolder = (ViewGroup)inflater.inflate(R.layout.beseye_datetime_picker_dialog, null);
			if(null != m_vgPickerHolder){
				mTxtTimeSelected = (TextView)m_vgPickerHolder.findViewById(R.id.txt_picker_title);
				
				mNpMonth = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerMonth);
				if(null != mNpMonth){
					mNpMonth.setOnValueChangedListener(mOnValueChangedListener);
					mMonthFormatter = new MonthFormatter();
					mNpMonth.setFormatter(mMonthFormatter);
				}
				mNpDay = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerDay);
				if(null != mNpDay){
					mNpDay.setOnValueChangedListener(mOnValueChangedListener);
				}
				mNpHour = (NumberPicker)m_vgPickerHolder.findViewById(R.id.numberPickerHour);
				if(null != mNpHour){
					mNpHour.setOnValueChangedListener(mOnValueChangedListener);
				}
				mBtnOK = (Button)m_vgPickerHolder.findViewById(R.id.btn_ok);
				if(null != mBtnOK){
					mBtnOK.setOnClickListener(this);
				}
				mBtnCancel = (Button)m_vgPickerHolder.findViewById(R.id.btn_cancel);
				if(null != mBtnCancel){
					mBtnCancel.setOnClickListener(this);
				}
				
				setContentView(m_vgPickerHolder);
			}
		}
		setDateAndPeriod(dateSelected, iPeriodInDay);
	}
	
	private OnValueChangeListener mOnValueChangedListener = new OnValueChangeListener(){
		@Override
		public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
			if(picker == mNpMonth){
				mShowDate.add(Calendar.MONTH, newVal-oldVal);
				changeDaysByMonth();
			}else if(picker == mNpDay){
				mShowDate.add(Calendar.DAY_OF_MONTH, newVal-oldVal);
				changeHoursByDay();
			}else if(picker == mNpHour){
				mShowDate.add(Calendar.HOUR_OF_DAY, newVal-oldVal);
			}
			
			updateDateInfo();
		}
	};
	
	public Calendar getPickedDate(){
		return mShowDate;
	}
	
	private void changeDaysByMonth(){
		int iCurMonth = mShowDate.get(Calendar.MONTH)+1;
		int iStartMonth = mStartDate.get(Calendar.MONTH)+1;
		int iEndMonth = mEndDate.get(Calendar.MONTH)+1;
		
		int iCurDay = mShowDate.get(Calendar.DAY_OF_MONTH);
		int iStartDay = mStartDate.get(Calendar.DAY_OF_MONTH) ;
		int iEndDay = mEndDate.get(Calendar.DAY_OF_MONTH) ;
		
		if(null != mNpDay){
			int iMinDay = 0, iMaxDay =0; 
			if(iStartMonth == iCurMonth){
				if(0 > miPeriodInDay){
					iMaxDay = iStartDay;
					iMinDay = (iStartDay+miPeriodInDay)>=1?iEndDay:1;
				}else{
					int iDayOfMonth = mShowDate.getActualMaximum(Calendar.DAY_OF_MONTH);
					iMinDay = iStartDay;
					iMaxDay = (iStartDay+miPeriodInDay)<=iDayOfMonth?iEndDay:iDayOfMonth;
				}
			}else if(iEndMonth == iCurMonth){
				if(0 > miPeriodInDay){
					int iDayOfMonth = mShowDate.getActualMaximum(Calendar.DAY_OF_MONTH);
					iMinDay = iEndDay;
					iMaxDay = (iEndDay-miPeriodInDay)<=iDayOfMonth?iStartDay:iDayOfMonth;
				}else{
					iMaxDay = iEndDay;
					iMinDay = (iEndDay-miPeriodInDay)>=1?iStartDay:1;
				}
			}else{
				int iDayOfMonth = mShowDate.getActualMaximum(Calendar.DAY_OF_MONTH);
				iMinDay = 1;
				iMaxDay = iDayOfMonth;
			}	
			
			//Log.i(TAG, "changeDaysByMonth(), ["+iCurDay+"], \nendDate  =["+mEndDate.toString()+"]"); 
			
			if(iCurDay > iMaxDay || iCurDay < iMinDay){
				mShowDate.add(Calendar.DAY_OF_MONTH, iMinDay - iCurDay);
				iCurDay = iMinDay;
			}
			
			mNpDay.setMinValue(iMinDay);
			mNpDay.setMaxValue(iMaxDay);
			mNpDay.setValue(iCurDay);
			
			changeHoursByDay();
		}
	}
	
	private void changeHoursByDay(){
		int iMinHour = 0, iMaxHour = 0, iCurHour = mShowDate.get(Calendar.HOUR_OF_DAY);
		int iHourOfDay = mShowDate.getActualMaximum(Calendar.HOUR_OF_DAY);
		
		if(isSameDay(mShowDate, mStartDate)){
			if(0 > miPeriodInDay){
				iMinHour = 0;
				iMaxHour = mStartDate.get(Calendar.HOUR_OF_DAY);
			}else{
				iMinHour = mStartDate.get(Calendar.HOUR_OF_DAY);
				iMaxHour = iHourOfDay;
			}
		}else if(isSameDay(mShowDate, mEndDate)){
			if(0 > miPeriodInDay){
				iMinHour = mEndDate.get(Calendar.HOUR_OF_DAY);
				iMaxHour = iHourOfDay;
			}else{
				iMinHour = 0;
				iMaxHour = mEndDate.get(Calendar.HOUR_OF_DAY);
			}
		}else{
			iMinHour = 0;
			iMaxHour = iHourOfDay;
		}
		
		if(null != mNpHour){
			mNpHour.setMinValue(iMinHour);
			mNpHour.setMaxValue(iMaxHour);
			
			if(iCurHour > iMaxHour || iCurHour < iMinHour){
				mShowDate.add(Calendar.HOUR_OF_DAY, iMinHour - iCurHour);
				iCurHour = iMinHour;
			}
			
			mNpHour.setValue( iCurHour);
		}
	}
	
	private void updateDateInfo(){
		if(null != mTxtTimeSelected){
			mTxtTimeSelected.setText(sDateFormat.format(mShowDate.getTime()));
		}
	}
	
	private void updatePicker(){
		mStartDate = Calendar.getInstance();
		mEndDate = Calendar.getInstance();
		mShowDate = Calendar.getInstance();
		
		mStartDate.setTime(mSelectedDate);
		mEndDate.setTime(mSelectedDate);
		mShowDate.setTime(mSelectedDate);
		
		mEndDate.add(Calendar.DAY_OF_YEAR, miPeriodInDay);
		
		Log.i(TAG, "updatePicker(), \nstartDate=["+mStartDate.toString()+"], \nendDate  =["+mEndDate.toString()+"]"); 
		
		int iStartMonth = mStartDate.get(Calendar.MONTH)+1;
		int iEndMonth = mEndDate.get(Calendar.MONTH)+1;
		
		if(null != mNpMonth){
			if(0 > miPeriodInDay){
				if(12 == iEndMonth && iStartMonth != iEndMonth){
					mNpMonth.setMinValue(iEndMonth);
					mNpMonth.setMaxValue(iStartMonth+12);
					
				}else{
					mNpMonth.setMinValue(iEndMonth);
					mNpMonth.setMaxValue(iStartMonth);
				}
				mNpMonth.setValue( mNpMonth.getMaxValue());
			}else{
				if(12 == iStartMonth && iStartMonth != iEndMonth){
					mNpMonth.setMinValue(iStartMonth);
					mNpMonth.setMaxValue(iEndMonth+12);
				}else{
					mNpMonth.setMinValue(iEndMonth);
					mNpMonth.setMaxValue(iStartMonth);
				}
				mNpMonth.setValue( mNpMonth.getMinValue());
			}
		}
		
		int iStartDay = mStartDate.get(Calendar.DAY_OF_MONTH) ;
		int iEndDay = mEndDate.get(Calendar.DAY_OF_MONTH) ;
		
		if(null != mNpDay){
			if(iStartMonth == iEndMonth){
				mNpDay.setMinValue(iEndDay);
				mNpDay.setMaxValue(iStartDay);
				mNpDay.setValue(mNpDay.getMaxValue());
			}else{
				if(0 > miPeriodInDay){
					mNpDay.setMaxValue(iStartDay);
					mNpDay.setMinValue((iStartDay+miPeriodInDay)>=1?iEndDay:1);
					mNpDay.setValue(mNpDay.getMaxValue());
				}else{
					int iDayOfMonth = mStartDate.getActualMaximum(Calendar.DAY_OF_MONTH);
					mNpDay.setMinValue(iStartDay);
					mNpDay.setMaxValue((iStartDay+miPeriodInDay)<=iDayOfMonth?iEndDay:iDayOfMonth);
					mNpDay.setValue(mNpDay.getMinValue());
				}
			}	
		}
		
		int iStartHour = mStartDate.get(Calendar.HOUR_OF_DAY) ;
		
		if(null != mNpHour){
			if(0 > miPeriodInDay){
				mNpHour.setMaxValue(iStartHour);
				mNpHour.setMinValue(0);
				mNpHour.setValue(mNpHour.getMaxValue());
			}else{
				int iHourOfDay = mStartDate.getActualMaximum(Calendar.HOUR_OF_DAY);
				mNpHour.setMinValue(iStartHour);
				mNpHour.setMaxValue(iHourOfDay);
				mNpHour.setValue(mNpHour.getMinValue());
			}
		}
		
		updateDateInfo();
	}
		
	public void setDateAndPeriod(Date dateSelected, int iPeriodInDay){
		if(dateSelected != null){
			mSelectedDate = dateSelected;
		}
		
		if(0 != iPeriodInDay){
			miPeriodInDay = iPeriodInDay;
		}
		
		updatePicker();
	}	
		
	public void setDate(Date dateSelected){
		if(dateSelected != null){
			mSelectedDate = dateSelected;
		}
		updatePicker();
	}
	
	public void setPeriod(int iPeriodInDay){
		if(0 != iPeriodInDay){
			miPeriodInDay = iPeriodInDay;
		}
		updatePicker();
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
