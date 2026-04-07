import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Register from './Register';
import { registerUser } from '../services/api';

// 1. Mock alert so the test doesn't crash
window.alert = jest.fn();

jest.mock('../services/api');

const renderRegister = () => {
  render(
    <BrowserRouter>
      <Register />
    </BrowserRouter>
  );
};

describe('Register Component Validation', () => {
  
  test('shows error when fields are empty', async () => {
    renderRegister();
    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    expect(await screen.findByText(/all fields are required/i)).toBeInTheDocument();
  });

  test('shows error for invalid email format', async () => {
    renderRegister();
    // FILL ALL FIELDS so we get past the "Required" check
    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'ValidUser' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'bad-email' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: 'Password123' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Password123' } });
    
    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    expect(await screen.findByText(/email must match standard format/i)).toBeInTheDocument();
  });

  test('shows error for short/invalid username', async () => {
    renderRegister();
    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'ab' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'test@wisc.edu' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: 'Password123' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Password123' } });

    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    expect(await screen.findByText(/username must be 3-20 characters/i)).toBeInTheDocument();
  });

  test('shows error for weak password', async () => {
    renderRegister();
    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'ValidUser' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'test@wisc.edu' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: '12345' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: '12345' } });

    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    expect(await screen.findByText(/password must be at least 8 characters/i)).toBeInTheDocument();
  });

  test('shows error when passwords do not match', async () => {
    renderRegister();
    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'ValidUser' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'test@wisc.edu' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: 'Password123' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Different123' } });

    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
  });

  test('successful validation calls registerUser API', async () => {
    registerUser.mockResolvedValueOnce({ data: { message: "Success" } });
    renderRegister();

    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'GarvP' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'garv@wisc.edu' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: 'Password123' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Password123' } });

    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));

    await waitFor(() => {
      expect(registerUser).toHaveBeenCalledWith({
        username: 'GarvP',
        email: 'garv@wisc.edu',
        password: 'Password123'
      });
    });
  });
});