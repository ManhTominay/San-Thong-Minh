package org.example.userservice.dto;

public record AuthenticatedUserDTO(Long id, String email, String role, String name) {
    public AuthenticatedUserDTO(Long id, String email, String role) {
        this(id, email, role, null);
    }
}
