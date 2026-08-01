package com.example.techfix;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AccountActivity extends AppCompatActivity {
  private SessionManager sessionManager;
  private CustomerRepository customerRepository;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    sessionManager = new SessionManager(this);

    if (!sessionManager.isLoggedIn() ||
        FirebaseAuth.getInstance().getCurrentUser() == null) {
      openAuthentication();
      return;
    }

    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_account);
    customerRepository = new CustomerRepository(this);

    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.main), (view, insets) -> {
          Insets systemBars =
              insets.getInsets(WindowInsetsCompat.Type.systemBars());
          view.setPadding(0, systemBars.top, 0, systemBars.bottom);
          return insets;
        });

    TextView accountName = findViewById(R.id.tvAccountName);
    TextView accountEmail = findViewById(R.id.tvAccountEmail);
    accountName.setText(sessionManager.getFullName());
    accountEmail.setText(sessionManager.getEmail());

    View managementWorkspace = findViewById(R.id.btnManagementWorkspace);
    managementWorkspace.setVisibility(View.GONE);
    FirebaseFirestore.getInstance()
        .collection("users")
        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
        .get()
        .addOnSuccessListener(profile -> {
          if ("manager".equals(profile.getString("role"))) {
            sessionManager.setRole(SessionManager.ROLE_MANAGER);
            managementWorkspace.setVisibility(View.VISIBLE);
            managementWorkspace.setOnClickListener(
                view
                -> startActivity(
                    new Intent(this, ManagementDashboardActivity.class)));
          } else {
            sessionManager.setRole(SessionManager.ROLE_CUSTOMER);
          }
        });
    findViewById(R.id.btnLogout).setOnClickListener(view -> {
      FirebaseRealtimeSync.stop();
      FirebaseAuth.getInstance().signOut();
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
    ((TextView)findViewById(R.id.tvActiveCount))
        .setText(String.format(java.util.Locale.US, "%02d",
                               customerRepository.countActiveAppointments(
                                   sessionManager.getUserId())));
    ((TextView)findViewById(R.id.tvCompletedCount))
        .setText(String.format(java.util.Locale.US, "%02d",
                               customerRepository.countCompletedAppointments(
                                   sessionManager.getUserId())));
    ((TextView)findViewById(R.id.tvSavedDeviceCount))
        .setText(customerRepository.countDistinctDevices(
                     sessionManager.getUserId()) +
                 "  ›");
  }

  @Override
  protected void onDestroy() {
    if (customerRepository != null)
      customerRepository.close();
    super.onDestroy();
  }

  private void openAuthentication() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }
}
