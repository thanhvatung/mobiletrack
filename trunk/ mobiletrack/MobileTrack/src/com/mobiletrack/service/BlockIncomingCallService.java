package com.mobiletrack.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;

public class BlockIncomingCallService extends IntentService{

	public BlockIncomingCallService() {
		 super("BlockIncomingCallService");
		// TODO Auto-generated constructor stub
	}
	
	 protected void onHandleIntent(Intent intent) {
         Context context = getBaseContext();
         
//         BluetoothHeadset bh = new BluetoothHeadset(this, null);
//
//         // Check headset status right before picking up the call
//         if ( bh != null) {
//                 if (bh.getState() != BluetoothHeadset.STATE_CONNECTED) {
//                         bh.close();
//                         return;
//                 }
//                 bh.close();
//         }

         // Make sure the phone is still ringing
         TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
         if (tm.getCallState() != TelephonyManager.CALL_STATE_RINGING) {
                 return;
         }

        // blockPhoneHeadsethook(context);
         
         //turn on airplan mode to hang up the call
         toggleRinger(context);
         toggleAirplanMode();
         
         //turn it off
         toggleRinger(context);
         toggleAirplanMode();
         return;
 }
	 
     private void blockPhoneHeadsethook(Context context) {
         // Simulate a press of the headset button to pick up the call
         Intent buttonDown = new Intent(Intent.ACTION_MEDIA_BUTTON);             
         buttonDown.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK));
         context.sendOrderedBroadcast(buttonDown, "android.permission.CALL_PRIVILEGED");

         // froyo and beyond trigger on buttonUp instead of buttonDown
         Intent buttonUp = new Intent(Intent.ACTION_MEDIA_BUTTON);               
         buttonUp.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
         context.sendOrderedBroadcast(buttonUp, "android.permission.CALL_PRIVILEGED");
     }
     
	 private void toggleRinger(Context context){
		 	AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		 	if(audioManager.getRingerMode() == 0){
		 		//turn on ringer
		 		audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		 		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_ON);
		 	}
		 	else{
		 		audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		 		audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, AudioManager.VIBRATE_SETTING_OFF);
		 	}		
	}
     
     
 	private void toggleAirplanMode()
	  {
 		// read the airplane mode setting
 		boolean isEnabled = Settings.System.getInt(
 		      getContentResolver(), 
 		      Settings.System.AIRPLANE_MODE_ON, 0) == 1;
 		Log.i("Block Calls",String.valueOf(isEnabled));
 		// toggle airplane mode
 		Settings.System.putInt(
 		      getContentResolver(),
 		      Settings.System.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);

 		// Post an intent to reload
 		Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
 		intent.putExtra("state", !isEnabled);
 		sendBroadcast(intent);
 				
	  }	
}



