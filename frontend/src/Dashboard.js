// frontend/src/pages/Dashboard.js
import React from "react";
import { Link } from "react-router-dom";
import "./Dashboard.css";

export default function Dashboard() {
  // TODO: 之后可以把这些改成从后端/Context/props 来
  const username = "User";
  const walletBalance = "14.50 CRYP";
  const walletUsd = "≈ $2,345.50 USD";
  const coinPrice = "$161.75";
  const coinDelta = "+5.2% in the last 24h";

  const popularItems = [
    { title: "Vintage Leather Jacket", price: "0.25 CRYP" },
    { title: "Custom Gaming PC", price: "1.30 CRYP" },
    { title: "Rare First Edition Book", price: "0.38 CRYP" },
    { title: "Handmade Necklace", price: "0.15 CRYP" },
  ];

  return (
    <div className="dashboard-page">
      <aside className="sidebar">
        <div className="logo">CrypMart</div>

        <nav className="nav-links">
          <Link to="/dashboard" className="nav-link active">
            Dashboard
          </Link>

          <Link to="/marketplace" className="nav-link">
            Marketplace
          </Link>

          <Link to="/wallet" className="nav-link">
            Detailed Wallet
          </Link>

          <button
            className="nav-link logout-btn"
            type="button"
            onClick={() => {
              // TODO: 这里接你们的 logout 逻辑（清 token / call API / navigate）
              console.log("logout");
            }}
          >
            Log Out
          </button>
        </nav>
      </aside>

      <main className="main-content">
        <header className="header">
          <h1>Welcome back, {username}</h1>
        </header>

        <div className="stats-grid">
          <section className="stat-card">
            <h3>Wallet Balance</h3>
            <div className="value">{walletBalance}</div>
            <div className="sub-value">{walletUsd}</div>
          </section>

          <section className="stat-card">
            <h3>Current Coin Price</h3>
            <div className="value">{coinPrice}</div>
            <div className="sub-value">{coinDelta}</div>
          </section>
        </div>

        <section className="chart-section">
          <h3>Price Trend (30 Days)</h3>
          <div className="chart-container" aria-label="Price trend chart">
            <svg className="line-graph" viewBox="0 0 1000 200" preserveAspectRatio="none">
              <path
                className="line-path"
                d="M 0 180 L 100 150 L 200 160 L 300 90 L 400 110 L 500 60 L 600 80 L 700 30 L 800 50 L 900 20 L 1000 10"
              />
            </svg>
          </div>
        </section>

        <h2 className="products-header">Popular in Marketplace</h2>

        <div className="products-grid">
          {popularItems.map((item) => (
            <div className="product-card" key={item.title}>
              <div className="product-image">Image Placeholder</div>
              <div className="product-title">{item.title}</div>
              <div className="product-price">{item.price}</div>
              <button className="buy-btn" type="button">
                View Details
              </button>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
