package com.app.beseye.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AmplitudeImageView extends ImageView {
	private float mfAmplitudeRatio = 0.0f;
	private RectF rectF = new RectF();

	public AmplitudeImageView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public AmplitudeImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public AmplitudeImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}
	
	public void setAmplitudeRatio(float fRatio){
		if(fRatio > 1.0f){
			mfAmplitudeRatio = 1.0f;
		}else if(fRatio < 0.0f){
			mfAmplitudeRatio = 0.0f;
		}else{
			mfAmplitudeRatio = fRatio;
		}
		
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		int iHeight = getHeight();
		rectF.set(0, iHeight*(1.0f - mfAmplitudeRatio), getWidth(), iHeight);
		canvas.clipRect(rectF);
		super.onDraw(canvas);        
	}
}
