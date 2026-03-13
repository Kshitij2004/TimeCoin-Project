# Marketplace Page

This folder contains the React marketplace page where the user can view TimeCoin pricing and submit a purchase.

## Files

- `Marketplace.js`
- `Marketplace.css`
- `Marketplace.test.js`

## Current Behavior

- loads current coin price, total supply, and circulating supply
- lets the user enter an amount to buy
- shows a live estimated USD total
- submits a purchase request to the backend
- refreshes coin data after a successful purchase

## API Calls

- `GET /api/coin`
- `POST /api/coin/buy`

The component uses the shared frontend API base URL from [src/services/api.js](../../services/api.js).

## Auth Status

Real auth is not finished yet. The page still uses:

```js
const user = { id: 1, username: "testuser1" };
const token = "fake-token";
```

That placeholder token is required because the backend `AuthFilter` still expects a non-empty `Authorization` header.

## Tests

Run from `frontend/`:

```bash
npm test -- --watchAll=false
```
