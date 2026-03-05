import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "./Marketplace.css";

// ── Auth stub ─────────────────────────────────────────────────────────────────
// TODO: replace with real auth context when built
// import { useAuth } from "../../context/AuthContext";
const user = { id: 1, username: "testuser1" };
const token = "fake-token";

const API_BASE = process.env.REACT_APP_API_URL ?? "http://localhost:3001";

export default function Marketplace() {
  const navigate = useNavigate();

  const [coin, setCoin] = useState(null);       // { current_price, circulating_supply, total_supply }
  const [amount, setAmount] = useState("");
  const [status, setStatus] = useState(null);   // { type: "success"|"error", message }
  const [loading, setLoading] = useState(false);
  const [coinLoading, setCoinLoading] = useState(true);

  // ── Auth gate ─────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!user) navigate("/login", { replace: true });
  }, [navigate]);

  // ── Fetch coin data on mount ──────────────────────────────────────────────
  useEffect(() => {
    if (!user) return;
    setCoinLoading(true);
    fetch(`${API_BASE}/api/coins`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => {
        if (!r.ok) throw new Error("Could not reach the server — coin data unavailable.");
        return r.json();
      })
      .then((data) => setCoin(data))
      .catch(() => {
        // Backend not running yet — silently fail so the form stays usable
        setCoin(null);
      })
      .finally(() => setCoinLoading(false));
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
      const res = await fetch(`${API_BASE}/api/transactions/buy`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ amount: parsedAmount }),
      });

      // Guard against non-JSON response (e.g. backend not running)
      const contentType = res.headers.get("content-type") ?? "";
      if (!contentType.includes("application/json")) {
        throw new Error("Backend server is not running. Start the server to enable purchases.");
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.message ?? "Purchase failed");

      setStatus({
        type: "success",
        message: `Successfully purchased ${parsedAmount} coins for $${(
          parsedAmount * coin.current_price
        ).toFixed(2)}!`,
      });
      setAmount("");

      // Refresh coin supply after purchase
      const updated = await fetch(`${API_BASE}/api/coins`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (updated.ok) setCoin(await updated.json());
    } catch (err) {
      setStatus({ type: "error", message: err.message });
    } finally {
      setLoading(false);
    }
  }

  // ── Derived ───────────────────────────────────────────────────────────────
  const totalCost =
    amount && coin ? (parseFloat(amount) * coin.current_price).toFixed(2) : null;

  if (!user) return null;

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
              {coinLoading ? "—" : coin ? `$${Number(coin.current_price).toFixed(2)}` : "—"}
            </span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Circulating Supply</span>
            <span className="stat-value" data-testid="circulating-supply">
              {coinLoading ? "—" : coin ? Number(coin.circulating_supply).toLocaleString() : "—"}
            </span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Total Supply</span>
            <span className="stat-value" data-testid="total-supply">
              {coinLoading ? "—" : coin ? Number(coin.total_supply).toLocaleString() : "—"}
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