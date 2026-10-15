local count = tonumber(ARGV[1])
for i = 1, count do
    local offset = (i - 1) * 2
    local accountOffset = 3 + ((i - 1) * 3)
    local account = ARGV[accountOffset]
    local joinedAt = ARGV[accountOffset + 1]
    local payload = ARGV[accountOffset + 2]
    redis.call('SET', KEYS[offset + 2], payload, 'PX', ARGV[2])
    redis.call('ZADD', KEYS[1], joinedAt, account)
    redis.call('DEL', KEYS[offset + 3])
end
return count
