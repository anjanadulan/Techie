package com.example.techfixv2.models;

import com.google.firebase.firestore.DocumentSnapshot;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Domain model representing a customer payment and invoice transaction.
 * Fulfills Coursework Objective #4 (Payment Processing) & Deliverable #3 (Complex Data Models).
 */
public class Payment implements Serializable {
    private String invoiceNo;
    private String repairId;
    private String customer;
    private String customerEmail;
    private double amount;
    private String paymentMethod;
    private String paymentStatus;
    private String date;
    private String branch;
    private String cardLast4;

    public Payment() {
        // Default constructor required for Firebase / serialization
    }

    public Payment(String invoiceNo, String repairId, String customer, String customerEmail,
                   double amount, String paymentMethod, String paymentStatus,
                   String date, String branch, String cardLast4) {
        this.invoiceNo = invoiceNo;
        this.repairId = repairId;
        this.customer = customer;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.date = date;
        this.branch = branch;
        this.cardLast4 = cardLast4;
    }

    public static Payment fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;

        Payment p = new Payment();
        p.setInvoiceNo(doc.getString("invoiceNo"));
        p.setRepairId(doc.getString("repairId"));
        p.setCustomer(doc.getString("customer"));
        p.setCustomerEmail(doc.getString("customerEmail"));
        
        Object amt = doc.get("amount");
        if (amt != null) {
            try {
                p.setAmount(Double.parseDouble(String.valueOf(amt)));
            } catch (Exception e) {
                p.setAmount(0.0);
            }
        }
        
        p.setPaymentMethod(doc.getString("paymentMethod"));
        p.setPaymentStatus(doc.getString("paymentStatus"));
        p.setDate(doc.getString("date"));
        p.setBranch(doc.getString("branch"));
        p.setCardLast4(doc.getString("cardLast4"));
        return p;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("invoiceNo", invoiceNo);
        map.put("repairId", repairId);
        map.put("customer", customer);
        map.put("customerEmail", customerEmail);
        map.put("amount", amount);
        map.put("paymentMethod", paymentMethod);
        map.put("paymentStatus", paymentStatus);
        map.put("date", date);
        map.put("branch", branch);
        map.put("cardLast4", cardLast4 != null ? cardLast4 : "");
        map.put("timestamp", System.currentTimeMillis());
        return map;
    }

    public String getFormattedAmount() {
        return String.format(Locale.US, "LKR %,.2f", amount);
    }

    // Getters and Setters
    public String getInvoiceNo() { return invoiceNo; }
    public void setInvoiceNo(String invoiceNo) { this.invoiceNo = invoiceNo; }

    public String getRepairId() { return repairId; }
    public void setRepairId(String repairId) { this.repairId = repairId; }

    public String getCustomer() { return customer; }
    public void setCustomer(String customer) { this.customer = customer; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
}
