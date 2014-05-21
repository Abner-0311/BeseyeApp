package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.Date;

import com.app.beseye.R;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

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
	
	private TextView m_txtTime;
	private TextView m_txtDate;
	
	private int miItmCount = 0;
	private int miItmHeight = 0;
	private int miHolderHeight = 0;
	private int miTotalHeight = 0;
	private int miIndHeight = 0;
	private int miIndRange = 0;
	private int miTop =0;
	
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
	
	public void calculateTotalLvHeight(int iItmCount, int iItmHeight, int iHolderHeight){
		if(0 < iItmCount){
			miItmCount = iItmCount;
			miItmHeight = iItmHeight;
			miHolderHeight = iHolderHeight;
			miTotalHeight = miItmCount*miItmHeight - miHolderHeight;
			miIndHeight = getHeight();
			miIndRange = iHolderHeight - miIndHeight;
			Log.i(TAG, "calculateTotalLvHeight(), [ "+miItmHeight+", "+miHolderHeight+", "+miTotalHeight+", "+miIndHeight+", "+miIndRange+"]");	
		}
	}
	
	public void updateIndicatorPosition(int iFirstIdx, int iPosBottom){
		int iCurPos = 0;
		iCurPos = iFirstIdx*miItmHeight - iPosBottom;
		
		float fRatio = (float)iCurPos/(float)miTotalHeight;
		miTop = (int) (miIndRange*fRatio);
		//layout(getLeft(), miTop, getLeft()+getWidth(), miTop+miIndHeight);
		//setTop(miTop);
		setY(miTop);
		invalidate();
		//
		//Log.i(TAG, "updateIndicatorPosition(), [ "+iCurPos+", "+fRatio+", "+iTop+", "+miIndRange+"]");	
	}
	
	public int getIndicatorPos(){
		return miTop + miIndHeight/2;
	}
	
	public void updateDateTime(long lts){
		Date date = new Date(lts);
		
		//Log.i(TAG, "updateDateTime(), [ "+date.getMonth()+"/"+date.getDate()+" "+date.getHours()+":"+date.getMinutes()+"]");	
		
		if(null != m_txtDate){
			m_txtDate.setText(String.format("%d/%d", date.getMonth()+1, date.getDate()));
		}
		
		if(null != m_txtTime){
			m_txtTime.setText(String.format("%d:%d", date.getHours(), date.getMinutes()));
		}
	}

//	@Override
//	protected void onLayout(boolean changed, int l, int t, int r, int b) {
//		Log.i(TAG, "onLayout(), [ "+changed+", "+l+","+t+","+r+","+b+"]"+"mTop="+miTop);	
//		super.onLayout(changed, getLeft(), miTop, getLeft()+getWidth(), miTop+miIndHeight);
//	}
	
	
}
