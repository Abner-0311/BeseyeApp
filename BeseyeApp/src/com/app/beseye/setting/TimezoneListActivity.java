package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.R;
import com.app.beseye.R.drawable;
import com.app.beseye.R.id;
import com.app.beseye.R.layout;
import com.app.beseye.R.string;
import com.app.beseye.adapter.TimezoneInfoAdapter;
import com.app.beseye.adapter.TimezoneInfoAdapter.TimezoneInfoHolder;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshListView;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;

public class TimezoneListActivity extends BeseyeBaseActivity {

	private PullToRefreshListView mlvTimezoneList;
	private TimezoneInfoAdapter mTimezoneInfoAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	protected List<TimeZone> mlstTimeZone;
	
	static final String[] sTimezoneList = {"ROC", 
										   "PRC", 
										   "Japan", 
										   "Hongkong", 
										   "ROK", 
										   "Singapore", 
										   "US/Eastern",
										   "US/Central",
										   "US/Pacific",
										   "US/Mountain",
										   "US/Alaska",
										   "US/Hawaii",
										   "US/Arizona",
										   "Canada/Atlantic",
										   "Canada/Newfoundland",
										   "Canada/Eastern",
										   "Canada/Central",
										   "Canada/Mountain",
										   "Canada/Pacific",
										   "NZ",
										   "Australia/ACT",
										   "Australia/West",
										   "Australia/Adelaide",
										   "Europe/Copenhagen",
										   "Atlantic/Reykjavik",
										   "Europe/Athens",
										   "Europe/Warsaw",
										   "Europe/Paris",
										   "Europe/Helsinki",
										   "Europe/Londo",
										   "Europe/Oslo",
										   "Europe/Prague",
										   "Europe/Amsterdam",
										   "Europe/Zurich",
										   "Europe/Stockholm",
										   "Europe/Berlin",
										   "Europe/Brussels",
										   "Europe/Budapest",
										   "Europe/Madrid",
										   "Europe/Vienna",
										   "Europe/Rome"};
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "WifiListActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setIcon(R.drawable.sl_nav_back_btn);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		//workaround to make icon clickable
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				getSupportActionBar().setDisplayHomeAsUpEnabled(false);
			}}, 100);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
				mIvBack.setVisibility(View.GONE);
			}
						
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.title_timezone);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			//mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}

		mlvTimezoneList = (PullToRefreshListView) findViewById(R.id.lst_timezone_list);
		
		if(null != mlvTimezoneList){
			mlvTimezoneList.setMode(LvExtendedMode.NONE);
			mlstTimeZone = new ArrayList<TimeZone>();
//			String[] ids=TimeZone.getAvailableIDs();
//			for(int i=0;i<ids.length;i++){	   
//			   TimeZone d= TimeZone.getTimeZone(ids[i]);
//			   mlstTimeZone.add(d);
//			}
			
			for(int i=0;i<sTimezoneList.length;i++){	   
			   TimeZone d= TimeZone.getTimeZone(sTimezoneList[i]);
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
				//Toast.makeText(this, info.mTimeZone.toString(), Toast.LENGTH_SHORT).show();
				Intent intent = new Intent();
				intent.putExtra(HWSettingsActivity.TIME_ZONE_INFO, info.mTimeZone.getID());
				setResult(RESULT_OK, intent);
				finish();
			}
		}else {
			super.onClick(view);
		}
	}
	
	static final private int MENU_ALPHABETICAL =0;
	static final private int MENU_TIMEZONE =1;
	private boolean mSortedByTimezone = true;
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ALPHABETICAL, 0, R.string.zone_list_menu_sort_alphabetically);
        menu.add(0, MENU_TIMEZONE, 0, R.string.zone_list_menu_sort_by_timezone);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mSortedByTimezone) {
            menu.findItem(MENU_TIMEZONE).setVisible(false);
            menu.findItem(MENU_ALPHABETICAL).setVisible(true);
        } else {
            menu.findItem(MENU_TIMEZONE).setVisible(true);
            menu.findItem(MENU_ALPHABETICAL).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case android.R.id.home:{
        		finish();
        		return true;
        	}  
            case MENU_TIMEZONE:
                setSorting(true);
                return true;
                
            case MENU_ALPHABETICAL:
                setSorting(false);
                return true;
                
            default:
                return false;
        }
    }
    
    private void setSorting(boolean timezone) {
        //setListAdapter(timezone ? mTimezoneSortedAdapter : mAlphabeticalAdapter);
        mSortedByTimezone = timezone;
        if(null != mTimezoneInfoAdapter){
        	mTimezoneInfoAdapter.setSortByTimezone(mSortedByTimezone);
        	mTimezoneInfoAdapter.notifyDataSetChanged();
        }
    }
}
