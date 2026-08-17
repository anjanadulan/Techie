package com.example.techfixv2;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
    private ImageView ivPhotoPreview;
    private View layoutPhotoPlaceholder;
    private View layoutRetakeOverlay;

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

    // Edit Mode properties
    private boolean isEditMode = false;
    private String bookingId = "";

    private static final int PICK_IMAGE_REQUEST = 102;
    private static final int CAMERA_IMAGE_REQUEST = 103;
    private static final int CAMERA_PERMISSION_CODE = 201;
    private Uri cameraImageUri = null;

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
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview);
        layoutPhotoPlaceholder = findViewById(R.id.layoutPhotoPlaceholder);
        layoutRetakeOverlay = findViewById(R.id.layoutRetakeOverlay);

        // Fetch picker options from Firestore
        fetchOptionsFromFirestore();

        // Setup click listeners
        btnBack.setOnClickListener(v -> finish());

        pickerDevice.setOnClickListener(v -> showDevicePickerDialog());
        pickerService.setOnClickListener(v -> showServicePickerDialog());
        pickerBranch.setOnClickListener(v -> showBranchPickerDialog());
        pickerDate.setOnClickListener(v -> openDatePicker());
        pickerTime.setOnClickListener(v -> openTimePicker());

        addPhotoCard.setOnClickListener(v -> showImageSourceSelector());
        if (layoutRetakeOverlay != null) {
            layoutRetakeOverlay.setOnClickListener(v -> showImageSourceSelector());
        }

        btnContinue.setOnClickListener(v -> saveBookingToFirestore());

        // Check for edit mode parameters
        if (getIntent().hasExtra("booking_id")) {
            bookingId = getIntent().getStringExtra("booking_id");
            isEditMode = true;
            if (btnContinue instanceof TextView) {
                ((TextView) btnContinue).setText("Update Booking");
            }
            loadExistingBookingDetails();
        } else if (getIntent().hasExtra("preselected_service")) {
            selectedService = getIntent().getStringExtra("preselected_service");
            String preselectedCategory = getIntent().getStringExtra("preselected_category");
            selectedCost = getIntent().getDoubleExtra("preselected_cost", 0.0);

            // Pre-populate UI picker displays
            pickerService.setText(selectedService + " (Est: LKR " + (int) selectedCost + ")");

            if (preselectedCategory != null) {
                selectedDevice = preselectedCategory.toLowerCase();
                pickerDevice.setText(selectedDevice);
            }
        }
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
                    String name = doc.getString("name");
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

    private void loadExistingBookingDetails() {
        if (bookingId == null || bookingId.isEmpty()) return;

        db.collection("appointments").document(bookingId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    selectedDevice = doc.getString("deviceName");
                    selectedBranch = doc.getString("branch");
                    selectedDate = doc.getString("date");
                    selectedTime = doc.getString("time");
                    
                    Object costVal = doc.get("cost");
                    if (costVal != null) {
                        selectedCost = Double.parseDouble(String.valueOf(costVal));
                    }

                    String fullDesc = doc.getString("description");
                    if (fullDesc != null) {
                        if (fullDesc.contains(" - ")) {
                            String[] parts = fullDesc.split(" - ", 2);
                            selectedService = parts[0];
                            etIssueDescription.setText(parts[1]);
                        } else {
                            selectedService = fullDesc;
                            etIssueDescription.setText("");
                        }
                    }

                    String photoUriStr = doc.getString("photoUri");
                    if (photoUriStr != null && !photoUriStr.isEmpty()) {
                        selectedImageUri = Uri.parse(photoUriStr);
                        showPhotoPreview(selectedImageUri);
                    }

                    // Pre-populate UI dropdown labels
                    pickerDevice.setText(selectedDevice);
                    pickerService.setText(selectedService + " (Est: LKR " + (int) selectedCost + ")");
                    pickerBranch.setText(selectedBranch);
                    pickerDate.setText(selectedDate);
                    pickerTime.setText(selectedTime);
                }
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

    private void showImageSourceSelector() {
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

        if (tvTitle != null) tvTitle.setText("Add Device Photo");
        if (tvMessage != null) tvMessage.setText("Capture a real-time diagnostic photo with your camera or select an existing image from your gallery.");

        if (btnOpt1 != null) {
            btnOpt1.setText("Take Photo with Camera");
            btnOpt1.setOnClickListener(v -> {
                dialog.dismiss();
                checkCameraPermissionAndOpen();
            });
        }

        if (btnOpt2 != null) {
            btnOpt2.setText("Choose from Gallery");
            btnOpt2.setOnClickListener(v -> {
                dialog.dismiss();
                openGalleryPicker();
            });
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to capture photos.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "TechFix Device Diagnostic");
            values.put(MediaStore.Images.Media.DESCRIPTION, "Captured by TechFix Repair App");
            cameraImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to initialize camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                selectedImageUri = data.getData();
                tvPhotoStatus.setText("Photo attached from Gallery");
                tvPhotoStatus.setTextColor(getResources().getColor(R.color.customer_success));
                showPhotoPreview(selectedImageUri);
            } else if (requestCode == CAMERA_IMAGE_REQUEST) {
                if (cameraImageUri != null) {
                    selectedImageUri = cameraImageUri;
                    tvPhotoStatus.setText("Photo captured from Camera");
                    tvPhotoStatus.setTextColor(getResources().getColor(R.color.customer_success));
                    showPhotoPreview(selectedImageUri);
                } else {
                    Toast.makeText(this, "Camera capture failed.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void showPhotoPreview(Uri uri) {
        if (ivPhotoPreview != null && layoutPhotoPlaceholder != null && uri != null) {
            ivPhotoPreview.setImageURI(uri);
            layoutPhotoPlaceholder.setVisibility(View.GONE);
            ivPhotoPreview.setVisibility(View.VISIBLE);
            if (layoutRetakeOverlay != null) {
                layoutRetakeOverlay.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveBookingToFirestore() {
        if (selectedDevice.isEmpty() || selectedDevice.contains("Select Device")) {
            Toast.makeText(this, "Please select your device category", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedService.isEmpty() || selectedService.contains("Select Service")) {
            Toast.makeText(this, "Please select a service type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedBranch.isEmpty() || selectedBranch.contains("Select Branch")) {
            Toast.makeText(this, "Please select your preferred branch", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedDate.isEmpty() || selectedDate.contains("Select Date")) {
            Toast.makeText(this, "Please pick a booking date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime.isEmpty() || selectedTime.contains("Select Time")) {
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
            clientName = email.split("@")[0];
        }

        String issueDesc = etIssueDescription.getText().toString().trim();
        String fullDescription = selectedService + (issueDesc.isEmpty() ? "" : " - " + issueDesc);

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
        appointment.put("photoUri", selectedImageUri != null ? selectedImageUri.toString() : "");

        btnContinue.setEnabled(false);

        if (isEditMode) {
            db.collection("appointments").document(bookingId)
                    .set(appointment)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(BookRepairActivity.this, "Booking updated successfully!", Toast.LENGTH_LONG).show();
                        
                        Intent intent = new Intent(BookRepairActivity.this, BookingHistory.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(BookRepairActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        btnContinue.setEnabled(true);
                    });
        } else {
            db.collection("appointments")
                    .add(appointment)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(BookRepairActivity.this, "Booking created successfully!", Toast.LENGTH_LONG).show();
                        
                        Intent intent = new Intent(BookRepairActivity.this, BookingHistory.class);
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
}
