package com.example.techfix.data.model;

public class Technician {
    private final long id;
    private final long branchId;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String specialty;
    private final boolean active;

    public Technician(long id, long branchId, String fullName, String email,
                      String phone, String specialty, boolean active) {
        this.id = id;
        this.branchId = branchId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.specialty = specialty;
        this.active = active;
    }

    public long getId() { return id; }
    public long getBranchId() { return branchId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getSpecialty() { return specialty; }
    public boolean isActive() { return active; }
}
