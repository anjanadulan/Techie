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

import android.graphics.Typeface;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AlertDialog;
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
        tvApptBranch.setText( branch);

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
        // Inflate custom modern card dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_form, null);

        TextView tvFormEyebrow = dialogView.findViewById(R.id.tvFormEyebrow);
        TextView tvFormTitle = dialogView.findViewById(R.id.tvFormTitle);
        View btnFormDismiss = dialogView.findViewById(R.id.btnFormDismiss);
        LinearLayout formFieldsContainer = dialogView.findViewById(R.id.formFieldsContainer);
        View btnFormDelete = dialogView.findViewById(R.id.btnFormDelete);
        View btnFormCancel = dialogView.findViewById(R.id.btnFormCancel);
        TextView btnFormSubmit = dialogView.findViewById(R.id.btnFormSubmit);

        // Initialize Android Material Alert Dialog with transparent window to support 24dp rounded corners
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Configure dialog header texts
        tvFormEyebrow.setText("RECORD CREATION");
        tvFormTitle.setText("Add New " + tvModuleTitle.getText().toString());
        btnFormDelete.setVisibility(View.GONE); // Delete button not applicable during creation
        btnFormSubmit.setText("Create Record");

        // Dynamically instantiate and bind EditText input fields for the active administrative module
        final ArrayList<EditText> inputs = new ArrayList<>();
        final String[] fields = getFieldsForModule();

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (14 * density);
        int padV = (int) (12 * density);
        int marginB = (int) (12 * density);
        int labelMarginB = (int) (4 * density);

        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];

            // Descriptive attribute label
            TextView tvLabel = new TextView(this);
            tvLabel.setText(formatFieldName(field));
            tvLabel.setTextSize(12);
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setTextColor(getResources().getColor(R.color.customer_muted));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.setMargins(0, i == 0 ? 0 : (int) (8 * density), 0, labelMarginB);
            tvLabel.setLayoutParams(labelParams);
            formFieldsContainer.addView(tvLabel);

            // Styled input field matching customer theme
            EditText et = new EditText(this);
            et.setHint("Enter " + formatFieldName(field).toLowerCase());
            et.setHintTextColor(getResources().getColor(R.color.customer_muted));
            et.setTextColor(getResources().getColor(R.color.customer_text));
            et.setTextSize(13);
            et.setBackgroundResource(R.drawable.bg_customer_input);
            et.setPadding(padH, padV, padH, padV);

            // Configure numeric input keyboards where appropriate for data validation
            if ("quantity".equals(field) || "price".equals(field) || "estimatedPrice".equals(field) || "amount".equals(field)) {
                et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }

            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            inputParams.setMargins(0, 0, 0, marginB);
            et.setLayoutParams(inputParams);

            formFieldsContainer.addView(et);
            inputs.add(et);
        }

        // Close dialog on dismiss or cancel button click
        btnFormDismiss.setOnClickListener(v -> dialog.dismiss());
        btnFormCancel.setOnClickListener(v -> dialog.dismiss());

        // Process creation payload and persist to Cloud Firestore
        btnFormSubmit.setOnClickListener(v -> {
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                String val = inputs.get(i).getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("quantity".equals(fields[i])) {
                    try {
                        data.put(fields[i], Integer.parseInt(val));
                    } catch (Exception e) {
                        data.put(fields[i], 0);
                    }
                } else if ("price".equals(fields[i]) || "estimatedPrice".equals(fields[i]) || "amount".equals(fields[i])) {
                    try {
                        data.put(fields[i], Double.parseDouble(val));
                    } catch (Exception e) {
                        data.put(fields[i], 0.0);
                    }
                } else {
                    data.put(fields[i], val);
                }
            }

            dialog.dismiss();
            refreshLayout.setRefreshing(true);

            // Cloud Firestore asynchronous document insertion
            db.collection(getCollectionName())
                    .add(data)
                    .addOnSuccessListener(ref -> {
                        loadModuleData();
                        Toast.makeText(this, "Record created successfully in Firestore!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Failed to create: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    /**
     * Display modern Material Design dialog for modifying or deleting an existing record in Cloud Firestore.
     * Pre-populates all existing document attributes into styled input fields.
     */
    private void showEditDeleteDialog(int index) {
        if (index < 0 || index >= loadedItems.size()) return;
        FirestoreItem item = loadedItems.get(index);

        // Inflate custom modern card dialog layout
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_form, null);

        TextView tvFormEyebrow = dialogView.findViewById(R.id.tvFormEyebrow);
        TextView tvFormTitle = dialogView.findViewById(R.id.tvFormTitle);
        View btnFormDismiss = dialogView.findViewById(R.id.btnFormDismiss);
        LinearLayout formFieldsContainer = dialogView.findViewById(R.id.formFieldsContainer);
        View btnFormDelete = dialogView.findViewById(R.id.btnFormDelete);
        View btnFormCancel = dialogView.findViewById(R.id.btnFormCancel);
        TextView btnFormSubmit = dialogView.findViewById(R.id.btnFormSubmit);

        // Initialize Android Material Alert Dialog with transparent window to support 24dp rounded corners
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Configure dialog header texts
        tvFormEyebrow.setText("RECORD MUTATION & AUDITING");
        tvFormTitle.setText("Edit " + tvModuleTitle.getText().toString());
        btnFormDelete.setVisibility(View.VISIBLE); // Reveal delete action button in edit mode
        btnFormSubmit.setText("Save Changes");

        // Dynamically instantiate and bind EditText input fields with existing document values
        final ArrayList<EditText> inputs = new ArrayList<>();
        final String[] fields = getFieldsForModule();

        float density = getResources().getDisplayMetrics().density;
        int padH = (int) (14 * density);
        int padV = (int) (12 * density);
        int marginB = (int) (12 * density);
        int labelMarginB = (int) (4 * density);

        for (int i = 0; i < fields.length; i++) {
            String field = fields[i];

            // Descriptive attribute label
            TextView tvLabel = new TextView(this);
            tvLabel.setText(formatFieldName(field));
            tvLabel.setTextSize(12);
            tvLabel.setTypeface(null, Typeface.BOLD);
            tvLabel.setTextColor(getResources().getColor(R.color.customer_muted));
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.setMargins(0, i == 0 ? 0 : (int) (8 * density), 0, labelMarginB);
            tvLabel.setLayoutParams(labelParams);
            formFieldsContainer.addView(tvLabel);

            // Styled input field pre-filled with existing data
            EditText et = new EditText(this);
            et.setHint("Enter " + formatFieldName(field).toLowerCase());
            et.setHintTextColor(getResources().getColor(R.color.customer_muted));
            et.setTextColor(getResources().getColor(R.color.customer_text));
            et.setTextSize(13);
            et.setBackgroundResource(R.drawable.bg_customer_input);
            et.setPadding(padH, padV, padH, padV);

            Object currentVal = item.rawData.get(field);
            et.setText(currentVal != null ? String.valueOf(currentVal) : "");

            // Configure numeric input keyboards where appropriate for data validation
            if ("quantity".equals(field) || "price".equals(field) || "estimatedPrice".equals(field) || "amount".equals(field)) {
                et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            }

            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            inputParams.setMargins(0, 0, 0, marginB);
            et.setLayoutParams(inputParams);

            formFieldsContainer.addView(et);
            inputs.add(et);
        }

        // Close dialog on dismiss or cancel button click
        btnFormDismiss.setOnClickListener(v -> dialog.dismiss());
        btnFormCancel.setOnClickListener(v -> dialog.dismiss());

        // Trigger modern confirmation dialog when Delete button is pressed
        btnFormDelete.setOnClickListener(v -> showDeleteConfirmDialog(item, dialog));

        // Process update payload and synchronize with Cloud Firestore
        btnFormSubmit.setOnClickListener(v -> {
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < fields.length; i++) {
                String val = inputs.get(i).getText().toString().trim();
                if (val.isEmpty()) {
                    Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("quantity".equals(fields[i])) {
                    try {
                        data.put(fields[i], Integer.parseInt(val));
                    } catch (Exception e) {
                        data.put(fields[i], 0);
                    }
                } else if ("price".equals(fields[i]) || "estimatedPrice".equals(fields[i]) || "amount".equals(fields[i])) {
                    try {
                        data.put(fields[i], Double.parseDouble(val));
                    } catch (Exception e) {
                        data.put(fields[i], 0.0);
                    }
                } else {
                    data.put(fields[i], val);
                }
            }

            dialog.dismiss();
            refreshLayout.setRefreshing(true);

            // Cloud Firestore asynchronous document update
            db.collection(getCollectionName()).document(item.id)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        loadModuleData();
                        Toast.makeText(this, "Record updated in Firestore!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }

    /**
     * Display high-finish confirmation dialog to prevent accidental deletion of database entries.
     * Uses danger-accented styling to alert the administrator of permanent Firestore removal.
     */
    private void showDeleteConfirmDialog(FirestoreItem item, AlertDialog parentDialog) {
        View confirmView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_delete_confirm, null);

        TextView tvDeleteDialogTitle = confirmView.findViewById(R.id.tvDeleteDialogTitle);
        TextView tvDeleteDialogMessage = confirmView.findViewById(R.id.tvDeleteDialogMessage);
        View btnDeleteCancel = confirmView.findViewById(R.id.btnDeleteCancel);
        View btnDeleteConfirm = confirmView.findViewById(R.id.btnDeleteConfirm);

        AlertDialog confirmDialog = new AlertDialog.Builder(this)
                .setView(confirmView)
                .create();

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        String itemLabel = item.title != null && !item.title.isEmpty() ? item.title : "this record";
        tvDeleteDialogTitle.setText("Delete " + itemLabel + "?");
        tvDeleteDialogMessage.setText("Are you sure you want to permanently remove this " +
                tvModuleTitle.getText().toString().toLowerCase() +
                " entry from Cloud Firestore? This action cannot be undone.");

        btnDeleteCancel.setOnClickListener(v -> confirmDialog.dismiss());

        // Perform irreversible Firestore document deletion
        btnDeleteConfirm.setOnClickListener(v -> {
            confirmDialog.dismiss();
            if (parentDialog != null) {
                parentDialog.dismiss();
            }

            refreshLayout.setRefreshing(true);
            db.collection(getCollectionName()).document(item.id)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        loadModuleData();
                        Toast.makeText(this, "Record permanently deleted from Firestore!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Deletion failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        confirmDialog.show();
    }

    /**
     * Display detailed read-only modal for administrative inspection.
     * Renders key-value metadata pairs and previews hardware or media photos if present.
     */
    private void showViewDetailsDialog(int index) {
        if (index < 0 || index >= loadedItems.size()) return;
        FirestoreItem item = loadedItems.get(index);

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_details, null);

        TextView tvDetailsEyebrow = dialogView.findViewById(R.id.tvDetailsEyebrow);
        TextView tvDetailsTitle = dialogView.findViewById(R.id.tvDetailsTitle);
        TextView tvDetailsStatus = dialogView.findViewById(R.id.tvDetailsStatus);
        View btnDetailsDismiss = dialogView.findViewById(R.id.btnDetailsDismiss);
        View cardDetailsImageContainer = dialogView.findViewById(R.id.cardDetailsImageContainer);
        ImageView ivDetailsImage = dialogView.findViewById(R.id.ivDetailsImage);
        LinearLayout detailsRowsContainer = dialogView.findViewById(R.id.detailsRowsContainer);
        View btnDetailsClose = dialogView.findViewById(R.id.btnDetailsClose);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Configure header metadata
        tvDetailsEyebrow.setText(tvModuleTitle.getText().toString().toUpperCase() + " AUDIT TRAIL");
        tvDetailsTitle.setText(item.title != null && !item.title.isEmpty() ? item.title : "Record Information");

        // Status badge configuration
        if (item.status != null && !item.status.isEmpty()) {
            tvDetailsStatus.setVisibility(View.VISIBLE);
            tvDetailsStatus.setText(item.status);
            applyStatusBadgeStyle(tvDetailsStatus, item.status);
        } else {
            tvDetailsStatus.setVisibility(View.GONE);
        }

        // Inspection image preview (e.g. Gallery images or Repair photo attachments)
        String imageUriStr = "";
        if (item.rawData.containsKey("imageUrl") && item.rawData.get("imageUrl") != null) {
            imageUriStr = String.valueOf(item.rawData.get("imageUrl")).trim();
        } else if (item.rawData.containsKey("photoUri") && item.rawData.get("photoUri") != null) {
            imageUriStr = String.valueOf(item.rawData.get("photoUri")).trim();
        }

        if (!imageUriStr.isEmpty() && !"null".equalsIgnoreCase(imageUriStr)) {
            try {
                Uri uri = Uri.parse(imageUriStr);
                ivDetailsImage.setImageURI(uri);
                cardDetailsImageContainer.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                cardDetailsImageContainer.setVisibility(View.GONE);
            }
        } else {
            cardDetailsImageContainer.setVisibility(View.GONE);
        }

        // Dynamically populate key-value rows in the details container
        float density = getResources().getDisplayMetrics().density;
        int rowPadV = (int) (8 * density);
        boolean isFirst = true;

        for (Map.Entry<String, Object> entry : item.rawData.entrySet()) {
            String key = entry.getKey();
            if ("imageUrl".equals(key) || "photoUri".equals(key)) continue; // Already rendered in image card preview

            if (!isFirst) {
                // Divider line between attribute rows
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (int) (1 * density)));
                divider.setBackgroundColor(getResources().getColor(R.color.divider_color));
                detailsRowsContainer.addView(divider);
            }
            isFirst = false;

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(0, rowPadV, 0, rowPadV);

            // Attribute Key Label
            TextView tvKey = new TextView(this);
            tvKey.setText(formatFieldName(key));
            tvKey.setTextSize(12);
            tvKey.setTextColor(getResources().getColor(R.color.customer_muted));
            tvKey.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            tvKey.setLayoutParams(keyParams);

            // Attribute Value Label
            TextView tvVal = new TextView(this);
            Object rawVal = entry.getValue();
            tvVal.setText(rawVal != null ? String.valueOf(rawVal) : "—");
            tvVal.setTextSize(13);
            tvVal.setTextColor(getResources().getColor(R.color.customer_text));
            tvVal.setTypeface(null, Typeface.BOLD);
            tvVal.setGravity(android.view.Gravity.END);
            LinearLayout.LayoutParams valParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f);
            tvVal.setLayoutParams(valParams);

            rowLayout.addView(tvKey);
            rowLayout.addView(tvVal);
            detailsRowsContainer.addView(rowLayout);
        }

        // Dismiss action handlers
        btnDetailsDismiss.setOnClickListener(v -> dialog.dismiss());
        btnDetailsClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    /**
     * Display comprehensive inspection modal for customer repair appointments.
     * Incorporates inline hardware damage photo preview, full customer credentials,
     * diagnostic issue narrative, quotation estimation, and direct status transition triggers.
     */
    private void showAppointmentDetailDialog(FirestoreItem item) {
        // Inflate custom modern appointment detail dialog layout
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

        // Initialize Android Material Alert Dialog with transparent window to support 24dp rounded corners
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Map<String, Object> data = item.rawData;

        // Repair Tracking ID formatted with prefix (#TF-XXXXX)
        String repairId = item.id != null && item.id.length() > 5 ? 
                "#TF-" + item.id.substring(0, 5).toUpperCase() : 
                (item.id != null ? "#TF-" + item.id.toUpperCase() : "#TF-0000");
        tvDetailApptId.setText(repairId);

        // Hardware device identifier
        String deviceName = data.containsKey("deviceName") && data.get("deviceName") != null ? 
                String.valueOf(data.get("deviceName")) : "Hardware Device";
        tvDetailDeviceName.setText(deviceName);

        // Visual status badge with context-aware color mapping
        String status = item.status != null ? item.status : "Pending";
        tvDetailStatus.setText(status);
        applyStatusBadgeStyle(tvDetailStatus, status);

        // Customer contact information
        String client = data.containsKey("clientName") && data.get("clientName") != null ? 
                String.valueOf(data.get("clientName")) : "Client";
        String email = data.containsKey("userEmail") && data.get("userEmail") != null ? 
                String.valueOf(data.get("userEmail")) : "";
        tvDetailClientName.setText(client);
        tvDetailClientEmail.setText(!email.isEmpty() ? " (" + email + ")" : "");

        // Service branch location
        String branch = data.containsKey("branch") && data.get("branch") != null ? 
                String.valueOf(data.get("branch")) : "Colombo";
        tvDetailBranch.setText("📍 Branch: " + branch);

        // Financial repair quotation calculation
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

        // Repair slot scheduling metadata
        String dateStr = data.containsKey("date") && data.get("date") != null ? String.valueOf(data.get("date")) : "";
        String timeStr = data.containsKey("time") && data.get("time") != null ? String.valueOf(data.get("time")) : "";
        String schedule = (dateStr + (!dateStr.isEmpty() && !timeStr.isEmpty() ? " • " : "") + timeStr).trim();
        tvDetailSchedule.setText("📅 Scheduled: " + (schedule.isEmpty() ? "Date unassigned" : schedule));

        // Customer diagnostic problem description
        String desc = data.containsKey("description") && data.get("description") != null ? 
                String.valueOf(data.get("description")) : "No issue description provided";
        tvDetailDescription.setText(desc);

        // Hardware damage photo preview safely handled via content/storage URI parsing
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

        // Dismiss action handlers
        btnDetailDismiss.setOnClickListener(v -> dialog.dismiss());
        btnDetailClose.setOnClickListener(v -> dialog.dismiss());

        // Launch modern status update dialog directly from detail viewer
        btnDetailUpdateStatus.setOnClickListener(v -> {
            dialog.dismiss();
            showStatusUpdateDialogForItem(item);
        });

        dialog.show();
    }

    /**
     * Helper dispatcher for index-based status update requests.
     */
    private void showStatusUpdateDialog(int index) {
        if (index < 0 || index >= loadedItems.size()) return;
        showStatusUpdateDialogForItem(loadedItems.get(index));
    }

    /**
     * Display modern Material Design status transition dialog for repair appointments.
     * Incorporates custom radio option selector, pipeline stage descriptions, and real-time Firestore mutation.
     */
    private void showStatusUpdateDialogForItem(FirestoreItem item) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_status_update, null);

        TextView tvStatusDialogTitle = dialogView.findViewById(R.id.tvStatusDialogTitle);
        View btnStatusDismiss = dialogView.findViewById(R.id.btnStatusDismiss);
        RadioButton rbPending = dialogView.findViewById(R.id.rbPending);
        RadioButton rbApproved = dialogView.findViewById(R.id.rbApproved);
        RadioButton rbInProgress = dialogView.findViewById(R.id.rbInProgress);
        RadioButton rbCompleted = dialogView.findViewById(R.id.rbCompleted);
        RadioButton rbCancelled = dialogView.findViewById(R.id.rbCancelled);
        View btnStatusCancel = dialogView.findViewById(R.id.btnStatusCancel);
        View btnStatusUpdate = dialogView.findViewById(R.id.btnStatusUpdate);

        // Initialize Android Material Alert Dialog with transparent window to support 24dp rounded corners
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // Set repair tracking identifier in dialog title
        String repairId = item.id != null && item.id.length() > 5 ? 
                "#TF-" + item.id.substring(0, 5).toUpperCase() : 
                (item.id != null ? "#TF-" + item.id.toUpperCase() : "#TF-0000");
        tvStatusDialogTitle.setText("Status: " + repairId);

        // Pre-select current workflow stage in radio group
        String currentStatus = item.status != null ? item.status : "Pending";
        if ("Approved".equalsIgnoreCase(currentStatus)) {
            rbApproved.setChecked(true);
        } else if ("In Progress".equalsIgnoreCase(currentStatus)) {
            rbInProgress.setChecked(true);
        } else if ("Completed".equalsIgnoreCase(currentStatus)) {
            rbCompleted.setChecked(true);
        } else if ("Cancelled".equalsIgnoreCase(currentStatus)) {
            rbCancelled.setChecked(true);
        } else {
            rbPending.setChecked(true);
        }

        // Dismiss action handlers
        btnStatusDismiss.setOnClickListener(v -> dialog.dismiss());
        btnStatusCancel.setOnClickListener(v -> dialog.dismiss());

        // Execute status transition mutation against Cloud Firestore
        btnStatusUpdate.setOnClickListener(v -> {
            String selectedStatus = "Pending";
            if (rbApproved.isChecked()) {
                selectedStatus = "Approved";
            } else if (rbInProgress.isChecked()) {
                selectedStatus = "In Progress";
            } else if (rbCompleted.isChecked()) {
                selectedStatus = "Completed";
            } else if (rbCancelled.isChecked()) {
                selectedStatus = "Cancelled";
            }

            final String newStatus = selectedStatus;
            dialog.dismiss();
            refreshLayout.setRefreshing(true);

            // Synchronize status mutation across Firestore appointments collection
            String targetCollection = "appointments".equalsIgnoreCase(moduleKey) || "statuses".equalsIgnoreCase(moduleKey) ?
                    "appointments" : getCollectionName();

            db.collection(targetCollection).document(item.id)
                    .update("status", newStatus)
                    .addOnSuccessListener(aVoid -> {
                        loadModuleData();
                        Toast.makeText(this, "Workflow status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        refreshLayout.setRefreshing(false);
                        Toast.makeText(this, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
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
