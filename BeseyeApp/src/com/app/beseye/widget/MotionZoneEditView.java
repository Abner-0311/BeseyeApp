package com.app.beseye.widget;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.app.beseye.R;
import com.app.beseye.setting.MotionZoneEditActivity;
import com.app.beseye.util.BeseyeMotionZoneUtil;

/*
 * View to drawing
 * Position of points:
 * 		1			4
 * 	
 * 		2			3
 */

public class MotionZoneEditView extends View {
  	// point1 and point 3 are of same group and same as point 2 and point4
	private Point[] mPoints = new Point[4];
	private Point[] mOldPoints = new Point[4];
    
	// position of image
	private Point mpThumbnailTopLeft = new Point();
	private Point mpThumbnailBotRight = new Point();
	private int miThumbnailWidth = 0;
	private int miThumbnailHeight = 0;
	private int miMinMotionZoneL = 0; 	
    
	private ArrayList<ColorBall> mArrColorballs = new ArrayList<ColorBall>();
    private int miGroupId = -1;
    private int miBalId = 0;
    private final double mdFatfingerRange = 1.5;
	
    private Paint mLinePaint, mRectPaint;
    
    private int oldX = -1;
    private int oldY = -1;
    private final int strokeWidth = getResources().getDimensionPixelSize(R.dimen.motion_zone_strokewidth);	
	    
    public MotionZoneEditView(Context context) {
        super(context);
        basicSetup();
    }

