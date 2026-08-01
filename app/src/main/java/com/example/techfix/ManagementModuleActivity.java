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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.techfix.data.model.AppointmentStatus;
import java.util.List;
import java.util.Locale;

public class ManagementModuleActivity extends ManagementScreen {
  public static final String APPOINTMENTS = "appointments";
  public static final String TECHNICIANS = "technicians";
  public static final String PRICES = "prices";
  public static final String PARTS = "parts";
  public static final String IMAGES = "images";
  public static final String PAYMENTS = "payments";
  public static final String STATUSES = "statuses";
  private static final String EXTRA_MODULE = "management_module";

  private ManagementRepository repository;
  private String module;
  private ModuleInfo moduleInfo;
  private String selectedBranch = "All";
  private Long pendingImageAppointmentId;

  private final ActivityResultLauncher<String[]> imagePicker = registerForActivityResult(
      new ActivityResultContracts.OpenDocument(), this::saveSelectedImage);

  public static void open(Activity activity, String module) {
    Intent intent = new Intent(activity, ManagementModuleActivity.class);
    intent.putExtra(EXTRA_MODULE, module);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showManagementLayout(R.layout.activity_management_module);
    repository = new ManagementRepository(this);
    module = getIntent().getStringExtra(EXTRA_MODULE);
    if (module == null)
      module = APPOINTMENTS;
    moduleInfo = moduleInfo(module);

    ((TextView) findViewById(R.id.tvModuleEyebrow)).setText(moduleInfo.eyebrow);
    ((TextView) findViewById(R.id.tvModuleTitle)).setText(moduleInfo.title);
    ((TextView) findViewById(R.id.tvModuleSection)).setText(moduleInfo.section);
    findViewById(R.id.btnModuleBack).setOnClickListener(view -> finish());
    findViewById(R.id.btnModuleAdd).setOnClickListener(view -> handleAdd());
    bindFilter(R.id.filterAll, "All");
    bindFilter(R.id.filterColombo, "Colombo");
    bindFilter(R.id.filterGalle, "Galle");
    reloadModule();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (repository != null)
      reloadModule();
  }

