package com.bank.auth.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_rt_token_hash", columnList = "tokenHash", unique = true),
    @Index(name = "idx_rt_user", columnList = "user_id"),
    @Index(name = "idx_rt_family", columnList = "familyId")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false, length = 128)
    private String deviceFingerprint;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    private boolean revoked = false;

    @Column(nullable = false, length = 36)
    private String familyId;

    public RefreshToken() {
    }

    public RefreshToken(String id, String tokenHash, User user, Instant expiresAt, Instant createdAt,
                        String deviceFingerprint, String ipAddress, boolean revoked, String familyId) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.deviceFingerprint = deviceFingerprint;
        this.ipAddress = ipAddress;
        this.revoked = revoked;
        this.familyId = familyId;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public String getFamilyId() { return familyId; }
    public void setFamilyId(String familyId) { this.familyId = familyId; }

    public static RefreshTokenBuilder builder() {
        return new RefreshTokenBuilder();
    }

    public static class RefreshTokenBuilder {
        private String id;
        private String tokenHash;
        private User user;
        private Instant expiresAt;
        private Instant createdAt;
        private String deviceFingerprint;
        private String ipAddress;
        private boolean revoked = false;
        private String familyId;

        RefreshTokenBuilder() {}

        public RefreshTokenBuilder id(String id) { this.id = id; return this; }
        public RefreshTokenBuilder tokenHash(String tokenHash) { this.tokenHash = tokenHash; return this; }
        public RefreshTokenBuilder user(User user) { this.user = user; return this; }
        public RefreshTokenBuilder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public RefreshTokenBuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public RefreshTokenBuilder deviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; return this; }
        public RefreshTokenBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public RefreshTokenBuilder revoked(boolean revoked) { this.revoked = revoked; return this; }
        public RefreshTokenBuilder familyId(String familyId) { this.familyId = familyId; return this; }

        public RefreshToken build() {
            return new RefreshToken(id, tokenHash, user, expiresAt, createdAt, deviceFingerprint, ipAddress, revoked, familyId);
        }
    }
}
