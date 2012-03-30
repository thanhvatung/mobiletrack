package com.mobiletrack.ui;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.TimeZone;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.handler.SmsHandler;
import com.mobiletrack.handler.TimeScheduleAlarmReceiver;
import com.mobiletrack.test.LocationLog;
import com.mobiletrack.ui.widget.BaseActivity;
import com.mobiletrack.util.ServiceStarter;

public class CustomerCodeActivity extends BaseActivity implements OnClickListener{

	private String record;
	private boolean agreed = false;
	public static String PHONE_NUMBER;
	public static String SERVER_BASE;
	private boolean initSucceed = false;
	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.customercode);
		getActivityHelper().setupActionBar(getTitle(), 0);
		loadLists();

		findViewById(R.id.button_config).setOnClickListener(this);
		findViewById(R.id.button_initialize).setOnClickListener(this);
		findViewById(R.id.button_codes).setOnClickListener(this);
		findViewById(R.id.button_return).setOnClickListener(this);
		findViewById(R.id.button_return).setEnabled(false);
		if (agreed)
		{
			((LinearLayout) findViewById(R.id.box_eula).getParent()).removeView(findViewById(R.id.box_eula));
		}
		else
		{
			setAgreement (agreed);

			findViewById(R.id.box_eula).setOnClickListener(new OnClickListener(){

				public void onClick(View v) {
					agreed = ((CheckBox) v).isChecked();
					setAgreement (agreed);
				}});
		}

		findViewById(R.id.button_eula).setOnClickListener(new OnClickListener() 
		{
			public void onClick(View v) 
			{
				Intent intent = new Intent(Intent.ACTION_VIEW); 
				Uri uri = Uri.parse(getString (R.string.xelex_eula_address)); 
				intent.setData(uri); 
				startActivity(intent);
			}
		});
	}
	protected void setAgreement(boolean agreed2) {
		agreed = agreed2;
		findViewById(R.id.button_codes).setEnabled(agreed);
		findViewById(R.id.button_config).setEnabled(agreed);
		findViewById(R.id.button_initialize).setEnabled(agreed);

	}

	public void onClick(View v) {
		saveLists();
		Config.takeSnapShot(this);
		if (v.getId() == findViewById(R.id.button_initialize).getId())
		{
			initialize();
			return;
		}
		else if (v.getId() == findViewById(R.id.button_codes).getId())
		{
			getCodes();
			return;
		}
		else if (v.getId() == findViewById(R.id.button_config).getId())
		{
			getConfig();
			return;
		}
		else if (v.getId() == findViewById(R.id.button_return).getId())
		{
			ServiceStarter.go(this);
			finish();
			return;
		}

	}

	private void getCodes() {

		new Fetcher().execute("CODES");
		/*Thread t = new Thread(){ 
			public void run(){
				String toCall = getWebService (SERVICE + record);
				toCall = toCall.replaceAll("\\<.*?>","");
				//		Toast.makeText(this, toCall, Toast.LENGTH_LONG).show();
				if (!( toCall == null || toCall.equals("")))
				{
					Config.getConfig().update(toCall+ CODES + PHONENUMBER);
					//					Toast.makeText(this, "Account Codes Sucessfully updated.", Toast.LENGTH_LONG).show();
				}
			}
		};
		t.run();*/
	}

	private void getConfig() {

		new Fetcher().execute("CONFIG");
		/*
		Thread t = new Thread(){ 
			public void run(){
				String toCall = getWebService (SERVICE + record);
				//		toCall = getURL (toCall);
				toCall = toCall.replaceAll("\\<.*?>","");
				if (!( toCall == null || toCall.equals("")))
				{
					Config.getConfig().update(toCall+ CONFIG + PHONENUMBER);
					//Toast.makeText(this, "Configuration Sucessfully updated.", Toast.LENGTH_LONG).show();
				}
			}
		};
		t.run();
		 */
		new Fetcher().execute("INITIALIZE");
		SmsHandler.setGPSAlarm(this.getApplicationContext());
		setRecurringAlarm(this.getApplicationContext());
	    //		if(LocationLog.canWriteToFlash()){
//		}
	}




	private void initialize() {
		new Fetcher().execute("CONFIG");
		new Fetcher().execute("INITIALIZE");
		SmsHandler.setGPSAlarm(this.getApplicationContext());
		setRecurringAlarm(this.getApplicationContext());
		new Fetcher().execute("CODES");
	}
	
	private void setRecurringAlarm(Context context) {
		 
	    Calendar updateTime = Calendar.getInstance();
	    updateTime.set(Calendar.HOUR_OF_DAY, 24);
	    updateTime.set(Calendar.MINUTE, (int)(Math.random()*5));

	    Log.i("SetGPSAlarm","Start at:"+updateTime.getTime());
	    Intent timeSchedule = new Intent(context, TimeScheduleAlarmReceiver.class);
	    timeSchedule.setAction(".handler.Time_Schedule_ALARM");
	    PendingIntent recurringTimeSchedule = PendingIntent.getBroadcast(context,
	            0, timeSchedule, PendingIntent.FLAG_CANCEL_CURRENT);
	    AlarmManager alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
	   // alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY,recurringTimeSchedule);
	   alarms.setRepeating(AlarmManager.RTC_WAKEUP, updateTime.getTimeInMillis(), AlarmManager.INTERVAL_DAY,recurringTimeSchedule);
	}
	

	private void saveLists() {
		record = ((EditText) findViewById(R.id.customer_code)).getText().toString();
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = this.openFileOutput("CUSTOMER", Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(record);
			oos.close();
			fos.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while saving the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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

		PHONE_NUMBER = ((EditText) findViewById(R.id.phone_number)).getText().toString();
		//		FileOutputStream fos = null;
		//		ObjectOutputStream oos = null;
		try {
			fos = this.openFileOutput("NUMBER", Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(PHONE_NUMBER);
			oos.close();
			fos.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while saving the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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
		try {
			fos = this.openFileOutput("AGREE", Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(agreed);
			oos.close();
			fos.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while saving the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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


	public static void GET_NUMBER (Context context)
	{
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput("NUMBER");
			ois = new ObjectInputStream(fis);
			PHONE_NUMBER = (String) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.e(context.getString(R.string.logging_tag),
					"Got Exception while loading the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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

		if (PHONE_NUMBER == null)
		{
			PHONE_NUMBER = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
			if (PHONE_NUMBER == null)
				PHONE_NUMBER = "";
		}
		Log.i("GUI","phoneNumber: "+ PHONE_NUMBER);
	}

	
	public static void GET_SERVER_BASE(Context context)
	{
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput("SERVER_BASE");
			ois = new ObjectInputStream(fis);
			SERVER_BASE = (String) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.e(context.getString(R.string.logging_tag),
					"Got Exception while loading the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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

		if (SERVER_BASE == null)
		{
			String CCode = "";
			
			fis = null;
			ois = null;

			//------------------
			//get the Customer Code
			try {
				fis = context.openFileInput("CUSTOMER");
				ois = new ObjectInputStream(fis);
				CCode = (String) ois.readObject();
				ois.close();
				fis.close();
			} catch (Exception e) {
				Log.e(context.getString(R.string.logging_tag),
						"Got Exception while loading the Lists exp ["
						+ e.getClass().getName() + "] msg ["
						+ e.getMessage() + "]");
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
			
			
			String toCall = getWebService (Config.SERVICE + CCode);
			SERVER_BASE = toCall.replaceAll("\\<.*?>","");

			//------------------
			//Now save the SERVER_BASE
			FileOutputStream fos = null;
			ObjectOutputStream oos = null;
			try {
				
				fos = context.openFileOutput("SERVER_BASE", Context.MODE_PRIVATE);
				oos = new ObjectOutputStream(fos);
				oos.writeObject(SERVER_BASE);
				oos.close();
				fos.close();
			} catch (Exception e) {
				Log.e(context.getString(R.string.logging_tag),
						"Got Exception while saving the Lists exp ["
						+ e.getClass().getName() + "] msg ["
						+ e.getMessage() + "]");
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
			//------------------

			
			if (SERVER_BASE == null)
				SERVER_BASE = "";
		}
		Log.i("GUI","SERVER_BASE: "+ SERVER_BASE);
	}
	
	
	
	private static String getWebService(String string) {
		try
		{
			String address = string;
			URL url = new URL(address);
			URLConnection connection = url.openConnection();
			HttpURLConnection httpConnection = (HttpURLConnection) connection;
			int responseCode = httpConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String input ="";
				String line;
				while ((line = in.readLine()) != null) {					
					input += line;
				}
				Log.i("WebService","Webservice url: "+input);
				return input;
			}
		}
		catch (Exception e)
		{
		}
		return "";
	}

	
	/**
	 * Load the list information from the persistent file
	 */

	private void loadLists() {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = this.openFileInput("CUSTOMER");
			ois = new ObjectInputStream(fis);
			record = (String) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while loading the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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

		if (record == null)
			record = "";
		else
			((EditText) findViewById(R.id.customer_code)).setText(record);

		try {
			fis = this.openFileInput("NUMBER");
			ois = new ObjectInputStream(fis);
			PHONE_NUMBER = (String) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while loading the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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
		try {
			fis = this.openFileInput("AGREE");
			ois = new ObjectInputStream(fis);
			agreed = ((Boolean) ois.readObject()).booleanValue();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while loading the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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
		if (PHONE_NUMBER == null)
		{
			PHONE_NUMBER = ((TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
			if (PHONE_NUMBER == null)
				PHONE_NUMBER = "";
		}
		else
			((EditText) findViewById(R.id.phone_number)).setText(PHONE_NUMBER);
	}


	private class Fetcher extends AsyncTask<String, String, Void> {

		@Override
		protected Void doInBackground(String... param) {

			String params = param[0];
			if (params.equals("INITIALIZE"))
			{
				getServerBase();
				
				if (!( SERVER_BASE == null || SERVER_BASE.equals("")))
				{
					Config.getConfig().update(SERVER_BASE+ Config.CONFIG_ServiceCall + PHONE_NUMBER);
					Config.getConfig().update(SERVER_BASE + Config.CODES_ServiceCall + PHONE_NUMBER);
					publishProgress("Initialization sucessful.");
					publishProgress ("ENABLE");
					return null;

				}
				publishProgress ("Configuration failed to initialize.");
			}
			else if (params.equals("CONFIG"))
			{
				if ( SERVER_BASE == null || SERVER_BASE.equals(""))
				{
					getServerBase();
				}
				
				if (!(SERVER_BASE == null || SERVER_BASE.equals("")))
				{
					Config.getConfig().update(SERVER_BASE+ Config.CONFIG_ServiceCall + PHONE_NUMBER);
					//Toast.makeText(this, "Configuration Sucessfully updated.", Toast.LENGTH_LONG).show();
					publishProgress ("Configuration Sucessfully updated.");
					publishProgress ("ENABLE");
					return null;
				}
				publishProgress ("Configuration may not have updated sucessfully.");
			}
			else if (params.equals("CODES"))
			{
				if ( SERVER_BASE == null || SERVER_BASE.equals(""))
				{
					getServerBase();
				}
				
				if (!( SERVER_BASE == null || SERVER_BASE.equals("")))
				{
					Config.getConfig().update(SERVER_BASE+ Config.CODES_ServiceCall + PHONE_NUMBER);
					publishProgress ("Client Codes Sucessfully updated.");
					publishProgress ("ENABLE");
					return null;
					//					Toast.makeText(this, "Account Codes Sucessfully updated.", Toast.LENGTH_LONG).show();
				}
				publishProgress ("Client Codes may not have updated sucessfully.");
			}
			else
			{
				publishProgress ("An error has occurred.");
			}
			this.cancel(true);
			return null;
		}
		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */

		private void getServerBase()
		{
			String toCall = getWebService (Config.SERVICE + record);
			//		toCall = getURL (toCall);
			toCall = toCall.replaceAll("\\<.*?>","");
			SERVER_BASE = toCall;
			
			FileOutputStream fos = null;
			ObjectOutputStream oos = null;
			try {
				
				fos = openFileOutput("SERVER_BASE", Context.MODE_PRIVATE);
				oos = new ObjectOutputStream(fos);
				oos.writeObject(SERVER_BASE);
				oos.close();
				fos.close();
			} catch (Exception e) {
				Log.e(getString(R.string.logging_tag),
						"Got Exception while saving the Lists exp ["
						+ e.getClass().getName() + "] msg ["
						+ e.getMessage() + "]");
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
		
		protected void onProgressUpdate(String... values) {
			if (values[0].equals("ENABLE"))
				findViewById(R.id.button_return).setEnabled(true);
			else
				Toast.makeText(getBaseContext(), values[0], Toast.LENGTH_LONG).show();

		}

		private String getWebService(String string) {
			try
			{
				String address = string;
				URL url = new URL(address);
				URLConnection connection = url.openConnection();
				HttpURLConnection httpConnection = (HttpURLConnection) connection;
				int responseCode = httpConnection.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_OK) {
					BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
					String input ="";
					String line;
					while ((line = in.readLine()) != null) {					
						input += line;
					}
					Log.i("WebService","Webservice url: "+input);
					return input;
				}

			}
			catch (Exception e)
			{
				publishProgress( "Code "+ record + " does not appear to be valid. Please check your code and network connection, and try again.");

			}
			this.cancel(true);
			return "";
		}
	}

//	public void onBackPressed() {
//		if (agreed && Config.getConfig()!= null)
//			super.onBackPressed();
//		else
//		{
//			Intent intent = new Intent();
//			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			intent.addCategory(Intent.CATEGORY_HOME);
//			intent.setAction(Intent.ACTION_MAIN);
//			startActivity (intent);
//		}
//	}
}
