import React, { useEffect, useMemo, useState } from 'react';
import { getBlocks, getChainStatus } from '../services/blockchainExplorerApi';
import './BlockchainExplorer.css';

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

function shortHash(hash) {
  if (!hash || typeof hash !== 'string') {
    return '-';
  }
  if (hash.length <= 16) {
    return hash;
  }
  return `${hash.slice(0, 8)}...${hash.slice(-8)}`;
}

export default function BlockchainExplorer() {
  const [status, setStatus] = useState(null);
  const [blocks, setBlocks] = useState([]);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0, totalPages: 0 });

  const [page, setPage] = useState(1);
  const [limit] = useState(10);

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    const run = async () => {
      setLoading(true);
      setError('');
      try {
        const [statusPayload, blocksPayload] = await Promise.all([
          getChainStatus({ signal: controller.signal }),
          getBlocks({ page, limit, signal: controller.signal })
        ]);

        setStatus(statusPayload);
        setBlocks(Array.isArray(blocksPayload?.data) ? blocksPayload.data : []);
        setPagination(blocksPayload?.pagination || { page, limit, total: 0, totalPages: 0 });
      } catch (err) {
        if (err.name !== 'AbortError') {
          setError(err.message || 'Failed to fetch blockchain explorer data');
          setStatus(null);
          setBlocks([]);
        }
      } finally {
        setLoading(false);
      }
    };

    run();
    return () => controller.abort();
  }, [page, limit]);

  const canPrev = page > 1 && !loading;
  const canNext = useMemo(() => {
    const totalPages = Number(pagination?.totalPages || 0);
    return !loading && totalPages > 0 && page < totalPages;
  }, [loading, pagination, page]);

  return (
    <div className="explorer-page">
      <div className="explorer-card">
        <div className="explorer-header">
          <h1>Blockchain Explorer</h1>
          <p>Inspect the current chain status and recent blocks.</p>
        </div>

        {status && (
          <section className="explorer-status-grid" aria-label="Chain status summary">
            <div className="explorer-stat">
              <span className="explorer-stat-label">Latest Height</span>
              <strong>{status.latestBlockHeight ?? '-'}</strong>
            </div>
            <div className="explorer-stat">
              <span className="explorer-stat-label">Total Blocks</span>
              <strong>{status.totalBlocks ?? 0}</strong>
            </div>
            <div className="explorer-stat">
              <span className="explorer-stat-label">Pending Transactions</span>
              <strong>{status.pendingTransactions ?? 0}</strong>
            </div>
            <div className="explorer-stat">
              <span className="explorer-stat-label">Latest Hash</span>
              <strong className="explorer-hash">{shortHash(status.latestBlockHash)}</strong>
            </div>
          </section>
        )}

        {loading && <p className="explorer-loading">Loading blockchain data...</p>}
        {error && <p className="explorer-error">{error}</p>}

        {!loading && !error && (
          <section>
            {blocks.length === 0 ? (
              <p className="explorer-empty">No blocks available.</p>
            ) : (
              <>
                <table className="explorer-table" aria-label="Recent blocks table">
                  <thead>
                    <tr>
                      <th>Height</th>
                      <th>Hash</th>
                      <th>Timestamp</th>
                      <th>Transactions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {blocks.map((block) => (
                      <tr key={`${block.blockHeight}-${block.blockHash}`}>
                        <td>{block.blockHeight}</td>
                        <td className="explorer-hash">{shortHash(block.blockHash)}</td>
                        <td>{formatDate(block.timestamp)}</td>
                        <td>{block.transactionCount}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                <div className="explorer-pagination">
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
          </section>
        )}
      </div>
    </div>
  );
}
