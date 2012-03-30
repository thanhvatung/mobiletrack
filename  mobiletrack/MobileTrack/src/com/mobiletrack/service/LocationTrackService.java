package com.mobiletrack.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.mobiletrack.config.Config;
import com.mobiletrack.handler.SmsHandler;
import com.mobiletrack.process.SMSBlockProcess;
import com.mobiletrack.record.LocationRecord;
import com.mobiletrack.record.VoiceSMSRecord;
import com.mobiletrack.test.LocationLog;
import com.mobiletrack.util.MobileTrackListener;
import com.mobiletrack.util.ServiceStarter;

public class LocationTrackService extends Service {

	private LocationManager mlocManager;
	private ConnectivityManager conManager;
	public static Location current;
	private static Location lastLocation;
	public static double SPEED = 0;
	private TimerTask locationTask;
	private static LocationListener gpsListener;
	private static LocationListener cellListener;
	private final static int gpsLocationFinderID = 1;
	private final static int cellLocationFinderID = 2;
	
//	private static LocationListener smsListener;
	//	private static LocationListener cellSpeedListener;
	public static boolean RUNNING = false;
	private final static String LOCATION_UPDATE_CMD = "LOCATION_UPDATE_CMD";
	private final static String EXTRA_KEY_INTERVAL = "TRACKING_INTERVAL"; 
	private final static int LOCATION_UPDATE_CMD_LOCATE = 1;
	private final static int LOCATION_UPDATE_CMD_STOPSERVICE = 4;
	private final static int LOCATION_UPDATE_CMD_VOICE_SMS_RECORD = 3;
	private final static String EXTRA_KEY_VOICE_SMS_RECORD = "EXTRA_KEY_VOICE_SMS_RECORD";
	private static final double DIST_ERROR_MARGIN = 10;
	private static final long TIME_ERROR_MARGIN = 1;
	private static final double MIN_ACCEPTABLE_ACCURACY = 500;
	private static final double MOBILE_ACCEPTABLE_ACCURACY = 1200;
	private static final long ACCURACY_THRESHOLD = 100;
	private static final float ACCURACY_RATIO = 2;
	public static final int MIN_GPS_ACCURACY = 150;
	public static final int MIN_GPS_DISABLE_ACCURACY = 30;
	private static long LAST_TIME = 0;
	private static Looper l;
	private static Timer locationTimer;
	private static Stack <VoiceSMSRecord> toTag;
	private boolean blockFlag = false;
	public static String fakeLocationRecord = "";
	private static final float METER_PER_SECOND_2_MILES_PER_HOUR = 2.2369362920544F;

