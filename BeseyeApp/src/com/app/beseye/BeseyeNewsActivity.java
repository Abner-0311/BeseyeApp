package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

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

import com.app.beseye.adapter.NewsListAdapter;
import com.app.beseye.adapter.NewsListAdapter.NewsListItmHolder;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshListView;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;

public class BeseyeNewsActivity extends BeseyeBaseActivity {

	private PullToRefreshListView mlvNewsList;
	private NewsListAdapter mNewsListAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	protected JSONArray mlstNews;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "BeseyeNewsActivity::onCreate()");
		super.onCreate(savedInstanceState);
		mbIgnoreSessionCheck = true;
		
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
				txtTitle.setText(R.string.cam_menu_news);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			//mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}

		mlvNewsList = (PullToRefreshListView) findViewById(R.id.lst_news_list);
		
		if(null != mlvNewsList){
			mlvNewsList.setMode(LvExtendedMode.NONE);
			mlstNews = new JSONArray();
			for(int idx =0; idx < 10;idx++)
				mlstNews.put(new JSONObject());
			
			mNewsListAdapter = new NewsListAdapter(this, mlstNews, R.layout.layout_news_itm, this);
			if(null != mlvNewsList){
				mlvNewsList.setAdapter(mNewsListAdapter);
			}
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_news_list;
	}
	
	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof NewsListItmHolder){
			NewsListItmHolder info = (NewsListItmHolder)view.getTag();
			if(null != info){
				//Toast.makeText(this, info.mTimeZone.toString(), Toast.LENGTH_SHORT).show();
//				Intent intent = new Intent();
//				intent.putExtra(HWSettingsActivity.TIME_ZONE_INFO, info.mtxtZoneInfo.getText());
//				setResult(RESULT_OK, intent);
//				finish();
			}
		}else {
			super.onClick(view);
		}
	}
}
