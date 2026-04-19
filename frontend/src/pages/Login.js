import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api.js';
import '../Login.css';

function Login() {
    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await api.post('/auth/login', {
                username: formData.username,
                password: formData.password
            });

            const data = response.data;

            // If 2FA is required, redirect to verification page with tempToken
            if (data.requires2FA && data.tempToken) {
                sessionStorage.setItem('2fa_temp_token', data.tempToken);
                navigate('/login/verify');
                return;
            }

            // Normal login — extract accessToken
            const token = data.accessToken || data.token ||
                (typeof data === 'string' ? data : null);

            if (token) {
                localStorage.setItem('token', token);
                navigate('/dashboard');
            } else {
                setError('Authentication successful, but no token was found in the response.');
            }
        } catch (err) {
            const message = err.response?.data?.message || 'Login failed. Please check your credentials.';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form onSubmit={handleSubmit}>
                    <h2>Sign in to CrypMart</h2>

                    {error && (
                        <div role="alert" style={{ color: '#ff4d4d', marginBottom: '15px', textAlign: 'center', fontSize: '14px' }}>
                            {error}
                        </div>
                    )}

                    <div className="input-group">
                        <input
                            type="text"
                            name="username"
                            placeholder="Username"
                            value={formData.username}
                            onChange={handleChange}
                            disabled={loading}
                            required
                        />
                    </div>

                    <div className="input-group">
                        <input
                            type="password"
                            name="password"
                            placeholder="Password"
                            value={formData.password}
                            onChange={handleChange}
                            disabled={loading}
                            required
                        />
                    </div>

                    <button type="submit" className="login-btn" disabled={loading}>
                        {loading ? 'LOGGING IN...' : 'LOG IN'}
                    </button>

                    <div className="links">
                        <a href="/forgot-password">Forgot Password?</a>
                        <div className="register-text">
                            Don't have an account?
                            <Link to="/register" className="register-link">Register here</Link>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default Login;