package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeUtils;

public class BeseyeClockIndicator extends LinearLayout {

	public BeseyeClockIndicator(Context context) {
		super(context);
		init(context);
	}

	public BeseyeClockIndicator(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public BeseyeClockIndicator(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private ViewGroup m_vgClockIndHolder;
	private ImageView m_imgHand;
	private ImageView m_imgMinHand;
	
	static private final SimpleDateFormat sDateFormat = new SimpleDateFormat("MM/dd");
	static private final SimpleDateFormat sTimeFormat = new SimpleDateFormat("kk:mm");
	private TimeZone mTimeZone;
	
	private TextView m_txtTime;
	private TextView m_txtDate;
	
	private int miItmCount = 0;
	private int miItmHeight = 0;
	private int miHolderHeight = 0;
	private int miTotalHeight = 0;
	private int miIndHeight = 0;
	private int miIndRange = 0;
	private int miTop =0;
	
	private boolean mbNow = false;
	
	private void init(Context context){
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			m_vgClockIndHolder = (ViewGroup)inflater.inflate(R.layout.beseye_clock_indicator, null);
			if(null != m_vgClockIndHolder){
				m_imgHand = (ImageView)m_vgClockIndHolder.findViewById(R.id.imgHand);
				m_imgMinHand = (ImageView)m_vgClockIndHolder.findViewById(R.id.imgMinHand);

				m_txtTime = (TextView)m_vgClockIndHolder.findViewById(R.id.txtTime);
				m_txtDate = (TextView)m_vgClockIndHolder.findViewById(R.id.txtDate);
				
				addView(m_vgClockIndHolder, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			}
		}
	}
	
	public void updateTimeZone(TimeZone tz){
		mTimeZone = tz;
		sDateFormat.setTimeZone(mTimeZone);
		sTimeFormat.setTimeZone(mTimeZone);
		updateDateTime(mlLastTime);
	}
	
	public void calculateTotalLvHeight(int iItmCount, int iItmHeight, int iHolderHeight){
		if(0 < iItmCount){
			miItmCount = iItmCount;
			miItmHeight = iItmHeight;
			miHolderHeight = iHolderHeight;
			miTotalHeight = (miItmCount*miItmHeight >= miHolderHeight)?miItmCount*miItmHeight - miHolderHeight:(miItmCount*miItmHeight);
			miIndHeight = getHeight();
			miIndRange = iHolderHeight - miIndHeight;
			if(DEBUG)
				Log.i(TAG, "calculateTotalLvHeight(), [ "+miItmHeight+", "+miHolderHeight+", "+miTotalHeight+", "+miIndHeight+", "+miIndRange+"]");	
		}
	}
	
	public void updateIndicatorPosition(int iFirstIdx, int iPosBottom){
		int iCurPos = 0;
		iCurPos = iFirstIdx*miItmHeight - iPosBottom;
		
		float fRatio = (float)iCurPos/(float)miTotalHeight;
		miTop = (int) (miIndRange*fRatio);
		
		setY(miTop);
		invalidate();

		//Log.i(TAG, "updateIndicatorPosition(), [ "+iFirstIdx+", "+iPosBottom+", "+iCurPos+", "+fRatio+", "+miTop+", "+miIndRange+"]");	
	}
	
	public int getIndPosByItmPos(int iPosItm){
		int iRet = (((miHolderHeight - miIndHeight)*(iPosItm*miItmHeight))/miTotalHeight);//+(miIndHeight/2)-5;
		if(DEBUG)
			Log.i(TAG, "getIndPosByItmPos(), iRet:"+iRet);	
		return iRet;
	}
	
	public int getIndicatorPos(){
		return miTop + miIndHeight/2;
	}
	
	public void updateDateTime(long lts){
		mlLastTime = lts;
		Date date = new Date(lts);
		Calendar cal = Calendar.getInstance(mTimeZone);
		cal.setTime(date);
		
		//Log.i(TAG, "updateDateTime(), [ "+date.getMonth()+"/"+date.getDate()+" "+date.getHours()+":"+date.getMinutes()+"]");	
		miNextHour = cal.get(Calendar.HOUR_OF_DAY)%24;
		miNextMin = cal.get(Calendar.MINUTE);
		
		BeseyeUtils.setVisibility(m_txtTime, mbNow?View.GONE:View.VISIBLE);
		
		if(mbNow){
			if(null != m_txtDate){
				m_txtDate.setText(this.getResources().getString(R.string.event_list_now));
			}
		}else{
			if(null != m_txtDate){
				m_txtDate.setText(sDateFormat.format(date));
			}
			
			if(null != m_txtTime){
				m_txtTime.setText(/*sTimeFormat.format(date)*/String.format("%02d:%02d", miNextHour, miNextMin));
			}
		}	
		
		if(inAnimation()){
			miPendingHour = miNextHour;
			miPeningMin = miNextMin;
		}else{
			if(miCurHour != miNextHour || miCurMin != miNextMin){
				performAnimation(miNextHour,miNextMin);
			}
		}
	}
	
	public void updateToNow(boolean bNow){
		mbNow = bNow;
		if(mbNow){
			BeseyeUtils.setVisibility(m_txtTime,View.GONE);
			if(null != m_txtDate){
				m_txtDate.setText(this.getResources().getString(R.string.event_list_now));
			}
		}
	}
	
	private void checkPendingTime(){
		if(!inAnimation()){
			if(-1 < miPendingHour && -1 < miPeningMin){
				if(miCurHour != miPendingHour || miCurMin != miPeningMin){
					performAnimation(miPendingHour,miPeningMin);
					miPendingHour = -1;
					miPeningMin = -1;
				}
			}
		}
	}
	
	final private Interpolator interpolator = new LinearInterpolator();
	
	private void performAnimation(final int iNextHour, final int iNextMin){
		if(!inAnimation()){
			//Log.i(TAG, "performAnimation(), [ "+miCurHour+":"+miCurMin+" -> "+iNextHour+":"+iNextMin+"]");	
			float fCurHourAngle = getHourAngle(miCurHour, miCurMin);
			float fNextHourAngle = getHourAngle(iNextHour,iNextMin);
			//float fHourDleta =  Math.abs(fNextHourAngle - fCurHourAngle);
			mHourRotateAnimation = new RotateAnimation(fCurHourAngle, fNextHourAngle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
//			if(fHourDleta <= 180.0f){
//				
//				
//			}else{
//				
//			}
			if(null != mHourRotateAnimation){
				mHourRotateAnimation.setDuration(500);
				mHourRotateAnimation.setInterpolator(interpolator);
				mHourRotateAnimation.setRepeatCount(0);
				mHourRotateAnimation.setFillAfter(true);
				mHourRotateAnimation.setAnimationListener(new AnimationListener(){
					@Override
					public void onAnimationEnd(Animation arg0) {
						mbInHourAnimation = false;
						if(!inAnimation()){
							miCurHour = iNextHour;
							miCurMin = iNextMin;
						}
						checkPendingTime();
					}

					@Override
					public void onAnimationRepeat(Animation arg0) {}

					@Override
					public void onAnimationStart(Animation arg0) {
						mbInHourAnimation = true;
					}});
			}	
			
			float fCurMinAngle = getMinAngle(miCurMin);
			float fNextMinAngle = getMinAngle(iNextMin);
			mMinRotateAnimation = new RotateAnimation(fCurMinAngle, fNextMinAngle, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
			
			if(null != mMinRotateAnimation){
				mMinRotateAnimation.setDuration(500);
				mMinRotateAnimation.setInterpolator(interpolator);
				mMinRotateAnimation.setRepeatCount(0);
				mMinRotateAnimation.setFillAfter(true);
				mMinRotateAnimation.setAnimationListener(new AnimationListener(){
					@Override
					public void onAnimationEnd(Animation arg0) {
						mbInMinAnimation = false;
						if(!inAnimation()){
							miCurHour = iNextHour;
							miCurMin = iNextMin;
						}
						checkPendingTime();
					}

					@Override
					public void onAnimationRepeat(Animation arg0) {}

					@Override
					public void onAnimationStart(Animation arg0) {
						mbInMinAnimation = true;
					}});
			}
			
			m_imgHand.startAnimation(mHourRotateAnimation);
			m_imgMinHand.startAnimation(mMinRotateAnimation);
		}
	}
	
	private float getHourAngle(int iHour, int iMin){
		return 30 * iHour + 0.5f*iMin;
	}
	
	private float getMinAngle(int iMin){
		return 6.0f*iMin;
	}
	
	private int miCurHour= 0;
	private int miCurMin= 0;
	private int miNextHour= 0;
	private int miNextMin= 0;
	private int miPendingHour= -1;
	private int miPeningMin= -1;
	private long mlLastTime = -1;
	
	RotateAnimation mHourRotateAnimation;
	RotateAnimation mMinRotateAnimation;
	
	private boolean mbInHourAnimation = false;
	private boolean mbInMinAnimation = false;
	
	private boolean inAnimation(){
		return mbInHourAnimation || mbInMinAnimation;
	}
}
