package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        // auto sign in
        handleAutoSignIn();
    }

    private void handleAutoSignIn() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            setupLandingPageUI();
            return;
        }

        String email = currentUser.getEmail();
        if (email == null || email.trim().isEmpty()) {
            mAuth.signOut();
            setupLandingPageUI();
            return;
        }

        // query users firestore
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("email", email.trim().toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                        String finalRole = "customer";
                        String finalName = "User";

                        // check admin
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String role = doc.getString("role");
                            String name = doc.getString("fullName");
                            if ("admin".equalsIgnoreCase(role)) {
                                finalRole = "admin";
                                if (name != null) finalName = name;
                                break; // admin match
                            } else if (role != null) {
                                finalRole = role;
                                if (name != null) finalName = name;
                            }
                        }

                        // sqlite
                        dbHelper.insertUser(finalName, email, finalRole);
                        navigateByRole(finalRole);
                    } else {
                        // fallback check uid
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.getUid())
                                .get()
                                .addOnCompleteListener(uidTask -> {
                                    if (uidTask.isSuccessful() && uidTask.getResult() != null && uidTask.getResult().exists()) {
                                        DocumentSnapshot doc = uidTask.getResult();
                                        String role = doc.getString("role");
                                        String name = doc.getString("fullName");
                                        if (role != null) {
                                            dbHelper.insertUser(name != null ? name : "User", email, role);
                                            navigateByRole(role);
                                            return;
                                        }
                                    }

                                    // fallback sign out
                                    String errorMsg = "Could not verify your role on the server.";
                                    if (task.getException() != null) {
                                        errorMsg = task.getException().getMessage();
                                    } else if (uidTask.getException() != null) {
                                        errorMsg = uidTask.getException().getMessage();
                                    }
                                    Toast.makeText(MainActivity.this, "Auto Sign-in Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                    setupLandingPageUI();
                                });
                    }
                });
    }

    private void navigateByRole(String role) {
        if ("admin".equalsIgnoreCase(role)) {
            Intent intent = new Intent(MainActivity.this, ManagerDashboard.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Intent intent = new Intent(MainActivity.this, CustomerHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void setupLandingPageUI() {
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // sign in btn
        findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Signin.class);
            startActivity(intent);
        });

        // sign up btn
        findViewById(R.id.btnSignUp).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Signup.class);
            startActivity(intent);
        });
    }
}