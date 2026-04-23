import api from './api.js';

// Pull the response error handler that was registered by api.js so we can
// invoke it directly without needing a real HTTP server or mock adapter.
const errorHandler = api.interceptors.response.handlers[0].rejected;

function makeError(status, { url = '/some/endpoint', data = {}, headers = {}, skipAuthRedirect, redirectOnError } = {}) {
    return {
        response: { status, data, headers },
        config: { url, skipAuthRedirect, redirectOnError },
    };
}

beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();

    delete window.location;
    window.location = { pathname: '/dashboard', href: '', search: '' };
    Object.defineProperty(window.location, 'href', {
        set: jest.fn(),
        get: jest.fn(() => ''),
        configurable: true,
    });
    Object.defineProperty(window.location, 'pathname', {
        value: '/dashboard',
        writable: true,
        configurable: true,
    });
});

describe('401 handling', () => {
    it('clears the token and redirects to /login for 401 on a protected route', async () => {
        localStorage.setItem('token', 'some-jwt');
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(401))).rejects.toBeTruthy();

        expect(localStorage.getItem('token')).toBeNull();
        expect(setHref).toHaveBeenCalledWith('/login');
    });

    it('does NOT redirect for 401 on an auth endpoint', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(401, { url: '/auth/login' }))).rejects.toBeTruthy();

        expect(setHref).not.toHaveBeenCalled();
    });

    it('does NOT redirect for 401 when already on the login page', async () => {
        window.location.pathname = '/login';
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(401))).rejects.toBeTruthy();

        expect(setHref).not.toHaveBeenCalled();
    });

    it('does NOT redirect when skipAuthRedirect is true', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(401, { skipAuthRedirect: true }))).rejects.toBeTruthy();

        expect(setHref).not.toHaveBeenCalled();
    });
});

describe('5xx handling', () => {
    it('redirects to /error for a 500', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(500, { data: { message: 'Internal Server Error' } }))).rejects.toBeTruthy();

        expect(setHref).toHaveBeenCalledWith(expect.stringContaining('/error?'));
        expect(setHref).toHaveBeenCalledWith(expect.stringContaining('status=500'));
    });

    it('includes the backend message in the redirect URL', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(503, { data: { message: 'Service down' } }))).rejects.toBeTruthy();

        const redirectUrl = setHref.mock.calls[0][0];
        expect(redirectUrl).toContain('message=Service+down');
    });

    it('includes retry-after from response headers for 429', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(429, { headers: { 'retry-after': '30' }, redirectOnError: true }))).rejects.toBeTruthy();

        const redirectUrl = setHref.mock.calls[0][0];
        expect(redirectUrl).toContain('retryAfter=30');
    });
});

describe('4xx handling (non-401)', () => {
    it('bubbles up a 404 without redirecting by default', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(404))).rejects.toBeTruthy();

        expect(setHref).not.toHaveBeenCalled();
    });

    it('redirects to /error for a 404 when redirectOnError is true', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(404, { redirectOnError: true }))).rejects.toBeTruthy();

        expect(setHref).toHaveBeenCalledWith(expect.stringContaining('status=404'));
    });

    it('bubbles up a 429 without redirecting by default', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler(makeError(429))).rejects.toBeTruthy();

        expect(setHref).not.toHaveBeenCalled();
    });
});

describe('network errors (no response)', () => {
    it('bubbles up when there is no response object', async () => {
        const setHref = jest.fn();
        Object.defineProperty(window.location, 'href', { set: setHref, configurable: true });

        await expect(errorHandler({ config: {} })).rejects.toBeTruthy();

        expect(setHref).not.toHaveBeenCalled();
    });
});