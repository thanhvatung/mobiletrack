package com.mobiletrack.handler;


import com.mobiletrack.util.ServiceStarter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TimeScheduleAlarmReceiver extends BroadcastReceiver {
	 
	public static final String NAME = ".handler.Time_Schedule_ALARM";
 
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.i("SetGPSAlarm","Reset GPS at midnight -- 1");
		if (intent.getAction().equals(NAME)) {
			setTimeSchedule(context, intent);
		}
	//	setTimeSchedule(context, intent);
	}
	private void setTimeSchedule(Context context, Intent intent) {
		//ServiceStarter.go(context);
		SmsHandler.setGPSAlarm(context);
		Log.i("SetGPSAlarm","Reset GPS at midnight -- 2");
	}
}