package org.leeminkan.common.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class Resilience4jStrategy implements RateLimiterStrategy {

    // Cache limiters so we don't recreate them every request
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();

    @Override
    public boolean isAllowed(String key, long limit, long durationSeconds) {
        RateLimiter rateLimiter = limiters.computeIfAbsent(key, k -> createLimiter(k, limit, durationSeconds));
        return rateLimiter.acquirePermission(1);
    }

    private RateLimiter createLimiter(String name, long limit, long durationSeconds) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(durationSeconds))
                .limitForPeriod((int) limit)
                .timeoutDuration(Duration.ZERO) // Fail immediately if limit reached (don't wait)
                .build();

        return RateLimiterRegistry.of(config).rateLimiter(name);
    }
}