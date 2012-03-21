package com.mobiletrack.map;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;

public class FamilyItemizedOverlay extends ItemizedOverlay {
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	private Context mContext;
	private int icon;
	private float CIRCLE_RADIUS = 0;
	private int radius;
	private Paint innerPaint, borderPaint;

	public FamilyItemizedOverlay(Drawable defaultMarker,Context context,int icon) {
		super(boundCenterBottom(defaultMarker));
		this.icon = icon;
		mContext = context;
	}
	
	public void addOverlay(OverlayItem overlay,float radius) {
//		CIRCLE_RADIUS = radius;
	    mOverlays.add(overlay);
	    populate();
	}
	
	public void clear(){
		mOverlays.clear();
	}
	
	@Override
	protected OverlayItem createItem(int i) {
		// TODO Auto-generated method stub
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return mOverlays.size();
	}
	
	@Override
	protected boolean onTap(int index) {
	  OverlayItem item = mOverlays.get(index);
	  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
	  dialog.setTitle(item.getTitle());
	  dialog.setMessage(item.getSnippet());
	  dialog.show();
	  return true;
	}
	
	@Override
	public boolean draw(Canvas canvas, MapView mapView, boolean shadow,long when) {
	    Point p = new Point();
	    for(OverlayItem item : mOverlays) {
//	    	Log.i("FamilyLocation",String.valueOf(CIRCLE_RADIUS));
	    	radius = metersToRadius(CIRCLE_RADIUS, mapView, item.getPoint().getLatitudeE6());
	        drawLocation(canvas, mapView, mapView.getProjection().toPixels(item.getPoint(), p));
	    }
	    return true;
	}
	
	public static int metersToRadius(float meters, MapView map, double latitude) {
	    return (int) -(map.getProjection().metersToEquatorPixels(meters) * (1/ Math.cos(Math.toRadians(latitude))));         
	}
		
	protected void drawLocation(Canvas canvas, MapView mapView,Point curScreenCoords) {
				
	    // Read the image
	    Bitmap markerImage = BitmapFactory.decodeResource(mContext.getResources(), icon);

	    // Draw the icon, centered around the given coordinates
	    int centerX = curScreenCoords.x - markerImage.getWidth() / 4;
	    //icon size is 32*39
	    int centerY = curScreenCoords.y - (markerImage.getHeight()+7) / 2;
	    canvas.drawBitmap(markerImage,
	        centerX,
	        centerY, null);			   
//	    curScreenCoords = toScreenPoint(curScreenCoords);    
	    // Draw inner info window
	    canvas.drawCircle((float) curScreenCoords.x, (float) curScreenCoords.y, radius, getInnerPaint());
	    // if needed, draw a border for info window
	    canvas.drawCircle(curScreenCoords.x, curScreenCoords.y, radius, getBorderPaint());
	    markerImage.recycle();
	}

	public Paint getInnerPaint() {
	    if (innerPaint == null) {
	        innerPaint = new Paint();
	        innerPaint.setColor(0x186666ff);
	        innerPaint.setStyle(Style.FILL);
	        innerPaint.setAntiAlias(true);
	    }
	    return innerPaint;
	}

	public Paint getBorderPaint() {
	    if (borderPaint == null) {
	        borderPaint = new Paint();
	        borderPaint.setColor(0xff6666ff);
	        borderPaint.setAntiAlias(true);
	        borderPaint.setStyle(Style.STROKE);
	        borderPaint.setStrokeWidth(2);
	    }
	    return borderPaint;
	}

}
