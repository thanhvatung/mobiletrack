package com.mobiletrack.ui.widget;


//import com.xelex.test.util.AnalyticsUtils;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobiletrack.R;
import com.mobiletrack.ui.CustomerCodeActivity;
import com.mobiletrack.ui.FamilyMapActivity;
import com.mobiletrack.ui.TimeExpenseActivity;

public class DashboardFragment extends Fragment {

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity()).trackEvent(
                "Home Screen Dashboard", "Click", label, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.ui_dashboard, container);

        // Attach event handlers
        root.findViewById(R.id.home_btn_configuratin).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Configuration");
                startActivity(new Intent(getActivity(),CustomerCodeActivity.class));
                
            }
            
        });

        root.findViewById(R.id.home_btn_uti).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("TimeExpense");
                // Launch sessions list
                startActivity(new Intent(getActivity(),TimeExpenseActivity.class));

            }
        });

        root.findViewById(R.id.home_btn_map).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Map");
                // Launch list of sessions and vendors the user has starred
                startActivity(new Intent(getActivity(),FamilyMapActivity.class));         
            }
        });

        root.findViewById(R.id.home_btn_description).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                // Launch map of conference venue
                fireTrackerEvent("Description");
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                		Uri.parse("https://market.android.com/details?id=com.mobiletrack&feature=search_result#?t=W251bGwsMSwxLDEsImNvbS5tb2JpbGV0cmFjayJd"));
                startActivity(browserIntent);
              //  startActivity(new Intent(getActivity(),DescriptionActivity.class));
            }
        });

        return root;
    }
}
