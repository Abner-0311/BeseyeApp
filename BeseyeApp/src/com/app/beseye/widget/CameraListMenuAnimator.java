package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.DEBUG;
import static com.app.beseye.util.BeseyeConfig.TAG;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.beseye.BeseyeNewsActivity;
import com.app.beseye.CameraListActivity;
import com.app.beseye.R;
import com.app.beseye.util.BeseyeUtils;

public class CameraListMenuAnimator {	
	private Animation m_aniMenuFadeIn = null;
	private Animation m_aniMenuFadeOut = null;
	
	private Animation m_aniMenuBgFadeIn = null;
	private Animation m_aniMenuBgFadeOut = null;
	
	private ViewGroup m_vgMenuHolderLayout = null;
	private View m_vMenuHolderBg = null;
	private ViewGroup m_vgMenu = null;
	
	private WeakReference<CameraListActivity> mCameraListActivity;
	
	private boolean m_bInMenuAnimation = false;
	private boolean m_bInMenuBgAnimation = false;
	
	public CameraListMenuAnimator(Context context, ViewGroup menuLayout) {
		m_vgMenuHolderLayout = menuLayout;
		mCameraListActivity = new WeakReference<CameraListActivity>((CameraListActivity)context);
		initViews(context);
	}

	private void initViews(Context context){
		if(null != m_vgMenuHolderLayout){
			m_vMenuHolderBg = m_vgMenuHolderLayout.findViewById(R.id.vg_cam_menu_container_bg);
			if(null != m_vMenuHolderBg){
				m_vMenuHolderBg.setVisibility(View.INVISIBLE);
			}
			
			m_vgMenu = (ViewGroup)m_vgMenuHolderLayout.findViewById(R.id.vg_cam_menu_container);
			if(null != m_vgMenu){
				m_vgMenu.setOnClickListener(mCameraListActivity.get());
				m_vgMenu.setVisibility(View.INVISIBLE);
			}
			
			m_vgMenuHolderLayout.setOnTouchListener(new OnTouchListener(){
				Rect rect = new Rect();
				@Override
				public boolean onTouch(View view, MotionEvent event) {
					m_vgMenu.getHitRect(rect);
					if(event.getAction() == MotionEvent.ACTION_DOWN && false == rect.contains((int)event.getX(), (int)event.getY())){
						performMenuAnimation();
						return true;
					}
					return false;
				}});
		}
		setupMenu(R.id.vg_my_cam, R.drawable.sl_menu_my_cam_icon, R.string.cam_menu_my_cam);
		setupMenu(R.id.vg_demo_cam, R.drawable.sl_menu_demo_cam_icon, R.string.cam_menu_demo_cam);
		setupMenu(R.id.vg_private_cam, R.drawable.sl_menu_demo_cam_icon, R.string.cam_menu_private_demo_cam);
		setupMenu(R.id.vg_news, R.drawable.sl_menu_news_icon, R.string.cam_menu_news);
		setupMenu(R.id.vg_about, R.drawable.sl_menu_about_icon, R.string.cam_menu_about);
		setupMenu(R.id.vg_support, R.drawable.sl_menu_support_icon, R.string.cam_menu_support);
		setupMenu(R.id.vg_logout, R.drawable.sl_menu_logout_icon, R.string.cam_menu_logout);
		
		View vSupport = m_vgMenuHolderLayout.findViewById(R.id.vg_support);
		BeseyeUtils.setVisibility(vSupport, View.GONE);
		
		checkNewsStatus();
		
		initAnimations(context);
	}

	public void checkNewsStatus(){
		if(null != m_vgMenuHolderLayout){
			View vMyCam = m_vgMenuHolderLayout.findViewById(R.id.vg_news);
			if(null != vMyCam){
				ImageView imgIndicator = (ImageView)vMyCam.findViewById(R.id.iv_menu_indicator);
				if(null != imgIndicator){
					imgIndicator.setVisibility(BeseyeNewsActivity.BeseyeNewsHistoryMgr.haveLatestNews()?View.VISIBLE:View.GONE);
				}
			}
		}
	}
	
	public void showPrivateCam(){
		if(null != m_vgMenuHolderLayout){
			BeseyeUtils.setVisibility(m_vgMenuHolderLayout.findViewById(R.id.vg_private_cam), View.VISIBLE);
		}
	}
	
	public boolean isPrivateCamShow(){
		boolean bShow = false;
		if(null != m_vgMenuHolderLayout){
			View vPirvateCam = m_vgMenuHolderLayout.findViewById(R.id.vg_private_cam);
			bShow = (null != vPirvateCam) && View.VISIBLE==vPirvateCam.getVisibility();
		}
		
		return bShow;
	}

	private void initAnimations(Context context){
		if(null == m_aniMenuFadeIn){
			m_aniMenuFadeIn = AnimationUtils.loadAnimation(context, R.anim.menu_enter_anim);
		}
		
		if(null == m_aniMenuFadeOut){
			m_aniMenuFadeOut = AnimationUtils.loadAnimation(context, R.anim.menu_exit_anim);
		}
		
		if(null == m_aniMenuBgFadeIn){
			m_aniMenuBgFadeIn = AnimationUtils.loadAnimation(context, R.anim.menu_bg_enter_anim);
		}
		
		if(null == m_aniMenuBgFadeOut){
			m_aniMenuBgFadeOut = AnimationUtils.loadAnimation(context, R.anim.menu_bg_exit_anim);
		}
		
		registerAnimationListeners();
	}
	
	
	private void registerAnimationListeners(){
		if(null != m_aniMenuFadeIn){
			m_aniMenuFadeIn.setAnimationListener(mMenuFadeInListener);
		}
		
		if(null != m_aniMenuFadeOut){
			m_aniMenuFadeOut.setAnimationListener(mMenuFadeOutListener);
		}
		
		if(null != m_aniMenuBgFadeIn){
			m_aniMenuBgFadeIn.setAnimationListener(mMenuBgFadeInListener);
		}
		
		if(null != m_aniMenuBgFadeOut){
			m_aniMenuBgFadeOut.setAnimationListener(mMenuBgFadeOutListener);
		}
	}
	
