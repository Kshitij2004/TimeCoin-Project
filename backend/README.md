# TimeCoin Backend

Spring Boot REST API backend for the TimeCoin platform. Handles wallet balances, coin pricing, and service marketplace data for UW-Madison students.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0 |
| Database | MySQL 8.0 (Dockerized) |
| Build Tool | Gradle |
| ORM | Hibernate / Spring Data JPA |

---

## How to Run

**Prerequisites:** Docker Desktop must be running.

```bash
# From the project root (Project_12/)
docker compose down -v                        # wipe old volumes (runs init.sql + seed.sql fresh)
docker compose up --build db backend          # build and start database and backend
```

Wait for both of these lines in the logs before testing:
```
Started BackendApplication
HikariPool-1 - Start completed
```

The API is then available at `http://localhost:8080`.

---

## Project Structure

```
src/main/java/t_12/backend/
├── entity/             # JPA entities — map directly to database tables
├── repository/         # Database access — Spring Data JPA interfaces
├── service/            # Business logic — all logic lives here, not in controllers
├── api/
│   ├── wallet/         # WalletController + WalletDTO
│   └── coin/           # CoinController + CoinDTO
├── exception/          # Global error handling
└── filter/             # Auth filter — runs before every request
```

### Why This Structure?

The backend uses a strict layered architecture. Each layer has one job:

```
HTTP Request
    → AuthFilter        checks for Authorization header, returns 401 if missing
    → Controller        receives the request, calls the service, returns the response
    → Service           business logic, calls the repository, throws exceptions if needed
    → Repository        talks to MySQL, returns data
    → back up as JSON
```

This separation means:
- Controllers stay thin; no SQL, no business logic
- Services are independently testable without HTTP or a database
- Swapping out the database or auth system only touches one layer

---

## Endpoints

### Authentication
Every request requires an `Authorization` header. Any non-empty value is accepted for now. Real JWT validation will be added in a future issue. Requests without the header receive:

```json
{ "status": 401, "error": "Unauthorized", "message": "Missing Authorization header" }
```

### GET /api/wallet
Returns the coin balance for a user.

**Query param:** `userId` (Long)

**Example:**
```
GET http://localhost:8080/api/wallet?userId=1
Authorization: dummy
```

**Response:**
```json
{
    "userId": 1,
    "coinBalance": 5.00000000
}
```

---

### GET /api/coin
Returns current TimeCoin price and supply info.

**Example:**
```
GET http://localhost:8080/api/coin
Authorization: dummy
```

**Response:**
```json
{
    "currentPrice": 10.00,
    "totalSupply": 1000000.00,
    "circulatingSupply": 500000.00
}
```

---

## Error Responses

All errors return the same JSON shape:

```json
{
    "timestamp": "2026-02-22T05:00:00",
    "status": 404,
    "error": "Not Found",
    "message": "Wallet not found for userId: 99"
}
```

| Status | Cause |
|---|---|
| 401 | Missing `Authorization` header |
| 404 | Wallet or coin record not found in database |
| 500 | Unexpected server error |

---

## Database

Tables are defined in `db/init.sql` and seeded with test data in `db/seed.sql`. Both files run automatically when the Docker volume is first created.

| Entity | Table | Notes |
|---|---|---|
| `Wallet` | `wallets` | `userId` is a plain Long for now — not yet joined to `User` |
| `Coin` | `coins` | Single global record tracking current price and supply |

`spring.jpa.hibernate.ddl-auto=update` is set, so Hibernate will adjust tables automatically if entity fields change. **Coordinate with the team before renaming columns.**

---

## Running Tests

Unit tests run without Docker or a database connection:

```bash
# From Project_12/backend/
./gradlew test
```

Tests use Mockito to mock the repository layer, so they test service logic in pure isolation. Current test coverage:

| Test Class | What It Tests |
|---|---|
| `WalletServiceTest` | Returns correct wallet, throws 404 when not found |
| `CoinServiceTest` | Returns correct coin data, throws 404 when none found |

---

## For the Frontend

- Base URL: `http://localhost:8080`
- All requests need `Authorization: <anything>` header
- Wallet endpoint requires `?userId=X` until real session auth is wired up
- All error responses follow `{ timestamp, status, error, message }`
- CORS is not yet configured — flag this if you hit issues calling from the frontend

---

## For Future Auth Implementation

The `AuthFilter` in `filter/AuthFilter.java` is the only file that needs to change when real JWT auth is added. It currently checks for the presence of any `Authorization` header. Replace the check inside `doFilterInternal()` with real token validation—no other files need to change.

`Wallet.userId` is currently a plain `Long`. Once the `User` entity is stable, this should be replaced with a proper `@ManyToOne` JPA relationship.