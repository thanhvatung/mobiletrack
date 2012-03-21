package com.mobiletrack.ui;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.mobiletrack.R;

public class SettingsActivity extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
	}
}