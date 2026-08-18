package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ManagementModuleActivity extends AppCompatActivity {

    private static class FirestoreItem {
        String id;
        String title;
        String subtitle;
        String status;
        Map<String, Object> rawData;

        FirestoreItem(String id, String title, String subtitle, String status, Map<String, Object> rawData) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.status = status;
            this.rawData = rawData;
        }
    }

    private TextView tvModuleEyebrow, tvModuleTitle, tvModuleMetric, tvModuleMetricLabel, tvModuleTrend, tvModuleSection;
    private LinearLayout managementList;
    private SwipeRefreshLayout refreshLayout;
    private View btnModuleAdd;
    
    // Filter controls
    private TextView filterAll, filterColombo, filterGalle;
    private String activeFilter = "All";

    private FirebaseFirestore db;
    private String moduleKey = "";
    private boolean isCrud = false;
    
    private final ArrayList<FirestoreItem> loadedItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_management_module);

        db = FirebaseFirestore.getInstance();

        // Bind layout headers
        tvModuleEyebrow = findViewById(R.id.tvModuleEyebrow);
        tvModuleTitle = findViewById(R.id.tvModuleTitle);
        tvModuleMetric = findViewById(R.id.tvModuleMetric);
        tvModuleMetricLabel = findViewById(R.id.tvModuleMetricLabel);
        tvModuleTrend = findViewById(R.id.tvModuleTrend);
        tvModuleSection = findViewById(R.id.tvModuleSection);
        managementList = findViewById(R.id.managementList);
        refreshLayout = findViewById(R.id.refreshLayout);
        btnModuleAdd = findViewById(R.id.btnModuleAdd);

        // Bind filter chips
        filterAll = findViewById(R.id.filterAll);
        filterColombo = findViewById(R.id.filterColombo);
        filterGalle = findViewById(R.id.filterGalle);

        // Parse key
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("module_key")) {
            moduleKey = intent.getStringExtra("module_key");
        }

        // Determine CRUD
        isCrud = "parts".equalsIgnoreCase(moduleKey) ||
                 "technicians".equalsIgnoreCase(moduleKey) ||
                 "prices".equalsIgnoreCase(moduleKey) ||
                 "branches".equalsIgnoreCase(moduleKey) ||
                 "categories".equalsIgnoreCase(moduleKey);

        if (isCrud) {
            btnModuleAdd.setVisibility(View.VISIBLE);
        } else {
            btnModuleAdd.setVisibility(View.GONE);
        }

        // Setup Back Button
        findViewById(R.id.btnModuleBack).setOnClickListener(v -> finish());

        // Setup Add Button
        btnModuleAdd.setOnClickListener(v -> showAddDialog());

        // Setup Filter Click Listeners
        filterAll.setOnClickListener(v -> updateFilterState("All"));
        filterColombo.setOnClickListener(v -> updateFilterState("Colombo"));
        filterGalle.setOnClickListener(v -> updateFilterState("Galle"));

        // Setup Swipe Refresh
        refreshLayout.setOnRefreshListener(this::loadModuleData);

        // Load data
        loadModuleData();
    }

    private void updateFilterState(String newFilter) {
        activeFilter = newFilter;

        // Reset all filter views to inactive style
        filterAll.setBackgroundResource(R.drawable.bg_management_chip);
        filterAll.setTextColor(getResources().getColor(R.color.management_muted));
        filterColombo.setBackgroundResource(R.drawable.bg_management_chip);
        filterColombo.setTextColor(getResources().getColor(R.color.management_muted));
        filterGalle.setBackgroundResource(R.drawable.bg_management_chip);
        filterGalle.setTextColor(getResources().getColor(R.color.management_muted));

        // Apply active background and text colors
        if ("All".equalsIgnoreCase(newFilter)) {
            filterAll.setBackgroundResource(R.drawable.bg_management_chip_active);
            filterAll.setTextColor(getResources().getColor(R.color.management_cyan));
        } else if ("Colombo".equalsIgnoreCase(newFilter)) {
            filterColombo.setBackgroundResource(R.drawable.bg_management_chip_active);
            filterColombo.setTextColor(getResources().getColor(R.color.management_cyan));
        } else if ("Galle".equalsIgnoreCase(newFilter)) {
            filterGalle.setBackgroundResource(R.drawable.bg_management_chip_active);
            filterGalle.setTextColor(getResources().getColor(R.color.management_cyan));
        }

        // Render matching rows locally
        renderList();
    }

    private String getCollectionName() {
        if (moduleKey == null) return "unknown";
        switch (moduleKey.toLowerCase()) {
            case "appointments":
            case "statuses": return "appointments";
            case "technicians": return "technicians";
            case "branches": return "branches";
            case "categories": return "device_categories";
            case "prices": return "service_prices";
            case "parts": return "spare_parts";
            case "images": return "repair_images";
            case "payments": return "payments";
            default: return "unknown";
        }
    }

    private void loadModuleData() {
        refreshLayout.setRefreshing(true);
        String collection = getCollectionName();

        setupHeaderTitles();

        db.collection(collection)
                .get()
                .addOnCompleteListener(task -> {
                    refreshLayout.setRefreshing(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        loadedItems.clear();
                        if (task.getResult().isEmpty()) {
                            seedInitialData(collection);
                        } else {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Map<String, Object> data = doc.getData();
                                FirestoreItem item = parseDocumentToItem(doc.getId(), data);
                                loadedItems.add(item);
                            }
                            // Render list using active filter
                            renderList();
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Failed to load database: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void renderList() {
        managementList.removeAllViews();
        int visibleCount = 0;

        for (int i = 0; i < loadedItems.size(); i++) {
            FirestoreItem item = loadedItems.get(i);
            boolean matchesFilter = false;

            if ("All".equalsIgnoreCase(activeFilter)) {
                matchesFilter = true;
            } else {
                String itemLoc = "";
                Map<String, Object> data = item.rawData;

                // Scan parameters in priority order
                if (data.containsKey("location") && data.get("location") != null) {
                    itemLoc = String.valueOf(data.get("location"));
                } else if (data.containsKey("branch") && data.get("branch") != null) {
                    itemLoc = String.valueOf(data.get("branch"));
                } else if (data.containsKey("name") && data.get("name") != null) {
                    itemLoc = String.valueOf(data.get("name"));
                }

                if (itemLoc.toLowerCase().contains(activeFilter.toLowerCase())) {
                    matchesFilter = true;
                }
            }

            if (matchesFilter) {
                addListItem(item.title, item.subtitle, item.status, i);
                visibleCount++;
            }
        }

        // Update the metric counter label dynamically
        tvModuleMetric.setText(String.valueOf(visibleCount));
    }

    private void setupHeaderTitles() {
        if (moduleKey == null) return;
        switch (moduleKey.toLowerCase()) {
            case "appointments":
                tvModuleEyebrow.setText("OPERATIONS");
                tvModuleTitle.setText("Appointments");
                tvModuleMetricLabel.setText("active bookings");
                tvModuleTrend.setText("STABLE");
                tvModuleSection.setText("Current queue");
                break;
            case "technicians":
                tvModuleEyebrow.setText("TEAM MANAGEMENT");
                tvModuleTitle.setText("Technicians");
                tvModuleMetricLabel.setText("technicians registered");
                tvModuleTrend.setText("DUTY ACTIVE");
                tvModuleSection.setText("Staff rosters");
                break;
            case "branches":
                tvModuleEyebrow.setText("LOCATIONS");
                tvModuleTitle.setText("Branches");
                tvModuleMetricLabel.setText("total outlets");
                tvModuleTrend.setText("STABLE");
                tvModuleSection.setText("Branch offices");
                break;
            case "categories":
                tvModuleEyebrow.setText("INVENTORY TYPES");
                tvModuleTitle.setText("Device Categories");
                tvModuleMetricLabel.setText("active categories");
                tvModuleTrend.setText("STABLE");
                tvModuleSection.setText("Supported categories");
                break;
            case "prices":
                tvModuleEyebrow.setText("RATES CONTROL");
                tvModuleTitle.setText("Service Prices");
                tvModuleMetricLabel.setText("services configured");
                tvModuleTrend.setText("UPDATED");
                tvModuleSection.setText("Price catalog");
                break;
            case "parts":
                tvModuleEyebrow.setText("STOCKS CONTROL");
                tvModuleTitle.setText("Spare Parts");
                tvModuleMetricLabel.setText("parts registered");
                tvModuleTrend.setText("INVENTORY");
                tvModuleSection.setText("In-stock items");
                break;
            case "images":
                tvModuleEyebrow.setText("MEDIA FILES");
                tvModuleTitle.setText("Repair Gallery");
                tvModuleMetricLabel.setText("images uploaded");
                tvModuleTrend.setText("ONLINE");
                tvModuleSection.setText("Repair screenshots");
                break;
            case "payments":
                tvModuleEyebrow.setText("FINANCES CONTROL");
                tvModuleTitle.setText("Payments Ledger");
                tvModuleMetricLabel.setText("invoice history");
                tvModuleTrend.setText("REVENUE");
                tvModuleSection.setText("Receipt logs");
                break;
            case "statuses":
                tvModuleEyebrow.setText("WORKFLOW");
                tvModuleTitle.setText("Repair Status");
                tvModuleMetricLabel.setText("workflow steps");
                tvModuleTrend.setText("STABLE");
                tvModuleSection.setText("Status mapping");
                break;
        }
    }

    private FirestoreItem parseDocumentToItem(String id, Map<String, Object> data) {
        String title = "Item";
        String subtitle = "Details";
        String status = "Active";

        if (moduleKey == null) return new FirestoreItem(id, title, subtitle, status, data);

        switch (moduleKey.toLowerCase()) {
            case "parts":
                title = String.valueOf(data.get("name"));
                subtitle = "Category: " + data.get("category") + " · Loc: " + data.get("location") + " · Qty: " + data.get("quantity") + " · Price: LKR " + data.get("price");
                status = Integer.parseInt(String.valueOf(data.get("quantity"))) <= 2 ? "Low Stock" : "In Stock";
                break;

            case "technicians":
                title = String.valueOf(data.get("name"));
                subtitle = "Loc: " + data.get("location") + " · Specials: " + data.get("specialCategory") + " · Tel: " + data.get("mobileNumber");
                status = String.valueOf(data.get("availability"));
                break;

            case "prices":
                title = String.valueOf(data.get("name"));
                subtitle = "Category: " + data.get("category") + " · Est: " + data.get("estimatedTime") + " · Price: LKR " + data.get("estimatedPrice");
                status = "active".equalsIgnoreCase(String.valueOf(data.get("status"))) ? "Active" : "Inactive";
                break;

            case "branches":
                title = String.valueOf(data.get("name")) + " Branch";
                subtitle = "Addr: " + data.get("address") + " · Tel: " + data.get("phoneNumber");
                status = "open".equalsIgnoreCase(String.valueOf(data.get("status"))) ? "Open" : "Closed";
                break;

            case "categories":
                title = String.valueOf(data.get("categoryName"));
                subtitle = "Location: " + data.get("location");
                status = "active".equalsIgnoreCase(String.valueOf(data.get("status"))) ? "Active" : "Inactive";
                break;

            case "appointments":
            case "statuses":
                title = "Client: " + data.get("clientName");
                subtitle = "Device: " + data.get("deviceName") + " · Desc: " + data.get("description") + " · Est: LKR " + data.get("cost");
                status = String.valueOf(data.get("status"));
                break;

            case "payments":
                title = "Bill: " + data.get("invoiceNo");
                subtitle = "Customer: " + data.get("customer") + " · LKR: " + data.get("amount");
                status = String.valueOf(data.get("paymentStatus"));
                break;

            default:
                title = String.valueOf(data.get("name") != null ? data.get("name") : id);
                subtitle = String.valueOf(data.get("description") != null ? data.get("description") : "No description");
                status = String.valueOf(data.get("status") != null ? data.get("status") : "Active");
                break;
        }

        return new FirestoreItem(id, title, subtitle, status, data);
    }

    private void addListItem(String title, String subtitle, String status, int index) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_management_row, managementList, false);

        TextView tvRowTitle = row.findViewById(R.id.tvRowTitle);
        TextView tvRowSubtitle = row.findViewById(R.id.tvRowSubtitle);
        TextView tvRowStatus = row.findViewById(R.id.tvRowStatus);

        tvRowTitle.setText(title);
        tvRowSubtitle.setText(subtitle);
        tvRowStatus.setText(status);

        if ("Pending".equalsIgnoreCase(status) || "Low Stock".equalsIgnoreCase(status) || "Busy".equalsIgnoreCase(status) || "Closed".equalsIgnoreCase(status) || "Inactive".equalsIgnoreCase(status) || "Off Duty".equalsIgnoreCase(status)) {
            tvRowStatus.setBackgroundResource(R.drawable.bg_management_status_warning);
            tvRowStatus.setTextColor(getResources().getColor(R.color.management_amber));
        } else if ("Completed".equalsIgnoreCase(status) || "Active".equalsIgnoreCase(status) || "Paid".equalsIgnoreCase(status) || "In Stock".equalsIgnoreCase(status) || "Open".equalsIgnoreCase(status) || "On Duty".equalsIgnoreCase(status)) {
            tvRowStatus.setBackgroundResource(R.drawable.bg_management_status);
            tvRowStatus.setTextColor(getResources().getColor(R.color.management_green));
        } else {
            tvRowStatus.setBackgroundResource(R.drawable.bg_management_status);
            tvRowStatus.setTextColor(getResources().getColor(R.color.management_cyan));
        }

        row.setOnClickListener(v -> {
            if ("statuses".equalsIgnoreCase(moduleKey)) {
                showStatusUpdateDialog(index);
            } else if (isCrud) {
                showEditDeleteDialog(index);
            } else {
                showViewDetailsDialog(index);
            }
        });

        managementList.addView(row);
    }

    private void showAddDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Add new " + tvModuleTitle.getText().toString());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final ArrayList<EditText> inputs = new ArrayList<>();
        final String[] fields = getFieldsForModule();

        for (String field : fields) {
            EditText et = new EditText(this);
            et.setHint(formatFieldName(field));
            if ("quantity".equals(field) || "price".equals(field) || "estimatedPrice".equals(field) || "amount".equals(field)) {
                et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
            layout.addView(et);
            inputs.add(et);
        }

        builder.setView(layout);

        builder.setPositiveButton("Create", (dialog, which) -> {
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                String val = inputs.get(i).getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("quantity".equals(fields[i])) {
                    data.put(fields[i], Integer.parseInt(val));
                } else if ("price".equals(fields[i]) || "estimatedPrice".equals(fields[i])) {
                    data.put(fields[i], Double.parseDouble(val));
                } else {
                    data.put(fields[i], val);
                }
            }

            refreshLayout.setRefreshing(true);
            db.collection(getCollectionName())
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        loadModuleData();
                        Toast.makeText(this, "Created in Firestore!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditDeleteDialog(int index) {
        if (index < 0 || index >= loadedItems.size()) return;
        FirestoreItem item = loadedItems.get(index);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Edit " + tvModuleTitle.getText().toString());

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final ArrayList<EditText> inputs = new ArrayList<>();
        final String[] fields = getFieldsForModule();

        for (String field : fields) {
            EditText et = new EditText(this);
            et.setHint(formatFieldName(field));
            Object currentVal = item.rawData.get(field);
            et.setText(currentVal != null ? String.valueOf(currentVal) : "");
            
            if ("quantity".equals(field) || "price".equals(field) || "estimatedPrice".equals(field)) {
                et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }
            layout.addView(et);
            inputs.add(et);
        }

        builder.setView(layout);

        builder.setPositiveButton("Update", (dialog, which) -> {
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                String val = inputs.get(i).getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("quantity".equals(fields[i])) {
                    data.put(fields[i], Integer.parseInt(val));
                } else if ("price".equals(fields[i]) || "estimatedPrice".equals(fields[i])) {
                    data.put(fields[i], Double.parseDouble(val));
                } else {
                    data.put(fields[i], val);
                }
            }

            refreshLayout.setRefreshing(true);
            db.collection(getCollectionName()).document(item.id)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        loadModuleData();
                        Toast.makeText(this, "Updated in Firestore!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNeutralButton("Delete", (dialog, which) -> {
            refreshLayout.setRefreshing(true);
            db.collection(getCollectionName()).document(item.id)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        loadModuleData();
                        Toast.makeText(this, "Deleted from Firestore!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showViewDetailsDialog(int index) {
        if (index < 0 || index >= loadedItems.size()) return;
        FirestoreItem item = loadedItems.get(index);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_info, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        View btnAction = dialogView.findViewById(R.id.btnAction);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (tvTitle != null) {
            tvTitle.setText(tvModuleTitle.getText().toString() + " Details");
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : item.rawData.entrySet()) {
            sb.append("• ").append(formatFieldName(entry.getKey())).append(": ").append(entry.getValue()).append("\n\n");
        }

        if (tvMessage != null) {
            tvMessage.setText(sb.toString().trim());
        }

        if (btnAction instanceof TextView) {
            ((TextView) btnAction).setText("Close");
        }
        btnAction.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showStatusUpdateDialog(int index) {
        if (index < 0 || index >= loadedItems.size()) return;
        FirestoreItem item = loadedItems.get(index);

        final String[] statusOptions = {"Pending", "Approved", "In Progress", "Completed"};
        
        int checkedItem = 0;
        for (int i = 0; i < statusOptions.length; i++) {
            if (statusOptions[i].equalsIgnoreCase(item.status)) {
                checkedItem = i;
                break;
            }
        }

        final int[] selectedIndex = {checkedItem};

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Update Repair Status");
        builder.setSingleChoiceItems(statusOptions, checkedItem, (dialog, which) -> {
            selectedIndex[0] = which;
        });

        builder.setPositiveButton("Update", (dialog, which) -> {
            String newStatus = statusOptions[selectedIndex[0]];
            
            refreshLayout.setRefreshing(true);
            db.collection("appointments").document(item.id)
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        loadModuleData();
                        Toast.makeText(this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private String formatFieldName(String databaseKey) {
        if (databaseKey == null) return "Field";
        switch (databaseKey) {
            case "name": return "Name";
            case "category": return "Category (laptop, iphone, android, tablet)";
            case "location": return "Location (Colombo / Galle)";
            case "quantity": return "Quantity";
            case "price": return "Price (LKR)";
            case "specialCategory": return "Special Category (e.g. laptop, iphone)";
            case "availability": return "Availability (On Duty / Off Duty)";
            case "mobileNumber": return "Mobile Number";
            case "estimatedTime": return "Estimated Time (e.g. 2 hours)";
            case "estimatedPrice": return "Estimated Price (LKR)";
            case "status": return "Status";
            case "address": return "Address";
            case "phoneNumber": return "Phone Number";
            case "categoryName": return "Category Name";
            case "clientName": return "Client Name";
            case "deviceName": return "Device Name";
            case "description": return "Description";
            case "cost": return "Cost";
            case "invoiceNo": return "Invoice Number";
            case "customer": return "Customer Name";
            case "amount": return "Amount (LKR)";
            case "paymentStatus": return "Payment Status";
            default: return databaseKey;
        }
    }

    private String[] getFieldsForModule() {
        if (moduleKey == null) return new String[]{};
        switch (moduleKey.toLowerCase()) {
            case "parts":
                return new String[]{"name", "category", "location", "quantity", "price"};
            case "technicians":
                return new String[]{"name", "location", "specialCategory", "availability", "mobileNumber"};
            case "prices":
                return new String[]{"name", "category", "estimatedTime", "estimatedPrice", "status"};
            case "branches":
                return new String[]{"name", "address", "phoneNumber", "status"};
            case "categories":
                return new String[]{"categoryName", "location", "status"};
            default:
                return new String[]{"name", "description", "status"};
        }
    }

    private void seedInitialData(String collection) {
        refreshLayout.setRefreshing(true);
        ArrayList<Map<String, Object>> mockList = new ArrayList<>();

        if ("spare_parts".equals(collection)) {
            mockList.add(createPartMap("iPhone 12 Screen Panels", "iphone", "Colombo", 5, 12000));
            mockList.add(createPartMap("MacBook Pro Keyboards", "laptop", "Colombo", 2, 8500));
            mockList.add(createPartMap("iPad Pro Batteries", "tablet", "Galle", 1, 6500));
        } else if ("technicians".equals(collection)) {
            mockList.add(createTechMap("Nilantha Kumara", "Colombo", "laptop, tablet", "On Duty", "0771234567"));
            mockList.add(createTechMap("Ruwan Silva", "Colombo", "iphone, android", "On Duty", "0777654321"));
            mockList.add(createTechMap("Kasun Perera", "Galle", "laptop, iphone", "Off Duty", "0711122334"));
        } else if ("service_prices".equals(collection)) {
            mockList.add(createPriceMap("Device Diagnosis", "laptop", "1 hour", 1500, "active"));
            mockList.add(createPriceMap("Keyboard Repair", "laptop", "3 hours", 8500, "active"));
            mockList.add(createPriceMap("Screen Replace", "iphone", "2 hours", 12000, "active"));
        } else if ("branches".equals(collection)) {
            mockList.add(createBranchMap("Colombo", "Galle Road, Colombo 03", "0112345678", "open"));
            mockList.add(createBranchMap("Galle", "Wakwella Road, Galle", "0912345678", "open"));
        } else if ("device_categories".equals(collection)) {
            mockList.add(createCategoryMap("laptop", "Colombo", "active"));
            mockList.add(createCategoryMap("iphone", "Colombo", "active"));
            mockList.add(createCategoryMap("android", "Galle", "active"));
            mockList.add(createCategoryMap("tablet", "Galle", "active"));
        } else if ("appointments".equals(collection)) {
            mockList.add(createAppointmentMap("Nimal Perera", "user@gmail.com", "MacBook Air M1", "Keyboard replacement", 15000, "Pending"));
            mockList.add(createAppointmentMap("Sunil Silva", "user@gmail.com", "iPhone 13 Pro", "OLED screen replacement", 35000, "In Progress"));
        } else if ("payments".equals(collection)) {
            mockList.add(createPaymentMap("INV-1038", "Nimal Perera", 8500, "Paid"));
            mockList.add(createPaymentMap("INV-1041", "Sunil Silva", 15000, "Paid"));
        }

        if (mockList.isEmpty()) {
            refreshLayout.setRefreshing(false);
            return;
        }

        for (int i = 0; i < mockList.size(); i++) {
            final int count = i;
            db.collection(collection)
                    .add(mockList.get(i))
                    .addOnCompleteListener(t -> {
                        if (count == mockList.size() - 1) {
                            loadModuleData();
                        }
                    });
        }
    }

    private Map<String, Object> createPartMap(String name, String cat, String loc, int qty, double price) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("category", cat);
        map.put("location", loc);
        map.put("quantity", qty);
        map.put("price", price);
        return map;
    }

    private Map<String, Object> createTechMap(String name, String loc, String cat, String avail, String num) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("location", loc);
        map.put("specialCategory", cat);
        map.put("availability", avail);
        map.put("mobileNumber", num);
        return map;
    }

    private Map<String, Object> createPriceMap(String name, String cat, String time, double price, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("category", cat);
        map.put("estimatedTime", time);
        map.put("estimatedPrice", price);
        map.put("status", status);
        return map;
    }

    private Map<String, Object> createBranchMap(String name, String addr, String tel, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("address", addr);
        map.put("phoneNumber", tel);
        map.put("status", status);
        return map;
    }

    private Map<String, Object> createCategoryMap(String catName, String loc, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("categoryName", catName);
        map.put("location", loc);
        map.put("status", status);
        return map;
    }

    private Map<String, Object> createAppointmentMap(String client, String email, String device, String desc, double cost, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("clientName", client);
        map.put("userEmail", email);
        map.put("deviceName", device);
        map.put("description", desc);
        map.put("cost", cost);
        map.put("status", status);
        return map;
    }

    private Map<String, Object> createPaymentMap(String inv, String client, double amt, String status) {
        Map<String, Object> map = new HashMap<>();
        map.put("invoiceNo", inv);
        map.put("customer", client);
        map.put("amount", amt);
        map.put("paymentStatus", status);
        return map;
    }
}
