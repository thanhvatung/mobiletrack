package com.mobiletrack.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.config.Config.AccountCodeItem;
import com.mobiletrack.config.Config.AccountCodeItem.MatterCode;
import com.mobiletrack.handler.CallHandler;
import com.mobiletrack.record.VoiceRecord;
import com.mobiletrack.service.LocationUploadService;
import com.mobiletrack.ui.widget.BaseActivity;
import com.mobiletrack.util.ServiceStarter;


public class AccountCodeActivity extends Activity implements
OnItemSelectedListener, OnClickListener {

	private Spinner _spinnerAccountCode;
	private Spinner _spinnerMatterCode;
	private EditText _textViewComment;
	private View _buttonAccept, _buttonCancel;
	private Hashtable<String, AccountCodeItem> _acctCodeMap;
	private VoiceRecord _voiceRecord;
	private AccountCodeItem _currentAccoutCodeItem;
	private ArrayAdapter<String> _matterCodeAdapter;
	private int _selectedMatterCodeId;
	private ArrayList<String> _accountCodeNames = new ArrayList<String>();
	private TextView _numberView;
	private static final int DIALOG_SUBMIT = 0;
	private static final int DIALOG_CREATE = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountcode);

		if (CallHandler.callStack.empty())
		{
			CallHandler.loadLists(this);
			if (CallHandler.callStack.empty())
			{
				finish();
			}
		}

		_numberView = (TextView) findViewById (R.id.numberView);
		_voiceRecord = CallHandler.callStack.peek();
		//		_voiceRecord = (VoiceRecord) this.getIntent().getExtras()
		//				.getSerializable(CallHandler.EXTRA_KEY_VOICE_RECORD);
		_numberView.setText("Other Party: " +_voiceRecord._otherParty);
		_spinnerAccountCode = (Spinner) findViewById(R.id.accoundCodeSpinner);
		_spinnerMatterCode = (Spinner) findViewById(R.id.matterCodeSpinner);

		_textViewComment = (EditText) findViewById(R.id.text_comment);

		_buttonAccept = (View) findViewById(R.id.accept_button);
		_buttonAccept.setOnClickListener(this);
		_buttonCancel = (View) findViewById(R.id.cancel_button);
		_buttonCancel.setOnClickListener(this);

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
			ArrayAdapter<String> accountCodeAdapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_dropdown_item,
					_accountCodeNames);
			_spinnerAccountCode.setAdapter(accountCodeAdapter);

			// set up default matter code spinner:
			String defaultAccountCodeName = _accountCodeNames.get(0);
			_spinnerAccountCode.setSelection(0);
			for (int i = 0; i < _accountCodeNames.size(); i++) {
				if (_accountCodeNames.get(i)
						.equals(config.nameOfMatchAcctCode(_voiceRecord
								._otherParty))) {
					_spinnerAccountCode.setSelection(i);
					defaultAccountCodeName = _accountCodeNames.get(i);
					break;
				}
			}

			_currentAccoutCodeItem = _acctCodeMap
			.get(defaultAccountCodeName);
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
			_spinnerMatterCode.setSelection(0);
		}
		_spinnerAccountCode.setOnItemSelectedListener(this);
		_spinnerMatterCode.setOnItemSelectedListener(this);
	}

	// TODO: need to implement layout data pass function if want to enhance the
	// efficiency
	// for layout switch, otherwise the heavy initialization code will be
	// executed
	// each time the layout changes.


	public void onResume()
	{
		super.onResume();
		if (!ServiceStarter.RUNNING)
		{
			ServiceStarter.go(this.getApplicationContext());
		}
		if (CallHandler.callStack.empty())
		{
			CallHandler.loadLists(this);
			if (CallHandler.callStack.empty())
			{
				finish();
			}
		}
		if (CallHandler.callStack.size() >1)
		{
			showDialog (DIALOG_CREATE);
			_voiceRecord = CallHandler.callStack.peek();
			_numberView.setText("Other Party: " +_voiceRecord._otherParty);
			if (_acctCodeMap != null && _acctCodeMap.size() > 0) {
				_selectedMatterCodeId = 0;
				_matterCodeAdapter.clear();
				_spinnerAccountCode.setSelection(0);
				String accountCodeName = _accountCodeNames.get(0);
				_currentAccoutCodeItem = _acctCodeMap
				.get(accountCodeName);
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
			}
		}
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * android.widget.AdapterView.OnItemSelectedListener#onItemSelected(android
	 * .widget.AdapterView, android.view.View, int, long)
	 */
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (parent == _spinnerAccountCode) {
			_selectedMatterCodeId = 0;
			_matterCodeAdapter.clear();
			String accountCodeName = _accountCodeNames.get(position);
			_currentAccoutCodeItem = _acctCodeMap
			.get(accountCodeName);
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
		} else if (parent == _spinnerMatterCode) {
			_selectedMatterCodeId = position;
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

	/*
	 * @Override OnClickListener
	 */
	public void onClick(View v) {
		if (v.getId() == R.id.accept_button) {
			// need to add account code and matter code along with comments,
			// then send
			if (_currentAccoutCodeItem == null) {
				LocationUploadService.sendVoiceSMSRecordWithLocation(this,	_voiceRecord);
			}
			else
			{
				String ac = _currentAccoutCodeItem.getCode();
				List<MatterCode> matterCodes = _currentAccoutCodeItem
				.getMatterCodeList();
				if (matterCodes != null && matterCodes.size() > 0) {
					String mc = matterCodes.get(_selectedMatterCodeId).getCode();
					_voiceRecord._matterCode = (mc);
				}
				_voiceRecord._accountCode =(ac);
				_voiceRecord._accountComment = (_textViewComment.getText()
						.toString());
				LocationUploadService.sendVoiceSMSRecordWithLocation(this,
						_voiceRecord);
			}
		} else if (v.getId() == R.id.cancel_button) {
			// nothing to add, just send
			// FTPTransferManager.sendRecordToServer(_voiceRecord.toString(),
			// this);
			LocationUploadService.sendVoiceSMSRecordWithLocation(this,
					_voiceRecord);
		} else {
		}
		
		
		CallHandler.callStack.pop();
		CallHandler.saveLists(this);
		if (CallHandler.callStack.empty()){
			//finish();
			Intent i = new Intent(Intent.ACTION_MAIN);
			i.addCategory(Intent.CATEGORY_HOME);
			startActivity(i);
		}
		else
		{
			_voiceRecord = CallHandler.callStack.peek();
			_numberView.setText("Other Party: " +_voiceRecord. _otherParty);
			if (_acctCodeMap != null && _acctCodeMap.size() > 0) {
				_selectedMatterCodeId = 0;
				_matterCodeAdapter.clear();
				_spinnerAccountCode.setSelection(0);
				String accountCodeName = _accountCodeNames.get(0);
				_currentAccoutCodeItem = _acctCodeMap
				.get(accountCodeName);
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
			}
			showDialog (DIALOG_SUBMIT);
		}
		
	}

	protected Dialog onCreateDialog (int id) {
		// TODO Auto-generated method stub
		if (!this.hasWindowFocus())
		{
			try
			{
				dismissDialog (DIALOG_CREATE);
			}
			catch (Exception e)
			{

			}
			try
			{
				dismissDialog (DIALOG_SUBMIT);
			}
			catch (Exception e)
			{

			}
		}
		AlertDialog.Builder bld = new AlertDialog.Builder(this);
		switch (id){
		case DIALOG_CREATE: 
		{bld.setMessage("More than one call needs to be reported. Your most recent call is first.").setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener()
		{	public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
		}
		}).setTitle("New Record");
		break;
		}
		case DIALOG_SUBMIT:
		{
			bld.setMessage("Please submit your previous call record now.").setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener()
			{	public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
			}).setTitle("Older Record");
			break;
		}
		}
		return bld.create();
	}


	@Override
	public void onBackPressed()
	{
		Intent i = new Intent(Intent.ACTION_MAIN);
		i.addCategory(Intent.CATEGORY_HOME);
		startActivity(i);
	}
	 	

	/*public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
//			LocationUploadService.sendVoiceSMSRecordWithLocation(this,_voiceRecord);
			finish();
		}

		return false;
	}
	 */
	@Override
	public void onPause()
	{
		super.onPause();
		finish();
	}
}