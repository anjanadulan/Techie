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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Signin extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnSignIn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signin);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);

        btnSignIn.setOnClickListener(v -> handleSignIn());

        // Navigate to Sign Up Activity
        findViewById(R.id.txtSignup).setOnClickListener(v -> {
            Intent intent = new Intent(Signin.this, Signup.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleSignIn() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.invalid_password));
            return;
        }

        btnSignIn.setEnabled(false);

        // 1. Firebase Auth Sign In
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String uid = mAuth.getCurrentUser().getUid();

                        // 2. Fetch User Role from Firestore
                        db.collection("users").document(uid).get()
                                .addOnCompleteListener(docTask -> {
                                    btnSignIn.setEnabled(true);

                                    String role = "customer";
                                    String name = "";

                                    if (docTask.isSuccessful() && docTask.getResult() != null) {
                                        DocumentSnapshot doc = docTask.getResult();
                                        if (doc.contains("role") && doc.getString("role") != null) {
                                            role = doc.getString("role");
                                        }
                                        if (doc.contains("fullName") && doc.getString("fullName") != null) {
                                            name = doc.getString("fullName");
                                        }
                                    }

                                    // Save/Sync to Local Database
                                    dbHelper.insertUser(name, email, role);

                                    // 3. Role-based Navigation
                                    if ("admin".equalsIgnoreCase(role)) {
                                        Toast.makeText(Signin.this, "Welcome Admin!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Signin.this, ManagerDashboard.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(Signin.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Signin.this, CustomerHome.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                    } else {
                        btnSignIn.setEnabled(true);
                        String err = task.getException() != null ? task.getException().getMessage() : "Sign in failed.";
                        Toast.makeText(Signin.this, err, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}