package com.mobiletrack.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.config.Config.AccountCodeItem;
import com.mobiletrack.config.Config.AccountCodeItem.MatterCode;
import com.mobiletrack.record.ExpenseRecord;
import com.mobiletrack.ui.widget.BaseActivity;

public class ExpenseRecordActivity extends Activity implements
		OnItemSelectedListener, OnClickListener, TextWatcher {
	private static final int MAXLENGTH = 7;
	private Spinner _spinnerAccountCode, _spinnerMatterCode;
	private Hashtable<String, AccountCodeItem> _acctCodeMap;
	private AccountCodeItem _currentAccoutCodeItem;
	private ArrayAdapter<String> _matterCodeAdapter;
	private ArrayList<String> _accountCodeNames = new ArrayList<String>();
	private ExpenseRecord expenseRecord;
	private int position;
	private ArrayList<String> _eventCategoryNames;
	private Spinner _EventMatterCode;

	@SuppressWarnings("unchecked")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.expenserecord);
		expenseRecord = (ExpenseRecord) this.getIntent().getExtras()
				.getSerializable("THE_RECORD");
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
		((EditText) findViewById(R.id.cost)).addTextChangedListener(this);
		_eventCategoryNames = (ArrayList<String>) config.getExpenseCategories();
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
//			_spinnerMatterCode.
			if (matterCodes != null && matterCodes.size() > 0) {
				Iterator<MatterCode> iter = matterCodes.iterator();
				while (iter.hasNext()) {
					String name = iter.next().getName();
					_matterCodeAdapter.add(name);
				}
			}
			_matterCodeAdapter.notifyDataSetChanged();
			if (_currentAccoutCodeItem.getMatterCodeList().size() > 0)
			{
				expenseRecord.MatterCode = _currentAccoutCodeItem
						.getMatterCodeList().get(0).getName();
			}
		} else if (parent == _spinnerMatterCode) {
			expenseRecord.MatterCode = _currentAccoutCodeItem
					.getMatterCodeList().get(position).getName();
		} else if (parent == _EventMatterCode) {
			expenseRecord.EventCategory = _eventCategoryNames.get(position);
		}
	}

	/*
	 * @Override(non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemSelectedListener#onNothingSelected(android
	 * .widget.AdapterView)
	 */
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub

	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.savebutton:
			expenseRecord.Cost = Double
					.valueOf(((EditText) findViewById(R.id.cost)).getText()
							.toString());
			expenseRecord.Comment = ((EditText) findViewById(R.id.comment))
					.getText().toString();
			setResult(1);
			this.getIntent().putExtra("THE_RECORD", expenseRecord);
			this.getIntent().putExtra("RETURNID", position);
			setResult(RESULT_OK, this.getIntent());
			finish();
			break;
		default:
			break;
		}
		finish();
	}

	public void afterTextChanged(Editable s) {
		String temp = s.toString();
		String[] array = temp.split("[.]");
		if (array[0].length() > MAXLENGTH) {
			s.replace(MAXLENGTH, MAXLENGTH + 1, "");
		} else if (array.length > 2) {
			s.clear();
			s.append(array[0] + "." + array[1]);
		} else if (array.length < 2) {

		} else {
			if (array[1].length() > 2) {
				array[0] += array[1].charAt(0);
				if (array[0].substring(0, 1).equals("0")) {
					array[0] = array[0].substring(1);
				}
				array[1] = array[1].substring(1);
				s.clear();
				s.append(array[0] + "." + array[1]);
			}

		}

	}

	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
		// TODO Auto-generated method stub

	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// TODO Auto-generated method stub

	}

}
