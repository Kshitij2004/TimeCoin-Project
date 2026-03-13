# TimeCoin Backend

Spring Boot backend for the integrated TimeCoin application. The backend now contains the blockchain repository interfaces, coin purchase flow, and transaction history API in one service.

## Tech Stack

- Java 21
- Spring Boot
- Spring Data JPA
- Spring Security
- MySQL 8
- Gradle

## Run

From the repo root:

```bash
docker compose up --build
```

Or run the backend only:

```bash
cd backend
./gradlew bootRun
```

Default local API base:

- `http://localhost:8080`

## Package Layout

```text
src/main/java/t_12/backend/
├── api/
│   ├── auth/
│   ├── coin/
│   ├── transaction/
│   └── wallet/
├── config/
├── entity/
├── exception/
├── filter/
├── repository/
└── service/
```

## Important Endpoints

### Auth

`POST /api/auth/register`

Request body:

```json
{
  "username": "badger",
  "email": "badger@wisc.edu",
  "password": "secret"
}
```

### Wallet

`GET /api/wallet?userId=1`

Requires an `Authorization` header because `AuthFilter` still enforces a non-empty header on non-auth routes.

### Coin Data

`GET /api/coin`

Alias:

`GET /api/coins`

Response shape:

```json
{
  "currentPrice": 10.00,
  "totalSupply": 1000000.00,
  "circulatingSupply": 500000.00
}
```

### Buy TimeCoin

`POST /api/coin/buy`

Request body:

```json
{
  "userId": 1,
  "symbol": "TC",
  "amount": 1.25
}
```

Compatibility route used by some frontend code:

`POST /api/transactions/buy`

### Transaction History

`GET /api/transactions?page=1&limit=10`

Headers:

```text
Authorization: Bearer fake-token
x-user-id: 1
```

Response shape:

```json
{
  "data": [
    {
      "type": "BUY",
      "amount": 1.25,
      "priceAtTime": 66000.00,
      "timestamp": "2026-02-23T12:00:00"
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

## Auth Note

The project currently has partial auth only:

- registration is implemented
- login is not implemented
- `AuthFilter` accepts any non-empty `Authorization` header on protected routes

That is why the frontend still sends a placeholder bearer token.

## Database Notes

The backend schema is defined in [db/init.sql](../db/init.sql). Important integrated tables include:

- `coins`
- `wallets`
- `transactions`
- `blocks`
- `block_transactions`
- `validators`
- `staking_events`
- `listings`

If you have an older local database volume, recreate it after schema changes:

```bash
docker compose down -v
docker compose up --build
```

## Tests

Run:

```bash
cd backend
./gradlew test
```
