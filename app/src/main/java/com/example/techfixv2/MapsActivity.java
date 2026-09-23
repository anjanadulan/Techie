package com.example.techfixv2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;

// Google Maps API implementation and SupportMapFragment integration
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private GoogleMap mMap;
    // FusedLocationProviderClient initialization for GPS hardware access
    private FusedLocationProviderClient fusedLocationClient;

    // Coordinates definition for TechFix service branches (Colombo and Galle)
    private final LatLng colomboBranchCoords = new LatLng(6.9149, 79.8510);
    private final LatLng galleBranchCoords = new LatLng(6.0367, 80.2170);

    // Google Maps markers for branch locations and user position
    private Marker markerColombo;
    private Marker markerGalle;
    private Marker markerUser;

    // ui
    private TextView tvGpsStatus;
    private TextView tvNearestBadge;
    private TextView tvNearestDistance;
    private TextView tvNearestBranchName;
    private TextView tvNearestBranchAddress;
    private TextView tvNearestBranchPhone;
    private TextView tvDistancesOverview;
    private TextView btnBookAtBranch;

    // Nearest branch auto-selection state
    private String currentNearestBranch = "Colombo";
    private Location lastKnownUserLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // FusedLocationProviderClient for real-time GPS telemetry
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // views
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvGpsStatus = findViewById(R.id.tvGpsStatus);
        tvNearestBadge = findViewById(R.id.tvNearestBadge);
        tvNearestDistance = findViewById(R.id.tvNearestDistance);
        tvNearestBranchName = findViewById(R.id.tvNearestBranchName);
        tvNearestBranchAddress = findViewById(R.id.tvNearestBranchAddress);
        tvNearestBranchPhone = findViewById(R.id.tvNearestBranchPhone);
        tvDistancesOverview = findViewById(R.id.tvDistancesOverview);
        btnBookAtBranch = findViewById(R.id.btnBookAtBranch);

        View btnGpsLocate = findViewById(R.id.btnGpsLocate);
        if (btnGpsLocate != null) {
            btnGpsLocate.setOnClickListener(v -> {
                Toast.makeText(this, "Refreshing GPS location...", Toast.LENGTH_SHORT).show();
                fetchUserLocationAndCalculateNearest();
            });
        }

        // map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // book branch
        btnBookAtBranch.setOnClickListener(v -> {
            Intent intent = new Intent(MapsActivity.this, BookRepairActivity.class);
            intent.putExtra("preselected_branch", currentNearestBranch);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Google Maps UI settings and zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);

        // Google Maps marker configuration for service branch locations
        markerColombo = mMap.addMarker(new MarkerOptions()
                .position(colomboBranchCoords)
                .title("TechFix Colombo Center")
                .snippet("Galle Road, Colombo 03 • Tel: 0112345678")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        markerGalle = mMap.addMarker(new MarkerOptions()
                .position(galleBranchCoords)
                .title("TechFix Galle Center")
                .snippet("Wakwella Road, Galle • Tel: 0912345678")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        // Interactive marker click listener for branch information display
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.equals(markerColombo)) {
                displayBranchDetails("Colombo");
            } else if (marker.equals(markerGalle)) {
                displayBranchDetails("Galle");
            }
            marker.showInfoWindow();
            return false;
        });

        // Google Maps camera viewport animation to encompass both branches
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(colomboBranchCoords);
        builder.include(galleBranchCoords);
        final LatLngBounds initialBounds = builder.build();

        // Center camera immediately over Sri Lanka service centers
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colomboBranchCoords, 9f));
        mMap.setOnMapLoadedCallback(() -> {
            try {
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(initialBounds, 160));
            } catch (Exception ignored) {}
        });

        // Runtime location permission verification and GPS calculation immediately on load
        checkLocationPermissionAndFetch();
    }

    private void checkLocationPermissionAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMapLocationAndFetch();
        } else {
            // req loc permissions
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMapLocationAndFetch() {
        if (mMap != null) {
            try {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(false); // custom fab
            } catch (SecurityException ignored) {}
        }
        fetchUserLocationAndCalculateNearest();
    }

    private void fetchUserLocationAndCalculateNearest() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            tvGpsStatus.setText("Location permission required for GPS");
            return;
        }

        tvGpsStatus.setText("Acquiring GPS fix via FusedLocationProvider...");

        // GPS location tracking using FusedLocationProviderClient last known location
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                processLocation(location);
            } else {
                // Request fresh GPS location update
                requestFreshLocation();
            }
        }).addOnFailureListener(e -> requestFreshLocation());
    }

    // Real-time GPS location acquisition with high accuracy
    private void requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        processLocation(location);
                    } else {
                        tvGpsStatus.setText("GPS unavailable (Using default Colombo)");
                        displayBranchDetails("Colombo");
                    }
                })
                .addOnFailureListener(e -> {
                    tvGpsStatus.setText("GPS error: " + e.getMessage());
                    displayBranchDetails("Colombo");
                });
    }

    private void processLocation(@NonNull Location location) {
        lastKnownUserLocation = location;
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // Geodesic distance calculation to Colombo branch using GPS
        float[] distResultsColombo = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                colomboBranchCoords.latitude, colomboBranchCoords.longitude, distResultsColombo);
        float distanceToColomboKm = distResultsColombo[0] / 1000f;

        // Geodesic distance calculation to Galle branch using GPS
        float[] distResultsGalle = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                galleBranchCoords.latitude, galleBranchCoords.longitude, distResultsGalle);
        float distanceToGalleKm = distResultsGalle[0] / 1000f;

        // Auto-selection of nearest branch based on GPS telemetry
        boolean isColomboNearest = distanceToColomboKm <= distanceToGalleKm;
        currentNearestBranch = isColomboNearest ? "Colombo" : "Galle";
        float nearestDistKm = isColomboNearest ? distanceToColomboKm : distanceToGalleKm;

        // Display nearest branch status and GPS telemetry
        tvGpsStatus.setText(String.format("GPS Active • Nearest: %s (%.1f km)", currentNearestBranch, nearestDistKm));

        // user marker
        if (markerUser != null) {
            markerUser.remove();
        }
        markerUser = mMap.addMarker(new MarkerOptions()
                .position(userLatLng)
                .title("Your Current Location")
                .snippet(String.format("Nearest: %s Branch (%.1f km)", currentNearestBranch, nearestDistKm))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

        // update card
        updateNearestCardUI(currentNearestBranch, nearestDistKm, distanceToColomboKm, distanceToGalleKm);

        // camera bounds
        try {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(colomboBranchCoords);
            builder.include(galleBranchCoords);
            builder.include(userLatLng);
            LatLngBounds bounds = builder.build();
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 180));
        } catch (Exception ignored) {}
    }

    private void updateNearestCardUI(String branchName, float nearestDistKm, float colomboDistKm, float galleDistKm) {
        tvNearestBadge.setVisibility(View.VISIBLE);
        tvNearestBadge.setText("RECOMMENDED NEAREST");
        tvNearestDistance.setText(String.format("%.1f km away", nearestDistKm));
        tvNearestBranchName.setText("TechFix " + branchName + " Branch");

        if ("Colombo".equalsIgnoreCase(branchName)) {
            tvNearestBranchAddress.setText("📍 Galle Road, Colombo 03");
            tvNearestBranchPhone.setText("📞 Contact: 0112345678 (Open: 8:30 AM - 6:00 PM)");
        } else {
            tvNearestBranchAddress.setText("📍 Wakwella Road, Galle");
            tvNearestBranchPhone.setText("📞 Contact: 0912345678 (Open: 8:30 AM - 6:00 PM)");
        }

        tvDistancesOverview.setText(String.format("Branch Distances — Colombo: %.1f km  |  Galle: %.1f km", colomboDistKm, galleDistKm));
        btnBookAtBranch.setText("Book Appointment at " + branchName + " Branch");
    }

    private void displayBranchDetails(String branchName) {
        currentNearestBranch = branchName;
        tvNearestBranchName.setText("TechFix " + branchName + " Branch");

        if (lastKnownUserLocation != null) {
            float[] results = new float[1];
            LatLng targetCoords = "Colombo".equalsIgnoreCase(branchName) ? colomboBranchCoords : galleBranchCoords;
            Location.distanceBetween(lastKnownUserLocation.getLatitude(), lastKnownUserLocation.getLongitude(),
                    targetCoords.latitude, targetCoords.longitude, results);
            float distKm = results[0] / 1000f;
            tvNearestDistance.setText(String.format("%.1f km away", distKm));
        } else {
            tvNearestDistance.setText("GPS not fixed");
        }

        if ("Colombo".equalsIgnoreCase(branchName)) {
            tvNearestBranchAddress.setText("📍 Galle Road, Colombo 03");
            tvNearestBranchPhone.setText("📞 Contact: 0112345678 (Open: 8:30 AM - 6:00 PM)");
        } else {
            tvNearestBranchAddress.setText("📍 Wakwella Road, Galle");
            tvNearestBranchPhone.setText("📞 Contact: 0912345678 (Open: 8:30 AM - 6:00 PM)");
        }

        btnBookAtBranch.setText("Book Appointment at " + branchName + " Branch");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted. Locating nearest branch...", Toast.LENGTH_SHORT).show();
                enableMapLocationAndFetch();
            } else {
                Toast.makeText(this, "Location permission denied. Distance calculation disabled.", Toast.LENGTH_LONG).show();
                tvGpsStatus.setText("Location permission denied (Tap 📍 to enable)");
                displayBranchDetails("Colombo");
            }
        }
    }
}
