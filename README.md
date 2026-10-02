# Real-Time Event Booking System

Modular-monolith backend (Spring Boot, PostgreSQL, Redis) for booking event seats
without double booking. Full documentation is written in Phase 10.

## Status

| Phase | Scope | State |
|---|---|---|
| 0 | Foundation: project skeleton, Docker Compose, Flyway schema, error handling | done |
| 1 | Auth and security | next |

## Run locally (Phase 0)

```bash
cp .env .env            # then set DB_PASSWORD
docker compose up -d            # Postgres + Redis

cd backend
set -a; source ../.env; set +a  # export env vars to this shell
mvn spring-boot:run

curl localhost:8080/actuator/health   # {"status":"UP"}
```

## Tests

```bash
cd backend
mvn test     # integration tests start Postgres/Redis via Testcontainers, so Docker must be running
```

`GlobalExceptionHandlerTest` is a web-slice test and needs no Docker.
