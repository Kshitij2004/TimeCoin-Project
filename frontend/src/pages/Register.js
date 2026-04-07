import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../services/api.js';
import '../Login.css'; 

const Register = () => {
    const navigate = useNavigate();
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const validateForm = () => {
        const { username, email, password, confirmPassword } = formData;

        // 1. Basic Check
        if (!username || !email || !password || !confirmPassword) {
            return "All fields are required.";
        }

        // 2. Email Validation (Standard format)
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            return "Email must match standard format (e.g., name@domain.com).";
        }

        // 3. Username Validation (3-20 chars, alphanumeric only)
        const usernameRegex = /^[a-zA-Z0-9]{3,20}$/;
        if (!usernameRegex.test(username)) {
            return "Username must be 3-20 characters and alphanumeric only.";
        }

        // 4. Password Validation (8+ chars, 1 upper, 1 lower, 1 number)
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
        if (!passwordRegex.test(password)) {
            return "Password must be at least 8 characters long and include an uppercase letter, a lowercase letter, and a number.";
        }

        // 5. Match Check
        if (password !== confirmPassword) {
            return "Passwords do not match.";
        }

        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        
        const validationError = validateForm();
        if (validationError) {
            setError(validationError);
            return;
        }

        setLoading(true);
        try {
            await registerUser({
                username: formData.username,
                email: formData.email,
                password: formData.password
            });
            
            alert("Account created! Redirecting to login...");
            navigate('/login');
        } catch (err) {
            setError(err.response?.data?.message || "Registration failed. Try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form onSubmit={handleSubmit}>
                    <h2>Create an Account</h2>
                    
                    {error && (
                        <div style={{ 
                            color: '#ff4d4d', 
                            backgroundColor: '#ffe6e6', 
                            padding: '10px', 
                            borderRadius: '5px', 
                            marginBottom: '15px', 
                            fontSize: '14px',
                            textAlign: 'center',
                            border: '1px solid #ffcccc'
                        }}>
                            {error}
                        </div>
                    )}

                    <div className="input-group">
                        <input 
                            name="username" 
                            placeholder="Username" 
                            onChange={handleChange} 
                            value={formData.username}
                            required 
                        />
                    </div>

                    <div className="input-group">
                        <input 
                            name="email" 
                            type="email" 
                            placeholder="Email" 
                            onChange={handleChange} 
                            value={formData.email}
                            required 
                        />
                    </div>

                    <div className="input-group">
                        <input 
                            name="password" 
                            type="password" 
                            placeholder="Password" 
                            onChange={handleChange} 
                            value={formData.password}
                            required 
                        />
                    </div>

                    <div className="input-group">
                        <input 
                            name="confirmPassword" 
                            type="password" 
                            placeholder="Confirm Password" 
                            onChange={handleChange} 
                            value={formData.confirmPassword}
                            required 
                        />
                    </div>
                    
                    <button type="submit" className="login-btn" disabled={loading}>
                        {loading ? "REGISTERING..." : "REGISTER"}
                    </button>

                    <div className="links">
                        <div className="register-text">
                            Already have an account? 
                            <Link to="/login" className="register-link"> Login here</Link>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default Register;