# UPI Payment Gateway Simulator

A simulated UPI-style payments backend and frontend, built phase by phase as a portfolio project. Every transfer, wallet, and request is internal — this does not integrate with any real payment network.

**Status:** Phase 7 of 14 complete.

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

### Why two different locking strategies on `Wallet`

Balance mutations (deposit now; transfer/refund from Phase 7) go through a `SELECT ... FOR UPDATE` pessimistic lock (`WalletRepository.findByUserIdForUpdate`) — a competing write on the same wallet waits rather than racing, which is the right failure mode when losing or double-applying money is unacceptable. Everything else on `Wallet` (freeze/unfreeze) relies on a plain `@Version` optimistic-locking column instead — lower-contention, and "retry on conflict" is a fine outcome there, so paying for a DB lock would be overkill. `WalletConcurrencyIntegrationTest` fires ten real concurrent deposits at one wallet and asserts the balance is exactly their sum — without the lock this is flaky-to-failing; with it, it's reliable.

One thing to carry into Phase 7: a transfer will need to lock two wallets (sender + receiver) in one transaction. Locking them in a consistent order (e.g. ascending wallet id) is what avoids two simultaneous transfers deadlocking on each other — noted in `Wallet`'s Javadoc now so it isn't rediscovered the hard way later.

### Why bank accounts don't move wallet money (yet)

Phase 5 only manages linked-account *records* — add, verify, set primary, delete. It doesn't wire them into `Wallet.balance` at all. Real UPI apps genuinely link accounts primarily so the system knows which account a transaction ultimately settles against, but this simulator already has a single, tested, pessimistic-locked balance holder (`Wallet`), and duplicating that logic for a second money-movement path here would compete with — not complement — Phase 7's actual transfer feature. Bank accounts exist as the realistic linked-account records recruiters would expect to see, and as a clean foundation a "top up from bank" feature could build on later without a schema change, but that wiring isn't built.

### Why verification outcomes are injected, not called inline

`BankAccountServiceImpl` depends on a `SimulatedOutcomeSource` interface rather than calling `ThreadLocalRandom` directly. The real implementation is genuinely random (~85% success, modeling that a bank's actual verification check isn't 100% either) — but unit tests inject a mock that forces both the success and failure branch deterministically, and `BankAccountControllerIntegrationTest` overrides the bean with `@MockitoBean` to force success, so "verify then set as primary" isn't a flaky end-to-end test. Small seam, but it's the difference between testing both branches on purpose and hoping the test run got lucky.

### Why a UPI ID requires a wallet and a verified bank account

`UpiId` resolves to a `Wallet`, not a `BankAccount` — consistent with the previous decision that `Wallet` is this simulator's actual balance holder. But creating one still checks that a verified bank account exists first, even though the UPI ID itself doesn't reference it afterward. That mirrors real UPI onboarding (a VPA has to represent a verified identity, not just an app account) and, more usefully here, it's what makes Phase 5 feel load-bearing rather than a side quest — you can't reach Phase 6 without having actually exercised Phase 5's verification flow first. Scope note: this phase deliberately stops at create/list/default/availability, matching the phase brief exactly — no delete, unlike bank accounts. Easy to add if wanted; leaving it out for now rather than expanding scope unprompted.

### Why transfers lock wallets by id, not by sender/recipient role

This is the payoff of a note left in Phase 4: a transfer locks *two* wallets in the same transaction. Locking them in role order — sender first, then recipient — deadlocks the moment two people pay each other simultaneously: transaction A holds Alice's lock waiting for Bob's, transaction B holds Bob's lock waiting for Alice's, both wait forever. `TransferServiceImpl` locks whichever wallet has the *lower id* first, regardless of who's sending or receiving — both transactions contend for the same wallet first, and one just waits instead of deadlocking. `TransferConcurrencyIntegrationTest` fires 40 real concurrent transfers between two wallets (20 each way), and asserts total money is conserved and nothing times out. Worth running yourself — multi-threaded behavior is the hardest to verify by reading code alone.

