const { withTransaction } = require("../db");

class PurchaseError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.name = "PurchaseError";
    this.statusCode = statusCode;
  }
}

function normalizeSymbol(symbol) {
  return String(symbol || "").trim().toUpperCase();
}

function validatePurchaseInput({ userId, symbol, amount }) {
  if (!Number.isInteger(userId) || userId <= 0) {
    throw new PurchaseError("userId must be a positive integer", 400);
  }

  if (typeof symbol !== "string" || symbol.trim().length === 0 || symbol.trim().length > 10) {
    throw new PurchaseError("symbol must be a non-empty string up to 10 chars", 400);
  }

  if (typeof amount !== "number" || amount <= 0) {
    throw new PurchaseError("amount must be a positive number", 400);
  }
}

function buildPurchaseService(deps = {}) {
  const runWithTransaction = deps.withTransaction || withTransaction;

  return {
    async purchaseCoin({ userId, symbol, amount }) {
      validatePurchaseInput({ userId, symbol, amount });
      const normalizedSymbol = normalizeSymbol(symbol);

      return runWithTransaction(async (connection) => {
        const [users] = await connection.execute(
          "SELECT id FROM users WHERE id = ?",
          [userId]
        );
        if (users.length === 0) {
          throw new PurchaseError("User not found", 404);
        }

        const [coins] = await connection.execute(
          "SELECT symbol, current_price_usd, circulating_supply FROM coins WHERE symbol = ? FOR UPDATE",
          [normalizedSymbol]
        );
        if (coins.length === 0) {
          throw new PurchaseError("Coin not found", 404);
        }

        const coin = coins[0];
        const circulatingSupply = Number(coin.circulating_supply);
        const coinPriceUsd = Number(coin.current_price_usd);

        if (circulatingSupply < amount) {
          throw new PurchaseError("Insufficient circulating supply", 409);
        }

        await connection.execute(
          "UPDATE coins SET circulating_supply = circulating_supply - ? WHERE symbol = ?",
          [amount, normalizedSymbol]
        );

        await connection.execute(
          `INSERT INTO wallets (user_id, symbol, balance)
          VALUES (?, ?, ?)
          ON DUPLICATE KEY UPDATE balance = balance + VALUES(balance)`,
          [userId, normalizedSymbol, amount]
        );

        const [walletRows] = await connection.execute(
          "SELECT user_id AS userId, symbol, balance FROM wallets WHERE user_id = ? AND symbol = ?",
          [userId, normalizedSymbol]
        );

        const totalUsd = Number((amount * coinPriceUsd).toFixed(2));
        const [insertResult] = await connection.execute(
          `INSERT INTO transactions
          (user_id, symbol, transaction_type, quantity, price_usd, total_usd)
          VALUES (?, ?, 'BUY', ?, ?, ?)`,
          [userId, normalizedSymbol, amount, coinPriceUsd, totalUsd]
        );

        const [transactionRows] = await connection.execute(
          `SELECT id, user_id AS userId, symbol, transaction_type AS transactionType,
          quantity, price_usd AS priceUsd, total_usd AS totalUsd, created_at AS createdAt
          FROM transactions
          WHERE id = ?`,
          [insertResult.insertId]
        );

        return {
          message: "Coin purchase successful",
          transaction: transactionRows[0],
          wallet: walletRows[0]
        };
      });
    }
  };
}

module.exports = {
  PurchaseError,
  buildPurchaseService
};
