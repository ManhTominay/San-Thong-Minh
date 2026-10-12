package org.example.bookingservice.dto;

import org.example.bookingservice.entity.Booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record BookingResponseDTO(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        Long fieldId,
        String fieldName,
        Long stadiumId,
        String stadiumName,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        Double totalPrice,
        String status,
        LocalDateTime createdAt
) {
    public static BookingResponseDTO from(Booking booking) {
        return new BookingResponseDTO(
                booking.getId(),
                booking.getUserId(),
                booking.getUserName(),
                booking.getUserEmail(),
                booking.getFieldId(),
                booking.getFieldName(),
                booking.getStadiumId(),
                booking.getStadiumName(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getTotalPrice(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
