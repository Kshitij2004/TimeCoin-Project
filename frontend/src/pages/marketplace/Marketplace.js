import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom"; 
import api from "../../services/api.js"; 
import "./Marketplace.css";

export default function Marketplace() {
  // Coin states
  const [coin, setCoin] = useState(null);
  const [amount, setAmount] = useState("");
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [coinLoading, setCoinLoading] = useState(true);

  // New states for Browsing Listings
  const [listings, setListings] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [categoryFilter, setCategoryFilter] = useState("All");
  const [listingsLoading, setListingsLoading] = useState(true);

  const navigate = useNavigate();

  // ── Fetch data on mount ──────────────────────────────────────────────
  useEffect(() => {
    const fetchData = async () => {
      setCoinLoading(true);
      setListingsLoading(true);
      try {
        // Fetch coin data and all marketplace listings
        const [coinRes, listingsRes] = await Promise.all([
          api.get("/coin"),
          api.get("/listings")
        ]);
        
        setCoin(coinRes.data);
        setListings(listingsRes.data);
      } catch (err) {
        console.error("Data fetch failed", err);
      } finally {
        setCoinLoading(false);
        setListingsLoading(false);
      }
    };

    fetchData();
  }, []);

  // ── Buy Coins handler ──────────────────────────────────────────────────
  async function handleBuy(e) {
    e.preventDefault();
    setStatus(null);
    const parsedAmount = parseFloat(amount);
    if (!parsedAmount || parsedAmount <= 0) {
      setStatus({ type: "error", message: "Please enter a valid amount." });
      return;
    }

    setLoading(true);
    try {
      await api.post("/coin/buy", { symbol: "TC", amount: parsedAmount });
      setStatus({ type: "success", message: `Successfully purchased ${parsedAmount} coins!` });
      setAmount("");
      const updated = await api.get("/coin");
      setCoin(updated.data);
    } catch (err) {
      setStatus({ type: "error", message: err.response?.data?.message || err.message });
    } finally {
      setLoading(false);
    }
  }

  // ── Filter & Search Logic ─────────────────────────────────────────────
  const filteredListings = listings.filter(item => {
    const matchesSearch = item.title.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = categoryFilter === "All" || item.category === categoryFilter;
    return matchesSearch && matchesCategory;
  });

  const totalCost = amount && coin ? (parseFloat(amount) * coin.currentPrice).toFixed(2) : null;

  return (
    <div className="marketplace-page">
      <div className="marketplace-card">
        <header className="marketplace-header">
          <div className="header-top-row" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <h1>Marketplace</h1>
            <button 
              className="buy-button" 
              style={{ width: 'auto', padding: '10px 20px', fontSize: '14px' }}
              onClick={() => navigate('/marketplace/new')}
            >
              + NEW LISTING
            </button>
          </div>
          <p>Manage your TimeCoin or browse goods and services.</p>
        </header>

        {/* ── Coin Stats & Buy Section ── */}
        <section className="market-stats" aria-label="Market statistics">
          <div className="stat-card">
            <span className="stat-label">Current Price</span>
            <span className="stat-value">${coinLoading ? "—" : coin ? Number(coin.currentPrice).toFixed(2) : "—"}</span>
          </div>
          <div className="stat-card">
            <span className="stat-label">Your Balance</span>
            <span className="stat-value">{coinLoading ? "—" : coin ? Number(coin.circulatingSupply).toLocaleString() : "—"} TC</span>
          </div>
        </section>

        <section className="buy-section">
          <form className="buy-form" onSubmit={handleBuy} noValidate>
            <div className="input-row">
              <input
                type="number"
                placeholder="Buy coins..."
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="amount-input"
              />
              <button type="submit" className="buy-button" disabled={loading || !amount}>
                {loading ? "..." : "Buy TC"}
              </button>
            </div>
            {status && <div className={`status-message status-${status.type}`}>{status.message}</div>}
          </form>
        </section>

        <hr style={{ margin: '40px 0', border: '0', borderTop: '1px solid #eee' }} />

        {/* ── Browsing Section ── */}
        <section className="browse-section">
          <h2>Available Listings</h2>
          
          <div className="filter-bar" style={{ display: 'flex', gap: '10px', marginBottom: '20px' }}>
            <input 
              type="text" 
              className="amount-input"
              placeholder="Search by keyword..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ flex: 2 }}
            />
            <select 
              className="amount-input"
              value={categoryFilter}
              onChange={(e) => setCategoryFilter(e.target.value)}
              style={{ flex: 1, cursor: 'pointer' }}
            >
              <option value="All">All Categories</option>
              <option value="Goods">Goods</option>
              <option value="Services">Services</option>
            </select>
          </div>

          {listingsLoading ? (
            <p>Loading listings...</p>
          ) : (
            <div className="listings-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '20px' }}>
              {filteredListings.length > 0 ? filteredListings.map(listing => (
                <div 
                  key={listing.id} 
                  className={`listing-card ${listing.isSold ? 'sold' : ''}`}
                  onClick={() => navigate(`/marketplace/${listing.id}`)}
                  style={{ 
                    border: '1px solid #ddd', 
                    padding: '15px', 
                    borderRadius: '8px', 
                    cursor: 'pointer',
                    opacity: listing.isSold ? 0.6 : 1,
                    position: 'relative'
                  }}
                >
                  <h3 style={{ margin: '0 0 10px 0' }}>{listing.title}</h3>
                  <p style={{ fontWeight: 'bold', color: '#2ecc71' }}>{listing.price} TC</p>
                  <span className="category-tag" style={{ fontSize: '12px', background: '#eee', padding: '2px 6px', borderRadius: '4px' }}>
                    {listing.category}
                  </span>
                  {listing.isSold && (
                    <div style={{ position: 'absolute', top: '10px', right: '10px', color: 'red', fontWeight: 'bold', border: '1px solid red', padding: '2px' }}>
                      SOLD
                    </div>
                  )}
                </div>
              )) : (
                <p>No listings found matching your criteria.</p>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}