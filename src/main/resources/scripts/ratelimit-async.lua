local matches = redis.call('KEYS', 'ratelimit:*:async')
local ratelimits = {}
local after = tonumber(ARGV[1])

for _,key in ipairs(matches) do
    local ratelimit = redis.call('LRANGE', key, 0, 1)
    local updatedAt = tonumber(ratelimit[1])
    local rateLimitKey = ratelimit[2]
    if (rateLimitKey ~= nil and updatedAt >= after) then
        ratelimits[#ratelimits+1] = redis.call('LRANGE', rateLimitKey, 0, 5)
        table.insert(ratelimits[#ratelimits], 1, string.sub(rateLimitKey, 11))
    end
end

return ratelimits