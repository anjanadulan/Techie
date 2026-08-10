package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import androidx.annotation.NonNull;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper(this);

        //Check if user is already logged in (works offline)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String email = currentUser.getEmail();
            String role = dbHelper.getUserRole(email);

            if ("admin".equalsIgnoreCase(role)) {
                Intent intent = new Intent(MainActivity.this, ManagerDashboard.class);
                startActivity(intent);
                finish();
                return;
            } else {
                Intent intent = new Intent(MainActivity.this, CustomerHome.class);
                startActivity(intent);
                finish();
                return;
            }
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // sign in
        findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Signin.class);
            startActivity(intent);
        });

        // sign up
        findViewById(R.id.btnSignUp).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Signup.class);
            startActivity(intent);
        });
    }
}