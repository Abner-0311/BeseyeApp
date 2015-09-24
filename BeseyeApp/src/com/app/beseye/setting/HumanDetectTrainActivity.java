package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;
import static com.app.beseye.util.BeseyeJSONUtil.NOTIFY_OBJ;
import static com.app.beseye.util.BeseyeJSONUtil.OBJ_TIMESTAMP;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONObject;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
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
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeIMPMMBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshListView;
import com.app.beseye.widget.RemoteImageView.RemoteImageCallback;

public class HumanDetectTrainActivity extends BeseyeBaseActivity implements RemoteImageCallback{
	final private static int NUM_OF_REFINE_IMG = 20;
	private PullToRefreshListView mlvHumanDetectTrainPicList;
	private HumanDetectTrainPicAdapter mHumanDetectTrainPicAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private JSONArray mArrTrainPic, mArrTrainPicToSend;
	private boolean mbHaveNextPage = false, mbNeedToShowIntro = true;
	
	private ViewPager mVpIntro;
	private IntroPageAdapter mIntroPageAdapter;
	private Button mbtnDone;
	private ImageView mIvTrainRet;
	
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
		
//		mArrTrainPic = new JSONArray();
//		for(int idx= 0 ;idx<20;idx++){
//			addTempObj();
//		}
		
		mbNeedToShowIntro = !SessionMgr.getInstance().getHumanDetectIntroShowOnce() || !SessionMgr.getInstance().getHumanDetectIntroShown();

		mlvHumanDetectTrainPicList = (PullToRefreshListView) findViewById(R.id.lv_train_pic_lst);
		if(null != mlvHumanDetectTrainPicList){
			mlvHumanDetectTrainPicList.setMode(LvExtendedMode.NONE);
			
			mHumanDetectTrainPicAdapter = new HumanDetectTrainPicAdapter(this, mArrTrainPic, R.layout.layout_human_detect_training_list_itm, this, this);
			if(null != mHumanDetectTrainPicAdapter){
				mHumanDetectTrainPicAdapter.setVCamid(mStrVCamID);
				mlvHumanDetectTrainPicList.setAdapter(mHumanDetectTrainPicAdapter);
			}
		}
		
