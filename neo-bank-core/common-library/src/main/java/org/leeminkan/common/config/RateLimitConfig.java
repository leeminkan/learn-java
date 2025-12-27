package org.leeminkan.common.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.ConnectionPoolSupport;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.leeminkan.common.ratelimit.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "app.rate-limit.enabled", havingValue = "true")
public class RateLimitConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    // --- FIX: Define RedisClient as a Shared Bean ---
    @Bean
    public RedisClient redisClient() {
        return RedisClient.create("redis://" + redisHost + ":" + redisPort);
    }

    // --- Option A: Bucket4j (Injects RedisClient) ---
    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.provider", havingValue = "bucket4j", matchIfMissing = true)
    public RateLimiterStrategy bucket4jRateLimiter(RedisClient redisClient) {
        StatefulRedisConnection<String, byte[]> connection = redisClient
                .connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

        ProxyManager<String> proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1))
                )
                .build();

        return new Bucket4jRateLimiter(proxyManager);
    }

    // --- Option B: Manual Lua Script (Injects RedisClient) ---
    // --- 1. Define the Connection Pool Bean ---
    // Only needed if provider is 'lua'
    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.provider", havingValue = "lua")
    public GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool(RedisClient redisClient) {

        GenericObjectPoolConfig<StatefulRedisConnection<String, String>> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(20);   // Max active connections (e.g. 20 concurrent threads)
        poolConfig.setMaxIdle(5);     // Keep 5 warm and ready
        poolConfig.setMinIdle(1);     // Always keep at least 1 open

        return ConnectionPoolSupport.createGenericObjectPool(
                () -> redisClient.connect(), // Supplier to create NEW connections
                poolConfig
        );
    }
    // --- 2. Inject Pool into Lua Strategy ---
    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.provider", havingValue = "lua")
    public RateLimiterStrategy luaRateLimiter(GenericObjectPool<StatefulRedisConnection<String, String>> pool) {
        return new LuaScriptRateLimiter(pool);
    }
    // --- Option C: Resilience4j (Local / In-Memory) ---
    @Bean
    @ConditionalOnProperty(name = "app.rate-limit.provider", havingValue = "local")
    public RateLimiterStrategy resilience4jRateLimiter() {
        return new Resilience4jStrategy();
    }

    // --- Aspect Bean ---
    @Bean
    public RateLimitAspect rateLimitAspect(RateLimiterStrategy rateLimiterStrategy) {
        return new RateLimitAspect(rateLimiterStrategy);
    }
}