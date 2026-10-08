package com.bank.auth.controller;

import com.bank.auth.dto.*;
import com.bank.auth.entity.User;
import com.bank.auth.service.AuthService;
import com.bank.auth.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication REST Controller.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService, RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Login endpoint.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Token refresh endpoint.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request,
                                                 HttpServletRequest httpRequest) {
        LoginResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Initiate MFA Setup (Requires authenticated user).
     */
    @PostMapping("/mfa/setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> setupMfa(@AuthenticationPrincipal User user) {
        MfaSetupResponse setupData = authService.setupMfa(user);
        return ResponseEntity.ok(ApiResponse.success("MFA setup initiated", setupData));
    }

    /**
     * Confirm MFA setup with code.
     */
    @PostMapping("/mfa/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmMfa(@AuthenticationPrincipal User user,
                                                         @Valid @RequestBody MfaVerifyRequest request) {
        authService.confirmMfaSetup(user, request.getCode());
        return ResponseEntity.ok(ApiResponse.success("MFA activated successfully"));
    }

    /**
     * Change Password endpoint.
     */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@AuthenticationPrincipal User user,
                                                             @Valid @RequestBody ChangePasswordRequest request,
                                                             HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        authService.changePassword(user, request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully. All active sessions revoked."));
    }

    /**
     * Logout endpoint (revokes all refresh tokens for current user).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal User user) {
        refreshTokenService.revokeAllUserTokens(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }
}
