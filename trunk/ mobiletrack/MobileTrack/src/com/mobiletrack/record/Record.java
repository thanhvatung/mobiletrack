package com.mobiletrack.record;

import java.io.Serializable;
import java.text.SimpleDateFormat;

public abstract class Record implements Serializable {
	protected static final String VERTICAL_BAR = "|";
	protected static final SimpleDateFormat _dateFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	protected Record() {

	}

	/**
	 * @deprecated: depending on if carrier's setup, should not use this piece of
	 *              code protected static String getMyPhoneNumber(Context
	 *              context){ TelephonyManager telephoneMgr = (TelephonyManager)
	 *              context.getSystemService(Context.TELEPHONY_SERVICE); return
	 *              telephoneMgr.getLine1Number(); }
	 **/
}