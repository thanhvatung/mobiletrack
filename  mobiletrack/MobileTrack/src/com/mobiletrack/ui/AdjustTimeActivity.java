package com.mobiletrack.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.mobiletrack.R;

public class AdjustTimeActivity extends Activity implements OnClickListener,
		TextWatcher {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.adjusttime);
		setResult(0);
		Button confirm = (Button) findViewById(R.id.confirm);
		EditText minutes = (EditText) findViewById(R.id.numminutes);
		EditText hours = (EditText) findViewById(R.id.numhours);
		EditText seconds = (EditText) findViewById(R.id.numseconds);
		int hrs = this.getIntent().getExtras().getInt("com.mobiletrack.HOURS");
		int min = this.getIntent().getExtras()
				.getInt("com.mobiletrack.MINUTES");
		int sec = this.getIntent().getExtras()
				.getInt("com.mobiletrack.SECONDS");
		hours.setText(Integer.toString(hrs));
		minutes.setText(Integer.toString(min));
		seconds.setText(Integer.toString(sec));
		seconds.addTextChangedListener(this);
		minutes.addTextChangedListener(this);
		hours.addTextChangedListener(this);

		/*
		 * seconds.setOnKeyListener(this); minutes.setOnKeyListener(this);
		 * hours.setOnKeyListener(this);
		 */
		confirm.setOnClickListener(this);

	}

	/*
	 * @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
	 * 
	 * if (((EditText) v).getText().toString().equals ("")) return false; switch
	 * (v.getId()) { case R.id.numhours: { int i = Integer.valueOf(((EditText)
	 * v).getText().toString()); if (i > 100) { ((EditText) v).setText("");
	 * 
	 * } break; } case R.id.numminutes: { int i = Integer.valueOf(((EditText)
	 * v).getText().toString()); if (i > 60) { ((EditText) v).setText(""); }
	 * break; } case R.id.numseconds: { int i = Integer.valueOf(((EditText)
	 * v).getText().toString()); if (i > 60) { ((EditText) v).setText(""); }
	 * break; } } return false; }
	 */

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.confirm: {
			String hrs = (((EditText) findViewById(R.id.numhours)).getText()
					.toString());
			if (hrs.equals(""))
				hrs = "0";
			String min = (((EditText) findViewById(R.id.numminutes)).getText()
					.toString());
			if (min.equals(""))
				min = "0";
			String sec = (((EditText) findViewById(R.id.numseconds)).getText()
					.toString());
			if (sec.equals(""))
				sec = "0";
			this.getIntent().putExtra("HRS", Integer.valueOf(hrs));
			this.getIntent().putExtra("MIN", Integer.valueOf(min));
			this.getIntent().putExtra("SEC", Integer.valueOf(sec));
			setResult(RESULT_OK, this.getIntent());
			finish();
			break;
		}
		}
	}

	public void afterTextChanged(Editable s) {
		if (s.length() > 0) {
			int i = Integer.valueOf(s.toString());
			if (i > 59) {
				s.clear();
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
