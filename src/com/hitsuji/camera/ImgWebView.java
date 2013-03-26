package com.hitsuji.camera;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import com.util.Log;
import com.util.Pair;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

public class ImgWebView extends WebView {
	private static final String TAG = ImgWebView.class.getSimpleName();
	private int mDefaultWidth = -1;
	private int mDefaultHeight = -1;
	private int mX = 0;
	private int mY = 0;

	private Mat mIntermediateMat;
	private Mat mIntermediateMat2;
	private Mat mIntermediateMat3;
	
	private Mat mDefaultKernel3x3;
	private Mat mShapeningKernel3x3;
	
	private Point mDefP;
	private Scalar lo, hi;
	private Scalar bl, wh;
	
	private float mScale = 1.0f;
	
	public ImgWebView(Context context, AttributeSet set) {
		super(context, set);
	}
	public ImgWebView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	
		mDefP = new Point(-1, -1);
		
		bl = new Scalar(0, 0, 255, 255);
		wh = new Scalar(255, 255, 255, 255);	
	}

	public Bitmap drawBitmap(int width, int height) {
		return drawBitmap(width, height, 1.0f);
	}
	public Bitmap drawBitmap(int width, int height, float scale) {
		if (width<=0 || height<=0) return null;
		//if (scale < 1) scale = 1;
		int cw = (int)(getContentWidth() * getScale());
		int ch = (int)(getContentHeight() * getScale());
		int imgWidth = width > cw ? width : cw;
		int imgHeight = height > ch ? height : ch;
		Log.d(TAG, "bmp w:"+width +" h:"+ height + " scale:"+scale);
		Log.d(TAG, "webview w:"+ getWidth() +" h:"+ getHeight());
		Log.d(TAG, "img w:"+ imgWidth +" h:"+ imgHeight);
		Log.d(TAG, "cw:"+ cw +" ch:"+ ch);
		//this.setInitialScale(200);
		//Bitmap bitmap = Bitmap.createBitmap((int)(width), (int)(height*scale), Config.ARGB_8888);
		//Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		Bitmap bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		/*
		Paint paint = new Paint();
		paint.setColor(Color.BLACK);
		android.graphics.Rect r = new android.graphics.Rect(0,0, imgWidth, imgHeight);
		canvas.drawRect(r, paint);
		*/
		//picture.draw(canvas);
		this.draw(canvas);
		//Log.d(TAG, "bmpresult w:"+bitmap.getWidth() + " h:"+bitmap.getHeight() + " mx:"+mX + " mY:"+mY);
		//if (scale<=1) return bitmap;
		//return bitmap;
		Log.d(TAG, "pre x:"+ mX +" y:"+ mY);
		if (mX+width>imgWidth) 
			mX = imgWidth-width;
		if (mY+height>imgHeight) 
			mY = imgHeight-height;
		Log.d(TAG, "post x:"+ mX +" y:"+ mY);
		Bitmap bmp = Bitmap.createBitmap(bitmap, mX, mY, width, height);
		bitmap.recycle();
		return bmp;

	}
	
	public Bitmap drawBitmap() {
		int cw = getContentWidth();
		int ch = getContentHeight();
		Bitmap bitmap = Bitmap.createBitmap(cw, ch, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		this.draw(canvas);
		return bitmap;
	}
	
	public int getContentWidth(){

		Class<WebView> c = WebView.class;
		Field f;
		try {
			f = c.getDeclaredField("mContentWidth");
			f.setAccessible(true);
			return f.getInt(this);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	public void setDefaultSize(int width, int height) {
		// TODO Auto-generated method stub
		mDefaultWidth = width;
		mDefaultHeight = height;
	}
	public int getDefaultWidth(){
		return mDefaultWidth;
	}
	public int getDefaultHeight(){
		return mDefaultHeight;
	}
	public void move(float x, float y) {
		// TODO Auto-generated method stub
		int cw = (int)(this.getScale() * getContentWidth());
		int ch = (int)(this.getScale() * getContentHeight());
		Log.d(TAG, "move mx:"+mX + " mY:"+mY + 
				" defw:"+mDefaultWidth +  " defy:"+mDefaultHeight + 
				" webw:"+getWidth() + " webh:"+getHeight()+
				" contw:"+getContentWidth() + " conth:"+getContentHeight()+
				" x:"+x + " y:"+y + " scale:"+this.getScale() + 
				" cw:"+cw + " cy:"+ch);
		if (mX+x<0) x = 0;
		if (mY+y<0) y = 0;

		int thw = mDefaultWidth > cw ? 
				mDefaultWidth : cw;
		int thh = mDefaultHeight > ch ? 
				mDefaultHeight : ch;
/*
		int thw = mDefaultWidth > getWidth() ? 
				mDefaultWidth : getWidth();
		int thh = mDefaultHeight > getHeight() ?
				mDefaultHeight : getHeight();	
				*/	
		if (mX+x+mDefaultWidth>thw) x = 0;
		if (mY+y+mDefaultHeight>thh) y = 0;
		mX += x;
		mY += y;
	}
	public int getX(){
		return mX;
	}
	public int getY(){
		return mY;
	}
	public void movePrescale(int w, int h) {
		// TODO Auto-generated method stub
		if (mX > w) mX = w-mDefaultWidth;
		if (mY > h) mY = h-mDefaultHeight;
	}
	
	public Mat createExtractMatrics(){
		initFields();
		
		Bitmap bmp = drawBitmap(mDefaultWidth, mDefaultHeight, mScale);
		if (bmp==null) return null;
		

		Utils.bitmapToMat(bmp, mIntermediateMat);
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
			if (area > TravelingCameraView.THRESHOLD_AREA ) {
				list.add(m);
			}
		}
		Mat rgba = new Mat();
		mIntermediateMat.copyTo(rgba);
		rgba.setTo(bl);
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
		mIntermediateMat.copyTo(rgba, mask);
		mask.release();
		for (int i=0; i<contours.size(); i++) {
			Mat m = contours.get(i);
			m.release();
		}
		contours.clear();
		hierarchy.release();
		consumeFields();
		return rgba;
	}
	

	public Pair<Mat, Mat> createMargeMatrics(){
		Mat rgba = new Mat();
		initFields();
		Bitmap bmp = drawBitmap(mDefaultWidth, mDefaultHeight, mScale);
		if (bmp==null) return null;
		Utils.bitmapToMat(bmp, mIntermediateMat);
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
			if (area > TravelingCameraView.THRESHOLD_AREA ) {
				list.add(m);
			}
		}		
		rgba.setTo(bl);
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


		rgba = new Mat(mIntermediateMat.height(), mIntermediateMat.width(), CvType.CV_8UC4);
		rgba.setTo(new Scalar(0,0,0,0));
		
		if (TravelingBrowserActivity.getInvertMask())
				Core.bitwise_not(mask, mask);
		Core.add(rgba, mIntermediateMat, rgba, mask);
		//Core.bitwise_not(mask, mIntermediateMat2);
		//Core.add(rgba, holder, rgba, mIntermediateMat2);
		bmp.recycle();

		//mask.release();
		for (int i=0; i<contours.size(); i++) {
			Mat m = contours.get(i);
			m.release();
		}
		contours.clear();
		//Bitmap bitmap = Bitmap.createBitmap(rgba.cols(), rgba.rows(), Bitmap.Config.ARGB_8888);
		//Utils.matToBitmap(rgba, bitmap);
		consumeFields();
		return new Pair<Mat,Mat>(rgba, mask);
	}
	
	private void initFields() {
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

		mIntermediateMat = new Mat();
		mIntermediateMat2 = new Mat();
		mIntermediateMat3 = new Mat();
		mDefaultKernel3x3 = new Mat( 3, 3, CvType.CV_8U);
		mShapeningKernel3x3 = new Mat( 3, 3, CvType.CV_8U);
		mShapeningKernel3x3.setTo(Scalar.all(-1.0));
		mShapeningKernel3x3.put(1, 1, 1+8);		
	}
	private void consumeFields(){
		mIntermediateMat.release();
		mIntermediateMat2.release();
		mIntermediateMat3.release();
		mDefaultKernel3x3.release();
		mShapeningKernel3x3.release();
		mIntermediateMat = null;
		mIntermediateMat2 = null;
		mIntermediateMat3 = null;
		mDefaultKernel3x3 = null;
		mShapeningKernel3x3 = null;
	}
}
