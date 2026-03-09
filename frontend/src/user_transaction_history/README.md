# user_transaction_history

This folder is a drop-in history page module.

Primary export:
- `HistoryPage` from `index.js`
- Compatibility default export: `History.js`

Expected backend:
- `GET http://localhost:3000/api/transactions?page=<page>&limit=<limit>`
- Header: `x-user-id: <id>`

To render in existing app, import this module into your route/page file and mount it at `/history`.
