package com.example.techfixv2;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
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
    
    // filter controls
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

        // layout headers
        tvModuleEyebrow = findViewById(R.id.tvModuleEyebrow);
        tvModuleTitle = findViewById(R.id.tvModuleTitle);
        tvModuleMetric = findViewById(R.id.tvModuleMetric);
        tvModuleMetricLabel = findViewById(R.id.tvModuleMetricLabel);
        tvModuleTrend = findViewById(R.id.tvModuleTrend);
        tvModuleSection = findViewById(R.id.tvModuleSection);
        managementList = findViewById(R.id.managementList);
        refreshLayout = findViewById(R.id.refreshLayout);
        btnModuleAdd = findViewById(R.id.btnModuleAdd);

        // filter chips
        filterAll = findViewById(R.id.filterAll);
        filterColombo = findViewById(R.id.filterColombo);
        filterGalle = findViewById(R.id.filterGalle);

        // parse key
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("module_key")) {
            moduleKey = intent.getStringExtra("module_key");
        }

        // crud check
        isCrud = "parts".equalsIgnoreCase(moduleKey) ||
                 "technicians".equalsIgnoreCase(moduleKey) ||
                 "prices".equalsIgnoreCase(moduleKey) ||
                 "branches".equalsIgnoreCase(moduleKey) ||
                 "categories".equalsIgnoreCase(moduleKey) ||
                 "images".equalsIgnoreCase(moduleKey);

        if (isCrud) {
            btnModuleAdd.setVisibility(View.VISIBLE);
        } else {
            btnModuleAdd.setVisibility(View.GONE);
        }

        // back btn
        findViewById(R.id.btnModuleBack).setOnClickListener(v -> finish());

        // add btn
        btnModuleAdd.setOnClickListener(v -> showAddDialog());

        // filter listeners
        filterAll.setOnClickListener(v -> updateFilterState("All"));
        filterColombo.setOnClickListener(v -> updateFilterState("Colombo"));
        filterGalle.setOnClickListener(v -> updateFilterState("Galle"));

        // swipe refresh
        refreshLayout.setOnRefreshListener(this::loadModuleData);

        // load data
        loadModuleData();
    }

    private void updateFilterState(String newFilter) {
        activeFilter = newFilter;

        // reset filter style
        filterAll.setBackgroundResource(R.drawable.bg_management_chip);
        filterAll.setTextColor(getResources().getColor(R.color.management_muted));
        filterColombo.setBackgroundResource(R.drawable.bg_management_chip);
        filterColombo.setTextColor(getResources().getColor(R.color.management_muted));
        filterGalle.setBackgroundResource(R.drawable.bg_management_chip);
        filterGalle.setTextColor(getResources().getColor(R.color.management_muted));

        // active filter style
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

        // render rows
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
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot doc : task.getResult()) {
                                Map<String, Object> data = doc.getData();
                                FirestoreItem item = parseDocumentToItem(doc.getId(), data);
                                loadedItems.add(item);
                            }
                        }
                        // render list
                        renderList();
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

                // scan params
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
                if ("appointments".equalsIgnoreCase(moduleKey) || "statuses".equalsIgnoreCase(moduleKey)) {
                    addAppointmentListItem(item, i);
                } else {
                    addListItem(item.title, item.subtitle, item.status, i);
                }
                visibleCount++;
            }
        }

        // update counter
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

            case "images":
                title = String.valueOf(data.get("name"));
                subtitle = "Branch: " + data.get("location") + " · LKR: " + data.get("price") + " · " + data.get("description");
                status = "Completed";
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

    private void addAppointmentListItem(FirestoreItem item, int index) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_appointment_management, managementList, false);

        TextView tvApptId = row.findViewById(R.id.tvApptId);
        TextView tvApptDateTime = row.findViewById(R.id.tvApptDateTime);
        TextView tvApptStatus = row.findViewById(R.id.tvApptStatus);
        TextView tvApptDeviceName = row.findViewById(R.id.tvApptDeviceName);
        ImageView ivApptDeviceIcon = row.findViewById(R.id.ivApptDeviceIcon);
        TextView tvApptBranch = row.findViewById(R.id.tvApptBranch);
        TextView tvApptClient = row.findViewById(R.id.tvApptClient);
        TextView tvApptDescription = row.findViewById(R.id.tvApptDescription);
        TextView tvApptCost = row.findViewById(R.id.tvApptCost);
        TextView tvApptPhotoBadge = row.findViewById(R.id.tvApptPhotoBadge);

        Map<String, Object> data = item.rawData;

        // Repair Tracking ID (#TF-XXXX)
        String repairId = item.id != null && item.id.length() > 5 ? 
                "#TF-" + item.id.substring(0, 5).toUpperCase() : 
                (item.id != null ? "#TF-" + item.id.toUpperCase() : "#TF-0000");
        tvApptId.setText(repairId);

        // Date and Time schedule
        String dateStr = data.containsKey("date") && data.get("date") != null ? String.valueOf(data.get("date")) : "";
        String timeStr = data.containsKey("time") && data.get("time") != null ? String.valueOf(data.get("time")) : "";
        String dateTime = (dateStr + (!dateStr.isEmpty() && !timeStr.isEmpty() ? " • " : "") + timeStr).trim();
        tvApptDateTime.setText(dateTime.isEmpty() ? "Schedule pending" : dateTime);

        // Status badge styling
        String status = item.status != null ? item.status : "Pending";
        tvApptStatus.setText(status);
        applyStatusBadgeStyle(tvApptStatus, status);

        // Device name & category-specific icon
        String deviceName = data.containsKey("deviceName") && data.get("deviceName") != null ? 
                String.valueOf(data.get("deviceName")) : "Hardware Device";
        tvApptDeviceName.setText(deviceName);

        String devLower = deviceName.toLowerCase();
        if (devLower.contains("laptop") || devLower.contains("macbook") || devLower.contains("desktop") || devLower.contains("computer")) {
            ivApptDeviceIcon.setImageResource(R.drawable.ic_customer_laptop);
            ivApptDeviceIcon.setBackgroundResource(R.drawable.bg_customer_soft_orange);
        } else {
            ivApptDeviceIcon.setImageResource(R.drawable.ic_customer_phone);
            ivApptDeviceIcon.setBackgroundResource(R.drawable.bg_customer_soft_blue);
        }

        // Service Center Branch
        String branch = data.containsKey("branch") && data.get("branch") != null ? 
                String.valueOf(data.get("branch")) : "Colombo";
        tvApptBranch.setText("📍 " + branch);

        // Client credentials
        String client = data.containsKey("clientName") && data.get("clientName") != null ? 
                String.valueOf(data.get("clientName")) : "Client";
        String email = data.containsKey("userEmail") && data.get("userEmail") != null ? 
                String.valueOf(data.get("userEmail")) : "";
        tvApptClient.setText("Client: " + client + (!email.isEmpty() ? " (" + email + ")" : ""));

        // Description excerpt
        String desc = data.containsKey("description") && data.get("description") != null ? 
                String.valueOf(data.get("description")) : "No diagnostic description provided";
        tvApptDescription.setText(desc);

        // Financial quotation
        Object costVal = data.get("cost");
        if (costVal != null) {
            try {
                tvApptCost.setText("LKR " + (int) Double.parseDouble(String.valueOf(costVal)));
            } catch (Exception e) {
                tvApptCost.setText("LKR " + String.valueOf(costVal));
            }
        } else {
            tvApptCost.setText("Cost TBD");
        }

        // Photo indicator badge
        Object photoObj = data.get("photoUri");
        if (photoObj != null && !String.valueOf(photoObj).trim().isEmpty() && !"null".equalsIgnoreCase(String.valueOf(photoObj).trim())) {
            tvApptPhotoBadge.setVisibility(View.VISIBLE);
        } else {
            tvApptPhotoBadge.setVisibility(View.GONE);
        }

        // Click listener opens the detailed modal with photo & status actions
        row.setOnClickListener(v -> showAppointmentDetailDialog(item));

        managementList.addView(row);
    }

    private void applyStatusBadgeStyle(TextView tv, String status) {
        if ("Pending".equalsIgnoreCase(status)) {
            tv.setBackgroundResource(R.drawable.bg_management_status_warning);
            tv.setTextColor(getResources().getColor(R.color.customer_orange));
        } else if ("Completed".equalsIgnoreCase(status)) {
            tv.setBackgroundResource(R.drawable.bg_status_success);
            tv.setTextColor(getResources().getColor(R.color.customer_success));
        } else if ("In Progress".equalsIgnoreCase(status)) {
            tv.setBackgroundResource(R.drawable.bg_customer_soft_blue);
            tv.setTextColor(getResources().getColor(R.color.customer_blue));
        } else if ("Approved".equalsIgnoreCase(status)) {
            tv.setBackgroundResource(R.drawable.bg_management_status);
            tv.setTextColor(getResources().getColor(R.color.management_cyan));
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            tv.setBackgroundResource(R.drawable.bg_status_cancelled);
            tv.setTextColor(getResources().getColor(R.color.customer_danger));
        } else {
            tv.setBackgroundResource(R.drawable.bg_management_status);
            tv.setTextColor(getResources().getColor(R.color.management_cyan));
        }
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

    private void showAppointmentDetailDialog(FirestoreItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_appointment_detail, null);

        TextView tvDetailApptId = dialogView.findViewById(R.id.tvDetailApptId);
        TextView tvDetailDeviceName = dialogView.findViewById(R.id.tvDetailDeviceName);
        TextView tvDetailStatus = dialogView.findViewById(R.id.tvDetailStatus);
        View btnDetailDismiss = dialogView.findViewById(R.id.btnDetailDismiss);

        TextView tvDetailClientName = dialogView.findViewById(R.id.tvDetailClientName);
        TextView tvDetailClientEmail = dialogView.findViewById(R.id.tvDetailClientEmail);
        TextView tvDetailBranch = dialogView.findViewById(R.id.tvDetailBranch);
        TextView tvDetailCost = dialogView.findViewById(R.id.tvDetailCost);
        TextView tvDetailSchedule = dialogView.findViewById(R.id.tvDetailSchedule);
        TextView tvDetailDescription = dialogView.findViewById(R.id.tvDetailDescription);

        View cardPhotoContainer = dialogView.findViewById(R.id.cardPhotoContainer);
        ImageView ivDetailPhoto = dialogView.findViewById(R.id.ivDetailPhoto);
        View layoutNoPhotoPlaceholder = dialogView.findViewById(R.id.layoutNoPhotoPlaceholder);

        View btnDetailClose = dialogView.findViewById(R.id.btnDetailClose);
        View btnDetailUpdateStatus = dialogView.findViewById(R.id.btnDetailUpdateStatus);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Map<String, Object> data = item.rawData;

        // Tracking ID
        String repairId = item.id != null && item.id.length() > 5 ? 
                "#TF-" + item.id.substring(0, 5).toUpperCase() : 
                (item.id != null ? "#TF-" + item.id.toUpperCase() : "#TF-0000");
        tvDetailApptId.setText(repairId);

        // Device
        String deviceName = data.containsKey("deviceName") && data.get("deviceName") != null ? 
                String.valueOf(data.get("deviceName")) : "Hardware Device";
        tvDetailDeviceName.setText(deviceName);

        // Status badge
        String status = item.status != null ? item.status : "Pending";
        tvDetailStatus.setText(status);
        applyStatusBadgeStyle(tvDetailStatus, status);

        // Client
        String client = data.containsKey("clientName") && data.get("clientName") != null ? 
                String.valueOf(data.get("clientName")) : "Client";
        String email = data.containsKey("userEmail") && data.get("userEmail") != null ? 
                String.valueOf(data.get("userEmail")) : "";
        tvDetailClientName.setText(client);
        tvDetailClientEmail.setText(!email.isEmpty() ? " (" + email + ")" : "");

        // Branch & Financials
        String branch = data.containsKey("branch") && data.get("branch") != null ? 
                String.valueOf(data.get("branch")) : "Colombo";
        tvDetailBranch.setText("📍 Branch: " + branch);

        Object costVal = data.get("cost");
        if (costVal != null) {
            try {
                tvDetailCost.setText("Est: LKR " + (int) Double.parseDouble(String.valueOf(costVal)));
            } catch (Exception e) {
                tvDetailCost.setText("Est: LKR " + String.valueOf(costVal));
            }
        } else {
            tvDetailCost.setText("Cost TBD");
        }

        // Schedule
        String dateStr = data.containsKey("date") && data.get("date") != null ? String.valueOf(data.get("date")) : "";
        String timeStr = data.containsKey("time") && data.get("time") != null ? String.valueOf(data.get("time")) : "";
        String schedule = (dateStr + (!dateStr.isEmpty() && !timeStr.isEmpty() ? " • " : "") + timeStr).trim();
        tvDetailSchedule.setText("📅 Scheduled: " + (schedule.isEmpty() ? "Date unassigned" : schedule));

        // Description
        String desc = data.containsKey("description") && data.get("description") != null ? 
                String.valueOf(data.get("description")) : "No issue description provided";
        tvDetailDescription.setText(desc);

        // Hardware Damage Photo View
        Object photoObj = data.get("photoUri");
        String photoUriStr = photoObj != null ? String.valueOf(photoObj).trim() : "";
        if (!photoUriStr.isEmpty() && !"null".equalsIgnoreCase(photoUriStr)) {
            try {
                Uri uri = Uri.parse(photoUriStr);
                ivDetailPhoto.setImageURI(uri);
                cardPhotoContainer.setVisibility(View.VISIBLE);
                layoutNoPhotoPlaceholder.setVisibility(View.GONE);
            } catch (Exception e) {
                cardPhotoContainer.setVisibility(View.GONE);
                layoutNoPhotoPlaceholder.setVisibility(View.VISIBLE);
            }
        } else {
            cardPhotoContainer.setVisibility(View.GONE);
            layoutNoPhotoPlaceholder.setVisibility(View.VISIBLE);
        }

        // Click handlers
        btnDetailDismiss.setOnClickListener(v -> dialog.dismiss());
        btnDetailClose.setOnClickListener(v -> dialog.dismiss());

        btnDetailUpdateStatus.setOnClickListener(v -> {
            dialog.dismiss();
            showStatusUpdateDialogForItem(item);
        });

        dialog.show();
    }

    private void showStatusUpdateDialog(int index) {
        if (index < 0 || index >= loadedItems.size()) return;
        showStatusUpdateDialogForItem(loadedItems.get(index));
    }

    private void showStatusUpdateDialogForItem(FirestoreItem item) {
        final String[] statusOptions = {"Pending", "Approved", "In Progress", "Completed", "Cancelled"};
        
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
            case "imageUrl": return "Image URL / Device Photo URI";
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
            case "images":
                return new String[]{"name", "category", "location", "description", "price", "imageUrl"};
            default:
                return new String[]{"name", "description", "status"};
        }
    }
}
