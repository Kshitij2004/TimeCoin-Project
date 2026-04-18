import React, { useEffect, useRef, useState } from 'react';

/**
 * BlockChainDiagram
 */
export default function BlockChainDiagram({
  blocks = [],
  selectedHeight,
  inspectingHeight,
  onSelectBlock,
  newBlockHeight,
}) {
  const scrollRef = useRef(null);
  const [animatedHeights, setAnimatedHeights] = useState(new Set());

  // Auto-scroll right when new blocks arrive
  // Guard against jsdom in tests where scrollTo is not implemented
  useEffect(() => {
    if (scrollRef.current && typeof scrollRef.current.scrollTo === 'function') {
      scrollRef.current.scrollTo({ left: scrollRef.current.scrollWidth, behavior: 'smooth' });
    }
  }, [blocks.length]);

  // Track which heights have played their entrance animation
  useEffect(() => {
    if (newBlockHeight != null) {
      setAnimatedHeights((prev) => new Set([...prev, newBlockHeight]));
    }
  }, [newBlockHeight]);

  if (!blocks.length) {
    return null;
  }

  // Blocks arrive newest-first from the API; we want oldest→newest left→right
  const ordered = [...blocks].sort((a, b) => a.blockHeight - b.blockHeight);

  return (
    <div className="chain-diagram-wrapper" role="region" aria-label="Visual block chain diagram">
      <div className="chain-diagram-scroll" ref={scrollRef}>
        <div className="chain-diagram-track">
          {ordered.map((block, idx) => {
            const isSelected = selectedHeight === block.blockHeight;
            const isInspecting = inspectingHeight === block.blockHeight;
            const isNew = animatedHeights.has(block.blockHeight);
            const status = (block.status || 'COMMITTED').toUpperCase();

            // Determine connector colour from the *source* block's status
            const connectorStatus = status;

            return (
              <React.Fragment key={block.blockHash || block.blockHeight}>
                <BlockNode
                  block={block}
                  status={status}
                  isSelected={isSelected}
                  isInspecting={isInspecting}
                  isNew={isNew}
                  onSelect={onSelectBlock}
                />
                {idx < ordered.length - 1 && (
                  <ChainConnector status={connectorStatus} />
                )}
              </React.Fragment>
            );
          })}
        </div>
      </div>

      {/* Legend */}
      <div className="chain-diagram-legend" aria-label="Status legend">
        <LegendItem status="COMMITTED" label="Committed" />
        <LegendItem status="PENDING" label="Pending" />
        <LegendItem status="ORPHANED" label="Orphaned" />
      </div>
    </div>
  );
}

// Block node

function BlockNode({ block, status, isSelected, isInspecting, isNew, onSelect }) {
  const [hovered, setHovered] = useState(false);

  const label = isInspecting ? 'Inspecting…' : isSelected ? 'Selected' : 'Inspect';
  const btnClass = [
    'chain-node-btn',
    isInspecting ? 'chain-node-btn--loading' : '',
    isSelected && !isInspecting ? 'chain-node-btn--selected' : '',
  ].filter(Boolean).join(' ');

  return (
    <div
      className={[
        'chain-node',
        `chain-node--${status.toLowerCase()}`,
        isSelected ? 'chain-node--selected' : '',
        isNew ? 'chain-node--new' : '',
        hovered ? 'chain-node--hovered' : '',
      ].filter(Boolean).join(' ')}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onClick={() => !isInspecting && onSelect && onSelect(block.blockHeight)}
      role="button"
      tabIndex={0}
      aria-label={`Block ${block.blockHeight}, ${block.transactionCount} transactions, status ${status}`}
      aria-pressed={isSelected}
      onKeyDown={(e) => {
        if ((e.key === 'Enter' || e.key === ' ') && !isInspecting) {
          e.preventDefault();
          onSelect && onSelect(block.blockHeight);
        }
      }}
    >
      {/* Status stripe at top */}
      <div className="chain-node-stripe" aria-hidden="true" />

      {/* Height */}
      <div className="chain-node-height">#{block.blockHeight}</div>

      {/* Truncated hash */}
      <div className="chain-node-hash" title={block.blockHash}>
        {shortHash(block.blockHash)}
      </div>

      {/* Divider */}
      <div className="chain-node-divider" aria-hidden="true" />

      {/* Tx count */}
      <div className="chain-node-meta">
        <span className="chain-node-meta-label">TXS</span>
        <span className="chain-node-meta-value">{block.transactionCount ?? 0}</span>
      </div>

      {/* Status badge */}
      <div className="chain-node-status">
        <span className={`chain-node-dot chain-node-dot--${status.toLowerCase()}`} aria-hidden="true" />
        <span className="chain-node-status-label">{status}</span>
      </div>

      {/* Inspect button */}
      <button
        type="button"
        className={btnClass}
        disabled={isInspecting}
        aria-busy={isInspecting ? 'true' : 'false'}
        onClick={(e) => {
          e.stopPropagation();
          if (!isInspecting) onSelect && onSelect(block.blockHeight);
        }}
      >
        {label}
      </button>
    </div>
  );
}

// Connector arrow

function ChainConnector({ status }) {
  const s = status.toLowerCase();
  return (
    <div className={`chain-connector chain-connector--${s}`} aria-hidden="true">
      <div className="chain-connector-line" />
      <div className="chain-connector-arrow" />
    </div>
  );
}

// Legend item

function LegendItem({ status, label }) {
  const s = status.toLowerCase();
  return (
    <div className="chain-legend-item">
      <span className={`chain-node-dot chain-node-dot--${s}`} aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

// Helpers

function shortHash(hash) {
  if (!hash || typeof hash !== 'string') return '—';
  if (hash.length <= 14) return hash;
  return `${hash.slice(0, 6)}…${hash.slice(-4)}`;
}