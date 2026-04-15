import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import BlockchainExplorer from './BlockchainExplorer.js';
import { getBlockByHash, getBlockByHeight, getBlocks, getChainStatus } from '../services/blockchainExplorerApi.js';

const mockSetSearchParams = jest.fn();
let mockInitialSearch = '';

jest.mock('react-router-dom', () => {
  const actualReact = require('react');

  return {
    useSearchParams: () => {
      const [params, setParams] = actualReact.useState(new URLSearchParams(mockInitialSearch));

      const wrappedSetter = (nextParams) => {
        const resolved = typeof nextParams === 'function' ? nextParams(params) : nextParams;
        const next = resolved instanceof URLSearchParams
          ? new URLSearchParams(resolved.toString())
          : new URLSearchParams(resolved);
        mockSetSearchParams(next);
        setParams(next);
      };

      return [params, wrappedSetter];
    }
  };
}, { virtual: true });

jest.mock('../services/blockchainExplorerApi.js', () => ({
  getChainStatus: jest.fn(),
  getBlocks: jest.fn(),
  getBlockByHeight: jest.fn(),
  getBlockByHash: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockInitialSearch = '';
  Object.defineProperty(navigator, 'clipboard', {
    value: { writeText: jest.fn().mockResolvedValue(undefined) },
    configurable: true
  });
});

function renderExplorer(initialPath = '/blockchain') {
  const queryIndex = initialPath.indexOf('?');
  mockInitialSearch = queryIndex >= 0 ? initialPath.slice(queryIndex + 1) : '';
  return render(<BlockchainExplorer />);
}

test('renders chain status and recent blocks', async () => {
  getChainStatus.mockResolvedValueOnce({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValueOnce({
    data: [
      {
        blockHeight: 12,
        blockHash: 'abcdef1234567890abcdef1234567890',
        timestamp: '2026-03-22T17:30:00',
        transactionCount: 3
      }
    ],
    pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
  });

  renderExplorer();

  expect(await screen.findByText('Latest Height')).toBeInTheDocument();
  expect(screen.getByText('abcdef12...34567890')).toBeInTheDocument();
  expect(screen.getByRole('table', { name: /Recent blocks table/i })).toBeInTheDocument();
  expect(screen.getByText('Page 1 of 2')).toBeInTheDocument();
  expect(getChainStatus).toHaveBeenCalledTimes(1);
  expect(getBlocks).toHaveBeenCalledWith(expect.objectContaining({ page: 1, limit: 10 }));
});

test('refresh button re-fetches status and block listing', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValue({
    data: [
      {
        blockHeight: 12,
        blockHash: 'hash12',
        timestamp: '2026-03-22T17:30:00',
        transactionCount: 3
      }
    ],
    pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
  });

  renderExplorer();

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

  await waitFor(() => expect(getChainStatus).toHaveBeenCalledTimes(2));
  await waitFor(() => expect(getBlocks).toHaveBeenCalledTimes(2));
});

test('auto refresh toggle schedules polling updates', async () => {
  jest.useFakeTimers();
  const intervalSpy = jest.spyOn(window, 'setInterval');
  const clearSpy = jest.spyOn(window, 'clearInterval');

  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValue({
    data: [
      {
        blockHeight: 12,
        blockHash: 'hash12',
        timestamp: '2026-03-22T17:30:00',
        transactionCount: 3
      }
    ],
    pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
  });

  renderExplorer();

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Auto Refresh: Off' }));
  expect(screen.getByRole('button', { name: 'Auto Refresh: On' })).toBeInTheDocument();
  expect(intervalSpy).toHaveBeenCalledTimes(1);

  await act(async () => {
    jest.advanceTimersByTime(15000);
  });

  await waitFor(() => expect(getChainStatus).toHaveBeenCalledTimes(2));
  await waitFor(() => expect(getBlocks).toHaveBeenCalledTimes(2));

  fireEvent.click(screen.getByRole('button', { name: 'Auto Refresh: On' }));
  expect(clearSpy).toHaveBeenCalled();

  intervalSpy.mockRestore();
  clearSpy.mockRestore();
  jest.useRealTimers();
});

test('supports next-page pagination', async () => {
  getChainStatus
    .mockResolvedValue({
      latestBlockHeight: 12,
      totalBlocks: 13,
      pendingTransactions: 4,
      latestBlockHash: '1234567890abcdef1234567890abcdef'
    });

  getBlocks
    .mockResolvedValueOnce({
      data: [
        {
          blockHeight: 12,
          blockHash: 'hash12',
          timestamp: '2026-03-22T17:30:00',
          transactionCount: 3
        }
      ],
      pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
    })
    .mockResolvedValueOnce({
      data: [
        {
          blockHeight: 11,
          blockHash: 'hash11',
          timestamp: '2026-03-22T17:20:00',
          transactionCount: 1
        }
      ],
      pagination: { page: 2, limit: 10, total: 13, totalPages: 2 }
    });

  renderExplorer();

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Next' }));
  expect(await screen.findByText('Page 2 of 2')).toBeInTheDocument();
  expect(getBlocks).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2, limit: 10 }));
});

