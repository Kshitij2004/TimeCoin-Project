// Change the import to use the CommonJS version for Jest compatibility
import axios from 'axios';

// 1. Centralized Base URL configuration
export const API_BASE_URL = process.env.REACT_APP_API_URL ?? 'http://localhost:8080';

const api = axios.create({
    baseURL: `${API_BASE_URL}/api`,
    headers: {
        'Content-Type': 'application/json',
    },
});

// ... the rest of your interceptor code stays exactly the same

// 2. REQUEST INTERCEPTOR: Attach JWT to every request
// Requirement: "Attach JWT token from localStorage/context to Authorization header"
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

// 3. RESPONSE INTERCEPTOR: Global 401 handling
// Requirement: "Handle 401 responses globally (redirect to login)"
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response && error.response.status === 401) {
            // Expired or missing tokens redirect to login page
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

// 4. API Methods (Refactored to use the centralized 'api' instance)

export const registerUser = async (userData) => {
    // Post to /auth/register relative to the baseURL defined above
    const response = await api.post('/auth/register', userData);
    return response.data;
};

// Added Login helper to ensure the token is handled properly
export const loginUser = async (credentials) => {
    const response = await api.post('/auth/login', credentials);
    if (response.data.token) {
        localStorage.setItem('token', response.data.token);
    }
    return response.data;
};

export default api;