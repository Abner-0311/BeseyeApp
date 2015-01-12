package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn;

public class EventFilterActivity extends BeseyeBaseActivity{
	static public final String KEY_EVENT_FILTER_VALUE = "KEY_EVENT_FILTER_VALUE";
	static public final int DEF_EVENT_FILTER_VALUE = BeseyeMMBEHttpTask.EVENT_FILTER_PEOPLE | BeseyeMMBEHttpTask.EVENT_FILTER_MOTION | BeseyeMMBEHttpTask.EVENT_FILTER_SOUND;
	
	static private enum EVENT_TYPE{
		EVENT_PEOPLE,
		EVENT_MOTION,
		EVENT_FIRE,
		EVENT_SOUND,
		EVENT_TYPE_COUNT;
	};
	
	static private final int s_NotifyTypeNum = EVENT_TYPE.EVENT_TYPE_COUNT.ordinal();
	
	private BeseyeSwitchBtn mNotifyMeSwitchBtn;
	private ViewGroup mVgEventType[];
	private ImageView mIvEventTypeCheck[], mIvEventTypeCheckBg[];
	private TextView mTxtSchedDays[];
	
	private String[] mStrObjKey = {BeseyeJSONUtil.NOTIFY_PEOPLE, 
								   BeseyeJSONUtil.NOTIFY_MOTION,
								   BeseyeJSONUtil.NOTIFY_FIRE,
								   BeseyeJSONUtil.NOTIFY_SOUND};
	
	private int[] mEventFilterNum = {BeseyeMMBEHttpTask.EVENT_FILTER_PEOPLE, 
									 BeseyeMMBEHttpTask.EVENT_FILTER_MOTION,
									 BeseyeMMBEHttpTask.EVENT_FILTER_FIRE,
									 BeseyeMMBEHttpTask.EVENT_FILTER_SOUND};
	
	
	private boolean[] mbEnabledLst = {true, true, false, false};
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	private int miOriginalFilterValue = DEF_EVENT_FILTER_VALUE;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
						
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.title_event_filter);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		
		miOriginalFilterValue = getIntent().getIntExtra(KEY_EVENT_FILTER_VALUE, DEF_EVENT_FILTER_VALUE);
		
		mVgEventType = new ViewGroup[s_NotifyTypeNum];
		mIvEventTypeCheck = new ImageView[s_NotifyTypeNum];
		mIvEventTypeCheckBg = new ImageView[s_NotifyTypeNum];
		mTxtSchedDays = new TextView[s_NotifyTypeNum];
		
		int iVgIds[] = {R.id.vg_people_event, R.id.vg_motion_event, R.id.vg_fire_event, R.id.vg_sound_event};
		int iStrIds[] = {R.string.notification_event_people,
						 R.string.notification_event_motion, 
						 R.string.notification_event_fire, 
						 R.string.notification_event_sound};
		
		
		for(int idx = 0; idx < s_NotifyTypeNum;idx++){
			mVgEventType[idx] = (ViewGroup)findViewById(iVgIds[idx]);
			if(null != mVgEventType[idx]){
				mIvEventTypeCheck[idx] = (ImageView)mVgEventType[idx].findViewById(R.id.iv_day_check);
				BeseyeUtils.setVisibility(mIvEventTypeCheck[idx], (miOriginalFilterValue&mEventFilterNum[idx]) > 0?View.VISIBLE:View.INVISIBLE);
				
				mIvEventTypeCheckBg[idx] = (ImageView)mVgEventType[idx].findViewById(R.id.iv_day_check_bg);
				if(null != mIvEventTypeCheckBg[idx]){
					mIvEventTypeCheckBg[idx].setTag(idx);
					//mIvEventTypeCheckBg[idx].setOnClickListener(this);
				}
				
				mTxtSchedDays[idx] = (TextView)mVgEventType[idx].findViewById(R.id.txt_day_title);
				if(null != mTxtSchedDays[idx]){
					mTxtSchedDays[idx].setText(iStrIds[idx]);
				}
				mVgEventType[idx].setOnClickListener(this);
				
				//Disable fire and sound detection
				if(false == mbEnabledLst[idx]){
					mVgEventType[idx].setVisibility(View.GONE);
				}
			}
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_event_filter;
	}
	
	@Override
	public void onClick(View view) {
		int idx = findIdxByView(view);
		if(0 <= idx){
			if(null != mIvEventTypeCheck[idx]){
				if(View.VISIBLE != mIvEventTypeCheck[idx].getVisibility()){
					mIvEventTypeCheck[idx].setVisibility(View.VISIBLE);
				}else if(1 < getNumOfChecked()){
					mIvEventTypeCheck[idx].setVisibility(View.INVISIBLE);
				}
			}
		}else if(R.id.iv_nav_left_btn == view.getId()){
			checkModification();
			finish();
		}else{
			super.onClick(view);
		}
	}
	
	private int findIdxByView(View view){
		int iRet = -1;
		for(int idx = 0; idx < s_NotifyTypeNum;idx++){
			if(view == mVgEventType[idx]){
				iRet = idx;
				break;
			}
		}
		
		return iRet;
	}
	
	private int getNumOfChecked(){
		int iRet = 0;
		for(int idx = 0; idx < s_NotifyTypeNum;idx++){
			if(View.VISIBLE == mIvEventTypeCheck[idx].getVisibility()){
				iRet++;
			}
		}
		if(DEBUG)
			Log.i(TAG, "getNumOfChecked(), iRet:"+iRet);
		return iRet;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			checkModification();
		}
		
		return super.onKeyUp(keyCode, event);
	}
	
	private int getNewFilterValue(){
		int iRet = 0;
		for(int idx = 0; idx < s_NotifyTypeNum;idx++){
			if(null != mVgEventType[idx]){
				if(View.VISIBLE == mIvEventTypeCheck[idx].getVisibility()){
					iRet+=mEventFilterNum[idx];
				}
			}
		}
		return iRet;
	}
	
	private void checkModification(){
		int iNewValue = getNewFilterValue();
		if(iNewValue != this.miOriginalFilterValue){
			Intent intent = new Intent();
			intent.putExtra(KEY_EVENT_FILTER_VALUE, iNewValue);
			setResult(RESULT_OK, intent);
		}
	}
}
