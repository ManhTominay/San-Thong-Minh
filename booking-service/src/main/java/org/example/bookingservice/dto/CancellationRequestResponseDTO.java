package org.example.bookingservice.dto;

import org.example.bookingservice.entity.CancellationRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record CancellationRequestResponseDTO(
        Long id,
        Long bookingId,
        Long requesterUserId,
        String requesterEmail,
        String reason,
        String status,
        LocalDateTime createdAt,
        LocalDateTime processedAt,
        Long processedByUserId,
        String processedByEmail,
        String adminNote,
        String bookingStatus,
        String userName,
        String userEmail,
        Long fieldId,
        String fieldName,
        Long stadiumId,
        String stadiumName,
        LocalDate bookingDate,
        LocalTime startTime,
        LocalTime endTime,
        Double totalPrice
) {
    public static CancellationRequestResponseDTO from(CancellationRequest request) {
        var booking = request.getBooking();
        return new CancellationRequestResponseDTO(
                request.getId(),
                booking.getId(),
                request.getRequesterUserId(),
                request.getRequesterEmail(),
                request.getReason(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getProcessedAt(),
                request.getProcessedByUserId(),
                request.getProcessedByEmail(),
                request.getAdminNote(),
                booking.getStatus(),
                booking.getUserName(),
                booking.getUserEmail(),
                booking.getFieldId(),
                booking.getFieldName(),
                booking.getStadiumId(),
                booking.getStadiumName(),
                booking.getBookingDate(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getTotalPrice()
        );
    }
}
