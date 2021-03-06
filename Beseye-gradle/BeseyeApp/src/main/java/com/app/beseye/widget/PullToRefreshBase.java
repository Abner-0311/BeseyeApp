/*******************************************************************************
 * Copyright 2011, 2012 Chris Banes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.TAG;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import com.app.beseye.widget.internal.BeseyeRefreshLayout;
import com.app.beseye.widget.internal.LoadingLayout;


public abstract class PullToRefreshBase<T extends View> extends LinearLayout {

	// ===========================================================
	// Constants
	// ===========================================================

	static final boolean DEBUG = false;

	static final String LOG_TAG = "PullToRefresh";

	static final float FRICTION = 1.8f;

	static final int PULL_TO_REFRESH = 0x0;
	static final int RELEASE_TO_REFRESH = 0x1;
	static final int REFRESHING = 0x2;
	static final int MANUAL_REFRESHING = 0x3;

	static final LvExtendedMode DEFAULT_MODE = LvExtendedMode.NONE;

	static final String STATE_STATE = "ptr_state";
	static final String STATE_MODE = "ptr_mode";
	static final String STATE_CURRENT_MODE = "ptr_current_mode";
	static final String STATE_DISABLE_SCROLLING_REFRESHING = "ptr_disable_scrolling";
	static final String STATE_SHOW_REFRESHING_VIEW = "ptr_show_refreshing_view";
	static final String STATE_SUPER = "ptr_super";

	// ===========================================================
	// Fields
	// ===========================================================

	private int mTouchSlop;
	private float mLastMotionX;
	private float mLastMotionY;
	private float mInitialMotionY;

	private boolean mIsBeingDragged = false;
	private int mState = PULL_TO_REFRESH;
	private LvExtendedMode mMode = DEFAULT_MODE;

	private LvExtendedMode mCurrentMode = LvExtendedMode.PULL_DOWN_TO_REFRESH;
	T mRefreshableView;
	private boolean mPullToRefreshEnabled = true;

	private boolean mShowViewWhileRefreshing = true;
	private boolean mDisableScrollingWhileRefreshing = true;
	private boolean mFilterTouchEvents = true;
	private LoadingLayout mHeaderLayout;
	private LoadingLayout mFooterLayout;

	private int mHeaderHeight;
	private final Handler mHandler = new Handler();

	private OnRefreshListener mOnRefreshListener;
	private OnRefreshListener2 mOnRefreshListener2;

	private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
	
	private OnLvHeaderListener mOnLvHeaderListener;
	private boolean mbOnLvHeaderShowTriggerred = false;

	// ===========================================================
	// Constructors
	// ===========================================================

	public PullToRefreshBase(Context context) {
		super(context);
		init(context, null);
	}

	public PullToRefreshBase(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	public PullToRefreshBase(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs);
	}

	public PullToRefreshBase(Context context, LvExtendedMode mode) {
		super(context);
		mMode = mode;
		init(context, null);
	}

	/**
	 * Get the mode that this view is currently in. This is only really useful
	 * when using <code>Mode.BOTH</code>.
	 * 
	 * @return Mode that the view is currently in
	 */
	public final LvExtendedMode getCurrentMode() {
		return mCurrentMode;
	}

	/**
	 * Returns whether the Touch Events are filtered or not. If true is
	 * returned, then the View will only use touch events where the difference
	 * in the Y-axis is greater than the difference in the X-axis. This means
	 * that the View will not interfere when it is used in a horizontal
	 * scrolling View (such as a ViewPager).
	 * 
	 * @return boolean - true if the View is filtering Touch Events
	 */
	public final boolean getFilterTouchEvents() {
		return mFilterTouchEvents;
	}

	/**
	 * Get the mode that this view has been set to. If this returns
	 * <code>Mode.BOTH</code>, you can use <code>getCurrentMode()</code> to
	 * check which mode the view is currently in
	 * 
	 * @return Mode that the view has been set to
	 */
	public final LvExtendedMode getMode() {
		return mMode;
	}

	/**
	 * Get the Wrapped Refreshable View. Anything returned here has already been
	 * added to the content view.
	 * 
	 * @return The View which is currently wrapped
	 */
	public final T getRefreshableView() {
		return mRefreshableView;
	}

	/**
	 * Get whether the 'Refreshing' View should be automatically shown when
	 * refreshing. Returns true by default.
	 * 
	 * @return - true if the Refreshing View will be show
	 */
	public final boolean getShowViewWhileRefreshing() {
		return mShowViewWhileRefreshing;
	}

	/**
	 * @deprecated Use the value from <code>getCurrentMode()</code> instead
	 * @return true if the current mode is Mode.PULL_DOWN_TO_REFRESH
	 */
	public final boolean hasPullFromTop() {
		return mCurrentMode == LvExtendedMode.PULL_DOWN_TO_REFRESH;
	}

	/**
	 * Returns whether the widget has disabled scrolling on the Refreshable View
	 * while refreshing.
	 * 
	 * @return true if the widget has disabled scrolling while refreshing
	 */
	public final boolean isDisableScrollingWhileRefreshing() {
		return mDisableScrollingWhileRefreshing;
	}

	/**
	 * Whether Pull-to-Refresh is enabled
	 * 
	 * @return enabled
	 */
	public final boolean isPullToRefreshEnabled() {
		return mPullToRefreshEnabled;
	}

	/**
	 * Returns whether the Widget is currently in the Refreshing mState
	 * 
	 * @return true if the Widget is currently refreshing
	 */
	public final boolean isRefreshing() {
		return mState == REFRESHING || mState == MANUAL_REFRESHING;
	}
	
	public static enum LvPullDirection {
		NONE,
		PULL_DOWN,
		PULL_UP
	}
	
	private LvPullDirection mLvPullMode = LvPullDirection.NONE;
	
	public LvPullDirection getPullDirection(){
		return mLvPullMode;
	}

	@Override
	public final boolean onInterceptTouchEvent(MotionEvent event) {

		if (!mPullToRefreshEnabled) {
			return false;
		}

		if (isRefreshing() && mDisableScrollingWhileRefreshing) {
			return true;
		}

		final int action = event.getAction();

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
			setIsBeingDragged(false);
			//checkLvHeaderCbState();
			return false;
		}

		if (action != MotionEvent.ACTION_DOWN && mIsBeingDragged) {
			return true;
		}

		switch (action) {
			case MotionEvent.ACTION_MOVE: {
				if (isReadyForPull() || isRefreshing()) {

					final float y = event.getY();
					final float dy = y - mLastMotionY;
					final float yDiff = Math.abs(dy);
					final float xDiff = Math.abs(event.getX() - mLastMotionX);

					if (yDiff > mTouchSlop && (!mFilterTouchEvents || yDiff > xDiff)) {
						if(isRefreshing()){
							if((dy >= 1f && LvExtendedMode.PULL_UP_TO_REFRESH == mCurrentMode)||
							   (dy <= -1f && LvExtendedMode.PULL_DOWN_TO_REFRESH == mCurrentMode))
							if(null != mOnRefreshListener)
								mOnRefreshListener.onRefreshCancel();
							resetHeader();
						}
						else{
							if (/*mMode.canPullDownUpdate() &&*/ dy >= 1f && isReadyForPullDown()) {
								mLvPullMode = LvPullDirection.PULL_DOWN;
								mLastMotionY = y;
								setIsBeingDragged(true);
								if (mMode == LvExtendedMode.BOTH || mMode == LvExtendedMode.NONE) {
									mCurrentMode = LvExtendedMode.PULL_DOWN_TO_REFRESH;
								}
							} else if (/*mMode.canPullUpLoadMore() &&*/ dy <= -1f && isReadyForPullUp()) {
								mLvPullMode = LvPullDirection.PULL_UP;
								mLastMotionY = y;
								setIsBeingDragged(true);
								if (mMode == LvExtendedMode.BOTH || mMode == LvExtendedMode.NONE) {
									mCurrentMode = LvExtendedMode.PULL_UP_TO_REFRESH;
								}
							}
						}
					}
				}
				break;
			}
			case MotionEvent.ACTION_DOWN: {
				if (isReadyForPull()) {
					mLastMotionY = mInitialMotionY = event.getY();
					mLastMotionX = event.getX();
					setIsBeingDragged(false);
					//checkLvHeaderCbState();
				}
				break;
			}
		}

		return mIsBeingDragged;
	}
	
	//Change it due to fake view disappear
	public void changeInitialMotionY(int iDelta){
		//Log.i(TAG, "changeInitialMotionY(), iDelta = "+iDelta);
		mLastMotionY = mInitialMotionY -= iDelta;
		mbNeedIgnoreONce = true;
	}
	
	private void setIsBeingDragged(boolean bIsBeingDragged){
		//Log.d(TAG, "setIsBeingDragged(), bIsBeingDragged :"+bIsBeingDragged);
		if(LvPullDirection.PULL_DOWN == mLvPullMode){
			if(false == mIsBeingDragged && true == bIsBeingDragged){
				if(null != mOnLvHeaderListener){
					mOnLvHeaderListener.onLvHeaderShow();
					mbOnLvHeaderShowTriggerred = true;
				}
			}
		}
		
		mIsBeingDragged = bIsBeingDragged;
	}
	
	private void checkLvHeaderCbState(){
		if(DEBUG)
			Log.d(TAG, "checkLvHeaderCbState()");
		
		if(true == mbOnLvHeaderShowTriggerred){
			if(null != mOnLvHeaderListener)
				mOnLvHeaderListener.onLvHeaderHide();
		}
		
		mbOnLvHeaderShowTriggerred = false;
	}
	

	/**
	 * Mark the current Refresh as complete. Will Reset the UI and hide the
	 * Refreshing View
	 */
	public final void onRefreshComplete() {
		if (mState != PULL_TO_REFRESH) {
			resetHeader();
		}
	}
	//Workaround for switch first lock view
	private boolean mbNeedIgnoreONce = false;
	
	@Override
	public final boolean onTouchEvent(MotionEvent event) {
		if (!mPullToRefreshEnabled) {
			return false;
		}

		if (isRefreshing() && mDisableScrollingWhileRefreshing) {
			return true;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN && event.getEdgeFlags() != 0) {
			return false;
		}

		switch (event.getAction()) {

			case MotionEvent.ACTION_MOVE: {
				if (mIsBeingDragged) {
					mLastMotionY = event.getY();
					if(false == mbNeedIgnoreONce)
						pullEvent();
					else 
						mbNeedIgnoreONce = false;
					return true;
				}
				break;
			}

			case MotionEvent.ACTION_DOWN: {
				 mInitialMotionY = event.getY();
				if (isReadyForPull()) {
					mLastMotionY = mInitialMotionY = event.getY();
					return true;
				}
				break;
			}

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP: {
				if (mIsBeingDragged) {
					setIsBeingDragged(false);

					if (mState == RELEASE_TO_REFRESH) {

						if (null != mOnRefreshListener) {
							if((mMode != LvExtendedMode.NONE) && 
							  !(mMode == LvExtendedMode.PULL_DOWN_TO_REFRESH && mLvPullMode == LvPullDirection.PULL_UP) && 
							  !(mMode == LvExtendedMode.PULL_UP_TO_REFRESH && mLvPullMode == LvPullDirection.PULL_DOWN))
							{
								setRefreshingInternal(true);
								mOnRefreshListener.onRefresh();
								return true;
							}
						} else if (null != mOnRefreshListener2) {
							setRefreshingInternal(true);
							if (mCurrentMode == LvExtendedMode.PULL_DOWN_TO_REFRESH) {
								mOnRefreshListener2.onPullDownToRefresh();
							} else if (mCurrentMode == LvExtendedMode.PULL_UP_TO_REFRESH) {
								mOnRefreshListener2.onPullUpToRefresh();
							}
							return true;
						}

						//return true;
					}

					smoothScrollTo(0);
					return true;
				}
				break;
			}
		}

		return false;
	}

	/**
	 * By default the Widget disabled scrolling on the Refreshable View while
	 * refreshing. This method can change this behaviour.
	 * 
	 * @param disableScrollingWhileRefreshing
	 *            - true if you want to disable scrolling while refreshing
	 */
	public final void setDisableScrollingWhileRefreshing(boolean disableScrollingWhileRefreshing) {
		mDisableScrollingWhileRefreshing = disableScrollingWhileRefreshing;
	}

	/**
	 * Set the Touch Events to be filtered or not. If set to true, then the View
	 * will only use touch events where the difference in the Y-axis is greater
	 * than the difference in the X-axis. This means that the View will not
	 * interfere when it is used in a horizontal scrolling View (such as a
	 * ViewPager), but will restrict which types of finger scrolls will trigger
	 * the View.
	 * 
	 * @param filterEvents
	 *            - true if you want to filter Touch Events. Default is true.
	 */
	public final void setFilterTouchEvents(boolean filterEvents) {
		mFilterTouchEvents = filterEvents;
	}

	/**
	 * Set the Last Updated Text. This displayed under the main label when
	 * Pulling
	 * 
	 * @param label
	 *            - Label to set
	 */
	public void setLastUpdatedLabel(CharSequence label) {
		if (null != mHeaderLayout) {
			mHeaderLayout.setSubHeaderText(label);
		}
		if (null != mFooterLayout) {
			mFooterLayout.setSubHeaderText(label);
		}

		// Refresh Height as it may have changed
		refreshLoadingViewsHeight();
	}

	/**
	 * Set the drawable used in the loading layout. This is the same as calling
	 * <code>setLoadingDrawable(drawable, Mode.BOTH)</code>
	 * 
	 * @param drawable
	 *            - Drawable to display
	 */
	public void setLoadingDrawable(Drawable drawable) {
		setLoadingDrawable(drawable, LvExtendedMode.BOTH);
	}

	/**
	 * Set the drawable used in the loading layout.
	 * 
	 * @param drawable
	 *            - Drawable to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setLoadingDrawable(Drawable drawable, LvExtendedMode mode) {
		if (null != mHeaderLayout && mode.canPullDownUpdate()) {
			mHeaderLayout.setLoadingDrawable(drawable);
		}
		if (null != mFooterLayout && mode.canPullUpLoadMore()) {
			mFooterLayout.setLoadingDrawable(drawable);
		}

		// The Loading Height may have changed, so refresh
		refreshLoadingViewsHeight();
	}

	@Override
	public void setLongClickable(boolean longClickable) {
		getRefreshableView().setLongClickable(longClickable);
	}

	/**
	 * Set the mode of Pull-to-Refresh that this view will use.
	 * 
	 * @param mode
	 *            - Mode to set the View to
	 */
	public void setMode(LvExtendedMode mode) {
		if (mode != mMode) {
			if (DEBUG) {
				Log.d(LOG_TAG, "Setting mode to: " + mode);
			}
			mMode = mode;
			updateUIForMode();
		}
	}

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public final void setOnRefreshListener(OnRefreshListener listener) {
		mOnRefreshListener = listener;
	}

	/**
	 * Set OnRefreshListener for the Widget
	 * 
	 * @param listener
	 *            - Listener to be used when the Widget is set to Refresh
	 */
	public final void setOnRefreshListener(OnRefreshListener2 listener) {
		mOnRefreshListener2 = listener;
	}
	
	public final void setOnLvHeaderListener(OnLvHeaderListener listener){
		mOnLvHeaderListener = listener;
	}

	/**
	 * Set Text to show when the Widget is being Pulled
	 * <code>setPullLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public void setPullLabel(String pullLabel) {
		setPullLabel(pullLabel, LvExtendedMode.BOTH);
	}

	/**
	 * Set Text to show when the Widget is being Pulled
	 * 
	 * @param pullLabel
	 *            - String to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setPullLabel(String pullLabel, LvExtendedMode mode) {
		if (null != mHeaderLayout && mode.canPullDownUpdate()) {
			mHeaderLayout.setPullLabel(pullLabel);
		}
		if (null != mFooterLayout && mode.canPullUpLoadMore()) {
			mFooterLayout.setPullLabel(pullLabel);
		}
	}

	/**
	 * A mutator to enable/disable Pull-to-Refresh for the current View
	 * 
	 * @param enable
	 *            Whether Pull-To-Refresh should be used
	 */
	public final void setPullToRefreshEnabled(boolean enable) {
		mPullToRefreshEnabled = enable;
	}

	public final void setRefreshing() {
		setRefreshing(true);
	}

	/**
	 * Sets the Widget to be in the refresh mState. The UI will be updated to
	 * show the 'Refreshing' view.
	 * 
	 * @param doScroll
	 *            - true if you want to force a scroll to the Refreshing view.
	 */
	public final void setRefreshing(boolean doScroll) {
		if (!isRefreshing()) {
			setRefreshingInternal(doScroll);
			mState = MANUAL_REFRESHING;
		}
	}

	/**
	 * Set Text to show when the Widget is refreshing
	 * <code>setRefreshingLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public void setRefreshingLabel(String refreshingLabel) {
		setRefreshingLabel(refreshingLabel, LvExtendedMode.BOTH);
	}

	/**
	 * Set Text to show when the Widget is refreshing
	 * 
	 * @param refreshingLabel
	 *            - String to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setRefreshingLabel(String refreshingLabel, LvExtendedMode mode) {
		if (null != mHeaderLayout && mode.canPullDownUpdate()) {
			mHeaderLayout.setRefreshingLabel(refreshingLabel);
		}
		if (null != mFooterLayout && mode.canPullUpLoadMore()) {
			mFooterLayout.setRefreshingLabel(refreshingLabel);
		}
	}
	
	public void updateLatestTimestamp(){
		if(mHeaderLayout instanceof LoadingLayout){
			((LoadingLayout)mHeaderLayout).setLatestUpdate();
		}
		
		if(mFooterLayout instanceof LoadingLayout){
			((LoadingLayout)mFooterLayout).setLatestUpdate();
		}
	}

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released. This is the same as calling
	 * <code>setReleaseLabel(releaseLabel, Mode.BOTH)</code>
	 * 
	 * @param releaseLabel
	 *            - String to display
	 */
	public void setReleaseLabel(String releaseLabel) {
		setReleaseLabel(releaseLabel, LvExtendedMode.BOTH);
	}

	/**
	 * Set Text to show when the Widget is being pulled, and will refresh when
	 * released
	 * 
	 * @param releaseLabel
	 *            - String to display
	 * @param mode
	 *            - Controls which Header/Footer Views will be updated.
	 *            <code>Mode.BOTH</code> will update all available, other values
	 *            will update the relevant View.
	 */
	public void setReleaseLabel(String releaseLabel, LvExtendedMode mode) {
		if (null != mHeaderLayout && mode.canPullDownUpdate()) {
			mHeaderLayout.setReleaseLabel(releaseLabel);
		}
		if (null != mFooterLayout && mode.canPullUpLoadMore()) {
			mFooterLayout.setReleaseLabel(releaseLabel);
		}
	}

	/**
	 * A mutator to enable/disable whether the 'Refreshing' View should be
	 * automatically shown when refreshing.
	 * 
	 * @param showView
	 */
	public final void setShowViewWhileRefreshing(boolean showView) {
		mShowViewWhileRefreshing = showView;
	}

	protected void addRefreshableView(Context context, T refreshableView) {
		addView(refreshableView, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f));
	}

	/**
	 * This is implemented by derived classes to return the created View. If you
	 * need to use a custom View (such as a custom ListView), override this
	 * method and return an instance of your custom class.
	 * 
	 * Be sure to set the ID of the view in this method, especially if you're
	 * using a ListActivity or ListFragment.
	 * 
	 * @param context
	 *            Context to create view with
	 * @param attrs
	 *            AttributeSet from wrapped class. Means that anything you
	 *            include in the XML layout declaration will be routed to the
	 *            created View
	 * @return New instance of the Refreshable View
	 */
	protected abstract T createRefreshableView(Context context, AttributeSet attrs);

	protected final LoadingLayout getFooterLayout() {
		return mFooterLayout;
	}

	protected final int getHeaderHeight() {
		return mHeaderHeight;
	}

	protected final LoadingLayout getHeaderLayout() {
		return mHeaderLayout;
	}

	protected final int getState() {
		return mState;
	}

	/**
	 * Allows Derivative classes to handle the XML Attrs without creating a
	 * TypedArray themsevles
	 * 
	 * @param a
	 *            - TypedArray of PullToRefresh Attributes
	 */
	protected void handleStyledAttributes(TypedArray a) {
	}

	/**
	 * Implemented by derived class to return whether the View is in a mState
	 * where the user can Pull to Refresh by scrolling down.
	 * 
	 * @return true if the View is currently the correct mState (for example,
	 *         top of a ListView)
	 */
	protected abstract boolean isReadyForPullDown();

	/**
	 * Implemented by derived class to return whether the View is in a mState
	 * where the user can Pull to Refresh by scrolling up.
	 * 
	 * @return true if the View is currently in the correct mState (for example,
	 *         bottom of a ListView)
	 */
	protected abstract boolean isReadyForPullUp();

	/**
	 * Called when the UI needs to be updated to the 'Pull to Refresh' state
	 */
	protected void onPullToRefresh() {
		switch (mCurrentMode) {
			case PULL_UP_TO_REFRESH:
				mFooterLayout.pullToRefresh();
				break;
			case PULL_DOWN_TO_REFRESH:
				mHeaderLayout.pullToRefresh();
				break;
			default:
				break;
		}
	}

	/**
	 * Called when the UI needs to be updated to the 'Release to Refresh' state
	 */
	protected void onReleaseToRefresh() {
		switch (mCurrentMode) {
			case PULL_UP_TO_REFRESH:
				mFooterLayout.releaseToRefresh();
				break;
			case PULL_DOWN_TO_REFRESH:
				mHeaderLayout.releaseToRefresh();
				break;
			default:
				break;
		}
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof Bundle) {
			Bundle bundle = (Bundle) state;

			mMode = LvExtendedMode.mapIntToMode(bundle.getInt(STATE_MODE, 0));
			mCurrentMode = LvExtendedMode.mapIntToMode(bundle.getInt(STATE_CURRENT_MODE, 0));

			mDisableScrollingWhileRefreshing = bundle.getBoolean(STATE_DISABLE_SCROLLING_REFRESHING, true);
			mShowViewWhileRefreshing = bundle.getBoolean(STATE_SHOW_REFRESHING_VIEW, true);

			// Let super Restore Itself
			super.onRestoreInstanceState(bundle.getParcelable(STATE_SUPER));

			final int viewState = bundle.getInt(STATE_STATE, PULL_TO_REFRESH);
			if (viewState == REFRESHING) {
				setRefreshingInternal(true);
				mState = viewState;
			}
			return;
		}

		super.onRestoreInstanceState(state);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putInt(STATE_STATE, mState);
		bundle.putInt(STATE_MODE, mMode.getIntValue());
		bundle.putInt(STATE_CURRENT_MODE, mCurrentMode.getIntValue());
		bundle.putBoolean(STATE_DISABLE_SCROLLING_REFRESHING, mDisableScrollingWhileRefreshing);
		bundle.putBoolean(STATE_SHOW_REFRESHING_VIEW, mShowViewWhileRefreshing);
		bundle.putParcelable(STATE_SUPER, super.onSaveInstanceState());
		return bundle;
	}

	// ===========================================================
	// Methods
	// ===========================================================

	protected void resetHeader() {
		mState = PULL_TO_REFRESH;
		setIsBeingDragged(false);

		if (mMode.canPullDownUpdate()) {
			mHeaderLayout.reset();
		}
		if (mMode.canPullUpLoadMore()) {
			mFooterLayout.reset();
		}

		smoothScrollTo(0);
	}

	protected final void setHeaderScroll(int y) {
		scrollTo(0, y);
	}
	
	@Override 
	public void scrollTo(int x, int y){
		super.scrollTo(x, -mHeaderPaddingTop+y);
	} 
	
	private int mHeaderPaddingTop = 0;
	
	protected void setHeaderPaddingTop(int iPadding){
		mHeaderPaddingTop = iPadding;
		scrollTo(getScrollX(), getScrollY());
	}

	protected void setRefreshingInternal(boolean doScroll) {
		mState = REFRESHING;

		if (mMode.canPullDownUpdate()) {
			mHeaderLayout.refreshing();
		}
		if (mMode.canPullUpLoadMore()) {
			mFooterLayout.refreshing();
		}

		if (doScroll) {
			if (mShowViewWhileRefreshing) {
				smoothScrollTo(mCurrentMode == LvExtendedMode.PULL_DOWN_TO_REFRESH ? -mHeaderHeight : mHeaderHeight);
			} else {
				smoothScrollTo(0);
			}
		}
	}

	protected final void smoothScrollTo(int y) {
		if (null != mCurrentSmoothScrollRunnable) {
			mCurrentSmoothScrollRunnable.stop();
		}
		//Log.i(TAG, "smoothScrollTo(), y = "+y+", getScrollY() = "+getScrollY());
		if (getScrollY() != y) {
			mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(mHandler, getScrollY()+mHeaderPaddingTop, y);
			mHandler.post(mCurrentSmoothScrollRunnable);
		}
		else{
			if(mState == PULL_TO_REFRESH)
				checkLvHeaderCbState();
		}
	}

	/**
	 * Updates the View State when the mode has been set. This does not do any
	 * checking that the mode is different to current state so always updates.
	 */
	protected void updateUIForMode() {
		// Remove Header, and then add Header Loading View again if needed
		if (this == mHeaderLayout.getParent()) {
			removeView(mHeaderLayout);
		}
		if (mMode.canPullDownUpdate()) {
			addView(mHeaderLayout, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));
		}

		// Remove Footer, and then add Footer Loading View again if needed
		if (this == mFooterLayout.getParent()) {
			removeView(mFooterLayout);
		}
		if (mMode.canPullUpLoadMore()) {
			addView(mFooterLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
					ViewGroup.LayoutParams.WRAP_CONTENT));

		}

		// Hide Loading Views
		refreshLoadingViewsHeight();

		// If we're not using Mode.BOTH, set mCurrentMode to mMode, otherwise
		// set it to pull down
		mCurrentMode = (mMode != LvExtendedMode.BOTH) ? mMode : LvExtendedMode.PULL_DOWN_TO_REFRESH;
	}

	private void init(Context context, AttributeSet attrs) {
		setOrientation(LinearLayout.VERTICAL);

		ViewConfiguration config = ViewConfiguration.get(context);
		mTouchSlop = config.getScaledTouchSlop();

		// Styleables from XML
//		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);
//		handleStyledAttributes(a);
//
//		if (a.hasValue(R.styleable.PullToRefresh_ptrMode)) {
//			mMode = LvExtendedMode.mapIntToMode(a.getInteger(R.styleable.PullToRefresh_ptrMode, 0));
//		}

		// Refreshable View
		// By passing the attrs, we can add ListView/GridView params via XML
		mRefreshableView = createRefreshableView(context, attrs);
		addRefreshableView(context, mRefreshableView);

		// We need to create now layouts now
		mHeaderLayout = new BeseyeRefreshLayout(context, LvExtendedMode.PULL_DOWN_TO_REFRESH, null);
		mFooterLayout = new BeseyeRefreshLayout(context, LvExtendedMode.PULL_UP_TO_REFRESH, null);

		// Add Header/Footer Views
		updateUIForMode();

		// Styleables from XML
//		if (a.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
//			Drawable background = a.getDrawable(R.styleable.PullToRefresh_ptrHeaderBackground);
//			if (null != background) {
//				setBackgroundDrawable(background);
//			}
//		}
//		if (a.hasValue(R.styleable.PullToRefresh_ptrAdapterViewBackground)) {
//			Drawable background = a.getDrawable(R.styleable.PullToRefresh_ptrAdapterViewBackground);
//			if (null != background) {
//				mRefreshableView.setBackgroundDrawable(background);
//			}
//		}
//		a.recycle();
//		a = null;
	}

	private boolean isReadyForPull() {
//		switch (mMode) {
//			case PULL_DOWN_TO_REFRESH:
//				return isReadyForPullDown();
//			case PULL_UP_TO_REFRESH:
//				return isReadyForPullUp();
//			case BOTH:
//				return isReadyForPullUp() || isReadyForPullDown();
//		}
//		return false;
		return isReadyForPullUp() || isReadyForPullDown();
	}

	private void measureView(View child) {
		ViewGroup.LayoutParams p = child.getLayoutParams();
		if (p == null) {
			p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		}

		int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
		int lpHeight = p.height;
		int childHeightSpec;
		if (lpHeight > 0) {
			childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
		} else {
			childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
		}
		child.measure(childWidthSpec, childHeightSpec);
	}

	/**
	 * Actions a Pull Event
	 * 
	 * @return true if the Event has been handled, false if there has been no
	 *         change
	 */
	private boolean pullEvent() {

		int newHeight;
		final int oldHeight = getScrollY();

//		switch (mCurrentMode) {
//			case PULL_UP_TO_REFRESH:
//				newHeight = Math.round(Math.max(mInitialMotionY - mLastMotionY, 0) / FRICTION);
//				break;
//			case PULL_DOWN_TO_REFRESH:
//			default:
//				newHeight = Math.round(Math.min(mInitialMotionY - mLastMotionY, 0) / FRICTION);
//				break;
//		}
		
		switch (mLvPullMode) {
			case PULL_UP:
				newHeight = Math.round(Math.max(mInitialMotionY - mLastMotionY, 0) / FRICTION);
				break;
			case PULL_DOWN:
			default:{
				//Drag from first lock item area
//				if(LvPullDirection.PULL_DOWN == mLvPullMode && 0 > mInitialMotionY && 0 > mLastMotionY){
//					Log.d(TAG, "pullEvent(), handle drag from first lock area case < "+mInitialMotionY+", "+mLastMotionY+">");
//					mInitialMotionY = 0;
//					newHeight = 0;
//				}
//				else
					newHeight = Math.round(Math.min(mInitialMotionY - mLastMotionY, 0) / FRICTION);
			
				break;
			}
		}
		
		//Log.i(TAG, "pullEvent(), < "+mInitialMotionY+", "+mLastMotionY+", "+", "+newHeight+">");
		
		setHeaderScroll(newHeight);

		if (newHeight != 0) {

			float scale = Math.abs(newHeight) / (float) mHeaderHeight;
//			switch (mCurrentMode) {
//				case PULL_UP_TO_REFRESH:
//					mFooterLayout.onPullY(scale);
//					break;
//				case PULL_DOWN_TO_REFRESH:
//					mHeaderLayout.onPullY(scale);
//					break;
//			}
			
			switch (mLvPullMode) {
				case PULL_UP:
					mFooterLayout.onPullY(scale);
					break;
				case PULL_DOWN:
				default:
					mHeaderLayout.onPullY(scale);
					break;
			}

			if (mState == PULL_TO_REFRESH && mHeaderHeight < Math.abs(newHeight)) {
				mState = RELEASE_TO_REFRESH;
				onReleaseToRefresh();
				return true;

			} else if (mState == RELEASE_TO_REFRESH && mHeaderHeight >= Math.abs(newHeight)) {
				mState = PULL_TO_REFRESH;
				onPullToRefresh();
				return true;
			}
		}

		return oldHeight != newHeight;
	}

	/**
	 * Re-measure the Loading Views height, and adjust internal padding as
	 * necessary
	 */
	private void refreshLoadingViewsHeight() {
		if (mMode.canPullDownUpdate()) {
			measureView(mHeaderLayout);
			mHeaderHeight = mHeaderLayout.getMeasuredHeight();
		} else if (mMode.canPullUpLoadMore()) {
			measureView(mFooterLayout);
			mHeaderHeight = mFooterLayout.getMeasuredHeight();
		}

		// Hide Loading Views
		switch (mMode) {
			case BOTH:
				setPadding(0, -mHeaderHeight, 0, -mHeaderHeight);
				break;
			case PULL_UP_TO_REFRESH:
				setPadding(0, 0, 0, -mHeaderHeight);
				break;
			case PULL_DOWN_TO_REFRESH:
				setPadding(0, -mHeaderHeight, 0, 0);
				break;
			case NONE:
			default:
				setPadding(0, 0, 0, 0);
				break;
		}
	}

	public static enum LvExtendedMode {
		/**
		 * Only allow the user to pull the listview up/down
		 */
		NONE(0x0),
		
		/**
		 * Only allow the user to Pull Down from the top to refresh, this is the
		 * default.
		 */
		PULL_DOWN_TO_REFRESH(0x1),

		/**
		 * Only allow the user to Pull Up from the bottom to refresh.
		 */
		PULL_UP_TO_REFRESH(0x2),

		/**
		 * Allow the user to both Pull Down from the top, and Pull Up from the
		 * bottom to refresh.
		 */
		BOTH(0x3);

		/**
		 * Maps an int to a specific mode. This is needed when saving state, or
		 * inflating the view from XML where the mode is given through a attr
		 * int.
		 * 
		 * @param modeInt
		 *            - int to map a Mode to
		 * @return Mode that modeInt maps to, or PULL_DOWN_TO_REFRESH by
		 *         default.
		 */
		public static LvExtendedMode mapIntToMode(int modeInt) {
			switch (modeInt) {
				case 0x0:
				default:
					return NONE;
				case 0x1:
					return PULL_DOWN_TO_REFRESH;
				case 0x2:
					return PULL_UP_TO_REFRESH;
				case 0x3:
					return BOTH;
			}
		}

		private int mIntValue;

		// The modeInt values need to match those from attrs.xml
		LvExtendedMode(int modeInt) {
			mIntValue = modeInt;
		}

		/**
		 * @return true if this mode permits Pulling Down from the top
		 */
		boolean canPullDownUpdate() {
			return this == PULL_DOWN_TO_REFRESH || this == BOTH;
		}

		/**
		 * @return true if this mode permits Pulling Up from the bottom
		 */
		boolean canPullUpLoadMore() {
			return this == PULL_UP_TO_REFRESH || this == BOTH;
		}

		int getIntValue() {
			return mIntValue;
		}

	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * Simple Listener that allows you to be notified when the user has scrolled
	 * to the end of the AdapterView. See (
	 * {@link PullToRefreshAdapterViewBase#setOnLastItemVisibleListener}.
	 * 
	 * @author Chris Banes
	 * 
	 */
	public static interface OnLastItemVisibleListener {

		/**
		 * Called when the user has scrolled to the end of the list
		 */
		public void onLastItemVisible();

	}

	/**
	 * Simple Listener to listen for any callbacks to Refresh.
	 * 
	 * @author Chris Banes
	 */
	public static interface OnRefreshListener {

		/**
		 * onRefresh will be called for both Pull Down from top, and Pull Up
		 * from Bottom
		 */
		public void onRefresh();

		public void onRefreshCancel();
	}

	/**
	 * An advanced version of the Listener to listen for callbacks to Refresh.
	 * This listener is different as it allows you to differentiate between Pull
	 * Ups, and Pull Downs.
	 * 
	 * @author Chris Banes
	 */
	public static interface OnRefreshListener2 {

		/**
		 * onPullDownToRefresh will be called only when the user has Pulled Down
		 * from the top, and released.
		 */
		public void onPullDownToRefresh();

		/**
		 * onPullUpToRefresh will be called only when the user has Pulled Up
		 * from the bottom, and released.
		 */
		public void onPullUpToRefresh();

	}
	
	public static interface OnLvHeaderListener{
		public void onLvHeaderShow();
		public void onLvHeaderHide();
	}

	final class SmoothScrollRunnable implements Runnable {

		static final int ANIMATION_DURATION_MS = 190;
		static final int ANIMATION_FPS = 1000 / 60;

		private final Interpolator mInterpolator;
		private final int mScrollToY;
		private final int mScrollFromY;
		private final Handler mHandler;

		private boolean mContinueRunning = true;
		private long mStartTime = -1;
		private int mCurrentY = -1;

		public SmoothScrollRunnable(Handler handler, int fromY, int toY) {
			mHandler = handler;
			mScrollFromY = fromY;
			mScrollToY = toY;
			mInterpolator = new AccelerateDecelerateInterpolator();
		}

		@Override
		public void run() {

			/**
			 * Only set mStartTime if this is the first time we're starting,
			 * else actually calculate the Y delta
			 */
			if (mStartTime == -1) {
				mStartTime = System.currentTimeMillis();
			} else {

				/**
				 * We do do all calculations in long to reduce software float
				 * calculations. We use 1000 as it gives us good accuracy and
				 * small rounding errors
				 */
				long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / ANIMATION_DURATION_MS;
				normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

				final int deltaY = Math.round((mScrollFromY - mScrollToY)
						* mInterpolator.getInterpolation(normalizedTime / 1000f));
				mCurrentY = mScrollFromY - deltaY;
				setHeaderScroll(mCurrentY);
			}

			// If we're not at the target Y, keep going...
			if (mContinueRunning && mScrollToY != mCurrentY) {
				mHandler.postDelayed(this, ANIMATION_FPS);
			}
			else{
				if(mState == PULL_TO_REFRESH)
					checkLvHeaderCbState();
			}
		}

		public void stop() {
			mContinueRunning = false;
			mHandler.removeCallbacks(this);
//			if(mState == PULL_TO_REFRESH)
//				checkLvHeaderCbState();
		}
	}

}
