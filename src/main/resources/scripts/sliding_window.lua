local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_seconds = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local window_start = now - window_seconds

redis.call('ZREMRANGEBYSCORE', key, '-inf', window_start)

local count = redis.call('ZCARD', key)

if count < limit then
	redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))
	redis.call('EXPIRE', key, window_seconds * 2)
	return {1, limit - count - 1, now + window_seconds}
else
	local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
	local retry_after = window_seconds - (now - tonumber(oldest[2]))
	return {0, 0, now + retry_after}
end