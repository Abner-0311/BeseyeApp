package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.ASSIGN_ST_PATH;
import static com.app.beseye.util.BeseyeConfig.COMPUTEX_DEMO;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.app.beseye.CameraViewActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeConfig;
import com.app.beseye.util.BeseyeUtils;

public class CameraViewControlAnimator {
	
	static private final long TIME_TO_HIDE_HEADER  = 7000L;
	
	static private Animation s_aniHeaderFadeIn = null;
	static private Animation s_aniHeaderFadeOut = null;
	
	static private Animation s_aniToolbarFadeIn = null;
	static private Animation s_aniToolbarFadeOut = null;
	
	private RelativeLayout m_vgHeaderLayout = null;
	private RelativeLayout m_vgToolbarLayout = null;
	
	private ViewGroup mVgNavbarPortrait;
	private ViewGroup mVgToolbarPortrait;
	private ViewGroup mVgNavbarLandscape;
	private ViewGroup mVgToolbarLandscape;
	
	private TextView mTxtDate, mTxtCamName, mTxtTime, mTxtEvent, mTxtGoLive;
	private ImageView mIvStreamType;
	private ImageButton mIbTalk, mIbRewind, mIbPlayPause, mIbFastForward, mIbSetting;	
	
	private WeakReference<CameraViewActivity> mCameraViewActivity;
	private int miOrientation;
	private int miStatusBarHeight = 0;
	private boolean m_bInHeaderAnimation = false;
	private boolean m_bInToolbarAnimation = false;
	
	private Runnable mHideHeaderRunnable = new Runnable(){
		@Override
		public void run() {
			if(View.VISIBLE == getVisibility())
				performControlAnimation();
		}};
	
	
	public void setOrientation(int iOrient){
		miOrientation = iOrient;
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
				m_vgHeaderLayout.addView((Configuration.ORIENTATION_PORTRAIT == miOrientation)?mVgNavbarPortrait:mVgNavbarLandscape, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				syncHeaderLayoutItmProperty();
			}
			
			if(null != m_vgToolbarLayout){
				m_vgToolbarLayout.removeAllViews();
				m_vgToolbarLayout.addView((Configuration.ORIENTATION_PORTRAIT == miOrientation)?mVgToolbarPortrait:mVgToolbarLandscape, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				syncToolbarLayoutItmProperty();
			}
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
			if(!ASSIGN_ST_PATH && null != txtCamName){
				syncViewProprety(mTxtCamName, txtCamName);
				mTxtCamName = txtCamName;
				mTxtCamName.setOnClickListener(act);
			}
			
			ImageView ivStreamType = (ImageView)vgReference.findViewById(R.id.iv_streaming_type);
			if(null != ivStreamType){
				syncViewProprety(mIvStreamType, ivStreamType);
				mIvStreamType = ivStreamType;
				mIvStreamType.setOnClickListener(act);
			}
			
			ImageButton ibSetting = (ImageButton)vgReference.findViewById(R.id.ib_settings);
			if(null != ibSetting){
				syncViewProprety(mIbSetting, ibSetting);
				mIbSetting = ibSetting;
				mIbSetting.setOnClickListener(act);
			}
		}
	}
	
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
				mIbTalk.setOnClickListener(act);
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
	
	public ImageButton getTalkView(){
		return mIbTalk;
	}
	
