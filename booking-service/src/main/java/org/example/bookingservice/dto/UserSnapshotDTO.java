package org.example.bookingservice.dto;

public record UserSnapshotDTO(
        Long id,
        String username,
        String name,
        String email,
        String role
) {
}
