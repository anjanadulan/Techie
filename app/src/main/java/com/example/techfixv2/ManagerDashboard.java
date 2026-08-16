package com.example.techfixv2;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ManagerDashboard extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Navigate to Admin Profile
        findViewById(R.id.btnAdminProfile).setOnClickListener(v -> {
            Intent intent = new Intent(ManagerDashboard.this, AdminProfile.class);
            startActivity(intent);
        });

        // Setup click listeners for the 9 operational modules
        setupModuleNavigation(R.id.manageAppointments, "appointments");
        setupModuleNavigation(R.id.manageTechnicians, "technicians");
        setupModuleNavigation(R.id.manageBranches, "branches");
        setupModuleNavigation(R.id.manageCategories, "categories");
        setupModuleNavigation(R.id.managePrices, "prices");
        setupModuleNavigation(R.id.manageParts, "parts");
        setupModuleNavigation(R.id.manageImages, "images");
        setupModuleNavigation(R.id.managePayments, "payments");
        setupModuleNavigation(R.id.manageStatuses, "statuses");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh all dashboard metrics, recent logs, and card descriptions
        loadDashboardMetrics();
        loadRecentActivity();
        loadModuleCardsMetrics();
    }

    private void loadDashboardMetrics() {
        TextView tvDashboardActive = findViewById(R.id.tvDashboardActive);
        TextView tvDashboardReady = findViewById(R.id.tvDashboardReady);

        if (tvDashboardActive == null || tvDashboardReady == null) return;

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int totalAppointments = task.getResult().size();
                        int inProgressAppointments = 0;

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            if ("In Progress".equalsIgnoreCase(status)) {
                                inProgressAppointments++;
                            }
                        }

                        tvDashboardActive.setText(String.format("%02d", totalAppointments));
                        tvDashboardReady.setText(String.format("%02d", inProgressAppointments));
                    }
                });
    }

    private void loadRecentActivity() {
        TextView tvDashboardActivityOne = findViewById(R.id.tvDashboardActivityOne);
        TextView tvDashboardActivityOneMeta = findViewById(R.id.tvDashboardActivityOneMeta);
        TextView tvDashboardActivityTwo = findViewById(R.id.tvDashboardActivityTwo);
        TextView tvDashboardActivityTwoMeta = findViewById(R.id.tvDashboardActivityTwoMeta);

        if (tvDashboardActivityOne == null || tvDashboardActivityTwo == null) return;

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        int size = task.getResult().size();
                        DocumentSnapshot doc = task.getResult().getDocuments().get(size - 1);

                        String clientName = doc.getString("clientName");
                        String deviceName = doc.getString("deviceName");
                        String branch = doc.getString("branch");
                        String status = doc.getString("status");

                        tvDashboardActivityOne.setText("New Booking: " + (deviceName != null ? deviceName : "Device"));
                        tvDashboardActivityOneMeta.setText((clientName != null ? clientName : "Client") + " · " + (branch != null ? branch : "Colombo") + " · " + (status != null ? status : "Pending"));
                    } else {
                        tvDashboardActivityOne.setText("No recent bookings");
                        tvDashboardActivityOneMeta.setText("Appointments queue is empty");
                    }
                });

        FirebaseFirestore.getInstance()
                .collection("payments")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        int size = task.getResult().size();
                        DocumentSnapshot doc = task.getResult().getDocuments().get(size - 1);

                        String invoiceNo = doc.getString("invoiceNo");
                        String customer = doc.getString("customer");
                        Object amount = doc.get("amount");
                        String status = doc.getString("paymentStatus");

                        tvDashboardActivityTwo.setText("Payment logged: " + (invoiceNo != null ? invoiceNo : "INV"));
                        tvDashboardActivityTwoMeta.setText((customer != null ? customer : "Client") + " · LKR " + (amount != null ? String.valueOf(amount) : "0") + " · " + (status != null ? status : "Paid"));
                    } else {
                        tvDashboardActivityTwo.setText("No recent payments");
                        tvDashboardActivityTwoMeta.setText("Payment ledger is empty");
                    }
                });
    }

    private void loadModuleCardsMetrics() {
        FirebaseFirestore fs = FirebaseFirestore.getInstance();

        // 1. Appointments Card
        TextView tvDashboardAppointmentsMeta = findViewById(R.id.tvDashboardAppointmentsMeta);
        if (tvDashboardAppointmentsMeta != null) {
            fs.collection("appointments").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    tvDashboardAppointmentsMeta.setText(task.getResult().size() + " bookings");
                }
            });
        }

        // 2. Technicians Card
        TextView tvDashboardTechniciansMeta = findViewById(R.id.tvDashboardTechniciansMeta);
        if (tvDashboardTechniciansMeta != null) {
            fs.collection("technicians").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    int total = task.getResult().size();
                    int active = 0;
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        String availability = doc.getString("availability");
                        if ("On Duty".equalsIgnoreCase(availability) || "Active".equalsIgnoreCase(availability)) {
                            active++;
                        }
                    }
                    tvDashboardTechniciansMeta.setText(total + " staff · " + active + " active");
                }
            });
        }

        // 3. Branches Card
        TextView tvDashboardBranchesMeta = findViewById(R.id.tvDashboardBranchesMeta);
        if (tvDashboardBranchesMeta != null) {
            fs.collection("branches").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    tvDashboardBranchesMeta.setText(task.getResult().size() + " branches");
                }
            });
        }

        // 4. Categories Card
        TextView tvDashboardCategoriesMeta = findViewById(R.id.tvDashboardCategoriesMeta);
        if (tvDashboardCategoriesMeta != null) {
            fs.collection("device_categories").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    tvDashboardCategoriesMeta.setText(task.getResult().size() + " categories");
                }
            });
        }

        // 5. Prices Card
        TextView tvDashboardPricesMeta = findViewById(R.id.tvDashboardPricesMeta);
        if (tvDashboardPricesMeta != null) {
            fs.collection("service_prices").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    tvDashboardPricesMeta.setText(task.getResult().size() + " services");
                }
            });
        }

        // 6. Parts Card & Top Stock Metric Counter
        TextView tvDashboardPartsMeta = findViewById(R.id.tvDashboardPartsMeta);
        TextView tvDashboardLowStock = findViewById(R.id.tvDashboardLowStock);
        if (tvDashboardPartsMeta != null || tvDashboardLowStock != null) {
            fs.collection("spare_parts").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    int totalQuantity = 0;
                    int lowStockCount = 0;

                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Object qtyObj = doc.get("quantity");
                        if (qtyObj != null) {
                            int qty = Integer.parseInt(String.valueOf(qtyObj));
                            totalQuantity += qty;
                            if (qty <= 2) {
                                lowStockCount++;
                            }
                        }
                    }

                    if (tvDashboardPartsMeta != null) {
                        tvDashboardPartsMeta.setText(totalQuantity + " units · " + lowStockCount + " low");
                    }
                    if (tvDashboardLowStock != null) {
                        tvDashboardLowStock.setText(String.format("%02d", lowStockCount));
                    }
                }
            });
        }

        // 7. Images Card
        TextView tvDashboardImagesMeta = findViewById(R.id.tvDashboardImagesMeta);
        if (tvDashboardImagesMeta != null) {
            fs.collection("repair_images").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    tvDashboardImagesMeta.setText(task.getResult().size() + " uploads");
                }
            });
        }

        // 8. Payments Card
        TextView tvDashboardPaymentsMeta = findViewById(R.id.tvDashboardPaymentsMeta);
        if (tvDashboardPaymentsMeta != null) {
            fs.collection("payments").get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    double totalRevenue = 0;
                    for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Object amtObj = doc.get("amount");
                        if (amtObj != null) {
                            totalRevenue += Double.parseDouble(String.valueOf(amtObj));
                        }
                    }
                    tvDashboardPaymentsMeta.setText("LKR " + (int) totalRevenue + " total");
                }
            });
        }
    }

    private void setupModuleNavigation(int viewId, String moduleKey) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> {
                Intent intent = new Intent(ManagerDashboard.this, ManagementModuleActivity.class);
                intent.putExtra("module_key", moduleKey);
                startActivity(intent);
            });
        }
    }
}