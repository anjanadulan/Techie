package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CustomerHome extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_customer_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bottom Navigation click listener to go to BookingHistory
        findViewById(R.id.navBookings).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHome.this, BookingHistory.class);
            startActivity(intent);
        });

        // Bottom Navigation click listener to go to Services
        findViewById(R.id.navServices).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHome.this, Services.class);
            startActivity(intent);
        });

        // Bottom Navigation click listener to go to Profile
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHome.this, UserProfile.class);
            startActivity(intent);
        });
    }
}