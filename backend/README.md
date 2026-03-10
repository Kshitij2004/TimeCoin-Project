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
| Frontend | React (runs on `localhost:3000`) |

---

## How to Run

**Prerequisites:** Docker Desktop must be running.

```bash
# From the project root (Project_12/)
docker compose down -v && docker compose up --build
```

This wipes old volumes and rebuilds everything. The database runs `init.sql` then `seed.sql` fresh on each start. Wait for both of these lines in the logs before testing:

```
Started BackendApplication
HikariPool-1 - Start completed
```

| Service | URL |
|---|---|
| Backend API | `http://localhost:8080` |
| Frontend | `http://localhost:3000` |

---

## Project Structure

```
src/main/java/t_12/backend/
├── api/
│   ├── auth/           # AuthController + RegisterRequest + UserDTO
│   ├── coin/           # CoinController + CoinDTO
│   └── wallet/         # WalletController + WalletDTO
├── config/             # SecurityConfig - Spring Security setup
├── entity/             # JPA entities - map directly to database tables
│   ├── Coin.java
│   ├── User.java
│   └── Wallet.java
├── exception/          # GlobalExceptionHandler, ResourceNotFoundException,
│                       #   DuplicateResourceException
├── filter/             # AuthFilter - runs before every request
├── repository/         # Spring Data JPA interfaces
│   ├── CoinRepository.java
│   ├── UserRepository.java
│   └── WalletRepository.java
├── service/            # Business logic - all logic lives here, not in controllers
│   ├── CoinService.java
│   ├── UserService.java
│   └── WalletService.java
├── BackendApplication.java
└── HealthController.java

src/test/java/t_12/backend/service/
├── CoinServiceTest.java
├── UserServiceTest.java
└── WalletServiceTest.java
```

### Layered Architecture

Each layer has exactly one job. A request flows like this:

```
HTTP Request
    → AuthFilter        validates the Authorization header, returns 401 if missing/invalid
    → SecurityConfig    Spring Security route rules (public vs. protected endpoints)
    → Controller        receives the request, calls the service, returns the response
    → Service           business logic, calls the repository, throws exceptions if needed
    → Repository        talks to MySQL, returns data
    → back up as JSON
```

**Why this matters for you:**
- Every endpoint follows the same pattern. Once you've read one, you've read them all
- Controllers stay thin: no SQL, no business logic, just wiring
- Services are independently testable without HTTP or a database
- Adding a new feature means adding files at each layer; you should never need to touch unrelated code

---

## Endpoints

### Authentication

Auth is handled by `AuthFilter` and configured in `SecurityConfig`. The `api/auth/` package currently exposes registration (login is planned but not implemented). Although `SecurityConfig` currently permits the wallet and coin endpoints (makes them publicly accessible at Spring Security layer), `AuthFilter` still **requires** an `Authorization` header on every request except `/api/auth/*`. In practice this means every call shown below must include a header (dummy values are accepted).

Requests to protected endpoints without a valid `Authorization` header receive:

```json
{ "status": 401, "error": "Unauthorized", "message": "Missing Authorization header" }
```

> **Note:** wallet and coin endpoints are technically "public" in `SecurityConfig`, but the `AuthFilter` still blocks requests that omit the header. This keeps the API simple while allowing the frontend to send any placeholder value.

#### POST /api/auth/register

Registers a new user.

**Request body:**
```json
{
    "username": "badger",
    "password": "secret"
}
```

**Response:** `201 Created` with a `UserDTO`, or `409 Conflict` if the username is already taken.

#### POST /api/auth/login

**Status:** _Not implemented yet._

The codebase currently has no controller method for handling login, and the frontend does not call this route. `AuthFilter` merely ensures an `Authorization` header is present on protected requests; it does **not** validate credentials. When the login feature is added, this section will describe the request/response payloads and any tokens returned.

---

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
| 401 | Missing or invalid `Authorization` header |
| 404 | Wallet, coin, or user record not found in database |
| 409 | Duplicate resource (e.g. registering a username that already exists) |
| 500 | Unexpected server error |

---

## Adding to the Backend

All three sections below follow the same layered pattern. Use the existing `wallet/` or `coin/` packages as a reference, they're the simplest working examples.

