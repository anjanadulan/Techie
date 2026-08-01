package com.example.techfix;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookRepairActivity extends CustomerScreen {
  private CustomerRepository repository;
  private SessionManager sessionManager;
  private List<CustomerRepository.ServiceItem> services;
  private List<CustomerRepository.BranchItem> branches;
  private CustomerRepository.ServiceItem selectedService;
  private CustomerRepository.BranchItem selectedBranch;
  private String selectedDevice = "Samsung Galaxy S23";
  private String selectedImagePath;
  private final Calendar appointmentTime = Calendar.getInstance();
  private final ActivityResultLauncher<Intent> imagePicker =
      registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() != RESULT_OK || result.getData() == null)
          return;
        Uri uri = result.getData().getData();
        if (uri == null)
          return;
        selectedImagePath = uri.toString();
        try {
          getContentResolver().takePersistableUriPermission(
              uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
          // Some document providers only grant access for the current session.
        }
        ((TextView) findViewById(R.id.tvPhotoStatus)).setText("Photo attached");
      });

  public static void open(Activity activity, long serviceId) {
    Intent intent = new Intent(activity, BookRepairActivity.class);
    intent.putExtra(ServiceDetailActivity.EXTRA_SERVICE_ID, serviceId);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    showCustomerLayout(R.layout.activity_book_repair);
    repository = new CustomerRepository(this);
    sessionManager = new SessionManager(this);
    services = repository.getServices("");
    branches = repository.getBranches();

    appointmentTime.add(Calendar.DAY_OF_MONTH, 1);
    appointmentTime.set(Calendar.HOUR_OF_DAY, 10);
    appointmentTime.set(Calendar.MINUTE, 30);
    appointmentTime.set(Calendar.SECOND, 0);
    appointmentTime.set(Calendar.MILLISECOND, 0);

    long requestedServiceId = getIntent().getLongExtra(ServiceDetailActivity.EXTRA_SERVICE_ID, -1);
    for (CustomerRepository.ServiceItem service : services) {
      if (service.id == requestedServiceId)
        selectedService = service;
    }
    if (selectedService == null && !services.isEmpty())
      selectedService = services.get(0);
    if (!branches.isEmpty())
      selectedBranch = branches.get(0);

    findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    findViewById(R.id.pickerDevice).setOnClickListener(v -> chooseDevice());
    findViewById(R.id.pickerService).setOnClickListener(v -> chooseService());
    findViewById(R.id.pickerBranch).setOnClickListener(v -> chooseBranch());
    findViewById(R.id.pickerDate).setOnClickListener(v -> chooseDate());
    findViewById(R.id.pickerTime).setOnClickListener(v -> chooseTime());
    findViewById(R.id.addPhotoCard).setOnClickListener(v -> choosePhoto());
    findViewById(R.id.btnContinue).setOnClickListener(v -> submitBooking());
    renderSelections();
  }

  private void chooseDevice() {
    String[] devices = {"Samsung Galaxy S23", "Apple iPhone", "Dell Inspiron 15", "Apple iPad Air",
        "Custom device"};
    new AlertDialog.Builder(this)
        .setTitle(R.string.device)
        .setItems(devices,
            (dialog, index) -> {
              selectedDevice = devices[index];
              renderSelections();
            })
        .show();
  }

  private void chooseService() {
    String[] names = new String[services.size()];
    for (int index = 0; index < services.size(); index++) names[index] = services.get(index).name;
    new AlertDialog.Builder(this)
        .setTitle(R.string.service)
        .setItems(names,
            (dialog, index) -> {
              selectedService = services.get(index);
              renderSelections();
            })
        .show();
  }

  private void chooseBranch() {
    String[] names = new String[branches.size()];
    for (int index = 0; index < branches.size(); index++) {
      names[index] = branches.get(index).name + " · " + branches.get(index).address;
    }
    new AlertDialog.Builder(this)
        .setTitle(R.string.preferred_branch)
        .setItems(names,
            (dialog, index) -> {
              selectedBranch = branches.get(index);
              renderSelections();
            })
        .show();
  }

  private void chooseDate() {
    new DatePickerDialog(this,
        (picker, year, month, day)
            -> {
          appointmentTime.set(year, month, day);
          renderSelections();
        },
        appointmentTime.get(Calendar.YEAR), appointmentTime.get(Calendar.MONTH),
        appointmentTime.get(Calendar.DAY_OF_MONTH))
        .show();
  }

  private void chooseTime() {
    new TimePickerDialog(this,
        (picker, hour, minute)
            -> {
          appointmentTime.set(Calendar.HOUR_OF_DAY, hour);
          appointmentTime.set(Calendar.MINUTE, minute);
          renderSelections();
        },
        appointmentTime.get(Calendar.HOUR_OF_DAY), appointmentTime.get(Calendar.MINUTE), false)
        .show();
  }

  private void renderSelections() {
    ((TextView) findViewById(R.id.pickerDevice)).setText(selectedDevice + "  ›");
    ((TextView) findViewById(R.id.pickerService))
        .setText(selectedService == null ? "No services available" : selectedService.name + "  ›");
    ((TextView) findViewById(R.id.pickerBranch))
        .setText(
            selectedBranch == null ? "No branches available" : selectedBranch.name + " branch  ›");
    ((TextView) findViewById(R.id.pickerDate))
        .setText(new SimpleDateFormat("dd MMM yyyy", Locale.US).format(appointmentTime.getTime()));
    ((TextView) findViewById(R.id.pickerTime))
        .setText(new SimpleDateFormat("h:mm a", Locale.US).format(appointmentTime.getTime()));
  }

  private void choosePhoto() {
    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
    intent.setType("image/*");
    intent.addCategory(Intent.CATEGORY_OPENABLE);
    intent.addFlags(
        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
    imagePicker.launch(intent);
  }

  private void submitBooking() {
    String issue = ((EditText) findViewById(R.id.etIssueDescription)).getText().toString().trim();
    if (selectedService == null || selectedBranch == null) {
      Toast.makeText(this, "Service and branch data are unavailable.", Toast.LENGTH_SHORT).show();
      return;
    }
    if (issue.isEmpty()) {
      ((EditText) findViewById(R.id.etIssueDescription)).setError("Describe the device issue.");
      return;
    }
    try {
      long appointmentId = repository.createAppointment(sessionManager.getUserId(),
          selectedBranch.id, selectedService.id, selectedDevice, issue,
          appointmentTime.getTimeInMillis(), selectedImagePath);
      Toast.makeText(this, R.string.booking_created, Toast.LENGTH_SHORT).show();
      RepairTrackingActivity.open(this, appointmentId);
      finish();
    } catch (IllegalArgumentException exception) {
      Toast.makeText(this, exception.getMessage(), Toast.LENGTH_LONG).show();
    } catch (RuntimeException exception) {
      Toast.makeText(this, R.string.booking_failed, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  protected void onDestroy() {
    if (repository != null)
      repository.close();
    super.onDestroy();
  }
}
