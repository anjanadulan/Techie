package com.example.techfix;

import android.os.Bundle;

public class ManagementDashboardActivity extends ManagementScreen {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showManagementLayout(R.layout.activity_management_dashboard);

    findViewById(R.id.btnManagementBack).setOnClickListener(view -> finish());
    bindModule(R.id.manageAppointments, ManagementModuleActivity.APPOINTMENTS);
    bindModule(R.id.manageTechnicians, ManagementModuleActivity.TECHNICIANS);
    bindModule(R.id.managePrices, ManagementModuleActivity.PRICES);
    bindModule(R.id.manageParts, ManagementModuleActivity.PARTS);
    bindModule(R.id.manageImages, ManagementModuleActivity.IMAGES);
    bindModule(R.id.managePayments, ManagementModuleActivity.PAYMENTS);
    bindModule(R.id.manageStatuses, ManagementModuleActivity.STATUSES);
  }

  private void bindModule(int viewId, String module) {
    findViewById(viewId).setOnClickListener(view -> ManagementModuleActivity.open(this, module));
  }
}
