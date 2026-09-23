package com.example.techfixv2.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class RepairAppointment {
    private String documentId;
    private String repairId;
    private String clientName;
    private String userEmail;
    private String deviceName;
    private String description;
    private String branch;
    private String date;
    private String time;
    private double cost;
    private String status;
    private String photoUri;

    public RepairAppointment() {}

    public RepairAppointment(String documentId, String clientName, String userEmail, String deviceName,
                             String description, String branch, String date, String time,
                             double cost, String status, String photoUri) {
        this.documentId = documentId;
        this.clientName = clientName;
        this.userEmail = userEmail;
        this.deviceName = deviceName;
        this.description = description;
        this.branch = branch;
        this.date = date;
        this.time = time;
        this.cost = cost;
        this.status = status;
        this.photoUri = photoUri;
        this.repairId = generateRepairId(documentId);
    }

    // firestore
    public static RepairAppointment fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        RepairAppointment appt = new RepairAppointment();
        appt.setDocumentId(doc.getId());
        appt.setRepairId(generateRepairId(doc.getId()));
        appt.setClientName(doc.getString("clientName"));
        appt.setUserEmail(doc.getString("userEmail"));
        appt.setDeviceName(doc.getString("deviceName"));
        appt.setDescription(doc.getString("description"));
        appt.setBranch(doc.getString("branch"));
        appt.setDate(doc.getString("date"));
        appt.setTime(doc.getString("time"));
        appt.setStatus(doc.getString("status") != null ? doc.getString("status") : "Pending");
        appt.setPhotoUri(doc.getString("photoUri"));

        Object costObj = doc.get("cost");
        if (costObj != null) {
            try {
                appt.setCost(Double.parseDouble(String.valueOf(costObj)));
            } catch (Exception ignored) {
                appt.setCost(0.0);
            }
        }
        return appt;
    }

    // to map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("clientName", clientName != null ? clientName : "Client");
        map.put("userEmail", userEmail != null ? userEmail.trim().toLowerCase() : "");
        map.put("deviceName", deviceName);
        map.put("description", description);
        map.put("branch", branch);
        map.put("date", date);
        map.put("time", time);
        map.put("cost", cost);
        map.put("status", status != null ? status : "Pending");
        map.put("photoUri", photoUri != null ? photoUri : "");
        return map;
    }

    private static String generateRepairId(String docId) {
        if (docId == null) return "#TF-0000";
        return "#TF-" + (docId.length() > 5 ? docId.substring(0, 5).toUpperCase() : docId.toUpperCase());
    }

    public String getFormattedCost() {
        return "LKR " + (int) cost;
    }

    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isInProgress() {
        return "in progress".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isEditable() {
        return isPending();
    }

    // getters setters
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getRepairId() { return repairId != null ? repairId : generateRepairId(documentId); }
    public void setRepairId(String repairId) { this.repairId = repairId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhotoUri() { return photoUri; }
    public void setPhotoUri(String photoUri) { this.photoUri = photoUri; }
}
