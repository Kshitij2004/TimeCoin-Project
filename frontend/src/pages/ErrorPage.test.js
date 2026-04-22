import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import ErrorPage from './ErrorPage.js';

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
    ...jest.requireActual('react-router-dom'),
    useNavigate: () => mockNavigate,
}));

// Helper: set window.location.search before rendering
function setSearch(params) {
    delete window.location;
    window.location = { search: params, pathname: '/error', href: '' };
}

beforeEach(() => {
    jest.clearAllMocks();
});

describe('ErrorPage - known status codes', () => {
    it('shows 404 title and status code', () => {
        setSearch('?status=404');
        render(<ErrorPage />);
        expect(screen.getByText('404')).toBeInTheDocument();
        expect(screen.getByText('Not Found')).toBeInTheDocument();
    });

    it('shows 500 title and status code', () => {
        setSearch('?status=500');
        render(<ErrorPage />);
        expect(screen.getByText('500')).toBeInTheDocument();
        expect(screen.getByText('Server Error')).toBeInTheDocument();
    });

    it('shows 403 title and status code', () => {
        setSearch('?status=403');
        render(<ErrorPage />);
        expect(screen.getByText('403')).toBeInTheDocument();
        expect(screen.getByText('Forbidden')).toBeInTheDocument();
    });

    it('shows 429 title and status code', () => {
        setSearch('?status=429');
        render(<ErrorPage />);
        expect(screen.getByText('429')).toBeInTheDocument();
        expect(screen.getByText('Too Many Requests')).toBeInTheDocument();
    });

    it('shows "Unexpected Error" for an unrecognised status code', () => {
        setSearch('?status=418');
        render(<ErrorPage />);
        expect(screen.getByText('418')).toBeInTheDocument();
        expect(screen.getByText('Unexpected Error')).toBeInTheDocument();
    });

    it('omits the status code block when no status param is provided', () => {
        setSearch('');
        render(<ErrorPage />);
        expect(screen.queryByText(/^\d{3}$/)).not.toBeInTheDocument();
    });
});

describe('ErrorPage - custom message', () => {
    it('prefers the message query param over the default detail text', () => {
        setSearch('?status=404&message=Listing+not+found');
        render(<ErrorPage />);
        expect(screen.getByText('Listing not found')).toBeInTheDocument();
    });
});

describe('ErrorPage - 429 retry-after', () => {
    it('shows retry-after info when provided', () => {
        setSearch('?status=429&retryAfter=60+seconds');
        render(<ErrorPage />);
        expect(screen.getByText(/60 seconds/)).toBeInTheDocument();
    });

    it('does not show retry-after section when param is absent', () => {
        setSearch('?status=429');
        render(<ErrorPage />);
        expect(screen.queryByText(/You can try again in/)).not.toBeInTheDocument();
    });
});

describe('ErrorPage - navigation buttons', () => {
    it('"Go Home" navigates to /dashboard', () => {
        setSearch('?status=500');
        render(<ErrorPage />);
        fireEvent.click(screen.getByRole('button', { name: /go home/i }));
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });

    it('"Go Back" calls navigate(-1)', () => {
        setSearch('?status=500');
        render(<ErrorPage />);
        fireEvent.click(screen.getByRole('button', { name: /go back/i }));
        expect(mockNavigate).toHaveBeenCalledWith(-1);
    });
});