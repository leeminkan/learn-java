package org.leeminkan.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket; // <--- Correct Type for v8.x
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest; // <--- Fixed Import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.function.Supplier;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitAspect {

    private final ProxyManager<String> proxyManager;

    @Around("@annotation(rateLimit)")
    public Object enforceRateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        System.out.println(">>> RATE LIMIT CHECKING: " + rateLimit.key()); // Debug Log
        String clientIp = getClientIp();

        // Construct Key: "rate_limit:method_key:ip"
        String key = "rate_limit:" + rateLimit.key() + ":" + clientIp;

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(
                        rateLimit.limit(),
                        Duration.of(rateLimit.duration(), rateLimit.unit().toChronoUnit())
                ))
                .build();

        // Fix: Use 'Bucket' interface, not DistributedBucket
        Bucket bucket = proxyManager.builder().build(key, configSupplier);

        if (bucket.tryConsume(1)) {
            return joinPoint.proceed();
        } else {
            log.warn("Rate limit exceeded for IP: {} on key: {}", clientIp, rateLimit.key());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }

    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        }
        return "unknown";
    }
}