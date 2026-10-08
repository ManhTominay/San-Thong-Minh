package org.example.bookingservice.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "field_id", nullable = false)
    private Long fieldId;

    @Column(name = "stadium_id")
    private Long stadiumId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "court_summary", length = 2000)
    private String courtSummary;

    @Column(name = "stadium_name")
    private String stadiumName;

    @Column(name = "stadium_address", length = 1000)
    private String stadiumAddress;

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Booking() {}

    public Booking(Long userId, Long fieldId, LocalDate bookingDate, LocalTime startTime, LocalTime endTime, Double totalPrice, String status) {
        this.userId = userId;
        this.fieldId = fieldId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }

    public Long getStadiumId() { return stadiumId; }
    public void setStadiumId(Long stadiumId) { this.stadiumId = stadiumId; }

    public LocalDate getBookingDate() { return bookingDate; }
    public void setBookingDate(LocalDate bookingDate) { this.bookingDate = bookingDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getCourtSummary() { return courtSummary; }
    public void setCourtSummary(String courtSummary) { this.courtSummary = courtSummary; }

    public String getStadiumName() { return stadiumName; }
    public void setStadiumName(String stadiumName) { this.stadiumName = stadiumName; }

    public String getStadiumAddress() { return stadiumAddress; }
    public void setStadiumAddress(String stadiumAddress) { this.stadiumAddress = stadiumAddress; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}