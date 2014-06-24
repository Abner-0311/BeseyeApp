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
package com.app.beseye.widget.internal;

import static com.app.beseye.util.BeseyeUtils.*;

import java.util.Date;

import com.app.beseye.R;
import com.app.beseye.util.BeseyeUtils;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

public class LoadingLayout extends FrameLayout {
	static final int DEFAULT_ROTATION_ANIMATION_DURATION = 600;

	private final ImageView mHeaderImage;
	private final Matrix mHeaderImageMatrix;

	private final TextView mHeaderText;
	private final TextView mSubHeaderText;

	private String mPullLabel;
	private String mRefreshingLabel;
	private String mReleaseLabel;

	private float mRotationPivotX, mRotationPivotY;
	com.app.beseye.widget.PullToRefreshBase.LvExtendedMode mMode;

	private final Animation mRotateAnimation;
	
	protected Date mdateUpdate = new Date();
	
	protected int getLayoutId(){
		return R.layout.pull_to_refresh_header; 
	}
	
	protected int getRefreshImageId(){
		return R.drawable.cycle;
	}
	
	protected int getIndicatorImageId(){
		return R.drawable.cycle;
	}
	
	public void setLatestUpdate(){
		mdateUpdate = new Date();
		mPullLabel = String.format(getContext().getString(R.string.loading_latest_update), BeseyeUtils.getDateDiffString(getContext(), mdateUpdate));
		mReleaseLabel = String.format(getContext().getString(R.string.loading_latest_update), BeseyeUtils.getDateDiffString(getContext(), mdateUpdate));
		
		if(null != mHeaderText)
			mHeaderText.setText(Html.fromHtml(mPullLabel));
	}
	
	public LoadingLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		ViewGroup header = (ViewGroup) LayoutInflater.from(context).inflate(getLayoutId(), this);
		mHeaderText = (TextView) header.findViewById(R.id.pull_to_refresh_text);
		mSubHeaderText = (TextView) header.findViewById(R.id.pull_to_refresh_sub_text);
		mHeaderImage = (ImageView) header.findViewById(R.id.pull_to_refresh_image);
		mHeaderImageMatrix = new Matrix();
		mRotateAnimation = new RotateAnimation(0, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
		init(context, LvExtendedMode.PULL_UP_TO_REFRESH, null);
	}

