package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;

import com.app.beseye.adapter.EventListAdapter;
import com.app.beseye.adapter.EventListAdapter.EventListItmHolder;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeClockIndicator;
import com.app.beseye.widget.BeseyeDatetimePickerDialog;
import com.app.beseye.widget.BeseyeDatetimePickerDialog.OnDatetimePickerClickListener;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

public class EventListActivity extends BeseyeBaseActivity{
	private PullToRefreshListView mMainListView;
	private EventListAdapter mEventListAdapter;
	private ViewGroup mVgEmptyView;
	private BeseyeClockIndicator mVgIndicator;
	private View mVwNavBar;
	private ImageView mIvCancel, mIvFilter, mIvCalendar;
	private ActionBar.LayoutParams mNavBarLayoutParams;

	//private String mStrVCamID = "928d102eab1643eb9f001e0ede19c848";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				//workaround
				//BeseyeJSONUtil.setJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID, mStrVCamID);
				
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
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
				mIvFilter.setVisibility(COMPUTEX_DEMO?View.INVISIBLE:View.VISIBLE);
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
		
		mVgIndicator = (BeseyeClockIndicator)findViewById(R.id.vg_event_indicator);
		
		mMainListView = (PullToRefreshListView) findViewById(R.id.lv_camera_lst);
		
