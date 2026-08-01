package com.example.techfix;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Signin extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Submit Sign In
        findViewById(R.id.btnSignIn).setOnClickListener(v -> {
            Toast.makeText(Signin.this, "Signing in...", Toast.LENGTH_SHORT).show();
        });

        // Navigate to Sign Up Activity
        findViewById(R.id.tvSignUp).setOnClickListener(v -> {
            Intent intent = new Intent(Signin.this, Signup.class);
            startActivity(intent);
            finish();
        });

        // Forgot password
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> {
            Toast.makeText(Signin.this, "Forgot password clicked", Toast.LENGTH_SHORT).show();
        });
    }
}