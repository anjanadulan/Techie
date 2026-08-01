package com.example.techfix;

import android.os.Bundle;
import android.widget.Toast;

public class BookRepairActivity extends CustomerScreen {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showCustomerLayout(R.layout.activity_book_repair);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnContinue).setOnClickListener(v -> Toast.makeText(this, R.string.booking_frontend_message, Toast.LENGTH_SHORT).show());
    }
}
