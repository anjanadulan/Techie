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

        // Change pw
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