package com.mobiletrack.service;

import android.accounts.AccountManager;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;

public class WipeService extends Service {

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, final int startId)
	{
		ContentResolver cr = getContentResolver();
		Cursor c = cr.query(ContactsContract.Contacts.CONTENT_URI,
				null, null, null, null);
		while (c.moveToNext())
		{
			try{
				String lookupKey = c.getString(c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
				Uri uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey);
				System.out.println("The uri is " + uri.toString());
				cr.delete(uri, null, null);
			}
			catch(Exception e)
			{
				System.out.println(e.getStackTrace());
			}
		}
		android.accounts.Account[] accounts = 
			AccountManager.get(this).getAccounts(); 
		for (android.accounts.Account account: accounts) {
			try
			{
			AccountManager.get(this).removeAccount(account, null, 
					null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		Uri deleteUri = Uri.parse("content://sms/");
		c = cr.query(deleteUri, null, null,
				null, null);
		while (c.moveToNext())
		{
			try
			{
				String pid = c.getString(0);
				String uri = deleteUri + pid;
				getContentResolver().delete(Uri.parse(uri),
						null, null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		deleteUri = CallLog.CONTENT_URI;
		c = cr.query(CallLog.Calls.CONTENT_URI, null, null,
				null, null);
		while (c.moveToNext())
		{
			try
			{
				String pid = c.getString(3);
				String uri = "content://call_log/calls/" +pid;
				getContentResolver().delete(Uri.parse(uri),
						null, null);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}
}