		mVpIntro = (ViewPager)findViewById(R.id.intro_gallery);
		if(null != mVpIntro){
			mVpIntro.setAdapter(new IntroPageAdapter(this));
			if(mbNeedToShowIntro){
				BeseyeUtils.setVisibility(mVpIntro, View.VISIBLE);
			}
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
		
		mIvTrainRet = (ImageView)findViewById(R.id.iv_train_ret);
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
	protected void onSessionComplete() {
		super.onSessionComplete();
		if(!mbNeedToShowIntro){
			monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectRefineListTask(this), true, mStrVCamID, NUM_OF_REFINE_IMG+"");
		}
	}
	
	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(BeseyeConfig.DEBUG)
			Log.d(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeIMPMMBEHttpTask.GetHumanDetectRefineListTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
				
					mArrTrainPic =  BeseyeJSONUtil.getJSONArray(result.get(0), BeseyeJSONUtil.MM_HD_IMG);
					if(null == mArrTrainPic || 0 == mArrTrainPic.length()){
						onNoTrainPicAvailable(49);
					}else{
						mbHaveNextPage = BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.MM_HD_IMG_PAGING);
						if(null != mHumanDetectTrainPicAdapter){
							mHumanDetectTrainPicAdapter.updateResultList(mArrTrainPic);
							mHumanDetectTrainPicAdapter.notifyDataSetChanged();
						}
						
						try {
							mArrTrainPicToSend = new JSONArray(mArrTrainPic.toString());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}else if(task instanceof BeseyeIMPMMBEHttpTask.SetHumanDetectRefineLabelTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString()+", mbHaveNextPage:"+mbHaveNextPage);
				
					mArrTrainPic = null;
					mArrTrainPicToSend = null;
					if(null != mHumanDetectTrainPicAdapter){
						mHumanDetectTrainPicAdapter.updateResultList(null);
						mHumanDetectTrainPicAdapter.notifyDataSetChanged();
					}
					
					if(mbHaveNextPage){
						onTrainPicAvailable(33);
					}else{
						onNoTrainPicAvailable(49);
					}
				}
			}else{
				super.onPostExecute(task, result, iRetCode);
			}
		}
	}
	
	@Override
	public void onErrorReport(AsyncTask<String, Double, List<JSONObject>> task, int iErrType, String strTitle,
			String strMsg) {
		// GetLatestThumbnailTask don't need to have onErrorReport because it has default image
		if(task instanceof BeseyeIMPMMBEHttpTask.GetHumanDetectRefineListTask){
			showErrorDialog(R.string.enhance_human_detect_train_fail_to_load_pic, true);
		}else if(task instanceof BeseyeIMPMMBEHttpTask.SetHumanDetectRefineLabelTask){
			showErrorDialog(R.string.enhance_human_detect_train_fail_to_label, false);
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
	
//	private Runnable mRefreshListRunnable = new Runnable(){
//		@Override
//		public void run() {
//			if(null != mHumanDetectTrainPicAdapter){
//				mHumanDetectTrainPicAdapter.notifyDataSetChanged();
//			}
//		}};
	
	@Override
	public void imageLoaded(boolean success, String strPath) {
		//Log.i(TAG, "imageLoaded(), strPath:"+strPath+", success:"+success);
		if(success && null != strPath){
			int iLenPic = (null != mArrTrainPic)?mArrTrainPic.length():0;
			if(0 < iLenPic){
				for(int idx = 0 ;idx < iLenPic;idx++){
					JSONObject objCheck = mArrTrainPic.optJSONObject(idx);
					if(strPath.endsWith(BeseyeJSONUtil.getJSONString(objCheck, BeseyeJSONUtil.MM_HD_IMG_PATH))){
						BeseyeJSONUtil.setJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOADED, true);
						try {
							mArrTrainPic.put(idx, objCheck);
							if(null != mHumanDetectTrainPicAdapter){
								mHumanDetectTrainPicAdapter.updateResultList(mArrTrainPic);
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
						break;
					}
				}
			}
		}
	}

	@Override
	public void onClick(View view) {
		if(view.getTag() instanceof HumanDetectTrainItmHolder){
			HumanDetectTrainItmHolder info = (HumanDetectTrainItmHolder)view.getTag();
			if(null != info){
				int iLenPic = (null != mArrTrainPic)?mArrTrainPic.length():0;
				if(0 < iLenPic){
					String strPath = BeseyeJSONUtil.getJSONString(info.mObjCam, BeseyeJSONUtil.MM_HD_IMG_PATH);
					for(int idx = 0 ;idx < iLenPic;idx++){
						JSONObject objCheck = mArrTrainPic.optJSONObject(idx);
						if(strPath.equals(BeseyeJSONUtil.getJSONString(objCheck, BeseyeJSONUtil.MM_HD_IMG_PATH))){
							boolean bImgLoaded = BeseyeJSONUtil.getJSONBoolean(info.mObjCam, BeseyeJSONUtil.MM_HD_IMG_LOADED, false);
//							if(false == bImgLoaded){
//								bImgLoaded = (null != info.mImgTrainPic)?info.mImgTrainPic.isLoaded():false;
//							}
							if(bImgLoaded){
								BeseyeJSONUtil.setJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOADED, true);
								BeseyeJSONUtil.setJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_DELETE, !BeseyeJSONUtil.getJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_DELETE, false));
								try {
									mArrTrainPic.put(idx, objCheck);
									if(null != mHumanDetectTrainPicAdapter){
										mHumanDetectTrainPicAdapter.updateResultList(mArrTrainPic);
										mHumanDetectTrainPicAdapter.notifyDataSetChanged();
									}
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							break;
						}
					}
				}
			}
		}else if(R.id.button_confirm == view.getId()){			
			sendLabelResult();
		}else if(R.id.button_done == view.getId()){
			SessionMgr.getInstance().setHumanDetectIntroShown(true);
			BeseyeUtils.setVisibility(mVpIntro, View.GONE);
			monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectRefineListTask(this), true, mStrVCamID, NUM_OF_REFINE_IMG+"");
		}else if(R.id.btn_continue == view.getId()){
			BeseyeUtils.setVisibility(mVgResultPage, View.GONE);
			monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectRefineListTask(this), true, mStrVCamID, NUM_OF_REFINE_IMG+"");
		}else if(R.id.btn_finish == view.getId()){
			finish();
		}else {
			super.onClick(view);
		}
	}
	
	private void sendLabelResult(){
		int iLenPic = (null != mArrTrainPic)?mArrTrainPic.length():0;
		if(0 < iLenPic){
			for(int idx = 0 ;idx < iLenPic;idx++){
				JSONObject objSend = mArrTrainPicToSend.optJSONObject(idx);
				JSONObject objDel = mArrTrainPic.optJSONObject(idx);
				String strLabel = (false == BeseyeJSONUtil.getJSONBoolean(objDel, BeseyeJSONUtil.MM_HD_IMG_LOADED))?BeseyeJSONUtil.MM_HD_IMG_LABEL_UNDEFINE:
					             ((false == BeseyeJSONUtil.getJSONBoolean(objDel, BeseyeJSONUtil.MM_HD_IMG_DELETE))?BeseyeJSONUtil.MM_HD_IMG_LABEL_HUMAN:BeseyeJSONUtil.MM_HD_IMG_LABEL_NO_HUMAN);
				BeseyeJSONUtil.setJSONString(objSend, BeseyeJSONUtil.MM_HD_IMG_LABEL, strLabel);
				try {
					mArrTrainPicToSend.put(idx, objSend);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			
			JSONObject objPost = new JSONObject();
			BeseyeJSONUtil.setJSONArray(objPost, BeseyeJSONUtil.MM_HD_IMG, mArrTrainPicToSend);
			
			monitorAsyncTask(new BeseyeIMPMMBEHttpTask.SetHumanDetectRefineLabelTask(this), true, mStrVCamID, objPost.toString());
		}
	}
	
	private void onNoTrainPicAvailable(int iCompletePercent){
		if(null != mIvTrainRet){
			mIvTrainRet.setImageResource(R.drawable.h_detection_ya_image);
		}
		
		if(null != mTxtRetDesc){
			mTxtRetDesc.setText(/*String.format(getString(R.string.recognition_percentage), iCompletePercent+"%")+*/getString(R.string.enhance_human_detect_no_pic_desc));
		}
		
		BeseyeUtils.setVisibility(mbtnContinue, View.GONE);
		BeseyeUtils.setVisibility(mVgResultPage, View.VISIBLE);
	}
	
	private void onTrainPicAvailable(int iCompletePercent){
		
		if(null != mIvTrainRet){
			mIvTrainRet.setImageResource(R.drawable.h_detection_ya_image);
		}
		
		if(null != mTxtRetDesc){
			mTxtRetDesc.setText(/*String.format(getString(R.string.recognition_percentage), iCompletePercent+"%")+*/getString(R.string.enhance_human_detect_reward_desc));
		}
		
		BeseyeUtils.setVisibility(mbtnContinue, View.VISIBLE);
		BeseyeUtils.setVisibility(mVgResultPage, View.VISIBLE);
	}
	
	private void onTrainProcessFinished(){
		if(null != mIvTrainRet){
			mIvTrainRet.setImageResource(R.drawable.h_detection_phd_image);
		}
		
		if(null != mTxtRetDesc){
			String strDone = getString(R.string.recognition_percentage_done);
			String strDesc = /*String.format(getString(R.string.recognition_percentage), getString(R.string.recognition_percentage_done))+*/getString(R.string.enhance_human_detect_train_done_desc);
			Spannable wordtoSpan = new SpannableString(strDesc);          

		    int i = strDesc.indexOf(strDone);
		    if(i >=0){
			    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strDone.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    }
			mTxtRetDesc.setText(wordtoSpan);
		}
		
		BeseyeUtils.setVisibility(mbtnContinue, View.GONE);
		BeseyeUtils.setVisibility(mVgResultPage, View.VISIBLE);
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
