package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;
import static com.app.beseye.util.BeseyeJSONUtil.ACC_DATA;import static com.app.beseye.util.BeseyeJSONUtil.NOTIFY_OBJ;
import static com.app.beseye.util.BeseyeJSONUtil.OBJ_TIMESTAMP;
import static com.app.beseye.util.BeseyeJSONUtil.STATUS;
import static com.app.beseye.util.BeseyeJSONUtil.getJSONObject;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.app.beseye.BeseyeBaseActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.httptask.BeseyeAccountTask;
import com.app.beseye.httptask.BeseyeCamBEHttpTask;
import com.app.beseye.httptask.BeseyeIMPMMBEHttpTask;
import com.app.beseye.httptask.BeseyeMMBEHttpTask;
import com.app.beseye.httptask.SessionMgr;
import com.app.beseye.util.BeseyeCamInfoSyncMgr;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeJSONUtil;
import com.app.beseye.util.BeseyeStorageAgent;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.BaseTwoBtnDialog;
import com.app.beseye.widget.BeseyeMemCache;
import com.app.beseye.widget.BeseyeSwitchBtn;
import com.app.beseye.widget.BaseTwoBtnDialog.OnTwoBtnClickListener;
import com.app.beseye.widget.BeseyeSwitchBtn.SwitchState;
import com.app.beseye.widget.BeseyeSwitchBtn.OnSwitchBtnStateChangedListener;

public class HumanDetectOptimizationActivity extends BeseyeBaseActivity{

	final public static String HD_TRAIN_PROGRESS = "HD_TRAIN_PROGRESS";
	private View mVwNavBar;
	private ActionBar.LayoutParams mNavBarLayoutParams;	
	private ViewGroup mVgHumanDetectTraining, mVgHumanDetectReset;

	//private boolean mbModified = false;
	private ViewPager mVpIntro;
	private Button mbtnNextStep;
	private boolean mbNeedToShowIntro = true;
	private int miTrainProgress = -1;
	private TextView mTxtTrainingHint;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if(DEBUG)
			Log.i(TAG, "HumanDetetOptimizationActivity::onCreate()");
		
		super.onCreate(savedInstanceState);
		
