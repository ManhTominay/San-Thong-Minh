package org.example.bookingservice;

import org.example.bookingservice.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BookingStatisticsQueryTest {

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void aggregateQueriesReturnEmptyRangeStatistics() {
        LocalDate from = LocalDate.of(2999, 1, 1);
        LocalDate to = LocalDate.of(2999, 1, 2);

        var overview = bookingRepository.getStatisticsOverview(from, to);

        assertEquals(0L, overview.totalBookings());
        assertEquals(0L, overview.confirmedBookings());
        assertEquals(0L, overview.pendingBookings());
        assertEquals(0L, overview.cancelledBookings());
        assertEquals(0L, overview.otherBookings());
        assertEquals(0.0, overview.totalRevenue());
        assertTrue(bookingRepository.getDailyStatistics(from, to).isEmpty());
        assertTrue(bookingRepository.getTopFieldStatistics(from, to, PageRequest.of(0, 5)).isEmpty());
    }
}
