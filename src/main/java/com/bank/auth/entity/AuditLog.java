package com.bank.auth.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_event_type", columnList = "eventType"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_ip", columnList = "ipAddress")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String userId;

    @Column(length = 100)
    private String username;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 1000)
    private String details;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(String id, String userId, String username, String eventType, String ipAddress, String details, LocalDateTime timestamp) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.eventType = eventType;
        this.ipAddress = ipAddress;
        this.details = details;
        this.timestamp = timestamp;
    }

    @PrePersist
    void onCreate() {
        this.timestamp = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    public static class AuditLogBuilder {
        private String id;
        private String userId;
        private String username;
        private String eventType;
        private String ipAddress;
        private String details;
        private LocalDateTime timestamp;

        AuditLogBuilder() {}

        public AuditLogBuilder id(String id) { this.id = id; return this; }
        public AuditLogBuilder userId(String userId) { this.userId = userId; return this; }
        public AuditLogBuilder username(String username) { this.username = username; return this; }
        public AuditLogBuilder eventType(String eventType) { this.eventType = eventType; return this; }
        public AuditLogBuilder ipAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }
        public AuditLogBuilder details(String details) { this.details = details; return this; }
        public AuditLogBuilder timestamp(LocalDateTime timestamp) { this.timestamp = timestamp; return this; }

        public AuditLog build() {
            return new AuditLog(id, userId, username, eventType, ipAddress, details, timestamp);
        }
    }
}
