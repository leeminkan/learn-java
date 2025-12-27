-- KEYS[1]: The rate limit key (e.g., "rate_limit:tx_create:192.168.1.1")
-- ARGV[1]: The limit (e.g., 5)
-- ARGV[2]: The window in seconds (e.g., 60)

local current = redis.call('INCR', KEYS[1])

if tonumber(current) == 1 then
    -- If this is the first request, set the expiry (Window)
    redis.call('EXPIRE', KEYS[1], ARGV[2])
end

if tonumber(current) > tonumber(ARGV[1]) then
    -- Limit exceeded
    return 0
else
    -- Allowed
    return 1
end