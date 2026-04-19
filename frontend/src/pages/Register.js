import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerUser } from '../services/api.js';
import '../Login.css';

function Register() {
    const [formData, setFormData] = useState({
        username: '',
        email: '',
        password: '',
        confirmPassword: '',
    });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [otpAuthUri, setOtpAuthUri] = useState(null);
    const navigate = useNavigate();

    const handleChange = (e) => {
        setFormData({ ...formData, [e.target.name]: e.target.value });
    };

    const validate = () => {
        const { username, email, password, confirmPassword } = formData;

        if (!username || !email || !password || !confirmPassword) {
            return 'All fields are required.';
        }

        if (username.length < 3 || username.length > 20) {
            return 'Username must be 3-20 characters long.';
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            return 'Email must match standard format (e.g. user@example.com).';
        }

        if (password.length < 8 || !/[A-Z]/.test(password) || !/[a-z]/.test(password) || !/[0-9]/.test(password)) {
            return 'Password must be at least 8 characters and contain uppercase, lowercase, and a number.';
        }

        if (password !== confirmPassword) {
            return 'Passwords do not match.';
        }

        return null;
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        const validationError = validate();
        if (validationError) {
            setError(validationError);
            return;
        }

        setLoading(true);
        try {
            const data = await registerUser({
                username: formData.username,
                email: formData.email,
                password: formData.password,
            });

            // 2FA is enabled by default — show QR code so user can set up authenticator
            if (data?.otpAuthUri) {
                setOtpAuthUri(data.otpAuthUri);
            } else {
                navigate('/login');
            }
        } catch (err) {
            const message = err.response?.data?.message || 'Registration failed. Please try again.';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    // Extract secret from otpauth URI for manual entry fallback
    const secret = otpAuthUri
        ? (otpAuthUri.match(/secret=([A-Z0-9]+)/)?.[1] || '')
        : '';

    const qrCodeUrl = otpAuthUri
        ? `https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=${encodeURIComponent(otpAuthUri)}`
        : null;

    // QR code setup screen shown after successful registration
    if (otpAuthUri) {
        return (
            <div className="login-container-wrapper">
                <div className="login-container" style={{ maxWidth: '480px' }}>
                    <h2>Set Up Two-Factor Authentication</h2>

                    <p style={{ color: 'var(--text-muted)', textAlign: 'center', marginBottom: '20px', fontSize: '14px', lineHeight: '1.6' }}>
                        Your account has 2FA enabled by default. Scan this QR code with Google Authenticator or Authy.
                    </p>

                    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '20px' }}>
                        <img
                            src={qrCodeUrl}
                            alt="2FA QR Code"
                            style={{ borderRadius: '8px', border: '4px solid white', background: 'white' }}
                        />
                    </div>

                    <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '13px', marginBottom: '12px' }}>
                        Or enter this code manually in your authenticator app:
                    </p>

                    <div style={{
                        background: 'var(--bg-dark)',
                        border: '1px solid var(--input-border)',
                        borderRadius: '6px',
                        padding: '12px',
                        textAlign: 'center',
                        fontFamily: 'monospace',
                        fontSize: '16px',
                        color: 'var(--accent-blue)',
                        letterSpacing: '2px',
                        marginBottom: '20px',
                        wordBreak: 'break-all'
                    }}>
                        {secret}
                    </div>

                    <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '13px', marginBottom: '28px', lineHeight: '1.5' }}>
                        After scanning or entering the code, you'll need the 6-digit code from the app every time you log in.
                    </p>

                    <button
                        className="login-btn"
                        onClick={() => navigate('/login')}
                    >
                        CONTINUE TO LOGIN
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form onSubmit={handleSubmit}>
                    <h2>Register</h2>

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
                        />
                    </div>

                    <div className="input-group">
                        <input
                            type="email"
                            name="email"
                            placeholder="Email"
                            value={formData.email}
                            onChange={handleChange}
                            disabled={loading}
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
                        />
                    </div>

                    <div className="input-group">
                        <input
                            type="password"
                            name="confirmPassword"
                            placeholder="Confirm Password"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            disabled={loading}
                        />
                    </div>

                    <button type="submit" className="login-btn" disabled={loading}>
                        {loading ? 'REGISTERING...' : 'REGISTER'}
                    </button>

                    <div className="links">
                        <div className="register-text">
                            Already have an account?{' '}
                            <Link to="/login" className="register-link">Sign in</Link>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default Register;