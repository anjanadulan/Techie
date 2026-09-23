package com.example.techfixv2.models;

import android.location.Location;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// OOP Domain Model Encapsulation for Service Branch
public class Branch {
    private String id;
    private String name;
    private String address;
    private String phoneNumber;
    private String status;
    private double latitude;
    private double longitude;

    // 1-to-many domain model aggregation: Branch contains lists of Technicians and SpareParts
    private List<Technician> technicians = new ArrayList<>();
    private List<SparePart> spareParts = new ArrayList<>();

    public Branch() {}

    public Branch(String id, String name, String address, String phoneNumber, String status, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Cloud Firestore document serialization for Branch entity
    public static Branch fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Branch branch = new Branch();
        branch.setId(doc.getId());
        branch.setName(doc.getString("name"));
        branch.setAddress(doc.getString("address"));
        branch.setPhoneNumber(doc.getString("phoneNumber"));
        branch.setStatus(doc.getString("status"));

        // Coordinates definition for TechFix service branches (Colombo and Galle)
        if ("Colombo".equalsIgnoreCase(branch.getName())) {
            branch.setLatitude(6.9149);
            branch.setLongitude(79.8510);
        } else if ("Galle".equalsIgnoreCase(branch.getName())) {
            branch.setLatitude(6.0367);
            branch.setLongitude(80.2170);
        }
        return branch;
    }

    // Convert Branch object to Cloud Firestore document map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("address", address);
        map.put("phoneNumber", phoneNumber);
        map.put("status", status != null ? status : "open");
        return map;
    }

    public boolean isOpen() {
        return "open".equalsIgnoreCase(status) || "active".equalsIgnoreCase(status);
    }

    // Geodesic distance calculation to nearest branch using GPS
    public float getDistanceTo(double userLat, double userLng) {
        float[] results = new float[1];
        Location.distanceBetween(userLat, userLng, latitude, longitude, results);
        return results[0] / 1000f;
    }

    public boolean hasAvailableTechnician(String deviceCategory) {
        for (Technician tech : technicians) {
            if (tech.isOnDuty() && (deviceCategory == null || tech.canRepairCategory(deviceCategory))) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSparePartInStock(String deviceCategory) {
        for (SparePart part : spareParts) {
            if (part.isInStock()) {
                if (deviceCategory == null || part.getCategory().toLowerCase().contains(deviceCategory.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    // getters setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : "Branch"; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address != null ? address : "Sri Lanka"; }
    public void setAddress(String address) { this.address = address; }

    public String getPhoneNumber() { return phoneNumber != null ? phoneNumber : "N/A"; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getStatus() { return status != null ? status : "open"; }
    public void setStatus(String status) { this.status = status; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public List<Technician> getTechnicians() { return technicians; }
    public void setTechnicians(List<Technician> technicians) { this.technicians = technicians; }
    public void addTechnician(Technician technician) { this.technicians.add(technician); }

    public List<SparePart> getSpareParts() { return spareParts; }
    public void setSpareParts(List<SparePart> spareParts) { this.spareParts = spareParts; }
    public void addSparePart(SparePart sparePart) { this.spareParts.add(sparePart); }
}
