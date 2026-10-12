package org.example.bookingservice.dto;

import java.time.LocalDate;
import java.util.List;

public record BookingStatisticsDTO(
        LocalDate fromDate,
        LocalDate toDate,
        BookingStatisticsOverviewDTO overview,
        List<DailyBookingStatisticsDTO> dailyBookings,
        List<FieldBookingStatisticsDTO> topFields
) {
}
