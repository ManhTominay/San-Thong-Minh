package org.example.bookingservice;

import org.example.bookingservice.dto.BookingStatisticsOverviewDTO;
import org.example.bookingservice.dto.DailyBookingStatisticsDTO;
import org.example.bookingservice.dto.FieldBookingStatisticsDTO;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingStatisticsServiceTest {

    @Test
    void statisticsAreLoadedAsAggregatedDatabaseResults() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        BookingService bookingService = new BookingService(
                bookingRepository,
                fieldRepository,
                new RestTemplate()
        );
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 8);
        BookingStatisticsOverviewDTO overview =
                new BookingStatisticsOverviewDTO(10L, 7L, 2L, 1L, 0L, 1_250_000.0);
        List<DailyBookingStatisticsDTO> daily = List.of(
                new DailyBookingStatisticsDTO(to, 3L, 450_000.0)
        );
        List<FieldBookingStatisticsDTO> fields = List.of(
                new FieldBookingStatisticsDTO(4L, "Sân 4", "Trung Tâm", 5L, 800_000.0)
        );
        when(bookingRepository.getStatisticsOverview(from, to)).thenReturn(overview);
        when(bookingRepository.getDailyStatistics(from, to)).thenReturn(daily);
        when(bookingRepository.getTopFieldStatistics(from, to, PageRequest.of(0, 5)))
                .thenReturn(fields);

        var result = bookingService.getStatistics(from, to);

        assertEquals(from, result.fromDate());
        assertEquals(to, result.toDate());
        assertEquals(10L, result.overview().totalBookings());
        assertEquals(7L, result.overview().confirmedBookings());
        assertEquals(1_250_000.0, result.overview().totalRevenue());
        assertEquals(daily, result.dailyBookings());
        assertEquals(fields, result.topFields());
        verify(bookingRepository).getStatisticsOverview(from, to);
        verify(bookingRepository).getDailyStatistics(from, to);
        verify(bookingRepository).getTopFieldStatistics(from, to, PageRequest.of(0, 5));
    }
}
