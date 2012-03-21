package com.mobiletrack.ui;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobiletrack.R;
import com.mobiletrack.record.ExpenseRecord;

public class RecordView extends LinearLayout {

	ExpenseRecord myRecord;
	TextView date, cost, mttrCode, acctCode;
	LinearLayout leftText, rightText;
	ImageView icon;

	public RecordView(Context context, ExpenseRecord r) {
		super(context);
		// global layout is horizontal
		setOrientation(LinearLayout.HORIZONTAL);
		leftText = new LinearLayout(context);
		rightText = new LinearLayout(context);
		leftText.setOrientation(LinearLayout.VERTICAL);
		rightText.setOrientation(LinearLayout.VERTICAL);
		icon = new ImageView(context);

		date = new TextView(context);
		cost = new TextView(context);
		mttrCode = new TextView(context);
		acctCode = new TextView(context);
		setRecord(r);
		addView(icon);
		leftText.setGravity(Gravity.LEFT);
		rightText.setGravity(Gravity.RIGHT);
		cost.setGravity(Gravity.RIGHT);
		rightText.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.FILL_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT));
		mttrCode.setGravity(Gravity.RIGHT);
		leftText.addView(date);
		leftText.addView(acctCode);
		rightText.addView(cost);
		rightText.addView(mttrCode);
		rightText.setPadding(5, 0, 5, 0);
		leftText.setPadding(5, 0, 5, 0);
		addView(leftText);
		addView(rightText);
		// rightText.setLayoutParams(new LinearLayout.LayoutParams
		// (LinearLayout.LayoutParams.FILL_PARENT,
		// LinearLayout.LayoutParams.FILL_PARENT));

	}

	public ContextMenuInfo getContextMenuInfo() {
		AdapterContextMenuInfo toReturn = new AdapterContextMenuInfo(this,
				this.getId(), 0);
		return toReturn;
	}

	public void setRecord(ExpenseRecord record) {
		myRecord = record;
		date.setText(record.theDate.toLocaleString());
		Bitmap mBitmap;

		if (record.Time) {
			if (record.TimerStarted) {
				record.Duration = System.currentTimeMillis() + record.Duration
						- (record.EventTimeMS);
				record.EventTimeMS = System.currentTimeMillis();
			}
			String[] array = new String[3];
			array[0] = Long.toString((record.Duration / 60 / 60 / 1000));
			array[1] = Long.toString((record.Duration / 60 / 1000) % 60);
			array[2] = Long.toString((record.Duration / 1000) % 60);
			while (array[1].length() < 2)
				array[1] = "0" + array[1];
			while (array[2].length() < 2)
				array[2] = "0" + array[2];
			String output = array[0] + ":" + array[1] + ":" + array[2];
			cost.setText(output);
			{
				if (record.TimerStarted)
					mBitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.urgent_clock);
				else
					mBitmap = BitmapFactory.decodeResource(getResources(),
							R.drawable.time_frame);
			}
		} else {
			String theCost = Double.toString(record.Cost);
			int i = theCost.indexOf(".");
			while (theCost.substring(i + 1).length() < 2) {
				theCost += "0";
			}
			cost.setText(theCost);
			mBitmap = BitmapFactory.decodeResource(getResources(),
					R.drawable.bills);
		}
		mttrCode.setText(record.MatterCode);
		acctCode.setText(record.AccountCode);
		icon.setImageBitmap(mBitmap);
	}

}
