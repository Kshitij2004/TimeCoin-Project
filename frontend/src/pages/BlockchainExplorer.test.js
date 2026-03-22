import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import BlockchainExplorer from './BlockchainExplorer';
import { getBlocks, getChainStatus } from '../services/blockchainExplorerApi';

jest.mock('../services/blockchainExplorerApi', () => ({
  getChainStatus: jest.fn(),
  getBlocks: jest.fn()
}));

beforeEach(() => {
  jest.clearAllMocks();
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
