package com.app.beseye;


import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.app.beseye.adapter.TrustDevListAdapter;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseTwoBtnDialog;
import com.app.beseye.widget.PullToRefreshListView;
import com.app.beseye.widget.BaseTwoBtnDialog.OnTwoBtnClickListener;

import android.app.Dialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class BeseyeTrustDevMgtActivity extends BeseyeBaseActivity {
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private PullToRefreshListView mMainListView;
	private TrustDevListAdapter mTrustDevListAdapter;
	private TextView mTxtNavTitle;
	private ViewGroup mVgBtnBundle;
	private ImageView mIvBack;
	private ImageView mIvDelete;
	private Button mBtnCancel;
	private Button mBtnDelete;
	
	private JSONArray mArrTrustDev = null;
	private JSONArray mArrTrustDevForDelete = null;
	private JSONArray mArrTrustDevIdsForDelete = null;
	private JSONArray mArrTrustDevNamesForDelete = null;
	
	private boolean mbIsDeleteMode = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		//Log.d(TAG, "PairingRemindActivity::onCreate()");
		super.onCreate(savedInstanceState);

		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
			
			mTxtNavTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != mTxtNavTitle){
				mTxtNavTitle.setText(R.string.cam_menu_trust_dev_mgt);
				//mTxtNavTitle.setOnClickListener(this);
			}
			
			mIvDelete = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_right_btn);
			if(null != mIvDelete){
				mIvDelete.setOnClickListener(this);
				mIvDelete.setVisibility(View.VISIBLE);
				mIvDelete.setImageResource(R.drawable.sl_cam_list_add);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT);
			mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		mArrTrustDev = new JSONArray();
		JSONObject obj1 = new JSONObject();
		BeseyeJSONUtil.setJSONString(obj1, BeseyeJSONUtil.ACC_TRUST_DEV_NAME, "HTC Butterfly");
		BeseyeJSONUtil.setJSONInt(obj1, BeseyeJSONUtil.ACC_TRUST_DEV_ID, 1);
		BeseyeJSONUtil.setJSONBoolean(obj1, BeseyeJSONUtil.ACC_TRUST_DEV_HOST, false);
		
		JSONObject obj2 = new JSONObject();
		BeseyeJSONUtil.setJSONString(obj2, BeseyeJSONUtil.ACC_TRUST_DEV_NAME, "HTC Desire Eye");
		BeseyeJSONUtil.setJSONInt(obj2, BeseyeJSONUtil.ACC_TRUST_DEV_ID, 2);
		BeseyeJSONUtil.setJSONBoolean(obj2, BeseyeJSONUtil.ACC_TRUST_DEV_HOST, true);
		
		JSONObject obj3 = new JSONObject();
		BeseyeJSONUtil.setJSONString(obj3, BeseyeJSONUtil.ACC_TRUST_DEV_NAME, "Abner's iPhone6 HaHaHaHaHaHaHaHaHa");
		BeseyeJSONUtil.setJSONInt(obj3, BeseyeJSONUtil.ACC_TRUST_DEV_ID, 3);
		BeseyeJSONUtil.setJSONBoolean(obj3, BeseyeJSONUtil.ACC_TRUST_DEV_HOST, false);
		
		mArrTrustDev.put(obj1);
		mArrTrustDev.put(obj2);
		mArrTrustDev.put(obj3);
		
		mArrTrustDev = reorderTrustDev(mArrTrustDev);
		
