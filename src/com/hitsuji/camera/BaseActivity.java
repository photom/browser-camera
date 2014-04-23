package com.hitsuji.camera;

import java.io.File;
import java.io.FileOutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.Utils;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import com.util.Log;
import com.util.Util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

public abstract class BaseActivity extends Activity implements CvCameraViewListener2{
	private static final String TAG = "BaseActivity";
	
	private static final int CONNY_PARAM1 = 80;
	private static final int CONNY_PARAM2 = 100;
	public final static boolean TARGET_BROWSER = false;
	public final static boolean TARGET_CAMERA = true;
	public final static int THRESHOLD_AREA = 100;

	public static final int VIEW_MODE_CAMERA  = 0;
	public static final int VIEW_MODE_WEBVIEW = 2;
	public static final int VIEW_MODE_EXTRACT = 3;
	public static final int VIEW_MODE_MERGE = 5;
	public static final int VIEW_MODE_SAVE = 6;
	public static final int VIEW_MODE_SETTINGS = 7;

	public static final int RET_CAMERA_ACTIVITY = 100;
	
	protected volatile int viewMode = VIEW_MODE_WEBVIEW;
	
	protected boolean invertMask;
	protected int smoothingFilterSize;
	protected int dilateErudeTimes;
	protected int cannyParam1;
	protected int cannyParam2;
	protected String url;
	protected String savePath;
	protected int hueUpperThreshold;
	protected int hueLowerThreshold;
	protected int saturationUpperThreshold;
	protected int saturationLowerThreshold;
	protected int brightnessUpperThreshold;
	protected int brightnessLowerThreshold;
	protected boolean imageProcessTarget;
	
	protected int cameraWidth;
	protected int cameraHeight;
	protected int cameraViewWidth;
	protected int cameraViewHeight;
	
	protected Scalar lo, hi;
	protected volatile Mat mRgba;
	protected volatile Mat mIntermediateMat;
	protected volatile Mat mIntermediateMat2;
	protected volatile Mat mIntermediateMat3;
	
	protected CameraView mCameraView;
	protected ImgWebView mWebview;

	protected MenuItem mItemCamera;
	protected MenuItem mItemWebview;
	protected MenuItem mItemExtract;
	protected MenuItem mItemMerge;
	protected MenuItem mItemSaveImage;
	protected MenuItem mItemSettings;

	protected Handler mProcHandler;
	protected HandlerThread mProcThread = new HandlerThread("proc");
	protected volatile Mat mSaveImageHolder;
	
