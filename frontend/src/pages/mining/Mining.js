import React, { useState, useEffect, useCallback } from 'react';
import api from '../../services/api.js'; 
import { mineCoin, fetchMiningStats } from '../../services/miningService.js';
import './Mining.css';

const Mining = () => {
    const [userWalletAddress, setUserWalletAddress] = useState(null);
    const [stats, setStats] = useState({
        totalCoinsMined: 0,
        totalMineCount: 0,
        secondsRemaining: 0
    });
    
    const [cooldown, setCooldown] = useState(0);
    const [isMining, setIsMining] = useState(false);
    const [error, setError] = useState(null);

    // 1. Fetch Wallet Info (Same as Dashboard)
    const fetchWalletInfo = useCallback(async () => {
        try {
            const res = await api.get("/wallet/balance");
            if (res.data?.walletAddress) {
                setUserWalletAddress(res.data.walletAddress);
            }
        } catch (err) {
            setError("Could not sync wallet info.");
        }
    }, []);

    // 2. Fetch Stats with Defensive Merging
    const loadMiningData = useCallback(async (address) => {
        if (!address) return;
        try {
            const res = await fetchMiningStats(address);
            
            // DEBUG: Open your browser console to see the real data structure!
            console.log("Backend Stats Response:", res.data);

            // Use functional update to merge data safely
            setStats(prev => ({
                ...prev,
                // Fallback to previous values if backend fields are missing
                totalCoinsMined: res.data?.totalCoinsMined ?? prev.totalCoinsMined,
                totalMineCount: res.data?.totalMineCount ?? prev.totalMineCount,
            }));

            if (res.data?.secondsRemaining !== undefined) {
                setCooldown(res.data.secondsRemaining);
            }
        } catch (err) {
            console.error("Mining stats failed:", err);
        }
    }, []);

    useEffect(() => {
        fetchWalletInfo();
    }, [fetchWalletInfo]);

    useEffect(() => {
        if (userWalletAddress) {
            loadMiningData(userWalletAddress);
            const interval = setInterval(() => loadMiningData(userWalletAddress), 15000);
            return () => clearInterval(interval);
        }
    }, [userWalletAddress, loadMiningData]);

    useEffect(() => {
        if (cooldown > 0) {
            const timer = setInterval(() => setCooldown(q => (q > 0 ? q - 1 : 0)), 1000);
            return () => clearInterval(timer);
        }
    }, [cooldown]);

    const handleMine = async () => {
        if (cooldown > 0 || isMining || !userWalletAddress) return;
        setIsMining(true);
        try {
            await mineCoin();
            setStats(prev => ({ ...prev, totalMineCount: prev.totalMineCount + 1 }));
            setCooldown(5); 
        } catch (err) {
            setError("Mining failed. Check connection.");
        } finally {
            setIsMining(false);
        }
    };

    return (
        <div className="mining-page">
            <h1 className="mining-header">Mining Center</h1>

            <div className="wallet-info-bar">
                <span>Active Wallet: <span className="address-text">{userWalletAddress || 'Fetching...'}</span></span>
                <span className="status-badge">{userWalletAddress ? 'CONNECTED' : 'SYNCING'}</span>
            </div>

            <div className="mining-stats-grid">
                <div className="stat-card">
                    <p className="stat-label">Total Mined (Ledger)</p>
                    <p className="stat-value">
                        {/* Defensive check: Convert to Number and fallback to 0 before toFixed */}
                        {Number(stats?.totalCoinsMined || 0).toFixed(4)} TC
                    </p>
                </div>
                <div className="stat-card">
                    <p className="stat-label">Mining Sessions</p>
                    <p className="stat-value">{stats?.totalMineCount || 0}</p>
                </div>
                <div className="stat-card">
                    <p className="stat-label">Est. Next Reward</p>
                    <p className="stat-value reward-text">+0.50 TC</p>
                </div>
                <div className="stat-card">
                    <p className="stat-label">System Status</p>
                    <p className="stat-value status-active">OPERATIONAL</p>
                </div>
            </div>

            <div className="mining-main-content">
                <div className="mining-card action-container">
                    <div className={`button-glow ${cooldown > 0 ? 'off' : 'on'}`}></div>
                    <button 
                        className={`mine-action-button ${cooldown > 0 ? 'disabled' : ''} ${isMining ? 'mining-pulse' : ''}`}
                        onClick={handleMine}
                        disabled={cooldown > 0 || isMining || !userWalletAddress}
                    >
                        {isMining ? "MINING..." : cooldown > 0 ? `${cooldown}s` : "MINE TIME"}
                    </button>
                    {error && <p className="error-message">{error}</p>}
                </div>

                <div className="mining-card history-container">
                    <h3 className="card-title">Mining Log</h3>
                    <div className="log-list">
                        <div className="log-item">
                            <span className="log-status confirmed">●</span>
                            <span className="log-text">Backend communication established</span>
                        </div>
                        <div className="log-item">
                            <span className="log-status pending">●</span>
                            <span className="log-text">Aggregation scheduler active</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Mining;