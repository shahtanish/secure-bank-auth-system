package com.bank.auth.controller;

import com.bank.auth.dto.ApiResponse;
import com.bank.auth.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Demo Controller exposing protected user & admin resources.
 */
@RestController
@RequestMapping("/api")
public class UserController {

    /**
     * Protected user profile endpoint.
     */
    @GetMapping("/user/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@AuthenticationPrincipal User user) {
        Map<String, Object> profileData = Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", user.getRole().name(),
                "mfaEnabled", user.isMfaEnabled(),
                "lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "First login"
        );
        return ResponseEntity.ok(ApiResponse.success("User profile fetched successfully", profileData));
    }

    /**
     * Protected admin endpoint (requires ROLE_ADMIN).
     */
    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAdminDashboard(@AuthenticationPrincipal User user) {
        Map<String, String> adminData = Map.of(
                "status", "Secure Bank Core Dashboard Online",
                "accessedBy", user.getUsername(),
                "clearance", "LEVEL_5_FINANCIAL_OPERATIONS"
        );
        return ResponseEntity.ok(ApiResponse.success("Admin dashboard accessed", adminData));
    }
}
