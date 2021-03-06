package com.hitsuji.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import com.util.Log;
import com.util.Pair;
import com.util.Util;

import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class CameraActivity extends BaseActivity {
	private static final String TAG = "CameraActivity";

	private Scalar bl= new Scalar(0, 0, 255, 255);
	private Scalar wh= new Scalar(255, 255, 255, 255);
	private Scalar bk= new Scalar(0, 0, 0, 0);
	private Mat mDefaultKernel3x3;
	private Mat mShapeningKernel3x3;
	private Point mDefP = new Point(-1, -1);
	
	public AtomicBoolean mDirtyPage = new AtomicBoolean(true);
	private float mScaleFactor = 1.0f;
	private ScaleGestureDetector mScaleGesDetector = null;
	private GestureDetector  mGesDetector = null;

	private Object mSaveImageLock = new Object();
	private Mat mHolder;

	private volatile Mat mWebviewImage;
	//private volatile Bitmap mWebviewBmp;
	
	private Handler mHandler;
	private ProgressBar mProgressBar;

	/** Called when the activity is first created. */
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
		setContentView(R.layout.camera);

		mCameraView = (CameraView) findViewById(R.id.camera_camera_view);
		mCameraView.setActivity(this);
		mCameraView.setCvCameraViewListener(this);
		mCameraView.enableFpsMeter();
		mCameraView.setCameraIndex(Camera.CameraInfo.CAMERA_FACING_BACK);
		
		viewMode = VIEW_MODE_CAMERA;
		mHandler = new Handler();
		
		mScaleGesDetector = new ScaleGestureDetector(this, mSGListener);
		mGesDetector = new  GestureDetector(this, mGListener);
		mGesDetector.setOnDoubleTapListener(mODTListener);

		
		setProgressBarIndeterminateVisibility(true);
		setProgressBarVisibility(true);
		mProgressBar = new ProgressBar(this, null, 
				android.R.attr.progressBarStyleHorizontal);

		mWebview = new ImgWebView(this);
		mWebview.getSettings().setJavaScriptEnabled(true);
		mWebview.addJavascriptInterface(new ImgWebView.JavaScriptInterface(mWebview), "HTMLOUT");
		mWebview.setWebChromeClient(new WebChromeClient(){
			public void onProgressChanged(WebView view, int progress) {
				Log.d(TAG, "ongprogresschanged:"+progress);
				if (mProgressBar.getVisibility() == ProgressBar.INVISIBLE){
					mProgressBar.setVisibility(ProgressBar.VISIBLE);
				}
				if (progress == 100) {
					mProgressBar.setVisibility(View.GONE);
					FrameLayout fl = (FrameLayout)CameraActivity.this.findViewById(R.id.cameralayout);
					fl.removeView(mProgressBar);		
				} else {
					mProgressBar.setProgress(progress);
				}
			}
		}); 
		mWebview.setWebViewClient(new WebViewClient(){
			public void onPageFinished(WebView view , String url){
				Log.d(TAG, "onpagefinished:"+url);
				mProgressBar.setVisibility(View.GONE);
				FrameLayout fl = (FrameLayout)CameraActivity.this.findViewById(R.id.cameralayout);
				fl.removeView(mProgressBar);
				mDirtyPage.set(false);
				String js = "javascript:"+
						"window.HTMLOUT.setContentWidth(document.getElementsByTagName('html')[0].scrollWidth);"+
						"window.HTMLOUT.setContentHeight(document.getElementsByTagName('html')[0].scrollHeight);" +
						"window.HTMLOUT.setDevicePixelRatio(window.devicePixelRatio);";
				mWebview.loadUrl(js);
				super.onPageFinished(view, url);
			}
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				mWebview.initContentSize();
				mHandler.post(new Runnable(){
					public void run(){
						mProgressBar.setVisibility(View.VISIBLE);
						FrameLayout fl = (FrameLayout)CameraActivity.this.findViewById(R.id.cameralayout);
						FrameLayout.LayoutParams fllp = new FrameLayout.LayoutParams(
								LayoutParams.MATCH_PARENT, 10);
						fllp.gravity = Gravity.TOP;
						fl.removeView(mProgressBar);
						fl.addView(mProgressBar, fllp);
					}
				});

			}
	        @Override
	        public void onScaleChanged(WebView view, float oldScale, float newScale) {
	            super.onScaleChanged(view, oldScale, newScale);
	            mWebview.setScale(newScale);
	        }
		}); 
		mWebview.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_INSET);
		mWebview.getSettings().setBuiltInZoomControls(true);
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");
		mItemCamera = menu.add("Camera");
		mItemWebview = menu.add("Browser");
		mItemExtract = menu.add("Masking");
		mItemMerge = menu.add("Merge");
		mItemSaveImage = menu.add("Save");
		mItemSettings = menu.add("Settings");
		return true;
	}


	@Override
	public void onResume() {
		super.onResume();
		setPrefs(this);
		Log.d(TAG, "onresume target:"+imageProcessTarget + " camerawidth:"+mCameraView.getWidth() + " cameraheight:"+mCameraView.getHeight());
		
		if (!hasCameraInfo()) {
			Intent intent = new Intent(CameraActivity.this,
					LoadCameraInfoActivity.class);
			intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		} else if (imageProcessTarget==TARGET_BROWSER) {
			//launch CameraActivity
			Intent intent = new Intent(CameraActivity.this,
					BrowserActivity.class);
			intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			return;
		}
		

		if (imageProcessTarget == TARGET_BROWSER) {
			switchView(viewMode, -1);
		} else {
			if (viewMode == VIEW_MODE_WEBVIEW) {
				switchView(viewMode, -1);
			} else if (viewMode == VIEW_MODE_EXTRACT ||
					viewMode == VIEW_MODE_MERGE) {
				switchView(viewMode, -1);
			} else if (viewMode == VIEW_MODE_WEBVIEW){
				switchView(viewMode, -1);
			} else {
				switchView(VIEW_MODE_CAMERA, -1);
			}
		}
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);

	}
	@Override
	protected int getCameraViewId(){
		return R.id.camera_camera_view;
	}

	public void switchView(int mode, int old) {
		if (mode == old) return;
		
		if (mode == VIEW_MODE_WEBVIEW) {
			if (mDirtyPage.get()) 
				mHandler.post(new Runnable(){
					public void run(){
						mWebview.loadUrl(url);
					}
				});			
		}
		if (old == VIEW_MODE_WEBVIEW) {
			refreshWebviewBmp();
		}
			
		switch(mode) {
		case VIEW_MODE_CAMERA:
			resetView(mCameraView);
			break;
		case VIEW_MODE_WEBVIEW:
			resetView(mWebview);
			break;
		case VIEW_MODE_EXTRACT:
			resetView(mCameraView);
			break;
		case VIEW_MODE_MERGE:
			resetView(mCameraView);
			break;
		default:
			break;
		}
		
	}
	public void refreshWebviewBmp(){
		if (mWebviewImage==null)
			mWebviewImage = new Mat();
		
		Bitmap bitmap;
		bitmap = mWebview.drawBitmap2(
			cameraWidth, cameraHeight,
			mCameraView.getWidth(), mCameraView.getHeight(),
			mCameraView.getScale());
		Utils.bitmapToMat(bitmap, mWebviewImage);
		
		Log.d(TAG,"refreshWebviewBmp webviewimage:"+ mWebviewImage);
		if (bitmap!=null)
			bitmap.recycle();
	}
	
	public void initWebView(int mode) {
		Mat mat;
		switch(mode) {
		case VIEW_MODE_CAMERA:
			break;
		case VIEW_MODE_WEBVIEW:
			break;
		case VIEW_MODE_EXTRACT:
			break;
		case VIEW_MODE_MERGE:
			mat = mWebview.createMatrics(this.cameraWidth, this.cameraHeight);
			setImageMatrics(mat);
			break;
		default:
			break;
		}
	}
	public void resetView(View view){
		FrameLayout fl;
		FrameLayout.LayoutParams fllp;
		fl = (FrameLayout)this.findViewById(R.id.cameralayout);
		fl.removeAllViews();
		fllp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		fllp.gravity = Gravity.CENTER;
		fl.addView(view, fllp);
	}

	public void setImageMatrics(Mat image) {
		Log.d(TAG, "setImageMatrics:"+image);
		mWebviewImage = image;
	}
	
	public void onCameraViewStarted(int width, int height) {
		Log.d(TAG, "onCameraViewStarted: width:"+width + " height:"+height);

		mWebview.setDefaultSize(width, height);
		
		mRgba = new Mat(height, width, CvType.CV_8UC4);
		mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
		mIntermediateMat2 = new Mat();
		mIntermediateMat3 = new Mat();

		mDefaultKernel3x3 = new Mat( 3, 3, CvType.CV_8U);
		mShapeningKernel3x3 = new Mat( 3, 3, CvType.CV_8U);
		Scalar sa = Scalar.all(-1.0);
		mShapeningKernel3x3.setTo(sa);
		mShapeningKernel3x3.put(1, 1, 1+8);
		mSaveImageHolder = new Mat();
	}

	public void onCameraViewStopped() {
		Log.d(TAG, "onCameraViewStopped");
		if (mRgba!=null)
			mRgba.release();
		if (mIntermediateMat != null)
			mIntermediateMat.release();
		if (mIntermediateMat2 != null)
			mIntermediateMat2.release();
		if (mIntermediateMat3 != null)
			mIntermediateMat3.release();
		if (mDefaultKernel3x3 != null)
			mDefaultKernel3x3.release();
		if (mShapeningKernel3x3 != null)
			mShapeningKernel3x3.release();
		if (mSaveImageHolder != null)
			mSaveImageHolder.release();
		
	}
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Log.d(TAG, "onCameraFrame:"+viewMode + " start");

		switch (viewMode) {
		default:
		case VIEW_MODE_CAMERA:
			setRgbaMatrics(inputFrame);
			break;
		case VIEW_MODE_EXTRACT:
			setExtractMatrics(inputFrame);
			break;
		case VIEW_MODE_MERGE:
			setMargeMatrics(inputFrame);
			break;
		case VIEW_MODE_SAVE:
			break;
		case VIEW_MODE_SETTINGS:
			break;

		}
		Log.d(TAG, "onCameraFrame:"+viewMode+ " end:" + mRgba);
		Log.d(TAG, "width:"+mRgba.cols()+ " height:"+mRgba.rows());
		mRgba.copyTo(mSaveImageHolder);
		return mRgba;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
		Intent intent;
		int old = viewMode;
		if (item == mItemCamera) {
			viewMode = VIEW_MODE_CAMERA;
			switchView(viewMode, old);
		} else if (item == mItemWebview) {
			viewMode = VIEW_MODE_WEBVIEW;
			switchView(viewMode, old);
		} else if (item == mItemExtract) {
			viewMode = VIEW_MODE_EXTRACT;
			switchView(viewMode, old);
		} else if (item == mItemMerge) {
			viewMode = VIEW_MODE_MERGE;
			switchView(viewMode, old);
		} else if (item == mItemSaveImage) {
			saveImage(this, this.mHandler);
		} else if (item == mItemSettings) {
			setViewMode( VIEW_MODE_CAMERA );
			intent = new Intent(this, com.hitsuji.android.Settings.class);
			intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			this.startActivity(intent);
		}

		return true;
	}

	private void setExtractMatrics(CvCameraViewFrame inputFrame){
		Log.d(TAG, "setExtractMatrics");
		setThreshold();
		mIntermediateMat = inputFrame.rgba();
		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat3, Imgproc.COLOR_RGB2HSV_FULL);

		int size = smoothingFilterSize;
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

		if (dilateErudeTimes>0){
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, dilateErudeTimes);
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, dilateErudeTimes);
		} else if  (dilateErudeTimes<0){
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP,dilateErudeTimes);
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, dilateErudeTimes);
		}
		
		if (invertMask)
			Core.bitwise_not(mask, mask);
		mIntermediateMat.copyTo(mRgba, mask);
		mask.release();
		for (int i=0; i<contours.size(); i++) {
			Mat m = contours.get(i);
			m.release();
		}
		contours.clear();		
	}
	
	private void setMargeMatrics(CvCameraViewFrame inputFrame){
		Log.d(TAG, "setMargeMatrics");
		setThreshold();

		mIntermediateMat = inputFrame.rgba();
		Imgproc.cvtColor(mIntermediateMat, mIntermediateMat3, Imgproc.COLOR_RGB2HSV_FULL);

		int size = smoothingFilterSize;
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

		Mat mask = Mat.zeros(mIntermediateMat.rows(), mIntermediateMat.cols(), CvType.CV_8UC1);
		Imgproc.drawContours(mask, list, -1, wh, Core.FILLED);

		if (dilateErudeTimes>0){
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, dilateErudeTimes);
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, dilateErudeTimes);
		} else if  (dilateErudeTimes<0){
			Imgproc.erode(mask, mask, 
					mDefaultKernel3x3, mDefP, dilateErudeTimes);
			Imgproc.dilate(mask, mask, 
					mDefaultKernel3x3, mDefP, dilateErudeTimes);
		}

		mRgba = new Mat(mIntermediateMat.height(), mIntermediateMat.width(), CvType.CV_8UC4);
		mRgba.setTo(bk);
		synchronized (mSaveImageLock) {
			Mat webviewImage = mWebviewImage;
			if (mWebviewImage !=null){
				if (invertMask)
					Core.bitwise_not(mask, mask);
				Core.add(mRgba, mIntermediateMat, mRgba, mask);
				Core.bitwise_not(mask, mIntermediateMat2);
				//Log.d(TAG, "webimagebmp w:"+webviewImage.getWidth() + " h:"+webImageBmp.getHeight());
				Log.d(TAG, "webviewimageimage:"+webviewImage);
				//Log.d(TAG, "rgba:"+mRgba + " webimage:"+ newMat);
				Core.add(mRgba, webviewImage, mRgba, mIntermediateMat2);
			}
		}
		mask.release();
		for (int i=0; i<contours.size(); i++) {
			Mat m = contours.get(i);
			m.release();
		}

		contours.clear();
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
			mCameraView.invalidate();
			return super.onScaleBegin(detector);
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			Log.d(TAG, "onScaleEnd : "+ detector.getScaleFactor() +
					" scale:"+mScaleFactor);
			//mScaleFactor *= detector.getScaleFactor();
			mCameraView.invalidate();
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
			//BrowserActivity.moveWebView(distanceX, distanceY);
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
	
	public void moveAndRedrawWebView(float x, float y) {
		mWebview.move(x, y);
		this.refreshWebviewBmp();
	}
}
