package org.leeminkan.common.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.function.Supplier;

@RequiredArgsConstructor
public class Bucket4jRateLimiter implements RateLimiterStrategy {

    private final ProxyManager<String> proxyManager;

    @Override
    public boolean isAllowed(String key, long limit, long durationSeconds) {
        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.simple(limit, Duration.ofSeconds(durationSeconds)))
                .build();

        Bucket bucket = proxyManager.builder().build(key, configSupplier);
        return bucket.tryConsume(1);
    }
}