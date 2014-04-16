package com.hitsuji.camera;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MergeCameraActivity extends Activity  {
	private static final String TAG = MergeCameraActivity.class.getSimpleName();
	private static final String URL = "http://141.213.21.94/mjpg/video.mjpg";

	
    private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
	private WebCameraService mBoundService;
	private AtomicBoolean mIsBound = new AtomicBoolean(false);

	private ImageView imgView;

	private LoopHandler mLoopHandler;
	private HandlerThread mLoopHandlerThread;
	private UpdateHandler mUpdateHandler;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "onCreate");

		mLoopHandlerThread = new HandlerThread("loop");
		mLoopHandlerThread.start();
		mLoopHandler = new LoopHandler(mLoopHandlerThread.getLooper());
        mUpdateHandler = new UpdateHandler(this.getMainLooper());
        
        imgView = new ImageView(this);
        setContentView(imgView, new LinearLayout.LayoutParams(WC, WC));
        mLoopHandler.sendEmptyMessage(0);
        
        Intent intent = new Intent(this, WebCameraService.class);
    	startService(intent);
	}

	@Override
	public void onStart(){
		super.onStart();
		Log.v(TAG, "onStart");
		doBindService();
	}
	@Override
	public void onResume(){
		super.onResume();
		Log.v(TAG, "onResume");

	}
	@Override
	protected void onPause() {
	    super.onPause();
	    Log.v(TAG, "onPause");

	}
	@Override
	protected void onStop() {
	    super.onStop();
	    Log.v(TAG, "onStop");
	    doUnbindService();
	}
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    Log.v(TAG, "onResume");
	}

    void doBindService() {
        bindService(new Intent(this, WebCameraService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound.set(true);
    }

    void doUnbindService() {
        if (mIsBound.get()) {
            unbindService(mConnection);
            mIsBound.set(false);
        }
    }

	private ServiceConnection mConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "ServiceConnection.onServiceConnected");
	        mBoundService = ((WebCameraService.LocalBinder)service).getService();
	    }

	    public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "ServiceConnection.onServiceDisconnected");
	        mBoundService = null;
        }
	};
    private class LoopHandler extends Handler {
        private AtomicBoolean mLoop = new AtomicBoolean(true);

        public LoopHandler(Looper l){
            super(l);
        }
        @Override
        public void handleMessage(Message msg) {
        	int idx=0;
            while (mLoop.get()){
            	if (mBoundService!=null){
            		Bitmap tmp = mBoundService.getLatest();
            		if (tmp!=null){
            			Message m = mUpdateHandler.obtainMessage(0, tmp);
            			mUpdateHandler.sendMessage(m);
            		}
            	}
                try {
                    Thread.sleep(1000);
                } catch(Exception e){}
                Log.d(TAG, "update"+idx++);
            }

        }
    }


    private class UpdateHandler extends Handler {
        private AtomicBoolean mLoop = new AtomicBoolean(true);

        public UpdateHandler(Looper l){
            super(l);
        }
        @Override
        public void handleMessage(Message msg) {
        	if (msg.what==0) {
        		Bitmap bmp = (Bitmap)msg.obj;
        		imgView.setImageBitmap(bmp);
        	}
        }
    }
}
