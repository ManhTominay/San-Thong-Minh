package org.example.bookingservice.dto;

public record AuthenticatedUserDTO(Long id, String email, String role, String name) {
    public AuthenticatedUserDTO(Long id, String email, String role) {
        this(id, email, role, null);
    }

    public boolean isAdmin() {
        if (role == null) {
            return false;
        }
        String normalizedRole = role.trim();
        return "ADMIN".equalsIgnoreCase(normalizedRole) || "ROLE_ADMIN".equalsIgnoreCase(normalizedRole);
    }
}
