package org.example.bookingservice.dto;

import java.time.LocalDate;

public record DailyBookingStatisticsDTO(
        LocalDate bookingDate,
        Long bookingCount,
        Double revenue
) {
}
