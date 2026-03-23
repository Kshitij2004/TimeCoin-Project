import {
  getBlockByHash,
  getBlockByHeight,
  getBlocks,
  getChainStatus
} from './blockchainExplorerApi';

beforeEach(() => {
  global.fetch = jest.fn();
});

afterEach(() => {
  jest.resetAllMocks();
});

test('getChainStatus requests chain status endpoint', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ totalBlocks: 1, latestBlockHeight: 0 })
  });

  const result = await getChainStatus({ apiBaseUrl: 'http://localhost:8080' });

  expect(fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/chain/status',
    expect.objectContaining({ method: 'GET' })
  );
  expect(result.totalBlocks).toBe(1);
});

test('getBlocks requests paginated block listing endpoint', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ data: [], pagination: { page: 2, limit: 5, total: 0, totalPages: 0 } })
  });

  const result = await getBlocks({ apiBaseUrl: 'http://localhost:8080', page: 2, limit: 5 });

  expect(fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/chain/blocks?page=2&limit=5',
    expect.objectContaining({ method: 'GET' })
  );
  expect(result.pagination.page).toBe(2);
  expect(result.pagination.limit).toBe(5);
});

test('getBlockByHeight validates non-negative integer height', async () => {
  expect(() => getBlockByHeight({ height: -1 })).toThrow('height must be a non-negative integer');
  expect(fetch).not.toHaveBeenCalled();
});

test('getBlockByHeight requests block detail endpoint', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ blockHeight: 0, transactions: [] })
  });

  const result = await getBlockByHeight({ apiBaseUrl: 'http://localhost:8080', height: 0 });

  expect(fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/chain/blocks/0',
    expect.objectContaining({ method: 'GET' })
  );
  expect(result.blockHeight).toBe(0);
});

test('getBlockByHash validates required hash', async () => {
  expect(() => getBlockByHash({ hash: '' })).toThrow('hash is required');
  expect(fetch).not.toHaveBeenCalled();
});

test('getBlockByHash encodes hash path segment', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ blockHash: 'a/b c' })
  });

  await getBlockByHash({ apiBaseUrl: 'http://localhost:8080', hash: 'a/b c' });

  expect(fetch).toHaveBeenCalledWith(
    'http://localhost:8080/api/chain/blocks/hash/a%2Fb%20c',
    expect.objectContaining({ method: 'GET' })
  );
});

test('request throws backend error message when response is not ok', async () => {
  fetch.mockResolvedValueOnce({
    ok: false,
    json: async () => ({ message: 'page must be a positive integer' })
  });

  await expect(getBlocks({ apiBaseUrl: 'http://localhost:8080', page: 0, limit: 20 }))
    .rejects
    .toThrow('page must be a positive integer');
});
