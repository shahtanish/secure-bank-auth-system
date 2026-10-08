package com.bank.auth.security;

import com.bank.auth.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token Provider using RSA-512 asymmetric signing.
 * 
 * WHY RSA OVER HMAC:
 * - With HMAC (HS256), the SAME key signs AND verifies tokens
 * - If ANY microservice is compromised, the attacker can FORGE tokens
 * - With RSA, the private key signs, the public key verifies
 * - Compromised services can only verify, NEVER forge
 * 
 * ACCESS TOKEN: 5 minutes max. Short-lived = minimal damage from theft.
 */
@Service
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    /**
     * Access token expiry: 5 minutes (300 seconds).
     * This is intentionally short to minimize the damage window
     * if a token is stolen. Refresh tokens handle re-authentication.
     */
    private static final long ACCESS_TOKEN_EXPIRY_MS = 5 * 60 * 1000; // 5 minutes

    private static final String ISSUER = "bank-secure-auth";

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    public JwtTokenProvider(
            @Value("${jwt.private-key-path:}") String privateKeyPath,
            @Value("${jwt.public-key-path:}") String publicKeyPath) throws Exception {

        if (privateKeyPath != null && !privateKeyPath.isBlank()
                && Files.exists(Path.of(privateKeyPath))) {
            // Production: Load from file system / vault
            this.privateKey = loadPrivateKey(privateKeyPath);
            this.publicKey = loadPublicKey(publicKeyPath);
            log.info("JWT: Loaded RSA keys from file system");
        } else {
            // Development: Generate ephemeral keys (NEVER do this in production)
            log.warn("JWT: Generating ephemeral RSA keys — FOR DEVELOPMENT ONLY");
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(4096);
            KeyPair keyPair = keyGen.generateKeyPair();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
        }
    }

    /**
     * Generate a short-lived access token with minimal claims.
     * Contains ONLY what's needed — no sensitive data.
     */
    public String generateAccessToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())              // Unique token ID (jti) — for revocation
                .subject(user.getId())                         // User ID as subject
                .claim("cif", user.getCif())                   // CIF for core banking sync
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("pwv", user.getPasswordVersion())       // Password version — invalidate on change
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(ACCESS_TOKEN_EXPIRY_MS)))
                .issuer(ISSUER)
                .signWith(privateKey, Jwts.SIG.RS512)          // RSA-SHA512 signature
                .compact();
    }

    /**
     * Validate and parse the JWT.
     * Returns claims if valid, null if invalid/expired.
     */
    public Claims validateToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT expired for subject: {}", e.getClaims().getSubject());
            return null;
        } catch (JwtException e) {
            log.warn("SECURITY: Invalid JWT token detected: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract user ID from an expired token.
     * Used ONLY in the refresh flow to identify the user.
     */
    public String extractSubjectFromExpiredToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (ExpiredJwtException e) {
            return e.getClaims().getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * Get the access token expiry in seconds (for response).
     */
    public long getAccessTokenExpirySeconds() {
        return ACCESS_TOKEN_EXPIRY_MS / 1000;
    }

    // ===== Key Loading from PEM files =====

    private RSAPrivateKey loadPrivateKey(String path) throws Exception {
        String key = Files.readString(Path.of(path))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    private RSAPublicKey loadPublicKey(String path) throws Exception {
        String key = Files.readString(Path.of(path))
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }
}
