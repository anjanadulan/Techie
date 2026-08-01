package com.example.techfix;

import android.os.Bundle;

public class CustomerHomeActivity extends CustomerScreen {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showCustomerLayout(R.layout.activity_customer_home);
        CustomerNavigation.bind(this, CustomerNavigation.HOME);
        findViewById(R.id.openServices).setOnClickListener(v -> CustomerNavigation.open(this, ServicesActivity.class));
        findViewById(R.id.searchServices).setOnClickListener(v -> CustomerNavigation.open(this, ServicesActivity.class));
        findViewById(R.id.activeRepairCard).setOnClickListener(v -> CustomerNavigation.open(this, RepairTrackingActivity.class));
        findViewById(R.id.popularService).setOnClickListener(v -> CustomerNavigation.open(this, ServiceDetailActivity.class));
    }
}
