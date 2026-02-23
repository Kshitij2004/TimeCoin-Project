const { query } = require("../db");

class TransactionsError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.name = "TransactionsError";
    this.statusCode = statusCode;
  }
}

function parsePagination(page, limit) {
  const parsedPage = Number(page || 1);
  const parsedLimit = Number(limit || 20);

  if (!Number.isInteger(parsedPage) || parsedPage < 1) { throw new TransactionsError("page must be a positive integer", 400); }
  if (!Number.isInteger(parsedLimit) || parsedLimit < 1 || parsedLimit > 100) { throw new TransactionsError("limit must be an integer between 1 and 100", 400); }

  return {
    page: parsedPage,
    limit: parsedLimit,
    offset: (parsedPage - 1) * parsedLimit
  };
}

function buildTransactionsService(deps = {}) {
  const runQuery = deps.query || query;

  return {
    async getUserTransactions({ userId, page, limit }) {
      if (!Number.isInteger(userId) || userId <= 0) { throw new TransactionsError("Authenticated user is required", 401); }
      const { page: resolvedPage, limit: resolvedLimit, offset } = parsePagination(page, limit);
      const countRows = await runQuery(
        "SELECT COUNT(*) AS total FROM transactions WHERE user_id = ?",
        [userId]
      );
      const total = Number(countRows[0]?.total || 0);
      const rows = await runQuery(
        `SELECT transaction_type, quantity, price_usd, created_at
        FROM transactions
        WHERE user_id = ?
        ORDER BY created_at DESC, id DESC
        LIMIT ? OFFSET ?`,
        [userId, resolvedLimit, offset]
      );
      const transactions = rows.map((row) => ({
        type: row.transaction_type,
        amount: row.quantity,
        priceAtTime: row.price_usd,
        timestamp: row.created_at
      }));

      return {
        data: transactions,
        pagination: {
          page: resolvedPage,
          limit: resolvedLimit,
          total,
          totalPages: total === 0 ? 0 : Math.ceil(total / resolvedLimit)
        }
      };
    }
  };
}

module.exports = {
  TransactionsError,
  buildTransactionsService
};
