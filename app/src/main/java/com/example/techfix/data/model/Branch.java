package com.example.techfix.data.model;

public class Branch {
    private final long id;
    private final String name;
    private final String address;
    private final String phone;
    private final double latitude;
    private final double longitude;
    private final boolean active;

    public Branch(long id, String name, String address, String phone,
                  double latitude, double longitude, boolean active) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.active = active;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isActive() { return active; }
}
