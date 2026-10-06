-- Release a seat lock only if it's owned by the given booking.
-- Compare-and-delete pattern prevents releasing someone else's lock.
--
-- KEYS[1]: seat lock key
-- ARGV[1]: booking ID that should own the lock
--
-- Returns:
--   1 if the lock was released
--   0 if the lock doesn't exist or is owned by someone else

local key = KEYS[1]
local bookingId = ARGV[1]

local currentOwner = redis.call('GET', key)

if currentOwner == bookingId then
    redis.call('DEL', key)
    return 1
else
    return 0
end
