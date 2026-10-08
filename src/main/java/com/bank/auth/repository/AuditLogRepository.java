package com.bank.auth.repository;

import com.bank.auth.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, String> {

    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    List<AuditLog> findByEventTypeAndTimestampAfter(String eventType, LocalDateTime after);

    /**
     * Count failed login attempts from a specific IP in the last N minutes.
     * Used for IP-based rate limiting decisions.
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.ipAddress = :ip " +
           "AND a.eventType LIKE 'LOGIN_FAILED%' AND a.timestamp > :since")
    long countFailedAttemptsFromIp(@Param("ip") String ipAddress, @Param("since") LocalDateTime since);
}
