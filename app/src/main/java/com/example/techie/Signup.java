package com.example.techie;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Signup extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return insets;
        });

        // Back Button
        findViewById(R.id.btnBackSignUp).setOnClickListener(v -> finish());

        // Submit Sign Up
        findViewById(R.id.btnSignUpSubmit).setOnClickListener(v -> {
            Toast.makeText(Signup.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        });

        // Navigate to Sign In Activity
        findViewById(R.id.tvAlreadyHaveAccount).setOnClickListener(v -> {
            Intent intent = new Intent(Signup.this, Signin.class);
            startActivity(intent);
            finish();
        });
    }
}