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
import android.graphics.Matrix;
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
	private int mContentWidth = -1;
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
	
	private volatile float mScale = 1.0f;
	
	private BaseActivity activity;
	
	public ImgWebView(Context context, AttributeSet set) {
		super(context, set);
	}
	public ImgWebView(BaseActivity context) {
		super(context);
		// TODO Auto-generated constructor stub
	
		mDefP = new Point(-1, -1);
		activity = context;
		bl = new Scalar(0, 0, 255, 255);
		wh = new Scalar(255, 255, 255, 255);	
	}

	public Bitmap drawBitmap(int cameraWidth, int cameraHeight, 
			float scale, float cameraScale) {
		if (cameraWidth<=0 || cameraHeight<=0) return null;
		if (getWidth()<=0 || getHeight()<=0) return null;
		//if (scale < 1) scale = 1;
		/*
		int cw = (int)(getContentWidth() * mScale * cameraScale);
		int ch = (int)(getContentHeight() * mScale * cameraScale);
		cameraWidth *= cameraScale;
		cameraHeight *= cameraScale;
		*/
		int cw = (int)(getContentWidth() * mScale);
		int ch = (int)(getContentHeight() * mScale);
		int imgWidth = getWidth() > cw ? getWidth() : cw;
		int imgHeight = getHeight() > ch ? getHeight() : ch;
		Log.d(TAG, "bmp w:"+cameraWidth +" h:"+ cameraHeight + " scale:"+scale);
		Log.d(TAG, "webview w:"+ getWidth() +" h:"+ getHeight());
		Log.d(TAG, "img w:"+ imgWidth +" h:"+ imgHeight);
		Log.d(TAG, "cw:"+ cw +" ch:"+ ch);
		Bitmap bitmap;
		//this.setInitialScale(200);
		//Bitmap bitmap = Bitmap.createBitmap((int)(width), (int)(height*scale), Config.ARGB_8888);
		//Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
		bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Config.ARGB_8888);
		//bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Config.ARGB_8888);
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
		if (mX+cameraWidth>imgWidth) 
			mX = imgWidth-cameraWidth;
		if (mY+cameraHeight>imgHeight) 
			mY = imgHeight-cameraHeight;
		
		Matrix matrix = new Matrix();
		float fx = (float)cameraWidth/(float)getWidth();
		float fy = (float)cameraHeight/(float)getHeight();
		matrix.setScale(fx, fy);
		Log.d(TAG, "post x:"+ mX +" y:"+ mY + " matrix:"+matrix + 
				" fx:"+fx + " fy:"+fy + " bitmapw:"+bitmap.getWidth() + " bitmaph:"+bitmap.getHeight());
		Bitmap bmp = Bitmap.createBitmap(bitmap, mX, mY, 
				getWidth(), getHeight(), matrix, true);
				
		//Bitmap bmp = Bitmap.createBitmap(bitmap, mX, mY, 
		//		cameraWidth, cameraHeight);
		bitmap.recycle();
		return bmp;

	}
	public Bitmap drawBitmap2(int cameraWidth, int cameraHeight, 
			int viewWidth, int viewHeight,
			float scale, float cameraScale) {
		if (cameraWidth<=0 || cameraHeight<=0) return null;
		if (viewWidth<=0 || viewHeight<=0) return null;
		
		int cw = (int)(getContentWidth() * mScale);
		int ch = (int)(getContentHeight() * mScale);
		int imgWidth = viewWidth > cw ? viewWidth : cw;
		int imgHeight = viewHeight > ch ? viewHeight : ch;
		Log.d(TAG, "bmp w:"+cameraWidth +" h:"+ cameraHeight + " scale:"+scale);
		Log.d(TAG, "webview w:"+ viewWidth +" h:"+ viewHeight);
		Log.d(TAG, "img w:"+ imgWidth +" h:"+ imgHeight);
		Log.d(TAG, "cw:"+ cw +" ch:"+ ch);
		Bitmap bitmap;
		bitmap = Bitmap.createBitmap(imgWidth, imgHeight, Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		this.draw(canvas);

		Log.d(TAG, "pre x:"+ mX +" y:"+ mY);
		if (mX+cameraWidth>imgWidth) 
			mX = imgWidth-cameraWidth;
		if (mY+cameraHeight>imgHeight) 
			mY = imgHeight-cameraHeight;
		
		Matrix matrix = new Matrix();
		float fx = (float)cameraWidth/(float)viewWidth;
		float fy = (float)cameraHeight/(float)viewHeight;
		matrix.setScale(fx, fy);
		Log.d(TAG, "post x:"+ mX +" y:"+ mY + " matrix:"+matrix + 
				" fx:"+fx + " fy:"+fy + 
				" bitmapw:"+bitmap.getWidth() + " bitmaph:"+bitmap.getHeight() +
				" viewwidth:"+viewWidth + " viewheight:"+viewHeight);
		Bitmap bmp = Bitmap.createBitmap(bitmap, mX, mY, 
				viewWidth, viewHeight, matrix, true);
				
		//Bitmap bmp = Bitmap.createBitmap(bitmap, mX, mY, 
		//		cameraWidth, cameraHeight);
		bitmap.recycle();
		return bmp;

	}
	public void setScale(float scale){
		this.mScale = scale;
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
		return mContentWidth;
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
		int cw = (int)(mScale * getContentWidth());
		int ch = (int)(mScale * getContentHeight());
		Log.d(TAG, "move mx:"+mX + " mY:"+mY + 
				" defw:"+mDefaultWidth +  " defh:"+mDefaultHeight + 
				" webw:"+getWidth() + " webh:"+getHeight()+
				" contw:"+getContentWidth() + " conth:"+getContentHeight()+
				" x:"+x + " y:"+y + " scale:"+mScale +
				" cw:"+cw + " cy:"+ch);
		if (mX+x<0) x = 0;
		if (mY+y<0) y = 0;
		int viewWidth = getWidth();
		int viewHeight = getHeight();
		
		int thw = viewWidth > cw ? 
				viewWidth : cw;
		int thh = viewHeight > ch ? 
				viewHeight : ch;

		if (mX+x+viewWidth>thw) x = 0;
		if (mY+y+viewHeight>thh) y = 0;
		mX += x;
		mY += y;
		Log.d(TAG, "mx:"+mX +" my:"+mY + " x:"+x + " y:"+y + " thw:"+thw + " thh:"+thh);
	}
	public float getX(){
		return mX;
	}
	public float getY(){
		return mY;
	}

	public Mat createExtractMatrics(int width, int height, float cameraScale){
		Log.d(TAG, "createExtractMatrics widthidth:"+width + " height:"+height + " mscale:"+mScale + " camerascale:"+cameraScale);
		initFields();
		
		Bitmap bmp = drawBitmap(width, height, mScale, cameraScale);
		if (bmp==null) return null;
		

		Utils.bitmapToMat(bmp, mIntermediateMat);
		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat3, Imgproc.COLOR_RGB2HSV_FULL);

		int size = activity.getSmoothingFilterSize();
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
			if (area > BaseActivity.THRESHOLD_AREA ) {
				list.add(m);
			}
		}
		Mat rgba = new Mat();
		mIntermediateMat.copyTo(rgba);
		rgba.setTo(bl);
		Mat mask = Mat.zeros(mIntermediateMat.rows(), mIntermediateMat.cols(), CvType.CV_8UC1);
		Imgproc.drawContours(mask, list, -1, wh, Core.FILLED);

		if (activity.getDilateErudeTimes()>0){
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
		} else if  (activity.getDilateErudeTimes()<0){
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
		}
		
		if (activity.getInvertMask())
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
	

	public Pair<Mat, Mat> createMargeMatrics(int width, int height){
		Log.d(TAG, "createMargeMatrics width:"+width + " height:"+height + " mscale:"+mScale);
		initFields();
		Bitmap bmp = drawBitmap(width, height, mScale, 1);
		if (bmp==null) return null;
		Utils.bitmapToMat(bmp, mIntermediateMat);
		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat3, Imgproc.COLOR_RGB2HSV_FULL);

		int size = activity.getSmoothingFilterSize();
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
			if (area > BaseActivity.THRESHOLD_AREA ) {
				list.add(m);
			}
		}
		Mat mask = Mat.zeros(mIntermediateMat.rows(), mIntermediateMat.cols(), CvType.CV_8UC1);
		Imgproc.drawContours(mask, list, -1, wh, Core.FILLED);

		if (activity.getDilateErudeTimes()>0){
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
		} else if  (activity.getDilateErudeTimes()<0){
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, activity.getDilateErudeTimes());
		}


		Mat rgba = new Mat(mIntermediateMat.height(), mIntermediateMat.width(), CvType.CV_8UC4);
		rgba.setTo(new Scalar(0,0,0,0));
		
		if (activity.getInvertMask())
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

	public Mat createMatrics(int width, int height){
		Log.d(TAG, "createMatrics width:"+width + " height:"+height + " mscale:"+mScale);
		Bitmap bmp = drawBitmap(width, height, mScale, 1);
		if (bmp==null) return null;
		Mat rgba = new Mat();
		Utils.bitmapToMat(bmp, rgba);
		bmp.recycle();
		return rgba;
	}
	
	private void initFields() {
		lo = new Scalar(
				activity.getHueLowerThreshold(),
				activity.getSaturationLowerThreshold(),
				activity.getBrightnessLowerThreshold()
				);
		hi = new Scalar(
				activity.getHueUpperThreshold(),
				activity.getSaturationUpperThreshold(),
				activity.getBrightnessUpperThreshold()
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
	
    public static class JavaScriptInterface {
    	ImgWebView imgWebView;
    	public JavaScriptInterface(ImgWebView iwv){
    		imgWebView = iwv;
    	}
        public int getContentWidth(String value) {
        	Log.d(TAG, "JavaScriptInterface.getContentWidth val:"+value);
            if (value != null) {
            	int width = Integer.parseInt(value);
            	imgWebView.mContentWidth = width;
                return width;
            } else 
            	return -1;
        }
    }

	public void initContentWidth() {
		mContentWidth = -1;
	}
}
