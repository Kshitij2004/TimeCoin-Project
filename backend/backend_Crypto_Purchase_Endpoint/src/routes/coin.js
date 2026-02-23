const express = require("express");
const { buildPurchaseService, PurchaseError } = require("../services/purchaseService");

const router = express.Router();
const purchaseService = buildPurchaseService();

router.post("/buy", async (req, res) => {
  try {
    const { userId, symbol, amount } = req.body;
    const result = await purchaseService.purchaseCoin({ userId, symbol, amount });
    return res.status(201).json(result);
  } catch (error) {
    if (error instanceof PurchaseError) {
      return res.status(error.statusCode).json({ error: error.message });
    }
    return res.status(500).json({ error: "Failed to purchase coin" });
  }
});

module.exports = router;
