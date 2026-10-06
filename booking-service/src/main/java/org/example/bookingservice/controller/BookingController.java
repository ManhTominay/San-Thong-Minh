package org.example.bookingservice.controller;

import org.example.bookingservice.dto.*;
import org.example.bookingservice.entity.Booking;
import org.example.bookingservice.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*") // <-- THÊM DÒNG NÀY ĐỂ CHO PHÉP FRONTEND GỌI API
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @GetMapping("/grid")
    public List<CourtGridDTO> getGrid(@RequestParam Long stadiumId, @RequestParam String date) {
        return bookingService.getGridData(stadiumId, LocalDate.parse(date));
    }

    @PostMapping
    public Booking createBooking(@RequestBody BookingRequestDTO req) {
        return bookingService.createBooking(req);
    }
}