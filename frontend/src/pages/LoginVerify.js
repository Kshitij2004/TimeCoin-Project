import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api.js';
import '../Login.css';

function LoginVerify() {
    const [code, setCode] = useState('');
    const [trustDevice, setTrustDevice] = useState(false);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const inputRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        // Redirect to login if there's no pending 2FA session
        const tempToken = sessionStorage.getItem('2fa_temp_token');
        if (!tempToken) {
            navigate('/login');
            return;
        }
        // Auto-focus the code input
        inputRef.current?.focus();
    }, [navigate]);

    const handleCodeChange = (e) => {
        // Only allow digits, max 6 characters
        const value = e.target.value.replace(/\D/g, '').slice(0, 6);
        setCode(value);
        setError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (code.length !== 6) {
            setError('Please enter the 6-digit code from your authenticator app.');
            return;
        }

        const tempToken = sessionStorage.getItem('2fa_temp_token');
        if (!tempToken) {
            navigate('/login');
            return;
        }

        setLoading(true);
        try {
            const response = await api.post('/auth/2fa/verify', {
                tempToken,
                code,
                trustDevice,
            });

            const data = response.data;
            const token = data.accessToken || data.token;

            if (token) {
                sessionStorage.removeItem('2fa_temp_token');
                localStorage.setItem('token', token);
                navigate('/dashboard');
            } else {
                setError('Verification succeeded but no token was returned.');
            }
        } catch (err) {
            const message = err.response?.data?.message || 'Invalid code. Please try again.';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    const handleBackToLogin = () => {
        sessionStorage.removeItem('2fa_temp_token');
        navigate('/login');
    };

    return (
        <div className="login-container-wrapper">
            <div className="login-container">
                <form onSubmit={handleSubmit}>
                    <h2>Two-Factor Authentication</h2>

                    <p style={{ color: 'var(--text-muted)', textAlign: 'center', marginBottom: '24px', fontSize: '14px', lineHeight: '1.5' }}>
                        Enter the 6-digit code from your authenticator app to complete sign-in.
                    </p>

                    {error && (
                        <div role="alert" style={{ color: '#ff4d4d', marginBottom: '15px', textAlign: 'center', fontSize: '14px' }}>
                            {error}
                        </div>
                    )}

                    <div className="input-group">
                        <input
                            ref={inputRef}
                            type="text"
                            inputMode="numeric"
                            placeholder="000000"
                            value={code}
                            onChange={handleCodeChange}
                            disabled={loading}
                            autoComplete="one-time-code"
                            style={{ textAlign: 'center', fontSize: '24px', letterSpacing: '8px' }}
                        />
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '20px', cursor: 'pointer' }}
                         onClick={() => setTrustDevice(!trustDevice)}>
                        <input
                            type="checkbox"
                            id="trustDevice"
                            checked={trustDevice}
                            onChange={(e) => setTrustDevice(e.target.checked)}
                            style={{ width: '16px', height: '16px', cursor: 'pointer', accentColor: 'var(--accent-blue)' }}
                        />
                        <label htmlFor="trustDevice" style={{ color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer' }}>
                            Trust this device
                        </label>
                    </div>

                    <button
                        type="submit"
                        className="login-btn"
                        disabled={loading || code.length !== 6}
                    >
                        {loading ? 'VERIFYING...' : 'VERIFY'}
                    </button>

                    <div className="links">
                        <button
                            type="button"
                            onClick={handleBackToLogin}
                            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', fontSize: '14px', cursor: 'pointer', textDecoration: 'none' }}
                        >
                            ← Back to login
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default LoginVerify;