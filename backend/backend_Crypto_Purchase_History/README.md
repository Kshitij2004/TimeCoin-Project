# Backend Crypto Purchase History (Java)

REST endpoint for returning an authenticated user's transaction history.

## Endpoint

- `GET /api/transactions`

Authentication for now uses header:
- `x-user-id: <integer>`

Query params:
- `page` (default `1`)
- `limit` (default `20`, max `100`)

## Response

```json
{
  "data": [
    {
      "type": "BUY",
      "amount": "0.25000000",
      "priceAtTime": "66000.00",
      "timestamp": "2026-02-23T12:00:00.000Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 3,
    "totalPages": 1
  }
}
```

Sorted by most recent first using `ORDER BY created_at DESC, id DESC`.

## Run

From `backend/backend_Crypto_Purchase_History`:

```bash
docker compose up --build
```

## Test with Postman

- Method: `GET`
- URL: `http://localhost:8080/api/transactions?page=1&limit=2`
- Header: `x-user-id: 1`

## Run unit tests

```bash
mvn test
```
