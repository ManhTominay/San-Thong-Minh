package org.example.userservice.controller;

import org.example.userservice.dto.AuthRequest;
import org.example.userservice.entity.User;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        try {
            String message = userService.register(request);
            return ResponseEntity.ok(Collections.singletonMap("message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        try {
            String token = userService.login(request);
            String identifier = request.getEmail() != null ? request.getEmail() : request.getUsername();
            User user = userRepository.findByEmail(identifier).orElse(null);
            String role = (user != null && user.getRole() != null) ? user.getRole() : "USER";

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("email", identifier);
            response.put("role", role);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    // --- TÍCH HỢP QUẢN LÝ ADMIN TRỰC TIẾP TẠI ĐÂY ---

    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    // API xử lý nút "Thêm tài khoản mới" trên trang admin
    @PostMapping("/admin/users")
    public ResponseEntity<?> createUserByAdmin(@RequestBody AuthRequest request) {
        try {
            String message = userService.register(request);
            return ResponseEntity.ok(Collections.singletonMap("message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PutMapping("/admin/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

            String newRole = request.get("role");
            if (newRole != null) {
                user.setRole(newRole.toUpperCase());
                userRepository.save(user);
                return ResponseEntity.ok(Collections.singletonMap("message", "Cập nhật quyền thành công!"));
            }
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Thiếu thông tin role!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            if (!userRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Collections.singletonMap("error", "Người dùng không tồn tại"));
            }
            userRepository.deleteById(id);
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa tài khoản thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", e.getMessage()));
        }
    }
}