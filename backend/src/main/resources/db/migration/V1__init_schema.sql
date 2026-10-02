-- Real-Time Event Booking System: initial schema.
-- Postgres is the source of truth. Redis only holds temporary seat locks.

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- fast ILIKE search on event titles

-- ---------------------------------------------------------------- users
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);
CREATE UNIQUE INDEX uq_users_email_lower ON users (lower(email));

-- ----------------------------------------------------------- categories
CREATE TABLE categories (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(60) NOT NULL,
    slug VARCHAR(60) NOT NULL,
    CONSTRAINT uq_categories_name UNIQUE (name),
    CONSTRAINT uq_categories_slug UNIQUE (slug)
);

-- --------------------------------------------------------------- events
CREATE TABLE events (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    category_id BIGINT       NOT NULL REFERENCES categories (id),
    location    VARCHAR(255) NOT NULL,
    poster_url  VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_events_category    ON events (category_id);
CREATE INDEX idx_events_location    ON events (lower(location));
CREATE INDEX idx_events_title_trgm  ON events USING gin (lower(title) gin_trgm_ops);

-- ---------------------------------------------------------------- shows
-- Deleting an event cascades to its shows, but bookings.show_id is RESTRICT,
-- so an event/show that already has bookings can never be deleted by accident.
CREATE TABLE shows (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    start_time TIMESTAMPTZ  NOT NULL,
    end_time   TIMESTAMPTZ  NOT NULL,
    venue      VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_shows_status CHECK (status IN ('SCHEDULED', 'CANCELLED')),
    CONSTRAINT chk_shows_time   CHECK (end_time > start_time)
);
CREATE INDEX idx_shows_event_start ON shows (event_id, start_time);

-- ---------------------------------------------------------------- seats
-- status is only AVAILABLE / BOOKED / BLOCKED here. "HELD" is NOT stored:
-- it is derived from a live Redis lock (seat-lock:{showId}:{seatId}).
CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    show_id     BIGINT         NOT NULL REFERENCES shows (id) ON DELETE CASCADE,
    row_label   VARCHAR(5)     NOT NULL,
    seat_number INT            NOT NULL,
    section     VARCHAR(50)    NOT NULL,
    price       NUMERIC(10, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    version     BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT uq_seats_position UNIQUE (show_id, row_label, seat_number),
    CONSTRAINT chk_seats_status  CHECK (status IN ('AVAILABLE', 'BOOKED', 'BLOCKED')),
    CONSTRAINT chk_seats_price   CHECK (price >= 0),
    CONSTRAINT chk_seats_number  CHECK (seat_number > 0)
);
CREATE INDEX idx_seats_show ON seats (show_id);

-- ------------------------------------------------------------- bookings
CREATE TABLE bookings (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      BIGINT         NOT NULL REFERENCES users (id),
    show_id      BIGINT         NOT NULL REFERENCES shows (id),
    status       VARCHAR(20)    NOT NULL,
    total_amount NUMERIC(10, 2) NOT NULL,
    expires_at   TIMESTAMPTZ    NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT chk_bookings_status CHECK (status IN ('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT chk_bookings_total  CHECK (total_amount >= 0)
);
CREATE INDEX idx_bookings_user_created   ON bookings (user_id, created_at DESC);
CREATE INDEX idx_bookings_status_expires ON bookings (status, expires_at);  -- expiry sweeper
CREATE INDEX idx_bookings_show           ON bookings (show_id);

-- -------------------------------------------------------- booking_seats
-- "active" is true only while the booking is CONFIRMED. The partial unique
-- index below is the database-level guarantee against double booking:
-- no two active rows can ever point at the same seat.
CREATE TABLE booking_seats (
    id               BIGSERIAL PRIMARY KEY,
    booking_id       UUID           NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    seat_id          BIGINT         NOT NULL REFERENCES seats (id),
    price_at_booking NUMERIC(10, 2) NOT NULL,
    active           BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_booking_seat UNIQUE (booking_id, seat_id)
);
CREATE UNIQUE INDEX uq_booking_seats_active_seat ON booking_seats (seat_id) WHERE active;
CREATE INDEX idx_booking_seats_booking ON booking_seats (booking_id);

-- ------------------------------------------------------------- payments
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    booking_id      UUID           NOT NULL REFERENCES bookings (id),
    amount          NUMERIC(10, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    idempotency_key VARCHAR(100)   NOT NULL,
    failure_reason  VARCHAR(255),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_payments_idempotency UNIQUE (idempotency_key),
    CONSTRAINT chk_payments_status CHECK (status IN ('INITIATED', 'SUCCESS', 'FAILED', 'REFUNDED'))
);
CREATE INDEX idx_payments_booking ON payments (booking_id);
