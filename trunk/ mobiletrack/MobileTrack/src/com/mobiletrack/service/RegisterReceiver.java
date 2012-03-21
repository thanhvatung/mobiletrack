package com.mobiletrack.service;

import com.mobiletrack.config.Config;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class RegisterReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
//			http://www.xtiservice.com/DevService.asmx/register?key=<KEY>&phn=<PHONENUMBER>&hwid=<HARDWAREID>
		} catch (Exception e) {
			Toast.makeText(
					context,
					"There was an error somewhere, but we still received an alarm",
					Toast.LENGTH_SHORT).show();
			e.printStackTrace();

		}
	}

}
