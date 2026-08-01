package com.example.techfix.data.model;

public class Payment {
    private final long id;
    private final long appointmentId;
    private final long amountCents;
    private final PaymentMethod method;
    private final PaymentStatus status;
    private final String reference;
    private final Long paidAt;
    private final long createdAt;

    public Payment(long id, long appointmentId, long amountCents, PaymentMethod method,
                   PaymentStatus status, String reference, Long paidAt, long createdAt) {
        this.id = id;
        this.appointmentId = appointmentId;
        this.amountCents = amountCents;
        this.method = method;
        this.status = status;
        this.reference = reference;
        this.paidAt = paidAt;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public long getAppointmentId() { return appointmentId; }
    public long getAmountCents() { return amountCents; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getReference() { return reference; }
    public Long getPaidAt() { return paidAt; }
    public long getCreatedAt() { return createdAt; }
}
