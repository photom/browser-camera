package com.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Log {

	private static final boolean DEBUG = false;
	private static final boolean VERBOSE = false;
	

	private final static Class [] klasses = {String.class, String.class};
	private static Method getMethod(String m){
		try {
			Class l = Platform.getLogger();
			if (l!=null)
				return l.getMethod(m, klasses);
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private static void call(String method, String tag, String msg) {
		Method d = getMethod(method);
		try {
			d.invoke(null, tag, msg);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void d(String tag, String msg) {
		if(DEBUG)call("d", tag, msg);
	}
	public static void e(String tag, String msg) {
		call("e", tag, msg);
	}
	public static void w(String tag, String msg) {
		call("w", tag, msg);
	}
	public static void v(String tag, String msg) {
		if(VERBOSE)call("v", tag, msg);
	}
	public static void i(String tag, String msg) {
		call("i", tag, msg);
	}
}