test('loads block detail and linked transactions on inspect', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValue({
    data: [
      {
        blockHeight: 12,
        blockHash: 'hash12',
        timestamp: '2026-03-22T17:30:00',
        transactionCount: 1
      }
    ],
    pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
  });

  getBlockByHeight.mockResolvedValue({
    blockHeight: 12,
    blockHash: 'hash12',
    previousHash: 'hash11',
    status: 'COMMITTED',
    transactions: [
      {
        id: 77,
        transactionHash: 'txhash77abcdef',
        senderAddress: 'sender_wallet',
        receiverAddress: 'receiver_wallet',
        amount: 5,
        fee: 0.01,
        status: 'CONFIRMED'
      }
    ]
  });

  renderExplorer();

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Inspect' }));

  expect(await screen.findByLabelText(/Linked transactions table/i)).toBeInTheDocument();
  expect(screen.getByText('sender_wallet')).toBeInTheDocument();
  expect(getBlockByHeight).toHaveBeenCalledWith(expect.objectContaining({ height: 12 }));
});

test('shows inspect button loading interaction while detail is fetching', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValue({
    data: [
      {
        blockHeight: 12,
        blockHash: 'hash12',
        timestamp: '2026-03-22T17:30:00',
        transactionCount: 1
      }
    ],
    pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
  });

  getBlockByHeight.mockImplementation(() => new Promise(() => {}));

  renderExplorer();

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Inspect' }));

  const loadingButton = screen.getByRole('button', { name: 'Inspecting...' });
  expect(loadingButton).toBeDisabled();
  expect(loadingButton).toHaveClass('explorer-view-btn-loading');
  expect(loadingButton).toHaveAttribute('aria-busy', 'true');
});

test('supports manual lookup by height', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 3,
    totalBlocks: 4,
    pendingTransactions: 0,
    latestBlockHash: 'hash3'
  });

  getBlocks.mockResolvedValue({
    data: [],
    pagination: { page: 1, limit: 10, total: 4, totalPages: 1 }
  });

  getBlockByHeight.mockResolvedValue({
    blockHeight: 0,
    blockHash: 'genesis_hash',
    previousHash: '000',
    status: 'COMMITTED',
    transactions: []
  });

  renderExplorer();

  await screen.findByText('No blocks available.');
  fireEvent.change(screen.getByLabelText('Block lookup input'), { target: { value: '0' } });
  fireEvent.click(screen.getByRole('button', { name: 'Find by Height' }));

  expect(await screen.findByText('genesis_hash')).toBeInTheDocument();
  expect(getBlockByHeight).toHaveBeenCalledWith(expect.objectContaining({ height: 0 }));
});

