package com.hitsuji.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;

import com.util.Log;
import com.util.Pair;

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

public class TravelingBrowserActivity extends Activity {
	private static final String TAG = "TravelingBrowserActivity";
	
	private static final int CONNY_PARAM1 = 80;
	private static final int CONNY_PARAM2 = 100;
	
	
	public static final int VIEW_MODE_CAMERA  = 0;
	public static final int VIEW_MODE_WEBVIEW = 2;
	public static final int VIEW_MODE_EXTRACT = 3;
	public static final int VIEW_MODE_MARGE = 5;
	public static final int VIEW_MODE_SETTINGS = 7;
	
	public final static boolean TARGET_CAMERA = false;
	public final static boolean TARGET_BROWSER = true;
	
	private MenuItem mItemPreviewCamera;
	private MenuItem mItemPreviewExtract;
	private MenuItem mItemPreviewWebview;
	private MenuItem mItemPreviewMerge;
	private MenuItem mItemSaveImage;
	private MenuItem mItemSettings;

	private Handler mHandler;
	private Handler mProcHandler;
	private HandlerThread mProcThread = new HandlerThread("proc");
	private ImgWebView mWebview;
	private WebViewCameraView mImgview;
	
	private static boolean InvertMask;
	private static int SmoothingFilterSize;
	private static int DilateErudeTimes;
	private static int CannyParam1;
	private static int CannyParam2;
	private static String Url;
	private static String SavePath;
	private static int HueUpperThreshold;
	private static int HueLowerThreshold;
	private static int SaturationUpperThreshold;
	private static int SaturationLowerThreshold;
	private static int BrightnessUpperThreshold;
	private static int BrightnessLowerThreshold;
	private static boolean ImageProcessTarget;
	
	private ProgressBar mProgressBar;
	
	//private static int ViewMode = VIEW_MODE_CAMERA;
	private static int ViewMode = VIEW_MODE_WEBVIEW;
	
	public AtomicBoolean mDirtyPage = new AtomicBoolean(true);
	public static AtomicBoolean mViewLock = new AtomicBoolean(false);
	
	private static TravelingBrowserActivity self;
	
