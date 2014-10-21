package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.TAG;


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.app.beseye.adapter.EventListAdapter;
import com.app.beseye.adapter.EventListAdapter.EventListItmHolder;
import com.app.beseye.adapter.EventListAdapter.IListViewScrollListenser;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.httptask.BeseyeNewsBEHttpTask;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.BlockingLifoQueue;
import com.app.beseye.widget.BeseyeClockIndicator;
import com.app.beseye.widget.BeseyeDatetimePickerDialog;
import com.app.beseye.widget.BeseyeDatetimePickerDialog.OnDatetimePickerClickListener;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnLastItemVisibleListener;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;



public class EventListActivity extends BeseyeBaseActivity implements IListViewScrollListenser{
	
	static private final long THUMBNAIL_URL_EXPIRE = 300L; 
	
	private PullToRefreshListView mMainListView;
	private EventListAdapter mEventListAdapter;
	private ViewGroup mVgEmptyView;
	private BeseyeClockIndicator mVgIndicator;
	private View mVwNavBar;
	private ImageView mIvCancel, mIvFilter, mIvCalendar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private TimeZone mTimeZone;

	//private String mStrVCamID = "928d102eab1643eb9f001e0ede19c848";
	private BeseyeMMBEHttpTask.GetThumbnailByEventListTask mGetThumbnailByEventListTask;
	private BeseyeMMBEHttpTask.GetEventListCountTask mGetEventListCountTask;
	private BeseyeMMBEHttpTask.GetEventListTask mGetEventListTask;
	private BeseyeMMBEHttpTask.GetEventListTask mGetNewEventListTask;
	private boolean mbNeedToLoadNewInNextRound  = false;
	private boolean mbNeedToReloadWhenResume = false;
	private boolean mbNeedToCalcu = true;
	private boolean mbIsScrolling = false;
	private int miTotalEventCount = 0;//Total count from server
	private long mlEventQueryPeriod = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVgIndicator = (BeseyeClockIndicator)findViewById(R.id.vg_event_indicator);
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			updateAttrByCamObj();
		} catch (JSONException e1) {
			Log.e(TAG, "CameraViewActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		//mStrVCamID = getIntent().getStringExtra(CameraListActivity.KEY_VCAM_ID);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_cam_list_nav, null);
		if(null != mVwNavBar){
			mIvCancel = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_menu_btn);
			if(null != mIvCancel){
				mIvCancel.setOnClickListener(this);
				mIvCancel.setImageResource(R.drawable.sl_event_list_cancel);
			}
			
			mIvFilter = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_add_cam_btn);
			if(null != mIvFilter){
				mIvFilter.setOnClickListener(this);
				mIvFilter.setImageResource(R.drawable.sl_event_list_filter);
				mIvFilter.setVisibility(View.INVISIBLE);
			}
			
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(mStrVCamName);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		mIvCalendar = (ImageView)findViewById(R.id.iv_calendar_icon);
		if(null != mIvCalendar){
			mIvCalendar.setOnClickListener(this);
		}
		
		mMainListView = (PullToRefreshListView) findViewById(R.id.lv_camera_lst);
		
		if(null != mMainListView){
			mMainListView.setVerticalScrollBarEnabled(false);
			mMainListView.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				Log.i(TAG, "onRefresh()");	
    				mMainListView.dettachFooterLoadMoreView();
    				mbNeedToCalcu = false;
    				loadEventList();
       			}

				@Override
				public void onRefreshCancel() {

				}
    		});
			
			mMainListView.setOnLastItemVisibleListener(new OnLastItemVisibleListener(){
				@Override
				public void onLastItemVisible() {
					if(mMainListView.isFooterLoadMoreViewAttached()){
						if(null == mGetEventListTask || AsyncTask.Status.FINISHED == mGetEventListTask.getStatus()){
							getEventListContent(miTaskSeedNum);
							//mMainListView.dettachFooterLoadMoreView();
						}
					}
				}});
			
			mMainListView.setOnScrollListener(new OnScrollListener(){
				boolean bNeedToTrack = false;
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					calculateTotalLvHeight();
					updateIndicatorPosition(firstVisibleItem);
				}

				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					if(scrollState == OnScrollListener.SCROLL_STATE_FLING || scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
						bNeedToTrack = true;
						mbIsScrolling = true;
					}else if(scrollState == OnScrollListener.SCROLL_STATE_IDLE && bNeedToTrack){
						bNeedToTrack = false;
						mbIsScrolling = false;
						int iFirstPos = getFirstItem();
						Log.e(TAG, "onScrollStateChanged(), SCROLL_STATE_IDLE, first item is :"+getFirstItem());
						getThunbnailAtPos((0==iFirstPos)?1:iFirstPos-1);
					}
				}});
			
			LayoutInflater inflater = getLayoutInflater();
			if(null != inflater){
				mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_add, null);
				if(null != mVgEmptyView){
					ViewGroup vgHolder = (ViewGroup)mVgEmptyView.findViewById(R.id.vg_msg_holder);
					if(null != vgHolder){
						vgHolder.setVisibility(View.INVISIBLE);
					}
					mMainListView.setEmptyView(mVgEmptyView);
				}
			}
			
        	mMainListView.setMode(LvExtendedMode.PULL_DOWN_TO_REFRESH);
        	
        	mEventListAdapter = new EventListAdapter(this, null, R.layout.layout_event_list_itm, this, this);
        	if(null != mEventListAdapter){
        		JSONArray EntList = new JSONArray();
        		JSONObject liveObj = new JSONObject();
				try {					
					liveObj.put(BeseyeJSONUtil.MM_START_TIME, (new Date()).getTime());
					liveObj.put(BeseyeJSONUtil.MM_IS_LIVE, true);
					
					BeseyeJSONUtil.appendObjToArrayBegin(EntList, liveObj);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				mEventListAdapter.updateResultList(EntList);
				mMainListView.getRefreshableView().setAdapter(mEventListAdapter);
				mVgIndicator.updateToNow(true);
				mVgIndicator.updateDateTime(new Date().getTime());
        	}
		}
	}
	
	public boolean isLvScrolling(){
		return mbIsScrolling;
	}
	
	private void loadEventList(){
		mbNeedToLoadNewInNextRound = false;
		
		if(null != mGetEventListTask){
			mGetEventListTask.cancel(true);
			mGetEventListTask = null;
		}
		
		if(null != mGetNewEventListTask){
			mGetNewEventListTask.cancel(true);
			mGetNewEventListTask = null;
		}
		
		if(null != mGetEventListCountTask){
			mGetEventListCountTask.cancel(true);
			mGetEventListCountTask = null;
		}
		
		if(null != mGetThumbnailByEventListTask){
			mGetThumbnailByEventListTask.cancel(true);
			mGetThumbnailByEventListTask = null;
		}
		
		cancelRunningTasks();
		
		mlTaskTs = System.currentTimeMillis();
		monitorAsyncTask((mGetEventListCountTask = new BeseyeMMBEHttpTask.GetEventListCountTask(EventListActivity.this)), true, mStrVCamID, (mlTaskTs-mlEventQueryPeriod )+"", mlEventQueryPeriod +"");
	}
	
	private void loadNewEventList(){
		JSONArray events = null;
		long lLatestEventTs = -1;
		if(null != mEventListAdapter){
			events = mEventListAdapter.getJSONList();
			JSONObject eventLatest = (null != events && 1 < events.length())?events.optJSONObject(1):null;
			if(null != eventLatest){
				long lStartTime = BeseyeJSONUtil.getJSONLong(eventLatest, BeseyeJSONUtil.MM_START_TIME);
				//long lEndTime = BeseyeJSONUtil.getJSONLong(eventLatest, BeseyeJSONUtil.MM_END_TIME);
				
				lLatestEventTs = lStartTime+1;
				
				Log.i(TAG, "loadNewEventList(), eventLatest:"+eventLatest.toString());	
			}
		}
		
		
		if(-1 < lLatestEventTs){
			if(null != mGetNewEventListTask){
				mGetNewEventListTask.cancel(true);
				mGetNewEventListTask = null;
			}
			monitorAsyncTask(mGetNewEventListTask = new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this, true), true, mStrVCamID, (lLatestEventTs)+"", (System.currentTimeMillis()-lLatestEventTs)+"", "100");
		}else{
			if(null != mGetNewEventListTask){
				Log.i(TAG, "loadNewEventList(), mGetNewEventListTask isn't null, load in next round ");	
				mbNeedToLoadNewInNextRound = true;
			}else{
				loadEventList();
				//monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this), true, mStrVCamID, (System.currentTimeMillis()-mlEventQueryPeriod )+"", mlEventQueryPeriod +"");
			}
		}
		
