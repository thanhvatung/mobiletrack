package com.mobiletrack.record;

import java.util.Date;

import com.mobiletrack.config.Config;

import android.content.Context;
import android.location.Location;

public class LocationRecord extends NetworkRecord {
	private double _altitude;
	private float _speed;
	private float _heading;
	private int _satelliteCount;
	private int _satellitesInView;
	private float _positionDilution;
	private float _horizontalDilution;
	private float _verticalDilution;
	private Date theDate;
	public static final float CONVERSION_FACTOR = (float) 2.23693629;

	public LocationRecord(Context context, Location location,
			float positionDilution, float horizontalDilution,
			float verticalDilution) {
		super(context);
		theDate = new Date (location.getTime());
		// have to read from config. The getLine1Number() may not work since
		// some carriers don't fill phone number to their sim card :(
		_localNumber = Config.getConfig().getPhoneNumber(); // getMyPhoneNumber(context);
		_latitude = location.getLatitude();
		_longitude = location.getLongitude();
		_altitude = location.getAltitude();
		if (location.hasSpeed()) {
			_speed = location.getSpeed() * CONVERSION_FACTOR;
		} else {
			_speed = -1;
		}
		_positionDilution = location.getAccuracy();
		_horizontalDilution = location.getAccuracy();
		_verticalDilution = verticalDilution;
		/* Debugging purposes only!!!!!
		if (positionDilution == horizontalDilution && verticalDilution == horizontalDilution && horizontalDilution == -1)
		{
			_heading = 77777;
		}
		else */if (location.hasBearing()) {
			_heading = location.getBearing();// 314;
		} else {
			_heading = 99999;
		}
		_satelliteCount = -1;
		_satellitesInView = -1;
		// hardcode for now
	}

	public String toString() {
		return "GPS" + VERTICAL_BAR + _dateFormat.format(theDate)
				+ VERTICAL_BAR + _localNumber + VERTICAL_BAR + _latitude
				+ VERTICAL_BAR + _longitude + VERTICAL_BAR + _altitude
				+ VERTICAL_BAR + _speed + VERTICAL_BAR + _heading
				+ VERTICAL_BAR + _satelliteCount + VERTICAL_BAR+ _satellitesInView
				+VERTICAL_BAR + _positionDilution + VERTICAL_BAR + _horizontalDilution
				+ VERTICAL_BAR + _verticalDilution + VERTICAL_BAR + _cellId
				+ VERTICAL_BAR + _mcc + VERTICAL_BAR + _mnc + VERTICAL_BAR + _lac
				+ VERTICAL_BAR;
		}
	
		
}