//		mArrTrustDev.put(obj1);
//		mArrTrustDev.put(obj2);
//		mArrTrustDev.put(obj3);
//		mArrTrustDev.put(obj1);
//		mArrTrustDev.put(obj2);
//		mArrTrustDev.put(obj3);
		
		mMainListView = (PullToRefreshListView)findViewById(R.id.lv_trust_dev_lst);
		if(null != mMainListView){
			mTrustDevListAdapter = new TrustDevListAdapter(this, mArrTrustDev, R.layout.layout_trust_dev_itm, this);
			mMainListView.setAdapter(mTrustDevListAdapter);
		}
		
		mVgBtnBundle = (ViewGroup)findViewById(R.id.vg_delete_bundle);
		
		mBtnCancel = (Button)findViewById(R.id.btn_cancel);
		if(null != mBtnCancel){
			mBtnCancel.setOnClickListener(this);
		}
		
		mBtnDelete = (Button)findViewById(R.id.btn_delete);
		if(null != mBtnDelete){
			mBtnDelete.setOnClickListener(this);
		}
		
		enterReadMode();
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_trust_dev_mgt;
	}
	
	
	
	@Override
	protected void onSessionComplete() {
		super.onSessionComplete();
	}

	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle, String strMsg) {
		super.onErrorReport(task, iErrType, strTitle, strMsg);
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		super.onPostExecute(task, result, iRetCode);
	}

	static private JSONArray reorderTrustDev(JSONArray arrSrc){
		JSONArray arrRet = null;
		if(null != arrSrc && 0 < arrSrc.length()){
			arrRet = new JSONArray();
			int iCount = (null != arrSrc)?arrSrc.length():0;
			for(int idx = 0; idx < iCount; idx++){
				JSONObject objChk = null;
				try {
					objChk = arrSrc.getJSONObject(idx);
					if(BeseyeJSONUtil.getJSONBoolean(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_HOST)){
						arrRet.put(objChk);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			for(int idx = 0; idx < iCount; idx++){
				JSONObject objChk = null;
				try {
					objChk = arrSrc.getJSONObject(idx);
					if(false == BeseyeJSONUtil.getJSONBoolean(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_HOST)){
						arrRet.put(objChk);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return arrRet;
	}
	
	static private String getTrustDevDeleteList(JSONArray arrSrc){
		String strRet = "";
		int iCount = (null != arrSrc)?arrSrc.length():0;
		for(int idx = 0; idx < iCount; idx++){
			if(0 != idx){
				strRet += ", ";
			}
			try {
				strRet+=arrSrc.getString(idx);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return strRet;
	}
	
	private boolean checkDeleteMode(){
		if(mbIsDeleteMode){
			if(null != mArrTrustDevIdsForDelete && 0 < mArrTrustDevIdsForDelete.length()){
				showMyDialog(DIALOG_ID_DELETE_TRUST_DEV);
			}else{
				enterReadMode();
			}
		}else{
			finish();
		}
		return true;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK){
			if(mbIsDeleteMode){
				enterReadMode();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
	}
	
	private void enterDeleteMode(){
		mbIsDeleteMode = true;
		
		try {
			mArrTrustDevForDelete = new JSONArray(mArrTrustDev.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(null != mTrustDevListAdapter){
			mTrustDevListAdapter.updateResultList(mArrTrustDevForDelete);
			mTrustDevListAdapter.setDeleteMode(true);
			refreshList();
		}
		
		
		BeseyeUtils.setVisibility(mIvDelete, View.INVISIBLE);
		BeseyeUtils.setVisibility(mVgBtnBundle, View.VISIBLE);
		
		mArrTrustDevIdsForDelete = null;
		mArrTrustDevNamesForDelete = null;
		
		if(null != mBtnDelete){
			String strDelete = String.format(getString(R.string.btn_delete_trust_dev_mgt), 0);
			mBtnDelete.setText(strDelete);
			mBtnDelete.setEnabled(false);
		}
	}
	
	private void enterReadMode(){
		mbIsDeleteMode = false;
		if(null != mTrustDevListAdapter){
			mTrustDevListAdapter.updateResultList(mArrTrustDev);
			mTrustDevListAdapter.setDeleteMode(false);
			refreshList();
		}
		
		BeseyeUtils.setVisibility(mIvDelete, View.VISIBLE);		
		BeseyeUtils.setVisibility(mVgBtnBundle, View.GONE);
	}
	
	private void refreshList(){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(null != mTrustDevListAdapter){
					mTrustDevListAdapter.notifyDataSetChanged();
					mMainListView.setVisibility(View.INVISIBLE);
					mMainListView.setVisibility(View.VISIBLE);
				}
			}}, 0);
	}
	
	private void toggleTrustDevDeleteState(JSONObject devObj){
		if(null != devObj){
			int iDeleteCnt = 0;
			JSONArray arrIds = new JSONArray();
			JSONArray arrNames = new JSONArray();
			
			int iId = BeseyeJSONUtil.getJSONInt(devObj, BeseyeJSONUtil.ACC_TRUST_DEV_ID, -1);
			int iCount = (null != mArrTrustDevForDelete)?mArrTrustDevForDelete.length():0;
			for(int idx = 0; idx < iCount; idx++){
				try {
					JSONObject objChk=mArrTrustDevForDelete.getJSONObject(idx);
					int iIdChk = BeseyeJSONUtil.getJSONInt(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_ID, -1);
					if(iIdChk == iId){
						BeseyeJSONUtil.setJSONBoolean(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_CHECK, !BeseyeJSONUtil.getJSONBoolean(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_CHECK, false));
						mArrTrustDevForDelete.put(idx, objChk);
					}
					
					if(BeseyeJSONUtil.getJSONBoolean(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_CHECK, false)){
						iDeleteCnt++;
						arrIds.put(BeseyeJSONUtil.getJSONInt(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_ID));
						arrNames.put(BeseyeJSONUtil.getJSONString(objChk, BeseyeJSONUtil.ACC_TRUST_DEV_NAME));
					}
					
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if(null != mTrustDevListAdapter){
				mTrustDevListAdapter.updateResultList(mArrTrustDevForDelete);
				refreshList();
			}
			
			if(null != mBtnDelete){
				String strDelete = String.format(getString(R.string.btn_delete_trust_dev_mgt), iDeleteCnt);
				mBtnDelete.setText(strDelete);
				mBtnDelete.setEnabled(0 < iDeleteCnt);
			}
			
			mArrTrustDevIdsForDelete = arrIds;
			mArrTrustDevNamesForDelete = arrNames;
		}
	}
	
	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof TrustDevListAdapter.TrustDevItmHolder){
			TrustDevListAdapter.TrustDevItmHolder holder  = (TrustDevListAdapter.TrustDevItmHolder)view.getTag();
			JSONObject devObj = holder.mObjTrustDev;
			toggleTrustDevDeleteState(devObj);
		}else{
			switch(view.getId()){
				case R.id.iv_nav_right_btn:{
					enterDeleteMode();
					break;
				}
				case R.id.iv_nav_left_btn:{
					if(mbIsDeleteMode){
						enterReadMode();
					}else{
						finish();
					}
					break;
				}
				case R.id.btn_cancel:{
					enterReadMode();
					break;
				}
				case R.id.btn_delete:{
					checkDeleteMode();
					break;
				}
				default:
					super.onClick(view);
			}
		}		
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_DELETE_TRUST_DEV:{		
				BaseTwoBtnDialog d = new BaseTwoBtnDialog(this);
				d.setBodyText(String.format(getString(R.string.dialog_delete_trust_dev), getTrustDevDeleteList(mArrTrustDevNamesForDelete)));
				d.setTitleText(getString(R.string.dialog_title_warning));
				d.setOnTwoBtnClickListener(new OnTwoBtnClickListener(){
					@Override
					public void onBtnYesClick() {
//						try {
//							mSched_obj_edit.put(SCHED_INDEX, mStrSchedIdx);
//							mSched_obj_edit.put(SCHED_ACTION, BeseyeJSONUtil.SCHED_ACTION_DELETE);
//						} catch (JSONException e1) {
//							Log.e(TAG, "PowerScheduleEditActivity DIALOG_ID_CAM_SCHED_DELETE, e1:"+e1.toString());
//						}
//						mDeleteTask = new BeseyeCamBEHttpTask.ModifyScheduleTask(PowerScheduleEditActivity.this);
//						monitorAsyncTask(mDeleteTask, true, mStrVCamID, mSched_obj_edit.toString());
					}
					@Override
					public void onBtnNoClick() {
						
					}} );
				dialog = d;
				break;
			}
			default:
				dialog = super.onCreateDialog(id);
		}
		return dialog;
	}
}
