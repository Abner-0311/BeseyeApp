package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.websockets.BeseyeWebsocketsUtil.WS_ATTR_CAM_UID;

import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.res.Configuration;
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
import com.app.beseye.ota.BeseyeCamSWVersionMgr;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_ERROR;
import com.app.beseye.ota.CamOTAFeedbackActivity;
import com.app.beseye.ota.CamOTAInstructionActivity;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_GROUP_VER_CHK_RET;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_GROUP;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.CAM_UPDATE_VER_CHECK_STATUS;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.OnCamGroupUpdateVersionCheckListener;
import com.app.beseye.ota.BeseyeCamSWVersionMgr.OnCamUpdateStatusChangedListener;
import com.app.beseye.ota.CamOTAFAQActivity;
import com.app.beseye.ota.CamSwUpdateRecord;
import com.app.beseye.pairing.PairingPlugPowerActivity;
import com.app.beseye.pairing.PairingRemindActivity;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseOneBtnDialog.OnOneBtnClickListener;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.BaseOneBtnDialog;
import com.app.beseye.widget.CameraListMenuAnimator;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

public class CameraListActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener,
																	  OnCamUpdateStatusChangedListener,
																	  OnCamGroupUpdateVersionCheckListener{
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
	
	private static Locale sLastLocale = null;
	private CAM_UPDATE_GROUP meUpdateGroup = CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		BeseyeCamSWVersionMgr.getInstance().registerOnCamUpdateStatusChangedListener(this);

		mbIsDemoCamMode = getIntent().getBooleanExtra(KEY_DEMO_CAM_MODE, false);
		if(mbIsDemoCamMode){
			if(null == sLastLocale || sLastLocale.equals(Locale.getDefault())){
				String strVCamListInfo = getIntent().getStringExtra(KEY_DEMO_CAM_INFO);
				if(null != strVCamListInfo && 0 < strVCamListInfo.length()){
					try {
						mVCamListInfoObj = new JSONObject(strVCamListInfo);
					} catch (JSONException e) {
						Log.i(TAG, "onCreate(), e:"+e.toString());	
					}
				}
			}
			sLastLocale = Locale.getDefault();
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
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
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
		
		long lPairingDoneTs = getIntent().getLongExtra(CameraViewActivity.KEY_PAIRING_DONE_TS, -1);
		if((-1 != lPairingDoneTs && (System.currentTimeMillis() - lPairingDoneTs < CameraViewActivity.MAX_PAIRING_DONE_TS))){
			if(!getIntent().getBooleanExtra(CameraViewActivity.KEY_PAIRING_OTA_NEED_UPDATE, false)){
				mPendingRunnableOnCreate = new Runnable(){
					@Override
					public void run() {
						Bundle b = new Bundle(getIntent().getExtras());
						launchActivityByClassName(CameraViewActivity.class.getName(), b);
						getIntent().putExtra(CameraViewActivity.KEY_PAIRING_DONE, false);
					}};
				if(BeseyeConfig.DEBUG)
					Log.i(TAG, "handle pairing done case");	
			}else{
				showMyDialog(DIALOG_ID_OTA_FORCE_UPDATE);
			}
		}else if(getIntent().getBooleanExtra(OpeningPage.KEY_EVENT_FLAG, false)){
			mPendingRunnableOnCreate = new Runnable(){
				@Override
				public void run() {
					Intent intentEvt = getIntent().getParcelableExtra(OpeningPage.KEY_EVENT_INTENT);
					launchActivityByIntent(intentEvt);
					getIntent().putExtra(OpeningPage.KEY_EVENT_FLAG, false);
				}};
		}
		
		if(mbIsDemoCamMode){
			meUpdateGroup = CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_DEMO;
		}else if(mbIsPrivateCamMode){
			meUpdateGroup = CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PRIVATE;
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
			monitorAsyncTask(new BeseyeNewsBEHttpTask.GetLatestNewsTask(this).setDialogId(-1), true, BeseyeUtils.DEF_NEWS_LANG);
		}
		
		checkLatestNew();
		
		if(null != mPendingRunnableOnCreate){
			BeseyeUtils.postRunnable(mPendingRunnableOnCreate, 100);
			mPendingRunnableOnCreate = null;
		}
		
		BeseyeCamSWVersionMgr.getInstance().setNeedPeriodCheckUpdateStatus(meUpdateGroup, true);
	}
	
	@Override
	protected void onPause() {
		BeseyeCamSWVersionMgr.getInstance().resetPoorNetworkError(meUpdateGroup);
		BeseyeCamSWVersionMgr.getInstance().setNeedPeriodCheckUpdateStatus(meUpdateGroup, false);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		BeseyeCamSWVersionMgr.getInstance().unregisterOnCamUpdateStatusChangedListener(this);
		super.onDestroy();
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
			monitorAsyncTask(new BeseyeNewsBEHttpTask.GetLatestNewsTask(this).setDialogId(-1), true, BeseyeUtils.DEF_NEWS_LANG);
			monitorAsyncTask(new BeseyeAccountTask.GetUserInfoTask(this), true);
		}else{
			fillVCamList(mVCamListInfoObj);
		}
	}
	
	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_OTA_FORCE_UPDATE:{
				BaseOneBtnDialog d = new BaseOneBtnDialog(this);
				d.setBodyText(getString(R.string.desc_dialog_cam_force_update));
				d.setTitleText(getString(R.string.dialog_title_attention));
				d.setOnOneBtnClickListener(new OnOneBtnClickListener(){
					@Override
					public void onBtnClick() {
						removeMyDialog(DIALOG_ID_OTA_FORCE_UPDATE);	
					}});
				dialog = d;
				d.setOnDismissListener(new OnDismissListener(){
					@Override
					public void onDismiss(DialogInterface dialog) {
//						try {
//							JSONObject cam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
//							if(null != cam_obj){
//								CamSwUpdateRecord camRec = BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(meUpdateGroup, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
//						    	if(null != camRec){
//									BeseyeCamSWVersionMgr.getInstance().performCamUpdate(camRec, false);
//						    	}
//							}
//						} catch (JSONException e1) {
//							Log.e(TAG, "SoundPairingNamingActivity, failed to parse, e1:"+e1.toString());
//						}
					}});
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		return dialog;
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if(DEBUG)
			Log.i(TAG, "onConfigurationChanged " + newConfig);
		super.onConfigurationChanged(newConfig);
		//if(newConfig.locale != )
		getIntent().getExtras().putString(KEY_DEMO_CAM_INFO, "");
	}
	
	protected int miOriginalVcamCnt = -1;
	
	private void fillVCamList(JSONObject objVCamList){
		//Log.d(TAG, "fillVCamList(), objVCamList="+objVCamList.toString());
		//mObjVCamList = objVCamList;
		
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
				
				int iSubscribeVcamCnt = BeseyeJSONUtil.getJSONInt(objVCamList, BeseyeJSONUtil.ACC_SUBSCRIBE_VCAM_CNT);
				if(0 < iSubscribeVcamCnt){
					JSONArray SubscribeVcamList = BeseyeJSONUtil.getJSONArray(objVCamList, BeseyeJSONUtil.ACC_SUBSCRIBE_VCAM_LST);
					for(int i = 0;i< iSubscribeVcamCnt;i++){
						try {
							JSONObject camObj = SubscribeVcamList.getJSONObject(i);
							if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
								//BeseyeJSONUtil.setJSONBoolean(camObj, BeseyeJSONUtil.ACC_IS_SUBSCRIBED_VCAM, true);
								arrCamList.put(camObj);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
				
				mBundleDemo.putString(KEY_DEMO_CAM_INFO, objVCamList.toString());
				mBundlePrivate.putString(KEY_DEMO_CAM_INFO, objVCamList.toString());
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
			
			BeseyeCamSWVersionMgr.getInstance().updateGroupCamList(meUpdateGroup, arrCamList);

			
			if(meUpdateGroup.equals(CAM_UPDATE_GROUP.CAM_UPDATE_GROUP_PERONSAL)){
				if(!SessionMgr.getInstance().getIsCamSWUpdateSuspended()){
					BeseyeCamSWVersionMgr.getInstance().registerOnCamGroupUpdateVersionCheckListener(this);
					BeseyeCamSWVersionMgr.getInstance().checkGroupOTAVer(meUpdateGroup, true);
				}
			}else{
				BeseyeCamSWVersionMgr.getInstance().checkGroupCamUpdateStatus(meUpdateGroup, true);
			}
			
			if(null != mCameraListAdapter){
				mCameraListAdapter.updateResultList(arrCamList);
				refreshList();
			}
			
			if((!mbIsDemoCamMode && !mbIsPrivateCamMode) && null == arrCamList || 0 == arrCamList.length()){
				addCamAddingView();
			}
			
			postToLvRreshComplete();
			miCurUpdateIdx = 0;
			updateCamItm(++miTaskSeedNum);
		}
		
//		if((isAppVersionChecked() && !isCamUpdatingInCurrentPage()) || checkWithinCamUpdatePeriod()){
//			getCamUpdateCandidateList(mObjVCamList);
//			mObjVCamList = null;
//		}
	}
	
	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
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
							if(BeseyeJSONUtil.getJSONBoolean(objUser, BeseyeJSONUtil.ACC_BESEYE_ACCOUNT) || BeseyeJSONUtil.getJSONBoolean(objUser, BeseyeJSONUtil.ACC_PRIVATECAM_ACCOUNT)){
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
								
								//temp workaround for Beseye Pro S test
								if(BeseyeConfig.DEBUG && "01d0f4f7b68e48a583e28449eacf99b2".equals(strVcamId)){
									JSONObject statusLstObj = new JSONObject();
									statusLstObj.put(BeseyeJSONUtil.CAM_WS_STATUS, 1);
									dataObj.put(BeseyeJSONUtil.CAM_STATUS_LST, statusLstObj);
								}
								
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
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, final int iErrType, String strTitle, String strMsg) {
		if(task instanceof BeseyeAccountTask.GetVCamListTask){
			postToLvRreshComplete();
		}else if(task instanceof BeseyeCamBEHttpTask.SetCamStatusTask){
			BeseyeUtils.postRunnable(new Runnable(){
				@Override
				public void run() {
					Bundle b = new Bundle();
					b.putString(KEY_WARNING_TEXT, BeseyeUtils.appendErrorCode(CameraListActivity.this, R.string.cam_setting_fail_to_update_cam_status, iErrType));
					showMyDialog(DIALOG_ID_WARNING, b);
					
					refreshList();
				}}, 0);
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
		
		if(iErrType == BeseyeHttpTask.ERR_TYPE_NO_CONNECTION){
			showNoNetworkUI();
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
					String strCamUID = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID);
					if(null != strCamUID){
						monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this, iSeed).setDialogId(-1), true, strCamUID);
					}
					//monitorAsyncTask(new BeseyeMMBEHttpTask.GetLiveStreamTask(CameraListActivity.this, iSeed).setDialogId(-1), true, BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID), "false");
				}else if(miCurUpdateIdx == iCount){
					for(int idx = 0; idx  < iCount; idx++){
						final JSONObject camObj = arrCamList.getJSONObject(idx);
						if(null != camObj){
							JSONObject dataObj = BeseyeJSONUtil.getJSONObject(camObj, BeseyeJSONUtil.ACC_DATA);
							if(null == dataObj){
								String strCamUID = BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID);
								Log.e(TAG, "updateCamItm(), find null data for strCamUID="+strCamUID);
								if(null != strCamUID){
									monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this, iSeed).setDialogId(-1), true, strCamUID);
								}
								break;
							}
						}
					}
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
		//launchActivityByClassName(WifiListActivity.class.getName(), b);
		launchActivityByClassName(PairingPlugPowerActivity.class.getName(), b);
	}

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof CameraListItmHolder){
			JSONObject cam_obj = ((CameraListItmHolder)view.getTag()).mObjCam;
			if(null != cam_obj){
				if(R.id.tv_camera_more == view.getId()){
					startSoundPairingProcess(BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID), false);
				}else if(R.id.btn_ota_support == view.getId()){
			    	CamSwUpdateRecord camRec = BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(meUpdateGroup, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
					Bundle b = new Bundle();
					boolean bIsOTATimeout =  null != camRec && camRec.getUpdateStatus().equals(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING) && camRec.isReachOTANoResponseTime();
					b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
					b.putInt(CamOTAFeedbackActivity.OTA_ERROR_CODE_FINAL, (null != camRec)?((bIsOTATimeout)?-1:camRec.getFinalErrCode()):-2);
					b.putInt(CamOTAFeedbackActivity.OTA_ERROR_CODE_DETAIL, (null != camRec)?((bIsOTATimeout)?-1:camRec.getDetailErrCode()):-2);

					launchActivityForResultByClassName(CamOTAFAQActivity.class.getName(), b, REQUEST_OTA_SUPPORT);
				}else if(R.id.btn_ota_update == view.getId()){
					Bundle b = new Bundle();
					b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
					b.putInt(CamOTAInstructionActivity.CAM_OTA_TYPE, CamOTAInstructionActivity.CAM_OTA_INSTR_TYPE.TYPE_UPDATE_ONE.ordinal());
					launchActivityByClassName(CamOTAInstructionActivity.class.getName(), b);			
				}else if(R.id.btn_ota_update_again == view.getId()){
			    	CamSwUpdateRecord camRec = BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(meUpdateGroup, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
			    	if(null != camRec){
						camRec.resetErrorInfo();
						camRec.changeUpdateStatus(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING, false);
						BeseyeCamSWVersionMgr.getInstance().performCamUpdate(camRec, false);//.checkCamOTAVer(camRec, true);
						refreshList();
			    	}else{
						Log.e(TAG, "onClick(), find null camRec for strCamUID="+BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
			    	}
				}else{
					Bundle b = new Bundle();
					b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
					b.putBoolean(KEY_DEMO_CAM_MODE, mbIsDemoCamMode);
					b.putBoolean(KEY_PRIVATE_CAM_MODE, mbIsPrivateCamMode);
					launchActivityByClassName(CameraViewActivity.class.getName(), b);
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
					immediateHideMenu();
					launchActivityByClassName(CameraListActivity.class.getName(), null);
				}else{
					showMenu();
				}
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
			immediateHideMenu();
		}else if(R.id.vg_demo_cam == view.getId()){
			if(!mbIsDemoCamMode){
				immediateHideMenu();
				launchActivityByClassName(CameraListActivity.class.getName(), mBundleDemo);
			}else{
				showMenu();
			}
		}else if(R.id.vg_private_cam == view.getId()){
			if(!mbIsPrivateCamMode){
				immediateHideMenu();
				launchActivityByClassName(CameraListActivity.class.getName(), mBundlePrivate);
			}else{
				showMenu();
			}
		}else if(R.id.vg_about == view.getId()){
			launchActivityByClassName(BeseyeAboutActivity.class.getName(), null);
			immediateHideMenu();
		}else if(R.id.vg_trust_mgt == view.getId()){
			launchActivityByClassName(BeseyeTrustDevMgtActivity.class.getName(), null);
			immediateHideMenu();
		}else if(R.id.vg_support == view.getId()){
			immediateHideMenu();
		}else if(R.id.vg_logout == view.getId()){
			invokeLogout();
			showMenu();
		}else if(R.id.txt_nav_title == view.getId()){
			if(BeseyeConfig.DEBUG){
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
	
	private void immediateHideMenu(){
		if(null != mCameraListMenuAnimator && !mCameraListMenuAnimator.isInAnimation()){
			mCameraListMenuAnimator.hideMenu();
		}
	}
	
	private void showMenu(){
		if(null != mCameraListMenuAnimator && !mCameraListMenuAnimator.isInAnimation()){
			mCameraListMenuAnimator.performMenuAnimation();
		}
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
						
						if(mActivityResume && mbNeedToCheckCamOnStatus){
							if(strVcamId.equals(mStrVCamIdCamOnBeforeRefresh)){
								if(( mbIsCamOnBeforeRefresh && BeseyeJSONUtil.isCamPowerOff(objCamSetup))){
									Toast.makeText(getApplicationContext(), String.format(getString(R.string.notify_cam_off_detect_cam_list), BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_NAME)), Toast.LENGTH_SHORT).show();
								}
								mStrVCamIdCamOnBeforeRefresh = null;
								mbIsCamOnBeforeRefresh = false;
							}
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
	
	private boolean mbNeedToCheckCamOnStatus = false;
	private boolean mbIsCamOnBeforeRefresh = false;
	private String mStrVCamIdCamOnBeforeRefresh = null;
	
	//From Notification
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
	    						mbIsCamOnBeforeRefresh = BeseyeJSONUtil.isCamPowerOn(camObj);
	    						mStrVCamIdCamOnBeforeRefresh = strCamUID;
	    						if(BeseyeConfig.DEBUG)
	    				    		Log.i(TAG, getClass().getSimpleName()+"::onCamSettingChangedCallback(), strCamUID = "+strCamUID+" is on before refresh");
	    					}
	    				}catch (JSONException e) {
	    					e.printStackTrace();
	    				}
	    			}
	    			
	    			mbNeedToCheckCamOnStatus = true;
	    			
	    			if(null != strCamUID)
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
    	    		}else if(null != strCamUID){
    	    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, strCamUID);
    	    			JSONObject camObj = BeseyeCamInfoSyncMgr.getInstance().getCamInfoByVCamId(strCamUID);
    	    			if(null != camObj){
    	    				BeseyeJSONUtil.setVCamWsStatus(camObj, true);
    	    			}
    	    			CamSwUpdateRecord camRec = BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(meUpdateGroup, strCamUID);
    			    	if(null != camRec){
    			    		if(!SessionMgr.getInstance().getIsCamSWUpdateSuspended()){
	    						//camRec.resetErrorInfo();
	    						BeseyeCamSWVersionMgr.getInstance().checkCamOTAVer(camRec, true);
    			    		}
    			    	}
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
    	    			
    	    		}else if(null != strCamUID){
    	    			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(/*DIALOG_ID_SYNCING*/-1), true, strCamUID);
    	    			JSONObject camObj = BeseyeCamInfoSyncMgr.getInstance().getCamInfoByVCamId(strCamUID);
    	    			if(null != camObj){
    	    				BeseyeJSONUtil.setVCamWsStatus(camObj, false);
    	    			}
    	    		}
    	    	}
    			return true;
    		}
		}
    	return false;
    }
    
    protected boolean onCameraOTAStart(JSONObject msgObj){
    	if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
        		final String strVcamId = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
        		JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
        		int iCount = (null != arrCamList)?arrCamList.length():0;
        		for(int i = 0;i < iCount;i++){
        			try {
        				JSONObject camObj = arrCamList.getJSONObject(i);
        				if(strVcamId.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
        					if(mActivityResume){
        						CamSwUpdateRecord camRec = BeseyeCamSWVersionMgr.getInstance().findCamSwUpdateRecord(meUpdateGroup, strVcamId);
            			    	if(null != camRec){
            			    		camRec.changeUpdateStatus(CAM_UPDATE_STATUS.CAM_UPDATE_STATUS_UPDATING, false);
            			    		refreshList();
            			    	}
            	    		}
        					break;
        				}
        			}catch (JSONException e) {
        				e.printStackTrace();
        			}
        		}
    			return true;
    		}
		}
    	return super.onCameraOTAStart(msgObj);
    }
    
    private boolean isInMyCameraList(String strVcamId){
    	boolean bRet = false;
    	if(null != strVcamId){
    		JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
    		if(null !=  arrCamList){
    			for(int idx = 0; idx < arrCamList.length();idx++){
        			JSONObject cmpObj = arrCamList.optJSONObject(idx);
        			if(null != cmpObj){
        				String strNewVcamid = BeseyeJSONUtil.getJSONString(cmpObj, BeseyeJSONUtil.ACC_ID);
        				if(strVcamId.equals(strNewVcamid)){
        					bRet = true;
        					break;
        				}
        			}
        		}
    		}
    	}
    	
    	return bRet;
    }
    
    private boolean onCameraEventTrigger(JSONObject msgObj){
    	if(BeseyeConfig.DEBUG && msgObj != null)
    		Log.i(TAG, getClass().getSimpleName()+"::onCameraEventTrigger(msgObj);(),  msgObj = "+msgObj);
		if(null != msgObj){
    		JSONObject objCus = BeseyeJSONUtil.getJSONObject(msgObj, BeseyeJSONUtil.PS_CUSTOM_DATA);
    		if(null != objCus){
    			String strCamUID = BeseyeJSONUtil.getJSONString(objCus, BeseyeJSONUtil.PS_CAM_UID);
    			if(!mActivityDestroy){
    	    		if(mActivityResume && isInMyCameraList(strCamUID)){
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
	protected void onServerError(int iErrCode){
		super.onServerError(iErrCode);
		LayoutInflater inflater = getLayoutInflater();
		if(null != inflater){
			mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_fail, null);
			if(null != mVgEmptyView){
				mMainListView.setEmptyView(mVgEmptyView);
			}
		}
	}
	
	@Override
	public void onConnectivityChanged(boolean bNetworkConnected){
		super.onConnectivityChanged(bNetworkConnected);
		showNoNetworkUI();
    }
	
	private void showNoNetworkUI(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				mCameraListAdapter.updateResultList(null);
				onNoNetworkError();
			}}, 0);
	}
	
	private void onNoNetworkError(){
		LayoutInflater inflater = getLayoutInflater();
		if(null != inflater){
			mVgEmptyView = (ViewGroup)inflater.inflate(R.layout.layout_camera_list_fail, null);
			if(null != mVgEmptyView){
				mVgEmptyView.post(new Runnable(){
					@Override
					public void run() {
						mMainListView.setEmptyView(mVgEmptyView);						
					}});
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
	
	static public final int REQUEST_OTA_SUPPORT = 1001;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(DEBUG)
			Log.w(TAG, "CameraListActivity::onActivityResult(), requestCode:"+requestCode+", resultCode:"+resultCode);
		
		if(REQUEST_OTA_SUPPORT == requestCode && resultCode == RESULT_OK){
			String strVcamId = intent.getStringExtra(CameraListActivity.KEY_VCAM_ID);
			BeseyeCamSWVersionMgr.getInstance().setOTAFeedbackTsByVcamId(meUpdateGroup, strVcamId);
			//Refresh it
			refreshList();
			if(!SessionMgr.getInstance().getIsCamSWUpdateSuspended()){
				BeseyeCamSWVersionMgr.getInstance().registerOnCamGroupUpdateVersionCheckListener(this);
				BeseyeCamSWVersionMgr.getInstance().checkGroupOTAVer(meUpdateGroup, false);
			}
		}else {
			super.onActivityResult(requestCode, resultCode, intent);
		}
	}
	
	@Override
	public void onCamUpdateStatusChanged(String strVcamId, CAM_UPDATE_STATUS curStatus, CAM_UPDATE_STATUS prevStatus, CamSwUpdateRecord objUpdateRec) {
		if(mActivityResume)
			refreshList();
	}

	@Override
	public void onCamUpdateProgress(String strVcamId, int iPercetage) {
		if(mActivityResume)
			refreshList();
	}
	
	@Override
	public void onCamUpdateVerChkStatusChanged(String strVcamId, CAM_UPDATE_VER_CHECK_STATUS curStatus, CAM_UPDATE_VER_CHECK_STATUS prevStatus, CamSwUpdateRecord objUpdateRec){
		if(mActivityResume){
			refreshList();
		}
		
		if(curStatus.equals(CAM_UPDATE_VER_CHECK_STATUS.CAM_UPDATE_VER_CHECK_UPDATED)){
			BeseyeCamSWVersionMgr.getInstance().checkCamUpdateStatus(objUpdateRec, true);
		}
	}
	
	@Override
	public void onCamUpdateVersionCheckAllCallback(
			CAM_GROUP_VER_CHK_RET chkRet, CAM_UPDATE_GROUP chkGroup,
			CAM_UPDATE_ERROR chkErr, List<String> lstVcamIds) {
//		BeseyeCamSWVersionMgr.getInstance().unregisterOnCamGroupUpdateVersionCheckListener(this);
//		if(chkRet.equals(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_ALL_OUT_OF_UPDATE) || chkRet.equals(CAM_GROUP_VER_CHK_RET.CAM_GROUP_VER_CHK_PARTIAL_UPDATED) ){
//			if(mActivityResume)
//				refreshList();
//		}
//		BeseyeCamSWVersionMgr.getInstance().checkGroupCamUpdateStatus(meUpdateGroup, true);
	}

}
