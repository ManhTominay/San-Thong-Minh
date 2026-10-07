package org.example.userservice.dto;

public record AdminUserDTO(
        Long id,
        String username,
        String name,
        String email,
        String role
) {
}
