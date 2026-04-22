// Centralized Axios client for CrypMart
import axios from 'axios';

/**
 * 1. Base URL Configuration
 * This uses an environment variable for production (like Docker/Vercel) 
 * but defaults to your local Spring Boot server (8080) for development.
 */
export const API_BASE_URL = process.env.REACT_APP_API_URL ?? 'http://localhost:8080';

const api = axios.create({
    baseURL: `${API_BASE_URL}/api`,
    headers: {
        'Content-Type': 'application/json',
    },
});

/**
 * 2. REQUEST INTERCEPTOR: Outgoing Security
 * Think of this as a "security guard" that checks every outgoing request.
 * If a JWT exists in localStorage, it automatically attaches the Bearer token
 * to the Authorization header so the Spring Boot backend can verify the user.
 */
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

/**
 * 3. RESPONSE INTERCEPTOR: Incoming Global Error Handling
 * This monitors every response coming back from the server.
 *
 * 401 on a non-auth endpoint -> session expired, clear token and go to /login.
 * Any other HTTP error -> redirect to /error with status + message so the user
 * sees an informative page instead of a silent failure.
 *
 * Redirect is skipped when:
 *  - The request URL is an auth endpoint (/auth/)
 *  - The user is already on the /login or /error page
 *  - The request was marked with skipAuthRedirect: true
 */
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (!error.response) {
            return Promise.reject(error);
        }

        const status = error.response.status;
        const requestUrl = error.config?.url || '';
        const isAuthRequest = requestUrl.includes('/auth/');
        const skipRedirect = error.config?.skipAuthRedirect === true;
        const onAuthPage = window.location.pathname.includes('/login');

        if (isAuthRequest || skipRedirect) {
            return Promise.reject(error);
        }

        if (status === 401) {
            if (!onAuthPage) {
                console.warn("Session expired or invalid. Redirecting to login...");
                localStorage.removeItem('token');
                window.location.href = '/login';
            }
            return Promise.reject(error);
        }

        // 5xx: unrecoverable - always redirect to the error page.
        // 4xx: components handle these with their own UI (e.g. empty states for
        // new accounts). Pass { redirectOnError: true } on a request to opt a
        // specific 4xx into the global redirect instead.
        const isServerError = status >= 500;
        const redirectOnError = error.config?.redirectOnError === true;
        const onErrorPage = window.location.pathname.includes('/error');

        if ((isServerError || redirectOnError) && !onErrorPage) {
            const message = error.response.data?.message || '';
            const retryAfter =
                error.response.headers?.['retry-after'] ||
                error.response.data?.retryAfter ||
                '';
            const params = new URLSearchParams({ status });
            if (message) params.set('message', message);
            if (retryAfter) params.set('retryAfter', retryAfter);
            window.location.href = `/error?${params.toString()}`;
        }

        return Promise.reject(error);
    }
);

/**
 * 4. API Helper Methods
 * These are abstraction layers so our components (Register.js / Login.js)
 * don't have to worry about URL paths or axios syntax directly.
 */

// Handles the POST request to create a new user account.
export const registerUser = async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
};

// Handles the POST request to authenticate a user.
export const loginUser = async (credentials) => {
    const response = await api.post('/auth/login', credentials);

    /**
     * Token Normalization:
     * The backend returns { accessToken, refreshToken }.
     * We save the accessToken as our JWT for subsequent requests.
     */
    if (response.data.accessToken) {
        localStorage.setItem('token', response.data.accessToken);
    } else if (response.data.token) {
        localStorage.setItem('token', response.data.token);
    } else if (typeof response.data === 'string') {
        localStorage.setItem('token', response.data);
    }
    return response.data;
};

export default api;