package com.mobiletrack.handler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.location.LocationManager;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.mobiletrack.config.Config;
import com.mobiletrack.record.ProviderRecord;
import com.mobiletrack.service.FTPTransferManager;



public class CellHandler {

	private static boolean gps = false;
	private static CellLocation location;
	private static ProviderRecord record;
	private static final String lastRecord = "lastRecord";
	protected static Context context;
	private static boolean started;

	private static final void writeValues(Context context) {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = context.openFileOutput(lastRecord,Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(location);
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

	private static final void readValues(Context context) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput(lastRecord);
			ois = new ObjectInputStream(fis);
			location = (CellLocation) ois.readObject();
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
	}

	private static final void sendRecord(final Context context, final CellLocation loc) {
		if (record == null)
		{
			record = new ProviderRecord (context);
		}
		new Thread (new Runnable()
		{
			public void run()
			{
				Config.takeSnapShot(context);
				Config c = Config.getConfig();
				LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
				gps = c.isGPSEnabled();
				ProviderRecord r;
				if (gps)
				{
					try
					{
						r = new ProviderRecord (context, locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
					}
					catch (Exception e)
					{
						r = new ProviderRecord (context);
					}
				}
				else
				{
					r = new ProviderRecord (context);
				}
				if (!(r.getOperator().equals(record.getOperator()) || r.getOperator().equals("") || r.getOperator().equals("Searching for Service") || r.getOperator().equals("Unknown")))
				{
					record = r;
					writeValues(context);
					FTPTransferManager.sendRecordToServer(record.toString(), context);
					location = loc;
				}
			}
		}).start();
	}

	public static synchronized void startMonitoring(Context context)
	{
		if (!started)
		{
			started = true;
			if (location == null)
				readValues(context);
			CellHandler.context = context;
			((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).listen(phoneStateListener, PhoneStateListener.LISTEN_CELL_LOCATION);
		}
	}
	public static synchronized void stopMonitoring (Context context)
	{
		if (started)
		{
			started = false;
			writeValues(context);
			((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
		}
	}

	private final static PhoneStateListener phoneStateListener = new PhoneStateListener() {  
		@Override 
		public void onCellLocationChanged(CellLocation loc)
		{
//			sendRecord (c);
		
			if (location == null)
			{
				sendRecord(context, loc);
//				location = loc;
			}
			else if (!location.equals(loc))
			{
				sendRecord (context, loc);
//				location = loc;
			}
			super.onCellLocationChanged (loc);  
		}
	};
}