package com.example.techfixv2;

import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize back button
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvTitle = findViewById(R.id.tvMapTitle);
        if (tvTitle != null) {
            tvTitle.setText("Service Center Branches");
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Initialize Coordinates for Colombo and Galle Centers
        LatLng colombo = new LatLng(6.9149, 79.8510);
        LatLng galle = new LatLng(6.0367, 80.2170);

        // Add markers to the map
        mMap.addMarker(new MarkerOptions().position(colombo).title("TechFix Colombo Center"));
        mMap.addMarker(new MarkerOptions().position(galle).title("TechFix Galle Center"));

        // Build a boundary box enclosing both points
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(colombo);
        builder.include(galle);
        final LatLngBounds bounds = builder.build();

        // Auto-center and zoom camera to show all markers when layout completes loading
        mMap.setOnMapLoadedCallback(() -> {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
        });
    }
}
