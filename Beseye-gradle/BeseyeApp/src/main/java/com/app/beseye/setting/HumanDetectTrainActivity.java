package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.adapter.HumanDetectTrainPicAdapter;
import com.app.beseye.adapter.HumanDetectTrainPicAdapter.HumanDetectTrainItmHolder;
import com.app.beseye.httptask.BeseyeIMPMMBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;
import com.app.beseye.widget.PullToRefreshListView;
import com.app.beseye.widget.RemoteImageView;
import com.app.beseye.widget.RemoteImageView.RemoteImageCallback;

public class HumanDetectTrainActivity extends BeseyeBaseActivity implements RemoteImageCallback{
	final private static int NUM_OF_REFINE_IMG = 20;
	private PullToRefreshListView mlvHumanDetectTrainPicList;
	private HumanDetectTrainPicAdapter mHumanDetectTrainPicAdapter;
	
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;
	private JSONArray mArrTrainPic, mArrTrainPicToSend;
	private boolean mbHaveNextPage = false;
	private ImageView mIvTrainRet;
	private ViewGroup mVgResultPage = null;
	private TextView  mTxtRetDesc;
	private Button mbtnFinish, mbtnContinue, mbtnConfirm;
	
	private int miTrainProgress = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mbIgnoreCamVerCheck = true;
		
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
			Toolbar parent =(Toolbar) mVwNavBar.getParent();
			parent.setContentInsetsAbsolute(0,0);
			parent.setPadding(0,0,0,0);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				mStrVCamName = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_NAME);
				miTrainProgress = BeseyeJSONUtil.getJSONInt(mCam_obj, HumanDetectOptimizationActivity.HD_TRAIN_PROGRESS, -1);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "CameraViewActivity::updateAttrByIntent(), failed to parse, e1:"+e1.toString());
		}
		
//		mArrTrainPic = new JSONArray();
//		for(int idx= 0 ;idx<20;idx++){
//			addTempObj();
//		}
	
		mlvHumanDetectTrainPicList = (PullToRefreshListView) findViewById(R.id.lv_train_pic_lst);
		if(null != mlvHumanDetectTrainPicList){
			mlvHumanDetectTrainPicList.setMode(LvExtendedMode.NONE);
			
			mHumanDetectTrainPicAdapter = new HumanDetectTrainPicAdapter(this, mArrTrainPic, R.layout.layout_human_detect_training_list_itm, this, this);
			if(null != mHumanDetectTrainPicAdapter){
				mHumanDetectTrainPicAdapter.setVCamid(mStrVCamID);
				mlvHumanDetectTrainPicList.setAdapter(mHumanDetectTrainPicAdapter);
			}
		}
		
