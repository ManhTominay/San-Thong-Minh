package org.example.bookingservice.controller;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.dto.BookingResponseDTO;
import org.example.bookingservice.dto.BookingStatisticsDTO;
import org.example.bookingservice.dto.BookingStatusDTO;
import org.example.bookingservice.dto.AuthenticatedUserDTO;
import org.example.bookingservice.dto.CancellationDecisionDTO;
import org.example.bookingservice.dto.CancellationRequestCreateDTO;
import org.example.bookingservice.dto.CourtGridDTO;
import org.example.bookingservice.service.AuthenticatedUserService;
import org.example.bookingservice.service.CancellationRequestService;
import org.example.bookingservice.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET,
        RequestMethod.POST,
        RequestMethod.PUT,
        RequestMethod.PATCH,
        RequestMethod.DELETE
})
public class BookingController {

    private final BookingService bookingService;
    private final CancellationRequestService cancellationRequestService;
    private final AuthenticatedUserService authenticatedUserService;

    public BookingController(
            BookingService bookingService,
            CancellationRequestService cancellationRequestService,
            AuthenticatedUserService authenticatedUserService) {
        this.bookingService = bookingService;
        this.cancellationRequestService = cancellationRequestService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBooking(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedUserDTO user = authenticatedUserService.requireUser(authorization);
        Optional<BookingResponseDTO> booking = bookingService.getBookingById(bookingId);
        if (booking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!user.isAdmin() && !booking.get().userId().equals(user.id())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền xem đơn đặt sân này");
        }
        return ResponseEntity.ok(booking.get());
    }

    @PostMapping("/{bookingId}/cancellation-requests")
    public ResponseEntity<?> createCancellationRequest(
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) CancellationRequestCreateDTO request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedUserDTO user = authenticatedUserService.requireUser(authorization);
        var saved = cancellationRequestService.create(
                bookingId,
                user,
                request == null ? null : request.reason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{bookingId}/cancellation-requests")
    public ResponseEntity<?> getCancellationHistory(
            @PathVariable Long bookingId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedUserDTO user = authenticatedUserService.requireUser(authorization);
        return ResponseEntity.ok(cancellationRequestService.getBookingHistory(bookingId, user));
    }

    @GetMapping("/admin/cancellation-requests")
    public ResponseEntity<?> getCancellationRequests(
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticatedUserService.requireAdmin(authorization);
        if (status != null && !Set.of("PENDING", "APPROVED", "REJECTED")
                .contains(status.trim().toUpperCase())) {
            return ResponseEntity.badRequest().body("Trạng thái yêu cầu hủy không hợp lệ");
        }
        return ResponseEntity.ok(cancellationRequestService.getAll(status));
    }

    @GetMapping("/admin/cancellation-requests/{requestId}")
    public ResponseEntity<?> getCancellationRequest(
            @PathVariable Long requestId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticatedUserService.requireAdmin(authorization);
        return ResponseEntity.ok(cancellationRequestService.getById(requestId));
    }

    @PatchMapping("/admin/cancellation-requests/{requestId}/approve")
    public ResponseEntity<?> approveCancellationRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody(required = false) CancellationDecisionDTO decision,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedUserDTO admin = authenticatedUserService.requireAdmin(authorization);
        return ResponseEntity.ok(cancellationRequestService.decide(
                requestId, admin, true, decision == null ? null : decision.note()));
    }

    @PatchMapping("/admin/cancellation-requests/{requestId}/reject")
    public ResponseEntity<?> rejectCancellationRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody(required = false) CancellationDecisionDTO decision,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedUserDTO admin = authenticatedUserService.requireAdmin(authorization);
        return ResponseEntity.ok(cancellationRequestService.decide(
                requestId, admin, false, decision == null ? null : decision.note()));
    }

