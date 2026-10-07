package org.example.userservice.controller;

import org.example.userservice.dto.AuthRequest;
import org.example.userservice.dto.AdminUserDTO;
import org.example.userservice.entity.User;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserController(
            UserService userService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {

        this.userService = userService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    // =========================================================
    // ĐĂNG KÝ
    // =========================================================

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
    public ResponseEntity<List<AdminUserDTO>>
    getAllUsers() {

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


    // =========================================================
    // ADMIN - THÊM USER
    // =========================================================

    @PostMapping("/admin/users")
    public ResponseEntity<?> createUserByAdmin(
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
    // ADMIN - ĐỔI ROLE
    // =========================================================

    @PutMapping("/admin/users/{id}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {

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


            String newRole =
                    request.get("role");


            if (
                    newRole != null
            ) {

                user.setRole(
                        newRole.toUpperCase()
                );


                userRepository.save(
                        user
                );


                return ResponseEntity.ok(
                        Collections.singletonMap(
                                "message",
                                "Cập nhật quyền thành công!"
                        )
                );
            }


            return ResponseEntity
                    .badRequest()
                    .body(
                            Collections.singletonMap(
                                    "error",
                                    "Thiếu thông tin role!"
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
    // ADMIN - XÓA USER
    // =========================================================

    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id
    ) {

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
}