	public ImageButton getPlayPauseView(){
		return mIbPlayPause;
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
	
	public CameraViewControlAnimator(Context context, RelativeLayout headerLayout, RelativeLayout toolbarLayout, int iStatusBarHeight) {
		m_vgHeaderLayout = headerLayout;
		m_vgToolbarLayout = toolbarLayout;
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
					RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, miStatusBarHeight);
					title.setLayoutParams(params);
				}
			}
			
			mVgToolbarPortrait = (ViewGroup)inflater.inflate(R.layout.layout_camera_view_footer_live, null);
			
			mVgNavbarLandscape = (ViewGroup)inflater.inflate(R.layout.layout_camera_view_navbar_land, null);
			
			mVgToolbarLandscape = (ViewGroup)inflater.inflate(R.layout.layout_camera_view_footer_live_land, null);
		}
		initAnimations(context);
	}

	private void initAnimations(Context context){
		if(null == s_aniHeaderFadeIn){
			s_aniHeaderFadeIn = AnimationUtils.loadAnimation(context, R.anim.header_enter);
		}
		
		if(null == s_aniHeaderFadeOut){
			s_aniHeaderFadeOut = AnimationUtils.loadAnimation(context, R.anim.header_exit);
		}
		
		if(null == s_aniToolbarFadeIn){
			s_aniToolbarFadeIn = AnimationUtils.loadAnimation(context, R.anim.footer_enter);
		}
		
		if(null == s_aniToolbarFadeOut){
			s_aniToolbarFadeOut = AnimationUtils.loadAnimation(context, R.anim.footer_exit);
		}
		
		registerAnimationListeners();
	}
	
	
	private void registerAnimationListeners(){
		if(null != s_aniHeaderFadeIn)
			s_aniHeaderFadeIn.setAnimationListener(mHeaderFadeInListener);
		
		if(null != s_aniHeaderFadeOut)
			s_aniHeaderFadeOut.setAnimationListener(mHeaderFadeOutListener);
		
		if(null != s_aniToolbarFadeIn)
			s_aniToolbarFadeIn.setAnimationListener(mToolbarFadeInListener);
		
		if(null != s_aniToolbarFadeOut)
			s_aniToolbarFadeOut.setAnimationListener(mToolbarFadeOutListener);
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
					animation = s_aniHeaderFadeOut;
					m_vgHeaderLayout.startAnimation(animation);
					
					if(null != act){
						act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
						act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
					}
					
				}else{
					//animation = s_aniHeaderFadeIn;
					m_vgHeaderLayout.bringToFront();
					m_vgHeaderLayout.setVisibility(View.VISIBLE);
					startHideControlRunnable();
					
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
			
			if(null != m_vgToolbarLayout){
				Animation animation = null;
				if(View.VISIBLE == m_vgToolbarLayout.getVisibility()){
					animation = s_aniToolbarFadeOut;
					m_vgToolbarLayout.startAnimation(animation);
				}else if(!mbP2PMode){
					//animation = s_aniToolbarFadeIn;
					m_vgToolbarLayout.bringToFront();
					m_vgToolbarLayout.setVisibility(View.VISIBLE);
				}
			}
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
		setControlVisibility(View.GONE);
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
			Log.d(TAG, "mHeaderFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_vgHeaderLayout.bringToFront();
			m_bInHeaderAnimation = true;
			Log.d(TAG, "mHeaderFadeInListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mHeaderFadeOutListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgHeaderLayout){
				m_vgHeaderLayout.setVisibility(View.INVISIBLE);
			}
			
			m_bInHeaderAnimation = false;
			Log.d(TAG, "mHeaderFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_bInHeaderAnimation = true;
			Log.d(TAG, "mHeaderFadeOutListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mToolbarFadeInListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgToolbarLayout){
				m_vgToolbarLayout.setVisibility(mbP2PMode?View.INVISIBLE:View.VISIBLE);
			}
			m_bInToolbarAnimation = false;
			Log.d(TAG, "mToolbarFadeInListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			if(!mbP2PMode)
				m_vgToolbarLayout.bringToFront();
			m_bInToolbarAnimation = true;
			Log.d(TAG, "mToolbarFadeInListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mToolbarFadeOutListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgToolbarLayout){
				m_vgToolbarLayout.setVisibility(View.INVISIBLE);
			}
			
			m_bInToolbarAnimation = false;
			Log.d(TAG, "mToolbarFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_bInToolbarAnimation = true;
			Log.d(TAG, "mToolbarFadeOutListener::onAnimationStart()");
		}
	};
}
