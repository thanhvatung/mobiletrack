package com.mobiletrack.record;

import java.util.Date;

import android.content.Context;

import com.mobiletrack.service.LocationTrackService;

public class OverrideRecord extends NetworkRecord {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7327448678182811119L;
	private static final float CONVERSION_FACTOR = (float) 2.23693629;
	private float speed = 0;
	public OverrideRecord(Context context) {
		super(context);
		speed = LocationTrackService.current.getSpeed() *CONVERSION_FACTOR;
	}
	public String toString()
	{
		return "ORIDE" + VERTICAL_BAR + _localNumber + VERTICAL_BAR
		+ _dateFormat.format(new Date()) + VERTICAL_BAR + speed
		+ VERTICAL_BAR + _latitude + VERTICAL_BAR + _longitude
		+ VERTICAL_BAR + _mcc + VERTICAL_BAR + _mnc
		+ VERTICAL_BAR + _lac + VERTICAL_BAR;
	}
}
