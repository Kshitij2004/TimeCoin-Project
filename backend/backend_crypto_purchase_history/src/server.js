require("dotenv").config();

const express = require("express");
const { query } = require("./db");
const transactionsRouter = require("./routes/transactions");

const app = express();
const port = Number(process.env.PORT || 3000);

app.use(express.json());

app.get("/health", async (req, res) => {
  try {
    await query("SELECT 1");
    return res.status(200).json({ status: "ok" });
  } catch (error) {
    return res.status(500).json({ status: "error" });
  }
});

app.use("/api/transactions", transactionsRouter);

app.listen(port, () => {
  console.log(`Backend listening on port ${port}`);
});
