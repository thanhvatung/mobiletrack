package com.mobiletrack.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import android.content.Context;
import android.telephony.ServiceState;
import android.util.Log;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.util.ServiceStarter;

public class FTPTransferManager {
	private static String LOGGING_TAG = null;
	private static final String DUMMY_RECORD = "\\{0\\}";
	private static final int FTP_PORT_NUMBER = 21;

	private static Thread _worker;
	private volatile static Vector<Object[]> _jobQueue;
	private static boolean _started = false;

	public static void sendRecordToServer(String record, Context context) {
		Config.takeSnapShot(context);
		if (LOGGING_TAG == null)
			LOGGING_TAG = context.getString(R.string.logging_tag);
		ServiceState servState = new ServiceState();
		if (servState.getRoaming()) {
			if (Config.getConfig().DisableRoamingUpload()) {
				Config.getConfig().queueEventRecord(record);
			} else {
				sendRecord(record, context);
			}
		} else {
			sendRecord(record, context);
		}
	}

	private static void sendRecord(String record, Context context) {

		//if (ServiceStarter.STARTED+ServiceStarter.TIMEOUT > System.currentTimeMillis() && LocationTrackService.RUNNING)
		if (!ServiceStarter.RUNNING)
			ServiceStarter.go(context.getApplicationContext());
		int tiReceiverType = Config.getConfig().getTiReceiverType();
		if (tiReceiverType == Config.TI_RECEIVER_TYPE_FTP) {
			sendRecordViaFTP(record, context);
		} else {
			// Log.e(LOGGING_TAG, "tiReceiverType is [" + tiReceiverType +
			// "], which is not FTP, has not been implemented!!!");
			sendRecordViaHTTP(record, context);
			return;
		}
	}

	private static void sendRecordViaHTTP(final String record, Context context) {
		int triesBeforeFail = Config.getConfig().getTriesBeforeFail();
		if (triesBeforeFail < 1) {
			triesBeforeFail = 1;
		}
		while (triesBeforeFail > 0) {
			Config.getConfig().queueEventRecord(record);
			String allRecords = Config.getConfig().getQueuedEventRecords();
			try {

				Config.getConfig().clearEventRecordsQueue();
				String theAddress = Config.getConfig().getReceiverServer();

				Log.i("WebService", "Origional -- theAddress: " +theAddress);
				
				
//				if (theAddress.contains("+")){
//					
//					theAddress = theAddress.replace('+', '&');
//					Log.i("WebService", "theAddress: " +theAddress);
//				}				
				String[] individualRecords = allRecords.split(context.getString(R.string.record_delimiter));
				
				for (int i = 0; i < individualRecords.length; i++)
				{
					String theRecord = URLEncoder.encode(individualRecords[i], "UTF-8");
//					Log.i("WebService:", theRecord);
					String address = theAddress.replaceAll(DUMMY_RECORD, theRecord);					
					URL url = new URL(address);
					URLConnection connection = url.openConnection();
					HttpURLConnection httpConnection = (HttpURLConnection) connection;
					int responseCode = httpConnection.getResponseCode();
					if (responseCode == HttpURLConnection.HTTP_OK) {
						allRecords = allRecords.replace(individualRecords[i]/*+ context.getString(R.string.record_delimiter)*/, "");
					} else {
						Config.	getConfig().queueEventRecord(individualRecords[i]);
					}
				}
				allRecords = allRecords.replaceAll("\n", "");
				if (!allRecords.equals(""))
				{
					triesBeforeFail--;
				}
				else
				{
					return;
				}

			} catch (Exception e) {
				triesBeforeFail--;
				Config.	getConfig().queueEventRecord(allRecords);
			}
		}
	}

	/*
	private static void postAJob(final String record, final Context context) {
		startService();
		new Thread(new Runnable(){
			public void run() {
				synchronized (_jobQueue) {
					_jobQueue.addElement(new Object[] { record, context });
					_jobQueue.notifyAll();
				}
			}
		}).start();

	}
*/
	public static synchronized void stopService() {
		_started = false;
		if (_jobQueue != null)
		{
			_jobQueue.notifyAll();
		}
	}

