package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class UserProfile extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmail;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);

        // Load profile data
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String name = dbHelper.getUserName(email);

            tvProfileEmail.setText(email);
            tvProfileName.setText(name);
        }

        // Bottom Navigation: Go to Home
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, CustomerHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation: Go to Services
        findViewById(R.id.navServices).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, Services.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation: Go to Bookings
        findViewById(R.id.navBookings).setOnClickListener(v -> {
            Intent intent = new Intent(UserProfile.this, BookingHistory.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Sign Out Button
        findViewById(R.id.btnProfileSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(UserProfile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Change Password Button
        findViewById(R.id.btnAdminResetPassword).setOnClickListener(v -> showChangePasswordDialog());
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