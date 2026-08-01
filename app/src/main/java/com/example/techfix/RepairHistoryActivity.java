package com.example.techfix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.techfix.data.model.AppointmentStatus;
import java.util.List;

public class RepairHistoryActivity extends CustomerScreen {
  private CustomerRepository repository;
  private SessionManager sessionManager;
  private boolean observingRealtime;
  private final FirebaseRealtimeSync.DataObserver dataObserver =
      () -> runOnUiThread(() -> {
        if (observingRealtime && repository != null && !isFinishing())
          bindHistory();
      });

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_repair_history);
    CustomerNavigation.bind(this, CustomerNavigation.BOOKINGS);
    repository = new CustomerRepository(this);
    sessionManager = new SessionManager(this);
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (!observingRealtime) {
      FirebaseRealtimeSync.addObserver(dataObserver);
      observingRealtime = true;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    bindHistory();
  }

  @Override
  protected void onStop() {
    if (observingRealtime) {
      FirebaseRealtimeSync.removeObserver(dataObserver);
      observingRealtime = false;
    }
    super.onStop();
  }

  private void bindHistory() {
    List<CustomerRepository.AppointmentItem> appointments =
        repository.getAppointments(sessionManager.getUserId());
    LinearLayout list = findViewById(R.id.repairHistoryList);
    list.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (CustomerRepository.AppointmentItem appointment : appointments) {
      View card =
          inflater.inflate(R.layout.view_repair_history_item, list, false);
      ((TextView)card.findViewById(R.id.historyItemId))
          .setText("#TF-" + appointment.id + " · " +
                   CustomerRepository.formatDate(appointment.createdAt));
      TextView status = card.findViewById(R.id.historyItemStatus);
      status.setText(CustomerRepository.statusLabel(appointment.status));
      boolean cancelled = appointment.status == AppointmentStatus.CANCELLED;
      status.setBackgroundResource(cancelled ? R.drawable.bg_status_cancelled
                                             : R.drawable.bg_status_success);
      status.setTextColor(getColor(cancelled ? R.color.customer_danger
                                             : R.color.customer_success));
      ((TextView)card.findViewById(R.id.historyItemTitle))
          .setText(appointment.deviceDetails);
      ((TextView)card.findViewById(R.id.historyItemSubtitle))
          .setText(appointment.serviceName + " · " +
                   (appointment.branchName == null
                        ? "Branch pending"
                        : appointment.branchName + " branch"));
      ((TextView)card.findViewById(R.id.historyItemPrice))
          .setText((appointment.status == AppointmentStatus.COMPLETED
                        ? "Total · "
                        : "Estimated · ") +
                   CustomerRepository.formatPrice(appointment.priceCents));
      card.setOnClickListener(
          v -> RepairTrackingActivity.open(this, appointment.id));
      list.addView(card);
    }
    findViewById(R.id.tvHistoryEmpty)
        .setVisibility(appointments.isEmpty() ? View.VISIBLE : View.GONE);
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
