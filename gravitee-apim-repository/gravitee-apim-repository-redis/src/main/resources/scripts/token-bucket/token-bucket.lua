-- Atomic token-bucket refill-and-consume.
-- KEYS[1] is the only key so all touched keys share one Redis Cluster hash slot (avoids CROSSSLOT);
-- everything else is passed as ARGV. Balances are whole tokens; the rate is refillRate tokens per
-- refillPeriod ms, so all refill arithmetic is integer, matching the Java TokenBucketCalculator.
local key = KEYS[1]
local requested = tonumber(ARGV[1]) -- whole tokens to consume
local refillRate = tonumber(ARGV[2]) -- whole tokens added per refill period (0 disables refill)
local refillPeriod = tonumber(ARGV[3]) -- refill period, in milliseconds
local capacity = tonumber(ARGV[4]) -- burst capacity, whole tokens
local now = tonumber(ARGV[5]) -- caller clock, epoch millis
local subscription = ARGV[6]
local expireAt = tonumber(ARGV[7]) -- PEXPIREAT target, epoch millis (real clock)

local tokens = tonumber(redis.call('HGET', key, 'tokens'))
local lastRefill = tonumber(redis.call('HGET', key, 'last_refill'))

-- A new bucket is initialised at capacity so a first-time consumer can burst immediately,
-- without waiting for tokens to accrue.
if tokens == nil then
    tokens = capacity
    lastRefill = now
end

-- Whole tokens accrued = floor(elapsed * refillRate / refillPeriod). Elapsed is clamped to 0 (out-of-order
-- timestamps never accrue negatively) and capped (a long-idle bucket never accrues past capacity) so the
-- multiplication stays exact in Lua's number type.
local elapsed = now - lastRefill
if elapsed < 0 then
    elapsed = 0
end
local newLast = lastRefill
if refillRate > 0 and refillPeriod > 0 then
    local maxUsefulElapsed = math.floor(capacity * refillPeriod / refillRate) + refillPeriod
    if elapsed > maxUsefulElapsed then
        elapsed = maxUsefulElapsed
    end
    local refill = math.floor(elapsed * refillRate / refillPeriod)
    if refill > 0 then
        tokens = tokens + refill
        if tokens > capacity then
            tokens = capacity
        end
        -- Anchor forward to now only once a whole token is credited; until then the elapsed time keeps
        -- accumulating against the unchanged anchor (no accrual lost, no re-credit). Mirrors the calculator.
        newLast = now
    end
end

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('HSET', key, 'tokens', tokens, 'last_refill', newLast, 'subscription', subscription)
redis.call('PEXPIREAT', key, expireAt)

return { allowed, tokens }