### Adding a New Endpoint

Say you want to add `GET /api/transactions` for a `Transaction` entity. Here's what to create, in order:

**1. Entity** - `entity/Transaction.java`

Maps to the `transactions` database table. Annotate with `@Entity` and `@Table`. Field names must match column names (or use `@Column` to map them).

```java
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;
    private Double amount;
}
```

**2. Repository** - `repository/TransactionRepository.java`

Extend `JpaRepository`. Spring Data generates all basic queries automatically. You only need to declare custom ones.

```java
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserId(Long userId);
}
```

**3. Service** - `service/TransactionService.java`

All business logic lives here. Inject the repository, call it, and throw a typed exception if data isn't found. Never return raw entities, convert to DTOs (see next step).

```java
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public List<TransactionDTO> getTransactionsByUser(Long userId) {
        List<Transaction> transactions = transactionRepository.findByUserId(userId);
        if (transactions.isEmpty()) {
            throw new ResourceNotFoundException("No transactions found for userId: " + userId);
        }
        return transactions.stream()
                .map(t -> new TransactionDTO(t.getId(), t.getUserId(), t.getAmount()))
                .toList();
    }
}
```

**4. DTO + Controller** - `api/transaction/TransactionDTO.java` and `TransactionController.java`

The DTO is what actually gets serialized to JSON. Keep it to fields the frontend needs. The controller is just wiring: inject the service, call it, return the result.

```java
// TransactionDTO.java
public class TransactionDTO {
    private Long id;
    private Long userId;
    private Double amount;
    // constructor, getters
}

// TransactionController.java
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<TransactionDTO>> getTransactions(@RequestParam Long userId) {
        return ResponseEntity.ok(transactionService.getTransactionsByUser(userId));
    }
}
```

That's the full vertical slice (for backend). No other files need to change.

---

### Adding a New Entity/Table

If your new feature needs a new table, do both of these:

**1. Add the SQL** to `db/init.sql` (schema) and `db/seed.sql` (test data). Both files run automatically when Docker volumes are created. Note: example not reflective of actual file contents.

```sql
-- init.sql
CREATE TABLE transactions (
    id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT         NOT NULL,
    amount  DECIMAL(18, 8) NOT NULL
);

-- seed.sql
INSERT INTO transactions (user_id, amount) VALUES (1, 25.00);
```

**2. Create the JPA entity** as shown in the section above. `spring.jpa.hibernate.ddl-auto=update` is set, so Hibernate will adjust the live table automatically if entity fields change but the canonical definition lives in `init.sql`. **Coordinate with the team before renaming columns.**

To apply schema changes locally, wipe and rebuild the volume:

```bash
docker compose down -v && docker compose up --build
```

---

### Adding Tests for a New Service

Tests live in `src/test/java/t_12/backend/`. They run without Docker or a database - Mockito mocks the repository so you're testing pure service logic.

Use `WalletServiceTest`, `CoinServiceTest`, or `UserServiceTest` as a template. The pattern is always the same:

```java
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void getTransactionsByUser_returnsTransactions_whenFound() {
        // Arrange
        List<Transaction> fakeData = List.of(new Transaction(1L, 1L, 25.00));
        when(transactionRepository.findByUserId(1L)).thenReturn(fakeData);

        // Act
        List<TransactionDTO> result = transactionService.getTransactionsByUser(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(25.00, result.get(0).getAmount());
    }

    @Test
    void getTransactionsByUser_throws404_whenNoneFound() {
        when(transactionRepository.findByUserId(99L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransactionsByUser(99L));
    }
}
```

Run tests with:

```bash
# From Project_12/backend/
./gradlew test
```

---

## Database

Tables are defined in `db/init.sql` and seeded with test data in `db/seed.sql`.

| Entity | Table | Notes |
|---|---|---|
| `User` | `users` | Stores credentials; used by auth |
| `Wallet` | `wallets` | `userId` references `users.id` |
| `Coin` | `coins` | Single global record tracking current price and supply |

---

## Notes

`Wallet.userId` references `users.id`. If the `User` entity schema changes, verify the wallet foreign key relationship still holds and wipe/rebuild your Docker volume to pick up the updated `init.sql`.