# Real-Time Event Booking Application

A backend application for booking seats for real-time events with secure authentication, Redis-based seat locking, payment simulation, booking lifecycle management, admin controls, and personalized event recommendations.

The application is built as a **modular monolith using Spring Boot**, with PostgreSQL as the primary database and Redis for temporary seat locks and fast-access operations.

---

## 🚀 Features

### 🔐 Authentication & Authorization
- User registration and login
- JWT-based authentication
- Role-based authorization
- Admin and user roles
- Secure password handling
- Protected admin management APIs

### 🎫 Event Catalog
- Event and category management
- Event details including:
  - Title
  - Description
  - Category
  - Location
  - Poster
- Show scheduling
- Seat management
- Seat availability retrieval

### 🔒 Real-Time Seat Locking
- Temporary seat holds using Redis
- Prevents multiple users from booking the same seat simultaneously
- Configurable seat-hold TTL
- Automatic release of expired holds

### 📦 Booking Management
- Create temporary booking holds
- Maximum seats per booking
- Booking expiry
- Booking confirmation
- Booking history and details
- Booking status management
- Seat status updates

### 💳 Payment
- Mock payment processing
- Payment status tracking
- Integration with booking confirmation flow

### 👨‍💼 Admin Management
Administrators can manage:

- Categories
- Events
- Shows
- Seats
- Bookings

Admin booking APIs support:

- Pagination
- Status filtering
- Show-based filtering
- Detailed booking information

### 🤖 Personalized Recommendations
The application provides rule-based event recommendations based on user activity.

Recommendation logic considers:

- Previously booked event categories
- Previously visited event locations
- Popular events
- Upcoming events
- Related events

Users with no booking history receive popular-event recommendations as a fallback, while anonymous users can receive upcoming-event recommendations.

The recommendation layer is designed so an AI-based recommendation provider can be introduced later without changing the controller/API structure.

### 📚 API Documentation
Interactive API documentation is provided using **Swagger UI / OpenAPI**.

Swagger supports:

- API discovery
- Request/response inspection
- Try-it-out API execution
- JWT authentication through the `Authorize` button

---

## 🏗️ Architecture

The application follows a modular-monolith architecture.

```text
                         Client
                           │
                           ▼
                  ┌─────────────────┐
                  │  Spring Boot    │
                  │   REST API      │
                  └────────┬────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
   Authentication      Event Catalog       Booking
        │                  │                  │
        │                  │                  ├── Payment
        │                  │                  └── Seat Locking
        │                  │
        │                  ▼
        │                Shows
        │                  │
        │                  ▼
        │                Seats
        │
        └──────────────────┬──────────────────┘
                           │
                ┌──────────┴──────────┐
                ▼                     ▼
           PostgreSQL              Redis
           Persistent Data       Temporary Locks
```

### Main Modules

```text
com.eventbooking
│
├── auth
│   └── security
│
├── booking
│   ├── controller
│   ├── repository
│   └── service
│
├── catalog
│   ├── category
│   ├── event
│   ├── seat
│   └── show
│
├── recommendation
│   ├── dto
│   ├── RecommendationProvider
│   ├── RecommendationService
│   └── RuleBasedRecommendationProvider
│
├── payment
│
└── common
    └── security
```

---

## 🛠️ Tech Stack

| Technology | Purpose |
|---|---|
| Java 21 | Backend development |
| Spring Boot 3.4.5 | REST API framework |
| Spring Security | Authentication & authorization |
| JWT | Stateless authentication |
| Spring Data JPA | Database persistence |
| PostgreSQL 16 | Primary database |
| Redis 7 | Seat locking / temporary state |
| Flyway | Database migrations |
| Maven | Build & dependency management |
| Docker Compose | PostgreSQL & Redis infrastructure |
| Testcontainers | Integration testing |
| JUnit | Automated testing |
| Swagger / OpenAPI | API documentation |
| Lombok | Boilerplate reduction |

---

## 🔄 Booking Flow

The core booking flow is designed around temporary seat locking.

```text
User
 │
 │ Selects seats
 ▼
Check Seat Availability
 │
 ▼
Create Booking Hold
 │
 ▼
Lock Seats in Redis
 │
 ▼
Payment Processing
 │
 ├───────────────┐
 │               │
 ▼               ▼
Success         Expired/Failed
 │               │
 ▼               ▼
Confirm        Release Seats
Booking        / Expire Booking
 │
 ▼
Seats become BOOKED
```

This prevents two users from successfully booking the same seat during the temporary reservation window.

---

## 🔑 Authentication Flow

```text
User
 │
 ▼
POST /api/auth/login
 │
 ▼
JWT generated
 │
 ▼
Client sends:
Authorization: Bearer <JWT>
 │
 ▼
JWT Authentication Filter
 │
 ▼
AuthenticatedUser
 │
 ▼
Controller
```

Admin APIs additionally verify the user's role before allowing management operations.

---

## ⚡ Redis Seat Locking

Redis is used for temporary seat locks rather than storing short-lived locking state only in PostgreSQL.

A simplified flow is:

```text
Seat selected
     │
     ▼
Redis lock created
     │
     ▼
TTL starts
     │
     ├──── Booking confirmed
     │          │
     │          ▼
     │      Seat BOOKED
     │
     └──── Hold expires
                │
                ▼
           Lock released
```

