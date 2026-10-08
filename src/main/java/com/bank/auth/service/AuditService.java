package com.bank.auth.service;

import com.bank.auth.entity.AuditLog;
import com.bank.auth.entity.User;
import com.bank.auth.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit Service for recording all authentication events.
 * 
 * Key design decisions:
 * 1. @Async — Audit writes don't block the login response
 * 2. REQUIRES_NEW — Audit log is saved even if the main transaction rolls back
 * 3. Every event includes: who, what, when, from where
 * 
 * In production, also ship these logs to a SIEM (Splunk, Elastic, etc.)
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Log an authentication event for a known user.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User user, String eventType, String ipAddress, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .eventType(eventType)
                    .ipAddress(ipAddress)
                    .details(sanitizeDetails(details))
                    .build();
            auditLogRepository.save(auditLog);

            log.info("AUDIT: [{}] user={} ip={} details={}",
                    eventType, user.getUsername(), ipAddress, sanitizeDetails(details));
        } catch (Exception e) {
            // NEVER let audit logging failure break the main flow
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    /**
     * Log an event for an unknown/anonymous user (e.g., failed login with unknown username).
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAnonymous(String eventType, String ipAddress, String attemptedUsername) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(sanitizeDetails(attemptedUsername))
                    .eventType(eventType)
                    .ipAddress(ipAddress)
                    .details("Attempted username not found in system")
                    .build();
            auditLogRepository.save(auditLog);

            log.warn("AUDIT: [{}] attempted_user={} ip={}",
                    eventType, sanitizeDetails(attemptedUsername), ipAddress);
        } catch (Exception e) {
            log.error("Failed to save anonymous audit log: {}", e.getMessage());
        }
    }

    /**
     * Sanitize details to prevent log injection attacks.
     * Strips newlines and control characters that could forge log entries.
     */
    private String sanitizeDetails(String details) {
        if (details == null) return "";
        // Remove newlines, carriage returns, and other control characters
        return details.replaceAll("[\\r\\n\\t]", " ")
                      .replaceAll("[\\x00-\\x1F]", "");
    }
}
