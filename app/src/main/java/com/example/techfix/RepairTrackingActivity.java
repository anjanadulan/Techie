package com.example.techfix;

import android.os.Bundle;

public class RepairTrackingActivity extends CustomerScreen {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showCustomerLayout(R.layout.activity_repair_tracking);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
