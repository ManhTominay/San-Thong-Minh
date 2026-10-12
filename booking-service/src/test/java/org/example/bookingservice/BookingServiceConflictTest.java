package org.example.bookingservice;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.dto.StadiumSnapshotDTO;
import org.example.bookingservice.dto.UserSnapshotDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.Field;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.FieldRepository;
import org.example.bookingservice.service.BookingService;
import org.example.bookingservice.dto.StadiumSnapshotDTO;
import org.example.bookingservice.dto.UserSnapshotDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

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

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            public <T> T getForObject(String url, Class<T> responseType, Object... uriVariables) {
                if (responseType == UserSnapshotDTO.class) {
                    return responseType.cast(
                            new UserSnapshotDTO(1L, "user", "Người dùng", "user@example.com", "USER"));
                }
                if (responseType == StadiumSnapshotDTO.class) {
                    return responseType.cast(new StadiumSnapshotDTO(4L, "Sân Trung Tâm"));
                }
                throw new AssertionError("Unexpected response type: " + responseType.getName());
            }
        };
        bookingService = new BookingService(bookingRepository, fieldRepository, restTemplate);
    }

    private void prepareBookingDependencies(LocalDate date) {
        Field field = new Field(7L, "Sân cầu lông", "BADMINTON", 100.0, 4L);
        lenient().when(fieldRepository.findById(7L)).thenReturn(Optional.of(field));
        lenient().when(fieldRepository.findByStadiumId(4L)).thenReturn(List.of(field));
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

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(request));
    }

    @Test
    void allowsBookingOnAnotherCourtAtTheSameTime() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        prepareBookingDependencies(date);
        when(bookingRepository.findByStadiumIdAndBookingDate(4L, date, "CANCELLED"))
                .thenReturn(List.of(existingBooking(7L, date, "Sân Cầu Lông 1: 09h30 - 11h00")));

        assertDoesNotThrow(() -> bookingService.createBooking(
                request(date, "Sân Cầu Lông 2: 10h00 - 11h30")));
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void ignoresLegacyBookingsForDifferentSports() {
        LocalDate date = LocalDate.of(2026, 10, 8);
        prepareBookingDependencies(date);
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