	private long interval;
	private long duration;
	private boolean isIntialized = false;
	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public static String getFakeLocationRecord(){
		return fakeLocationRecord;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Check if user is using mock locations
		    
		if (Settings.Secure.getString(getContentResolver(),Settings.Secure.ALLOW_MOCK_LOCATION).equals("1")) {
			//get the current time
			Calendar c = Calendar.getInstance(); 
			int year = c.get(Calendar.YEAR);
			// calendar start counting the month from zero
			int month = c.get(Calendar.MONTH)+1;
			int day = c.get(Calendar.DATE);
			
			int hour = c.get(Calendar.HOUR_OF_DAY);
			int minute = c.get(Calendar.MINUTE);
			int second =c.get(Calendar.SECOND);
			String localNumber = Config.getConfig().getPhoneNumber();
			String fakeLocationStartTime = year+"-"+month+"-"+day+" "+hour+":"+minute+":"+second;	
			String message = "There is an application on the phone that forces fake GPS readings";
			
			fakeLocationRecord = "MSG|"+localNumber+"|"+fakeLocationStartTime+"|"+message;
			Log.i("FakeLocation", fakeLocationRecord);
			//FTPTransferManager.sendRecordToServer(fakeLocationRecord, this);			
		}
		
		if (toTag == null)
			loadLists();
		if (intent == null)
		{
			return super.onStartCommand(new Intent(), flags, startId);
		}
		if (!ServiceStarter.RUNNING)
			ServiceStarter.go(this.getApplicationContext());
		if (l == null)
			l = Looper.myLooper();
		if (locationTimer == null){
			locationTimer = new Timer();
			Log.i("LocationTrackService -- Debug info", "locationTimer is null");
		}
		Config.takeSnapShot(this);
		mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		conManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		int cmd = intent.getIntExtra(LOCATION_UPDATE_CMD, 0);

		if (cmd == LOCATION_UPDATE_CMD_LOCATE) {
			Log.i("SMSHandler", "location track service");
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.0");
			if (Config.getConfig().getGPSInterval()<=0){
				Log.i("Location Interval",Long.toString(Config.getConfig().getGPSInterval()));
				return 0;
			}
			locateOnce(startId);
		}
		else if (cmd == LOCATION_UPDATE_CMD_VOICE_SMS_RECORD)
		{
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.1");
			VoiceSMSRecord voiceSMSRecord = (VoiceSMSRecord) intent.getExtras().getSerializable(EXTRA_KEY_VOICE_SMS_RECORD);
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.2");
			locateOnceForVoiceSMSRecord(voiceSMSRecord);
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.3");
		}
		else if (cmd == LOCATION_UPDATE_CMD_STOPSERVICE)
		{
			RUNNING = false;
			isIntialized  = false;
			callUpdates();
			unLocate();
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.4");
		}
		else{
			if (!RUNNING)
				locateOnce(startId);
			interval = intent.getLongExtra(EXTRA_KEY_INTERVAL, interval);
			duration = interval = intent.getLongExtra(SmsHandler.EXTRA_KEY_DURATION, interval);
			if (interval == 0)
				interval = 60000;
			RUNNING = true;
			confirmInitialization();
			//			if (cellSpeedListener == null)
			//				cellSpeedListener = new MobileTrackListener(this, 3);
			//			if (gpsSpeedListener == null)
			//				gpsSpeedListener = new MobileTrackListener (this);
			callUpdates();
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.5");
		}
		Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.6");
		return Service.START_STICKY;
	}

	@SuppressWarnings("unchecked")
	private void loadLists() {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = this.openFileInput("LOCATION_PIPE");
			ois = new ObjectInputStream(fis);
			toTag = (Stack<VoiceSMSRecord>) ois.readObject();
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
		if (toTag == null)
		{
			toTag = new Stack <VoiceSMSRecord>();
		}
	}

