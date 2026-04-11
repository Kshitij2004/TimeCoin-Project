import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../services/api.js';
import '../Login.css'; 

const Register = () => {
    // Hooks for navigation and local component state
    const navigate = useNavigate(); // Used to redirect the user after successful registration
    
    // formData holds all the values from the input fields in a single object
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: ''
    });

    // Local state for UI feedback: error messages and the loading spinner status
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    /**
     * Updates the formData state whenever a user types in an input field.
     * It uses the 'name' attribute of the input to dynamically update the correct key.
     */
    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    /**
     * Client-side validation logic. 
     * Running this before calling the API saves server resources and provides instant feedback.
     */
    const validateForm = () => {
        const { username, email, password, confirmPassword } = formData;

        // 1. Basic Check: Ensure no fields are empty
        if (!username || !email || !password || !confirmPassword) {
            return "All fields are required.";
        }

        // 2. Email Validation: Checks for a standard name@domain.com structure
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            return "Email must match standard format (e.g., name@domain.com).";
        }

        // 3. Username Validation: Ensures length and prevents special characters (security best practice)
        const usernameRegex = /^[a-zA-Z0-9]{3,20}$/;
        if (!usernameRegex.test(username)) {
            return "Username must be 3-20 characters and alphanumeric only.";
        }

        // 4. Password Complexity: Enforces 1 Upper, 1 Lower, 1 Number, and 8+ length
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
        if (!passwordRegex.test(password)) {
            return "Password must be at least 8 characters long and include an uppercase letter, a lowercase letter, and a number.";
        }

        // 5. Confirmation Match: Prevents typos in the password field
        if (password !== confirmPassword) {
            return "Passwords do not match.";
        }

        return null; // No errors found
    };

    /**
     * Handles the form submission process
     */
    const handleSubmit = async (e) => {
        e.preventDefault(); // Prevents the browser from refreshing the page
        setError(''); // Reset error state on a new attempt
        
        // Step A: Run local validation
        const validationError = validateForm();
        if (validationError) {
            setError(validationError);
            return;
        }

        // Step B: Set loading to true to disable the button and show "Registering..."
        setLoading(true);
        try {
            // Step C: Send the data to the Java/Spring Boot backend
            // Note: We don't send confirmPassword to the backend, only the core data.
            await registerUser({
                username: formData.username,
                email: formData.email,
                password: formData.password
            });
            
            // Step D: Success feedback and navigation
            alert("Account created! Redirecting to login...");
            navigate('/login');
        } catch (err) {
            // Step E: Error handling. Extracts the error message from the backend response if available.
            setError(err.response?.data?.message || "Registration failed. Try again.");
        } finally {
            // Step F: Always stop the loading state, whether the call succeeded or failed
            setLoading(false);
        }
    };

    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form onSubmit={handleSubmit}>
                    <h2>Create an Account</h2>
                    
                    {/* Conditional Rendering: Only show this div if there is an error message */}
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

                    {/* Input Groups: 'name' must match the keys in our formData state */}
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
                    
                    {/* The button is disabled during the API call to prevent duplicate submissions */}
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