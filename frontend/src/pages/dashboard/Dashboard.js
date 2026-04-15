import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api.js";
import "./Dashboard.css";

// Helper: makes requests that won't trigger the 401 redirect in api.js
const safeGet = (url) => api.get(url, { skipAuthRedirect: true });

// ── Auth stub ────────────────────────────────────────────────
// Registers a throwaway user and logs in to get a real JWT.
// Returns user info from the register response.
async function ensureAuth() {
    const existing = localStorage.getItem("token");
    const cachedUser = localStorage.getItem("dashUser");
    if (existing && cachedUser) {
        return JSON.parse(cachedUser);
    }

    // Clear any stale token before starting
    localStorage.removeItem("token");

    const ts = Date.now();
    const tempUser = {
        username: "dash_" + ts,
        email: "dash_" + ts + "@wisc.edu",
        password: "Temp1234!",
    };

    // Register
    try {
        await api.post("/auth/register", tempUser, { skipAuthRedirect: true });
    } catch (err) {
        if (err.response?.status !== 409) throw err;
    }

    // Login
    const loginRes = await api.post("/auth/login", {
        username: tempUser.username,
        password: tempUser.password,
    }, { skipAuthRedirect: true });

    let token;
    if (loginRes.data.accessToken) {
        token = loginRes.data.accessToken;
    } else if (loginRes.data.token) {
        token = loginRes.data.token;
    } else if (typeof loginRes.data === "string") {
        token = loginRes.data.replace(/"/g, "");
    } else {
        throw new Error("Unexpected login response");
    }

    localStorage.setItem("token", token);

    const userInfo = {
        username: tempUser.username,
        walletAddress: null, // will be set from register response if available
        userId: null,
    };

    // Try to extract from register response (may have been a 409)
    try {
        const regRes = await api.post("/auth/register", {
            username: tempUser.username,
            email: tempUser.email,
            password: tempUser.password,
        }, { skipAuthRedirect: true });
        userInfo.walletAddress = regRes.data.walletAddress;
        userInfo.userId = regRes.data.id;
    } catch {
        // Already registered, that's fine — we'll get wallet address from /wallet if needed
    }

    localStorage.setItem("dashUser", JSON.stringify(userInfo));
    return userInfo;
}

function Dashboard() {
    const [userInfo, setUserInfo] = useState(null);
    const [balance, setBalance] = useState({ available: 0, staked: 0, total: 0 });
    const [coin, setCoin] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [balanceError, setBalanceError] = useState(null);
    const [hasPending, setHasPending] = useState(false);
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        const init = async () => {
            try {
                const user = await ensureAuth();
                setUserInfo(user);

                // Fetch ledger-derived balance (issue #120)
                if (user.walletAddress) {
                    try {
                        const balanceRes = await safeGet("/balances/" + user.walletAddress);
                        setBalance(balanceRes.data);
                    } catch (err) {
                        if (err.response?.status !== 404) {
                            setBalanceError(err.response?.data?.message || "Failed to load balance.");
                        }
                    }
                }

                // Fetch coin price
                try {
                    const coinRes = await safeGet("/coin");
                    setCoin(coinRes.data);
                } catch {}

                // Fetch transactions
                try {
                    const txnRes = await safeGet("/transactions?page=1&limit=10");
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

            } catch (err) {
                console.error("Dashboard init failed:", err);
            } finally {
                setLoading(false);
            }
        };

        init();
    }, []);

    const handleCopyAddress = () => {
        if (userInfo?.walletAddress) {
            navigator.clipboard.writeText(userInfo.walletAddress).then(() => {
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

        return (
            <>
                <div className="wallet-address-bar">
                    <span className="wallet-address-label">Wallet Address:</span>
                    <code className="wallet-address-value">
                        {userInfo?.walletAddress || "—"}
                    </code>
                    <button className="copy-btn" onClick={handleCopyAddress}>
                        {copied ? "✓ Copied" : "Copy"}
                    </button>
                </div>

                {hasPending && (
                    <div className="pending-banner" role="alert">
                        <span className="pending-icon">⏳</span>
                        You have unconfirmed outgoing transactions. Your available balance may change once they are confirmed.
                    </div>
                )}

                <div className="stats-grid">
                    <div className="stat-card">
                        <h3>Available Balance</h3>
                        <div className="value" data-testid="available-balance">
                            {Number(balance.available).toFixed(4)} TC
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
                            {Number(balance.staked).toFixed(4)} TC
                        </div>
                    </div>
                    <div className="stat-card">
                        <h3>Total Balance</h3>
                        <div className="value" data-testid="total-balance">
                            {Number(balance.total).toFixed(4)} TC
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
        localStorage.removeItem("dashUser");
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
                    <h1>Welcome back{userInfo ? ", " + userInfo.username : ""}</h1>
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