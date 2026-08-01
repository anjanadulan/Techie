package com.example.techfix;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

public class ServicesActivity extends CustomerScreen {
  private CustomerRepository repository;

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_services);
    CustomerNavigation.bind(this, CustomerNavigation.SERVICES);
    repository = new CustomerRepository(this);

    EditText search = findViewById(R.id.etServiceSearch);
    search.setText("");
    search.setHint(R.string.search_services);
    search.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence value, int start, int count,
                                    int after) {}
      @Override
      public void onTextChanged(CharSequence value, int start, int before,
                                int count) {
        showServices(value.toString());
      }
      @Override
      public void afterTextChanged(Editable value) {}
    });
    findViewById(R.id.filterAll).setOnClickListener(v -> search.setText(""));
    findViewById(R.id.filterPhone)
        .setOnClickListener(v -> search.setText("Phone"));
    findViewById(R.id.filterLaptop)
        .setOnClickListener(v -> search.setText("Laptop"));
    findViewById(R.id.filterTablet)
        .setOnClickListener(v -> search.setText("Tablet"));
    showServices("");
  }

  private void showServices(String query) {
    List<CustomerRepository.ServiceItem> services =
        repository.getServices(query);
    LinearLayout list = findViewById(R.id.servicesList);
    list.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (CustomerRepository.ServiceItem service : services) {
      View row = inflater.inflate(R.layout.view_service_row, list, false);
      ((TextView)row.findViewById(R.id.serviceRowTitle)).setText(service.name);
      ((TextView)row.findViewById(R.id.serviceRowMeta))
          .setText(service.categoryName + " · " + service.estimatedMinutes +
                   " min");
      ((TextView)row.findViewById(R.id.serviceRowPrice))
          .setText("From " +
                   CustomerRepository.formatPrice(service.priceCents));
      row.setOnClickListener(
          view -> ServiceDetailActivity.open(this, service.id));
      list.addView(row);
    }
    findViewById(R.id.tvServicesEmpty)
        .setVisibility(services.isEmpty() ? View.VISIBLE : View.GONE);
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
