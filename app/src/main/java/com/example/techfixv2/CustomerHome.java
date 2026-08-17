package com.example.techfixv2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerHome extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Request storage permission when landing on the customer home dashboard
        checkStoragePermission();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Bottom Navigation click listener to go to BookingHistory
        findViewById(R.id.navBookings).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHome.this, BookingHistory.class);
            startActivity(intent);
        });

        // Bottom Navigation click listener to go to BookRepairActivity
        findViewById(R.id.navBookRepair).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHome.this, BookRepairActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation click listener to go to Services
        findViewById(R.id.navServices).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHome.this, Services.class);
            startActivity(intent);
        });

        // Bottom Navigation click listener to go to Profile
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHome.this, UserProfile.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load the client's active repair details dynamically
        loadActiveRepairDetails();
        // Load dynamic popular services list
        loadPopularServices();
    }

    private void loadPopularServices() {
        LinearLayout popularServicesContainer = findViewById(R.id.popularServicesContainer);
        if (popularServicesContainer == null) return;
        popularServicesContainer.removeAllViews();

        FirebaseFirestore.getInstance()
                .collection("service_prices")
                .limit(6) // retrieve enough documents to filter down to 3 active ones
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        LayoutInflater inflater = LayoutInflater.from(this);
                        int count = 0;
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            if (status != null && !"active".equalsIgnoreCase(status)) {
                                continue;
                            }

                            String name = doc.getString("name");
                            String category = doc.getString("category");
                            String estTime = doc.getString("estimatedTime");
                            Object priceVal = doc.get("estimatedPrice");

                            View itemView = inflater.inflate(R.layout.item_popular_service, popularServicesContainer, false);
                            
                            ImageView ivIcon = itemView.findViewById(R.id.ivPopularServiceIcon);
                            TextView tvName = itemView.findViewById(R.id.tvPopularServiceName);
                            TextView tvMeta = itemView.findViewById(R.id.tvPopularServiceMeta);

                            if (tvName != null) tvName.setText(name);
                            double price = priceVal != null ? Double.parseDouble(String.valueOf(priceVal)) : 0.0;
                            if (tvMeta != null) {
                                tvMeta.setText("From LKR " + (int) price + " · " + (estTime != null ? estTime : "1-2 hours"));
                            }

                            // Set dynamic icon and color styling matching the device type
                            if (ivIcon != null && category != null) {
                                String catLower = category.toLowerCase();
                                if (catLower.contains("laptop") || catLower.contains("macbook") || catLower.contains("desktop")) {
                                    ivIcon.setImageResource(R.drawable.ic_customer_laptop);
                                    ivIcon.setBackgroundResource(R.drawable.bg_customer_soft_orange);
                                } else {
                                    ivIcon.setImageResource(R.drawable.ic_customer_phone);
                                    ivIcon.setBackgroundResource(R.drawable.bg_customer_soft_blue);
                                }
                            }

                            // Click to pre-book this popular service
                            itemView.setOnClickListener(v -> {
                                Intent intent = new Intent(CustomerHome.this, BookRepairActivity.class);
                                intent.putExtra("preselected_service", name);
                                intent.putExtra("preselected_category", category);
                                intent.putExtra("preselected_cost", price);
                                startActivity(intent);
                            });

                            popularServicesContainer.addView(itemView);
                            count++;
                            if (count >= 3) break;
                        }
                    }
                });
    }

    private void loadActiveRepairDetails() {
        TextView tvActiveRepairId = findViewById(R.id.tvActiveRepairId);
        TextView tvActiveRepairStatus = findViewById(R.id.tvActiveRepairStatus);
        TextView tvActiveRepairSummary = findViewById(R.id.tvActiveRepairSummary);

        if (tvActiveRepairId == null || tvActiveRepairStatus == null || tvActiveRepairSummary == null) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvActiveRepairId.setText("No Session");
            tvActiveRepairStatus.setText("Offline");
            tvActiveRepairSummary.setText("Please sign in to view your repairs.");
            return;
        }

        String email = user.getEmail();
        if (email == null) return;

        // Fetch user's latest appointment from Firestore
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("userEmail", email.trim().toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        DocumentSnapshot activeDoc = null;

                        // Try to find the first in-progress or pending appointment
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            if (!"Completed".equalsIgnoreCase(status)) {
                                activeDoc = doc;
                                break;
                            }
                        }

                        // If all are completed, default to the most recent one
                        if (activeDoc == null) {
                            int size = task.getResult().size();
                            activeDoc = task.getResult().getDocuments().get(size - 1);
                        }

                        String rawId = activeDoc.getId();
                        String repairId = "#TF-" + (rawId.length() > 5 ? rawId.substring(0, 5).toUpperCase() : rawId.toUpperCase());
                        String device = activeDoc.getString("deviceName");
                        String desc = activeDoc.getString("description");
                        String status = activeDoc.getString("status");

                        tvActiveRepairId.setText("Repair ID: " + repairId);
                        tvActiveRepairStatus.setText(status != null ? status : "Pending");
                        tvActiveRepairSummary.setText(device + (desc != null && !desc.isEmpty() ? " · " + desc : ""));

                        // Apply color styles to status badge
                        if ("completed".equalsIgnoreCase(status)) {
                            tvActiveRepairStatus.setBackgroundResource(R.drawable.bg_status_success);
                            tvActiveRepairStatus.setTextColor(getResources().getColor(R.color.customer_success));
                        } else if ("in progress".equalsIgnoreCase(status)) {
                            tvActiveRepairStatus.setBackgroundResource(R.drawable.bg_management_status_warning);
                            tvActiveRepairStatus.setTextColor(getResources().getColor(R.color.customer_orange));
                        } else {
                            tvActiveRepairStatus.setBackgroundResource(R.drawable.bg_customer_chip);
                            tvActiveRepairStatus.setTextColor(getResources().getColor(R.color.customer_muted));
                        }
                    } else {
                        // Display clean placeholder values if no repair is booked
                        tvActiveRepairId.setText("No Active Repairs");
                        tvActiveRepairStatus.setText("Idle");
                        tvActiveRepairStatus.setBackgroundResource(R.drawable.bg_customer_chip);
                        tvActiveRepairStatus.setTextColor(getResources().getColor(R.color.customer_muted));
                        tvActiveRepairSummary.setText("Book a new repair to track your progress in real-time!");
                    }
                });
    }

    private static final int STORAGE_PERMISSION_CODE = 101;

    private void checkStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, STORAGE_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Storage Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Storage Permission Denied.", Toast.LENGTH_LONG).show();
            }
        }
    }
}