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
import androidx.appcompat.app.AlertDialog;
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

import java.util.ArrayList;
import java.util.List;

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

        // Click listener for Find a Branch card
        View btnOpenBranches = findViewById(R.id.openBranches);
        if (btnOpenBranches != null) {
            btnOpenBranches.setOnClickListener(v -> showBranchLocationsDialog());
        }

        // Click listener for Branch Availability card
        View btnOpenAvailability = findViewById(R.id.openAvailability);
        if (btnOpenAvailability != null) {
            btnOpenAvailability.setOnClickListener(v -> showAvailabilityDialog());
        }
    }

    private void showBranchLocationsDialog() {
        FirebaseFirestore.getInstance().collection("branches").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String name = doc.getString("name");
                    String address = doc.getString("address");
                    String phone = doc.getString("phoneNumber");
                    String status = doc.getString("status");

                    sb.append("📍 ").append(name != null ? name : "Branch").append(" Branch\n")
                      .append("Address: ").append(address != null ? address : "N/A").append("\n")
                      .append("Contact: ").append(phone != null ? phone : "N/A").append("\n")
                      .append("Status: ").append(status != null ? status.toUpperCase() : "OPEN").append("\n\n");
                }
                new AlertDialog.Builder(this)
                        .setTitle("TechFix Centers near you")
                        .setMessage(sb.toString().trim())
                        .setPositiveButton("Close", null)
                        .show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("TechFix Branches")
                        .setMessage("1. Colombo Branch\nAddress: Galle Road, Colombo 03\nContact: 0112345678\n\n2. Galle Branch\nAddress: Wakwella Road, Galle\nContact: 0912345678")
                        .setPositiveButton("Close", null)
                        .show();
            }
        });
    }

    private void showAvailabilityDialog() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("branches").get().addOnCompleteListener(branchTask -> {
            if (branchTask.isSuccessful() && branchTask.getResult() != null) {
                List<DocumentSnapshot> branches = branchTask.getResult().getDocuments();
                
                db.collection("technicians").get().addOnCompleteListener(techTask -> {
                    List<DocumentSnapshot> techs = techTask.isSuccessful() && techTask.getResult() != null ?
                            techTask.getResult().getDocuments() : new ArrayList<>();
                            
                    db.collection("parts").get().addOnCompleteListener(partsTask -> {
                        List<DocumentSnapshot> parts = partsTask.isSuccessful() && partsTask.getResult() != null ?
                                partsTask.getResult().getDocuments() : new ArrayList<>();
                                
                        StringBuilder sb = new StringBuilder();
                        for (DocumentSnapshot bDoc : branches) {
                            String bName = bDoc.getString("name");
                            String bAddr = bDoc.getString("address");
                            String bPhone = bDoc.getString("phoneNumber");
                            String bStatus = bDoc.getString("status");
                            
                            sb.append("📍 ").append(bName != null ? bName : "Branch").append(" Branch\n")
                              .append("Status: ").append(bStatus != null ? bStatus.toUpperCase() : "OPEN").append("\n")
                              .append("Address: ").append(bAddr != null ? bAddr : "N/A").append("\n")
                              .append("Phone: ").append(bPhone != null ? bPhone : "N/A").append("\n\n");
                              
                            // Technicians Roster
                            sb.append("👨‍🔧 Roster Technicians:\n");
                            boolean hasTech = false;
                            for (DocumentSnapshot tDoc : techs) {
                                String tLoc = tDoc.getString("location");
                                if (bName != null && bName.equalsIgnoreCase(tLoc)) {
                                    String tName = tDoc.getString("name");
                                    String tAvail = tDoc.getString("availability");
                                    sb.append(" - ").append(tName).append(" (").append(tAvail != null ? tAvail : "On Duty").append(")\n");
                                    hasTech = true;
                                }
                            }
                            if (!hasTech) sb.append(" - No technicians registered\n");
                            
                            // Parts Stock
                            sb.append("\n📦 Spare-Part Inventory:\n");
                            boolean hasPart = false;
                            for (DocumentSnapshot pDoc : parts) {
                                String pLoc = pDoc.getString("location");
                                if (bName != null && bName.equalsIgnoreCase(pLoc)) {
                                    String pName = pDoc.getString("name");
                                    Object qtyVal = pDoc.get("quantity");
                                    int qty = qtyVal != null ? (int) Double.parseDouble(String.valueOf(qtyVal)) : 0;
                                    sb.append(" - ").append(pName).append(" (Qty: ").append(qty).append(")\n");
                                    hasPart = true;
                                }
                            }
                            if (!hasPart) sb.append(" - Out of stock\n");
                            
                            sb.append("\n----------------------------------\n\n");
                        }
                        
                        new AlertDialog.Builder(this)
                                .setTitle("TechFix Branch Availability")
                                .setMessage(sb.toString().trim())
                                .setPositiveButton("Close", null)
                                .show();
                    });
                });
            } else {
                Toast.makeText(this, "Failed to load branch list.", Toast.LENGTH_SHORT).show();
            }
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