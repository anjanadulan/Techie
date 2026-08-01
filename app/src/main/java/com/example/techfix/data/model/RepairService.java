package com.example.techfix.data.model;

public class RepairService {
    private final long id;
    private final long categoryId;
    private final String name;
    private final String description;
    private final long basePriceCents;
    private final int estimatedMinutes;
    private final boolean active;

    public RepairService(long id, long categoryId, String name, String description,
                         long basePriceCents, int estimatedMinutes, boolean active) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.basePriceCents = basePriceCents;
        this.estimatedMinutes = estimatedMinutes;
        this.active = active;
    }

    public long getId() { return id; }
    public long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public long getBasePriceCents() { return basePriceCents; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public boolean isActive() { return active; }
}
