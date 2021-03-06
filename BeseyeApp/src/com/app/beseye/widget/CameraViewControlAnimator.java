package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;


import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.app.beseye.CameraViewActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeFeatureConfig;
import com.app.beseye.util.BeseyeNewFeatureMgr;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.util.BeseyeNewFeatureMgr.BESEYE_NEW_FEATURE;

public class CameraViewControlAnimator {
	
	static private final long TIME_TO_HIDE_HEADER  = 7000L;
	
	private Animation m_aniHeaderFadeIn = null;
	private Animation m_aniHeaderFadeOut = null;
	
	private Animation m_aniToolbarFadeIn = null;
	private Animation m_aniToolbarFadeOut = null;
	
	private Animation m_aniHoldToTalkFadeIn = null;
	private Animation m_aniHoldToTalkFadeOut = null;
	
	private RelativeLayout m_vgHeaderLayout = null;
	private RelativeLayout m_vgToolbarLayout = null;
	
	private ViewGroup mVgNavbarPortrait;
	private ViewGroup mVgToolbarPortrait;
	private ViewGroup mVgNavbarLandscape;
	private ViewGroup mVgToolbarLandscape;
	
	private ViewGroup mVgHoldToTalk;
	private AmplitudeImageView mAmplitudeImageView;
	
	private TextView mTxtDate, mTxtCamName, mTxtTime, mTxtEvent, mTxtGoLive;
	private ImageView mIvStreamType, mIvBack, mIvScreenshotNew, mIvSettingNew;
	private ImageButton mIbTalk, mIbRewind, mIbPlayPause, mIbFastForward, mIbSetting, mIbScreenshot;	
	
	private WeakReference<CameraViewActivity> mCameraViewActivity;
	private int miOrientation;
	private int miStatusBarHeight = 0;
	private Rect mRectTalkBtn;
	
	private boolean m_bInHeaderAnimation = false;
	private boolean m_bInToolbarAnimation = false;
	private boolean m_bInHoldToTalkAnimation = false;
	private boolean m_bCheckHoldToTalkAfterAnim = false;
	
