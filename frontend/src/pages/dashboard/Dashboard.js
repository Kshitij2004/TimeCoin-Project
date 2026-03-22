import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api.js"; // Using our new centralized client
import "./Dashboard.css";

// We no longer need SEED_USER_ID or getAuthToken because 
// the backend identifies the user via the JWT in the header.

function Dashboard() {
    const [wallet, setWallet] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loadingWallet, setLoadingWallet] = useState(true);
    const [loadingTxns, setLoadingTxns] = useState(true);
    const [walletError, setWalletError] = useState(null);
    const [txnError, setTxnError] = useState(null);
    const [copied, setCopied] = useState(false);

    // — Fetch data using our centralized API utility ——————————————————————
    useEffect(() => {
        const fetchDashboardData = async () => {
            // Fetch Wallet
            try {
                setLoadingWallet(true);
                // The JWT is automatically attached by our api.js interceptor!
                const walletRes = await api.get("/wallet");
                setWallet(walletRes.data);
            } catch (err) {
                setWalletError(err.response?.data?.message || "Failed to load wallet.");
                // If this error is a 401, api.js automatically redirects to /login
            } finally {
                setLoadingWallet(false);
            }

            // Fetch Transactions
            try {
                setLoadingTxns(true);
                const txnRes = await api.get("/wallet/transactions?page=1&limit=10");
                
                // Handle different response structures
                if (txnRes.data && txnRes.data.data) {
                    setTransactions(txnRes.data.data);
                } else if (Array.isArray(txnRes.data)) {
                    setTransactions(txnRes.data);
                }
            } catch (err) {
                setTxnError(err.response?.data?.message || "Failed to load transactions.");
            } finally {
                setLoadingTxns(false);
            }
        };

        fetchDashboardData();
    }, []);

    const handleCopyAddress = () => {
        const address = wallet ? wallet.walletAddress : null;
        if (address) {
            navigator.clipboard.writeText(address).then(() => {
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
        if (loadingWallet) {
            return (
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Loading wallet...</p>
                </div>
            );
        }

        if (walletError) {
            return (
                <div className="error-container">
                    <p className="error-message">Error: {walletError}</p>
                    <button className="retry-btn" onClick={() => window.location.reload()}>
                        Retry
                    </button>
                </div>
            );
        }

        if (!wallet) return null;

        return (
            <>
                <div className="wallet-address-bar">
                    <span className="wallet-address-label">Wallet Address:</span>
                    <code className="wallet-address-value">{wallet.walletAddress || "—"}</code>
                    <button className="copy-btn" onClick={handleCopyAddress}>
                        {copied ? "✓ Copied" : "Copy"}
                    </button>
                </div>

                <div className="stats-grid">
                    <div className="stat-card">
                        <h3>Wallet Balance</h3>
                        <div className="value">{Number(wallet.coinBalance || 0).toFixed(2)} CRYP</div>
                    </div>
                    <div className="stat-card">
                        <h3>Current Coin Price</h3>
                        <div className="value">$161.75</div>
                        <div className="sub-value">+5.2% in the last 24h</div>
                    </div>
                </div>
            </>
        );
    };

    const renderTransactionsSection = () => {
        if (loadingTxns) {
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
                                    {tx.amount != null ? Number(tx.amount).toFixed(2) + " CRYP" : "—"}
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
        localStorage.removeItem('token');
        window.location.href = '/login';
    };

    return (
        <div className="dashboard-page">
            <aside className="sidebar">
                <div className="logo">CrypMart</div>
                <nav className="nav-links">
                    <Link to="/dashboard" className="nav-link active">Dashboard</Link>
                    <Link to="/marketplace" className="nav-link">Marketplace</Link>
                    <Link to="/history" className="nav-link">Detailed Wallet</Link>
                    <button onClick={handleLogout} className="nav-link logout-btn" style={{background: 'none', border: 'none', textAlign: 'left', cursor: 'pointer'}}>
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
                    {/* Simplified product cards for brevity */}
                    {[1, 2, 3, 4].map(i => (
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