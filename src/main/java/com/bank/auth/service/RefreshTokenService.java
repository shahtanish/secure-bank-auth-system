package com.bank.auth.service;

import com.bank.auth.entity.RefreshToken;
import com.bank.auth.entity.User;
import com.bank.auth.exception.InvalidRefreshTokenException;
import com.bank.auth.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Service managing Refresh Token lifecycle, rotation, and theft detection.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private static final Duration REFRESH_TOKEN_VALIDITY = Duration.ofDays(7);
    private final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AuditService auditService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
    }

    /**
     * Create a new refresh token family for a fresh login.
     * Returns the raw token string (shown ONLY ONCE to client).
     */
    @Transactional
    public String createRefreshToken(User user, String deviceFingerprint, String ipAddress) {
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_VALIDITY))
                .createdAt(Instant.now())
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .familyId(UUID.randomUUID().toString()) // New family ID
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    /**
     * Rotate refresh token on usage.
     * Old token is marked revoked, new token in the SAME family is issued.
     * 
     * CRITICAL THEFT DETECTION:
     * If a token that is ALREADY REVOKED is presented, it indicates token theft!
     * We immediately revoke ALL tokens belonging to that token family.
     */
    @Transactional
    public String rotateRefreshToken(String rawToken, String deviceFingerprint, String ipAddress) {
        String tokenHash = hashToken(rawToken);
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("SECURITY: Refresh token hash not found in database. IP: {}", ipAddress);
                    return new InvalidRefreshTokenException("Invalid refresh token");
                });

        User user = existingToken.getUser();

        // 🚨 TOKEN THEFT DETECTED: A revoked token is being reused!
        if (existingToken.isRevoked()) {
            log.error("SECURITY CRITICAL: Revoked refresh token reuse detected! FamilyId: {}, User: {}, IP: {}",
                    existingToken.getFamilyId(), user.getUsername(), ipAddress);

            // Nuke the entire family of tokens
            refreshTokenRepository.revokeAllByFamilyId(existingToken.getFamilyId());

            auditService.log(user, "REFRESH_TOKEN_THEFT_DETECTED", ipAddress,
                    "Revoked token reused. Entire family " + existingToken.getFamilyId() + " revoked.");

            throw new InvalidRefreshTokenException("Security breach detected. Session terminated.");
        }

        // Check expiration
        if (existingToken.isExpired()) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new InvalidRefreshTokenException("Refresh token expired. Please login again.");
        }

        // Check device fingerprint matching
        if (!existingToken.getDeviceFingerprint().equals(deviceFingerprint)) {
            log.warn("SECURITY: Device fingerprint mismatch during token refresh for user: {}", user.getUsername());
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);

            auditService.log(user, "REFRESH_TOKEN_DEVICE_MISMATCH", ipAddress,
                    "Device mismatch during refresh token rotation.");

            throw new InvalidRefreshTokenException("Device mismatch during session refresh.");
        }

        // Mark current token as revoked
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        // Issue new token in the SAME family
        String newRawToken = generateSecureToken();
        String newTokenHash = hashToken(newRawToken);

        RefreshToken newToken = RefreshToken.builder()
                .tokenHash(newTokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(REFRESH_TOKEN_VALIDITY))
                .createdAt(Instant.now())
                .deviceFingerprint(deviceFingerprint)
                .ipAddress(ipAddress)
                .familyId(existingToken.getFamilyId()) // Retain family ID
                .revoked(false)
                .build();

        refreshTokenRepository.save(newToken);
        return newRawToken;
    }

    /**
     * Revoke all refresh tokens for a user (global logout).
     */
    @Transactional
    public void revokeAllUserTokens(String userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private String generateSecureToken() {
        byte[] randomBytes = new byte[64]; // 512 bits entropy
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 digest algorithm not available", e);
        }
    }
}
