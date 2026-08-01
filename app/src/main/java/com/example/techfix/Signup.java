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
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class Signup extends AppCompatActivity {

  private TextInputLayout fullNameLayout;
  private TextInputLayout emailLayout;
  private TextInputLayout passwordLayout;
  private TextInputLayout confirmPasswordLayout;
  private TextInputEditText fullNameInput;
  private TextInputEditText emailInput;
  private TextInputEditText passwordInput;
  private TextInputEditText confirmPasswordInput;
  private TechFixDatabaseHelper databaseHelper;
  private FirebaseAuth firebaseAuth;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    EdgeToEdge.enable(this);
    setContentView(R.layout.activity_signup);

    ViewCompat.setOnApplyWindowInsetsListener(
        findViewById(R.id.main), (v, insets) -> {
          Insets systemBars =
              insets.getInsets(WindowInsetsCompat.Type.systemBars());
          v.setPadding(0, systemBars.top, 0, systemBars.bottom);
          return insets;
        });

    fullNameLayout = findViewById(R.id.tilSignUpFullName);
    emailLayout = findViewById(R.id.tilSignUpEmail);
    passwordLayout = findViewById(R.id.tilSignUpPassword);
    confirmPasswordLayout = findViewById(R.id.tilSignUpConfirmPassword);
    fullNameInput = findViewById(R.id.etFullName);
    emailInput = findViewById(R.id.etSignUpEmail);
    passwordInput = findViewById(R.id.etSignUpPassword);
    confirmPasswordInput = findViewById(R.id.etSignUpConfirmPassword);
    databaseHelper = new TechFixDatabaseHelper(this);
    firebaseAuth = FirebaseAuth.getInstance();

    findViewById(R.id.btnSignUpSubmit).setOnClickListener(v -> createAccount());

    findViewById(R.id.tvAlreadyHaveAccount).setOnClickListener(v -> {
      Intent intent = new Intent(Signup.this, Signin.class);
      startActivity(intent);
      finish();
    });
  }

  private void createAccount() {
    String fullName = getValue(fullNameInput);
    String email = getValue(emailInput);
    String password = getValue(passwordInput);
    String confirmPassword = getValue(confirmPasswordInput);
    clearErrors();

    if (fullName.length() < 2) {
      fullNameLayout.setError(getString(R.string.invalid_name));
      fullNameInput.requestFocus();
      return;
    }

    if (TextUtils.isEmpty(email) ||
        !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
      emailLayout.setError(getString(R.string.invalid_email));
      emailInput.requestFocus();
      return;
    }

    if (password.length() < 8) {
      passwordLayout.setError(getString(R.string.invalid_password));
      passwordInput.requestFocus();
      return;
    }

    if (!password.equals(confirmPassword)) {
      confirmPasswordLayout.setError(
          getString(R.string.passwords_do_not_match));
      confirmPasswordInput.requestFocus();
      return;
    }

    findViewById(R.id.btnSignUpSubmit).setEnabled(false);
    firebaseAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, task -> {
          if (!task.isSuccessful() || firebaseAuth.getCurrentUser() == null) {
            findViewById(R.id.btnSignUpSubmit).setEnabled(true);
            emailLayout.setError(
                task.getException() instanceof
                        com.google.firebase.auth
                            .FirebaseAuthUserCollisionException
                    ? getString(R.string.email_already_registered)
                    : getString(R.string.registration_failed));
            return;
          }

          FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
          UserProfileChangeRequest profile =
              new UserProfileChangeRequest.Builder()
                  .setDisplayName(fullName)
                  .build();
          Map<String, Object> profileData = new HashMap<>();
          profileData.put("fullName", fullName);
          profileData.put("email", email.toLowerCase(java.util.Locale.ROOT));
          profileData.put("role", "customer");
          profileData.put(
              "updatedAt",
              com.google.firebase.firestore.FieldValue.serverTimestamp());

          firebaseUser.updateProfile(profile)
              .continueWithTask(ignored
                                -> FirebaseFirestore.getInstance()
                                       .collection("users")
                                       .document(firebaseUser.getUid())
                                       .set(profileData))
              .addOnCompleteListener(profileTask -> {
                if (profileTask.isSuccessful()) {
                  completeRegistration(firebaseUser, fullName, email);
                } else {
                  findViewById(R.id.btnSignUpSubmit).setEnabled(true);
                  firebaseUser.delete();
                  firebaseAuth.signOut();
                  Toast
                      .makeText(this, R.string.registration_failed,
                                Toast.LENGTH_SHORT)
                      .show();
                }
              });
        });
  }

  private void completeRegistration(FirebaseUser firebaseUser, String fullName,
                                    String email) {
    findViewById(R.id.btnSignUpSubmit).setEnabled(true);
    try {
      User user = databaseHelper.getOrCreateFirebaseUser(
          fullName, email, firebaseUser.getUid());
      new SessionManager(this).startSession(user, SessionManager.ROLE_CUSTOMER);
      FirebaseSyncScheduler.enqueueNow(this);
      FirebaseSyncScheduler.schedulePeriodic(this);
      FirebaseRealtimeSync.start(this);
      Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
      Intent intent = new Intent(this, CustomerHomeActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                      Intent.FLAG_ACTIVITY_CLEAR_TASK);
      startActivity(intent);
      finish();
    } catch (RuntimeException exception) {
      firebaseUser.delete();
      firebaseAuth.signOut();
      Toast.makeText(this, R.string.registration_failed, Toast.LENGTH_SHORT)
          .show();
    }
  }

  private void clearErrors() {
    fullNameLayout.setError(null);
    emailLayout.setError(null);
    passwordLayout.setError(null);
    confirmPasswordLayout.setError(null);
  }

  private String getValue(TextInputEditText input) {
    return input.getText() == null ? "" : input.getText().toString().trim();
  }

  @Override
  protected void onDestroy() {
    if (databaseHelper != null) {
      databaseHelper.close();
    }
    super.onDestroy();
  }
}
