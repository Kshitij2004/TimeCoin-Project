import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { API_BASE_URL } from "../../services/api";
import "./Send.css";

// — Auth stub ————————————————————————————————————————
// TODO: replace with real auth context when built
const SEED_USER_ID = 1;
const API_BASE = API_BASE_URL;

async function getAuthToken() {
    const ts = Date.now();
    const tempUser = {
        username: "sendtest_" + ts,
        email: "sendtest_" + ts + "@wisc.edu",
        password: "Temp1234!",
    };

    await fetch(`${API_BASE}/api/auth/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(tempUser),
    });

    const loginRes = await fetch(`${API_BASE}/api/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            username: tempUser.username,
            password: tempUser.password,
        }),
    });

    if (!loginRes.ok) throw new Error("Login failed (" + loginRes.status + ")");
    const token = await loginRes.text();
    return token.replace(/"/g, "");
}

function Send() {
    const [token, setToken] = useState(null);
    const [wallet, setWallet] = useState(null);
    const [recipientAddress, setRecipientAddress] = useState("");
    const [amount, setAmount] = useState("");
    const [memo, setMemo] = useState("");
    const [loading, setLoading] = useState(false);
    const [walletLoading, setWalletLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    // — Get JWT on mount ————————————————————————————
    useEffect(() => {
        getAuthToken()
            .then((t) => setToken(t))
            .catch((err) => {
                setError("Auth failed: " + err.message);
                setWalletLoading(false);
            });
    }, []);

    // — Fetch wallet for balance display ————————————
    useEffect(() => {
        if (!token) return;
        setWalletLoading(true);
        fetch(`${API_BASE}/api/wallet`, {
            headers: {
                Authorization: "Bearer " + token,
                "x-user-id": String(SEED_USER_ID),
            },
        })
            .then((r) => {
                if (!r.ok) throw new Error("Failed to load wallet (" + r.status + ")");
                return r.json();
            })
            .then((data) => setWallet(data))
            .catch((err) => setError(err.message))
            .finally(() => setWalletLoading(false));
    }, [token]);

    // — Client-side validation ——————————————————————
    const validate = () => {
        if (!recipientAddress.trim()) {
            return "Recipient wallet address is required.";
        }
        if (recipientAddress.trim().length < 3) {
            return "Invalid wallet address format.";
        }
        if (wallet && recipientAddress.trim() === wallet.walletAddress) {
            return "Cannot send to your own wallet address.";
        }

        const numAmount = parseFloat(amount);
        if (!amount || isNaN(numAmount)) {
            return "Please enter a valid amount.";
        }
        if (numAmount <= 0) {
            return "Amount must be greater than 0.";
        }
        if (wallet && numAmount > parseFloat(wallet.coinBalance)) {
            return "Insufficient balance. You have " + parseFloat(wallet.coinBalance).toFixed(2) + " CRYP.";
        }
        return null;
    };

    // — Submit transfer —————————————————————————————
    const handleSubmit = (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        const validationError = validate();
        if (validationError) {
            setError(validationError);
            return;
        }

        if (!token || !wallet) {
            setError("Wallet not loaded yet. Please wait.");
            return;
        }

        setLoading(true);

        const requestBody = {
            senderAddress: wallet.walletAddress,
            receiverAddress: recipientAddress.trim(),
            amount: parseFloat(amount),
            fee: 0.01,
            nonce: 0,
        };

        fetch(`${API_BASE}/api/transactions/transfer`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: "Bearer " + token,
                "x-user-id": String(SEED_USER_ID),
            },
            body: JSON.stringify(requestBody),
        })
            .then((r) => {
                if (!r.ok) {
                    return r.json().then((body) => {
                        throw new Error(body.message || "Transfer failed (" + r.status + ")");
                    });
                }
                return r.json();
            })
            .then((tx) => {
                setSuccess({
                    hash: tx.transactionHash,
                    amount: tx.amount,
                    receiver: tx.receiverAddress,
                    status: tx.status,
                });
                setRecipientAddress("");
                setAmount("");
                setMemo("");
                // Refresh wallet balance
                return fetch(`${API_BASE}/api/wallet`, {
                    headers: {
                        Authorization: "Bearer " + token,
                        "x-user-id": String(SEED_USER_ID),
                    },
                });
            })
            .then((r) => r && r.ok ? r.json() : null)
            .then((data) => { if (data) setWallet(data); })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false));
    };

    const balance = wallet ? parseFloat(wallet.coinBalance).toFixed(2) : "—";

    return (
        <div className="send-page">
            <aside className="sidebar">
                <div className="logo">CrypMart</div>
                <nav className="nav-links">
                    <Link to="/dashboard" className="nav-link">Dashboard</Link>
                    <Link to="/marketplace" className="nav-link">Marketplace</Link>
                    <Link to="/send" className="nav-link active">Send</Link>
                    <Link to="/history" className="nav-link">Detailed Wallet</Link>
                    <Link to="/login" className="nav-link logout-btn">Log Out</Link>
                </nav>
            </aside>

            <main className="main-content">
                <header className="header">
                    <h1>Send TimeCoin</h1>
                </header>

                <div className="send-card">
                    <div className="balance-display">
                        <span className="balance-label">Available Balance</span>
                        <span className="balance-value">
                            {walletLoading ? "Loading..." : balance + " CRYP"}
                        </span>
                    </div>

                    {success && (
                        <div className="success-container">
                            <h3>Transfer Submitted!</h3>
                            <p>
                                <strong>{success.amount} CRYP</strong> sent to{" "}
                                <code>{success.receiver}</code>
                            </p>
                            <p>Status: <span className="status-tag">{success.status}</span></p>
                            <div className="hash-display">
                                <span className="hash-label">Transaction Hash:</span>
                                <code className="hash-value">{success.hash}</code>
                            </div>
                            <Link to={"/history"} className="view-status-link">
                                View Transaction Status →
                            </Link>
                        </div>
                    )}

                    {error && (
                        <div className="error-container">
                            <p className="error-message">{error}</p>
                        </div>
                    )}

                    {!success && (
                        <div className="send-form-wrapper">
                            <div className="form-group">
                                <label htmlFor="recipient">Recipient Wallet Address</label>
                                <input
                                    id="recipient"
                                    type="text"
                                    placeholder="e.g. addr_2"
                                    value={recipientAddress}
                                    onChange={(e) => setRecipientAddress(e.target.value)}
                                    disabled={loading}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="amount">Amount (CRYP)</label>
                                <input
                                    id="amount"
                                    type="number"
                                    step="0.01"
                                    min="0"
                                    placeholder="0.00"
                                    value={amount}
                                    onChange={(e) => setAmount(e.target.value)}
                                    disabled={loading}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="memo">Memo (optional)</label>
                                <input
                                    id="memo"
                                    type="text"
                                    placeholder="What's this for?"
                                    value={memo}
                                    onChange={(e) => setMemo(e.target.value)}
                                    disabled={loading}
                                />
                            </div>

                            <div className="fee-info">
                                <span>Network Fee:</span>
                                <span>0.01 CRYP</span>
                            </div>

                            <button
                                className="send-btn"
                                onClick={handleSubmit}
                                disabled={loading || walletLoading}
                            >
                                {loading ? "Sending..." : "Send TimeCoin"}
                            </button>
                        </div>
                    )}
                </div>
            </main>
        </div>
    );
}

export default Send;