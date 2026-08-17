package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
    private FirebaseFirestore db;
    private LinearLayout repairHistoryList;
    private TextView tvHistoryEmpty;

    // Filter Chips
    private TextView filterAll, filterActive, filterCompleted;
    private String activeFilter = "All"; // "All", "Active", "Completed"

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
        db = FirebaseFirestore.getInstance();

        repairHistoryList = findViewById(R.id.repairHistoryList);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);

        filterAll = findViewById(R.id.filterAll);
        filterActive = findViewById(R.id.filterActive);
        filterCompleted = findViewById(R.id.filterCompleted);

        // Bind filter clicks
        setupFilterListeners();

        // Load list
        loadFirestoreBookingHistory();

        // Bottom Navigation click listener to go back to CustomerHome
        findViewById(R.id.navHome).setOnClickListener(v -> {
            Intent intent = new Intent(BookingHistory.this, CustomerHome.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // Bottom Navigation click listener to go to BookRepairActivity
        findViewById(R.id.navBookRepair).setOnClickListener(v -> {
            Intent intent = new Intent(BookingHistory.this, BookRepairActivity.class);
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

    private void setupFilterListeners() {
        if (filterAll == null || filterActive == null || filterCompleted == null) return;

        filterAll.setOnClickListener(v -> {
            activeFilter = "All";
            updateFilterChipsUI();
            loadFirestoreBookingHistory();
        });

        filterActive.setOnClickListener(v -> {
            activeFilter = "Active";
            updateFilterChipsUI();
            loadFirestoreBookingHistory();
        });

        filterCompleted.setOnClickListener(v -> {
            activeFilter = "Completed";
            updateFilterChipsUI();
            loadFirestoreBookingHistory();
        });
    }

    private void updateFilterChipsUI() {
        // Reset all to default state
        filterAll.setBackgroundResource(R.drawable.bg_customer_chip);
        filterAll.setTextColor(getResources().getColor(R.color.customer_muted));
        filterAll.setTypeface(null, android.graphics.Typeface.NORMAL);

        filterActive.setBackgroundResource(R.drawable.bg_customer_chip);
        filterActive.setTextColor(getResources().getColor(R.color.customer_muted));
        filterActive.setTypeface(null, android.graphics.Typeface.NORMAL);

        filterCompleted.setBackgroundResource(R.drawable.bg_customer_chip);
        filterCompleted.setTextColor(getResources().getColor(R.color.customer_muted));
        filterCompleted.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Apply selected style to active one
        if ("All".equals(activeFilter)) {
            filterAll.setBackgroundResource(R.drawable.bg_customer_chip_selected);
            filterAll.setTextColor(getResources().getColor(R.color.white));
            filterAll.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if ("Active".equals(activeFilter)) {
            filterActive.setBackgroundResource(R.drawable.bg_customer_chip_selected);
            filterActive.setTextColor(getResources().getColor(R.color.white));
            filterActive.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if ("Completed".equals(activeFilter)) {
            filterCompleted.setBackgroundResource(R.drawable.bg_customer_chip_selected);
            filterCompleted.setTextColor(getResources().getColor(R.color.white));
            filterCompleted.setTypeface(null, android.graphics.Typeface.BOLD);
        }
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

        // Fetch bookings matching user email
        db.collection("appointments")
                .whereEqualTo("userEmail", email.trim().toLowerCase())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int displayedItems = 0;
                        LayoutInflater inflater = LayoutInflater.from(this);

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String status = doc.getString("status");
                            if (status == null) status = "Pending";

                            // Filter checklist
                            if ("Active".equals(activeFilter) && "Completed".equalsIgnoreCase(status)) {
                                continue;
                            }
                            if ("Completed".equals(activeFilter) && !"Completed".equalsIgnoreCase(status)) {
                                continue;
                            }

                            displayedItems++;

                            String rawId = doc.getId();
                            String repairId = "#TF-" + (rawId.length() > 5 ? rawId.substring(0, 5).toUpperCase() : rawId.toUpperCase());
                            String device = doc.getString("deviceName");
                            String desc = doc.getString("description");
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

                            // Badge backgrounds
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

                            // Setup Click actions
                            final String finalStatus = status;
                            final String finalDevice = device;
                            final String finalDesc = desc;
                            final String finalCost = cost;
                            final String finalDate = date;
                            final String finalBranch = doc.getString("branch");
                            final String finalTime = doc.getString("time");

                            itemView.setOnClickListener(v -> showBookingActionDialog(rawId, finalDevice, finalDesc, finalStatus, finalCost, finalDate, finalBranch, finalTime));

                            repairHistoryList.addView(itemView);
                        }

                        if (displayedItems == 0) {
                            tvHistoryEmpty.setText("No repairs found matching this category.");
                            tvHistoryEmpty.setVisibility(View.VISIBLE);
                        } else {
                            tvHistoryEmpty.setVisibility(View.GONE);
                        }
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Failed to load bookings: " + err, Toast.LENGTH_LONG).show();
                        tvHistoryEmpty.setText("Error loading bookings.");
                        tvHistoryEmpty.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void showBookingActionDialog(String docId, String device, String desc, String status, String cost, String date, String branch, String time) {
        if ("Pending".equalsIgnoreCase(status)) {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_options, null);
            TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
            TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
            TextView btnOpt1 = dialogView.findViewById(R.id.btnOption1);
            TextView btnOpt2 = dialogView.findViewById(R.id.btnOption2);
            View btnCancel = dialogView.findViewById(R.id.btnCancel);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            if (tvTitle != null) tvTitle.setText("Manage Appointment");
            if (tvMessage != null) tvMessage.setText("Select an action to perform on your pending repair booking.");

            if (btnOpt1 != null) {
                btnOpt1.setText("Edit Appointment Details");
                btnOpt1.setOnClickListener(v -> {
                    dialog.dismiss();
                    Intent intent = new Intent(BookingHistory.this, BookRepairActivity.class);
                    intent.putExtra("booking_id", docId);
                    startActivity(intent);
                });
            }

            if (btnOpt2 != null) {
                btnOpt2.setText("Cancel / Delete Appointment");
                btnOpt2.setOnClickListener(v -> {
                    dialog.dismiss();
                    confirmCancelBooking(docId);
                });
            }

            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
        } else {
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_info, null);
            TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
            TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
            View btnAction = dialogView.findViewById(R.id.btnAction);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            if (tvTitle != null) tvTitle.setText("Repair Details");
            if (btnAction instanceof TextView) {
                ((TextView) btnAction).setText("Close");
            }
            btnAction.setOnClickListener(v -> dialog.dismiss());

            String info = "Device: " + device + "\n" +
                    "Details: " + (desc != null ? desc : "None") + "\n" +
                    "Assigned Branch: " + (branch != null ? branch : "Colombo") + "\n" +
                    "Visiting Schedule: " + date + " @ " + (time != null ? time : "TBD") + "\n" +
                    "Estimated Cost: " + cost + "\n" +
                    "Current Status: " + status + "\n\n" +
                    "Note: This repair has been accepted or is underway. Please contact your branch for any schedule changes.";

            if (tvMessage != null) tvMessage.setText(info);
            dialog.show();
        }
    }

    private void confirmCancelBooking(String docId) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_generic_options, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
        TextView btnOpt1 = dialogView.findViewById(R.id.btnOption1);
        TextView btnOpt2 = dialogView.findViewById(R.id.btnOption2);
        View btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        if (tvTitle != null) tvTitle.setText("Cancel Appointment");
        if (tvMessage != null) tvMessage.setText("Are you sure you want to cancel and delete this repair appointment?");

        if (btnOpt1 != null) {
            btnOpt1.setText("No, Keep Appointment");
            btnOpt1.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnOpt2 != null) {
            btnOpt2.setText("Yes, Cancel Appointment");
            btnOpt2.setOnClickListener(v -> {
                dialog.dismiss();
                db.collection("appointments").document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(BookingHistory.this, "Appointment cancelled successfully.", Toast.LENGTH_SHORT).show();
                            loadFirestoreBookingHistory();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(BookingHistory.this, "Failed to cancel: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
            });
        }

        if (btnCancel != null) {
            btnCancel.setVisibility(View.GONE);
        }

        dialog.show();
    }
}