package com.example.techfix;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class ServiceDetailActivity extends CustomerScreen {
  public static final String EXTRA_SERVICE_ID = "service_id";
  private CustomerRepository repository;
  private long serviceId;

  public static void open(Activity activity, long serviceId) {
    Intent intent = new Intent(activity, ServiceDetailActivity.class);
    intent.putExtra(EXTRA_SERVICE_ID, serviceId);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_service_detail);
    repository = new CustomerRepository(this);
    serviceId = getIntent().getLongExtra(EXTRA_SERVICE_ID, -1);
    if (serviceId <= 0) {
      java.util.List<CustomerRepository.ServiceItem> services = repository.getServices("");
      if (!services.isEmpty())
        serviceId = services.get(0).id;
    }
    bindService();
    findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    findViewById(R.id.btnBookRepair)
        .setOnClickListener(v -> BookRepairActivity.open(this, serviceId));
  }

  private void bindService() {
    CustomerRepository.ServiceItem service = repository.getService(serviceId);
    if (service == null) {
      finish();
      return;
    }
    ((TextView) findViewById(R.id.tvServiceName)).setText(service.name);
    ((TextView) findViewById(R.id.tvServiceDescription)).setText(service.description);
    ((TextView) findViewById(R.id.tvServicePrice))
        .setText(CustomerRepository.formatPrice(service.priceCents));
    ((TextView) findViewById(R.id.tvServiceTime)).setText(service.estimatedMinutes + " min");
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
