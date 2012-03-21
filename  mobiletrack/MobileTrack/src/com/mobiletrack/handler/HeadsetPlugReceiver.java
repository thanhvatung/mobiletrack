package com.mobiletrack.handler;

import com.mobiletrack.config.Config;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class HeadsetPlugReceiver extends BroadcastReceiver {

	private static HeadsetPlugReceiver instance;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) {
			int state = intent.getIntExtra("state", -1);
			Config.getConfig().setHeadSetPlugIn(state == 1);
		}

	}

	public static HeadsetPlugReceiver getInstance() {
		if (instance == null) {
			instance = new HeadsetPlugReceiver();
		}
		return instance;
	}

}
