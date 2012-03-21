package com.mobiletrack.handler;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

import com.mobiletrack.config.Config;
import com.mobiletrack.service.WipeService;
import com.mobiletrack.util.ServiceStarter;


/**
 * @author Guang&Xuemei
 *
 */
public class OnBootReceiver extends BroadcastReceiver {


	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// set the GPSAlarm & DeviceRegAlarm
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
			Config.takeSnapShot(context); // get the new configurations

			// start outgoing SMS monitor
			ServiceStarter.go(context);
			SmsHandler.setGPSAlarm(context);
			String t = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getSimSerialNumber();
			String storedID = loadStringFromFile(context);
			if (t!= null && !(t.equals (storedID ))&& (Config.getConfig()).isWipeOnSimEnabled())
			{
				Intent i = new Intent(context, WipeService.class);
				context.startService(i);
			}
			if (t != null)
			{
				SmsHandler.writePhoneState(context);
			}
		}
	}
	public String loadStringFromFile (Context context) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		String toReturn = null;
		try {
			fis = context.openFileInput("IMSI");
			ois = new ObjectInputStream(fis);
			toReturn = (String) ois.readObject();
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
		return toReturn;
	}
}
