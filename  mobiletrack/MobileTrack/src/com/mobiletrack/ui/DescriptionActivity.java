package com.mobiletrack.ui;

import com.mobiletrack.R;
import android.os.Bundle;

import com.mobiletrack.ui.widget.BaseActivity;

public class DescriptionActivity extends BaseActivity{
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.description);
		getActivityHelper().setupActionBar(getTitle(), 0);
	}
}
