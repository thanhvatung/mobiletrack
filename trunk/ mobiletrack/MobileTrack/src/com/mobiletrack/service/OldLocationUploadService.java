package com.mobiletrack.service;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.record.LocationRecord;
import com.mobiletrack.record.VoiceSMSRecord;

public class OldLocationUploadService extends Service {
	private final static String EXTRA_KEY_DURATION = "TRACKING_DURATION";
	private final static String EXTRA_KEY_INTERVAL = "TRACKING_INTERVAL"; 
	private final static String LOCATION_UPDATE_CMD = "LOCATION_UPDATE_CMD";
	private final static String EXTRA_KEY_VOICE_SMS_RECORD = "EXTRA_KEY_VOICE_SMS_RECORD";
	private static final String NMEA_DOP_SENTENCE = "$GPGSA";

	private final static int LOCATION_UPDATE_CMD_LOCATE = 1;
	private final static int LOCATION_UPDATE_CMD_TRACK = 2;
	private final static int LOCATION_UPDATE_CMD_VOICE_SMS_RECORD = 3;
	private final static int LOCATION_UPDATE_CMD_STOPSERVICE = 4;
	private LocationManager _locManager;
	private Hashtable<Integer, Object[]> _serviceUtilityPool;
	private Hashtable<Integer, Object[]> _serviceDilutionPool;
	private final static int LOCATION_RECORD_BUFFER_SIZE = 3;//10;
	private static int _record_buffer_count = 0;
	private final static String EMPTY_BUFFER = "";
	private static String _record_buffer = EMPTY_BUFFER;
	private static final float DEFAULT_GPS_MIN_DISTANCE = 0.0F;//1000.0F;
	private static final float LOCATE_ONCE_GPS_MIN_DISTANCE = 10.0F;//1000.0F;
	private static long _lastGPSRecordTime = 0;
	private static Context _context;
	private static Location mLastLocation = null;

	@Override
	public int onStartCommand(Intent intent, int flags, final int startId) {
		if (_serviceUtilityPool == null) {
			_serviceUtilityPool = new Hashtable<Integer, Object[]>();
		}
		if (_serviceDilutionPool == null) {
			_serviceDilutionPool = new Hashtable<Integer, Object[]>();
		}
		if (intent == null)
		{
			return super.onStartCommand(new Intent(), flags, startId);
		}

		Config.takeSnapShot(this);
		int cmd = intent.getIntExtra(LOCATION_UPDATE_CMD, 0);
		final long interval = intent.getLongExtra(EXTRA_KEY_INTERVAL, 0);
		if (cmd == LOCATION_UPDATE_CMD_LOCATE) {
			locateOnce(startId);
		} else if (cmd == LOCATION_UPDATE_CMD_VOICE_SMS_RECORD) {
			VoiceSMSRecord voiceSMSRecord = (VoiceSMSRecord) intent.getExtras().getSerializable(EXTRA_KEY_VOICE_SMS_RECORD);
			locateOnceForVoiceSMSRecord(voiceSMSRecord, startId);
		} else if (cmd == LOCATION_UPDATE_CMD_TRACK) {
			final long duration = intent.getLongExtra(EXTRA_KEY_DURATION, 0);
			if (duration != 0) {
				TimerTask trackingTimer = new TimerTask() {
					public void run() {
						stopTrackingServiceById(startId, "stop location service from the tracking timer");
						Log.i(getString(R.string.logging_tag),
								"stop location service id [" + this.hashCode()
								                           + "] now duration [" + duration + "] interval [" + interval + "]");
					}
				};
				boolean started = startTracking(interval, startId, trackingTimer);
				if (started) {
					new Timer().schedule(trackingTimer, duration);
					Log.i(getString(R.string.logging_tag),
							"start location service id [" + trackingTimer.hashCode()
							+ "] duration [" + duration + "] interval [" + interval + "] id:[" + this + "]");
				}
			}
		} else if (cmd == LOCATION_UPDATE_CMD_STOPSERVICE) {
			stopTrackingServiceByTime(System.currentTimeMillis(), "stop location service from outside");
		}
		return Service.START_REDELIVER_INTENT;
//		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	/**
	 * start the location update service to upload location information
	 * for the length of <code>duration</code> in minutes.
	 * @param duration
	 */
	public static void startTrackingService(Context context, long duration, long interval) {
		_context = context;
		Intent intent = new Intent(context, OldLocationUploadService.class);
		intent.putExtra(EXTRA_KEY_DURATION, duration);
		intent.putExtra(EXTRA_KEY_INTERVAL, interval);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_TRACK);
		context.startService(intent);
	}

