package org.example.bookingservice.controller;

import org.example.bookingservice.dto.BookingRequestDTO;
import org.example.bookingservice.dto.BookingResponseDTO;
import org.example.bookingservice.dto.BookingStatisticsDTO;
import org.example.bookingservice.dto.BookingStatusDTO;
import org.example.bookingservice.dto.CourtGridDTO;
import org.example.bookingservice.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    // 1. Endpoint lấy danh sách theo Query Param (/api/bookings/my-bookings?userId=7)
    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings(@RequestParam(required = false) Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body("userId không hợp lệ");
        }
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    // 2. BỔ SUNG: Endpoint lấy danh sách theo Path Variable (/api/bookings/user/7) để khớp với userBooking.html
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getBookingsByUserIdPath(@PathVariable Long userId) {
        if (userId == null || userId <= 0) {
            return ResponseEntity.badRequest().body("userId không hợp lệ");
        }
        return ResponseEntity.ok(bookingService.getBookingsByUserId(userId));
    }

    // 3. BỔ SUNG: Endpoint lấy danh sách bookings có filter optional userId (/api/bookings?userId=7)
    @GetMapping
    public ResponseEntity<?> getAllBookings(@RequestParam(required = false) Long userId) {
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
            @RequestBody BookingStatusDTO statusRequest) {

        Set<String> allowedStatuses = Set.of("PENDING", "CONFIRMED", "CANCELLED");
        String status = statusRequest != null && statusRequest.getStatus() != null
                ? statusRequest.getStatus().trim().toUpperCase()
                : "";

        if (!allowedStatuses.contains(status)) {
            return ResponseEntity.badRequest().body("Trạng thái đơn đặt sân không hợp lệ");
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
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO bookingRequest) {
        if (bookingRequest == null) {
            return ResponseEntity.badRequest().body("Dữ liệu đơn hàng không được để trống");
        }

        BookingResponseDTO createdBooking = bookingService.createBooking(bookingRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBooking);
    }
}