package com.hitsuji.camera;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.preference.PreferenceManager;
import com.util.Log;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

class TravelingCameraView extends TravelingCameraViewBase {
	private final static String TAG = TravelingCameraView.class.getSimpleName();
	
  public final static int THRESHOLD_AREA = 100;
	
	private int mCameraWidth = -1;
	private int mCameraHeight = -1;
	
	private Mat mRgba;
	private Mat mIntermediateMat;
	private Mat mIntermediateMat2;
	private Mat mIntermediateMat3;
	private Mat mHolder = new Mat();
	
	private Mat mDefaultKernel3x3;
	private Mat mShapeningKernel3x3;
	
	private Point mDefP;
	private Object mSaveImageLock;
	private Scalar lo, hi;
	private Scalar bl, wh;
	private TravelingCameraActivity mParent;

	private float mScaleFactor = 1.0f;
	private ScaleGestureDetector mScaleGesDetector = null;
	private  GestureDetector  mGesDetector = null;
	
	public TravelingCameraView(TravelingCameraActivity context) {
		super(context);
		mParent = context;
		mSaveImageLock = new Object();
		
		mScaleGesDetector = new ScaleGestureDetector(context, mSGListener);
		mGesDetector = new  GestureDetector(context, mGListener);
		mGesDetector.setOnDoubleTapListener(mODTListener);
	}

	@Override
	public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
		super.surfaceChanged(_holder, format, width, height);

			// initialize Mats before usage
		mRgba = new Mat();
		mIntermediateMat = new Mat();
		mIntermediateMat2 = new Mat();
		mIntermediateMat3 = new Mat();
				
		mDefP = new Point(-1, -1);
		mDefaultKernel3x3 = new Mat( 3, 3, CvType.CV_8U);
		mShapeningKernel3x3 = new Mat( 3, 3, CvType.CV_8U);

		Scalar sa = Scalar.all(-1.0);
		mShapeningKernel3x3.setTo(sa);
		mShapeningKernel3x3.put(1, 1, 1+8);
		
		bl = new Scalar(0, 0, 255, 255);
		wh = new Scalar(255, 255, 255, 255);

