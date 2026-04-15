import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api.js";
import "./Dashboard.css";

// ── Auth stub ────────────────────────────────────────────────
// TODO: Remove once login flow is wired up. Dashboard will then
// be wrapped in ProtectedRoute like all other pages.
async function ensureAuth() {
    const existing = localStorage.getItem("token");
    if (existing) return existing;

    const ts = Date.now();
    const tempUser = {
        username: "dash_" + ts,
        email: "dash_" + ts + "@wisc.edu",
        password: "Temp1234!",
    };

    try {
        await api.post("/auth/register", tempUser);
    } catch (err) {
        if (err.response?.status !== 409) throw err;
    }

    const loginRes = await api.post("/auth/login", {
        username: tempUser.username,
        password: tempUser.password,
    });

    let token;
    if (loginRes.data.token) {
        token = loginRes.data.token;
    } else if (typeof loginRes.data === "string") {
        token = loginRes.data.replace(/"/g, "");
    } else {
        throw new Error("Unexpected login response");
    }

    localStorage.setItem("token", token);
    return token;
}

function Dashboard() {
    const [wallet, setWallet] = useState(null);
    const [balance, setBalance] = useState(null);
    const [coin, setCoin] = useState(null);
    const [transactions, setTransactions] = useState([]);
    const [loadingWallet, setLoadingWallet] = useState(true);
    const [loadingBalance, setLoadingBalance] = useState(true);
    const [loadingTxns, setLoadingTxns] = useState(true);
    const [walletError, setWalletError] = useState(null);
    const [balanceError, setBalanceError] = useState(null);
    const [txnError, setTxnError] = useState(null);
    const [copied, setCopied] = useState(false);
    const [hasPending, setHasPending] = useState(false);
    const [authReady, setAuthReady] = useState(false);

    // Step 0: get a real JWT before any API calls
    useEffect(() => {
        ensureAuth()
            .then(() => setAuthReady(true))
            .catch((err) => {
                console.error("Auth setup failed:", err);
                setWalletError("Authentication failed. Please refresh.");
                setLoadingWallet(false);
                setLoadingBalance(false);
                setLoadingTxns(false);
            });
    }, []);

    // Step 1–3: fetch data once auth is ready
    useEffect(() => {
        if (!authReady) return;

        const fetchDashboardData = async () => {
            let walletData = null;
            try {
                setLoadingWallet(true);
                const userId = 1;
                const walletRes = await api.get("/wallet?userId=" + userId);
                walletData = walletRes.data;
                setWallet(walletData);
            } catch (err) {
                setWalletError(err.response?.data?.message || "Failed to load wallet.");
            } finally {
                setLoadingWallet(false);
            }

            // Fetch ledger-derived balance (issue #120)
            if (walletData && walletData.walletAddress) {
                try {
                    setLoadingBalance(true);
                    const balanceRes = await api.get("/balances/" + walletData.walletAddress);
                    setBalance(balanceRes.data);
                } catch (err) {
                    setBalanceError(err.response?.data?.message || "Failed to load balance.");
                } finally {
                    setLoadingBalance(false);
                }
            } else {
                setBalance({ available: 0, staked: 0, total: 0 });
                setLoadingBalance(false);
            }

            try {
                const coinRes = await api.get("/coin");
                setCoin(coinRes.data);
            } catch {}

            try {
                setLoadingTxns(true);
                const txnRes = await api.get("/transactions?page=1&limit=10");
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
            } finally {
                setLoadingTxns(false);
            }
        };

        fetchDashboardData();
    }, [authReady]);

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
        if (!authReady || loadingWallet || loadingBalance) {
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