package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.TAG;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import com.app.beseye.R;

public class CameraViewControlAnimator {
	
	static private final long TIME_TO_HIDE_HEADER  = 7000L;
	
	static private Animation s_aniHeaderFadeIn = null;
	static private Animation s_aniHeaderFadeOut = null;
	
	static private Animation s_aniToolbarFadeIn = null;
	static private Animation s_aniToolbarFadeOut = null;
	
	private RelativeLayout m_vgHeaderLayout = null;
	private RelativeLayout m_vgToolbarLayout = null;
	
	private boolean m_bInHeaderAnimation = false;
	private boolean m_bInToolbarAnimation = false;
	
	private Handler mHandler = new Handler();
	private Runnable mHideHeaderRunnable = new Runnable(){
		@Override
		public void run() {
			if(View.VISIBLE == getVisibility())
				performControlAnimation();
		}};
	
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
	
	public CameraViewControlAnimator(Context context, RelativeLayout headerLayout, RelativeLayout toolbarLayout) {
		m_vgHeaderLayout = headerLayout;
		m_vgToolbarLayout = toolbarLayout;
		initViews(context);
	}

	private void initViews(Context context){
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
		if(null != mHandler && null != mHideHeaderRunnable){
			mHandler.removeCallbacks(mHideHeaderRunnable);
			mHandler.postDelayed(mHideHeaderRunnable, TIME_TO_HIDE_HEADER);
		}
	}
	
	public void cancelHideControl(){
		if(null != mHandler && null != mHideHeaderRunnable){
			mHandler.removeCallbacks(mHideHeaderRunnable);
		}
	}
	
	public void performControlAnimation(){
		if(false == isInAnimation()){
			if(null != m_vgHeaderLayout){
				Animation animation = null;
				if(View.VISIBLE == m_vgHeaderLayout.getVisibility()){
					animation = s_aniHeaderFadeOut;
					m_vgHeaderLayout.startAnimation(animation);
				}else{
					//animation = s_aniHeaderFadeIn;
					m_vgHeaderLayout.bringToFront();
					m_vgHeaderLayout.setVisibility(View.VISIBLE);
					startHideControlRunnable();
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
		if(null != m_vgHeaderLayout)
			m_vgHeaderLayout.setVisibility(visibility);
		
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
}
