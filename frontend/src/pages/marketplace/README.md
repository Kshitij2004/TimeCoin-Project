# Marketplace Feature

This directory contains all frontend code for the `/marketplace` page, where logged-in users can view the current coin price and purchase coins.

---

## Files

| File | Description |
|------|-------------|
| `Marketplace.js` | Main marketplace page component |
| `Marketplace.css` | Styles for the marketplace page |
| `Marketplace.test.jsx` | Unit tests for the marketplace component |
| `README.md` | This file |

---

## What the Page Does

- Displays current coin price, circulating supply, and total supply
- Input field for the amount of coins to buy
- Shows a live cost estimate as the user types
- Buy button that calls the purchase API
- Shows success or error feedback after a transaction
- Auth-gated — redirects to `/login` if not logged in

---

## Auth

Auth is currently **stubbed** with a hardcoded user while the auth system is being built:

```js
// TODO: replace with real auth context when built
const user = { id: 1, username: "testuser1" };
const token = "fake-token";
```

When auth is ready, replace the stub with the real `useAuth` hook and remove the `.skip` from the auth gate test in `Marketplace.test.jsx`.

---

## API Endpoints Used

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/coins` | Fetches current price and supply |
| `POST` | `/api/transactions/buy` | Submits a purchase |

The backend routes for these live in `routes/marketplace.routes.js`.

---

## Running Tests

First time setup — install dependencies from the project root:

```bash
npm install -D vitest @testing-library/react @testing-library/user-event @testing-library/jest-dom jsdom
```

Make sure `vite.config.js` has the test block and `src/test/setup.js` exists (see the main project README for details).

Then run:

```bash
npm test
```

---

## TODO

- [ ] Replace stubbed auth with real `useAuth` context
- [ ] Enable skipped auth gate tests in `Marketplace.test.jsx`
- [ ] Add `Marketplace.css` for styling
- [ ] Wire up `/marketplace` route in `App.js`