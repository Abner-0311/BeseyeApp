package com.app.beseye.setting;


import static com.app.beseye.util.BeseyeConfig.DEBUG;
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
  	//point1 and point 3 are of same group and same as point 2 and point4
	Point[] points = new Point[4];
	Point pTopLeft = new Point();
    Point pBotRight = new Point();
    Point thumbnailTopLeft = new Point();
    Point thumbnailBotRight = new Point();
    
    int viewWidth = 0;
    int viewHeight = 0;
	
	int groupId = -1;
    private ArrayList<ColorBall> colorballs = new ArrayList<ColorBall>();
    // array that holds the balls
    private int balID = 0;
    // variable to know what ball is being dragged
    Paint paint, paint2;
    Canvas canvas;
    int oldX = -1;
    int oldY = -1;

    public MotionZoneEditView(Context context) {
        super(context);
        paint = new Paint();
        paint2 = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
        fullscreen();
    }

    public MotionZoneEditView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        fullscreen();
    }

    public MotionZoneEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint2 = new Paint();
        setFocusable(true); // necessary for getting the touch events
        canvas = new Canvas();
        fullscreen();
    }

    // the method that draws the balls
    @Override
    protected void onDraw(Canvas canvas) {
        if(points[3]==null) //point4 null when user did not touch and move on screen.
            return;
        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = left > points[i].x ? points[i].x:left;
            top = top > points[i].y ? points[i].y:top;
            right = right < points[i].x ? points[i].x:right;
            bottom = bottom < points[i].y ? points[i].y:bottom;
        }
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint2.setAntiAlias(true);
        paint2.setDither(true);
        paint2.setStrokeJoin(Paint.Join.ROUND);
//        paint.setStrokeWidth(5);
        
        //draw stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.parseColor("#00bbb3"));
        DisplayMetrics dm = getResources().getDisplayMetrics() ;
        float strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm);
        paint.setStrokeWidth(strokeWidth);
        
       // paint.setStrokeWidth(5);
        canvas.drawRect(
                    left + colorballs.get(0).getWidthOfBall() / 2,
                    top + colorballs.get(0).getWidthOfBall() / 2, 
                    right + colorballs.get(2).getWidthOfBall() / 2, 
                    bottom + colorballs.get(2).getWidthOfBall() / 2, paint);
     
        //fill the rectangle
        paint2.setStyle(Paint.Style.FILL);
        paint2.setColor(Color.parseColor("#000000"));
        paint2.setAlpha(153);
        paint2.setStrokeWidth(0);
////        canvas.drawRect(
////                left + colorballs.get(0).getWidthOfBall() / 2,
////                top + colorballs.get(0).getWidthOfBall() / 2, 
////                right + colorballs.get(2).getWidthOfBall() / 2, 
////                bottom + colorballs.get(2).getWidthOfBall() / 2, paint);
//        
        pTopLeft.y = top + colorballs.get(0).getWidthOfBall() / 2;
        pTopLeft.x = left + colorballs.get(0).getWidthOfBall() / 2;
   		pBotRight.y = bottom + colorballs.get(2).getWidthOfBall() / 2;
   		pBotRight.x = right + colorballs.get(2).getWidthOfBall() / 2;
//        
////        Rect topO = new Rect(0, 0, canvas.getWidth(), pTopLeft.y);
////        Rect leftO = new Rect(0, pTopLeft.y, pTopLeft.x, pBotRight.y);
////        Rect rightO = new Rect(pBotRight.x, pTopLeft.y, canvas.getWidth(), pBotRight.y);
////        Rect bottomO = new Rect(0, pBotRight.y, canvas.getWidth(), canvas.getHeight());
//        
        canvas.drawRect(0, 0, canvas.getWidth(), pTopLeft.y-strokeWidth/2, paint2);
        canvas.drawRect(0, pTopLeft.y-strokeWidth/2, pTopLeft.x-strokeWidth/2, pBotRight.y+strokeWidth/2, paint2);
        canvas.drawRect(pBotRight.x+strokeWidth/2, pTopLeft.y-strokeWidth/2, canvas.getWidth(), pBotRight.y+strokeWidth/2, paint2);
        canvas.drawRect(0, pBotRight.y+strokeWidth/2, canvas.getWidth(), canvas.getHeight(), paint2);
        
