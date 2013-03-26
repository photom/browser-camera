package com.hitsuji.camera;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;

public class BaseActivity extends ActivityGroup {
	private LocalActivityManager mLam;

	private BaseActivity self;
	public BaseActivity(){
		super();
		self = this;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.group);
		mLam = getLocalActivityManager();

		setView(TravelingCameraActivity.class, "browser");
		setView(TravelingCameraActivity.class, "camera");
	}
	
  private void setView(Class<?> cls, String name){
	  Intent intent = new Intent(getApplicationContext(), cls);
	  Window window = mLam.startActivity(name, intent);
	  ViewGroup group = (ViewGroup)findViewById(R.id.grouplayout);
	  group.addView(window.getDecorView());
    }

}
