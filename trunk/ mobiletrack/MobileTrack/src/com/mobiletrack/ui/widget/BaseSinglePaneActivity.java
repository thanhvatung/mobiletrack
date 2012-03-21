package com.mobiletrack.ui.widget;

import com.mobiletrack.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * A {@link BaseActivity} that simply contains a single fragment. The intent used to invoke this
 * activity is forwarded to the fragment as arguments during fragment instantiation. Derived
 * activities should only need to implement
 * {@link com.google.android.apps.iosched.ui.BaseSinglePaneActivity#onCreatePane()}.
 */
public abstract class BaseSinglePaneActivity extends BaseActivity {
    private Fragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_activity_singlepane_empty);
        getActivityHelper().setupActionBar(getTitle(), 0);

        final String customTitle = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        getActivityHelper().setActionBarTitle(customTitle != null ? customTitle : getTitle());

        if (savedInstanceState == null) {
            mFragment = onCreatePane();
            mFragment.setArguments(intentToFragmentArguments(getIntent()));

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.root_container, mFragment)
                    .commit();
        }
    }
    protected abstract Fragment onCreatePane();
}
