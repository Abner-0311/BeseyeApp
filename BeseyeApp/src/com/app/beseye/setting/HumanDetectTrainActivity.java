package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.TAG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.adapter.HumanDetectTrainPicAdapter;
import com.app.beseye.adapter.HumanDetectTrainPicAdapter.HumanDetectTrainItmHolder;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshListView;

public class HumanDetectTrainActivity extends BeseyeBaseActivity {
	private PullToRefreshListView mlvHumanDetectTrainPicList;
	private HumanDetectTrainPicAdapter mHumanDetectTrainPicAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private JSONArray mArrTrainPic;
	private ViewPager mVpIntro;
	private IntroPageAdapter mIntroPageAdapter;
	private Button mbtnDone;
	
	private ViewGroup mVgResultPage = null;
	private TextView  mTxtRetDesc;
	private Button mbtnFinish, mbtnContinue, mbtnConfirm;
	
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
		for(int idx= 0 ;idx<20;idx++){
			addTempObj();
		}

		mlvHumanDetectTrainPicList = (PullToRefreshListView) findViewById(R.id.lv_train_pic_lst);
		if(null != mlvHumanDetectTrainPicList){
			mlvHumanDetectTrainPicList.setMode(LvExtendedMode.NONE);
			
			mHumanDetectTrainPicAdapter = new HumanDetectTrainPicAdapter(this, mArrTrainPic, R.layout.layout_human_detect_training_list_itm, this);
			if(null != mHumanDetectTrainPicAdapter){
				mlvHumanDetectTrainPicList.setAdapter(mHumanDetectTrainPicAdapter);
			}
		}
		
		mVpIntro = (ViewPager)findViewById(R.id.intro_gallery);
		if(null != mVpIntro){
			mVpIntro.setAdapter(new IntroPageAdapter(this));
		}
		
		mbtnContinue = (Button)findViewById(R.id.button_confirm);
		if(null != mbtnContinue){
			mbtnContinue.setOnClickListener(HumanDetectTrainActivity.this);
		}
		
		mVgResultPage = (ViewGroup)findViewById(R.id.vg_human_detect_train_ret);
		if(null != mVgResultPage){
			//mTxtTrainPercent = (TextView)mVgResultPage.findViewById(R.id.tv_training_percent);
			mTxtRetDesc = (TextView)mVgResultPage.findViewById(R.id.tv_training_desc);
			if(null != mTxtRetDesc){
				mTxtRetDesc.setText(String.format(getString(R.string.recognition_percentage), "23%")+getString(R.string.enhance_human_detect_reward_desc));
			}
			mbtnFinish = (Button)findViewById(R.id.btn_finish);
			if(null != mbtnFinish){
				mbtnFinish.setOnClickListener(HumanDetectTrainActivity.this);
			}
			mbtnContinue = (Button)findViewById(R.id.btn_continue);
			if(null != mbtnContinue){
				mbtnContinue.setOnClickListener(HumanDetectTrainActivity.this);
			}
			BeseyeUtils.setVisibility(mVgResultPage, View.GONE);
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
		}else if(R.id.button_confirm == view.getId()){
			BeseyeUtils.setVisibility(mVgResultPage, View.VISIBLE);
		}else if(R.id.button_done == view.getId()){
			BeseyeUtils.setVisibility(mVpIntro, View.GONE);
		}else if(R.id.btn_continue == view.getId()){
			BeseyeUtils.setVisibility(mVgResultPage, View.GONE);
		}else if(R.id.btn_finish == view.getId()){
			finish();
		}else {
			super.onClick(view);
		}
	}
	
	public class IntroPageAdapter extends PagerAdapter {
		private static final int NUM_OF_INTRO_PAGE = 3;
		private Context mContext;
		private LayoutInflater mInflater;
		
		public IntroPageAdapter(Context c) {
	        mContext = c;
	        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    }
		
		@Override
		public void destroyItem(View view, int position, Object object) {
			((ViewPager) view).removeView((View)object);
		}

		@Override
		public void finishUpdate(View arg0) {}

		@Override
		public int getCount() {
			return NUM_OF_INTRO_PAGE;
		}

		@Override
		public Object instantiateItem(View view, int position) {
			int iLayoutId = R.layout.layout_human_detect_intro_page_1;
			if(1 == position){
				iLayoutId = R.layout.layout_human_detect_intro_page_2;
			}else if(2 == position){
				iLayoutId = R.layout.layout_human_detect_intro_page_3;
			}
			
			ViewGroup vGroup = (ViewGroup) mInflater.inflate(iLayoutId, null);
			if(null != vGroup){
				if(2 == position){
					mbtnDone = (Button)vGroup.findViewById(R.id.button_done);
					if(null != mbtnDone){
						mbtnDone.setOnClickListener(HumanDetectTrainActivity.this);
					}
					
					TextView tvDesc = (TextView)vGroup.findViewById(R.id.tv_enhance_human_detect_intro_p3_desc1);
					if(null != tvDesc){
						String strNone = getString(R.string.enhance_human_detect_intro_p3_desc_highlight);
						String strDesc = getString(R.string.enhance_human_detect_intro_p3_desc);
						Spannable wordtoSpan = new SpannableString(strDesc);          

						//Spannable str = (Spannable) tvDesc.getEditableText();
					    int i = strDesc.indexOf(strNone);
					    if(i >=0){
						    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strNone.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					    }
					    tvDesc.setText(wordtoSpan);
					}
				}
				((ViewPager) view).addView(/*img*/vGroup);
	            return vGroup;
			}
			return null;
		}
		
		@Override
		public void notifyDataSetChanged() {
		    super.notifyDataSetChanged();
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public void restoreState(Parcelable arg0, ClassLoader arg1) {}

		@Override
		public Parcelable saveState() {
			return null;
		}

		@Override
		public void startUpdate(View view) {}
	}
}
