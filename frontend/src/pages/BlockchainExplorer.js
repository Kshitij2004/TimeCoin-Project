import React, { useEffect, useMemo, useState } from 'react';
import { getBlockByHeight, getBlocks, getChainStatus } from '../services/blockchainExplorerApi';
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
  const [selectedHeight, setSelectedHeight] = useState(null);
  const [blockDetail, setBlockDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');

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
        setSelectedHeight(null);
        setBlockDetail(null);
        setDetailError('');
      } catch (err) {
        if (err.name !== 'AbortError') {
          setError(err.message || 'Failed to fetch blockchain explorer data');
          setStatus(null);
          setBlocks([]);
          setSelectedHeight(null);
          setBlockDetail(null);
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

  async function handleSelectBlock(height) {
    setSelectedHeight(height);
    setDetailLoading(true);
    setDetailError('');

    try {
      const detailPayload = await getBlockByHeight({ height });
      setBlockDetail(detailPayload);
    } catch (err) {
      setDetailError(err.message || 'Failed to load block detail');
      setBlockDetail(null);
    } finally {
      setDetailLoading(false);
    }
  }

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
                      <tr
                        key={`${block.blockHeight}-${block.blockHash}`}
                        className={selectedHeight === block.blockHeight ? 'explorer-row-selected' : ''}
                      >
                        <td>{block.blockHeight}</td>
                        <td className="explorer-hash">{shortHash(block.blockHash)}</td>
                        <td>{formatDate(block.timestamp)}</td>
                        <td>
                          {block.transactionCount}
                          <button
                            type="button"
                            className="explorer-view-btn"
                            onClick={() => handleSelectBlock(block.blockHeight)}
                          >
                            Inspect
                          </button>
                        </td>
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

        <section className="explorer-detail-panel" aria-label="Block inspector">
          <h2>Block Inspector</h2>

          {!selectedHeight && !detailLoading && !detailError && (
            <p className="explorer-detail-empty">Select a block row to inspect linked transactions.</p>
          )}

          {detailLoading && <p className="explorer-loading">Loading block detail...</p>}
          {detailError && <p className="explorer-error">{detailError}</p>}

          {blockDetail && !detailLoading && !detailError && (
            <>
              <div className="explorer-detail-grid">
                <div>
                  <span className="explorer-stat-label">Height</span>
                  <strong>{blockDetail.blockHeight}</strong>
                </div>
                <div>
                  <span className="explorer-stat-label">Status</span>
                  <strong>{blockDetail.status}</strong>
                </div>
                <div>
                  <span className="explorer-stat-label">Hash</span>
                  <strong className="explorer-hash">{shortHash(blockDetail.blockHash)}</strong>
                </div>
                <div>
                  <span className="explorer-stat-label">Previous Hash</span>
                  <strong className="explorer-hash">{shortHash(blockDetail.previousHash)}</strong>
                </div>
              </div>

              {Array.isArray(blockDetail.transactions) && blockDetail.transactions.length > 0 ? (
                <table className="explorer-table" aria-label="Linked transactions table">
                  <thead>
                    <tr>
                      <th>Tx Hash</th>
                      <th>Sender</th>
                      <th>Receiver</th>
                      <th>Amount</th>
                      <th>Fee</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {blockDetail.transactions.map((tx) => (
                      <tr key={tx.transactionHash || tx.id}>
                        <td className="explorer-hash">{shortHash(tx.transactionHash)}</td>
                        <td>{tx.senderAddress || '-'}</td>
                        <td>{tx.receiverAddress || '-'}</td>
                        <td>{tx.amount ?? '-'}</td>
                        <td>{tx.fee ?? '-'}</td>
                        <td>{tx.status || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              ) : (
                <p className="explorer-detail-empty">This block has no linked transactions.</p>
              )}
            </>
          )}
        </section>
      </div>
    </div>
  );
}
