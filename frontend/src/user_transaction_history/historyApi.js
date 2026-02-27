export async function fetchTransactionHistory({ apiBaseUrl, userId, page, limit, signal }) {
  const response = await fetch(`${apiBaseUrl}/api/transactions?page=${page}&limit=${limit}`, {
    method: 'GET',
    headers: {
      'x-user-id': String(userId)
    },
    signal
  });

  if (!response.ok) {
    let message = 'Failed to fetch transactions';
    try {
      const payload = await response.json();
      if (payload && payload.error) {
        message = payload.error;
      }
    } catch (error) {
      // Ignore parse failures and keep fallback message.
    }
    throw new Error(message);
  }

  return response.json();
}
