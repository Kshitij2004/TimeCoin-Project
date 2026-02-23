const test = require("node:test");
const assert = require("node:assert/strict");
const {
  buildTransactionsService,
  TransactionsError
} = require("../src/services/transactionsService");

test("getUserTransactions returns paginated transactions ordered by most recent", async () => {
  let call = 0;
  const service = buildTransactionsService({
    query: async (sql, params) => {
      call += 1;
      if (call === 1) {
        assert.match(sql, /COUNT\(\*\)/);
        assert.deepEqual(params, [1]);
        return [{ total: 3 }];
      }

      assert.match(sql, /ORDER BY created_at DESC, id DESC/);
      assert.deepEqual(params, [1, 2, 0]);
      return [
        {
          transaction_type: "BUY",
          quantity: "2.00000000",
          price_usd: "65000.00",
          created_at: "2026-02-23T12:00:00.000Z"
        },
        {
          transaction_type: "SELL",
          quantity: "1.00000000",
          price_usd: "64000.00",
          created_at: "2026-02-23T11:00:00.000Z"
        }
      ];
    }
  });

  const result = await service.getUserTransactions({ userId: 1, page: 1, limit: 2 });

  assert.equal(result.pagination.total, 3);
  assert.equal(result.pagination.totalPages, 2);
  assert.equal(result.pagination.page, 1);
  assert.equal(result.pagination.limit, 2);
  assert.equal(result.data.length, 2);
  assert.equal(result.data[0].type, "BUY");
  assert.equal(result.data[0].amount, "2.00000000");
  assert.equal(result.data[0].priceAtTime, "65000.00");
});

test("getUserTransactions validates pagination", async () => {
  const service = buildTransactionsService({
    query: async () => {
      throw new Error("query should not be called");
    }
  });

  await assert.rejects(
    () => service.getUserTransactions({ userId: 1, page: 0, limit: 20 }),
    (error) => {
      assert.ok(error instanceof TransactionsError);
      assert.equal(error.statusCode, 400);
      assert.equal(error.message, "page must be a positive integer");
      return true;
    }
  );
});

test("getUserTransactions requires authenticated user", async () => {
  const service = buildTransactionsService({
    query: async () => {
      throw new Error("query should not be called");
    }
  });

  await assert.rejects(
    () => service.getUserTransactions({ userId: null, page: 1, limit: 20 }),
    (error) => {
      assert.ok(error instanceof TransactionsError);
      assert.equal(error.statusCode, 401);
      assert.equal(error.message, "Authenticated user is required");
      return true;
    }
  );
});
