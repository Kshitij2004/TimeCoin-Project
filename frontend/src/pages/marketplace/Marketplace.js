import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api.js";
import "./Marketplace.css";

export default function Marketplace() {
  // coin and balance state
  const [coin, setCoin] = useState(null);
  const [balance, setBalance] = useState(null);
  const [amount, setAmount] = useState("");
  const [sellAmount, setSellAmount] = useState("");
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [sellLoading, setSellLoading] = useState(false);
  const [coinLoading, setCoinLoading] = useState(true);

  // listing state
  const [listings, setListings] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("All");
  const [listingsLoading, setListingsLoading] = useState(true);

  const navigate = useNavigate();

  // fetch coin data, listings, and user balance on mount
  useEffect(() => {
    const fetchData = async () => {
      setCoinLoading(true);
      setListingsLoading(true);
      try {
        const [coinRes, listingsRes] = await Promise.all([
          api.get("/coin"),
          api.get("/listings"),
        ]);

        setCoin(coinRes.data);
        setListings(listingsRes.data);

        try {
          const balanceRes = await api.get("/wallet/balance");
          setBalance(balanceRes.data);
        } catch {
          setBalance(null);
        }
      } catch (err) {
        console.error("Data fetch failed", err);
      } finally {
        setCoinLoading(false);
        setListingsLoading(false);
      }
    };

    fetchData();
  }, []);

  // refreshes coin price and user balance after a trade
  async function refreshCoinAndBalance() {
    try {
      const updated = await api.get("/coin");
      setCoin(updated.data);
    } catch {}
    try {
      const balanceRes = await api.get("/wallet/balance");
      setBalance(balanceRes.data);
    } catch {}
  }

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
      await api.post("/coin/buy", { symbol: "TC", amount: parsedAmount });
      setStatus({ type: "success", message: `Successfully purchased ${parsedAmount} coins!` });
      setAmount("");
      await refreshCoinAndBalance();
    } catch (err) {
      setStatus({ type: "error", message: err.response?.data?.message || err.message });
    } finally {
      setLoading(false);
    }
  }

  async function handleSell(e) {
    e.preventDefault();
    setStatus(null);
    const parsedAmount = parseFloat(sellAmount);
    if (!parsedAmount || parsedAmount <= 0) {
      setStatus({ type: "error", message: "Please enter a valid amount greater than 0." });
      return;
    }

    if (balance && parsedAmount > balance.available) {
      setStatus({ type: "error", message: `Insufficient balance. You have ${balance.available} TC.` });
      return;
    }

    setSellLoading(true);
    try {
      await api.post("/coin/sell", { symbol: "TC", amount: parsedAmount });
      setStatus({ type: "success", message: `Successfully sold ${parsedAmount} coins!` });
      setSellAmount("");
      await refreshCoinAndBalance();
    } catch (err) {
      setStatus({ type: "error", message: err.response?.data?.message || err.message });
    } finally {
      setSellLoading(false);
    }
  }

  // filter listings by search term and category
  const filteredListings = listings.filter((item) => {
    const matchesSearch = item.title.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = categoryFilter === "All" || item.category === categoryFilter;
    return matchesSearch && matchesCategory;
  });

  const activeListings = filteredListings.filter((l) => l.status === "ACTIVE");
  const soldListings = filteredListings.filter((l) => l.status === "SOLD");

  const totalCost = amount && coin ? (parseFloat(amount) * coin.currentPrice).toFixed(2) : null;

  return (
    <div className="marketplace-page">
      <div className="marketplace-card">
        <header className="marketplace-header">
          <div className="header-top-row" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h1>Marketplace</h1>
            <button
              className="buy-button"
              style={{ width: "auto", padding: "10px 20px", fontSize: "14px" }}
              onClick={() => navigate("/marketplace/new")}
            >
              + NEW LISTING
            </button>
          </div>
          <p>Manage your TimeCoin or browse goods and services.</p>
        </header>

        {/* market stats */}
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
              {coinLoading ? "—" : coin ? Number(coin.circulatingSupply).toLocaleString() : "—"} TC
            </span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Your Balance</span>
            <span className="stat-value" data-testid="user-balance">
              {balance ? `${Number(balance.available).toFixed(4)} TC` : "—"}
            </span>
          </div>
        </section>

        {/* buy and sell forms */}
        <section className="buy-section">
          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
            {/* buy form */}
            <form className="buy-form" onSubmit={handleBuy} noValidate>
              <label htmlFor="amount" className="form-label">Buy TimeCoin</label>
              <div className="input-row">
                <input
                  id="amount"
                  data-testid="amount-input"
                  type="number"
                  placeholder="0.00"
                  value={amount}
                  onChange={(e) => { setAmount(e.target.value); setStatus(null); }}
                  disabled={loading}
                  className="amount-input"
                />
                <button type="submit" data-testid="buy-button" className="buy-button" disabled={loading || !amount}>
                  {loading ? "..." : "Buy TC"}
                </button>
              </div>
              {totalCost && !status && (
                <p className="cost-preview" data-testid="cost-preview">
                  Estimated total: <strong>${totalCost}</strong>
                </p>
              )}
            </form>

            {/* sell form */}
            <form className="buy-form" onSubmit={handleSell} noValidate>
              <label htmlFor="sell-amount" className="form-label">Sell TimeCoin</label>
              <div className="input-row">
                <input
                  id="sell-amount"
                  data-testid="sell-amount-input"
                  type="number"
                  placeholder="0.00"
                  value={sellAmount}
                  onChange={(e) => { setSellAmount(e.target.value); setStatus(null); }}
                  disabled={sellLoading}
                  className="amount-input"
                />
                <button type="submit" data-testid="sell-button" className="buy-button sell-btn" disabled={sellLoading || !sellAmount}>
                  {sellLoading ? "..." : "Sell TC"}
                </button>
              </div>
              {sellAmount && balance && (
                <p className="cost-preview">
                  Balance after: <strong>{(balance.available - parseFloat(sellAmount || 0)).toFixed(4)} TC</strong>
                </p>
              )}
            </form>
          </div>

          {/* shared status message for buy/sell */}
          {status && (
            <div className={`status-message status-${status.type}`} data-testid="status-message" role="alert">
              {status.message}
            </div>
          )}
        </section>

        <hr style={{ margin: "40px 0", border: "0", borderTop: "1px solid rgba(108,149,201,0.25)" }} />

        {/* browse listings */}
        <section className="browse-section">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
            <h2 style={{ margin: 0, color: "#4bb9e9" }}>Available Listings</h2>
            <span style={{ color: "#9ab6d6", fontSize: "14px" }}>
              {activeListings.length} active · {soldListings.length} sold · {listings.length} total
            </span>
          </div>

          {/* search and category filter */}
          <div className="filter-bar" style={{ display: "flex", gap: "10px", marginBottom: "20px" }}>
            <input
              type="text"
              className="amount-input"
              placeholder="Search by title..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              data-testid="search-input"
              style={{ flex: 2 }}
            />
            <select
              className="amount-input"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              data-testid="category-filter"
              style={{ flex: 1, cursor: "pointer" }}
            >
              <option value="All">All Categories</option>
              <option value="Goods">Goods</option>
              <option value="Services">Services</option>
            </select>
          </div>

          {listingsLoading ? (
            <p className="loading-text">Loading listings...</p>
          ) : (
            <div className="listings-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "16px" }}>
              {activeListings.length > 0 ? (
                activeListings.map((listing) => (
                  <div
                    key={listing.id}
                    className="listing-card"
                    onClick={() => navigate(`/marketplace/${listing.id}`)}
                    data-testid={`listing-${listing.id}`}
                  >
                    <div className="listing-status-badge listing-status-active">ACTIVE</div>
                    <h3 className="listing-title">{listing.title}</h3>
                    <p className="listing-price">{listing.price} TC</p>
                    <span className="listing-category">{listing.category}</span>
                  </div>
                ))
              ) : (
                <p style={{ color: "#9ab6d6" }}>No active listings found matching your criteria.</p>
              )}
            </div>
          )}

          {/* sold listings shown separately below active */}
          {soldListings.length > 0 && (
            <>
              <h3 style={{ color: "#9ab6d6", marginTop: "32px", marginBottom: "12px" }}>Sold</h3>
              <div className="listings-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "16px" }}>
                {soldListings.map((listing) => (
                  <div
                    key={listing.id}
                    className="listing-card listing-card-sold"
                    onClick={() => navigate(`/marketplace/${listing.id}`)}
                    data-testid={`listing-${listing.id}`}
                  >
                    <div className="listing-status-badge listing-status-sold">SOLD</div>
                    <h3 className="listing-title">{listing.title}</h3>
                    <p className="listing-price">{listing.price} TC</p>
                    <span className="listing-category">{listing.category}</span>
                  </div>
                ))}
              </div>
            </>
          )}
        </section>
      </div>
    </div>
  );
}