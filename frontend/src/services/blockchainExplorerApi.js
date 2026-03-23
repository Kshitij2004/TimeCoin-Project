import { API_BASE_URL } from './api.js';

async function parseJson(response) {
  try {
    return await response.json();
  } catch (error) {
    return {};
  }
}

async function request(url, { signal } = {}) {
  const response = await fetch(url, {
    method: 'GET',
    signal
  });

  const payload = await parseJson(response);
  if (!response.ok) {
    const message = payload?.message || payload?.error || 'Failed to fetch blockchain data';
    throw new Error(message);
  }
  return payload;
}

export function getChainStatus({ apiBaseUrl = API_BASE_URL, signal } = {}) {
  return request(`${apiBaseUrl}/api/chain/status`, { signal });
}

export function getBlocks({
  page = 1,
  limit = 20,
  apiBaseUrl = API_BASE_URL,
  signal
} = {}) {
  return request(`${apiBaseUrl}/api/chain/blocks?page=${page}&limit=${limit}`, { signal });
}

export function getBlockByHeight({
  height,
  apiBaseUrl = API_BASE_URL,
  signal
} = {}) {
  if (!Number.isInteger(height) || height < 0) {
    throw new Error('height must be a non-negative integer');
  }

  return request(`${apiBaseUrl}/api/chain/blocks/${height}`, { signal });
}

export function getBlockByHash({
  hash,
  apiBaseUrl = API_BASE_URL,
  signal
} = {}) {
  if (!hash || typeof hash !== 'string') {
    throw new Error('hash is required');
  }

  return request(`${apiBaseUrl}/api/chain/blocks/hash/${encodeURIComponent(hash)}`, { signal });
}
