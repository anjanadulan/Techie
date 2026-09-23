package com.example.techfixv2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
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

import com.example.techfixv2.models.Branch;
import com.example.techfixv2.models.Technician;
import com.example.techfixv2.models.SparePart;
import com.example.techfixv2.models.RepairedDevice;
import com.example.techfixv2.adapters.RepairGalleryAdapter;
import androidx.viewpager.widget.ViewPager;

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
            btnOpenBranches.setOnClickListener(v -> showBranchSelectorMenu());
        }

        // Click listener for Branch Availability card
        View btnOpenAvailability = findViewById(R.id.openAvailability);
        if (btnOpenAvailability != null) {
            btnOpenAvailability.setOnClickListener(v -> showAvailabilityDialog());
        }

        // Click listener for "View All" services
        View btnOpenServices = findViewById(R.id.openServices);
        if (btnOpenServices != null) {
            btnOpenServices.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerHome.this, Services.class);
                startActivity(intent);
            });
        }

        // Click listener for Search bar container redirect
        View searchServices = findViewById(R.id.searchServices);
        if (searchServices != null) {
            searchServices.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerHome.this, Services.class);
                startActivity(intent);
            });
        }

        // Category Cards Redirect bindings
        View btnCatPhone = findViewById(R.id.catPhoneCard);
        if (btnCatPhone != null) {
            btnCatPhone.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerHome.this, Services.class);
                intent.putExtra("filter_category", "Phone");
                startActivity(intent);
            });
        }

        View btnCatLaptop = findViewById(R.id.catLaptopCard);
        if (btnCatLaptop != null) {
            btnCatLaptop.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerHome.this, Services.class);
                intent.putExtra("filter_category", "Laptop");
                startActivity(intent);
            });
        }

        View btnCatTablet = findViewById(R.id.catTabletCard);
        if (btnCatTablet != null) {
            btnCatTablet.setOnClickListener(v -> {
                Intent intent = new Intent(CustomerHome.this, Services.class);
                intent.putExtra("filter_category", "Tablet");
                startActivity(intent);
            });
        }
    }

    private void showBranchSelectorMenu() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_options, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        TextView btnOpt1 = dialogView.findViewById(R.id.btnOption1);
        TextView btnOpt2 = dialogView.findViewById(R.id.btnOption2);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (tvTitle != null) tvTitle.setText("Find a Branch");
        if (tvMessage != null) tvMessage.setText("View branch details or navigate to service centers using GPS Maps.");

        if (btnOpt1 != null) {
            btnOpt1.setText("View Branch Contact & Hours");
            btnOpt1.setOnClickListener(v -> {
                dialog.dismiss();
                showBranchLocationsDialog();
            });
        }

        if (btnOpt2 != null) {
            btnOpt2.setText("Open GPS Map Navigation");
            btnOpt2.setOnClickListener(v -> {
                dialog.dismiss();
                launchGoogleMaps();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showBranchLocationsDialog() {
        FirebaseFirestore.getInstance().collection("branches").get().addOnCompleteListener(task -> {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_info, null);
            TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
            TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
            View btnAction = dialogView.findViewById(R.id.btnAction);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            if (tvTitle != null) tvTitle.setText("TechFix Centers");
            if (btnAction instanceof TextView) {
                ((TextView) btnAction).setText("Close");
            }
            btnAction.setOnClickListener(v -> dialog.dismiss());

            TextView btnCancel = dialogView.findViewById(R.id.btnCancel);
            if (btnCancel != null) {
                btnCancel.setText("View on Map");
                btnCancel.setVisibility(View.VISIBLE);
                btnCancel.setOnClickListener(v -> {
                    dialog.dismiss();
                    launchGoogleMaps();
                });
            }

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
                if (tvMessage != null) tvMessage.setText(sb.toString().trim());
            } else {
                if (tvMessage != null) {
                    tvMessage.setText("1. Colombo Branch\nAddress: Galle Road, Colombo 03\nContact: 0112345678\n\n2. Galle Branch\nAddress: Wakwella Road, Galle\nContact: 0912345678");
                }
            }
            dialog.show();
        });
    }

    private void launchGoogleMaps() {
        try {
            Intent intent = new Intent(CustomerHome.this, MapsActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to load map screen.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAvailabilityDialog() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("branches").get().addOnCompleteListener(branchTask -> {
            if (branchTask.isSuccessful() && branchTask.getResult() != null) {
                List<DocumentSnapshot> branches = branchTask.getResult().getDocuments();
                
                db.collection("technicians").get().addOnCompleteListener(techTask -> {
                    List<DocumentSnapshot> techs = techTask.isSuccessful() && techTask.getResult() != null ?
                            techTask.getResult().getDocuments() : new ArrayList<>();
                            
                    db.collection("spare_parts").get().addOnCompleteListener(partsTask -> {
                        List<DocumentSnapshot> parts = partsTask.isSuccessful() && partsTask.getResult() != null ?
                                partsTask.getResult().getDocuments() : new ArrayList<>();
                                
                        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_info, null);
                        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
                        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
                        View btnAction = dialogView.findViewById(R.id.btnAction);

                        AlertDialog dialog = new AlertDialog.Builder(this)
                                .setView(dialogView)
                                .create();

                        if (dialog.getWindow() != null) {
                            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                        }

                        if (tvTitle != null) tvTitle.setText("Branch Availability");
                        if (btnAction instanceof TextView) {
                            ((TextView) btnAction).setText("Close");
                        }
                        btnAction.setOnClickListener(v -> dialog.dismiss());

                        // Populate Domain Models and establish 1-to-many aggregations
                        List<Branch> branchList = new ArrayList<>();
                        for (DocumentSnapshot bDoc : branches) {
                            Branch branch = Branch.fromDocument(bDoc);
                            if (branch == null) continue;

                            // Associate roster technicians located at this branch
                            for (DocumentSnapshot tDoc : techs) {
                                Technician tech = Technician.fromDocument(tDoc);
                                if (tech != null && branch.getName().equalsIgnoreCase(tech.getLocation())) {
                                    branch.addTechnician(tech);
                                }
                            }

                            // Associate spare parts stored at this branch
                            for (DocumentSnapshot pDoc : parts) {
                                SparePart part = SparePart.fromDocument(pDoc);
                                if (part != null && branch.getName().equalsIgnoreCase(part.getLocation())) {
                                    branch.addSparePart(part);
                                }
                            }

                            branchList.add(branch);
                        }

                        StringBuilder sb = new StringBuilder();
                        for (Branch b : branchList) {
                            sb.append("📍 ").append(b.getName()).append(" Branch\n")
                              .append("Status: ").append(b.getStatus().toUpperCase()).append("\n")
                              .append("Address: ").append(b.getAddress()).append("\n")
                              .append("Phone: ").append(b.getPhoneNumber()).append("\n\n");

                            // Technicians Roster using Technician domain model
                            sb.append("👨‍🔧 Roster Technicians:\n");
                            if (b.getTechnicians().isEmpty()) {
                                sb.append(" - No technicians registered\n");
                            } else {
                                for (Technician t : b.getTechnicians()) {
                                    sb.append(" - ").append(t.getName()).append(" (").append(t.getAvailability()).append(")\n");
                                }
                            }

                            // Parts Stock using SparePart domain model
                            sb.append("\n📦 Spare-Part Inventory:\n");
                            if (b.getSpareParts().isEmpty()) {
                                sb.append(" - Out of stock\n");
                            } else {
                                for (SparePart p : b.getSpareParts()) {
                                    String stockNote = p.isLowStock() ? " [Low Stock]" : "";
                                    sb.append(" - ").append(p.getName()).append(" (Qty: ").append(p.getQuantity()).append(")").append(stockNote).append("\n");
                                }
                            }

                            sb.append("\n----------------------------------\n\n");
                        }
                        
                        if (tvMessage != null) tvMessage.setText(sb.toString().trim());
                        dialog.show();
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
        // Load recent completed repairs showcase using RepairGalleryAdapter
        loadRepairedDevicesGallery();
    }

    private void loadRepairedDevicesGallery() {
        ViewPager pager = findViewById(R.id.pagerRepairedDevices);
        TextView tvGalleryCounter = findViewById(R.id.tvGalleryCounter);
        if (pager == null) return;

        FirebaseFirestore.getInstance().collection("repair_images")
                .get()
                .addOnCompleteListener(task -> {
                    List<RepairedDevice> devices = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            RepairedDevice device = RepairedDevice.fromDocument(doc);
                            if (device != null) {
                                devices.add(device);
                            }
                        }
                    } else {
                        // Fallback defaults if database collection is empty
                        devices.add(new RepairedDevice("1", "iPhone 13 Pro OLED Display", "Phone", "Colombo",
                                "Cracked display replaced with genuine OEM panel. Restored 120Hz ProMotion touch response.", 18500, "", "Completed"));
                        devices.add(new RepairedDevice("2", "MacBook Pro M1 Keyboard & Cleaning", "Laptop", "Colombo",
                                "Sticky scissor keys replaced and motherboard ultrasonic cleaned following tea spill.", 28500, "", "Completed"));
                        devices.add(new RepairedDevice("3", "iPad Air 4 Battery Replacement", "Tablet", "Galle",
                                "Swollen degraded battery replaced with new OEM cell. Battery health restored to 100%.", 12200, "", "Completed"));

                        // Seed into Firestore so Admin can see them in Management module too!
                        for (RepairedDevice d : devices) {
                            FirebaseFirestore.getInstance().collection("repair_images").add(d.toMap());
                        }
                    }

                    RepairGalleryAdapter adapter = new RepairGalleryAdapter(CustomerHome.this, devices);
                    pager.setAdapter(adapter);

                    if (tvGalleryCounter != null) {
                        tvGalleryCounter.setText("1 of " + devices.size() + " ›");
                    }

                    pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                        @Override
                        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

                        @Override
                        public void onPageSelected(int position) {
                            if (tvGalleryCounter != null) {
                                tvGalleryCounter.setText((position + 1) + " of " + devices.size() + " ›");
                            }
                        }

                        @Override
                        public void onPageScrollStateChanged(int state) {}
                    });
                });
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