	/**
	 * start the location update service to upload the current location
	 * information only once
	 */
	public static void locateOnceService(Context context) {
		_context = context;
		Intent intent = new Intent(context, OldLocationUploadService.class);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_LOCATE);
		context.startService(intent);
	}

	/**
	 * stop the tracking service 
	 */
	public static void stopTrackingService(Context context) {
		Intent intent = new Intent(context, OldLocationUploadService.class);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_STOPSERVICE);
		context.startService(intent);
	}

	public static void sendVoiceSMSRecordWithLocation(Context context, VoiceSMSRecord voiceSMSRecord) {
		_context = context;
		Intent intent = new Intent(context, OldLocationUploadService.class);
		intent.putExtra(LOCATION_UPDATE_CMD, LOCATION_UPDATE_CMD_VOICE_SMS_RECORD);
		intent.putExtra(EXTRA_KEY_VOICE_SMS_RECORD, voiceSMSRecord);
		context.startService(intent);
	}

	private void stopTrackingServiceById(int startId, String stopMsg) {
		if (_locManager != null) {
			Object[] listeners = (Object[]) _serviceUtilityPool.get(new Integer(startId));
			if (listeners != null && listeners.length == 5) {
				_locManager.removeUpdates((LocationListener) listeners[0]);
				_locManager.removeNmeaListener((GpsStatus.NmeaListener) listeners[1]);
			}
		}
		flushRecordBuffer();
		stopSelf();
		_serviceUtilityPool.remove(new Integer(startId));
		Log.i(getString(R.string.logging_tag), stopMsg);
	}

	private void stopTrackingServiceByTime(long stopTime, String stopMsg) {
		if (_locManager != null) {
			Enumeration<Object[]> enums = _serviceUtilityPool.elements();
			while (enums.hasMoreElements()) {
				Object[] serviceUtilities = (Object[]) enums.nextElement();
				if (serviceUtilities != null && serviceUtilities.length == 5) {
					long startTime = ((Long) serviceUtilities[2]).longValue();
					if (startTime < stopTime) {
						_locManager.removeUpdates((LocationListener) serviceUtilities[0]);
						_locManager.removeNmeaListener((GpsStatus.NmeaListener) serviceUtilities[1]);
						((TimerTask) serviceUtilities[3]).cancel();
						flushRecordBuffer();
						_serviceUtilityPool.remove(((Integer) serviceUtilities[4]));
						//stopSelf(); // no use
					}
				}
			}
		}
		Log.i(getString(R.string.logging_tag), stopMsg);
	}

	private void locateOnceForVoiceSMSRecord(final VoiceSMSRecord voiceSMSRecord, final int startId) {
		final Object callLocationTimerTaskStore[] = new Object[1];
		String provider = initLocManager();
		if (provider == null)
		{
			FTPTransferManager.sendRecordToServer(voiceSMSRecord.toString(), _context);
			return;
		}

		// set up the NMEA Listener
		final GpsStatus.NmeaListener nmeaListener = new GpsStatus.NmeaListener() {
			public void onNmeaReceived(long timestamp, String nmea) {
				if (nmea != null && nmea.length() > 0) {
					// Get rid of checksum
					nmea = nmea.replaceAll("\\*..$", "");

					String[] nmeaSplit = nmea.split(",");

					if (nmeaSplit != null && nmeaSplit.length == 18) {
						if (nmeaSplit[0] != null && nmeaSplit[0].equals(NMEA_DOP_SENTENCE)){
							try {
								setPDilution(Float.parseFloat(nmeaSplit[15]), startId);
							} catch (NumberFormatException nfe) {
								setPDilution((float) 0.1, startId);
							}

							try {
								setHDilution(Float.parseFloat(nmeaSplit[16]), startId);
							} catch (NumberFormatException nfe) {
								setHDilution((float) 0.1, startId);
							}

							try {
								setVDilution(Float.parseFloat(nmeaSplit[17]), startId);
							} catch (NumberFormatException nfe) {
								setVDilution((float) 0.1, startId);
							}
						}
					}

				}
			}
		};

		// setup the Location Listener
		final LocationListener locListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				Log.i(getString(R.string.logging_tag), "locateOnce for VoiceSMSRecord got location [" + location + "]");
				VoiceSMSRecord vsRecord = voiceSMSRecord;
				if (location != null) {
					if (callLocationTimerTaskStore[0] != null)
						((TimerTask) callLocationTimerTaskStore[0]).cancel();
					vsRecord._latitude=(location.getLatitude());
					vsRecord._longitude=(location.getLongitude());
					FTPTransferManager.sendRecordToServer(vsRecord.toString(), _context);
					if (_locManager != null) {
						_locManager.removeUpdates(this);
						_locManager.removeNmeaListener(nmeaListener);
					}
					_serviceUtilityPool.remove(new Integer(startId));
					stopSelf();
					Log.i(getString(R.string.logging_tag), "speed =" + location.getSpeed());
					Log.i(getString(R.string.logging_tag), "stop location from locateOnceForVoiceSMSRecord() id:[" + this + "]");
				} else {
					Log.i(getString(R.string.logging_tag), "locateOnce service: returned location is null this time");
					FTPTransferManager.sendRecordToServer(vsRecord.toString(), _context);
				}
			}

			public void onProviderDisabled(String arg0) {
		        Log.e("GPS", "provider disabled " + arg0);
		    }
		    public void onProviderEnabled(String arg0) {
		        Log.e("GPS", "provider enabled " + arg0);
		    }
		    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
		        Log.e("GPS", "status changed to " + arg0 + " [" + arg1 + "]");
		    }
		    };

		// set up the CallLocationTimeout timer
		TimerTask callLocationTimer = null;
		int timeout = Config.getConfig().getCallLocationTimeout();
		if (timeout > 0) {
			callLocationTimer = new TimerTask() {
				public void run() {
					FTPTransferManager.sendRecordToServer(voiceSMSRecord.toString(), _context);
					if (_locManager != null) {
						_locManager.removeUpdates(locListener);
						_locManager.removeNmeaListener(nmeaListener);
					}
					_serviceUtilityPool.remove(new Integer(startId));
					stopSelf();
					Log.i(getString(R.string.logging_tag), "stop location from locateOnceForVoiceSMSRecord()" +
							" CallLocationTimer() id:[" + this + "]");
				}
			};
		}
		callLocationTimerTaskStore[0] = callLocationTimer;

		_locManager.addNmeaListener(nmeaListener);
		_locManager.requestLocationUpdates(provider, Config.GPS_INTERVAL_FULL_TIME_TRACKING, LOCATE_ONCE_GPS_MIN_DISTANCE, locListener);
		if (callLocationTimer != null)
			new Timer().schedule(callLocationTimer, timeout * 1000);
		Log.i(getString(R.string.logging_tag), "start locateOnce for VoiceSMSRecord id:[" + this + "]");
	}

	private void locateOnce(final int startId) {
		String provider = initLocManager();
		if (provider == null)
			return;
		final Context context = this;

		final GpsStatus.NmeaListener nmeaListener = new GpsStatus.NmeaListener() {
			public void onNmeaReceived(long timestamp, String nmea) {
				if (nmea != null && nmea.length() > 0) {
					// Get rid of checksum
					nmea = nmea.replaceAll("\\*..$", "");

					String[] nmeaSplit = nmea.split(",");

					if (nmeaSplit != null && nmeaSplit.length == 18) {
						if (nmeaSplit[0] != null && nmeaSplit[0].equals(NMEA_DOP_SENTENCE)){
							try {
								setPDilution(Float.parseFloat(nmeaSplit[15]), startId);
							} catch (NumberFormatException nfe) {
								setPDilution((float) 0.1, startId);
							}

							try {
								setHDilution(Float.parseFloat(nmeaSplit[16]), startId);
							} catch (NumberFormatException nfe) {
								setHDilution((float) 0.1, startId);
							}

							try {
								setVDilution(Float.parseFloat(nmeaSplit[17]), startId);
							} catch (NumberFormatException nfe) {
								setVDilution((float) 0.1, startId);
							}
						}
					}

				}
			}
		};

		LocationListener locListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				Log.i(getString(R.string.logging_tag), "locateOnce got location [" + location + "]");
				if (location != null) {
					float bearing = 0;
					if (null != mLastLocation)
					{
						bearing = mLastLocation.bearingTo(location);
					}
					Config.takeSnapShot(context);
//					float speedMilesPerHour = Config.getConfig().getCurrentSpeed();

					mLastLocation = location;
					location.setBearing(bearing);
//					location.setSpeed(speedMilesPerHour);
					sendLocationRecord(location, false, startId);
					displayRecord(location, context);
					if (_locManager != null) {
						_locManager.removeUpdates(this);
						_locManager.removeNmeaListener(nmeaListener);
					}
					stopSelf();
					Log.i(getString(R.string.logging_tag), "speed =" + location.getSpeed());
					Log.i(getString(R.string.logging_tag), "stop location from locateOnce() id:[" + this + "]");
				} else {
					Log.i(getString(R.string.logging_tag), "locateonce service: returned location is null this time");
				}
			}

			 public void onProviderDisabled(String arg0) {
			        Log.e("GPS", "provider disabled " + arg0);
			    }
			    public void onProviderEnabled(String arg0) {
			        Log.e("GPS", "provider enabled " + arg0);
			    }
			    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			        Log.e("GPS", "status changed to " + arg0 + " [" + arg1 + "]");
			    }
		};

		_locManager.addNmeaListener(nmeaListener);

		_locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Config.GPS_INTERVAL_FULL_TIME_TRACKING,  Config.GPS_INTERVAL_FULL_TIME_TRACKING, locListener);
		_locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, Config.GPS_INTERVAL_FULL_TIME_TRACKING, Config.GPS_INTERVAL_FULL_TIME_TRACKING, locListener);
		
		Log.i(getString(R.string.logging_tag), "start locateOnce id:[" + this + "]");
	}

	private boolean startTracking(final long interval, final int startId, TimerTask trackingTimer) {
		String provider = initLocManager();
		if (provider == null)
			return false;
		LocationListener locListener = new LocationListener() {
			public void onLocationChanged(Location location) {
				updateWithNewLocation(location, interval, startId);
				Log.i(getString(R.string.logging_tag), "speed =" + location.getSpeed());
			}

			public void onProviderDisabled(String provider){
//				updateWithNewLocation(null, interval, startId);
			}

			public void onProviderEnabled(String provider) 
			{
//				updateWithNewLocation(null, interval, startId);
//				if (provider.equals("gps"))
//					_locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval,  DEFAULT_GPS_MIN_DISTANCE, this);
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {}
		};

		GpsStatus.NmeaListener nmeaListener = new GpsStatus.NmeaListener() {
			public void onNmeaReceived(long timestamp, String nmea) {
				if (nmea != null && nmea.length() > 0) {
					// Get rid of checksum
					nmea = nmea.replaceAll("\\*..$", "");

					String[] nmeaSplit = nmea.split(",");

					if (nmeaSplit != null && nmeaSplit.length == 18) {
						if (nmeaSplit[0] != null && nmeaSplit[0].equals(NMEA_DOP_SENTENCE)){
							try {
								setPDilution(Float.parseFloat(nmeaSplit[15]), startId);
							} catch (NumberFormatException nfe) {
								setPDilution((float) 0.1, startId);
							}

							try {
								setHDilution(Float.parseFloat(nmeaSplit[16]), startId);
							} catch (NumberFormatException nfe) {
								setHDilution((float) 0.1, startId);
							}

							try {
								setVDilution(Float.parseFloat(nmeaSplit[17]), startId);
							} catch (NumberFormatException nfe) {
								setVDilution((float) 0.1, startId);
							}
						}
					}

				}
			}
		};

		_locManager.addNmeaListener(nmeaListener);
		_locManager.requestLocationUpdates(provider, interval,  DEFAULT_GPS_MIN_DISTANCE, locListener);
		long startTime = System.currentTimeMillis();
		_serviceUtilityPool.put(new Integer(startId),
				new Object[]{locListener,            //0
			nmeaListener,           //1
			new Long(startTime),    //2
			trackingTimer,          //3
			new Integer(startId)}); //4
		Log.i(getString(R.string.logging_tag), "start tracking interval [" + interval + "] id:[" + this + "]");
		return true;
	}

	private String initLocManager() {
		String context = Context.LOCATION_SERVICE;
		_locManager = (LocationManager) getSystemService(context);

		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(true);
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

	private void updateWithNewLocation(Location location, long interval, int startId) {
		if (location != null) {
			long curTimeMillis = System.currentTimeMillis();
			if (curTimeMillis < _lastGPSRecordTime + interval) {
				return;
			} else {
				_lastGPSRecordTime = curTimeMillis;
			}
			float bearing = 0;
			if (null != mLastLocation)
			{
				bearing = mLastLocation.bearingTo(location);
			}
			Config.takeSnapShot(this);
//			float speedMilesPerHour = Config.getConfig().getCurrentSpeed();

			mLastLocation = location;
			location.setBearing(bearing);
//			location.setSpeed(speedMilesPerHour);
			sendLocationRecord(location, false, startId);//true); do not buffer for now
			displayRecord(location, this);
		} else {
			Log.i(getString(R.string.logging_tag), "start tracking service: returned location is null this time");
		}
	}

	private void sendLocationRecord(Location location, boolean toBuffer, int startId) {
		float pDilution = getPDilution(startId);
		float hDilution = getHDilution(startId);
		float vDilution = getVDilution(startId);
		if (isLocationValid(hDilution, vDilution)) {
			LocationRecord record = new LocationRecord(this, location, pDilution, hDilution, vDilution);
			if (toBuffer) {
				if (_record_buffer_count < LOCATION_RECORD_BUFFER_SIZE) {
					_record_buffer += "\n" + record.toString();
					_record_buffer_count++;
				}
				if (_record_buffer_count == LOCATION_RECORD_BUFFER_SIZE) {
					FTPTransferManager.sendRecordToServer(_record_buffer.toString(), this);    
					_record_buffer_count = 0;
					_record_buffer = EMPTY_BUFFER;
				}
			} else {
				FTPTransferManager.sendRecordToServer(record.toString(), this);
			}
		}
	}

	private boolean isLocationValid(float hDilution, float vDilution) {
		Config.takeSnapShot(_context);
		Log.i(getString(R.string.logging_tag), "checking dilution h[" + hDilution + "] v[" + vDilution + "]");
		Log.i(getString(R.string.logging_tag), "checking dilution " +
				"maxH[" + Config.getConfig().getMaxHDilution() + "] " +
				"maxV[" + Config.getConfig().getMaxVDilution() + "]");
		return ((hDilution <= Config.getConfig().getMaxHDilution())
				&& (vDilution <= Config.getConfig().getMaxVDilution()));
	}

	private void flushRecordBuffer() {
		if (_record_buffer_count > 0 && !_record_buffer.equals(EMPTY_BUFFER)) {
			FTPTransferManager.sendRecordToServer(_record_buffer.toString(), this);   
			_record_buffer_count = 0;
			_record_buffer = EMPTY_BUFFER;
		}
	}

	private static void displayRecord(Location location, Context context) {
		/** comment out the displayRecord
        if (location != null) {
            // Screen Logging Purpose (to delete or leave?)
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            Geocoder gc = new Geocoder(context, Locale.getDefault());
            try {
                List<Address> addresses = gc.getFromLocation(latitude, longitude, 1);
                StringBuilder sb = new StringBuilder();
                sb.append("lat[" + latitude + "] log[" + longitude + "]");
                if (addresses.size() > 0) {
                    Address address = addresses.get(0);

                    for (int i = 0; i < address.getMaxAddressLineIndex(); i++)
                        sb.append(address.getAddressLine(i)).append("\n");

                    sb.append(address.getLocality()).append("\n");
                    sb.append(address.getPostalCode()).append("\n");
                    sb.append(address.getCountryName());
                }
                Toast toast = Toast.makeText(context.getApplicationContext(), sb, Toast.LENGTH_SHORT);
                toast.show();
            } catch (IOException e) {
                Log.e(context.getString(R.string.logging_tag),
                        "got exp while updating new location exp ["
                        + e.getClass().getName() + "] msg [" + e.getMessage() + "]");
            }
        }
		 **/
	}

	private static void displayGPSNotEnabledWarning(Context context) {
		String msg = "Your GPS is disabled, Please enable it!";
		Toast toast = Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	private synchronized void setDilution(float value, int index, int startId) {
		Integer key = new Integer(startId);
		Object[] dilutions = null;
		if (_serviceDilutionPool.contains(key)) {
			dilutions = (Object[]) _serviceDilutionPool.get(key);
		} else {
			dilutions = new Object[3];
		}
		dilutions[index] = new Float(value);
		_serviceDilutionPool.put(key, dilutions);
	}

	private synchronized float getDilution(int index, int startId) {
		Integer key = new Integer(startId);
		if (_serviceDilutionPool.contains(key)) {
			Object[] dilutions = (Object[]) _serviceDilutionPool.get(key);
			if (dilutions != null && dilutions.length == 3)
				return ((Float) dilutions[index]).floatValue();
		}
		return 0.55F;
	}

	/**
	 * @param PDilution the _PDilution to set
	 */
	public synchronized void setPDilution(float PDilution, int startId) {
		setDilution(PDilution, 0, startId);
	}

	/**
	 * @return the _PDilution
	 */
	public synchronized float getPDilution(int startId) {
		return getDilution(0, startId);
	}

	/**
	 * @param HDilution the _HDilution to set
	 */
	public synchronized void setHDilution(float HDilution, int startId) {
		setDilution(HDilution, 1, startId);
	}

	/**
	 * @return the _HDilution
	 */
	public synchronized float getHDilution(int startId) {
		return getDilution(1, startId);
	}

	/**
	 * @param VDilution the _VDilution to set
	 */
	public synchronized void setVDilution(float VDilution, int startId) {
		setDilution(VDilution, 2, startId);
	}

	/**
	 * @return the _VDilution
	 */
	public synchronized float getVDilution(int startId) {
		return getDilution(2, startId);
	}


}
