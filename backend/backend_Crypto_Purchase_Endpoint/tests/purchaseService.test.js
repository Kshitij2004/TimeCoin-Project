const test = require("node:test");
const assert = require("node:assert/strict");
const { buildPurchaseService, PurchaseError } = require("../src/services/purchaseService");

function createMockConnection(results) {
  const queue = [...results];
  return {
    async execute() {
      if (queue.length === 0) {
        throw new Error("No more mocked query results");
      }
      return queue.shift();
    }
  };
}

test("purchaseCoin succeeds and returns updated wallet balance", async () => {
  const connection = createMockConnection([
    [[{ id: 1 }]],
    [[{ symbol: "TC", current_price_usd: "66000.00", circulating_supply: "10.00000000" }]],
    [{ affectedRows: 1 }],
    [{ affectedRows: 1 }],
    [[{ userId: 1, symbol: "TC", balance: "1.25000000" }]],
    [{ insertId: 42 }],
    [[{
      id: 42,
      userId: 1,
      symbol: "TC",
      transactionType: "BUY",
      quantity: "1.25000000",
      priceUsd: "66000.00",
      totalUsd: "82500.00",
      createdAt: "2026-02-23T00:00:00.000Z"
    }]]
  ]);

  const service = buildPurchaseService({
    withTransaction: async (work) => work(connection)
  });

  const result = await service.purchaseCoin({
    userId: 1,
    symbol: "tc",
    amount: 1.25
  });

  assert.equal(result.message, "Coin purchase successful");
  assert.equal(result.wallet.userId, 1);
  assert.equal(result.wallet.symbol, "TC");
  assert.equal(result.wallet.balance, "1.25000000");
  assert.equal(result.transaction.priceUsd, "66000.00");
});

test("purchaseCoin returns insufficient supply error", async () => {
  const connection = createMockConnection([
    [[{ id: 1 }]],
    [[{ symbol: "TC", current_price_usd: "66000.00", circulating_supply: "0.50000000" }]]
  ]);

  const service = buildPurchaseService({
    withTransaction: async (work) => work(connection)
  });

  await assert.rejects(
    () => service.purchaseCoin({ userId: 1, symbol: "TC", amount: 1 }),
    (error) => {
      assert.ok(error instanceof PurchaseError);
      assert.equal(error.statusCode, 409);
      assert.equal(error.message, "Insufficient circulating supply");
      return true;
    }
  );
});

test("purchaseCoin validates amount", async () => {
  const service = buildPurchaseService({
    withTransaction: async () => {
      throw new Error("Should not call transaction for invalid input");
    }
  });

  await assert.rejects(
    () => service.purchaseCoin({ userId: 1, symbol: "TC", amount: 0 }),
    (error) => {
      assert.ok(error instanceof PurchaseError);
      assert.equal(error.statusCode, 400);
      assert.equal(error.message, "amount must be a positive number");
      return true;
    }
  );
});
