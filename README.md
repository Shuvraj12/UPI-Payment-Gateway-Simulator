# UPI Payment Gateway Simulator

A simulated UPI-style payments backend and frontend, built phase by phase as a portfolio project. Every transfer, wallet, and request is internal — this does not integrate with any real payment network.

**Status:** Phase 1 of 14 complete.

## Tech stack

| Layer | Choice |
|---|---|
| Backend | Java 17, Spring Boot 3.5.16, Spring Security 6, Spring Data JPA |
| Auth | JWT (jjwt 0.13.0) |
| Database | MySQL 8/9 (H2 in-memory for tests) |
| API docs | springdoc-openapi / Swagger UI |
| Frontend | React 19, Vite, Tailwind CSS v4, React Router, Axios |
| Build | Maven (backend), npm (frontend) |

### Why Spring Boot 3.5.16, not 4.1

Spring Boot 3.5 reached end-of-life on 2026-06-30; 3.5.16 is the final patch. Spring Boot 4.1 is current, but two dependencies this project needs — **jjwt** (no Jackson 3 support yet) and **springdoc-openapi** (active Jackson-3-related bugs as of early 2026) — aren't friction-free on top of it yet. For a portfolio project, a stable, fully-patched line beats chasing the newest major version. Worth revisiting once the ecosystem catches up.

## Repository layout

```
upi-payment-gateway-simulator/
├── backend/     Spring Boot REST API
├── frontend/    React + Vite SPA
└── README.md    You are here
```

### Backend package structure

```
backend/src/main/java/com/upisimulator/
├── config/        Bean configuration (OpenAPI, and more as phases add it)
├── controller/     REST endpoints
├── dto/            Request/response DTOs (ApiResponse<T> wrapper, ErrorResponse)
├── entity/         JPA entities — empty until Phase 2 (User)
├── exception/      GlobalExceptionHandler + custom exceptions
├── mapper/         Entity <-> DTO conversion — empty until Phase 2
├── repository/     Spring Data JPA repositories — empty until Phase 2
├── security/       JWT utilities + SecurityConfig
├── service/        Business logic (interface + impl) — empty until Phase 2
└── util/           Shared constants (ApiPaths)
```

Packages with no classes yet still exist as `package-info.java` files, so the intended architecture is visible in the repo from Phase 1 onward rather than materializing folder-by-folder.

### Frontend structure

```
frontend/src/
├── components/    LedgerEntry (live health check), RoadmapList
├── pages/         Home, NotFound
├── services/      api.js (axios instance), healthService.js
├── App.jsx        Route definitions
├── main.jsx       Entry point (wraps App in BrowserRouter)
└── index.css      Tailwind v4 import + design tokens
```

## Getting started

### Prerequisites

- JDK 17
- Maven 3.9+ (or your IDE's bundled Maven — IntelliJ IDEA includes one)
- Node.js 20+ and npm
- MySQL 8+ running locally (the app creates the `upi_simulator` database on first run)

### Backend

```powershell
cd backend
# set these however you prefer - env vars, IDE run config, or edit application.yml directly
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your-mysql-password"
mvn spring-boot:run
```

The API comes up on `http://localhost:8080/api/v1`. Swagger UI is at `http://localhost:8080/api/v1/swagger-ui.html`.

*(macOS/Linux: use `export DB_PASSWORD=...` instead of `$env:DB_PASSWORD=...`.)*

Tests don't need MySQL running — `src/test/resources/application.yml` points the test classpath at an in-memory H2 database instead:

```powershell
mvn test
```

### Frontend

```powershell
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`. Copy `.env.example` to `.env` if your backend isn't on the default port.

## Environment variables

| Variable | Where | Default | Purpose |
|---|---|---|---|
| `DB_HOST` | backend | `localhost` | MySQL host |
| `DB_PORT` | backend | `3306` | MySQL port |
| `DB_NAME` | backend | `upi_simulator` | Database name (auto-created) |
| `DB_USERNAME` | backend | `root` | MySQL username |
| `DB_PASSWORD` | backend | `root` | MySQL password |
| `JWT_SECRET` | backend | dev placeholder | HMAC signing key — regenerate before any real deployment |
| `JWT_EXPIRATION` | backend | `900000` (15 min) | Access token lifetime, ms |
| `JWT_REFRESH_EXPIRATION` | backend | `604800000` (7 days) | Refresh token lifetime, ms |
| `VITE_API_BASE_URL` | frontend | `http://localhost:8080/api/v1` | Backend base URL |

## API endpoints (Phase 1)

| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/health` | Service status + a generated reference ID, used by the homepage to confirm the frontend can reach the backend |

## Database schema (Phase 1)

None yet. This phase wires up the MySQL connection (`spring.jpa.hibernate.ddl-auto=update`) so Hibernate is ready to create tables the moment the first entity (`User`) lands in Phase 2. `ddl-auto=update` is a deliberate short-term choice for iteration speed through the early phases; migrating to Flyway once the core schema (Wallet, BankAccount, UpiId) stabilizes around Phase 4-5 is the plan.

## Roadmap

- [x] **Phase 1** — Project setup, JWT config skeleton, basic home page
- [ ] Phase 2 — Authentication (register, login, refresh, logout)
- [ ] Phase 3 — User profile
- [ ] Phase 4 — Wallet
- [ ] Phase 5 — Bank accounts
- [ ] Phase 6 — UPI IDs
- [ ] Phase 7 — Money transfer
- [ ] Phase 8 — QR payments
- [ ] Phase 9 — Money requests
- [ ] Phase 10 — Transaction history
- [ ] Phase 11 — Analytics dashboard
- [ ] Phase 12 — Admin dashboard
- [ ] Phase 13 — Notifications
- [ ] Phase 14 — Docker + deployment

## License

This project is created for educational and portfolio purposes.
