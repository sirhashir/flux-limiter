local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local data = redis.call('HMGET', key, 'tokens', 'last_refill')
local tokens = tonumber(data[1])
local last_refill = tonumber(data[2])

if tokens == nil then
	tokens = capacity
	last_refill = now
end

local elapsed = now - last_refill
local new_tokens = math.min(capacity, tokens + elapsed * refill_rate)

if new_tokens >= 1 then
	new_tokens = new_tokens - 1
	redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', now)
	redis.call('EXPIRE', key, capacity / refill_rate * 2)
	return {1, math.floor(new_tokens), 0}
else
	local retry_after = math.ceil((1 - new_tokens) / refill_rate)
	redis.call('HMSET', key, 'tokens', new_tokens, 'last_refill', now)
	redis.call('EXPIRE', key, capacity / refill_rate * 2)
	return {0, 0, retry_after}
end