import React from 'react';
import { Link } from 'react-router-dom';
import '../Dashboard.css';

function Dashboard() {
    return (
        <div className="dashboard-page">
            <aside className="sidebar">
                <div className="logo">CrypMart</div>
                <nav className="nav-links">
                    <Link to="/dashboard" className="nav-link active">Dashboard</Link>
                    <Link to="/blockchain" className="nav-link">Blockchain Explorer</Link>
                    <Link to="/marketplace" className="nav-link">Marketplace</Link>
                    <Link to="/history" className="nav-link">Detailed Wallet</Link>
                    <Link to="/login" className="nav-link logout-btn">Log Out</Link>
                </nav>
            </aside>

            <main className="main-content">
                <header className="header">
                    <h1>Welcome back, User</h1>
                </header>

                <div className="stats-grid">
                    <div className="stat-card">
                        <h3>Wallet Balance</h3>
                        <div className="value">14.50 CRYP</div>
                        <div className="sub-value">≈ $2,345.50 USD</div>
                    </div>
                    <div className="stat-card">
                        <h3>Current Coin Price</h3>
                        <div className="value">$161.75</div>
                        <div className="sub-value">+5.2% in the last 24h</div>
                    </div>
                </div>

                <div className="chart-section">
                    <h3>Price Trend (30 Days)</h3>
                    <div className="chart-container">
                        <svg className="line-graph" viewBox="0 0 1000 200" preserveAspectRatio="none">
                            <path className="line-path" d="M 0 180 L 100 150 L 200 160 L 300 90 L 400 110 L 500 60 L 600 80 L 700 30 L 800 50 L 900 20 L 1000 10" />
                        </svg>
                    </div>
                </div>

                <h2 className="products-header">Popular in Marketplace</h2>
                <div className="products-grid">
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Vintage Leather Jacket</div>
                        <div className="product-price">0.25 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Custom Gaming PC</div>
                        <div className="product-price">1.30 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Rare First Edition Book</div>
                        <div className="product-price">0.38 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                    <div className="product-card">
                        <div className="product-image">Image Placeholder</div>
                        <div className="product-title">Handmade Necklace</div>
                        <div className="product-price">0.15 CRYP</div>
                        <button className="buy-btn">View Details</button>
                    </div>
                </div>
            </main>
        </div>
    );
}

export default Dashboard;
