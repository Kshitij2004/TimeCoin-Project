# TimeCoin Frontend

React frontend for the TimeCoin project. The current UI includes landing, login, registration, dashboard, marketplace, and transaction history pages.

## Routes

- `/`
- `/login`
- `/register`
- `/dashboard`
- `/marketplace`
- `/history`

Protected pages currently use a mock authenticated state in [src/App.js](src/App.js).

## API Base URL

The frontend expects:

```text
REACT_APP_API_URL=http://localhost:8080
```

If the env var is not set, it defaults to `http://localhost:8080`.

## Main Feature Areas

- Registration calls `POST /api/auth/register`
- Marketplace reads `GET /api/coin` and posts `POST /api/coin/buy`
- History reads `GET /api/transactions`

Because backend login is not implemented yet, the marketplace and history pages currently send a placeholder bearer token to satisfy the backend auth filter.

## Run

```bash
cd frontend
npm install
npm start
```

App URL:

- `http://localhost:3000`

## Tests

```bash
cd frontend
npm test -- --watchAll=false
```

Current tests cover:

- marketplace page behavior
- transaction history page behavior

## Notes

- The old standalone `user_transaction_history` folder was removed after its code was merged into [src/pages/History.js](src/pages/History.js).
- Marketplace auth is still stubbed in [src/pages/marketplace/Marketplace.js](src/pages/marketplace/Marketplace.js).
