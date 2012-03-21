package com.mobiletrack.record;

import android.content.Context;

public class SMSRecord extends VoiceSMSRecord {
	public SMSRecord(Context context, String direction) {
		super(context, VoiceSMSRecord.EVENT_TYPE_SMS, direction);
	}
}
