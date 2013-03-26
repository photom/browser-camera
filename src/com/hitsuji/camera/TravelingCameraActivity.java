package com.hitsuji.camera;

import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Mat;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import com.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.LinearLayout.LayoutParams;

public class TravelingCameraActivity extends Activity {
	private static final String TAG = "TravelingCameraActivity";
	
	public static final int VIEW_MODE_CAMERA  = 0;
	public static final int VIEW_MODE_WEBVIEW = 2;
	public static final int VIEW_MODE_EXTRACT = 3;
	public static final int VIEW_MODE_MARGE = 5;
	public static final int VIEW_MODE_SETTINGS = 7;
	
	private MenuItem mItemPreviewCamera;
	private MenuItem mItemPreviewExtract;
	private MenuItem mItemPreviewWebview;
	private MenuItem mItemPreviewMarge;
	private MenuItem mItemSaveImage;
	private MenuItem mItemSettings;

	private Handler mHandler;
	private Handler mProcHandler;
	private HandlerThread mProcThread = new HandlerThread("proc");
	private TravelingCameraView mCameraView;
	private static TravelingCameraActivity self;
	
	public TravelingCameraActivity() {
		Log.v(TAG, "Instantiated new " + this.getClass());
	}

	
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt( Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);		
		mCameraView = new TravelingCameraView(this);
		setContentView(R.layout.camera);
		FrameLayout fl = (FrameLayout) this.findViewById(R.id.cameralayout);
		fl.addView(mCameraView);
		
		mHandler = new Handler();
		mProcThread.start();
		mProcHandler = new Handler(mProcThread.getLooper());
		self = this;
	}

	@Override
	public void onStart(){
		super.onStart();
		
	}
	@Override
	public void onResume(){
		super.onResume();
		Log.d(TAG, "onresume");
		boolean flag = true;
		TravelingBrowserActivity.setPrefs(this);
		if (TravelingBrowserActivity.getTarget() == 
				TravelingBrowserActivity.TARGET_BROWSER) {
			if (TravelingBrowserActivity.getViewMode() !=
					TravelingBrowserActivity.VIEW_MODE_CAMERA) {
				Intent intent = new Intent(this,
						TravelingBrowserActivity.class);
				intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				this.startActivity(intent);
				flag = false;
			}
		}
		if (flag) mCameraView.setCameraFlag(false);
		else mCameraView.setCameraFlag(true);
		Log.d(TAG, "onresume end:"+flag);
	}
	
	@Override
	public void onPause(){
		Log.d(TAG, "onpause");
		mCameraView.setCameraFlag(true);
		super.onPause();
	}

	@Override
	public void onStop(){
		super.onStop();
		
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.v(TAG, "onCreateOptionsMenu");
		mItemPreviewWebview = menu.add("Browser");		
		mItemPreviewCamera = menu.add("Camera");
		mItemPreviewExtract = menu.add("Masking");
		mItemPreviewMarge = menu.add("Synthesis");
		mItemSaveImage = menu.add("Save");
		mItemSettings = menu.add("Settings");
		
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.v(TAG, "Menu Item selected " + item);
		Intent intent;
		synchronized (this){
			if (item == mItemPreviewCamera) {
				TravelingBrowserActivity.setViewMode( VIEW_MODE_CAMERA );
				if (TravelingBrowserActivity.getTarget() ==
						TravelingBrowserActivity.TARGET_BROWSER) {
					mCameraView.setCameraFlag(true);
					intent = new Intent(this, TravelingBrowserActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
					
				}				
			} else if (item == mItemPreviewWebview) {
				TravelingBrowserActivity.setViewMode( VIEW_MODE_WEBVIEW );
				mCameraView.setCameraFlag(true);
				if (TravelingBrowserActivity.getTarget() ==
						TravelingBrowserActivity.TARGET_CAMERA) {
					TravelingCameraActivity.this.finish();
				} else {
					intent = new Intent(this, TravelingBrowserActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
				}
			} else if (item == mItemPreviewExtract) {
				TravelingBrowserActivity.setViewMode( VIEW_MODE_EXTRACT );
				if (TravelingBrowserActivity.getTarget() ==
						TravelingBrowserActivity.TARGET_BROWSER) {
					mCameraView.setCameraFlag(true);
					intent = new Intent(this, TravelingBrowserActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
					
				}
			} else if (item == mItemPreviewMarge) {
				TravelingBrowserActivity.setViewMode( VIEW_MODE_MARGE );
				if (TravelingBrowserActivity.getTarget() ==
						TravelingBrowserActivity.TARGET_BROWSER) {
					mCameraView.setCameraFlag(true);
					intent = new Intent(this, TravelingBrowserActivity.class);
					intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
					startActivity(intent);
				}
			} else if (item == mItemSaveImage)
				mProcHandler.post(new Runnable(){
					boolean mRet;
					public void run(){
						String path = TravelingBrowserActivity.getSaveImagePath();
						mRet = mCameraView.saveImage(path);
						mHandler.post(new Runnable(){
							public void run(){
								String path = TravelingBrowserActivity.getSaveImagePath();
								if (mRet) 
									Toast.makeText(TravelingCameraActivity.this, "saved image: " + path, Toast.LENGTH_SHORT).show();
								else
									Toast.makeText(TravelingCameraActivity.this, "fail to save image: " + path, Toast.LENGTH_SHORT).show();
							}
						});
					}
				});
			else if (item == mItemSettings){
				//TravelingBrowserActivity.setViewMode( VIEW_MODE_SETTINGS );
				mCameraView.setCameraFlag(true);
        intent = new Intent(this, com.hitsuji.android.Settings.class);
				intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				TravelingBrowserActivity.makePageDirty();
        this.startActivity(intent);
        //this.finish();
			}
		}
		return true;
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK){
			TravelingBrowserActivity.setViewMode(VIEW_MODE_WEBVIEW);
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public static Mat captureCamera(){
		return self==null ? null : self.mCameraView.captureCamera();
	}
	
	public static int getCameraWidth(){
		return self==null ? -1 : self.mCameraView.getCameraWidth();
	}
	public static int getCameraHeight(){
		return self==null ? -1 : self.mCameraView.getCameraHeight();
	}
}
