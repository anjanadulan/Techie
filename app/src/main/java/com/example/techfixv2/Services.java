package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class Services extends AppCompatActivity {

    private SwipeRefreshLayout refreshLayout;
    private EditText etServiceSearch;
    private LinearLayout servicesList;
    private TextView tvServicesEmpty;

    // Filter Category Chips
    private TextView filterAll, filterPhone, filterLaptop, filterTablet;

    private FirebaseFirestore db;
    private List<DocumentSnapshot> allServiceDocs = new ArrayList<>();

    // Current filter states
    private String activeCategory = "All"; // "All", "Phone", "Laptop", "Tablet"
    private String searchQuery = "";

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

        // Initialize Views
        refreshLayout = findViewById(R.id.refreshLayout);
        etServiceSearch = findViewById(R.id.etServiceSearch);
        servicesList = findViewById(R.id.servicesList);
        tvServicesEmpty = findViewById(R.id.tvServicesEmpty);

        filterAll = findViewById(R.id.filterAll);
        filterPhone = findViewById(R.id.filterPhone);
        filterLaptop = findViewById(R.id.filterLaptop);
        filterTablet = findViewById(R.id.filterTablet);

        // Swipe refresh setup
        refreshLayout.setOnRefreshListener(this::loadLiveServicePrices);

        // Bind Category Chips click listeners
        setupCategoryChipListeners();

        // Search text watcher for live in-memory searching
        setupSearchTextWatcher();

        // Fetch dynamic service prices list from Firestore
        loadLiveServicePrices();

        // Bottom Navigation click listener: Go to Home
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, CustomerHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation click listener: Go to Bookings
        findViewById(R.id.navBookings).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, BookingHistory.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation click listener: Go to BookRepairActivity
        findViewById(R.id.navBookRepair).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, BookRepairActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation click listener: Go to Profile
        findViewById(R.id.navProfile).setOnClickListener(v -> {
            Intent intent = new Intent(Services.this, UserProfile.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupCategoryChipListeners() {
        if (filterAll == null || filterPhone == null || filterLaptop == null || filterTablet == null) return;

        filterAll.setOnClickListener(v -> {
            activeCategory = "All";
            updateCategoryChipsUI();
            filterAndRenderServices();
        });

        filterPhone.setOnClickListener(v -> {
            activeCategory = "Phone";
            updateCategoryChipsUI();
            filterAndRenderServices();
        });

        filterLaptop.setOnClickListener(v -> {
            activeCategory = "Laptop";
            updateCategoryChipsUI();
            filterAndRenderServices();
        });

        filterTablet.setOnClickListener(v -> {
            activeCategory = "Tablet";
            updateCategoryChipsUI();
            filterAndRenderServices();
        });
    }

    private void updateCategoryChipsUI() {
        // Reset all category chips to unselected design
        TextView[] chips = {filterAll, filterPhone, filterLaptop, filterTablet};
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.bg_customer_chip);
            chip.setTextColor(getResources().getColor(R.color.customer_muted));
            chip.setTypeface(null, android.graphics.Typeface.NORMAL);
        }

        // Apply selected design highlight to active category chip
        if ("All".equals(activeCategory)) {
            filterAll.setBackgroundResource(R.drawable.bg_customer_chip_selected);
            filterAll.setTextColor(getResources().getColor(R.color.white));
            filterAll.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if ("Phone".equals(activeCategory)) {
            filterPhone.setBackgroundResource(R.drawable.bg_customer_chip_selected);
            filterPhone.setTextColor(getResources().getColor(R.color.white));
            filterPhone.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if ("Laptop".equals(activeCategory)) {
            filterLaptop.setBackgroundResource(R.drawable.bg_customer_chip_selected);
            filterLaptop.setTextColor(getResources().getColor(R.color.white));
            filterLaptop.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if ("Tablet".equals(activeCategory)) {
            filterTablet.setBackgroundResource(R.drawable.bg_customer_chip_selected);
            filterTablet.setTextColor(getResources().getColor(R.color.white));
            filterTablet.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }

    private void setupSearchTextWatcher() {
        etServiceSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Ignore hint placeholder value if match
                String txt = s.toString();
                if (txt.equals(getString(R.string.search_services))) {
                    searchQuery = "";
                } else {
                    searchQuery = txt.trim();
                }
                filterAndRenderServices();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear query if user starts typing in search input field
        etServiceSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && etServiceSearch.getText().toString().equals(getString(R.string.search_services))) {
                etServiceSearch.setText("");
            }
        });
    }

    private void loadLiveServicePrices() {
        refreshLayout.setRefreshing(true);

        db.collection("service_prices")
                .get()
                .addOnCompleteListener(task -> {
                    refreshLayout.setRefreshing(false);
                    if (task.isSuccessful() && task.getResult() != null) {
                        allServiceDocs = task.getResult().getDocuments();
                        filterAndRenderServices();
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Failed to load services: " + err, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void filterAndRenderServices() {
        if (servicesList == null) return;
        servicesList.removeAllViews();

        int matchCount = 0;
        LayoutInflater inflater = LayoutInflater.from(this);

        for (DocumentSnapshot doc : allServiceDocs) {
            String name = doc.getString("name");
            String category = doc.getString("category");
            String estTime = doc.getString("estimatedTime");
            Object priceVal = doc.get("estimatedPrice");
            String status = doc.getString("status");

            // 1. Skip inactive service categories
            if (status != null && !"active".equalsIgnoreCase(status)) {
                continue;
            }

            // 2. Apply Category Filters (smart mapping to accommodate seeded inputs)
            if (!"All".equals(activeCategory)) {
                String dbCat = category != null ? category.toLowerCase() : "";
                if ("Phone".equals(activeCategory)) {
                    if (!dbCat.equals("phone") && !dbCat.equals("iphone") && !dbCat.equals("android")) {
                        continue;
                    }
                } else if ("Laptop".equals(activeCategory)) {
                    if (!dbCat.equals("laptop") && !dbCat.equals("desktop") && !dbCat.equals("macbook")) {
                        continue;
                    }
                } else if ("Tablet".equals(activeCategory)) {
                    if (!dbCat.equals("tablet") && !dbCat.equals("ipad")) {
                        continue;
                    }
                }
            }

            // 3. Apply Keyword Search Query Filter
            if (!searchQuery.isEmpty()) {
                String searchTarget = (name != null ? name.toLowerCase() : "") + " " + (category != null ? category.toLowerCase() : "");
                if (!searchTarget.contains(searchQuery.toLowerCase())) {
                    continue;
                }
            }

            // Render matching service card
            matchCount++;
            View itemView = inflater.inflate(R.layout.item_service_price, servicesList, false);

            TextView tvServiceName = itemView.findViewById(R.id.tvServiceName);
            TextView tvServiceDescription = itemView.findViewById(R.id.tvServiceDescription);
            TextView tvServicePrice = itemView.findViewById(R.id.tvServicePrice);

            if (tvServiceName != null) tvServiceName.setText(name);
            if (tvServiceDescription != null) {
                tvServiceDescription.setText("Category: " + (category != null ? category : "General") + 
                        " · Repair time: " + (estTime != null ? estTime : "1-2 hours"));
            }
            double priceDouble = priceVal != null ? Double.parseDouble(String.valueOf(priceVal)) : 0.0;
            if (tvServicePrice != null) {
                tvServicePrice.setText("Starting from LKR " + (int) priceDouble);
            }

            // Click to pre-book this service
            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(Services.this, BookRepairActivity.class);
                intent.putExtra("preselected_service", name);
                intent.putExtra("preselected_category", category);
                intent.putExtra("preselected_cost", priceDouble);
                startActivity(intent);
            });

            servicesList.addView(itemView);
        }

        // Show empty placeholder layout if no matching repairs found
        if (matchCount == 0) {
            tvServicesEmpty.setVisibility(View.VISIBLE);
        } else {
            tvServicesEmpty.setVisibility(View.GONE);
        }
    }
}