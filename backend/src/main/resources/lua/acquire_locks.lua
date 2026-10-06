-- Atomically acquire locks on multiple seats for a booking.
-- All-or-nothing: if any seat is already locked, no locks are acquired.
--
-- KEYS: seat lock keys in format "seat-lock:{showId}:{seatId}"
-- ARGV[1]: booking ID (UUID as string)
-- ARGV[2]: TTL in seconds (e.g., 300 for 5 minutes)
--
-- Returns:
--   "OK" if all locks acquired successfully
--   "LOCKED:{key}" if any seat is already locked (no locks acquired)

local bookingId = ARGV[1]
local ttl = tonumber(ARGV[2])

-- Phase 1: Check if any seat is already locked
for i, key in ipairs(KEYS) do
    local existing = redis.call('GET', key)
    if existing then
        return "LOCKED:" .. key
    end
end

-- Phase 2: All seats are free, acquire all locks atomically
for i, key in ipairs(KEYS) do
    redis.call('SET', key, bookingId, 'EX', ttl)
end

return "OK"
