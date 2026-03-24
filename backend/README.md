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
| Auth | JWT via jjwt 0.12.6, BCrypt password hashing |
| Security | Spring Security |
| Testing | JUnit 5 + Mockito |
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
│   ├── auth/           # Registration and login endpoints
│   ├── balance/        # Ledger-dervied wallet balance queries
│   ├── block/          # Manual block assembly trigger
│   ├── blockchain/     # Public blockchain explorer endpoints
│   ├── coin/           # TimeCoin price info and purchase
│   ├── listings/       # Marketplace listing CRUD
│   ├── marketplace/    # Listing purchase endpoint
│   ├── transaction/    # Transfer submission and history
│   └── wallet/         # Wallet info and transaction history
├── config/             # Spring Security route rules nad JWT filter registration
├── entity/             # JPA entities - map directly to database tables
├── exception/          # Typed exceptions and global error handler
├── filter/             # Validates JWT on every request before it hits a controller
├── repository/         # Spring Data JPA interfaces
└── service/            # All business logic lives here, not in controllers

src/test/java/t_12/backend/service/ # All service tests live here
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

Auth is handled by `AuthFilter` and configured in `SecurityConfig`. The `api/auth/` package exposes both registration and login. Every non-auth request must include a valid JWT in the `Authorization` header using the format `Bearer <token>`.

Requests to protected endpoints without a valid `Authorization` header receive:

```json
{ "status": 401, "error": "Unauthorized", "message": "Missing Authorization header" }
```

> **Note:** wallet, coin, and transaction routes are protected. Use a valid JWT in `Authorization: Bearer <token>`.

#### POST /api/auth/register

Registers a new user.

**Request body:**
```json
{
    "username": "badger",
    "email": "badger@wisc.edu",
    "password": "secret"
}
```

**Response:** `201 Created` with a `UserDTO`, or `409 Conflict` if the username or email is already taken.

#### POST /api/auth/login

Authenticates a user and returns a signed JWT as a plain string response body.

**Request body:**
```json
{
    "username": "badger",
    "password": "secret"
}
```

**Response:** `200 OK` with a JWT token string.

---

### GET /api/wallet

Returns the coin balance for a user.

**Query param:** `userId` (Long)

**Example:**
```
GET http://localhost:8080/api/wallet?userId=1
Authorization: Bearer <jwt>
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

### POST /api/transactions/buy

Creates a purchase transaction and updates the user's wallet balance.

**Headers:**
```text
Authorization: Bearer <jwt>
x-user-id: 9
```

**Request body:**
```json
{
    "userId": 9,
    "symbol": "TC",
    "amount": 1.25
}
```

**Response:** `201 Created` with the created transaction and updated wallet.

### GET /api/transactions

Returns paginated transaction history for the authenticated user.

**Headers:**
```text
Authorization: Bearer <jwt>
x-user-id: 9
```

**Example:**
```
GET http://localhost:8080/api/transactions?page=1&limit=10
```

**Response shape:**
```json
{
    "data": [
        {
            "type": "BUY",
            "amount": 1.25,
            "priceAtTime": 10.00,
            "timestamp": "2026-03-16T23:00:00"
        }
    ],
    "pagination": {
        "page": 1,
        "limit": 10,
        "total": 1,
        "totalPages": 1
    }
}
```

### POST /api/transactions/transfer

Submits a blockchain transfer. Valid transactions are enqueued in the mempool as `PENDING`.

**Headers:**
```text
Authorization: Bearer <jwt>
```

**Request body:**
```json
{
    "senderAddress": "wlt_sender_address",
    "receiverAddress": "wlt_receiver_address",
    "amount": 5.00000000,
    "fee": 0.01000000,
    "nonce": 1
}
```

**Response:** `201 Created` with a transaction entity whose `status` is `PENDING` and `blockId` is `null`.

### GET /api/transactions/{hash}

Looks up a single transaction by its `transactionHash`.

**Headers:**
```text
Authorization: Bearer <jwt>
```

**Example:**
```text
GET http://localhost:8080/api/transactions/0cd3e34249e38f1dd6dd14e1fe56ab94751d07a6005d1ecc9f0a3947919c80bc
```

**Response:** `200 OK` with the matching transaction, or `404 Not Found` if no transaction exists for that hash.

---

## Testing With Postman

Use a freshly registered user after any `docker compose down -v`, because wiping volumes recreates the database from `seed.sql`.

1. Register a user:

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json
```

```json
{
    "username": "postmanuser",
    "email": "postmanuser@wisc.edu",
    "password": "secret123"
}
```

Save the returned `id`.

