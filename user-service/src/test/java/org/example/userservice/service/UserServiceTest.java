package org.example.userservice.service;

import org.example.userservice.dto.AuthRequest;
import org.example.userservice.entity.User;
import org.example.userservice.repository.UserRepository;
import org.example.userservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder, mock(JwtUtil.class));

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void adminRegistrationPersistsRequestedAdminRole() {
        User createdUser = userService.register(newUserRequest(), "ADMIN");

        assertEquals("ROLE_ADMIN", createdUser.getRole());
        verify(userRepository).save(createdUser);
    }

    @Test
    void regularRegistrationAlwaysPersistsUserRole() {
        userService.register(newUserRequest());

        verify(userRepository).save(org.mockito.ArgumentMatchers.argThat(
                user -> "ROLE_USER".equals(user.getRole())
        ));
    }

    @Test
    void invalidRequestedRoleIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(newUserRequest(), "SUPER_ADMIN")
        );
    }

    private AuthRequest newUserRequest() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@example.com");
        request.setPassword("secret");
        return request;
    }
}
