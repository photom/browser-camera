package com.util;

public class Sync {
	static {
		System.loadLibrary("sync");
	}
	//sync
	public static native void a();
	//get api secret
	public static native String b();
	//get secret key for encrypt session key
	public static native String c();
}
