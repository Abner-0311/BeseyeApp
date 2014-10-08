package com.app.beseye.widget.internal;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.app.beseye.R;
import com.app.beseye.widget.PullToRefreshBase.LvExtendedMode;

public class BeseyeLoadMoreLayout extends LoadingLayout {
	public BeseyeLoadMoreLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BeseyeLoadMoreLayout(Context context, LvExtendedMode pullDownToRefresh, TypedArray a) {
		super(context, pullDownToRefresh, a);
	}
	
	protected int getLayoutId(){
		return R.layout.beseye_loadmore_layout; 
	}
	
	protected int getRefreshImageId(){
		return R.drawable.common_loading;
	}
	
	protected int getIndicatorImageId(){
		return R.drawable.common_loading;
	}

}
