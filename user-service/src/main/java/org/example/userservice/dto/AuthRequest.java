package org.example.userservice.dto;
import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String name;
    private String email;
    private String phone;
    private String password;
}