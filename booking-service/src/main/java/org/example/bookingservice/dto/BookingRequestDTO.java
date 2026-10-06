package org.example.bookingservice.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class BookingRequestDTO {
    private Long subCourtId; // ID của field
    private Long userId;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Double totalPrice;
}