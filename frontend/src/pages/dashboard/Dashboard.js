import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api.js";
import "./Dashboard.css";

function Dashboard() {
    const [balance, setBalance] = useState(null);
    const [coin, setCoin] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [balanceError, setBalanceError] = useState(null);
    const [txnError, setTxnError] = useState(null);
    const [hasPending, setHasPending] = useState(false);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        const fetchDashboardData = async () => {
            // Fetch ledger-derived balance (issue #120)
            // Uses JWT to identify user — no wallet address needed
            try {
                const balanceRes = await api.get("/wallet/balance");
                setBalance(balanceRes.data);
            } catch (err) {
                setBalanceError(err.response?.data?.message || "Failed to load balance.");
            }

            // Fetch coin price
            try {
                const coinRes = await api.get("/coin");
                setCoin(coinRes.data);
            } catch {}

            // Fetch transactions
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
            } catch (err) {
                setTxnError(err.response?.data?.message || "Failed to load transactions.");
            }

            setLoading(false);
        };

        fetchDashboardData();
    }, []);

    const handleCopyAddress = () => {
        if (balance?.walletAddress) {
            navigator.clipboard.writeText(balance.walletAddress).then(() => {
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
            });
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

    const renderWalletSection = () => {
        if (loading) {
            return (
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Loading wallet...</p>
                </div>
            );
        }

        if (balanceError && !balance) {
            return (
                <div className="error-container">
                    <p className="error-message">Error: {balanceError}</p>
                    <button className="retry-btn" onClick={() => window.location.reload()}>
                        Retry
                    </button>
                </div>
            );
        }

        return (
            <>
                {balance?.walletAddress && (
                    <div className="wallet-address-bar">
                        <span className="wallet-address-label">Wallet Address:</span>
                        <code className="wallet-address-value">{balance.walletAddress}</code>
                        <button className="copy-btn" onClick={handleCopyAddress}>
                            {copied ? "✓ Copied" : "Copy"}
                        </button>
                    </div>
                )}

                {/* Pending transactions indicator (issue #120) */}
                {hasPending && (
                    <div className="pending-banner" role="alert">
                        <span className="pending-icon">⏳</span>
                        You have unconfirmed outgoing transactions. Your available balance may change once they are confirmed.
                    </div>
                )}

                {/* Ledger-derived balance display (issue #120) */}
                <div className="stats-grid">
                    <div className="stat-card">
                        <h3>Available Balance</h3>
                        <div className="value" data-testid="available-balance">
                            {balance ? Number(balance.available).toFixed(4) + " TC" : "0.0000 TC"}
                        </div>
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
            </>
        );
    };

    const renderTransactionsSection = () => {
        if (loading) {
            return (
                <div className="loading-container"><div className="spinner"></div></div>
            );
        }

        if (txnError) {
            return <div className="error-message">Error loading transactions: {txnError}</div>;
        }

        if (!transactions || transactions.length === 0) {
            return <p className="empty-state">No recent transactions.</p>;
        }

        return (
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
        );
    };

    const handleLogout = () => {
        localStorage.removeItem("token");
        window.location.href = "/login";
    };

    return (
        <div className="dashboard-page">
            <aside className="sidebar">
                <div className="logo">CrypMart</div>
                <nav className="nav-links">
                    <Link to="/dashboard" className="nav-link active">Dashboard</Link>
                    <Link to="/marketplace" className="nav-link">Marketplace</Link>
                    <Link to="/history" className="nav-link">Detailed Wallet</Link>
                    <button onClick={handleLogout} className="nav-link logout-btn" style={{ background: "none", border: "none", textAlign: "left", cursor: "pointer" }}>
                        Log Out
                    </button>
                </nav>
            </aside>

            <main className="main-content">
                <header className="header">
                    <h1>Welcome back</h1>
                </header>

                {renderWalletSection()}

                <div className="chart-section">
                    <h3>Price Trend (30 Days)</h3>
                    <div className="chart-container">
                        <svg className="line-graph" viewBox="0 0 1000 200" preserveAspectRatio="none">
                            <path className="line-path" d="M 0 180 L 100 150 L 200 160 L 300 90 L 400 110 L 500 60 L 600 80 L 700 30 L 800 50 L 900 20 L 1000 10" />
                        </svg>
                    </div>
                </div>

                <h2 className="section-header">Recent Transactions</h2>
                {renderTransactionsSection()}

                <h2 className="products-header">Popular in Marketplace</h2>
                <div className="products-grid">
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} className="product-card">
                            <div className="product-image">Image Placeholder</div>
                            <div className="product-title">Item {i}</div>
                            <button className="buy-btn">View Details</button>
                        </div>
                    ))}
                </div>
            </main>
        </div>
    );
}

export default Dashboard;