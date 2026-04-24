local key = KEYS[1]
local limit = tonumber(ARGV[1])
local window_seconds = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local window_start = now - (now % window_seconds)
local window_key = key .. ":" .. window_start

local count = redis.call('INCR', window_key)

if count == 1 then
	redis.call('EXPIRE', window_key, window_seconds * 2)
end

local reset_at = window_start + window_seconds

if count <= limit then
	return {1, limit - count, reset_at}
else
	return {0, 0, reset_at}
end