		mScaleFactor = 1.0f;
	}
	public int getCameraWidth (){
		return mCameraWidth;
	}
	public int getCameraHeight(){
		return mCameraHeight;
	}
	
	private void setRgbaMatrics(VideoCapture capture){
		capture.retrieve(mRgba, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
	}
	private void setExtractMatrics2(VideoCapture capture){
		lo = new Scalar(
				TravelingBrowserActivity.getHueLowerThreshold(),
				TravelingBrowserActivity.getSaturationLowerThreshold(),
				TravelingBrowserActivity.getBrightnessLowerThreshold()
				);
		hi = new Scalar(
				TravelingBrowserActivity.getHueUpperThreshold(),
				TravelingBrowserActivity.getSaturationUpperThreshold(),
				TravelingBrowserActivity.getBrightnessUpperThreshold()
				);
		
		capture.retrieve(mIntermediateMat, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat3, Imgproc.COLOR_RGB2HSV_FULL);

		int size = TravelingBrowserActivity.getSmoothingFilterSize();
		if (size > 0)
			Imgproc.medianBlur(mIntermediateMat3, mIntermediateMat3, size);
				
		Core.inRange(mIntermediateMat3, lo, hi, mIntermediateMat2); // green

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(mIntermediateMat2, contours, hierarchy,Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		ArrayList<MatOfPoint> list = new ArrayList<MatOfPoint>();
		for (int i=0; i<contours.size(); i++) {
			MatOfPoint m = contours.get(i);
			double area = Imgproc.contourArea(m);
			if (area > THRESHOLD_AREA ) {
				list.add(m);
			}
		}		
		mRgba.setTo(bl);
		Mat mask = Mat.zeros(mIntermediateMat.rows(), mIntermediateMat.cols(), CvType.CV_8UC1);
		Imgproc.drawContours(mask, list, -1, wh, Core.FILLED);

		if (TravelingBrowserActivity.getDilateErudeTimes()>0){
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
		} else if  (TravelingBrowserActivity.getDilateErudeTimes()<0){
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
		}
		
		if (TravelingBrowserActivity.getInvertMask())
			Core.bitwise_not(mask, mask);
		mIntermediateMat.copyTo(mRgba, mask);
		mask.release();
		for (int i=0; i<contours.size(); i++) {
			Mat m = contours.get(i);
			m.release();
		}
		contours.clear();		
	}
	
	private void setMargeMatrics2(VideoCapture capture){
		lo = new Scalar(
				TravelingBrowserActivity.getHueLowerThreshold(),
				TravelingBrowserActivity.getSaturationLowerThreshold(),
				TravelingBrowserActivity.getBrightnessLowerThreshold()
				);
		hi = new Scalar(
				TravelingBrowserActivity.getHueUpperThreshold(),
				TravelingBrowserActivity.getSaturationUpperThreshold(),
				TravelingBrowserActivity.getBrightnessUpperThreshold()
				);
		
		capture.retrieve(mIntermediateMat, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat3, Imgproc.COLOR_RGB2HSV_FULL);

		int size = TravelingBrowserActivity.getSmoothingFilterSize();
		if (size > 0)
			Imgproc.medianBlur(mIntermediateMat3, mIntermediateMat3, size);
				
		Core.inRange(mIntermediateMat3, lo, hi, mIntermediateMat2); // green

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Mat hierarchy = new Mat();
		Imgproc.findContours(mIntermediateMat2, contours, hierarchy,Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

		ArrayList<MatOfPoint> list = new ArrayList<MatOfPoint>();
		for (int i=0; i<contours.size(); i++) {
			MatOfPoint m = contours.get(i);
			double area = Imgproc.contourArea(m);
			if (area > THRESHOLD_AREA ) {
				list.add(m);
			}
		}		
		mRgba.setTo(bl);
		Mat mask = Mat.zeros(mIntermediateMat.rows(), mIntermediateMat.cols(), CvType.CV_8UC1);
		Imgproc.drawContours(mask, list, -1, wh, Core.FILLED);

		if (TravelingBrowserActivity.getDilateErudeTimes()>0){
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
		} else if  (TravelingBrowserActivity.getDilateErudeTimes()<0){
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, TravelingBrowserActivity.getDilateErudeTimes());
		}

		Bitmap bmp = TravelingBrowserActivity.drawBitmap(
				mIntermediateMat.width(), 
				mIntermediateMat.height(), 
				mScaleFactor);

		mRgba = new Mat(mIntermediateMat.height(), mIntermediateMat.width(), CvType.CV_8UC4);
		mRgba.setTo(new Scalar(0,0,0,0));
		
		if (bmp!=null){
			if (TravelingBrowserActivity.getInvertMask())
				Core.bitwise_not(mask, mask);
			Core.add(mRgba, mIntermediateMat, mRgba, mask);
			Core.bitwise_not(mask, mIntermediateMat2);
			Mat viewcap = new Mat();
			Utils.bitmapToMat(bmp, viewcap);
			Core.add(mRgba, viewcap, mRgba, mIntermediateMat2);
			viewcap.release();
			bmp.recycle();
		}
		mask.release();
		for (int i=0; i<contours.size(); i++) {
			Mat m = contours.get(i);
			m.release();
		}
		contours.clear();
	}
	@Override
	protected Bitmap processFrame(VideoCapture capture) {
		//Log.d(TAG, "processframe start");
		ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> list = new ArrayList<MatOfPoint>();		
		Bitmap bitmap = null;
		Mat mask = null;
		if (mRgba==null) mRgba = new Mat();
		if (mIntermediateMat == null) mIntermediateMat = new Mat();

		
		if (TravelingBrowserActivity.getTarget() == 
				TravelingBrowserActivity.TARGET_BROWSER ||
				mStopCameraFlag.get()) {
			setRgbaMatrics(capture);
		} else {
			switch (TravelingBrowserActivity.getViewMode()) {
			case TravelingCameraActivity.VIEW_MODE_CAMERA:
			case TravelingCameraActivity.VIEW_MODE_SETTINGS:
			case TravelingCameraActivity.VIEW_MODE_WEBVIEW:
				setRgbaMatrics(capture);
				break;
			case TravelingCameraActivity.VIEW_MODE_EXTRACT:
				setExtractMatrics2(capture);
				break;
			case TravelingCameraActivity.VIEW_MODE_MARGE:
				setMargeMatrics2(capture);
				break;
			}
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

	@Override
	public void run() {
		super.run();
			// Explicitly deallocate Mats
			if (mRgba != null)
				mRgba.release();
			if (mIntermediateMat != null)
				mIntermediateMat.release();
			if (mIntermediateMat2 != null)
				mIntermediateMat2.release();
			if (mIntermediateMat3 != null)
				mIntermediateMat3.release();
			
			mRgba = null;
			mIntermediateMat = null;
			mIntermediateMat2 = null;
			mIntermediateMat3 = null;
	}

	public boolean saveImage(String path) {
		// TODO Auto-generated method stub
		Bitmap bitmap = null;
		synchronized (mSaveImageLock) {
			if (mHolder != null && 
					mHolder.cols()>0 && mHolder.rows()>0) {
				bitmap = Bitmap.createBitmap(
						mHolder.cols(), 
						mHolder.rows(), 
						Bitmap.Config.ARGB_8888);
				Utils.matToBitmap(mHolder, bitmap);
			} else {
				return false;
			}
		}
		if (bitmap !=null) {
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
	
	public Mat captureCamera(){
		Mat m = new Mat();
		synchronized (mSaveImageLock) {
			if (mHolder!=null)
				mHolder.copyTo(m);
			else
				return null;
		}
		return m;
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
			Log.d(TAG, "onScaleEnd : "+ detector.getScaleFactor() +
					" scale:"+mScaleFactor);
			//mScaleFactor *= detector.getScaleFactor();
			invalidate();
			super.onScaleEnd(detector);
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {

			float f = detector.getScaleFactor();
			mScaleFactor *= (f - (f-1.0f) * 0.5f);
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
			TravelingBrowserActivity.moveWebView(distanceX, distanceY);
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