	private Runnable mHideHeaderRunnable = new Runnable(){
		@Override
		public void run() {
			if(View.VISIBLE == getVisibility() && false == isInAnimation()){
				CameraViewActivity act = mCameraViewActivity.get();
				if(null != act && act.isScreenTouched()){
					Log.i(TAG, "isScreenTouched is true, extend...");
					extendHideControl();
				}else{
					performControlAnimation();
				}
			}
		}};
	
	
	public void setOrientation(int iOrient){
		miOrientation = iOrient;
		
		onTalkBtnReleased();
		
		CameraViewActivity act = mCameraViewActivity.get();
		if(null != act){
			if(Configuration.ORIENTATION_PORTRAIT == miOrientation){
				if(m_vgHeaderLayout.getVisibility() == View.VISIBLE && false == isInAnimation()){
					act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
					act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				}
			}else{
				act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
				act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			}
			
			if(null != m_vgHeaderLayout){
				m_vgHeaderLayout.removeAllViews();
				m_vgHeaderLayout.addView((Configuration.ORIENTATION_PORTRAIT == miOrientation)?mVgNavbarPortrait:mVgNavbarLandscape, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				syncHeaderLayoutItmProperty();
			}
			
			if(null != m_vgToolbarLayout){
				m_vgToolbarLayout.removeAllViews();
				m_vgToolbarLayout.addView((Configuration.ORIENTATION_PORTRAIT == miOrientation)?mVgToolbarPortrait:mVgToolbarLandscape, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				syncToolbarLayoutItmProperty();
			}
			
			setControlVisibility(View.VISIBLE);
			extendHideControl();
		}
	}
	
	private void syncHeaderLayoutItmProperty(){
		ViewGroup vgReference = null;
		if(Configuration.ORIENTATION_PORTRAIT == miOrientation){
			vgReference = mVgNavbarPortrait;
		}else{
			vgReference = mVgNavbarLandscape;
		}
		
		CameraViewActivity act = mCameraViewActivity.get();
		if(null != act){
			TextView txtDate = (TextView)vgReference.findViewById(R.id.txt_streaming_date);
			if(null != txtDate){
				syncViewProprety(mTxtDate, txtDate);
				mTxtDate = txtDate;
			}
			
			TextView txtTime = (TextView)vgReference.findViewById(R.id.txt_streaming_time);
			if(null != txtTime){
				syncViewProprety(mTxtTime, txtTime);
				mTxtTime = txtTime;
			}
			
			TextView txtCamName = (TextView)vgReference.findViewById(R.id.txt_cam_name);
			if(/*!ASSIGN_ST_PATH && */null != txtCamName){
				syncViewProprety(mTxtCamName, txtCamName);
				mTxtCamName = txtCamName;
				mTxtCamName.setOnClickListener(act);
				//mTxtCamName.setMovementMethod(ScrollingMovementMethod.getInstance());
			}
			
			ImageView ivStreamType = (ImageView)vgReference.findViewById(R.id.iv_streaming_type);
			if(null != ivStreamType){
				syncViewProprety(mIvStreamType, ivStreamType);
				mIvStreamType = ivStreamType;
				mIvStreamType.setOnClickListener(act);
			}
			
			ImageView ivBack = (ImageView)vgReference.findViewById(R.id.iv_back);
			if(null != ivBack){
				syncViewProprety(mIvBack, ivBack);
				mIvBack = ivBack;
				mIvBack.setOnClickListener(act);
			}
			
			ImageButton ibSetting = (ImageButton)vgReference.findViewById(R.id.ib_settings);
			if(null != ibSetting){
				syncViewProprety(mIbSetting, ibSetting);
				mIbSetting = ibSetting;
				mIbSetting.setOnClickListener(act);
			}
			
			ImageView ivSettingNew = (ImageView)vgReference.findViewById(R.id.iv_settings_news);
			if(null != ivSettingNew){
				syncViewProprety(mIvSettingNew, ivSettingNew);
				mIvSettingNew = ivSettingNew;
				checkSettingNewStatus(ivSettingNew.getVisibility() == View.VISIBLE);
			}
		}
	}
	
	public void checkSettingNewStatus(boolean bIsSettingViewVisible){
		if(null != mIvSettingNew){
			boolean bAllNewFeaturesInSettingPageTrigger = BeseyeNewFeatureMgr.getInstance().isFeatureClicked(BESEYE_NEW_FEATURE.FEATURE_SCHEDULE);
						
			BeseyeUtils.setVisibility(mIvSettingNew, (!bIsSettingViewVisible || bAllNewFeaturesInSettingPageTrigger)?View.INVISIBLE:View.VISIBLE);
		}
	}
	
	private long mlLastPressDownTs = -1;
	private boolean mbLongPressMode = false;
	
	private void syncToolbarLayoutItmProperty(){
		ViewGroup vgReference = null;
		if(Configuration.ORIENTATION_PORTRAIT == miOrientation){
			vgReference = mVgToolbarPortrait;
		}else{
			vgReference = mVgToolbarLandscape;
		}
		
		CameraViewActivity act = mCameraViewActivity.get();
		if(null != act){
			
			TextView txtEvent = (TextView)vgReference.findViewById(R.id.txt_events);
			if(null != txtEvent){
				syncViewProprety(mTxtEvent, txtEvent);
				mTxtEvent = txtEvent;
				mTxtEvent.setOnClickListener(act);
			}
			
			TextView txtGoLive = (TextView)vgReference.findViewById(R.id.txt_go_live);
			if(null != txtGoLive){
				syncViewProprety(mTxtGoLive, txtGoLive);
				mTxtGoLive = txtGoLive;
				mTxtGoLive.setOnClickListener(act);
				//mTxtGoLive.setEnabled(!mbIsLiveMode);
			}
			
			ImageButton ibTalk = (ImageButton)vgReference.findViewById(R.id.ib_talk);
			if(null != ibTalk){
				syncViewProprety(mIbTalk, ibTalk);
				mIbTalk = ibTalk;
				//mIbTalk.setOnClickListener(act);
				mIbTalk.setOnTouchListener(new OnTouchListener(){
					@Override
					public boolean onTouch(View view, MotionEvent event) {
						if(event.getAction() == MotionEvent.ACTION_DOWN){
							if(BeseyeFeatureConfig.TWO_WAY_TALK_LONG_PRESS_SIMU){
								long lCurPressDownTs = System.currentTimeMillis();
								if(-1 != mlLastPressDownTs && (lCurPressDownTs - mlLastPressDownTs) < 1000L){
									mbLongPressMode = !mbLongPressMode;
									mlLastPressDownTs = -1;
									Log.i(TAG, "mbLongPressMode becomes "+mbLongPressMode);
	
								}else{
									mlLastPressDownTs = lCurPressDownTs;
								}
							}
							onTalkBtnPressed(view);
						}else if (event.getAction() == MotionEvent.ACTION_MOVE){
							if(null != mRectTalkBtn && !mRectTalkBtn.contains(view.getLeft() + (int) event.getX(), view.getTop() + (int) event.getY())){
					            // User moved outside bounds
								if(!BeseyeConfig.DEBUG || false == mbLongPressMode){
									onTalkBtnReleased();
								}
					        }else{
					        	extendHideControl();
					        }
			            }else if (event.getAction() == MotionEvent.ACTION_UP){
			            	if(!BeseyeConfig.DEBUG || false == mbLongPressMode){
								onTalkBtnReleased();
							}
			            }
						return false;
					}});
				//mIbTalk.setEnabled(false);//not implement
			}
			
			ImageButton ibRewind = (ImageButton)vgReference.findViewById(R.id.ib_rewind);
			if(null != ibRewind){
				syncViewProprety(mIbRewind, ibRewind);
				mIbRewind = ibRewind;
				mIbRewind.setOnClickListener(act);
				mIbRewind.setEnabled(false);//not implement
			}
			
			ImageButton ibPlayPause = (ImageButton)vgReference.findViewById(R.id.ib_play_pause);
			if(null != ibPlayPause){
				syncViewProprety(mIbPlayPause, ibPlayPause);
				mIbPlayPause = ibPlayPause;
				mIbPlayPause.setOnClickListener(act);
			}
			
			ImageButton ibScreenshot = (ImageButton)vgReference.findViewById(R.id.ib_screenshot);
			if(null != ibScreenshot){
				syncViewProprety(mIbScreenshot, ibScreenshot);
				mIbScreenshot = ibScreenshot;
				//mIbScreenshot.setOnClickListener(act);
				mIbScreenshot.setOnTouchListener(new OnTouchListener(){
					@Override
					public boolean onTouch(View view, MotionEvent event) {
						if(event.getAction() == MotionEvent.ACTION_DOWN){
							CameraViewActivity act = mCameraViewActivity.get();
							if(null != act){
								act.pressToScreenshot();
							}
						}
						return false;
					}});
			}
			
			ImageView ivScreenshotNew = (ImageView)vgReference.findViewById(R.id.iv_camera_news);
			if(null != ivScreenshotNew){
				mIvScreenshotNew = ivScreenshotNew;
				BeseyeUtils.setVisibility(mIvScreenshotNew, !BeseyeNewFeatureMgr.getInstance().isScreenshotFeatureClicked()?View.VISIBLE:View.INVISIBLE);
			}
			
			ImageButton ibFastForward = (ImageButton)vgReference.findViewById(R.id.ib_fast_forward);
			if(null != ibFastForward){
				syncViewProprety(mIbFastForward, ibFastForward);
				mIbFastForward = ibFastForward;
				mIbFastForward.setOnClickListener(act);
				mIbFastForward.setEnabled(false);//not implement
			}
		}
	}
	
	public TextView getCamNameView(){
		return mTxtCamName;
	}
	
	public TextView getTimeView(){
		return mTxtTime;
	}
	
	public TextView getDateView(){
		return mTxtDate;
	}
	
	public TextView getEventsView(){
		return mTxtEvent;
	}
	
	public TextView getGoLiveView(){
		return mTxtGoLive;
	}
	
	public ImageView getStremTypeView(){
		return mIvStreamType;
	}
	
	public ImageButton getSettingView(){
		return mIbSetting;
	}
	
	public ImageView getSettingNewView(){
		return mIvSettingNew;
	}
	
	public ImageButton getTalkView(){
		return mIbTalk;
	}
	
	public ImageButton getPlayPauseView(){
		return mIbPlayPause;
	}
	
	public ImageButton getScreenshotView(){
		return mIbScreenshot;
	}
	
	public ImageView getScreenshotNewView(){
		return mIvScreenshotNew;
	}
	
	public ImageButton getPlayPauseViewOrient(){
		ViewGroup vgReference = null;
		if(Configuration.ORIENTATION_PORTRAIT == miOrientation){
			vgReference = mVgToolbarLandscape;
		}else{
			vgReference = mVgToolbarPortrait;
		}
		return (ImageButton)vgReference.findViewById(R.id.ib_play_pause);
	}
	
	public ImageButton getRewindView(){
		return mIbRewind;
	}
	
	public ImageButton getFastForwardView(){
		return mIbFastForward;
	}
	
	static private void syncViewProprety(View viewSrc, View viewDest){
		if(null != viewSrc && null != viewDest){
			viewDest.setEnabled(viewSrc.isEnabled());
			viewDest.setVisibility(viewSrc.getVisibility());
			viewDest.setClickable(viewSrc.isClickable());
			viewDest.setFocusable(viewSrc.isFocusable());
			
			if(viewDest instanceof TextView && viewSrc instanceof TextView){
				((TextView)viewDest).setText(((TextView)viewSrc).getText());
			}
		}
	}
	
	public void setStatusBarHeight(int iHeight){
		miStatusBarHeight = iHeight;
	}
	
	public CameraViewControlAnimator(Context context, RelativeLayout headerLayout, RelativeLayout toolbarLayout, ViewGroup vgHoldToTalk, int iStatusBarHeight) {
		m_vgHeaderLayout = headerLayout;
		m_vgToolbarLayout = toolbarLayout;
		mVgHoldToTalk = vgHoldToTalk;
		miStatusBarHeight = iStatusBarHeight;
		mCameraViewActivity = new WeakReference<CameraViewActivity>((CameraViewActivity)context);
		initViews(context);
	}

	private void initViews(Context context){
		LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if(null != inflater){
			mVgNavbarPortrait = (ViewGroup)inflater.inflate(R.layout.layout_camera_view_navbar, null);
			if(null != mVgNavbarPortrait){
				ViewGroup title = (ViewGroup)mVgNavbarPortrait.findViewById(R.id.vg_nav_bar_title_up);
				if(null != title){
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, miStatusBarHeight);
					title.setLayoutParams(params);
				}
			}
			
			mVgToolbarPortrait = (ViewGroup)inflater.inflate(R.layout.layout_camera_view_footer_live, null);
			mVgNavbarLandscape = (ViewGroup)inflater.inflate(R.layout.layout_camera_view_navbar_land, null);
			mVgToolbarLandscape = (ViewGroup)inflater.inflate(R.layout.layout_camera_view_footer_live_land, null);
		}
		
		mAmplitudeImageView = (AmplitudeImageView)mVgHoldToTalk.findViewById(R.id.img_hold_to_talk_mic_mask);
		
		initAnimations(context);
	}
	
	public void setAmplitudeRatio(float fRatio){
		if(null != mAmplitudeImageView){
			mAmplitudeImageView.setAmplitudeRatio(fRatio);
		}
	}

	private void initAnimations(Context context){
		if(null == m_aniHeaderFadeIn){
			m_aniHeaderFadeIn = AnimationUtils.loadAnimation(context, R.anim.header_enter);
		}
		
		if(null == m_aniHeaderFadeOut){
			m_aniHeaderFadeOut = AnimationUtils.loadAnimation(context, R.anim.header_exit);
		}
		
		if(null == m_aniToolbarFadeIn){
			m_aniToolbarFadeIn = AnimationUtils.loadAnimation(context, R.anim.footer_enter);
		}
		
		if(null == m_aniToolbarFadeOut){
			m_aniToolbarFadeOut = AnimationUtils.loadAnimation(context, R.anim.footer_exit);
		}
		
		if(null == m_aniHoldToTalkFadeIn){
			m_aniHoldToTalkFadeIn = AnimationUtils.loadAnimation(context, R.anim.hold_to_talk_enter);
		}
		
		if(null == m_aniHoldToTalkFadeOut){
			m_aniHoldToTalkFadeOut = AnimationUtils.loadAnimation(context, R.anim.hold_to_talk_exit);
		}
		
		registerAnimationListeners();
	}
	
	
	private void registerAnimationListeners(){
		if(null != m_aniHeaderFadeIn)
			m_aniHeaderFadeIn.setAnimationListener(mHeaderFadeInListener);
		
		if(null != m_aniHeaderFadeOut)
			m_aniHeaderFadeOut.setAnimationListener(mHeaderFadeOutListener);
		
		if(null != m_aniToolbarFadeIn)
			m_aniToolbarFadeIn.setAnimationListener(mToolbarFadeInListener);
		
		if(null != m_aniToolbarFadeOut)
			m_aniToolbarFadeOut.setAnimationListener(mToolbarFadeOutListener);
		
		if(null != m_aniHoldToTalkFadeIn)
			m_aniHoldToTalkFadeIn.setAnimationListener(mHoldToTalkFadeInListener);
		
		if(null != m_aniHoldToTalkFadeOut)
			m_aniHoldToTalkFadeOut.setAnimationListener(mHoldToTalkFadeOutListener);
	}
	
	public int getVisibility(){
		return (null != m_vgHeaderLayout)?m_vgHeaderLayout.getVisibility():View.GONE;
	}
	
	public boolean isInAnimation(){
		return m_bInHeaderAnimation || m_bInToolbarAnimation;
	}
	
	public void startHideControlRunnable(){
		extendHideControl();
	}
	
	public void extendHideControl(){
		if(null != mHideHeaderRunnable){
			BeseyeUtils.removeRunnable(mHideHeaderRunnable);
			BeseyeUtils.postRunnable(mHideHeaderRunnable, TIME_TO_HIDE_HEADER);
		}
	}
	
	public void cancelHideControl(){
		if(null != mHideHeaderRunnable){
			BeseyeUtils.removeRunnable(mHideHeaderRunnable);
		}
	}
	
	public void performControlAnimation(){
		if(false == isInAnimation()){
			if(null != m_vgHeaderLayout){
				Animation animation = null;
				CameraViewActivity act = mCameraViewActivity.get();
				if(View.VISIBLE == m_vgHeaderLayout.getVisibility()){
					if(Configuration.ORIENTATION_PORTRAIT != miOrientation){
						if(null != mVgHoldToTalk && View.VISIBLE == mVgHoldToTalk.getVisibility()){
							Thread.dumpStack();
							return;
						}
						animation = m_aniHeaderFadeOut;
						m_vgHeaderLayout.startAnimation(animation);
						
						if(null != act){
							act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
							act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						}
						cancelHideControl();
					}
				}else{
					//animation = m_aniHeaderFadeIn;
					m_vgHeaderLayout.bringToFront();
					m_vgHeaderLayout.setVisibility(View.VISIBLE);
					startHideControlRunnable();
					
					if(null != act){
						if(Configuration.ORIENTATION_PORTRAIT == miOrientation){
							if(!act.isShowStatusForEvent()){
								act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
								act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
							}
						}else{
							act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
							act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						}
					}
				}
			}
			
			if(null != m_vgToolbarLayout){
				Animation animation = null;
				if(View.VISIBLE == m_vgToolbarLayout.getVisibility()){
					if(Configuration.ORIENTATION_PORTRAIT != miOrientation){
						animation = m_aniToolbarFadeOut;
						m_vgToolbarLayout.startAnimation(animation);
						cancelHideControl();
					}
				}else if(!mbP2PMode){
					//animation = m_aniToolbarFadeIn;
					m_vgToolbarLayout.bringToFront();
					m_vgToolbarLayout.setVisibility(View.VISIBLE);
				}
			}
		}
	}
	
	public void performHoldToTalkAnimation(){
		if(false == m_bInHoldToTalkAnimation){
			if(null != mVgHoldToTalk){
				Animation animation = null;
				//CameraViewActivity act = mCameraViewActivity.get();
				if(View.VISIBLE == mVgHoldToTalk.getVisibility()){					
					animation = m_aniHoldToTalkFadeOut;
					mVgHoldToTalk.startAnimation(animation);
				}else{
					//animation = m_aniHeaderFadeIn;
					mVgHoldToTalk.bringToFront();
					mVgHoldToTalk.setVisibility(View.VISIBLE);
				}
			}
		}else{
			m_bCheckHoldToTalkAfterAnim = true;
		}
	}
	
	private void cancelAnimation(){
		if(isInAnimation()){
			if(null != m_vgHeaderLayout){
				Animation animation = m_vgHeaderLayout.getAnimation();
				if(null != animation)
					animation.cancel();
			}
			
			if(null != m_vgToolbarLayout){
				Animation animation = m_vgToolbarLayout.getAnimation();
				if(null != animation)
					animation.cancel();
			}
		}
	}
	
	private void setControlVisibility(int visibility){
		cancelAnimation();
		if(null != m_vgHeaderLayout){
			m_vgHeaderLayout.setVisibility(visibility);
			if(visibility == View.VISIBLE){
				CameraViewActivity act = mCameraViewActivity.get();
				if(null != act){
					if(Configuration.ORIENTATION_PORTRAIT == miOrientation){
						act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
						act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
					}else{
						act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
						act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
					}
				}
			}
		}
		
		if(null != m_vgToolbarLayout)
			m_vgToolbarLayout.setVisibility(mbP2PMode?View.INVISIBLE:visibility);
	}
	
	public void showControl(){
		setControlVisibility(View.VISIBLE);	
	}
	
	public void hideControl(){
		if(Configuration.ORIENTATION_PORTRAIT != miOrientation){
			setControlVisibility(View.GONE);
		}
		
//		CameraViewActivity act = mCameraViewActivity.get();
//		if(null != act){
//			act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//			act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//		}
	}
	
	private boolean mbP2PMode = false;
	
	public void setP2PMode(boolean bIsP2P){
		mbP2PMode = bIsP2P;
		if(null != m_vgToolbarLayout && mbP2PMode){
			if(null != m_vgToolbarLayout)
				m_vgToolbarLayout.setVisibility(View.INVISIBLE);
		}
	}
	
	private AnimationListener mHeaderFadeInListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgHeaderLayout){
				m_vgHeaderLayout.setVisibility(View.VISIBLE);
			}
			m_bInHeaderAnimation = false;
			startHideControlRunnable();
			if(DEBUG)
				Log.d(TAG, "mHeaderFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_vgHeaderLayout.bringToFront();
			m_bInHeaderAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mHeaderFadeInListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mHeaderFadeOutListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgHeaderLayout){
				m_vgHeaderLayout.setVisibility(View.INVISIBLE);
			}
			
