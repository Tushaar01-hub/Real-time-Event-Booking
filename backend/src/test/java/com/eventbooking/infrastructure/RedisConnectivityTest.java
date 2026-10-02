package com.eventbooking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventbooking.support.AbstractIntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Smoke test for the primitive the seat-lock design relies on: SET NX with a TTL. */
class RedisConnectivityTest extends AbstractIntegrationTest {

    @Autowired
    StringRedisTemplate redis;

    @Test
    void setIfAbsentWithTtlWorks() {
        String key = "seat-lock:test:1";

        Boolean first = redis.opsForValue().setIfAbsent(key, "booking-a", Duration.ofMinutes(5));
        Boolean second = redis.opsForValue().setIfAbsent(key, "booking-b", Duration.ofMinutes(5));

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(redis.opsForValue().get(key)).isEqualTo("booking-a");
        assertThat(redis.getExpire(key)).isPositive();

        redis.delete(key);
    }
}
