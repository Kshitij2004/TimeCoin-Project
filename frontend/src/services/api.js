export const API_BASE_URL = process.env.REACT_APP_API_URL ?? 'http://localhost:8080';

export const registerUser = (userData) => {
    return fetch(`${API_BASE_URL}/api/auth/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(userData)
    }).then(async (response) => {
        const payload = await response.json().catch(() => ({}));
        if (!response.ok) {
            const error = new Error(payload?.message || payload?.error || 'Registration failed');
            error.response = { data: payload };
            throw error;
        }
        return payload;
    });
};
