package org.example.bookingservice.dto;

import org.example.bookingservice.entity.Booking;

import java.time.LocalDate;
import java.time.LocalTime;

public class BookingAvailabilityDTO {
    private final Long id;
    private final Long fieldId;
    private final Long stadiumId;
    private final String stadiumName;
    private final LocalDate bookingDate;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String courtSummary;
    private final String status;

    public BookingAvailabilityDTO(Booking booking) {
        this.id = booking.getId();
        this.fieldId = booking.getFieldId();
        this.stadiumId = booking.getStadiumId();
        this.stadiumName = booking.getStadiumName();
        this.bookingDate = booking.getBookingDate();
        this.startTime = booking.getStartTime();
        this.endTime = booking.getEndTime();
        this.courtSummary = booking.getCourtSummary();
        this.status = booking.getStatus();
    }

    public Long getId() { return id; }
    public Long getFieldId() { return fieldId; }
    public Long getStadiumId() { return stadiumId; }
    public String getStadiumName() { return stadiumName; }
    public LocalDate getBookingDate() { return bookingDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getCourtSummary() { return courtSummary; }
    public String getStatus() { return status; }
}
