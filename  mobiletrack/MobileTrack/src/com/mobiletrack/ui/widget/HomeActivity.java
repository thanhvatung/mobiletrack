package com.mobiletrack.ui.widget;

import java.util.ArrayList;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.record.ExpenseRecord;
import com.mobiletrack.util.ServiceStarter;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.util.Log;


public class HomeActivity extends BaseActivity {
    private static final String TAG = "HomeActivity";
    private Config config;
    ArrayList<ExpenseRecord> records;
	private int id = 0;
//    private TagStreamFragment mTagStreamFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("FamilyTrack",this.toString());
        Config.takeSnapShot(this);
		config = Config.getConfig();
		if (config.isAppTerminated()) {
			ServiceStarter.stop(this.getApplicationContext());
			finish();
			return;
		}
//		else if (config.getReceiverServer().equals("www.google.com"))
//		{
//			//getCodes();
//			finish();
//			return;
//		}
        setContentView(R.layout.ui_activity_home);
        getActivityHelper().setupActionBar(null, 0);

        FragmentManager fm = getSupportFragmentManager();
//        checkNotification();
        ServiceStarter.go(this.getApplicationContext());
//        mTagStreamFragment = (TagStreamFragment) fm.findFragmentById(R.id.fragment_tag_stream);
    }
    
//	private void checkNotification() {
//		if (config.isAppTerminated()) {
//			removeNotification();
//		} else {
//			boolean needNotification = false;
//			for (int i = 0; i < records.size(); i++) {
//				if (records.get(i).TimerStarted)
//					needNotification = true;
//			}
//			if (needNotification)
//				addNotification(this.id);
//			else
//				removeNotification();
//		}
//	}
//	
//	private void removeNotification() {
//		String ns = Context.NOTIFICATION_SERVICE;
//		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
//		mNotificationManager.cancel(1);
//	}
//
//	private void addNotification(int j) {
//		String ns = Context.NOTIFICATION_SERVICE;
//		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
//
//		int icon = R.drawable.urgent_clock;
//		CharSequence tickerText = "Timer active";
//		long when = System.currentTimeMillis();
//		Notification notification = new Notification(icon, tickerText, when);
//
//		Context context = getApplicationContext();
//		CharSequence contentTitle = "An event timer is running";
//		CharSequence contentText = "Select to manage unsubmitted events.";
//		Intent notificationIntent = this.getIntent();
//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
//				notificationIntent, 0);
//		notification.setLatestEventInfo(context, contentTitle, contentText,
//				contentIntent);
//		final int HELLO_ID = 1;
//		mNotificationManager.notify(HELLO_ID, notification);
//	}
}
