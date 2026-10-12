package org.example.bookingservice.dto;

public record FieldBookingStatisticsDTO(
        Long fieldId,
        String fieldName,
        String stadiumName,
        Long bookingCount,
        Double revenue
) {
}
