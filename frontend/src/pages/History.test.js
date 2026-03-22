import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import History from './History';
import api from '../services/api.js';

// 1. Mock the centralized API service
jest.mock('../services/api.js', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
  },
}));

beforeEach(() => {
  jest.clearAllMocks();
  localStorage.clear();
});

test('shows login message when not authenticated', () => {
  // Note: Since we updated History.js to rely on localStorage/interceptors, 
  // we simulate "unauthenticated" by ensuring no token exists.
  render(<History />); 
  
  // If your ProtectedRoute isn't wrapping this in the test, 
  // we check the internal "auth" logic of the component.
  // Note: In the refactored History, 'auth' is true if the token exists.
  expect(screen.queryByText(/Please log in/i)).toBeInTheDocument();
  expect(api.get).not.toHaveBeenCalled();
});

test('renders transactions table after successful fetch', async () => {
  // Set the token so the component thinks it is authenticated
  localStorage.setItem('token', 'real-web-token');

  api.get.mockResolvedValueOnce({
    data: {
      data: [
        {
          type: 'BUY',
          amount: 0.25,
          priceAtTime: 66000,
          timestamp: '2026-02-23T12:00:00'
        }
      ],
      pagination: { page: 1, limit: 10, total: 1, totalPages: 1 }
    }
  });

  render(<History pageSize={10} />);

  expect(await screen.findByText('BUY')).toBeInTheDocument();
  expect(screen.getByRole('table', { name: /Transaction history table/i })).toBeInTheDocument();
  
  // Verify the call uses the correct URL. 
  // Interceptors handle the headers, so we just check the endpoint.
  expect(api.get).toHaveBeenCalledWith(
    expect.stringContaining('/transactions?page=1&limit=10'),
    expect.any(Object) // for the AbortSignal
  );
});

test('shows empty state when no transactions are returned', async () => {
  localStorage.setItem('token', 'real-web-token');

  api.get.mockResolvedValueOnce({
    data: {
      data: [],
      pagination: { page: 1, limit: 10, total: 0, totalPages: 0 }
    }
  });

  render(<History />);

  expect(await screen.findByText(/No transactions yet/i)).toBeInTheDocument();
});

test('supports basic next-page pagination', async () => {
  localStorage.setItem('token', 'real-web-token');

  api.get
    .mockResolvedValueOnce({
      data: {
        data: [{ type: 'BUY', amount: 1, priceAtTime: 60000, timestamp: '2026-02-23T12:00:00' }],
        pagination: { page: 1, limit: 1, total: 2, totalPages: 2 }
      }
    })
    .mockResolvedValueOnce({
      data: {
        data: [{ type: 'SELL', amount: 0.5, priceAtTime: 61000, timestamp: '2026-02-23T11:00:00' }],
        pagination: { page: 2, limit: 1, total: 2, totalPages: 2 }
      }
    });

  render(<History pageSize={1} />);

  // First page
  expect(await screen.findByText('BUY')).toBeInTheDocument();
  
  // Click Next
  const nextButton = screen.getByRole('button', { name: /Next/i });
  fireEvent.click(nextButton);

  // Second page
  expect(await screen.findByText('SELL')).toBeInTheDocument();
  expect(api.get).toHaveBeenCalledTimes(2);
});