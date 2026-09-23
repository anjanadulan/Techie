package com.example.techfixv2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.techfixv2.models.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookingHistory extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;
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
        dbHelper = new DatabaseHelper(this);

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
                            TextView tvItemPaymentStatus = itemView.findViewById(R.id.tvItemPaymentStatus);
                            TextView tvItemDate = itemView.findViewById(R.id.tvItemDate);
                            TextView tvItemCost = itemView.findViewById(R.id.tvItemCost);

                            tvItemRepairId.setText(repairId);
                            tvItemDevice.setText(device + (desc != null && !desc.isEmpty() ? " · " + desc : ""));
                            tvItemStatus.setText(status);
                            tvItemDate.setText(date);
                            tvItemCost.setText(cost);

                            String paymentStatus = doc.getString("paymentStatus");
                            if (paymentStatus == null || paymentStatus.isEmpty()) paymentStatus = "Unpaid";
                            String invoiceNo = doc.getString("invoiceNo");
                            String paymentMethod = doc.getString("paymentMethod");

                            if (tvItemPaymentStatus != null) {
                                if ("Paid".equalsIgnoreCase(paymentStatus)) {
                                    tvItemPaymentStatus.setVisibility(View.VISIBLE);
                                    tvItemPaymentStatus.setText("PAID ✓");
                                    tvItemPaymentStatus.setBackgroundResource(R.drawable.bg_status_success);
                                    tvItemPaymentStatus.setTextColor(getResources().getColor(R.color.customer_success));
                                } else {
                                    tvItemPaymentStatus.setVisibility(View.GONE);
                                }
                            }

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
                            final String finalPaymentStatus = paymentStatus;
                            final String finalInvoiceNo = invoiceNo != null ? invoiceNo : ("INV-2026-" + (rawId.length() > 4 ? rawId.substring(0, 4) : "0001"));
                            final String finalPaymentMethod = paymentMethod != null ? paymentMethod : "Paid";
                            final double parsedCost = costVal != null ? Double.parseDouble(String.valueOf(costVal)) : 0.0;

                            itemView.setOnClickListener(v -> showBookingActionDialog(
                                    rawId, repairId, finalDevice, finalDesc, finalStatus,
                                    finalCost, parsedCost, finalDate, finalBranch, finalTime,
                                    finalPaymentStatus, finalInvoiceNo, finalPaymentMethod));

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

    private void showBookingActionDialog(String docId, String repairId, String device, String desc,
                                        String status, String cost, double costVal, String date,
                                        String branch, String time, String paymentStatus,
                                        String invoiceNo, String paymentMethod) {
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

        if ("Pending".equalsIgnoreCase(status)) {
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
        } else {
            // Repair is In Progress or Completed
            if (tvTitle != null) tvTitle.setText("Repair & Invoice Details");
            if (tvMessage != null) {
                tvMessage.setText(String.format("Status: %s • Assigned: %s Branch • Total: %s", status, (branch != null ? branch : "Colombo"), cost));
            }

            if (btnOpt1 != null) {
                if ("Paid".equalsIgnoreCase(paymentStatus)) {
                    btnOpt1.setText("🧾 View Paid Invoice & Receipt");
                    btnOpt1.setOnClickListener(v -> {
                        dialog.dismiss();
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userEmail = user != null && user.getEmail() != null ? user.getEmail() : "";
                        String custName = dbHelper != null ? dbHelper.getUserName(userEmail) : "Customer";
                        Payment p = new Payment(
                                invoiceNo != null ? invoiceNo : "INV-2026-PAID",
                                docId,
                                custName,
                                userEmail,
                                costVal,
                                paymentMethod != null ? paymentMethod : "Settled",
                                "Paid",
                                date,
                                branch != null ? branch : "Colombo",
                                "4242"
                        );
                        showReceiptDialog(p, device, desc);
                    });
                } else {
                    btnOpt1.setText("💳 Pay Invoice / Checkout (" + cost + ")");
                    btnOpt1.setOnClickListener(v -> {
                        dialog.dismiss();
                        showPaymentDialog(docId, repairId, device, desc, costVal, branch, date);
                    });
                }
            }

            if (btnOpt2 != null) {
                btnOpt2.setText("📄 View Full Diagnostic Details");
                btnOpt2.setOnClickListener(v -> {
                    dialog.dismiss();
                    showDetailedInfoDialog(device, desc, branch, date, time, cost, status, paymentStatus, invoiceNo);
                });
            }
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void showDetailedInfoDialog(String device, String desc, String branch, String date, String time, String cost, String status, String paymentStatus, String invoiceNo) {
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

        if (tvTitle != null) tvTitle.setText("Repair Diagnostic Report");
        if (btnAction instanceof TextView) {
            ((TextView) btnAction).setText("Close");
        }
        btnAction.setOnClickListener(v -> dialog.dismiss());

        String info = "Device: " + device + "\n" +
                "Issue / Service: " + (desc != null ? desc : "Standard Hardware Repair") + "\n" +
                "Assigned Branch: " + (branch != null ? branch : "Colombo Center") + "\n" +
                "Schedule: " + date + " @ " + (time != null ? time : "Standard Slot") + "\n" +
                "Cost: " + cost + "\n" +
                "Work Status: " + status + "\n" +
                "Payment Status: " + (paymentStatus != null ? paymentStatus.toUpperCase() : "UNPAID") +
                (invoiceNo != null && !invoiceNo.isEmpty() ? "\nInvoice Reference: #" + invoiceNo : "") + "\n\n" +
                "Note: All genuine parts and labor are backed by TechFix standard 6-month repair warranty.";

        if (tvMessage != null) tvMessage.setText(info);
        dialog.show();
    }

    private void showPaymentDialog(String docId, String repairId, String device, String desc, double amount, String branch, String date) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_checkout, null);
        TextView tvInvoiceNo = dialogView.findViewById(R.id.tvCheckoutInvoiceNo);
        TextView tvDevice = dialogView.findViewById(R.id.tvCheckoutDevice);
        TextView tvService = dialogView.findViewById(R.id.tvCheckoutService);
        TextView tvBranch = dialogView.findViewById(R.id.tvCheckoutBranch);
        TextView tvTotal = dialogView.findViewById(R.id.tvCheckoutTotal);

        RadioGroup rgPaymentMethod = dialogView.findViewById(R.id.rgPaymentMethod);
        RadioButton rbPaymentCard = dialogView.findViewById(R.id.rbPaymentCard);
        RadioButton rbPaymentCash = dialogView.findViewById(R.id.rbPaymentCash);
        View layoutCardFields = dialogView.findViewById(R.id.layoutCardFields);

        EditText etCardHolder = dialogView.findViewById(R.id.etCardHolder);
        EditText etCardNumber = dialogView.findViewById(R.id.etCardNumber);
        EditText etCardExpiry = dialogView.findViewById(R.id.etCardExpiry);
        EditText etCardCvv = dialogView.findViewById(R.id.etCardCvv);

        View btnPayConfirm = dialogView.findViewById(R.id.btnPayConfirm);
        View btnPayCancel = dialogView.findViewById(R.id.btnPayCancel);

        String generatedInvoice = "INV-2026-" + (int)(1000 + Math.random() * 9000);
        tvInvoiceNo.setText("Invoice: #" + generatedInvoice);
        tvDevice.setText(device != null ? device : "TechFix Repair Device");
        tvService.setText(desc != null && !desc.isEmpty() ? desc : "Diagnostic & Hardware Service");
        tvBranch.setText("Branch: " + (branch != null ? branch : "Colombo Center"));
        tvTotal.setText(String.format(Locale.US, "LKR %,.2f", amount));

        rgPaymentMethod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPaymentCard) {
                layoutCardFields.setVisibility(View.VISIBLE);
            } else {
                layoutCardFields.setVisibility(View.GONE);
            }
        });

        AlertDialog checkoutDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (checkoutDialog.getWindow() != null) {
            checkoutDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnPayCancel.setOnClickListener(v -> checkoutDialog.dismiss());

        btnPayConfirm.setOnClickListener(v -> {
            String method;
            String cardLast4 = "";

            if (rbPaymentCard.isChecked()) {
                String holder = etCardHolder.getText().toString().trim();
                String cardNum = etCardNumber.getText().toString().trim().replaceAll("\\s+", "");
                String expiry = etCardExpiry.getText().toString().trim();
                String cvv = etCardCvv.getText().toString().trim();

                if (holder.isEmpty()) {
                    Toast.makeText(BookingHistory.this, "Please enter cardholder name", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (cardNum.length() < 12) {
                    Toast.makeText(BookingHistory.this, "Please enter a valid 16-digit card number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (expiry.isEmpty() || !expiry.contains("/")) {
                    Toast.makeText(BookingHistory.this, "Please enter expiry date (MM/YY)", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (cvv.length() < 3) {
                    Toast.makeText(BookingHistory.this, "Please enter a 3 or 4-digit CVV", Toast.LENGTH_SHORT).show();
                    return;
                }

                cardLast4 = cardNum.length() >= 4 ? cardNum.substring(cardNum.length() - 4) : "4242";
                method = "Credit/Debit Card (•••• " + cardLast4 + ")";
            } else {
                method = "Cash at Branch Counter";
                cardLast4 = "Cash";
            }

            FirebaseUser user = mAuth.getCurrentUser();
            String userEmail = user != null && user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
            String rawCustName = dbHelper != null ? dbHelper.getUserName(userEmail) : "Customer";
            final String customerName = (rawCustName != null && !rawCustName.isEmpty() && !"User".equals(rawCustName))
                    ? rawCustName : (userEmail.contains("@") ? userEmail.split("@")[0] : "Customer");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US);
            String timestamp = sdf.format(new Date());

            Payment payment = new Payment(
                    generatedInvoice,
                    docId,
                    customerName,
                    userEmail,
                    amount,
                    method,
                    "Paid",
                    timestamp,
                    branch != null ? branch : "Colombo",
                    cardLast4
            );

            btnPayConfirm.setEnabled(false);

            // 1. Write payment record to Firestore "payments" collection
            db.collection("payments").add(payment.toMap())
                    .addOnSuccessListener(docRef -> {
                        // 2. Update appointment document with payment status
                        Map<String, Object> updateMap = new HashMap<>();
                        updateMap.put("paymentStatus", "Paid");
                        updateMap.put("invoiceNo", generatedInvoice);
                        updateMap.put("paymentMethod", method);
                        updateMap.put("paidAmount", amount);

                        db.collection("appointments").document(docId).update(updateMap);

                        // 3. Save to local SQLite database
                        if (dbHelper != null) {
                            dbHelper.addPayment(generatedInvoice, docId, customerName, amount, method, "Paid", timestamp);
                        }

                        checkoutDialog.dismiss();
                        Toast.makeText(BookingHistory.this, "Payment successful! Invoice #" + generatedInvoice + " issued.", Toast.LENGTH_LONG).show();

                        // 4. Show Digital Receipt & refresh list
                        showReceiptDialog(payment, device, desc);
                        loadFirestoreBookingHistory();
                    })
                    .addOnFailureListener(e -> {
                        btnPayConfirm.setEnabled(true);
                        Toast.makeText(BookingHistory.this, "Payment processing failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        checkoutDialog.show();
    }

    private void showReceiptDialog(Payment payment, String device, String desc) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_payment_receipt, null);
        TextView tvInvoice = dialogView.findViewById(R.id.tvReceiptInvoiceNo);
        TextView tvDate = dialogView.findViewById(R.id.tvReceiptDate);
        TextView tvCustomer = dialogView.findViewById(R.id.tvReceiptCustomer);
        TextView tvBranch = dialogView.findViewById(R.id.tvReceiptBranch);
        TextView tvDetails = dialogView.findViewById(R.id.tvReceiptDetails);
        TextView tvMethod = dialogView.findViewById(R.id.tvReceiptMethod);
        TextView tvTotal = dialogView.findViewById(R.id.tvReceiptTotal);
        View btnDone = dialogView.findViewById(R.id.btnReceiptDone);

        tvInvoice.setText("#" + payment.getInvoiceNo());
        tvDate.setText(payment.getDate() != null ? payment.getDate() : "Just now");
        tvCustomer.setText(payment.getCustomer() != null ? payment.getCustomer() : "Customer");
        tvBranch.setText((payment.getBranch() != null ? payment.getBranch() : "Colombo") + " Branch");
        tvDetails.setText((device != null ? device : "Device") + " · " + (desc != null && !desc.isEmpty() ? desc : "Repair Service"));
        tvMethod.setText(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "Paid");
        tvTotal.setText(payment.getFormattedAmount());

        AlertDialog receiptDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (receiptDialog.getWindow() != null) {
            receiptDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnDone.setOnClickListener(v -> receiptDialog.dismiss());
        receiptDialog.show();
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