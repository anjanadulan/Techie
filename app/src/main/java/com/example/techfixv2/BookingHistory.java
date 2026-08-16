package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
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

public class BookingHistory extends AppCompatActivity {

    private FirebaseAuth mAuth;
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

        mAuth = FirebaseAuth.getInstance();
        repairHistoryList = findViewById(R.id.repairHistoryList);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);

        // Load dynamic real-time booking history from Firestore
        loadFirestoreBookingHistory();

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

    private void loadFirestoreBookingHistory() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvHistoryEmpty.setText("Please sign in to view bookings.");
            tvHistoryEmpty.setVisibility(View.VISIBLE);
            return;
        }

        String email = user.getEmail();
        if (email == null) return;

        repairHistoryList.removeAllViews();

        // Fetch bookings matching this user's email from Firestore "appointments" collection
        FirebaseFirestore.getInstance()
                .collection("appointments")
                .whereEqualTo("userEmail", email.trim().toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            tvHistoryEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvHistoryEmpty.setVisibility(View.GONE);
                            LayoutInflater inflater = LayoutInflater.from(this);

                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                String rawId = doc.getId();
                                String repairId = "#TF-" + (rawId.length() > 5 ? rawId.substring(0, 5).toUpperCase() : rawId.toUpperCase());
                                String device = doc.getString("deviceName");
                                String desc = doc.getString("description");
                                String status = doc.getString("status");
                                Object costVal = doc.get("cost");
                                String cost = costVal != null ? "LKR " + String.valueOf(costVal) : "Pending";
                                String date = doc.getString("date");
                                if (date == null) date = "Recent";

                                View itemView = inflater.inflate(R.layout.item_repair_history, repairHistoryList, false);

                                TextView tvItemRepairId = itemView.findViewById(R.id.tvItemRepairId);
                                TextView tvItemDevice = itemView.findViewById(R.id.tvItemDevice);
                                TextView tvItemStatus = itemView.findViewById(R.id.tvItemStatus);
                                TextView tvItemDate = itemView.findViewById(R.id.tvItemDate);
                                TextView tvItemCost = itemView.findViewById(R.id.tvItemCost);

                                tvItemRepairId.setText(repairId);
                                tvItemDevice.setText(device + (desc != null && !desc.isEmpty() ? " · " + desc : ""));
                                tvItemStatus.setText(status);
                                tvItemDate.setText(date);
                                tvItemCost.setText(cost);

                                // Dynamic badge color configuration
                                if ("completed".equalsIgnoreCase(status)) {
                                    tvItemStatus.setBackgroundResource(R.drawable.bg_status_success);
                                    tvItemStatus.setTextColor(getResources().getColor(R.color.customer_success));
                                } else if ("in progress".equalsIgnoreCase(status)) {
                                    tvItemStatus.setBackgroundResource(R.drawable.bg_management_status_warning);
                                    tvItemStatus.setTextColor(getResources().getColor(R.color.customer_orange));
                                } else {
                                    tvItemStatus.setBackgroundResource(R.drawable.bg_customer_chip);
                                    tvItemStatus.setTextColor(getResources().getColor(R.color.customer_muted));
                                }

                                repairHistoryList.addView(itemView);
                            }
                        }
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Failed to load bookings: " + err, Toast.LENGTH_LONG).show();
                        tvHistoryEmpty.setText("Error loading bookings.");
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }
}