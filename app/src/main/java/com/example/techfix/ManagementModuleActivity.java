package com.example.techfix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.techfix.data.model.AppointmentStatus;
import java.util.List;
import java.util.Locale;

public class ManagementModuleActivity extends ManagementScreen {
  public static final String APPOINTMENTS = "appointments";
  public static final String BRANCHES = "branches";
  public static final String CATEGORIES = "categories";
  public static final String TECHNICIANS = "technicians";
  public static final String PRICES = "prices";
  public static final String PARTS = "parts";
  public static final String IMAGES = "images";
  public static final String PAYMENTS = "payments";
  public static final String STATUSES = "statuses";
  private static final String EXTRA_MODULE = "management_module";

  private ManagementRepository repository;
  private FirebaseManagementApi managementApi;
  private String module;
  private ModuleInfo moduleInfo;
  private String selectedBranch = "All";
  private Long pendingImageAppointmentId;
  private final FirebaseRealtimeSync.DataObserver dataObserver =
      () -> runOnUiThread(() -> {
        if (repository != null)
          reloadModule();
      });

  private final ActivityResultLauncher<String[]> imagePicker =
      registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                                uri -> {
                                  if (uri == null)
                                    pendingImageAppointmentId = null;
                                  else
                                    saveSelectedImage(uri);
                                });
  private final ActivityResultLauncher<Intent> cameraCapture =
      registerForActivityResult(
          new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != RESULT_OK ||
                result.getData() == null) {
              pendingImageAppointmentId = null;
              return;
            }
            String imageUri = result.getData().getStringExtra(
                RepairCameraActivity.RESULT_IMAGE_URI);
            if (imageUri != null)
              saveSelectedImage(Uri.parse(imageUri));
            else
              pendingImageAppointmentId = null;
          });

  public static void open(Activity activity, String module) {
    Intent intent = new Intent(activity, ManagementModuleActivity.class);
    intent.putExtra(EXTRA_MODULE, module);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!showManagementLayout(R.layout.activity_management_module))
      return;
    repository = new ManagementRepository(this);
    managementApi = new FirebaseManagementApi();
    module = getIntent().getStringExtra(EXTRA_MODULE);
    if (module == null)
      module = APPOINTMENTS;
    moduleInfo = moduleInfo(module);

    ((TextView)findViewById(R.id.tvModuleEyebrow)).setText(moduleInfo.eyebrow);
    ((TextView)findViewById(R.id.tvModuleTitle)).setText(moduleInfo.title);
    ((TextView)findViewById(R.id.tvModuleSection)).setText(moduleInfo.section);
    findViewById(R.id.btnModuleBack).setOnClickListener(view -> finish());
    findViewById(R.id.btnModuleAdd).setOnClickListener(view -> handleAdd());
    bindFilter(R.id.filterAll, "All");
    bindFilter(R.id.filterColombo, "Colombo");
    bindFilter(R.id.filterGalle, "Galle");
    FirebaseRealtimeSync.addObserver(dataObserver);
    reloadModule();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (repository != null)
      reloadModule();
  }

  @Override
  protected void onDestroy() {
    FirebaseRealtimeSync.removeObserver(dataObserver);
    if (repository != null)
      repository.close();
    super.onDestroy();
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
    filter.setBackgroundResource(active ? R.drawable.bg_management_chip_active
                                        : R.drawable.bg_management_chip);
    filter.setTextColor(
        getColor(active ? R.color.management_cyan : R.color.management_muted));
  }

  private void reloadModule() {
    try {
      ManagementRepository.ModuleSummary summary =
          repository.getSummary(module, selectedBranch);
      ((TextView)findViewById(R.id.tvModuleMetric)).setText(summary.metric);
      ((TextView)findViewById(R.id.tvModuleMetricLabel)).setText(summary.label);
      ((TextView)findViewById(R.id.tvModuleTrend)).setText(summary.trend);
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
      View card =
          inflater.inflate(R.layout.view_management_item, container, false);
      ((TextView)card.findViewById(R.id.tvManagementItemCode))
          .setText(item.code);
      TextView status = card.findViewById(R.id.tvManagementItemStatus);
      status.setText(item.status.toUpperCase(Locale.US));
      styleStatus(status, item.status);
      ImageView preview = card.findViewById(R.id.ivManagementItemPreview);
      if (IMAGES.equals(module)) {
        preview.setVisibility(View.VISIBLE);
        if (item.imagePath != null) {
          if (item.imagePath.startsWith("https://") ||
              item.imagePath.startsWith("http://"))
            RemoteImageLoader.load(preview, item.imagePath);
          else
            preview.setImageURI(Uri.parse(item.imagePath));
        }
      }
      ((TextView)card.findViewById(R.id.tvManagementItemTitle))
          .setText(item.title);
      ((TextView)card.findViewById(R.id.tvManagementItemMeta))
          .setText(item.meta);
      ((TextView)card.findViewById(R.id.tvManagementItemDetail))
          .setText(item.detail);
      TextView action = card.findViewById(R.id.btnManagementItemAction);
      action.setText(item.action);
      View.OnClickListener listener = view -> handleItemAction(item);
      action.setOnClickListener(listener);
      TextView statusAction = card.findViewById(R.id.btnManagementItemStatus);
      if (APPOINTMENTS.equals(module) || STATUSES.equals(module)) {
        statusAction.setVisibility(View.VISIBLE);
        statusAction.setOnClickListener(view -> showStatusPicker(item.id));
      }
      card.setOnClickListener(listener);
      container.addView(card);
    }
    if (container.getChildCount() == 0)
      addEmptyState(container);
  }

  private void addEmptyState(LinearLayout container) {
    TextView empty = new TextView(this);
    empty.setText("No " + moduleInfo.title.toLowerCase(Locale.US) +
                  " found for this selection.");
    empty.setTextColor(getColor(R.color.management_muted));
    empty.setTextSize(14);
    empty.setPadding(0, dp(32), 0, dp(32));
    container.addView(empty);
  }

  private void handleItemAction(ManagementRepository.ManagementRecord item) {
    try {
      switch (module) {
      case BRANCHES:
        repository.setBranchActive(item.id,
                                   "INACTIVE".equalsIgnoreCase(item.status));
        reloadModule();
        break;
      case CATEGORIES:
        repository.setCategoryActive(
            item.id, "INACTIVE".equalsIgnoreCase(item.status));
        reloadModule();
        break;
      case APPOINTMENTS:
        showAppointmentActions(item.id);
        break;
      case STATUSES:
        showStatusPicker(item.id);
        break;
      case TECHNICIANS:
        boolean activate = "OFF DUTY".equalsIgnoreCase(item.status);
        repository.setTechnicianActive(item.id, activate);
        reloadModule();
        break;
      case PRICES:
        showNumberEditor(
            "Update service price", "Price in LKR",
            value -> repository.updateServicePrice(item.id, value));
        break;
      case PARTS:
        showNumberEditor(
            "Adjust available stock", "Quantity",
            value
            -> repository.updatePartQuantity(item.id, Math.toIntExact(value)));
        break;
      case IMAGES:
        repository.featureRepairImage(item.id);
        reloadModule();
        break;
      case PAYMENTS:
        if ("PAID".equalsIgnoreCase(item.status)) {
          Toast.makeText(this, "Payment receipt is ready.", Toast.LENGTH_SHORT)
              .show();
        } else {
          repository.markPaymentPaid(item.id);
          Toast.makeText(this,
                         "Cash payment submitted for server confirmation.",
                         Toast.LENGTH_SHORT)
              .show();
        }
        break;
      default:
        break;
      }
    } catch (RuntimeException exception) {
      showError(exception);
    }
  }

  private void showAppointmentActions(long appointmentId) {
    String[] actions = {"Auto-assign best technician",
                        "Choose or change technician", "Update repair status"};
    new AlertDialog.Builder(this)
        .setTitle("Manage appointment")
        .setItems(actions, (dialog, index) -> {
          if (index == 0) {
            try {
              String remoteId =
                  repository.getAppointmentRemoteId(appointmentId);
              managementApi.autoAssignAppointment(
                  remoteId,
                  cloudCallback(
                      "Auto-assignment requested. Waiting for live confirmation."));
            } catch (RuntimeException exception) {
              showError(exception);
            }
          } else if (index == 1) {
            showTechnicianPicker(appointmentId);
          } else {
            showStatusPicker(appointmentId);
          }
        })
        .show();
  }

  private void showTechnicianPicker(long appointmentId) {
    List<ManagementRepository.TechnicianChoice> technicians =
        repository.getAvailableTechnicians(appointmentId);
    if (technicians.isEmpty()) {
      Toast.makeText(this,
                     "No active technician is available at this branch.",
                     Toast.LENGTH_SHORT)
          .show();
      return;
    }
    String[] labels = new String[technicians.size()];
    for (int index = 0; index < technicians.size(); index++)
      labels[index] = technicians.get(index).label();
    new AlertDialog.Builder(this)
        .setTitle("Assign technician")
        .setItems(labels, (dialog, index) -> {
          try {
            String remoteId = repository.getAppointmentRemoteId(appointmentId);
            managementApi.reassignAppointment(
                remoteId, technicians.get(index).id,
                cloudCallback(
                    "Technician change requested. Waiting for live confirmation."));
          } catch (RuntimeException exception) {
            showError(exception);
          }
        })
        .show();
  }

  private void showStatusPicker(long appointmentId) {
    AppointmentStatus[] statuses = {AppointmentStatus.ASSIGNED,
                                    AppointmentStatus.IN_PROGRESS,
                                    AppointmentStatus.WAITING_FOR_PARTS,
                                    AppointmentStatus.READY_FOR_PAYMENT,
                                    AppointmentStatus.COMPLETED,
                                    AppointmentStatus.CANCELLED};
    String[] labels = new String[statuses.length];
    for (int index = 0; index < statuses.length; index++)
      labels[index] = ManagementRepository.statusLabel(statuses[index]);
    new AlertDialog.Builder(this)
        .setTitle("Update repair status")
        .setItems(labels,
                  (dialog, index) -> {
                    try {
                      AppointmentStatus status = statuses[index];
                      String remoteId =
                          repository.getAppointmentRemoteId(appointmentId);
                      managementApi.updateRepairStatus(
                          remoteId, status,
                          "Repair status updated by management to " +
                              ManagementRepository.statusLabel(status) + ".",
                          cloudCallback(
                              "Status update requested. Waiting for live confirmation."));
                    } catch (RuntimeException exception) {
                      showError(exception);
                    }
                  })
        .show();
  }

  private void showNumberEditor(String title, String hint,
                                NumberAction action) {
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
    dialog.setOnShowListener(ignored
                             -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setOnClickListener(button -> {
                                      String entered =
                                          value.getText().toString().trim();
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
    case BRANCHES:
      showBranchDialog();
      return;
    case CATEGORIES:
      showCategoryDialog();
      return;
    case IMAGES:
      chooseAppointment("Attach image to repair", choice -> {
        pendingImageAppointmentId = choice.id;
        showImageSourcePicker();
      });
      return;
    case PAYMENTS:
      chooseAppointment("Record payment for repair", choice -> {
        repository.createPendingPayment(choice.id);
        reloadModule();
      });
      return;
    case STATUSES:
      chooseAppointment("Choose repair to update",
                        choice -> showStatusPicker(choice.id));
      return;
    case APPOINTMENTS:
      showAppointmentDialog();
      return;
    default:
      showCreateDialog();
    }
  }

  private void showAppointmentDialog() {
    if ("All".equals(selectedBranch)) {
      Toast.makeText(this,
                     "Select the Colombo or Galle branch before creating an appointment.",
                     Toast.LENGTH_LONG)
          .show();
      return;
    }
    try {
      List<ManagementRepository.ServiceChoice> services =
          repository.getActiveServiceChoices();
      if (services.isEmpty()) {
        Toast.makeText(this, "No active repair services are available.",
                       Toast.LENGTH_LONG)
            .show();
        return;
      }

      LinearLayout form = dialogForm();
      TextView branch = new TextView(this);
      branch.setText("Request location · " + selectedBranch + " branch");
      branch.setTextColor(getColor(R.color.management_muted));
      branch.setPadding(0, dp(8), 0, dp(8));
      form.addView(branch);
      EditText customerEmail = formField(
          form, "Customer email",
          InputType.TYPE_CLASS_TEXT |
              InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
      EditText device = formField(
          form, "Device details",
          InputType.TYPE_CLASS_TEXT |
              InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
      EditText problem = formField(
          form, "Problem description",
          InputType.TYPE_CLASS_TEXT |
              InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
              InputType.TYPE_TEXT_FLAG_MULTI_LINE);

      TextView serviceLabel = new TextView(this);
      serviceLabel.setText("Repair service");
      serviceLabel.setTextColor(getColor(R.color.management_muted));
      serviceLabel.setPadding(0, dp(12), 0, 0);
      form.addView(serviceLabel);
      Spinner servicePicker = new Spinner(this);
      String[] labels = new String[services.size()];
      for (int index = 0; index < services.size(); index++)
        labels[index] = services.get(index).label();
      ArrayAdapter<String> adapter = new ArrayAdapter<>(
          this, android.R.layout.simple_spinner_item, labels);
      adapter.setDropDownViewResource(
          android.R.layout.simple_spinner_dropdown_item);
      servicePicker.setAdapter(adapter);
      form.addView(servicePicker, new LinearLayout.LayoutParams(
                                      LinearLayout.LayoutParams.MATCH_PARENT,
                                      LinearLayout.LayoutParams.WRAP_CONTENT));

      showFormDialog(
          "Create customer appointment", form,
          () -> {
            int serviceIndex = servicePicker.getSelectedItemPosition();
            if (serviceIndex < 0 || serviceIndex >= services.size())
              throw new IllegalArgumentException(
                  "Choose an active repair service.");
            repository.createCustomerAppointment(
                customerEmail.getText().toString(),
                services.get(serviceIndex).id, device.getText().toString(),
                problem.getText().toString(), selectedBranch);
          });
    } catch (RuntimeException exception) {
      showError(exception);
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
        ignored
        -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
               .setOnClickListener(button -> {
                 String value = name.getText().toString().trim();
                 if (value.isEmpty()) {
                   name.setError("This field is required.");
                   return;
                 }
                 try {
                   switch (module) {
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

  private void showBranchDialog() {
    LinearLayout form = dialogForm();
    EditText name = formField(form, "Branch name", InputType.TYPE_CLASS_TEXT);
    EditText address = formField(form, "Address", InputType.TYPE_CLASS_TEXT);
    EditText phone = formField(form, "Phone", InputType.TYPE_CLASS_PHONE);
    EditText latitude = formField(form, "Latitude",
                                  InputType.TYPE_CLASS_NUMBER |
                                      InputType.TYPE_NUMBER_FLAG_DECIMAL |
                                      InputType.TYPE_NUMBER_FLAG_SIGNED);
    EditText longitude = formField(form, "Longitude",
                                   InputType.TYPE_CLASS_NUMBER |
                                       InputType.TYPE_NUMBER_FLAG_DECIMAL |
                                       InputType.TYPE_NUMBER_FLAG_SIGNED);
    showFormDialog(
        "Add service branch", form,
        ()
            -> repository.addBranch(
                name.getText().toString(), address.getText().toString(),
                phone.getText().toString(),
                Double.parseDouble(latitude.getText().toString().trim()),
                Double.parseDouble(longitude.getText().toString().trim())));
  }

  private void showCategoryDialog() {
    LinearLayout form = dialogForm();
    EditText name = formField(form, "Category name", InputType.TYPE_CLASS_TEXT);
    EditText description = formField(
        form, "Description",
        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES |
            InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    showFormDialog(
        "Add device category", form,
        ()
            -> repository.addCategory(name.getText().toString(),
                                      description.getText().toString()));
  }

  private LinearLayout dialogForm() {
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(20), dp(8), dp(20), 0);
    return form;
  }

  private EditText formField(LinearLayout form, String hint, int inputType) {
    EditText field = new EditText(this);
    field.setHint(hint);
    field.setInputType(inputType);
    field.setPadding(0, dp(12), 0, dp(12));
    form.addView(field, new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
    return field;
  }

  private void showFormDialog(String title, LinearLayout form,
                              FormAction action) {
    AlertDialog dialog = new AlertDialog.Builder(this)
                             .setTitle(title)
                             .setView(form)
                             .setNegativeButton("Cancel", null)
                             .setPositiveButton("Save", null)
                             .create();
    dialog.setOnShowListener(
        ignored
        -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
               .setOnClickListener(button -> {
                 try {
                   action.save();
                   dialog.dismiss();
                   reloadModule();
                 } catch (RuntimeException exception) {
                   Toast.makeText(this, message(exception), Toast.LENGTH_LONG)
                       .show();
                 }
               }));
    dialog.show();
  }

  private void chooseAppointment(String title, AppointmentAction action) {
    try {
      List<ManagementRepository.AppointmentChoice> choices =
          repository.getAppointmentChoices();
      if (choices.isEmpty()) {
        Toast
            .makeText(this, "No repair appointments are available.",
                      Toast.LENGTH_SHORT)
            .show();
        return;
      }
      String[] labels = new String[choices.size()];
      for (int index = 0; index < choices.size(); index++)
        labels[index] = choices.get(index).label;
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
      getContentResolver().takePersistableUriPermission(
          uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    } catch (SecurityException ignored) {
      // The selected provider may only grant access for the current app
      // session.
    }
    try {
      repository.addRepairImage(pendingImageAppointmentId, uri.toString());
      pendingImageAppointmentId = null;
      reloadModule();
    } catch (RuntimeException exception) {
      showError(exception);
    }
  }

  private void showImageSourcePicker() {
    new AlertDialog.Builder(this)
        .setTitle("Add repaired-device photo")
        .setItems(new String[] {"Take photo", "Choose from gallery"},
                  (dialog, index) -> {
                    if (index == 0)
                      cameraCapture.launch(
                          new Intent(this, RepairCameraActivity.class));
                    else
                      imagePicker.launch(new String[] {"image/*"});
                  })
        .setOnCancelListener(dialog -> pendingImageAppointmentId = null)
        .show();
  }

  private void styleStatus(TextView status, String value) {
    String normalized = value.toUpperCase(Locale.US);
    boolean warning =
        normalized.contains("PENDING") || normalized.contains("LOW") ||
        normalized.contains("BUSY") || normalized.contains("WAITING") ||
        normalized.contains("OFF DUTY") || normalized.contains("CANCELLED");
    status.setBackgroundResource(warning
                                     ? R.drawable.bg_management_status_warning
                                     : R.drawable.bg_management_status);
    status.setTextColor(getColor(warning ? R.color.management_amber
                                         : R.color.management_green));
  }

  private void showError(RuntimeException exception) {
    Toast.makeText(this, message(exception), Toast.LENGTH_LONG).show();
  }

  private FirebaseManagementApi.Callback cloudCallback(
      String acceptedMessage) {
    return new FirebaseManagementApi.Callback() {
      @Override
      public void onSuccess() {
        if (canShowCloudResult())
          Toast.makeText(ManagementModuleActivity.this, acceptedMessage,
                         Toast.LENGTH_SHORT)
              .show();
      }

      @Override
      public void onFailure(Exception error) {
        if (!canShowCloudResult())
          return;
        String errorMessage = error == null ? null : error.getMessage();
        Toast.makeText(
                 ManagementModuleActivity.this,
                 errorMessage == null || errorMessage.trim().isEmpty()
                     ? "Unable to complete the Firebase management action."
                     : errorMessage,
                 Toast.LENGTH_LONG)
            .show();
      }
    };
  }

  private boolean canShowCloudResult() {
    return !isFinishing() && !isDestroyed();
  }

  private String message(RuntimeException exception) {
    return exception.getMessage() == null
        ? "Unable to complete the management action."
        : exception.getMessage();
  }

  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  private ModuleInfo moduleInfo(String requestedModule) {
    switch (requestedModule) {
    case BRANCHES:
      return new ModuleInfo("LOCATIONS", "Branches", "Service locations",
                            "Add branch", "Branch name");
    case CATEGORIES:
      return new ModuleInfo("DEVICE CATALOG", "Device categories",
                            "Supported devices", "Add category",
                            "Category name");
    case TECHNICIANS:
      return new ModuleInfo("WORKFORCE", "Technicians", "Team availability",
                            "Add technician", "Technician name");
    case PRICES:
      return new ModuleInfo("SERVICE CATALOG", "Service prices",
                            "Current pricing", "Add service", "Service name");
    case PARTS:
      return new ModuleInfo("INVENTORY", "Spare parts", "Stock levels",
                            "Add spare part", "Part name or SKU");
    case IMAGES:
      return new ModuleInfo("MEDIA LIBRARY", "Repair gallery", "Recent work",
                            "Upload repair image", "Image caption");
    case PAYMENTS:
      return new ModuleInfo("FINANCE", "Payments", "Latest transactions",
                            "Record payment", "Appointment ID");
    case STATUSES:
      return new ModuleInfo("REPAIR WORKFLOW", "Status updates", "Update queue",
                            "Create update", "Appointment ID");
    case APPOINTMENTS:
    default:
      return new ModuleInfo("BOOKING DESK", "Appointments", "Repair queue",
                            "Create appointment", "Customer device");
    }
  }

  private interface NumberAction {
    void save(long value);
  }

  private interface FormAction {
    void save();
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

    ModuleInfo(String eyebrow, String title, String section, String addTitle,
               String addHint) {
      this.eyebrow = eyebrow;
      this.title = title;
      this.section = section;
      this.addTitle = addTitle;
      this.addHint = addHint;
    }
  }
}
