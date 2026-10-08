package com.bank.auth.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter using Token Bucket algorithm (Bucket4j).
 * 
 * Applied BEFORE Spring Security filters to stop brute-force
 * attacks at the earliest possible point.
 * 
 * Two limits per IP:
 * 1. Burst limit: 10 requests per minute (handles short bursts)
 * 2. Sustained limit: 50 requests per hour (prevents slow brute-force)
 * 
 * In production, use Redis-backed Bucket4j for distributed rate limiting.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // Only rate-limit authentication endpoints
        String path = request.getServletPath();
        if (!isAuthEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        Bucket bucket = ipBuckets.computeIfAbsent(clientIp, this::createBucket);

        if (!bucket.tryConsume(1)) {
            log.warn("SECURITY: Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Create a rate limit bucket for an IP address.
     * Uses two bandwidth limits for burst and sustained protection.
     */
    private Bucket createBucket(String ip) {
        return Bucket.builder()
                // Burst: 10 requests per minute
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillGreedy(10, Duration.ofMinutes(1))
                        .build())
                // Sustained: 50 requests per hour
                .addLimit(Bandwidth.builder()
                        .capacity(50)
                        .refillGreedy(50, Duration.ofHours(1))
                        .build())
                .build();
    }

    /**
     * Check if the request path is an authentication endpoint.
     */
    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/");
    }

    /**
     * Extract the real client IP, accounting for reverse proxies.
     * 
     * WARNING: X-Forwarded-For can be spoofed by clients.
     * Only trust it if your load balancer/WAF strips and sets it.
     * In production, configure your reverse proxy (nginx, ALB, etc.)
     * to set the real IP and strip client-provided headers.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP (closest to client)
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
