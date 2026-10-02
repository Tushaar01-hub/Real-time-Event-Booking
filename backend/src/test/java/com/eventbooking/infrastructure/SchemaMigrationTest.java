package com.eventbooking.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventbooking.support.AbstractIntegrationTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

class SchemaMigrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void flywayCreatesAllCoreTables() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains(
                "users", "categories", "events", "shows", "seats", "bookings", "booking_seats", "payments");
    }

    @Test
    void categoriesAreSeeded() {
        Integer count = jdbc.queryForObject("SELECT count(*) FROM categories", Integer.class);

        assertThat(count).isPositive();
    }

    /**
     * Layer 3 of the double-booking defence, proven at the schema level:
     * many PENDING bookings may reference a seat, but only ONE may be active (confirmed).
     */
    @Test
    @Transactional
    void databaseRejectsTwoActiveBookingsForTheSameSeat() {
        Long userId = jdbc.queryForObject(
                "INSERT INTO users (email, password_hash, full_name) VALUES (?, 'hash', 'Test User') RETURNING id",
                Long.class, "user-" + UUID.randomUUID() + "@test.dev");
        Long categoryId = jdbc.queryForObject("SELECT id FROM categories LIMIT 1", Long.class);
        Long eventId = jdbc.queryForObject(
                "INSERT INTO events (title, category_id, location) VALUES ('Test Event', ?, 'Delhi') RETURNING id",
                Long.class, categoryId);
        Long showId = jdbc.queryForObject(
                "INSERT INTO shows (event_id, start_time, end_time, venue) "
                        + "VALUES (?, now() + interval '7 days', now() + interval '7 days 3 hours', 'Main Hall') "
                        + "RETURNING id", Long.class, eventId);
        Long seatId = jdbc.queryForObject(
                "INSERT INTO seats (show_id, row_label, seat_number, section, price) "
                        + "VALUES (?, 'A', 1, 'STANDARD', 500) RETURNING id", Long.class, showId);

        UUID bookingOne = insertBooking(userId, showId);
        UUID bookingTwo = insertBooking(userId, showId);

        // Two pending (inactive) rows for the same seat are fine: Redis decides who holds it.
        insertBookingSeat(bookingOne, seatId);
        insertBookingSeat(bookingTwo, seatId);

        // The first booking gets confirmed...
        assertThatCode(() -> activate(bookingOne)).doesNotThrowAnyException();

        // ...and the database itself refuses to confirm a second booking for the same seat.
        assertThatThrownBy(() -> activate(bookingTwo)).isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID insertBooking(Long userId, Long showId) {
        return jdbc.queryForObject(
                "INSERT INTO bookings (user_id, show_id, status, total_amount, expires_at) "
                        + "VALUES (?, ?, 'PENDING', 500, now() + interval '5 minutes') RETURNING id",
                UUID.class, userId, showId);
    }

    private void insertBookingSeat(UUID bookingId, Long seatId) {
        jdbc.update("INSERT INTO booking_seats (booking_id, seat_id, price_at_booking) VALUES (?, ?, 500)",
                bookingId, seatId);
    }

    private void activate(UUID bookingId) {
        jdbc.update("UPDATE booking_seats SET active = true WHERE booking_id = ?", bookingId);
    }
}
