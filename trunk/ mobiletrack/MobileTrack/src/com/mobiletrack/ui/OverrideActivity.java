package com.mobiletrack.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiletrack.R;
import com.mobiletrack.config.Config;
import com.mobiletrack.process.SMSBlockProcess;
import com.mobiletrack.record.OverrideRecord;
import com.mobiletrack.service.FTPTransferManager;
import com.mobiletrack.util.ServiceStarter;

public class OverrideActivity extends Activity {


	@Override
	public void onResume()
	{
		super.onResume();
		if (!ServiceStarter.RUNNING)
		{
			ServiceStarter.go(this.getApplicationContext());
			finish();
		}

		Config.takeSnapShot(this);
		if (!Config.getConfig().isSpeeding() || SMSBlockProcess.OVERRIDE)
			finish();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!ServiceStarter.RUNNING)
		{
			ServiceStarter.go(this.getApplicationContext());
			finish();
		}

		Config.takeSnapShot(this);
		int i = Config.getConfig().canOverride();
		setContentView(R.layout.override);
		if (!Config.getConfig().isSpeeding() || SMSBlockProcess.OVERRIDE)
			finish();
		if (i != 2)
		{
			((TextView) findViewById(R.id.password_instructions)).setText("This feature has been disabled due to your speed. Please press the Override button below if you would like to continue.");
			findViewById(R.id.password_field).setVisibility(View.INVISIBLE);
		}

		findViewById(R.id.button_override).setOnClickListener(new OnClickListener()
		{

			public void onClick(View v){

				String input =((EditText) findViewById(R.id.password_field)).getText().toString();
				String password = Config.getConfig().getOverridePassword();
				if ( Config.getConfig().canOverride() == 1 || input.equals(password))
				{
					override();
					finish();
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Password incorrect", Toast.LENGTH_SHORT).show();
					((EditText) findViewById(R.id.password_field)).setText("");
				}
			}
		}
		);
		findViewById(R.id.button_cancel).setOnClickListener(new OnClickListener()
		{

			public void onClick(View v) {
				/*Intent intent = new Intent();
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.addCategory(Intent.CATEGORY_HOME);
				intent.setAction(Intent.ACTION_MAIN);
				startActivity(intent);*/
				finish();
			}
		}
		);
	}
	@Override
	public void onPause()
	{
		super.onPause();
		finish();
	}
	private void override ()
	{
		SMSBlockProcess.OVERRIDE = true;
		FTPTransferManager.sendRecordToServer((new OverrideRecord(this)).toString(), this);

	}
}
