package com.mobiletrack.util;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import com.mobiletrack.config.Config;
import com.mobiletrack.handler.CellHandler;
import com.mobiletrack.handler.SmsHandler;
import com.mobiletrack.monitor.DataMonitor;
import com.mobiletrack.monitor.SmsMonitor;
import com.mobiletrack.process.SMSBlockProcess;
import com.mobiletrack.service.FTPTransferManager;
import com.mobiletrack.ui.CustomerCodeActivity;

public class ServiceStarter {

	public static boolean RUNNING = false;
	public static void go (Context context)
	{
		Log.i("StartService", String.valueOf(RUNNING));
		if (Looper.myQueue() == null)
			Looper.prepare();
		CustomerCodeActivity.GET_NUMBER(context);
		Config.takeSnapShot(context);
//		SmsHandler.setGPSAlarm(context);
		SmsHandler.setDeviceRegAlarm(context);
		Config c = Config.getConfig();
		if (RUNNING)
			stop(context);
		RUNNING = true;
		SmsMonitor.startMonitoring(context);
		//		SpeedDetectionService.startDetection(context);
		FTPTransferManager.startService();
//		SMSBlockProcess.startMonitoring(context);
		if (c.networkNotify())
			CellHandler.startMonitoring(context);
		if (c.isDataEnabled())
			DataMonitor.startMonitoring(context);

	}

	public static void stop(Context context)
	{
		if (!RUNNING)
			return;
		RUNNING = false;
		DataMonitor.stopMonitoring(context);
		SmsMonitor.stopMonitoring(context);
		CellHandler.stopMonitoring(context);
		try
		{
			FTPTransferManager.stopService();
		}
		catch (Exception e)
		{

		}
		/*	try
	{
		SpeedDetectionService.stopDetection(context);
	}
	catch (Exception e)
	{
	}*/
		SMSBlockProcess.stopMonitoring();
		System.gc();

	}
	public static void reset (boolean resetTo)
	{
		RUNNING = resetTo;
	}
}
