package org.leeminkan.common.ratelimit;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.impl.GenericObjectPool; // <--- Import Pool
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@RequiredArgsConstructor
public class LuaScriptRateLimiter implements RateLimiterStrategy {

    // Inject the Pool instead of the Client
    private final GenericObjectPool<StatefulRedisConnection<String, String>> redisConnectionPool;
    private String luaScript;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource("scripts/rate_limit.lua");
            this.luaScript = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load rate limit script", e);
        }
    }

    @Override
    public boolean isAllowed(String key, long limit, long durationSeconds) {
        // "Borrow" a connection from the pool
        try (StatefulRedisConnection<String, String> connection = redisConnectionPool.borrowObject()) {

            RedisCommands<String, String> syncCommands = connection.sync();

            Long result = syncCommands.eval(
                    luaScript,
                    ScriptOutputType.INTEGER,
                    new String[]{key},
                    String.valueOf(limit),
                    String.valueOf(durationSeconds)
            );

            return result != null && result == 1L;

        } catch (Exception e) {
            // Fail Open strategy: If Redis fails, allow the request to prevent outage?
            // Or Fail Closed: Throw exception.
            // Here we log and rethrow to keep consistent behavior.
            log.error("Redis Rate Limit Error", e);
            throw new RuntimeException("Failed to check rate limit", e);
        }
    }
}