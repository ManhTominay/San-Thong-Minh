package org.example.bookingservice;

import org.example.bookingservice.dto.BookingResponseDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookingServiceSnapshotTest {

    @Test
    void getAllBookingsReturnsStoredSnapshotsWithoutCallingOtherServices() {
        BookingRepository bookingRepository = mock(BookingRepository.class);
        FieldRepository fieldRepository = mock(FieldRepository.class);
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
                throw new AssertionError("Reading bookings must not call dependent services");
            }
        };
        BookingService bookingService = new BookingService(bookingRepository, fieldRepository, restTemplate);

        Booking booking = new Booking();
        booking.setId(12L);
        booking.setUserId(7L);
        booking.setUserName("Nguyen Van A");
        booking.setUserEmail("a@example.com");
        booking.setFieldId(4L);
        booking.setFieldName("Sân 4");
        booking.setStadiumId(2L);
        booking.setStadiumName("Sân Trung Tâm");
        booking.setBookingDate(LocalDate.of(2026, 10, 8));
        booking.setStartTime(LocalTime.of(9, 0));
        booking.setEndTime(LocalTime.of(10, 0));
        booking.setTotalPrice(150_000.0);
        booking.setStatus("CONFIRMED");
        when(bookingRepository.findAll()).thenReturn(List.of(booking));

        List<BookingResponseDTO> result = bookingService.getAllBookings();

        assertEquals(1, result.size());
        BookingResponseDTO response = result.get(0);
        assertEquals(12L, response.id());
        assertEquals("Nguyen Van A", response.userName());
        assertEquals("a@example.com", response.userEmail());
        assertEquals("Sân 4", response.fieldName());
        assertEquals(2L, response.stadiumId());
        assertEquals("Sân Trung Tâm", response.stadiumName());
        assertEquals(LocalDate.of(2026, 10, 8), response.bookingDate());
        assertEquals(LocalTime.of(9, 0), response.startTime());
        assertEquals(LocalTime.of(10, 0), response.endTime());
        assertEquals(150_000.0, response.totalPrice());
        assertEquals("CONFIRMED", response.status());
        verifyNoInteractions(fieldRepository);
    }
}
