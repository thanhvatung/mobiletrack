package com.mobiletrack.ui;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.config.Config.AccountCodeItem;
import com.mobiletrack.config.Config.AccountCodeItem.MatterCode;
import com.mobiletrack.record.ExpenseRecord;
import com.mobiletrack.service.FTPTransferManager;
import com.mobiletrack.ui.widget.BaseActivity;
import com.mobiletrack.util.ServiceStarter;

public class TimeExpenseActivity extends Activity implements OnClickListener {

	LinearLayout list;
	ArrayList<ExpenseRecord> records;
	private int id = 0;
	private String _recordsFileName;
	private Config config;

	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Config.takeSnapShot(this);
		config = Config.getConfig();
		if (config.isAppTerminated()) {
			ServiceStarter.stop(this.getApplicationContext());
			finish();
			return;
		}
		else if (config.getReceiverServer().equals("www.google.com"))
		{
			//getCodes();
			finish();
			return;
		}

		setContentView(R.layout.timeexpense);
//		getActivityHelper().setupActionBar(getTitle(), 0);
		list = (LinearLayout) findViewById(R.id.time_expense_list);
		_recordsFileName = getString(R.string.conf_account_records_lists_file_name);
		loadLists();
		/************************** FOR TESTING PURPOSES ONLY *******************************/
		checkNotification();
		ServiceStarter.go(this.getApplicationContext());
	}

	private void getCodes() {
		Intent intent = new Intent()
		.setClass(this, CustomerCodeActivity.class);
		this.startActivity(intent);
	}

	private void addRecord(ExpenseRecord record) {
		if (records.size() > 0) {
			View ruler = new View(this);
			ruler.setBackgroundColor(0xFF888888);
			list.addView(ruler, new LinearLayout.LayoutParams(
					LayoutParams.FILL_PARENT, 2));
		}
		records.add(record);
		RecordView r = new RecordView(this, record);
		r.setId(id);
		id++;
		this.registerForContextMenu(r);
		r.setOnClickListener(this);
		list.addView(r);
	}

	
	public boolean onCreateOptionsMenu(Menu menu) {
		if (config.isAppTerminated()) {
			return false;
		} else {
			// Inflate menu from XML resource
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.timeexpenseoptionsmenu, menu);
			// Only add extra menu items for a saved note
			return super.onCreateOptionsMenu(menu);
		}
	}

	
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		if (config.isAppTerminated()) {
			// this.cl
		} else {
			super.onCreateContextMenu(menu, v,
					new AdapterContextMenuInfo(v, v.getId(), v.getId()));
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.timeexpensecontextmenu, menu);
			menu.setHeaderTitle(R.string.context_prompt);
		}
	}

	private void removeNotification() {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
		mNotificationManager.cancel(1);
	}

	private void addNotification(int j) {
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		int icon = R.drawable.urgent_clock;
		CharSequence tickerText = "Timer active";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);

		Context context = getApplicationContext();
		CharSequence contentTitle = "An event timer is running";
		CharSequence contentText = "Select to manage unsubmitted events.";
		Intent notificationIntent = this.getIntent();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		final int HELLO_ID = 1;
		mNotificationManager.notify(HELLO_ID, notification);
	}

	
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.new_time) {
			newTime();
			return true;
		} else if (item.getItemId() == R.id.new_expense) {
			newExpense();
			return true;
		} else if (item.getItemId() == R.id.delete_all_records) {
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setMessage("Are you sure you want to delete all records?");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					removeRecords();
				}
			});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			dialog.show();
			return true;
		} else if (item.getItemId() == R.id.submit_all_records) {
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setMessage("Are you sure you want to submit all records?");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					while (records.size() > 0) {
						submitRecord(0);
					}
				}
			});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			dialog.show();
			return true;
		} else if (item.getItemId() == R.id.close_menu) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	private void removeRecords() {
		records = new ArrayList<ExpenseRecord>();
		id = 0;
		list.removeAllViews();
		checkNotification();
		saveLists();
	}

	private void checkNotification() {
		if (config.isAppTerminated()) {
			removeNotification();
		} else {
			boolean needNotification = false;
			for (int i = 0; i < records.size(); i++) {
				if (records.get(i).TimerStarted)
					needNotification = true;
			}
			if (needNotification)
				addNotification(this.id);
			else
				removeNotification();
		}

	}

	private void removeRecord(int position) {
		records.remove(position);
		list.removeViewAt(2 * position);
		int i = 1;
		if (list.getChildCount() > 1) {
			if (position == 0)
				i = 0;
			list.removeViewAt((2 * position) - i);
		}
		checkNotification();
		id--;
		orderLists();
		saveLists();
	}

	private void orderLists() {
		int len = list.getChildCount();
		for (int i = 0; i < len; i += 2) {
			if (list.getChildAt(i).getId() != i / 2)
				list.getChildAt(i).setId(i / 2);
		}
	}

	
	public void onClick(View v) {
		viewRecord(v.getId());
	}

	
	public boolean onContextItemSelected(MenuItem item) {
		final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
		.getMenuInfo();
		if (item.getItemId() == R.id.view_record) {
			viewRecord(info.position);
			return true;
		} else if (item.getItemId() == R.id.delete_record) {
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setMessage("Are you sure you want to delete this record?");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					removeRecord(info.position);
				}
			});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			dialog.show();
			return true;
		} else if (item.getItemId() == R.id.submit_record) {
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setMessage("Are you sure you want to submit this record?");
			dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					submitRecord(info.position);
				}
			});
			dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
					new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
				}
			});
			dialog.show();
			return true;
		} else if (item.getItemId() == R.id.close_menu) {
			return true;
		} else {
			return super.onContextItemSelected(item);
		}
	}

	private void submitRecord(int id) {

		Config.takeSnapShot(this);
		Config config = Config.getConfig();
		Map<String, AccountCodeItem> _acctCodeMap = config.getAcctCodeItemMap();
		ExpenseRecord r = records.get(id);
		if (_acctCodeMap != null)
			if (_acctCodeMap.containsKey(r.AccountCode)) {
				AccountCodeItem i = _acctCodeMap.get(r.AccountCode);
				r.AccountCode = i.getCode();
				for (MatterCode j : i.getMatterCodeList()) {
					if (j.getName().equals(r.MatterCode)) {
						r.MatterCode = j.getCode();
					}
				}
			}
		FTPTransferManager.sendRecordToServer(records.get(id).toString(), this);
		removeRecord(id);
	}

	private void viewRecord(long id) {
		int len = records.size();
		for (int j = 0; j < len; j++) {
			if (list.getChildAt(j * 2).getId() == id) {
				Intent intent;
				if (records.get(j).Time)
					intent = new Intent().setClass(this,
							TimeRecordActivity.class);
				else
					intent = new Intent().setClass(this,
							ExpenseRecordActivity.class);
				intent.putExtra("THE_RECORD", records.get(j)).putExtra(
						"MY_OLD_ID", j);
				this.startActivityForResult(intent, 1);
				return;
			}
		}
	}

	private void newTime() {
		Intent intent = new Intent().setClass(this, TimeRecordActivity.class);
//		this.startActivity(intent);
		ExpenseRecord r = new ExpenseRecord();
		intent.putExtra("THE_RECORD", r);

		this.startActivityForResult(intent, 1);
	}

	private void newExpense() {
		Intent intent = new Intent()
		.setClass(this, ExpenseRecordActivity.class);
		ExpenseRecord r = new ExpenseRecord();
		intent.putExtra("THE_RECORD", r);

		this.startActivityForResult(intent, 1);
	}

	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			int j = data.getExtras().getInt("RETURNID");
			ExpenseRecord r = (ExpenseRecord) data.getExtras().getSerializable(
			"THE_RECORD");
			if (j > -1) {
				((RecordView) list.getChildAt(2 * j)).setRecord(r);
				records.set(j, r);
			} else
				addRecord(r);
			checkNotification();
		}
		saveLists();
	}

	private void saveLists() {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = this.openFileOutput(_recordsFileName, Context.MODE_PRIVATE);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(records);
			oos.close();
			fos.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while saving the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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

	/**
	 * Load the list information from the persistent file
	 */
	@SuppressWarnings("unchecked")
	private void loadLists() {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = this.openFileInput(_recordsFileName);
			ois = new ObjectInputStream(fis);
			records = (ArrayList<ExpenseRecord>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			Log.e(getString(R.string.logging_tag),
					"Got Exception while loading the Lists exp ["
					+ e.getClass().getName() + "] msg ["
					+ e.getMessage() + "]");
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

		if (records == null)
			records = new ArrayList<ExpenseRecord>();
		populateViews();
	}

	private void populateViews() {
		for (ExpenseRecord record : records){
			if (list.getChildCount() > 0) {
				View ruler = new View(this);
				ruler.setBackgroundColor(0xFF888888);
				list.addView(ruler, new LinearLayout.LayoutParams(
						LayoutParams.FILL_PARENT, 2));
			}
			RecordView r = new RecordView(this, record);
			r.setId(id);
			id++;
			this.registerForContextMenu(r);
			r.setOnClickListener(this);
			list.addView(r);
		}
	}

	
	public void onResume() {
		super.onResume();
		int len = records.size();
		for (int i = 0; i < len; i++) {
			((RecordView) list.getChildAt(2 * i)).setRecord(records.get(i));
		}

	}
}
