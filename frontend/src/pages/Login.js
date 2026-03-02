import React from 'react';
import { Link } from 'react-router-dom';
import '../Login.css';

function Login() {
    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form>
                    <h2>Sign in to CrypMart</h2>
                    
                    <div className="input-group">
                        <input type="text" name="username" placeholder="Username or Email" required />
                    </div>
                    
                    <div className="input-group">
                        <input type="password" name="password" placeholder="Password" required />
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