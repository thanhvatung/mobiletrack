package com.mobiletrack.record;

import android.content.Context;
import android.location.Location;

public class ProviderRecord extends NetworkRecord {

	
	private static final long serialVersionUID = 8701267478314688376L;

	public ProviderRecord(Context context, Location l) {
		super(context);
		_latitude	= l.getLatitude();
		_longitude	= l.getLongitude();
	}
	
	public ProviderRecord (Context context)
	{
		super (context);
	}
}
