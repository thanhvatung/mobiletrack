package com.mobiletrack.handler;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;

public class LocationScheduleThread implements Runnable{
	private ScheduledFuture<?> task;
	private Context context;
	private FutureTask<String> track;
	
	public LocationScheduleThread(ScheduledFuture<?> task , FutureTask<String> track,Context context) {
		// TODO Auto-generated constructor stub
		this.task = task;
		this.context = context;
		this.track = track;
	}

	public void run() {
		// TODO Auto-generated method stub
		if(task.isDone()){
			try {
				if(!track.get().equals("YES")){
					SmsHandler.setGPSAlarm(context);
					Log.i("SetGPSAlarm","Reset -- 1");
				}
				else{
					Log.i("SetGPSAlarm","Done -- 1");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else{
			try {
				Thread.sleep(task.getDelay(TimeUnit.MILLISECONDS));
				try {
					if(!track.get().equals("YES")){
						SmsHandler.setGPSAlarm(context);
						Log.i("SetGPSAlarm","Reset -- 2");
					}
					else{
						Log.i("SetGPSAlarm","Done -- 2");
					}
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		}
	}
}
