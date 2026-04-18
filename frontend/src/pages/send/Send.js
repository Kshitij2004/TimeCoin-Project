import { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import api from "../../services/api.js";
import "./Send.css";

function Send() {
    const [balance, setBalance] = useState(null);
    const [walletAddress, setWalletAddress] = useState(null);
    const [recipientAddress, setRecipientAddress] = useState("");
    const [amount, setAmount] = useState("");
    const [memo, setMemo] = useState("");
    const [loading, setLoading] = useState(false);
    const [walletLoading, setWalletLoading] = useState(true);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [showConfirm, setShowConfirm] = useState(false);

    // Fetch wallet address and ledger-derived balance on mount
    useEffect(() => {
        const fetchWalletData = async () => {
            setWalletLoading(true);
            try {
                // Get wallet address
                const walletRes = await api.get("/wallet");
                setWalletAddress(walletRes.data.walletAddress);

                // Get ledger-derived balance (issue #120)
                const balanceRes = await api.get("/wallet/balance");
                setBalance(balanceRes.data);
            } catch (err) {
                setError(err.response?.data?.message || "Failed to load wallet data.");
            } finally {
                setWalletLoading(false);
            }
        };

        fetchWalletData();
    }, []);

    const networkFee = 0.01;
    const parsedAmount = parseFloat(amount) || 0;
    const availableBalance = balance ? parseFloat(balance.available) : 0;
    const estimatedBalanceAfter = (availableBalance - parsedAmount - networkFee).toFixed(4);

    // Client-side validation
    const validate = () => {
        if (!recipientAddress.trim()) {
            return "Recipient wallet address is required.";
        }
        if (recipientAddress.trim().length < 3) {
            return "Invalid wallet address format.";
        }
        if (walletAddress && recipientAddress.trim() === walletAddress) {
            return "Cannot send to your own wallet address.";
        }

        if (!amount || isNaN(parsedAmount)) {
            return "Please enter a valid amount.";
        }
        if (parsedAmount <= 0) {
            return "Amount must be greater than 0.";
        }
        if (parsedAmount + networkFee > availableBalance) {
            return "Insufficient balance. You have " + availableBalance.toFixed(4) + " TC (fee: " + networkFee + " TC).";
        }
        return null;
    };

    // Show confirmation modal
    const handleFormSubmit = (e) => {
        e.preventDefault();
        setError(null);
        setSuccess(null);

        const validationError = validate();
        if (validationError) {
            setError(validationError);
            return;
        }

        setShowConfirm(true);
    };

    // Actually submit the transfer
    const handleConfirmSend = async () => {
        setShowConfirm(false);
        setLoading(true);
        setError(null);

        const requestBody = {
            senderAddress: walletAddress,
            receiverAddress: recipientAddress.trim(),
            amount: parsedAmount,
            fee: networkFee,
            nonce: 0,
            memo: memo.trim(),
        };

        try {
            const response = await api.post("/transactions/transfer", requestBody);
            const tx = response.data;

            setSuccess({
                hash: tx.transactionHash,
                amount: tx.amount,
                receiver: tx.receiverAddress,
                status: tx.status,
            });

            setRecipientAddress("");
            setAmount("");
            setMemo("");

            // Refresh balance after transfer
            try {
                const balanceRes = await api.get("/wallet/balance");
                setBalance(balanceRes.data);
            } catch {}

        } catch (err) {
            setError(err.response?.data?.message || "Transfer failed. Please try again.");
        } finally {
            setLoading(false);
        }
    };

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
                        style={{ background: 'none', border: 'none', textAlign: 'left', cursor: 'pointer' }}
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
                            {walletLoading ? "Loading..." : availableBalance.toFixed(4) + " TC"}
                        </span>
                    </div>

                    {success && (
                        <div className="success-container">
                            <h3>Transfer Submitted!</h3>
                            <p>
                                <strong>{success.amount} TC</strong> sent to{" "}
                                <code>{success.receiver}</code>
                            </p>
                            <p>Status: <span className="status-tag">{success.status}</span></p>
                            <div className="hash-display">
                                <span className="hash-label">Transaction Hash:</span>
                                <code className="hash-value">{success.hash}</code>
                            </div>
                            <Link to={"/blockchain"} className="view-status-link">
                                View in Block Explorer →
                            </Link>
                        </div>
                    )}

                    {error && (
                        <div className="error-container">
                            <p className="error-message">{error}</p>
                        </div>
                    )}

                    {!success && (
                        <form className="send-form-wrapper" onSubmit={handleFormSubmit}>
                            <div className="form-group">
                                <label htmlFor="recipient">Recipient Wallet Address</label>
                                <input
                                    id="recipient"
                                    type="text"
                                    placeholder="e.g. wlt_abc123..."
                                    value={recipientAddress}
                                    onChange={(e) => setRecipientAddress(e.target.value)}
                                    disabled={loading}
                                />
                            </div>

                            <div className="form-group">
                                <label htmlFor="amount">Amount (TC)</label>
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
                                <span>{networkFee} TC</span>
                            </div>

                            {/* Estimated balance after transfer (issue #115) */}
                            {parsedAmount > 0 && (
                                <div className="fee-info estimated-balance">
                                    <span>Balance after transfer:</span>
                                    <span className={parseFloat(estimatedBalanceAfter) < 0 ? "negative" : ""}>
                                        {estimatedBalanceAfter} TC
                                    </span>
                                </div>
                            )}

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

                {/* Confirmation modal (issue #115) */}
                {showConfirm && (
                    <div className="modal-overlay" onClick={() => setShowConfirm(false)}>
                        <div className="modal-card" onClick={(e) => e.stopPropagation()}>
                            <h3>Confirm Transfer</h3>
                            <div className="modal-details">
                                <div className="modal-row">
                                    <span className="modal-label">To:</span>
                                    <code className="modal-value">{recipientAddress}</code>
                                </div>
                                <div className="modal-row">
                                    <span className="modal-label">Amount:</span>
                                    <span className="modal-value">{parsedAmount} TC</span>
                                </div>
                                <div className="modal-row">
                                    <span className="modal-label">Fee:</span>
                                    <span className="modal-value">{networkFee} TC</span>
                                </div>
                                <div className="modal-row">
                                    <span className="modal-label">Total:</span>
                                    <span className="modal-value modal-total">{(parsedAmount + networkFee).toFixed(4)} TC</span>
                                </div>
                                {memo && (
                                    <div className="modal-row">
                                        <span className="modal-label">Memo:</span>
                                        <span className="modal-value">{memo}</span>
                                    </div>
                                )}
                                <div className="modal-row">
                                    <span className="modal-label">Balance after:</span>
                                    <span className="modal-value">{estimatedBalanceAfter} TC</span>
                                </div>
                            </div>
                            <div className="modal-actions">
                                <button className="modal-cancel" onClick={() => setShowConfirm(false)}>
                                    Cancel
                                </button>
                                <button className="modal-confirm" onClick={handleConfirmSend}>
                                    Confirm & Send
                                </button>
                            </div>
                        </div>
                    </div>
                )}
            </main>
        </div>
    );
}

export default Send;