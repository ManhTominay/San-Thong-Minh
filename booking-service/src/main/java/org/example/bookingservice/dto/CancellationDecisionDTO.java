package org.example.bookingservice.dto;

import jakarta.validation.constraints.Size;

public record CancellationDecisionDTO(
        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        String note
) {
}
