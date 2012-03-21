package com.mobiletrack.ui;

import java.io.InputStream;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TabHost;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.util.ServiceStarter;

public class MTActivity extends TabActivity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initConfig();

		setContentView(R.layout.main);

		// Resources res = getResources(); // Resource object to get Drawables
		TabHost tabHost = getTabHost(); // The activity TabHost
		TabHost.TabSpec spec; // Resusable TabSpec for each tab
		Intent intent; // Reusable Intent for each tab

		/**
		 * comment out the location activity // Create an Intent to launch an
		 * Activity for the tab (to be reused) intent = new
		 * Intent().setClass(this, LocationActivity.class);
		 * 
		 * // Initialize a TabSpec for each tab and add it to the TabHost spec =
		 * tabHost.newTabSpec("location").setIndicator("Location")//,
		 * res.getDrawable(R.drawable.ic_tab_location) .setContent(intent);
		 * tabHost.addTab(spec);
		 **/

		// Do the same for the other tabs
		intent = new Intent().setClass(this, DialerActivity.class);
		spec = tabHost.newTabSpec("dialer").setIndicator("Dialer"/*
																 * ,
																 * res.getDrawable
																 * (R.drawable.
																 * ic_tab_dialer
																 * )
																 */)
				.setContent(intent);
		tabHost.addTab(spec);

		intent = new Intent().setClass(this, SettingsActivity.class);
		spec = tabHost.newTabSpec("about").setIndicator("About"/*
																 * ,
																 * res.getDrawable
																 * (R.drawable.
																 * ic_tab_settingss
																 * )
																 */)
				.setContent(intent);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);
		ServiceStarter.go(this);
	}

	// For testing purpose: initialize the account and device settings
	// with the local xml files
	private void initConfig() {
		Config.takeSnapShot(this);
		String phoneNumber = Config.getConfig().getPhoneNumber();
		// only load the default configuration if the phone number equals
		// non_init_phonenumber
		if (phoneNumber != null
				&& phoneNumber.equals(getString(R.string.non_init_phonenumber))) {
			try {
				InputStream isAccountSetting = getAssets().open(
						"account_setting.xml");
				Config.getConfig().update(isAccountSetting);
				isAccountSetting.close();
				InputStream isDeviceSetting = getAssets().open(
						"device_setting.xml");
				Config.getConfig().update(isDeviceSetting);
				isDeviceSetting.close();
			} catch (Exception e) {
				Log.e(getString(R.string.logging_tag),
						"Got exp while trying to init the config with assets exp ["
								+ e.getClass().getName() + "] msg ["
								+ e.getMessage() + "]");
			}
		}
	}
}