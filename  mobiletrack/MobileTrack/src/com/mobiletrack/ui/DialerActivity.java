package com.mobiletrack.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.mobiletrack.R;
import com.phonelib.net.CallApplication;


public class DialerActivity extends Activity implements OnClickListener {
	private static final int PICK_CONTACT = 0;
	private String mPhoneNumber = "";
	private TextView mNumberBox;
	private final int[] BUTTON_IDS = { R.id.num1_button, R.id.num2_button,
			R.id.num3_button, R.id.num4_button, R.id.num5_button,
			R.id.num6_button, R.id.num7_button, R.id.num8_button,
			R.id.num9_button, R.id.num0_button, R.id.send_button,
			R.id.contacts_button, R.id.delete_button, R.id.star_button,
			R.id.pound_button };

	private View[] mButtonViews;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialer);
		initButtons();
		mNumberBox = (TextView) findViewById(R.id.phone_number);
	}

	public void initButtons() {
		mButtonViews = new View[BUTTON_IDS.length];
		for (int i = 0; i < BUTTON_IDS.length; i++) {
			mButtonViews[i] = findViewById(BUTTON_IDS[i]);
			mButtonViews[i].setOnClickListener(this);
		}
	}

	private void updateNumberField() {
		mNumberBox.setText(mPhoneNumber);
		// hide V-keyboard
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(mNumberBox.getWindowToken(), 0);
	}

	public void onClick(View v) {
		if (v.getId() == R.id.num1_button) {
			mPhoneNumber += "" + 1;
		} else if (v.getId() == R.id.num2_button) {
			mPhoneNumber += "" + 2;
		} else if (v.getId() == R.id.num3_button) {
			mPhoneNumber += "" + 3;
		} else if (v.getId() == R.id.num4_button) {
			mPhoneNumber += "" + 4;
		} else if (v.getId() == R.id.num5_button) {
			mPhoneNumber += "" + 5;
		} else if (v.getId() == R.id.num6_button) {
			mPhoneNumber += "" + 6;
		} else if (v.getId() == R.id.num7_button) {
			mPhoneNumber += "" + 7;
		} else if (v.getId() == R.id.num8_button) {
			mPhoneNumber += "" + 8;
		} else if (v.getId() == R.id.num9_button) {
			mPhoneNumber += "" + 9;
		} else if (v.getId() == R.id.num0_button) {
			mPhoneNumber += "" + 0;
		} else if (v.getId() == R.id.star_button) {
			mPhoneNumber += "*";
		} else if (v.getId() == R.id.pound_button) {
			mPhoneNumber += "#";
		} else if (v.getId() == R.id.send_button) {
			try {
				CallApplication.launch(this, mPhoneNumber);
			} catch (Exception e) {
				Log.e("helloandroid dialing example", "Call failed", e);
			}
		} else if (v.getId() == R.id.contacts_button) {
			showContacts();
		} else if (v.getId() == R.id.delete_button) {
			if (mPhoneNumber.length() > 0)
				mPhoneNumber = mPhoneNumber.substring(0,
						mPhoneNumber.length() - 1);
		} else {
		}

		updateNumberField();
	}

	private void showContacts() {
		Intent intent = new Intent(Intent.ACTION_PICK);
		intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
		startActivityForResult(intent, PICK_CONTACT);

	}

	@Override
	public void onActivityResult(int reqCode, int resultCode, Intent data) {
		super.onActivityResult(reqCode, resultCode, data);

		switch (reqCode) {
		case (PICK_CONTACT):
			if (resultCode == Activity.RESULT_OK) {
				getContactInfo(data);
			}
			updateNumberField();
			break;
		}
	}

	protected void getContactInfo(Intent intent) {

		Cursor cursor = managedQuery(intent.getData(), null, null, null, null);
		while (cursor.moveToNext()) {
			String contactId = cursor.getString(cursor
					.getColumnIndex(ContactsContract.Contacts._ID));
			// name =
			// cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));

			String hasPhone = cursor
					.getString(cursor
							.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

			if (hasPhone.equalsIgnoreCase("1"))
				hasPhone = "true";
			else
				hasPhone = "false";

			if (Boolean.parseBoolean(hasPhone)) {
				Cursor phones = getContentResolver().query(
						ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
						null,
						ContactsContract.CommonDataKinds.Phone.CONTACT_ID
								+ " = " + contactId, null, null);
				while (phones.moveToNext()) {
					mPhoneNumber = phones
							.getString(phones
									.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				}
				phones.close();
			}
			/*
			 * // Find Email Addresses Cursor emails =
			 * getContentResolver().query
			 * (ContactsContract.CommonDataKinds.Email.
			 * CONTENT_URI,null,ContactsContract
			 * .CommonDataKinds.Email.CONTACT_ID + " = " + contactId,null,
			 * null); while (emails.moveToNext()) { String emailAddress =
			 * emails.
			 * getString(emails.getColumnIndex(ContactsContract.CommonDataKinds
			 * .Email.DATA)); } emails.close();
			 * 
			 * Cursor address = getContentResolver().query(
			 * ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
			 * null,
			 * ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID +
			 * " = " + contactId, null, null); while (address.moveToNext()) { //
			 * These are all private class variables, don't forget to create
			 * them. String poBox =
			 * address.getString(address.getColumnIndex(ContactsContract
			 * .CommonDataKinds.StructuredPostal.POBOX)); String street =
			 * address
			 * .getString(address.getColumnIndex(ContactsContract.CommonDataKinds
			 * .StructuredPostal.STREET)); city =
			 * address.getString(address.getColumnIndex
			 * (ContactsContract.CommonDataKinds.StructuredPostal.CITY)); state
			 * =address.getString(address.getColumnIndex(ContactsContract.
			 * CommonDataKinds.StructuredPostal.REGION)); postalCode =
			 * address.getString
			 * (address.getColumnIndex(ContactsContract.CommonDataKinds
			 * .StructuredPostal.POSTCODE)); country =
			 * address.getString(address.
			 * getColumnIndex(ContactsContract.CommonDataKinds
			 * .StructuredPostal.COUNTRY)); type =
			 * address.getString(address.getColumnIndex
			 * (ContactsContract.CommonDataKinds.StructuredPostal.TYPE)); }
			 */
		}
		cursor.close();
	}

}