//		mVpIntro = (ViewPager)findViewById(R.id.intro_gallery);
//		if(null != mVpIntro){
//			mVpIntro.setAdapter(new IntroPageAdapter(this));
//			if(mbNeedToShowIntro){
//				BeseyeUtils.setVisibility(mVpIntro, View.VISIBLE);
//			}
//		}
//		
		mbtnConfirm = (Button)findViewById(R.id.button_confirm);
		if(null != mbtnConfirm){
			mbtnConfirm.setOnClickListener(HumanDetectTrainActivity.this);
			mbtnConfirm.setEnabled(false);
			mbtnConfirm.setVisibility(View.GONE);
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
		//if(!mbNeedToShowIntro){
		if(100 <= miTrainProgress && (BeseyeConfig.PRODUCTION_VER || !SessionMgr.getInstance().getHumanDetectIntroShowAlways())){
			onTrainProcessFinished(true);
		}else{
			monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectRefineListTask(this), true, mStrVCamID, NUM_OF_REFINE_IMG+"");
		}
	}
	
	private RemoteImageCallback mRemoteImageCallbackForPreload = new RemoteImageCallback(){
		@Override
		public void imageLoaded(boolean success, String StrUri) {
			updateImgLoadState(success, StrUri, true);
		}};
	
	private RemoteImageView mImgPreload[] = null;
	private void preloadImages(final int iCntToPreload){
		BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				if(null != mArrTrainPic){
//					int iCount = mArrTrainPic.length();
//					int iRealCntToPreload = iCount - (NUM_OF_REFINE_IMG - iCntToPreload);
					
					int iRealCntToPreload = mArrTrainPic.length();
					if(0 < iRealCntToPreload){
						mImgPreload = new RemoteImageView[iRealCntToPreload];
						for(int idx = 0; idx < iRealCntToPreload;idx++){
							mImgPreload[idx] = new RemoteImageView(HumanDetectTrainActivity.this);
							final String strPath = BeseyeJSONUtil.getJSONString(mArrTrainPic.optJSONObject(idx), BeseyeJSONUtil.MM_HD_IMG_PATH);
							if(BeseyeConfig.DEBUG)
								Log.i(TAG, "preloadImages(), strPath:"+strPath.toString());

							if(null != mImgPreload[idx] && null != strPath && 0 < strPath.length()){
								mImgPreload[idx].setURI(BeseyeIMPMMBEHttpTask.getRefineImgPath(strPath), R.drawable.h_detection_loading_image, mStrVCamID, mRemoteImageCallbackForPreload);
								mImgPreload[idx].disableLoadLastImgByVCamId();
								mImgPreload[idx].disablebBmpTransitionEffect();
								mImgPreload[idx].loadImage();
							}
						}
					}	
				}
			}}, 100L);
	}

	static final private long TIME_TO_CHECK_IMG_STATE = 10000L;
	private boolean mbHaveCheckImgState = false;
	private Runnable mCheckImageStateRunnable = new Runnable(){
		@Override
		public void run() {
			if(DEBUG)
				Log.i(TAG, "mCheckImageStateRunnable::run(), mbHaveCheckImgState:"+mbHaveCheckImgState);
			mbHaveCheckImgState = true;
			int iLenPic = (null != mArrTrainPic)?mArrTrainPic.length():0;
			if(0 < iLenPic){
				boolean bNeedToRefresh = false;
				for(int idx = 0 ;idx < iLenPic;idx++){
					JSONObject objCheck = mArrTrainPic.optJSONObject(idx);
					if(false == BeseyeJSONUtil.getJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_PRELOAD_LOADED) && 
					   false == BeseyeJSONUtil.getJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOADED) && 
					   false == BeseyeJSONUtil.getJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOAD_FAILED)){
						
						BeseyeJSONUtil.setJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOAD_FAILED, true);
						try {
							mArrTrainPic.put(idx, objCheck);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						bNeedToRefresh = true;
					}
				}
				if(bNeedToRefresh){
					if(null != mHumanDetectTrainPicAdapter){
						mHumanDetectTrainPicAdapter.updateResultList(mArrTrainPic);
						mHumanDetectTrainPicAdapter.notifyDataSetChanged();
					}
				}
			}
			checkImgState();
		}};
		
	private void postCheckImageStateRunnable(){
		BeseyeUtils.setEnabled(mbtnConfirm, false);
		BeseyeUtils.setVisibility(mbtnConfirm, View.VISIBLE);
		mbHaveCheckImgState = false;
		BeseyeUtils.removeRunnable(mCheckImageStateRunnable);
		BeseyeUtils.postRunnable(mCheckImageStateRunnable, TIME_TO_CHECK_IMG_STATE);
	}
		
	private void checkImgState(){
		boolean bDisabledBtn = false;
		if(false == mbHaveCheckImgState){
			if(null != mArrTrainPic && 0 < mArrTrainPic.length()){
				int iCount = mArrTrainPic.length();
				for(int idx = 0; idx < iCount;idx++){
					JSONObject objChk = mArrTrainPic.optJSONObject(idx);
					if(!BeseyeJSONUtil.getJSONBoolean(objChk, BeseyeJSONUtil.MM_HD_IMG_LOADED) && 
					   !BeseyeJSONUtil.getJSONBoolean(objChk, BeseyeJSONUtil.MM_HD_IMG_PRELOAD_LOADED) && 
					   !BeseyeJSONUtil.getJSONBoolean(objChk, BeseyeJSONUtil.MM_HD_IMG_LOAD_FAILED)){
						bDisabledBtn = true;
						break;
					}
				}
			}else{
				bDisabledBtn = true;
			}
		}
		
		if(false == bDisabledBtn){
			BeseyeUtils.removeRunnable(mCheckImageStateRunnable);
		}
		BeseyeUtils.setEnabled(mbtnConfirm, !bDisabledBtn);
	}
	
	private void postAndGetTrainProgress(){
		mShowUIAfterGetProgress = new Runnable(){
			@Override
			public void run() {
				onNoTrainPicAvailable(miTrainProgress, true);
				mShowUIAfterGetProgress = null;
			}
		};
		monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectProgressTask(HumanDetectTrainActivity.this), true, mStrVCamID);

	}
	
	private Runnable mShowUIAfterGetProgress = null;
	
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
						if(-1 == miTrainProgress){
							postAndGetTrainProgress();
						}else{
							onNoTrainPicAvailable(miTrainProgress, true);
						}
					}else{
						postCheckImageStateRunnable();
						
						mbHaveNextPage = BeseyeJSONUtil.getJSONBoolean(result.get(0), BeseyeJSONUtil.MM_HD_IMG_PAGING);
						if(null != mHumanDetectTrainPicAdapter){
							mHumanDetectTrainPicAdapter.updateResultList(mArrTrainPic);
							mHumanDetectTrainPicAdapter.notifyDataSetChanged();
						}
						preloadImages(mArrTrainPic.length());
						
						try {
							mArrTrainPicToSend = new JSONArray(mArrTrainPic.toString());
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}else{
					if(-1 == miTrainProgress){
						postAndGetTrainProgress();
					}else{
						onNoTrainPicAvailable(miTrainProgress, true);
					}				}
			}else if(task instanceof BeseyeIMPMMBEHttpTask.SetHumanDetectRefineLabelTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString()+", mbHaveNextPage:"+mbHaveNextPage);
				
					mArrTrainPic = null;
					mArrTrainPicToSend = null;
										
					mShowUIAfterGetProgress = new Runnable(){
						@Override
						public void run() {
							if(null != mHumanDetectTrainPicAdapter){
								mHumanDetectTrainPicAdapter.updateResultList(null);
								mHumanDetectTrainPicAdapter.notifyDataSetChanged();
							}
							
							if(100 > miTrainProgress){
								onTrainRetAndPicAvailable(miTrainProgress, mbHaveNextPage);
							}else{
								onTrainProcessFinished(false);
							}
							mShowUIAfterGetProgress = null;
						}
					};
					monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectProgressTask(HumanDetectTrainActivity.this), true, mStrVCamID);
				}
			}else if(task instanceof BeseyeIMPMMBEHttpTask.GetHumanDetectProgressTask){
				if(0 == iRetCode){
					miTrainProgress = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_HD_TRAIN_PROGRESS);
					BeseyeJSONUtil.setJSONInt(mCam_obj, HumanDetectOptimizationActivity.HD_TRAIN_PROGRESS, miTrainProgress);
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
					if(null != mShowUIAfterGetProgress){
						mShowUIAfterGetProgress.run();
						mShowUIAfterGetProgress = null;
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
			//showErrorDialog(R.string.enhance_human_detect_train_fail_to_load_pic, true);
		}else if(task instanceof BeseyeIMPMMBEHttpTask.SetHumanDetectRefineLabelTask){
			showErrorDialog(R.string.enhance_human_detect_train_fail_to_label, false, iErrType);
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
		updateImgLoadState(success, strPath, false);
	}
	
	private void updateImgLoadState(boolean success, String strPath, boolean bIsPreload){
		if(DEBUG)
			Log.i(TAG, "updateImgLoadState(), strPath:"+strPath+", success:"+success+", bIsPreload:"+bIsPreload);
		if(null != strPath){
			int iLenPic = (null != mArrTrainPic)?mArrTrainPic.length():0;
			if(0 < iLenPic){
				for(int idx = 0 ;idx < iLenPic;idx++){
					JSONObject objCheck = mArrTrainPic.optJSONObject(idx);
					if(strPath.endsWith(BeseyeJSONUtil.getJSONString(objCheck, BeseyeJSONUtil.MM_HD_IMG_PATH))){
						boolean bHavePreLoaded = BeseyeJSONUtil.getJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_PRELOAD_LOADED);
						if(bIsPreload){
							BeseyeJSONUtil.setJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_PRELOAD_LOADED, success);
						}else{
							BeseyeJSONUtil.setJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOADED, success);
						}
							
						boolean bHaveFailed = BeseyeJSONUtil.getJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOAD_FAILED);
						BeseyeJSONUtil.setJSONBoolean(objCheck, BeseyeJSONUtil.MM_HD_IMG_LOAD_FAILED, !success);
						try {
							mArrTrainPic.put(idx, objCheck);
							if(null != mHumanDetectTrainPicAdapter){
								mHumanDetectTrainPicAdapter.updateResultList(mArrTrainPic);
								if((bHaveFailed == success || bHavePreLoaded != success)){
									mHumanDetectTrainPicAdapter.notifyDataSetChanged();
								}
							}
						} catch (JSONException e) {
							e.printStackTrace();
						}
						
						checkImgState();
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
			
			monitorAsyncTask(new BeseyeIMPMMBEHttpTask.SetHumanDetectRefineLabelTask(this).setDialogId(DIALOG_ID_SETTING), true, mStrVCamID, objPost.toString());
		}
	}
	
	private String getProgressText(){
		return (-1 == miTrainProgress ? "--":miTrainProgress)+"%";
	}
	
	private void setColorSpanText(TextView txtView, String strContent, String strHighlight){
		Spannable wordtoSpan = new SpannableString(strContent);          
		if(null != txtView){
			int i = strContent.indexOf(strHighlight);
		    if(i >=0){
			    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    }
		    txtView.setText(wordtoSpan);
		}
	}
	
	private void onNoTrainPicAvailable(int iCompletePercent, boolean bHideExitButton){
		if(null != mIvTrainRet){
			mIvTrainRet.setImageResource(R.drawable.h_detection_ya_image);
		}
		
		if(null != mTxtRetDesc){
			String strProgress = getProgressText();
			String strDesc = String.format(getString(R.string.recognition_percentage), strProgress)+getString(R.string.enhance_human_detect_no_pic_desc);
			setColorSpanText(mTxtRetDesc, strDesc, strProgress);
		}
		
		BeseyeUtils.setVisibility(mbtnFinish, bHideExitButton?View.GONE:View.VISIBLE);
		BeseyeUtils.setVisibility(mbtnContinue, View.GONE);
		BeseyeUtils.setVisibility(mVgResultPage, View.VISIBLE);
	}
	
	private void onTrainRetAndPicAvailable(int iCompletePercent, boolean bHaveMorrePic){
		if(null != mIvTrainRet){
			mIvTrainRet.setImageResource(R.drawable.h_detection_ya_image);
		}
		
		if(null != mTxtRetDesc){
			String strProgress = getProgressText();
			String strDesc = String.format(getString(R.string.recognition_percentage), strProgress)+getString(R.string.enhance_human_detect_reward_desc);
			setColorSpanText(mTxtRetDesc, strDesc, strProgress);
		}
		
		BeseyeUtils.setVisibility(mbtnFinish, View.VISIBLE);
		BeseyeUtils.setVisibility(mbtnContinue, bHaveMorrePic?View.VISIBLE:View.GONE);
		BeseyeUtils.setVisibility(mVgResultPage, View.VISIBLE);
	}
	
	private void onTrainProcessFinished(boolean bHideExitButton){
		if(null != mIvTrainRet){
			mIvTrainRet.setImageResource(R.drawable.h_detection_phd_image);
		}
		
		if(null != mTxtRetDesc){
			String strDone = getString(R.string.recognition_percentage_done);
			String strDesc = String.format(getString(R.string.recognition_percentage), getString(R.string.recognition_percentage_done))+getString(R.string.enhance_human_detect_train_done_desc);
			Spannable wordtoSpan = new SpannableString(strDesc);          

		    int i = strDesc.indexOf(strDone);
		    if(i >=0){
			    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strDone.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    }
			mTxtRetDesc.setText(wordtoSpan);
		}
		
		
		BeseyeUtils.setVisibility(mbtnFinish, bHideExitButton?View.GONE:View.VISIBLE);

		BeseyeUtils.setVisibility(mbtnContinue, View.GONE);
		BeseyeUtils.setVisibility(mVgResultPage, View.VISIBLE);
	}
}
