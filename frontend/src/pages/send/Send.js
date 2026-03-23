import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api.js"; // Using our centralized client
import "./Send.css";

function Send() {
    const [wallet, setWallet] = useState(null);
    const [recipientAddress, setRecipientAddress] = useState("");
    const [amount, setAmount] = useState("");
    const [memo, setMemo] = useState("");
    const [loading, setLoading] = useState(false);
    const [walletLoading, setWalletLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);

    // — Fetch wallet for balance display on mount ————————————
    useEffect(() => {
        const fetchWallet = async () => {
            setWalletLoading(true);
            try {
                // Token is automatically attached by api.js
                const response = await api.get("/wallet");
                setWallet(response.data);
            } catch (err) {
                setError(err.response?.data?.message || "Failed to load wallet data.");
            } finally {
                setWalletLoading(false);
            }
        };

        fetchWallet();
    }, []);

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
    const handleSubmit = async (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        const validationError = validate();
        if (validationError) {
            setError(validationError);
            return;
        }

        if (!wallet) {
            setError("Wallet data not available.");
            return;
        }

        setLoading(true);

        const requestBody = {
            senderAddress: wallet.walletAddress,
            receiverAddress: recipientAddress.trim(),
            amount: parseFloat(amount),
            fee: 0.01,
            nonce: 0,
            memo: memo.trim() // Included the memo in the request
        };

        try {
            // Using the centralized api client for the POST request
            const response = await api.post("/transactions/transfer", requestBody);
            const tx = response.data;

            setSuccess({
                hash: tx.transactionHash,
                amount: tx.amount,
                receiver: tx.receiverAddress,
                status: tx.status,
            });

            // Reset form
            setRecipientAddress("");
            setAmount("");
            setMemo("");

            // Refresh wallet balance after successful transfer
            const walletUpdate = await api.get("/wallet");
            setWallet(walletUpdate.data);

        } catch (err) {
            setError(err.response?.data?.message || "Transfer failed. Please try again.");
        } finally {
            setLoading(false);
        }
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
                    <button 
                        onClick={() => { localStorage.removeItem('token'); window.location.href = '/login'; }} 
                        className="nav-link logout-btn"
                        style={{background: 'none', border: 'none', textAlign: 'left', cursor: 'pointer'}}
                    >
                        Log Out
                    </button>
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
                        <form className="send-form-wrapper" onSubmit={handleSubmit}>
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
                                type="submit"
                                className="send-btn"
                                disabled={loading || walletLoading}
                            >
                                {loading ? "Sending..." : "Send TimeCoin"}
                            </button>
                        </form>
                    )}
                </div>
            </main>
        </div>
    );
}

export default Send;