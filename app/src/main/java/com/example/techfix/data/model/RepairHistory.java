package com.example.techfix.data.model;

public class RepairHistory {
    private final long id;
    private final long appointmentId;
    private final AppointmentStatus status;
    private final String notes;
    private final String imagePath;
    private final long recordedAt;

    public RepairHistory(long id, long appointmentId, AppointmentStatus status,
                         String notes, String imagePath, long recordedAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.status = status;
        this.notes = notes;
        this.imagePath = imagePath;
        this.recordedAt = recordedAt;
    }

    public long getId() { return id; }
    public long getAppointmentId() { return appointmentId; }
    public AppointmentStatus getStatus() { return status; }
    public String getNotes() { return notes; }
    public String getImagePath() { return imagePath; }
    public long getRecordedAt() { return recordedAt; }
}
