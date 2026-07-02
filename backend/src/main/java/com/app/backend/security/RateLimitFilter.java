package com.app.backend.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /** Per-IP request budgets (capacity == greedy refill per minute). */
    private enum Limit {
        AUTH(5),
        READ(60),
        WRITE(10);

        final int perMinute;

        Limit(int perMinute) {
            this.perMinute = perMinute;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Limit limit = classify(request.getRequestURI(), request.getMethod());
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        String ip = getClientIp(request);
        Bucket bucket = buckets.computeIfAbsent(limit.name() + ":" + ip, _ -> newBucket(limit.perMinute));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":429,\"detail\":\"Too many requests\"}");
        }
    }

    private Limit classify(String uri, String method) {
        if (uri.startsWith("/api/auth/")) {
            return Limit.AUTH;
        }
        if (uri.startsWith("/api/uploads/")) {
            return Limit.WRITE;
        }
        if (uri.startsWith("/api/properties")) {
            return "GET".equals(method) ? Limit.READ : Limit.WRITE;
        }
        return null;
    }

    @Scheduled(fixedRate = 300_000)
    void evictStaleBuckets() {
        buckets.clear();
    }

    private Bucket newBucket(int perMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(perMinute).refillGreedy(perMinute, Duration.ofMinutes(1)).build())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
