package com.example.techfix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ManagementModuleActivity extends ManagementScreen {
  public static final String APPOINTMENTS = "appointments";
  public static final String TECHNICIANS = "technicians";
  public static final String PRICES = "prices";
  public static final String PARTS = "parts";
  public static final String IMAGES = "images";
  public static final String PAYMENTS = "payments";
  public static final String STATUSES = "statuses";
  private static final String EXTRA_MODULE = "management_module";

  private String module;
  private ModuleInfo moduleInfo;
  private String selectedBranch = "All";

  private final ActivityResultLauncher<String[]> imagePicker = registerForActivityResult(
      new ActivityResultContracts.OpenDocument(), this::showSelectedImage);

  public static void open(Activity activity, String module) {
    Intent intent = new Intent(activity, ManagementModuleActivity.class);
    intent.putExtra(EXTRA_MODULE, module);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showManagementLayout(R.layout.activity_management_module);
    module = getIntent().getStringExtra(EXTRA_MODULE);
    if (module == null)
      module = APPOINTMENTS;
    moduleInfo = moduleInfo(module);

    ((TextView) findViewById(R.id.tvModuleEyebrow)).setText(moduleInfo.eyebrow);
    ((TextView) findViewById(R.id.tvModuleTitle)).setText(moduleInfo.title);
    ((TextView) findViewById(R.id.tvModuleMetric)).setText(moduleInfo.metric);
    ((TextView) findViewById(R.id.tvModuleMetricLabel)).setText(moduleInfo.metricLabel);
    ((TextView) findViewById(R.id.tvModuleTrend)).setText(moduleInfo.trend);
    ((TextView) findViewById(R.id.tvModuleSection)).setText(moduleInfo.section);

    findViewById(R.id.btnModuleBack).setOnClickListener(view -> finish());
    findViewById(R.id.btnModuleAdd).setOnClickListener(view -> handleAdd());
    bindFilter(R.id.filterAll, "All");
    bindFilter(R.id.filterColombo, "Colombo");
    bindFilter(R.id.filterGalle, "Galle");
    renderItems();
  }

  private void bindFilter(int viewId, String branch) {
    findViewById(viewId).setOnClickListener(view -> {
      selectedBranch = branch;
      updateFilterAppearance();
      renderItems();
    });
  }

  private void updateFilterAppearance() {
    updateFilter(R.id.filterAll, "All".equals(selectedBranch));
    updateFilter(R.id.filterColombo, "Colombo".equals(selectedBranch));
    updateFilter(R.id.filterGalle, "Galle".equals(selectedBranch));
  }

  private void updateFilter(int viewId, boolean active) {
    TextView filter = findViewById(viewId);
    filter.setBackgroundResource(
        active ? R.drawable.bg_management_chip_active : R.drawable.bg_management_chip);
    filter.setTextColor(getColor(active ? R.color.management_cyan : R.color.management_muted));
  }

  private void renderItems() {
    LinearLayout container = findViewById(R.id.managementList);
    container.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (ManagementItem item : moduleInfo.items) {
      if (!"All".equals(selectedBranch) && !selectedBranch.equals(item.branch))
        continue;
      View card = inflater.inflate(R.layout.view_management_item, container, false);
      ((TextView) card.findViewById(R.id.tvManagementItemCode)).setText(item.code);
      TextView status = card.findViewById(R.id.tvManagementItemStatus);
      status.setText(item.status);
      styleStatus(status, item.status);
      if (IMAGES.equals(module))
        card.findViewById(R.id.ivManagementItemPreview).setVisibility(View.VISIBLE);
      ((TextView) card.findViewById(R.id.tvManagementItemTitle)).setText(item.title);
      ((TextView) card.findViewById(R.id.tvManagementItemMeta)).setText(item.meta);
      ((TextView) card.findViewById(R.id.tvManagementItemDetail)).setText(item.detail);
      TextView action = card.findViewById(R.id.btnManagementItemAction);
      action.setText(item.action);
      View.OnClickListener listener = view -> handleItemAction(card, item);
      action.setOnClickListener(listener);
      card.setOnClickListener(listener);
      container.addView(card);
    }
    if (container.getChildCount() == 0) {
      TextView empty = new TextView(this);
      empty.setText("No " + moduleInfo.title.toLowerCase() + " for this branch.");
      empty.setTextColor(getColor(R.color.management_muted));
      empty.setTextSize(14);
      empty.setPadding(0, dp(32), 0, dp(32));
      container.addView(empty);
    }
  }

  private void handleItemAction(View card, ManagementItem item) {
    TextView status = card.findViewById(R.id.tvManagementItemStatus);
    TextView detail = card.findViewById(R.id.tvManagementItemDetail);
    switch (module) {
      case APPOINTMENTS:
      case STATUSES:
        showStatusPicker(status, detail);
        break;
      case TECHNICIANS:
        String technicianState =
            "AVAILABLE".contentEquals(status.getText()) ? "OFF DUTY" : "AVAILABLE";
        status.setText(technicianState);
        detail.setText("AVAILABLE".equals(technicianState) ? "Ready for assignment"
                                                             : "Removed from new assignments");
        styleStatus(status, technicianState);
        break;
      case PRICES:
        showValueEditor("Update service price", "Price in LKR", detail, "LKR ");
        break;
      case PARTS:
        showValueEditor("Adjust available stock", "Quantity", detail, " units available");
        break;
      case IMAGES:
        status.setText("FEATURED");
        detail.setText("Visible in the customer repair gallery");
        styleStatus(status, "FEATURED");
        break;
      case PAYMENTS:
        status.setText("PAID");
        detail.setText("Receipt ready to send");
        styleStatus(status, "PAID");
        break;
      default:
        break;
    }
  }

  private void showStatusPicker(TextView status, TextView detail) {
    String[] statuses = {"ASSIGNED", "IN PROGRESS", "QUALITY CHECK", "READY", "COMPLETED"};
    new AlertDialog.Builder(this)
        .setTitle("Update repair status")
        .setItems(statuses,
            (dialog, index) -> {
              status.setText(statuses[index]);
              detail.setText("Customer notification queued");
              styleStatus(status, statuses[index]);
            })
        .show();
  }

  private void showValueEditor(String title, String hint, TextView target, String valueDecoration) {
    EditText value = new EditText(this);
    value.setHint(hint);
    value.setInputType(InputType.TYPE_CLASS_NUMBER);
    int padding = dp(20);
    value.setPadding(padding, padding, padding, padding);
    new AlertDialog.Builder(this)
        .setTitle(title)
        .setView(value)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Save",
            (dialog, which) -> {
              String entered = value.getText().toString().trim();
              if (!entered.isEmpty())
                target.setText(valueDecoration.startsWith("LKR") ? valueDecoration + entered
                                                                 : entered + valueDecoration);
            })
        .show();
  }

  private void handleAdd() {
    if (IMAGES.equals(module)) {
      imagePicker.launch(new String[] {"image/*"});
      return;
    }
    EditText name = new EditText(this);
    name.setHint(moduleInfo.addHint);
    int padding = dp(20);
    name.setPadding(padding, padding, padding, padding);
    new AlertDialog.Builder(this)
        .setTitle(moduleInfo.addTitle)
        .setView(name)
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Continue",
            (dialog, which)
                -> Toast
                    .makeText(this, "Frontend form ready for " + moduleInfo.title.toLowerCase(),
                        Toast.LENGTH_SHORT)
                    .show())
        .show();
  }

  private void showSelectedImage(Uri uri) {
    if (uri != null)
      Toast.makeText(this, "Repair image selected for upload", Toast.LENGTH_SHORT).show();
  }

  private void styleStatus(TextView status, String value) {
    String normalized = value.toUpperCase();
    boolean warning = normalized.contains("PENDING") || normalized.contains("LOW")
        || normalized.contains("BUSY") || normalized.contains("AWAITING")
        || normalized.contains("DUE") || normalized.contains("OFF DUTY");
    status.setBackgroundResource(
        warning ? R.drawable.bg_management_status_warning : R.drawable.bg_management_status);
    status.setTextColor(getColor(warning ? R.color.management_amber : R.color.management_green));
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private ModuleInfo moduleInfo(String requestedModule) {
    switch (requestedModule == null ? APPOINTMENTS : requestedModule) {
      case TECHNICIANS:
        return new ModuleInfo("WORKFORCE", "Technicians", "04", "technicians on shift", "1 BUSY",
            "Team availability", "Add technician", "Technician name",
            items(new ManagementItem("NP", "AVAILABLE", "Nimal Perera · Phone specialist",
                      "Colombo · 3 repairs today", "Ready for assignment", "MANAGE", "Colombo"),
                new ManagementItem("TS", "BUSY", "Tharindu Silva · Laptop specialist",
                    "Colombo · Repair #TF-1042", "Available after 2:30 PM", "MANAGE", "Colombo"),
                new ManagementItem("SF", "AVAILABLE", "Sachini Fernando · Phone specialist",
                    "Galle · 2 repairs today", "Ready for assignment", "MANAGE", "Galle"),
                new ManagementItem("KJ", "AVAILABLE", "Kavindu Jayasekara · Desktop specialist",
                    "Galle · 1 repair today", "Ready for assignment", "MANAGE", "Galle")));
      case PRICES:
        return new ModuleInfo("SERVICE CATALOG", "Service prices", "05", "active repair services",
            "AVG 6.1K", "Current pricing", "Add service", "Service name",
            items(new ManagementItem("PHONE", "ACTIVE", "Screen replacement",
                      "Phone · 90 minute estimate", "LKR 8,500", "EDIT", "Colombo"),
                new ManagementItem("PHONE", "ACTIVE", "Battery replacement",
                    "Phone · 60 minute estimate", "LKR 6,000", "EDIT", "Galle"),
                new ManagementItem("LAPTOP", "ACTIVE", "Keyboard repair",
                    "Laptop · 90 minute estimate", "LKR 4,500", "EDIT", "Galle"),
                new ManagementItem("TABLET", "ACTIVE", "Charging port repair",
                    "Tablet · 90 minute estimate", "LKR 7,000", "EDIT", "Colombo")));
      case PARTS:
        return new ModuleInfo("INVENTORY", "Spare parts", "38", "units across both branches",
            "2 LOW", "Stock levels", "Add spare part", "Part name or SKU",
            items(new ManagementItem("COL-PH-01", "IN STOCK", "Universal OLED display",
                      "Phone · Colombo", "8 units available", "ADJUST", "Colombo"),
                new ManagementItem("COL-BAT-01", "IN STOCK", "Phone battery pack",
                    "Phone · Colombo", "12 units available", "ADJUST", "Colombo"),
                new ManagementItem("GAL-KEY-01", "LOW STOCK", "Laptop keyboard assembly",
                    "Laptop · Galle", "4 units available", "ADJUST", "Galle"),
                new ManagementItem("GAL-DIS-01", "LOW STOCK", "Universal OLED display",
                    "Phone · Galle", "5 units available", "ADJUST", "Galle")));
      case IMAGES:
        return new ModuleInfo("MEDIA LIBRARY", "Repair gallery", "24", "published repair images",
            "+6 MONTH", "Recent work", "Upload repair image", "Image caption",
            items(new ManagementItem("BEFORE / AFTER", "PUBLISHED", "iPhone display restoration",
                      "Colombo · Screen replacement", "Uploaded today", "FEATURE", "Colombo"),
                new ManagementItem("DETAIL", "FEATURED", "Laptop keyboard replacement",
                    "Galle · Keyboard repair", "Homepage sample image", "FEATURE", "Galle"),
                new ManagementItem("BEFORE / AFTER", "PUBLISHED", "Tablet charging port repair",
                    "Colombo · Charging repair", "Uploaded 2 days ago", "FEATURE", "Colombo")));
      case PAYMENTS:
        return new ModuleInfo("FINANCE", "Payments", "LKR 84.5K", "collected today", "+12% WEEK",
            "Latest transactions", "Record payment", "Appointment ID",
            items(new ManagementItem("#PAY-084", "PAID", "LKR 8,500 · #TF-1038", "Card · Galle",
                      "Receipt sent · 21 min ago", "RECEIPT", "Galle"),
                new ManagementItem("#PAY-083", "PENDING", "LKR 6,000 · #TF-1041", "Cash · Colombo",
                    "Awaiting customer collection", "MARK PAID", "Colombo"),
                new ManagementItem("#PAY-082", "PAID", "LKR 4,500 · #TF-1036", "Card · Colombo",
                    "Receipt sent · 2 hours ago", "RECEIPT", "Colombo")));
      case STATUSES:
        return new ModuleInfo("REPAIR WORKFLOW", "Status updates", "08", "repairs require tracking",
            "3 READY", "Update queue", "Create update", "Appointment ID",
            items(new ManagementItem("#TF-1042", "QUALITY CHECK", "iPhone 14 Pro · Display",
                      "Colombo · Nimal Perera", "Customer last notified 32 min ago", "UPDATE",
                      "Colombo"),
                new ManagementItem("#TF-1041", "IN PROGRESS", "Samsung Galaxy S23 · Battery",
                    "Colombo · Tharindu Silva", "Repair started at 11:10 AM", "UPDATE", "Colombo"),
                new ManagementItem("#TF-1039", "READY", "Dell Inspiron 15 · Keyboard",
                    "Galle · Kavindu Jayasekara", "Customer notified · awaiting pickup", "UPDATE",
                    "Galle")));
      case APPOINTMENTS:
      default:
        return new ModuleInfo("BOOKING DESK", "Appointments", "12", "appointments scheduled today",
            "+4 NEW", "Today's queue", "Create appointment", "Customer or device",
            items(
                new ManagementItem("#TF-1042", "IN PROGRESS", "iPhone 14 Pro · Screen replacement",
                    "Colombo · Nimal Perera · 10:30 AM", "Due in 45 minutes", "UPDATE", "Colombo"),
                new ManagementItem("#TF-1041", "ASSIGNED",
                    "Samsung Galaxy S23 · Battery replacement",
                    "Colombo · Tharindu Silva · 11:30 AM", "Check-in confirmed", "UPDATE",
                    "Colombo"),
                new ManagementItem("#TF-1040", "PENDING", "Dell Inspiron 15 · Diagnostics",
                    "Galle · Unassigned · 1:00 PM", "Technician assignment required", "ASSIGN",
                    "Galle"),
                new ManagementItem("#TF-1039", "READY", "Apple iPad Air · Charging port",
                    "Galle · Sachini Fernando · 9:00 AM", "Ready for customer pickup", "VIEW",
                    "Galle")));
    }
  }

  private List<ManagementItem> items(ManagementItem... entries) {
    return new ArrayList<>(Arrays.asList(entries));
  }

  private static final class ModuleInfo {
    final String eyebrow;
    final String title;
    final String metric;
    final String metricLabel;
    final String trend;
    final String section;
    final String addTitle;
    final String addHint;
    final List<ManagementItem> items;

    ModuleInfo(String eyebrow, String title, String metric, String metricLabel, String trend,
        String section, String addTitle, String addHint, List<ManagementItem> items) {
      this.eyebrow = eyebrow;
      this.title = title;
      this.metric = metric;
      this.metricLabel = metricLabel;
      this.trend = trend;
      this.section = section;
      this.addTitle = addTitle;
      this.addHint = addHint;
      this.items = items;
    }
  }

  private static final class ManagementItem {
    final String code;
    final String status;
    final String title;
    final String meta;
    final String detail;
    final String action;
    final String branch;

    ManagementItem(String code, String status, String title, String meta, String detail,
        String action, String branch) {
      this.code = code;
      this.status = status;
      this.title = title;
      this.meta = meta;
      this.detail = detail;
      this.action = action;
      this.branch = branch;
    }
  }
}
