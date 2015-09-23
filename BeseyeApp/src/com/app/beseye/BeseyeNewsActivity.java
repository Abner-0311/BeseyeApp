package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.adapter.NewsListAdapter;
import com.app.beseye.adapter.NewsListAdapter.NewsListItmHolder;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeNewsBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnLastItemVisibleListener;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

public class BeseyeNewsActivity extends BeseyeBaseActivity {
	static private final int NUM_NEWS_QUERY = 10;
	
	private PullToRefreshListView mMainListView;
	private NewsListAdapter mNewsListAdapter;
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private boolean mbRefreshCase = false;
	protected JSONArray mlstNews;
	private BeseyeNewsBEHttpTask.GetNewsListTask mGetNewsListTask = null;
	private String mStrLocale = BeseyeUtils.DEF_NEWS_LANG;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG){
			Log.d(TAG, "BeseyeNewsActivity::onCreate()");
			Log.i(TAG, "BeseyeNewsActivity::onCreate(), locale:"+Locale.getDefault());
		}
		super.onCreate(savedInstanceState);
		
		mStrLocale = BeseyeUtils.getLocaleString();		
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
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			//mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}

		mMainListView = (PullToRefreshListView) findViewById(R.id.lst_news_list);
		
		if(null != mMainListView){
			mMainListView.setMode(LvExtendedMode.PULL_DOWN_TO_REFRESH);
			mMainListView.setOnLastItemVisibleListener(new OnLastItemVisibleListener(){
				@Override
				public void onLastItemVisible() {
					if(mMainListView.isFooterLoadMoreViewAttached()){
						if(null == mGetNewsListTask || AsyncTask.Status.FINISHED == mGetNewsListTask.getStatus()){
							int iLastIdx = getLastNewsIdx();
							if(0 <= iLastIdx){
								monitorAsyncTask(mGetNewsListTask = new BeseyeNewsBEHttpTask.GetNewsListTask(BeseyeNewsActivity.this), true, (iLastIdx)+"" , ""+NUM_NEWS_QUERY, mStrLocale);
								return;
							}
							mMainListView.dettachFooterLoadMoreView();
						}
					}
				}});
			mMainListView.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				monitorAsyncTask(mGetNewsListTask = new BeseyeNewsBEHttpTask.GetNewsListTask(BeseyeNewsActivity.this), true, "-1", ""+NUM_NEWS_QUERY, mStrLocale);
    				mbRefreshCase = true;
    			}

				@Override
				public void onRefreshCancel() {

				}
    		});
			
			mNewsListAdapter = new NewsListAdapter(this, mlstNews, R.layout.layout_news_itm, this);
			if(null != mMainListView){
				mMainListView.setAdapter(mNewsListAdapter);
			}
		}
	}
	
	private int getLastNewsIdx(){
		int iRet = -1;
		if(null != mNewsListAdapter){
			JSONArray arrNews = mNewsListAdapter.getJSONList();
			if(null != arrNews && 0 < arrNews.length()){
				try {
					JSONObject newsObj = arrNews.getJSONObject(arrNews.length()-1);
					iRet = BeseyeJSONUtil.getJSONInt(newsObj, BeseyeJSONUtil.NEWS_ID);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return iRet;
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_news_list;
	}
	
	private void postToLvRreshComplete(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(null != mMainListView){
					mMainListView.onRefreshComplete();
					mMainListView.updateLatestTimestamp();
				}
			}}, 0);
	}
	
	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof NewsListItmHolder){
			NewsListItmHolder info = (NewsListItmHolder)view.getTag();
			if(null != info && null != info.mObjEvent){
				BeseyeNewsHistoryMgr.setRead(BeseyeJSONUtil.getJSONInt(info.mObjEvent, BeseyeJSONUtil.NEWS_ID));
				int iType = BeseyeJSONUtil.getJSONInt(info.mObjEvent, BeseyeJSONUtil.NEWS_TYPE);
				if(iType == BeseyeJSONUtil.NEWS_TYPE_ANNOUNCE){
					String strUrl = BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(BeseyeJSONUtil.getJSONObject(info.mObjEvent,BeseyeJSONUtil.NEWS_CONTENT), BeseyeJSONUtil.NEWS_OTHER), BeseyeJSONUtil.NEWS_URL);
					if(null != strUrl && 0 < strUrl.length()){
						Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(strUrl));
						startActivity(browserIntent);
					}
				}else if(iType == BeseyeJSONUtil.NEWS_TYPE_CAM_UPDATE){
					Bundle bundle = getIntent().getExtras();
					bundle.putString(CameraUpdateActivity.KEY_UPDATE_INFO, info.mObjEvent.toString());
					launchActivityByClassName(CameraUpdateActivity.class.getName(),bundle);
				}else{
					Log.i(TAG, "onClick(), iType:"+iType);	
				}
				if(null != mNewsListAdapter){
					mNewsListAdapter.notifyDataSetChanged();
				}
			}
		}else {
			super.onClick(view);
		}
	}
	
	protected void onSessionComplete(){
		super.onSessionComplete();
		monitorAsyncTask(mGetNewsListTask = new BeseyeNewsBEHttpTask.GetNewsListTask(this), true, "-1", ""+NUM_NEWS_QUERY, mStrLocale);
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,String strMsg) {	
		if(task instanceof BeseyeNewsBEHttpTask.GetNewsListTask){
			postToLvRreshComplete();
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		
		if(iErrType == BeseyeHttpTask.ERR_TYPE_NO_CONNECTION){
			onNoNetworkError();
		}
	}
	
	private ViewGroup mVgEmptyView;
	
	@Override
	protected void onServerError(){
		super.onServerError();
		LayoutInflater inflater = getLayoutInflater();
		if(null != inflater){
			mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_fail, null);
			if(null != mVgEmptyView){
				mMainListView.setEmptyView(mVgEmptyView);
			}
		}
	}
	
	private void onNoNetworkError(){
		LayoutInflater inflater = getLayoutInflater();
		if(null != inflater){
			mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_fail, null);
			if(null != mVgEmptyView){
				mMainListView.setEmptyView(mVgEmptyView);
			}
		}
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(DEBUG)
			Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		
		if(!task.isCancelled()){
			if(task instanceof BeseyeNewsBEHttpTask.GetNewsListTask){
				if(mbRefreshCase){
					if(null != mNewsListAdapter){
						mNewsListAdapter.updateResultList(null);
					}
					mbRefreshCase = false;
				}
				if(null != mMainListView)
					mMainListView.dettachFooterLoadMoreView();
					
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
					JSONArray arrNews = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.NEWS_LIST);
					int iCountNew = (null != arrNews)?arrNews.length():0;
					
					JSONArray arrOld = (null != mNewsListAdapter)?mNewsListAdapter.getJSONList():null;
					if(null != arrOld){
						for(int idx = 0;idx<iCountNew;idx++){
							try {
								arrOld.put(arrNews.getJSONObject(idx));
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						arrNews = arrOld;
					}
					
					if(iCountNew == NUM_NEWS_QUERY && null != mMainListView){
						mMainListView.attachFooterLoadMoreView(false, true);
					}
					
					if(null != mNewsListAdapter){
						mNewsListAdapter.updateResultList(arrNews);
						mNewsListAdapter.notifyDataSetChanged();
					}
					
					if(null != arrNews && 0 < arrNews.length()){
						try {
							JSONObject objFirst = arrNews.getJSONObject(0);
							if(null != objFirst){
								BeseyeNewsHistoryMgr.setMaxTouchNewsIdx(BeseyeJSONUtil.getJSONInt(objFirst, BeseyeJSONUtil.NEWS_ID));
							}
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}

				postToLvRreshComplete();
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
		
		if(task == mGetNewsListTask){
			mGetNewsListTask = null;
		}
	}
	
	static public class BeseyeNewsHistoryMgr{
		static private Set<Integer> sNewsReadHistorySet = null;
		static private final String DIVIDER = ";";
		static int siMaxNewsId = -1;
		static int siMaxTouchNewsId = -1;
		static int siShowInd = -1;
		
		static void showValues(){
			if(DEBUG)
				Log.i(TAG, "showValues(), ("+siMaxNewsId+", "+siMaxTouchNewsId+", "+siShowInd+")");
		}
		
		synchronized static public void init(){
			if(null == sNewsReadHistorySet){
				String strHistory = SessionMgr.getInstance().getNewsHistory();
				sNewsReadHistorySet = new TreeSet<Integer>();
				if(null != strHistory && 0 < strHistory.length()){
					String[] toNum = strHistory.split(DIVIDER);
					for(String num : toNum){
						sNewsReadHistorySet.add(Integer.parseInt(num));
					}
				}
				
				siMaxTouchNewsId = SessionMgr.getInstance().getNewsLastMax();
				siShowInd = SessionMgr.getInstance().getNewsShowInd();
				showValues();
			}
		}
		
		synchronized static public void deinit(){
			sNewsReadHistorySet = null;
			SessionMgr.getInstance().setNewsHistory("");
			SessionMgr.getInstance().setNewsLastMax(-1);
			SessionMgr.getInstance().setNewsShowInd(-1);
		}
		
		synchronized static public void setMaxTouchNewsIdx(int iMax){
			init();
			SessionMgr.getInstance().setNewsLastMax(iMax);
			siMaxTouchNewsId = iMax;
			showValues();
		}
		
		synchronized static public int getMaxTouchNewsIdx(){
			init();
			return siMaxTouchNewsId;
		}
		
		synchronized static public int getMaxReadIdx(){
			init();
			if(null != sNewsReadHistorySet && 0 < sNewsReadHistorySet.size()){
				return (Integer) sNewsReadHistorySet.toArray()[sNewsReadHistorySet.size()-1];
			}
			return -1;
		}
		
		synchronized static public boolean isUnread(int idx){
			init();
			return null != sNewsReadHistorySet && !sNewsReadHistorySet.contains(idx);
		}
		
		synchronized static public boolean showNewsIndicator(){
			init();
			showValues();
			return -1 < siMaxNewsId && siShowInd < siMaxNewsId;
		}
		
		synchronized static public void hideNewsIndicator(){
			init();
			siShowInd = siMaxNewsId;
			SessionMgr.getInstance().setNewsShowInd(siShowInd);
			showValues();
		}
		
		synchronized static public void setRead(int idx){
			init();
			if(null != sNewsReadHistorySet){
				sNewsReadHistorySet.add(idx);
				saveHistory();
			}
		}
		
		synchronized static public void setMaxNewId(int idx){
			init();
			siMaxNewsId = idx;
//			if(-1 < siMaxNewsId && siMaxTouchNewsId < siMaxNewsId){
//				siShowInd = true;
//				SessionMgr.getInstance().setNewsShowInd(siShowInd);
//			}
			showValues();
		}
		
		synchronized static public int getMaxNewId(){
			return siMaxNewsId;
		}
		
		synchronized static public boolean haveLatestNews(){
			return siMaxNewsId > 0 && siMaxNewsId > siMaxTouchNewsId;
		}
		
		static private void saveHistory(){
			if(null != sNewsReadHistorySet){
				String strToSave = null;
				for(Integer num : sNewsReadHistorySet){
					if(null != strToSave){
						strToSave+=(DIVIDER+num);
					}else{
						strToSave = num.toString();
					}
				}
				
				if(null != strToSave){
					SessionMgr.getInstance().setNewsHistory(strToSave);
				}
			}
		}
	}
}
