import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import BlockchainExplorer from './BlockchainExplorer';
import { getBlockByHash, getBlockByHeight, getBlocks, getChainStatus } from '../services/blockchainExplorerApi';

jest.mock('../services/blockchainExplorerApi', () => ({
  getChainStatus: jest.fn(),
  getBlocks: jest.fn(),
  getBlockByHeight: jest.fn(),
  getBlockByHash: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
  Object.defineProperty(navigator, 'clipboard', {
    value: { writeText: jest.fn().mockResolvedValue(undefined) },
    configurable: true
  });
});

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

  render(<BlockchainExplorer />);

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

  render(<BlockchainExplorer />);

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Refresh' }));

  await waitFor(() => {
    expect(getChainStatus).toHaveBeenCalledTimes(2);
    expect(getBlocks).toHaveBeenCalledTimes(2);
  });
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

  render(<BlockchainExplorer />);

  await screen.findByText('Page 1 of 2');
  fireEvent.click(screen.getByRole('button', { name: 'Auto Refresh: Off' }));
  expect(screen.getByRole('button', { name: 'Auto Refresh: On' })).toBeInTheDocument();
  expect(intervalSpy).toHaveBeenCalledTimes(1);

  await act(async () => {
    jest.advanceTimersByTime(15000);
  });

  await waitFor(() => {
    expect(getChainStatus).toHaveBeenCalledTimes(2);
    expect(getBlocks).toHaveBeenCalledTimes(2);
  });

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

  render(<BlockchainExplorer />);

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

  render(<BlockchainExplorer />);

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

  render(<BlockchainExplorer />);

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

  render(<BlockchainExplorer />);

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

  render(<BlockchainExplorer />);

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

  render(<BlockchainExplorer />);

  await screen.findByText('Page 1 of 2');
  const copyButton = screen.getByRole('button', { name: 'Copy block hash at height 12' });
  fireEvent.click(copyButton);

  expect(navigator.clipboard.writeText).toHaveBeenCalledWith(fullHash);
  await waitFor(() => {
    expect(copyButton).toHaveTextContent('Copied');
    expect(copyButton).toHaveClass('explorer-copy-btn-copied');
  });
});
