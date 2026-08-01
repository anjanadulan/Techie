package com.example.techfix.data.model;

public class SparePart {
    private final long id;
    private final long branchId;
    private final Long categoryId;
    private final String name;
    private final String sku;
    private final long unitPriceCents;
    private final int quantityAvailable;
    private final boolean active;

    public SparePart(long id, long branchId, Long categoryId, String name, String sku,
                     long unitPriceCents, int quantityAvailable, boolean active) {
        this.id = id;
        this.branchId = branchId;
        this.categoryId = categoryId;
        this.name = name;
        this.sku = sku;
        this.unitPriceCents = unitPriceCents;
        this.quantityAvailable = quantityAvailable;
        this.active = active;
    }

    public long getId() { return id; }
    public long getBranchId() { return branchId; }
    public Long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public long getUnitPriceCents() { return unitPriceCents; }
    public int getQuantityAvailable() { return quantityAvailable; }
    public boolean isActive() { return active; }
}
