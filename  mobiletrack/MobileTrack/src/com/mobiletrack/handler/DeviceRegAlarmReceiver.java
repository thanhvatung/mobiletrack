/**
 * 
 */
package com.mobiletrack.handler;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.mobiletrack.config.Config;
import com.mobiletrack.service.FTPTransferManager;
import com.mobiletrack.service.LocationTrackService;

/**
 * @author Guang&Xuemei
 * 
 */
public class DeviceRegAlarmReceiver extends BroadcastReceiver {

	public static final String ACTION_MOBILETRACK_DEVICE_REG_ALARM = "com.mobiletrack.handler.ACTION_MOBILETRACK_DEVICE_REG_ALARM";
	public static final String DEVICE_REG_URL = "http://www.xtiservice.com/DevService.asmx/register?key="; // <KEY>&phn=<PHONENUMBER>&hwid=<HARDWAREID>"

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(ACTION_MOBILETRACK_DEVICE_REG_ALARM)) {
			// GPS alarm is fired when gps tracking is on so we star the
			// tracking service
			doDeviceReg(context, intent);
			sendLocationFakeRecord(context);
		}

	}
	
	private void sendLocationFakeRecord(Context context){
		String record = LocationTrackService.fakeLocationRecord;
		Log.i("FakeLocation","Send record:"+record);
		if (!record.equals("")){
			FTPTransferManager.sendRecordToServer(record, context);
			LocationTrackService.fakeLocationRecord = "";
		}
	}

	private void doDeviceReg(Context context, Intent intent) {
		ServiceState servState = new ServiceState();
		if (!servState.getRoaming()) {
			Config.takeSnapShot(context);
			Config config = Config.getConfig();
			int triesBeforeFail = 3;
			if (triesBeforeFail < 1) {
				triesBeforeFail = 1;
			}
			while (triesBeforeFail > 0) {
				try {

					String address = DEVICE_REG_URL;
					TelephonyManager telephonyManager = (TelephonyManager) context
							.getSystemService(Context.TELEPHONY_SERVICE);
					String deviceID = telephonyManager.getDeviceId();
					if (deviceID == null)
						deviceID = "";
					address = address + config.getKey() + "&phn="
							+ config.getPhoneNumber() + "&hwid=" + deviceID;
					URL url = new URL(address);

					URLConnection connection = url.openConnection();
					HttpURLConnection httpConnection = (HttpURLConnection) connection;
					int responseCode = httpConnection.getResponseCode();
					if (responseCode == HttpURLConnection.HTTP_OK) {
						return;
					} else {
						triesBeforeFail--;
					}
				} catch (Exception e) {
					triesBeforeFail--;
				}
			}
			// device registration failed:
			int duration = Toast.LENGTH_SHORT;
			Toast toast = Toast.makeText(context, "Device Registration Failed",
					duration);
			toast.show();
		}
	}

}
