import React, { useEffect, useMemo, useState } from 'react';
import api from '../services/api.js'; // Using our centralized client
import './History.css';

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

export default function History({ pageSize = 10 }) {
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [rows, setRows] = useState([]);
  const [pagination, setPagination] = useState({ 
    page: 1, 
    limit: pageSize, 
    total: 0, 
    totalPages: 0 
  });

  // Check for token to determine if we should even attempt a fetch
  const token = localStorage.getItem('token');

  useEffect(() => {
    // FIX: If no token exists, do not attempt to fetch data.
    // This prevents the "Failed to fetch" error from overriding the login message.
    if (!token) return;

    const controller = new AbortController();

    const fetchHistory = async () => {
      setLoading(true);
      setError('');

      try {
        const response = await api.get(`/transactions?page=${page}&limit=${pageSize}`, {
          signal: controller.signal
        });

        const payload = response.data;
        setRows(Array.isArray(payload?.data) ? payload.data : []);
        setPagination(payload?.pagination || { 
          page, 
          limit: pageSize, 
          total: 0, 
          totalPages: 0 
        });
      } catch (err) {
        if (err.name !== 'CanceledError') {
          setError(err.response?.data?.message || 'Failed to fetch transactions');
          setRows([]);
        }
      } finally {
        setLoading(false);
      }
    };

    fetchHistory();
    return () => controller.abort();
  }, [page, pageSize, token]);

  const canPrev = page > 1 && !loading;
  const canNext = useMemo(() => {
    const totalPages = Number(pagination.totalPages || 0);
    return !loading && totalPages > 0 && page < totalPages;
  }, [loading, pagination.totalPages, page]);

  return (
    <div className="history-page">
      <div className="history-card">
        <div className="history-header">
          <h1>Transaction History</h1>
          <p>Track your most recent coin purchases and sales.</p>
        </div>

        {/* If there is no token, show the Login Message. 
           Otherwise, show the loading/error/table states.
        */}
        {!token ? (
          <p className="history-empty">Please log in to view your transaction history.</p>
        ) : (
          <>
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
                  <button 
                    type="button" 
                    onClick={() => setPage((prev) => Math.max(1, prev - 1))} 
                    disabled={!canPrev}
                  >
                    Previous
                  </button>
                  <span>
                    Page {pagination.page || page} of {pagination.totalPages || 0}
                  </span>
                  <button 
                    type="button" 
                    onClick={() => setPage((prev) => prev + 1)} 
                    disabled={!canNext}
                  >
                    Next
                  </button>
                </div>
              </>
            )}
          </>
        )}
      </div>
    </div>
  );
}