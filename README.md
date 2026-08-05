# UPI Payment Gateway Simulator

A simulated UPI-style payments backend and frontend, built phase by phase as a portfolio project. Every transfer, wallet, and request is internal — this does not integrate with any real payment network.

**Status:** Phase 2 of 14 complete.

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

### Why refresh tokens live in localStorage (for now)

The access token stays in memory only (React state), but the refresh token is persisted to `localStorage` so a page refresh doesn't force a re-login. That's simpler to build than the alternative, but it means an XSS vulnerability could steal a refresh token — `localStorage` is readable by any script on the page. A production system would put the refresh token in an `httpOnly` cookie instead (unreadable from JS, but requires the backend to set `Set-Cookie`, handle CSRF, and adjust CORS to send credentials). Flagging this as a known, deliberate simplification rather than an oversight — worth hardening if this project ever needs to look production-ready specifically from a security-review angle.

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
├── config/         OpenApiConfig, JpaAuditingConfig
├── controller/      HomeController, AuthController
├── dto/             ApiResponse<T>, ErrorResponse, Register/Login/Refresh requests, AuthResponse
├── entity/          BaseEntity, User, Role, RefreshToken
├── exception/       ApiException + 4 subtypes, GlobalExceptionHandler
├── mapper/          Still empty — see package-info for why
├── repository/      UserRepository, RefreshTokenRepository
├── security/        JwtUtil, JwtProperties, SecurityConfig, JwtAuthenticationFilter,
│                     UserDetailsServiceImpl, RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── service/         AuthService (+ impl/AuthServiceImpl)
└── util/            ApiPaths
```

Packages with no classes yet still exist as `package-info.java` files, so the intended architecture is visible in the repo from Phase 1 onward rather than materializing folder-by-folder.

### Frontend structure

```
frontend/src/
├── components/    Header (auth-aware), LedgerEntry (live health check), RoadmapList
├── context/       AuthContext — session state, restores from stored refresh token on load
├── pages/         Home, Login, Register, NotFound
├── services/      api.js (axios instance + token interceptor), authService.js, healthService.js
├── App.jsx        Route definitions, wraps everything in AuthProvider
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

The API comes up on `http://localhost:8080/api/v1`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

*(Correction from Phase 1's README: Swagger isn't under `/api/v1` — that prefix comes from `ApiPaths.BASE` on our own controllers, and springdoc's own paths are configured separately in `application.yml`.)*

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

## API endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/health` | Public | Service status + a generated reference ID, used by the homepage to confirm the frontend can reach the backend |
| POST | `/api/v1/auth/register` | Public | Create an account (name, email, password, phone) — returns an access + refresh token pair |
| POST | `/api/v1/auth/login` | Public | Authenticate with email + password — returns a new token pair |
| POST | `/api/v1/auth/refresh` | Public* | Exchange a refresh token for a new access token. Rotates the refresh token — the old one is revoked and cannot be reused |
| POST | `/api/v1/auth/logout` | Public* | Revoke a refresh token |

\* "Public" meaning no `Authorization` header is required — the refresh token in the request body is itself the credential for these two.

Everything else now requires `Authorization: Bearer <accessToken>`; an unauthenticated request to a protected path gets a consistent `401` body from `RestAuthenticationEntryPoint` rather than Spring's default error page.

## Database schema

**`users`** — one row per account.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| full_name | VARCHAR | |
| email | VARCHAR | unique, doubles as the Spring Security username |
| password | VARCHAR | BCrypt hash, never returned in any response (`@JsonIgnore`) |
| phone_number | VARCHAR | unique, validated as a 10-digit Indian mobile number |
| role | VARCHAR | `USER` or `ADMIN`; enum stored as a string, not an ordinal, so adding a role later doesn't renumber existing rows |
| enabled | BOOLEAN | default `true`; Phase 12's "freeze user" will flip this |
| created_at / updated_at | DATETIME | via JPA auditing (`BaseEntity`) |

**`refresh_tokens`** — one row per issued refresh token.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| jti | VARCHAR | unique, the JWT's own ID claim — this is what's looked up, not the raw token string |
| user_id | BIGINT, FK → users.id | |
| expires_at | DATETIME | |
| revoked | BOOLEAN | flipped on logout, and on every successful refresh (rotation) |
| created_at / updated_at | DATETIME | |

Still on `ddl-auto=update` for now (see Phase 1 notes on migrating to Flyway once the schema stabilizes around Phase 4-5).

## Roadmap

- [x] **Phase 1** — Project setup, JWT config skeleton, basic home page
- [x] **Phase 2** — Authentication (register, login, refresh with rotation, logout)
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

This project is open-source and available for learning and educational purposes.
