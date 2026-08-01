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

public class Signup extends AppCompatActivity {

    private TextInputLayout fullNameLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText fullNameInput;
    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private UserDatabaseHelper databaseHelper;

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

        fullNameLayout = findViewById(R.id.tilSignUpFullName);
        emailLayout = findViewById(R.id.tilSignUpEmail);
        passwordLayout = findViewById(R.id.tilSignUpPassword);
        confirmPasswordLayout = findViewById(R.id.tilSignUpConfirmPassword);
        fullNameInput = findViewById(R.id.etFullName);
        emailInput = findViewById(R.id.etSignUpEmail);
        passwordInput = findViewById(R.id.etSignUpPassword);
        confirmPasswordInput = findViewById(R.id.etSignUpConfirmPassword);
        databaseHelper = new UserDatabaseHelper(this);

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

        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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
            confirmPasswordLayout.setError(getString(R.string.passwords_do_not_match));
            confirmPasswordInput.requestFocus();
            return;
        }

        UserDatabaseHelper.RegistrationResult result =
                databaseHelper.registerUser(fullName, email, password);

        if (result == UserDatabaseHelper.RegistrationResult.EMAIL_ALREADY_EXISTS) {
            emailLayout.setError(getString(R.string.email_already_registered));
            emailInput.requestFocus();
            return;
        }

        if (result == UserDatabaseHelper.RegistrationResult.ERROR) {
            Toast.makeText(this, R.string.registration_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, Signin.class);
        intent.putExtra(Signin.EXTRA_EMAIL, email);
        startActivity(intent);
        finish();
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
