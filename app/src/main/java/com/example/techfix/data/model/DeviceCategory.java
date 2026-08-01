package com.example.techfix.data.model;

public class DeviceCategory {
    private final long id;
    private final String name;
    private final String description;
    private final boolean active;

    public DeviceCategory(long id, String name, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
