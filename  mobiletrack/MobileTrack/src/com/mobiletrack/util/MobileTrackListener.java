package com.mobiletrack.util;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;

import com.mobiletrack.config.Config;
import com.mobiletrack.record.LocationRecord;
import com.mobiletrack.service.FTPTransferManager;
import com.mobiletrack.service.LocationTrackService;

public class MobileTrackListener implements LocationListener {

	LocationTrackService parent;
	int id;
	
	public MobileTrackListener(LocationTrackService locationTrackService, int i) {
		parent = locationTrackService;
		id = i;
		}

	
	@SuppressWarnings("static-access")
	public void onLocationChanged(Location location) {
		int timeout = Config.getConfig().getCallLocationTimeout();
		// if we're GPS...
		if (id == parent.getGPSLocationFinderID()){
			Log.i("LocationTrackService -- Debug info","LocationFinder ID: "+Integer.toString(id));
			parent.disableCell();
			parent.reportLocation(location, id);
			// Debugging purposes only
			//FTPTransferManager.sendRecordToServer(new LocationRecord(parent.getApplicationContext(), location, -1, -1, -1).toString(), parent.getApplicationContext());
		}
		if(id == parent.getCellLocationFinderID()){
			Log.i("LocationTrackService -- Debug info","LocationFinder ID: "+Integer.toString(id));			
//	        try{
//	        	 Thread.sleep(timeout*1000);	            
//	             parent.reportLocation (location, id);
//	             parent.disableGPS();
//	             Log.i("LocationTrackService -- Debug info","wait until GPS timeout");
//	         }
//	         catch (Throwable t)
//	             {
//	             throw new OutOfMemoryError("An Error has occured");
//	         }
			 parent.reportLocation (location, id);
			
		}
	}

	
	public void onProviderDisabled(String provider) {
		if(id == parent.getGPSLocationFinderID()){
			parent.disableGPS();
		}
		if(id == parent.getCellLocationFinderID()){
			parent.disableCell();
		}
		Log.i("LocationTrackService -- Debug info","LocationFinder Disabled ID: "+Integer.toString(id));
	}

	
	public void onProviderEnabled(String provider) {
		if (id == 1)
		{
			Log.i("LocationTrackService -- Debug info","providerEnabled: "+Integer.toString(id));
		}
		if(id == 2){
			Log.i("LocationTrackService -- Debug info","providerEnabled: "+Integer.toString(id));
		}
	}

	
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (id == 1)
		{
			Log.i("LocationTrackService -- Debug info","onStatusChanged: "+Integer.toString(id));
		}
		if(id == 2){
			Log.i("LocationTrackService -- Debug info","onStatusChanged: "+Integer.toString(id));
		}
	}

}
