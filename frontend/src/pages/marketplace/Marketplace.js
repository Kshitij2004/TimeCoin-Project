import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api.js"; // Using our new centralized client
import "./Marketplace.css";

export default function Marketplace() {
  const navigate = useNavigate();

  const [coin, setCoin] = useState(null);
  const [amount, setAmount] = useState("");
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [coinLoading, setCoinLoading] = useState(true);

  // ── Fetch coin data on mount ──────────────────────────────────────────────
  useEffect(() => {
    const fetchCoinData = async () => {
      setCoinLoading(true);
      try {
        // Interceptor automatically attaches the JWT from localStorage
        const res = await api.get("/coin");
        setCoin(res.data);
      } catch (err) {
        // If 401, interceptor redirects to /login. 
        // Otherwise, we show a silent fail or error message.
        setCoin(null);
      } finally {
        setCoinLoading(false);
      }
    };

    fetchCoinData();
  }, []);

  // ── Buy handler ───────────────────────────────────────────────────────────
  async function handleBuy(e) {
    e.preventDefault();
    setStatus(null);

    const parsedAmount = parseFloat(amount);
    if (!parsedAmount || parsedAmount <= 0) {
      setStatus({ type: "error", message: "Please enter a valid amount greater than 0." });
      return;
    }

    setLoading(true);
    try {
      // Acceptance Criteria: Uses shared utility for POST request
      const res = await api.post("/coin/buy", {
        symbol: "TC", // TimeCoin
        amount: parsedAmount,
      });

      setStatus({
        type: "success",
        message: `Successfully purchased ${parsedAmount} coins for $${(
          parsedAmount * (coin?.currentPrice || 0)
        ).toFixed(2)}!`,
      });
      setAmount("");

      // Refresh coin supply after purchase using centralized client
      const updated = await api.get("/coin");
      setCoin(updated.data);
    } catch (err) {
      // Capture error message from backend
      setStatus({ 
        type: "error", 
        message: err.response?.data?.message || err.message 
      });
    } finally {
      setLoading(false);
    }
  }

  // ── Derived ───────────────────────────────────────────────────────────────
  const totalCost =
    amount && coin ? (parseFloat(amount) * coin.currentPrice).toFixed(2) : null;

  return (
    <div className="marketplace-page">
      <div className="marketplace-card">
        <header className="marketplace-header">
          <h1>Marketplace</h1>
          <p>Buy coins at the current market rate</p>
        </header>

        {/* ── Market stats ── */}
        <section className="market-stats" aria-label="Market statistics">
          <div className="stat-card">
            <span className="stat-label">Current Price</span>
            <span className="stat-value" data-testid="coin-price">
              {coinLoading ? "—" : coin ? `$${Number(coin.currentPrice).toFixed(2)}` : "—"}
            </span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Circulating Supply</span>
            <span className="stat-value" data-testid="circulating-supply">
              {coinLoading ? "—" : coin ? Number(coin.circulatingSupply).toLocaleString() : "—"}
            </span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Total Supply</span>
            <span className="stat-value" data-testid="total-supply">
              {coinLoading ? "—" : coin ? Number(coin.totalSupply).toLocaleString() : "—"}
            </span>
          </div>
        </section>

        {/* ── Buy form ── */}
        <section className="buy-section">
          <form className="buy-form" onSubmit={handleBuy} noValidate>
            <label htmlFor="amount" className="form-label">
              Amount to buy
            </label>
            <div className="input-row">
              <input
                id="amount"
                data-testid="amount-input"
                type="number"
                min="0.00000001"
                step="any"
                placeholder="0.00"
                value={amount}
                onChange={(e) => {
                  setAmount(e.target.value);
                  setStatus(null);
                }}
                onWheel={(e) => e.target.blur()}
                disabled={loading}
                className="amount-input"
              />
              <button
                type="submit"
                data-testid="buy-button"
                className="buy-button"
                disabled={loading || !amount}
              >
                {loading ? "Processing…" : "Buy"}
              </button>
            </div>

            {totalCost && !status && (
              <p className="cost-preview" data-testid="cost-preview">
                Estimated total: <strong>${totalCost}</strong>
              </p>
            )}
          </form>

          {/* ── Status message ── */}
          {status && (
            <div
              className={`status-message status-${status.type}`}
              role="alert"
              data-testid="status-message"
            >
              {status.message}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}