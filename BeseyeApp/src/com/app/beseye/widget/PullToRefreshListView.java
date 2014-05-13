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

import com.app.beseye.R;
import com.app.beseye.widget.internal.BeseyeLoadMoreLayout;
import com.app.beseye.widget.internal.BeseyeRefreshLayout;
import com.app.beseye.widget.internal.EmptyViewMethodAccessor;
import com.app.beseye.widget.internal.LoadingLayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;


public class PullToRefreshListView extends PullToRefreshAdapterViewBase<ListView> {

	private LoadingLayout mHeaderLoadingView;
	private LoadingLayout mFooterLoadingView;

	private FrameLayout mLvHeaderLoadingFrame;
	private FrameLayout mLvFooterLoadingFrame;

	public PullToRefreshListView(Context context) {
		super(context);
		setDisableScrollingWhileRefreshing(false);
	}

	public PullToRefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDisableScrollingWhileRefreshing(false);
	}
	
	public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
	    super(context, attrs, defStyle);
	}

	public PullToRefreshListView(Context context, LvExtendedMode mode) {
		super(context, mode);
		setDisableScrollingWhileRefreshing(false);
	}

	@Override
	public ContextMenuInfo getContextMenuInfo() {
		return ((InternalListView) getRefreshableView()).getContextMenuInfo();
	}

	public void setPullLabel(String pullLabel, LvExtendedMode mode) {
		super.setPullLabel(pullLabel, mode);

		if (null != mHeaderLoadingView && mode.canPullDownUpdate()) {
			mHeaderLoadingView.setPullLabel(pullLabel);
		}
		if (null != mFooterLoadingView && mode.canPullUpLoadMore()) {
			mFooterLoadingView.setPullLabel(pullLabel);
		}
	}

	public void setRefreshingLabel(String refreshingLabel, LvExtendedMode mode) {
		super.setRefreshingLabel(refreshingLabel, mode);

		if (null != mHeaderLoadingView && mode.canPullDownUpdate()) {
			mHeaderLoadingView.setRefreshingLabel(refreshingLabel);
		}
		if (null != mFooterLoadingView && mode.canPullUpLoadMore()) {
			mFooterLoadingView.setRefreshingLabel(refreshingLabel);
		}
	}

	public void setReleaseLabel(String releaseLabel, LvExtendedMode mode) {
		super.setReleaseLabel(releaseLabel, mode);

		if (null != mHeaderLoadingView && mode.canPullDownUpdate()) {
			mHeaderLoadingView.setReleaseLabel(releaseLabel);
		}
		if (null != mFooterLoadingView && mode.canPullUpLoadMore()) {
			mFooterLoadingView.setReleaseLabel(releaseLabel);
		}
	}

	@Override
	protected final ListView createRefreshableView(Context context, AttributeSet attrs) {
		ListView lv = new InternalListView(context, attrs);

		// Get Styles from attrs
		TypedArray a = null;//context.obtainStyledAttributes(attrs, R.styleable.PullToRefresh);

		
		// Create Loading Views ready for use later
		mLvHeaderLoadingFrame = new FrameLayout(context);
		mHeaderLoadingView = new BeseyeRefreshLayout(context, LvExtendedMode.PULL_DOWN_TO_REFRESH, a);
		mLvHeaderLoadingFrame.addView(mHeaderLoadingView, FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
		mHeaderLoadingView.setVisibility(View.GONE);
		//lv.addHeaderView(mLvHeaderLoadingFrame, null, false);

		mLvFooterLoadingFrame = new FrameLayout(context);
		mFooterLoadingView = new BeseyeRefreshLayout(context, LvExtendedMode.PULL_UP_TO_REFRESH, a);
		mLvFooterLoadingFrame.addView(mFooterLoadingView, FrameLayout.LayoutParams.FILL_PARENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		mFooterLoadingView.setVisibility(View.GONE);
		
		if(null != a)
			a.recycle();

		// Set it to this so it can be used in ListActivity/ListFragment
		lv.setId(android.R.id.list);
		return lv;
	}
	
	private View mHeaderLoadMoreView;
	private View mFooterLoadMoreView;
	private boolean mbHeaderLoadMoreViewAttached = false;
	private boolean mbFooterLoadMoreViewAttached = false;
	
	public void setHeaderLoadMoreView(View view){
		mHeaderLoadMoreView = view;
	} 
	
	public void setFooterLoadMoreView(View view){
		mFooterLoadMoreView = view;
	} 
	
	public void updateLatestTimestamp(){
		super.updateLatestTimestamp();
		if(mHeaderLoadingView instanceof LoadingLayout){
			((LoadingLayout)mHeaderLoadingView).setLatestUpdate();
		}
		
		if(mFooterLoadingView instanceof LoadingLayout){
			((LoadingLayout)mFooterLoadingView).setLatestUpdate();
		}
	}
	
	public void attachHeaderLoadMoreView(boolean bShowPrivacyCmt){
		if(null == mHeaderLoadMoreView){
			LoadingLayout loadingView= new BeseyeLoadMoreLayout(getContext(), LvExtendedMode.PULL_DOWN_TO_REFRESH, null);
			mHeaderLoadMoreView = loadingView;
			if(null != loadingView){
				loadingView.setBackgroundColor(getResources().getColor(R.color.loading_bg));
				if(bShowPrivacyCmt){
					//loadingView.setRefreshingLabel(getContext().getString(R.string.load_more_privacy_cmt));
				}else{
					loadingView.setRefreshingLabel("");
				}
				loadingView.startRefreshingAnimation();
			}
		}
		
		if(null != mHeaderLoadMoreView && !mbHeaderLoadMoreViewAttached){
			if(null != mRefreshableView){
				mRefreshableView.post(new Runnable(){
					@Override
					public void run() {
						if(null != mRefreshableView){
							mRefreshableView.addHeaderView(mHeaderLoadMoreView, null, false);
							mbHeaderLoadMoreViewAttached = true;
						}
					}});
			}
			updateLatestTimestamp();
		}
	}
	
	public void attachFooterLoadMoreView(boolean bShowPrivacyCmt, boolean bShowLoading){
		if(null == mFooterLoadMoreView){
			LoadingLayout loadingView= new BeseyeLoadMoreLayout(getContext(), LvExtendedMode.PULL_DOWN_TO_REFRESH, null);
			mFooterLoadMoreView = loadingView;
			if(null != loadingView){
				loadingView.setBackgroundColor(getResources().getColor(R.color.loading_bg));
			}
		}
		
		if(mFooterLoadMoreView instanceof LoadingLayout){
			if(bShowPrivacyCmt){
				//((LoadingLayout)mFooterLoadMoreView).setRefreshingLabel(getContext().getString(R.string.load_more_privacy_cmt));
			}else{
				((LoadingLayout)mFooterLoadMoreView).setRefreshingLabel("");
			}	
			
			if(bShowLoading){
				((LoadingLayout)mFooterLoadMoreView).showRefreshingVisibility(View.VISIBLE);
				((LoadingLayout)mFooterLoadMoreView).startRefreshingAnimation();
			}else{
				((LoadingLayout)mFooterLoadMoreView).showRefreshingVisibility(View.GONE);
			}
		}
		
		if(null != mFooterLoadMoreView && !mbFooterLoadMoreViewAttached){
			if(null != mRefreshableView){
				mRefreshableView.post(new Runnable(){
					@Override
					public void run() {
						if(null != mRefreshableView){
							mRefreshableView.addFooterView(mFooterLoadMoreView, null, false);
							mbFooterLoadMoreViewAttached = true;
						}
					}});
			}
			
			updateLatestTimestamp();
		}
	}
	
	public void dettachHeaderLoadMoreView(){
		if(null != mHeaderLoadMoreView && mbHeaderLoadMoreViewAttached){
			if(null != mRefreshableView){
				mRefreshableView.removeHeaderView(mHeaderLoadMoreView);
				mbHeaderLoadMoreViewAttached = false;
			}
		}
	}
	
	public void dettachFooterLoadMoreView(){
		if(null != mFooterLoadMoreView && mbFooterLoadMoreViewAttached){
			if(null != mRefreshableView){
				mRefreshableView.removeFooterView(mFooterLoadMoreView);
				mbFooterLoadMoreViewAttached = false;
			}
		}
	}
	
	public boolean isHeaderLoadMoreViewAttached(){
		return mbHeaderLoadMoreViewAttached;
	}
	
	public boolean isFooterLoadMoreViewAttached(){
		return mbFooterLoadMoreViewAttached;
	}
	
	public void setMode(LvExtendedMode mode) {
		if(LvExtendedMode.NONE == mode){
			removeHeader();
			removeFooter();
		}
		else if(LvExtendedMode.PULL_DOWN_TO_REFRESH == mode){
			addHeader();
			removeFooter();
		}
		else if(LvExtendedMode.PULL_UP_TO_REFRESH == mode){
			removeHeader();
			addFooter();
		}
		else if(LvExtendedMode.BOTH == mode){
			addHeader();
			addFooter();
		}
		
		super.setMode(mode);
	}
	
	private void addHeader(){
		if(null != mRefreshableView && null != mLvHeaderLoadingFrame && 0 == mRefreshableView.getHeaderViewsCount()){
			mRefreshableView.addHeaderView(mLvHeaderLoadingFrame, null, false);
		}
	}
	
	private void removeHeader(){
		if(null != mRefreshableView && null != mLvHeaderLoadingFrame && null != mLvHeaderLoadingFrame.getParent() && 0 < mRefreshableView.getHeaderViewsCount()){
			mRefreshableView.removeHeaderView(mLvHeaderLoadingFrame);
		}
	}
	
	private void addFooter(){
		if(null != mRefreshableView && null != mLvFooterLoadingFrame && 0 == mRefreshableView.getFooterViewsCount()){
			mRefreshableView.addFooterView(mLvFooterLoadingFrame, null, false);
		}
	}
	
	private void removeFooter(){
		if(null != mRefreshableView && null != mLvFooterLoadingFrame && null != mLvFooterLoadingFrame.getParent() && 0 < mRefreshableView.getFooterViewsCount()){
			mRefreshableView.removeFooterView(mLvFooterLoadingFrame);
		}
	}

	protected int getNumberInternalFooterViews() {
		return null != mFooterLoadingView ? 1 : 0;
	}

	protected int getNumberInternalHeaderViews() {
		return null != mHeaderLoadingView ? 1 : 0;
	}

	@Override
	protected void resetHeader() {

		// If we're not showing the Refreshing view, or the list is empty, then
		// the header/footer views won't show so we use the
		// normal method
		ListAdapter adapter = mRefreshableView.getAdapter();
		if (!getShowViewWhileRefreshing() || null == adapter || adapter.isEmpty()) {
			super.resetHeader();
			return;
		}

		LoadingLayout originalLoadingLayout;
		LoadingLayout listViewLoadingLayout;

		int scrollToHeight = getHeaderHeight();
		int selection;
		boolean scroll;
		int iVisibility = View.VISIBLE;

		switch (getCurrentMode()) {
			case PULL_UP_TO_REFRESH:
				originalLoadingLayout = getFooterLayout();
				listViewLoadingLayout = mFooterLoadingView;
				selection = mRefreshableView.getCount() - 1;//Because there is header
				scroll = mRefreshableView.getLastVisiblePosition() == selection;
				if(LvExtendedMode.NONE == getMode() || LvExtendedMode.PULL_DOWN_TO_REFRESH == getMode()){
					iVisibility = View.INVISIBLE;
				}
				break;
			case PULL_DOWN_TO_REFRESH:
			default:
				originalLoadingLayout = getHeaderLayout();
				listViewLoadingLayout = mHeaderLoadingView;
				scrollToHeight *= -1;
				selection = 0;
				scroll = mRefreshableView.getFirstVisiblePosition() == selection;
				if(LvExtendedMode.NONE == getMode() || LvExtendedMode.PULL_UP_TO_REFRESH == getMode()){
					iVisibility = View.INVISIBLE;
				}
				break;
		}

		// Set our Original View to Visible
		if(null != originalLoadingLayout)
			originalLoadingLayout.setVisibility(iVisibility);

		/**
		 * Scroll so the View is at the same Y as the ListView header/footer,
		 * but only scroll if we've pulled to refresh and it's positioned
		 * correctly
		 */
		if (scroll && getState() != MANUAL_REFRESHING) {
			mRefreshableView.setSelection(selection);
			setHeaderScroll(scrollToHeight);
		}

		// Hide the ListView Header/Footer
		if(null != listViewLoadingLayout)
			listViewLoadingLayout.setVisibility(View.GONE);

		super.resetHeader();
	}

	@Override
	protected void setRefreshingInternal(boolean doScroll) {

		// If we're not showing the Refreshing view, or the list is empty, then
		// the header/footer views won't show so we use the
		// normal method
		ListAdapter adapter = mRefreshableView.getAdapter();
		if (!getShowViewWhileRefreshing() || null == adapter || adapter.isEmpty()) {
			super.setRefreshingInternal(doScroll);
			return;
		}

		super.setRefreshingInternal(false);

		final LoadingLayout originalLoadingLayout, listViewLoadingLayout;
		final int selection, scrollToY;
		int iVisibility = View.VISIBLE;

		switch (getCurrentMode()) {
			case PULL_UP_TO_REFRESH:
				originalLoadingLayout = getFooterLayout();
				listViewLoadingLayout = mFooterLoadingView;
				selection = mRefreshableView.getCount() - 1;
				scrollToY = getScrollY() - getHeaderHeight();
//				if(/*LvExtendedMode.NONE == getMode() || */LvExtendedMode.PULL_DOWN_TO_REFRESH == getMode() /*|| (LvExtendedMode.PULL_UP_TO_REFRESH == getMode() && LvPullDirection.PULL_DOWN == getPullDirection())*/){
//					iVisibility = View.INVISIBLE;
//				}
//				else 
					if(LvExtendedMode.NONE == getMode() || LvExtendedMode.PULL_DOWN_TO_REFRESH == getMode()){
					iVisibility = View.GONE;
				}
				break;
			case PULL_DOWN_TO_REFRESH:
			default:
				originalLoadingLayout = getHeaderLayout();
				listViewLoadingLayout = mHeaderLoadingView;
				selection = 0;
				scrollToY = getScrollY() + getHeaderHeight();
//				if(/*LvExtendedMode.NONE == getMode() || */LvExtendedMode.PULL_DOWN_TO_REFRESH == getMode() /*|| (LvExtendedMode.PULL_UP_TO_REFRESH == getMode() && LvPullDirection.PULL_DOWN == getPullDirection())*/){
//					iVisibility = View.INVISIBLE;
//				}
//				else 
				if(LvExtendedMode.NONE == getMode() || LvExtendedMode.PULL_UP_TO_REFRESH == getMode()){
					iVisibility = View.GONE;
				}
				break;
		}

		if (doScroll) {
			// We scroll slightly so that the ListView's header/footer is at the
			// same Y position as our normal header/footer
			setHeaderScroll(scrollToY);
		}

		// Hide our original Loading View
		if(null != originalLoadingLayout){
			originalLoadingLayout.setVisibility(View.INVISIBLE);
		}

		// Show the ListView Loading View and set it to refresh
		if(null != listViewLoadingLayout){
			listViewLoadingLayout.setVisibility(iVisibility);
			listViewLoadingLayout.refreshing();
		}
		
		if (doScroll) {
			// Make sure the ListView is scrolled to show the loading
			// header/footer
			//if(View.VISIBLE == iVisibility)
				mRefreshableView.setSelection(selection);

			// Smooth scroll as normal
			smoothScrollTo(0);
		}
	}
	
	public void setOnItemClickListener(OnItemClickListener listener) {
		if(null != getRefreshableView())
			getRefreshableView().setOnItemClickListener(listener);
	}
	
	public void setSelection(int keyIndex) {
		if(null != getRefreshableView())
			getRefreshableView().setSelection(keyIndex);
	}
	
	public void invalidateViews() {
		if(null != getRefreshableView())
			getRefreshableView().invalidateViews();
	}
	
	public void setAdapter(ListAdapter adapter){
		if(null != getRefreshableView())
			getRefreshableView().setAdapter(adapter);
	}

	class InternalListView extends ListView implements EmptyViewMethodAccessor {

		private boolean mAddedLvFooter = false;

		public InternalListView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		@Override
		public void draw(Canvas canvas) {
			/**
			 * This is a bit hacky, but ListView has got a bug in it when using
			 * Header/Footer Views and the list is empty. This masks the issue
			 * so that it doesn't cause an FC. See Issue #66.
			 */
			try {
				super.draw(canvas);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public ContextMenuInfo getContextMenuInfo() {
			return super.getContextMenuInfo();
		}
		
		public void reset(){
			mAddedLvFooter = false;
		}

		@Override
		public void setAdapter(ListAdapter adapter) {
			// Add the Footer View at the last possible moment
			if (!mAddedLvFooter && null != mLvFooterLoadingFrame && 0 == getFooterViewsCount()) {
				addFooterView(mLvFooterLoadingFrame, null, false);
				mAddedLvFooter = true;
			}

			super.setAdapter(adapter);
		}

		@Override
		public void setEmptyView(View emptyView) {
			PullToRefreshListView.this.setEmptyView(emptyView);
		}

		@Override
		public void setEmptyViewInternal(View emptyView) {
			super.setEmptyView(emptyView);
		}
	}

}
