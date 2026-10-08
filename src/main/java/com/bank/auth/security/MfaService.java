package com.bank.auth.security;

import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * TOTP (Time-based One-Time Password) Service.
 * Implements RFC 6238 for MFA authentication.
 * 
 * Uses HMAC-SHA1 with 30-second time steps and 6-digit codes,
 * compatible with Google Authenticator, Authy, Microsoft Authenticator, etc.
 * 
 * Security features:
 * - 160-bit (20 byte) secret keys
 * - ±1 time-step tolerance (handles clock drift)
 * - Constant-time comparison (prevents timing attacks)
 */
@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);

    private static final int SECRET_SIZE = 20;          // 160 bits
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int TIME_STEP_TOLERANCE = 1;   // Allow ±1 step (30s) drift
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final String ISSUER = "BankSecureAuth";

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base32 base32 = new Base32();

    /**
     * Generate a new TOTP secret for user enrollment.
     * Returns a Base32-encoded secret string.
     */
    public String generateSecret() {
        byte[] secret = new byte[SECRET_SIZE];
        secureRandom.nextBytes(secret);
        return base32.encodeToString(secret);
    }

    /**
     * Generate the otpauth:// URI for authenticator app configuration.
     * This URI can be encoded as a QR code for easy scanning.
     * 
     * Format: otpauth://totp/ISSUER:USERNAME?secret=XXX&issuer=ISSUER&algorithm=SHA1&digits=6&period=30
     */
    public String generateOtpAuthUri(String secret, String username) {
        try {
            String encodedIssuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);

            return String.format(
                    "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                    encodedIssuer, encodedUsername, secret, encodedIssuer, CODE_DIGITS, TIME_STEP_SECONDS
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate OTP auth URI", e);
        }
    }

    /**
     * Verify a TOTP code.
     * Checks the current time step AND adjacent time steps (±tolerance)
     * to handle clock drift between server and authenticator app.
     * 
     * Uses constant-time comparison to prevent timing attacks.
     */
    public boolean verifyCode(String base32Secret, String code) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }

        // Parse the code as a number
        int codeInt;
        try {
            codeInt = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return false;
        }

        byte[] secret = base32.decode(base32Secret);
        long currentTimeStep = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

        // Check current time step and adjacent steps for clock drift tolerance
        for (int i = -TIME_STEP_TOLERANCE; i <= TIME_STEP_TOLERANCE; i++) {
            int generatedCode = generateTOTP(secret, currentTimeStep + i);
            if (constantTimeEquals(codeInt, generatedCode)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Generate a TOTP code for a given time step.
     * Implements the TOTP algorithm per RFC 6238.
     */
    private int generateTOTP(byte[] secret, long timeStep) {
        try {
            // Convert time step to 8-byte big-endian
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

            // HMAC-SHA1
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(timeBytes);

            // Dynamic truncation (RFC 4226 Section 5.4)
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            // Modulo to get N-digit code
            int mod = (int) Math.pow(10, CODE_DIGITS);
            return binary % mod;

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP generation failed", e);
        }
    }

    /**
     * Constant-time comparison to prevent timing attacks.
     * A regular == comparison can leak information about which digits match.
     */
    private boolean constantTimeEquals(int a, int b) {
        // XOR produces 0 only when both values are identical
        return (a ^ b) == 0;
    }
}
