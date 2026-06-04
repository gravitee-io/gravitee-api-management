
local key = KEYS[1]
-- weight is passed as an ARGV (not a KEY) so that KEYS[] contains only the rate-limit
-- key. On Redis Cluster all keys in a command must hash to the same slot; keeping a
-- single key avoids CROSSSLOT errors.
local weight = tonumber(ARGV[1])

-- Check that the key already exists
local exists = redis.call('HEXISTS', key, 'limit')

-- Increment the counter
redis.call('HINCRBY', key, 'counter', weight)

if exists == 0 then
    -- Create the rate-limit
    redis.call('HMSET', key, 'limit', tonumber(ARGV[3]), 'reset', tonumber(ARGV[4]), 'subscription', ARGV[5])
    redis.call('PEXPIREAT', key, tonumber(ARGV[4]))
end

-- Finally, returns values from Redis
return redis.call('HMGET', key, 'counter', 'limit', 'reset', 'subscription')
