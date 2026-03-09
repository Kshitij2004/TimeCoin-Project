import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { registerUser } from '../services/api';

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
        // Required Fields & Email Format
        if (!formData.username || !formData.email || !formData.password) return "All fields are required.";
        if (!/\S+@\S+\.\S+/.test(formData.email)) return "Invalid email format.";
        
        // Matching Passwords
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
            // Calls registration API
            await registerUser({
                username: formData.username,
                email: formData.email,
                password: formData.password
            });
            
            // Redirects to login on success
            alert("Account created! Redirecting to login...");
            navigate('/login');
        } catch (err) {
            // Shows error message from Java backend
            setError(err.response?.data?.message || "Registration failed. Try again.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: '400px', margin: 'auto', padding: '20px' }}>
            <h2>Create an Account</h2>
            <form onSubmit={handleSubmit}>
                <input name="username" placeholder="Username" onChange={handleChange} style={inputStyle} />
                <input name="email" type="email" placeholder="Email" onChange={handleChange} style={inputStyle} />
                <input name="password" type="password" placeholder="Password" onChange={handleChange} style={inputStyle} />
                <input name="confirmPassword" type="password" placeholder="Confirm Password" onChange={handleChange} style={inputStyle} />
                
                <button type="submit" disabled={loading} style={buttonStyle}>
                    {loading ? "Registering..." : "Register"}
                </button>
            </form>
            {error && <p style={{ color: 'red' }}>{error}</p>}
        </div>
    );
};

const inputStyle = { display: 'block', width: '100%', marginBottom: '10px', padding: '8px' };
const buttonStyle = { width: '100%', padding: '10px', cursor: 'pointer' };

export default Register;