2. Log in to get a JWT:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
    "username": "postmanuser",
    "password": "secret123"
}
```

Copy the token returned in the response body.

Important:
- Copy the full raw JWT (`header.payload.signature` with two dots)
- Do not include quotes
- Do not include the word `Bearer` in the token value field
- Use a freshly registered user (seed users use placeholder hashes)

3. Verify wallet keypair data:

```http
GET http://localhost:8080/api/wallet?userId=<user-id>
Authorization: Bearer <token>
```

Expected result: `200 OK` with `walletAddress` and `publicKey`. `privateKey` is not returned by wallet lookup.

4. Buy TimeCoin:

```http
POST http://localhost:8080/api/transactions/buy
Authorization: Bearer <token>
x-user-id: <user-id>
Content-Type: application/json
```

```json
{
    "userId": <user-id>,
    "symbol": "TC",
    "amount": 1.25
}
```

Expected result: `201 Created` with `message`, `transaction`, and `wallet` in the response body.

5. Check transaction history:

```http
GET http://localhost:8080/api/transactions?page=1&limit=10
Authorization: Bearer <token>
x-user-id: <user-id>
```

Expected result: `200 OK` with `data` and `pagination`, including the new `BUY` transaction.

6. Submit a transfer (mempool enqueue):

```http
POST http://localhost:8080/api/transactions/transfer
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
    "senderAddress": "<sender-wallet-address>",
    "receiverAddress": "<receiver-wallet-address>",
    "amount": 5.00000000,
    "fee": 0.01000000,
    "nonce": 1
}
```

Expected result: `201 Created` with `status: "PENDING"` and `blockId: null`.

7. Look up the transfer by hash:

```http
GET http://localhost:8080/api/transactions/<transaction-hash>
Authorization: Bearer <token>
```

Expected result: `200 OK` and the same transaction in `PENDING` state.

8. Duplicate prevention check:

Re-send the exact same `POST /api/transactions/transfer` payload from step 6.

Expected result: `409 Conflict` because the same transaction cannot appear twice in `PENDING` state.

9. New pending transfer check:

Send the same transfer payload again, but increment `nonce` (for example, `nonce: 2`).

Expected result: `201 Created` with a new pending transaction.

> Do not use the seeded users for login testing. The seeded rows are useful for wallet and data setup, but their stored password hashes are placeholders rather than real bcrypt hashes.

### Blockchain Explorer Postman Collection

The explorer endpoints are public `GET` routes, so no JWT is required:

- `GET /api/chain/status`
- `GET /api/chain/blocks?page=1&limit=10`
- `GET /api/chain/blocks/{height}`
- `GET /api/chain/blocks/hash/{hash}`

Import this collection into Postman:

- `backend/postman/blockchain_explorer.postman_collection.json`

After import, set:

- `baseUrl` to `http://localhost:8080`
- `blockHeight` and `blockHash` to values from your local data (defaults are included)

### Postman JWT Troubleshooting

If you see `401 Unauthorized` with message `Invalid or expired token`:
- Re-run `POST /api/auth/login` and copy the newest token again.
- In Postman `Authorization` tab, choose `Bearer Token` and paste only the raw JWT.
- Remove any manually added `Authorization` header from the `Headers` tab.
- For wallet endpoint, use `GET /api/wallet?userId=<id>` and remove unrelated headers like `x-user-id` or `userID`.

If you see `ENOTFOUND {{baseUrl}}`:
- Either select an environment that defines `baseUrl`, or use a literal URL like `http://localhost:8080/...`.

If login returns `500`:
- Verify you are logging in with the same username/password that you just registered.
- Do not send `Authorization` header on `/api/auth/login`.

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

## Database

Tables are defined in `db/init.sql` and seeded with test data in `db/seed.sql`.

| Entity | Table | Notes |
|---|---|---|
| `User` | `users` | Stores credentials; used by auth |
| `Wallet` | `wallets` | One per user; holds `coin_balance` and Ed25519 keypair |
| `Coin` | `coins` | Single global row tracking current price and supply |
| `Block` | `blocks` | Each block links to its predecessor via `previous_hash` |
| `BlockTransaction` | `block_transactions` | Join table linking blocks to their transactions |
| `Transaction` | `transactions` | Covers both blockchain transfers and marketplace purchases |
| `Listing` | `listings` | Marketplace listings; soft-deleted via `REMOVED` status |
| `StakingEvent` | `staking_events` | Immutable audit log of every stake/unstake action |
| `Validator` | `validators` | Users who have opted into PoS consensus by staking |

### Balance Calculation

Wallet balances are derived from confirmed on-chain state by `BalanceService`, not read from a stored column:
```
available = received - sent - fees - staked
total     = available + staked
```

The `coin_balance` column on `wallets` acts as a running cache updated on each purchase or transfer. It will be replaced by ledger-derived balance once that feature fully lands.

---

## Notes

`Wallet.userId` references `users.id`. If the `User` entity schema changes, verify the wallet foreign key relationship still holds and wipe/rebuild your Docker volume to pick up the updated `init.sql`.
