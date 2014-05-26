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
	
	private String mStrVCamID = "2e26ea2bccb34937a65dfa02488e58dc";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//mbIgnoreSessionCheck = true;
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mStrVCamID = getIntent().getStringExtra(CameraListActivity.KEY_VCAM_ID);
		
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
				txtTitle.setText(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_NAME));
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
    				monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this), true, mStrVCamID, (System.currentTimeMillis()-BeseyeMMBEHttpTask.SEVEN_DAYS_IN_MS)+"", BeseyeMMBEHttpTask.SEVEN_DAYS_IN_MS+"");
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
			if(0 < miEventCount){
				ListView list  = mMainListView.getRefreshableView();
				if(null != list){
					View vFirstChild = list.getChildAt(list.getHeaderViewsCount());
					if(null != vFirstChild){
						mVgIndicator.calculateTotalLvHeight(miEventCount, vFirstChild.getHeight(), list.getHeight());
						mbNeedToCalcu = false;
					}
				}
			}
		}
	}
	
	private void updateIndicatorPosition(int iFirstIdx){
		ListView list  = mMainListView.getRefreshableView();
		if(null != list && !mbNeedToCalcu && 0 < miEventCount){			
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
		monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this), true, mStrVCamID, (System.currentTimeMillis()-BeseyeMMBEHttpTask.SEVEN_DAYS_IN_MS)+"", BeseyeMMBEHttpTask.SEVEN_DAYS_IN_MS+"");
	}
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode+", "+System.currentTimeMillis());	
		if(!task.isCancelled()){
			if(task instanceof BeseyeMMBEHttpTask.GetEventListTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					miEventCount = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_OBJ_CNT);
					if(0 < miEventCount){
						JSONArray EntList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_OBJ_LST);
						getThumbnailByEventList(EntList);
						JSONObject liveObj = new JSONObject();
						try {
							liveObj.put(BeseyeJSONUtil.MM_START_TIME, (new Date()).getTime());
							liveObj.put(BeseyeJSONUtil.MM_IS_LIVE, true);
							BeseyeJSONUtil.appendObjToArrayBegin(EntList, liveObj);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						
						mEventListAdapter.updateResultList(EntList);
						refreshList();
						if(null != mMainListView){
							mMainListView.onRefreshComplete();
							mMainListView.updateLatestTimestamp();
						}
						checkClockByTime();
					}
				}
			}else if(task instanceof BeseyeMMBEHttpTask.GetThumbnailByEventListTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					JSONArray thumbnailList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_THUMBNAILS);
					if(null != thumbnailList){
						JSONArray EntList = (null != mEventListAdapter)?mEventListAdapter.getJSONList():null;
						if(null != EntList){
							int iEventCount = EntList.length();
							int iCount = thumbnailList.length();
							if(iEventCount == (iCount+1)){
								for(int i = 0;i<iCount;i++){
									try {
										EntList.getJSONObject(i+1).put(BeseyeJSONUtil.MM_THUMBNAIL, thumbnailList.getJSONObject(i));
									} catch (JSONException e) {
										e.printStackTrace();
									}
								}
								refreshList();
							}
						}
					}
				}
			}else{
				Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
		
		if(task.equals(mGetThumbnailByEventListTask)){
			mGetThumbnailByEventListTask = null;
		}
	}
	private BeseyeMMBEHttpTask.GetThumbnailByEventListTask mGetThumbnailByEventListTask;
	private void getThumbnailByEventList(JSONArray EntList){
		int iCount = (null != EntList)?EntList.length():0;
		if(0 < iCount){
			if(null != mGetThumbnailByEventListTask){
				mGetThumbnailByEventListTask.cancel(true);
			}
			JSONObject obj = new JSONObject();
			try {
				obj.put(BeseyeJSONUtil.MM_VCAM_UUID, mStrVCamID);
				obj.put(BeseyeJSONUtil.MM_SIZE, iCount);
				obj.put(BeseyeJSONUtil.MM_MAX_NUM, 3);
				JSONArray timeLst = new JSONArray();
				for(int i = 0;i<iCount;i++){
					JSONObject event = EntList.getJSONObject(i);
					JSONObject time = new JSONObject();
					long lStartTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_START_TIME);
					long lEndTime = BeseyeJSONUtil.getJSONLong(event, BeseyeJSONUtil.MM_END_TIME);
					time.put(BeseyeJSONUtil.MM_START_TIME, lStartTime);
					time.put(BeseyeJSONUtil.MM_DURATION, (0 < lEndTime)?(lEndTime - lStartTime):3000);
					timeLst.put(time);
				}
				obj.put(BeseyeJSONUtil.MM_EVT_LST, timeLst);
				monitorAsyncTask(mGetThumbnailByEventListTask = new BeseyeMMBEHttpTask.GetThumbnailByEventListTask(EventListActivity.this), true, obj.toString());

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
				b.putString(CameraListActivity.KEY_VCAM_ID, getIntent().getStringExtra(CameraListActivity.KEY_VCAM_ID));
				b.putString(CameraListActivity.KEY_VCAM_NAME, getIntent().getStringExtra(CameraListActivity.KEY_VCAM_NAME));
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

	@Override
	protected int getLayoutId() {
		return R.layout.layout_event_list;
	}
}