	public TravelingBrowserActivity() {
		super();
		Log.v(TAG, "Instantiated new " + this.getClass());
		self = this;
	}

	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);		
		setContentView(R.layout.browser);
		
		mHandler = new Handler();
		mProcThread.start();
		mProcHandler = new Handler(mProcThread.getLooper());
		
		setProgressBarIndeterminateVisibility(true);
		setProgressBarVisibility(true);
		mProgressBar = new ProgressBar(this, null, 
				android.R.attr.progressBarStyleHorizontal);
		
		mImgview = new WebViewCameraView(this);
		mWebview = new ImgWebView(this);
		mWebview.setWebChromeClient(new WebChromeClient(){
			public void onProgressChanged(WebView view, int progress) {
				Log.d(TAG, "ongprogresschanged:"+progress);
				if (mProgressBar.getVisibility() == ProgressBar.INVISIBLE){
					mProgressBar.setVisibility(ProgressBar.VISIBLE);
				}
				if (progress == 100) {
					mProgressBar.setVisibility(View.GONE);
					FrameLayout fl = (FrameLayout)TravelingBrowserActivity.this.findViewById(R.id.browserlayout);
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
				FrameLayout fl = (FrameLayout)TravelingBrowserActivity.this.findViewById(R.id.browserlayout);
				fl.removeView(mProgressBar);
				mDirtyPage.set(false);						
				super.onPageFinished(view, url);
			}
		  @Override
		  public void onPageStarted(WebView view, String url, Bitmap favicon) {
				mHandler.post(new Runnable(){
					public void run(){
						mProgressBar.setVisibility(View.VISIBLE);
						FrameLayout fl = (FrameLayout)TravelingBrowserActivity.this.findViewById(R.id.browserlayout);
						FrameLayout.LayoutParams fllp = new FrameLayout.LayoutParams(
								LayoutParams.MATCH_PARENT, 10);
						fllp.gravity = Gravity.TOP;
						fl.removeView(mProgressBar);
						fl.addView(mProgressBar, fllp);
					}
				});
		    
		    }
		}); 
		mWebview.getSettings().setJavaScriptEnabled(true);
		mWebview.getSettings().setPluginsEnabled(true);
		mWebview.setScrollBarStyle(WebView.SCROLLBARS_INSIDE_INSET);
		mWebview.getSettings().setBuiltInZoomControls(true);

		FrameLayout.LayoutParams p = null;
		if (TravelingCameraViewBase.getFrameWidth() > 0) {
			p = new FrameLayout.LayoutParams(
					TravelingCameraViewBase.getFrameWidth(),
					TravelingCameraViewBase.getFrameHeight()); 
		} else {
			p = new FrameLayout.LayoutParams(
					LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
		}
		p.gravity = Gravity.CENTER;
		FrameLayout fl = (FrameLayout)this.findViewById(R.id.browserlayout);
		if (ImageProcessTarget==TARGET_CAMERA) {
			fl.addView(mWebview, p);
		} else {
			fl.addView(mImgview, p);
		}
		//
		
		TravelingBrowserActivity.setCascade(this);
	}

	@Override
	public void onResume(){
		Log.d(TAG, "onresume");
		super.onResume();
		setPrefs(this);
		if (mDirtyPage.get()) 
			mHandler.post(new Runnable(){
				public void run(){
					mWebview.loadUrl(Url);
				}
			});

		boolean flag = true;
		if (ImageProcessTarget==TARGET_CAMERA) {
			if (ViewMode != VIEW_MODE_WEBVIEW){
				Intent intent = new Intent(TravelingBrowserActivity.this,
						TravelingCameraActivity.class);
				intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				TravelingBrowserActivity.this.startActivity(intent);
				flag = false;
			}
		}

		if (TravelingCameraActivity.getCameraWidth()>0 &&
				TravelingCameraActivity.getCameraHeight()>0 ) {
			TravelingBrowserActivity.setDefaultWebviewSize(
					TravelingCameraActivity.getCameraWidth(),
					TravelingCameraActivity.getCameraHeight());
		}

		if (ImageProcessTarget == TARGET_CAMERA) {
			switchView(ViewMode, -1);
			flag = false;
		} else {
			if (ViewMode == VIEW_MODE_WEBVIEW) {
				switchView(ViewMode, -1);
				flag = false;
			} else if (ViewMode == VIEW_MODE_EXTRACT ||
					ViewMode == VIEW_MODE_MARGE) {
				switchView(ViewMode, -1);
				flag = false;
			} else if (ViewMode == VIEW_MODE_CAMERA){
				switchView(ViewMode, -1);
			}
			
		}
		if (flag) this.mImgview.setCameraFlag(false);
		else mImgview.setCameraFlag(true);
		Log.d(TAG, "onresume end:"+flag);
	}
	
	@Override
	public void onPause(){
		Log.d(TAG, "onpause");
		mImgview.setCameraFlag(true);
		super.onPause();
	}
	
	public static void setPrefs(Context context){
		SharedPreferences prefs;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		InvertMask = prefs.getBoolean("InvertMask", false);
		SmoothingFilterSize = prefs.getInt("SmoothingFilterSize", 1);
		if (SmoothingFilterSize % 2 == 0) {
			SmoothingFilterSize += 1;
		}
		HueUpperThreshold = prefs.getInt("HueUpperThreshold", 100);
		HueLowerThreshold = prefs.getInt("HueLowerThreshold", 0);
		SaturationUpperThreshold = prefs.getInt("SaturationUpperThreshold", 100);
		SaturationLowerThreshold = prefs.getInt("SaturationLowerThreshold", 0);
		BrightnessUpperThreshold = prefs.getInt("BrightnesUpperThreshold", 100);
		BrightnessLowerThreshold = prefs.getInt("BrightnesLowerThreshold", 0);
		ImageProcessTarget = prefs.getBoolean("ImageProcessTarget", false);
		
		CannyParam1 = CONNY_PARAM1;
		CannyParam2 = CONNY_PARAM2;
		DilateErudeTimes = prefs.getInt("DilateErudeTimes", 3);
		Url = prefs.getString("MixedPageUrl", "http://www.google.com");
		SavePath = prefs.getString("SaveImagePath","/mnt/sdcard/TravelingCamera.png");
		Log.d(TAG, "onresume filtersize:"+SmoothingFilterSize +
				"dilateerudetime:"+ DilateErudeTimes+
				" cannyparam1:"+CannyParam1 + " cannyparam2:"+CannyParam2);		
	}
	
	public static int getSmoothingFilterSize(){
		return SmoothingFilterSize;
	}
	public static int getCanneyParam1(){
		return CannyParam1;
	}
	public static int getCanneyParam2(){
		return CannyParam2;
	}
	public static int getDilateErudeTimes(){
		return DilateErudeTimes;
	}
	public static String getSaveImagePath(){
		return SavePath;
	}
	public static boolean getInvertMask(){
		return InvertMask;
	}
	public static int getHueUpperThreshold(){
		return HueUpperThreshold;
	}
	public static int getHueLowerThreshold(){
		return HueLowerThreshold;
	}	
	public static int getSaturationUpperThreshold(){
		return SaturationUpperThreshold;
	}
	public static int getSaturationLowerThreshold(){
		return SaturationLowerThreshold;
	}	
	public static int getBrightnessUpperThreshold(){
		return BrightnessUpperThreshold;
	}
	public static int getBrightnessLowerThreshold(){
		return BrightnessLowerThreshold;
	}	
	public static boolean getTarget(){
		return ImageProcessTarget;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.v(TAG, "onCreateOptionsMenu");
		mItemPreviewWebview = menu.add("Browser");		
		mItemPreviewCamera = menu.add("Camera");
		mItemPreviewExtract = menu.add("Masking");
		mItemPreviewMerge = menu.add("Synthesis");
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
			if (item == mItemPreviewCamera) {
				setViewMode( VIEW_MODE_CAMERA );
				if (TravelingBrowserActivity.getTarget() ==
						TravelingBrowserActivity.TARGET_BROWSER) {
					if (old == VIEW_MODE_WEBVIEW)
						switchView(VIEW_MODE_CAMERA, old);
					mImgview.setCameraFlag(false);
				} else {
					intent = new Intent(this, TravelingCameraActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
					mImgview.setCameraFlag(true);
				}
			} else if (item == mItemPreviewWebview) {
				setViewMode( VIEW_MODE_WEBVIEW );
				switchView(VIEW_MODE_WEBVIEW, old);
				mImgview.setCameraFlag(true);
			} else if (item == mItemPreviewExtract) {
				setViewMode( VIEW_MODE_EXTRACT );
				if (TravelingBrowserActivity.getTarget() ==
						TravelingBrowserActivity.TARGET_CAMERA) {
					intent = new Intent(this, TravelingCameraActivity.class);
					startActivity(intent);
					mImgview.setCameraFlag(true);
				} else {
					if (old == VIEW_MODE_WEBVIEW)
						switchView(VIEW_MODE_EXTRACT, old);
					mImgview.setCameraFlag(false);
				}
			} else if (item == mItemPreviewMerge) {
				setViewMode( VIEW_MODE_MARGE );
				if (TravelingBrowserActivity.getTarget() ==
						TravelingBrowserActivity.TARGET_CAMERA) {
					intent = new Intent(TravelingBrowserActivity.this,
							TravelingCameraActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					TravelingBrowserActivity.this.startActivity(intent);
					mImgview.setCameraFlag(true);
				} else {
					if (old == VIEW_MODE_WEBVIEW)
						switchView(VIEW_MODE_MARGE, old);
					mImgview.setCameraFlag(false);
				}
			} else if (item == mItemSaveImage)
				mProcHandler.post(new Runnable(){
					boolean mRet;
					public void run(){
						if (TravelingBrowserActivity.getTarget() ==
								TravelingBrowserActivity.TARGET_CAMERA) {
							Bitmap bitmap = TravelingBrowserActivity.drawBitmap(
									mWebview.getWidth(), 
									mWebview.getHeight(), 1.0f);
							try {
								FileOutputStream out = new FileOutputStream(SavePath);
							  mRet = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
							} catch (Exception e) {
							   e.printStackTrace();
							   mRet = false;
							}
						} else {
							if (ViewMode==VIEW_MODE_WEBVIEW) {
								Bitmap bitmap = mWebview.drawBitmap();
								try {
									FileOutputStream out = new FileOutputStream(SavePath);
								  mRet = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
								} catch (Exception e) {
								   e.printStackTrace();
								   mRet = false;
								}
							} else {
								String path = TravelingBrowserActivity.getSaveImagePath();
								mRet = mImgview.saveImage(path);
							}
						}
						mHandler.post(new Runnable(){
							public void run(){
								if (mRet) 
									Toast.makeText(TravelingBrowserActivity.this, "saved image: " + SavePath, Toast.LENGTH_SHORT).show();
								else
									Toast.makeText(TravelingBrowserActivity.this, "fail to save image: " + SavePath, Toast.LENGTH_SHORT).show();
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
				mImgview.setCameraFlag(true);
				mDirtyPage.set(true);
        intent = new Intent(this, com.hitsuji.android.Settings.class);
				intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        this.startActivity(intent);
        //this.finish();
			}
		}
		return true;
	}
	
	public void switchView(int mode, int old) {
		FrameLayout fl;
		FrameLayout.LayoutParams fllp;
		Mat mat;
		
		if (mode == old) return;
		switch(mode) {
		case VIEW_MODE_CAMERA:
			resetView(mImgview);
			//fl = (FrameLayout)this.findViewById(R.id.browserlayout);
			//fl.removeAllViews();
			//mImgview.setVisibility(View.INVISIBLE);
			//fllp = new FrameLayout.LayoutParams(
			//		LayoutParams.MATCH_PARENT,
			//		LayoutParams.MATCH_PARENT);
			//fllp.gravity = Gravity.CENTER;
			//fl.addView(mImgview, fllp);	
			break;
		case VIEW_MODE_WEBVIEW:
			resetView(mWebview);
				//fl = (FrameLayout)this.findViewById(R.id.browserlayout);
				//fl.removeAllViews();
				//mImgview.setVisibility(View.INVISIBLE);
				//fllp = new FrameLayout.LayoutParams(
				//		LayoutParams.MATCH_PARENT,
				//		LayoutParams.MATCH_PARENT);
				//fllp.gravity = Gravity.CENTER;
				//fl.addView(mWebview, fllp);
				//mWebview.setLayoutParams(fllp);
				//mWebview.setVisibility(View.VISIBLE);
				break;
			case VIEW_MODE_EXTRACT:
				resetView(mImgview);
				mat = mWebview.createExtractMatrics();
				mImgview.setImageMatrics(mat);
				//fl = (FrameLayout)this.findViewById(R.id.browserlayout);
				//fl.removeAllViews();
				//fllp = new FrameLayout.LayoutParams(
				//		LayoutParams.MATCH_PARENT,
				//		LayoutParams.MATCH_PARENT);
				//fllp.gravity = Gravity.CENTER;
				//fl.addView(mImgview, fllp);
				//mWebview.setVisibility(View.INVISIBLE);
				//mImgview.setLayoutParams(fllp);
				//mImgview.setVisibility(View.VISIBLE);
				//bmp.recycle();
				break;
			case VIEW_MODE_MARGE:
				//Mat cap = TravelingCameraActivity.captureCamera();
				//if (cap==null) return;
				resetView(mImgview);
				Pair<Mat, Mat> caps = mWebview.createMargeMatrics();
				mImgview.setImageMatrics(caps);
				//fl = (FrameLayout)this.findViewById(R.id.browserlayout);
				//fl.removeAllViews();
				
				//fllp = new FrameLayout.LayoutParams(
				//		mWebview.getDefaultWidth()>0 ? mWebview.getDefaultWidth() : LayoutParams.MATCH_PARENT,
				//		LayoutParams.MATCH_PARENT);
				//fllp.gravity = Gravity.CENTER;
				//fl.addView(mImgview, fllp);
				//mWebview.setVisibility(View.INVISIBLE);
				//mImgview.setLayoutParams(fllp);
				//mImgview.setVisibility(View.VISIBLE);
				//bmp.recycle();
				break;
				
			default:
				break;
		}
	}
	
	public void resetView(View view){
		FrameLayout fl;
		FrameLayout.LayoutParams fllp;
		fl = (FrameLayout)this.findViewById(R.id.browserlayout);
		fl.removeAllViews();
		fllp = new FrameLayout.LayoutParams(
				mWebview.getDefaultWidth()>0 ? mWebview.getDefaultWidth() : LayoutParams.MATCH_PARENT,
				mWebview.getDefaultHeight()>0 ? mWebview.getDefaultHeight() : LayoutParams.MATCH_PARENT);
		fllp.gravity = Gravity.CENTER;
		fl.addView(view, fllp);
	}
	
	
	public static int getViewMode(){
		synchronized (mViewLock){
			return ViewMode;
		}
	}
	public static void setViewMode(int mode){
		synchronized (mViewLock){
			ViewMode = mode;
		}
	}
	public static void makePageDirty(){
		if (self!=null)self.mDirtyPage.set(true);
	}
	
	public static Bitmap drawBitmap(int width, int height, float scale) {
		return self==null || self.mWebview==null ? null : self.mWebview.drawBitmap(width, height, scale);
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// When the user center presses, let them pick a contact.
			 if(TravelingBrowserActivity.getTarget() !=
				  TravelingBrowserActivity.TARGET_BROWSER ||
				  ViewMode == VIEW_MODE_WEBVIEW){
				 if (mWebview != null && mWebview.canGoBack() && !mViewLock.get()) {
					 mWebview.goBack();
					 return true;
				 }
			 }
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	private CascadeClassifier   mCascade;
	public static CascadeClassifier getCascade(){
		return self!=null ? self.mCascade : null;
	}
	public static void setCascade(Context context) {
		if (self.mCascade!=null) return;
		try {
			InputStream is = context.getResources().openRawResource(R.raw.lbpcascade_frontalface);
			File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
			File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
			FileOutputStream os = new FileOutputStream(cascadeFile);

			byte[] buffer = new byte[4096];
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			is.close();
			os.close();

			self.mCascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
			if (self.mCascade.empty()) {
				Log.e(TAG, "Failed to load cascade classifier");
				self.mCascade = null;
			} else
				Log.v(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

			cascadeFile.delete();
			cascadeDir.delete();

		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
		}
	}

	public static void setWebviewSize(int width, int height) {
		// TODO Auto-generated method stub
		if(self==null || self.mWebview==null) return;
		final int w = width;
		final int h = height;
		
		self.mHandler.post(new Runnable(){
			public void run(){
				/*
				int defw = self.mWebview.getDefaultWidth();
				int defh = self.mWebview.getDefaultHeight();
				int cw = self.mWebview.getContentWidth();
				int ch = self.mWebview.getContentHeight();
				if (cw==0 || ch==0) return;
				
				int thw = defw;
				int thh = defw;
				if (cw < defw) {
					thw = defw*defw/cw;
				}
				if (ch < defh) {
					thh = defh*defh/ch;
				}
				Log.d(TAG, "scale defw:"+defw + " defh:"+defh +
						" cw:"+cw + " ch"+ ch + 
						" thw:"+thw + " thh:"+thh);
				
				int ww = w > thw ? w : thw;
				int hh = h > thh ? h : thh;
				}*/
				FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
						w, h);
				p.gravity = Gravity.CENTER;
				//self.mWebview.movePrescale(ww, hh);
				self.mWebview.setLayoutParams( p );
			}
		});
	}



	public static int setDefaultWebviewSize(int width, int height) {
		// TODO Auto-generated method stub
		if (width<0 || height<0 || self==null) return -1;
		Log.d(TAG, "setdefaultwebviewsize w:"+width + " h:"+height);
		self.mWebview.setDefaultSize(width, height);
		int cw = self.mWebview.getContentWidth();
		int ch = self.mWebview.getContentHeight();
		if (cw<=0) return -1;
		//setWebviewSize(width*width/cw, height*width/cw);
		setWebviewSize(width, height);
		return 0;
	}

	public static int getDefaultWebviewWidth(){
		return self.mWebview.getDefaultWidth();
	}
	public static int getDefaultWebviewHeight(){
		return self.mWebview.getDefaultHeight();
	}



	public static void moveWebView(float x, float y) {
		// TODO Auto-generated method stub
		self.mWebview.move(x, y);
	}
	public void moveAndRedrawWebView(float x, float y) {
		// TODO Auto-generated method stub
		self.mWebview.move(x, y);
		Mat mat;
		
		if (TravelingBrowserActivity.getTarget() == 
				TravelingBrowserActivity.TARGET_BROWSER) {
			switch (ViewMode) {
			case VIEW_MODE_EXTRACT:
				mat = mWebview.createExtractMatrics();
				mImgview.setImageMatrics(mat);
				break;
			case VIEW_MODE_MARGE:
				Pair<Mat, Mat> caps = mWebview.createMargeMatrics();
				mImgview.setImageMatrics(caps);
				break;
			default:
				break;
			}
		}
	}
}
