package com.example.techfixv2.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Technician {
    private String id;
    private String name;
    private String location;
    private String specialCategory;
    private String availability;
    private String mobileNumber;

    public Technician() {}

    public Technician(String id, String name, String location, String specialCategory, String availability, String mobileNumber) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.specialCategory = specialCategory;
        this.availability = availability;
        this.mobileNumber = mobileNumber;
    }

    // firestore
    public static Technician fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Technician tech = new Technician();
        tech.setId(doc.getId());
        tech.setName(doc.getString("name"));
        tech.setLocation(doc.getString("location"));
        tech.setSpecialCategory(doc.getString("specialCategory"));
        tech.setAvailability(doc.getString("availability"));
        tech.setMobileNumber(doc.getString("mobileNumber"));
        return tech;
    }

    // to map
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("location", location);
        map.put("specialCategory", specialCategory);
        map.put("availability", availability != null ? availability : "On Duty");
        map.put("mobileNumber", mobileNumber);
        return map;
    }


    public boolean isOnDuty() {
        if (availability == null) return false;
        String lower = availability.trim().toLowerCase();
        return lower.equals("on duty") || lower.equals("active") || lower.equals("available");
    }


    public boolean canRepairCategory(String category) {
        if (category == null || specialCategory == null) return false;
        return specialCategory.toLowerCase().contains(category.trim().toLowerCase());
    }

    // getters setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : "Staff Technician"; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location != null ? location : "Colombo"; }
    public void setLocation(String location) { this.location = location; }

    public String getSpecialCategory() { return specialCategory != null ? specialCategory : "General"; }
    public void setSpecialCategory(String specialCategory) { this.specialCategory = specialCategory; }

    public String getAvailability() { return availability != null ? availability : "On Duty"; }
    public void setAvailability(String availability) { this.availability = availability; }

    public String getMobileNumber() { return mobileNumber != null ? mobileNumber : "N/A"; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }
}