//        canvas.drawRect(leftO, paint);
//        canvas.drawRect(topO, paint);
//        canvas.drawRect(rightO, paint);
//        canvas.drawRect(bottomO, paint);
        
        //draw the corners
        // draw the balls on the canvas
        paint.setColor(Color.parseColor("#00bbb3"));
//      paint.setTextSize(18);
        paint.setStrokeWidth(0);
        for (int i =0; i < colorballs.size(); i ++) {
            ColorBall ball = colorballs.get(i);
            canvas.drawBitmap(ball.getBitmap(), ball.getX(), ball.getY(),
                    paint);

 //           canvas.drawText("" + (i+1), ball.getX(), ball.getY(), paint);
        }
    }

    // events when touching the screen
    public boolean onTouchEvent(MotionEvent event) {
        int eventaction = event.getAction();

        int X = (int) event.getX();
        int Y = (int) event.getY();
        int dX = 0, dY = 0;
        
        switch (eventaction) {

        case MotionEvent.ACTION_DOWN: // touch down so check if the finger is on
                                        // a ball
            if (points[0] == null) {
                //initialize rectangle.
                points[0] = new Point();
                points[0].x = X;
                points[0].y = Y;

                points[1] = new Point();
                points[1].x = X;
                points[1].y = Y + 30;

                points[2] = new Point();
                points[2].x = X + 30;
                points[2].y = Y + 30;

                points[3] = new Point();
                points[3].x = X +30;
                points[3].y = Y;

                balID = 2;
                groupId = 1;
                 // declare each ball with the ColorBall class
                for (Point pt : points) {
                     colorballs.add(new ColorBall(getContext(), R.drawable.motionzone_control_point_2, pt));
                }
            } else {
                //resize rectangle
                balID = -1;
                groupId = -1;
                for (int i = colorballs.size()-1; i>=0; i--) {
                    ColorBall ball = colorballs.get(i);
                    // check if inside the bounds of the ball (circle)
                    // get the center for the ball
                    int centerX = ball.getX() + ball.getWidthOfBall();
                    int centerY = ball.getY() + ball.getHeightOfBall();
                    
                    Log.v(TAG, "Kelly ball"+ball.getHeightOfBall());
      //              paint.setColor(Color.CYAN);
                    paint.setColor(Color.parseColor("#00bbb3"));
                    // calculate the radius from the touch to the center of the
                    // ball
                    double radCircle = Math
                            .sqrt((double) (((centerX - X) * (centerX - X)) + (centerY - Y)
                                    * (centerY - Y)));

                    if (radCircle < (ball.getWidthOfBall()*2)) {

                        balID = ball.getID();
                        if (balID == 1 || balID == 3) {
                            groupId = 2;
                        } else {
                            groupId = 1;
                        }
                        invalidate();
                        break;
                    }
//                    invalidate();
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
                // move the balls the same as the finger
                colorballs.get(balID).setX(X);
                colorballs.get(balID).setY(Y);

                //paint.setColor(Color.CYAN);
                paint.setColor(Color.parseColor("#00bbb3"));
                if (groupId == 1) {
                    colorballs.get(1).setX(colorballs.get(0).getX());
                    colorballs.get(1).setY(colorballs.get(2).getY());
                    colorballs.get(3).setX(colorballs.get(2).getX());
                    colorballs.get(3).setY(colorballs.get(0).getY());
                } else {
                    colorballs.get(0).setX(colorballs.get(1).getX());
                    colorballs.get(0).setY(colorballs.get(3).getY());
                    colorballs.get(2).setX(colorballs.get(3).getX());
                    colorballs.get(2).setY(colorballs.get(1).getY());
                }

                invalidate();
            } else {
            	//move dX, dY
            	dX = X - oldX;
            	dY = Y - oldY;
            	
              	Log.v(TAG, "Kelly " + X + " " + Y);
              	Log.v(TAG, "K fullsize " + viewWidth + " " + viewHeight);
            	
            	paint.setColor(Color.parseColor("#00bbb3"));
            	
            	colorballs.get(1).setX(colorballs.get(1).getX() + dX);
                colorballs.get(1).setY(colorballs.get(1).getY() + dY);
            	colorballs.get(2).setX(colorballs.get(2).getX() + dX);
                colorballs.get(2).setY(colorballs.get(2).getY() + dY);
            	colorballs.get(3).setX(colorballs.get(3).getX() + dX);
                colorballs.get(3).setY(colorballs.get(3).getY() + dY);
            	colorballs.get(0).setX(colorballs.get(0).getX() + dX);
                colorballs.get(0).setY(colorballs.get(0).getY() + dY);
                
                invalidate();
                
                oldX = X;
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

//    @Override
//    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
//            super.onSizeChanged(xNew, yNew, xOld, yOld);
//            viewWidth = xNew;
//            viewHeight = yNew;
//            /*
//            these viewWidth and viewHeight variables
//            are the global int variables
//            that were declared above
//            */
//    }
    
    public void fullscreen(){
    	if (points[0] == null) {
            //initialize rectangle.
            points[0] = new Point();
            points[0].x = thumbnailTopLeft.x;
            points[0].y = thumbnailTopLeft.y;

            points[1] = new Point();
            points[1].x = thumbnailTopLeft.x;
            points[1].y = thumbnailBotRight.y;

            points[2] = new Point();
            points[2].x = thumbnailBotRight.x;
            points[2].y = thumbnailBotRight.y;

            points[3] = new Point();
            points[3].x = thumbnailBotRight.x;
            points[3].y = thumbnailTopLeft.y;

            balID = 2;
            groupId = 1;
             // declare each ball with the ColorBall class
            for (Point pt : points) {
                 colorballs.add(new ColorBall(getContext(), R.drawable.motionzone_control_point_2, pt));
            }
        } else {
        	points[0].x = thumbnailTopLeft.x;
            points[0].y = thumbnailTopLeft.y;

            points[1].x = thumbnailTopLeft.x;
            points[1].y = thumbnailBotRight.y;

            points[2].x = thumbnailBotRight.x;
            points[2].y = thumbnailBotRight.y;

            points[3].x = thumbnailBotRight.x;
            points[3].y = thumbnailTopLeft.y;

        }
    	invalidate();
    }
    
    public void setImageBoundary(int ThumbnailWidth, int ThumbnailHeight){
    	DisplayMetrics dm = getResources().getDisplayMetrics() ;
        //float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 25, dm);
    	float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
    			this.getResources().getDimension(R.dimen.motion_zone_padding)/ getResources().getDisplayMetrics().density, dm);
        
    	
        int width = (int) ((ThumbnailHeight - padding*2)/9*16);
        
       	thumbnailTopLeft.x= (int) ((ThumbnailWidth - width)/2 -21);
    	thumbnailTopLeft.y= (int) (padding-21);
    	thumbnailBotRight.x= (int) (ThumbnailWidth - (ThumbnailWidth - width)/2-21);
    	thumbnailBotRight.y= (int) (ThumbnailHeight - padding-21);
    	
    	Log.v(TAG, "Kelly image width "+ width);
    	Log.v(TAG, "Kelly padding " + padding);
    	Log.v(TAG, "Kelly view " + viewWidth + " " + viewHeight);
    	
    	Log.v(TAG, "Kelly x y " + thumbnailTopLeft.x + " "+ thumbnailTopLeft.y);
    	Log.v(TAG, "Kelly x 2 " + thumbnailBotRight.x+ " " + thumbnailBotRight.y);
    	
    }

    public static class ColorBall {

        Bitmap bitmap;
        Context mContext;
        Point point;
        int id;
        static int count = 0;

        public ColorBall(Context context, int resourceId, Point point) {
            this.id = count++;
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
