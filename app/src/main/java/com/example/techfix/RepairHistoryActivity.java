package com.example.techfix;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.techfix.data.model.AppointmentStatus;
import java.util.List;

public class RepairHistoryActivity extends CustomerScreen {
  private CustomerRepository repository;
  private SessionManager sessionManager;
  private final int[] cardIds = {R.id.historyCard1, R.id.historyCard2, R.id.historyCard3};
  private final int[] idIds = {R.id.historyId1, R.id.historyId2, R.id.historyId3};
  private final int[] statusIds = {R.id.historyStatus1, R.id.historyStatus2, R.id.historyStatus3};
  private final int[] titleIds = {R.id.historyTitle1, R.id.historyTitle2, R.id.historyTitle3};
  private final int[] subtitleIds = {
      R.id.historySubtitle1, R.id.historySubtitle2, R.id.historySubtitle3};
  private final int[] priceIds = {R.id.historyPrice1, R.id.historyPrice2, R.id.historyPrice3};

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_repair_history);
    CustomerNavigation.bind(this, CustomerNavigation.BOOKINGS);
    repository = new CustomerRepository(this);
    sessionManager = new SessionManager(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    bindHistory();
  }

  private void bindHistory() {
    List<CustomerRepository.AppointmentItem> appointments =
        repository.getAppointments(sessionManager.getUserId());
    for (int index = 0; index < cardIds.length; index++) {
      View card = findViewById(cardIds[index]);
      if (index >= appointments.size()) {
        card.setVisibility(View.GONE);
        continue;
      }
      CustomerRepository.AppointmentItem appointment = appointments.get(index);
      card.setVisibility(View.VISIBLE);
      ((TextView) findViewById(idIds[index]))
          .setText("#TF-" + appointment.id + " · "
              + CustomerRepository.formatDate(appointment.createdAt));
      TextView status = findViewById(statusIds[index]);
      status.setText(CustomerRepository.statusLabel(appointment.status));
      boolean cancelled = appointment.status == AppointmentStatus.CANCELLED;
      status.setBackgroundResource(
          cancelled ? R.drawable.bg_status_cancelled : R.drawable.bg_status_success);
      status.setTextColor(getColor(cancelled ? R.color.customer_danger : R.color.customer_success));
      ((TextView) findViewById(titleIds[index])).setText(appointment.deviceDetails);
      ((TextView) findViewById(subtitleIds[index]))
          .setText(appointment.serviceName + " · "
              + (appointment.branchName == null ? "Branch pending"
                                                : appointment.branchName + " branch"));
      ((TextView) findViewById(priceIds[index]))
          .setText((appointment.status == AppointmentStatus.COMPLETED ? "Total · " : "Estimated · ")
              + CustomerRepository.formatPrice(appointment.priceCents));
      card.setOnClickListener(v -> RepairTrackingActivity.open(this, appointment.id));
    }
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
