package com.hitsuji.camera;

import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import com.util.Log;
import com.util.Pair;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.SurfaceHolder;

public class WebViewCameraView extends TravelingCameraViewBase {
	private final static String TAG = WebViewCameraView.class.getSimpleName();

	public final static int THRESHOLD_AREA = 100;

	private int mCameraWidth = -1;
	private int mCameraHeight = -1;
	private Object mSaveImageLock;
	private Mat mRgba;

	private TravelingBrowserActivity mParent;
	private Mat mWebviewImage;
	private Mat mWebviewMask;
	private AtomicBoolean mWebviewImageLock = new AtomicBoolean(false);

	private ScaleGestureDetector mScaleGesDetector = null;
	private GestureDetector  mGesDetector = null;
	private Mat mHolder;
	
	public WebViewCameraView(TravelingBrowserActivity context) {
		super(context);
		// TODO Auto-generated constructor stub
		mParent = context;
		mSaveImageLock = new Object();
		mScaleGesDetector = new ScaleGestureDetector(context, mSGListener);
		mGesDetector = new  GestureDetector(context, mGListener);
		mGesDetector.setOnDoubleTapListener(mODTListener);
		mHolder = new Mat();
	}

	@Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
		super.surfaceChanged(_holder, format, width, height);

		// initialize Mats before usage
		mRgba = new Mat();

	}

	private void setRgbaMatrics(VideoCapture capture){
		capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
	}

	private void setMargeMatrics(VideoCapture capture) {
		capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
	}

	@Override
	protected Bitmap processFrame(VideoCapture capture) {
		//Log.d(TAG, "processframe start");

		Bitmap bitmap = null;
		if (mRgba!=null){
			mRgba.release();
			mRgba = null;
		}
		mRgba = new Mat();

		if (TravelingBrowserActivity.getTarget() == 
				TravelingBrowserActivity.TARGET_BROWSER &&
				TravelingBrowserActivity.getViewMode() ==
				TravelingBrowserActivity.VIEW_MODE_CAMERA){
			setRgbaMatrics(capture); 
		} else if (TravelingBrowserActivity.getTarget() == 
				TravelingBrowserActivity.TARGET_BROWSER &&
				TravelingBrowserActivity.getViewMode() ==
				TravelingBrowserActivity.VIEW_MODE_EXTRACT){
			synchronized(mWebviewImageLock) {
				if (mWebviewImage!=null) {
					mRgba.release();
					mRgba = mWebviewImage.clone();
				} else 
					setRgbaMatrics(capture); 
			}
		} else if (TravelingBrowserActivity.getTarget() == 
				TravelingBrowserActivity.TARGET_BROWSER &&
				TravelingBrowserActivity.getViewMode() ==
				TravelingBrowserActivity.VIEW_MODE_MARGE){
			capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
			Mat tmp = null;
			synchronized(mWebviewImageLock){
				if(mWebviewImage!=null && mWebviewMask!=null) {
					Mat tmp2 = Mat.zeros(mRgba.rows(), mRgba.cols(), 
							CvType.CV_8UC4);
					Core.add(tmp2, mRgba, tmp2, mWebviewMask);
					tmp = mWebviewImage.clone();
					Core.add(tmp2, tmp, mRgba);
					//mRgba = mWebviewImage.clone();
					tmp.release();
					tmp2.release();
				} else {
					tmp = new Mat(mRgba.height(), mRgba.width(), 
							mRgba.type(), new Scalar(255,255,255, 255));
					Core.add(mRgba, tmp, mRgba);
					tmp.release();
				}
			}
			/*
			Log.d(TAG, "rgba w:"+ mRgba.width() + " h:"+mRgba.height() + " type:"+mRgba.type() +
					" cann:"+mRgba.channels() + " mat:"+mRgba.toString() + 
					" tmp w:"+tmp.width() + " h:"+tmp.height() + " type:"+tmp.type() +
					" cann:"+tmp.channels() + " mat:"+tmp.toString());
			 */

		} else {
			setRgbaMatrics(capture); 
		}

		if (bitmap==null) {
			bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(mRgba, bitmap);
			synchronized (mSaveImageLock) {
				mRgba.copyTo(mHolder);
			}			
		}

		//Log.d(TAG, "processframe end");
		return bitmap;
	}

	public void setImageMatrics(Mat cap) {
		// TODO Auto-generated method stub
		synchronized (mWebviewImageLock) {
			mWebviewImage = cap;
		}
	}
	public void setImageMatrics(Pair<Mat, Mat> caps) {
		// TODO Auto-generated method stub
		if (caps == null) return;
		synchronized (mWebviewImageLock) {
			mWebviewImage = caps.getFirst();
			mWebviewMask = caps.getSecond();
			Core.bitwise_not(mWebviewMask, mWebviewMask);
		}
	}
	@Override
	public void run() {
		super.run();
		// Explicitly deallocate Mats
		if (mRgba != null)
			mRgba.release();
		mRgba = null;		
	}
	public boolean saveImage(String path) {
		// TODO Auto-generated method stub
		synchronized (mSaveImageLock) {
			if (mHolder != null) {
				Bitmap bitmap = Bitmap.createBitmap(
						mHolder.cols(), 
						mHolder.rows(), 
						Bitmap.Config.ARGB_8888);
				Utils.matToBitmap(mHolder, bitmap);
				try {
					FileOutputStream out = new FileOutputStream(path);
				  return bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
				} catch (Exception e) {
				   e.printStackTrace();
				   return false;
				}
			} else {
				return false;
			}
							
		}
	}

	public boolean onTouchEvent(MotionEvent event) {
		//Log.d(TAG, event.toString() + " count:"+event.getPointerCount());
		return event.getPointerCount() == 1 ? 
				mGesDetector.onTouchEvent(event) : mScaleGesDetector.onTouchEvent(event);
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
