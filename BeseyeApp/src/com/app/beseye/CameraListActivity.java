package com.app.beseye;

import static com.app.beseye.util.BeseyeConfig.*;

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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.adapter.CameraListAdapter;
import com.app.beseye.adapter.CameraListAdapter.CameraListItmHolder;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.pairing.SoundPairingActivity;
import com.app.beseye.setting.CamSettingMgr.CAM_CONN_STATUS;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshBase.OnRefreshListener;
import com.app.beseye.widget.PullToRefreshListView;

public class CameraListActivity extends BeseyeBaseActivity implements OnSwitchBtnStateChangedListener{
	static public final String KEY_VCAM_OBJ 	= "KEY_VCAM_OBJ";
	static public final String KEY_VCAM_ID 		= "KEY_VCAM_ID";
	static public final String KEY_VCAM_NAME 	= "KEY_VCAM_NAME";
	static public final String KEY_VCAM_ADMIN 	= "KEY_VCAM_ADMIN";
	static public final String KEY_VCAM_UPSIDEDOWN 	= "KEY_VCAM_UPSIDEDOWN";
	
	private PullToRefreshListView mMainListView;
	private CameraListAdapter mCameraListAdapter;
	private ViewGroup mVgEmptyView;
	private View mVwNavBar;
	private ImageView mIvMenu, mIvAddCam;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_cam_list_nav, null);
		if(null != mVwNavBar){
			mIvMenu = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_menu_btn);
			if(null != mIvMenu){
				mIvMenu.setOnClickListener(this);
				//mIvMenu.setVisibility(COMPUTEX_DEMO?View.INVISIBLE:View.VISIBLE);
			}
			
			mIvAddCam = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_add_cam_btn);
			if(null != mIvAddCam){
				mIvAddCam.setOnClickListener(this);
				mIvAddCam.setVisibility(COMPUTEX_DEMO?View.INVISIBLE:View.VISIBLE);
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
        	
        	mCameraListAdapter = new CameraListAdapter(this, null, R.layout.layout_camera_list_itm, this, this);
        	if(null != mCameraListAdapter){
        		mMainListView.getRefreshableView().setAdapter(mCameraListAdapter);
        	}
		}
		
		if(getIntent().getBooleanExtra(CameraViewActivity.KEY_PAIRING_DONE, false)){
			Log.i(TAG, "handle pairing done case");	
			Bundle b = new Bundle(getIntent().getExtras());
			launchActivityByClassName(CameraViewActivity.class.getName(), b);
			getIntent().putExtra(CameraViewActivity.KEY_PAIRING_DONE, false);
		}
	}
	
	private void refreshList(){
		if(null != mCameraListAdapter){
			mCameraListAdapter.notifyDataSetChanged();
		}
	}
	
	protected void onSessionComplete(){
		Log.i(TAG, "onSessionComplete()");	
		monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(this), true);
	}
	
	protected int miOriginalVcamCnt = -1;
	
	@Override
	public void onPostExecute(AsyncTask task, List<JSONObject> result, int iRetCode) {
		Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeAccountTask.GetVCamListTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					JSONArray arrCamList = new JSONArray();
					int iVcamCnt = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_VCAM_CNT);
					miOriginalVcamCnt = iVcamCnt;
					if(0 < iVcamCnt){
						JSONArray VcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_VCAM_LST);
						for(int i = 0;i< iVcamCnt;i++){
							try {
								JSONObject camObj = VcamList.getJSONObject(i);
								if(BeseyeJSONUtil.getJSONBoolean(camObj, BeseyeJSONUtil.ACC_VCAM_ATTACHED)){
									arrCamList.put(camObj);
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
						int iDemoVcamCnt = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.ACC_DEMO_VCAM_CNT);
						if(0 < iDemoVcamCnt){
							JSONArray DemoVcamList = BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.ACC_DEMO_VCAM_LST);
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
						
						if(null != mCameraListAdapter){
							mCameraListAdapter.updateResultList(arrCamList);
							refreshList();
						}
						
						postToLvRreshComplete();
						getCamsInfo(arrCamList);
					}/*else{
						onToastShow(task, "no Vcam attached.");
						Bundle b = new Bundle();
						b.putBoolean(OpeningPage.KEY_IGNORE_ACTIVATED_FLAG, true);
						launchDelegateActivity(WifiSetupGuideActivity.class.getName(), b);
					}*/
				}
			}else if(task instanceof BeseyeMMBEHttpTask.GetLiveStreamTask){
				if(0 == iRetCode){
					String strVcamId = ((BeseyeMMBEHttpTask.GetLiveStreamTask)task).getVcamId();
					//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> VCAMID = "+strVcamId+", result.get(0)="+result.get(0).toString());
					JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
					int iCount = (null != arrCamList)?arrCamList.length():0;
					for(int i = 0;i < iCount;i++){
						try {
							JSONObject camObj = arrCamList.getJSONObject(i);
							//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> camObj = "+camObj.toString());
							if(strVcamId.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
								camObj.put(BeseyeJSONUtil.ACC_VCAM_CONN_STATE, CAM_CONN_STATUS.CAM_ON.getValue());
								refreshList();
								monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(-1), true, strVcamId);
								break;
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}else if(task instanceof BeseyeMMBEHttpTask.GetLatestThumbnailTask){
				if(0 == iRetCode){
					Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());
					String strVcamId = ((BeseyeMMBEHttpTask.GetLatestThumbnailTask)task).getVcamId();
					//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> VCAMID = "+strVcamId+", result.get(0)="+result.get(0).toString());
					JSONArray arrCamList = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
					int iCount = (null != arrCamList)?arrCamList.length():0;
					for(int i = 0;i < iCount;i++){
						try {
							JSONObject camObj = arrCamList.getJSONObject(i);
							//Log.e(TAG, "onPostExecute(), GetLiveStreamTask=> camObj = "+camObj.toString());
							if(strVcamId.equals(BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID))){
								camObj.put(BeseyeJSONUtil.ACC_VCAM_THUMB, BeseyeJSONUtil.getJSONString(BeseyeJSONUtil.getJSONObject(result.get(0), BeseyeJSONUtil.MM_THUMBNAIL), BeseyeJSONUtil.MM_THUMBNAIL_PATH));
								refreshList();
								break;
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}else{
				//Log.e(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", result.get(0)="+result.get(0).toString());	
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}

	@Override
	public void onErrorReport(AsyncTask task, int iErrType, String strTitle,
			String strMsg) {
		if(task instanceof BeseyeAccountTask.GetVCamListTask){
			postToLvRreshComplete();
		}else if(task instanceof BeseyeMMBEHttpTask.GetLiveStreamTask){
			Log.e(TAG, "onPostExecute(), GetLiveStreamTask failed");
		}else
			super.onErrorReport(task, iErrType, strTitle, strMsg);
	}
	
	private void getCamsInfo(JSONArray arrCamList){
		int iCount = (null != arrCamList)?arrCamList.length():0;
		for(int i = 0;i < iCount;i++){
			try {
				JSONObject camObj = arrCamList.getJSONObject(i);
				monitorAsyncTask(new BeseyeMMBEHttpTask.GetLiveStreamTask(this).setDialogId(-1), true, BeseyeJSONUtil.getJSONString(camObj, BeseyeJSONUtil.ACC_ID), "false");
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

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof CameraListItmHolder){
			JSONObject cam_obj = ((CameraListItmHolder)view.getTag()).mObjCam;
			if(null != cam_obj){
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, cam_obj.toString());
				
//				b.putString(CameraListActivity.KEY_VCAM_ID, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_ID));
//				b.putString(CameraListActivity.KEY_VCAM_NAME, BeseyeJSONUtil.getJSONString(cam_obj, BeseyeJSONUtil.ACC_NAME));
//				b.putBoolean(CameraListActivity.KEY_VCAM_ADMIN, BeseyeJSONUtil.getJSONBoolean(cam_obj, BeseyeJSONUtil.ACC_SUBSC_ADMIN, true));
				launchActivityForResultByClassName(CameraViewActivity.class.getName(), b, REQUEST_CAM_VIEW_CHANGE);
				//Log.e(TAG, "onClick(), "+cam_obj.toString());
				return;
			}
		}else if(R.id.iv_nav_menu_btn == view.getId()){
			Toast.makeText(this, "logout", Toast.LENGTH_SHORT).show();
			invokeLogout();
			//monitorAsyncTask(new BeseyeAccountTask.CamDettachTask(this), true, "5dc166880720448cafa563be507b9730");
		}else if(R.id.iv_nav_add_cam_btn == view.getId()){
			Intent intent = new Intent();
			intent.putExtra(SoundPairingActivity.KEY_ORIGINAL_VCAM_CNT, miOriginalVcamCnt);
			launchActivityByClassName(WifiSetupGuideActivity.class.getName());
		}else
			super.onClick(view);
	}
	
	static public final int REQUEST_CAM_VIEW_CHANGE = 1;

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if(REQUEST_CAM_VIEW_CHANGE == requestCode){
			if(resultCode == RESULT_OK){
				//monitorAsyncTask(new BeseyeAccountTask.GetVCamListTask(CameraListActivity.this), true);
				try {
					JSONObject Cam_obj = new JSONObject(intent.getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
					if(null != Cam_obj){
						JSONArray camArr = (null != mCameraListAdapter)?mCameraListAdapter.getJSONList():null;
						int iCount = (null != camArr)?camArr.length():0;
						String strVcamId = BeseyeJSONUtil.getJSONString(Cam_obj, BeseyeJSONUtil.ACC_VCAM_ID);
						for(int i = 0;i<iCount;i++){
							JSONObject obj = camArr.getJSONObject(i);
							if(null != obj && BeseyeJSONUtil.getJSONString(obj, BeseyeJSONUtil.ACC_VCAM_ID).equals(strVcamId)){
								BeseyeJSONUtil.setJSONString(obj, BeseyeJSONUtil.ACC_NAME, BeseyeJSONUtil.getJSONString(Cam_obj, BeseyeJSONUtil.ACC_NAME));
								BeseyeJSONUtil.setJSONInt(obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE, BeseyeJSONUtil.getJSONInt(Cam_obj, BeseyeJSONUtil.ACC_VCAM_CONN_STATE));
								refreshList();
								break;
							}
						}
					}
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
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
			if(null != cam_obj){
				try {
					cam_obj.put(BeseyeJSONUtil.ACC_VCAM_CONN_STATE, state.equals(SwitchState.SWITCH_ON)?CAM_CONN_STATUS.CAM_ON.getValue():CAM_CONN_STATUS.CAM_OFF.getValue());
					refreshList();
				} catch (JSONException e) {
					e.printStackTrace();
				}
				return;
			}
		}
	}
}
