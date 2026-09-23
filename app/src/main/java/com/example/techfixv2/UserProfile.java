package com.example.techfixv2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public class UserProfile extends AppCompatActivity {

    // views
    private TextView tvProfileName, tvProfileEmail;
    private TextView tvCurrentLocationText, tvNearestBranchText;
    private TextView tvActiveCount, tvCompletedCount, tvTotalSpent;
    private TextView tvCameraStatus, tvLocationStatus, tvStorageStatus;

    // auth and db
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;
    private FusedLocationProviderClient fusedLocationClient;

    // permission codes
    private static final int PERMISSION_REQ_CAMERA = 301;
    private static final int PERMISSION_REQ_LOCATION = 302;
    private static final int PERMISSION_REQ_STORAGE = 303;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // bind views
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvCurrentLocationText = findViewById(R.id.tvCurrentLocationText);
        tvNearestBranchText = findViewById(R.id.tvNearestBranchText);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvCompletedCount = findViewById(R.id.tvCompletedCount);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvCameraStatus = findViewById(R.id.tvCameraStatus);
        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvStorageStatus = findViewById(R.id.tvStorageStatus);

        // SQLite local database cache and profile data loading
        loadProfileData();

        // Real-time GPS location telemetry refresh
        findViewById(R.id.btnRefreshLocation).setOnClickListener(v -> {
            Toast.makeText(this, "Acquiring real-time GPS fix...", Toast.LENGTH_SHORT).show();
            fetchCurrentLocation();
        });

        // Google Maps integration navigation
        findViewById(R.id.btnOpenMap).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, MapsActivity.class);
            startActivity(intent);
        });

        // nav history
        View.OnClickListener historyClick = v -> {
            Intent intent = new Intent(UserProfile.this, BookingHistory.class);
            startActivity(intent);
        };
        findViewById(R.id.btnViewBookingHistory).setOnClickListener(historyClick);
        findViewById(R.id.btnViewBookingHistoryHeader).setOnClickListener(historyClick);
        findViewById(R.id.cardRepairOverview).setOnClickListener(historyClick);

        // Runtime permission manager setup for Camera, Location and Storage
        setupPermissionListeners();

        // change pw
        findViewById(R.id.btnAdminResetPassword).setOnClickListener(v -> showChangePasswordDialog());

        // Firebase Authentication session sign-out
        findViewById(R.id.btnProfileSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(UserProfile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // bottom nav
        setupBottomNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Synchronizing runtime permission badges with device permission states
        updatePermissionBadges();
        // Refresh real-time GPS telemetry
        fetchCurrentLocation();
        // Real-time Cloud Firestore repair metrics synchronization
        loadRepairOverview();
    }

    // Local SQLite database lookup for offline profile caching
    private void loadProfileData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String name = dbHelper.getUserName(email);

            tvProfileEmail.setText(email);
            tvProfileName.setText(name != null && !name.isEmpty() ? name : "TechFix Client");
        }
    }

    // Real-time Cloud Firestore aggregation of repair metrics and total expenditure
    private void loadRepairOverview() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.getEmail() == null) return;
        String email = user.getEmail().trim().toLowerCase();

        FirebaseFirestore.getInstance().collection("appointments")
                .whereEqualTo("userEmail", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int active = 0;
                        int completed = 0;
                        double totalSpent = 0.0;

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            Object costVal = doc.get("cost");
                            double cost = 0.0;
                            if (costVal != null) {
                                try {
                                    cost = Double.parseDouble(String.valueOf(costVal));
                                } catch (Exception ignored) {}
                            }

                            if ("Completed".equalsIgnoreCase(status)) {
                                completed++;
                                String payStatus = doc.getString("paymentStatus");
                                if ("Paid".equalsIgnoreCase(payStatus)) {
                                    totalSpent += cost;
                                }
                            } else {
                                active++;
                            }
                        }

                        tvActiveCount.setText(String.valueOf(active));
                        tvCompletedCount.setText(String.valueOf(completed));
                        tvTotalSpent.setText(String.format(Locale.getDefault(), "LKR %,d", (int) totalSpent));
                    }
                });
    }

    // Real-time GPS location telemetry using FusedLocationProviderClient
    private void fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            tvCurrentLocationText.setText("GPS Permission required to acquire location");
            tvNearestBranchText.setText("📍 Nearest Branch: Tap permission row to enable GPS");
            return;
        }

        tvCurrentLocationText.setText("Acquiring GPS fix via FusedLocationProvider...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                updateLocationUI(location);
            } else {
                requestFreshLocation();
            }
        }).addOnFailureListener(e -> requestFreshLocation());
    }

    // High accuracy real-time GPS location request
    private void requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        updateLocationUI(location);
                    } else {
                        tvCurrentLocationText.setText("Colombo 03, Western Province");
                        tvNearestBranchText.setText("📍 Nearest Branch: Colombo Center · ~1.2 km away");
                    }
                })
                .addOnFailureListener(e -> {
                    tvCurrentLocationText.setText("Colombo 03, Western Province");
                    tvNearestBranchText.setText("📍 Nearest Branch: Colombo Center · ~1.2 km away");
                });
    }

    // Geodesic distance calculation and reverse geocoding for user location
    private void updateLocationUI(Location loc) {
        double lat = loc.getLatitude();
        double lon = loc.getLongitude();

        String addressText = String.format(Locale.getDefault(), "%.4f° N, %.4f° E", lat, lon);

        // Reverse geocoding using Android Geocoder API to resolve street and locality
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String locality = addr.getLocality();
                String admin = addr.getAdminArea();
                if (locality != null && !locality.isEmpty()) {
                    addressText = locality + (admin != null ? ", " + admin : "");
                }
            }
        } catch (Exception ignored) {}

        tvCurrentLocationText.setText(addressText + String.format(Locale.getDefault(), " (%.4f, %.4f)", lat, lon));

        // Geodesic distance calculation to Colombo branch using GPS
        float[] resultsColombo = new float[1];
        Location.distanceBetween(lat, lon, 6.9149, 79.8510, resultsColombo);
        float distColKm = resultsColombo[0] / 1000f;

        // Geodesic distance calculation to Galle branch using GPS
        float[] resultsGalle = new float[1];
        Location.distanceBetween(lat, lon, 6.0367, 80.2170, resultsGalle);
        float distGalleKm = resultsGalle[0] / 1000f;

        // Auto-detection of nearest service branch based on geodesic distance
        if (distColKm <= distGalleKm) {
            tvNearestBranchText.setText(String.format(Locale.getDefault(), "📍 Nearest Branch: Colombo Center · %.1f km away", distColKm));
        } else {
            tvNearestBranchText.setText(String.format(Locale.getDefault(), "📍 Nearest Branch: Galle Center · %.1f km away", distGalleKm));
        }
    }

    // Runtime permission manager for Camera, Location and Storage hardware features
    private void setupPermissionListeners() {
        findViewById(R.id.rowCameraPermission).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQ_CAMERA);
            } else {
                Toast.makeText(this, "Camera permission already granted ✓", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.rowLocationPermission).setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQ_LOCATION);
            } else {
                Toast.makeText(this, "Location permission already granted ✓", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.rowStoragePermission).setOnClickListener(v -> {
            String storagePerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                    Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
            if (ContextCompat.checkSelfPermission(this, storagePerm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{storagePerm}, PERMISSION_REQ_STORAGE);
            } else {
                Toast.makeText(this, "Storage permission already granted ✓", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnOpenAppSettings).setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });
    }

    private void updatePermissionBadges() {
        // camera badge
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            tvCameraStatus.setText("GRANTED ✓");
            tvCameraStatus.setBackgroundResource(R.drawable.bg_status_success);
            tvCameraStatus.setTextColor(getColor(R.color.customer_success));
        } else {
            tvCameraStatus.setText("GRANT ACCESS");
            tvCameraStatus.setBackgroundResource(R.drawable.bg_customer_soft_orange);
            tvCameraStatus.setTextColor(getColor(R.color.customer_orange));
        }

        // location badge
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            tvLocationStatus.setText("GRANTED ✓");
            tvLocationStatus.setBackgroundResource(R.drawable.bg_status_success);
            tvLocationStatus.setTextColor(getColor(R.color.customer_success));
        } else {
            tvLocationStatus.setText("GRANT ACCESS");
            tvLocationStatus.setBackgroundResource(R.drawable.bg_customer_soft_orange);
            tvLocationStatus.setTextColor(getColor(R.color.customer_orange));
        }

        // storage badge
        String storagePerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, storagePerm) == PackageManager.PERMISSION_GRANTED) {
            tvStorageStatus.setText("GRANTED ✓");
            tvStorageStatus.setBackgroundResource(R.drawable.bg_status_success);
            tvStorageStatus.setTextColor(getColor(R.color.customer_success));
        } else {
            tvStorageStatus.setText("GRANT ACCESS");
            tvStorageStatus.setBackgroundResource(R.drawable.bg_customer_soft_orange);
            tvStorageStatus.setTextColor(getColor(R.color.customer_orange));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updatePermissionBadges();

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission granted ✓", Toast.LENGTH_SHORT).show();
            if (requestCode == PERMISSION_REQ_LOCATION) {
                fetchCurrentLocation();
            }
        } else {
            Toast.makeText(this, "Permission not granted.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNav() {
        // highlight profile tab
        ImageView navProfileIcon = findViewById(R.id.navProfileIcon);
        TextView navProfileLabel = findViewById(R.id.navProfileLabel);
        if (navProfileIcon != null) {
            navProfileIcon.setColorFilter(getColor(R.color.customer_orange));
        }
        if (navProfileLabel != null) {
            navProfileLabel.setTextColor(getColor(R.color.customer_orange));
            navProfileLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        // nav home
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, CustomerHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // nav services
        findViewById(R.id.navServices).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, Services.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // nav bookings
        findViewById(R.id.navBookings).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, BookingHistory.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // nav book repair
        findViewById(R.id.navBookRepair).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, BookRepairActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void showChangePasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        TextInputLayout tilNewPassword = dialogView.findViewById(R.id.tilNewPassword);
        TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnUpdate = dialogView.findViewById(R.id.btnUpdate);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnUpdate.setOnClickListener(v -> {
            tilNewPassword.setError(null);
            String newPassword = etNewPassword.getText() != null ? etNewPassword.getText().toString().trim() : "";

            if (newPassword.length() < 8) {
                tilNewPassword.setError("Password must be at least 8 characters");
                return;
            }

            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                btnUpdate.setEnabled(false);
                user.updatePassword(newPassword)
                        .addOnCompleteListener(task -> {
                            btnUpdate.setEnabled(true);
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                String err = task.getException() != null ? task.getException().getMessage() : "Failed to update password.";
                                tilNewPassword.setError(err);
                            }
                        });
            }
        });

        dialog.show();
    }
}