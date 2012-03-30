package com.mobiletrack.handler;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Stack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.process.SMSBlockProcess;
import com.mobiletrack.record.VoiceRecord;
import com.mobiletrack.service.BlockIncomingCallService;
import com.mobiletrack.service.LocationUploadService;
import com.mobiletrack.ui.AccountCodeActivity;
import com.phonelib.net.CallApplication;


public class CallHandler extends BroadcastReceiver {
	public static final String EXTRA_KEY_VOICE_RECORD = "EXTRA_KEY_VOICE_RECORD";
	private static VoiceRecord _currentVoiceRecord = null;
	private static String mLastPhoneState = TelephonyManager.EXTRA_STATE_IDLE;
	public static Stack <VoiceRecord> callStack = new Stack <VoiceRecord>();

	@SuppressWarnings("unchecked")
	public static void loadLists(Context context) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput("VOICE_PIPE");
			ois = new ObjectInputStream(fis);
			callStack = (Stack  <VoiceRecord> ) ois.readObject();
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
		if (callStack == null)
		{
			callStack = new Stack  <VoiceRecord> ();
		}
	}

	public static void saveLists(Context context)
	{
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = context.openFileOutput("VOICE_PIPE", Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(callStack);
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
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// do not respond if the app has been terminated
		Config.takeSnapShot(context);
		if (Config.getConfig().isAppTerminated())
			return;

		if (intent.getAction().equals("android.intent.action.PHONE_STATE"))
			handleIncomingCall(context, intent);
		else if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
			handleOutgoingCall(context, intent);

		mLastPhoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
	}

	private void handleIncomingCall(Context context, Intent intent) {
		Config.takeSnapShot(context);
		Config config = Config.getConfig();
		String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
		Log.d(context.getString(R.string.logging_tag), "phone state changed [" + state + "]");
		
		if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
			Bundle bundle = intent.getExtras();
			String phoneNumber = bundle.getString("incoming_number");
			if (null == phoneNumber || phoneNumber.length() == 0){
				phoneNumber = context.getString(R.string.unknown_number);
			}
			if (config.isNumberToRestrict(phoneNumber)
					|| (!(config.isVIGenericAllowed() || (config.isVIAllowed() && SMSBlockProcess.OVERRIDE)) && !config.isNumberInAllowedNumberDisabled(phoneNumber)))
			{
				
				// block this incoming call
				if (config.isNumberToRestrict(phoneNumber)|| !config.isVIGenericAllowed()){	
					
					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
					     // only for android before 2.3
						CallApplication.end();
					}
					else{			
						Log.i("Block Calls","1");						
						context.startService(new Intent(context, BlockIncomingCallService.class));
					//	clearCallLog(context, phoneNumber);
					}
				}
				else
				{
					SMSBlockProcess.addNotification(context);
				}
			} else {
				// otherwise we will allow this incoming call
				// record this call if the user accepts
				_currentVoiceRecord = new VoiceRecord(context, VoiceRecord.DIRECTION_MT);
				_currentVoiceRecord._otherParty =(phoneNumber);
			//	BlockIncomingCallService.turnOnRinger(context);
			}
			
			
		} 
		else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
			// the call has been accepted (could be incoming or outgoing)
			if (_currentVoiceRecord != null) {
				_currentVoiceRecord.generateGeneralInfo(context);
				_currentVoiceRecord._eventStart=(System.currentTimeMillis());
				context.startService(new Intent(context, BlockIncomingCallService.class));
			}
		} else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
			// the call has been terminated (could be incoming or outgoing)
			if (mLastPhoneState.equals(TelephonyManager.EXTRA_STATE_RINGING))
				return;
			if (_currentVoiceRecord != null) {
				_currentVoiceRecord._eventEnd =(System.currentTimeMillis());
				sendRecordPerAccoundCode(context);

				//String record = _currentVoiceRecord.toString();
				//_currentVoiceRecord = null;
				//FTPTransferManager.sendRecordToServer(record, context);
			}
		}
		return;
	}
	

	private void sendRecordPerAccoundCode(Context context) {
		Config.takeSnapShot(context);
		Config config = Config.getConfig();
		//String record = _currentVoiceRecord.toString();
		String phoneNumber = _currentVoiceRecord._otherParty;
		//String phoneNumber = "5857341111"; // debugging
		if (config.isNumToIgnoreByAcctCode(phoneNumber))
		{
			LocationUploadService.sendVoiceSMSRecordWithLocation(context, _currentVoiceRecord);
		} else{
			// some device does not have call display option, so cannot check account code
			String accountCode = (phoneNumber == null)? null : config.isNumMatchAcctCode(phoneNumber);

			switch (config.getUseAccountCodes())
			{
			case Config.ACCOUNT_CODE_DO_NOT_USE:
				LocationUploadService.sendVoiceSMSRecordWithLocation(context, _currentVoiceRecord);
				break;
			case Config.ACCOUNT_CODE_ALWAYS_USE:
				startAccountActivity(context, _currentVoiceRecord);
				break;
			case Config.ACCOUNT_CODE_USE_ON_MATCH_ONLY:
				if (accountCode != null)
				{
					startAccountActivity(context, _currentVoiceRecord);
				}
				else
				{
					//FTPTransferManager.sendRecordToServer(record, context);
					LocationUploadService.sendVoiceSMSRecordWithLocation(context, _currentVoiceRecord);
					PromptUser(context, R.string.no_account_code, "");
				}
				break;
			case Config.ACCOUNT_CODE_AUTOMATIC:
				if (accountCode != null)
				{
					_currentVoiceRecord._accountCode = (accountCode);
					//FTPTransferManager.sendRecordToServer(_currentVoiceRecord.toString(), context);
					LocationUploadService.sendVoiceSMSRecordWithLocation(context, _currentVoiceRecord);
				}
				else
				{
					//FTPTransferManager.sendRecordToServer(record, context);
					LocationUploadService.sendVoiceSMSRecordWithLocation(context, _currentVoiceRecord);
					PromptUser(context, R.string.no_account_code, "");
				}
				break;
			}
		}
		_currentVoiceRecord = null;
	}

	private void startAccountActivity(Context context, VoiceRecord voiceRecord)
	{
		Intent intentAccountCode = new Intent(context, AccountCodeActivity.class);
		//		intentAccountCode.putExtra(EXTRA_KEY_VOICE_RECORD, voiceRecord);
		callStack.push(_currentVoiceRecord);
		saveLists(context);
		intentAccountCode.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intentAccountCode);
		//		AccountCodeActivity.setRecord (callStack.peek());
	}

	private void handleOutgoingCall(Context context, Intent intent) {
		String phoneNumber = getResultData();
		if (phoneNumber != null) {
			Config.takeSnapShot(context);
			Config config = Config.getConfig();
				if (config.isNumberToRestrict(phoneNumber)
						|| (!(config.isVOGenericAllowed() || (config.isVOAllowed() && SMSBlockProcess.OVERRIDE)) && !config.isNumberInAllowedNumberDisabled(phoneNumber))) {
					Log.i(context.getString(R.string.logging_tag),
							"need to display sth: blocking the outgoing number [" + phoneNumber + "]");
					setResultData(null);
					PromptUser(context, R.string.outgoing_call_end_warning, "number[" + phoneNumber + "]");
					return;
				} else if(config.isVOGenericAllowed()
						|| (!config.isVOGenericAllowed() && config.isNumberInAllowedNumberDisabled(phoneNumber))) {
					String numberToRedirect = config.getNumberToRedirect(phoneNumber);
					if ((numberToRedirect != null) && (numberToRedirect.length() > 0)) {
						Log.i(context.getString(R.string.logging_tag),
								"need to display sth: blocking the outgoing number [" + phoneNumber + "] redirect to [" + numberToRedirect + "]");
						setResultData(null);
						PromptUser(context, R.string.outgoing_call_transferred_warning, "");
						try {
							Uri uri = Uri.fromParts("tel", numberToRedirect, null);
							Intent newIntent = new Intent(Intent.ACTION_CALL, uri);
							newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(newIntent);
						} catch (Exception e) {
							PromptUser(context, R.string.outgoing_call_init_failed, "[" + numberToRedirect + "]");
						}
						_currentVoiceRecord = new VoiceRecord(context, VoiceRecord.DIRECTION_MO);
						_currentVoiceRecord._otherParty =(numberToRedirect);
						return;
					}
				}
			}
			// otherwise we will allow this outgoing call
			// record this call if the call gets accepted
			_currentVoiceRecord = new VoiceRecord(context, VoiceRecord.DIRECTION_MO);
			_currentVoiceRecord._otherParty=(phoneNumber);
		}
	

	private void PromptUser(Context context, int rid, String extraMsg) {
		String msg = context.getString(rid);
		if (extraMsg != null && extraMsg.length() != 0)
		{
			msg = msg  + " [" + extraMsg + "]";
		}
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();
	}

	/**
	 * Not Tested yet
	 * 
	 */
	
	 
	public void clearCallLog(Context context, String mobileNumber) {
		Cursor c = context.getContentResolver().query(
				CallLog.Calls.CONTENT_URI, null,
				Calls.NUMBER + "='" + mobileNumber + "'", null,
				Calls.DATE + " DESC");
		context.getContentResolver().cancelSync(Calls.CONTENT_URI);
		// startManagingCursor(c);
		String number = null;
		String date;
		int dateColumn = c.getColumnIndex(Calls.DATE);
		int numberColumn = c.getColumnIndex(Calls.NUMBER);
		if (c != null) {
			if (c.moveToFirst()) {
				// do {
				// Get the field values
				date = c.getString(dateColumn);
				number = c.getString(numberColumn);
				Log.i("Block Calls", number);
				Log.i("Block Calls", date);
				// alert("number", number);
				// } while (c.moveToNext());
			}
			int i = context.getContentResolver().delete(Calls.CONTENT_URI,
					Calls.NUMBER + "='" + number + "'", null);
			Log.i("Block Calls", Integer.toString(i));
			//telManager.listen(new StateListener(), LISTEN_NONE);
		}
	} 
}