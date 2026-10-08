package org.example.bookingservice;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.Field;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceConflictTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FieldRepository fieldRepository;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void rejectsBookingOnSameCourtAndOverlappingTime() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        when(fieldRepository.findByStadiumId(4L)).thenReturn(List.of(
                Field.builder().id(7L).stadiumId(4L).name("Sân cầu lông").pricePerHour(100.0).build()));
        when(fieldRepository.existsById(7L)).thenReturn(true);
        when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of(existingBooking(7L, date, "Sân Cầu Lông 1: 09h30 - 11h00")));

        BookingRequestDTO request = request(date, "Sân Cầu Lông 1: 10h00 - 11h30");

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void allowsBookingOnAnotherCourtAtTheSameTime() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        when(fieldRepository.findByStadiumId(4L)).thenReturn(List.of(
                Field.builder().id(7L).stadiumId(4L).name("Sân cầu lông").pricePerHour(100.0).build()));
        when(fieldRepository.existsById(7L)).thenReturn(true);
        when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of(existingBooking(7L, date, "Sân Cầu Lông 1: 09h30 - 11h00")));

        assertDoesNotThrow(() -> bookingService.createBooking(
                request(date, "Sân Cầu Lông 2: 10h00 - 11h30")));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void ignoresLegacyBookingsForDifferentSports() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        when(fieldRepository.findByStadiumId(4L)).thenReturn(List.of(
                Field.builder().id(7L).stadiumId(4L).name("Sân cầu lông").pricePerHour(100.0).build()));
        when(fieldRepository.existsById(7L)).thenReturn(true);
        when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of(existingBooking(7L, date, "Pickleball 1: 09h30 - 11h00")));

        assertDoesNotThrow(() -> bookingService.createBooking(
                request(date, "Sân Cầu Lông 1: 10h00 - 11h30")));
        verify(bookingRepository).save(any(Booking.class));
    }

    private BookingRequestDTO request(LocalDate date, String summary) {
        BookingRequestDTO request = new BookingRequestDTO();
        request.setUserId(1L);
        request.setFieldId(7L);
        request.setStadiumId(4L);
        request.setBookingDate(date);
        request.setStartTime(LocalTime.of(10, 0));
        request.setEndTime(LocalTime.of(11, 30));
        request.setCourtSummary(summary);
        request.setTotalPrice(100.0);
        request.setStatus("PENDING");
        return request;
    }

    private Booking existingBooking(Long fieldId, LocalDate date, String summary) {
        Booking booking = new Booking();
        booking.setFieldId(fieldId);
        booking.setStadiumId(4L);
        booking.setBookingDate(date);
        booking.setStartTime(LocalTime.of(9, 30));
        booking.setEndTime(LocalTime.of(11, 0));
        booking.setCourtSummary(summary);
        booking.setStatus("PENDING");
        return booking;
    }
}