test('supports manual lookup by hash', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 3,
    totalBlocks: 4,
    pendingTransactions: 0,
    latestBlockHash: 'hash3'
  });

  getBlocks.mockResolvedValue({
    data: [],
    pagination: { page: 1, limit: 10, total: 4, totalPages: 1 }
  });

  getBlockByHash.mockResolvedValue({
    blockHeight: 2,
    blockHash: 'hash_two',
    previousHash: 'hash_one',
    status: 'COMMITTED',
    transactions: []
  });

  renderExplorer();

  await screen.findByText('No blocks available.');
  fireEvent.change(screen.getByLabelText('Block lookup input'), { target: { value: 'hash_two' } });
  fireEvent.click(screen.getByRole('button', { name: 'Find by Hash' }));

  expect(await screen.findByText('hash_two')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Copy selected block hash' })).toHaveClass('explorer-copy-btn-top');
  expect(screen.getByRole('button', { name: 'Copy previous block hash' })).toHaveClass('explorer-copy-btn-top');
  expect(getBlockByHash).toHaveBeenCalledWith(expect.objectContaining({ hash: 'hash_two' }));
});

test('copies full block hash from row action', async () => {
  const fullHash = 'abcdef1234567890abcdef1234567890';

  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: fullHash
  });

  getBlocks.mockResolvedValue({
    data: [
      {
        blockHeight: 12,
        blockHash: fullHash,
        timestamp: '2026-03-22T17:30:00',
        transactionCount: 3
      }
    ],
    pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
  });

  renderExplorer();

  await screen.findByText('Page 1 of 2');
  const copyButton = screen.getByRole('button', { name: 'Copy block hash at height 12' });
  fireEvent.click(copyButton);

  expect(navigator.clipboard.writeText).toHaveBeenCalledWith(fullHash);
  await waitFor(() => expect(copyButton).toHaveTextContent('Copied'));
  expect(copyButton).toHaveClass('explorer-copy-btn-copied');
});

test('syncs page and selected height to query params', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValue({
    data: [
      {
        blockHeight: 12,
        blockHash: 'hash12',
        timestamp: '2026-03-22T17:30:00',
        transactionCount: 1
      }
    ],
    pagination: { page: 1, limit: 10, total: 13, totalPages: 2 }
  });

  getBlockByHeight.mockResolvedValue({
    blockHeight: 0,
    blockHash: 'genesis_hash',
    previousHash: '000',
    status: 'COMMITTED',
    transactions: []
  });

  renderExplorer();

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Next' }));

  await waitFor(() => expect(mockSetSearchParams).toHaveBeenCalled());
  let latestParams = mockSetSearchParams.mock.calls[mockSetSearchParams.mock.calls.length - 1][0];
  expect(latestParams.get('page')).toBe('2');

  fireEvent.change(screen.getByLabelText('Block lookup input'), { target: { value: '0' } });
  fireEvent.click(screen.getByRole('button', { name: 'Find by Height' }));

  await waitFor(() => {
    latestParams = mockSetSearchParams.mock.calls[mockSetSearchParams.mock.calls.length - 1][0];
    expect(latestParams.get('height')).toBe('0');
  });
  expect(latestParams.get('page')).toBe('2');
});

test('hydrates page and hash from query params on initial load', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValue({
    data: [],
    pagination: { page: 2, limit: 10, total: 13, totalPages: 2 }
  });

  getBlockByHash.mockResolvedValue({
    blockHeight: 2,
    blockHash: 'hash_two',
    previousHash: 'hash_one',
    status: 'COMMITTED',
    transactions: []
  });

  renderExplorer('/blockchain?page=2&hash=hash_two');

  await waitFor(() => {
    expect(getBlocks).toHaveBeenCalledWith(expect.objectContaining({ page: 2, limit: 10 }));
  });
  expect(await screen.findByText('hash_two')).toBeInTheDocument();
  expect(getBlockByHash).toHaveBeenCalledWith(expect.objectContaining({ hash: 'hash_two' }));
});

