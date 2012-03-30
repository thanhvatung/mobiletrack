package com.mobiletrack.handler;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.config.Config.GPSItemInfo;
import com.mobiletrack.process.SMSBlockProcess;
import com.mobiletrack.record.DeviceStatusRecord;
import com.mobiletrack.record.SMSRecord;
import com.mobiletrack.record.VoiceSMSRecord;
import com.mobiletrack.service.LocationUploadService;
import com.mobiletrack.service.WipeService;
import com.mobiletrack.ui.CustomerCodeActivity;
import com.mobiletrack.util.ServiceStarter;

public class SmsHandler extends BroadcastReceiver {
	public final static String EXTRA_KEY_DURATION = "EXTRA GPS TRACKING DURATION";
	public final static String EXTRA_KEY_INTERVAL = "EXTRA GPS TRACKING INTERVAL";	
	private static String SMS_CMD_PREFIX = null;
	private static String SMS_CMD_POSTFIX = null;
	private static String SMS_CMD_LOCATE = null;
	private static String SMS_CMD_TRACK = null;
	private static String SMS_CMD_UPDATECFG = null;
	private static String SMS_CMD_UPDATEACCT = null;
	private static String SMS_CMD_STAT = null;
	private static String SMS_CMD_TERM = null;
	private static String SMS_CMD_WIPE = null;

	private static PendingIntent alarmIntentGPS[] = new PendingIntent[20];
	private static AlarmManager alarms;
	private static PendingIntent alarmIntentDeviceReg;
	public static DeviceStatusRecord r;
	