	private void saveLists()
	{
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = openFileOutput("LOCATION_PIPE", Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(toTag);
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

	private void locateOnce(int startId) {

		locationTask = new TimerTask(){
			@Override
			public void run() {
				try
				{
					//					Looper.prepare();
					sendRecord();				
					//					Looper.myLooper().quit();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		};
		Log.i("GPSstatus", "Method:LocateOne");
		locate();	
	}

	protected void sendRecord() {
		if (current != null)
		{
			if (current.getBearing() == 99999)
				current.setBearing (0);
			if (!current.hasSpeed())
			{
				current.setSpeed((float) SPEED);
			}
			Log.i("SMSHandler", "Current time: " + Long.toString(current.getTime()));
			Log.i("SMSHandler", "Last time: " + Long.toString(LAST_TIME));
			if (current.getTime() != LAST_TIME)
			{
				LocationRecord record = new LocationRecord(this, current, current.getAccuracy(), -1, -1);
				FTPTransferManager.sendRecordToServer(record.toString(), this);
				unLocate();
				LAST_TIME = current.getTime();
				Log.i("SMSHandler", "Msg sent" + Long.toString(LAST_TIME));
			}
		}
	}

	private void locate() {
		confirmInitialization();
		int timeout = Config.getConfig().getCallLocationTimeout();
		Log.i("GPSstatus", "timeout: "+ Integer.toString(Config.getConfig().getCallLocationTimeout()));
		Log.i("GPSstatus", "CallEnabled: " + Boolean.toString(Config.getConfig().isCallLocationEnabled()));
		
		mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, cellListener, l );
		if(Config.getConfig().isGPSEnabled())
			mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener, l );	
		locationTimer.schedule(locationTask, timeout*1000);
	}

	private void locateOnceForVoiceSMSRecord(final VoiceSMSRecord voiceSMSRecord) {
		String msgType = voiceSMSRecord.toString().substring(0, 3);
		Log.i("GPSstatus", "locateOnceForVoiceSMSRecord -- 2.1" + msgType);
		toTag.add(voiceSMSRecord);
		saveLists();
		
		
		if(!Config.getConfig().isCallLocationEnabled()){
			Log.i("GPSstatus", "locateOnceForVoiceSMSRecord -- 2.1.1: " + Config.getConfig().isCallLocationEnabled());
			Iterator<VoiceSMSRecord> itr = toTag.iterator();
			while (itr.hasNext()){
				VoiceSMSRecord temp = toTag.pop();
				toTag.remove(temp);
				FTPTransferManager.sendRecordToServer(temp.toString(), this);
				saveLists();
			}
			return;
		}
		else{
			locationTask = new TimerTask(){
				@Override
				public void run() {
					try
					{
						// Looper.prepare();
						Log.i("GPSstatus", "locateOnceForVoiceSMSRecord -- 2.2");
						sendRecords ();
						toTag.clear();						
						// Looper.loop();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}	
				}
			};
			Log.i("GPSstatus", "locateOnceForVoiceSMSRecord -- 2.3");
			locate();
		}
	}

	protected void sendRecords() {
		Log.i("LocationTrackService -- Debug info", "sendRecords method -- 3.1");
		if (toTag == null){
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 3.1.0, toTag is null");
			loadLists();
		}
		if (current != null)
		{
			if (current.getBearing() == 99999)
				current.setBearing (0);
			Iterator<VoiceSMSRecord> itr = toTag.iterator();
			while (itr.hasNext()){
				VoiceSMSRecord temp = toTag.pop();
				Log.i("LocationTrackService -- Debug info", "sendRecords method -- 3.2" + temp.toString());
				toTag.remove(temp);
				// if it's more than 2 minutes, set them as nothing.
				if (temp._eventEnd + 2*60*1000 > System.currentTimeMillis())
				{
					temp._latitude = current.getLatitude();
					temp._longitude = current.getLongitude();
				}
				else
				{
					temp._latitude = 0;
					temp._longitude = 0;
				}
				FTPTransferManager.sendRecordToServer(temp.toString(), this);
				saveLists();
			}
			current = null;
		}
		else{
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 3.3");
			Iterator<VoiceSMSRecord> itr = toTag.iterator();
			while (itr.hasNext()){
				VoiceSMSRecord temp = toTag.pop();
				toTag.remove(temp);
				FTPTransferManager.sendRecordToServer(temp.toString(), this);
				saveLists();
			}
		}
		unLocate();
	}
	
	public synchronized void reportLocation(Location location, int id) {
//		boolean replace = false;
		String networkTag = "3G";
		//For test only
		if (id == 1){
			Log.i("GPSstatus","Location Update: "+conManager.getActiveNetworkInfo().getTypeName()+" is working");			
			LocationLog.writeLocationLog(this,"GPS", location);
			networkTag = "GPS";
		}

//		if (conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isAvailable()
//				&& conManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting()
//				){
////			LocationLog.writeLocationLog(this,"3G",location);
//		}
//		else 
//		if (conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isAvailable()
//				&& conManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting()){
//			LocationLog.writeLocationLog(this,"WIFI",location);	
//			networkTag = "WIFI";
//		}

		//end

		//Just for test 
//		if (location.getSpeed() == 0){
//			location.setSpeed(10/ METER_PER_SECOND_2_MILES_PER_HOUR);
//	    }
		
		float speedMilesPerHour = METER_PER_SECOND_2_MILES_PER_HOUR * location.getSpeed();
		Log.i("GPSstatus","Speed: "+ speedMilesPerHour);
		Log.i("GPSstatus","BlockFlag: "+ blockFlag);
		Log.i("GPSstatus","interval: "+ interval);

		
		SPEED = location.getSpeed();
//		Config.getConfig().setCurrentSpeed((int)SPEED);
//		Config.takeSnapShot(this);
		
		Log.i("GPSstatus","Speed Limit: "+ Config.getConfig().getSpeedThreshold());
		Log.i("GPSstatus","Current Speed: "+ Config.getConfig().getCurrentSpeed());

		
		if (!Config.getConfig().isSpeeding() && blockFlag == true){
//			LocationUploadService.stopTrackingService(this);
//			LocationUploadService.startTrackingService(this, duration,interval);
			Log.i("GPSstatus","Stop SpeedDetectionService and Monitoring");
			SpeedDetectionService.stopDetection(this);
			SMSBlockProcess.stopMonitoring();
			blockFlag = false;
		}
		//end test
		
		if (current == null)
		{
			current = location;
			if (lastLocation == null)
				lastLocation = location;
//			current.setSpeed(0);
			if (location.hasSpeed()){
				current.setSpeed(location.getSpeed());
				SPEED = location.getSpeed();
			}
			else{
				SPEED = 0;
			}
//			locationTask.run();
			Log.i("GPSstatus","Location Update: "+"null");
		}
		// some math:
		/*
		 * we should try to calculate speed if the distance between us now and us previously is
		 *  greater than our average accuracy. Oh, or if more than 30 seconds have passed.
		 */
		if (location.hasSpeed())
		{
			if (Config.getConfig().isSpeeding()&&blockFlag == false){
				blockFlag = true;
				Log.i("GPSstatus","isSpeeding: " + Config.getConfig().isSpeeding());
			//	SMSBlockProcess.startMonitoring(this.getApplicationContext());
				Log.i("GPSstatus","Interval value:"+interval);
				if (interval >= 5*60*1000){
//					LocationUploadService.stopTrackingService(this);
//					long tempInterval = 60*1000;
//					LocationUploadService.startTrackingService(this, duration,tempInterval);
//					Log.i("BlockSMSService","use temp interval value:"+String.valueOf(tempInterval));
					SpeedDetectionService.startDetection(this);
				}
				mlocManager.removeUpdates(gpsListener);
				mlocManager.removeUpdates(cellListener);
			}
			if (!location.hasBearing())
				location.setBearing(lastLocation.bearingTo(location));
			if (!current.hasBearing())
				current.setBearing(lastLocation.bearingTo(location));
			lastLocation = location;
			current = location;
			if (location.getAccuracy() < ACCURACY_THRESHOLD)
				disableCell();
//			locationTask.run();
//			return;
			Log.i("GPSstatus","Location Update: "+"has speed: " + Config.getConfig().getCurrentSpeed());
		}
		else
		{
			location = setSpeed (location, lastLocation);
			if (!location.hasBearing())
				location.setBearing(lastLocation.bearingTo(location));
			if (!current.hasBearing())
				current.setBearing(lastLocation.bearingTo(location));
			current = location;
			Log.i("GPSstatus","Location Update: "+"has no speed:" + Config.getConfig().getCurrentSpeed());
		}
		//Config.getConfig().setCurrentSpeed((int)SPEED);
		//Config.takeSnapShot(this);
		Log.i("GPSstatus","Location Update: "+"come here: "+ Config.getConfig().getCurrentSpeed());
		

//		// if the new location's accuracy is less than twice as inaccurate as the old location's
//		// or within the acceptable threshold
//		// or if it's been more than fifteen minutes.
//		if (location.getAccuracy()/current.getAccuracy() < ACCURACY_RATIO
//				|| location.getAccuracy() < MIN_GPS_ACCURACY
//				|| location.getTime() > current.getTime() + 15*60*1000)
//		{
//			Log.i("LocationTrackService -- Debug info","Location Update: "+"come here:--2");
//			// it's probably worth taking a new location if we're within twice the old accuracy and it's been more than a minute.
//			if (location.getTime() > 60*1000 + current.getTime() && location.getAccuracy()/current.getAccuracy() < ACCURACY_RATIO)
//			{
//				replace = true;
//			}
//			// failing that, how's our accuracy? Improvements should be kept.
//			else if (location.getAccuracy() <= current.getAccuracy())
//			{
//				replace = true;
//			}
//			// if we moved more than the average accuracy, we've moved in real
//			// terms, and this should be reflected.
//			else if (location.distanceTo(current) >  (location.getAccuracy()+ lastLocation.getAccuracy()))
//			{
//				replace = true;
//			}
//
//			if (replace)
//				current = setSpeed (location, current);
//			lastLocation = location;
//		}
	}


	private Location setSpeed(Location currentLocation, Location previousLocation) {
		if (currentLocation.hasSpeed())
			return currentLocation;

		double dist = currentLocation.distanceTo(previousLocation);
		double curTime = currentLocation.getTime();
		double lastTime = previousLocation.getTime();
		double timeDiff = (curTime - lastTime)/1000;
		double newAccuracy = currentLocation.getAccuracy();
		double oldAccuracy = previousLocation.getAccuracy();
		double baseAccuracy = newAccuracy + oldAccuracy;
		if (timeDiff > TIME_ERROR_MARGIN * (interval/1000))
		{
			SPEED = 0;
			currentLocation.setSpeed((float) SPEED);
		}
		else if ((timeDiff > (interval/1000)/TIME_ERROR_MARGIN && timeDiff > 60  && baseAccuracy < MIN_ACCEPTABLE_ACCURACY)|| baseAccuracy < dist/DIST_ERROR_MARGIN)
		{
			SPEED = ((dist)/timeDiff);

			currentLocation.setSpeed((float) SPEED);
			if (Config.getConfig().isSpeeding())
				SMSBlockProcess.startMonitoring(this.getApplicationContext());
		}
		if (SPEED < .1)
			SPEED = 0;
		
		return currentLocation;
	}

	protected void unLocate()
	{
		if (mlocManager != null)
		{
			if (gpsListener != null)
				mlocManager.removeUpdates(gpsListener);
			if (cellListener != null)
				mlocManager.removeUpdates(cellListener);
//			if (smsListener != null)
//				mlocManager.removeUpdates(smsListener);
		}

	}

	private synchronized void callUpdates() {
		if (RUNNING)
		{
			Log.i("GPSstatus", "callUpdates -- 1.5.1");
			int timeout = Config.getConfig().getCallLocationTimeout();
			TimerTask startTimer = new TimerTask(){
				@Override
				public void run() {
					try
					{
						Log.i("GPSstatus", "callUpdates -- Start");
						confirmInitialization();
						if(Config.getConfig().isGPSEnabled())
							mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener, l );
						mlocManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, cellListener, l );
						
//						mlocManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 0, 0, smsListener, l );
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			};
			TimerTask endTimer = new TimerTask(){
				@Override
				public void run() {
					try
					{
						Log.i("GPSstatus", "callUpdates -- End");
						unLocate();
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			};
			if (! isIntialized)
			{
				Log.i("GPSstatus", "callUpdates -- Interval:"+ interval );
				isIntialized = true;
//				if ( interval/2 > 90000)
//				{
//					locationTimer.schedule (startTimer, 90000, 90000);
//					if (timeout * 1000 < interval)
//						locationTimer.schedule(endTimer, 90000 + timeout * 1000, 90000);
//					else
//						locationTimer.schedule(endTimer, 120000, 90000);
//				}
//				else
//				{
					locationTimer.schedule (startTimer,0, interval);
					if (timeout * 1000 < interval)
						locationTimer.schedule(endTimer, timeout * 1000, interval);
					else
						locationTimer.schedule(endTimer, 3*interval/4, interval);
//				}
			}
			else{
				Log.i("GPSstatus", "callUpdates -- 1.5.3");
			}
		}
		else
		{
			unLocate();
			locationTimer.cancel();
			locationTimer.purge();
			locationTimer = new Timer();
			Log.i("LocationTrackService -- Debug info", "sendRecords method -- 1.5.4");
		}
	}

	protected void confirmInitialization() {
		if (gpsListener == null)
			gpsListener = new MobileTrackListener(this, gpsLocationFinderID);
		if (cellListener == null)
			cellListener = new MobileTrackListener(this, cellLocationFinderID);
//		if (smsListener == null)
//			smsListener = new MobileTrackListener(this, 3);
		if (l == null)
			l = Looper.myLooper();
	}

	public void disableCell()
	{
		if (cellListener != null)
		{
			mlocManager.removeUpdates(cellListener);
		}
		else
		{
			if (current.getAccuracy() < MIN_GPS_DISABLE_ACCURACY)
			{
				cellListener = new MobileTrackListener (this, 2);
				unLocate();
			}
		}
	}
	
	public void disableGPS()
	{
		if (gpsListener != null)
		{
			mlocManager.removeUpdates(gpsListener);
		}
		else
		{
			if (current.getAccuracy() < MIN_GPS_DISABLE_ACCURACY)
			{
				gpsListener = new MobileTrackListener (this, 1);
				unLocate();
			}
		}
	}
	
	public int getCellLocationFinderID(){
		return cellLocationFinderID;
	}
	
	public int getGPSLocationFinderID(){
		return gpsLocationFinderID;
	}
}
