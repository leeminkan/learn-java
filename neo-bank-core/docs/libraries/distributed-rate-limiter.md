# 🛡️ "Smart" Rate Limiter Library

* **Module:** `common-library`
* **Package:** `org.leeminkan.common.ratelimit`
* **Strategies:** Bucket4j (Redis), Custom Lua Script (Redis), Resilience4j (Local)

## 📖 Overview

The Smart Rate Limiter is a pluggable, annotation-driven library designed to protect our microservices from abuse and cascading failures.

It implements the **Strategy Pattern**, allowing developers to switch between **Distributed Rate Limiting** (strict, global consistency using Redis) and **Local Rate Limiting** (high-performance, per-instance protection) via a simple configuration flag.

## ✨ Key Features

* **Strategy Pattern:** Toggle between `Bucket4j`, `Lua Script`, or `Resilience4j` without changing code.
* **Distributed State:** Enforce strict quotas across all microservice instances (e.g., "10 requests/sec total across the cluster").
* **Low Latency:** Optimized implementations using Lettuce Redis driver or in-memory counters.
* **Context Aware:** Limits are automatically keyed by **Client IP**, preventing "noisy neighbor" problems.

## 🚀 How to Use

### 1. Add Dependency

Ensure your service depends on `common-library`:

```xml
<dependency>
    <groupId>org.leeminkan</groupId>
    <artifactId>common-library</artifactId>
    <version>${project.version}</version>
</dependency>

```

### 2. Configure the Provider

Enable the library and choose your strategy in `application.yml`:

```yaml
app:
  rate-limit:
    enabled: true
    # Options: 
    # 'bucket4j' (Default) - Industry standard distributed token bucket
    # 'lua' - Custom atomic Lua script (High control, low overhead)
    # 'local' - Resilience4j (In-memory, per-instance limits)
    provider: bucket4j 

spring:
  data:
    redis:
      host: redis
      port: 6379

```

### 3. Annotate Your Controller

Protect sensitive endpoints by adding `@RateLimit` to the method.

```java
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @PostMapping
    // Result: 5 requests allowed every 60 seconds
    @RateLimit(key = "create_tx", limit = 5, duration = 60) 
    public ResponseEntity<Transaction> create(...) {
        // ... business logic
    }
}

```

## 🧠 Strategy Guide: Which one to choose?

| Provider | Type | Backend | Pros | Cons | Best For |
| --- | --- | --- | --- | --- | --- |
| **`bucket4j`** | Distributed | Redis | Robust, standard algorithm, highly accurate. | Requires Redis network call (~2ms). | **API Quotas**, Billing limits, User Tiers. |
| **`lua`** | Distributed | Redis | **Lowest network overhead** for distributed locks. Zero dependencies. | Maintenance of custom Lua scripts. | **High-Scale/HFT** distributed systems requiring atomic exactness. |
| **`local`** | Local | JVM Memory | **Zero Latency** (Microseconds). No Redis dependency. | Limits are *per instance*. 5 instances = 5x capacity. | **DDoS Protection**, Hardware protection (Bulkheading). |

### Visual Architecture Comparison

**Distributed (Bucket4j / Lua):**
All instances talk to a single "Source of Truth" (Redis).

**Local (Resilience4j):**
Each instance maintains its own independent counter.

## 🔧 Internal Implementation Details

### The Custom Lua Script (`lua` provider)

For the "Expert" implementation, we bypass standard libraries and execute an atomic script directly on Redis. This prevents "Time-of-Check to Time-of-Use" (TOCTOU) race conditions.

**Path:** `common-library/src/main/resources/scripts/rate_limit.lua`

```lua
local current = redis.call('INCR', KEYS[1])
if tonumber(current) == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end
if tonumber(current) > tonumber(ARGV[1]) then
    return 0 -- Rejected
else
    return 1 -- Allowed
end

```

## ❓ Troubleshooting

**Q: I switched to `provider: local` and my limit increased?**

* **A:** Yes. `local` is per-instance. If you have `limit=10` and deploy 3 replicas, your total cluster capacity is now `30`. Use `bucket4j` or `lua` for strict global limits.

**Q: `LuaScriptRateLimiter` fails with "Connection Refused"?**

* **A:** Ensure your `spring.data.redis.host` points to the correct Docker service name (usually `redis`), not `localhost`.

**Q: Why does the Account Service not connect to Redis?**

* **A:** The configuration is conditional (`@ConditionalOnProperty`). If `app.rate-limit.enabled` is false (default), the Redis beans are never loaded, keeping the service lightweight.