### Fixing a Phase 4 bug: `referenceNumber` couldn't actually be shared

`Transaction`'s Javadoc said from Phase 4 that a transfer would produce two rows sharing a UTR — but the column was marked `unique = true`, which would have rejected the second row the moment a transfer actually tried to write it. Replaced with a composite `(reference_number, direction)` unique constraint: exactly one DEBIT and one CREDIT per UTR, which is the real invariant. Found while implementing the feature the constraint was written in anticipation of — worth knowing about since it means every Phase 4–6 deposit record that landed in the database will need a schema migration if `ddl-auto` doesn't handle it automatically during the first startup with this code.

### Why idempotency keys are required, not optional

Every transfer request carries a client-generated UUID `idempotencyKey`. On the first request with a given key, the transfer executes. On any later request with the *same* key, `TransactionRepository.findByIdempotencyKey` finds the already-completed row and returns its result without moving money again — so a network retry or double-submit can't double-charge. It's checked before any wallet is touched. A request that failed validation never persists a row, so retrying with the same key after a real failure (insufficient balance, frozen wallet) still processes normally. Defense in depth: the column also has a unique constraint, so even a race between two simultaneous requests with the same key falls through to the `DataIntegrityViolationException` handler added in Phase 4.

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
├── config/         OpenApiConfig, JpaAuditingConfig, WebConfig (serves /uploads/**)
├── controller/      HomeController, AuthController, ProfileController, WalletController,
│                     BankAccountController, UpiIdController, TransferController
├── dto/             ApiResponse<T>, ErrorResponse, auth/profile/wallet/bank-account/
│                     upi-id/transfer DTOs
├── entity/          BaseEntity, User, Role, RefreshToken, Wallet, Transaction (+3 enums,
│                     grown in Phase 7 with idempotencyKey + counterpartyVpa),
│                     BankAccount (+3 enums), UpiId
├── exception/       ApiException + 5 subtypes, GlobalExceptionHandler
├── mapper/          Still empty — see package-info for why
├── repository/      UserRepository, RefreshTokenRepository, WalletRepository (+ id-based
│                     locking lookup), TransactionRepository (+ idempotency + daily-sum
│                     queries), BankAccountRepository, UpiIdRepository
├── security/        JwtUtil, JwtProperties, SecurityConfig, JwtAuthenticationFilter,
│                     UserDetailsServiceImpl, RestAuthenticationEntryPoint, RestAccessDeniedHandler
├── service/         AuthService, ProfileService, FileStorageService, WalletService,
│                     BankAccountService, SimulatedOutcomeSource, UpiIdService,
│                     TransferService (+ impl/)
└── util/            ApiPaths, UtrGenerator, MaskingUtil
```

Packages with no classes yet still exist as `package-info.java` files, so the intended architecture is visible in the repo from Phase 1 onward rather than materializing folder-by-folder.

### Frontend structure

```
frontend/src/
├── components/    Header (auth-aware), ProtectedRoute, LedgerEntry, RoadmapList,
│                   TransactionRow (shows "Sent to"/"Received from"), BankAccountCard
├── context/       AuthContext — session state, restores from stored refresh token on load
├── pages/         Home, Login, Register, Profile, Wallet, BankAccounts, UpiIds,
│                   SendMoney, NotFound
├── services/      api.js, authService.js, profileService.js, walletService.js,
│                   bankAccountService.js, upiIdService.js, transferService.js, healthService.js
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
| `UPLOAD_DIR` | backend | `uploads` | Where profile pictures are written on disk |
| `VITE_API_BASE_URL` | frontend | `http://localhost:8080/api/v1` | Backend base URL |

## API endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/health` | Public | Service status + a generated reference ID, used by the homepage to confirm the frontend can reach the backend |
| POST | `/api/v1/auth/register` | Public | Create an account (name, email, password, phone) — returns an access + refresh token pair |
| POST | `/api/v1/auth/login` | Public | Authenticate with email + password — returns a new token pair |
| POST | `/api/v1/auth/refresh` | Public* | Exchange a refresh token for a new access token. Rotates the refresh token — the old one is revoked and cannot be reused |
| POST | `/api/v1/auth/logout` | Public* | Revoke a refresh token |
| GET | `/api/v1/profile` | Bearer | Get the caller's own profile |
| PUT | `/api/v1/profile` | Bearer | Update full name + phone number (email is immutable here) |
| PUT | `/api/v1/profile/password` | Bearer | Change password — revokes every other active session |
| POST | `/api/v1/profile/picture` | Bearer | Upload a profile picture, `multipart/form-data`, JPEG/PNG up to 2MB |
| DELETE | `/api/v1/profile` | Bearer | Soft-delete the account — requires the current password in the body |
| POST | `/api/v1/wallet` | Bearer | Create a wallet (once per account) |
| GET | `/api/v1/wallet` | Bearer | Get balance and freeze status |
| POST | `/api/v1/wallet/deposit` | Bearer | Simulated deposit — capped at ₹1,00,000/request, not a real payment rail |
| PUT | `/api/v1/wallet/freeze` | Bearer | Freeze your own wallet (blocks deposits; will block transfers from Phase 7) |
| PUT | `/api/v1/wallet/unfreeze` | Bearer | Unfreeze your own wallet |
| GET | `/api/v1/wallet/transactions` | Bearer | Paginated ledger (`?page=&size=&sort=`) — filters/search arrive Phase 10 |
| POST | `/api/v1/bank-accounts` | Bearer | Link a new bank account (first one becomes primary automatically) |
| GET | `/api/v1/bank-accounts` | Bearer | List linked accounts (masked account numbers) |
| POST | `/api/v1/bank-accounts/{id}/verify` | Bearer | Simulate a verification check (~85% success, retryable on failure) |
| PUT | `/api/v1/bank-accounts/{id}/primary` | Bearer | Set a verified account as primary |
| DELETE | `/api/v1/bank-accounts/{id}` | Bearer | Remove a linked account (blocked if it's primary and others exist) |
| POST | `/api/v1/upi-ids` | Bearer | Create a UPI ID — requires an existing wallet and a verified bank account |
| GET | `/api/v1/upi-ids` | Bearer | List your UPI IDs |
| GET | `/api/v1/upi-ids/availability` | Bearer | Check whether a username is available (`?username=`), for live-typing checks |
| PUT | `/api/v1/upi-ids/{id}/default` | Bearer | Set a UPI ID as the default |
| GET | `/api/v1/transfers/resolve` | Bearer | Look up the account name behind a UPI ID (`?vpa=`), to confirm before sending |
| POST | `/api/v1/transfers` | Bearer | Send money to a UPI ID. Requires an `idempotencyKey` (UUID); capped at &#8377;1,00,000/transfer and &#8377;2,00,000/day |

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
| profile_picture_url | VARCHAR | nullable; e.g. `/uploads/profile-pictures/<uuid>.jpg` |
| deleted | BOOLEAN | default `false`; self-service soft delete (Phase 3) — kept distinct from `enabled` so admin-freeze and self-delete stay distinguishable |
| deleted_at | DATETIME | nullable, set when `deleted` flips to `true` |
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

**`wallets`** — one row per user (unique `user_id`).

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| user_id | BIGINT, FK → users.id, unique | |
| balance | DECIMAL(19,2) | mutated only through a pessimistic-locked lookup — see decisions above |
| frozen | BOOLEAN | default `false`, self-service (distinct from Phase 12's admin freeze on `users.enabled`) |
| frozen_at | DATETIME | nullable |
| version | BIGINT | `@Version` — optimistic lock for non-balance updates (freeze/unfreeze) |
| created_at / updated_at | DATETIME | |

**`transactions`** — one row per wallet-affecting event, from that wallet's own point of view.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| reference_number | VARCHAR(12), unique | UPI-style numeric UTR |
| wallet_id | BIGINT, FK → wallets.id | |
| type | VARCHAR | `DEPOSIT` today; `TRANSFER`/`QR_PAYMENT`/`REQUEST_SETTLEMENT`/`REFUND` reserved for later phases |
| direction | VARCHAR | `CREDIT` or `DEBIT` |
| amount | DECIMAL(19,2) | |
| balance_after | DECIMAL(19,2) | snapshot of the wallet's balance right after this entry |
| status | VARCHAR | `SUCCESS` today; `PENDING`/`FAILED` become reachable once transfers can fail partway |
| description | VARCHAR | nullable |
| created_at / updated_at | DATETIME | |

**`bank_accounts`** — many per user, hard-deleted (see decisions above for why this one isn't soft-deleted).

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| user_id | BIGINT, FK → users.id | |
| account_holder_name | VARCHAR | |
| bank_name | VARCHAR | one of a curated set of major Indian banks (enum) |
| account_number | VARCHAR | stored in full; every API response masks all but the last 4 digits |
| ifsc_code | VARCHAR(11) | validated against the real IFSC format |
| account_type | VARCHAR | `SAVINGS` or `CURRENT` |
| primary | BOOLEAN | exactly one `true` per user, enforced in the service layer |
| verification_status | VARCHAR | `PENDING`, `VERIFIED`, or `FAILED` |
| verified_at | DATETIME | nullable |
| created_at / updated_at | DATETIME | |

**`upi_ids`** — many per user (capped at 3), resolves to a `wallet_id` directly rather than via `user_id` — see decisions above for why.

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK | |
| user_id | BIGINT, FK → users.id | |
| wallet_id | BIGINT, FK → wallets.id | the fast-lookup path a transfer (Phase 7) will need |
| vpa | VARCHAR, unique | full address, e.g. `priya123@upisim`, always lowercased |
| primary | BOOLEAN | exactly one `true` per user, enforced in the service layer |
| created_at / updated_at | DATETIME | |

Still on `ddl-auto=update`. Phase 4's README flagged Phase 5 as a reasonable point to switch to Flyway; revising that. Hand-writing migration SQL for six tables that has to exactly match what Hibernate already auto-generates, with no way to run `mvn` here to confirm it actually applies, is a real risk of shipping something broken — worse than staying on auto-DDL. This is a better one to do locally, where it's actually verifiable. Concrete trigger for when it stops being optional: the first time the schema needs a change `ddl-auto=update` can't do safely (a column rename, or tightening a nullable column to `NOT NULL` on existing data) — that's where auto-DDL goes from convenient to actively risky.

## Roadmap

- [x] **Phase 1** — Project setup, JWT config skeleton, basic home page
- [x] **Phase 2** — Authentication (register, login, refresh with rotation, logout)
- [x] **Phase 3** — User profile (edit, change password, picture upload, soft delete)
- [x] **Phase 4** — Wallet (create, balance, simulated deposit, freeze, transaction ledger)
- [x] **Phase 5** — Bank accounts (add, verify, primary account, delete)
- [x] **Phase 6** — UPI IDs (create, default, availability checking)
- [x] **Phase 7** — Money transfer (send, receive, idempotency, daily limits, deadlock-safe locking)
- [ ] Phase 8 — QR payments
- [ ] Phase 8 — QR payments
- [ ] Phase 9 — Money requests
- [ ] Phase 10 — Transaction history
- [ ] Phase 11 — Analytics dashboard
- [ ] Phase 12 — Admin dashboard
- [ ] Phase 13 — Notifications
- [ ] Phase 14 — Docker + deployment

## License

This project is open-source and available for learning and educational purposes.
