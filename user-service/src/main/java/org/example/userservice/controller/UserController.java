package org.example.userservice.controller;

import org.example.userservice.dto.AuthRequest;
import org.example.userservice.dto.AdminUserDTO;
import org.example.userservice.dto.AuthenticatedUserDTO;
import org.example.userservice.entity.User;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.JwtUtil;
import org.example.userservice.service.UserService;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    public UserController(
            UserService userService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {

        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }


    // =========================================================
    // ĐĂNG KÝ
    // =========================================================

    @GetMapping("/auth/identity")
    public ResponseEntity<?> getAuthenticatedIdentity(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập"));
        }

        try {
            String email = jwtUtil.extractEmail(authorization.substring(7).trim());
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Tài khoản không tồn tại"));
            }
            return ResponseEntity.ok(new AuthenticatedUserDTO(
                    user.getId(), user.getEmail(), user.getRole(), user.getName()));
        } catch (JwtException | IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
        }
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(
            @RequestBody AuthRequest request
    ) {

        try {

            String message =
                    userService.register(request);

            return ResponseEntity.ok(
                    Collections.singletonMap(
                            "message",
                            message
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // ĐĂNG NHẬP
    // =========================================================

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @RequestBody AuthRequest request
    ) {

        try {

            String token =
                    userService.login(request);


            String identifier =
                    request.getEmail() != null
                            ? request.getEmail()
                            : request.getUsername();


            User user =
                    userRepository
                            .findByEmail(identifier)
                            .orElse(null);


            String role =
                    (
                            user != null
                                    &&
                                    user.getRole() != null
                    )
                            ? user.getRole()
                            : "ROLE_USER";


            Map<String, Object> response =
                    new HashMap<>();


            response.put(
                    "token",
                    token
            );

            response.put(
                    "email",
                    identifier
            );

            response.put(
                    "role",
                    role
            );


            if (user != null) {

                response.put(
                        "userId",
                        user.getId()
                );

                response.put(
                        "name",
                        user.getName()
                );
            }


            return ResponseEntity.ok(
                    response
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(401)
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // HỒ SƠ KHÁCH HÀNG
    // GET /api/users/profile?email=...
    // =========================================================

    @GetMapping("/users/profile")
    public ResponseEntity<?> getProfile(
            @RequestParam String email
    ) {

        try {

            User user =
                    userRepository
                            .findByEmail(email)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Không tìm thấy người dùng"
                                            )
                            );


            /*
             * Không trả password ra frontend.
             */
            Map<String, Object> profile =
                    new HashMap<>();


            profile.put(
                    "id",
                    user.getId()
            );

            profile.put(
                    "username",
                    user.getUsername()
            );

            profile.put(
                    "name",
                    user.getName()
            );

            profile.put(
                    "email",
                    user.getEmail()
            );

            profile.put(
                    "phone",
                    user.getPhone()
            );

            profile.put(
                    "role",
                    user.getRole()
            );


            return ResponseEntity.ok(
                    profile
            );

        } catch (Exception e) {

            return ResponseEntity
                    .status(404)
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // CẬP NHẬT HỒ SƠ
    // PUT /api/users/profile
    // =========================================================

    @PutMapping("/users/profile")
    public ResponseEntity<?> updateProfile(
            @RequestBody Map<String, String> request
    ) {

        try {

            String email =
                    request.get("email");


            if (
                    email == null
                            ||
                            email.trim().isEmpty()
            ) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Collections.singletonMap(
                                        "error",
                                        "Thiếu email người dùng"
                                )
                        );
            }


            User user =
                    userRepository
                            .findByEmail(email)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Không tìm thấy người dùng"
                                            )
                            );


            // =============================
            // HỌ TÊN
            // =============================

            if (
                    request.containsKey("name")
            ) {

                String name =
                        request.get("name");

                user.setName(
                        name != null
                                ? name.trim()
                                : null
                );
            }


            // =============================
            // SỐ ĐIỆN THOẠI
            // =============================

            if (
                    request.containsKey("phone")
            ) {

                String phone =
                        request.get("phone");

                user.setPhone(
                        phone != null
                                ? phone.trim()
                                : null
                );
            }


            User saved =
                    userRepository.save(user);


            Map<String, Object> response =
                    new HashMap<>();


            response.put(
                    "message",
                    "Cập nhật hồ sơ thành công"
            );

            response.put(
                    "name",
                    saved.getName()
            );

            response.put(
                    "phone",
                    saved.getPhone()
            );


            return ResponseEntity.ok(
                    response
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // ĐỔI MẬT KHẨU
    // PUT /api/users/change-password
    // =========================================================

    @PutMapping("/users/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> request
    ) {

        try {

            String email =
                    request.get("email");

            String oldPassword =
                    request.get("oldPassword");

            String newPassword =
                    request.get("newPassword");


            if (
                    email == null
                            ||
                            oldPassword == null
                            ||
                            newPassword == null
            ) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Collections.singletonMap(
                                        "error",
                                        "Thiếu thông tin đổi mật khẩu"
                                )
                        );
            }


            User user =
                    userRepository
                            .findByEmail(email)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Không tìm thấy người dùng"
                                            )
                            );


            // Kiểm tra mật khẩu cũ
            if (
                    !passwordEncoder.matches(
                            oldPassword,
                            user.getPassword()
                    )
            ) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Collections.singletonMap(
                                        "error",
                                        "Mật khẩu hiện tại không đúng"
                                )
                        );
            }


            // Kiểm tra mật khẩu mới
            if (
                    newPassword.length() < 6
            ) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                Collections.singletonMap(
                                        "error",
                                        "Mật khẩu mới phải có ít nhất 6 ký tự"
                                )
                        );
            }


            user.setPassword(
                    passwordEncoder.encode(
                            newPassword
                    )
            );


            userRepository.save(
                    user
            );


            return ResponseEntity.ok(
                    Collections.singletonMap(
                            "message",
                            "Đổi mật khẩu thành công"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // ADMIN - DANH SÁCH USER
    // =========================================================

    @GetMapping("/admin/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResponseEntity<?> authorizationError = verifyAdminAuthorization(authorization);
        if (authorizationError != null) {
            return authorizationError;
        }

        return ResponseEntity.ok(
                userRepository.findAll().stream()
                        .map(user -> new AdminUserDTO(
                                user.getId(),
                                user.getUsername(),
                                user.getName(),
                                user.getEmail(),
                                user.getRole()
                        ))
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/admin/users/{id}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResponseEntity<?> authorizationError = verifyAdminAuthorization(authorization);
        if (authorizationError != null) {
            return authorizationError;
        }

        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(new AdminUserDTO(
                        user.getId(),
                        user.getUsername(),
                        user.getName(),
                        user.getEmail(),
                        user.getRole()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    // =========================================================
    // ADMIN - THÊM USER
    // =========================================================

    @PostMapping("/admin/users")
    public ResponseEntity<?> createUserByAdmin(
            @RequestBody AuthRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResponseEntity<?> authorizationError = verifyAdminAuthorization(authorization);
        if (authorizationError != null) {
            return authorizationError;
        }

        try {
            User createdUser = userService.register(request, request.getRole());
            return ResponseEntity.ok(Map.of(
                    "message", "Tạo tài khoản thành công",
                    "role", createdUser.getRole()
            ));

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // ADMIN - ĐỔI ROLE
    // =========================================================

    @PutMapping("/admin/users/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResponseEntity<?> authorizationError = verifyAdminAuthorization(authorization);
        if (authorizationError != null) {
            return authorizationError;
        }

        try {

            User user =
                    userRepository
                            .findById(id)
                            .orElseThrow(
                                    () ->
                                            new RuntimeException(
                                                    "Không tìm thấy người dùng với ID: "
                                                            +
                                                            id
                                            )
                            );


            String newRole = request.get("role");
            if (newRole == null || newRole.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Collections.singletonMap("error", "Thiếu thông tin role!"));
            }

            user.setRole(normalizeRole(newRole));
            userRepository.save(user);
            return ResponseEntity.ok(Collections.singletonMap(
                    "message", "Cập nhật quyền thành công!"
            ));

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }


    // =========================================================
    // ADMIN - XÓA USER
    // =========================================================

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        ResponseEntity<?> authorizationError = verifyAdminAuthorization(authorization);
        if (authorizationError != null) {
            return authorizationError;
        }

        try {

            if (
                    !userRepository.existsById(id)
            ) {

                return ResponseEntity
                        .status(404)
                        .body(
                                Collections.singletonMap(
                                        "error",
                                        "Người dùng không tồn tại"
                                )
                        );
            }


            userRepository.deleteById(
                    id
            );


            return ResponseEntity.ok(
                    Collections.singletonMap(
                            "message",
                            "Xóa tài khoản thành công!"
                    )
            );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    e.getMessage()
                            )
                    );
        }
    }

    private ResponseEntity<?> verifyAdminAuthorization(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "Vui lòng đăng nhập bằng tài khoản Admin"));
        }

        try {
                    String email = jwtUtil.extractEmail(authorization.substring(7).trim());
                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("message", "Tài khoản không tồn tại"));
                    }
                    if (!isAdminRole(user.getRole())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("message", "Tài khoản không có quyền Admin"));
                    }
                    return null;
        } catch (JwtException | IllegalArgumentException exception) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("message", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"));
        }
    }

    private String normalizeRole(String role) {
        String normalizedRole = role.trim().toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith("ROLE_")) {
                    normalizedRole = normalizedRole.substring("ROLE_".length());
        }
        if (!normalizedRole.equals("USER") && !normalizedRole.equals("ADMIN")) {
                    throw new IllegalArgumentException("Vai trò chỉ được là USER hoặc ADMIN");
        }
        return "ROLE_" + normalizedRole;
    }

    private boolean isAdminRole(String role) {
        return role != null && "ADMIN".equals(
                        role.trim().toUpperCase(Locale.ROOT).replaceFirst("^ROLE_", "")
        );
    }
}