package com.example.techfixv2;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookRepairActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // View Components
    private TextView btnBack;
    private TextView pickerDevice;
    private TextView pickerService;
    private TextView pickerBranch;
    private TextView pickerDate;
    private TextView pickerTime;
    private EditText etIssueDescription;
    private View addPhotoCard;
    private TextView tvPhotoStatus;
    private View btnContinue;

    // Data lists fetched from Firestore
    private List<String> deviceList = new ArrayList<>();
    private List<String> serviceNames = new ArrayList<>();
    private List<Double> servicePrices = new ArrayList<>();
    private List<String> branchList = new ArrayList<>();

    // Selected Values
    private String selectedDevice = "";
    private String selectedService = "";
    private double selectedCost = 0.0;
    private String selectedBranch = "";
    private String selectedDate = "";
    private String selectedTime = "";
    private Uri selectedImageUri = null;

    private static final int PICK_IMAGE_REQUEST = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_repair);

        dbHelper = new DatabaseHelper(this);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        btnBack = findViewById(R.id.btnBack);
        pickerDevice = findViewById(R.id.pickerDevice);
        pickerService = findViewById(R.id.pickerService);
        pickerBranch = findViewById(R.id.pickerBranch);
        pickerDate = findViewById(R.id.pickerDate);
        pickerTime = findViewById(R.id.pickerTime);
        etIssueDescription = findViewById(R.id.etIssueDescription);
        addPhotoCard = findViewById(R.id.addPhotoCard);
        tvPhotoStatus = findViewById(R.id.tvPhotoStatus);
        btnContinue = findViewById(R.id.btnContinue);

        // Fetch picker options from Firestore
        fetchOptionsFromFirestore();

        // Setup click listeners
        btnBack.setOnClickListener(v -> finish());

        pickerDevice.setOnClickListener(v -> showDevicePickerDialog());
        pickerService.setOnClickListener(v -> showServicePickerDialog());
        pickerBranch.setOnClickListener(v -> showBranchPickerDialog());
        pickerDate.setOnClickListener(v -> openDatePicker());
        pickerTime.setOnClickListener(v -> openTimePicker());

        addPhotoCard.setOnClickListener(v -> openGalleryPicker());

        btnContinue.setOnClickListener(v -> saveBookingToFirestore());
    }

    private void fetchOptionsFromFirestore() {
        // 1. Fetch devices
        db.collection("device_categories").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                deviceList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String name = doc.getString("categoryName");
                    if (name != null) deviceList.add(name);
                }
            } else {
                // Fallbacks in case Firestore collection is unpopulated
                deviceList.clear();
                deviceList.add("iPhone / iOS Device");
                deviceList.add("Android Smartphone");
                deviceList.add("Apple MacBook / Laptop");
                deviceList.add("iPad / Tablet");
            }
        });

        // 2. Fetch services
        db.collection("service_prices").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                serviceNames.clear();
                servicePrices.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String name = doc.getString("serviceName");
                    Object priceVal = doc.get("estimatedPrice");
                    if (name != null && priceVal != null) {
                        serviceNames.add(name);
                        servicePrices.add(Double.parseDouble(String.valueOf(priceVal)));
                    }
                }
            } else {
                // Fallbacks
                serviceNames.clear();
                servicePrices.clear();
                serviceNames.add("Screen Replacement");
                servicePrices.add(15000.0);
                serviceNames.add("Battery Replacement");
                servicePrices.add(8500.0);
                serviceNames.add("Keyboard Repair");
                servicePrices.add(12000.0);
                serviceNames.add("Charging Port Repair");
                servicePrices.add(6500.0);
            }
        });

        // 3. Fetch branches
        db.collection("branches").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                branchList.clear();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    String name = doc.getString("name");
                    if (name != null) branchList.add(name);
                }
            } else {
                // Fallbacks
                branchList.clear();
                branchList.add("Colombo");
                branchList.add("Galle");
            }
        });
    }

    private void showDevicePickerDialog() {
        if (deviceList.isEmpty()) {
            Toast.makeText(this, "Loading devices...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] devices = deviceList.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select Device Category")
                .setItems(devices, (dialog, which) -> {
                    selectedDevice = devices[which];
                    pickerDevice.setText(selectedDevice);
                }).show();
    }

    private void showServicePickerDialog() {
        if (serviceNames.isEmpty()) {
            Toast.makeText(this, "Loading services...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] services = serviceNames.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select Service Type")
                .setItems(services, (dialog, which) -> {
                    selectedService = services[which];
                    selectedCost = servicePrices.get(which);
                    pickerService.setText(selectedService + " (Est: LKR " + (int) selectedCost + ")");
                }).show();
    }

    private void showBranchPickerDialog() {
        if (branchList.isEmpty()) {
            Toast.makeText(this, "Loading branches...", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] branches = branchList.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Select Service Branch")
                .setItems(branches, (dialog, which) -> {
                    selectedBranch = branches[which];
                    pickerBranch.setText(selectedBranch);
                }).show();
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = year + "-" + String.format("%02d", (month + 1)) + "-" + String.format("%02d", dayOfMonth);
                    pickerDate.setText(selectedDate);
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        // Prevent booking past dates
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void openTimePicker() {
        Calendar cal = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String amPm = hourOfDay >= 12 ? "PM" : "AM";
                    int formattedHour = hourOfDay > 12 ? hourOfDay - 12 : (hourOfDay == 0 ? 12 : hourOfDay);
                    selectedTime = String.format("%02d", formattedHour) + ":" + String.format("%02d", minute) + " " + amPm;
                    pickerTime.setText(selectedTime);
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
        timePickerDialog.show();
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            tvPhotoStatus.setText("Photo attached successfully");
            tvPhotoStatus.setTextColor(getResources().getColor(R.color.customer_success));
        }
    }

    private void saveBookingToFirestore() {
        if (selectedDevice.isEmpty()) {
            Toast.makeText(this, "Please select your device category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedService.isEmpty()) {
            Toast.makeText(this, "Please select a service type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedBranch.isEmpty()) {
            Toast.makeText(this, "Please select your preferred branch", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Please pick a booking date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Please pick a booking time slot", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Error: User is not authenticated", Toast.LENGTH_LONG).show();
            return;
        }

        String email = user.getEmail();
        String clientName = dbHelper.getUserName(email);
        if (clientName == null || clientName.trim().isEmpty()) {
            clientName = email.split("@")[0]; // Fallback to email prefix
        }

        String issueDesc = etIssueDescription.getText().toString().trim();
        String fullDescription = selectedService + (issueDesc.isEmpty() ? "" : " - " + issueDesc);

        // Prepare Firestore database values
        Map<String, Object> appointment = new HashMap<>();
        appointment.put("clientName", clientName);
        appointment.put("userEmail", email.trim().toLowerCase());
        appointment.put("deviceName", selectedDevice);
        appointment.put("description", fullDescription);
        appointment.put("branch", selectedBranch);
        appointment.put("date", selectedDate);
        appointment.put("time", selectedTime);
        appointment.put("cost", selectedCost);
        appointment.put("status", "Pending");

        btnContinue.setEnabled(false);

        db.collection("appointments")
                .add(appointment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(BookRepairActivity.this, "Booking created successfully!", Toast.LENGTH_LONG).show();
                    
                    // Finish and redirect to CustomerHome
                    Intent intent = new Intent(BookRepairActivity.this, CustomerHome.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BookRepairActivity.this, "Booking failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    btnContinue.setEnabled(true);
                });
    }
}
