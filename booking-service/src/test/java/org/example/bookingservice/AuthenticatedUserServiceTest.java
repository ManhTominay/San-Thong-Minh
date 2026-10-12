package org.example.bookingservice;

import org.example.bookingservice.dto.AuthenticatedUserDTO;
import org.example.bookingservice.service.AuthenticatedUserService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticatedUserServiceTest {

    @Test
    void requiresAValidBearerToken() {
        AuthenticatedUserService service = new AuthenticatedUserService(new RestTemplate());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireUser(null)
        );

        assertEquals(401, exception.getStatusCode().value());
    }

    @Test
    void rejectsARegularUserFromAdminOperations() {
        AuthenticatedUserService service = serviceReturning(
                new AuthenticatedUserDTO(8L, "user@example.com", "ROLE_USER", "Người dùng"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.requireAdmin("Bearer verified-token")
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void acceptsAnAdminIdentityResolvedByUserService() {
        AuthenticatedUserService service = serviceReturning(
                new AuthenticatedUserDTO(1L, "admin@example.com", "ROLE_ADMIN", "Admin"));

        AuthenticatedUserDTO identity = service.requireAdmin("Bearer verified-token");

        assertEquals(1L, identity.id());
        assertEquals("admin@example.com", identity.email());
    }

    private AuthenticatedUserService serviceReturning(AuthenticatedUserDTO identity) {
        RestTemplate restTemplate = new RestTemplate() {
            @Override
            public <T> ResponseEntity<T> exchange(
                    String url,
                    HttpMethod method,
                    HttpEntity<?> requestEntity,
                    Class<T> responseType,
                    Object... uriVariables) {
                if (!"http://user-service/api/auth/identity".equals(url)
                        || !HttpMethod.GET.equals(method)
                        || !"Bearer verified-token".equals(
                                requestEntity.getHeaders().getFirst("Authorization"))) {
                    throw new AssertionError("Unexpected identity lookup request");
                }
                return new ResponseEntity<>(responseType.cast(identity), HttpStatus.OK);
            }
        };
        AuthenticatedUserService service = new AuthenticatedUserService(restTemplate);
        ReflectionTestUtils.setField(service, "userServiceUrl", "http://user-service");
        return service;
    }
}
