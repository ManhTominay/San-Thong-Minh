package org.example.bookingservice.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class BookingRequestDTO {

    private Long userId;
    private Long fieldId;
    private Long stadiumId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String courtSummary;
    private String stadiumName;
    private String stadiumAddress;
    private Double totalPrice;
    private String status;

    public BookingRequestDTO() {}

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
}