		if(null != mMainListView){
			mMainListView.setVerticalScrollBarEnabled(false);
			mMainListView.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				Log.i(TAG, "onRefresh()");	
    				mbNeedToCalcu = false;
    				monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this), true, mStrVCamID, (System.currentTimeMillis()-BeseyeMMBEHttpTask.ONE_HOUR_IN_MS )+"", BeseyeMMBEHttpTask.ONE_HOUR_IN_MS +"");
    				if(null != mGetThumbnailByEventListTask){
    					mGetThumbnailByEventListTask.cancel(true);
    					mGetThumbnailByEventListTask = null;
    				}
    				//monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this), true, mStrVCamID, "1389918731000", "600000");
    			}

				@Override
				public void onRefreshCancel() {

				}
    		});
			
			mMainListView.setOnScrollListener(new OnScrollListener(){
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					calculateTotalLvHeight();
					updateIndicatorPosition(firstVisibleItem);
				}

				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {
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
//					if(null != mVgEmptyView){
//						TextView txt = (TextView)mVgEmptyView.findViewById(R.id.txtNoItmList);
//						if(null != txt){
//							txt.setText(R.string.no_camera);
//						}
//					}
				}
			}
			
        	mMainListView.setMode(LvExtendedMode.PULL_DOWN_TO_REFRESH);
        	
        	mEventListAdapter = new EventListAdapter(this, null, R.layout.layout_event_list_itm, this);
        	if(null != mEventListAdapter){
        		mMainListView.getRefreshableView().setAdapter(mEventListAdapter);
        	}
		}
	}
	
	private boolean mbNeedToCalcu = true;
	private int miEventCount = 0;
	
	private void calculateTotalLvHeight(){
		if(mbNeedToCalcu){
			if(0 <= miEventCount){
				ListView list  = mMainListView.getRefreshableView();
				if(null != list){
					View vFirstChild = list.getChildAt(list.getHeaderViewsCount()+((miEventCount>0)?1:0));
					if(null != vFirstChild){
						mVgIndicator.calculateTotalLvHeight(miEventCount+1, vFirstChild.getHeight(), list.getHeight());
						mbNeedToCalcu = false;
					}
				}
			}
		}
	}
	
	private void updateIndicatorPosition(int iFirstIdx){
		ListView list  = mMainListView.getRefreshableView();
		if(null != list && !mbNeedToCalcu && 0 <= miEventCount){			
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
		if(0 <= miLastTaskSeedNum){
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
		monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this), true, mStrVCamID, (System.currentTimeMillis()-BeseyeMMBEHttpTask.SEVEN_DAYS_IN_MS*3)+"", BeseyeMMBEHttpTask.SEVEN_DAYS_IN_MS*3 +"");
	}
	
	private int miTaskSeedNum = 0;
	private int miLastTaskSeedNum = -1;
	private int miCurUpdateIdx = -1;
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode+", "+System.currentTimeMillis());	
		if(!task.isCancelled()){
			if(task instanceof BeseyeMMBEHttpTask.GetEventListTask){
				//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
				JSONArray OldEntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
				JSONArray EntList = new JSONArray();
				if(0 == iRetCode){
					//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					miEventCount = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_OBJ_CNT);
					if(0 < miEventCount){
						EntList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_OBJ_LST);
					}
					
					int iOldCount = (null != OldEntList)?OldEntList.length():0;
					if(1 < iOldCount){
						try {
							JSONObject oldFirstobj = OldEntList.getJSONObject(1);
							long lStartTs = BeseyeJSONUtil.getJSONLong(oldFirstobj, BeseyeJSONUtil.MM_START_TIME);
							if(0 < lStartTs){
								for(int idx = 0; idx < miEventCount;idx++){
									if(lStartTs == BeseyeJSONUtil.getJSONLong(EntList.getJSONObject(idx), BeseyeJSONUtil.MM_START_TIME)){
										Log.e(TAG, "onPostExecute(), update old info to new list at "+idx);	
										for(int idx2 = 0; idx2 < iOldCount && (idx+idx2) < miEventCount;idx2++){
											EntList.getJSONObject(idx+idx2).put(BeseyeJSONUtil.MM_THUMBNAIL_PATH, BeseyeJSONUtil.getJSONArray(OldEntList.getJSONObject(1+idx2),BeseyeJSONUtil.MM_THUMBNAIL_PATH));
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
						JSONObject fakeObj = new JSONObject();
						Calendar cal = Calendar.getInstance();
						cal.add(Calendar.MINUTE, -5);
						fakeObj.put(BeseyeJSONUtil.MM_START_TIME, cal.getTime().getTime());
						fakeObj.put(BeseyeJSONUtil.MM_IS_LIVE, false);
						
						BeseyeJSONUtil.appendObjToArrayBegin(EntList, fakeObj);
						
						liveObj.put(BeseyeJSONUtil.MM_START_TIME, (new Date()).getTime());
						liveObj.put(BeseyeJSONUtil.MM_IS_LIVE, true);
						
						BeseyeJSONUtil.appendObjToArrayBegin(EntList, liveObj);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					mEventListAdapter.updateResultList(EntList);
				}
				
				miCurUpdateIdx = 1;
				getThumbnailByEventList(++miTaskSeedNum);
				//getThumbnailByEventList(EntList);
				
//				if(null != EntList && 1 == EntList.length()){
//					calculateTotalLvHeight();
//				}
				
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
											
											
//											JSONObject time = new JSONObject();
//											long lStartTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME);
//											long lEndTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_END_TIME);
//											time.put(BeseyeJSONUtil.MM_START_TIME, lStartTime);
//											time.put(BeseyeJSONUtil.MM_DURATION, (0 < lEndTime)?(lEndTime - lStartTime):3000);
//											
//											String strThbKey = String.format("%s_%s_%s", mStrVCamID, BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_START_TIME), BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_DURATION));
//											if(strThbKey.equals(((BeseyeMMBEHttpTask.GetThumbnailByEventListTask)task).getKey())){
//												Log.e(TAG, "onPostExecute(), assign path "+(null != path)+" find strThbKey:"+strThbKey);	
//												event.put(BeseyeJSONUtil.MM_THUMBNAIL_PATH, (null != path)?new JSONArray(path):BeseyeJSONUtil.getJSONArray(thumbnailList.getJSONObject(0),BeseyeJSONUtil.MM_THUMBNAIL_PATH));
//												if(isItmInScreen(i-1)){
//													refreshList();
//												}
//												break;
//											}
											//EntList.getJSONObject(i+1).put(BeseyeJSONUtil.MM_THUMBNAIL_PATH, BeseyeJSONUtil.getJSONArray(thumbnailList.getJSONObject(i),BeseyeJSONUtil.MM_THUMBNAIL_PATH));
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
	
	private BeseyeMMBEHttpTask.GetThumbnailByEventListTask mGetThumbnailByEventListTask;
//	private void getThumbnailByEventList(JSONArray EntList){
//		int iCount = (null != EntList)?EntList.length():0;
//		if(0 < iCount){
//			if(null != mGetThumbnailByEventListTask){
//				mGetThumbnailByEventListTask.cancel(true);
//			}
//			JSONObject obj = new JSONObject();
//			try {
//				obj.put(BeseyeJSONUtil.MM_VCAM_UUID, mStrVCamID);
//				obj.put(BeseyeJSONUtil.MM_SIZE, "small");
//				obj.put("ContinuousTimeQuery", true);
//				obj.put("urlPathQuery", true);
//				obj.put("urlExpireTime", 300);
//				
//				JSONArray timeLst = new JSONArray();
//				for(int i = 0;i<iCount;i++){
//					JSONObject event = EntList.getJSONObject(i);
//					JSONObject time = new JSONObject();
//					long lStartTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME);
//					long lEndTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_END_TIME);
//					
//					time.put(BeseyeJSONUtil.MM_START_TIME, lStartTime);
//					time.put(BeseyeJSONUtil.MM_DURATION, (0 < lEndTime)?(lEndTime - lStartTime):3000);
//					time.put(BeseyeJSONUtil.MM_MAX_NUM, 10);
//					
//					String strThbKey = String.format("%s_%s_%s", mStrVCamID, BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_START_TIME), BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_DURATION));
//					
//					timeLst.put(time);
//					
//				}
//				obj.put(BeseyeJSONUtil.MM_EVT_LST, timeLst);
//				monitorAsyncTask(new BeseyeMMBEHttpTask.GetThumbnailByEventListTask(EventListActivity.this), true, "", obj.toString());
//				
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//	}
	
	private void getThumbnailByEventList(int iSeed){
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
		int iStartIdx = miCurUpdateIdx;
		if(0 < iCount && iStartIdx < iCount){
			Log.e(TAG, "getThumbnailByEventList(), iCount="+iCount+", iStartIdx="+iStartIdx);
			JSONObject obj = new JSONObject();
			try {
				obj.put(BeseyeJSONUtil.MM_VCAM_UUID, mStrVCamID);
				obj.put(BeseyeJSONUtil.MM_SIZE, "small");
				obj.put("ContinuousTimeQuery", true);
				obj.put("urlPathQuery", true);
				obj.put("urlExpireTime", 300);
				
				JSONArray timeLst = new JSONArray();
				for(int i = iStartIdx;i<iCount && i <(iStartIdx+5);i++, miCurUpdateIdx++){
					JSONObject event = EntList.getJSONObject(i);
					JSONObject time = new JSONObject();
					long lStartTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME);
					long lEndTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_END_TIME);
					
					time.put(BeseyeJSONUtil.MM_START_TIME, lStartTime);
					time.put(BeseyeJSONUtil.MM_DURATION, (0 < lEndTime)?(lEndTime - lStartTime):3000);
					time.put(BeseyeJSONUtil.MM_MAX_NUM, 20);
					
					//String strThbKey = String.format("%s_%s_%s", mStrVCamID, BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_START_TIME), BeseyeJSONUtil.getJSONLong(time, BeseyeJSONUtil.MM_DURATION));
					
					timeLst.put(time);
					
				}
				obj.put(BeseyeJSONUtil.MM_EVT_LST, timeLst);
				monitorAsyncTask(new BeseyeMMBEHttpTask.GetThumbnailByEventListTask(EventListActivity.this, miTaskSeedNum).setDialogId(-1), true, "", obj.toString());
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

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof EventListItmHolder){
			JSONObject event_obj = ((EventListItmHolder)view.getTag()).mObjEvent;
			if(null != event_obj){
				Bundle b = new Bundle();
				b.putString(CameraViewActivity.KEY_TIMELINE_INFO, event_obj.toString());
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
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
			BeseyeDatetimePickerDialog d = new BeseyeDatetimePickerDialog(this); 
			d.setOnDatetimePickerClickListener(new OnDatetimePickerClickListener(){
				@Override
				public void onBtnOKClick(Calendar pickDate) {
					if(!COMPUTEX_DEMO)
						Toast.makeText(EventListActivity.this, "onBtnOKClick(),pickDate="+pickDate.getTime().toLocaleString(), Toast.LENGTH_SHORT).show();
					
					Calendar calendar = Calendar.getInstance();
					calendar.setTime(pickDate.getTime());
					calendar.set(Calendar.MINUTE, 0);
					calendar.set(Calendar.SECOND, 0);
					
					Date timeToCheck= calendar.getTime();
					gotoEventByTime(timeToCheck);
				}
	
				@Override
				public void onBtnCancelClick() {
					if(!COMPUTEX_DEMO)
						Toast.makeText(EventListActivity.this, "onBtnCancelClick(),", Toast.LENGTH_SHORT).show();
				}});
			
			d.show();
		}else
			super.onClick(view);
	}
	
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
				iIdxToJump = iEventCnt;
			}
		}
		
		Log.e(TAG, "gotoEventByTime(), iIdxToJump="+iIdxToJump+", time="+time);
		if(0 <= iIdxToJump){
			//mMainListView.getRefreshableView().smoothScrollToPosition(iIdxToJump);
			//mMainListView.getRefreshableView().setSelection(iIdxToJump);
			
			mMainListView.getRefreshableView().setSelectionFromTop(iIdxToJump, 0);
		}
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_event_list;
	}
}
