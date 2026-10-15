local count = tonumber(ARGV[1])
for i = 1, count do
    local offset = (i - 1) * 3
    redis.call('DEL', KEYS[offset + 1], KEYS[offset + 2])
    redis.call('PSETEX', KEYS[offset + 3], ARGV[2], ARGV[3])
end
return count
