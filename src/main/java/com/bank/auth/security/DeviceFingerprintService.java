package com.bank.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Device Fingerprint Service.
 * 
 * Generates a fingerprint based on request headers to identify devices.
 * Used to detect logins from new/unknown devices.
 * 
 * In a production banking system, this would be enhanced with:
 * - Client-side JavaScript fingerprinting (canvas, WebGL, fonts)
 * - TLS fingerprinting (JA3/JA4)
 * - Browser extension detection
 * 
 * This server-side implementation provides a baseline.
 */
@Service
public class DeviceFingerprintService {

    /**
     * Generate a device fingerprint from HTTP request headers.
     * Combines User-Agent, Accept headers, and other browser characteristics.
     */
    public String generateFingerprint(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();

        // Collect identifying headers
        sb.append(nullSafe(request.getHeader("User-Agent")));
        sb.append("|");
        sb.append(nullSafe(request.getHeader("Accept")));
        sb.append("|");
        sb.append(nullSafe(request.getHeader("Accept-Language")));
        sb.append("|");
        sb.append(nullSafe(request.getHeader("Accept-Encoding")));
        sb.append("|");
        sb.append(nullSafe(request.getHeader("Sec-CH-UA"))); // Client Hints
        sb.append("|");
        sb.append(nullSafe(request.getHeader("Sec-CH-UA-Platform")));
        sb.append("|");
        sb.append(nullSafe(request.getHeader("Sec-CH-UA-Mobile")));

        // If client sends a custom fingerprint header (from JS fingerprinting)
        String clientFingerprint = request.getHeader("X-Device-Fingerprint");
        if (clientFingerprint != null && !clientFingerprint.isBlank()) {
            sb.append("|");
            sb.append(clientFingerprint);
        }

        return hashFingerprint(sb.toString());
    }

    /**
     * Generate a fingerprint from a provided string (for when client 
     * sends their own fingerprint in the request body).
     */
    public String generateFingerprint(HttpServletRequest request, String clientFingerprint) {
        if (clientFingerprint != null && !clientFingerprint.isBlank()) {
            // Combine server and client fingerprints for stronger identification
            String serverFingerprint = generateFingerprint(request);
            return hashFingerprint(serverFingerprint + "|CLIENT:" + clientFingerprint);
        }
        return generateFingerprint(request);
    }

    /**
     * Hash the fingerprint data into a fixed-length string.
     */
    private String hashFingerprint(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
