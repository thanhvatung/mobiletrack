package com.mobiletrack.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.Spinner;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.config.Config.AccountCodeItem;
import com.mobiletrack.config.Config.AccountCodeItem.MatterCode;
import com.mobiletrack.record.ExpenseRecord;

public class TimeRecordActivity extends Activity implements
		OnItemSelectedListener, OnClickListener {
	private Spinner _spinnerAccountCode, _spinnerMatterCode;
	private Hashtable<String, AccountCodeItem> _acctCodeMap;
	private AccountCodeItem _currentAccoutCodeItem;
	private ArrayAdapter<String> _matterCodeAdapter;
	private ArrayList<String> _accountCodeNames = new ArrayList<String>();
	private ArrayList<String> _eventCategoryNames = new ArrayList<String>();
	private ExpenseRecord expenseRecord;
	private int position;
	private Chronometer timer;
	private Spinner _EventMatterCode;

	@SuppressWarnings("unchecked")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.timerecord);
		timer = (Chronometer) findViewById(R.id.chronometer1);
		Button controlButton = (Button) findViewById(R.id.start_stop_button);
		Button adjustButton = (Button) findViewById(R.id.adjust_button);
		adjustButton.setOnClickListener(this);
		controlButton.setOnClickListener(this);
		expenseRecord = (ExpenseRecord) this.getIntent().getExtras()
				.getSerializable("THE_RECORD");
		expenseRecord.Time = true;
		setText();
		if (expenseRecord.Duration >= 0) {
			long timerDuration = expenseRecord.Duration;
			if (expenseRecord.TimerStarted) {

				timer.setBase(SystemClock.elapsedRealtime()
						- (System.currentTimeMillis() + timerDuration - (expenseRecord.EventTimeMS)));
				timer.start();
			} else
				timer.setBase(SystemClock.elapsedRealtime() - timerDuration);
		}
		position = this.getIntent().getExtras().getInt("MY_OLD_ID", -1);
		setResult(0);
		Button save = (Button) findViewById(R.id.savebutton);
		save.setOnClickListener(this);
		_spinnerAccountCode = (Spinner) findViewById(R.id.account_spinner);
		_spinnerMatterCode = (Spinner) findViewById(R.id.matter_spinner);
		_EventMatterCode = (Spinner) findViewById(R.id.event_spinner);
		Config.takeSnapShot(this);
		Config config = Config.getConfig();
		_acctCodeMap = config.getAcctCodeItemMap();
		if (_acctCodeMap != null && _acctCodeMap.size() > 0) {
			// ArrayList<String> accountCodeNames = new ArrayList<String>();
			Enumeration<String> enu = _acctCodeMap.keys();
			while (enu.hasMoreElements()) {
				String name = enu.nextElement();
				_accountCodeNames.add(name == null ? "" : name);
			}
			Collections.sort(_accountCodeNames);
			// set up account code spinner
			ArrayAdapter<String> accountCodeAdapter = new ArrayAdapter<String>(
					this, android.R.layout.simple_spinner_dropdown_item,
					_accountCodeNames);
			_spinnerAccountCode.setAdapter(accountCodeAdapter);
			// set up default matter code spinner:
			if (_accountCodeNames.contains(expenseRecord.AccountCode)) {
				_spinnerAccountCode.setSelection(_accountCodeNames
						.indexOf(expenseRecord.AccountCode));
			} else {
				_spinnerAccountCode.setSelection(0);
			}

			_currentAccoutCodeItem = _acctCodeMap.get(_accountCodeNames
					.get(_spinnerAccountCode.getSelectedItemPosition()));
			List<MatterCode> matterCodes = _currentAccoutCodeItem
					.getMatterCodeList();
			List<String> matterCodeNames = new ArrayList<String>();
			if (matterCodes != null && matterCodes.size() > 0) {
				Iterator<MatterCode> iter = matterCodes.iterator();
				while (iter.hasNext()) {
					String name = iter.next().getName();
					matterCodeNames.add(name);
				}
			}
			_matterCodeAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_dropdown_item,
					matterCodeNames);
			_spinnerMatterCode.setAdapter(_matterCodeAdapter);
			if (matterCodeNames.contains(expenseRecord.MatterCode)) {
				_spinnerMatterCode.setSelection(matterCodeNames
						.indexOf(expenseRecord.MatterCode));
			} else {
				_spinnerMatterCode.setSelection(0);
			}
			if (expenseRecord.Cost != 0) {
				((EditText) findViewById(R.id.cost)).setText(Double
						.toString(expenseRecord.Cost));
			}
			if (!expenseRecord.Comment.equals("")) {
				((EditText) findViewById(R.id.comment))
						.setText(expenseRecord.Comment);
			}

		}
		_eventCategoryNames = (ArrayList<String>) config.getTimeCategories();
		if (_eventCategoryNames != null) {
			Collections.sort(_eventCategoryNames);
			if (_eventCategoryNames.size() > 0) {
				ArrayAdapter<String> _EventCodeAdapter = new ArrayAdapter<String>(
						this, android.R.layout.simple_spinner_dropdown_item,
						_eventCategoryNames);
				_EventMatterCode.setAdapter(_EventCodeAdapter);
				if (_eventCategoryNames.contains(expenseRecord.EventCategory)) {
					_EventMatterCode.setSelection(_eventCategoryNames
							.indexOf(expenseRecord.EventCategory));
				} else
					_EventMatterCode.setSelection(0);
			}

		}
		_EventMatterCode.setOnItemSelectedListener(this);
		_spinnerAccountCode.setOnItemSelectedListener(this);
		_spinnerMatterCode.setOnItemSelectedListener(this);
	}

	private void setText() {
		if (expenseRecord.TimerStarted) {
			((Button) findViewById(R.id.start_stop_button)).setText("Stop");
		} else {
			((Button) findViewById(R.id.start_stop_button)).setText("Start");
		}

	}

	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (parent == _spinnerAccountCode) {
			_matterCodeAdapter.clear();
			String accountCodeName = _accountCodeNames.get(position);
			expenseRecord.AccountCode = accountCodeName;
			expenseRecord.MatterCode = "";
			_currentAccoutCodeItem = _acctCodeMap.get(accountCodeName);
			List<MatterCode> matterCodes = _currentAccoutCodeItem
					.getMatterCodeList();
			if (matterCodes != null && matterCodes.size() > 0) {
				Iterator<MatterCode> iter = matterCodes.iterator();
				while (iter.hasNext()) {
					String name = iter.next().getName();
					_matterCodeAdapter.add(name);
				}
			}
			_matterCodeAdapter.notifyDataSetChanged();
			if (_currentAccoutCodeItem.getMatterCodeList().size() > 0)
				expenseRecord.MatterCode = _currentAccoutCodeItem
						.getMatterCodeList().get(0).getName();
		} else if (parent == _spinnerMatterCode) {
			expenseRecord.MatterCode = _currentAccoutCodeItem
					.getMatterCodeList().get(position).getName();
		} else if (parent == _EventMatterCode) {
			expenseRecord.EventCategory = _eventCategoryNames.get(position);
		}
	}

	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.savebutton: {
			// expenseRecord.Cost = Double.valueOf(((EditText)
			// findViewById(R.id.cost)).getText().toString());
			expenseRecord.Comment = ((EditText) findViewById(R.id.comment))
					.getText().toString();
			setResult(1);
			if (expenseRecord.TimerStarted) {
				long stoppedMilliseconds = 0;
				String chronoText = timer.getText().toString();
				String array[] = chronoText.split(":");
				if (array.length == 2) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60
							* 1000 + Integer.parseInt(array[1]) * 1000;
				} else if (array.length == 3) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 60
							* 1000 + Integer.parseInt(array[1]) * 60 * 1000
							+ Integer.parseInt(array[2]) * 1000;
				}
				expenseRecord.Duration = stoppedMilliseconds;
				expenseRecord.LastRecordedTime = SystemClock.elapsedRealtime();
				expenseRecord.EventTimeMS = System.currentTimeMillis();
				timer.stop();
			}
			this.getIntent().putExtra("THE_RECORD", expenseRecord);
			this.getIntent().putExtra("RETURNID", position);
			setResult(RESULT_OK, this.getIntent());
			finish();
			break;
		}
		case R.id.start_stop_button: {
			if (expenseRecord.TimerStarted) {
				long stoppedMilliseconds = 0;
				String chronoText = timer.getText().toString();
				String array[] = chronoText.split(":");
				if (array.length == 2) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60
							* 1000 + Integer.parseInt(array[1]) * 1000;
				} else if (array.length == 3) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 60
							* 1000 + Integer.parseInt(array[1]) * 60 * 1000
							+ Integer.parseInt(array[2]) * 1000;
				}
				timer.stop();
				expenseRecord.Duration = stoppedMilliseconds;
			} else {
				long stoppedMilliseconds = 0;
				String chronoText = timer.getText().toString();
				String array[] = chronoText.split(":");
				if (array.length == 2) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60
							* 1000 + Integer.parseInt(array[1]) * 1000;
				} else if (array.length == 3) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 60
							* 1000 + Integer.parseInt(array[1]) * 60 * 1000
							+ Integer.parseInt(array[2]) * 1000;
				}
				timer.setBase(SystemClock.elapsedRealtime()
						- stoppedMilliseconds);
				timer.start();
				// expenseRecord.StartTime = new Date();
			}
			expenseRecord.TimerStarted = !expenseRecord.TimerStarted;
			expenseRecord.EventTimeMS = System.currentTimeMillis();
			setText();
			break;
		}
		case R.id.adjust_button: {
			Intent intent = new Intent().setClass(this,
					AdjustTimeActivity.class);
			String chronoText = timer.getText().toString();
			String array[] = chronoText.split(":");
			int i = 0;
			int hours = 0;
			if (array.length == 3) {
				hours = Integer.parseInt(array[i]);
				i++;
			}
			int minutes = Integer.parseInt(array[i]);
			i++;
			int seconds = Integer.parseInt(array[i]);
			intent.putExtra("com.mobiletrack.HOURS", hours)
					.putExtra("com.mobiletrack.MINUTES", minutes)
					.putExtra("com.mobiletrack.SECONDS", seconds);
			startActivityForResult(intent, 1);
			break;
		}
		default:
			break;
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			int h = data.getExtras().getInt("HRS");
			int m = data.getExtras().getInt("MIN");
			int s = data.getExtras().getInt("SEC");
			h *= 60 * 60 * 1000;
			m *= 60 * 1000;
			s *= 1000;
			expenseRecord.Duration = h + m + s;
			timer.setBase(SystemClock.elapsedRealtime()
					- expenseRecord.Duration);
		}

	}

	@Override
	public void onBackPressed() {
//		 do something on back.
		expenseRecord = (ExpenseRecord) this.getIntent().getExtras()
				.getSerializable("THE_RECORD");
		if (expenseRecord.Time) {
			setResult(1);
			if (expenseRecord.TimerStarted) {
				long stoppedMilliseconds = 0;
				String chronoText = timer.getText().toString();
				String array[] = chronoText.split(":");
				if (array.length == 2) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60
							* 1000 + Integer.parseInt(array[1]) * 1000;
				} else if (array.length == 3) {
					stoppedMilliseconds = Integer.parseInt(array[0]) * 60 * 60
							* 1000 + Integer.parseInt(array[1]) * 60 * 1000
							+ Integer.parseInt(array[2]) * 1000;
				}
				expenseRecord.Duration = stoppedMilliseconds;
				expenseRecord.LastRecordedTime = SystemClock.elapsedRealtime();
				expenseRecord.EventTimeMS = System.currentTimeMillis();
				timer.stop();
			}
			this.getIntent().putExtra("THE_RECORD", expenseRecord);
			this.getIntent().putExtra("RETURNID", position);
			setResult(RESULT_OK, this.getIntent());
			finish();
		}
		super.onBackPressed();
		//return;
	}

	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}

}
