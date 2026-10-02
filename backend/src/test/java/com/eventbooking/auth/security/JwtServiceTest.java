package com.eventbooking.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventbooking.common.security.AuthenticatedUser;
import com.eventbooking.common.security.Role;
import io.jsonwebtoken.security.WeakKeyException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-chars!!";
    private static final JwtProperties PROPS = new JwtProperties(SECRET, Duration.ofMinutes(60));

    private final JwtService service = new JwtService(PROPS);

    @Test
    void issuedTokenRoundTrips() {
        IssuedToken token = service.issue(7L, "a@b.com", Role.ADMIN);

        assertThat(service.parse(token.value())).contains(new AuthenticatedUser(7L, "a@b.com", Role.ADMIN));
        assertThat(token.expiresInSeconds()).isEqualTo(3600);
    }

    @Test
    void expiredTokenIsRejected() {
        Clock twoHoursAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(2)), ZoneOffset.UTC);
        IssuedToken oldToken = new JwtService(PROPS, twoHoursAgo).issue(7L, "a@b.com", Role.USER);

        assertThat(service.parse(oldToken.value())).isEmpty();
    }

    @Test
    void tokenWithTamperedPayloadIsRejected() {
        String token = service.issue(7L, "a@b.com", Role.USER).value();
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"1\",\"email\":\"a@b.com\",\"role\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8));

        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(service.parse(forged)).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService attacker = new JwtService(
                new JwtProperties("a-completely-different-secret-of-32-plus-chars", Duration.ofMinutes(60)));

        String token = attacker.issue(7L, "a@b.com", Role.ADMIN).value();

        assertThat(service.parse(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(service.parse("not-a-jwt")).isEmpty();
        assertThat(service.parse("")).isEmpty();
    }

    @Test
    void secretShorterThan256BitsIsRejected() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", Duration.ofMinutes(1))))
                .isInstanceOf(WeakKeyException.class);
    }
}