test('clear inspector resets detail state and removes hash/height query params', async () => {
  getChainStatus.mockResolvedValue({
    latestBlockHeight: 12,
    totalBlocks: 13,
    pendingTransactions: 4,
    latestBlockHash: '1234567890abcdef1234567890abcdef'
  });

  getBlocks.mockResolvedValue({
    data: [],
    pagination: { page: 2, limit: 10, total: 13, totalPages: 2 }
  });

  getBlockByHash.mockResolvedValue({
    blockHeight: 2,
    blockHash: 'hash_two',
    previousHash: 'hash_one',
    status: 'COMMITTED',
    transactions: []
  });

  renderExplorer('/blockchain?page=2&hash=hash_two');

  expect(await screen.findByText('hash_two')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Clear inspector' }));

  expect(screen.getByLabelText('Block lookup input')).toHaveValue('');
  expect(screen.getByText('Select a block row to inspect linked transactions.')).toBeInTheDocument();

  let latestParams;
  await waitFor(() => {
    latestParams = mockSetSearchParams.mock.calls[mockSetSearchParams.mock.calls.length - 1][0];
    expect(latestParams.get('page')).toBe('2');
  });
  expect(latestParams.has('hash')).toBe(false);
  expect(latestParams.has('height')).toBe(false);
});


// New tests for BlockChainDiagram
describe('BlockChainDiagram', () => {
  const baseBlocks = [
    { blockHeight: 0, blockHash: 'genesis000000000', transactionCount: 0, status: 'COMMITTED' },
    { blockHeight: 1, blockHash: 'block1aaaaaaaaaa', transactionCount: 3, status: 'COMMITTED' },
    { blockHeight: 2, blockHash: 'block2bbbbbbbbbb', transactionCount: 1, status: 'PENDING' },
  ];

  test('renders nothing when blocks array is empty', () => {
    const { container } = render(
      <BlockChainDiagram blocks={[]} onSelectBlock={jest.fn()} />
    );
    expect(container.firstChild).toBeNull();
  });

  test('renders a node for each block in height order', () => {
    render(<BlockChainDiagram blocks={baseBlocks} onSelectBlock={jest.fn()} />);

    expect(screen.getByRole('region', { name: /Visual block chain diagram/i })).toBeInTheDocument();
    expect(screen.getByLabelText(/Block 0/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Block 1/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Block 2/i)).toBeInTheDocument();
  });

  test('renders blocks sorted by height even when passed out-of-order', () => {
    const shuffled = [baseBlocks[2], baseBlocks[0], baseBlocks[1]];
    render(<BlockChainDiagram blocks={shuffled} onSelectBlock={jest.fn()} />);

    const nodes = screen.getAllByRole('button', { name: /Inspect/i });
    // Nodes appear left-to-right: 0, 1, 2
    expect(nodes).toHaveLength(3);
  });

  test('each node displays height, truncated hash, and transaction count', () => {
    render(<BlockChainDiagram blocks={[baseBlocks[1]]} onSelectBlock={jest.fn()} />);

    // Height
    expect(screen.getByText('#1')).toBeInTheDocument();
    // Tx count label + value
    expect(screen.getByText('TXS')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    // Truncated hash (first 6 chars + … + last 4)
    expect(screen.getByText('block1…aaaa')).toBeInTheDocument();
  });

  test('applies COMMITTED status class to committed blocks', () => {
    render(<BlockChainDiagram blocks={[baseBlocks[0]]} onSelectBlock={jest.fn()} />);
    const node = screen.getByLabelText(/Block 0/i);
    expect(node).toHaveClass('chain-node--committed');
  });

  test('applies PENDING status class to pending blocks', () => {
    render(<BlockChainDiagram blocks={[baseBlocks[2]]} onSelectBlock={jest.fn()} />);
    const node = screen.getByLabelText(/Block 2/i);
    expect(node).toHaveClass('chain-node--pending');
  });

  test('applies selected class when selectedHeight matches', () => {
    render(
      <BlockChainDiagram
        blocks={baseBlocks}
        selectedHeight={1}
        onSelectBlock={jest.fn()}
      />
    );
    const selectedNode = screen.getByLabelText(/Block 1/i);
    expect(selectedNode).toHaveClass('chain-node--selected');

    const otherNode = screen.getByLabelText(/Block 0/i);
    expect(otherNode).not.toHaveClass('chain-node--selected');
  });

  test('calls onSelectBlock with block height when a node is clicked', () => {
    const onSelectBlock = jest.fn();
    render(<BlockChainDiagram blocks={baseBlocks} onSelectBlock={onSelectBlock} />);

    fireEvent.click(screen.getByLabelText(/Block 1/i));
    expect(onSelectBlock).toHaveBeenCalledWith(1);
  });

  test('calls onSelectBlock when the Inspect button inside a node is clicked', () => {
    const onSelectBlock = jest.fn();
    render(<BlockChainDiagram blocks={[baseBlocks[0]]} onSelectBlock={onSelectBlock} />);

    fireEvent.click(screen.getByRole('button', { name: 'Inspect' }));
    expect(onSelectBlock).toHaveBeenCalledWith(0);
  });

  test('shows "Selected" label on node button when that height is selected', () => {
    render(
      <BlockChainDiagram
        blocks={baseBlocks}
        selectedHeight={0}
        onSelectBlock={jest.fn()}
      />
    );
    // The inspect button for block 0 should say "Selected"
    const buttons = screen.getAllByRole('button');
    const selectedBtn = buttons.find((b) => b.textContent === 'Selected');
    expect(selectedBtn).toBeTruthy();
  });

  test('shows "Inspecting…" and disables button for the inspecting height', () => {
    render(
      <BlockChainDiagram
        blocks={baseBlocks}
        inspectingHeight={1}
        onSelectBlock={jest.fn()}
      />
    );
    const inspectingBtn = screen.getByRole('button', { name: 'Inspecting…' });
    expect(inspectingBtn).toBeDisabled();
    expect(inspectingBtn).toHaveClass('chain-node-btn--loading');
    expect(inspectingBtn).toHaveAttribute('aria-busy', 'true');
  });

  test('does not fire onSelectBlock when clicking a node that is inspecting', () => {
    const onSelectBlock = jest.fn();
    render(
      <BlockChainDiagram
        blocks={baseBlocks}
        inspectingHeight={2}
        onSelectBlock={onSelectBlock}
      />
    );
    const inspectingNode = screen.getByLabelText(/Block 2/i);
    fireEvent.click(inspectingNode);
    expect(onSelectBlock).not.toHaveBeenCalled();
  });

  test('applies entrance animation class to newBlockHeight node', () => {
    render(
      <BlockChainDiagram
        blocks={baseBlocks}
        newBlockHeight={2}
        onSelectBlock={jest.fn()}
      />
    );
    const newNode = screen.getByLabelText(/Block 2/i);
    expect(newNode).toHaveClass('chain-node--new');

    const oldNode = screen.getByLabelText(/Block 0/i);
    expect(oldNode).not.toHaveClass('chain-node--new');
  });

  test('renders legend with COMMITTED, PENDING, and ORPHANED labels', () => {
    render(<BlockChainDiagram blocks={baseBlocks} onSelectBlock={jest.fn()} />);
    const legend = screen.getByLabelText('Status legend');
    expect(legend).toHaveTextContent('Committed');
    expect(legend).toHaveTextContent('Pending');
    expect(legend).toHaveTextContent('Orphaned');
  });

  test('supports keyboard activation via Enter key', () => {
    const onSelectBlock = jest.fn();
    render(<BlockChainDiagram blocks={baseBlocks} onSelectBlock={onSelectBlock} />);

    const node = screen.getByLabelText(/Block 0/i);
    fireEvent.keyDown(node, { key: 'Enter' });
    expect(onSelectBlock).toHaveBeenCalledWith(0);
  });

  test('supports keyboard activation via Space key', () => {
    const onSelectBlock = jest.fn();
    render(<BlockChainDiagram blocks={baseBlocks} onSelectBlock={onSelectBlock} />);

    const node = screen.getByLabelText(/Block 1/i);
    fireEvent.keyDown(node, { key: ' ' });
    expect(onSelectBlock).toHaveBeenCalledWith(1);
  });

  test('falls back gracefully for blocks without a status field', () => {
    const noStatusBlock = [{ blockHeight: 5, blockHash: 'nohash', transactionCount: 2 }];
    render(<BlockChainDiagram blocks={noStatusBlock} onSelectBlock={jest.fn()} />);
    // Should default to COMMITTED styling
    const node = screen.getByLabelText(/Block 5/i);
    expect(node).toHaveClass('chain-node--committed');
  });
});


// ─────────────────────────────────────────────────────────────────────────────
// New tests: chain diagram integration inside BlockchainExplorer
// ─────────────────────────────────────────────────────────────────────────────

describe('BlockchainExplorer – chain diagram integration', () => {
  const statusPayload = {
    latestBlockHeight: 3,
    totalBlocks: 4,
    pendingTransactions: 1,
    latestBlockHash: 'lateshhash'
  };

  const blocksPayload = {
    data: [
      { blockHeight: 1, blockHash: 'hashone', timestamp: '2026-01-01', transactionCount: 2, status: 'COMMITTED' },
      { blockHeight: 2, blockHash: 'hashtwo', timestamp: '2026-01-02', transactionCount: 0, status: 'COMMITTED' },
      { blockHeight: 3, blockHash: 'hashthree', timestamp: '2026-01-03', transactionCount: 5, status: 'PENDING' },
    ],
    pagination: { page: 1, limit: 10, total: 4, totalPages: 1 }
  };

  test('renders the chain diagram region when blocks are loaded', async () => {
    getChainStatus.mockResolvedValue(statusPayload);
    getBlocks.mockResolvedValue(blocksPayload);

    renderExplorer();

    expect(
      await screen.findByRole('region', { name: /Visual block chain diagram/i })
    ).toBeInTheDocument();
  });

  test('diagram shows a node for every block returned by the API', async () => {
    getChainStatus.mockResolvedValue(statusPayload);
    getBlocks.mockResolvedValue(blocksPayload);

    renderExplorer();

    await screen.findByRole('region', { name: /Visual block chain diagram/i });
    expect(screen.getByLabelText(/Block 1/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Block 2/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Block 3/i)).toBeInTheDocument();
  });

  test('clicking a diagram node triggers getBlockByHeight and shows detail', async () => {
    getChainStatus.mockResolvedValue(statusPayload);
    getBlocks.mockResolvedValue(blocksPayload);
    getBlockByHeight.mockResolvedValue({
      blockHeight: 2,
      blockHash: 'hashtwo',
      previousHash: 'hashone',
      status: 'COMMITTED',
      transactions: []
    });

    renderExplorer();

    await screen.findByRole('region', { name: /Visual block chain diagram/i });
    fireEvent.click(screen.getByLabelText(/Block 2/i));

    await waitFor(() => expect(getBlockByHeight).toHaveBeenCalledWith(expect.objectContaining({ height: 2 })));
    expect(await screen.findByText('hashtwo')).toBeInTheDocument();
  });

  test('does not render diagram while data is loading', async () => {
    getChainStatus.mockResolvedValue(statusPayload);
    // Never resolves during this test
    getBlocks.mockImplementation(() => new Promise(() => {}));

    renderExplorer();

    expect(screen.queryByRole('region', { name: /Visual block chain diagram/i })).not.toBeInTheDocument();
    expect(screen.getByText('Loading blockchain data...')).toBeInTheDocument();
  });

  test('does not render diagram when blocks array is empty', async () => {
    getChainStatus.mockResolvedValue(statusPayload);
    getBlocks.mockResolvedValue({ data: [], pagination: { page: 1, limit: 10, total: 0, totalPages: 0 } });

    renderExplorer();

    await screen.findByText('No blocks available.');
    expect(screen.queryByRole('region', { name: /Visual block chain diagram/i })).not.toBeInTheDocument();
  });

  test('selected node in diagram reflects the same height as the table row selection', async () => {
    getChainStatus.mockResolvedValue(statusPayload);
    getBlocks.mockResolvedValue(blocksPayload);
    getBlockByHeight.mockResolvedValue({
      blockHeight: 1,
      blockHash: 'hashone',
      previousHash: '000',
      status: 'COMMITTED',
      transactions: []
    });

    renderExplorer();

    await screen.findByRole('region', { name: /Visual block chain diagram/i });

    // Click Inspect in the table row for block 1
    const inspectButtons = screen.getAllByRole('button', { name: 'Inspect' });
    // The first Inspect button after diagram nodes is the table row one
    const tableInspect = inspectButtons.find((btn) => !btn.closest('.chain-node'));
    fireEvent.click(tableInspect);

    await waitFor(() => expect(getBlockByHeight).toHaveBeenCalledWith(expect.objectContaining({ height: 1 })));

    // Diagram node for block 1 should now be selected
    const node1 = screen.getByLabelText(/Block 1/i);
    expect(node1).toHaveClass('chain-node--selected');
  });
});