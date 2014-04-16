package com.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;


public class Util {
	private static final String TAG = Util.class.getSimpleName();
	
	private static final int MAX_LENGTH = 1024*20;
	private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:s");//2011-12-11T23:38:12
	private static String UA = null;
	
	public static final boolean VALID_AD = false;
	public static final boolean VALID_RADIO = false;
	public static final boolean VALID_PAYPAL = false;
	
	public static final String getUserAgent(){
		if (UA==null) initUserAgent(null); 
		return UA;
	}
	public static void initUserAgent(Context c){
		if (UA != null) return;
		
		String v = getVersion(c);
		UA = "hitsuji/" + (v == null ? "0.0" : v);
		Log.d(TAG, "useragent:"+UA);
	}
	
	public static String readSmallFile(String file){
		if (file==null) {
			Log.e(TAG, "invalid param:"+file);
			return null;
		}
		File f = new File(file);
		if (!f.exists() || f.length()<=0){
			Log.e(TAG, "file does not exist:"+file);
			return null;
		} else if (f.length() > MAX_LENGTH) {
			//Log.e(TAG, "invalid file:"+file);
		}
		
		byte[] buff = new byte[(int)f.length()];
		int len, ret = 0;
		FileInputStream fis = null;
		String result = null;
		try {
			 fis = new FileInputStream(f);
			 len = fis.read(buff, 0, (int)f.length());
			 result = new String(buff);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			ret = -1;
		}
		if (fis!=null) {
			try {
				fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (ret < 0) {
			return null;
		}
		return result;
	}
	
	public static JSONObject parseJsonobj(String content) {
		if (content==null) {
			Log.e(TAG, "invalid param:"+content);
		}
		JSONObject jObject = null;
		try {
			jObject = new JSONObject(content);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			if (content==null) {
				Log.e(TAG, "fail to parse json object:"+content);
			}
			e.printStackTrace();
			return null;
		}
		return jObject;
	}
	
	public static long convertLocaltime(String utc) {
        TimeZone tz = TimeZone.getDefault();
        int offset = tz.getRawOffset();
        long time = System.currentTimeMillis();
        try {
			Date date = FORMAT.parse(utc);
			Log.d(TAG, "fetch time:"+utc + " offset:"+offset);
			time = date.getTime();
			time += offset;
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return time;
	}
	public static long getUtc() {
        //TimeZone tz = TimeZone.getDefault();
        //int offset = tz.getRawOffset();
        long time = System.currentTimeMillis();
        return time;
	}
	public static boolean empty(String str) {
		return str==null || str.length()==0;
	}


	public static void write(String name, String content) {
		// TODO Auto-generated method stub
		File file = new File(name);
		if (file.exists()) file.delete();
		
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
			osw.write(content);
			osw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Sync.a();
	}


	public static int write(String name, Object obj) {
		// TODO Auto-generated method stub
		File file = new File(name);
		if (file.exists()) file.delete();

		ObjectOutputStream oos;
		FileOutputStream fos;
		try {
			file.createNewFile();
			fos = new FileOutputStream(file);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(obj);
			oos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		Sync.a();
		return 0;
	}
	
	public static Object read(String name) {
		File file = new File(name);
		if (!file.exists()) return null;
	    FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object obj = ois.readObject();
			return obj;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    return null;
	}
	
	public static String getName(Context c) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
		return sp.getString("name", null);
	}
	
	public static String getVersion(Context c) {
		try
		{
		    String app_ver = c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
		    return app_ver;
		}
		catch (NameNotFoundException e)
		{
			e.printStackTrace();
		    return null;
		}
	}
	public static String currentTime(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
		Date date = new Date();
		return sdf.format(date);
	}
}
