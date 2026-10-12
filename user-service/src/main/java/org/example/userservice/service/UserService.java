package org.example.userservice.service;

import org.example.userservice.dto.AuthRequest;
import org.example.userservice.entity.User;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public String register(AuthRequest request) {
        register(request, "USER");
        return "Đăng ký thành công!";
    }

    public User register(AuthRequest request, String requestedRole) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        String role = normalizeRole(requestedRole);
        User user = User.builder()
                .username(request.getEmail()) // Lấy email làm username tạm thời để không bị lỗi database
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        return userRepository.save(user);
    }

    private String normalizeRole(String role) {
        String normalizedRole = role == null ? "USER" : role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        if (!normalizedRole.equals("USER") && !normalizedRole.equals("ADMIN")) {
            throw new IllegalArgumentException("Vai trò chỉ được là USER hoặc ADMIN");
        }
        return "ROLE_" + normalizedRole;
    }

    public String login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Sai tài khoản hoặc mật khẩu!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Sai tài khoản hoặc mật khẩu!");
        }
        return jwtUtil.generateToken(user.getEmail());
    }
}