package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.TAG;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.app.beseye.R;

public class MotionNotificationSettingView extends View {
  	// point1 and point 3 are of same group and same as point 2 and point4
	private Point[] points = new Point[4];
    
    // variable to know what ball is being dragged
    private Paint linePaint;
    private Canvas canvas;
    
    DisplayMetrics dm = getResources().getDisplayMetrics() ;
    float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm);

    public MotionNotificationSettingView(Context context) {
        super(context);
        linePaint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    public MotionNotificationSettingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MotionNotificationSettingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {
        if(points[3]==null) 
            return;
        
        linePaint.setAntiAlias(true);
        linePaint.setDither(true);			
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        // draw line
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.parseColor("#00bbb3"));
        linePaint.setStrokeWidth(strokeWidth);
        
//        canvas.drawRect(points[0].x+strokeWidth/2, 
//        				points[0].y+strokeWidth/2, 
//        				points[2].x-strokeWidth/2, 
//        				points[2].y-strokeWidth/2, linePaint);
        
        canvas.drawRect(50, 50, 200, 200, linePaint); 
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }
    
    
    public void Init(int viewWidth, int viewHeight, double[] r){
    	//initialize rectangle.
    	double leftR = r[0];
    	double topR = r[1];
    	double rightR = r[2]; 
    	double botR = r[3];
    	
        points[0] = new Point();
        points[0].x = (int) (leftR*viewWidth);
        points[0].y = (int) (topR*viewHeight);

        points[1] = new Point();
        points[1].x = (int) (leftR*viewWidth);
        points[1].y = (int) (botR*viewHeight);

        points[2] = new Point();
        points[2].x = (int) (rightR*viewWidth);
        points[2].y = (int) (botR*viewHeight);

        points[3] = new Point();
        points[3].x = (int) (rightR*viewWidth);
        points[3].y = (int) (topR*viewHeight);
        
        invalidate();
    }
}
