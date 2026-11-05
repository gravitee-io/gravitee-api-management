-- Writes a distributed event hash, fencing against concurrent writers and stamping the
-- entry with the Redis server clock.

-- KEYS[1] = the event hash key
-- KEYS[2] = the lease key (distributed_sync_lease:<clusterId>)
-- ARGV[1] = expected lease value (<nodeId>:<epoch>)
-- ARGV[2..] = field/value pairs for HSET, followed by Fields.updatedAt (without its value)

local lease = redis.call('GET', KEYS[2])
if lease ~= ARGV[1] then
  if lease == false then
    return redis.error_reply('DSP_LEASE_EXPIRED')
  else
    return redis.error_reply('DSP_LEASE_HELD_BY_' .. lease)
  end
end

-- Replace the last HSET pair (Fields.updatedAt / placeholder) with the server clock.
local n = #ARGV
local time = redis.call('TIME')
local ms = math.floor(time[1] * 1000 + time[2] / 1000)
ARGV[n] = tostring(ms)

return redis.call('HSET', KEYS[1], table.unpack(ARGV, 2, n))