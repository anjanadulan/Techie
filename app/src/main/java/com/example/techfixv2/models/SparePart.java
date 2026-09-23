package com.example.techfixv2.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;


public class SparePart {
    private String id;
    private String name;
    private String category;
    private String location;
    private int quantity;
    private double price;

    public SparePart() {}

    public SparePart(String id, String name, String category, String location, int quantity, double price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.location = location;
        this.quantity = quantity;
        this.price = price;
    }

    /**
     * firestore
     */
    public static SparePart fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        SparePart part = new SparePart();
        part.setId(doc.getId());
        part.setName(doc.getString("name"));
        part.setCategory(doc.getString("category"));
        part.setLocation(doc.getString("location"));

        Object qtyObj = doc.get("quantity");
        if (qtyObj != null) {
            try {
                part.setQuantity((int) Double.parseDouble(String.valueOf(qtyObj)));
            } catch (Exception ignored) {
                part.setQuantity(0);
            }
        }

        Object priceObj = doc.get("price");
        if (priceObj != null) {
            try {
                part.setPrice(Double.parseDouble(String.valueOf(priceObj)));
            } catch (Exception ignored) {
                part.setPrice(0.0);
            }
        }
        return part;
    }

    /**
     * convert
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("category", category);
        map.put("location", location);
        map.put("quantity", quantity);
        map.put("price", price);
        return map;
    }


    public boolean isInStock() {
        return quantity > 0;
    }

    public boolean isLowStock() {
        return quantity <= 2;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name != null ? name : "Spare Part"; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category != null ? category : "General"; }
    public void setCategory(String category) { this.category = category; }

    public String getLocation() { return location != null ? location : "Colombo"; }
    public void setLocation(String location) { this.location = location; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}
