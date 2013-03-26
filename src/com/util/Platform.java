package com.util;

public class Platform {
	
	public static Class<?> getLogger(){
		return getAndroidLogger();
	}
	private static Class<?> getLinuxLogger(){
		return null;
	}
	private static Class<?> getAndroidLogger(){
		try {
			Class<?> klass = Class.forName("android.util.Log");
			return klass;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	public static Class<?> getEnvironment(){
		return getAndroidEnvironment();
	}
	
	private static Class<?> getLinuxEnvironment(){
		return null;
	}
	private static Class<?> getAndroidEnvironment(){
		try {
			Class<?> klass = Class.forName("android.os.Environment");
			return klass;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
}
