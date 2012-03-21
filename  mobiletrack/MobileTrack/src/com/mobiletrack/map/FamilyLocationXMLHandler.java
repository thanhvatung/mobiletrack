package com.mobiletrack.map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;

public class FamilyLocationXMLHandler extends DefaultHandler{
	private FamilyItemizedOverlay overlay;
	private OverlayItem item;	
	private String locationDataTime;
	private String name;
	private String address;
	private float radius;
	private GeoPoint point;
	private double latitude;
	private double longitude;
	
	Boolean currentElement = false;
	String currentValue = null;
	
	public FamilyLocationXMLHandler(FamilyItemizedOverlay overlay){
		this.overlay = overlay;
	}
	
	
	public FamilyItemizedOverlay getFamilyLocations(){
		return overlay;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName,Attributes attributes) throws SAXException {

		currentElement = true;
	}

	/** Called when tag closing ( ex:- <name>AndroidPeople</name>
	* -- </name> )*/
	@Override
	public void endElement(String uri, String localName, String qName)throws SAXException {

		currentElement = false;
		if (localName.equalsIgnoreCase("FamilyLocations")){
			int lat = (int) (latitude * 1e6);
			int lng = (int) (longitude * 1e6);
			point = new GeoPoint(lat, lng);
			item = new OverlayItem(point, name, String.valueOf(radius)+"\n"+locationDataTime+"\n"+address); 
			overlay.addOverlay(item,radius);
		}
		/** set value */
		if (localName.equalsIgnoreCase("LocationDateTime"))
			locationDataTime = currentValue;
		else if (localName.equalsIgnoreCase("Name"))
			name = currentValue;
		else if (localName.equalsIgnoreCase("Latitude"))
			latitude = Double.valueOf(currentValue);
		else if (localName.equalsIgnoreCase("Longitude"))
			longitude = Double.valueOf(currentValue);
		else if (localName.equalsIgnoreCase("Address"))
			address = currentValue;
		else if (localName.equalsIgnoreCase("Radius"))
			radius = Float.valueOf(currentValue);		
	}

	/** Called to get tag characters ( ex:- <name>AndroidPeople</name>
	* -- to get AndroidPeople Character ) */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		
		if (currentElement) {
		currentValue = new String(ch, start, length);
		currentElement = false;
		}
	}
}