The seat-hold duration is configurable through application properties.

---

## 🤖 Recommendation System

The recommendation system uses a provider-based design.

```text
RecommendationController
          │
          ▼
RecommendationService
          │
          ▼
RecommendationProvider
          │
          ▼
RuleBasedRecommendationProvider
```

Currently, recommendations are generated using booking history and event information.

### Personalized recommendations

For users with confirmed booking history:

1. Find previously booked categories.
2. Find previously booked locations.
3. Find upcoming events matching those preferences.
4. Generate recommendations.
5. Provide a reason such as:

```text
Based on your interest in Music
```

### Fallback

If a user has no relevant booking history:

```text
Personalized recommendations
          │
          ▼
No history
          │
          ▼
Popular events
```

The provider abstraction leaves room for adding an external AI/ML recommendation provider later.

---

## 📡 API Overview

### Authentication

```text
POST /api/auth/signup
POST /api/auth/login
```

### Events

```text
GET    /api/events
GET    /api/events/{id}
POST   /api/events
PUT    /api/events/{id}
DELETE /api/events/{id}
```

### Shows

```text
GET    /api/events/{eventId}/shows
POST   /api/events/{eventId}/shows
PUT    /api/shows/{id}
DELETE /api/shows/{id}
```

### Seats

```text
GET  /api/shows/{showId}/seats
POST /api/shows/{showId}/seats
```

### Bookings

```text
POST /api/bookings/hold
POST /api/bookings/{bookingId}/confirm
GET  /api/bookings/{bookingId}
GET  /api/bookings
```

### Admin

```text
GET /api/admin/bookings
GET /api/admin/bookings/{id}
```

### Recommendations

```text
GET /api/recommendations
GET /api/recommendations/popular
GET /api/recommendations/upcoming
GET /api/recommendations/related/{eventId}
```

> The exact available operations and request/response schemas can be explored interactively through Swagger UI.

---

## 📖 Swagger UI

After starting the application, open:

```text
http://localhost:8080/swagger-ui/index.html
```

Swagger provides interactive documentation for the REST APIs.

For protected endpoints:

1. Login through the authentication API.
2. Copy the JWT.
3. Click **Authorize**.
4. Enter the JWT.
5. Execute protected APIs directly from Swagger UI.

---

## ⚙️ Configuration

The application uses environment variables for sensitive configuration.

Example:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=eventbooking
DB_USERNAME=eventbooking
DB_PASSWORD=<your-password>

JWT_SECRET=<minimum-32-character-secret>

ADMIN_EMAIL=<admin-email>
ADMIN_PASSWORD=<admin-password>
```

Sensitive values should **not** be committed to Git.

The project uses `.env` for local configuration and keeps it excluded through `.gitignore`.

---

## 🐳 Running Infrastructure with Docker

PostgreSQL and Redis can be started using Docker Compose.

From the project root:

```bash
docker compose up -d
```

Check the containers:

```bash
docker compose ps
```

The application expects:

```text
PostgreSQL → localhost:5432
Redis      → localhost:6379
```

To stop the infrastructure:

```bash
docker compose down
```

---

## ▶️ Running the Application

### Prerequisites

Make sure you have:

- Java 21
- Maven
- Docker Desktop
- PostgreSQL/Redis infrastructure available

### Start infrastructure

```bash
docker compose up -d
```

### Set required environment variables

Example for Git Bash:

```bash
export DB_PASSWORD='<your-password>'
export JWT_SECRET='<minimum-32-character-secret>'
```

### Start Spring Boot

From the `backend` directory:

```bash
mvn spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

---

## 🧪 Running Tests

Run the complete test suite:

```bash
mvn clean test
```

The project uses integration testing with Testcontainers for infrastructure-dependent tests.

---

## 🔍 Testing Strategy

The application contains automated tests covering important application flows, including:

- Authentication
- Event catalog
- Seat management
- Booking lifecycle
- Redis-based booking behavior
- Payment flow
- Booking expiry
- Admin APIs
- Recommendation APIs

Integration tests verify the interaction between the Spring Boot application and its persistence/infrastructure components.

---

## 🔐 Security Considerations

- JWT-based stateless authentication
- Role-based authorization for admin operations
- Passwords are not stored as plain text
- Secrets are supplied through environment variables
- `.env` is excluded from version control
- Protected APIs require authentication

---

## 📈 Future Improvements

Potential future enhancements include:

- Real payment gateway integration
- Email/SMS booking notifications
- Advanced AI/ML recommendation provider
- Event search improvements
- Distributed deployment
- Monitoring and observability
- Analytics dashboard
- Rate limiting
- CI/CD pipeline
- Cloud deployment

---

## 🎯 Project Goals

This project was designed to demonstrate practical backend engineering concepts rather than only basic CRUD operations.

Key engineering areas demonstrated:

- REST API design
- Authentication and authorization
- Database modeling
- Transactional booking workflows
- Concurrency-aware seat reservation
- Redis integration
- Payment workflow design
- Admin management
- Recommendation architecture
- Integration testing
- API documentation
- Docker-based infrastructure

---

## 👨‍💻 Author

**Tushaar**

B.Tech Computer Science Engineering

---

## 📄 License

This project is intended for educational and portfolio purposes.
