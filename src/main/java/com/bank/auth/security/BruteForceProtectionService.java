package com.bank.auth.security;

import com.bank.auth.entity.User;
import com.bank.auth.exception.AccountLockedException;
import com.bank.auth.repository.UserRepository;
import com.bank.auth.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

/**
 * Brute Force Protection Service.
 * 
 * Implements PROGRESSIVE lockout instead of a simple "3 strikes" policy:
 * - 3 failures  → 1 minute lockout
 * - 5 failures  → 5 minute lockout
 * - 7 failures  → 30 minute lockout
 * - 10 failures → 24 hour lockout
 * - 15 failures → PERMANENT lock (requires admin to unlock)
 * 
 * This makes brute-force attacks exponentially more expensive
 * while allowing legitimate users to recover from typos.
 */
@Service
public class BruteForceProtectionService {

    private static final Logger log = LoggerFactory.getLogger(BruteForceProtectionService.class);

    private static final int MAX_ATTEMPTS_BEFORE_PERMANENT_LOCK = 15;

    // Progressive lockout policy: threshold → lockout duration
    private static final TreeMap<Integer, Duration> LOCKOUT_POLICY = new TreeMap<>(Map.of(
            3,  Duration.ofMinutes(1),
            5,  Duration.ofMinutes(5),
            7,  Duration.ofMinutes(30),
            10, Duration.ofHours(24)
    ));

    private final UserRepository userRepository;
    private final AuditService auditService;

    public BruteForceProtectionService(UserRepository userRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /**
     * Record a failed login attempt and apply progressive lockout.
     * Uses REQUIRES_NEW to ensure lockout counters are committed even when login transaction fails.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String userId, String ipAddress) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        user.setLastFailedLogin(LocalDateTime.now());

        int attempts = user.getFailedLoginAttempts();

        if (attempts >= MAX_ATTEMPTS_BEFORE_PERMANENT_LOCK) {
            // ===== PERMANENT LOCK — requires admin intervention =====
            user.setEnabled(false);
            log.error("SECURITY CRITICAL: Account '{}' PERMANENTLY DISABLED after {} failed attempts. IP: {}",
                    user.getUsername(), attempts, ipAddress);
            auditService.log(user, "ACCOUNT_PERMANENTLY_LOCKED", ipAddress,
                    "Locked after " + attempts + " consecutive failed login attempts");
        } else {
            // ===== Progressive lockout =====
            Map.Entry<Integer, Duration> entry = LOCKOUT_POLICY.floorEntry(attempts);
            if (entry != null) {
                Duration lockoutDuration = entry.getValue();
                user.setLockoutUntil(LocalDateTime.now().plus(lockoutDuration));
                log.warn("SECURITY: Account '{}' locked for {} after {} failed attempts. IP: {}",
                        user.getUsername(), lockoutDuration, attempts, ipAddress);
                auditService.log(user, "ACCOUNT_TEMPORARILY_LOCKED", ipAddress,
                        "Locked for " + lockoutDuration + " after " + attempts + " failed attempts");
            }
        }

        userRepository.save(user);
    }

    /**
     * Reset failed attempts on successful login.
     */
    @Transactional
    public void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            user.setLastFailedLogin(null);
            userRepository.save(user);
        }
    }

    /**
     * Check if the account is currently locked.
     * NEVER reveal exact unlock time to potential attacker.
     */
    public void checkLockStatus(User user) {
        if (!user.isEnabled()) {
            throw new AccountLockedException(
                    "Account has been locked. Please contact your administrator.");
        }
        if (user.isAccountLocked()) {
            // SECURITY: Don't tell attacker when the lock expires
            throw new AccountLockedException(
                    "Account is temporarily locked due to too many failed attempts. Try again later.");
        }
    }
}
