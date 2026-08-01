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

public class Signin extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "email";

    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TechFixDatabaseHelper databaseHelper;

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

        emailLayout = findViewById(R.id.tilEmail);
        passwordLayout = findViewById(R.id.tilPassword);
        emailInput = findViewById(R.id.etEmail);
        passwordInput = findViewById(R.id.etPassword);
        databaseHelper = new TechFixDatabaseHelper(this);

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

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.invalid_email));
            emailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordLayout.setError(getString(R.string.invalid_password));
            passwordInput.requestFocus();
            return;
        }

        TechFixDatabaseHelper.AuthenticationResult result =
                databaseHelper.authenticate(email, password);

        if (result.getStatus() == TechFixDatabaseHelper.AuthenticationResult.Status.SUCCESS) {
            User user = result.getUser();
            new SessionManager(this).startSession(user);
            Toast.makeText(
                    this,
                    getString(R.string.welcome_name, user.getFullName()),
                    Toast.LENGTH_SHORT
            ).show();
            openAccount();
            return;
        }

        if (result.getStatus()
                == TechFixDatabaseHelper.AuthenticationResult.Status.INVALID_CREDENTIALS) {
            passwordLayout.setError(getString(R.string.invalid_credentials));
            passwordInput.requestFocus();
            return;
        }

        Toast.makeText(this, R.string.login_failed, Toast.LENGTH_SHORT).show();
    }

    private String getValue(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private void openAccount() {
        Intent intent = new Intent(this, AccountActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
