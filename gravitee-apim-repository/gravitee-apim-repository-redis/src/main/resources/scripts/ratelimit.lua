
local key = KEYS[1]

local weight = tonumber(ARGV[1])

-- Check that the key already exists
local exists = redis.call('HEXISTS', key, 'counter')

-- Increment the counter
redis.call('HINCRBY', key, 'counter', weight)

if exists == 0 then
    -- Create the rate-limit
    local resetAt = tonumber(ARGV[2]);

    redis.call('HSET', key, 'reset', resetAt)
    redis.call('PEXPIREAT', key, resetAt)
end

-- Finally, returns values from Redis
return redis.call('HMGET', key, 'counter', 'reset')
