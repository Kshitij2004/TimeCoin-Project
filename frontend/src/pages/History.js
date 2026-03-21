import React, { useEffect, useMemo, useState } from 'react';
import { API_BASE_URL } from '../services/api';
import './History.css';

function formatDate(value) {
  if (!value) {
    return '-';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return String(value);
  }

  return date.toLocaleString();
}

function resolveAuthState(isAuthenticated) {
  if (typeof isAuthenticated === 'boolean') {
    return isAuthenticated;
  }

  return localStorage.getItem('isAuthenticated') === 'true';
}

function resolveUserId(userId) {
  if (Number.isInteger(userId) && userId > 0) {
    return userId;
  }

  const stored = Number(localStorage.getItem('userId'));
  if (Number.isInteger(stored) && stored > 0) {
    return stored;
  }

  return 1;
}

async function fetchTransactionHistory({ apiBaseUrl, userId, page, limit, signal }) {
  const response = await fetch(`${apiBaseUrl}/api/transactions?page=${page}&limit=${limit}`, {
    method: 'GET',
    headers: {
      Authorization: 'Bearer fake-token',
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
      if (payload && payload.message) {
        message = payload.message;
      }
    } catch (error) {
      // Keep the fallback message if parsing fails.
    }
    throw new Error(message);
  }

  return response.json();
}

export default function History({
  apiBaseUrl = API_BASE_URL,
  isAuthenticated = true,
  userId = 1,
  pageSize = 10
}) {
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [rows, setRows] = useState([]);
  const [pagination, setPagination] = useState({ page: 1, limit: pageSize, total: 0, totalPages: 0 });

  const auth = resolveAuthState(isAuthenticated);
  const resolvedUserId = resolveUserId(userId);

  useEffect(() => {
    if (!auth) {
      return undefined;
    }

    const controller = new AbortController();

    const run = async () => {
      setLoading(true);
      setError('');

      try {
        const payload = await fetchTransactionHistory({
          apiBaseUrl,
          userId: resolvedUserId,
          page,
          limit: pageSize,
          signal: controller.signal
        });
        setRows(Array.isArray(payload?.data) ? payload.data : []);
        setPagination(payload?.pagination || { page, limit: pageSize, total: 0, totalPages: 0 });
      } catch (err) {
        if (err.name !== 'AbortError') {
          setError(err.message || 'Failed to fetch transactions');
          setRows([]);
        }
      } finally {
        setLoading(false);
      }
    };

    run();
    return () => controller.abort();
  }, [auth, apiBaseUrl, resolvedUserId, page, pageSize]);

  const canPrev = page > 1 && !loading;
  const canNext = useMemo(() => {
    const totalPages = Number(pagination.totalPages || 0);
    return !loading && totalPages > 0 && page < totalPages;
  }, [loading, pagination.totalPages, page]);

  if (!auth) {
    return (
      <div className="history-page">
        <div className="history-card">
          <h1>Transaction History</h1>
          <p className="history-empty">Please log in to view your transaction history.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="history-page">
      <div className="history-card">
        <div className="history-header">
          <h1>Transaction History</h1>
          <p>Track your most recent coin purchases and sales.</p>
        </div>

        {loading && <p className="history-loading">Loading transactions...</p>}
        {error && <p className="history-error">{error}</p>}

        {!loading && !error && rows.length === 0 && (
          <p className="history-empty">No transactions yet. Your completed purchases will appear here.</p>
        )}

        {!loading && !error && rows.length > 0 && (
          <>
            <table className="history-table" aria-label="Transaction history table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Amount</th>
                  <th>Price At Time</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, idx) => (
                  <tr key={`${row.timestamp}-${row.type}-${idx}`}>
                    <td>{formatDate(row.timestamp)}</td>
                    <td>{row.type}</td>
                    <td>{row.amount}</td>
                    <td>{row.priceAtTime}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            <div className="history-pagination">
              <button type="button" onClick={() => setPage((prev) => Math.max(1, prev - 1))} disabled={!canPrev}>
                Previous
              </button>
              <span>
                Page {pagination.page || page} of {pagination.totalPages || 0}
              </span>
              <button type="button" onClick={() => setPage((prev) => prev + 1)} disabled={!canNext}>
                Next
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
