package com.mobiletrack.test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import com.mobiletrack.config.Config;

import android.content.Context;
import android.content.ContextWrapper;
import android.location.Location;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class LocationLog {
	//for test only, record will be saved into a file
	private static File logFile;
	private static Toast toast;
	private static final String DATE_FORMAT_NOW = "HH:mm:ss";
	private static FileWriter fw = null;
	private static ArrayList<String> locationList = new ArrayList<String>(); 
	
	public static void init(){
		try {
//			if(locationList.size() == 0){
				Log.i("LocationLog","First Lunch");
				logFile = new File(Environment.getExternalStorageDirectory().getPath(), "log.txt");
				Log.i("LocationLog",logFile.getAbsolutePath());
				fw = new FileWriter(logFile);
			}
//			if (locationList.size()>0){
//				Log.i("LocationLog","Writing data to SDCard: "+locationList.size());
//				for(int i = 0 ; i<locationList.size();i++){
//					String temp = locationList.remove(i);
//					fw.write(temp);
//					Log.i("LocationLog","Writing data to SDCard: "+temp);					
//				}
//				fw.flush();
//				locationList.clear();				
//			}
//		} 
	    catch (IOException e) {
			Log.i("LocationLog","error" + e.getMessage());			
			
		}		
	}
	
	public static void writeLocationLog(Context context,String networkType ,Location location){
			
		String speed = String.valueOf(location.getSpeed());
		String accuracy = String.valueOf(location.getAccuracy());		
	    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	    String time = sdf.format(location.getTime());
		
//		toast = Toast.makeText(context,networkType+ "\n"+
//				"Time: "+time+"\n"+
//				"Speed: "+speed+"\n"+
//				"Accuracy: "+accuracy, Toast.LENGTH_LONG);
//		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
//		toast.show();
	    String interval = String.valueOf(Config.getConfig().getGPSInterval()/1000/60);
		String oneLocation = networkType+ "\t"+time+"\t"+speed+"\t"+accuracy+"\t"+interval+"\r\n";
//		locationList.add(oneLocation);
//		try{
//			fw.write(oneLocation);
//			fw.flush();
//		}
//		catch (IOException e) {
//			
//		}
		Log.i("LocationLog",oneLocation);
	}
	
	public static boolean canWriteToFlash() {
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	Log.i("LocationLog","USB Connected");
	        return true;
	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	        // Read only isn't good enough
	        return false;
	    } else {
	        return false;
	    }
	}	
}
