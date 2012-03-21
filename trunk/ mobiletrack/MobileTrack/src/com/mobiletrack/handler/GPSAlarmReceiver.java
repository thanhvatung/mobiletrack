/**
 * 
 */
package com.mobiletrack.handler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mobiletrack.R;
import com.mobiletrack.service.LocationUploadService;

public class GPSAlarmReceiver extends BroadcastReceiver {

	public static final String ACTION_MOBILETRACK_GPS_ALARM = "com.mobiletrack.handler.ACTION_MOBILETRACK_GPS_ALARM";

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ACTION_MOBILETRACK_GPS_ALARM)) {
			// GPS alarm is fired when gps tracking is on so we star the
			// tracking service
			startGPSTrack(context, intent);
		}
	}

	private void startGPSTrack(Context context, Intent intent) {
		long duration = intent.getLongExtra(SmsHandler.EXTRA_KEY_DURATION, 0);
		long interval = intent.getLongExtra(SmsHandler.EXTRA_KEY_INTERVAL, 0);

		Log.i(context.getString(R.string.logging_tag),
				"start tracking service GPSAlarmReceiver() " + "interval ["
						+ interval + "] " + "duration [" + duration + "]");
		LocationUploadService.startTrackingService(context, duration, interval);
	}

}
