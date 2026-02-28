import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import HistoryPage from './HistoryPage';

beforeEach(() => {
  global.fetch = jest.fn();
  localStorage.clear();
});

afterEach(() => {
  jest.resetAllMocks();
});

test('shows login message when not authenticated', () => {
  render(<HistoryPage isAuthenticated={false} />);
  expect(screen.getByText(/Please log in to view your transaction history/i)).toBeInTheDocument();
  expect(fetch).not.toHaveBeenCalled();
});

test('renders transactions table after successful fetch', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({
      data: [
        {
          type: 'BUY',
          amount: 0.25,
          priceAtTime: 66000,
          timestamp: '2026-02-23T12:00:00'
        }
      ],
      pagination: { page: 1, limit: 10, total: 1, totalPages: 1 }
    })
  });

  render(<HistoryPage isAuthenticated userId={1} apiBaseUrl="http://localhost:3001" />);

  await waitFor(() => expect(screen.getByText('BUY')).toBeInTheDocument());
  expect(screen.getByRole('table', { name: /Transaction history table/i })).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledWith(
    'http://localhost:3001/api/transactions?page=1&limit=10',
    expect.objectContaining({
      method: 'GET',
      headers: { 'x-user-id': '1' }
    })
  );
});

test('shows empty state when no transactions are returned', async () => {
  fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({
      data: [],
      pagination: { page: 1, limit: 10, total: 0, totalPages: 0 }
    })
  });

  render(<HistoryPage isAuthenticated userId={1} />);

  await waitFor(() => {
    expect(screen.getByText(/No transactions yet/i)).toBeInTheDocument();
  });
});

test('supports basic next-page pagination', async () => {
  fetch
    .mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        data: [{ type: 'BUY', amount: 1, priceAtTime: 60000, timestamp: '2026-02-23T12:00:00' }],
        pagination: { page: 1, limit: 1, total: 2, totalPages: 2 }
      })
    })
    .mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        data: [{ type: 'SELL', amount: 0.5, priceAtTime: 61000, timestamp: '2026-02-23T11:00:00' }],
        pagination: { page: 2, limit: 1, total: 2, totalPages: 2 }
      })
    });

  render(<HistoryPage isAuthenticated userId={1} pageSize={1} />);

  await waitFor(() => expect(screen.getByText('BUY')).toBeInTheDocument());
  fireEvent.click(screen.getByRole('button', { name: 'Next' }));
  await waitFor(() => expect(screen.getByText('SELL')).toBeInTheDocument());
  expect(fetch).toHaveBeenCalledTimes(2);
});
