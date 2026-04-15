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
 * It is specifically designed to handle "Auth Failures" (401/403 errors).
 *
 * Redirect is skipped when:
 *  - The request URL is an auth endpoint (/auth/)
 *  - The user is already on the login page
 *  - The request was marked with skipAuthRedirect: true
 */
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {

            const requestUrl = error.config?.url || '';
            const isAuthRequest = requestUrl.includes('/auth/');
            const skipRedirect = error.config?.skipAuthRedirect === true;

            if (!isAuthRequest && !skipRedirect && !window.location.pathname.includes('/login')) {
                console.warn("Session expired or invalid. Redirecting to login...");
                localStorage.removeItem('token');
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

/**
 * 4. API Helper Methods
 */

export const registerUser = async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
};

export const loginUser = async (credentials) => {
    const response = await api.post('/auth/login', credentials);

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