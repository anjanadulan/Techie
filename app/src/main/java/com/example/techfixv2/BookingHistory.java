package com.example.techfixv2;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class BookingHistory extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private LinearLayout repairHistoryList;
    private TextView tvHistoryEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        repairHistoryList = findViewById(R.id.repairHistoryList);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);

        // Load and populate bookings from local SQLite Database
        loadLocalBookingHistory();

        // Bottom Navigation click listener to go back to CustomerHome
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(BookingHistory.this, CustomerHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation click listener to go to Services
        findViewById(R.id.navServices).setOnClickListener(v -> {
            Intent intent = new Intent(BookingHistory.this, Services.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation click listener to go to Profile
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(BookingHistory.this, UserProfile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void loadLocalBookingHistory() {
        repairHistoryList.removeAllViews();
        Cursor cursor = dbHelper.getAllRepairs();

        if (cursor == null || cursor.getCount() == 0) {
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            if (cursor != null) cursor.close();
            return;
        }

        tvHistoryEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);

        while (cursor.moveToNext()) {
            // Read fields: repair_id TEXT, device TEXT, status TEXT, cost TEXT, date TEXT
            String repairId = cursor.getString(cursor.getColumnIndexOrThrow("repair_id"));
            String device = cursor.getString(cursor.getColumnIndexOrThrow("device"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status"));
            String cost = cursor.getString(cursor.getColumnIndexOrThrow("cost"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));

            View itemView = inflater.inflate(R.layout.item_repair_history, repairHistoryList, false);

            TextView tvItemRepairId = itemView.findViewById(R.id.tvItemRepairId);
            TextView tvItemDevice = itemView.findViewById(R.id.tvItemDevice);
            TextView tvItemStatus = itemView.findViewById(R.id.tvItemStatus);
            TextView tvItemDate = itemView.findViewById(R.id.tvItemDate);
            TextView tvItemCost = itemView.findViewById(R.id.tvItemCost);

            tvItemRepairId.setText(repairId);
            tvItemDevice.setText(device);
            tvItemStatus.setText(status);
            tvItemDate.setText(date);
            tvItemCost.setText(cost);

            // Dynamic badge color coding based on status
            if ("completed".equalsIgnoreCase(status)) {
                tvItemStatus.setBackgroundResource(R.drawable.bg_status_success);
                tvItemStatus.setTextColor(getResources().getColor(R.color.customer_success));
            } else if ("in progress".equalsIgnoreCase(status)) {
                tvItemStatus.setBackgroundResource(R.drawable.bg_management_status_warning);
                tvItemStatus.setTextColor(getResources().getColor(R.color.customer_orange));
            } else {
                // Pending or other
                tvItemStatus.setBackgroundResource(R.drawable.bg_customer_chip);
                tvItemStatus.setTextColor(getResources().getColor(R.color.customer_muted));
            }

            repairHistoryList.addView(itemView);
        }
        cursor.close();
    }
}