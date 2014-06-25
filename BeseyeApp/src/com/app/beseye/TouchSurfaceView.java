package com.app.beseye;

import static com.app.beseye.TouchSurfaceView.State.*;
import static com.app.beseye.util.BeseyeConfig.*;

import com.app.beseye.util.BeseyeUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Scroller;

public class TouchSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

	protected static float mSurfaceWidth, mSurfaceHeight;
	
	//
	// SuperMin and SuperMax multipliers. Determine how much the image can be
	// zoomed below or above the zoom boundaries, before animating back to the
	// min/max zoom boundary.
	//
	private static final float SUPER_MIN_MULTIPLIER = .75f;
	private static final float SUPER_MAX_MULTIPLIER = 1.25f;

    //
    // Scale of image ranges from minScale to maxScale, where minScale == 1
    // when the image is stretched to fit view.
    //
    private float normalizedScale;
    
    //
    // Matrix applied to image. MSCALE_X and MSCALE_Y should always be equal.
    // MTRANS_X and MTRANS_Y are the other values used. prevMatrix is the matrix
    // saved prior to the screen rotating.
    //
	private Matrix matrix, prevMatrix;

    public static enum State { NONE, DRAG, ZOOM, FLING, ANIMATE_ZOOM };
    private State state;

    private float minScale;
    private float maxScale;
    private float superMinScale;
    private float superMaxScale;
    private float[] m;
    
    private Context context;
    private Fling fling;

    //
    // Size of view and previous view size (ie before rotation)
    //
    private int viewWidth, viewHeight, prevViewWidth, prevViewHeight;
    
    private int displayWidth, displayHeight;
    
    //
    // Size of image when it is stretched to fit view. Before and After rotation.
    //
    private float matchViewWidth, matchViewHeight, prevMatchViewWidth, prevMatchViewHeight;
    
    private float mfPaddingTop, mfPaddingBottom;
    
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    
    public static boolean mIsPaused = false, mIsSurfaceReady = false, mHasFocus = true;
    
    // This is what SDL runs in. It invokes SDL_main(), eventually
    protected static Thread mSDLThread;

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.i(TAG, "surfaceCreated()......");
		holder.setType(SurfaceHolder.SURFACE_TYPE_GPU);
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.i(TAG, "surfaceChanged()......");
        int sdlFormat = 0x15151002; // SDL_PIXELFORMAT_RGB565 by default
        switch (format) {
        case PixelFormat.A_8:
            Log.v(TAG, "pixel format A_8");
            break;
        case PixelFormat.LA_88:
            Log.v(TAG, "pixel format LA_88");
            break;
        case PixelFormat.L_8:
            Log.v(TAG, "pixel format L_8");
            break;
        case PixelFormat.RGBA_4444:
            Log.v(TAG, "pixel format RGBA_4444");
            sdlFormat = 0x15421002; // SDL_PIXELFORMAT_RGBA4444
            break;
        case PixelFormat.RGBA_5551:
            Log.v(TAG, "pixel format RGBA_5551");
            sdlFormat = 0x15441002; // SDL_PIXELFORMAT_RGBA5551
            break;
        case PixelFormat.RGBA_8888:
            Log.v(TAG, "pixel format RGBA_8888");
            sdlFormat = 0x16462004; // SDL_PIXELFORMAT_RGBA8888
            break;
        case PixelFormat.RGBX_8888:
            Log.v(TAG, "pixel format RGBX_8888");
            sdlFormat = 0x16261804; // SDL_PIXELFORMAT_RGBX8888
            break;
        case PixelFormat.RGB_332:
            Log.v(TAG, "pixel format RGB_332");
            sdlFormat = 0x14110801; // SDL_PIXELFORMAT_RGB332
            break;
        case PixelFormat.RGB_565:
            Log.v(TAG, "pixel format RGB_565");
            sdlFormat = 0x15151002; // SDL_PIXELFORMAT_RGB565
            break;
        case PixelFormat.RGB_888:
            Log.v(TAG, "pixel format RGB_888");
            // Not sure this is right, maybe SDL_PIXELFORMAT_RGB24 instead?
            sdlFormat = 0x16161804; // SDL_PIXELFORMAT_RGB888
            break;
        default:
            Log.v(TAG, "pixel format unknown " + format);
            break;
        }

        mSurfaceWidth = width;
        mSurfaceHeight = height;
        
        if(((float)mSurfaceWidth/mSurfaceHeight) > 16.0/9/0){
        	displayHeight = (int) mSurfaceHeight;
        	displayWidth = (int) ((displayHeight/9.0)*16.0);
        }else{
        	displayWidth = (int) mSurfaceWidth;
        	displayHeight = (int) ((displayWidth/16.0)*9.0);
        }
        
        Log.d(TAG, "surfaceChanged(), mWidth:"+mSurfaceWidth+", mHeight:"+mSurfaceHeight+", displayWidth:"+displayWidth+", displayHeight:"+displayHeight);
        //SDLActivity.onNativeResize(width, height, sdlFormat);
        Log.v(TAG, "Window size:" + width + "x"+height);
        
        mfPaddingTop = this.getContext().getResources().getDimension(R.dimen.liveview_navbar_height_landscape);
        mfPaddingBottom = this.getContext().getResources().getDimension(R.dimen.liveview_toolbar_height_landscape);

        // Set mIsSurfaceReady to 'true' *before* making a call to handleResume
        TouchSurfaceView.mIsSurfaceReady = true;
        //SDLActivity.onNativeSurfaceChanged();
        drawDefaultBackground();
        
        BeseyeUtils.postRunnable(new Runnable(){
			@Override
			public void run() {
				Log.i(TAG, "Trigger onMeasure()......, matrix="+matrix);
				//fixScaleTrans();
				prevMatrix.setValues(m);
		        prevMatchViewHeight = matchViewHeight;
		        prevMatchViewWidth = matchViewWidth;
		        prevViewHeight = viewHeight;
		        prevViewWidth = viewWidth;
				TouchSurfaceView.this.measure(MeasureSpec.makeMeasureSpec((int)mSurfaceHeight, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int)mSurfaceHeight, MeasureSpec.EXACTLY));
				TouchSurfaceView.this.measure(MeasureSpec.makeMeasureSpec((int)mSurfaceWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int)mSurfaceHeight, MeasureSpec.EXACTLY));
				drawStreamBitmap();
			}}, 0);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		TouchSurfaceView.mIsSurfaceReady = false;
		Log.i(TAG, "surfaceDestroyed()......");
	}
	
	static{
		if (TouchSurfaceView.mSDLThread == null) {
            // This is the entry point to the C app.
            // Start up the C app thread and enable sensor input for the first time

        	TouchSurfaceView.mSDLThread = new Thread(new SDLMain(), "SDLThread");
            //enableSensor(Sensor.TYPE_ACCELEROMETER, true);
        	TouchSurfaceView.mSDLThread.start();
        }
	}
	/**
	    Simple nativeInit() runnable
	*/
	static class SDLMain implements Runnable {
	    @Override
	    public void run() {
	    	Log.i(TAG, "nativeInit in");
	        // Runs SDL_main()
	    	CameraViewActivity.nativeInit();
	
	    	Log.i(TAG, "nativeInit out");
	        //Log.v(TAG, "SDL thread terminated");
	    }
	}
	
	private int miBackgroundColor;
    
	public TouchSurfaceView(Context context) {
		super(context);
		sharedConstructing(context);
	}

	public TouchSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		sharedConstructing(context);
	}

	public TouchSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		sharedConstructing(context);
	}
	
    private void sharedConstructing(Context context) {
    	Log.i(TAG, "sharedConstructing()");
        super.setClickable(true);
        getHolder().addCallback(this);	
        
        this.context = context;
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        matrix = new Matrix();
        prevMatrix = new Matrix();
        m = new float[9];
        normalizedScale = 1;
        minScale = 1;
        maxScale = 4;
        superMinScale = SUPER_MIN_MULTIPLIER * minScale;
        superMaxScale = SUPER_MAX_MULTIPLIER * maxScale;
        drawStreamBitmap();
        //setImageMatrix(matrix);
        //setScaleType(ScaleType.MATRIX);
        setState(NONE);
        setOnTouchListener(new TouchImageViewListener());
        
        miBackgroundColor = context.getResources().getColor(R.color.liveview_background);
    }
    
    @Override
    public Parcelable onSaveInstanceState() {
		Bundle bundle = new Bundle();
		bundle.putParcelable("instanceState", super.onSaveInstanceState());
		bundle.putFloat("saveScale", normalizedScale);
		bundle.putFloat("matchViewHeight", matchViewHeight);
		bundle.putFloat("matchViewWidth", matchViewWidth);
		bundle.putInt("viewWidth", viewWidth);
		bundle.putInt("viewHeight", viewHeight);
		matrix.getValues(m);
		bundle.putFloatArray("matrix", m);
		Log.i(TAG, "onSaveInstanceState(), matrix="+matrix);
		return bundle;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
      	if (state instanceof Bundle) {
	        Bundle bundle = (Bundle) state;
	        normalizedScale = bundle.getFloat("saveScale");
	        m = bundle.getFloatArray("matrix");
	        prevMatrix.setValues(m);
	        prevMatchViewHeight = bundle.getFloat("matchViewHeight");
	        prevMatchViewWidth = bundle.getFloat("matchViewWidth");
	        prevViewHeight = bundle.getInt("viewHeight");
	        prevViewWidth = bundle.getInt("viewWidth");
	        super.onRestoreInstanceState(bundle.getParcelable("instanceState"));
	        Log.i(TAG, "onRestoreInstanceState(), prevMatrix="+prevMatrix);
	        return;
      	}

      	super.onRestoreInstanceState(state);
    }
    
    public void onOrientationChanged(){
    	restoreDisplayAttr();
    }
    
    private void restoreDisplayAttr(){
    	
    	fixScaleTrans();
    	prevMatrix.setValues(m);
        prevMatchViewHeight = matchViewHeight;
        prevMatchViewWidth = matchViewWidth;
        prevViewHeight = viewHeight;
        prevViewWidth = viewWidth;
        Log.i(TAG, "restoreDisplayAttr()....., prevMatrix="+prevMatrix);
        drawStreamBitmap();
    }

    /**
     * Set the max zoom multiplier. Default value: 3.
     * @param max max zoom multiplier.
     */
    public void setMaxZoom(float max) {
        maxScale = max;
        superMaxScale = SUPER_MAX_MULTIPLIER * maxScale;
    }
    
    /**
     * Set the min zoom multiplier. Default value: 1.
     * @param min min zoom multiplier.
     */
    public void setMinZoom(float min) {
    	minScale = min;
    	superMinScale = SUPER_MIN_MULTIPLIER * minScale;
    }
    
    /**
     * For a given point on the view (ie, a touch event), returns the
     * point relative to the original drawable's coordinate system.
     * @param x
     * @param y
     * @return PointF relative to original drawable's coordinate system.
     */
    public PointF getDrawablePointFromTouchPoint(float x, float y) {
    	return transformCoordTouchToBitmap(x, y, true);
    }
    
    /**
     * For a given point on the view (ie, a touch event), returns the
     * point relative to the original drawable's coordinate system.
     * @param p
     * @return PointF relative to original drawable's coordinate system.
     */
    public PointF getDrawablePointFromTouchPoint(PointF p) {
    	return transformCoordTouchToBitmap(p.x, p.y, true);
    }
    
    /**
     * Performs boundary checking and fixes the image matrix if it 
     * is out of bounds.
     */
    private void fixTrans() {
        matrix.getValues(m);
        float transX = m[Matrix.MTRANS_X];
        float transY = m[Matrix.MTRANS_Y];
        
        float fixTransX = getFixTrans(transX, viewWidth, getImageWidth());
        float fixTransY = getFixTrans(transY, viewHeight, getImageHeight());
        
        if (fixTransX != 0 || fixTransY != 0) {
        	Log.i(TAG, "fixTrans()....., call postTranslate, fixTransX="+fixTransX);
            matrix.postTranslate(fixTransX, fixTransY);
        }
    }
    
    /**
     * When transitioning from zooming from focus to zoom from center (or vice versa)
     * the image can become unaligned within the view. This is apparent when zooming
     * quickly. When the content size is less than the view size, the content will often
     * be centered incorrectly within the view. fixScaleTrans first calls fixTrans() and 
     * then makes sure the image is centered correctly within the view.
     */
    private void fixScaleTrans() {
    	fixTrans();
    	matrix.getValues(m);
    	if (getImageWidth() < viewWidth) {
    		m[Matrix.MTRANS_X] = (viewWidth - getImageWidth()) / 2;
    	}
    	
    	if (getImageHeight() < viewHeight) {
    		m[Matrix.MTRANS_Y] = (viewHeight - getImageHeight()) / 2;
    	}
    	matrix.setValues(m);
    }

    private float getFixTrans(float trans, float viewSize, float contentSize) {
        float minTrans, maxTrans;

        if (contentSize <= viewSize) {
            minTrans = 0;
            maxTrans = viewSize - contentSize;
            
        } else {
            minTrans = viewSize - contentSize;
            maxTrans = 0;
        }

        if (trans < minTrans)
            return -trans + minTrans;
        if (trans > maxTrans)
            return -trans + maxTrans;
        return 0;
    }
    
    private float getFixDragTrans(float delta, float viewSize, float contentSize) {
        if (contentSize <= viewSize) {
            return 0;
        }
        return delta;
    }
    
    private float getImageWidth() {
    	return matchViewWidth * normalizedScale;
    }
    
    private float getImageHeight() {
    	return matchViewHeight * normalizedScale;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Surface drawable = getNativeSurface();//getDrawable();
//        if (mStreamBitmap == null || mStreamBitmap.getWidth() == 0 || mStreamBitmap.getHeight() == 0) {
//        	super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        	return;
//        }
        
        int drawableWidth = 1280;//null != mStreamBitmap ?mStreamBitmap.getWidth():0;
        int drawableHeight = 720;//null != mStreamBitmap ? mStreamBitmap.getHeight():0;
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        viewWidth = setViewSize(widthMode, widthSize, drawableWidth);
        viewHeight = setViewSize(heightMode, heightSize, drawableHeight);
        
        //
        // Set view dimensions
        //
        setMeasuredDimension(viewWidth, viewHeight);
        
        //
    	// Scale image for view
    	//
        float scaleX = (float) viewWidth / drawableWidth;
        float scaleY = (float) viewHeight / drawableHeight;
        float scale = Math.min(scaleX, scaleY);

        //
        // Center the image
        //
        float redundantYSpace = viewHeight - (scale * drawableHeight);
        float redundantXSpace = viewWidth - (scale * drawableWidth);
        matchViewWidth = viewWidth - redundantXSpace;
        matchViewHeight = viewHeight - redundantYSpace;
        
        if (normalizedScale == 1) {
        	//
        	// Stretch and center image to fit view
        	//
        	matrix.setScale(scale, scale);
        	matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);
        	
        } else {
        	prevMatrix.getValues(m);
        	
        	//
        	// Rescale Matrix after rotation
        	//
        	m[Matrix.MSCALE_X] = matchViewWidth / drawableWidth * normalizedScale;
        	m[Matrix.MSCALE_Y] = matchViewHeight / drawableHeight * normalizedScale;
        	
        	//
        	// TransX and TransY from previous matrix
        	//
            float transX = m[Matrix.MTRANS_X];
            float transY = m[Matrix.MTRANS_Y];
            
            //
            // Width
            //
            float prevActualWidth = prevMatchViewWidth * normalizedScale;
            float actualWidth = getImageWidth();
            translateMatrixAfterRotate(Matrix.MTRANS_X, transX, prevActualWidth, actualWidth, prevViewWidth, viewWidth, drawableWidth);
            
            //
            // Height
            //
            float prevActualHeight = prevMatchViewHeight * normalizedScale;
            float actualHeight = getImageHeight();
            translateMatrixAfterRotate(Matrix.MTRANS_Y, transY, prevActualHeight, actualHeight, prevViewHeight, viewHeight, drawableHeight);
            
            //
            // Set the matrix to the adjusted scale and translate values.
            //
            matrix.setValues(m);
        }
        fixTrans();
        Log.i(TAG, "onMeasure(), matrix:"+matrix.toString()+", normalizedScale="+normalizedScale+", matchViewWidth="+matchViewWidth+", matchViewHeight="+matchViewHeight);
        //setImageMatrix(matrix);
        drawStreamBitmap();
    }
    
    /**
     * Set view dimensions based on layout params
     * 
     * @param mode 
     * @param size
     * @param drawableWidth
     * @return
     */
    private int setViewSize(int mode, int size, int drawableWidth) {
    	int viewSize;
    	switch (mode) {
		case MeasureSpec.EXACTLY:
			viewSize = size;
			break;
			
		case MeasureSpec.AT_MOST:
			viewSize = Math.min(drawableWidth, size);
			break;
			
		case MeasureSpec.UNSPECIFIED:
			viewSize = drawableWidth;
			break;
			
		default:
			viewSize = size;
		 	break;
		}
    	return viewSize;
    }
    
    /**
     * After rotating, the matrix needs to be translated. This function finds the area of image 
     * which was previously centered and adjusts translations so that is again the center, post-rotation.
     * 
     * @param axis Matrix.MTRANS_X or Matrix.MTRANS_Y
     * @param trans the value of trans in that axis before the rotation
     * @param prevImageSize the width/height of the image before the rotation
     * @param imageSize width/height of the image after rotation
     * @param prevViewSize width/height of view before rotation
     * @param viewSize width/height of view after rotation
     * @param drawableSize width/height of drawable
     */
    private void translateMatrixAfterRotate(int axis, float trans, float prevImageSize, float imageSize, int prevViewSize, int viewSize, int drawableSize) {
    	if (imageSize < viewSize) {
        	//
        	// The width/height of image is less than the view's width/height. Center it.
        	//
        	m[axis] = (viewSize - (drawableSize * m[Matrix.MSCALE_X])) * 0.5f;
        	
        } else if (trans > 0) {
        	//
        	// The image is larger than the view, but was not before rotation. Center it.
        	//
        	m[axis] = -((imageSize - viewSize) * 0.5f);
        	
        } else {
        	//
        	// Find the area of the image which was previously centered in the view. Determine its distance
        	// from the left/top side of the view as a fraction of the entire image's width/height. Use that percentage
        	// to calculate the trans in the new view width/height.
        	//
        	float percentage = (Math.abs(trans) + (0.5f * prevViewSize)) / prevImageSize;
        	m[axis] = -((percentage * imageSize) - (viewSize * 0.5f));
        }
    }
    
    private void setState(State state) {
    	this.state = state;
    }
    
    /**
     * Gesture Listener detects a single click or long click and passes that on
     * to the view's listener.
     * @author Ortiz
     *
     */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
    	
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e)
        {
        	//Log.i(TAG, "onSingleTapConfirmed()");
        	triggermSingleTapCallback();
        	return performClick();
        }
        
        @Override
        public void onLongPress(MotionEvent e)
        {
        	Log.i(TAG, "onLongPress()");
        	performLongClick();
        }
        
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
        	Log.i(TAG, "onFling()");
        	if (fling != null) {
        		//
        		// If a previous fling is still active, it should be cancelled so that two flings
        		// are not run simultaenously.
        		//
        		fling.cancelFling();
        	}
        	fling = new Fling((int) velocityX, (int) velocityY);
        	compatPostOnAnimation(fling);
        	return super.onFling(e1, e2, velocityX, velocityY);
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
        	Log.i(TAG, "onDoubleTap()");
        	boolean consumed = false;
        	if (state == NONE) {
        		triggermDoubleTapCallback();
	        	float targetZoom = (normalizedScale == minScale) ? maxScale : minScale;
	        	DoubleTapZoom doubleTap = new DoubleTapZoom(normalizedScale, targetZoom, e.getX(), e.getY(), false);
	        	compatPostOnAnimation(doubleTap);
	        	consumed = true;
        	}
        	return consumed;
        }
    }
    
    /**
     * Responsible for all touch events. Handles the heavy lifting of drag and also sends
     * touch events to Scale Detector and Gesture Detector.
     * @author Ortiz
     *
     */
    private class TouchImageViewListener implements OnTouchListener {
    	
    	//
        // Remember last point position for dragging
        //
        private PointF last = new PointF();
    	
    	@Override
        public boolean onTouch(View v, MotionEvent event) {
            mScaleDetector.onTouchEvent(event);
            mGestureDetector.onTouchEvent(event);
            PointF curr = new PointF(event.getX(), event.getY());
            
            if (state == NONE || state == DRAG || state == FLING) {
	            switch (event.getAction()) {
	                case MotionEvent.ACTION_DOWN:
	                	last.set(curr);
	                    if (fling != null)
	                    	fling.cancelFling();
	                    setState(DRAG);
	                    break;
	                    
	                case MotionEvent.ACTION_MOVE:
	                    if (state == DRAG) {
	                        float deltaX = curr.x - last.x;
	                        float deltaY = curr.y - last.y;
	                        float fixTransX = getFixDragTrans(deltaX, viewWidth, getImageWidth());
	                        float fixTransY = getFixDragTrans(deltaY, viewHeight, getImageHeight());
	                        matrix.postTranslate(fixTransX, fixTransY);
	                        fixTrans();
	                        last.set(curr.x, curr.y);
	                    }
	                    break;
	
	                case MotionEvent.ACTION_UP:
	                case MotionEvent.ACTION_POINTER_UP:
	                    setState(NONE);
	                    break;
	            }
            }
            
            //setImageMatrix(matrix);
            drawStreamBitmap();
            invalidate();
            //
            // indicate event was handled
            //
            triggerTouchCallback();
            return true;
        }
    }

    /**
     * ScaleListener detects user two finger scaling and scales image.
     * @author Ortiz
     *
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
        	Log.i(TAG, "onScaleBegin()");
            setState(ZOOM);
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
        	Log.i(TAG, "onScale()");
        	scaleImage(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY(), true);
            return true;
        }
        
        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
        	Log.i(TAG, "onScaleEnd()");
        	super.onScaleEnd(detector);
        	setState(NONE);
        	boolean animateToZoomBoundary = false;
        	float targetZoom = normalizedScale;
        	if (normalizedScale > maxScale) {
        		targetZoom = maxScale;
        		animateToZoomBoundary = true;
        		
        	} else if (normalizedScale < minScale) {
        		targetZoom = minScale;
        		animateToZoomBoundary = true;
        	}
        	
        	if (animateToZoomBoundary) {
	        	DoubleTapZoom doubleTap = new DoubleTapZoom(normalizedScale, targetZoom, viewWidth / 2, viewHeight / 2, true);
	        	compatPostOnAnimation(doubleTap);
        	}
        }
    }
    
    private void scaleImage(float deltaScale, float focusX, float focusY, boolean stretchImageToSuper) {
    	
    	float lowerScale, upperScale;
    	if (stretchImageToSuper) {
    		lowerScale = superMinScale;
    		upperScale = superMaxScale;
    		
    	} else {
    		lowerScale = minScale;
    		upperScale = maxScale;
    	}
    	
    	float origScale = normalizedScale;
        normalizedScale *= deltaScale;
        if (normalizedScale > upperScale) {
            normalizedScale = upperScale;
            deltaScale = upperScale / origScale;
        } else if (normalizedScale < lowerScale) {
            normalizedScale = lowerScale;
            deltaScale = lowerScale / origScale;
        }
        
        matrix.postScale(deltaScale, deltaScale, focusX, focusY);
        fixScaleTrans();
    }
    
    /**
     * DoubleTapZoom calls a series of runnables which apply
     * an animated zoom in/out graphic to the image.
     * @author Ortiz
     *
     */
    private class DoubleTapZoom implements Runnable {
    	
    	private long startTime;
    	private static final float ZOOM_TIME = 500;
    	private float startZoom, targetZoom;
    	private float bitmapX, bitmapY;
    	private boolean stretchImageToSuper;
    	private AccelerateDecelerateInterpolator interpolator = new AccelerateDecelerateInterpolator();
    	private PointF startTouch;
    	private PointF endTouch;

    	DoubleTapZoom(float startZoom, float targetZoom, float focusX, float focusY, boolean stretchImageToSuper) {
    		setState(ANIMATE_ZOOM);
    		startTime = System.currentTimeMillis();
    		this.startZoom = startZoom;
    		this.targetZoom = targetZoom;
    		this.stretchImageToSuper = stretchImageToSuper;
    		PointF bitmapPoint = transformCoordTouchToBitmap(focusX, focusY, false);
    		this.bitmapX = bitmapPoint.x;
    		this.bitmapY = bitmapPoint.y;
    		
    		//
    		// Used for translating image during scaling
    		//
    		startTouch = transformCoordBitmapToTouch(bitmapX, bitmapY);
    		endTouch = new PointF(viewWidth / 2, viewHeight / 2);
    	}

		@Override
		public void run() {
			float t = interpolate();
			float deltaScale = calculateDeltaScale(t);
			scaleImage(deltaScale, bitmapX, bitmapY, stretchImageToSuper);
			translateImageToCenterTouchPosition(t);
			fixScaleTrans();
			drawStreamBitmap();
			//setImageMatrix(matrix);
			
			if (t < 1f) {
				//
				// We haven't finished zooming
				//
				compatPostOnAnimation(this);
				
			} else {
				//
				// Finished zooming
				//
				setState(NONE);
			}
		}
		
		/**
		 * Interpolate between where the image should start and end in order to translate
		 * the image so that the point that is touched is what ends up centered at the end
		 * of the zoom.
		 * @param t
		 */
		private void translateImageToCenterTouchPosition(float t) {
			float targetX = startTouch.x + t * (endTouch.x - startTouch.x);
			float targetY = startTouch.y + t * (endTouch.y - startTouch.y);
			PointF curr = transformCoordBitmapToTouch(bitmapX, bitmapY);
			matrix.postTranslate(targetX - curr.x, targetY - curr.y);
		}
		
		/**
		 * Use interpolator to get t
		 * @return
		 */
		private float interpolate() {
			long currTime = System.currentTimeMillis();
			float elapsed = (currTime - startTime) / ZOOM_TIME;
			elapsed = Math.min(1f, elapsed);
			return interpolator.getInterpolation(elapsed);
		}
		
		/**
		 * Interpolate the current targeted zoom and get the delta
		 * from the current zoom.
		 * @param t
		 * @return
		 */
		private float calculateDeltaScale(float t) {
			float zoom = startZoom + t * (targetZoom - startZoom);
			return zoom / normalizedScale;
		}
    }
    
    /**
     * This function will transform the coordinates in the touch event to the coordinate 
     * system of the drawable that the imageview contain
     * @param x x-coordinate of touch event
     * @param y y-coordinate of touch event
     * @param clipToBitmap Touch event may occur within view, but outside image content. True, to clip return value
     * 			to the bounds of the bitmap size.
     * @return Coordinates of the point touched, in the coordinate system of the original drawable.
     */
    private PointF transformCoordTouchToBitmap(float x, float y, boolean clipToBitmap) {
         matrix.getValues(m);
         float origW = 1280;//getDrawable().getIntrinsicWidth();
         float origH = 720;//getDrawable().getIntrinsicHeight();
         float transX = m[Matrix.MTRANS_X];
         float transY = m[Matrix.MTRANS_Y];
         float finalX = ((x - transX) * origW) / getImageWidth();
         float finalY = ((y - transY) * origH) / getImageHeight();
         
         if (clipToBitmap) {
        	 finalX = Math.min(Math.max(x, 0), origW);
        	 finalY = Math.min(Math.max(y, 0), origH);
         }
         
         return new PointF(finalX , finalY);
    }
    
    /**
     * Inverse of transformCoordTouchToBitmap. This function will transform the coordinates in the
     * drawable's coordinate system to the view's coordinate system.
     * @param bx x-coordinate in original bitmap coordinate system
     * @param by y-coordinate in original bitmap coordinate system
     * @return Coordinates of the point in the view's coordinate system.
     */
    private PointF transformCoordBitmapToTouch(float bx, float by) {
        matrix.getValues(m);        
        float origW = 1280;//getDrawable().getIntrinsicWidth();
        float origH = 720;//getDrawable().getIntrinsicHeight();
        float px = bx / origW;
        float py = by / origH;
        float finalX = m[Matrix.MTRANS_X] + getImageWidth() * px;
        float finalY = m[Matrix.MTRANS_Y] + getImageHeight() * py;
        return new PointF(finalX , finalY);
    }
    
    /**
     * Fling launches sequential runnables which apply
     * the fling graphic to the image. The values for the translation
     * are interpolated by the Scroller.
     * @author Ortiz
     *
     */
    private class Fling implements Runnable {
    	
        Scroller scroller;
    	int currX, currY;
    	
    	Fling(int velocityX, int velocityY) {
    		setState(FLING);
    		scroller = new Scroller(context);
    		matrix.getValues(m);
    		
    		int startX = (int) m[Matrix.MTRANS_X];
    		int startY = (int) m[Matrix.MTRANS_Y];
    		int minX, maxX, minY, maxY;
    		
    		if (getImageWidth() > viewWidth) {
    			minX = viewWidth - (int) getImageWidth();
    			maxX = 0;
    			
    		} else {
    			minX = maxX = startX;
    		}
    		
    		if (getImageHeight() > viewHeight) {
    			minY = viewHeight - (int) getImageHeight();
    			maxY = 0;
    			
    		} else {
    			minY = maxY = startY;
    		}
    		
    		scroller.fling(startX, startY, (int) velocityX, (int) velocityY, minX,
                    maxX, minY, maxY);
    		currX = startX;
    		currY = startY;
    	}
    	
    	public void cancelFling() {
    		if (scroller != null) {
    			setState(NONE);
    			scroller.forceFinished(true);
    		}
    	}
    	
		@Override
		public void run() {
			if (scroller.isFinished()) {
        		scroller = null;
        		return;
        	}
			
			if (scroller.computeScrollOffset()) {
	        	int newX = scroller.getCurrX();
	            int newY = scroller.getCurrY();
	            int transX = newX - currX;
	            int transY = newY - currY;
	            currX = newX;
	            currY = newY;
	            matrix.postTranslate(transX, transY);
	            fixTrans();
	            //setImageMatrix(matrix);
	            drawStreamBitmap();
	            compatPostOnAnimation(this);
        	}
		}
    }
    
    //@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void compatPostOnAnimation(Runnable runnable) {
    	//if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN) {
        //    postOnAnimation(runnable);
            
        //} else {
            postDelayed(runnable, 1000/60);
        //}
    }
    
    private void printMatrixInfo() {
    	matrix.getValues(m);
    	Log.i(TAG, "Scale: " + m[Matrix.MSCALE_X] + " TransX: " + m[Matrix.MTRANS_X] + " TransY: " + m[Matrix.MTRANS_Y]);
    }
    
    //Abner Add Begin
    private OnTouchSurfaceCallback mOnTouchSurfaceCallback;
    
    static public interface OnTouchSurfaceCallback{
    	public void onSingleTapConfirm();
    	public void onDoubleTapConfirm();
    	public void onTouch();
    }
    
    public void registerSingleTapCallback(OnTouchSurfaceCallback cb){
    	mOnTouchSurfaceCallback = cb;
    }
    
    public void unregisterSingleTapCallback(){
    	mOnTouchSurfaceCallback = null;
    }
    
    private void triggermSingleTapCallback(){
    	if(null != mOnTouchSurfaceCallback){
    		mOnTouchSurfaceCallback.onSingleTapConfirm();
    	}
    }
    
    private void triggermDoubleTapCallback(){
    	if(null != mOnTouchSurfaceCallback){
    		mOnTouchSurfaceCallback.onDoubleTapConfirm();
    	}
    }
    
    private void triggerTouchCallback(){
    	if(null != mOnTouchSurfaceCallback){
    		mOnTouchSurfaceCallback.onTouch();
    	}
    }
    
    public Surface getNativeSurface() {
        return getHolder().getSurface();
    }
    
    private Bitmap mStreamBitmap;
    private boolean mbUpsideDown = false;
    float redundantYSpace;
    float redundantXSpace;
    float scale;
    
    public void setUpsideDown(boolean bUpsideDown){
    	mbUpsideDown = bUpsideDown;
    	updateMatrix();
    }
    
    public Bitmap getBitmapBySize(int iWidth, int iHeight){
    	Log.d(TAG, "getBitmapBySize(), iWidth:"+iWidth+", iHeight:"+iHeight);
    	if(null != mStreamBitmap && (mStreamBitmap.getWidth() != iWidth || mStreamBitmap.getHeight() != iHeight) && false == mStreamBitmap.isRecycled()){
    		mStreamBitmap.recycle();
    		mStreamBitmap = null;
    	}
    	
    	if(null == mStreamBitmap){
    		mStreamBitmap = Bitmap.createBitmap(iWidth, iHeight, Config.RGB_565);
    		
    		float scaleX = (float) mSurfaceWidth / iWidth ;
            float scaleY = (float) mSurfaceHeight / iHeight ;
            scale = Math.min(scaleX, scaleY);
            //scale = (mbUpsideDown)?-scale:scale;
            //c
    		
            redundantXSpace = mSurfaceWidth - iWidth*scale;
            redundantYSpace = mSurfaceHeight - iHeight*scale;
            updateMatrix();
    	}
    	
    	return mStreamBitmap;
    }
    
    private void updateMatrix(){
    	matrix.setScale(mbUpsideDown?-scale:scale, 
					mbUpsideDown?-scale:scale);
    	if(null != mStreamBitmap && false == mStreamBitmap.isRecycled()){
        	matrix.postTranslate(redundantXSpace / 2+(mbUpsideDown?mStreamBitmap.getWidth()*scale:0), 
				     redundantYSpace / 2+(mbUpsideDown?mStreamBitmap.getHeight()*scale:0));
    	}
    }
    
    public void drawStreamBitmap(){
    	drawStreamBitmap(mStreamBitmap);
    }
    
    public void drawStreamBitmap(Bitmap bmp){
    	Canvas canvas = getHolder().lockCanvas();
    	if(null != canvas){
    		try{
				canvas.drawColor(miBackgroundColor/*, PorterDuff.Mode.CLEAR*/);
		        //Log.d(TAG, "drawStreamBitmap(), redundantXSpace:"+redundantXSpace+", redundantYSpace:"+redundantYSpace+", scale:"+scale);
		        //updateMatrix();
				//printMatrixInfo();
		        
				if(null != bmp && false == bmp.isRecycled())
					canvas.drawBitmap(bmp, matrix, null);
				getHolder().unlockCanvasAndPost(canvas);
    		}catch(java.lang.IllegalStateException e){
    			Log.e(TAG, "drawStreamBitmap(), e"+e.toString());
    		}
    	}
    }
    
    public void drawDefaultBackground(){
    	Canvas canvas = getHolder().lockCanvas();
    	if(null != canvas){    		
    		canvas.drawColor(miBackgroundColor/*, PorterDuff.Mode.CLEAR*/);
    		getHolder().unlockCanvasAndPost(canvas);
    	}
    }
    //Abner Add End
}
