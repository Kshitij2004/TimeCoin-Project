import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api.js'; // Import our centralized client
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

            // Log the response to DevTools to verify the structure if redirect fails
            console.log('Login Response Data:', response.data);

            /**
             * FIX: Flexible token extraction.
             * 1. Check for response.data.token (Standard JSON object)
             * 2. Check for response.data (If backend returns plain string JWT)
             */
            const token = response.data?.token || (typeof response.data === 'string' ? response.data : null);

            if (token) {
                // Store the real JWT in localStorage for the Interceptor to use
                localStorage.setItem('token', token);
                
                // Redirect to the dashboard
                navigate('/dashboard');
            } else {
                setError('Authentication successful, but no token was found in the response.');
            }
        } catch (err) {
            // Capture specific backend error messages if available
            const message = err.response?.data?.message || 'Login failed. Please check your credentials.';
            setError(message);
            console.error('Login error:', err);
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