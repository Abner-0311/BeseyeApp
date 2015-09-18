package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.adapter.HumanDetectTrainPicAdapter;
import com.app.beseye.adapter.HumanDetectTrainPicAdapter.HumanDetectTrainItmHolder;
import com.app.beseye.adapter.TimezoneInfoAdapter.TimezoneInfoHolder;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshListView;

public class HumanDetectTrainActivity extends BeseyeBaseActivity {
	private PullToRefreshListView mlvHumanDetectTrainPicList;
	private HumanDetectTrainPicAdapter mHumanDetectTrainPicAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private JSONArray mArrTrainPic;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_signup_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
						
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.title_human_detect_train);
			}
//			
//			ImageView mIvRight = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_right_btn);
//			if(null != mIvRight){
//				mIvRight.setVisibility(View.GONE);
//			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			//mNavBarLayoutParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "CameraViewActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
		mArrTrainPic = new JSONArray();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();
		addTempObj();

		mlvHumanDetectTrainPicList = (PullToRefreshListView) findViewById(R.id.lv_train_pic_lst);
		if(null != mlvHumanDetectTrainPicList){
			mlvHumanDetectTrainPicList.setMode(LvExtendedMode.NONE);
			
			mHumanDetectTrainPicAdapter = new HumanDetectTrainPicAdapter(this, mArrTrainPic, R.layout.layout_human_detect_training_list_itm, this);
			if(null != mHumanDetectTrainPicAdapter){
				mlvHumanDetectTrainPicList.setAdapter(mHumanDetectTrainPicAdapter);
			}
		}
	}

	private void addTempObj(){
		JSONObject objImg = new JSONObject();
		BeseyeJSONUtil.setJSONString(objImg, BeseyeJSONUtil.ACC_ID, mStrVCamID);
		BeseyeJSONUtil.setJSONString(objImg, BeseyeJSONUtil.MM_HD_IMG_PATH, BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_VCAM_THUMB));
		mArrTrainPic.put(objImg);
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_human_detect_training;
	}
	
	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof HumanDetectTrainItmHolder){
			HumanDetectTrainItmHolder info = (HumanDetectTrainItmHolder)view.getTag();
			if(null != info){
				BeseyeJSONUtil.setJSONBoolean(info.mObjCam, BeseyeJSONUtil.MM_HD_IMG_DELETE, !BeseyeJSONUtil.getJSONBoolean(info.mObjCam, BeseyeJSONUtil.MM_HD_IMG_DELETE, false));
				if(null != mHumanDetectTrainPicAdapter){
					mHumanDetectTrainPicAdapter.notifyDataSetChanged();
				}
			}
		}else {
			super.onClick(view);
		}
	}
}