		getSupportActionBar().setDisplayOptions(0);
		getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM);
		
		mVwNavBar = getLayoutInflater().inflate(R.layout.layout_base_nav, null);
		if(null != mVwNavBar){
			ImageView mIvBack = (ImageView)mVwNavBar.findViewById(R.id.iv_nav_left_btn);
			if(null != mIvBack){
				mIvBack.setOnClickListener(this);
			}
						
			TextView txtTitle = (TextView)mVwNavBar.findViewById(R.id.txt_nav_title);
			if(null != txtTitle){
				txtTitle.setText(R.string.enhance_human_detect);
				txtTitle.setOnClickListener(this);
			}
			
			mNavBarLayoutParams = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
	        getSupportActionBar().setCustomView(mVwNavBar, mNavBarLayoutParams);
			Toolbar parent =(Toolbar) mVwNavBar.getParent();
			parent.setContentInsetsAbsolute(0,0);
			parent.setPadding(0,0,0,0);
		}
		
		try {
			mCam_obj = new JSONObject(getIntent().getStringExtra(CameraListActivity.KEY_VCAM_OBJ));
			if(null != mCam_obj){
				mStrVCamID = BeseyeJSONUtil.getJSONString(mCam_obj, BeseyeJSONUtil.ACC_ID);
				miTrainProgress = BeseyeJSONUtil.getJSONInt(mCam_obj, HumanDetectOptimizationActivity.HD_TRAIN_PROGRESS, -1);
			}
		} catch (JSONException e1) {
			Log.e(TAG, "HumanDetetOptimizationActivity::onCreate(), failed to parse, e1:"+e1.toString());
		}
		
		mVgHumanDetectTraining = (ViewGroup)findViewById(R.id.vg_human_detect_zone_text);
		if(null != mVgHumanDetectTraining){
			mVgHumanDetectTraining.setOnClickListener(this);
		}
		
		mVgHumanDetectReset = (ViewGroup)findViewById(R.id.vg_human_detect_reset);
		if(null != mVgHumanDetectReset){
			mVgHumanDetectReset.setOnClickListener(this);
		}
		
		mTxtTrainingHint = (TextView)findViewById(R.id.human_detect_notification_title_hint);
		setTrainHintText();
		
		mbNeedToShowIntro = SessionMgr.getInstance().getHumanDetectIntroShowAlways() || !SessionMgr.getInstance().getHumanDetectIntroShown();

		mVpIntro = (ViewPager)findViewById(R.id.intro_gallery);
		if(null != mVpIntro){
			mVpIntro.setAdapter(new IntroPageAdapter(this));
			mVpIntro.setOnPageChangeListener(new OnPageChangeListener(){
				int iCurPage = 0;
				AlphaAnimation aniFadeIn = new AlphaAnimation(0.0f, 1.0f);
				AlphaAnimation aniFadeOut = new AlphaAnimation(1.0f, 0.0f);
				@Override
				public void onPageScrollStateChanged(int arg0) {

				}

				@Override
				public void onPageScrolled(int arg0, float arg1, int arg2) {
					
				}

				@Override
				public void onPageSelected(int position) {
					Log.d(TAG, "onPageSelected(), iCurPage:"+iCurPage+", position="+position);	

					if(iCurPage != position){
						if(1 == iCurPage && 2 == position){
							aniFadeOut.cancel();
							aniFadeIn.setDuration(1000);
							aniFadeIn.setAnimationListener(new AnimationListener(){
								@Override
								public void onAnimationStart(Animation animation) {
									BeseyeUtils.setVisibility(mbtnNextStep, View.INVISIBLE);
								}

								@Override
								public void onAnimationEnd(Animation animation) {
									BeseyeUtils.setVisibility(mbtnNextStep, View.VISIBLE);
								}

								@Override
								public void onAnimationRepeat(
										Animation animation) {
									
								}});
							
							mbtnNextStep.startAnimation(aniFadeIn);
						}else if(2 == iCurPage && 1 == position){
							aniFadeIn.cancel();
							aniFadeOut.setDuration(10);
							aniFadeOut.setAnimationListener(new AnimationListener(){
								@Override
								public void onAnimationStart(Animation animation) {
									BeseyeUtils.setVisibility(mbtnNextStep, View.VISIBLE);
								}

								@Override
								public void onAnimationEnd(Animation animation) {
									BeseyeUtils.setVisibility(mbtnNextStep, View.INVISIBLE);
								}

								@Override
								public void onAnimationRepeat(
										Animation animation) {
									
								}});
							mbtnNextStep.startAnimation(aniFadeOut);
						}
						iCurPage = position;
					}
				}});
			if(mbNeedToShowIntro){
				BeseyeUtils.setVisibility(mVpIntro, View.VISIBLE);
			}
		}
	}
	
	private void setTrainHintText(){
		String strHint = getString(R.string.enhance_human_detect_hint);
		String strProgress = (-1 == miTrainProgress ? "--":miTrainProgress)+"%";
		String strDesc = strHint+"\n\n"+String.format(getString(R.string.recognition_percentage), strProgress);
		Spannable wordtoSpan = new SpannableString(strDesc);          
		if(null != mTxtTrainingHint){
			int i = strDesc.indexOf(strProgress);
		    if(i >=0){
			    wordtoSpan.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.csl_link_font_color)), i, i+strProgress.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		    }
		    mTxtTrainingHint.setText(wordtoSpan);
		}
	}

	@Override
	public void onPostExecute(AsyncTask<String, Double, List<JSONObject>> task, List<JSONObject> result, int iRetCode) {
		if(BeseyeConfig.DEBUG)
			Log.d(TAG, "onPostExecute(), "+task.getClass().getSimpleName()+", iRetCode="+iRetCode);	
		if(!task.isCancelled()){
			if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
				
					JSONObject notify_obj =  BeseyeJSONUtil.getJSONObject(result.get(0), NOTIFY_OBJ);
					BeseyeJSONUtil.setJSONObject(getJSONObject(mCam_obj, ACC_DATA), NOTIFY_OBJ, notify_obj);
					BeseyeJSONUtil.setJSONLong(mCam_obj, OBJ_TIMESTAMP, BeseyeJSONUtil.getJSONLong(result.get(0), BeseyeJSONUtil.OBJ_TIMESTAMP));
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
				}
			}else if(task instanceof BeseyeIMPMMBEHttpTask.GetHumanDetectProgressTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
				
					miTrainProgress = BeseyeJSONUtil.getJSONInt(result.get(0), BeseyeJSONUtil.MM_HD_TRAIN_PROGRESS);
					BeseyeJSONUtil.setJSONInt(mCam_obj, HD_TRAIN_PROGRESS, miTrainProgress);
					BeseyeCamInfoSyncMgr.getInstance().updateCamInfo(mStrVCamID, mCam_obj);
				}
				
				setTrainHintText();
			}else if(task instanceof BeseyeIMPMMBEHttpTask.SetHumanDetectResetTask){
				if(0 == iRetCode){
					if(DEBUG)
						Log.i(TAG, "onPostExecute(), "+result.toString());
				
					miTrainProgress = 0;
					BeseyeJSONUtil.setJSONInt(mCam_obj, HD_TRAIN_PROGRESS, miTrainProgress);
					setTrainHintText();
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
		if(task instanceof BeseyeCamBEHttpTask.SetNotifySettingTask){
			showErrorDialog(R.string.cam_setting_fail_to_update_notify_setting, true, iErrType);
		}else if (task instanceof BeseyeIMPMMBEHttpTask.SetHumanDetectResetTask){
			showErrorDialog(R.string.enhance_human_detect_train_fail_to_reset, true, iErrType);
		}else{
			super.onErrorReport(task, iErrType, strTitle, strMsg);
		}
	}
	
	@Override
	protected void onSessionComplete(){
		super.onSessionComplete();
		//monitorAsyncTask(new BeseyeMMBEHttpTask.GetLatestThumbnailTask(this).setDialogId(-1), true, mStrVCamID);
		monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectProgressTask(this).setDialogId(-1), true, mStrVCamID);
		if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID){
			monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	@Override
	public void onClick(View view){
		switch(view.getId()){
			case R.id.vg_human_detect_zone_text:{
				Bundle b = new Bundle();
				b.putString(CameraListActivity.KEY_VCAM_OBJ, mCam_obj.toString());
				launchActivityByClassName(HumanDetectTrainActivity.class.getName(),b);
				break;
			} 
			case R.id.vg_human_detect_reset:{
				showMyDialog(DIALOG_ID_RESET_HUMAN_DETECT);
				break;
			}
			case R.id.txt_nav_title:{
				if(BeseyeConfig.DEBUG){
					//BeseyeStorageAgent.doDeleteCacheByFolder(this, mStrVCamID);
					BeseyeStorageAgent.doDeleteCache(this);
					BeseyeMemCache.cleanMemCache();
					Toast.makeText(this, "Delete cache...", Toast.LENGTH_SHORT).show();
				}
				break;
			}
			case R.id.button_done:{
				SessionMgr.getInstance().setHumanDetectIntroShown(true);
				BeseyeUtils.setVisibility(mVpIntro, View.GONE);
				monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectProgressTask(this).setDialogId(-1), true, mStrVCamID);
				break;
			}
			default:
				super.onClick(view);	
		}
	}
	
	@Override
	protected void onResume() {
		if(DEBUG)
			Log.i(TAG, "HumanDetetOptimizationActivity::onResume()");	
		super.onResume();
		
		if(!mbFirstResume){
			monitorAsyncTask(new BeseyeIMPMMBEHttpTask.GetHumanDetectProgressTask(this).setDialogId(-1), true, mStrVCamID);
			monitorAsyncTask(new BeseyeAccountTask.GetCamInfoTask(this).setDialogId(-1), true, mStrVCamID);
			if(null == BeseyeJSONUtil.getJSONObject(mCam_obj, BeseyeJSONUtil.ACC_DATA) && null != mStrVCamID)
				monitorAsyncTask(new BeseyeCamBEHttpTask.GetCamSetupTask(this).setDialogId(-1), true, mStrVCamID);
		}
	}
	
	@Override
	protected int getLayoutId() {
		return R.layout.layout_human_detect_optimization;
	}
		
	@Override
	public void onCamSetupChanged(String strVcamId, long lTs,
			JSONObject objCamSetup) {
		super.onCamSetupChanged(strVcamId, lTs, objCamSetup);
		miTrainProgress = BeseyeJSONUtil.getJSONInt(mCam_obj, HumanDetectOptimizationActivity.HD_TRAIN_PROGRESS, -1);
		setTrainHintText();
	}

	@Override
	protected Dialog onCreateDialog(int id, final Bundle bundle) {
		Dialog dialog;
		switch(id){
			case DIALOG_ID_RESET_HUMAN_DETECT:{
				BaseTwoBtnDialog d = new BaseTwoBtnDialog(this);
				d.setBodyText(getString(R.string.dialog_reset_human_detect));
				d.setTitleText(getString(R.string.dialog_title_warning));
				d.setOnTwoBtnClickListener(new OnTwoBtnClickListener(){
					@Override
					public void onBtnYesClick() {
						monitorAsyncTask(new BeseyeIMPMMBEHttpTask.SetHumanDetectResetTask(HumanDetectOptimizationActivity.this), true, mStrVCamID);
					}
					@Override
					public void onBtnNoClick() {
						
					}} );
				dialog = d;
				dialog.setOnDismissListener(new OnDismissListener(){
					@Override
					public void onDismiss(DialogInterface dialog) {
						removeMyDialog(DIALOG_ID_RESET_HUMAN_DETECT);
					}});
				break;
			}
			default:
				dialog = super.onCreateDialog(id, bundle);
		}
		return dialog;
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
					mbtnNextStep = (Button)vGroup.findViewById(R.id.button_done);
					if(null != mbtnNextStep){
						mbtnNextStep.setOnClickListener(HumanDetectOptimizationActivity.this);
						mbtnNextStep.setVisibility(View.INVISIBLE);
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