	public LoadingLayout(Context context, final LvExtendedMode mode, TypedArray attrs) {
		super(context);
		ViewGroup header = (ViewGroup) LayoutInflater.from(context).inflate(getLayoutId(), this);
		mHeaderText = (TextView) header.findViewById(R.id.pull_to_refresh_text);
		mSubHeaderText = (TextView) header.findViewById(R.id.pull_to_refresh_sub_text);
		mHeaderImage = (ImageView) header.findViewById(R.id.pull_to_refresh_image);
		mHeaderImageMatrix = new Matrix();
		mRotateAnimation = new RotateAnimation(0, -360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
		init(context, mode, attrs);
	}
	
	private void init(Context context, final LvExtendedMode mode, TypedArray attrs){
		mHeaderImage.setScaleType(ScaleType.MATRIX);
		mHeaderImage.setImageMatrix(mHeaderImageMatrix);

		final Interpolator interpolator = new LinearInterpolator();
		
		mRotateAnimation.setInterpolator(interpolator);
		mRotateAnimation.setDuration(DEFAULT_ROTATION_ANIMATION_DURATION);
		mRotateAnimation.setRepeatCount(Animation.INFINITE);
		mRotateAnimation.setRepeatMode(Animation.RESTART);
		
		mMode = mode;
		switch (mode) {
			case PULL_UP_TO_REFRESH:
				// Load in labels
//				mPullLabel = context.getString(R.string.pull_to_refresh_from_bottom_pull_label);
//				mRefreshingLabel = context.getString(R.string.pull_to_refresh_from_bottom_refreshing_label);
//				mReleaseLabel = context.getString(R.string.pull_to_refresh_from_bottom_release_label);
				mRefreshingLabel = context.getString(R.string.pull_to_refresh_from_bottom_refreshing_label);
				mPullLabel = String.format(getContext().getString(R.string.loading_latest_update), getDateDiffString(getContext(), mdateUpdate));
				mReleaseLabel = String.format(getContext().getString(R.string.loading_latest_update), getDateDiffString(getContext(), mdateUpdate));
				
				break;

			case PULL_DOWN_TO_REFRESH:
			default:
				// Load in labels
//				mPullLabel = context.getString(R.string.pull_to_refresh_pull_label);
//				mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
//				mReleaseLabel = context.getString(R.string.pull_to_refresh_release_label);
				
				mRefreshingLabel = context.getString(R.string.pull_to_refresh_refreshing_label);
				mPullLabel = String.format(getContext().getString(R.string.loading_latest_update), getDateDiffString(getContext(), mdateUpdate));
				mReleaseLabel = String.format(getContext().getString(R.string.loading_latest_update), getDateDiffString(getContext(), mdateUpdate));
				break;
		}

//		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderTextColor)) {
//			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderTextColor);
//			setTextColor(null != colors ? colors : ColorStateList.valueOf(0xFF000000));
//		}
//		if (attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderSubTextColor)) {
//			ColorStateList colors = attrs.getColorStateList(R.styleable.PullToRefresh_ptrHeaderSubTextColor);
//			setSubTextColor(null != colors ? colors : ColorStateList.valueOf(0xFF000000));
//		}
//		if (null != attrs && attrs.hasValue(R.styleable.PullToRefresh_ptrHeaderBackground)) {
//			Drawable background = attrs.getDrawable(R.styleable.PullToRefresh_ptrHeaderBackground);
//			if (null != background) {
//				setBackgroundDrawable(background);
//			}
//		}

		if(this instanceof BeseyeRefreshLayout){
			setIndicatorDrawable();
		}else{
			setLoadingDrawable();
//			// Try and get defined drawable from Attrs
//			Drawable imageDrawable = null;
////			if (null != attrs && attrs.hasValue(R.styleable.PullToRefresh_ptrDrawable)) {
////				imageDrawable = attrs.getDrawable(R.styleable.PullToRefresh_ptrDrawable);
////			}
//
//			// If we don't have a user defined drawable, load the default
//			if (null == imageDrawable) {
//				imageDrawable = context.getResources().getDrawable(getRefreshImageId());
//			}
//
//			// Set Drawable, and save width/height
//			setLoadingDrawable(imageDrawable);
		}
		

		reset();
	}

	public void reset() {
		mHeaderText.setText(Html.fromHtml(mPullLabel));
		mHeaderImage.setVisibility(View.VISIBLE);
		mHeaderImage.clearAnimation();

		resetImageRotation();
		if(this instanceof BeseyeRefreshLayout){
			setIndicatorDrawable();
			mfScaleOfHeight  = 0.0f;
			if(LvExtendedMode.PULL_UP_TO_REFRESH == mMode){
				mHeaderImageMatrix.setRotate(180, mRotationPivotX, mRotationPivotY);
				mHeaderImage.setImageMatrix(mHeaderImageMatrix);
			}
		}

		if(null != mSubHeaderText)
			mSubHeaderText.setVisibility(View.GONE);
		
//		if (TextUtils.isEmpty(mSubHeaderText.getText())) {
//			mSubHeaderText.setVisibility(View.GONE);
//		} else {
//			mSubHeaderText.setVisibility(View.VISIBLE);
//		}
	}

	public void releaseToRefresh() {
		mHeaderText.setText(Html.fromHtml(mReleaseLabel));
	}

	public void setPullLabel(String pullLabel) {
		mPullLabel = pullLabel;
	}

	public void refreshing() {
		mHeaderText.setText(Html.fromHtml(mPullLabel));
		if(this instanceof BeseyeRefreshLayout){
			setLoadingDrawable();
		}
		mHeaderImage.startAnimation(mRotateAnimation);

		if(null != mSubHeaderText){
			mSubHeaderText.setText(Html.fromHtml(mRefreshingLabel));
			mSubHeaderText.setVisibility(View.VISIBLE);
		}
	}
	
	public void showRefreshText() {
		mHeaderText.setText(Html.fromHtml(mRefreshingLabel));
		if(null != mSubHeaderText){
			mSubHeaderText.setVisibility(View.GONE);
		}
	}

	public void setRefreshingLabel(String refreshingLabel) {
		mRefreshingLabel = refreshingLabel;
		if(null != mHeaderText){
			if(null == mRefreshingLabel || 0 == mRefreshingLabel.length())
				mHeaderText.setVisibility(View.GONE);
			else{
				mHeaderText.setText(mRefreshingLabel);
			}
		}		
	}
	
	public void startRefreshingAnimation() {
		mHeaderImage.startAnimation(mRotateAnimation);
	}
	
	public void showRefreshingVisibility(int iVisibility) {
		mHeaderImage.setVisibility(iVisibility);
		if(View.GONE == iVisibility)
			mHeaderImage.clearAnimation();
	}

	public void setReleaseLabel(String releaseLabel) {
		mReleaseLabel = releaseLabel;
	}

	public void pullToRefresh() {
		mHeaderText.setText(Html.fromHtml(mPullLabel));
	}

	public void setTextColor(ColorStateList color) {
		mHeaderText.setTextColor(color);
		mSubHeaderText.setTextColor(color);
	}

	public void setSubTextColor(ColorStateList color) {
		mSubHeaderText.setTextColor(color);
	}

	public void setTextColor(int color) {
		setTextColor(ColorStateList.valueOf(color));
	}

	private Drawable mLoadingDrawable = null;
	public void setLoadingDrawable(Drawable imageDrawable) {
		// Set Drawable, and save width/height
		if(null == mLoadingDrawable)
			mLoadingDrawable = imageDrawable;
		
		mHeaderImage.setImageDrawable(mLoadingDrawable);
		mRotationPivotX = mLoadingDrawable.getIntrinsicWidth() / 2f;
		mRotationPivotY = mLoadingDrawable.getIntrinsicHeight() / 2f;
	}
	
	protected void setLoadingDrawable(){
		if(null == mLoadingDrawable){
			mLoadingDrawable = getResources().getDrawable(getRefreshImageId());
		}
		
		mHeaderImage.setImageDrawable(mLoadingDrawable);
		mRotationPivotX = mLoadingDrawable.getIntrinsicWidth() / 2f;
		mRotationPivotY = mLoadingDrawable.getIntrinsicHeight() / 2f;
	}
	
	private Drawable mIndDrawable = null;
	protected void setIndicatorDrawable(){
		if(null == mIndDrawable){
			mIndDrawable = getResources().getDrawable(getIndicatorImageId());
		}
		
		mHeaderImage.setImageDrawable(mIndDrawable);
		mRotationPivotX = mIndDrawable.getIntrinsicWidth() / 2f;
		mRotationPivotY = mIndDrawable.getIntrinsicHeight() / 2f;
	}

	public void setSubTextColor(int color) {
		setSubTextColor(ColorStateList.valueOf(color));
	}

	public void setSubHeaderText(CharSequence label) {
		if(null != mSubHeaderText){
			if (TextUtils.isEmpty(label)) {
				mSubHeaderText.setVisibility(View.GONE);
			} else {
				mSubHeaderText.setText(label);
				mSubHeaderText.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private float mfScaleOfHeight = 0.0f;
	static private Animation s_aniClockwise180;
	static private Animation s_aniCounterClockwise180;
	static private int siAnimDuration = 500;
	
	public void onPullY(float scaleOfHeight) {
		if(this instanceof BeseyeRefreshLayout){
			if(0 < Float.compare(scaleOfHeight, 1.0f) && 0 >= Float.compare(mfScaleOfHeight, 1.0f)){
				if(null == s_aniCounterClockwise180){
					siAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
					s_aniCounterClockwise180 = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
					if(null != s_aniCounterClockwise180){
						s_aniCounterClockwise180.setDuration(siAnimDuration);
						s_aniCounterClockwise180.setRepeatCount(0);
						s_aniCounterClockwise180.setFillAfter(true);
					}
				}
					
				mHeaderImage.startAnimation(s_aniCounterClockwise180);
				
			}else if(0 > Float.compare(scaleOfHeight, 1.0f) && 0 <= Float.compare(mfScaleOfHeight, 1.0f)){
				if(null == s_aniClockwise180){
					siAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
					s_aniClockwise180 = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,0.5f);
					if(null != s_aniClockwise180){
						s_aniClockwise180.setDuration(siAnimDuration);
						s_aniClockwise180.setRepeatCount(0);
						s_aniClockwise180.setFillAfter(true);
					}
				}
					
				mHeaderImage.startAnimation(s_aniClockwise180);
			}
			mfScaleOfHeight = scaleOfHeight;
		}
//		Log.d(iKalaUtil.IKALA_APP_TAG, "onPullY(), scaleOfHeight < "+scaleOfHeight+">");
//		mHeaderImageMatrix.setRotate(scaleOfHeight * 90, mRotationPivotX, mRotationPivotY);
//		mHeaderImage.setImageMatrix(mHeaderImageMatrix);
	}

	private void resetImageRotation() {
		mHeaderImageMatrix.reset();
		mHeaderImage.setImageMatrix(mHeaderImageMatrix);
	}
	
	public void setHeaderImageVisibility(int iVisibleType){
		if(null != mHeaderImage)
			mHeaderImage.setVisibility(iVisibleType);
	}
}
