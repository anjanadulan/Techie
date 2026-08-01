package com.example.techfix;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import java.util.List;

public class CustomerHomeActivity extends CustomerScreen {
  private CustomerRepository repository;
  private SessionManager sessionManager;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_customer_home);
    CustomerNavigation.bind(this, CustomerNavigation.HOME);
    repository = new CustomerRepository(this);
    sessionManager = new SessionManager(this);

    findViewById(R.id.openServices)
        .setOnClickListener(v -> CustomerNavigation.open(this, ServicesActivity.class));
    findViewById(R.id.searchServices)
        .setOnClickListener(v -> CustomerNavigation.open(this, ServicesActivity.class));
  }

  @Override
  protected void onResume() {
    super.onResume();
    bindPopularService();
    bindActiveRepair();
  }

  private void bindPopularService() {
    List<CustomerRepository.ServiceItem> services = repository.getServices("");
    View card = findViewById(R.id.popularService);
    if (services.isEmpty()) {
      card.setVisibility(View.GONE);
      return;
    }
    CustomerRepository.ServiceItem service = services.get(0);
    ((TextView) findViewById(R.id.tvPopularServiceName)).setText(service.name);
    ((TextView) findViewById(R.id.tvPopularServiceMeta))
        .setText("From " + CustomerRepository.formatPrice(service.priceCents) + " · "
            + service.estimatedMinutes + " min");
    card.setOnClickListener(v -> ServiceDetailActivity.open(this, service.id));
  }

  private void bindActiveRepair() {
    View card = findViewById(R.id.activeRepairCard);
    CustomerRepository.AppointmentItem appointment =
        repository.getLatestActiveAppointment(sessionManager.getUserId());
    if (appointment == null) {
      card.setVisibility(View.GONE);
      return;
    }
    card.setVisibility(View.VISIBLE);
    ((TextView) findViewById(R.id.tvActiveRepairId)).setText("Repair #TF-" + appointment.id);
    ((TextView) findViewById(R.id.tvActiveRepairStatus))
        .setText(CustomerRepository.statusLabel(appointment.status));
    ((TextView) findViewById(R.id.tvActiveRepairSummary))
        .setText(appointment.deviceDetails + " · " + appointment.serviceName);
    card.setOnClickListener(v -> RepairTrackingActivity.open(this, appointment.id));
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
