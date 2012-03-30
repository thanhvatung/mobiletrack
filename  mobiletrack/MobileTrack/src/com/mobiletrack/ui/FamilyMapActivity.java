package com.mobiletrack.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.map.FamilyItemizedOverlay;
import com.mobiletrack.map.FamilyLocationXMLHandler;
import com.mobiletrack.util.ServiceStarter;

public class FamilyMapActivity extends MapActivity {

	private MapController mapController;
	private MapView mapView;
	private FamilyItemizedOverlay itemizedoverlay;
    private MyLocationOverlay mMyLocationOverlay;
    private ArrayList<Overlay> overlayItems;
    private Timer timer;

    //XML
    private SAXParserFactory spf;
    private SAXParser sp;
    private XMLReader xr;
    private URL sourceUrl;
    private FamilyLocationXMLHandler myXMLHandler;
    
    private Drawable drawable;

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		
		Config.takeSnapShot(this);
		Config config = Config.getConfig();
		if (config.isAppTerminated()) {
			ServiceStarter.stop(this.getApplicationContext());
			finish();
			return;
		}
		else if (config.getReceiverServer().equals("www.google.com"))
		{
			//getCodes();
			Log.i("FamilyTrack","Registration problem");
			finish();
			return;
		}
		
		Log.i("GUI","Come Here");
		setContentView(R.layout.map); // bind the layout to the activity
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapController = mapView.getController();
		mapController.setZoom(16); // Zoon 1 is world view
		
		//Add overlay of the user and family memebers
		//Add user's location

        mMyLocationOverlay = new MyLocationOverlay(this, mapView);
        mMyLocationOverlay.runOnFirstFix(new Runnable() { public void run() {
        	GeoPoint myPoint = mMyLocationOverlay.getMyLocation();
            mapView.getController().animateTo(myPoint);
            mapView.getController().setCenter(myPoint);
        }});	
        
        //Add Family member's location
        drawable = this.getResources().getDrawable(R.drawable.red_pin);
        
		try {   		
			spf = SAXParserFactory.newInstance();
	    	sp = spf.newSAXParser();
	    	xr = sp.getXMLReader();
	    	String phoneNumber = Config.getConfig().getPhoneNumber();
			sourceUrl = new URL(
					"https://www.gofamilytrack.com//FamilyTrackService/FTrackService.asmx/FamilyLocations?phone="
					+phoneNumber);
			
			/** Create handler to handle XML Tags ( extends DefaultHandler ) */
	
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//Address
//		geocoder = new Geocoder(this, Locale.ENGLISH);		
//		String provider = initLocManager();
//		if (provider == null)
//			return;
//		Log.i("FamilyLocation",provider);
//		locationManager.requestLocationUpdates(provider, 0,
//				0, new GeoUpdateHandler());	
	}
	
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Config.getConfig().isAppTerminated()) {
			return false;
		} else {
			// Inflate menu from XML resource
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.refresh_menu_items, menu);
			// Only add extra menu items for a saved note
			return super.onCreateOptionsMenu(menu);
		}
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.menu_refresh) {
			refreshMap();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void refreshMap(){
		timer.cancel();
	    startFamilyLocationUpdate();
	}
	
	
	private void startFamilyLocationUpdate(){
		
		timer = new Timer();
		TimerTask task = new TimerTask() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				Log.i("FamilyLocation","Start timer ");
				displayLocations();
			}
		};
		timer.scheduleAtFixedRate(task, 0, 1*20*1000);
	}
	
	@Override
	public void onBackPressed() {
	    // do something on back.
		finish();
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        mMyLocationOverlay.enableMyLocation();
        startFamilyLocationUpdate();
    }

    @Override
    protected void onStop() {
        mMyLocationOverlay.disableMyLocation();
        timer.cancel();
        super.onStop();
    }
    
    protected void displayLocations(){
    	/** Send URL to parse XML Tags */
		if(!mapView.getOverlays().isEmpty()){
			Log.i("FamilyLocation","Not empty");			
			mapView.getOverlays().clear();
//			mapView.invalidate();
			mapView.postInvalidate();
		}		
    	try {
    		Log.i("FamilyLocation","come here ---- 1");
    		overlayItems = new ArrayList<Overlay>();
            overlayItems.add(mMyLocationOverlay);
    		itemizedoverlay = new FamilyItemizedOverlay(drawable,this,R.drawable.red_pin);
    		myXMLHandler = new FamilyLocationXMLHandler(itemizedoverlay);
			xr.setContentHandler(myXMLHandler);
			xr.parse(new InputSource(sourceUrl.openStream()));
			if(myXMLHandler.getFamilyLocations().size() != 0)
				overlayItems.add(myXMLHandler.getFamilyLocations());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mapView.getOverlays().addAll(overlayItems);
    }
    
//	private String initLocManager() {
//		locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
//		Criteria criteria = new Criteria();
//		criteria.setAccuracy(Criteria.ACCURACY_FINE);
//		criteria.setAltitudeRequired(false);
//		criteria.setBearingRequired(false);
//		criteria.setSpeedRequired(true);
//		criteria.setCostAllowed(true);
//		criteria.setPowerRequirement(Criteria.POWER_LOW);
//		String provider = locationManager.getBestProvider(criteria, true);
//		Log.i("FamilyLocation","----1");
//		if (provider == null || provider.equals("")) {
////			displayGPSNotEnabledWarning(this);
//			return null;
//		}
//		return provider;
//	}

//	public class GeoUpdateHandler implements LocationListener {
//
//		public void onLocationChanged(Location location) {
//			
//			if(!mapView.getOverlays().isEmpty()){
//				Log.i("FamilyLocation","Not empty");
//				mapView.getOverlays().clear();
//				itemizedoverlay.clear();
//				overlayItems.remove(1);
//				mapView.invalidate();
//			}
//			int lat = (int) (location.getLatitude() * 1e6);
//			int lng = (int) (location.getLongitude() * 1e6);
//			GeoPoint point = new GeoPoint(lat, lng);
//			Log.i("FamilyLocation",String.valueOf(location.getAccuracy()));
//			
//			try {
//				addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			String myAddress = null;
//			if(addresses != null){
//				 Address returnedAddress = addresses.get(0);
//				   StringBuilder strReturnedAddress = new StringBuilder("Address:\n");
//				   for(int i=0; i<returnedAddress.getMaxAddressLineIndex(); i++) {
//				    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
//				   }
//				    myAddress = strReturnedAddress.toString();
//				  }
//			else{
//				    myAddress ="Address can not be found";
//			}
//			OverlayItem overlayitem = new OverlayItem(point, "Current position", myAddress);
//			mapController.animateTo(point);
////			mapController.setCenter(point);
//			itemizedoverlay.addOverlay(overlayitem);
//			overlayItems.add(itemizedoverlay);
////			mapView.getOverlays().add(itemizedoverlay);
//			mapView.getOverlays().addAll(overlayItems);
//		}
//
//		public void onProviderDisabled(String provider) {
//		}
//
//		public void onProviderEnabled(String provider) {
//		}
//
//		public void onStatusChanged(String provider, int status, Bundle extras) {
//		}
//	}
}
			
