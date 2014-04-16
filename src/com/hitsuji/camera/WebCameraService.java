package com.hitsuji.camera;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebCameraService extends Service {
    private static final String TAG = WebCameraService.class.getSimpleName();
	private static final String URL = "http://141.213.21.94/mjpg/video.mjpg";
	private NotificationManager mNM;
	private final IBinder mBinder = new LocalBinder();
	private int NOTIFICATION = 1;//R.string.local_service_started;
	private MjpegHandler mHandler;
    private HandlerThread mHandlerThread;


	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	@Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "oncreate");
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mHandlerThread = new HandlerThread("mjpeg");
        mHandlerThread.start();;
        mHandler = new MjpegHandler(mHandlerThread.getLooper());
        mHandler.sendEmptyMessage(0);
    }
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "Received start id " + startId + ": " + intent);


        return START_STICKY;
    }
	@Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ondestroy");
        mHandlerThread.quit();
        mNM.cancel(NOTIFICATION);
    }

    public Bitmap getLatest(){
        return mHandler.getLatest();
    }

    public class LocalBinder extends Binder {
		WebCameraService getService() {
            return WebCameraService.this;
        }
    }



    class MjpegHandler extends Handler {
        private AtomicBoolean mLoop = new AtomicBoolean(true);
        private AtomicBoolean mLock = new AtomicBoolean(false);
        private Bitmap  tmpBitmap = null;
        private volatile Bitmap latestBitmap = null;

        public MjpegHandler(Looper l){
            super(l);
        }

        private Bitmap getLatest(){
            synchronized (mLock) {
                return latestBitmap;
            }
        }

        @Override
        public void handleMessage(Message msg) {
            while (mLoop.get()){
                HttpResponse res = null;
                InputStream is = null;
                DefaultHttpClient httpclient = new DefaultHttpClient();
                try {
                	Log.d(TAG, "service loop");
                    res = httpclient.execute(new HttpGet(URL));
                    is = res.getEntity().getContent();
                    MjpegInputStream mis =  new MjpegInputStream(is);
                    Bitmap tmp = mis.readMjpegFrame();
                    Log.d(TAG, "bitmap:"+tmp);

                    tmpBitmap = tmp;
                    synchronized (mLock){
                        latestBitmap = tmpBitmap;
                    }
                } catch(IOException e){
                    e.printStackTrace();;
                    Log.d(TAG, e.getMessage());
                    try {
                        Thread.sleep(1000);;
                    }catch(Exception err){}
                    continue;
                }



            }
        }
    }

}
