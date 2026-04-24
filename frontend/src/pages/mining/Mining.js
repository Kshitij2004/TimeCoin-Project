import React, { useState, useEffect, useCallback } from 'react';
import api from '../../services/api.js'; 
import { mineCoin } from '../../services/miningService.js';
import './Mining.css';

// Sync interval with Dashboard.js (30 seconds)
const REFRESH_INTERVAL_MS = 30_000;

const Mining = () => {
    const [userWalletAddress, setUserWalletAddress] = useState(null);
    const [balance, setBalance] = useState(null); 
    const [sessionClicks, setSessionClicks] = useState(0); // "Mines This Session"
    
    const [cooldown, setCooldown] = useState(0);
    const [isMining, setIsMining] = useState(false);
    const [error, setError] = useState(null);

    // Syncs Ledger balance with the backend to ensure data consistency
    const fetchWalletInfo = useCallback(async () => {
        try {
            const res = await api.get("/wallet/balance");
            if (res.data?.walletAddress) {
                setUserWalletAddress(res.data.walletAddress);
                setBalance(res.data); 
            }
        } catch (err) {
            setError("Wallet sync paused.");
        }
    }, []);

    // Initial data load on component mount
    useEffect(() => {
        fetchWalletInfo();
    }, [fetchWalletInfo]);

    // Persistent Heartbeat: Keeps the Ledger card updated
    useEffect(() => {
        if (userWalletAddress) {
            const interval = setInterval(fetchWalletInfo, REFRESH_INTERVAL_MS);
            return () => clearInterval(interval);
        }
    }, [userWalletAddress, fetchWalletInfo]);

    // Cooldown Timer Logic for UI feedback
    useEffect(() => {
        if (cooldown > 0) {
            const timer = setInterval(() => setCooldown(q => (q > 0 ? q - 1 : 0)), 1000);
            return () => clearInterval(timer);
        }
    }, [cooldown]);

    const handleMine = async () => {
        if (cooldown > 0 || isMining || !userWalletAddress) return;
        setIsMining(true);
        setError(null);
        try {
            await mineCoin();
            
            // Success Logic: Increment local session count and set cooldown
            setSessionClicks(prev => prev + 1); 
            setCooldown(10); 
            fetchWalletInfo(); // Update Ledger immediately after a successful mint
        } catch (err) {
            setError("Minting failed. Check connection.");
        } finally {
            setIsMining(false);
        }
    };

    return (
        <div className="mining-container">
            <header className="header" style={{ textAlign: 'center', marginBottom: '40px' }}>
                <h1>Mining Center</h1>
                <div className="wallet-address-bar" style={{ opacity: 0.8, marginTop: '10px' }}>
                    <span>Active: </span>
                    <code style={{ color: 'var(--accent-blue)', marginLeft: '10px' }}>
                        {userWalletAddress || 'Connecting...'}
                    </code>
                </div>
            </header>

            {/* Persistence Ledger and Session Counter Cards */}
            <div className="stats-grid" style={{ 
                display: 'grid', 
                gridTemplateColumns: '1fr 1fr', 
                gap: '20px',
                maxWidth: '800px',
                margin: '0 auto'
            }}>
                <div className="stat-card" style={{ borderLeft: '4px solid var(--accent-blue)' }}>
                    <h3>Total Ledger Balance</h3>
                    <div className="value">
                        {balance ? Number(balance.total).toFixed(4) : "0.0000"} <span style={{ fontSize: '1rem' }}>TC</span>
                    </div>
                </div>
                <div className="stat-card" style={{ borderLeft: '4px solid #f39c12' }}>
                    <h3>Mines This Session</h3>
                    <div className="value">{sessionClicks}</div>
                </div>
            </div>

            {/* Centralized Action Card with visual feedback */}
            <div className="mining-card" style={{ 
                marginTop: '40px', 
                padding: '80px', 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'center',
                background: 'linear-gradient(145deg, #1a1a2e 0%, #16213e 100%)',
                boxShadow: '0 20px 40px rgba(0,0,0,0.4)',
                borderRadius: '12px',
                maxWidth: '800px',
                margin: '40px auto',
                position: 'relative'
            }}>
                <div className={`button-glow ${cooldown > 0 ? 'off' : 'on'}`} style={{
                    width: '220px',
                    height: '220px',
                    position: 'absolute',
                    borderRadius: '50%',
                    background: cooldown > 0 ? 'transparent' : 'rgba(52, 152, 219, 0.2)',
                    filter: 'blur(30px)',
                    transition: '0.5s'
                }}></div>
                
                <button 
                    className={`mine-action-button ${cooldown > 0 ? 'disabled' : ''} ${isMining ? 'mining-pulse' : ''}`}
                    onClick={handleMine}
                    disabled={cooldown > 0 || isMining || !userWalletAddress}
                    style={{
                        width: '200px',
                        height: '200px',
                        borderRadius: '50%',
                        border: 'none',
                        background: cooldown > 0 ? '#2c3e50' : 'var(--accent-blue)',
                        color: 'white',
                        fontSize: '1.4rem',
                        fontWeight: '800',
                        cursor: cooldown > 0 ? 'not-allowed' : 'pointer',
                        zIndex: 2,
                        boxShadow: cooldown > 0 ? 'none' : '0 10px 25px rgba(52, 152, 219, 0.4)',
                        transition: 'all 0.3s ease'
                    }}
                >
                    {isMining ? "MINTING..." : cooldown > 0 ? `${cooldown}s` : "MINT TIME"}
                </button>

                {error && (
                    <p style={{ color: '#e74c3c', marginTop: '25px', fontWeight: '500' }}>
                        {error}
                    </p>
                )}
                
                <p style={{ marginTop: '30px', opacity: 0.4, fontSize: '0.9rem' }}>
                    Click to secure the next block and update the ledger.
                </p>
            </div>
        </div>
    );
};

export default Mining;