  private void bindFilter(int viewId, String branch) {
    findViewById(viewId).setOnClickListener(view -> {
      selectedBranch = branch;
      updateFilterAppearance();
      reloadModule();
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

  private void reloadModule() {
    try {
      ManagementRepository.ModuleSummary summary = repository.getSummary(module, selectedBranch);
      ((TextView) findViewById(R.id.tvModuleMetric)).setText(summary.metric);
      ((TextView) findViewById(R.id.tvModuleMetricLabel)).setText(summary.label);
      ((TextView) findViewById(R.id.tvModuleTrend)).setText(summary.trend);
      renderItems(repository.getRecords(module, selectedBranch));
    } catch (RuntimeException exception) {
      showError(exception);
    }
  }

  private void renderItems(List<ManagementRepository.ManagementRecord> items) {
    LinearLayout container = findViewById(R.id.managementList);
    container.removeAllViews();
    LayoutInflater inflater = LayoutInflater.from(this);
    for (ManagementRepository.ManagementRecord item : items) {
      View card = inflater.inflate(R.layout.view_management_item, container, false);
      ((TextView) card.findViewById(R.id.tvManagementItemCode)).setText(item.code);
      TextView status = card.findViewById(R.id.tvManagementItemStatus);
      status.setText(item.status.toUpperCase(Locale.US));
      styleStatus(status, item.status);
      ImageView preview = card.findViewById(R.id.ivManagementItemPreview);
      if (IMAGES.equals(module)) {
        preview.setVisibility(View.VISIBLE);
        if (item.imagePath != null) {
          if (item.imagePath.startsWith("https://") || item.imagePath.startsWith("http://"))
            RemoteImageLoader.load(preview, item.imagePath);
          else
            preview.setImageURI(Uri.parse(item.imagePath));
        }
      }
      ((TextView) card.findViewById(R.id.tvManagementItemTitle)).setText(item.title);
      ((TextView) card.findViewById(R.id.tvManagementItemMeta)).setText(item.meta);
      ((TextView) card.findViewById(R.id.tvManagementItemDetail)).setText(item.detail);
      TextView action = card.findViewById(R.id.btnManagementItemAction);
      action.setText(item.action);
      View.OnClickListener listener = view -> handleItemAction(item);
      action.setOnClickListener(listener);
      card.setOnClickListener(listener);
      container.addView(card);
    }
    if (container.getChildCount() == 0)
      addEmptyState(container);
  }

  private void addEmptyState(LinearLayout container) {
    TextView empty = new TextView(this);
    empty.setText("No " + moduleInfo.title.toLowerCase(Locale.US) + " found for this selection.");
    empty.setTextColor(getColor(R.color.management_muted));
    empty.setTextSize(14);
    empty.setPadding(0, dp(32), 0, dp(32));
    container.addView(empty);
  }

  private void handleItemAction(ManagementRepository.ManagementRecord item) {
    try {
      switch (module) {
        case APPOINTMENTS:
        case STATUSES:
          showStatusPicker(item.id);
          break;
        case TECHNICIANS:
          boolean activate = "OFF DUTY".equalsIgnoreCase(item.status);
          repository.setTechnicianActive(item.id, activate);
          reloadModule();
          break;
        case PRICES:
          showNumberEditor("Update service price", "Price in LKR",
              value -> repository.updateServicePrice(item.id, value));
          break;
        case PARTS:
          showNumberEditor("Adjust available stock", "Quantity",
              value -> repository.updatePartQuantity(item.id, Math.toIntExact(value)));
          break;
        case IMAGES:
          repository.featureRepairImage(item.id);
          reloadModule();
          break;
        case PAYMENTS:
          if ("PAID".equalsIgnoreCase(item.status)) {
            Toast.makeText(this, "Payment receipt is ready.", Toast.LENGTH_SHORT).show();
          } else {
            repository.markPaymentPaid(item.id);
            reloadModule();
          }
          break;
        default:
          break;
      }
    } catch (RuntimeException exception) {
      showError(exception);
    }
  }

  private void showStatusPicker(long appointmentId) {
    AppointmentStatus[] statuses = {AppointmentStatus.ASSIGNED, AppointmentStatus.IN_PROGRESS,
        AppointmentStatus.WAITING_FOR_PARTS, AppointmentStatus.READY_FOR_PAYMENT,
        AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED};
    String[] labels = new String[statuses.length];
    for (int index = 0; index < statuses.length; index++)
      labels[index] = ManagementRepository.statusLabel(statuses[index]);
    new AlertDialog.Builder(this)
        .setTitle("Update repair status")
        .setItems(labels,
            (dialog, index) -> {
              try {
                repository.updateAppointmentStatus(appointmentId, statuses[index]);
                reloadModule();
              } catch (RuntimeException exception) {
                showError(exception);
              }
            })
        .show();
  }

  private void showNumberEditor(String title, String hint, NumberAction action) {
    EditText value = new EditText(this);
    value.setHint(hint);
    value.setInputType(InputType.TYPE_CLASS_NUMBER);
    int padding = dp(20);
    value.setPadding(padding, padding, padding, padding);
    AlertDialog dialog = new AlertDialog.Builder(this)
                             .setTitle(title)
                             .setView(value)
                             .setNegativeButton("Cancel", null)
                             .setPositiveButton("Save", null)
                             .create();
    dialog.setOnShowListener(
        ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
          String entered = value.getText().toString().trim();
          if (entered.isEmpty()) {
            value.setError("A value is required.");
            return;
          }
          try {
            action.save(Long.parseLong(entered));
            dialog.dismiss();
            reloadModule();
          } catch (RuntimeException exception) {
            value.setError(message(exception));
          }
        }));
    dialog.show();
  }

  private void handleAdd() {
    switch (module) {
      case IMAGES:
        chooseAppointment("Attach image to repair", choice -> {
          pendingImageAppointmentId = choice.id;
          imagePicker.launch(new String[] {"image/*"});
        });
        return;
      case PAYMENTS:
        chooseAppointment("Record payment for repair", choice -> {
          repository.createPendingPayment(choice.id);
          reloadModule();
        });
        return;
      case STATUSES:
        chooseAppointment("Choose repair to update", choice -> showStatusPicker(choice.id));
        return;
      default:
        showCreateDialog();
    }
  }

  private void showCreateDialog() {
    EditText name = new EditText(this);
    name.setHint(moduleInfo.addHint);
    int padding = dp(20);
    name.setPadding(padding, padding, padding, padding);
    AlertDialog dialog = new AlertDialog.Builder(this)
                             .setTitle(moduleInfo.addTitle)
                             .setView(name)
                             .setNegativeButton("Cancel", null)
                             .setPositiveButton("Save", null)
                             .create();
    dialog.setOnShowListener(
        ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
          String value = name.getText().toString().trim();
          if (value.isEmpty()) {
            name.setError("This field is required.");
            return;
          }
          try {
            switch (module) {
              case APPOINTMENTS:
                repository.createWalkInAppointment(value, selectedBranch);
                break;
              case TECHNICIANS:
                repository.addTechnician(value, selectedBranch);
                break;
              case PRICES:
                repository.addService(value);
                break;
              case PARTS:
                repository.addSparePart(value, selectedBranch);
                break;
              default:
                return;
            }
            dialog.dismiss();
            reloadModule();
          } catch (RuntimeException exception) {
            name.setError(message(exception));
          }
        }));
    dialog.show();
  }

  private void chooseAppointment(String title, AppointmentAction action) {
    try {
      List<ManagementRepository.AppointmentChoice> choices = repository.getAppointmentChoices();
      if (choices.isEmpty()) {
        Toast.makeText(this, "No repair appointments are available.", Toast.LENGTH_SHORT).show();
        return;
      }
      String[] labels = new String[choices.size()];
      for (int index = 0; index < choices.size(); index++) labels[index] = choices.get(index).label;
      new AlertDialog.Builder(this)
          .setTitle(title)
          .setItems(labels,
              (dialog, index) -> {
                try {
                  action.run(choices.get(index));
                } catch (RuntimeException exception) {
                  showError(exception);
                }
              })
          .show();
    } catch (RuntimeException exception) {
      showError(exception);
    }
  }

  private void saveSelectedImage(Uri uri) {
    if (uri == null || pendingImageAppointmentId == null)
      return;
    try {
      getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (SecurityException ignored) {
      // The selected provider may only grant access for the current app session.
    }
    try {
      repository.addRepairImage(pendingImageAppointmentId, uri.toString());
      pendingImageAppointmentId = null;
      reloadModule();
    } catch (RuntimeException exception) {
      showError(exception);
    }
  }

  private void styleStatus(TextView status, String value) {
    String normalized = value.toUpperCase(Locale.US);
    boolean warning = normalized.contains("PENDING") || normalized.contains("LOW")
        || normalized.contains("BUSY") || normalized.contains("WAITING")
        || normalized.contains("OFF DUTY") || normalized.contains("CANCELLED");
    status.setBackgroundResource(
        warning ? R.drawable.bg_management_status_warning : R.drawable.bg_management_status);
    status.setTextColor(getColor(warning ? R.color.management_amber : R.color.management_green));
  }

  private void showError(RuntimeException exception) {
    Toast.makeText(this, message(exception), Toast.LENGTH_LONG).show();
  }

  private String message(RuntimeException exception) {
    return exception.getMessage() == null ? "Unable to complete the management action."
                                          : exception.getMessage();
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private ModuleInfo moduleInfo(String requestedModule) {
    switch (requestedModule) {
      case TECHNICIANS:
        return new ModuleInfo(
            "WORKFORCE", "Technicians", "Team availability", "Add technician", "Technician name");
      case PRICES:
        return new ModuleInfo(
            "SERVICE CATALOG", "Service prices", "Current pricing", "Add service", "Service name");
      case PARTS:
        return new ModuleInfo(
            "INVENTORY", "Spare parts", "Stock levels", "Add spare part", "Part name or SKU");
      case IMAGES:
        return new ModuleInfo("MEDIA LIBRARY", "Repair gallery", "Recent work",
            "Upload repair image", "Image caption");
      case PAYMENTS:
        return new ModuleInfo(
            "FINANCE", "Payments", "Latest transactions", "Record payment", "Appointment ID");
      case STATUSES:
        return new ModuleInfo(
            "REPAIR WORKFLOW", "Status updates", "Update queue", "Create update", "Appointment ID");
      case APPOINTMENTS:
      default:
        return new ModuleInfo("BOOKING DESK", "Appointments", "Repair queue", "Create appointment",
            "Customer device");
    }
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }

  private interface NumberAction {
    void save(long value);
  }

  private interface AppointmentAction {
    void run(ManagementRepository.AppointmentChoice choice);
  }

  private static final class ModuleInfo {
    final String eyebrow;
    final String title;
    final String section;
    final String addTitle;
    final String addHint;

    ModuleInfo(String eyebrow, String title, String section, String addTitle, String addHint) {
      this.eyebrow = eyebrow;
      this.title = title;
      this.section = section;
      this.addTitle = addTitle;
      this.addHint = addHint;
    }
  }
}
