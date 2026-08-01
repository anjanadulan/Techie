package com.example.techfix;

import android.os.Bundle;

public class RepairHistoryActivity extends CustomerScreen {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_repair_history);
    CustomerNavigation.bind(this, CustomerNavigation.BOOKINGS);
    findViewById(R.id.historyActiveRepair)
        .setOnClickListener(v -> CustomerNavigation.open(this, RepairTrackingActivity.class));
  }
}
