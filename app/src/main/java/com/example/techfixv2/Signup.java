package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Signup extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUpSubmit;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        tilFullName = findViewById(R.id.tilSignUpFullName);
        tilEmail = findViewById(R.id.tilSignUpEmail);
        tilPassword = findViewById(R.id.tilSignUpPassword);
        tilConfirmPassword = findViewById(R.id.tilSignUpConfirmPassword);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etSignUpEmail);
        etPassword = findViewById(R.id.etSignUpPassword);
        etConfirmPassword = findViewById(R.id.etSignUpConfirmPassword);

        btnSignUpSubmit = findViewById(R.id.btnSignUpSubmit);
        btnSignUpSubmit.setOnClickListener(v -> handleSignUp());

        // Navigate to Sign In Activity
        findViewById(R.id.txtLogin).setOnClickListener(v -> {
            Intent intent = new Intent(Signup.this, Signin.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleSignUp() {
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String fullName = etFullName.getText() != null ? etFullName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (fullName.isEmpty()) {
            tilFullName.setError(getString(R.string.invalid_name));
            return;
        }

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        }

        if (password.length() < 8) {
            tilPassword.setError(getString(R.string.invalid_password));
            return;
        }

        if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError(getString(R.string.passwords_do_not_match));
            return;
        }

        btnSignUpSubmit.setEnabled(false);

        // 1. Create User in Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();

                        // 2. Save to Firestore
                        Map<String, Object> user = new HashMap<>();
                        user.put("fullName", fullName);
                        user.put("email", email);
                        user.put("role", "customer");

                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    // 3. Save to Local Database
                                    dbHelper.insertUser(fullName, email, "customer");

                                    Toast.makeText(Signup.this, "Account created successfully! Please sign in.", Toast.LENGTH_SHORT).show();

                                    // 4. Redirect to Sign In Activity
                                    Intent intent = new Intent(Signup.this, Signin.class);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnSignUpSubmit.setEnabled(true);
                                    Toast.makeText(Signup.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        btnSignUpSubmit.setEnabled(true);
                        String err = task.getException() != null ? task.getException().getMessage() : "Sign up failed.";
                        Toast.makeText(Signup.this, err, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
