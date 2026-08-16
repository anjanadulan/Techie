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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Services extends AppCompatActivity {

    private LinearLayout servicesContainer;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_services);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        servicesContainer = findViewById(R.id.servicesContainer);

        // Load live dynamic service prices list from Firestore
        loadLiveServicePrices();

        // Bottom Navigation: Go to Home
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, CustomerHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation: Go to Bookings
        findViewById(R.id.navBookings).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, BookingHistory.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation: Go to BookRepairActivity
        findViewById(R.id.navBookRepair).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, BookRepairActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation: Go to Profile
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, UserProfile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void loadLiveServicePrices() {
        if (servicesContainer == null) return;
        servicesContainer.removeAllViews();

        // Query service options from Firestore service_prices collection
        db.collection("service_prices")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        if (task.getResult().isEmpty()) {
                            showEmptyPlaceholder();
                        } else {
                            LayoutInflater inflater = LayoutInflater.from(this);

                            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                                String name = doc.getString("name");
                                String category = doc.getString("category");
                                String estTime = doc.getString("estimatedTime");
                                Object priceVal = doc.get("estimatedPrice");
                                String status = doc.getString("status");

                                // Only display active services
                                if (status != null && !"active".equalsIgnoreCase(status)) {
                                    continue;
                                }

                                View itemView = inflater.inflate(R.layout.item_service_price, servicesContainer, false);

                                TextView tvServiceName = itemView.findViewById(R.id.tvServiceName);
                                TextView tvServiceDescription = itemView.findViewById(R.id.tvServiceDescription);
                                TextView tvServicePrice = itemView.findViewById(R.id.tvServicePrice);

                                if (tvServiceName != null) tvServiceName.setText(name);
                                if (tvServiceDescription != null) {
                                    tvServiceDescription.setText("Category: " + (category != null ? category : "General") + 
                                            " · Repair time: " + (estTime != null ? estTime : "1-2 hours"));
                                }
                                if (tvServicePrice != null) {
                                    int price = priceVal != null ? (int) Double.parseDouble(String.valueOf(priceVal)) : 0;
                                    tvServicePrice.setText("Starting from LKR " + price);
                                }

                                servicesContainer.addView(itemView);
                            }
                        }
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Failed to load services: " + err, Toast.LENGTH_LONG).show();
                        showEmptyPlaceholder();
                    }
                });
    }

    private void showEmptyPlaceholder() {
        TextView tvPlaceholder = new TextView(this);
        tvPlaceholder.setText("No services available at the moment.");
        tvPlaceholder.setPadding(0, 50, 0, 0);
        tvPlaceholder.setGravity(android.view.Gravity.CENTER);
        tvPlaceholder.setTextColor(getResources().getColor(R.color.customer_muted));
        servicesContainer.addView(tvPlaceholder);
    }
}