package com.mobiletrack.record;

import android.content.Context;

public class VoiceRecord extends VoiceSMSRecord {


	public VoiceRecord(Context context, String direction) {
		super(context, VoiceSMSRecord.EVENT_TYPE_VOICE, direction);
	}

	public String toString() {
		return super.toString();
		}

}