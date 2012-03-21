package com.mobiletrack.service;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.mobiletrack.record.VoiceSMSRecord;
import com.mobiletrack.util.ServiceStarter;

public class LocationUploadService extends BroadcastReceiver {
	private final static String EXTRA_KEY_DURATION = "TRACKING_DURATION";
	private final static String EXTRA_KEY_INTERVAL = "TRACKING_INTERVAL"; 
	private final static String LOCATION_UPDATE_CMD = "LOCATION_UPDATE_CMD";
	private final static String EXTRA_KEY_VOICE_SMS_RECORD = "EXTRA_KEY_VOICE_SMS_RECORD";
	private final static int LOCATION_UPDATE_CMD_LOCATE = 1;
	private final static int LOCATION_UPDATE_CMD_TRACK = 2;
	private final static int LOCATION_UPDATE_CMD_VOICE_SMS_RECORD = 3;
	private final static int LOCATION_UPDATE_CMD_STOPSERVICE = 4;
	private static long endAt;
	/**
	 * start the location update service to upload location information
	 * for the length of <code>duration</code> in minutes.
	 * @param duration
	 */
	public static void startTrackingService(Context context, long duration, long interval) {
		Log.i("BlockSMSService","Tracking Service interval value:"+String.valueOf(interval));
		Intent intent = new Intent(context, LocationTrackService.class);
		endAt = duration +System.currentTimeMillis();
		updateTracker(context, duration, interval);
//		reinitialize (context, duration, interval);
		intent.putExtra(EXTRA_KEY_DURATION, duration);
		intent.putExtra(EXTRA_KEY_INTERVAL, interval);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_TRACK);
		context.startService(intent);
	}

	/**
	 * start the location update service to upload the current location
	 * information only once
	 */
	public static void locateOnceService(Context context) {
		Intent intent = new Intent(context, LocationTrackService.class);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_LOCATE);
		Log.i("LocationTrackService -- Debug info", "command");
		context.startService(intent);
	}

	/**
	 * stop the tracking service 
	 */
	public static void stopTrackingService(Context context) {
		Intent intent = new Intent(context, LocationTrackService.class);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_STOPSERVICE);
		context.startService(intent);
		PendingIntent sender = PendingIntent.getBroadcast(context, 2486, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		
		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.cancel(sender);
	}

	public static void sendVoiceSMSRecordWithLocation(Context context, VoiceSMSRecord voiceSMSRecord) {
		if (!ServiceStarter.RUNNING)
			ServiceStarter.go(context.getApplicationContext());
		Intent intent = new Intent(context, LocationTrackService.class);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_VOICE_SMS_RECORD);
		intent.putExtra(EXTRA_KEY_VOICE_SMS_RECORD, voiceSMSRecord);
		Log.i("LocationTrackService -- Debug info", "SMS");
		context.startService(intent);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Bundle bundle = intent.getExtras();
			long duration = bundle.getLong("duration");
			long interval = bundle.getLong("interval");
			if (bundle.containsKey("endat"))
				endAt = bundle.getLong("endat", endAt);
			locateOnceService(context);
			if (endAt < System.currentTimeMillis())
			{
				stopTrackingService(context);
			}
			else if (!bundle.containsKey("endat"))
			{
				Intent newIntent = new Intent(context, LocationTrackService.class);
				newIntent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_TRACK);
				context.startService(newIntent);
				updateTracker(context, duration, interval);
			}

		} catch (Exception e) {
			Toast.makeText(context, "There was an error somewhere, but we still received an alarm", Toast.LENGTH_SHORT).show();
			e.printStackTrace();

		}
	}


	public static void updateTracker(Context context, long minutesToTrack, long interval)
	{
		int i = (int) interval;
		
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		// add 5 minutes to the calendar object
		cal.add(Calendar.MILLISECOND, i);
		Intent intent = new Intent(context, LocationUploadService.class);
		intent.putExtra("duration", minutesToTrack - i*1000*60);
		intent.putExtra("interval", interval);
		// We need 
		PendingIntent sender = PendingIntent.getBroadcast(context, 24863971, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
	}

	public static void reinitialize (Context context, long minutesToTrack, long interval)
	{
		int i = (int) interval;
		
		// get a Calendar object with current time
		Calendar cal = Calendar.getInstance();
		// add 5 minutes to the calendar object
		cal.add(Calendar.MILLISECOND, i);
		Intent intent = new Intent(context, LocationUploadService.class);
		intent.putExtra("duration", minutesToTrack - i*1000*60);
		intent.putExtra("interval", interval);
		intent.putExtra("endat", endAt);
		// We need 
		PendingIntent sender = PendingIntent.getBroadcast(context, 2486, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		// Get the AlarmManager service
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		am.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), AlarmManager.INTERVAL_FIFTEEN_MINUTES, sender);
	}
}
