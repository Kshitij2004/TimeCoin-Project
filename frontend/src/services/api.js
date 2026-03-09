import axios from 'axios';

// Create an instance to avoid repeating the base URL
const api = axios.create({
    baseURL: 'http://localhost:8080/api', // Your Java Spring Boot address
});

export const registerUser = (userData) => {
    return api.post('/auth/register', userData);
};

export default api;