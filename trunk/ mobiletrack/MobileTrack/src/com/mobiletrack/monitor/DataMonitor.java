package com.mobiletrack.monitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import android.content.Context;

import com.mobiletrack.record.DataRecord;
import com.mobiletrack.service.FTPTransferManager;

public class DataMonitor{
	private static final String TRANSMIT = "TRANSMIT";
	private static final String RECEIVE = "RECEIVE";
	private static final String DATE = "NEXTUPLOAD";
	private static final String LASTRECEIVE = "LASTRECEIVE";
	private static final String LASTTRANSMIT = "LASTTRANSMIT";

	private static final String[] CELL_INTERFACES = { //
		"rmnet0", "pdp0", "ppp0" //
	};
	private static final String[] WIFI_INTERFACES = { //
		"eth0", "tiwlan0", "wlan0", "athwlan0", "eth1" //
	};
	private static boolean STARTED = false;
	private static HashMap <String, Long> transmitted = new HashMap <String, Long> ();
	private static HashMap <String, Long> received = new HashMap <String, Long> ();
	private static HashMap <String, Long> lasttransmitted = new HashMap <String, Long> ();
	private static HashMap <String, Long> lastreceived = new HashMap <String, Long> ();
	private static Thread t;
	private static String oldData = "";
	//	private static Context context;
	private static long lastModified;
	private static Date nextUpload;
	private static Context context;
	public static synchronized void startMonitoring(Context theContext) {
		if (STARTED)
			return;
		STARTED = true;
		try {
			context = theContext;
			readDate();
			readMaps();
			if (t == null)
				t = new Thread (new DataObserver("/proc/net/dev"));
			t.setName("Data Monitor");
			t.start();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static synchronized void stopMonitoring(Context context) {
		if (!STARTED)
			return;
		try {
			writeMaps();
			t.interrupt();
			STARTED = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class DataObserver implements Runnable {
		private String thePath;
		public DataObserver(String path ) {
			super ();
			thePath = path;
		}

		public void run()
		{
			int i = 0;
			while (STARTED){
				i++;
				try
				{
					Thread.sleep(8000);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				transmit(thePath);
				if (i == 6)
				{
					i = 0;
					writeMaps();
					context = context.getApplicationContext();
				}
			}
		}
	}

	public static void transmit(String path) {
		try {
			InputStream in;
			if (path == null)
			{
				path = "/proc/net/dev";
			}
			File f = new File (path);

			if (!(f.lastModified() == lastModified))
			{
				lastModified = f.lastModified();
				in = new FileInputStream (f);
				InputStreamReader isr = new InputStreamReader(in);
				char[] inputBuffer = new char[1300];
				isr.read(inputBuffer); 
				String data = new String(inputBuffer);
				isr.close();
				in.close();
				if (!data.equals(oldData))
				{

					oldData  = data;
					String[] rows = (data.split("\\n"));

					// first 2 lines are junk.
					for (int i = 2; i < rows.length; i++)
					{
						String s = rows[i];
						s = s.replaceAll("\\s++", " ").replaceAll(": ", ":");
						String[] groups = s.split(":");
						if (groups.length == 2)
						{
							s = groups[1];
							String [] temp = s.split("\\s");
							if (temp.length == 16)
							{
								long read = Long.valueOf(temp[0]);
								long last = 0;
								long stored = 0;

								if (lastreceived.containsKey(groups[0]))
									last = Long.valueOf(lastreceived.get(groups[0]));
								if (received.containsKey(groups[0]))
									stored = Long.valueOf(received.get(groups[0]));

								if (last == 0)
									last = read;
								else if (read > last)
									received.put(groups[0], stored + read - last);
								else if (last > read)
									received.put(groups[0], stored + read);
								lastreceived.put(groups[0], read);

								read = Long.valueOf(temp[8]);
								last = 0;
								stored = 0;

								if (lasttransmitted.containsKey(groups[0]))
									last = Long.valueOf(lasttransmitted.get(groups[0]));
								if (transmitted.containsKey(groups[0]))
									stored = Long.valueOf(transmitted.get(groups[0]));

								if (last == 0)
									last = read;
								else if (read > last)
									transmitted.put(groups[0], stored + read - last);
								else if (last > read)
									transmitted.put(groups[0], stored + read);
								lasttransmitted.put(groups[0], read);
							}
						}
					}
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		uploadToServer();
	}

	public static void writeDate()
	{
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = context.openFileOutput(DATE, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(nextUpload);
			oos.close();
			fos.close();
		} catch (Exception e) {
			try {
				if (fos != null)
					fos.close();
			} catch (Exception e1) {
			}
			try {
				if (oos != null)
					oos.close();
			} catch (Exception e2) {
			}
		}
	}
	public static void writeMaps()
	{
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = context.openFileOutput(TRANSMIT, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(transmitted);
			oos.close();
			fos.close();
			fos = context.openFileOutput(RECEIVE, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(received);
			oos.close();
			fos.close();
			fos = context.openFileOutput(LASTTRANSMIT, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(lasttransmitted);
			oos.close();
			fos.close();
			fos = context.openFileOutput(LASTRECEIVE, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(transmitted);
			oos.close();
			fos.close();
		} catch (Exception e) {
			try {
				if (fos != null)
					fos.close();
			} catch (Exception e1) {
			}
			try {
				if (oos != null)
					oos.close();
			} catch (Exception e2) {
			}
		}
	}
	public static void readDate()
	{
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput(DATE);
			ois = new ObjectInputStream(fis);
			nextUpload = (Date) ois.readObject();
			ois.close();
			fis.close();
		}
		catch (Exception e)
		{
			try
			{
				if (fis != null)
					fis.close();
			}
			catch (Exception e1)
			{
			}
			try
			{
				if (ois != null)
					ois.close();
			}
			catch (Exception e2)
			{
			}
		}
		if (nextUpload == null)
		{
			nextUpload = new Date();
			//			nextUpload.setTime(nextUpload.getTime() + 5*1000*60);
		}
	}

	@SuppressWarnings({ "unchecked" })
	public static void readMaps()
	{
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = context.openFileInput(TRANSMIT);
			ois = new ObjectInputStream(fis);
			transmitted = (HashMap<String, Long>) ois.readObject();
			ois.close();
			fis.close();
			fis = context.openFileInput(RECEIVE);
			ois = new ObjectInputStream(fis);
			received = (HashMap<String, Long>) ois.readObject();
			ois.close();
			fis.close();
			fis = context.openFileInput(LASTTRANSMIT);
			ois = new ObjectInputStream(fis);
			lasttransmitted = (HashMap<String, Long>) ois.readObject();
			ois.close();
			fis.close();
			fis = context.openFileInput(LASTRECEIVE);
			ois = new ObjectInputStream(fis);
			lastreceived = (HashMap<String, Long>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			try {
				if (fis != null)
					fis.close();
			} catch (Exception e1) {
			}
			try {
				if (ois != null)
					ois.close();
			} catch (Exception e2) {
			}
		}
		if (transmitted == null)
			transmitted = new HashMap <String, Long> ();
		if (received == null)
		{
			received = new HashMap <String, Long> ();
		}
	}
	public static void uploadToServer ()
	{
		if (System.currentTimeMillis() < nextUpload.getTime())
			return;
		Iterator<String> itr = transmitted.keySet().iterator();
		while (itr.hasNext())
		{
			String next = itr.next();
			DataRecord r = new DataRecord(context);

			if (transmitted.containsKey(next))
				r.sent = transmitted.get(next);
			else
				r.sent = 0;
			if (received.containsKey(next))
				r.recieved = received.get(next);
			else
				r.recieved = 0;
			transmitted.put(next, (long)0);
			received.put(next, (long)0);

			while (next.charAt(0) ==' ')
				next =next.substring(1);
			for (int i = 0; i < CELL_INTERFACES.length; i++)
			{
				if (CELL_INTERFACES[i].equals(next))
					next = "CELL";
			}
			for (int i = 0; i < WIFI_INTERFACES.length && !next.equals("CELL"); i++)
			{
				if (WIFI_INTERFACES[i].equals(next))
					next = "WIFI";
			}
			r.type = next;
			if (r.sent +r.recieved != 0 && !r.type.equals("lo"))
				FTPTransferManager.sendRecordToServer(r.toString(), context);
		}
		nextUpload.setTime(nextUpload.getTime() + 3*60*60*1000);
		writeDate ();
		writeMaps();
	}
}
