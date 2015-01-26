package com.app.beseye;


import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;

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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.adapter.CameraListAdapter;
import com.app.beseye.adapter.CameraListAdapter.CameraListItmHolder;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeHttpTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.httptask.BeseyeNewsBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.httptask.SessionMgr.SERVER_MODE;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.NetworkMgr;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.CameraListMenuAnimator;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

public class CameraListActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener{
	static public final String KEY_VCAM_OBJ 	= "KEY_VCAM_OBJ";
	static public final String KEY_VCAM_ID 		= "KEY_VCAM_ID";
	static public final String KEY_VCAM_NAME 	= "KEY_VCAM_NAME";
	static public final String KEY_VCAM_ADMIN 	= "KEY_VCAM_ADMIN";
	static public final String KEY_VCAM_UPSIDEDOWN 	= "KEY_VCAM_UPSIDEDOWN";
	
	static public final String KEY_DEMO_CAM_MODE 	= "KEY_DEMO_CAM_MODE";
	static public final String KEY_DEMO_CAM_INFO 	= "KEY_DEMO_CAM_INFO";
	static public final String KEY_VALID_CAM_INFO 	= "KEY_VALID_CAM_INFO";
	
	static public final String KEY_PRIVATE_CAM_MODE 	= "KEY_PRIVATE_CAM_MODE";
	
	private PullToRefreshListView mMainListView;
	private CameraListAdapter mCameraListAdapter;
	private ViewGroup mVgEmptyView;
	private View mVwNavBar;
	private ImageView mIvMenu, mIvAddCam, mIvNewsInd;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	private boolean mbIsDemoCamMode = false;
	private boolean mbIsPrivateCamMode = false;
	private JSONObject mVCamListInfoObj = null;
	private Bundle mBundleDemo;
	private Bundle mBundlePrivate;
	private CameraListMenuAnimator mCameraListMenuAnimator;
	private Runnable mPendingRunnableOnCreate = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mbIsDemoCamMode = getIntent().getBooleanExtra(KEY_DEMO_CAM_MODE, false);
		if(mbIsDemoCamMode){
			String strVCamListInfo = getIntent().getStringExtra(KEY_DEMO_CAM_INFO);
			if(null != strVCamListInfo && 0 < strVCamListInfo.length()){
				try {
					mVCamListInfoObj = new JSONObject(strVCamListInfo);
				} catch (JSONException e) {
					Log.i(TAG, "onCreate(), e:"+e.toString());	
				}
			}
		}else{
			mBundleDemo = new Bundle();
			mBundleDemo.putBoolean(KEY_DEMO_CAM_MODE, true);
		}
		