    public MotionZoneEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        basicSetup();
    }

    public MotionZoneEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        basicSetup();
    }
    
    private void basicSetup(){
    	mLinePaint = new Paint();
        mRectPaint = new Paint();
        setFocusable(true);
  
        // line
        mLinePaint.setAntiAlias(true);
        mLinePaint.setDither(true);			
        mLinePaint.setStrokeJoin(Paint.Join.ROUND);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setColor(getResources().getColor(R.color.beseye_color_normal));
        mLinePaint.setStrokeWidth(strokeWidth);
        
        // rectangle
        mRectPaint.setAntiAlias(true);
        mRectPaint.setDither(true);
        mRectPaint.setStrokeJoin(Paint.Join.ROUND);
        mRectPaint.setStyle(Paint.Style.FILL);
        mRectPaint.setColor(getResources().getColor(R.color.mask_black));
        mRectPaint.setAlpha(BeseyeMotionZoneUtil.siMaskAlpha);
        mRectPaint.setStrokeWidth(0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(null == mPoints[0]){
        	 Log.e(TAG, "onDraw: mPoints are null");
        } else{
        	// draw mask
	        canvas.drawRect(mpThumbnailTopLeft.x, mpThumbnailTopLeft.y, mpThumbnailBotRight.x, mPoints[0].y, mRectPaint);	             
	        canvas.drawRect(mpThumbnailTopLeft.x, mPoints[0].y, mPoints[0].x, mPoints[2].y, mRectPaint);
	        canvas.drawRect(mPoints[2].x, mPoints[0].y, mpThumbnailBotRight.x, mPoints[2].y, mRectPaint);      
	        canvas.drawRect(mpThumbnailTopLeft.x, mPoints[2].y, mpThumbnailBotRight.x, mpThumbnailBotRight.y, mRectPaint);
	        // draw line
	        canvas.drawRect(mPoints[0].x, mPoints[0].y, mPoints[2].x, mPoints[2].y, mLinePaint);       
	        // draw balls
	        for (int i =0; i < mArrColorballs.size(); i ++) {
	            ColorBall ball = mArrColorballs.get(i);
	            canvas.drawBitmap(ball.getBitmap(), ball.getX()-ball.getWidthOfBall()/2, ball.getY()-ball.getHeightOfBall()/2, null);
	        }
        }
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();
        int dX = 0, dY = 0;
        
        switch (eventaction) {

	        case MotionEvent.ACTION_DOWN: // touch down
	            if (null == mPoints[0]) {
	               Log.e(TAG, "onTouchEvent: mPoints are null");
	            } else {
	            	//check if the finger is on a ball
	                miBalId = -1;
	                miGroupId = -1;
	                for (int i = mArrColorballs.size()-1; i>=0; i--) {
	                    ColorBall ball = mArrColorballs.get(i);
	           
	                    // calculate the radius from the touch to the center of the ball
	                    double radCircle = Math
	                            .sqrt((double) (((ball.getX() - X) * (ball.getX() - X)) 
	                            		+ (ball.getY() - Y) * (ball.getY() - Y)));
	
	                    if (radCircle < (ball.getWidthOfBall()*mdFatfingerRange)) {
	                        miBalId = ball.getID();
	                        if (miBalId == 1 || miBalId == 3) {
	                            miGroupId = 2;
	                        } else {
	                            miGroupId = 1;
	                        }
	                        break;
	                    }
	                } 
	                if(-1 == miBalId){
	                	// move rectangle
	                	oldX = X;
	                	oldY = Y;
	                }
	            }
	            break;
	
	        case MotionEvent.ACTION_MOVE: // touch drag with the ball
	
	            if (miBalId > -1) {
	            	// resize
	            	X = X > mpThumbnailBotRight.x? mpThumbnailBotRight.x : X;
            		X = X < mpThumbnailTopLeft.x? mpThumbnailTopLeft.x : X;
            		Y = Y > mpThumbnailBotRight.y? mpThumbnailBotRight.y : Y;
            		Y = Y < mpThumbnailTopLeft.y? mpThumbnailTopLeft.y : Y;
                	
	                // move the balls the same as the finger
	            	if(true == islegalScaleW(X)) {
		                mArrColorballs.get(miBalId).setX(X);
		                if (miGroupId == 1) {
		                    mArrColorballs.get(1).setX(mArrColorballs.get(0).getX());
		                    mArrColorballs.get(3).setX(mArrColorballs.get(2).getX());
		                } else {
		                    mArrColorballs.get(0).setX(mArrColorballs.get(1).getX());
		                    mArrColorballs.get(2).setX(mArrColorballs.get(3).getX());
		                }
	            	}
	            	if(true == islegalScaleH(Y)) {
	                    mArrColorballs.get(miBalId).setY(Y);
		                if (miGroupId == 1) {
		                    mArrColorballs.get(1).setY(mArrColorballs.get(2).getY());
		                    mArrColorballs.get(3).setY(mArrColorballs.get(0).getY());
		                } else {
		                    mArrColorballs.get(0).setY(mArrColorballs.get(3).getY());
		                    mArrColorballs.get(2).setY(mArrColorballs.get(1).getY());
		                }
	            	}
	            } else {
	            	// move dX, dY
	            	dX = X - oldX;
	            	dY = Y - oldY;

	            	if(mPoints[2].x  + dX > mpThumbnailBotRight.x) {
	                	dX = mpThumbnailBotRight.x - mPoints[2].x ;
	                }
	                if(mPoints[0].x  + dX < mpThumbnailTopLeft.x) {
	                	dX = mpThumbnailTopLeft.x - mPoints[0].x ; 
	                }
                	mArrColorballs.get(1).setX(mArrColorballs.get(1).getX() + dX);
	                mArrColorballs.get(2).setX(mArrColorballs.get(2).getX() + dX);
	            	mArrColorballs.get(3).setX(mArrColorballs.get(3).getX() + dX);
	            	mArrColorballs.get(0).setX(mArrColorballs.get(0).getX() + dX);
	                oldX = X;

	                if(mPoints[2].y  + dY > mpThumbnailBotRight.y) {
	                	dY = mpThumbnailBotRight.y - mPoints[2].y ;
	                }
	                if(mPoints[0].y  + dY < mpThumbnailTopLeft.y) {
	                	dY = mpThumbnailTopLeft.y - mPoints[0].y ; 
	                }
	                mArrColorballs.get(1).setY(mArrColorballs.get(1).getY() + dY);
	                mArrColorballs.get(2).setY(mArrColorballs.get(2).getY() + dY);
	                mArrColorballs.get(3).setY(mArrColorballs.get(3).getY() + dY);
	                mArrColorballs.get(0).setY(mArrColorballs.get(0).getY() + dY);
	                oldY = Y;
	            }
	            break;
	
	        case MotionEvent.ACTION_UP:
	            break;
        }
        invalidate();
        return true;
    }
    
    private boolean islegalScaleW(int X){
    	int width;
    	if(miBalId==2 || miBalId==3){
    		width = X - mArrColorballs.get(0).getX();
    	} else {
    		width = mArrColorballs.get(2).getX() - X;
    	}
    	if(width < miMinMotionZoneL) {
    		return false;
    	} 
    	return true;
    }
    
    private boolean islegalScaleH(int Y){
    	int height;
    	if(miBalId==0 || miBalId==3){
    		height = mArrColorballs.get(2).getY() - Y;
    	} else {
    		height = Y - mArrColorballs.get(0).getY();
    	}
    	
    	if(height < miMinMotionZoneL) {
    		return false;
    	} 
    	return true;
    }
    
    public void fullscreen(){
    	if (null == mPoints[0]) {
            Log.e(TAG, "fullscreen: mPoints are null");
    	} else {
	    	mPoints[0].x = mpThumbnailTopLeft.x;
	        mPoints[0].y = mpThumbnailTopLeft.y;
	
	        mPoints[1].x = mpThumbnailTopLeft.x;
	        mPoints[1].y = mpThumbnailBotRight.y;
	
	        mPoints[2].x = mpThumbnailBotRight.x;
	        mPoints[2].y = mpThumbnailBotRight.y;
	
	        mPoints[3].x = mpThumbnailBotRight.x;
	        mPoints[3].y = mpThumbnailTopLeft.y;
	            
	    	invalidate();
	    }
    }
    
    public double[] getNewRatio(){
    	double[] ratios = new double[4];
    	
    	if (null == mPoints[0]) {
            Log.e(TAG, "getNewRatio(): mPoints are null");
    	} else {
	        ratios[0] = (mPoints[0].x - mpThumbnailTopLeft.x)/(double)miThumbnailWidth;
	        ratios[1] = (mPoints[0].y - mpThumbnailTopLeft.y)/(double)miThumbnailHeight;
	        ratios[2] = (mPoints[2].x - mpThumbnailTopLeft.x)/(double)miThumbnailWidth;
	        ratios[3] = (mPoints[2].y - mpThumbnailTopLeft.y)/(double)miThumbnailHeight;
    	}
    	return ratios;
    }
    
    public boolean isChange(){
    	if(Arrays.equals(mPoints, mOldPoints)){
    		return false;
    	}
    	return true;
    }
    
    public void init(int viewWidth, int viewHeight, double[] r){
    	
    	float padding = getResources().getDimensionPixelSize(R.dimen.motion_zone_padding);

    	if(viewWidth>0 && viewHeight>0){
		    if((double)viewWidth/16.0 > (double)viewHeight/9.0){
		    	miThumbnailHeight = (int) (viewHeight - padding*2);
		    	miThumbnailWidth = (int) ((double)miThumbnailHeight/9.0*16);
		        
		       	mpThumbnailTopLeft.x= (int) ((viewWidth - miThumbnailWidth)/2);
		    	mpThumbnailTopLeft.y= (int) (padding);
		    	mpThumbnailBotRight.x= (int) (viewWidth - (viewWidth - miThumbnailWidth)/2);
		    	mpThumbnailBotRight.y= (int) (viewHeight - padding);
		    } else {
		    	miThumbnailWidth = (int) (viewWidth - padding*2);
		    	miThumbnailHeight = (int) ((double)viewWidth/16.0*9);
		    	
		       	mpThumbnailTopLeft.x= (int) (padding);
		    	mpThumbnailTopLeft.y= (int) ((viewHeight - miThumbnailHeight)/2);
		    	mpThumbnailBotRight.x= (int) (viewWidth - padding);
		    	mpThumbnailBotRight.y= (int) (viewHeight - (viewHeight - miThumbnailHeight)/2);
		    }
		    
		    miMinMotionZoneL = (int) ((double)(mpThumbnailBotRight.y - mpThumbnailTopLeft.y)*BeseyeMotionZoneUtil.sdMinZoneRatio);
		    setInitMotionZone(r[0], r[1], r[2], r[3]);
    	} 
    }
    
    private void setInitMotionZone(double leftR, double topR, double rightR, double botR){
    	//initialize rectangle.
        mPoints[0] = new Point();
        mPoints[0].x = (int) (mpThumbnailTopLeft.x + leftR*miThumbnailWidth);
        mPoints[0].y = (int) (mpThumbnailTopLeft.y + topR*miThumbnailHeight);

        mPoints[1] = new Point();
        mPoints[1].x = (int) (mpThumbnailTopLeft.x + leftR*miThumbnailWidth);
        mPoints[1].y = (int) (mpThumbnailTopLeft.y + botR*miThumbnailHeight);

        mPoints[2] = new Point();
        mPoints[2].x = (int) (mpThumbnailTopLeft.x + rightR*miThumbnailWidth);
        mPoints[2].y = (int) (mpThumbnailTopLeft.y + botR*miThumbnailHeight);

        mPoints[3] = new Point();
        mPoints[3].x = (int) (mpThumbnailTopLeft.x + rightR*miThumbnailWidth);
        mPoints[3].y = (int) (mpThumbnailTopLeft.y + topR*miThumbnailHeight);

        miBalId = 2;
        miGroupId = 1;
        for (int i=0; i<mPoints.length; i++) {
             mArrColorballs.add(new ColorBall(getContext(), mPoints[i], i));
             mOldPoints[i] = new Point();
             mOldPoints[i].x = mPoints[i].x;
             mOldPoints[i].y = mPoints[i].y;
        }
      
        invalidate();
    }

    public static class ColorBall {
        Bitmap mBitmap;
        Context mContext;
        Point mPoint;
        int miId;
        static int siCount = 0;

        public ColorBall(Context context, Point point, int id) {
            this.miId = id;
            mBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.motionzone_control_point_2);
            mContext = context;
            this.mPoint = point;
        }

        public int getWidthOfBall() {
            return mBitmap.getWidth();
        }

        public int getHeightOfBall() {
            return mBitmap.getHeight();
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public int getX() {
            return mPoint.x;
        }

        public int getY() {
            return mPoint.y;
        }

        public int getID() {
            return miId;
        }

        public void setX(int x) {
            mPoint.x = x;
        }

        public void setY(int y) {
            mPoint.y = y;
        }
    }
}
