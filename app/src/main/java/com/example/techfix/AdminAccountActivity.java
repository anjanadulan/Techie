package com.example.techfix;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminAccountActivity extends ManagementScreen {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!showManagementLayout(R.layout.activity_admin_account))
      return;

    SessionManager sessionManager = new SessionManager(this);
    ((TextView)findViewById(R.id.tvAdminName))
        .setText(sessionManager.getFullName());
    ((TextView)findViewById(R.id.tvAdminEmail))
        .setText(sessionManager.getEmail());

    findViewById(R.id.btnAdminBack).setOnClickListener(view -> finish());
    findViewById(R.id.btnAdminDashboard).setOnClickListener(view -> {
      Intent intent = new Intent(this, ManagementDashboardActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    });
    findViewById(R.id.btnAdminSync).setOnClickListener(view -> {
      FirebaseSyncScheduler.enqueueNow(this);
      Toast.makeText(this, "Cloud sync started", Toast.LENGTH_SHORT).show();
    });
    findViewById(R.id.btnAdminResetPassword)
        .setOnClickListener(view -> showPasswordChangeDialog(sessionManager));
    findViewById(R.id.btnAdminLogout)
        .setOnClickListener(view -> logoutManager());
  }

  private void showPasswordChangeDialog(SessionManager sessionManager) {
    int padding = Math.round(20 * getResources().getDisplayMetrics().density);
    LinearLayout form = new LinearLayout(this);
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(padding, padding / 2, padding, 0);

    EditText currentPassword = new EditText(this);
    currentPassword.setHint("Current password");
    currentPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                                 InputType.TYPE_TEXT_VARIATION_PASSWORD);
    form.addView(currentPassword);

    EditText newPassword = new EditText(this);
    newPassword.setHint("New password (minimum 8 characters)");
    newPassword.setInputType(InputType.TYPE_CLASS_TEXT |
                             InputType.TYPE_TEXT_VARIATION_PASSWORD);
    form.addView(newPassword);

    AlertDialog dialog = new AlertDialog.Builder(this)
                             .setTitle("Change administrator password")
                             .setView(form)
                             .setNegativeButton("Cancel", null)
                             .setPositiveButton("Change password", null)
                             .create();
    dialog.setOnShowListener(ignored
                             -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                    .setOnClickListener(button -> {
                                      String current = currentPassword.getText()
                                                           .toString();
                                      String replacement = newPassword.getText()
                                                               .toString();
                                      if (current.isEmpty()) {
                                        currentPassword.setError(
                                            "Enter the current password.");
                                        return;
                                      }
                                      if (replacement.length() < 8) {
                                        newPassword.setError(
                                            "Use at least 8 characters.");
                                        return;
                                      }
                                      changePassword(
                                          sessionManager.getEmail(), current,
                                          replacement, dialog);
                                    }));
    dialog.show();
  }

  private void changePassword(String email, String currentPassword,
                              String newPassword, AlertDialog dialog) {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
      Toast.makeText(this, "Sign in again before changing the password.",
                     Toast.LENGTH_LONG)
          .show();
      return;
    }
    user.reauthenticate(
            EmailAuthProvider.getCredential(email, currentPassword))
        .continueWithTask(ignored -> user.updatePassword(newPassword))
        .addOnSuccessListener(ignored -> {
          dialog.dismiss();
          Toast.makeText(this, "Administrator password updated.",
                         Toast.LENGTH_LONG)
              .show();
        })
        .addOnFailureListener(error
                              -> Toast.makeText(
                                          this,
                                          "Current password is incorrect or the update failed.",
                                          Toast.LENGTH_LONG)
                                     .show());
  }
}