    // Resolve the account from the verified token instead of trusting a browser-supplied userId.
    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings(
            @RequestParam(required = false) Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedUserDTO user = authenticatedUserService.requireUser(authorization);
        if (userId != null && !user.isAdmin() && !user.id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền xem lịch sử đặt sân của tài khoản khác");
        }
        if (user.isAdmin() && userId == null) {
            return ResponseEntity.ok(bookingService.getAllBookings());
        }
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId == null ? user.id() : userId));
    }

    // 2. BỔ SUNG: Endpoint lấy danh sách theo Path Variable (/api/bookings/user/7) để khớp với userBooking.html
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUserIdPath(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body("userId không hợp lệ");
        }
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    @GetMapping("/user/{userId}/cancellation-requests")
    public ResponseEntity<?> getUserCancellationRequests(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedUserDTO user = authenticatedUserService.requireUser(authorization);
        if (!user.isAdmin() && !user.id().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Bạn không có quyền xem yêu cầu hủy của tài khoản này");
        }
        return ResponseEntity.ok(cancellationRequestService.getRequesterHistory(userId));
    }

    // Lịch theo ngày dùng dữ liệu của mọi người đặt; userId chỉ lọc lịch cá nhân khi không có ngày.
    @GetMapping
    public ResponseEntity<?> getAllBookings(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookingDate,
            @RequestParam(required = false) Long fieldId,
            @RequestParam(required = false) Long stadiumId) {
        if (bookingDate != null) {
            if ((fieldId != null && fieldId <= 0) || (stadiumId != null && stadiumId <= 0)) {
                return ResponseEntity.badRequest().body("fieldId không hợp lệ");
            }
            if (fieldId != null && stadiumId != null) {
                return ResponseEntity.badRequest().body("Chỉ truyền một trong hai tham số fieldId hoặc stadiumId");
            }
            return ResponseEntity.ok(bookingService.getBookingsByDate(bookingDate, fieldId, stadiumId));
        }
        if (userId != null && userId > 0) {
            return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
        }
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/statistics")
    public ResponseEntity<BookingStatisticsDTO> getStatistics(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return ResponseEntity.ok(bookingService.getStatistics(from, to));
    }

    @PutMapping("/{bookingId}/status")
    public ResponseEntity<?> updateBookingStatus(
            @PathVariable Long bookingId,
            @RequestBody BookingStatusDTO statusRequest,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        authenticatedUserService.requireAdmin(authorization);
        Set<String> allowedStatuses = Set.of("PENDING", "CONFIRMED");
        String status = statusRequest != null && statusRequest.getStatus() != null
                ? statusRequest.getStatus().trim().toUpperCase()
                : "";

        if (!allowedStatuses.contains(status)) {
            return ResponseEntity.badRequest().body(
                    "Trạng thái đơn đặt sân không hợp lệ. Hủy đơn cần được duyệt qua yêu cầu hủy.");
        }

        Optional<BookingResponseDTO> updatedBooking = bookingService.updateBookingStatus(bookingId, status);
        if (updatedBooking.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updatedBooking.get());
    }

    // 4. Endpoint Lấy dữ liệu Ma trận Sân (/api/bookings/grid)
    @GetMapping("/grid")
    public ResponseEntity<?> getGrid(
            @RequestParam(required = false, defaultValue = "1") Long stadiumId,
            @RequestParam(required = false) String date) {

        try {
            LocalDate bookingDate = (date != null && !date.isEmpty())
                    ? LocalDate.parse(date)
                    : LocalDate.now();

            List<CourtGridDTO> gridData = bookingService.getGridData(stadiumId, bookingDate);
            return ResponseEntity.ok(gridData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi lấy dữ liệu grid: " + e.getMessage());
        }
    }

    // 5. Endpoint Tạo Đơn Đặt Sân (/api/bookings)
    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestBody BookingRequestDTO bookingRequest,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (bookingRequest == null) {
            return ResponseEntity.badRequest().body("Dữ liệu đơn hàng không được để trống");
        }

        AuthenticatedUserDTO user = authenticatedUserService.requireUser(authorization);
        try {
            BookingResponseDTO createdBooking = bookingService.createBooking(bookingRequest, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
        } catch (ResponseStatusException exception) {
            String message = exception.getReason() != null
                    ? exception.getReason()
                    : "Không thể tạo lịch đặt sân";
            return ResponseEntity.status(exception.getStatusCode())
                    .body(Map.of("message", message));
        } catch (IllegalArgumentException exception) {
            String message = exception.getMessage() != null
                    ? exception.getMessage()
                    : "Thông tin đặt sân không hợp lệ";
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
    }
}