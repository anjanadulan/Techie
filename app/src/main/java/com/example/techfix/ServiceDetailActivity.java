package com.example.techfix;

import android.os.Bundle;

public class ServiceDetailActivity extends CustomerScreen {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showCustomerLayout(R.layout.activity_service_detail);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnBookRepair).setOnClickListener(v -> CustomerNavigation.open(this, BookRepairActivity.class));
    }
}
