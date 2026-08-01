package com.example.techfix.data.model;

public class Appointment {
    private final long id;
    private final long userId;
    private final Long branchId;
    private final Long technicianId;
    private final long serviceId;
    private final String deviceDetails;
    private final String problemDescription;
    private final AppointmentStatus status;
    private final long appointmentAt;
    private final long createdAt;

    public Appointment(long id, long userId, Long branchId, Long technicianId,
                       long serviceId, String deviceDetails, String problemDescription,
                       AppointmentStatus status, long appointmentAt, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.branchId = branchId;
        this.technicianId = technicianId;
        this.serviceId = serviceId;
        this.deviceDetails = deviceDetails;
        this.problemDescription = problemDescription;
        this.status = status;
        this.appointmentAt = appointmentAt;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getUserId() { return userId; }
    public Long getBranchId() { return branchId; }
    public Long getTechnicianId() { return technicianId; }
    public long getServiceId() { return serviceId; }
    public String getDeviceDetails() { return deviceDetails; }
    public String getProblemDescription() { return problemDescription; }
    public AppointmentStatus getStatus() { return status; }
    public long getAppointmentAt() { return appointmentAt; }
    public long getCreatedAt() { return createdAt; }
}
