import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import LoginVerify from './LoginVerify';
import api from '../services/api';

// Mock the api module
jest.mock('../services/api');

// Mock useNavigate so we can track redirects
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

const renderLoginVerify = () => {
    render(
        <BrowserRouter>
            <LoginVerify />
        </BrowserRouter>
    );
};

describe('LoginVerify Component', () => {

    beforeEach(() => {
        mockNavigate.mockClear();
        sessionStorage.clear();
        localStorage.clear();
        jest.clearAllMocks();
    });

    test('redirects to /login when no temp token exists', () => {
        renderLoginVerify();
        expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    test('renders 2FA form when temp token exists', () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        renderLoginVerify();

        expect(screen.getByText(/Two-Factor Authentication/i)).toBeInTheDocument();
        expect(screen.getByPlaceholderText('000000')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /VERIFY/i })).toBeInTheDocument();
    });

    test('only allows digits in the code input', () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        renderLoginVerify();

        const input = screen.getByPlaceholderText('000000');
        fireEvent.change(input, { target: { value: 'abc123def' } });
        expect(input.value).toBe('123');
    });

    test('limits code input to 6 digits', () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        renderLoginVerify();

        const input = screen.getByPlaceholderText('000000');
        fireEvent.change(input, { target: { value: '12345678' } });
        expect(input.value).toBe('123456');
    });

    test('verify button is disabled when code is incomplete', () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        renderLoginVerify();

        const button = screen.getByRole('button', { name: /VERIFY/i });
        expect(button).toBeDisabled();

        const input = screen.getByPlaceholderText('000000');
        fireEvent.change(input, { target: { value: '123' } });
        expect(button).toBeDisabled();

        fireEvent.change(input, { target: { value: '123456' } });
        expect(button).not.toBeDisabled();
    });

    test('shows error when submitting incomplete code', async () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        renderLoginVerify();

        const input = screen.getByPlaceholderText('000000');
        fireEvent.change(input, { target: { value: '123' } });

        // Force click despite disabled state by submitting the form directly
        const form = input.closest('form');
        fireEvent.submit(form);

        expect(await screen.findByText(/Please enter the 6-digit code/i)).toBeInTheDocument();
    });

    test('successful verification stores token and navigates to dashboard', async () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        api.post.mockResolvedValueOnce({
            data: { accessToken: 'real-jwt-token', refreshToken: 'refresh-abc' },
        });

        renderLoginVerify();

        const input = screen.getByPlaceholderText('000000');
        fireEvent.change(input, { target: { value: '123456' } });
        fireEvent.click(screen.getByRole('button', { name: /VERIFY/i }));

        await waitFor(() => {
            expect(api.post).toHaveBeenCalledWith('/auth/2fa/verify', {
                tempToken: 'fake-temp-token',
                code: '123456',
                trustDevice: false,
            });
        });

        await waitFor(() => {
            expect(localStorage.getItem('token')).toBe('real-jwt-token');
            expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
        });
    });

    test('shows error message on invalid code', async () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        api.post.mockRejectedValueOnce({
            response: { data: { message: 'Invalid 2FA code' } },
        });

        renderLoginVerify();

        const input = screen.getByPlaceholderText('000000');
        fireEvent.change(input, { target: { value: '999999' } });
        fireEvent.click(screen.getByRole('button', { name: /VERIFY/i }));

        expect(await screen.findByText(/Invalid 2FA code/i)).toBeInTheDocument();
        expect(localStorage.getItem('token')).toBeNull();
    });

    test('trust device checkbox toggles', () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        renderLoginVerify();

        const checkbox = screen.getByRole('checkbox');
        expect(checkbox).not.toBeChecked();

        fireEvent.click(checkbox);
        expect(checkbox).toBeChecked();
    });

    test('trust device value is sent to API', async () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        api.post.mockResolvedValueOnce({
            data: { accessToken: 'real-jwt-token' },
        });

        renderLoginVerify();

        fireEvent.click(screen.getByRole('checkbox'));
        fireEvent.change(screen.getByPlaceholderText('000000'), { target: { value: '123456' } });
        fireEvent.click(screen.getByRole('button', { name: /VERIFY/i }));

        await waitFor(() => {
            expect(api.post).toHaveBeenCalledWith('/auth/2fa/verify', {
                tempToken: 'fake-temp-token',
                code: '123456',
                trustDevice: true,
            });
        });
    });

    test('back to login button clears temp token and navigates', () => {
        sessionStorage.setItem('2fa_temp_token', 'fake-temp-token');
        renderLoginVerify();

        fireEvent.click(screen.getByRole('button', { name: /Back to login/i }));

        expect(sessionStorage.getItem('2fa_temp_token')).toBeNull();
        expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
});