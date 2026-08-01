package com.example.techfix;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class AvailabilityActivity extends CustomerScreen {
  private CustomerRepository repository;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_availability);
    repository = new CustomerRepository(this);
    findViewById(R.id.btnAvailabilityBack).setOnClickListener(view -> finish());
    renderAvailability();
  }

  private void renderAvailability() {
    List<CustomerRepository.AvailabilityItem> items =
        repository.getAvailability();
    LinearLayout list = findViewById(R.id.availabilityList);
    LayoutInflater inflater = LayoutInflater.from(this);
    for (CustomerRepository.AvailabilityItem item : items) {
      View card =
          inflater.inflate(R.layout.view_availability_item, list, false);
      ((TextView)card.findViewById(R.id.tvAvailabilityType)).setText(item.type);
      ((TextView)card.findViewById(R.id.tvAvailabilityTitle))
          .setText(item.title);
      ((TextView)card.findViewById(R.id.tvAvailabilityBranch))
          .setText(item.branch);
      ((TextView)card.findViewById(R.id.tvAvailabilityDetail))
          .setText(item.detail);
      TextView status = card.findViewById(R.id.tvAvailabilityStatus);
      status.setText(item.status);
      boolean unavailable = item.status.equals("Out of stock");
      status.setTextColor(getColor(unavailable ? R.color.customer_danger
                                               : R.color.customer_success));
      list.addView(card);
    }
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
