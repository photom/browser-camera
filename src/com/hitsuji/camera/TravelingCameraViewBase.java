package com.hitsuji.camera;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.highgui.Highgui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;

import com.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public abstract class TravelingCameraViewBase extends SurfaceView implements SurfaceHolder.Callback, Runnable {
	private static final String TAG = "TCamera::SurfaceView";

	private SurfaceHolder       mHolder;
	private VideoCapture        mCamera;
	private int mFrameWidth = -1;
	private int mFrameHeight = -1;
	private static TravelingCameraViewBase self = null;
	private int mCameraWidth = -1;
	private int mCameraHeight = -1;
	protected AtomicBoolean mStopCameraFlag = new AtomicBoolean(false);
	
	public TravelingCameraViewBase(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
		getHolder().setFormat(PixelFormat.TRANSLUCENT);
		self = this;
		Log.v(TAG, "Instantiated new " + this.getClass());
	}
	
	public void setCameraFlag(boolean b){
		Log.v(TAG, "setcameraflag:"+ b);
		mStopCameraFlag.set(b);
	}
	
	public static int getFrameWidth(){
		return self == null ? -1 : self.mFrameWidth;
	}
	public static int getFrameHeight(){
		return self == null ? -1 : self.mFrameHeight;
	}
	
	public void surfaceChanged(SurfaceHolder _holder, int format, int width, int height) {
		Log.v(TAG, "surfaceCreated");
		synchronized (this) {
			if (mCamera != null && mCamera.isOpened()) {
				Log.v(TAG, "before mCamera.getSupportedPreviewSizes()");
				List<Size> sizes = mCamera.getSupportedPreviewSizes();
				Log.v(TAG, "after mCamera.getSupportedPreviewSizes()");
				mFrameWidth = width;
				mFrameHeight = height;

				// selecting optimal camera preview size
				{
					double minDiff = Double.MAX_VALUE;
					for (Size size : sizes) {
						if (Math.abs(size.height - height) < minDiff) {
							mFrameWidth = (int) size.width;
							mFrameHeight = (int) size.height;
							minDiff = Math.abs(size.height - height);
						}
					}
				}

				mCamera.set(Highgui.CV_CAP_PROP_FRAME_WIDTH, mFrameWidth);
				mCamera.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT, mFrameHeight);
			}
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG, "surfaceCreated");
		mCamera = new VideoCapture(Highgui.CV_CAP_ANDROID);
		if (mCamera.isOpened()) {
			(new Thread(this)).start();
		} else {
			mCamera.release();
			mCamera = null;
			Log.e(TAG, "Failed to open native camera");
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG, "surfaceDestroyed");
		if (mCamera != null) {
			synchronized (this) {
				mCamera.release();
				mCamera = null;
			}
		}
	}

	protected abstract Bitmap processFrame(VideoCapture capture);
	public void run() {
		Log.v(TAG, "Starting processing thread");
		while (true) {
			Bitmap bmp = null;
			//Log.d(TAG, "stopcameraflag:"+mStopCameraFlag.get());
			if(mStopCameraFlag.get()) {
				continue;
			}
			synchronized (this) {
				if (mCamera == null){
					Log.e(TAG, "mCamera null");
					break;
				}
				if (!mCamera.grab()) {
					Log.e(TAG, "mCamera.grab() failed");
					break;
				}
				
				if (mCameraWidth<0){
					Mat tmp = new Mat();
					mCamera.retrieve(tmp, Highgui.CV_CAP_ANDROID_COLOR_FRAME_RGBA);
					int w,h;
					h = tmp.height();
					w = tmp.width();
					int ret = TravelingBrowserActivity.setDefaultWebviewSize(w,h);
					if (ret ==0 ){
						mCameraWidth = w;
						mCameraHeight = h;
					}
					
				}
				
				bmp = processFrame(mCamera);
			}

			if (bmp != null) {
				Canvas canvas = mHolder.lockCanvas();
				if (canvas != null) {
					canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);
					canvas.drawBitmap(bmp, (canvas.getWidth() - bmp.getWidth()) / 2, (canvas.getHeight() - bmp.getHeight()) / 2, null);
					mHolder.unlockCanvasAndPost(canvas);
				}
				bmp.recycle();
			}
		}

		Log.v(TAG, "Finishing processing thread");
	}
}