package org.example.bookingservice.service;

import org.example.bookingservice.dto.AuthenticatedUserDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticatedUserService {

    private final RestTemplate restTemplate;

    @Value("${booking.integrations.user-service-url:http://localhost:8083}")
    private String userServiceUrl;

    public AuthenticatedUserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public AuthenticatedUserDTO requireUser(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        try {
            AuthenticatedUserDTO user = restTemplate.exchange(
                    userServiceUrl + "/api/auth/identity",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    AuthenticatedUserDTO.class
            ).getBody();
            if (user == null || user.id() == null || user.email() == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "User Service trả về thông tin tài khoản không hợp lệ"
                );
            }
            return user;
        } catch (HttpClientErrorException.Unauthorized exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Phiên đăng nhập không hợp lệ hoặc đã hết hạn"
            );
        } catch (HttpClientErrorException.Forbidden exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Không thể xác thực tài khoản vì User Service hiện không khả dụng"
            );
        }
    }

    public AuthenticatedUserDTO requireAdmin(String authorization) {
        AuthenticatedUserDTO user = requireUser(authorization);
        if (!user.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Admin mới được xử lý yêu cầu hủy");
        }
        return user;
    }
}
