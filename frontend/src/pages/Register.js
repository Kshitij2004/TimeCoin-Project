import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../services/api.js';
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

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (formData.password !== formData.confirmPassword) {
            setError('Passwords do not match.');
            return;
        }

        setLoading(true);
        try {
            const response = await api.post('/auth/register', {
                username: formData.username,
                email: formData.email,
                password: formData.password,
            });

            // 2FA is enabled by default — show QR code so user can set up authenticator
            if (response.data.otpAuthUri) {
                setOtpAuthUri(response.data.otpAuthUri);
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

    const qrCodeUrl = otpAuthUri
        ? `https://chart.googleapis.com/chart?cht=qr&chs=220x220&chl=${encodeURIComponent(otpAuthUri)}`
        : null;

    // QR code setup screen shown after successful registration
    if (otpAuthUri) {
        return (
            <div className="login-container-wrapper">
                <div className="login-container" style={{ maxWidth: '480px' }}>
                    <h2>Set Up Two-Factor Authentication</h2>

                    <p style={{ color: 'var(--text-muted)', textAlign: 'center', marginBottom: '20px', fontSize: '14px', lineHeight: '1.6' }}>
                        Your account has 2FA enabled by default. Scan this QR code with Google Authenticator or Authy to complete setup.
                    </p>

                    <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '20px' }}>
                        <img
                            src={qrCodeUrl}
                            alt="2FA QR Code"
                            style={{ borderRadius: '8px', border: '4px solid white' }}
                        />
                    </div>

                    <p style={{ color: 'var(--text-muted)', textAlign: 'center', fontSize: '13px', marginBottom: '28px', lineHeight: '1.5' }}>
                        After scanning, you'll receive a 6-digit code in your authenticator app. You'll need this code every time you log in.
                    </p>

                    <button
                        className="login-btn"
                        onClick={() => navigate('/login')}
                    >
                        I'VE SCANNED THE CODE — GO TO LOGIN
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form onSubmit={handleSubmit}>
                    <h2>Create Account</h2>

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
                            type="email"
                            name="email"
                            placeholder="Email"
                            value={formData.email}
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

                    <div className="input-group">
                        <input
                            type="password"
                            name="confirmPassword"
                            placeholder="Confirm Password"
                            value={formData.confirmPassword}
                            onChange={handleChange}
                            disabled={loading}
                            required
                        />
                    </div>

                    <button type="submit" className="login-btn" disabled={loading}>
                        {loading ? 'CREATING ACCOUNT...' : 'CREATE ACCOUNT'}
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