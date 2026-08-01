package com.example.techfix;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Signin extends AppCompatActivity {
  public static final String EXTRA_EMAIL = "email";

  private TextInputLayout emailLayout;
  private TextInputLayout passwordLayout;
  private TextInputEditText emailInput;
  private TextInputEditText passwordInput;
  private TechFixDatabaseHelper databaseHelper;
  private FirebaseAuth firebaseAuth;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_signin);

    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.main), (v, insets) -> {
          Insets systemBars =
              insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(0, systemBars.top, 0, systemBars.bottom);
          return insets;
        });

    emailLayout = findViewById(R.id.tilEmail);
    passwordLayout = findViewById(R.id.tilPassword);
    emailInput = findViewById(R.id.etEmail);
    passwordInput = findViewById(R.id.etPassword);
    databaseHelper = new TechFixDatabaseHelper(this);
    firebaseAuth = FirebaseAuth.getInstance();

    String registeredEmail = getIntent().getStringExtra(EXTRA_EMAIL);
    if (registeredEmail != null) {
      emailInput.setText(registeredEmail);
      passwordInput.requestFocus();
    }

    findViewById(R.id.btnSignIn).setOnClickListener(v -> logIn());

    findViewById(R.id.tvSignUp).setOnClickListener(v -> {
      Intent intent = new Intent(Signin.this, Signup.class);
      startActivity(intent);
      finish();
    });
  }

  private void logIn() {
    String email = getValue(emailInput);
    String password = getValue(passwordInput);
    emailLayout.setError(null);
    passwordLayout.setError(null);

    if (TextUtils.isEmpty(email) ||
        !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      emailLayout.setError(getString(R.string.invalid_email));
      emailInput.requestFocus();
      return;
    }

    if (TextUtils.isEmpty(password)) {
      passwordLayout.setError(getString(R.string.invalid_password));
      passwordInput.requestFocus();
      return;
    }

    findViewById(R.id.btnSignIn).setEnabled(false);
    firebaseAuth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, task -> {
          findViewById(R.id.btnSignIn).setEnabled(true);
          if (!task.isSuccessful() || firebaseAuth.getCurrentUser() == null) {
            passwordLayout.setError(getString(R.string.invalid_credentials));
            passwordInput.requestFocus();
            return;
          }
          FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
          String fullName = firebaseUser.getDisplayName();
          if (fullName == null || fullName.trim().isEmpty())
            fullName = email.substring(0, email.indexOf('@'));
          String resolvedName = fullName;
          Map<String, Object> profile = new HashMap<>();
          profile.put("fullName", resolvedName);
          profile.put("email", email.toLowerCase(Locale.ROOT));
          profile.put("updatedAt", FieldValue.serverTimestamp());
          FirebaseFirestore.getInstance()
              .collection("users")
              .document(firebaseUser.getUid())
              .get()
              .continueWithTask(profileTask -> {
                String role = profileTask.getResult().exists()
                                  ? profileTask.getResult().getString("role")
                                  : SessionManager.ROLE_CUSTOMER;
                profile.put("role", SessionManager.ROLE_MANAGER.equals(role)
                                        ? SessionManager.ROLE_MANAGER
                                        : SessionManager.ROLE_CUSTOMER);
                return FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(firebaseUser.getUid())
                    .set(profile, SetOptions.merge());
              })
              .addOnCompleteListener(profileTask -> {
                if (!profileTask.isSuccessful()) {
                  firebaseAuth.signOut();
                  Toast
                      .makeText(this, R.string.login_failed, Toast.LENGTH_SHORT)
                      .show();
                  return;
                }
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(firebaseUser.getUid())
                    .get()
                    .addOnSuccessListener(savedProfile
                                          -> completeLogin(
                                              firebaseUser, resolvedName, email,
                                              savedProfile.getString("role")))
                    .addOnFailureListener(error -> {
                      firebaseAuth.signOut();
                      Toast.makeText(this, R.string.login_failed,
                                     Toast.LENGTH_SHORT)
                          .show();
                    });
              });
        });
  }

  private void completeLogin(FirebaseUser firebaseUser, String fullName,
                             String email, String role) {
    try {
      User user = databaseHelper.getOrCreateFirebaseUser(
          fullName, email, firebaseUser.getUid());
      String resolvedRole = SessionManager.ROLE_MANAGER.equals(role)
                                ? SessionManager.ROLE_MANAGER
                                : SessionManager.ROLE_CUSTOMER;
      new SessionManager(this).startSession(user, resolvedRole);
      FirebaseSyncScheduler.enqueueNow(this);
      FirebaseSyncScheduler.schedulePeriodic(this);
      FirebaseRealtimeSync.start(this);
      Toast
          .makeText(this, getString(R.string.welcome_name, user.getFullName()),
                    Toast.LENGTH_SHORT)
          .show();
      openAccount(resolvedRole);
    } catch (RuntimeException exception) {
      firebaseAuth.signOut();
      Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
    }
  }

  private String getValue(TextInputEditText input) {
    return input.getText() == null ? "" : input.getText().toString().trim();
  }

  private void openAccount(String role) {
    Class<?> destination = SessionManager.ROLE_MANAGER.equals(role)
                               ? ManagementDashboardActivity.class
                               : CustomerHomeActivity.class;
    Intent intent = new Intent(this, destination);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                    Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  @Override
  protected void onDestroy() {
    if (databaseHelper != null) {
      databaseHelper.close();
    }
    super.onDestroy();
  }
}
