package org.example.bookingservice;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.dto.AuthenticatedUserDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.Field;
import org.example.bookingservice.entity.StadiumRecord;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.example.bookingservice.repository.StadiumRecordRepository;
import org.example.bookingservice.service.BookingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceConflictTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private StadiumRecordRepository stadiumRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(bookingRepository, fieldRepository, stadiumRepository);
    }

    private void prepareBookingDependencies(LocalDate date) {
        Field field = new Field(7L, "Sân cầu lông", "BADMINTON", 100.0, 4L);
        lenient().when(fieldRepository.findById(7L)).thenReturn(Optional.of(field));
        lenient().when(fieldRepository.findByStadiumId(4L)).thenReturn(List.of(field));
        lenient().when(stadiumRepository.findById(4L))
                .thenReturn(Optional.of(new StadiumRecord(4L, "Sân Trung Tâm", "Địa chỉ")));
        lenient().when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of());
        lenient().when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsBookingOnSameCourtAndOverlappingTime() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        prepareBookingDependencies(date);
        when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of(existingBooking(7L, date, "Sân Cầu Lông 1: 09h30 - 11h00")));

        BookingRequestDTO request = request(date, "Sân Cầu Lông 1: 10h00 - 11h30");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> bookingService.createBooking(request, user()));
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        org.junit.jupiter.api.Assertions.assertTrue(exception.getReason().contains("vừa được đặt trùng giờ"));
    }

    @Test
    void allowsBookingOnAnotherCourtAtTheSameTime() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        prepareBookingDependencies(date);
        when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of(existingBooking(7L, date, "Sân Cầu Lông 1: 09h30 - 11h00")));

        assertDoesNotThrow(() -> bookingService.createBooking(
                request(date, "Sân Cầu Lông 2: 10h00 - 11h30"), user()));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void ignoresLegacyBookingsForDifferentSports() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        prepareBookingDependencies(date);
        when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of(existingBooking(7L, date, "Pickleball 1: 09h30 - 11h00")));

        assertDoesNotThrow(() -> bookingService.createBooking(
                request(date, "Sân Cầu Lông 1: 10h00 - 11h30"), user()));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void persistsBookingForAuthenticatedUserRatherThanBrowserUserId() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        prepareBookingDependencies(date);
        BookingRequestDTO request = request(date, "Sân Cầu Lông 1: 10h00 - 11h30");
        request.setUserId(999L);

        bookingService.createBooking(request, user());

        verify(bookingRepository).save(org.mockito.ArgumentMatchers.argThat(
                booking -> booking.getUserId().equals(1L)
                        && booking.getUserName().equals("Người dùng")
                        && booking.getUserEmail().equals("user@example.com")
                        && booking.getStadiumId().equals(4L)
                        && booking.getStadiumName().equals("Sân Trung Tâm")
        ));
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

    private AuthenticatedUserDTO user() {
        return new AuthenticatedUserDTO(1L, "user@example.com", "ROLE_USER", "Người dùng");
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