//		if(null != mGetThumbnailByEventListTask){
//			mGetThumbnailByEventListTask.cancel(true);
//			mGetThumbnailByEventListTask = null;
//		}
	}
	
	private int getCurEventCount(){
		JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
		return (null != EntList)?EntList.length()-1:0;
	}
	
	private void calculateTotalLvHeight(){
		if(mbNeedToCalcu){
			int iEventCount = miTotalEventCount;//getCurEventCount();
			if(0 <= iEventCount){
				ListView list  = mMainListView.getRefreshableView();
				if(null != list){
					View vFirstChild = list.getChildAt(list.getHeaderViewsCount()+((getCurEventCount()>0)?1:0));
					if(null != vFirstChild){
						mVgIndicator.calculateTotalLvHeight(iEventCount+1, vFirstChild.getHeight(), list.getHeight());
						mbNeedToCalcu = false;
					}
				}
			}
		}
	}
	
	private void updateIndicatorPosition(int iFirstIdx){
		ListView list  = mMainListView.getRefreshableView();
		if(null != list && !mbNeedToCalcu && 0 <= miTotalEventCount){			
			if(0 <= iFirstIdx){
				View topChild = list.getChildAt(0 == iFirstIdx?1:0);
				if(null != topChild){
					if(0 == iFirstIdx && 0 < list.getHeaderViewsCount()){
						iFirstIdx = 1;
					}
					mVgIndicator.updateIndicatorPosition(iFirstIdx, topChild.getBottom());
					//Log.i(TAG, "updateIndicatorPosition(), pos = "+mVgIndicator.getIndicatorPos());	
					View view= findLvItmByPos(mVgIndicator.getIndicatorPos());
					if(null != view){
						EventListItmHolder holder = (EventListItmHolder)view.getTag();
						mVgIndicator.updateToNow(BeseyeJSONUtil.getJSONBoolean(holder.mObjEvent, BeseyeJSONUtil.MM_IS_LIVE, false));
						mVgIndicator.updateDateTime(BeseyeJSONUtil.getJSONLong(holder.mObjEvent, BeseyeJSONUtil.MM_START_TIME));
//						if(null != mEventListAdapter && mEventListAdapter.setSelectedItm(iFirstVisiblePos+i - list.getHeaderViewsCount())){
//							mEventListAdapter.notifyDataSetChanged();
//							Log.i(TAG, "findLvItmByPos(), pos = "+iPos+", ["+iFirstVisiblePos+", "+iLastVisiblePos+"], obj="+holder.mObjEvent);	
//						}
					}
				}
			}
		}
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if(mbNeedToReloadWhenResume){
			loadNewEventList();
			mbNeedToReloadWhenResume = false;
		}else if(0 <= miLastTaskSeedNum){
			Log.i(TAG, "onResume(), resume task , miLastTaskSeedNum="+miLastTaskSeedNum);	
			getThumbnailByEventList(miLastTaskSeedNum);
			miLastTaskSeedNum = -1;
		}
	}
	
	private View findLvItmByPos(int iPos){
		View view = null;
		ListView list  = mMainListView.getRefreshableView();
		if(null != list){
			int iFirstVisiblePos = list.getFirstVisiblePosition();
			int iLastVisiblePos = list.getLastVisiblePosition();
			int iVisibleCount = iLastVisiblePos - iFirstVisiblePos +1;
			for(int i = 0; i< iVisibleCount;i++){
				View child = list.getChildAt(i);
				if(null != child && child.getTag() instanceof EventListItmHolder){
					if(child.getTop() < iPos && iPos <=child.getBottom()){
						view = child;
						break;
					}
				}
			}
		}
		return view;
	}
	
	private void refreshList(){
		if(null != mEventListAdapter){
			mEventListAdapter.notifyDataSetChanged();
		}
	}
	
	protected void onSessionComplete(){
		Log.i(TAG, "onSessionComplete()");	
		super.onSessionComplete();
		
		loadEventList();
	}
	
	private long mlTaskTs = -1;
	private int miTaskSeedNum = 0;
	private int miLastTaskSeedNum = -1;
	private int miCurUpdateEventIdx = -1;
	private int miCurUpdateThunbnailIdx = -1;
	private JSONArray mArrOldEventList = null;
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode+", "+System.currentTimeMillis());	
		if(!task.isCancelled()){
			if(task instanceof BeseyeMMBEHttpTask.GetEventListCountTask){
				Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
				miTotalEventCount = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_CNT);
				
				mArrOldEventList = mEventListAdapter.getJSONList();
				
				JSONArray EntList = new JSONArray();
				JSONObject liveObj = new JSONObject();
				try {
					liveObj.put(BeseyeJSONUtil.MM_START_TIME, (new Date()).getTime());
					liveObj.put(BeseyeJSONUtil.MM_IS_LIVE, true);
					EntList.put(liveObj);
//					for(int idx = 0; idx < iCount;idx++){
//						EntList.put(new JSONObject());
//					}
				}catch (JSONException e) {
					e.printStackTrace();
				}
				
				mEventListAdapter.updateResultList(EntList);
				
				if(null != mMainListView){
					mMainListView.onRefreshComplete();
					mMainListView.updateLatestTimestamp();
				}
				
				checkClockByTime();
				mbNeedToCalcu = true;
				miCurUpdateEventIdx = 0;
				miCurUpdateThunbnailIdx = 1;
				getEventListContent(++miTaskSeedNum);
//				getThumbnailByEventList(miTaskSeedNum);
				
			}else if(task instanceof BeseyeMMBEHttpTask.GetEventListTask){
				//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
				
				boolean bHaveFooter = false;
				
				if(null != mMainListView){
					bHaveFooter = mMainListView.isFooterLoadMoreViewAttached();
					mMainListView.dettachFooterLoadMoreView();
				}
				
				int iTaskSeed = ((BeseyeMMBEHttpTask.GetEventListTask)task).iTaskSeed;
				if(0 <= iTaskSeed){
					if(iTaskSeed != miTaskSeedNum){
						Log.e(TAG, "onPostExecute(), iTaskSeed is not equal to miTaskSeedNum");	
					}else{
						if(0 == iRetCode){
							JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
							int iOldCount = (null != EntList)?EntList.length():0;
							int iCount = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_OBJ_CNT);
							if(0 < iCount){
								int iTotalCount = iOldCount+iCount;
								
								JSONArray newEntList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_OBJ_LST);
								for(int idx = 0; idx < iCount;idx++){
									try {
										EntList.put(++miCurUpdateEventIdx, newEntList.opt(idx));
									} catch (JSONException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								
								if(1 == iOldCount && null != mArrOldEventList){
									int iOldCMemCount = (null != mArrOldEventList)?mArrOldEventList.length()-1:0;
									if(1 < iOldCMemCount){
										try {
											JSONObject oldFirstobj = mArrOldEventList.getJSONObject(1);
											long lStartTs = BeseyeJSONUtil.getJSONLong(oldFirstobj, BeseyeJSONUtil.MM_START_TIME);
											if(0 < lStartTs){
												for(int idx = iOldCount; idx < iTotalCount;idx++){
													if(lStartTs == BeseyeJSONUtil.getJSONLong(EntList.getJSONObject(idx), BeseyeJSONUtil.MM_START_TIME)){
														Log.e(TAG, "onPostExecute(), update old info to new list at "+idx);	
														for(int idx2 = 0; idx2 < iOldCMemCount && (idx+idx2) < iTotalCount;idx2++){
															JSONObject newObj = EntList.getJSONObject(idx+idx2);
															JSONObject oldObj = mArrOldEventList.getJSONObject(1+idx2);
															if(null != newObj && null != oldObj){
																newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH, 		BeseyeJSONUtil.getJSONArray(oldObj,BeseyeJSONUtil.MM_THUMBNAIL_PATH));
																newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE,  BeseyeJSONUtil.getJSONArray(oldObj,BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE));
																newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_REQ, 		-1L);
																newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_EXPIRE, 		-1L);
															}
														}
														break;
													}
												}
											}
										} catch (JSONException e) {
											Log.e(TAG, "onPostExecute(), e:"+e.toString());	
										}
									}
									mArrOldEventList = null;
								}
								//getEventListContent(iTaskSeed);
								getThumbnailByEventList(iTaskSeed);
								
								if(null != mTimeWantToReach){
									final Date date = mTimeWantToReach;
									mTimeWantToReach = null;
									BeseyeUtils.postRunnable(new Runnable(){
										@Override
										public void run() {
											gotoEventByTime(date);
										}}, 50);
								}
								
								if(null != mMainListView){
									mMainListView.attachFooterLoadMoreView(false, true);
								}
							}
						}
					}
				}else{
					JSONArray OldEntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
					JSONArray EntList = new JSONArray();
					
					boolean bAppendCase = ((BeseyeMMBEHttpTask.GetEventListTask)task).mbAppend;
					Log.e(TAG, "onPostExecute(), bAppendCase: "+bAppendCase);	
					
					if(0 == iRetCode){
						//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
						int iCount = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_OBJ_CNT);
						if(0 < iCount){
							EntList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_OBJ_LST);
						}
						
						int iOldCount = (null != OldEntList)?OldEntList.length():0;
						if(1 < iOldCount){
							try {
								JSONObject oldFirstobj = OldEntList.getJSONObject(1);
								long lStartTs = BeseyeJSONUtil.getJSONLong(oldFirstobj, BeseyeJSONUtil.MM_START_TIME);
								if(0 < lStartTs){
									for(int idx = 0; idx < iCount;idx++){
										if(lStartTs == BeseyeJSONUtil.getJSONLong(EntList.getJSONObject(idx), BeseyeJSONUtil.MM_START_TIME)){
											Log.e(TAG, "onPostExecute(), update old info to new list at "+idx);	
											for(int idx2 = 0; idx2 < iOldCount && (idx+idx2) < iCount;idx2++){
												JSONObject newObj = EntList.getJSONObject(idx+idx2);
												JSONObject oldObj = OldEntList.getJSONObject(1+idx2);
												if(null != newObj && null != oldObj){
													newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH,       BeseyeJSONUtil.getJSONArray(oldObj,BeseyeJSONUtil.MM_THUMBNAIL_PATH));
													newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE, BeseyeJSONUtil.getJSONArray(oldObj,BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE));
													newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_REQ,        -1L);
													newObj.put(BeseyeJSONUtil.MM_THUMBNAIL_EXPIRE,     -1L);
												}
											}
											break;
										}
									}
								}
							} catch (JSONException e) {
								Log.e(TAG, "onPostExecute(), e:"+e.toString());	
							}
						}
					}
					
					if(null != EntList){
						JSONObject liveObj = new JSONObject();
						try {
							liveObj.put(BeseyeJSONUtil.MM_START_TIME, (new Date()).getTime());
							liveObj.put(BeseyeJSONUtil.MM_IS_LIVE, true);
							
							BeseyeJSONUtil.appendObjToArrayBegin(EntList, liveObj);
							
							int iOldCount = (null != OldEntList)?OldEntList.length():0;
							if(bAppendCase && 1 < iOldCount){
								int iOldEventCount = OldEntList.length();
								JSONObject newEventEnd = (null != EntList && 1 < EntList.length())?EntList.optJSONObject(EntList.length()-1):null;
								long lNewEventEndTs = BeseyeJSONUtil.getJSONLong(newEventEnd, BeseyeJSONUtil.MM_START_TIME, -1);
								boolean bNeedCheck = true;
								for(int idx = 1;idx < iOldEventCount;idx++){
									JSONObject oldEvent = OldEntList.optJSONObject(idx);
									if(null != oldEvent){
										if(!bNeedCheck || -1 == lNewEventEndTs || BeseyeJSONUtil.getJSONLong(oldEvent, BeseyeJSONUtil.MM_START_TIME, -1) < lNewEventEndTs){
											bNeedCheck = false;
											EntList.put(oldEvent);
											//miEventCount++;
											//miCurUpdateEventIdx++;
										}
									}
								}
								
								int iNewCount = EntList.length();
								Log.e(TAG, "onPostExecute(), (iNewCount - iOldCount):"+(iNewCount - iOldCount));	
								miTotalEventCount += (iNewCount - iOldCount);
								miCurUpdateEventIdx += (iNewCount - iOldCount);
								//miEventCount = (null != EntList)?EntList.length()-1:1;
								
								if(mbNeedToLoadNewInNextRound){
									Log.e(TAG, "onPostExecute(), mbNeedToLoadNewInNextRound is true");	
									BeseyeUtils.postRunnable(new Runnable(){
										@Override
										public void run() {
											loadNewEventList();
										}}, 0);
									mbNeedToLoadNewInNextRound = false;
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
						
						if(bHaveFooter && null != mMainListView){
							mMainListView.attachFooterLoadMoreView(false, true);
						}
						
						mEventListAdapter.updateResultList(EntList);
						//recheck form head
						miCurUpdateThunbnailIdx = 1;
						getThumbnailByEventList(miTaskSeedNum);
					}
					
					mbNeedToCalcu = true;
				}
				
				refreshList();
				if(null != mMainListView){
					mMainListView.onRefreshComplete();
					mMainListView.updateLatestTimestamp();
				}
				checkClockByTime();
				
			}else if(task instanceof BeseyeMMBEHttpTask.GetThumbnailByEventListTask){
				if(0 == iRetCode){
					//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					//String path = ((BeseyeMMBEHttpTask.GetThumbnailByEventListTask)task).getPath();
					//JSONArray thumbnailList = null != path ?new JSONArray():BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_THUMBNAILS);
					JSONArray thumbnailList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_THUMBNAILS);
					if(null != thumbnailList){
						JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
						if(null != EntList){
							int iEventCount = EntList.length();
							int iCount = thumbnailList.length();
							if(0 < iCount){
								try {
									JSONObject thumb = thumbnailList.getJSONObject(0);
									long lFirstTs = BeseyeJSONUtil.getJSONLong(thumb, BeseyeJSONUtil.MM_TIMESTAMP);
									//Log.e(TAG, "onPostExecute(), lFirstTs:"+lFirstTs);
									for(int i = 1;i<iEventCount;i++){
										try {
											JSONObject event = EntList.getJSONObject(i);
											if(0 < lFirstTs && lFirstTs == BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME)){
												boolean bRefreshFlag = false;
												for(int idx = 0; idx < iCount;idx++){
													event = EntList.getJSONObject(i + idx);
													event.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH, BeseyeJSONUtil.getJSONArray(thumbnailList.getJSONObject(idx),BeseyeJSONUtil.MM_THUMBNAIL_PATH));
													event.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE, BeseyeJSONUtil.getJSONArray(thumbnailList.getJSONObject(idx),BeseyeJSONUtil.MM_THUMBNAIL_PATH_CACHE));

													event.put(BeseyeJSONUtil.MM_THUMBNAIL_EXPIRE, System.currentTimeMillis()+THUMBNAIL_URL_EXPIRE);
													if(!bRefreshFlag && isItmInScreen(i + idx)){
														Log.e(TAG, "onPostExecute(), refresh for "+(i + idx));
														bRefreshFlag = true;
													}
												}
												
												if(bRefreshFlag){
													refreshList();
												}
												break;
											}

										} catch (JSONException e) {
											e.printStackTrace();
										}
									}
								} catch (JSONException e1) {
									e1.printStackTrace();
								}
								
								//refreshList();
							}
						}
					}
				}
				final int iseed = ((BeseyeMMBEHttpTask.GetThumbnailByEventListTask)task).getTaskSeed();
				BeseyeUtils.postRunnable(new Runnable(){
					@Override
					public void run() {
						getThumbnailByEventList(iseed);
					}}, 500);
				
			}else{
				Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
		
		if(task.equals(mGetThumbnailByEventListTask)){
			mGetThumbnailByEventListTask = null;
		}else if(task.equals(mGetEventListTask)){
			mGetEventListTask = null;
		}else if(task.equals(mGetNewEventListTask)){
			mGetNewEventListTask = null;
		}else if(task.equals(mGetEventListCountTask)){
			mGetEventListCountTask = null;
		}
	}
	
	private boolean isItmInScreen(int itmIdx){
		boolean bRet = false;
		if(null != mMainListView && null != mMainListView.getRefreshableView()){
			int iFirstItm = mMainListView.getRefreshableView().getFirstVisiblePosition();
			int iLastItm = mMainListView.getRefreshableView().getLastVisiblePosition();
			bRet = (iFirstItm <= itmIdx) && (itmIdx <= iLastItm);
		}
		return bRet;
	}
	
	private int getFirstItem(){
		int iRet = -1;
		if(null != mMainListView && null != mMainListView.getRefreshableView()){
			iRet = mMainListView.getRefreshableView().getFirstVisiblePosition();
		}
		return iRet;
	}
	
	private int getItemCountInView(){
		int iRet = 0;
		if(null != mMainListView && null != mMainListView.getRefreshableView()){
			iRet = mMainListView.getRefreshableView().getLastVisiblePosition() - mMainListView.getRefreshableView().getFirstVisiblePosition()+1;
		}
		return iRet;
	}
	
	private void getEventListContent(int iSeed){
		if(iSeed != miTaskSeedNum || mActivityDestroy){
			Log.e(TAG, "getEventListContent(), iSeed="+iSeed+", miTaskSeedNum="+miTaskSeedNum+", mActivityDestroy="+mActivityDestroy);
			return;
		}
		
		if(!mActivityResume){
			Log.e(TAG, "getEventListContent(), mActivityResume="+mActivityResume+", iSeed="+iSeed);
			miLastTaskSeedNum  = iSeed;
			return;
		}
		
		JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
		
		if(null != EntList && miCurUpdateEventIdx >= EntList.length()){
			Log.e(TAG, "getEventListContent(), miCurUpdateEventIdx="+miCurUpdateEventIdx+", EntList.length()="+EntList.length());
			return;
		}
		
		JSONObject lastObj = (null != EntList)?EntList.optJSONObject(miCurUpdateEventIdx):null;
		
		if(null != lastObj){
			long lStartTime = BeseyeJSONUtil.getJSONLong(lastObj, BeseyeJSONUtil.MM_START_TIME, -1);
			long lDuration = mlEventQueryPeriod - ((-1 == lStartTime)?0:(mlTaskTs-lStartTime+1)) ;
			Log.e(TAG, "getEventListContent(), miCurUpdateEventIdx:"+miCurUpdateEventIdx+", lDuration:"+lDuration+", lastObj="+lastObj.toString()+", mTimeWantToReach:"+((mTimeWantToReach!=null)?mTimeWantToReach.toLocaleString():""));
			if(null != mGetEventListTask){
				mGetEventListTask.cancel(true);
				mGetEventListTask = null;
			}
			monitorAsyncTask((mGetEventListTask = new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this, iSeed)).setDialogId( (null != mTimeWantToReach)?DIALOG_ID_LOADING:-1), true, mStrVCamID, (mlTaskTs-mlEventQueryPeriod )+"", lDuration+"", (0 == getCurEventCount())?"15":(null != mTimeWantToReach)?"10000":"100");
		}
	}
	
	static final private int THUMBNAIL_BUNDLE_SIZE = 1;
	static final private int THUMBNAIL_JUMP_BUNDLE_SIZE = 5;
	static final private int THUMBNAIL_NUM = 10;
	
	private static ExecutorService THUNBNAIL_TASK_EXECUTOR; 
	static {  
		//THUNBNAIL_TASK_EXECUTOR = (ExecutorService) Executors.newFixedThreadPool(5); 
		THUNBNAIL_TASK_EXECUTOR = new ThreadPoolExecutor(3, 5, 0L, TimeUnit.MILLISECONDS, new BlockingLifoQueue<Runnable>());
	}
	
	private void getThumbnailByEventList(int iSeed){
		if(null != mGetThumbnailByEventListTask && iSeed <= miTaskSeedNum){
			Log.e(TAG, "getThumbnailByEventList(), mGetThumbnailByEventListTask is not null, iSeed="+iSeed);
			return;
		}
		
		if(iSeed != miTaskSeedNum || mActivityDestroy){
			Log.e(TAG, "getThumbnailByEventList(), iSeed="+iSeed+", miTaskSeedNum="+miTaskSeedNum+", mActivityDestroy="+mActivityDestroy);
			return;
		}
		
		if(!mActivityResume){
			Log.e(TAG, "getThumbnailByEventList(), mActivityResume="+mActivityResume+", iSeed="+iSeed);
			miLastTaskSeedNum  = iSeed;
			return;
		}
		
		JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
		int iCount = (null != EntList)?EntList.length():0;
		int iStartIdx = miCurUpdateThunbnailIdx;
		if(0 < iCount && iStartIdx < iCount){
			Log.e(TAG, "getThumbnailByEventList(), iCount="+iCount+", iStartIdx="+iStartIdx);
			JSONObject obj = new JSONObject();
			try {
				obj.put(BeseyeJSONUtil.MM_VCAM_UUID, mStrVCamID);
				obj.put(BeseyeJSONUtil.MM_SIZE, "small");
				obj.put("ContinuousTimeQuery", true);
				obj.put("urlPathQuery", true);
				obj.put("urlExpireTime", THUMBNAIL_URL_EXPIRE);
				
				JSONArray timeLst = new JSONArray();
				int iCountToExpectGet = (1 == iStartIdx)?1:THUMBNAIL_BUNDLE_SIZE;
				int iCountToGet = 0;
				for(int i = iStartIdx; i<iCount && iCountToGet < iCountToExpectGet;i++, miCurUpdateThunbnailIdx++){
					JSONObject event = EntList.getJSONObject(i);
					long lExpireTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_THUMBNAIL_EXPIRE, -1);
					if(null != BeseyeJSONUtil.getJSONArray(event, BeseyeJSONUtil.MM_THUMBNAIL_PATH) || (-1 < lExpireTime && System.currentTimeMillis() > lExpireTime)){
						Log.e(TAG, "getThumbnailByEventList(), skip "+i);
						continue;
					}
					
					BeseyeJSONUtil.setJSONLong(event, BeseyeJSONUtil.MM_THUMBNAIL_REQ, System.currentTimeMillis());
					JSONObject time = new JSONObject();
					long lStartTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME);
					long lEndTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_END_TIME, 0L);
					
					time.put(BeseyeJSONUtil.MM_START_TIME, lStartTime);
					long lDuration = (0 == lEndTime || (lEndTime - lStartTime) < 20000)?20000:(lEndTime - lStartTime);
					time.put(BeseyeJSONUtil.MM_DURATION, lDuration);
					time.put(BeseyeJSONUtil.MM_MAX_NUM, THUMBNAIL_NUM);
					
					//String strThbKey = String.format("%s_%s_%s", mStrVCamID, BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_START_TIME), BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_DURATION));
					
					timeLst.put(time);
					iCountToGet++;
				}
				
				if(0 < iCountToGet){
					obj.put(BeseyeJSONUtil.MM_EVT_LST, timeLst);
					Log.e(TAG, "getThumbnailByEventList(), obj:"+obj);
					monitorAsyncTask((mGetThumbnailByEventListTask = new BeseyeMMBEHttpTask.GetThumbnailByEventListTask(EventListActivity.this, iSeed)).setDialogId(-1), true, "", obj.toString());
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void getThunbnailAtPos(int iPos){
		JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
		int iCount = (null != EntList)?EntList.length():0;
		int iStartIdx = ((iPos + THUMBNAIL_JUMP_BUNDLE_SIZE >= iCount)? (iCount-THUMBNAIL_JUMP_BUNDLE_SIZE):iPos);//-(null != mMainListView ?mMainListView.getRefreshableView().getHeaderViewsCount():0);
		
		if(0 < iCount && iStartIdx < iCount){
			Log.e(TAG, "getThunbnailAtPos(), iCount="+iCount+", iStartIdx="+iStartIdx);
			JSONObject obj = new JSONObject();
			try {
				obj.put(BeseyeJSONUtil.MM_VCAM_UUID, mStrVCamID);
				obj.put(BeseyeJSONUtil.MM_SIZE, "small");
				obj.put("ContinuousTimeQuery", true);
				obj.put("urlPathQuery", true);
				obj.put("urlExpireTime", THUMBNAIL_URL_EXPIRE);
				
				JSONArray timeLst = new JSONArray();
				int iCountToExpectGet = iStartIdx+THUMBNAIL_JUMP_BUNDLE_SIZE;
				for(int i = iStartIdx; i < iCountToExpectGet;i++){
					JSONObject event = EntList.getJSONObject(i);
					long lExpireTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_THUMBNAIL_EXPIRE, -1);
					if(null != BeseyeJSONUtil.getJSONArray(event, BeseyeJSONUtil.MM_THUMBNAIL_PATH) || 
					   (System.currentTimeMillis() - BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_THUMBNAIL_REQ)) < 10000 ||
					   (-1 < lExpireTime && System.currentTimeMillis() > lExpireTime)){
						continue;
					}
					
					BeseyeJSONUtil.setJSONLong(event, BeseyeJSONUtil.MM_THUMBNAIL_REQ, System.currentTimeMillis());
					
					JSONObject time = new JSONObject();
					long lStartTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME);
					long lEndTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_END_TIME, 0L);
					
					time.put(BeseyeJSONUtil.MM_START_TIME, lStartTime);
					long lDuration = (0 == lEndTime || (lEndTime - lStartTime) < 20000)?20000:(lEndTime - lStartTime);
					time.put(BeseyeJSONUtil.MM_DURATION, lDuration);
					time.put(BeseyeJSONUtil.MM_MAX_NUM, THUMBNAIL_NUM);
					
					//String strThbKey = String.format("%s_%s_%s", mStrVCamID, BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_START_TIME), BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_DURATION));
					
					timeLst.put(time);
				}
				
				if(0 < timeLst.length()){
					Log.e(TAG, "getThunbnailAtPos(), timeLst="+timeLst.toString());
					obj.put(BeseyeJSONUtil.MM_EVT_LST, timeLst);
					monitorAsyncTask((new BeseyeMMBEHttpTask.GetThumbnailByEventListTask(EventListActivity.this, miTaskSeedNum)).setDialogId(-1), true, THUNBNAIL_TASK_EXECUTOR, "", obj.toString());
				}
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void checkClockByTime(){
		Date now = new Date();
		long lTimeToCheck = ((60 - now.getSeconds())*1000);
		//Log.i(TAG, "checkClockByTime(), now : "+now.toLocaleString()+", lTimeToCheck:"+lTimeToCheck);	
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
				if(null != EntList && 0 < EntList.length()){
					try {
						JSONObject liveObj = EntList.getJSONObject(0);
						if(null != liveObj){
							liveObj.put(BeseyeJSONUtil.MM_START_TIME, (new Date()).getTime());
							View view= findLvItmByPos(mVgIndicator.getIndicatorPos());
							if(null != view){
								EventListItmHolder holder = (EventListItmHolder)view.getTag();
								if(liveObj.equals(holder.mObjEvent)){
									mVgIndicator.updateDateTime(BeseyeJSONUtil.getJSONLong(holder.mObjEvent, BeseyeJSONUtil.MM_START_TIME));
								}
							}
							checkClockByTime();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}}, lTimeToCheck);
	}
	
	private int getEventPeriodByPlan(){
		int iRet = -1;//temp workaround 0;
		int iPlan = BeseyeJSONUtil.getJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_PLAN);
		switch(iPlan){
			case 1:{
				iRet = -1;
				break;
			}
			case 2:{
				iRet = -7;
				break;
			}
			case 3:{
				iRet = -30;
				break;
			}
		}
		return iRet;
	}
	
	private void setEventQueryPeriodByPlan(){
		int iPlan = BeseyeJSONUtil.getJSONInt(mCam_obj, BeseyeJSONUtil.ACC_VCAM_PLAN);
		switch(iPlan){
			case 1:{
				mlEventQueryPeriod = BeseyeMMBEHttpTask.ONE_DAY_IN_MS;
				break;
			}
			case 2:{
				mlEventQueryPeriod = BeseyeMMBEHttpTask.SEVEN_DAYS_IN_MS;
				break;
			}
			case 3:{
				mlEventQueryPeriod = BeseyeMMBEHttpTask.THIRTY_DAYS_IN_MS;
				break;
			}
			default:{
				mlEventQueryPeriod = BeseyeMMBEHttpTask.ONE_DAY_IN_MS;
			}
		}
		
		Log.e(TAG, "setEventQueryPeriodByPlan(), iPlan="+iPlan+", mlEventQueryPeriod="+mlEventQueryPeriod);
	}

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof EventListItmHolder){
			JSONObject event_obj = ((EventListItmHolder)view.getTag()).mObjEvent;
			if(null != event_obj){
				Bundle b = new Bundle();
				b.putString(CameraViewActivity.KEY_TIMELINE_INFO, event_obj.toString());
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				b.putBoolean(CameraListActivity.KEY_DEMO_CAM_MODE, getIntent().getBooleanExtra(CameraListActivity.KEY_DEMO_CAM_MODE, false));
				Intent intent = new Intent();
				intent.setClassName(this, CameraViewActivity.class.getName());
				intent.putExtras(b);
				//intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
				//intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
				launchActivityByIntent(intent);
				
				//launchActivityByClassName(CameraViewActivity.class.getName(), b);
				return;
			}
		}else if(R.id.iv_nav_menu_btn == view.getId()){
			finish();
		}else if(R.id.iv_nav_add_cam_btn == view.getId()){
			//launchActivityByClassName(WifiSetupGuideActivity.class.getName());
		}else if(R.id.iv_calendar_icon == view.getId()){
			BeseyeDatetimePickerDialog d = new BeseyeDatetimePickerDialog(this, new Date(), getEventPeriodByPlan()); 
			d.setOnDatetimePickerClickListener(new OnDatetimePickerClickListener(){
				@Override
				public void onBtnOKClick(Calendar pickDate) {
//					if(!COMPUTEX_DEMO)
//						Toast.makeText(EventListActivity.this, "onBtnOKClick(),pickDate="+pickDate.getTime().toLocaleString(), Toast.LENGTH_SHORT).show();
					
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(pickDate.getTime());
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					
					Date timeToCheck= calendar.getTime();
					gotoEventByTime(timeToCheck);
				}
	
				@Override
				public void onBtnCancelClick() {
//					if(!COMPUTEX_DEMO)
//						Toast.makeText(EventListActivity.this, "onBtnCancelClick(),", Toast.LENGTH_SHORT).show();
				}});
			
			d.show();
		}else
			super.onClick(view);
	}
	
	private Date mTimeWantToReach = null;
	
	private void gotoEventByTime(Date time){
		JSONArray evtList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
		int iEventCnt = (null != evtList)?evtList.length():0;
		long lTimeToCheck = time.getTime();
		int iIdxToJump = -1;
		ListView listview = mMainListView.getRefreshableView();
		if(0 < iEventCnt && null != listview){
			int idx = 1;
			for(; idx < iEventCnt;idx++){
				JSONObject event;
				try {
					event = evtList.getJSONObject(idx);
					long lStartTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME);
					if(lTimeToCheck > lStartTime){
						iIdxToJump = (idx-1) + listview.getHeaderViewsCount();
						Log.i(TAG, "gotoEventByTime(), lStartTime="+new Date(lStartTime));
						break;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			if(1 == iIdxToJump){
				//iIdxToJump = 2;
			}else if(idx == iEventCnt && -1 == iIdxToJump){
				Log.e(TAG, "gotoEventByTime(), can not find time ");
				iIdxToJump = miTotalEventCount;
			}
		}
		
		Log.e(TAG, "gotoEventByTime(), iIdxToJump="+iIdxToJump+", time="+time);
		if(0 <= iIdxToJump){
			//mMainListView.getRefreshableView().smoothScrollToPosition(iIdxToJump);
			//mMainListView.getRefreshableView().setSelection(iIdxToJump);
			if(miCurUpdateEventIdx < (miTotalEventCount-1) && miCurUpdateEventIdx < iIdxToJump){
				Log.e(TAG, "gotoEventByTime(), need to get further event, miCurUpdateEventIdx:"+miCurUpdateEventIdx+", iEventCnt:"+iEventCnt+", miTotalEventCount:"+miTotalEventCount);
				mTimeWantToReach = time;
				getEventListContent(miTaskSeedNum);
				return;
			}
			final int iJumpTo = iIdxToJump;
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					mMainListView.getRefreshableView().setSelectionFromTop(iJumpTo, 0);
					JSONArray evtList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
					JSONObject jumpObj = (null != evtList && iJumpTo < evtList.length())?evtList.optJSONObject(iJumpTo):null;
					if(null != jumpObj && null == BeseyeJSONUtil.getJSONArray(jumpObj, BeseyeJSONUtil.MM_THUMBNAIL_PATH)){
						Log.e(TAG, "gotoEventByTime(), need to load thunbnail for jumpObj:"+jumpObj);
						getThunbnailAtPos(iJumpTo-1);
					}
				}}, 200);
			
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_event_list;
	}
	
	public void onCamSetupChanged(String strVcamId, long lTs, JSONObject objCamSetup){
		super.onCamSetupChanged(strVcamId, lTs, objCamSetup);
		if(strVcamId.equals(mStrVCamID)){
			updateAttrByCamObj();
		}
    }
	
	private void updateAttrByCamObj(){
		if(null != mCam_obj){
			Log.i(TAG, "CameraViewActivity::updateAttrByIntent(), mCam_obj:"+mCam_obj.toString());
			mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
			mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			mTimeZone = TimeZone.getTimeZone(BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA), BeseyeJSONUtil.CAM_TZ, TimeZone.getDefault().getID()));
			if(null != mVgIndicator){
				mVgIndicator.updateTimeZone(mTimeZone);
			}
			setEventQueryPeriodByPlan();
		}
	}
	
	private boolean checkEventById(JSONObject msgObj){
		Log.i(TAG, getClass().getSimpleName()+"::checkEventById(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(null != mStrVCamID && mStrVCamID.equals(strCamUID)){
    				if(!mActivityDestroy){
    		    		if(!mActivityResume){
    		    			mbNeedToReloadWhenResume = true;
    		    		}else{
    		    			loadNewEventList();
    		    		}
    		    	}
    			}
    			return true;
    		}
		}
    	return false;
	}
	
	protected boolean onCameraMotionEvent(JSONObject msgObj){
		return checkEventById(msgObj);
	}
	
    protected boolean onCameraPeopleEvent(JSONObject msgObj){
    	return checkEventById(msgObj);
    }
    
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
}