	public int getVisibility(){
		return (null != m_vgMenuHolderLayout)?m_vgMenuHolderLayout.getVisibility():View.GONE;
	}
	
	public boolean isInAnimation(){
		return m_bInMenuAnimation || m_bInMenuBgAnimation;
	}
	
	private void setupMenu(int iVgMenuId, int iIconId, int iMenuTopic){
		if(null != m_vgMenuHolderLayout){
			View vMyCam = m_vgMenuHolderLayout.findViewById(iVgMenuId);
			if(null != vMyCam){
				ImageView imgMyCam = (ImageView)vMyCam.findViewById(R.id.iv_menu_icon);
				if(null != imgMyCam){
					imgMyCam.setImageResource(iIconId);
				}
				
				TextView txtTopic = (TextView)vMyCam.findViewById(R.id.txt_menu_title);
				if(null != txtTopic){
					txtTopic.setText(iMenuTopic);
				}
				vMyCam.setOnClickListener(mCameraListActivity.get());
			}
		}
	}
	
	public void performMenuAnimation(){
		if(false == isInAnimation()){
			if(null != m_vgMenu){
				Animation animation = null;
				Animation animationBg = null;
				//CameraListActivity act = mCameraListActivity.get();
				if(DEBUG)
					Log.d(TAG, "performMenuAnimation(), m_vgMenu.getVisibility():"+m_vgMenu.getVisibility());
				if(View.VISIBLE == m_vgMenu.getVisibility()){
					animation = m_aniMenuFadeOut;
					animationBg = m_aniMenuBgFadeOut;
				}else{
					if(null != m_vgMenuHolderLayout){
						m_vgMenuHolderLayout.setVisibility(View.VISIBLE);
					}
					animation = m_aniMenuFadeIn;
					animationBg = m_aniMenuBgFadeIn;
				}
				m_vgMenu.startAnimation(animation);
				m_vMenuHolderBg.startAnimation(animationBg);
			}
		}
	}
	
	private void cancelAnimation(){
		if(isInAnimation()){
			if(null != m_vgMenu){
				Animation animation = m_vgMenu.getAnimation();
				if(null != animation)
					animation.cancel();
			}
		}
	}
	
//	private void setMenuVisibility(int visibility){
//		//cancelAnimation();
//		performMenuAnimation();
//	}
	
//	public void showMenu(){
//		setMenuVisibility(View.VISIBLE);
//	}
//	
//	public void hideMenu(){
//		setMenuVisibility(View.GONE);
//	}
	
	private AnimationListener mMenuFadeInListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgMenu){
				m_vgMenu.setVisibility(View.VISIBLE);
				m_vgMenu.bringToFront();
				m_vgMenu.setEnabled(true);
			}
			m_bInMenuAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mMenuFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			if(null != m_vgMenu){
				m_vgMenu.setVisibility(View.INVISIBLE);
			}
			m_bInMenuAnimation = true;
			
			if(DEBUG)
				Log.d(TAG, "mMenuFadeInListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mMenuFadeOutListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vgMenu){
				m_vgMenu.setVisibility(View.INVISIBLE);
				m_vgMenu.setEnabled(false);
			}
			
			if(null != m_vgMenuHolderLayout){
				m_vgMenuHolderLayout.setVisibility(View.INVISIBLE);
			}
			
			m_bInMenuAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mMenuFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_bInMenuAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mMenuFadeOutListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mMenuBgFadeInListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vMenuHolderBg){
				m_vMenuHolderBg.setVisibility(View.VISIBLE);
				m_vMenuHolderBg.setEnabled(true);
			}
			m_bInMenuBgAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mMenuBgFadeInListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			if(null != m_vMenuHolderBg){
				m_vMenuHolderBg.setVisibility(View.INVISIBLE);
			}
			m_bInMenuBgAnimation = true;
			
			if(DEBUG)
				Log.d(TAG, "mMenuBgFadeInListener::onAnimationStart()");
		}
	};
	
	private AnimationListener mMenuBgFadeOutListener = new AnimationListener(){
		public void onAnimationEnd(Animation animation) {
			if(null != m_vMenuHolderBg){
				m_vMenuHolderBg.setVisibility(View.INVISIBLE);
				m_vMenuHolderBg.setEnabled(false);
			}
			
			m_bInMenuBgAnimation = false;
			if(DEBUG)
				Log.d(TAG, "mMenuBgFadeOutListener::onAnimationEnd()");
		}

		public void onAnimationRepeat(Animation animation) {
		}

		public void onAnimationStart(Animation animation) {
			m_bInMenuBgAnimation = true;
			if(DEBUG)
				Log.d(TAG, "mMenuBgFadeOutListener::onAnimationStart()");
		}
	};
}
