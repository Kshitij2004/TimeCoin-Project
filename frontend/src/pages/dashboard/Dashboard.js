import { useState, useEffect, useCallback } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api.js";
import "./Dashboard.css";

// Poll interval for auto-refresh (30 seconds)
const REFRESH_INTERVAL_MS = 30_000;

function Dashboard() {
    const [balance, setBalance] = useState(null);
    const [coin, setCoin] = useState(null);
    const [priceHistory, setPriceHistory] = useState([]);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [balanceError, setBalanceError] = useState(null);
    const [hasPending, setHasPending] = useState(false);
    const [copied, setCopied] = useState(false);

    // Stake/unstake form state
    const [stakeAmount, setStakeAmount] = useState("");
    const [unstakeAmount, setUnstakeAmount] = useState("");
    const [stakeLoading, setStakeLoading] = useState(false);
    const [unstakeLoading, setUnstakeLoading] = useState(false);
    const [stakeStatus, setStakeStatus] = useState(null);

    // Refresh balance + coin price (polled)
    const refreshData = useCallback(async () => {
        try {
            const balanceRes = await api.get("/wallet/balance");
            setBalance(balanceRes.data);
        } catch (err) {
            setBalanceError(err.response?.data?.message || "Failed to load balance.");
        }

        try {
            const coinRes = await api.get("/coin");
            setCoin(coinRes.data);
        } catch {}
    }, []);

    // Initial data load
    useEffect(() => {
        const init = async () => {
            await refreshData();

            // Price history for the chart
            try {
                const historyRes = await api.get("/coins/price-history?range=30d");
                setPriceHistory(historyRes.data || []);
            } catch {}

            // Recent transactions
            try {
                const txnRes = await api.get("/wallet/transactions?page=1&limit=10");
                let txnList = [];
                if (txnRes.data && txnRes.data.data) {
                    txnList = txnRes.data.data;
                } else if (Array.isArray(txnRes.data)) {
                    txnList = txnRes.data;
                }
                setTransactions(txnList);
                setHasPending(txnList.some(
                    (tx) => tx.status && tx.status.toLowerCase() === "pending"
                ));
            } catch {}

            setLoading(false);
        };

        init();
    }, [refreshData]);

    // Auto-refresh balance and price every 30 seconds
    useEffect(() => {
        const interval = setInterval(refreshData, REFRESH_INTERVAL_MS);
        return () => clearInterval(interval);
    }, [refreshData]);

    const handleCopyAddress = () => {
        if (balance?.walletAddress) {
            navigator.clipboard.writeText(balance.walletAddress).then(() => {
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
            });
        }
    };

    const handleStake = async (e) => {
        e.preventDefault();
        setStakeStatus(null);
        const amount = parseFloat(stakeAmount);
        if (!amount || amount <= 0) {
            setStakeStatus({ type: "error", message: "Enter an amount greater than 0." });
            return;
        }
        if (balance && amount > balance.available) {
            setStakeStatus({ type: "error", message: `Insufficient available balance (${Number(balance.available).toFixed(4)} TC).` });
            return;
        }

        setStakeLoading(true);
        try {
            const res = await api.post("/staking/stake", { amount });
            setBalance({
                walletAddress: res.data.walletAddress,
                available: res.data.available,
                staked: res.data.staked,
                total: res.data.total,
            });
            setStakeStatus({ type: "success", message: `Staked ${amount} TC successfully.` });
            setStakeAmount("");
        } catch (err) {
            setStakeStatus({ type: "error", message: err.response?.data?.message || "Stake failed." });
        } finally {
            setStakeLoading(false);
        }
    };

    const handleUnstake = async (e) => {
        e.preventDefault();
        setStakeStatus(null);
        const amount = parseFloat(unstakeAmount);
        if (!amount || amount <= 0) {
            setStakeStatus({ type: "error", message: "Enter an amount greater than 0." });
            return;
        }
        if (balance && amount > balance.staked) {
            setStakeStatus({ type: "error", message: `Insufficient staked balance (${Number(balance.staked).toFixed(4)} TC).` });
            return;
        }

        setUnstakeLoading(true);
        try {
            const res = await api.post("/staking/unstake", { amount });
            setBalance({
                walletAddress: res.data.walletAddress,
                available: res.data.available,
                staked: res.data.staked,
                total: res.data.total,
            });
            setStakeStatus({ type: "success", message: `Unstaked ${amount} TC successfully.` });
            setUnstakeAmount("");
        } catch (err) {
            setStakeStatus({ type: "error", message: err.response?.data?.message || "Unstake failed." });
        } finally {
            setUnstakeLoading(false);
        }
    };

    const formatTimestamp = (timestamp) => {
        if (!timestamp) return "—";
        const date = new Date(timestamp);
        return date.toLocaleDateString("en-US", {
            month: "short",
            day: "numeric",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit",
        });
    };

    const getStatusClass = (status) => {
        if (!status) return "";
        switch (status.toLowerCase()) {
            case "completed":
            case "confirmed":
                return "status-completed";
            case "pending":
                return "status-pending";
            case "failed":
                return "status-failed";
            default:
                return "";
        }
    };

    // Build SVG path from priceHistory data
    const buildChartPath = () => {
        if (!priceHistory.length) return "";
        const prices = priceHistory.map((p) => Number(p.price));
        const min = Math.min(...prices);
        const max = Math.max(...prices);
        const range = max - min || 1;

        const W = 1000;
        const H = 200;
        const padX = 10;
        const padY = 20;

        const step = priceHistory.length > 1 ? (W - padX * 2) / (priceHistory.length - 1) : 0;

        return prices
            .map((price, i) => {
                const x = padX + i * step;
                const y = H - padY - ((price - min) / range) * (H - padY * 2);
                return `${i === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`;
            })
            .join(" ");
    };

    // Donut chart for available vs staked
    const renderDonut = () => {
        if (!balance) return null;
        const available = Number(balance.available) || 0;
        const staked = Number(balance.staked) || 0;
        const total = available + staked;

        if (total === 0) {
            return (
                <div className="donut-container">
                    <svg viewBox="0 0 120 120" className="donut-svg">
                        <circle cx="60" cy="60" r="50" fill="none" stroke="var(--border-color)" strokeWidth="14" />
                    </svg>
                    <div className="donut-label">
                        <span className="donut-value">0</span>
                        <span className="donut-sub">TC total</span>
                    </div>
                </div>
            );
        }

        const circumference = 2 * Math.PI * 50;
        const availableLen = (available / total) * circumference;
        const stakedLen = (staked / total) * circumference;

        return (
            <div className="donut-container">
                <svg viewBox="0 0 120 120" className="donut-svg">
                    {/* Available segment */}
                    <circle
                        cx="60" cy="60" r="50"
                        fill="none"
                        stroke="var(--accent-blue)"
                        strokeWidth="14"
                        strokeDasharray={`${availableLen} ${circumference}`}
                        transform="rotate(-90 60 60)"
                    />
                    {/* Staked segment */}
                    <circle
                        cx="60" cy="60" r="50"
                        fill="none"
                        stroke="#f39c12"
                        strokeWidth="14"
                        strokeDasharray={`${stakedLen} ${circumference}`}
                        strokeDashoffset={-availableLen}
                        transform="rotate(-90 60 60)"
                    />
                </svg>
                <div className="donut-label">
                    <span className="donut-value">{total.toFixed(2)}</span>
                    <span className="donut-sub">TC total</span>
                </div>
            </div>
        );
    };

    const handleLogout = () => {
        localStorage.removeItem("token");
        window.location.href = "/login";
    };

    return (
        <div className="dashboard-page">
            {/* Sidebar — issue #114: links to all pages, active highlight */}
            <aside className="sidebar">
                <div className="logo">CrypMart</div>
                <nav className="nav-links">
                    <Link to="/dashboard" className="nav-link active">Dashboard</Link>
                    <Link to="/marketplace" className="nav-link">Marketplace</Link>
                    <Link to="/send" className="nav-link">Send</Link>
                    <Link to="/history" className="nav-link">Detailed Wallet</Link>
                    <Link to="/blockchain" className="nav-link">Block Explorer</Link>
                    <Link to="/about" className="nav-link">About</Link>
                    <button
                        onClick={handleLogout}
                        className="nav-link logout-btn"
                        style={{ background: "none", border: "none", textAlign: "left", cursor: "pointer" }}
                    >
                        Log Out
                    </button>
                </nav>
            </aside>

            <main className="main-content">
                <header className="header">
                    <h1>Welcome back</h1>
                </header>

                {loading ? (
                    <div className="loading-container">
                        <div className="spinner"></div>
                        <p>Loading dashboard...</p>
                    </div>
                ) : (
                    <>
                        {/* Wallet address bar */}
                        {balance?.walletAddress && (
                            <div className="wallet-address-bar">
                                <span className="wallet-address-label">Wallet Address:</span>
                                <code className="wallet-address-value">{balance.walletAddress}</code>
                                <button className="copy-btn" onClick={handleCopyAddress}>
                                    {copied ? "✓ Copied" : "Copy"}
                                </button>
                            </div>
                        )}

                        {/* Pending transactions indicator */}
                        {hasPending && (
                            <div className="pending-banner" role="alert">
                                <span className="pending-icon">⏳</span>
                                You have unconfirmed outgoing transactions. Your available balance may change once they are confirmed.
                            </div>
                        )}

                        {/* Balance breakdown */}
                        <div className="stats-grid">
                            <div className="stat-card">
                                <h3>Available Balance</h3>
                                <div className="value" data-testid="available-balance">
                                    {balance ? Number(balance.available).toFixed(4) + " TC" : "0.0000 TC"}
                                </div>
                                {balanceError && (
                                    <div className="sub-value" style={{ color: "#e74c3c" }}>
                                        {balanceError}
                                    </div>
                                )}
                            </div>
                            <div className="stat-card">
                                <h3>Staked</h3>
                                <div className="value" data-testid="staked-balance">
                                    {balance ? Number(balance.staked).toFixed(4) + " TC" : "0.0000 TC"}
                                </div>
                            </div>
                            <div className="stat-card">
                                <h3>Total Balance</h3>
                                <div className="value" data-testid="total-balance">
                                    {balance ? Number(balance.total).toFixed(4) + " TC" : "0.0000 TC"}
                                </div>
                            </div>
                            <div className="stat-card">
                                <h3>Current Coin Price</h3>
                                <div className="value">
                                    {coin ? "$" + Number(coin.currentPrice).toFixed(2) : "—"}
                                </div>
                            </div>
                        </div>

                        {/* Stake / Unstake section */}
                        <div className="staking-section">
                            <h2 className="section-header">Staking</h2>
                            <div className="staking-grid">
                                {/* Donut visualization */}
                                <div className="staking-card">
                                    <h3>Breakdown</h3>
                                    {renderDonut()}
                                    <div className="donut-legend">
                                        <div className="legend-item">
                                            <span className="legend-dot" style={{ background: "var(--accent-blue)" }}></span>
                                            <span>Available: {balance ? Number(balance.available).toFixed(2) : "0.00"} TC</span>
                                        </div>
                                        <div className="legend-item">
                                            <span className="legend-dot" style={{ background: "#f39c12" }}></span>
                                            <span>Staked: {balance ? Number(balance.staked).toFixed(2) : "0.00"} TC</span>
                                        </div>
                                    </div>
                                </div>

                                {/* Stake form */}
                                <div className="staking-card">
                                    <h3>Stake TC</h3>
                                    <form onSubmit={handleStake} className="staking-form">
                                        <input
                                            type="number"
                                            step="0.01"
                                            min="0"
                                            placeholder="Amount"
                                            value={stakeAmount}
                                            onChange={(e) => { setStakeAmount(e.target.value); setStakeStatus(null); }}
                                            disabled={stakeLoading}
                                            className="staking-input"
                                            data-testid="stake-input"
                                        />
                                        <button
                                            type="submit"
                                            className="staking-btn stake-btn"
                                            disabled={stakeLoading || !stakeAmount}
                                            data-testid="stake-button"
                                        >
                                            {stakeLoading ? "Staking..." : "Stake"}
                                        </button>
                                    </form>
                                </div>

                                {/* Unstake form */}
                                <div className="staking-card">
                                    <h3>Unstake TC</h3>
                                    <form onSubmit={handleUnstake} className="staking-form">
                                        <input
                                            type="number"
                                            step="0.01"
                                            min="0"
                                            placeholder="Amount"
                                            value={unstakeAmount}
                                            onChange={(e) => { setUnstakeAmount(e.target.value); setStakeStatus(null); }}
                                            disabled={unstakeLoading}
                                            className="staking-input"
                                            data-testid="unstake-input"
                                        />
                                        <button
                                            type="submit"
                                            className="staking-btn unstake-btn"
                                            disabled={unstakeLoading || !unstakeAmount}
                                            data-testid="unstake-button"
                                        >
                                            {unstakeLoading ? "Unstaking..." : "Unstake"}
                                        </button>
                                    </form>
                                </div>
                            </div>

                            {stakeStatus && (
                                <div
                                    className={`status-message status-${stakeStatus.type}`}
                                    role="alert"
                                    data-testid="staking-status"
                                >
                                    {stakeStatus.message}
                                </div>
                            )}
                        </div>

                        {/* Price chart — issue #114: real data from price-history API */}
                        <div className="chart-section">
                            <h3>Price Trend (30 Days)</h3>
                            <div className="chart-container">
                                {priceHistory.length > 0 ? (
                                    <svg className="line-graph" viewBox="0 0 1000 200" preserveAspectRatio="none">
                                        <path className="line-path" d={buildChartPath()} />
                                    </svg>
                                ) : (
                                    <p className="empty-state">No price history available yet.</p>
                                )}
                            </div>
                        </div>

                        {/* Recent Transactions */}
                        <h2 className="section-header">Recent Transactions</h2>
                        {transactions.length === 0 ? (
                            <p className="empty-state">No recent transactions.</p>
                        ) : (
                            <div className="transactions-table-wrapper">
                                <table className="transactions-table">
                                    <thead>
                                        <tr>
                                            <th>Type</th>
                                            <th>Amount</th>
                                            <th>Status</th>
                                            <th>Date</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {transactions.map((tx, index) => (
                                            <tr key={tx.id || index}>
                                                <td className="tx-type">{tx.type || "—"}</td>
                                                <td className="tx-amount">
                                                    {tx.amount != null ? Number(tx.amount).toFixed(2) + " TC" : "—"}
                                                </td>
                                                <td>
                                                    <span className={"tx-status " + getStatusClass(tx.status)}>
                                                        {tx.status || "—"}
                                                    </span>
                                                </td>
                                                <td className="tx-date">
                                                    {formatTimestamp(tx.timestamp || tx.createdAt)}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </>
                )}
            </main>
        </div>
    );
}

export default Dashboard;