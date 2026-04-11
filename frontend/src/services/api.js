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
 */
api.interceptors.response.use(
    (response) => response, // If the request is successful (200 OK), just pass the data through.
    (error) => {
        // We look for 401 (Unauthorized) or 403 (Forbidden) status codes.
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            
            // UX CHECK: We don't want to redirect if the user is ALREADY trying to log in.
            if (!window.location.pathname.includes('/login')) {
                console.warn("Session expired or invalid. Redirecting to login...");
                
                // Security Step: Wipe the corrupted or expired token so it's not used again.
                localStorage.removeItem('token');
                
                // Hard Redirect: Using window.location.href instead of 'navigate' 
                // forces a full page reload, clearing any sensitive data in React state.
                window.location.href = '/login';
            }
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
     * Some backends return { "token": "..." } and others return a raw string.
     * This logic ensures the token is saved correctly regardless of the backend format.
     */
    if (response.data.token) {
        localStorage.setItem('token', response.data.token);
    } else if (typeof response.data === 'string') {
        localStorage.setItem('token', response.data);
    }
    return response.data;
};

export default api;