			m_bInHeaderAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mHeaderFadeOutListener::onAnimationEnd()");
			
//			BeseyeUtils.postRunnable(new Runnable(){
//				@Override
//				public void run() {
//					CameraViewActivity act = mCameraViewActivity.get();
//					if(null != act){
//						act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//						act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//					}
//				}}, 1000);
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_bInHeaderAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mHeaderFadeOutListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mToolbarFadeInListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgToolbarLayout){
				m_vgToolbarLayout.setVisibility(mbP2PMode?View.INVISIBLE:View.VISIBLE);
			}
			m_bInToolbarAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mToolbarFadeInListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			if(!mbP2PMode)
				m_vgToolbarLayout.bringToFront();
			m_bInToolbarAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mToolbarFadeInListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mToolbarFadeOutListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgToolbarLayout){
				m_vgToolbarLayout.setVisibility(View.INVISIBLE);
			}
			
			m_bInToolbarAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mToolbarFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_bInToolbarAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mToolbarFadeOutListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mHoldToTalkFadeInListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != mVgHoldToTalk){
				mVgHoldToTalk.setVisibility(mbP2PMode?View.INVISIBLE:View.VISIBLE);
			}
			m_bInHoldToTalkAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mHoldToTalkFadeInListener::onAnimationEnd()");
			
			checkHoldToTalkBtnStatus();
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			if(!mbP2PMode)
				mVgHoldToTalk.bringToFront();
			m_bInHoldToTalkAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mHoldToTalkFadeInListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mHoldToTalkFadeOutListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != mVgHoldToTalk){
				mVgHoldToTalk.setVisibility(View.INVISIBLE);
			}
			
			m_bInHoldToTalkAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mHoldToTalkFadeOutListener::onAnimationEnd()");
			checkHoldToTalkBtnStatus();
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_bInHoldToTalkAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mHoldToTalkFadeOutListener::onAnimationStart()");
		}
	};
	
	private void checkHoldToTalkBtnStatus(){
		if(m_bCheckHoldToTalkAfterAnim){
			m_bCheckHoldToTalkAfterAnim = false;
			
			if((mVgHoldToTalk.getVisibility() == View.INVISIBLE && mIbTalk.isPressed()) || (mVgHoldToTalk.getVisibility() == View.VISIBLE && !mIbTalk.isPressed()) ){
				performHoldToTalkAnimation();
			}
		}
	}
	
	private void onTalkBtnPressed(View view){
		if(DEBUG)
			Log.i(TAG, "onTalkBtnPressed()");
		
		if(null == mRectTalkBtn){
			mRectTalkBtn = new Rect(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
	        cancelHideControl();
	        performHoldToTalkAnimation();
	        CameraViewActivity act = mCameraViewActivity.get();
			if(null != act){
				act.pressToTalk();
			}
		}else{
			Log.w(TAG, "onTalkBtnPressed(), mRectTalkBtn isn't null");
		}
	}
	
	private void onTalkBtnReleased(){
		if(DEBUG)
			Log.i(TAG, "onTalkBtnReleased()");
		if(null != mRectTalkBtn){
        	startHideControlRunnable();
        	performHoldToTalkAnimation();
        	mRectTalkBtn = null;
        	CameraViewActivity act = mCameraViewActivity.get();
			if(null != act){
				act.releaseTalkMode();
			}
    	}else{
    		Log.w(TAG, "onTalkBtnReleased(), mRectTalkBtn is null");
    	}
	}
	
	public void terminateTalkMode(){
		if(DEBUG)
			Log.i(TAG, "terminateTalkMode()");
		mbLongPressMode = false;
		onTalkBtnReleased();
	}
	
	public boolean isInHoldToTalkMode(){
		return mRectTalkBtn != null;
	}
}
