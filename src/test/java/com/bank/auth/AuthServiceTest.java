package com.bank.auth;

import com.bank.auth.dto.LoginRequest;
import com.bank.auth.dto.LoginResponse;
import com.bank.auth.dto.RefreshTokenRequest;
import com.bank.auth.entity.Role;
import com.bank.auth.entity.User;
import com.bank.auth.exception.AccountLockedException;
import com.bank.auth.exception.InvalidRefreshTokenException;
import com.bank.auth.repository.UserRepository;
import com.bank.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.bank.auth.repository.RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private com.bank.auth.repository.AuditLogRepository auditLogRepository;

    private static final String TEST_USER = "testuser";
    private static final String TEST_PASS = "ValidPassword#2026!";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();
        authService.registerUser("CIF_TEST_01", TEST_USER, TEST_PASS, "test@worldbank.org", Role.USER);
    }

    @Test
    @DisplayName("1. Successful Login returns RSA signed JWT and Refresh Token")
    void testSuccessfulLogin() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("User-Agent", "Bank-Tester/1.0");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USER);
        loginRequest.setPassword(TEST_PASS);

        LoginResponse response = authService.login(loginRequest, request);

        assertNotNull(response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(300, response.getExpiresIn());
    }

    @Test
    @DisplayName("2. Wrong Password throws BadCredentialsException and increments failure counter")
    void testWrongPassword() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USER);
        loginRequest.setPassword("WrongPassword#123!");

        assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest, request));

        User user = userRepository.findByUsername(TEST_USER).orElseThrow();
        assertEquals(1, user.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("3. Progressive Lockout activates after consecutive failures")
    void testProgressiveLockout() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USER);
        loginRequest.setPassword("WrongPassword#123!");

        // Trigger 3 failures to hit 1-minute lockout threshold
        for (int i = 0; i < 3; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(loginRequest, request));
        }

        // 4th attempt with correct password should still be blocked due to lockout!
        loginRequest.setPassword(TEST_PASS);
        assertThrows(AccountLockedException.class, () -> authService.login(loginRequest, request));
    }

    @Test
    @DisplayName("4. Refresh Token Rotation & Theft Detection")
    void testRefreshTokenRotationAndTheftDetection() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(TEST_USER);
        loginRequest.setPassword(TEST_PASS);

        LoginResponse loginResponse = authService.login(loginRequest, request);
        String initialRefreshToken = loginResponse.getRefreshToken();

        // Rotate token once
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken, null);
        LoginResponse refreshedResponse = authService.refreshToken(refreshReq, request);

        assertNotNull(refreshedResponse.getAccessToken());
        assertNotNull(refreshedResponse.getRefreshToken());
        assertNotEquals(initialRefreshToken, refreshedResponse.getRefreshToken());

        // THEFT SCENARIO: Attempting to reuse the revoked initialRefreshToken
        assertThrows(InvalidRefreshTokenException.class, () -> authService.refreshToken(refreshReq, request));
    }
}
