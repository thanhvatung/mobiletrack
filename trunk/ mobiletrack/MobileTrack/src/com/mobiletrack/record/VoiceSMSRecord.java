package com.mobiletrack.record;

import java.util.Date;

import com.mobiletrack.config.Config;


import android.content.Context;


public abstract class VoiceSMSRecord extends NetworkRecord {
	protected static final String EVENT_TYPE_VOICE = "VOICE";
	protected static final String EVENT_TYPE_SMS = "SMS";

	public static final String DIRECTION_MO = "MO";
	public static final String DIRECTION_MT = "MT";

	public String _eventType;
	public long _eventStart;
	public long _eventEnd;
	public String _direction;
	public String _localNumber;
	public String _otherParty;
	public double _latitude;
	public double _longitude;
	public String _accountCode = "";
	public String _matterCode = "";
	public String _accountComment = "";

	protected VoiceSMSRecord(Context context, String eventType, String direction) {
		super(context);
		_eventType = eventType;
		_direction = direction;

		_eventStart = 0;
		_eventEnd = 0;
		_otherParty = "";
		_latitude = 0;
		_longitude = 0;
		Config.takeSnapShot(context);
		_localNumber = (Config.getConfig().getPhoneNumber());
	}

/*	public void setEventStart(long eventStart) {
		_eventStart = eventStart;
	}

	public void setEventEnd(long eventEnd) {
		_eventEnd = eventEnd;
	}

	public void setLocalNumber(String localNumber) {
		_localNumber = localNumber;
	}

	public void setOtherParty(String otherParty) {
		_otherParty = otherParty;
	}

	public String getOtherParty() {
		return _otherParty;
	}

	public void setLatitude(double latitude) {
		_latitude = latitude;
	}

	public void setLongitude(double longitude) {
		_longitude = longitude;
	}

	public void setAccountCode(String accountCode) {
		_accountCode = accountCode;
	}

	public void setMatterCode(String matterCode) {
		_matterCode = matterCode;
	}

	public void setAccountComment(String accountComment) {
		_accountComment = accountComment;
	}*/
	public String toString() {
		return _eventType + VERTICAL_BAR + _network + VERTICAL_BAR
				+ _dateFormat.format(new Date(_eventStart)) + VERTICAL_BAR
				+ _dateFormat.format(new Date(_eventEnd)) + VERTICAL_BAR
				+ _direction + VERTICAL_BAR + _localNumber + VERTICAL_BAR
				+ _otherParty + VERTICAL_BAR + _latitude + VERTICAL_BAR
				+ _longitude + VERTICAL_BAR + _cellId + VERTICAL_BAR + _mcc
				+ VERTICAL_BAR + _mnc + VERTICAL_BAR + _lac + VERTICAL_BAR + _accountCode + VERTICAL_BAR
				+ _matterCode + VERTICAL_BAR + _accountComment + VERTICAL_BAR;
	}
}
