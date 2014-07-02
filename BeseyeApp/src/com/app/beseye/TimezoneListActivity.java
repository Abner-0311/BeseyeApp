package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.adapter.TimezoneInfoAdapter;
import com.app.beseye.adapter.TimezoneInfoAdapter.TimezoneInfoHolder;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.PullToRefreshListView;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;

public class TimezoneListActivity extends BeseyeBaseActivity {

	private PullToRefreshListView mlvTimezoneList;
	private TimezoneInfoAdapter mTimezoneInfoAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	protected List<TimeZone> mlstTimeZone;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "WifiListActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_wifilist_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			BeseyeSwitchBtn mWifiSwitchBtn = (BeseyeSwitchBtn)mVwNavBar.findViewById(R.id.sb_wifi_btn);
			if(null != mWifiSwitchBtn){
				mWifiSwitchBtn.setVisibility(View.GONE);
			}
			
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.title_timezone);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}

		mlvTimezoneList = (PullToRefreshListView) findViewById(R.id.lst_timezone_list);
		
		if(null != mlvTimezoneList){
			mlvTimezoneList.setMode(LvExtendedMode.NONE);
			mlstTimeZone = new ArrayList<TimeZone>();
			String[] ids=TimeZone.getAvailableIDs();
			for(int i=0;i<ids.length;i++){	   
			   TimeZone d= TimeZone.getTimeZone(ids[i]);
			   mlstTimeZone.add(d);
			}
			
			mTimezoneInfoAdapter = new TimezoneInfoAdapter(this, mlstTimeZone, R.layout.timezone_list_item, this);
			if(null != mlvTimezoneList){
				mlvTimezoneList.setAdapter(mTimezoneInfoAdapter);
			}
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_timezone_list;
	}
	
	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof TimezoneInfoHolder){
			TimezoneInfoHolder info = (TimezoneInfoHolder)view.getTag();
			if(null != info){
				Toast.makeText(this, info.mTimeZone.toString(), Toast.LENGTH_SHORT).show();
			}
		}else if(view.getId() == R.id.iv_nav_left_btn){
			finish();
		}
	}
}
