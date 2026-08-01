package com.example.techfix;

import android.os.Bundle;

public class ServicesActivity extends CustomerScreen {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showCustomerLayout(R.layout.activity_services);
        CustomerNavigation.bind(this, CustomerNavigation.SERVICES);
        int[] rows = {R.id.serviceScreen, R.id.serviceBattery, R.id.serviceKeyboard, R.id.serviceDiagnostic};
        for (int row : rows) findViewById(row).setOnClickListener(v -> CustomerNavigation.open(this, ServiceDetailActivity.class));
    }
}
