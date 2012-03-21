package com.mobiletrack.process;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.ui.OverrideActivity;


public class SMSBlockProcess {

	public static HashSet<String> values = new HashSet <String>();
	private static final String blockedSMS = "BlockedSMS";
	protected static final long SUSPEND_UNTIL = 0;
	public static boolean OVERRIDE = false;


	public static void writeValues(Context context) {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = context.openFileOutput(blockedSMS, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(values);
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

	@SuppressWarnings("unchecked")
	public static void readValues(Context context) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput(blockedSMS);
			ois = new ObjectInputStream(fis);
			values = (HashSet<String>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			try {
				if (fis != null)
					fis.close();
			} catch (Exception e1) {
			}
			try {
				if (ois != null)
					ois.close();
			} catch (Exception e2) {
			}
		}
		if (values.size() == 0)
		{
			loadBlacklist(context);
		}
	}

	private static void loadBlacklist(Context context) {
		values.add("com.android.mms");
		values.add("com.motorola.blur.conversations");
		values.add("com.handcent.nextsms");
		values.add("com.sonyericcson.conversations");
		values.add("com.jb.mms");
		values.add("com.dexterltd.hide_my_sms_lite");
		writeValues (context);
	}

	protected Object alertDialog;
	private volatile static Thread t;
	public static synchronized void stopMonitoring()
	{
		try
		{
			t.interrupt();
		}
		catch (Exception e)
		{
		}
	}
	public static synchronized void startMonitoring(final Context context) {

		if (values.size() == 0)
		{
			readValues(context);
		}
		if (values == null)
		{
			values = new HashSet <String>();
		}
		if (t == null)
		{
			t = new Thread (new Runnable()
			{
				public void run()
				{

					Config.takeSnapShot(context);
					Config c = Config.getConfig();
					ActivityManager mgr = (ActivityManager) context
					.getSystemService(Context.ACTIVITY_SERVICE);
					int j = 0;
					while (!c.isAppTerminated())
					{
						if (j != 0)
							j = 0;
						while (!c.isTXTGenericAllowed() && !OVERRIDE)
						{
							if (j == 0) {
								c = Config.getConfig();
								j = 30;
								writeValues(context);
								addNotification (context);
							}
							RunningTaskInfo l = mgr.getRunningTasks(1).get(0);
							try
							{
								String p = l.topActivity.getPackageName();
								if (values.contains(p)) {
									//									String s = "android."
									//								if ("com.android.mms".equals(l.topActivity.getPackageName())){
									final Intent intent = new Intent();
									if (c.canOverride() == 0 ||! c.isTXTAllowed())
									{
										intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										intent.addCategory(Intent.CATEGORY_HOME);
										intent.setAction(Intent.ACTION_MAIN);
										String s = intent.resolveActivity(context.getPackageManager()).getPackageName();
										//									l = mgr.getRunningTasks(1).get(0);
										//									String s = l.topActivity.getPackageName();
										if (values.contains(s))
										{
											values.remove(s);
											writeValues(context);
										}
										if (s.equals(p))
										{
											break;
										}
										else
											context.startActivity(intent);
									}
									else
									{
										intent.setClass(context, OverrideActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										context.startActivity(intent);
									}
								}
							}
							catch (Exception e)
							{
							}

							try {
								Thread.sleep(2250);
							}
							catch (Exception e)
							{
							}
							j--;
						}
						removeNotification(context);
						try {
							Thread.sleep(45000);
						}
						catch (Exception e)
						{
						}
						c = Config.getConfig();
						if (!c.isSpeeding() && OVERRIDE)
							OVERRIDE = false;
					}
				}
			});
			t.start();
			t.setName("SMS Blocker");
		}
		else if (!t.isAlive())
		{
			t.start();
		}
		else
		{
			t.interrupt();
		}
	}

	public static void addNotification(Context context) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

		int icon = R.drawable.sms_block;
		CharSequence tickerText = "Voice/Text Disabled";
		long when = System.currentTimeMillis();
		Notification notification = new Notification( icon, tickerText, when);

		CharSequence contentTitle = "Voice/Text Disabled";
		CharSequence contentText = "Voice and/or Text messaging has been Disabled";
		Intent intent = new Intent();
		int temp = Config.getConfig().canOverride();
		if (temp == 0 || !Config.getConfig().isTXTAllowed())
		{
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addCategory(Intent.CATEGORY_HOME);
			intent.setAction(Intent.ACTION_MAIN);
		}
		else
		{
			intent.setClass(context, OverrideActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			//			context.startActivity(intent);
		}
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				intent, 0);
		notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		final int HELLO_ID = 4;
		mNotificationManager.notify(HELLO_ID, notification);
	}
	public static void removeNotification (Context context)
	{
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(4);
	}

}
