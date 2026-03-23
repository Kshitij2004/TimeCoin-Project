// Centralized Axios client for CrypMart
import axios from 'axios';

// 1. Centralized Base URL configuration
export const API_BASE_URL = process.env.REACT_APP_API_URL ?? 'http://localhost:8080';

const api = axios.create({
    baseURL: `${API_BASE_URL}/api`,
    headers: {
        'Content-Type': 'application/json',
    },
});

// 2. REQUEST INTERCEPTOR: Attach JWT to every request
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

// 3. RESPONSE INTERCEPTOR: Global Auth Handling
// This catches expired tokens or server restarts (like your Docker issue)
api.interceptors.response.use(
    (response) => response,
    (error) => {
        // Handle 401 (Unauthorized) and 403 (Forbidden)
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
            
            // Only redirect if we aren't already on the login page 
            // to avoid infinite refresh loops
            if (!window.location.pathname.includes('/login')) {
                console.warn("Session expired or invalid. Redirecting to login...");
                
                // Clear the invalid token
                localStorage.removeItem('token');
                
                // Force a hard redirect to ensure the app state is wiped clean
                window.location.href = '/login';
            }
        }
        return Promise.reject(error);
    }
);

// 4. API Helper Methods

export const registerUser = async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data;
};

export const loginUser = async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    // Standardizing token storage here
    if (response.data.token) {
        localStorage.setItem('token', response.data.token);
    } else if (typeof response.data === 'string') {
        localStorage.setItem('token', response.data);
    }
    return response.data;
};

export default api;