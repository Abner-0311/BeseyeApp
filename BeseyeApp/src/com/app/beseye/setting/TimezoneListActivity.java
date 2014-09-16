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

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.R;
import com.app.beseye.adapter.TimezoneInfoAdapter;
import com.app.beseye.adapter.TimezoneInfoAdapter.TimezoneInfoHolder;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshListView;

public class TimezoneListActivity extends BeseyeBaseActivity {
	public final static String KEY_TZ = "KEY_TZ";
	private PullToRefreshListView mlvTimezoneList;
	private TimezoneInfoAdapter mTimezoneInfoAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private String mStrCurTZ = null;
	
	protected List<BeseyeTimeZone> mlstTimeZone;
	
	public static class BeseyeTimeZone{
		public TimeZone tz;
		public String strDisplayName;
		
		BeseyeTimeZone(String strId, String strDisplayName){
			tz = TimeZone.getTimeZone(strId);
			this.strDisplayName = strDisplayName;
		}
		
		public String getDisplayName(){
			return strDisplayName;
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "TimezoneListActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		getSupportActionBar().setIcon(R.drawable.sl_nav_back_btn);
		getSupportActionBar().setDisplayShowHomeEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		
		mStrCurTZ = getIntent().getStringExtra(KEY_TZ);
		
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
			generateTimeZoneList();
			
			mTimezoneInfoAdapter = new TimezoneInfoAdapter(this, mlstTimeZone, R.layout.timezone_list_item, this);
			if(null != mTimezoneInfoAdapter){
				mTimezoneInfoAdapter.setCurrentTZ(mStrCurTZ);
				mlvTimezoneList.setAdapter(mTimezoneInfoAdapter);
			}
		}
	}
	
	private void generateTimeZoneList(){
		mlstTimeZone = new ArrayList<BeseyeTimeZone>();
		mlstTimeZone.add(new BeseyeTimeZone("ROC", getString(R.string.tz_asia_taiwan)));
		mlstTimeZone.add(new BeseyeTimeZone("PRC", getString(R.string.tz_asia_china)));
		mlstTimeZone.add(new BeseyeTimeZone("Japan",  getString(R.string.tz_asia_japan)));
		mlstTimeZone.add(new BeseyeTimeZone("Hongkong",  getString(R.string.tz_asia_hk)));
		mlstTimeZone.add(new BeseyeTimeZone("ROK",  getString(R.string.tz_asia_korea)));
		mlstTimeZone.add(new BeseyeTimeZone("Singapore",  getString(R.string.tz_asia_singpore)));
		
		mlstTimeZone.add(new BeseyeTimeZone("US/Eastern",  getString(R.string.tz_us_eastern)));
		mlstTimeZone.add(new BeseyeTimeZone("US/Central",  getString(R.string.tz_us_central)));
		mlstTimeZone.add(new BeseyeTimeZone("US/Pacific",  getString(R.string.tz_us_pacific)));
		mlstTimeZone.add(new BeseyeTimeZone("US/Mountain",  getString(R.string.tz_us_mountain)));
		mlstTimeZone.add(new BeseyeTimeZone("US/Alaska",  getString(R.string.tz_us_alaska)));
		mlstTimeZone.add(new BeseyeTimeZone("US/Hawaii",  getString(R.string.tz_us_hawaii)));
		mlstTimeZone.add(new BeseyeTimeZone("US/Arizona",  getString(R.string.tz_us_arizona)));
		
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Atlantic",  getString(R.string.tz_can_atlantic)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Newfoundland",  getString(R.string.tz_can_newfoundland)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Eastern",  getString(R.string.tz_can_eastern)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Central",  getString(R.string.tz_can_central)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Mountain",  getString(R.string.tz_can_mountain)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Pacific",  getString(R.string.tz_can_pacific)));
		
		mlstTimeZone.add(new BeseyeTimeZone("NZ",  getString(R.string.tz_new_zealand)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Eastern",  getString(R.string.tz_aus_eastern)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Eastern",  getString(R.string.tz_aus_western)));
		mlstTimeZone.add(new BeseyeTimeZone("Canada/Eastern",  getString(R.string.tz_aus_central)));
		
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Copenhagen",  getString(R.string.tz_euro_denmark)));
		mlstTimeZone.add(new BeseyeTimeZone("Atlantic/Reykjavik",  getString(R.string.tz_euro_iceland)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Athens",  getString(R.string.tz_euro_greece)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Warsaw",  getString(R.string.tz_euro_poland)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Paris",  getString(R.string.tz_euro_france)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Helsinki",  getString(R.string.tz_euro_finland)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/London",  getString(R.string.tz_euro_uk)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Oslo",  getString(R.string.tz_euro_norway)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Prague",  getString(R.string.tz_euro_czech)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Amsterdam",  getString(R.string.tz_euro_holland)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Zurich",  getString(R.string.tz_euro_swiss)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Stockholm",  getString(R.string.tz_euro_sweden)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Berlin",  getString(R.string.tz_euro_germany)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Brussels",  getString(R.string.tz_euro_belgium)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Budapest",  getString(R.string.tz_euro_hungary)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Madrid",  getString(R.string.tz_euro_spain)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Vienna",  getString(R.string.tz_euro_austria)));
		mlstTimeZone.add(new BeseyeTimeZone("Europe/Rome",  getString(R.string.tz_euro_italy)));
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
				intent.putExtra(HWSettingsActivity.TIME_ZONE_INFO, info.mTimeZone.tz.getID());
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
