package com.hitsuji.camera;

import java.util.List;

import com.util.Log;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

public class CameraView extends org.opencv.android.JavaCameraView{
	private static final String TAG = "CameraView";
	private ScaleGestureDetector mScaleGesDetector = null;
	private GestureDetector  mGesDetector = null;
	private BaseActivity mParent = null;
	
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mScaleGesDetector = new ScaleGestureDetector(context, mSGListener);
		mGesDetector = new  GestureDetector(context, mGListener);
		mGesDetector.setOnDoubleTapListener(mODTListener);
	}
	
	public void setActivity(BaseActivity context){
		mParent = context;
	}
	
	@Override
	protected boolean initializeCamera(int width, int height) {
		boolean ret = super.initializeCamera(width, height);
		
		Camera.Parameters params = mCamera.getParameters();
        Log.d(TAG, "getSupportedPreviewSizes()");
        JavaCameraSizeAccessor accessor = new JavaCameraSizeAccessor();
        List<android.hardware.Camera.Size> supportedSizes = params.getSupportedPreviewSizes();
        for (Object size : supportedSizes) {
        	int w = accessor.getWidth(size);
        	int h = accessor.getHeight(size);
        	Log.d(TAG, "size width:"+width + " height:"+height + " w:"+w + " h:"+h);
        }
		
		return ret;
	}

	public boolean onTouchEvent(MotionEvent event) {
		//Log.d(TAG, event.toString() + " count:"+event.getPointerCount());
		return event.getPointerCount() == 1 ? 
				mGesDetector.onTouchEvent(event) : mScaleGesDetector.onTouchEvent(event);
	}
	
	public float getScale(){
		return this.mScale;
	}
	private SimpleOnScaleGestureListener mSGListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			Log.d(TAG, "onScaleBegin : "+ detector.getScaleFactor());
			invalidate();
			return super.onScaleBegin(detector);
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			//mScaleFactor *= detector.getScaleFactor();
			invalidate();
			super.onScaleEnd(detector);
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {

			return true;
		};
	};
	private GestureDetector.OnGestureListener mGListener = new GestureDetector.OnGestureListener() {

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public void onShowPress(MotionEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, 
				float distanceX,
				float distanceY) {
			// TODO Auto-generated method stub
			//Log.d(TAG, "x:"+distanceX + " y:"+distanceY );
			if (mParent!=null)
				mParent.moveAndRedrawWebView(distanceX, distanceY);
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean onDown(MotionEvent e) {
			// TODO Auto-generated method stub
			return true;
		}
	};

	private GestureDetector.OnDoubleTapListener mODTListener = new GestureDetector.OnDoubleTapListener(){

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// TODO Auto-generated method stub
			Log.d(TAG, "ondoubletap:"+e.toString());
			return true;
		}

		@Override
		public boolean onSingleTapConfirmed(MotionEvent e) {
			//TODO Auto-generated method stub
			Log.d(TAG, "onsingletapconfirmed:"+e.toString());
			return true;
		}

		@Override
		public boolean onDoubleTapEvent(MotionEvent e) {
			// TODO Auto-generated method stub
			return true;
		}
	};
}
