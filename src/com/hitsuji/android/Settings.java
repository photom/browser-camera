package com.hitsuji.android;

import com.hitsuji.camera.R;
import com.hitsuji.camera.R.xml;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener{
	private static final String TAG = Settings.class.getSimpleName();
	
	
	@Override  
	public void onCreate(Bundle savedInstanceState){  
		super.onCreate(savedInstanceState);	  
		addPreferencesFromResource(R.xml.pref);
		PreferenceScreen ps = this.getPreferenceScreen();
		setTitle(getPackageManager().getApplicationLabel(getApplicationInfo()));
		SharedPreferences prefs = 
			    PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		ListPreference listPref = (ListPreference) findPreference("ImageProcessTarget");
		listPref.setSummary(listPref.getEntry());
	}

	@Override
	public void onResume(){
		super.onResume();
	}
	
	@Override
	public void onDestroy(){
		SharedPreferences prefs = 
			    PreferenceManager.getDefaultSharedPreferences(this);
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
	    Preference pref = findPreference(key);

	    if (pref instanceof ListPreference) {
	        ListPreference listPref = (ListPreference) pref;
	        pref.setSummary(listPref.getEntry());
	    }
	}


}
