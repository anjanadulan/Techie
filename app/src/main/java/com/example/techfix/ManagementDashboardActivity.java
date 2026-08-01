package com.example.techfix;

import android.os.Bundle;
import android.widget.TextView;
import java.util.Locale;

public class ManagementDashboardActivity extends ManagementScreen {
  private ManagementRepository repository;
  private final FirebaseRealtimeSync.DataObserver dataObserver =
      () -> runOnUiThread(this::refreshDashboard);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!showManagementLayout(R.layout.activity_management_dashboard))
      return;
    repository = new ManagementRepository(this);

    findViewById(R.id.btnManagementBack).setOnClickListener(
        view -> openAdminAccount());
    findViewById(R.id.btnAdminAccount).setOnClickListener(
        view -> openAdminAccount());
    bindModule(R.id.manageAppointments, ManagementModuleActivity.APPOINTMENTS);
    bindModule(R.id.manageBranches, ManagementModuleActivity.BRANCHES);
    bindModule(R.id.manageCategories, ManagementModuleActivity.CATEGORIES);
    bindModule(R.id.manageTechnicians, ManagementModuleActivity.TECHNICIANS);
    bindModule(R.id.managePrices, ManagementModuleActivity.PRICES);
    bindModule(R.id.manageParts, ManagementModuleActivity.PARTS);
    bindModule(R.id.manageImages, ManagementModuleActivity.IMAGES);
    bindModule(R.id.managePayments, ManagementModuleActivity.PAYMENTS);
    bindModule(R.id.manageStatuses, ManagementModuleActivity.STATUSES);
    FirebaseRealtimeSync.addObserver(dataObserver);
  }

  @Override
  protected void onResume() {
    super.onResume();
    refreshDashboard();
  }

  private void refreshDashboard() {
    if (repository == null)
      return;
    ManagementRepository.DashboardStats stats = repository.getDashboardStats();
    setText(R.id.tvDashboardActive, twoDigits(stats.activeRepairs));
    setText(R.id.tvDashboardReady, twoDigits(stats.readyRepairs));
    setText(R.id.tvDashboardLowStock, twoDigits(stats.lowStockParts));
    setText(R.id.tvDashboardSummary,
            stats.activeRepairs + " repairs active across Colombo and Galle");
    setText(R.id.tvDashboardAppointmentsMeta,
            stats.activeRepairs + " active repairs");
    setText(
        R.id.tvDashboardBranchesMeta,
        repository.getSummary(ManagementModuleActivity.BRANCHES, "All").metric +
            " locations");
    setText(R.id.tvDashboardCategoriesMeta,
            repository.getSummary(ManagementModuleActivity.CATEGORIES, "All")
                    .metric +
                " categories");
    setText(R.id.tvDashboardTechniciansMeta,
            stats.activeTechnicians + " active technicians");
    setText(R.id.tvDashboardPartsMeta,
            stats.lowStockParts + " low-stock items");
    setText(
        R.id.tvDashboardImagesMeta,
        repository.getSummary(ManagementModuleActivity.IMAGES, "All").metric +
            " images");
    setText(R.id.tvDashboardPaymentsMeta,
            ManagementRepository.formatPrice(stats.paidCents));
    setText(R.id.tvDashboardActivityOne, stats.recentActivity.isEmpty()
                                             ? "No repair activity recorded yet"
                                             : stats.recentActivity.get(0));
    setText(R.id.tvDashboardActivityTwo,
            stats.recentActivity.size() < 2
                ? "Management updates will appear here"
                : stats.recentActivity.get(1));
    findViewById(R.id.tvDashboardActivityOneMeta)
        .setVisibility(android.view.View.GONE);
    findViewById(R.id.tvDashboardActivityTwoMeta)
        .setVisibility(android.view.View.GONE);
  }

  private void bindModule(int viewId, String module) {
    findViewById(viewId).setOnClickListener(
        view -> ManagementModuleActivity.open(this, module));
  }

  private void openAdminAccount() {
    startActivity(new android.content.Intent(this, AdminAccountActivity.class));
  }

  private void setText(int viewId, String value) {
    ((TextView)findViewById(viewId)).setText(value);
  }

  private String twoDigits(long value) {
    return String.format(Locale.US, "%02d", value);
  }

  @Override
  protected void onDestroy() {
    FirebaseRealtimeSync.removeObserver(dataObserver);
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
