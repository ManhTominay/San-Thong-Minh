package org.example.bookingservice;

import org.example.bookingservice.dto.AuthenticatedUserDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.CancellationRequest;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.CancellationRequestRepository;
import org.example.bookingservice.service.CancellationRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancellationRequestServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CancellationRequestRepository cancellationRequestRepository;

    private CancellationRequestService service;

    @BeforeEach
    void setUp() {
        service = new CancellationRequestService(bookingRepository, cancellationRequestRepository);
    }

    @Test
    void createsRequestForBookingOwnerAndKeepsBookingPending() {
        Booking booking = booking("PENDING");
        when(bookingRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking));
        when(cancellationRequestRepository.existsByBooking_IdAndStatus(42L, "PENDING")).thenReturn(false);
        when(cancellationRequestRepository.save(any(CancellationRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var created = service.create(42L, new AuthenticatedUserDTO(7L, "user@example.com", "ROLE_USER"), "Bận việc");

        assertEquals("PENDING", created.status());
        assertEquals("PENDING", booking.getStatus());
        assertEquals("Bận việc", created.reason());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsRequestForAnotherUsersBooking() {
        when(bookingRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking("CONFIRMED")));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(42L, new AuthenticatedUserDTO(8L, "other@example.com", "ROLE_USER"), null)
        );

        assertEquals(403, exception.getStatusCode().value());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicatePendingRequest() {
        when(bookingRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking("PENDING")));
        when(cancellationRequestRepository.existsByBooking_IdAndStatus(42L, "PENDING")).thenReturn(true);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.create(42L, new AuthenticatedUserDTO(7L, "user@example.com", "ROLE_USER"), null)
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(cancellationRequestRepository, never()).save(any());
    }

    @Test
    void returnsAllCancellationRequestsForRequesterNewestFirst() {
        Booking booking = booking("PENDING");
        CancellationRequest request = request(booking);
        when(cancellationRequestRepository.findByRequesterUserIdOrderByCreatedAtDescIdDesc(7L))
                .thenReturn(List.of(request));

        var history = service.getRequesterHistory(7L);

        assertEquals(1, history.size());
        assertEquals(42L, history.get(0).bookingId());
        assertEquals("PENDING", history.get(0).status());
    }

    @Test
    void approvesRequestAndMarksBookingCancelled() {
        Booking booking = booking("CONFIRMED");
        CancellationRequest request = request(booking);
        when(cancellationRequestRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(request));
        when(bookingRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(cancellationRequestRepository.save(request)).thenReturn(request);

        var result = service.decide(
                3L, new AuthenticatedUserDTO(1L, "admin@example.com", "ROLE_ADMIN"), true, "Đã xác nhận"
        );

        assertEquals("APPROVED", result.status());
        assertEquals("CANCELLED", booking.getStatus());
        assertEquals("admin@example.com", result.processedByEmail());
        verify(bookingRepository).save(booking);
    }

    @Test
    void rejectsRequestWithoutChangingBookingStatus() {
        Booking booking = booking("CONFIRMED");
        CancellationRequest request = request(booking);
        when(cancellationRequestRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(request));
        when(bookingRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(booking));
        when(cancellationRequestRepository.save(request)).thenReturn(request);

        var result = service.decide(
                3L, new AuthenticatedUserDTO(1L, "admin@example.com", "ROLE_ADMIN"), false, "Không đủ điều kiện"
        );

        assertEquals("REJECTED", result.status());
        assertEquals("CONFIRMED", booking.getStatus());
        assertEquals("Không đủ điều kiện", result.adminNote());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void refusesToProcessResolvedRequestAgain() {
        CancellationRequest request = request(booking("CONFIRMED"));
        request.setStatus("APPROVED");
        when(cancellationRequestRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(request));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.decide(
                        3L, new AuthenticatedUserDTO(1L, "admin@example.com", "ROLE_ADMIN"), true, null)
        );

        assertEquals(409, exception.getStatusCode().value());
        verify(bookingRepository, never()).findByIdForUpdate(42L);
    }

    private Booking booking(String status) {
        Booking booking = new Booking();
        booking.setId(42L);
        booking.setUserId(7L);
        booking.setStatus(status);
        return booking;
    }

    private CancellationRequest request(Booking booking) {
        CancellationRequest request = new CancellationRequest();
        request.setId(3L);
        request.setBooking(booking);
        request.setRequesterUserId(7L);
        request.setRequesterEmail("user@example.com");
        request.setStatus("PENDING");
        return request;
    }
}
