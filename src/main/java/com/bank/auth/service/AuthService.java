package com.bank.auth.service;

import com.bank.auth.dto.*;
import com.bank.auth.entity.Role;
import com.bank.auth.entity.User;
import com.bank.auth.exception.MfaRequiredException;
import com.bank.auth.exception.PasswordPolicyException;
import com.bank.auth.repository.RefreshTokenRepository;
import com.bank.auth.repository.UserRepository;
import com.bank.auth.security.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Core Authentication Service.
 * Orchestrates security layers: validation, brute-force checks, password verification,
 * MFA verification, device fingerprinting, and token generation.
 */
@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final BruteForceProtectionService bruteForceService;
    private final MfaService mfaService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final AuditService auditService;
    private final PasswordPolicyValidator passwordPolicyValidator;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       RefreshTokenService refreshTokenService,
                       BruteForceProtectionService bruteForceService,
                       MfaService mfaService,
                       DeviceFingerprintService deviceFingerprintService,
                       AuditService auditService,
                       PasswordPolicyValidator passwordPolicyValidator) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.bruteForceService = bruteForceService;
        this.mfaService = mfaService;
        this.deviceFingerprintService = deviceFingerprintService;
        this.auditService = auditService;
        this.passwordPolicyValidator = passwordPolicyValidator;
    }

    /**
     * Primary Login method.
     */
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        // 1. Sanitize & Validate input strings
        String username = request.getUsername() != null ? request.getUsername().trim().toLowerCase() : "";

        // 2. Constant-time user retrieval attempt
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            // Waste constant time to prevent timing attacks / username enumeration
            passwordEncoder.encode("dummy_password_for_timing_safety_123!");
            auditService.logAnonymous("LOGIN_FAILED_USER_NOT_FOUND", clientIp, username);
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userOpt.get();

        // 3. Brute Force Lock Check
        bruteForceService.checkLockStatus(user);

        // 4. Password Verification
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            bruteForceService.recordFailedAttempt(user.getId(), clientIp);
            auditService.log(user, "LOGIN_FAILED_BAD_PASSWORD", clientIp, userAgent);
            throw new BadCredentialsException("Invalid credentials");
        }

        // 5. Password Age Policy Check (e.g., 90-day expiry)
        if (user.getPasswordChangedAt() != null &&
                user.getPasswordChangedAt().isBefore(LocalDateTime.now().minusDays(90))) {
            auditService.log(user, "LOGIN_FAILED_PASSWORD_EXPIRED", clientIp, userAgent);
            throw new CredentialsExpiredException("Password expired. Please reset your password.");
        }

        // 6. Multi-Factor Authentication Check
        if (user.isMfaEnabled()) {
            if (request.getMfaCode() == null || request.getMfaCode().isBlank()) {
                auditService.log(user, "MFA_REQUIRED_PROMPT", clientIp, userAgent);
                throw new MfaRequiredException("MFA code required");
            }
            if (!mfaService.verifyCode(user.getMfaSecret(), request.getMfaCode())) {
                bruteForceService.recordFailedAttempt(user.getId(), clientIp);
                auditService.log(user, "LOGIN_FAILED_BAD_MFA", clientIp, userAgent);
                throw new BadCredentialsException("Invalid credentials");
            }
        }

        // 7. Device Fingerprint Tracking
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest, request.getDeviceFingerprint());
        boolean isKnownDevice = user.getTrustedDeviceFingerprints().contains(deviceFingerprint);

        if (!isKnownDevice) {
            log.info("New device detected for user '{}'. Fingerprint: {}", user.getUsername(), deviceFingerprint);
            user.getTrustedDeviceFingerprints().add(deviceFingerprint);
            auditService.log(user, "NEW_DEVICE_DETECTED", clientIp, "UA: " + userAgent);
        }

        // 8. Reset Lockout Counter & Update Last Login Time
        bruteForceService.resetFailedAttempts(user);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 9. Issue RSA-512 Signed Access Token & Rotatable Refresh Token
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user, deviceFingerprint, clientIp);

        auditService.log(user, "LOGIN_SUCCESS", clientIp, "Device: " + (isKnownDevice ? "known" : "new"));

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirySeconds())
                .mfaEnabled(user.isMfaEnabled())
                .mfaRequired(false)
                .message("Authentication successful")
                .build();
    }

    /**
     * Refresh Token Endpoint logic.
     */
    public LoginResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String clientIp = extractClientIp(httpRequest);
        String deviceFingerprint = deviceFingerprintService.generateFingerprint(httpRequest, request.getDeviceFingerprint());

        // 1. Look up refresh token by hash to identify user BEFORE rotation
        String tokenHash = hashToken(request.getRefreshToken());
        com.bank.auth.entity.RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new com.bank.auth.exception.InvalidRefreshTokenException("Invalid refresh token"));

        User user = existingToken.getUser();

        // 2. Rotate token (detects theft if token was revoked)
        String newRefreshToken = refreshTokenService.rotateRefreshToken(
                request.getRefreshToken(), deviceFingerprint, clientIp);

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account disabled");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        auditService.log(user, "TOKEN_REFRESH_SUCCESS", clientIp, "Refreshed access token");

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirySeconds())
                .mfaEnabled(user.isMfaEnabled())
                .message("Token refreshed successfully")
                .build();
    }

    /**
     * Setup TOTP MFA for an authenticated user.
     */
    public MfaSetupResponse setupMfa(User user) {
        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String otpAuthUri = mfaService.generateOtpAuthUri(secret, user.getUsername());
        auditService.log(user, "MFA_SETUP_INITIATED", "INTERNAL", "Generated new TOTP secret");

        return MfaSetupResponse.builder()
                .secret(secret)
                .otpAuthUri(otpAuthUri)
                .message("Scan the OTP URI or enter the secret key into your authenticator app, then verify with a code.")
                .build();
    }

    /**
     * Confirm TOTP MFA setup with initial code.
     */
    public void confirmMfaSetup(User user, String code) {
        if (user.getMfaSecret() == null) {
            throw new IllegalArgumentException("MFA setup has not been initiated");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), code)) {
            throw new BadCredentialsException("Invalid MFA code");
        }
        user.setMfaEnabled(true);
        userRepository.save(user);
        auditService.log(user, "MFA_ENABLED", "INTERNAL", "MFA setup confirmed and activated");
    }

    /**
     * Password Change logic.
     */
    public void changePassword(User user, ChangePasswordRequest request, String clientIp) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            auditService.log(user, "PASSWORD_CHANGE_FAILED", clientIp, "Current password incorrect");
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        // Validate strength
        passwordPolicyValidator.validate(request.getNewPassword());

        // Update password and increment version (invalidates all active JWT access tokens)
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setPasswordVersion(user.getPasswordVersion() + 1);
        userRepository.save(user);

        // Revoke all existing refresh tokens
        refreshTokenService.revokeAllUserTokens(user.getId());

        auditService.log(user, "PASSWORD_CHANGED_SUCCESS", clientIp, "Password updated. All sessions revoked.");
    }

    /**
     * Create user account (Registration / Admin creation).
     */
    public User registerUser(String cif, String username, String rawPassword, String email, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        passwordPolicyValidator.validate(rawPassword);

        User user = User.builder()
                .cif(cif)
                .username(username.toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(rawPassword))
                .email(email.toLowerCase().trim())
                .role(role != null ? role : Role.USER)
                .enabled(true)
                .accountNonExpired(true)
                .failedLoginAttempts(0)
                .passwordVersion(1)
                .passwordChangedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }

    private String hashToken(String rawToken) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