		mbIsPrivateCamMode = getIntent().getBooleanExtra(KEY_PRIVATE_CAM_MODE, false);
		if(mbIsPrivateCamMode){
			String strVCamListInfo = getIntent().getStringExtra(KEY_DEMO_CAM_INFO);
			if(null != strVCamListInfo && 0 < strVCamListInfo.length()){
				try {
					mVCamListInfoObj = new JSONObject(strVCamListInfo);
				} catch (JSONException e) {
					Log.i(TAG, "onCreate(), e:"+e.toString());	
				}
			}
		}else{
			mBundlePrivate = new Bundle();
			mBundlePrivate.putBoolean(KEY_PRIVATE_CAM_MODE, true);
		}
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_cam_list_nav, null);
		if(null != mVwNavBar){
			mIvMenu = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_menu_btn);
			if(null != mIvMenu){
				mIvMenu.setOnClickListener(this);
				//mIvMenu.setVisibility(COMPUTEX_DEMO?View.INVISIBLE:View.VISIBLE);
			}
			
			mIvNewsInd = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_news);
			
			mIvAddCam = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_add_cam_btn);
			if(null != mIvAddCam){
				mIvAddCam.setOnClickListener(this);
				mIvAddCam.setVisibility((mbIsDemoCamMode || mbIsPrivateCamMode)?View.INVISIBLE:View.VISIBLE);
			}
			
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				if(mbIsDemoCamMode)
					txtTitle.setText(R.string.cam_menu_demo_cam);
				
				if(mbIsPrivateCamMode)
					txtTitle.setText(R.string.cam_menu_private_demo_cam);
				
				txtTitle.setOnClickListener(this);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.FILL_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		mMainListView = (PullToRefreshListView) findViewById(R.id.lv_camera_lst);
		
		if(null != mMainListView){
			mMainListView.setOnRefreshListener(new OnRefreshListener() {
    			@Override
    			public void onRefresh() {
    				Log.i(TAG, "onRefresh()");	
    				monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this), true);
    			}

				@Override
				public void onRefreshCancel() {

				}
    		});
			
			addCamListBgView();
			
        	mMainListView.setMode(LvExtendedMode.PULL_DOWN_TO_REFRESH);
        	
        	mCameraListAdapter = new CameraListAdapter(this, null, R.layout.layout_camera_list_itm, this, this);
        	if(null != mCameraListAdapter){
        		mMainListView.getRefreshableView().setAdapter(mCameraListAdapter);
        		mCameraListAdapter.setIsDemoCamList(mbIsDemoCamMode);
        	}
		}
		
		ViewGroup mVgMenu = (ViewGroup)findViewById(R.id.vg_cam_menu);
		if(null != mVgMenu){
			mCameraListMenuAnimator = new CameraListMenuAnimator(this, mVgMenu);
			if(true == SessionMgr.getInstance().getShowPirvateCam())
				mCameraListMenuAnimator.showPrivateCam();
		}
		
		if(getIntent().getBooleanExtra(CameraViewActivity.KEY_PAIRING_DONE, false) && 
		   (null == savedInstanceState || !savedInstanceState.getBoolean(CameraViewActivity.KEY_PAIRING_DONE_HANDLED, false))){
			mPendingRunnableOnCreate = new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle(getIntent().getExtras());
					launchActivityByClassName(CameraViewActivity.class.getName(), b);
					getIntent().putExtra(CameraViewActivity.KEY_PAIRING_DONE, false);
				}};
			if(BeseyeConfig.DEBUG)
				Log.i(TAG, "handle pairing done case");	
			
		}else if(getIntent().getBooleanExtra(OpeningPage.KEY_EVENT_FLAG, false)){
			mPendingRunnableOnCreate = new Runnable(){
				@Override
				public void run() {
					Intent intentEvt = getIntent().getParcelableExtra(OpeningPage.KEY_EVENT_INTENT);
					launchActivityByIntent(intentEvt);
					getIntent().putExtra(OpeningPage.KEY_EVENT_FLAG, false);
				}};
		}
	}
	
	private void checkLatestNew(){
		if(null != mIvNewsInd){
			mIvNewsInd.setVisibility(BeseyeNewsActivity.BeseyeNewsHistoryMgr.showNewsIndicator()?View.VISIBLE:View.GONE);
		}
		
		if(null != mCameraListMenuAnimator)
			mCameraListMenuAnimator.checkNewsStatus();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		if(null != mOnResumeUpdateCamListRunnable){
			if(BeseyeConfig.DEBUG)
				Log.i(TAG, "onResume(), mOnResumeUpdateCamListRunnable trigger...");
    		BeseyeUtils.postRunnable(mOnResumeUpdateCamListRunnable, 0);
    		mOnResumeUpdateCamListRunnable = null;
    	}else if(0 <= miLastTaskSeedNum){
    		if(BeseyeConfig.DEBUG)
    			Log.i(TAG, "onResume(), resume task , miLastTaskSeedNum="+miLastTaskSeedNum);	
			updateCamItm(miLastTaskSeedNum);
			miLastTaskSeedNum = -1;
		}
		
		if(false == mbFirstResume){
			monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this).setDialogId(-1), true);
			monitorAsyncTask(new BeseyeNewsBEHttpTask.GetLatestNewsTask(this).setDialogId(-1), true, BeseyeNewsActivity.DEF_NEWS_LANG);
		}
		
		checkLatestNew();
		
		if(null != mPendingRunnableOnCreate){
			BeseyeUtils.postRunnable(mPendingRunnableOnCreate, 100);
			mPendingRunnableOnCreate = null;
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if(getIntent().getBooleanExtra(CameraViewActivity.KEY_PAIRING_DONE, false)){
			outState.putBoolean(CameraViewActivity.KEY_PAIRING_DONE_HANDLED, true);
		}
	}

	private void refreshList(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(null != mCameraListAdapter){
					mCameraListAdapter.notifyDataSetChanged();
				}
			}}, 0);
	}
	
	protected void onSessionComplete(){
		if(BeseyeConfig.DEBUG)
			Log.i(TAG, "onSessionComplete()");	
		super.onSessionComplete();
		if((!mbIsDemoCamMode && !mbIsPrivateCamMode)|| null == mVCamListInfoObj){
			monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
			monitorAsyncTask(new BeseyeNewsBEHttpTask.GetLatestNewsTask(this).setDialogId(-1), true, BeseyeNewsActivity.DEF_NEWS_LANG);
			monitorAsyncTask(new BeseyeAccountTask.GetUserInfoTask(this), true);
		}else{
			fillVCamList(mVCamListInfoObj);
		}
	}
	
	protected int miOriginalVcamCnt = -1;
	
	private void fillVCamList(JSONObject objVCamList){
		//Log.d(TAG, "fillVCamList(), objVCamList="+objVCamList.toString());
		mObjVCamList = objVCamList;
		
		JSONArray arrCamListOld = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
		
		JSONArray arrCamList = new JSONArray();
		int iVcamCnt = BeseyeJSONUtil.getJSONInt(objVCamList, BeseyeJSONUtil.ACC_VCAM_CNT);
		//miOriginalVcamCnt = iVcamCnt;
		if(BeseyeConfig.DEBUG)
			Log.e(TAG, "fillVCamList(), miOriginalVcamCnt="+miOriginalVcamCnt);
		
		if(0 < iVcamCnt){
			JSONArray VcamList = BeseyeJSONUtil.getJSONArray(objVCamList, BeseyeJSONUtil.ACC_VCAM_LST);
			if(!mbIsDemoCamMode && !mbIsPrivateCamMode){
				for(int i = 0;i< iVcamCnt;i++){
					try {
						JSONObject camObj = VcamList.getJSONObject(i);
						if(/*!DEMO_CAM_ID.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID)) && */BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
							arrCamList.put(camObj);
							String strVcamId = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID);
							BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(strVcamId, camObj);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				mBundleDemo.putString(KEY_DEMO_CAM_INFO, objVCamList.toString());
				mBundlePrivate.putString(KEY_DEMO_CAM_INFO, objVCamList.toString());
				
				if(checkCamUpdateValid()){
					resumeCamUpdate(arrCamList);
				}
			}else{
				mVCamListInfoObj = objVCamList;
			}
			
			miOriginalVcamCnt = arrCamList.length();
			if(mbIsDemoCamMode){
				int iDemoVcamCnt = BeseyeJSONUtil.getJSONInt(objVCamList, BeseyeJSONUtil.ACC_DEMO_VCAM_CNT);
				if(0 < iDemoVcamCnt){
					JSONArray DemoVcamList = BeseyeJSONUtil.getJSONArray(objVCamList, BeseyeJSONUtil.ACC_DEMO_VCAM_LST);
					for(int i = 0; i < iDemoVcamCnt;i++){
						try {
							JSONObject camObj = DemoVcamList.getJSONObject(i);
							if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
								arrCamList.put(camObj);
							}
							//VcamList.put(DemoVcamList.get(i));
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			if(mbIsPrivateCamMode){
				int iPrivateVcamCnt = BeseyeJSONUtil.getJSONInt(objVCamList, BeseyeJSONUtil.ACC_PRIVATE_VCAM_CNT);
				if(0 < iPrivateVcamCnt){
					JSONArray PrivateVcamList = BeseyeJSONUtil.getJSONArray(objVCamList, BeseyeJSONUtil.ACC_PRIVATE_VCAM_LST);
					for(int i = 0; i < iPrivateVcamCnt;i++){
						try {
							JSONObject camObj = PrivateVcamList.getJSONObject(i);
							if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
								arrCamList.put(camObj);
							}
							//VcamList.put(DemoVcamList.get(i));
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			//reuse info
			if(null != arrCamListOld && 0 < arrCamListOld.length()){
				for(int idx = 0; idx < arrCamList.length();idx++){
					JSONObject newObj = arrCamList.optJSONObject(idx);
					if(null != newObj){
						String strNewVcamid = BeseyeJSONUtil.getJSONString(newObj, BeseyeJSONUtil.ACC_ID);
						if(null != strNewVcamid){
							for(int idxOld = 0; idxOld < arrCamListOld.length();idxOld++){
								JSONObject oldObj = arrCamListOld.optJSONObject(idxOld);
								if(null != oldObj && strNewVcamid.equals(BeseyeJSONUtil.getJSONString(oldObj, BeseyeJSONUtil.ACC_ID))){
									try {
										newObj.put(BeseyeJSONUtil.ACC_DATA, BeseyeJSONUtil.getJSONObject(oldObj, BeseyeJSONUtil.ACC_DATA));
										arrCamListOld.put(idxOld, null);
									} catch (JSONException e) {
										e.printStackTrace();
									}
									break;
								}
							}
						}
					}
				}
				
				for(int idxOld = 0; idxOld < arrCamListOld.length();idxOld++){
					JSONObject oldObj = arrCamListOld.optJSONObject(idxOld);
					if(null != oldObj){
						BeseyeJSONUtil.setJSONBoolean(oldObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED, false);
						BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(BeseyeJSONUtil.getJSONString(oldObj, BeseyeJSONUtil.ACC_ID), oldObj);
						BeseyeStorageAgent.doDeleteCacheByFolder(this, BeseyeJSONUtil.getJSONString(oldObj, BeseyeJSONUtil.ACC_ID));
					}
				}
			}		
			
			if(null != mCameraListAdapter){
				mCameraListAdapter.updateResultList(arrCamList);
				refreshList();
			}
			
			if(null == arrCamList || 0 == arrCamList.length()){
				addCamAddingView();
			}
			
			postToLvRreshComplete();
			miCurUpdateIdx = 0;
			updateCamItm(++miTaskSeedNum);
		}
		
		if(isAppVersionChecked() && !isCamUpdating()){
			getCamUpdateCandidateList(mObjVCamList);
			mObjVCamList = null;
		}
	}
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		if(BeseyeConfig.DEBUG)
			Log.d(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					JSONObject objVCamList = result.get(0);
					fillVCamList(objVCamList);
				}
			}else if (task instanceof BeseyeAccountTask.GetUserInfoTask){
				if(0 == iRetCode){
					JSONObject obj = result.get(0);
					if(null != obj){
						//Log.i(TAG, "onPostExecute(), obj "+obj);
						JSONObject objUser = BeseyeJSONUtil.getJSONObject(obj, BeseyeJSONUtil.ACC_USER);
						if(null != objUser){
							if(BeseyeJSONUtil.getJSONBoolean(objUser, BeseyeJSONUtil.ACC_BESEYE_ACCOUNT)){
								SessionMgr.getInstance().setShowPirvateCam(true);
								mCameraListMenuAnimator.showPrivateCam();
							}
						}
					}
				}
			}else if(task instanceof BeseyeCamBEHttpTask.GetCamSetupTask){
				if(0 == iRetCode){
					JSONObject dataObj = BeseyeJSONUtil.getJSONObject(result.get(0), BeseyeJSONUtil.ACC_DATA);
					final String strVcamId = ((BeseyeCamBEHttpTask.GetCamSetupTask)task).getVcamId();
					final int iSeedNum=((BeseyeCamBEHttpTask.GetCamSetupTask)task).getTaskSeed();
					//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> VCAMID = "+strVcamId+", result.get(0)="+result.get(0).toString());
					JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
					int iCount = (null != arrCamList)?arrCamList.length():0;
					for(int i = 0;i < iCount;i++){
						try {
							JSONObject camObj = arrCamList.getJSONObject(i);
							//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> camObj = "+camObj.toString());
							if(strVcamId.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
								//camObj.put(BeseyeJSONUtil.ACC_VCAM_CONN_STATE, BeseyeJSONUtil.getJSONInt(dataObj, BeseyeJSONUtil.CAM_STATUS));
								camObj.put(BeseyeJSONUtil.OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
								camObj.put(BeseyeJSONUtil.ACC_DATA, dataObj);
								//Need broadcast????
								if(0 > iSeedNum)
									BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(strVcamId, camObj);
								
								refreshList();
								BeseyeUtils.postRunnable(new Runnable(){

									@Override
									public void run() {
										if(mActivityResume){
											monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(CameraListActivity.this, iSeedNum).setDialogId(-1), true, strVcamId);
										}
									}}, 200);
								
								break;
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
					
					if(0 > iSeedNum && null != mOnResumeUpdateCamInfoRunnable && mOnResumeUpdateCamInfoRunnable.isSameVCamId(strVcamId)){
						if(DEBUG)
							Log.e(TAG, "onPostExecute(), remove mOnResumeUpdateCamInfoRunnable due to strVcamId ="+strVcamId);
						mOnResumeUpdateCamInfoRunnable = null;
					}
				}
				
				updateCamItm(((BeseyeCamBEHttpTask.GetCamSetupTask)task).getTaskSeed());
			}else if(task instanceof BeseyeMMBEHttpTask.GetLatestThumbnailTask){
				if(0 == iRetCode){
					//Log.d(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					String strVcamId = ((BeseyeMMBEHttpTask.GetLatestThumbnailTask)task).getVcamId();
					//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> VCAMID = "+strVcamId+", result.get(0)="+result.get(0).toString());
					JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
					int iCount = (null != arrCamList)?arrCamList.length():0;
					for(int i = 0;i < iCount;i++){
						try {
							JSONObject camObj = arrCamList.getJSONObject(i);
							//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> camObj = "+camObj.toString());
							if(strVcamId.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
								camObj.put(BeseyeJSONUtil.ACC_VCAM_THUMB, BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(result.get(0), BeseyeJSONUtil.MM_THUMBNAIL), "url"));
								//Log.e(TAG, "onPostExecute(), GetLatestThumbnailTask=> camObj = "+camObj.toString());
								refreshList();
								break;
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}else if(task instanceof BeseyeCamBEHttpTask.SetCamStatusTask){
				if(0 == iRetCode){
					String strVcamId = ((BeseyeCamBEHttpTask.SetCamStatusTask)task).getVcamId();
					//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> VCAMID = "+strVcamId+", result.get(0)="+result.get(0).toString());
					JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
					int iCount = (null != arrCamList)?arrCamList.length():0;
					for(int i = 0;i < iCount;i++){
						try {
							JSONObject camObj = arrCamList.getJSONObject(i);
							//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> camObj = "+camObj.toString());
							if(strVcamId.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
								JSONObject dataObj = BeseyeJSONUtil.getJSONObject(camObj, BeseyeJSONUtil.ACC_DATA);
								if(null != dataObj){
									dataObj.put(BeseyeJSONUtil.CAM_STATUS, BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.CAM_STATUS));
								}
								//camObj.put(BeseyeJSONUtil.ACC_VCAM_CONN_STATE, BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.CAM_STATUS));
								camObj.put(BeseyeJSONUtil.OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
								BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(strVcamId, camObj);
								//monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(-1), true, strVcamId);
								refreshList();
								break;
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}else if(task instanceof BeseyeNewsBEHttpTask.GetLatestNewsTask){
				if(0 == iRetCode){
					int iLatestNewId = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.NEWS_NEWS_ID);
					int iCurLatestReadNewId = BeseyeNewsActivity.BeseyeNewsHistoryMgr.getMaxReadIdx();
					if(DEBUG)
						Log.e(TAG, "onPostExecute(), iLatestNewId:"+iLatestNewId+"=> iCurLatestReadNewId = "+iCurLatestReadNewId);
					BeseyeNewsActivity.BeseyeNewsHistoryMgr.setMaxNewId(iLatestNewId);
					checkLatestNew();
				}
			}else{
				//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeAccountTask.GetVCamListTask){
			postToLvRreshComplete();
		}else if(task instanceof BeseyeCamBEHttpTask.SetCamStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, getResources().getString(R.string.cam_setting_fail_to_update_cam_status));
					showMyDialog(DIALOG_ID_WARNING, b);
					
					refreshList();
				}}, 0);
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
		
		if(iErrType == BeseyeHttpTask.ERR_TYPE_NO_CONNECTION){
			onNoNetworkError();
		}
	}
	
	private int miTaskSeedNum = 0;
	private int miLastTaskSeedNum = -1;
	private int miCurUpdateIdx = -1;
	
//	private void getCamsInfo(JSONArray arrCamList, final int seed){
//		int iCount = (null != arrCamList)?arrCamList.length():0;
//		for(int i = 0;i < iCount;i++){
//			try {
//				final JSONObject camObj = arrCamList.getJSONObject(i);
//				
//
//			} catch (JSONException e) {
//				e.printStackTrace();
//			}
//		}
//	}
	
	private void updateCamItm(int iSeed){
		if(iSeed != miTaskSeedNum || mActivityDestroy){
			Log.e(TAG, "updateCamItm(), iSeed="+iSeed+", miTaskSeedNum="+miTaskSeedNum+", mActivityDestroy="+mActivityDestroy);
			return;
		}
		
//		if(!mActivityResume){
//			Log.e(TAG, "updateCamItm(), mActivityResume="+mActivityResume+", iSeed="+iSeed);
//			miLastTaskSeedNum  = iSeed;
//			return;
//		}
		
		if(null != mCameraListAdapter){
			JSONArray arrCamList = mCameraListAdapter.getJSONList();
			int iCount = (null != arrCamList)?arrCamList.length():0;
			try {
				if(miCurUpdateIdx < iCount){
					final JSONObject camObj = arrCamList.getJSONObject(miCurUpdateIdx++);
					
					monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this, iSeed).setDialogId(-1), true, BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID));
					//monitorAsyncTask(new BeseyeMMBEHttpTask.GetLiveStreamTask(CameraListActivity.this, iSeed).setDialogId(-1), true, BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID), "false");
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
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
	
	private void startSoundPairingProcess(String strVcamId, boolean bFakeProcess){
		Bundle b = new Bundle();
		b.putInt(SoundPairingActivity.KEY_ORIGINAL_VCAM_CNT, miOriginalVcamCnt);
		b.putBoolean(PairingRemindActivity.KEY_ADD_CAM_FROM_LIST, true);
		b.putBoolean(SoundPairingActivity.KEY_CHANGE_WIFI_BEBEBE, null != strVcamId && 0 < strVcamId.length());
		b.putBoolean(SoundPairingActivity.KEY_FAKE_PROCESS, bFakeProcess);
		if(null != strVcamId && 0 < strVcamId.length()){
			if(BeseyeConfig.DEBUG)
				Log.e(TAG, "startSoundPairingProcess(), strVcamId="+strVcamId);
			b.putString(SoundPairingActivity.KEY_CHANGE_WIFI_VCAM, strVcamId);
		}
		//launchActivityByClassName(PairingRemindActivity.class.getName(), b);
		launchActivityByClassName(WifiListActivity.class.getName(), b);

	}

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof CameraListItmHolder){
			JSONObject cam_obj = ((CameraListItmHolder)view.getTag()).mObjCam;
			if(null != cam_obj){
				if(R.id.tv_camera_more == view.getId()){
					startSoundPairingProcess(BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID), false);
				}else{
					Bundle b = new Bundle();
					b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
					b.putBoolean(KEY_DEMO_CAM_MODE, mbIsDemoCamMode);
					launchActivityForResultByClassName(CameraViewActivity.class.getName(), b, REQUEST_CAM_VIEW_CHANGE);
					//Log.e(TAG, "onClick(), "+cam_obj.toString());
				}
			}
		}else if(R.id.iv_nav_menu_btn == view.getId()){
			if(View.VISIBLE != mCameraListMenuAnimator.getVisibility()){
				BeseyeNewsActivity.BeseyeNewsHistoryMgr.hideNewsIndicator();
				checkLatestNew();
			}
			toggleMenu();
			miShowMoreCountMenu++;
		}else if(R.id.iv_nav_add_cam_btn == view.getId()){
			startSoundPairingProcess("", false);
		}else if(R.id.vg_my_cam == view.getId()){
			if(mCameraListMenuAnimator.isPrivateCamShow()){
				if(mbIsDemoCamMode || mbIsPrivateCamMode){
					launchActivityByClassName(CameraListActivity.class.getName(), null);
				}
				showMenu();
			}else{
				if(mbIsDemoCamMode){
					finish();
				}else{
					showMenu();
				}
			}
		}else if(R.id.vg_news == view.getId()){
			Bundle bundle = new Bundle();
			JSONArray arrCamLst = new JSONArray();;
			if(mbIsDemoCamMode || mbIsPrivateCamMode){
				int iVcamCnt = BeseyeJSONUtil.getJSONInt(mVCamListInfoObj, BeseyeJSONUtil.ACC_VCAM_CNT);
				if(0 < iVcamCnt){
					JSONArray VcamList = BeseyeJSONUtil.getJSONArray(mVCamListInfoObj, BeseyeJSONUtil.ACC_VCAM_LST);
					for(int i = 0;i< iVcamCnt;i++){
						try {
							JSONObject camObj = VcamList.getJSONObject(i);
							if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
								arrCamLst.put(camObj);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}

			}else{
				arrCamLst = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
			}
			bundle.putString(KEY_VALID_CAM_INFO, (null != arrCamLst)?arrCamLst.toString():"");
			
			launchActivityByClassName(BeseyeNewsActivity.class.getName(), bundle);
			showMenu();
		}else if(R.id.vg_demo_cam == view.getId()){
			if(!mbIsDemoCamMode){
				launchActivityByClassName(CameraListActivity.class.getName(), mBundleDemo);
			}
			showMenu();
		}else if(R.id.vg_private_cam == view.getId()){
			if(!mbIsPrivateCamMode){
				launchActivityByClassName(CameraListActivity.class.getName(), mBundlePrivate);
			}
			showMenu();
		}else if(R.id.vg_about == view.getId()){
			launchActivityByClassName(BeseyeAboutActivity.class.getName(), null);
			showMenu();
		}else if(R.id.vg_support == view.getId()){
			showMenu();
		}else if(R.id.vg_logout == view.getId()){
			invokeLogout();
			showMenu();
		}else if(R.id.txt_nav_title == view.getId()){
			if(BeseyeConfig.DEBUG && SessionMgr.getInstance().getServerMode().ordinal() <= SERVER_MODE.MODE_STAGING_TOKYO.ordinal()){
				++miShowMoreCount;
				if( miShowMoreCount == 1 && miShowMoreCountMenu >= 6){
					if(null != mCameraListAdapter){
						mCameraListAdapter.setShowMore(true);
						refreshList();
					}
					
					if(null != mCameraListMenuAnimator){
						mCameraListMenuAnimator.showPrivateCam();
					}
					miShowMoreCount = 0;
					miShowMoreCountMenu = 0;
				}
//				else if( miShowMoreCount == 2){
//					startSoundPairingProcess("", true);
//					miShowMoreCount = 0;
//				}
			}
		}else
			super.onClick(view);
	}
	
	private int miShowMoreCount = 0;
	private int miShowMoreCountMenu = 0;
	
	private void toggleMenu(){
		if(null != mCameraListMenuAnimator && !mCameraListMenuAnimator.isInAnimation()){
			if(View.VISIBLE == mCameraListMenuAnimator.getVisibility()){
				hideMenu();
			}else{
				showMenu();
			}
		}
	}
	
	private void hideMenu(){
		if(null != mCameraListMenuAnimator && !mCameraListMenuAnimator.isInAnimation()){
			mCameraListMenuAnimator.performMenuAnimation();
		}
	}
	
	private void showMenu(){
		if(null != mCameraListMenuAnimator && !mCameraListMenuAnimator.isInAnimation()){
			mCameraListMenuAnimator.performMenuAnimation();
		}
	}
	
	static public final int REQUEST_CAM_VIEW_CHANGE = 1;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_CAM_VIEW_CHANGE == requestCode){
			if(resultCode == RESULT_OK){
				//monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this), true);
//				try {
//					JSONObject Cam_obj = new JSONObject(intent.getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
//					if(null != Cam_obj){
//						JSONArray camArr = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
//						int iCount = (null != camArr)?camArr.length():0;
//						String strVcamId = BeseyeJSONUtil.getJSONString(Cam_obj, BeseyeJSONUtil.ACC_ID);
//						for(int i = 0;i<iCount;i++){
//							JSONObject obj = camArr.getJSONObject(i);
//							if(null != obj && BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_ID).equals(strVcamId)){
//								camArr.put(i, Cam_obj);
//								//BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.ACC_NAME, BeseyeJSONUtil.getJSONString(Cam_obj, BeseyeJSONUtil.ACC_NAME));
//								//BeseyeJSONUtil.setJSONInt(obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, BeseyeJSONUtil.getJSONInt(Cam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE));
//								refreshList();
//								break;
//							}
//						}
//					}
//					
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
			}
		}else
			super.onActivityResult(requestCode, resultCode, intent);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.layout_camera_list;
	}

	@Override
	public void onSwitchBtnStateChanged(SwitchState state, View view) {
		if(view.getTag() instanceof CameraListItmHolder){
			JSONObject cam_obj = ((CameraListItmHolder)view.getTag()).mObjCam;
			monitorAsyncTask(new BeseyeCamBEHttpTask.SetCamStatusTask(this).setDialogId(DIALOG_ID_SETTING), true, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID), SwitchState.SWITCH_ON.equals(state)?"1":"0");
		}
	}
	
	public void onCamSetupChanged(String strVcamId, long lTs, JSONObject objCamSetup){
		if(null == strVcamId){
			Log.e(TAG, "CameraListActivity::onCamSetupChanged(), strVcamId is null");
			return;
		}
		JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
		int iCount = (null != arrCamList)?arrCamList.length():0;
		for(int i = 0;i < iCount;i++){
			try {
				JSONObject camObj = arrCamList.getJSONObject(i);
				if(strVcamId.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
					if(BeseyeConfig.DEBUG)
						Log.i(TAG, "CameraListActivity::onCamSetupChanged(),  lTs = "+lTs+", objCamSetup="+objCamSetup.toString());
					
					if(lTs >= BeseyeJSONUtil.getJSONLong(camObj, BeseyeJSONUtil.OBJ_TIMESTAMP)){
						
						arrCamList.put(i,objCamSetup);
						
						if(mActivityResume && mbNeedToCheckCamOnStatus && (mbisCamOnBeforeRefresh && BeseyeJSONUtil.isCamPowerOff(objCamSetup))){
							Toast.makeText(getApplicationContext(), String.format(getString(R.string.notify_cam_off_detect_cam_list), BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_NAME)), Toast.LENGTH_SHORT).show();
						}
						
						refreshList();
						
						if(null != mOnResumeUpdateCamInfoRunnable && mOnResumeUpdateCamInfoRunnable.isSameVCamId(strVcamId)){
							if(BeseyeConfig.DEBUG)
								Log.e(TAG, "CameraListActivity::onCamSetupChanged(), remove mOnResumeUpdateCamInfoRunnable due to strVcamId ="+strVcamId);
							mOnResumeUpdateCamInfoRunnable = null;
						}
						break;
					}
					mbNeedToCheckCamOnStatus = false;
				}
			}catch (JSONException e) {
				e.printStackTrace();
			}
		}
    }
	boolean mbNeedToCheckCamOnStatus = false;
	boolean mbisCamOnBeforeRefresh = false;
	
	protected void onCamSettingChangedCallback(JSONObject DataObj){
    	//super.onCamSettingChangedCallback(DataObj);
    	if(null != DataObj){
			final String strCamUID = BeseyeJSONUtil.getJSONString(DataObj, WS_ATTR_CAM_UID);
			//long lTs = BeseyeJSONUtil.getJSONLong(DataObj, WS_ATTR_TS);
			if(!mActivityDestroy){
	    		if(!mActivityResume){
	    			if(null == mOnResumeUpdateCamInfoRunnable || mOnResumeUpdateCamInfoRunnable.isSameVCamId(strCamUID)){
	    				setOnResumeUpdateCamInfoRunnable(new OnResumeUpdateCamInfoRunnable(strCamUID));
	    			}else{
	    				setOnResumeUpdateCamListRunnable(new Runnable(){
							@Override
							public void run() {
								monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true);
		
							}});
	    				mOnResumeUpdateCamInfoRunnable = null;
	    			}
	    			
	    		}else{
	    			JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
	    			int iCount = (null != arrCamList)?arrCamList.length():0;
	    			for(int i = 0;i < iCount;i++){
	    				try {
	    					JSONObject camObj = arrCamList.getJSONObject(i);
	    					if(strCamUID.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
	    						mbisCamOnBeforeRefresh = BeseyeJSONUtil.isCamPowerOn(camObj);
	    					}
	    				}catch (JSONException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    			
	    			mbNeedToCheckCamOnStatus = true;
	    			
	    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, strCamUID);
	    		}
	    	}
		}
    }
	
    protected boolean onCameraOnline(JSONObject msgObj){
    	if(BeseyeConfig.DEBUG)
    		Log.i(TAG, getClass().getSimpleName()+"::onCameraOnline(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(!mActivityDestroy){
    	    		if(!mActivityResume){
    	    			if(null == mOnResumeUpdateCamInfoRunnable || mOnResumeUpdateCamInfoRunnable.isSameVCamId(strCamUID)){
    	    				setOnResumeUpdateCamInfoRunnable(new OnResumeUpdateCamInfoRunnable(strCamUID));
    	    			}else{
    	    				setOnResumeUpdateCamListRunnable(new Runnable(){
    							@Override
    							public void run() {
    								monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true);
    		
    							}});
    	    				mOnResumeUpdateCamInfoRunnable = null;
    	    			}
    	    			
    	    		}else{
    	    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, strCamUID);
    	    		}
    	    	}
    			return true;
    		}
		}
    	return false;
    }
    
    protected boolean onCameraOffline(JSONObject msgObj){
    	if(BeseyeConfig.DEBUG)
    		Log.i(TAG, getClass().getSimpleName()+"::onCameraOffline(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(!mActivityDestroy){
    	    		if(!mActivityResume){
    	    			if(null == mOnResumeUpdateCamInfoRunnable || mOnResumeUpdateCamInfoRunnable.isSameVCamId(strCamUID)){
    	    				setOnResumeUpdateCamInfoRunnable(new OnResumeUpdateCamInfoRunnable(strCamUID));
    	    			}else{
    	    				setOnResumeUpdateCamListRunnable(new Runnable(){
    							@Override
    							public void run() {
    								monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true);
    		
    							}});
    	    				mOnResumeUpdateCamInfoRunnable = null;
    	    			}
    	    			
    	    		}else{
    	    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, strCamUID);
    	    		}
    	    	}
    			return true;
    		}
		}
    	return false;
    }
    
    private boolean onCameraEventTrigger(JSONObject msgObj){
    	if(BeseyeConfig.DEBUG)
    		Log.i(TAG, getClass().getSimpleName()+"::onCameraEventTrigger(msgObj);(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(!mActivityDestroy){
    	    		if(mActivityResume){
    	    			monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, strCamUID);
    	    		}
    	    	}
    			return true;
    		}
		}
    	return false;
    }
    
	protected boolean onCameraMotionEvent(JSONObject msgObj){
		return onCameraEventTrigger(msgObj);
	}
	
    protected boolean onCameraPeopleEvent(JSONObject msgObj){
    	return onCameraEventTrigger(msgObj);
    }
	
	private Runnable mOnResumeUpdateCamListRunnable = null;
	private void setOnResumeUpdateCamListRunnable(Runnable run){
		if(BeseyeConfig.DEBUG)
			Log.i(TAG, "setOnResumeUpdateCamListRunnable()");
    	mOnResumeUpdateCamListRunnable = run;
    }
	
	@Override
    protected boolean onCameraActivated(JSONObject msgObj){
		if(BeseyeConfig.DEBUG)
			Log.i(TAG, getClass().getSimpleName()+"::onCameraActivated(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			if(false == mActivityResume){
					setOnResumeUpdateCamListRunnable(new Runnable(){
						@Override
						public void run() {
							monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this).setDialogId(-1), true);
						}});
					mOnResumeUpdateCamInfoRunnable = null;
				}else{
					monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this).setDialogId(-1), true);
				}
    			Toast.makeText(this, getString(R.string.toast_new_cam_activated), Toast.LENGTH_SHORT).show();
    			return true;
    		}
		}
    	return false;
    }
	
	@Override
	protected boolean onCameraDeactivated(JSONObject msgObj){
		if(BeseyeConfig.DEBUG)
			Log.i(TAG, getClass().getSimpleName()+"::onCameraDeactivated(),  msgObj = "+msgObj);
		if(null != msgObj){
			JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
			if(null != objCus){
				//String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
				if(false == mActivityResume){
					setOnResumeUpdateCamListRunnable(new Runnable(){
						@Override
						public void run() {
							monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this).setDialogId(-1), true);
						}});
					mOnResumeUpdateCamInfoRunnable = null;
				}else{
					monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this).setDialogId(-1), true);
				}
				Toast.makeText(this, String.format(getString(R.string.toast_cam_deactivated), BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_NAME)), Toast.LENGTH_SHORT).show();
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(View.VISIBLE == mCameraListMenuAnimator.getVisibility()){
				hideMenu();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
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
	
	private void onNoNetworkError(){
		LayoutInflater inflater = getLayoutInflater();
		if(null != inflater){
			mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_fail, null);
			if(null != mVgEmptyView){
				mMainListView.setEmptyView(mVgEmptyView);
			}
		}
	}
	
	private void addCamAddingView(){
		LayoutInflater inflater = getLayoutInflater();
		if(null != inflater){
			mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_add, null);
			if(null != mVgEmptyView){
				mMainListView.setEmptyView(mVgEmptyView);
			}
		}
	}
	
	private void addCamListBgView(){
		LayoutInflater inflater = getLayoutInflater();
		if(null != inflater){
			mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_bg, null);
			if(null != mVgEmptyView){
				mMainListView.setEmptyView(mVgEmptyView);
			}
		}
	}
}
