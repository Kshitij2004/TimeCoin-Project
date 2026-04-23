import { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import api from "../../services/api.js";
import "./Marketplace.css";

// Default URL-synced filter values. Params equal to these are stripped from the
// URL so a clean "/marketplace" stays clean after a Clear.
const DEFAULTS = {
  q: "",
  category: "All",
  owner: "All",
  sort: "newest",
  minPrice: "",
  maxPrice: "",
};

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

  // listings + filter state (filters live in the URL so they survive reload/share)
  const [listings, setListings] = useState([]);
  const [searchParams, setSearchParams] = useSearchParams();
  const [listingsLoading, setListingsLoading] = useState(true);
  const [deletingId, setDeletingId] = useState(null);

  // identity (used to tell "mine" vs "others" apart)
  const [currentUserId, setCurrentUserId] = useState(null);

  const navigate = useNavigate();

  // Read filter values from the URL, falling back to defaults when absent.
  const searchTerm = searchParams.get("q") ?? DEFAULTS.q;
  const categoryFilter = searchParams.get("category") ?? DEFAULTS.category;
  const ownershipFilter = searchParams.get("owner") ?? DEFAULTS.owner;
  const sortBy = searchParams.get("sort") ?? DEFAULTS.sort;
  const minPrice = searchParams.get("minPrice") ?? DEFAULTS.minPrice;
  const maxPrice = searchParams.get("maxPrice") ?? DEFAULTS.maxPrice;

  // Update a single filter param. Empty / default values are deleted so URLs
  // stay short and a reset really means "/marketplace".
  const updateParam = (key, value) => {
    const next = new URLSearchParams(searchParams);
    if (value === "" || value == null || value === DEFAULTS[key]) {
      next.delete(key);
    } else {
      next.set(key, value);
    }
    setSearchParams(next, { replace: true });
  };

  const clearFilters = () => {
    setSearchParams(new URLSearchParams(), { replace: true });
  };

  const hasActiveFilters =
    searchTerm !== DEFAULTS.q ||
    categoryFilter !== DEFAULTS.category ||
    ownershipFilter !== DEFAULTS.owner ||
    sortBy !== DEFAULTS.sort ||
    minPrice !== DEFAULTS.minPrice ||
    maxPrice !== DEFAULTS.maxPrice;

  // fetch coin data, listings, balance, and identity on mount
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

        try {
          const walletRes = await api.get("/wallet");
          setCurrentUserId(walletRes.data?.userId ?? null);
        } catch {
          setCurrentUserId(null);
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

  async function handleDeleteListing(e, listingId) {
    e.stopPropagation();
    if (!window.confirm("Remove this listing? Buyers will no longer see it.")) {
      return;
    }
    setDeletingId(listingId);
    try {
      await api.delete(`/listings/${listingId}`);
      setListings((prev) => prev.filter((l) => l.id !== listingId));
    } catch (err) {
      setStatus({ type: "error", message: err.response?.data?.message || "Failed to remove listing." });
    } finally {
      setDeletingId(null);
    }
  }

  const handleLogout = () => {
    localStorage.removeItem("token");
    window.location.href = "/login";
  };

  const isMine = (listing) => currentUserId != null && listing.sellerId === currentUserId;

  // Parse price-range bounds once. NaN means "no bound on this side".
  const minPriceNum = parseFloat(minPrice);
  const maxPriceNum = parseFloat(maxPrice);

  const filteredListings = listings.filter((item) => {
    const matchesSearch = item.title.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = categoryFilter === "All" || item.category === categoryFilter;
    const matchesOwnership =
      ownershipFilter === "All" ||
      (ownershipFilter === "Mine" && isMine(item)) ||
      (ownershipFilter === "Others" && !isMine(item));
    const price = Number(item.price);
    const matchesMin = isNaN(minPriceNum) || price >= minPriceNum;
    const matchesMax = isNaN(maxPriceNum) || price <= maxPriceNum;
    return matchesSearch && matchesCategory && matchesOwnership && matchesMin && matchesMax;
  });

  const sortedListings = [...filteredListings].sort((a, b) => {
    if (sortBy === "price-asc") return Number(a.price) - Number(b.price);
    if (sortBy === "price-desc") return Number(b.price) - Number(a.price);
    const aKey = a.createdAt ? new Date(a.createdAt).getTime() : (a.id ?? 0);
    const bKey = b.createdAt ? new Date(b.createdAt).getTime() : (b.id ?? 0);
    return bKey - aKey;
  });

  const activeListings = sortedListings.filter((l) => l.status === "ACTIVE");
  const soldListings = sortedListings.filter((l) => l.status === "SOLD");
  const myListingsCount = currentUserId != null
    ? listings.filter((l) => l.sellerId === currentUserId).length
    : 0;

  const totalCost = amount && coin ? (parseFloat(amount) * coin.currentPrice).toFixed(2) : null;

  const renderListingCard = (listing, sold = false) => {
    const mine = isMine(listing);
    return (
      <div
        key={listing.id}
        className={`listing-card ${sold ? "listing-card-sold" : ""} ${mine ? "listing-card-mine" : ""}`}
        onClick={() => navigate(`/marketplace/${listing.id}`)}
        data-testid={`listing-${listing.id}`}
      >
        {listing.imageUrl && (
          <img
            src={listing.imageUrl}
            alt={listing.title}
            className="listing-thumb"
            onError={(e) => { e.currentTarget.style.display = "none"; }}
            loading="lazy"
          />
        )}
        <div className="listing-badges">
          <div className={`listing-status-badge listing-status-${sold ? "sold" : "active"}`}>
            {sold ? "SOLD" : "ACTIVE"}
          </div>
          {mine && <div className="listing-status-badge listing-status-mine">MINE</div>}
        </div>
        <h3 className="listing-title">{listing.title}</h3>
        <p className="listing-price">{listing.price} TC</p>
        <span className="listing-category">{listing.category}</span>
        {mine && !sold && (
          <div className="listing-owner-actions">
            <button
              type="button"
              className="listing-edit-btn"
              onClick={(e) => {
                e.stopPropagation();
                navigate(`/marketplace/${listing.id}/edit`);
              }}
              data-testid={`edit-listing-${listing.id}`}
              aria-label="Edit listing"
            >
              Edit
            </button>
            <button
              type="button"
              className="listing-remove-btn"
              onClick={(e) => handleDeleteListing(e, listing.id)}
              disabled={deletingId === listing.id}
              data-testid={`remove-listing-${listing.id}`}
              aria-label="Remove listing"
            >
              {deletingId === listing.id ? "…" : "Remove"}
            </button>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="marketplace-page">
      {/* sidebar — mirrors the Dashboard for cross-page navigation */}
      <aside className="sidebar">
        <div className="logo">CrypMart</div>
        <nav className="nav-links">
          <Link to="/dashboard" className="nav-link">Dashboard</Link>
          <Link to="/marketplace" className="nav-link active">Marketplace</Link>
          <Link to="/send" className="nav-link">Send</Link>
          <Link to="/history" className="nav-link">Detailed Wallet</Link>
          <Link to="/blockchain" className="nav-link">Block Explorer</Link>
          <Link to="/mining" className="nav-link">Mining</Link>
          <button
            onClick={handleLogout}
            className="nav-link logout-btn"
            style={{ background: "none", border: "none", textAlign: "left", cursor: "pointer" }}
          >
            Log Out
          </button>
        </nav>
      </aside>

      <main className="marketplace-main">
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
            <div className="stat-card">
              <span className="stat-label">My Listings</span>
              <span className="stat-value" data-testid="my-listings-count">
                {myListingsCount}
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
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px", flexWrap: "wrap", gap: "8px" }}>
              <h2 style={{ margin: 0, color: "#4bb9e9" }}>Available Listings</h2>
              <span style={{ color: "#9ab6d6", fontSize: "14px" }}>
                {activeListings.length} active · {soldListings.length} sold · {listings.length} total
              </span>
            </div>

            {/* ownership filter — All / Mine / Others */}
            <div
              className="ownership-filter"
              role="tablist"
              aria-label="Filter listings by owner"
              data-testid="ownership-filter"
            >
              {["All", "Mine", "Others"].map((opt) => (
                <button
                  key={opt}
                  type="button"
                  role="tab"
                  aria-selected={ownershipFilter === opt}
                  className={`ownership-tab ${ownershipFilter === opt ? "ownership-tab-active" : ""}`}
                  onClick={() => updateParam("owner", opt)}
                  data-testid={`ownership-${opt.toLowerCase()}`}
                  disabled={opt === "Mine" && currentUserId == null}
                  title={opt === "Mine" && currentUserId == null ? "Sign in to see your listings" : ""}
                >
                  {opt}
                </button>
              ))}
            </div>

            {/* search, category, sort */}
            <div className="filter-bar" style={{ display: "flex", gap: "10px", marginBottom: "12px", flexWrap: "wrap" }}>
              <input
                type="text"
                className="amount-input"
                placeholder="Search by title..."
                value={searchTerm}
                onChange={(e) => updateParam("q", e.target.value)}
                data-testid="search-input"
                style={{ flex: 2, minWidth: "180px" }}
              />
              <select
                className="amount-input"
                value={categoryFilter}
                onChange={(e) => updateParam("category", e.target.value)}
                data-testid="category-filter"
                style={{ flex: 1, cursor: "pointer", minWidth: "140px" }}
              >
                <option value="All">All Categories</option>
                <option value="Goods">Goods</option>
                <option value="Services">Services</option>
              </select>
              <select
                className="amount-input"
                value={sortBy}
                onChange={(e) => updateParam("sort", e.target.value)}
                data-testid="sort-filter"
                style={{ flex: 1, cursor: "pointer", minWidth: "140px" }}
              >
                <option value="newest">Newest first</option>
                <option value="price-asc">Price: low to high</option>
                <option value="price-desc">Price: high to low</option>
              </select>
            </div>

            {/* price-range filter + clear */}
            <div className="filter-bar" style={{ display: "flex", gap: "10px", marginBottom: "20px", flexWrap: "wrap", alignItems: "center" }}>
              <label className="price-range-label" htmlFor="minPrice">Price (TC)</label>
              <input
                id="minPrice"
                type="number"
                min="0"
                step="0.01"
                className="amount-input price-range-input"
                placeholder="Min"
                value={minPrice}
                onChange={(e) => updateParam("minPrice", e.target.value)}
                data-testid="min-price-input"
              />
              <span style={{ color: "#9ab6d6" }}>–</span>
              <input
                id="maxPrice"
                type="number"
                min="0"
                step="0.01"
                className="amount-input price-range-input"
                placeholder="Max"
                value={maxPrice}
                onChange={(e) => updateParam("maxPrice", e.target.value)}
                data-testid="max-price-input"
              />
              <button
                type="button"
                className="clear-filters-btn"
                onClick={clearFilters}
                disabled={!hasActiveFilters}
                data-testid="clear-filters"
                style={{ marginLeft: "auto" }}
              >
                Clear filters
              </button>
            </div>

            {listingsLoading ? (
              <p className="loading-text">Loading listings...</p>
            ) : (
              <div className="listings-grid" style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(220px, 1fr))", gap: "16px" }}>
                {activeListings.length > 0 ? (
                  activeListings.map((listing) => renderListingCard(listing, false))
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
                  {soldListings.map((listing) => renderListingCard(listing, true))}
                </div>
              </>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
