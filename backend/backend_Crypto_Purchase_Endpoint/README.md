# Crypto Currency Backend

Minimal backend scaffold for the crypto currency project using Node.js, Express, MySQL, and Docker.

## Implemented endpoint

- `POST /api/transactions/buy`

Request body:

```json
{
  "userId": 1,
  "symbol": "BTC",
  "quantity": 0.25,
  "priceUsd": 67000
}
```

Response (`201`):

```json
{
  "message": "Buy transaction created",
  "transaction": {
    "id": 1,
    "userId": 1,
    "symbol": "BTC",
    "transactionType": "BUY",
    "quantity": "0.25000000",
    "priceUsd": "67000.00",
    "totalUsd": "16750.00",
    "createdAt": "2026-02-23T00:00:00.000Z"
  }
}
```

## Run with Docker

From `Crypto_Currency/`:

```bash
docker compose up --build
```

Health check:

```bash
curl http://localhost:3000/health
```

## Test in Postman

- Method: `POST`
- URL: `http://localhost:3000/api/transactions/buy`
- Header: `Content-Type: application/json`
- Body: raw JSON (example above)

## Inspect in MySQL Workbench

Connect to:
- Host: `127.0.0.1`
- Port: `3306`
- User: `app_user`
- Password: `app_password`
- Database: `crypto_db`

Query:

```sql
SELECT * FROM transactions ORDER BY id DESC;
```
