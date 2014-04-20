package com.hitsuji.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;

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

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class BrowserActivity extends BaseActivity   {
	private static final String TAG = "BrowserActivity";

	private Handler mHandler;

	public static AtomicBoolean mViewLock = new AtomicBoolean(false);

	private ProgressBar mProgressBar;

	public AtomicBoolean mDirtyPage = new AtomicBoolean(true);
	private volatile Mat mWebviewImage;
	private volatile Mat mWebviewMask;

	protected int getCameraViewId(){
		return R.id.browser_camera_view;
	}
	/** Called when the activity is first created. */
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);		
		setContentView(R.layout.browser);
		
		viewMode = VIEW_MODE_WEBVIEW;
		
		mHandler = new Handler();

		setProgressBarIndeterminateVisibility(true);
		setProgressBarVisibility(true);
		mProgressBar = new ProgressBar(this, null, 
				android.R.attr.progressBarStyleHorizontal);

		mCameraView = (CameraView) findViewById(R.id.browser_camera_view);
		mCameraView.setActivity(this);
		mCameraView.setCvCameraViewListener(this);
		mCameraView.enableFpsMeter();
		mCameraView.setCameraIndex(Camera.CameraInfo.CAMERA_FACING_BACK);
		
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
					FrameLayout fl = (FrameLayout)BrowserActivity.this.findViewById(R.id.browserlayout);
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
				FrameLayout fl = (FrameLayout)BrowserActivity.this.findViewById(R.id.browserlayout);
				fl.removeView(mProgressBar);
				mDirtyPage.set(false);
				mWebview.loadUrl("javascript:window.HTMLOUT.getContentWidth(document.getElementsByTagName('html')[0].scrollWidth);");
				super.onPageFinished(view, url);
			}
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				mWebview.initContentWidth();
				mHandler.post(new Runnable(){
					public void run(){
						mProgressBar.setVisibility(View.VISIBLE);
						FrameLayout fl = (FrameLayout)BrowserActivity.this.findViewById(R.id.browserlayout);
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
	public void onResume(){
		Log.d(TAG, "onresume");
		super.onResume();

		setPrefs(this);

		if (!hasCameraInfo()) {
			Intent intent = new Intent(BrowserActivity.this,
					LoadCameraInfoActivity.class);
			intent.setFlags(intent.getFlags() | 
					Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
		} else if (imageProcessTarget==TARGET_CAMERA) {
			//launch CameraActivity
			Intent intent = new Intent(BrowserActivity.this,
					CameraActivity.class);
			intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			startActivity(intent);
			this.finish();
			return;
		} else {
			this.setDefaultWebviewSize(cameraViewWidth, cameraViewHeight);
		}
		if (mDirtyPage.get()) 
			mHandler.post(new Runnable(){
				public void run(){
					mWebview.loadUrl(url);
				}
			});

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);
		
		if (imageProcessTarget == TARGET_CAMERA) {
			switchView(viewMode, -1);
		} else {
			if (viewMode == VIEW_MODE_WEBVIEW) {
				switchView(viewMode, -1);
			} else if (viewMode == VIEW_MODE_EXTRACT ||
					viewMode == VIEW_MODE_MERGE) {
				switchView(viewMode, -1);
			} else if (viewMode == VIEW_MODE_CAMERA){
				switchView(viewMode, -1);
			} else {
				switchView(VIEW_MODE_WEBVIEW, -1);
			}
		}

		Log.d(TAG, "onresume end");
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.v(TAG, "onCreateOptionsMenu");
		mItemWebview = menu.add("Browser");		
		mItemCamera = menu.add("Camera");
		mItemExtract = menu.add("Masking");
		mItemMerge = menu.add("Merge");
		mItemSaveImage = menu.add("Save");
		mItemSettings = menu.add("Settings");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.v(TAG, "Menu Item selected " + item);
		Intent intent;
		final int old = getViewMode();

		synchronized (this){
			if (item == mItemCamera) {
				setViewMode( VIEW_MODE_CAMERA );
				if (imageProcessTarget ==
						BrowserActivity.TARGET_BROWSER) {
					if (old == VIEW_MODE_WEBVIEW)
						switchView(VIEW_MODE_CAMERA, old);
					//mImgview.setCameraFlag(false);
				} else {
					intent = new Intent(this, CameraActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
					this.finish();
				}
			} else if (item == mItemWebview) {
				setViewMode( VIEW_MODE_WEBVIEW );
				switchView(VIEW_MODE_WEBVIEW, old);
				//mImgview.setCameraFlag(true);
			} else if (item == mItemExtract) {
				setViewMode( VIEW_MODE_EXTRACT );
				if (imageProcessTarget ==
						BrowserActivity.TARGET_CAMERA) {
					intent = new Intent(this, CameraActivity.class);
					startActivity(intent);
					//mImgview.setCameraFlag(true);
				} else {
					if (old != VIEW_MODE_EXTRACT)
						switchView(VIEW_MODE_EXTRACT, old);
					//mImgview.setCameraFlag(false);
				}
			} else if (item == mItemMerge) {
				setViewMode( VIEW_MODE_MERGE );
				if (imageProcessTarget ==
						BrowserActivity.TARGET_CAMERA) {
					intent = new Intent(BrowserActivity.this,
							CameraActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					BrowserActivity.this.startActivity(intent);
					//mImgview.setCameraFlag(true);
				} else {
					if (old != VIEW_MODE_MERGE)
						switchView(VIEW_MODE_MERGE, old);
					//mImgview.setCameraFlag(false);
				}
			} else if (item == mItemSaveImage)
				mProcHandler.post(new Runnable(){
					boolean mRet;
					public void run(){
						File dirF = new File(savePath);
						if (!dirF.exists())
							dirF.mkdirs();
						
						if (viewMode==VIEW_MODE_WEBVIEW) {
							Bitmap bitmap = mWebview.drawBitmap(
									cameraWidth, cameraHeight,
									mCameraView.getScale());
							try {
								FileOutputStream out = new FileOutputStream(savePath+Util.currentTime() + ".png");
								mRet = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
							} catch (Exception e) {
								e.printStackTrace();
								mRet = false;
							}
						} else {
							Bitmap bitmap = Bitmap.createBitmap(mSaveImageHolder.cols(), mSaveImageHolder.rows(), Config.ARGB_8888); 
							Utils.matToBitmap(mSaveImageHolder, bitmap);
							try {
								FileOutputStream out = new FileOutputStream(savePath+Util.currentTime() + ".png");
							  mRet = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
							  bitmap.recycle();
							} catch (Exception e) {
							   e.printStackTrace();
							   mRet = false;
							}
						}
						
						mHandler.post(new Runnable(){
							public void run(){
								if (mRet) 
									Toast.makeText(BrowserActivity.this, "saved image: " + savePath, Toast.LENGTH_SHORT).show();
								else
									Toast.makeText(BrowserActivity.this, "fail to save image: " + savePath, Toast.LENGTH_SHORT).show();
							}
						});
					}
				});
			else if (item == mItemSettings){
				setViewMode( VIEW_MODE_WEBVIEW );
				mHandler.post(new Runnable(){
					public void run(){
						switchView(VIEW_MODE_WEBVIEW, old);
					}
				});
				//mImgview.setCameraFlag(true);
				mDirtyPage.set(true);
				intent = new Intent(this, com.hitsuji.android.Settings.class);
				intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				this.startActivity(intent);
			}
		}
		return true;
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		Log.d(TAG, "onActivityResult requestCode:"+requestCode + " resultCode:"+resultCode);
		if (resultCode == RET_CAMERA_ACTIVITY) {
			finish();
		}
	}
	public void switchView(int mode, int old) {
		if (mode == old) return;
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
		
		initView(mode);
	}
	public void initView(int mode) {
		Mat mat;
		switch(mode) {
		case VIEW_MODE_CAMERA:
			break;
		case VIEW_MODE_WEBVIEW:
			break;
		case VIEW_MODE_EXTRACT:
			mat = mWebview.createExtractMatrics(this.cameraWidth, this.cameraHeight, this.mCameraView.getScale());
			Log.d(TAG, "switchview mode:"+mode+" extract width:"+mat.cols() + " height:"+mat.rows());
			setImageMatrics(mat, null);
			break;
		case VIEW_MODE_MERGE:
			Pair<Mat, Mat> caps = mWebview.createMargeMatrics(this.cameraWidth, this.cameraHeight);
			setImageMatrics(caps.getFirst(), caps.getSecond());
			break;
		default:
			break;
		}
	}
	public void setImageMatrics(Mat image, Mat mask) {
		Log.d(TAG, "setImageMatrics:"+image + " mask:"+mask);
		mWebviewImage = image;
		if (mask!=null) {
			mWebviewMask = mask;
			Core.bitwise_not(mWebviewMask, mWebviewMask);
		}
	}

	public void resetView(View view){
		FrameLayout fl;
		FrameLayout.LayoutParams fllp;
		fl = (FrameLayout)this.findViewById(R.id.browserlayout);
		fl.removeAllViews();
		fllp = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT,
				LayoutParams.MATCH_PARENT);
		fllp.gravity = Gravity.CENTER;
		fl.addView(view, fllp);
	}

	/*
	public static void makePageDirty(){
		if (self!=null)self.mDirtyPage.set(true);
	}

	public static Bitmap drawBitmap(int width, int height, float scale) {
		return self==null || self.mWebview==null ? null : self.mWebview.drawBitmap(width, height, scale);
	}*/
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// When the user center presses, let them pick a contact.
			if(imageProcessTarget !=
					BrowserActivity.TARGET_BROWSER ||
					viewMode == VIEW_MODE_WEBVIEW){
				if (mWebview != null && mWebview.canGoBack() && !mViewLock.get()) {
					mWebview.goBack();
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}



	public void setWebviewSize(final int width, final int height) {
		mHandler.post(new Runnable(){
			public void run(){

				FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				p.gravity = Gravity.CENTER;
				//self.mWebview.movePrescale(ww, hh);
				mWebview.setLayoutParams( p );
			}
		});
	}



	public int setDefaultWebviewSize(int width, int height) {
		Log.d(TAG, "setdefaultwebviewsize w:"+width + " h:"+height);
		mWebview.setDefaultSize(width, height);
		int cw = mWebview.getContentWidth();
		int ch = mWebview.getContentHeight();
		if (cw<=0 || ch<=0) return -1;
		//setWebviewSize(width*width/cw, height*width/cw);
		//setWebviewSize(width, height);
		return 0;
	}

	@Override
	public void onCameraViewStarted(int width, int height) {
		Log.d(TAG, "onCameraViewStarted width:"+width + " height:"+height);
		if (width<=0 || height<=0)
			return;

		if (viewMode != VIEW_MODE_CAMERA){
			setDefaultWebviewSize(cameraViewWidth, cameraViewHeight);
			initView(getViewMode());
		}
		mSaveImageHolder = new Mat();
	}

	@Override
	public void onCameraViewStopped() {
		Log.d(TAG, "onCameraViewStopped");
		if (mWebviewImage!=null)
			mWebviewImage.release();
		if (mWebviewMask!=null)
			mWebviewMask.release();
		if (mRgba!=null)
			mRgba.release();
		if (mSaveImageHolder != null)
			mSaveImageHolder.release();
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		if (mRgba!=null){
			mRgba.release();
			mRgba = null;
		}
		setRgbaMatrics(inputFrame);
Log.d(TAG, "target:"+getTarget() + " viewmode:"+getViewMode() + 
		" cameraimg:"+mRgba + " webviewimage:" + mWebviewImage);
		if (getTarget() == 
				BrowserActivity.TARGET_BROWSER &&
				getViewMode() ==
				BrowserActivity.VIEW_MODE_CAMERA){
			Log.d(TAG, "width:"+mRgba.cols()+ " height:"+mRgba.rows());
		} else if (getTarget() == BrowserActivity.TARGET_BROWSER &&
				getViewMode() == BrowserActivity.VIEW_MODE_EXTRACT){
			if (mWebviewImage!=null) {
				mRgba.release();
				mRgba = mWebviewImage.clone();
			}
		} else if (getTarget() == BrowserActivity.TARGET_BROWSER &&
				getViewMode() == BrowserActivity.VIEW_MODE_MERGE){
			Mat tmp = null;
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
		mRgba.copyTo(mSaveImageHolder);
		return mRgba;
	}

	public void moveAndRedrawWebView(float x, float y) {
		mWebview.move(x, y);
		Mat mat;
		
		if (getTarget() == TARGET_BROWSER) {
			switch (viewMode) {
			case VIEW_MODE_EXTRACT:
				mat = mWebview.createExtractMatrics(this.cameraWidth, this.cameraHeight, mCameraView.getScale());
				setImageMatrics(mat, null);
				break;
			case VIEW_MODE_MERGE:
				Pair<Mat, Mat> caps = mWebview.createMargeMatrics(this.cameraWidth, this.cameraHeight);
				setImageMatrics(caps.getFirst(), caps.getSecond());
				break;
			default:
				break;
			}
		}
	}

}
