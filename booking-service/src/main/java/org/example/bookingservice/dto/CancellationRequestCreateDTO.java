package org.example.bookingservice.dto;

import jakarta.validation.constraints.Size;

public record CancellationRequestCreateDTO(
        @Size(max = 1000, message = "Lý do hủy không được vượt quá 1000 ký tự")
        String reason
) {
}
