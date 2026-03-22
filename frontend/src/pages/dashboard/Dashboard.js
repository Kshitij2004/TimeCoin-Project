import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { API_BASE_URL } from "../../services/api.js";
import "./Dashboard.css";

// — Auth stub ————————————————————————————————————————
// TODO: replace with real auth context when built
// import { useAuth } from "../../context/AuthContext";
const SEED_USER_ID = 1; // matches seed.sql user_id with wallet data

const API_BASE = API_BASE_URL;

/**
 * Registers a throwaway user then logs in to obtain a real JWT.
 */
async function getAuthToken() {
    const ts = Date.now();
    const tempUser = {
        username: "dashtest_" + ts,
        email: "dashtest_" + ts + "@wisc.edu",
        password: "Temp1234!",
    };

    // Register — ignore 409 if user somehow already exists
    await fetch(`${API_BASE}/api/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(tempUser),
    });

    // Login to get JWT
    const loginRes = await fetch(`${API_BASE}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            username: tempUser.username,
            password: tempUser.password,
        }),
    });

    if (!loginRes.ok) {
        throw new Error("Login failed (" + loginRes.status + ")");
    }

    const token = await loginRes.text();
    return token.replace(/"/g, ""); // strip quotes if wrapped
}

function Dashboard() {
    const [token, setToken] = useState(null);
    const [wallet, setWallet] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loadingWallet, setLoadingWallet] = useState(true);
    const [loadingTxns, setLoadingTxns] = useState(true);
    const [walletError, setWalletError] = useState(null);
    const [txnError, setTxnError] = useState(null);
    const [copied, setCopied] = useState(false);

    // — Get a real JWT on mount ——————————————————————
    useEffect(() => {
        getAuthToken()
            .then((t) => setToken(t))
            .catch((err) => {
                setWalletError("Auth failed: " + err.message);
                setLoadingWallet(false);
                setTxnError("Auth failed: " + err.message);
                setLoadingTxns(false);
            });
    }, []);

    // — Fetch wallet once token is ready —————————————
    useEffect(() => {
        if (!token) return;
        setLoadingWallet(true);
        fetch(`${API_BASE}/api/wallet`, {
            headers: {
                Authorization: "Bearer " + token,
                "x-user-id": String(SEED_USER_ID),
            },
        })
            .then((r) => {
                if (!r.ok) throw new Error("Failed to fetch wallet (" + r.status + ")");
                return r.json();
            })
            .then((data) => setWallet(data))
            .catch((err) => setWalletError(err.message))
            .finally(() => setLoadingWallet(false));
    }, [token]);

    // — Fetch transactions once token is ready ———————
    useEffect(() => {
        if (!token) return;
        setLoadingTxns(true);
        fetch(`${API_BASE}/api/wallet/transactions?page=1&limit=10`, {
            headers: {
                Authorization: "Bearer " + token,
                "x-user-id": String(SEED_USER_ID),
            },
        })
            .then((r) => {
                if (!r.ok) throw new Error("Failed to fetch transactions (" + r.status + ")");
                return r.json();
            })
            .then((data) => {
                if (data && data.data) {
                    setTransactions(data.data);
                } else if (Array.isArray(data)) {
                    setTransactions(data);
                }
            })
            .catch((err) => setTxnError(err.message))
            .finally(() => setLoadingTxns(false));
    }, [token]);

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

    // — Wallet section ———————————————————————————————
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

        const coinBalance = wallet.coinBalance || 0;
        const walletAddress = wallet.walletAddress || "—";

        return (
            <>
                <div className="wallet-address-bar">
                    <span className="wallet-address-label">Wallet Address:</span>
                    <code className="wallet-address-value">{walletAddress}</code>
                    <button
                        className="copy-btn"
                        onClick={handleCopyAddress}
                        title="Copy to clipboard"
                    >
                        {copied ? "✓ Copied" : "Copy"}
                    </button>
                </div>

                <div className="stats-grid">
                    <div className="stat-card">
                        <h3>Wallet Balance</h3>
                        <div className="value">{Number(coinBalance).toFixed(2)} CRYP</div>
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

    // — Transactions section —————————————————————————
    const renderTransactionsSection = () => {
        if (loadingTxns) {
            return (
                <div className="loading-container">
                    <div className="spinner"></div>
                    <p>Loading transactions...</p>
                </div>
            );
        }

        if (txnError) {
            return (
                <div className="error-container">
                    <p className="error-message">Error: {txnError}</p>
                    <button className="retry-btn" onClick={() => window.location.reload()}>
                        Retry
                    </button>
                </div>
            );
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
                                    {tx.amount != null
                                        ? Number(tx.amount).toFixed(2) + " CRYP"
                                        : "—"}
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

    return (
        <div className="dashboard-page">
            <aside className="sidebar">
                <div className="logo">CrypMart</div>
                <nav className="nav-links">
                    <Link to="/dashboard" className="nav-link active">Dashboard</Link>
                    <Link to="/marketplace" className="nav-link">Marketplace</Link>
                    <Link to="/history" className="nav-link">Detailed Wallet</Link>
                    <Link to="/login" className="nav-link logout-btn">Log Out</Link>
                </nav>
            </aside>

            <main className="main-content">
                <header className="header">
                    <h1>Welcome back, User</h1>
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
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Vintage Leather Jacket</div>
                        <div className="product-price">0.25 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Custom Gaming PC</div>
                        <div className="product-price">1.30 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Rare First Edition Book</div>
                        <div className="product-price">0.38 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Handmade Necklace</div>
                        <div className="product-price">0.15 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                </div>
            </main>
        </div>
    );
}

export default Dashboard;