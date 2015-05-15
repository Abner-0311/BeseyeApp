package com.app.beseye.setting;

import static com.app.beseye.util.BeseyeConfig.TAG;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class MotionZoneEditView extends View {
  	// point1 and point 3 are of same group and same as point 2 and point4
	private Point[] points = new Point[4];
    
	// position of image
	private Point thumbnailTopLeft = new Point();
	private Point thumbnailBotRight = new Point();
	private int thumbnailWidth = 0;
	private int thumbnailHeight = 0;
	private int minMotionZoneL = 0; 	// (1/5) of thumbnailHeight;
    
    // boundary of motion zone
    // int left, top, right, bottom;
    
	private int groupId = -1;
    private ArrayList<ColorBall> colorballs = new ArrayList<ColorBall>();
    
    // array that holds the balls
    private int balID = 0;
    
    // variable to know what ball is being dragged
    private Paint linePaint, rectPaint;
    private Canvas canvas;
    private int oldX = -1;
    private int oldY = -1;
    
    DisplayMetrics dm = getResources().getDisplayMetrics() ;
    float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm);

    public MotionZoneEditView(Context context) {
        super(context);
        linePaint = new Paint();
        rectPaint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    public MotionZoneEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MotionZoneEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint = new Paint();
        rectPaint = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {
        if(points[3]==null) //point4 null when user did not touch and move on screen.
            return;
        
        linePaint.setAntiAlias(true);
        linePaint.setDither(true);			
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        rectPaint.setAntiAlias(true);
        rectPaint.setDither(true);
        rectPaint.setStrokeJoin(Paint.Join.ROUND);
        
        // draw line
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setColor(Color.parseColor("#00bbb3"));
        linePaint.setStrokeWidth(strokeWidth);
        
        canvas.drawRect(points[0].x, points[0].y, points[2].x, points[2].y, linePaint);
     
        // fill the rectangle
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(Color.parseColor("#000000"));
        rectPaint.setAlpha(153);	//255*0.6
        rectPaint.setStrokeWidth(0);

        canvas.drawRect(thumbnailTopLeft.x, 
        				thumbnailTopLeft.y, 
        				thumbnailBotRight.x, 
        				points[0].y-strokeWidth/2 <= thumbnailTopLeft.y ? thumbnailTopLeft.y :points[0].y-strokeWidth/2, rectPaint);
       
        canvas.drawRect(thumbnailTopLeft.x, 
        				points[0].y-strokeWidth/2 <= thumbnailTopLeft.y ? thumbnailTopLeft.y :points[0].y-strokeWidth/2, 
        				points[0].x-strokeWidth/2 <= thumbnailTopLeft.x ? thumbnailTopLeft.x :points[0].x-strokeWidth/2, 
        				points[2].y+strokeWidth/2 >= thumbnailBotRight.y? thumbnailBotRight.y:points[2].y+strokeWidth/2, rectPaint);
     
        canvas.drawRect(points[2].x+strokeWidth/2 >= thumbnailBotRight.x? thumbnailBotRight.x:points[2].x+strokeWidth/2, 
        				points[0].y-strokeWidth/2 <= thumbnailTopLeft.y ? thumbnailTopLeft.y :points[0].y-strokeWidth/2, 
        				thumbnailBotRight.x, 
        				points[2].y+strokeWidth/2 >= thumbnailBotRight.y? thumbnailBotRight.y:points[2].y+strokeWidth/2, rectPaint);
        
        canvas.drawRect(thumbnailTopLeft.x, 
        				points[2].y+strokeWidth/2 >= thumbnailBotRight.y? thumbnailBotRight.y:points[2].y+strokeWidth/2, 
        				thumbnailBotRight.x, 
        				thumbnailBotRight.y, rectPaint);
        
        // draw the balls on the canvas, reuse linePaint for ball
        linePaint.setColor(Color.parseColor("#00bbb3"));
        linePaint.setStrokeWidth(0);
        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), ball.getX()-ball.getWidthOfBall()/2, ball.getY()-ball.getHeightOfBall()/2, linePaint);
        }
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();
        int dX = 0, dY = 0;
        
        switch (eventaction) {

	        case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on a ball
	            if (points[0] == null) {
	               Log.v(TAG, "points are null");
	            } else {
	                // resize rectangle
	                balID = -1;
	                groupId = -1;
	                for (int i = colorballs.size()-1; i>=0; i--) {
	                    ColorBall ball = colorballs.get(i);
	                             
	                    // check if inside the bounds of the ball (circle)
	                    // calculate the radius from the touch to the center of the ball
	                    double radCircle = Math
	                            .sqrt((double) (((ball.getX() - X) * (ball.getX() - X)) 
	                            		+ (ball.getY() - Y) * (ball.getY() - Y)));
	
	                    if (radCircle < (ball.getWidthOfBall()*1.5)) {
	
	                        balID = ball.getID();
	                        if (balID == 1 || balID == 3) {
	                            groupId = 2;
	                        } else {
	                            groupId = 1;
	                        }
	                        invalidate();
	                        break;
	                    }
	                } 
	                if(-1 == balID){
	                	// move rectangle
	                	oldX = X;
	                	oldY = Y;
	                }
	                invalidate();
	            }
	            break;
	
	        case MotionEvent.ACTION_MOVE: // touch drag with the ball
	
	            if (balID > -1) {
	            	X = X > thumbnailBotRight.x? thumbnailBotRight.x : X;
            		X = X < thumbnailTopLeft.x? thumbnailTopLeft.x : X;
            		Y = Y > thumbnailBotRight.y? thumbnailBotRight.y : Y;
            		Y = Y < thumbnailTopLeft.y? thumbnailTopLeft.y : Y;
                	
	                // move the balls the same as the finger
	            	if(true == islegalScaleW(X)) {
		                colorballs.get(balID).setX(X);
		                if (groupId == 1) {
		                    colorballs.get(1).setX(colorballs.get(0).getX());
		                    colorballs.get(3).setX(colorballs.get(2).getX());
		                } else {
		                    colorballs.get(0).setX(colorballs.get(1).getX());
		                    colorballs.get(2).setX(colorballs.get(3).getX());
		                }
	            	}
	            	if(true == islegalScaleH(Y)) {
	                    colorballs.get(balID).setY(Y);
		                if (groupId == 1) {
		                    colorballs.get(1).setY(colorballs.get(2).getY());
		                    colorballs.get(3).setY(colorballs.get(0).getY());
		                } else {
		                    colorballs.get(0).setY(colorballs.get(3).getY());
		                    colorballs.get(2).setY(colorballs.get(1).getY());
		                }
	            	}
	            } else {
	            	// move dX, dY
	            	dX = X - oldX;
	            	dY = Y - oldY;

	            	if(points[2].x  + dX > thumbnailBotRight.x) {
	                	dX = thumbnailBotRight.x - points[2].x ;
	                }
	                if(points[0].x  + dX < thumbnailTopLeft.x) {
	                	dX = thumbnailTopLeft.x - points[0].x ; 
	                }
                	colorballs.get(1).setX(colorballs.get(1).getX() + dX);
	                colorballs.get(2).setX(colorballs.get(2).getX() + dX);
	            	colorballs.get(3).setX(colorballs.get(3).getX() + dX);
	            	colorballs.get(0).setX(colorballs.get(0).getX() + dX);
	                oldX = X;

	                if(points[2].y  + dY > thumbnailBotRight.y) {
	                	dY = thumbnailBotRight.y - points[2].y ;
	                }
	                if(points[0].y  + dY < thumbnailTopLeft.y) {
	                	dY = thumbnailTopLeft.y - points[0].y ; 
	                }
	                colorballs.get(1).setY(colorballs.get(1).getY() + dY);
	                colorballs.get(2).setY(colorballs.get(2).getY() + dY);
	                colorballs.get(3).setY(colorballs.get(3).getY() + dY);
	                colorballs.get(0).setY(colorballs.get(0).getY() + dY);
	                oldY = Y;
	             
	            }
	            break;
	
	        case MotionEvent.ACTION_UP:
	            // touch drop - just do things here after dropping
	            break;
        }
        // redraw the canvas
        invalidate();
        return true;
    }
    
    private boolean islegalScaleW(int X){
    	int width;
    	if(balID==2 || balID==3){
    		width = X - colorballs.get(0).getX();
    	} else {
    		width = colorballs.get(2).getX() - X;
    	}
    	if(width < minMotionZoneL) {
    		return false;
    	} 
    	return true;
    }
    private boolean islegalScaleH(int Y){
    	int height;
    	if(balID==0 || balID==3){
    		height = colorballs.get(2).getY() - Y;
    	} else {
    		height = Y - colorballs.get(0).getY();
    	}
    	
    	if(height < minMotionZoneL) {
    		return false;
    	} 
    	return true;
    }
    
    public void fullscreen(){
    	points[0].x = thumbnailTopLeft.x;
        points[0].y = thumbnailTopLeft.y;

        points[1].x = thumbnailTopLeft.x;
        points[1].y = thumbnailBotRight.y;

        points[2].x = thumbnailBotRight.x;
        points[2].y = thumbnailBotRight.y;

        points[3].x = thumbnailBotRight.x;
        points[3].y = thumbnailTopLeft.y;
            
    	invalidate();
    }
    
    public double[] getNewRatio(){
    	double[] ratios = new double[4];
   
        ratios[0] = (points[0].x - thumbnailTopLeft.x)/(double)thumbnailWidth;
        ratios[1] = (points[0].y - thumbnailTopLeft.y)/(double)thumbnailHeight;
        ratios[2] = (points[2].x - thumbnailTopLeft.x)/(double)thumbnailWidth;
        ratios[3] = (points[2].y - thumbnailTopLeft.y)/(double)thumbnailHeight;
 
    	return ratios;
    }
    
    public void Init(int viewWidth, int viewHeight, double[] r){
    	DisplayMetrics dm = getResources().getDisplayMetrics() ;
    	float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
    			this.getResources().getDimension(R.dimen.motion_zone_padding)/ getResources().getDisplayMetrics().density, dm);
        
    	thumbnailHeight = (int) (viewHeight - padding*2);
    	thumbnailWidth = (int) ((double)thumbnailHeight/9.0*16);
        
       	thumbnailTopLeft.x= (int) ((viewWidth - thumbnailWidth)/2);
    	thumbnailTopLeft.y= (int) (padding);
    	thumbnailBotRight.x= (int) (viewWidth - (viewWidth - thumbnailWidth)/2);
    	thumbnailBotRight.y= (int) (viewHeight - padding);
    	
    	minMotionZoneL = (int) ((double)(viewHeight - padding*2)/5.0);
    		
    	setInitMotionZone(r[0], r[1], r[2], r[3]);
    }
    
    private void setInitMotionZone(double leftR, double topR, double rightR, double botR){
    	//initialize rectangle.
        points[0] = new Point();
        points[0].x = (int) (thumbnailTopLeft.x + leftR*thumbnailWidth);
        points[0].y = (int) (thumbnailTopLeft.y + topR*thumbnailHeight);

        points[1] = new Point();
        points[1].x = (int) (thumbnailTopLeft.x + leftR*thumbnailWidth);
        points[1].y = (int) (thumbnailTopLeft.y + botR*thumbnailHeight);

        points[2] = new Point();
        points[2].x = (int) (thumbnailTopLeft.x + rightR*thumbnailWidth);
        points[2].y = (int) (thumbnailTopLeft.y + botR*thumbnailHeight);

        points[3] = new Point();
        points[3].x = (int) (thumbnailTopLeft.x + rightR*thumbnailWidth);
        points[3].y = (int) (thumbnailTopLeft.y + topR*thumbnailHeight);

        balID = 2;
        groupId = 1;
        int id = 0;
         // declare each ball with the ColorBall class
        for (Point pt : points) {
             colorballs.add(new ColorBall(getContext(), pt, id));
             id++;
        }
        
        invalidate();
    }

    public static class ColorBall {

        Bitmap bitmap;
        Context mContext;
        Point point;
        int id;
        static int count = 0;

        public ColorBall(Context context, Point point, int id) {
            this.id = id;
            bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.motionzone_control_point_2);
            mContext = context;
            this.point = point;
        }

        public int getWidthOfBall() {
            return bitmap.getWidth();
        }

        public int getHeightOfBall() {
            return bitmap.getHeight();
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public int getX() {
            return point.x;
        }

        public int getY() {
            return point.y;
        }

        public int getID() {
            return id;
        }

        public void setX(int x) {
            point.x = x;
        }

        public void setY(int y) {
            point.y = y;
        }
    }
}
