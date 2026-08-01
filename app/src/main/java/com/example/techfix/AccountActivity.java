package com.example.techfix;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AccountActivity extends AppCompatActivity {
  private SessionManager sessionManager;
  private CustomerRepository customerRepository;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sessionManager = new SessionManager(this);

    if (!sessionManager.isLoggedIn()) {
      openAuthentication();
      return;
    }

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_account);
    customerRepository = new CustomerRepository(this);

    ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
      Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
      view.setPadding(0, systemBars.top, 0, systemBars.bottom);
      return insets;
    });

    TextView accountName = findViewById(R.id.tvAccountName);
    TextView accountEmail = findViewById(R.id.tvAccountEmail);
    accountName.setText(sessionManager.getFullName());
    accountEmail.setText(sessionManager.getEmail());

    findViewById(R.id.btnManagementWorkspace)
        .setOnClickListener(
            view -> startActivity(new Intent(this, ManagementDashboardActivity.class)));
    findViewById(R.id.btnLogout).setOnClickListener(view -> {
      sessionManager.clearSession();
      openAuthentication();
    });
    CustomerNavigation.bind(this, CustomerNavigation.PROFILE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (customerRepository == null || sessionManager == null)
      return;
    ((TextView) findViewById(R.id.tvActiveCount))
        .setText(String.format(java.util.Locale.US, "%02d",
            customerRepository.countActiveAppointments(sessionManager.getUserId())));
    ((TextView) findViewById(R.id.tvCompletedCount))
        .setText(String.format(java.util.Locale.US, "%02d",
            customerRepository.countCompletedAppointments(sessionManager.getUserId())));
    ((TextView) findViewById(R.id.tvSavedDeviceCount))
        .setText(customerRepository.countDistinctDevices(sessionManager.getUserId()) + "  ›");
  }

  @Override
  protected void onDestroy() {
    if (customerRepository != null)
      customerRepository.close();
    super.onDestroy();
  }

  private void openAuthentication() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }
}
