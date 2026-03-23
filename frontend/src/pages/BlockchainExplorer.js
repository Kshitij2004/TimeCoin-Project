import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { getBlockByHash, getBlockByHeight, getBlocks, getChainStatus } from '../services/blockchainExplorerApi.js';
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
  const [searchParams, setSearchParams] = useSearchParams();

  const initialPage = useMemo(() => {
    const raw = Number(searchParams.get('page'));
    return Number.isInteger(raw) && raw > 0 ? raw : 1;
  }, [searchParams]);

  const initialLookup = useMemo(() => (
    searchParams.get('hash') || searchParams.get('height') || ''
  ), [searchParams]);

  const [status, setStatus] = useState(null);
  const [blocks, setBlocks] = useState([]);
  const [pagination, setPagination] = useState({ page: 1, limit: 10, total: 0, totalPages: 0 });
  const [selectedHeight, setSelectedHeight] = useState(null);
  const [blockDetail, setBlockDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [detailError, setDetailError] = useState('');
  const [inspectingHeight, setInspectingHeight] = useState(null);
  const [lookupValue, setLookupValue] = useState(initialLookup);
  const [copiedKey, setCopiedKey] = useState('');
  const [hydratedFromQuery, setHydratedFromQuery] = useState(false);
  const detailRequestIdRef = useRef(0);
  const detailAbortControllerRef = useRef(null);

  const [page, setPage] = useState(initialPage);
  const [limit] = useState(10);
  const [refreshTick, setRefreshTick] = useState(0);
  const [autoRefresh, setAutoRefresh] = useState(false);

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
  }, [page, limit, refreshTick]);

  useEffect(() => () => {
    if (detailAbortControllerRef.current) {
      detailAbortControllerRef.current.abort();
      detailAbortControllerRef.current = null;
    }
  }, []);

  useEffect(() => {
    if (!copiedKey) {
      return undefined;
    }

    const timeoutId = window.setTimeout(() => {
      setCopiedKey('');
    }, 1500);

    return () => window.clearTimeout(timeoutId);
  }, [copiedKey]);

  useEffect(() => {
    if (!autoRefresh) {
      return undefined;
    }

    const intervalId = window.setInterval(() => {
      setRefreshTick((tick) => tick + 1);
    }, 15000);

    return () => window.clearInterval(intervalId);
  }, [autoRefresh]);

  useEffect(() => {
    if (hydratedFromQuery) {
      return;
    }

    const queryHash = searchParams.get('hash');
    const queryHeight = Number(searchParams.get('height'));

    if (queryHash) {
      setLookupValue(queryHash);
      handleLookupByHashValue(queryHash, false);
    } else if (Number.isInteger(queryHeight) && queryHeight >= 0) {
      setLookupValue(String(queryHeight));
      handleSelectBlock(queryHeight, false);
    }

    setHydratedFromQuery(true);
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hydratedFromQuery, searchParams]);

  const canPrev = page > 1 && !loading;
  const canNext = useMemo(() => {
    const totalPages = Number(pagination?.totalPages || 0);
    return !loading && totalPages > 0 && page < totalPages;
  }, [loading, pagination, page]);

  function updateSearchQuery({ nextPage = page, height = null, hash = null } = {}) {
    const next = new URLSearchParams(searchParams);

    if (Number.isInteger(nextPage) && nextPage > 1) {
      next.set('page', String(nextPage));
    } else {
      next.delete('page');
    }

    if (Number.isInteger(height) && height >= 0) {
      next.set('height', String(height));
      next.delete('hash');
    } else if (typeof hash === 'string' && hash.trim().length > 0) {
      next.set('hash', hash.trim());
      next.delete('height');
    } else {
      next.delete('height');
      next.delete('hash');
    }

    setSearchParams(next, { replace: true });
  }

  function beginDetailRequest() {
    if (detailAbortControllerRef.current) {
      detailAbortControllerRef.current.abort();
    }

    const controller = new AbortController();
    detailAbortControllerRef.current = controller;
    detailRequestIdRef.current += 1;

    return {
      requestId: detailRequestIdRef.current,
      signal: controller.signal
    };
  }

  async function handleSelectBlock(height, syncQuery = true) {
    const { requestId, signal } = beginDetailRequest();
    setSelectedHeight(height);
    setInspectingHeight(height);
    setDetailLoading(true);
    setDetailError('');
    if (syncQuery) {
      updateSearchQuery({ height, hash: null });
    }

    try {
      const detailPayload = await getBlockByHeight({ height, signal });
      if (requestId !== detailRequestIdRef.current) {
        return;
      }
      setBlockDetail(detailPayload);
    } catch (err) {
      if (err.name === 'AbortError' || requestId !== detailRequestIdRef.current) {
        return;
      }
      setDetailError(err.message || 'Failed to load block detail');
      setBlockDetail(null);
    } finally {
      if (requestId === detailRequestIdRef.current) {
        detailAbortControllerRef.current = null;
        setDetailLoading(false);
        setInspectingHeight(null);
      }
    }
  }

  async function handleLookupByHeight() {
    const parsedHeight = Number(lookupValue);
    if (!Number.isInteger(parsedHeight) || parsedHeight < 0) {
      setDetailError('Enter a valid non-negative block height');
      return;
    }
    await handleSelectBlock(parsedHeight, true);
  }

  async function handleLookupByHashValue(value, syncQuery = true) {
    const trimmed = value.trim();
    if (!trimmed) {
      setDetailError('Enter a block hash');
      return;
    }

    const { requestId, signal } = beginDetailRequest();
    setSelectedHeight(null);
    setInspectingHeight(null);
    setDetailLoading(true);
    setDetailError('');
    if (syncQuery) {
      updateSearchQuery({ height: null, hash: trimmed });
    }

    try {
      const detailPayload = await getBlockByHash({ hash: trimmed, signal });
      if (requestId !== detailRequestIdRef.current) {
        return;
      }
      setBlockDetail(detailPayload);
    } catch (err) {
      if (err.name === 'AbortError' || requestId !== detailRequestIdRef.current) {
        return;
      }
      setDetailError(err.message || 'Failed to load block detail');
      setBlockDetail(null);
    } finally {
      if (requestId === detailRequestIdRef.current) {
        detailAbortControllerRef.current = null;
        setDetailLoading(false);
      }
    }
  }

  async function handleLookupByHash() {
    await handleLookupByHashValue(lookupValue, true);
  }

  function handleClearInspector() {
    if (detailAbortControllerRef.current) {
      detailAbortControllerRef.current.abort();
      detailAbortControllerRef.current = null;
    }
    detailRequestIdRef.current += 1;
    setLookupValue('');
    setSelectedHeight(null);
    setBlockDetail(null);
    setDetailError('');
    setInspectingHeight(null);
    setDetailLoading(false);
    updateSearchQuery({ height: null, hash: null });
  }

  function handlePageChange(nextPage) {
    setPage(nextPage);
    updateSearchQuery({
      nextPage,
      height: selectedHeight,
      hash: selectedHeight == null && blockDetail?.blockHash ? blockDetail.blockHash : null
    });
  }

  async function handleCopyHash(hashValue, key) {
    if (!hashValue || typeof hashValue !== 'string') {
      return;
    }

    try {
      if (navigator?.clipboard?.writeText) {
        await navigator.clipboard.writeText(hashValue);
      } else {
        const textArea = document.createElement('textarea');
        textArea.value = hashValue;
        textArea.style.position = 'fixed';
        textArea.style.opacity = '0';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
      }
      setCopiedKey(key);
    } catch (err) {
      setCopiedKey('');
    }
  }

  function renderCopyButton(hashValue, label, key, topRight = false) {
    const isCopied = copiedKey === key;
    return (
      <button
        type="button"
        className={`explorer-copy-btn ${topRight ? 'explorer-copy-btn-top' : ''} ${isCopied ? 'explorer-copy-btn-copied' : ''}`}
        onClick={() => handleCopyHash(hashValue, key)}
        aria-label={label}
      >
        {isCopied ? 'Copied' : 'Copy'}
      </button>
    );
  }

  function renderHashWithCopy(hashValue, label, key) {
    if (!hashValue || typeof hashValue !== 'string') {
      return <span>-</span>;
    }

    return (
      <span className="explorer-hash-cell">
        <span className="explorer-hash">{shortHash(hashValue)}</span>
        {renderCopyButton(hashValue, label, key)}
      </span>
    );
  }

  function renderAddress(addressValue) {
    if (!addressValue || typeof addressValue !== 'string') {
      return '-';
    }

    return (
      <span className="explorer-hash" title={addressValue}>
        {shortHash(addressValue)}
      </span>
    );
  }

  return (
    <div className="explorer-page">
      <div className="explorer-card">
        <div className="explorer-header">
          <div>
            <h1>Blockchain Explorer</h1>
            <p>Inspect the current chain status and recent blocks.</p>
          </div>
          <div className="explorer-header-actions">
            <button
              type="button"
              className={`explorer-auto-btn ${autoRefresh ? 'explorer-auto-btn-on' : ''}`}
              onClick={() => setAutoRefresh((enabled) => !enabled)}
              aria-label={`Auto Refresh: ${autoRefresh ? 'On' : 'Off'}`}
            >
              Auto Refresh: {autoRefresh ? 'On' : 'Off'}
            </button>
            <button
              type="button"
              className="explorer-refresh-btn"
              onClick={() => setRefreshTick((tick) => tick + 1)}
              disabled={loading}
            >
              {loading ? 'Refreshing...' : 'Refresh'}
            </button>
          </div>
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
            <div className="explorer-stat explorer-stat-hash">
              <span className="explorer-stat-label">Latest Hash</span>
              <strong className="explorer-hash">{shortHash(status.latestBlockHash)}</strong>
              {renderCopyButton(status.latestBlockHash, 'Copy latest block hash', 'latest-hash', true)}
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
                        <td>
                          {renderHashWithCopy(
                            block.blockHash,
                            `Copy block hash at height ${block.blockHeight}`,
                            `row-${block.blockHeight}`
                          )}
                        </td>
                        <td>{formatDate(block.timestamp)}</td>
                        <td>
                          {block.transactionCount}
                          {(() => {
                            const isInspecting = detailLoading && inspectingHeight === block.blockHeight;
                            const isSelected = !isInspecting
                              && selectedHeight === block.blockHeight
                              && Boolean(blockDetail)
                              && !detailError;
                            return (
                              <button
                                type="button"
                                className={`explorer-view-btn ${isInspecting ? 'explorer-view-btn-loading' : ''} ${isSelected ? 'explorer-view-btn-selected' : ''}`}
                                onClick={() => handleSelectBlock(block.blockHeight)}
                                disabled={isInspecting}
                                aria-busy={isInspecting ? 'true' : 'false'}
                              >
                                {isInspecting ? 'Inspecting...' : isSelected ? 'Selected' : 'Inspect'}
                              </button>
                            );
                          })()}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>

                <div className="explorer-pagination">
                  <button
                    type="button"
                    onClick={() => handlePageChange(Math.max(1, page - 1))}
                    disabled={!canPrev}
                  >
                    Previous
                  </button>
                  <span>
                    Page {pagination.page || page} of {pagination.totalPages || 0}
                  </span>
                  <button
                    type="button"
                    onClick={() => handlePageChange(page + 1)}
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
          <div className="explorer-lookup">
            <input
              type="text"
              value={lookupValue}
              onChange={(event) => setLookupValue(event.target.value)}
              placeholder="Enter block height or hash"
              aria-label="Block lookup input"
            />
            <button type="button" onClick={handleLookupByHeight}>Find by Height</button>
            <button type="button" onClick={handleLookupByHash}>Find by Hash</button>
            <button
              type="button"
              className="explorer-clear-btn"
              onClick={handleClearInspector}
              aria-label="Clear inspector"
            >
              Clear
            </button>
          </div>

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
                <div className="explorer-stat-hash">
                  <span className="explorer-stat-label">Hash</span>
                  <strong className="explorer-hash">{shortHash(blockDetail.blockHash)}</strong>
                  {renderCopyButton(blockDetail.blockHash, 'Copy selected block hash', 'detail-hash', true)}
                </div>
                <div className="explorer-stat-hash">
                  <span className="explorer-stat-label">Previous Hash</span>
                  <strong className="explorer-hash">{shortHash(blockDetail.previousHash)}</strong>
                  {renderCopyButton(blockDetail.previousHash, 'Copy previous block hash', 'detail-prev-hash', true)}
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
                        <td>
                          {renderHashWithCopy(
                            tx.transactionHash,
                            `Copy transaction hash ${tx.id ?? tx.transactionHash}`,
                            `tx-${tx.id ?? tx.transactionHash}`
                          )}
                        </td>
                        <td>{renderAddress(tx.senderAddress)}</td>
                        <td>{renderAddress(tx.receiverAddress)}</td>
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
