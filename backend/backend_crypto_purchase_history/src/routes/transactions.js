const express = require("express");
const {
  buildTransactionsService,
  TransactionsError
} = require("../services/transactionsService");

const router = express.Router();
const transactionsService = buildTransactionsService();

function getAuthenticatedUserId(req) {
  const raw = req.header("x-user-id");
  const parsed = Number(raw);
  if (!Number.isInteger(parsed) || parsed <= 0) { return null; }
  return parsed;
}

router.get("/", async (req, res) => {
  try {
    const userId = getAuthenticatedUserId(req);
    if (!userId) { return res.status(401).json({ error: "Missing or invalid x-user-id header" }); }
    const result = await transactionsService.getUserTransactions({
      userId,
      page: req.query.page,
      limit: req.query.limit
    });
    return res.status(200).json(result);
  } catch (error) {
    if (error instanceof TransactionsError) { return res.status(error.statusCode).json({ error: error.message }); }
    return res.status(500).json({ error: "Failed to fetch transactions" });
  }
});

module.exports = router;
