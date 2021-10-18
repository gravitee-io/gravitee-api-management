
local key = KEYS[1]
local weight = tonumber(KEYS[2])

-- Check that the key already exists
local exists = redis.call('HEXISTS', key, 'limit')

-- Increment the counter
redis.call('HINCRBY', key, 'counter', weight)

if exists == 0 then
    -- Create the rate-limit
    redis.call('HMSET', key, 'limit', tonumber(ARGV[2]), 'reset', tonumber(ARGV[3]), 'subscription', ARGV[4])
    redis.call('PEXPIREAT', key, tonumber(ARGV[3]))
end

-- Finally, returns values from Redis
return redis.call('HMGET', key, 'counter', 'limit', 'reset', 'subscription')
