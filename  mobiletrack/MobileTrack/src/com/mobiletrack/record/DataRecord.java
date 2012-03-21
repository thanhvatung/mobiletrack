package com.mobiletrack.record;

import java.util.Date;

import android.content.Context;

public class DataRecord extends NetworkRecord {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1314503630901953941L;
	public long sent;
	public long recieved;
	public String type;
	public DataRecord(Context context) {
		super(context);
		
	}

	public String toString()
	{
		return "DATA" + VERTICAL_BAR  + _localNumber + VERTICAL_BAR + _dateFormat.format(new Date()) +VERTICAL_BAR + type + VERTICAL_BAR + sent + VERTICAL_BAR + recieved;
	}
}
