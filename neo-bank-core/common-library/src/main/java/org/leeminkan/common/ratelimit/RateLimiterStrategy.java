package org.leeminkan.common.ratelimit;

public interface RateLimiterStrategy {
    /**
     * Checks if the request is allowed.
     * @param key Unique key for the limit (e.g. "rate_limit:api:127.0.0.1")
     * @param limit Max requests allowed
     * @param durationSeconds Time window in seconds
     * @return true if allowed, false if limit exceeded
     */
    boolean isAllowed(String key, long limit, long durationSeconds);
}