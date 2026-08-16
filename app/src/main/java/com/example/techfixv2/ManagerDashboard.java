package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class ManagerDashboard extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();

        // Navigate to Admin Profile
        findViewById(R.id.btnAdminProfile).setOnClickListener(v -> {
            Intent intent = new Intent(ManagerDashboard.this, AdminProfile.class);
            startActivity(intent);
        });

        // Setup click listeners for the 9 operational modules
        setupModuleNavigation(R.id.manageAppointments, "appointments");
        setupModuleNavigation(R.id.manageTechnicians, "technicians");
        setupModuleNavigation(R.id.manageBranches, "branches");
        setupModuleNavigation(R.id.manageCategories, "categories");
        setupModuleNavigation(R.id.managePrices, "prices");
        setupModuleNavigation(R.id.manageParts, "parts");
        setupModuleNavigation(R.id.manageImages, "images");
        setupModuleNavigation(R.id.managePayments, "payments");
        setupModuleNavigation(R.id.manageStatuses, "statuses");
    }

    private void setupModuleNavigation(int viewId, String moduleKey) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(v -> {
                Intent intent = new Intent(ManagerDashboard.this, ManagementModuleActivity.class);
                intent.putExtra("module_key", moduleKey);
                startActivity(intent);
            });
        }
    }
}