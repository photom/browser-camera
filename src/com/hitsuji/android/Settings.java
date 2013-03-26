package com.hitsuji.android;

import com.hitsuji.camera.R;
import com.hitsuji.camera.R.xml;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity {
	private static final String TAG = Settings.class.getSimpleName();
	
	
	@Override  
	public void onCreate(Bundle savedInstanceState){  
		super.onCreate(savedInstanceState);  
		addPreferencesFromResource(R.xml.pref);
		PreferenceScreen ps = this.getPreferenceScreen();
		setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));
	}

	@Override
	public void onResume(){
		super.onResume();
	}
	
}
