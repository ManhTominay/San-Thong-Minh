package org.example.bookingservice.service;

import org.example.bookingservice.dto.AuthenticatedUserDTO;
import org.example.bookingservice.dto.CancellationRequestResponseDTO;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.entity.CancellationRequest;
import org.example.bookingservice.repository.BookingRepository;
import org.example.bookingservice.repository.CancellationRequestRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CancellationRequestService {

    private final BookingRepository bookingRepository;
    private final CancellationRequestRepository cancellationRequestRepository;

    public CancellationRequestService(
            BookingRepository bookingRepository,
            CancellationRequestRepository cancellationRequestRepository) {
        this.bookingRepository = bookingRepository;
        this.cancellationRequestRepository = cancellationRequestRepository;
    }

    @Transactional
    public CancellationRequestResponseDTO create(
            Long bookingId,
            AuthenticatedUserDTO requester,
            String reason) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt sân"));
        if (!booking.getUserId().equals(requester.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền yêu cầu hủy đơn này");
        }
        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đặt sân này đã được hủy");
        }
        if (!List.of("PENDING", "CONFIRMED", "APPROVED", "SUCCESS")
                .contains(booking.getStatus() == null ? "" : booking.getStatus().toUpperCase())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Trạng thái đơn hiện tại không cho phép gửi yêu cầu hủy"
            );
        }
        if (cancellationRequestRepository.existsByBooking_IdAndStatus(bookingId, "PENDING")) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Đơn này đã có yêu cầu hủy đang chờ Admin xét duyệt"
            );
        }

        CancellationRequest request = new CancellationRequest();
        request.setBooking(booking);
        request.setRequesterUserId(requester.id());
        request.setRequesterEmail(requester.email());
        request.setReason(normalizeNote(reason));
        request.setStatus("PENDING");
        return CancellationRequestResponseDTO.from(cancellationRequestRepository.save(request));
    }

    @Transactional(readOnly = true)
    public List<CancellationRequestResponseDTO> getBookingHistory(
            Long bookingId,
            AuthenticatedUserDTO requester) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt sân"));
        if (!requester.isAdmin() && !booking.getUserId().equals(requester.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem yêu cầu hủy của đơn này");
        }
        return cancellationRequestRepository.findByBooking_IdOrderByCreatedAtDescIdDesc(bookingId)
                .stream()
                .map(CancellationRequestResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CancellationRequestResponseDTO> getRequesterHistory(Long requesterUserId) {
        return cancellationRequestRepository.findByRequesterUserIdOrderByCreatedAtDescIdDesc(requesterUserId)
                .stream()
                .map(CancellationRequestResponseDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CancellationRequestResponseDTO> getAll(String status) {
        List<CancellationRequest> requests = status == null || status.isBlank()
                ? cancellationRequestRepository.findAllByOrderByCreatedAtDescIdDesc()
                : cancellationRequestRepository.findByStatusOrderByCreatedAtDescIdDesc(status.trim().toUpperCase());
        return requests.stream().map(CancellationRequestResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public CancellationRequestResponseDTO getById(Long requestId) {
        CancellationRequest request = cancellationRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hủy"));
        return CancellationRequestResponseDTO.from(request);
    }

    @Transactional
    public CancellationRequestResponseDTO decide(
            Long requestId,
            AuthenticatedUserDTO admin,
            boolean approve,
            String note) {
        CancellationRequest request = cancellationRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu hủy"));
        if (!"PENDING".equals(request.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Yêu cầu này đã được xử lý trước đó"
            );
        }

        Booking booking = bookingRepository.findByIdForUpdate(request.getBooking().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy đơn đặt sân"));
        if (approve) {
            if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Đơn đặt sân đã được hủy");
            }
            booking.setStatus("CANCELLED");
            bookingRepository.save(booking);
            request.setStatus("APPROVED");
        } else {
            request.setStatus("REJECTED");
        }

        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByUserId(admin.id());
        request.setProcessedByEmail(admin.email());
        request.setAdminNote(normalizeNote(note));
        return CancellationRequestResponseDTO.from(cancellationRequestRepository.save(request));
    }

    private String normalizeNote(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