	protected BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
			{
				Log.i(TAG, "OpenCV loaded successfully");
				mCameraView.enableView();
			} break;
			default:
			{
				super.onManagerConnected(status);
			} break;
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mProcThread.start();
		mProcHandler = new Handler(mProcThread.getLooper());

	}
	
	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "onStart");
	}
	@Override
	public void onPause(){
		Log.d(TAG, "onpause");
		if (mCameraView != null)
			mCameraView.disableView();
		
		super.onPause();
	}
	@Override
	public void onStop() {
		Log.d(TAG, "onstop");
		super.onStop();
	}
	@Override
	public void onDestroy() {
		Log.d(TAG, "ondestroy");
		super.onDestroy();
	}

	protected abstract int getCameraViewId();
	public abstract void moveAndRedrawWebView(float x, float y);
	
	public void setPrefs(Context context){
		SharedPreferences prefs;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		invertMask = prefs.getBoolean("InvertMask", false);
		smoothingFilterSize = prefs.getInt("SmoothingFilterSize", 1);
		if (smoothingFilterSize % 2 == 0) {
			smoothingFilterSize += 1;
		}
		hueUpperThreshold = prefs.getInt("HueUpperThreshold", 100);
		hueLowerThreshold = prefs.getInt("HueLowerThreshold", 0);
		saturationUpperThreshold = prefs.getInt("SaturationUpperThreshold", 100);
		saturationLowerThreshold = prefs.getInt("SaturationLowerThreshold", 0);
		brightnessUpperThreshold = prefs.getInt("BrightnesUpperThreshold", 100);
		brightnessLowerThreshold = prefs.getInt("BrightnesLowerThreshold", 0);
		if (!prefs.getString("ImageProcessTarget", "Browser").equals("Browser") &&
			!prefs.getString("ImageProcessTarget", "Browser").equals("Camera")){
			Editor editor = prefs.edit();
			editor.putString("ImageProcessTarget", "Browser");
			editor.commit();
		}
		Log.d(TAG, "target:"+prefs.getString("ImageProcessTarget", "Browser"));
		imageProcessTarget = prefs.getString("ImageProcessTarget", "Browser").equals("Browser") ? 
				TARGET_BROWSER : TARGET_CAMERA;
		cannyParam1 = CONNY_PARAM1;
		cannyParam2 = CONNY_PARAM2;
		dilateErudeTimes = prefs.getInt("DilateErudeTimes", 3);
		url = prefs.getString("MixedPageUrl", "http://www.google.com/imghp");
		String app = context.getResources().getString(R.string.app_name).replaceAll(" ", "_");
		String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator +
				Environment.DIRECTORY_PICTURES +File.separator+ app+ File.separator; 
		File dirF = new File(dir);
		if (!dirF.exists())
			dirF.mkdirs();
		//savePath = prefs.getString("SaveImagePath", dir);
		savePath = dir;
		Log.d(TAG, "osetPrefs filtersize:" + smoothingFilterSize +
				"dilateerudetime:"+ dilateErudeTimes+
				" cannyparam1:" + cannyParam1 + " cannyparam2:" + cannyParam2);		
	}	
	public int getViewMode(){
		return viewMode;
	}
	public void setViewMode(int mode){
		viewMode = mode;
	}
	
	public int getSmoothingFilterSize(){
		return smoothingFilterSize;
	}
	public int getCanneyParam1(){
		return cannyParam1;
	}
	public int getCanneyParam2(){
		return cannyParam2;
	}
	public int getDilateErudeTimes(){
		return dilateErudeTimes;
	}
	public String getSaveImagePath(){
		return savePath;
	}
	public boolean getInvertMask(){
		return invertMask;
	}
	public int getHueUpperThreshold(){
		return hueUpperThreshold;
	}
	public int getHueLowerThreshold(){
		return hueLowerThreshold;
	}	
	public int getSaturationUpperThreshold(){
		return saturationUpperThreshold;
	}
	public int getSaturationLowerThreshold(){
		return saturationLowerThreshold;
	}	
	public int getBrightnessUpperThreshold(){
		return brightnessUpperThreshold;
	}
	public int getBrightnessLowerThreshold(){
		return brightnessLowerThreshold;
	}	
	public boolean getTarget(){
		return imageProcessTarget;
	}
	
	protected void setThreshold(){
		lo = new Scalar(
				hueLowerThreshold,
				saturationLowerThreshold,
				brightnessLowerThreshold);

		hi = new Scalar(
				hueUpperThreshold,
				saturationUpperThreshold,
				brightnessUpperThreshold);
	}
	
	protected void setRgbaMatrics(CvCameraViewFrame inputFrame){
		Log.d(TAG, "setRgbaMatrics:"+inputFrame.rgba());
		mRgba = inputFrame.rgba();
	}
	
	protected boolean hasCameraInfo(){
		SharedPreferences prefs;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		int width = prefs.getInt("CAMERA_WIDTH", -1);
		int height = prefs.getInt("CAMERA_HEIGHT", -1);
		cameraWidth = width;
		cameraHeight = height;
		cameraViewWidth = prefs.getInt("CAMERA_VIEW_WIDTH", -1);
		cameraViewHeight = prefs.getInt("CAMERA_VIEW_HEIGHT", -1);
		Log.d(TAG, "hasCameraInfo"+
				" width:"+width + " height:"+height + 
				" cameraviewwidth:"+cameraViewWidth +
				" cameraviewheight:"+cameraViewHeight);
		return width>0 && height>0 && cameraViewWidth>0 && cameraViewHeight>0;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "onkeydown keycode:"+keyCode + " KEYCODE_BACK:"+KeyEvent.KEYCODE_BACK);
	    switch(keyCode){
	    case KeyEvent.KEYCODE_BACK:
			if(viewMode == VIEW_MODE_WEBVIEW &&
				(mWebview != null && mWebview.canGoBack())){
				mWebview.goBack();
				return true;
			}
	    	
			Log.d(TAG, "finishActivity");
	    	this.finishActivity(RET_CAMERA_ACTIVITY);
	    	this.finish();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	

	public void saveImage(final Context context, final Handler viewHandler) {
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
				
				viewHandler.post(new Runnable(){
					public void run(){
						if (mRet) 
							Toast.makeText(context, "saved image: " + savePath, Toast.LENGTH_SHORT).show();
						else
							Toast.makeText(context, "fail to save image: " + savePath, Toast.LENGTH_SHORT).show();
					}
				});
			}
		});
	}

}
