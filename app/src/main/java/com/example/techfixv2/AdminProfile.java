package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminProfile extends AppCompatActivity {

    private TextView tvAdminName, tvAdminEmail;
    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminEmail = findViewById(R.id.tvAdminEmail);

        // Load Admin Info
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String name = dbHelper.getUserName(email);

            tvAdminEmail.setText(email);
            tvAdminName.setText(name);
        }

        // Back to Dashboard Button
        findViewById(R.id.btnBackToDashboard).setOnClickListener(v -> {
            finish();
        });

        // Sign Out Button
        findViewById(R.id.btnAdminSignOut).setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(AdminProfile.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Change Password Button
        findViewById(R.id.btnAdminResetPassword).setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showChangePasswordDialog() {
        android.view.View dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        com.google.android.material.textfield.TextInputLayout tilNewPassword = dialogView.findViewById(R.id.tilNewPassword);
        com.google.android.material.textfield.TextInputEditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        android.widget.Button btnUpdate = dialogView.findViewById(R.id.btnUpdate);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
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
                                android.widget.Toast.makeText(this, "Password updated successfully!", android.widget.Toast.LENGTH_SHORT).show();
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