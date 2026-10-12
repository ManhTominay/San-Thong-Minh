package org.example.bookingservice.dto;

public record BookingStatisticsOverviewDTO(
        Long totalBookings,
        Long confirmedBookings,
        Long pendingBookings,
        Long cancelledBookings,
        Long otherBookings,
        Double totalRevenue
) {
}