	private static Context tempContext;

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
				return;
		}
		// do not respond if the app has been terminated
		Config.takeSnapShot(context);
		if (Config.getConfig().isAppTerminated())
			return;

		// ---get the SMS message passed in---
		Bundle bundle = intent.getExtras();
		SmsMessage[] msgs = null;
		String str = "";
		if (bundle != null) {
			// ---retrieve the SMS message received---
			Object[] pdus = (Object[]) bundle.get("pdus");
			msgs = new SmsMessage[pdus.length];
			for (int i = 0; i < msgs.length; i++) {
				msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				str += "SMS from " + msgs[i].getOriginatingAddress();
				str += " :";
				str += msgs[i].getMessageBody().toString();
				str += "\n";
				try {
					parseAndExecuteSms(msgs[i], context);
				} catch (Exception e) {
					Log.e(context.getString(R.string.logging_tag),
							"Got exp while parsing the sms exp [" + e.getClass().getName() + "] msg [" + e.getMessage() + "]");
				}
				Log.i("SMSHandler","SMSHandler 0.0: "+ i);
			}

			//---display the new SMS message---
			//Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
		}
	}

	private void parseAndExecuteSms(SmsMessage sms, Context context)
	{
		Config.getConfig().getServerBase();
		
		if (SMS_CMD_PREFIX == null)
			SMS_CMD_PREFIX = context.getString(R.string.sms_cmd_prefix);
		if (SMS_CMD_POSTFIX == null)
			SMS_CMD_POSTFIX = context.getString(R.string.sms_cmd_postfix);
		if (SMS_CMD_LOCATE == null)
			SMS_CMD_LOCATE = SMS_CMD_PREFIX
			+ context.getString(R.string.sms_cmd_locate)
			+ SMS_CMD_POSTFIX;
		if (SMS_CMD_TRACK == null)
			SMS_CMD_TRACK = SMS_CMD_PREFIX + context.getString(R.string.sms_cmd_track) + SMS_CMD_POSTFIX;
		if (SMS_CMD_UPDATECFG == null)
			SMS_CMD_UPDATECFG = SMS_CMD_PREFIX + context.getString(R.string.sms_cmd_updatecfg) + SMS_CMD_POSTFIX;
		if (SMS_CMD_UPDATEACCT == null)
			SMS_CMD_UPDATEACCT = SMS_CMD_PREFIX + context.getString(R.string.sms_cmd_updateacct) + SMS_CMD_POSTFIX;
		if (SMS_CMD_STAT == null)
			SMS_CMD_STAT = SMS_CMD_PREFIX + context.getString(R.string.sms_cmd_status) + SMS_CMD_POSTFIX;
		if (SMS_CMD_TERM == null)
			SMS_CMD_TERM = SMS_CMD_PREFIX + context.getString(R.string.sms_cmd_terminate) + SMS_CMD_POSTFIX;
		if (SMS_CMD_WIPE == null)
			SMS_CMD_WIPE = SMS_CMD_PREFIX + context.getString(R.string.sms_cmd_wipe) + SMS_CMD_POSTFIX;

		// Note Important: because the sms transfered by the carrier from
		//                 an email, different carrier will prefix different
		//                 information, we will filter all those information
		String body = sms.getMessageBody();
		body = body.replace(" ", "");
		body = body.replace("\n", "");
		String cmd = getMsgCmd(body);
		//		SmsMonitor.startMonitoring(context);
		//		SMSBlockService.writeValues(context);
		//		SMSBlockService.startMonitoring(context);
		// [1] log all sms messages
		//		Config config = Config.getConfig();
		//		if (config.isTXTGenericAllowed()) {
		SMSRecord smsRecord = new SMSRecord(context,
				VoiceSMSRecord.DIRECTION_MT);
		smsRecord._otherParty =(sms.getOriginatingAddress());
		smsRecord._eventStart =(System.currentTimeMillis());
		smsRecord._eventEnd =(System.currentTimeMillis());
		// config.queueEventRecord(smsRecord.toString());
		// send the record to the server right away
//		LocationUploadService.sendVoiceSMSRecordWithLocation(context,
//				smsRecord);
		Log.i("SMSHandler","SMSHandler 1.0");
		if (cmd != null){
			deleteMessage(context, sms);			
		}
		//		} else {

		//			deleteMessage(context, sms);
		//		}

		// [2] quit if this is a non-cmd sms
		if (cmd == null)
		{
			Log.i("BlockingSMS","BlockingSMS 1.0");
			if (!Config.getConfig().isTXTGenericAllowed() && !SMSBlockProcess.OVERRIDE)
			{
				String message = Config.getConfig().getAutoReplyMessage();
				if (message != null && !message.equals("") && !sms.getMessageBody().startsWith("Autoreply"))
				{
					SmsManager smsm = SmsManager.getDefault();
					smsm.sendTextMessage(sms.getOriginatingAddress(), null, message, null, null);
					Log.i("BlockingSMS","Phone number:"+sms.getOriginatingAddress());
				}
			}
			LocationUploadService.sendVoiceSMSRecordWithLocation(context,
					smsRecord);
			Log.i("BlockingSMS","Phone number:"+smsRecord._otherParty);
			if(Config.getConfig().isNumberToRestrict(smsRecord._otherParty))
				deleteMessage(context, sms);
			
			return;
		}
		// [3] handle command sms
		body = contentFilter(body);
		if (cmd.equals(SMS_CMD_LOCATE)) {
			if (!Config.getConfig().isGPSEnabled())
				return;

			/** settle down when the spec is finalized
            assert(body.length() > 2); // "*0" or "*1"
            assert(body.charAt(0) == '*');
            char cmdChar = body.charAt(1);
            assert(cmdChar == '0' || cmdChar == '1');
			 **/
			char cmdChar = '0';
			if ((body.length() > 2) && (body.charAt(0) == '*'))
				cmdChar = body.charAt(1);
			if (cmdChar == '0')
			{	
				Log.i("SMSHandler","SMSHandler 1.1");
				// do not send if it is roaming
				ServiceState servState = new ServiceState();
				if (!servState.getRoaming()){
					Log.i("SMSHandler","SMSHandler 1.1.2");
					LocationUploadService.locateOnceService(context);
				}
			} else if (cmdChar == '1')
			{
				Log.i("SMSHandler","SMSHandler 1.2");
				// send even if the device is roaming
				LocationUploadService.locateOnceService(context);
			}
		} else if (cmd.equals(SMS_CMD_TRACK))
		{
			if (!Config.getConfig().isGPSEnabled())
				return;
			Log.i("SMSHandler","SMSHandler 1.3");
			assert(body.length() >= 4); // "*2|0"
			char cmdChar = body.charAt(body.length() - 1);
			assert(cmdChar == '0' || cmdChar == '1');
			// the number of minutes for the device to track the location
			int duration = 0;
			if (body.indexOf('/') > 0)
				duration = Integer.parseInt(body.substring(1, body.indexOf('/')));
			else if (body.indexOf('|') > 0)
				duration = Integer.parseInt(body.substring(1, body.indexOf('|')));
			if (cmdChar == '0')
			{
				// do not send if roaming
				ServiceState servState = new ServiceState();
				if (!servState.getRoaming())
					LocationUploadService.startTrackingService(context, duration * 60 * 1000, 30 * 1000/*Config.getConfig().getGPSInterval()*/);
			} else if (cmdChar == '1')
			{
				// send even if the device is roaming
				LocationUploadService.startTrackingService(context, duration * 60 * 1000, 30 * 1000/*Config.getConfig().getGPSInterval()*/);
			}
		} else if (cmd.equals(SMS_CMD_UPDATECFG))
		{
			
//			assert(body.length() > 2); //"*http://xxx.xxx.xxx.xxx/..."
//			assert(body.charAt(0) == '*');
			String url = body.substring(1);
			Log.i("SMSHandler","SMSHandler 1.4"+ url);
			Config.takeSnapShot(context);
			
			//Toast.makeText(context,"Server:" + CustomerCodeActivity.SERVER_BASE , Toast.LENGTH_LONG).show();
			Config.getConfig().update(CustomerCodeActivity.SERVER_BASE+ Config.CONFIG_ServiceCall + CustomerCodeActivity.PHONE_NUMBER);
			
			//Config.getConfig().update(url);
			Config.takeSnapShot(context); // get the new configurations
			tempContext = context;
			int timeout = Config.getConfig().getCallLocationTimeout();
			
			//after record sent to server, then set up alarm
			TimerTask gpsAlarmTask = new TimerTask() {	
				
				@Override
				public void run() {
					// TODO Auto-generated method stub
					Log.i("SMSHandler","SMSHandler 1.4.2: job Start: "+ tempContext.getPackageName());
					setGPSAlarm(tempContext);
					setDeviceRegAlarm(tempContext);
				}
			};
			Timer gpsAlarmTimer = new Timer();
			writePhoneState(tempContext);
			gpsAlarmTimer.schedule(gpsAlarmTask, (timeout+5)*1000);			
			Log.i("SMSHandler","SMSHandler 1.4.1: job Scheduled: "+ timeout);
			
			
		} else if (cmd.equals(SMS_CMD_UPDATEACCT))
		{
			//assert(body.length() > 2); //"*http://xxx.xxx.xxx.xxx/..."
			//assert(body.charAt(0) == '*');
			//String url = body.substring(1);
			Config.takeSnapShot(context);
			Config.getConfig().update(CustomerCodeActivity.SERVER_BASE + Config.CODES_ServiceCall + CustomerCodeActivity.PHONE_NUMBER);
			//Config.getConfig().update(url);
		} else if (cmd.equals(SMS_CMD_STAT))
		{
			if (r != null)
				r.end();
			r = new DeviceStatusRecord(context.getApplicationContext());
		} else if (cmd.equals(SMS_CMD_TERM)) {
			// terminate the service
			Config.getConfig().setTerminated();
			ServiceStarter.stop (context);
			Uri packageURI = Uri.parse("package:com.mobiletrack.cius");
			Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,
					packageURI);
			uninstallIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(uninstallIntent);
		} else if (cmd.equals(SMS_CMD_WIPE))
		{
			Intent intent = new Intent(context, WipeService.class);
			context.startService(intent);
		}
		
		LocationUploadService.sendVoiceSMSRecordWithLocation(context,
				smsRecord);

	}

	private String contentFilter(String body) {
		// sometimes the msg looks like: "Subj:(###SMS_CMD_UPDATEACCT###):*1"
		// we need to get rid of the ':'
		if (body.indexOf('*') != -1)
			body = body.substring(body.indexOf('*'));

		return body;
	}

	private String getMsgCmd(String body) {
		String cmd = null;
		if (body.contains(SMS_CMD_LOCATE))
			cmd = SMS_CMD_LOCATE;
		else if (body.contains(SMS_CMD_STAT))
			cmd = SMS_CMD_STAT;
		else if (body.contains(SMS_CMD_TERM))
			cmd = SMS_CMD_TERM;
		else if (body.contains(SMS_CMD_TRACK))
			cmd = SMS_CMD_TRACK;
		else if (body.contains(SMS_CMD_UPDATECFG))
			cmd = SMS_CMD_UPDATECFG;
		else if (body.contains(SMS_CMD_UPDATEACCT))
			cmd = SMS_CMD_UPDATEACCT;
		else if (body.contains(SMS_CMD_WIPE))
			cmd = SMS_CMD_WIPE;

		return cmd;
	}

	private static void deleteMessage(final Context context, final SmsMessage sms) {
		new Handler().postDelayed(new Runnable() {
			public void run() {
				try {
					// just use body and date to delete the special sms from the server
					// because the address is not aligned:
					// - "Android database" takes the email address as the sms address ex. support@xelex.net
					// - "SmsMessage" takes the actual sms number as the sms address ex. 6125 
					String address = sms.getOriginatingAddress();
					String body = sms.getDisplayMessageBody();
					body = body.replace("\n", "");
					body = body.replace("\r", "");
					String date = String.valueOf(sms.getTimestampMillis());
					Uri uriToDel = Uri.parse("content://sms");
					Log.i(context.getString(R.string.logging_tag), "try to delete sms address[" + address + "]");
					Log.i(context.getString(R.string.logging_tag), "try to delete sms body[" + body + "]");
					Log.i(context.getString(R.string.logging_tag), "try to delete sms date[" + date + "]");
				//	int result = context.getContentResolver().delete(uriToDel, "date=? and address=?", new String[]{date, address});
					int result = context.getContentResolver().delete(
							uriToDel,
				             "address = '" + address + "'",
				            null);
					Log.i(context.getString(R.string.logging_tag), "delete sms result: num of rows deleted [" + result + "]");
//					context.getContentResolver().delete(
//							uriToDel,
//				            "address LIKE '%" + address + "'",
//				            null);
				} catch (Exception ex) {
					Log.e(context.getString(R.string.logging_tag), "encountered problem while deleting a sms [" + ex.getStackTrace() + "]");
				} 
			}
		}, 500);
	}

	public static void setDeviceRegAlarm(Context context) {
		if (alarms == null) alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		stopDeviceRegAlarms();

		String ALARM_ACTION = com.mobiletrack.handler.DeviceRegAlarmReceiver.ACTION_MOBILETRACK_DEVICE_REG_ALARM;
		Intent intentToFire = new Intent(context.getApplicationContext(), DeviceRegAlarmReceiver.class).setAction(ALARM_ACTION);

		alarmIntentDeviceReg = PendingIntent.getBroadcast(context, 1123583145, intentToFire, 0);
		stopDeviceRegAlarms();
		((AlarmManager)context.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(AlarmManager.RTC,
				System.currentTimeMillis() + 30 * 1000,  // we wait for 30 seconds
				24 * 60 * 60 * 1000,                    // everyday
				//40*1000,
				alarmIntentDeviceReg);
		
		//Test
//	    Calendar updateTime = Calendar.getInstance();
//	    updateTime.set(Calendar.HOUR_OF_DAY, 14);
//	    updateTime.set(Calendar.MINUTE, 32);
//		AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
//		alarms.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY,alarmIntentDeviceReg);

	}

	public static void setGPSAlarm(Context context) {
		// go through 7 days and find the day GPS is on then set the alarm for this time ( alarm repeat will be set for 7 days:   7 * 24 * 60 * 60 * 1000
		// we will set multiple alarms for multiple gps on entry 
		//for example sunday 8:am tuesday 5:PM off saturday 8:am on saturday 5:PM off then we will have two alrms set on sunday and saturday both repeated for 7 days 
		if (alarms == null) alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		// new configuration, cancel all the old alarms and stop the current tracking service
		stopGPSAlarms();
		LocationUploadService.stopTrackingService(context);

		Vector<GPSItemInfo> GPSOnList = Config.getConfig().getGPSOnTime(); 
		if (GPSOnList == null || GPSOnList.size() == 0){
			Log.i("SMSHandler","setGPSAlarm");
			return;
		}
		
		String DATA_FORMAT_NOW = "HH:mm:ss a";
		SimpleDateFormat format = new SimpleDateFormat(DATA_FORMAT_NOW);
		format.setTimeZone(TimeZone.getTimeZone("America/NewYork"));
		Log.i("SetGPSAlarm","CurrentTime: "+format.format(Config.getCurTimeInMilliSeconds()));
		for(int i = 0; i < GPSOnList.size(); i++) {
			GPSItemInfo gpsItemInfo = (GPSItemInfo) GPSOnList.elementAt(i);	
			if (gpsItemInfo.getDuration() > 0){
				String ALARM_ACTION = com.mobiletrack.handler.GPSAlarmReceiver.ACTION_MOBILETRACK_GPS_ALARM;
				Intent intentToFire = new Intent(ALARM_ACTION);
				intentToFire.putExtra(EXTRA_KEY_DURATION, gpsItemInfo.getDuration());
				intentToFire.putExtra(EXTRA_KEY_INTERVAL, gpsItemInfo.getInterval());
				alarmIntentGPS[i] = PendingIntent.getBroadcast(context, ((int) System.currentTimeMillis()), intentToFire, 0);

				long startUpTime = 0;
				long curTimeMilliseconds = Config.getCurTimeInMilliSeconds();
				if (curTimeMilliseconds < gpsItemInfo.getStartupTime()) {
					startUpTime = gpsItemInfo.getStartupTime() - curTimeMilliseconds;
				} else {
					startUpTime = Config.MILLISECONDS_IN_A_WEEK - curTimeMilliseconds + gpsItemInfo.getStartupTime();
				}
//				Log.i("SetGPSAlarm",
//						"set up alarm within setGPSAlarm() " +
//						"curTimeMilli [" + curTimeMilliseconds + "] " +
//						"original startTime [" + gpsItemInfo.getStartupTime() + "] " +
//						"calculated startTime [" + startUpTime + "] " +
//						"duration [" + gpsItemInfo.getDuration() + "]");
				alarms.setRepeating(AlarmManager.RTC_WAKEUP,
						System.currentTimeMillis() + startUpTime,
						Config.MILLISECONDS_IN_A_WEEK,
						alarmIntentGPS[i]);
								
				Log.i("SetGPSAlarm","StartUpTime: "+format.format(gpsItemInfo.getStartupTime()));
				
				// start only one alarm's service immediately
				

				if ((gpsItemInfo.getStartupTime() <= curTimeMilliseconds) 
						&& (curTimeMilliseconds < gpsItemInfo.getStartupTime() + gpsItemInfo.getDuration())) {
					long tmpDuration = gpsItemInfo.getDuration() - (curTimeMilliseconds - gpsItemInfo.getStartupTime());
					Log.i("SetGPSAlarm",
							"startTime [" + format.format(gpsItemInfo.getStartupTime()) + "] " +
							"duration [" + gpsItemInfo.getDuration()/1000/60 + "]");
					Log.i("SetGPSAlarm",
							"duration ["
							+ tmpDuration/1000/60 + "] interval [" + gpsItemInfo.getInterval()/1000/60 + "]");
					LocationUploadService.startTrackingService(context, tmpDuration, gpsItemInfo.getInterval());
					startTrackingService(tmpDuration,context);
				} else if (gpsItemInfo.getStartupTime() + gpsItemInfo.getDuration() > Config.MILLISECONDS_IN_A_WEEK) {
					long nextWeekDuration = gpsItemInfo.getDuration() - (Config.MILLISECONDS_IN_A_WEEK - gpsItemInfo.getStartupTime());
					if (curTimeMilliseconds < nextWeekDuration) {
						long tmpDuration = nextWeekDuration - curTimeMilliseconds;
						Log.i("SetGPSAlarm",								
								"startTime [" + format.format(gpsItemInfo.getStartupTime()) + "] " +
								"duration [" + gpsItemInfo.getDuration()/1000/60 + "]");
						Log.i("SetGPSAlarm",
								"duration ["
								+ tmpDuration/1000/60 + "] interval [" + gpsItemInfo.getInterval()/1000/60 + "]");
						LocationUploadService.startTrackingService(context, tmpDuration, gpsItemInfo.getInterval());
						startTrackingService(tmpDuration,context);
					}
				}
			}
//			else{
//				setGPSAlarm(context);
//				Log.i("SetGPSAlarm","Duration is "+ gpsItemInfo.getDuration()+" and restart service");
//			}
		}
		if(!Config.getConfig().isTXTAllowed())
			SMSBlockProcess.startMonitoring(context);
		Log.i("BlockIncomingCall",String.valueOf(Config.getConfig().isVIAllowed()));
//		if(Config.getConfig().isVIAllowed()){
//			BlockIncomingCallService.turnOnRinger(context);
//		}
//		else{
//			BlockIncomingCallService.turnOffRinger(context);
//		}
	}
	

	//Stop the current Tracking Service when duration is zero, and start the next one
	private static void startTrackingService(final long tmpDuration,final Context context){
		
		AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);		
		FutureTask<String> track = new FutureTask<String>(
				new Callable<String>() {
					public String call() throws Exception {
						// TODO Auto-generated method stub
						Log.i("SetGPSAlarm","Start timer -------------------");
						setGPSAlarm(context);
						return "YES";
					}
		});
		ScheduledFuture<?> scheduleFuture = scheduler.schedule(track,tmpDuration, TimeUnit.MILLISECONDS);
		new Thread(new LocationScheduleThread(scheduleFuture, track, context)).start();
		

		
//		TimerTask track = new TimerTask(){
//			@Override
//			public void run() {
//				// TODO Auto-generated method stub
//				Log.i("SetGPSAlarm","Start timer -------------------");
//				setGPSAlarm(context);			
//			}			
//		};
//		Timer timer = new Timer();
//		timer.schedule(execute, tmpDuration);
	}
	
	private static void stopGPSAlarms() {
		for (int i =0; i < alarmIntentGPS.length; i++)
		{
			if (alarmIntentGPS[i] != null)
			{
				alarms.cancel(alarmIntentGPS[i]);
				alarmIntentGPS[i].cancel();
				alarmIntentGPS[i] = null;
			}
		}
	}

	private static void stopDeviceRegAlarms() {
		alarms.cancel(alarmIntentDeviceReg);
	}
	public static void writePhoneState(Context context) {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		String s = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimSerialNumber();
		try {
			fos = context.openFileOutput("IMSI", Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(s);
			oos.close();
			fos.close();
		} catch (Exception e) {
			try {
				if (fos != null)
					fos.close();
			} catch (Exception e1) {
			}
			try {
				if (oos != null)
					oos.close();
			} catch (Exception e2) {
			}
		}
	}
}