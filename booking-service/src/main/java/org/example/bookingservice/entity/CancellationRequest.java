package org.example.bookingservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "cancellation_requests",
        indexes = {
                @Index(name = "idx_cancellation_booking_created", columnList = "booking_id, created_at"),
                @Index(name = "idx_cancellation_status_created", columnList = "status, created_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cancellation_pending_booking",
                columnNames = "pending_booking_id"
        )
)
public class CancellationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cancellation_booking"))
    private Booking booking;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "requester_email", nullable = false, length = 320)
    private String requesterEmail;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(
            name = "pending_booking_id",
            insertable = false,
            updatable = false,
            columnDefinition = "BIGINT GENERATED ALWAYS AS (CASE WHEN status = 'PENDING' THEN booking_id ELSE NULL END) STORED"
    )
    private Long pendingBookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by_user_id")
    private Long processedByUserId;

    @Column(name = "processed_by_email", length = 320)
    private String processedByEmail;

    @Column(name = "admin_note", length = 1000)
    private String adminNote;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public Long getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(Long requesterUserId) { this.requesterUserId = requesterUserId; }
    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    public Long getProcessedByUserId() { return processedByUserId; }
    public void setProcessedByUserId(Long processedByUserId) { this.processedByUserId = processedByUserId; }
    public String getProcessedByEmail() { return processedByEmail; }
    public void setProcessedByEmail(String processedByEmail) { this.processedByEmail = processedByEmail; }
    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}
