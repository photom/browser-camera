package com.hitsuji.camera;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.Window;
import android.view.WindowManager;

import com.util.Log;


public class LoadCameraInfoActivity extends BaseActivity implements CvCameraViewListener2{
	private static final String TAG = "LoadCameraInfoActivity";

	private int mCameraWidth;
	private int mCameraHeight;
	private Handler mHandler;
	private boolean doOnce = false;
	

	protected int getCameraViewId(){
		return R.id.camera_camera_view;
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.camera);
		mHandler = new Handler();
		mCameraView = (CameraView) findViewById(R.id.camera_camera_view);
		mCameraView.setCvCameraViewListener(this);
		mCameraView.enableFpsMeter();
	}
	@Override
	public void onStart() {
		super.onStart();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, this, mLoaderCallback);

	}

	@Override
	public void onPause() {
		if (mCameraView != null)
			mCameraView.disableView();
	
		super.onPause();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	@Override
	public void onCameraViewStarted(final int width, final int height) {
		SharedPreferences prefs;
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		editor.putInt("CAMERA_WIDTH", width);
		editor.putInt("CAMERA_HEIGHT", height);
		editor.putInt("CAMERA_VIEW_WIDTH", this.mCameraView.getWidth());
		editor.putInt("CAMERA_VIEW_HEIGHT", this.mCameraView.getHeight());
		editor.commit();
		mCameraWidth = width; 
		mCameraHeight = height;
		Log.d(TAG, "onCameraViewStarted width:"+width + " height:"+height + 
				" viewwidth:"+this.mCameraView.getWidth() + " viewheight:"+this.mCameraView.getHeight());
		doOnce = false;
		
	}
	@Override
	public void onCameraViewStopped() {
		
	}
	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat img = inputFrame.gray();
		Point point = new Point(mCameraWidth/2, mCameraHeight/2);
		Core.putText(img, "preparing...", point, 
				Core.FONT_HERSHEY_PLAIN, 1.0, 
				new Scalar(1, 0, 0));
		if (!doOnce){
			SharedPreferences prefs;
			prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = prefs.edit();
			editor.putFloat("CAMERA_SCALE", this.mCameraView.getScale());
			editor.commit();
			
			mHandler.postAtTime(new Runnable(){

				@Override
				public void run() {
					Log.d(TAG, "back to prev actiivty");
					LoadCameraInfoActivity.this.finish();				
				}
			
			}, 1000);
			doOnce = true;
		}
		return img;
	}
	@Override
	public void moveAndRedrawWebView(float x, float y) {
		//do nothing
	}
	
	
}
