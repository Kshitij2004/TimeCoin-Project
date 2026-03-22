import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../services/api.js';
import '../Login.css'; // Importing your existing Login CSS for consistent styling

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
        if (!formData.username || !formData.email || !formData.password) return "All fields are required.";
        if (!/\S+@\S+\.\S+/.test(formData.email)) return "Invalid email format.";
        if (formData.password !== formData.confirmPassword) return "Passwords do not match.";
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
            // Acceptance Criteria: Uses shared api utility via registerUser
            await registerUser({
                username: formData.username,
                email: formData.email,
                password: formData.password
            });
            
            alert("Account created! Redirecting to login...");
            navigate('/login');
        } catch (err) {
            // Captures global error handling from axios interceptor
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
                        <div style={{ color: 'red', marginBottom: '10px', textAlign: 'center' }}>
                            {error}
                        </div>
                    )}

                    <div className="input-group">
                        <input 
                            name="username" 
                            placeholder="Username" 
                            onChange={handleChange} 
                            required 
                        />
                    </div>

                    <div className="input-group">
                        <input 
                            name="email" 
                            type="email" 
                            placeholder="Email" 
                            onChange={handleChange} 
                            required 
                        />
                    </div>

                    <div className="input-group">
                        <input 
                            name="password" 
                            type="password" 
                            placeholder="Password" 
                            onChange={handleChange} 
                            required 
                        />
                    </div>

                    <div className="input-group">
                        <input 
                            name="confirmPassword" 
                            type="password" 
                            placeholder="Confirm Password" 
                            onChange={handleChange} 
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