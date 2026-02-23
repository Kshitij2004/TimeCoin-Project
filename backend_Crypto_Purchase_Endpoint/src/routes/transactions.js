const express = require("express");
const { query } = require("../db");
const router = express.Router();

router.post("/buy", async (req, res) => {
  try {
    const { userId, symbol, quantity, priceUsd } = req.body;
    if (!Number.isInteger(userId) || userId <= 0) { return res.status(400).json({ error: "userId must be a positive integer" }); }
    if (typeof symbol !== "string" || symbol.trim().length === 0 || symbol.length > 10) { return res.status(400).json({ error: "symbol must be a non-empty string up to 10 chars" }); }
    if (typeof quantity !== "number" || quantity <= 0) { return res.status(400).json({ error: "quantity must be a positive number" }); }
    if (typeof priceUsd !== "number" || priceUsd <= 0) { return res.status(400).json({ error: "priceUsd must be a positive number" }); }

    const totalUsd = Number((quantity * priceUsd).toFixed(2));
    const normalizedSymbol = symbol.trim().toUpperCase();
    const insertResult = await query(
      `INSERT INTO transactions
      (user_id, symbol, transaction_type, quantity, price_usd, total_usd)
      VALUES (?, ?, 'BUY', ?, ?, ?)`,
      [userId, normalizedSymbol, quantity, priceUsd, totalUsd]
    );

    const created = await query(
      `SELECT id, user_id AS userId, symbol, transaction_type AS transactionType,
      quantity, price_usd AS priceUsd, total_usd AS totalUsd, created_at AS createdAt
      FROM transactions
      WHERE id = ?`,
      [insertResult.insertId]
    );

    return res.status(201).json({
      message: "Buy transaction created",
      transaction: created[0]
    });
  } catch (error) {
    if (error && error.code === "ER_NO_REFERENCED_ROW_2") { return res.status(404).json({ error: "User not found" }); }
    return res.status(500).json({ error: "Failed to create buy transaction" });
  }
});

module.exports = router;
