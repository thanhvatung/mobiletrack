package com.mobiletrack.service;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.handler.HeadsetPlugReceiver;
import com.mobiletrack.process.SMSBlockProcess;

public class SpeedDetectionService extends Service {
	private static LocationManager _locManager;
	private static LocationListener locListener;
	private static final float SPEED_DETECTION_GPS_MIN_DISTANCE = 10.0F; // 10 meters
	private static final long SPEED_DETECTION_GPS_INTERVAL = 2*60 * 1000; // 2 minutes
	// 1 Meter per Second = 2.2369362920544 Miles per Hour
	private static final float METER_PER_SECOND_2_MILES_PER_HOUR = 2.2369362920544F;
	private static boolean started = false;
	private static HeadsetPlugReceiver receiver;
	private static Location mLastLocation = null; 
	private int count = 0;
	private boolean isGPSFix;
	private long mLastLocationMillis = 0;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		start();
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static void startDetection(Context context) {
		if (!started) {
			started = true;
			Log.i("GPSstatus", "SpeedDetectionService -- Start -- 1:"+started);
			Intent intent = new Intent(context, SpeedDetectionService.class);
			context.startService(intent);
		}
	}

	public static void stopDetection(Context context) {
		if (started) {
			started = false;
			Log.i("GPSstatus", "SpeedDetectionService -- Start -- 2:"+started);
			_locManager.removeUpdates(locListener);
			Intent intent = new Intent(context, SpeedDetectionService.class);
			context.stopService(intent);
			
		}
	}

	private void start() {
		SMSBlockProcess.startMonitoring(this.getApplicationContext());
		receiver = HeadsetPlugReceiver.getInstance();
		registerReceiver(receiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
		Config.takeSnapShot(this);
		String provider = initLocManager();
		if (provider == null)
			return;
		//final Context context = this;
		locListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				Log.i("GPSstatus", "SpeedDetection service: got location [" + location + "]");
				if (location != null) {
					mLastLocationMillis = SystemClock.elapsedRealtime();
					//float speedMeterPerSecond = calculateSpeedInMeterPerSecond(mLastLocation, location);
					mLastLocation = location;
					//if (location.hasSpeed()) speedMeterPerSecond = location.getSpeed(); // meters/second
					float speedMilesPerHour = METER_PER_SECOND_2_MILES_PER_HOUR * location.getSpeed();//speedMeterPerSecond;
					Log.i("GPSstatus", "SpeedDetection service: speed [" + speedMilesPerHour + "]");
					if(speedMilesPerHour < Config.getConfig().getSpeedThreshold()){
						Log.i("GPSstatus", "Count ++:"+ count);
						count++;
					}
					else{
						Log.i("GPSstatus", "Set count to 0");
						count = 0;
					}
					
				//	Config.getConfig().setCurrentSpeed((int) speedMilesPerHour);
				//	Config.takeSnapShot(getApplicationContext());
				//	Log.i("GPSstatus", "SpeedDetection service: Current Speed [" + Config.getConfig().getCurrentSpeed() + "]");
				} else {
					Log.i("GPSstatus", "SpeedDetection service: returned location is null this time");
				//	Config.getConfig().setCurrentSpeed(0);
				//	Config.takeSnapShot(getApplicationContext());
				}
				
				if(count == 2){
					Log.i("GPSstatus", "Stop Monitoring");
					SMSBlockProcess.stopMonitoring();
					stopDetection(getApplicationContext());
					count = 0;
				}
			}
			public void onProviderDisabled(String provider) {
			//	Config.getConfig().setCurrentSpeed(0);
			//	Config.takeSnapShot(getApplicationContext());
			}
			public void onProviderEnabled(String provider) {}
			public void onStatusChanged(String provider, int status, Bundle extras) {
				if(status == 0){ //out of service
				//	Config.getConfig().setCurrentSpeed(0);
				//	Config.takeSnapShot(getApplicationContext());
				}
			}
		};
		_locManager.requestLocationUpdates(provider, SPEED_DETECTION_GPS_INTERVAL, SPEED_DETECTION_GPS_MIN_DISTANCE, locListener);
		_locManager.addGpsStatusListener(new MyGPSListener());
		Log.i("GPSstatus", "start speed detection service");
	}

	protected float calculateSpeedInMeterPerSecond(Location lastLocation, Location currentLocation) {
		/*
		 *  Formula:
        var R = 6371; // km
        var dLat = (lat2-lat1).toRad();
        var dLon = (lon2-lon1).toRad(); 
        var a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(lat1.toRad()) * Math.cos(lat2.toRad()) * 
                Math.sin(dLon/2) * Math.sin(dLon/2); 
        var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
        var d = R * c;
		 */
		if (null == lastLocation) {
			return 0;
		}
		long dTime = currentLocation.getTime() - lastLocation.getTime();
		if (dTime > 1800000) // too long for the last location
		{
			return 0;
		}
		float dist = currentLocation.distanceTo(lastLocation);

		return dist * 1000 / dTime;
	}

	private String initLocManager() {
		String context = Context.LOCATION_SERVICE;
		_locManager = (LocationManager)getSystemService(context);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setSpeedRequired(true);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);
		String provider = _locManager.getBestProvider(criteria, true);

		if (provider == null || provider.equals("")) {
			displayGPSNotEnabledWarning(this);
			return null;
		}

		return provider;
	}

	private static void displayGPSNotEnabledWarning(Context context) {
		String msg = "Your GPS is disabled, Please enable it!";
		Toast toast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		unregisterReceiver(receiver);
	}
	
	class MyGPSListener implements GpsStatus.Listener {
		private int count = 0;
	    public void onGpsStatusChanged(int event) {
	        switch (event) {
	            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
	                if (mLastLocation != null){
	                	Log.i("GPSstatus", "Last Millis:"+ mLastLocationMillis);
	                    isGPSFix = (SystemClock.elapsedRealtime() - mLastLocationMillis) < 3000;
	                }
	                if (isGPSFix) { // A fix has been acquired.
	                    // Do something.
	                	Log.i("GPSstatus", "I have GPS Signal");
	                } else { // The fix has been lost.
	                    // Do something.
	                	count++;
	                	Log.i("GPSstatus", "Lost GPS Signal"+count);
	                	if(count >= Config.getConfig().getCallLocationTimeout()){
	                		SMSBlockProcess.stopMonitoring();	
//	                		SMSBlockProcess.removeNotification(getBaseContext());
	                		stopDetection(getBaseContext());	                		
	                		Config.getConfig().setCurrentSpeed(0);
	                		_locManager.removeGpsStatusListener(this);
	                		count = 0;
	                		Log.i("GPSstatus", "STOP!");
	                	}
	                }
	                break;
	            case GpsStatus.GPS_EVENT_FIRST_FIX:
	                // Do something.
	                isGPSFix = true;
	                break;
	        }
	    }
	}

}
