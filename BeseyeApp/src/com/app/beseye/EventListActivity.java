package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

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
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

public class EventListActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener{
	
	private PullToRefreshListView mMainListView;
	private EventListAdapter mEventListAdapter;
	private ViewGroup mVgEmptyView, mVgIndicator;
	private View mVwNavBar;
	private ImageView mIvMenu, mIvAddCam;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//mbIgnoreSessionCheck = true;
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_cam_list_nav, null);
		if(null != mVwNavBar){
			mIvMenu = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_menu_btn);
			if(null != mIvMenu){
				mIvMenu.setOnClickListener(this);
			}
			
			mIvAddCam = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_add_cam_btn);
			if(null != mIvAddCam){
				mIvAddCam.setOnClickListener(this);
			}
			
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_NAME));
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		mVgIndicator = (ViewGroup)findViewById(R.id.vg_event_indicator);
		
		mMainListView = (PullToRefreshListView) findViewById(R.id.lv_camera_lst);
		
		if(null != mMainListView){
			mMainListView.setVerticalScrollBarEnabled(false);
			mMainListView.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				Log.i(TAG, "onRefresh()");	
    				mbNeedToCalcu = false;
    				monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(EventListActivity.this), true, "bes0001", "1389918731000", "6000000");
    			}

				@Override
				public void onRefreshCancel() {

				}
    		});
			
			mMainListView.setOnScrollListener(new OnScrollListener(){
				@Override
				public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
					calculateTotalLvHeight();
					
					View topChild = mMainListView.getRefreshableView().getChildAt(0);
//					if(null != topChild)
//						topChild.setPressed(true);
					Log.i(TAG, "onScroll(), [ "+firstVisibleItem+", "+visibleItemCount+", "+totalItemCount+", "+mMainListView.getRefreshableView().getFirstVisiblePosition()+", "+((null != topChild)?topChild.getBottom():-9999)+"]");	
					updateIndicatorPosition(firstVisibleItem);
					//mMainListView.isHeaderLoadMoreViewAttached();
//    				int lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
//    				int iActualIndex = mMaskTargetListItemIndex;//+(isFakeViewExist()?1:0);
//    				
//    				//Log.i(iKalaUtil.IKALA_APP_TAG, "onScroll(), firstVisibleItem = "+firstVisibleItem+", lastVisibleItem = "+lastVisibleItem+", iActualIndex = "+iActualIndex);
//    				if((-1 < mMaskTargetListItemIndex) && ((iActualIndex < firstVisibleItem) || (iActualIndex > lastVisibleItem))){
//    					removeMaskView();
//    				}
				}

				@Override
				public void onScrollStateChanged(AbsListView view,
						int scrollState) {
//					mScrollState = scrollState;
////					if(Configuration.DEBUG)
////						Log.e(iKalaUtil.IKALA_APP_TAG, "onScrollStateChanged(), mScrollState:"+mScrollState);
//					
//					if(mScrollState != OnScrollListener.SCROLL_STATE_IDLE){
//						cancelRunMaskAnimation();
//					}else{
//						prepareRunMaskAnimation();
//					}
				}});
			
			LayoutInflater inflater = getLayoutInflater();
			if(null != inflater){
				mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_add, null);
				if(null != mVgEmptyView){
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
        	
        	mEventListAdapter = new EventListAdapter(this, null, R.layout.layout_camera_list_itm, this, this);
        	if(null != mEventListAdapter){
        		mMainListView.getRefreshableView().setAdapter(mEventListAdapter);
        	}
		}
	}
	
	private boolean mbNeedToCalcu = true;
	private int miEventCount = 0;
	private int miItemHeight = 0;
	private int miLvHeight = 0;
	private int miTotalHeight = 0;
	private int miIndHeight = 0;
	private int miIndRange = 0;
	
	private void calculateTotalLvHeight(){
		if(mbNeedToCalcu){
			if(0 < miEventCount){
				ListView list  = mMainListView.getRefreshableView();
				if(null != list){
					miLvHeight = list.getHeight();
					View vFirstChild = list.getChildAt(list.getHeaderViewsCount());
					if(null != vFirstChild){
						miItemHeight = vFirstChild.getHeight();
						miTotalHeight = miEventCount*miItemHeight - miLvHeight;
						
						if(null != mVgIndicator){
							miIndHeight = mVgIndicator.getHeight();
							miIndRange = miLvHeight - miIndHeight;
						}
						Log.i(TAG, "calculateTotalLvHeight(), [ "+miItemHeight+", "+miLvHeight+", "+miTotalHeight+", "+miIndHeight+", "+miIndRange+"]");	
						mbNeedToCalcu = false;
					}
				}
			}
		}
	}
	
	private void updateIndicatorPosition(int iFirstIdx){
		ListView list  = mMainListView.getRefreshableView();
		if(null != list && !mbNeedToCalcu){
			int iCurPos = 0, iTop = 0;;
			if(0 < iFirstIdx){
				View topChild = list.getChildAt(0);
				if(null != topChild){
					iCurPos = iFirstIdx*miItemHeight - topChild.getBottom();
				}
			}
			float fRatio = (float)iCurPos/(float)miTotalHeight;
			iTop = (int) (miIndRange*fRatio);
			
			Log.i(TAG, "updateIndicatorPosition(), [ "+iCurPos+", "+fRatio+", "+iTop+", "+miIndRange+"]");	
			
			if(null != mVgIndicator){
				mVgIndicator.layout(mVgIndicator.getLeft(), iTop, mVgIndicator.getLeft()+mVgIndicator.getWidth(), iTop+miIndHeight);
			}
		}
	}
	
	private void refreshList(){
		if(null != mEventListAdapter){
			mEventListAdapter.notifyDataSetChanged();
		}
	}
	
	protected void onSessionComplete(){
		Log.i(TAG, "onSessionComplete()");	
		monitorAsyncTask(new BeseyeMMBEHttpTask.GetEventListTask(this), true, "bes0001", "1389918731000", "6000000");
	}
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeMMBEHttpTask.GetEventListTask){
				if(0 == iRetCode){
					//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					miEventCount = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_OBJ_CNT);
					if(0 < miEventCount){
						JSONArray EntList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_OBJ_LST);
						mEventListAdapter.updateResultList(EntList);
						refreshList();
						if(null != mMainListView)
							mMainListView.onRefreshComplete();
					}
				}
			}else{
				Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof EventListItmHolder){
//			JSONObject cam_obj = ((EventListItmHolder)view.getTag()).mObjCam;
//			if(null != cam_obj){
//				Bundle b = new Bundle();
//				b.putString(CameraListActivity.KEY_VCAM_ID, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
//				b.putString(CameraListActivity.KEY_VCAM_NAME, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME));
//				launchActivityByClassName(CameraViewActivity.class.getName(), b);
//				return;
//			}
		}else if(R.id.iv_nav_menu_btn == view.getId()){
			finish();
		}else if(R.id.iv_nav_add_cam_btn == view.getId()){
			launchActivityByClassName(WifiSetupGuideActivity.class.getName());
		}else
			super.onClick(view);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_event_list;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		if(view.getTag() instanceof EventListItmHolder){
			JSONObject cam_obj = ((EventListItmHolder)view.getTag()).mObjCam;
			if(null != cam_obj){
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_ID, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
				b.putString(CameraListActivity.KEY_VCAM_NAME, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME));
				//launchActivityByClassName(CameraViewActivity.class.getName(), b);
				return;
			}
		}
	}
}
