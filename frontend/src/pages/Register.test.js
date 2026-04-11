import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Register from './Register';
import { registerUser } from '../services/api';

/** * 1. Global Mocks
 * We mock window.alert because Jest runs in a Node environment (jsdom), 
 * which doesn't actually have a visual alert popup. This prevents the test from crashing.
 */
window.alert = jest.fn();

/**
 * 2. API Mocking
 * We don't want to hit the real Spring Boot server during tests. 
 * This replaces the real 'registerUser' function with a "spy" that we can control.
 */
jest.mock('../services/api');

/**
 * Helper function to render the component wrapped in a Router.
 * Since 'Register' uses useNavigate and Link, it must be inside a <BrowserRouter>.
 */
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
    
    // Simulate clicking the submit button without typing anything
    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    
    // Assert: Check if the specific error message appears in the DOM
    expect(await screen.findByText(/all fields are required/i)).toBeInTheDocument();
  });

  test('shows error for invalid email format', async () => {
    renderRegister();
    
    // We fill all fields, but provide an invalid email format to trigger that specific regex
    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'ValidUser' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'bad-email' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: 'Password123' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Password123' } });
    
    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    
    // Assert: Ensure the email-specific regex error is displayed
    expect(await screen.findByText(/email must match standard format/i)).toBeInTheDocument();
  });

  test('shows error for short/invalid username', async () => {
    renderRegister();
    
    // Test the 3-character minimum requirement
    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'ab' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'test@wisc.edu' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: 'Password123' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Password123' } });

    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    
    expect(await screen.findByText(/username must be 3-20 characters/i)).toBeInTheDocument();
  });

  test('shows error for weak password', async () => {
    renderRegister();
    
    // Test the complexity requirement (8+ chars, upper, lower, number)
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
    
    // Intentionally provide a different confirmation password
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Different123' } });

    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));
    
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
  });

  test('successful validation calls registerUser API', async () => {
    /**
     * Arrange: Tell our mock exactly what to return when called.
     * This simulates a successful 200 OK response from the backend.
     */
    registerUser.mockResolvedValueOnce({ data: { message: "Success" } });
    
    renderRegister();

    // Act: Fill the form with perfectly valid data
    fireEvent.change(screen.getByPlaceholderText(/Username/i), { target: { value: 'GarvP' } });
    fireEvent.change(screen.getByPlaceholderText(/Email/i), { target: { value: 'garv@wisc.edu' } });
    fireEvent.change(screen.getByPlaceholderText(/^Password$/i), { target: { value: 'Password123' } });
    fireEvent.change(screen.getByPlaceholderText(/Confirm Password/i), { target: { value: 'Password123' } });

    fireEvent.click(screen.getByRole('button', { name: /REGISTER/i }));

    /**
     * Assert: 'waitFor' is used for asynchronous actions.
     * We verify that the 'registerUser' function was called with the correct 
     * arguments, and that it DID NOT include the 'confirmPassword' (as per your API logic).
     */
    await waitFor(() => {
      expect(registerUser).toHaveBeenCalledWith({
        username: 'GarvP',
        email: 'garv@wisc.edu',
        password: 'Password123'
      });
    });
  });
});