package com.example.techfixv2.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a completed device repair record with diagnostic details and showcase photo.
 */
public class RepairedDevice {
    private String id;
    private String name;
    private String category;
    private String location;
    private String description;
    private double price;
    private String imageUrl;
    private String status;

    public RepairedDevice() {}

    public RepairedDevice(String id, String name, String category, String location, String description, double price, String imageUrl, String status) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.location = location;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.status = status;
    }

    /**
     * Factory method to deserialize a Firestore DocumentSnapshot into a typed RepairedDevice instance.
     */
    public static RepairedDevice fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        RepairedDevice device = new RepairedDevice();
        device.setId(doc.getId());
        device.setName(doc.getString("name"));
        device.setCategory(doc.getString("category"));
        device.setLocation(doc.getString("location"));
        device.setDescription(doc.getString("description"));
        device.setImageUrl(doc.getString("imageUrl"));
        device.setStatus(doc.getString("status") != null ? doc.getString("status") : "Completed");

        Object priceObj = doc.get("price");
        if (priceObj != null) {
            try {
                device.setPrice(Double.parseDouble(String.valueOf(priceObj)));
            } catch (Exception ignored) {
                device.setPrice(0.0);
            }
        }
        return device;
    }

    /**
     * Converts instance into a Map for Firestore persistence.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("category", category != null ? category : "Phone");
        map.put("location", location != null ? location : "Colombo");
        map.put("description", description);
        map.put("price", price);
        map.put("imageUrl", imageUrl != null ? imageUrl : "");
        map.put("status", status != null ? status : "Completed");
        return map;
    }

    public String getFormattedPrice() {
        return "LKR " + (int) price;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : "Repaired Device"; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category != null ? category : "Phone"; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location != null ? location : "Colombo"; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description != null ? description : "Hardware restored to full functionality."; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status != null ? status : "Completed"; }
    public void setStatus(String status) { this.status = status; }
}