	public static void startService() {
		if (_started)
			return;

		_jobQueue = new Vector<Object[]>();
		_started = true;
		_worker = new Thread(new Runnable() {
			public void run() {
				while (_started) {
					try 
					{
						final HashSet <String> records = new HashSet <String>();
						StringBuffer allRecords = null;
						Context context = null;
						synchronized (_jobQueue) {
							if (_jobQueue.size() == 0) {
								_jobQueue.wait();
							} else {
								context = (Context) ((Object[]) _jobQueue
										.elementAt(0))[1];
								for (int i = 0; i < _jobQueue.size(); i++) {
									String record = (String) ((Object[]) _jobQueue
											.elementAt(i))[0];
									records.add(record);
								}
								_jobQueue.clear();
							}
						}
						Iterator <String> itr = records.iterator();
						while (itr.hasNext())
						{
							String record = itr.next();
							if (allRecords == null) {
								allRecords = new StringBuffer();
								allRecords.append(record);
							} else {
								allRecords.append(context
										.getString(R.string.record_delimiter)
										+ record);
							}
						}
						if (allRecords != null && context != null) {
							sendRecordViaFTP(allRecords.toString(), context);
						}
					} catch (Exception e) {
						Log.i("MobileTracker",
								"FTPTransferManager's transfer service got exp ["
								+ e.getClass().getName() + "] msg ["
								+ e.getMessage() + "]");
					}
				}
			}
		});
		_worker.start();
	}

	private static void sendRecordViaFTP(final String record, Context context) {
		FTPClient ftpClient = new FTPClient();
		int triesBeforeFail = Config.getConfig().getTriesBeforeFail();
		if (triesBeforeFail < 1)
			triesBeforeFail = 1;
		int ftpConnectionTimeout = Config.getConfig().getConnectionTimeout();

		while (triesBeforeFail > 0) {
			InputStream is = null;
			try {
				String receiverServer = Config.getConfig().getReceiverServer();
				String userName = Config.getConfig().getUserName();
				String passWord = Config.getConfig().getPassword();
				String remoteDir = Config.getConfig().getFTPRemoteDir();

				TimerTask connTimeoutTask = new TimerTask() {
					public void run() {
						Config.getConfig().queueEventRecord(record);
					}
				};
				Timer connTimer = new Timer();
				connTimer
				.schedule(connTimeoutTask, ftpConnectionTimeout * 1000);

				ftpClient.connect(receiverServer, FTP_PORT_NUMBER);
				ftpClient.login(userName, passWord);
				ftpClient.enterLocalPassiveMode();
				ftpClient.changeWorkingDirectory(remoteDir);
				ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

				Config.getConfig().queueEventRecord(record);
				String allRecords = Config.getConfig().getQueuedEventRecords();
				Log.i(LOGGING_TAG, "FTP transfer records [" + allRecords + "]");

				/**
				 * testing purpose if (System.currentTimeMillis() % 2 == 1) {
				 * throw new Exception("fake exception"); }
				 **/

				is = new ByteArrayInputStream(allRecords.getBytes());
				String fileName = "data_" + Config.getConfig().getPhoneNumber()
				+ "_" + System.currentTimeMillis() + ".dat";
				boolean succeed = ftpClient.storeFile(fileName, is);

				if (succeed) {
					connTimer.cancel();
					Config.getConfig().clearEventRecordsQueue();
				} else {
					triesBeforeFail--;
					if (triesBeforeFail > 0)
						continue;
				}
			} catch (Exception e) {
				Log.e(LOGGING_TAG,
						"Got exception while sending record via FTP exp ["
						+ e.getClass().getName() + "] msg ["
						+ e.getMessage() + "]");
				triesBeforeFail--;
				if (triesBeforeFail > 0)
					continue;
			}

			cleanUpFTPClient(ftpClient, is);
			return;
		}
	}

	private static void cleanUpFTPClient(FTPClient ftpClient, InputStream is) {
		try {
			ftpClient.logout();
		} catch (Exception e) {
			Log.e(LOGGING_TAG,
					"Got Exception while logging out ftpClient exp ["
					+ e.getClass().getName() + "]  msg ["
					+ e.getMessage() + "]");
		}

		try {
			ftpClient.disconnect();
			ftpClient = null;
		} catch (Exception e) {
			Log.e(LOGGING_TAG,
					"Got Exception while disconnecting ftpClient exp ["
					+ e.getClass().getName() + "]  msg ["
					+ e.getMessage() + "]");
			ftpClient = null;
		} finally {
			ftpClient = null;
		}

		try {
			if (is != null) {
				is.close();
				is = null;
			}
		} catch (Exception e) {
			Log.e(LOGGING_TAG,
					"Got Exception while closing the inputStream for ftpClient exp ["
					+ e.getClass().getName() + "]  msg ["
					+ e.getMessage() + "]");
			is = null;
		} finally {
			is = null;
		}
	}

}
