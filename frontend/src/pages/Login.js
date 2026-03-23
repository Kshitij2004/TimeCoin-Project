import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api.js'; // Import our new centralized client
import '../Login.css';

function Login() {
    // 1. Setup state to hold form data
    const [formData, setFormData] = useState({
        username: '',
        password: ''
    });
    const [error, setError] = useState('');
    const navigate = useNavigate();

    // 2. Handle input changes
    const handleChange = (e) => {
        setFormData({
            ...formData,
            [e.target.name]: e.target.value
        });
    };

    // 3. Handle form submission
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(''); // Clear previous errors

        try {
            // Use the centralized api client
            const response = await api.post('/auth/login', {
                username: formData.username,
                password: formData.password
            });

            // Acceptance Criteria: Store JWT in localStorage
            if (response.data && response.data.token) {
                localStorage.setItem('token', response.data.token);
                
                // Redirect to dashboard on success
                navigate('/dashboard');
            }
        } catch (err) {
            // Display error message from backend or a default one
            setError(err.response?.data?.message || 'Login failed. Please check your credentials.');
            console.error('Login error:', err);
        }
    };

    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form onSubmit={handleSubmit}>
                    <h2>Sign in to CrypMart</h2>
                    
                    {/* Error message display */}
                    {error && <div style={{ color: 'red', marginBottom: '10px', textAlign: 'center' }}>{error}</div>}
                    
                    <div className="input-group">
                        <input 
                            type="text" 
                            name="username" 
                            placeholder="Username or Email" 
                            value={formData.username}
                            onChange={handleChange}
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
                            required 
                        />
                    </div>
                    
                    <button type="submit" className="login-btn">LOG IN</button>

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