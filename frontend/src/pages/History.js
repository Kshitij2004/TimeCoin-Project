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

function tryResolveUserIdFromToken(token) {
  if (!token || typeof token !== 'string') {
    return null;
  }

  try {
    const tokenParts = token.split('.');
    if (tokenParts.length < 2) {
      return null;
    }

    const base64Payload = tokenParts[1].replace(/-/g, '+').replace(/_/g, '/');
    const normalized = base64Payload.padEnd(
      base64Payload.length + ((4 - (base64Payload.length % 4)) % 4),
      '='
    );
    const payload = JSON.parse(window.atob(normalized));
    const tokenUserId = Number(payload?.sub);
    return Number.isInteger(tokenUserId) && tokenUserId > 0 ? tokenUserId : null;
  } catch (error) {
    return null;
  }
}

function resolveUserId(userId, token) {
  if (Number.isInteger(userId) && userId > 0) {
    return userId;
  }

  const stored = Number(localStorage.getItem('userId'));
  if (Number.isInteger(stored) && stored > 0) {
    return stored;
  }

  const fromToken = tryResolveUserIdFromToken(token);
  if (Number.isInteger(fromToken) && fromToken > 0) {
    return fromToken;
  }

  return null;
}

function resolveAuthToken(authToken) {
  if (typeof authToken === 'string' && authToken.trim().length > 0) {
    return authToken.trim();
  }

  const candidates = [
    localStorage.getItem('token'),
    localStorage.getItem('authToken'),
    localStorage.getItem('jwt')
  ];

  const found = candidates.find((value) => typeof value === 'string' && value.trim().length > 0);
  return found ? found.trim() : null;
}

async function fetchTransactionHistory({ apiBaseUrl, userId, page, limit, token, signal }) {
  const response = await fetch(`${apiBaseUrl}/api/transactions?page=${page}&limit=${limit}`, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
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
  authToken = null,
  userId = 1,
  pageSize = 10
}) {
  const [page, setPage] = useState(1);
  const [refreshTick, setRefreshTick] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [rows, setRows] = useState([]);
  const [pagination, setPagination] = useState({ page: 1, limit: pageSize, total: 0, totalPages: 0 });

  const auth = resolveAuthState(isAuthenticated);
  const resolvedToken = resolveAuthToken(authToken);
  const resolvedUserId = resolveUserId(userId, resolvedToken);
  const hasSession = auth && Boolean(resolvedToken) && Boolean(resolvedUserId);

  useEffect(() => {
    if (!hasSession) {
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
          token: resolvedToken,
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
  }, [hasSession, apiBaseUrl, resolvedUserId, page, pageSize, refreshTick, resolvedToken]);

  const canPrev = page > 1 && !loading;
  const canNext = useMemo(() => {
    const totalPages = Number(pagination.totalPages || 0);
    return !loading && totalPages > 0 && page < totalPages;
  }, [loading, pagination.totalPages, page]);

  if (!hasSession) {
    const missingToken = auth && !resolvedToken;
    const missingUserId = auth && resolvedToken && !resolvedUserId;
    return (
      <div className="history-page">
        <div className="history-card">
          <div className="history-header">
            <h1>Transaction History</h1>
            <p>Track your most recent coin purchases and sales.</p>
          </div>
          <p className="history-empty">
            {missingToken
              ? 'No JWT token found. Save your login token in localStorage as token/authToken/jwt.'
              : missingUserId
                ? 'Unable to resolve user ID from props/localStorage/JWT subject.'
                : 'Please log in to view your transaction history.'}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="history-page">
      <div className="history-card">
        <div className="history-header">
          <div>
            <h1>Transaction History</h1>
            <p>Track your most recent coin purchases and sales.</p>
          </div>
          <div className="history-header-actions">
            <button
              type="button"
              className="history-refresh-btn"
              onClick={() => setRefreshTick((tick) => tick + 1)}
              disabled={loading}
            >
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
        </div>

        <section className="history-status-grid" aria-label="History summary">
          <div className="history-stat">
            <span className="history-stat-label">User ID</span>
            <strong>{resolvedUserId}</strong>
          </div>
          <div className="history-stat">
            <span className="history-stat-label">Total Transactions</span>
            <strong>{pagination.total || 0}</strong>
          </div>
          <div className="history-stat">
            <span className="history-stat-label">Current Page</span>
            <strong>{pagination.page || page}</strong>
          </div>
          <div className="history-stat">
            <span className="history-stat-label">Page Size</span>
            <strong>{pagination.limit || pageSize}</strong>
          </div>
        </section>

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
