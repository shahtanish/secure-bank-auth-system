package com.bank.auth.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Core User entity with explicit getters, setters, and builder.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_username", columnList = "username", unique = true),
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false, length = 20)
    private String cif;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String passwordHash;

    @Column(unique = true, nullable = false, length = 150)
    private String email;

    // ===== MFA =====
    private boolean mfaEnabled = false;

    @JsonIgnore
    @Column(length = 64)
    private String mfaSecret;

    // ===== Brute Force / Account Lock =====
    private int failedLoginAttempts = 0;
    private LocalDateTime lockoutUntil;
    private LocalDateTime lastFailedLogin;

    // ===== Password Policy =====
    private LocalDateTime passwordChangedAt;
    private int passwordVersion = 1;

    // ===== Device Trust =====
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "trusted_devices", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "device_fingerprint")
    private Set<String> trustedDeviceFingerprints = new HashSet<>();

    // ===== Account Status =====
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private boolean accountNonExpired = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    public User() {
    }

    public User(String id, String cif, String username, String passwordHash, String email, boolean mfaEnabled, String mfaSecret,
                int failedLoginAttempts, LocalDateTime lockoutUntil, LocalDateTime lastFailedLogin,
                LocalDateTime passwordChangedAt, int passwordVersion, Set<String> trustedDeviceFingerprints,
                boolean enabled, boolean accountNonExpired, Role role, LocalDateTime createdAt, LocalDateTime lastLoginAt) {
        this.id = id;
        this.cif = cif;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.mfaEnabled = mfaEnabled;
        this.mfaSecret = mfaSecret;
        this.failedLoginAttempts = failedLoginAttempts;
        this.lockoutUntil = lockoutUntil;
        this.lastFailedLogin = lastFailedLogin;
        this.passwordChangedAt = passwordChangedAt;
        this.passwordVersion = passwordVersion;
        this.trustedDeviceFingerprints = trustedDeviceFingerprints != null ? trustedDeviceFingerprints : new HashSet<>();
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.role = role != null ? role : Role.USER;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.passwordChangedAt == null) {
            this.passwordChangedAt = LocalDateTime.now();
        }
    }

    public boolean isAccountLocked() {
        if (lockoutUntil == null) return false;
        if (LocalDateTime.now().isAfter(lockoutUntil)) {
            this.lockoutUntil = null;
            this.failedLoginAttempts = 0;
            return false;
        }
        return true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCif() { return cif; }
    public void setCif(String cif) { this.cif = cif; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockoutUntil() { return lockoutUntil; }
    public void setLockoutUntil(LocalDateTime lockoutUntil) { this.lockoutUntil = lockoutUntil; }

    public LocalDateTime getLastFailedLogin() { return lastFailedLogin; }
    public void setLastFailedLogin(LocalDateTime lastFailedLogin) { this.lastFailedLogin = lastFailedLogin; }

    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public int getPasswordVersion() { return passwordVersion; }
    public void setPasswordVersion(int passwordVersion) { this.passwordVersion = passwordVersion; }

    public Set<String> getTrustedDeviceFingerprints() { return trustedDeviceFingerprints; }
    public void setTrustedDeviceFingerprints(Set<String> trustedDeviceFingerprints) { this.trustedDeviceFingerprints = trustedDeviceFingerprints; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isAccountNonExpired() { return accountNonExpired; }
    public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    // Builder
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private String id;
        private String cif;
        private String username;
        private String passwordHash;
        private String email;
        private boolean mfaEnabled = false;
        private String mfaSecret;
        private int failedLoginAttempts = 0;
        private LocalDateTime lockoutUntil;
        private LocalDateTime lastFailedLogin;
        private LocalDateTime passwordChangedAt;
        private int passwordVersion = 1;
        private Set<String> trustedDeviceFingerprints = new HashSet<>();
        private boolean enabled = true;
        private boolean accountNonExpired = true;
        private Role role = Role.USER;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;

        UserBuilder() {}

        public UserBuilder id(String id) { this.id = id; return this; }
        public UserBuilder cif(String cif) { this.cif = cif; return this; }
        public UserBuilder username(String username) { this.username = username; return this; }
        public UserBuilder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder mfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; return this; }
        public UserBuilder mfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; return this; }
        public UserBuilder failedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; return this; }
        public UserBuilder lockoutUntil(LocalDateTime lockoutUntil) { this.lockoutUntil = lockoutUntil; return this; }
        public UserBuilder lastFailedLogin(LocalDateTime lastFailedLogin) { this.lastFailedLogin = lastFailedLogin; return this; }
        public UserBuilder passwordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; return this; }
        public UserBuilder passwordVersion(int passwordVersion) { this.passwordVersion = passwordVersion; return this; }
        public UserBuilder trustedDeviceFingerprints(Set<String> trustedDeviceFingerprints) { this.trustedDeviceFingerprints = trustedDeviceFingerprints; return this; }
        public UserBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public UserBuilder accountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; return this; }
        public UserBuilder role(Role role) { this.role = role; return this; }
        public UserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserBuilder lastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; return this; }

        public User build() {
            return new User(id, cif, username, passwordHash, email, mfaEnabled, mfaSecret, failedLoginAttempts,
                    lockoutUntil, lastFailedLogin, passwordChangedAt, passwordVersion, trustedDeviceFingerprints,
                    enabled, accountNonExpired, role, createdAt, lastLoginAt);
        }
    }
}
