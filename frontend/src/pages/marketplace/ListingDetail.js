import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../../services/api.js";
import "./Marketplace.css";

export default function ListingDetail() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [listing, setListing] = useState(null);
  const [balance, setBalance] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [purchaseLoading, setPurchaseLoading] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  // fetch listing details and user balance on mount
  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const listingRes = await api.get(`/listings/${id}`);
        setListing(listingRes.data);

        try {
          const balanceRes = await api.get("/wallet/balance");
          setBalance(balanceRes.data);
        } catch {
          setBalance(null);
        }
      } catch (err) {
        setError(err.response?.data?.message || "Listing not found or failed to load.");
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);

  // sends the purchase request after user confirms
  const handlePurchase = async () => {
    setPurchaseLoading(true);
    setError("");

    try {
      await api.post(`/listings/${id}/purchase`);
      alert("Success! You have purchased this item.");
      navigate("/marketplace");
    } catch (err) {
      setError(err.response?.data?.message || "Purchase failed. Please check your balance.");
      setShowConfirm(false);
    } finally {
      setPurchaseLoading(false);
    }
  };

  if (loading) return <div className="marketplace-page"><p className="loading-text">Loading details...</p></div>;
  if (error && !listing) return <div className="marketplace-page"><p style={{ color: "#ef4444" }}>{error}</p></div>;
  if (!listing) return null;

  const isActive = listing.status === "ACTIVE";
  const hasEnoughBalance = balance && balance.available >= listing.price;

  return (
    <div className="marketplace-page">
      <div className="marketplace-card">
        <header className="marketplace-header">
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            <h1>{listing.title}</h1>
            <div className={`listing-status-badge listing-status-${isActive ? "active" : "sold"}`}>
              {listing.status}
            </div>
          </div>
        </header>

        <div style={{ marginTop: "20px" }}>
          {listing.imageUrl && (
            <img
              src={listing.imageUrl}
              alt={listing.title}
              className="listing-detail-image"
              onError={(e) => { e.currentTarget.style.display = "none"; }}
            />
          )}
          <p style={{ fontSize: "1.1rem", lineHeight: "1.6", color: "#c2d6ef" }}>
            {listing.description}
          </p>

          {/* listing info cards */}
          <div className="listing-detail-info">
            <div className="stat-card">
              <span className="stat-label">Price</span>
              <span className="stat-value" style={{ color: "#2ecc71" }}>{listing.price} TC</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Category</span>
              <span className="stat-value">{listing.category}</span>
            </div>
            <div className="stat-card">
              <span className="stat-label">Your Balance</span>
              <span className="stat-value" style={{ color: hasEnoughBalance ? "#2ecc71" : "#ef4444" }}>
                {balance ? `${Number(balance.available).toFixed(4)} TC` : "—"}
              </span>
            </div>
          </div>

          {/* error display */}
          {error && (
            <div className="status-message status-error" role="alert" style={{ marginTop: "16px" }}>
              {error}
            </div>
          )}

          {/* purchase confirmation panel, shown when user clicks buy */}
          {showConfirm && isActive && (
            <div className="purchase-confirm-panel">
              <h3 style={{ margin: "0 0 12px 0", color: "#4bb9e9" }}>Confirm Purchase</h3>
              <div style={{ color: "#c2d6ef", marginBottom: "8px" }}>
                <p style={{ margin: "4px 0" }}>Item: <strong>{listing.title}</strong></p>
                <p style={{ margin: "4px 0" }}>Price: <strong style={{ color: "#2ecc71" }}>{listing.price} TC</strong></p>
                <p style={{ margin: "4px 0" }}>
                  Your balance: <strong style={{ color: hasEnoughBalance ? "#2ecc71" : "#ef4444" }}>
                    {balance ? `${Number(balance.available).toFixed(4)} TC` : "Unknown"}
                  </strong>
                </p>
                {balance && (
                  <p style={{ margin: "4px 0" }}>
                    After purchase: <strong>{(balance.available - listing.price).toFixed(4)} TC</strong>
                  </p>
                )}
              </div>

              {!hasEnoughBalance && (
                <div className="status-message status-error" style={{ marginBottom: "12px" }}>
                  Insufficient balance to complete this purchase.
                </div>
              )}

              <div style={{ display: "flex", gap: "10px" }}>
                <button
                  className="buy-button"
                  onClick={handlePurchase}
                  disabled={purchaseLoading || !hasEnoughBalance}
                  style={{ flex: 1 }}
                >
                  {purchaseLoading ? "PROCESSING..." : "CONFIRM PURCHASE"}
                </button>
                <button
                  className="buy-button"
                  style={{ flex: 1, backgroundColor: "rgba(75,75,75,0.5)" }}
                  onClick={() => setShowConfirm(false)}
                  disabled={purchaseLoading}
                >
                  CANCEL
                </button>
              </div>
            </div>
          )}

          {/* action buttons */}
          <div style={{ display: "flex", gap: "10px", marginTop: "24px" }}>
            {isActive && !showConfirm && (
              <button className="buy-button" onClick={() => setShowConfirm(true)}>
                BUY NOW
              </button>
            )}
            {!isActive && (
              <button className="buy-button" style={{ opacity: 0.4, cursor: "not-allowed" }} disabled>
                {listing.status}
              </button>
            )}
            <button
              className="buy-button"
              style={{ backgroundColor: "rgba(75,75,75,0.5)" }}
              onClick={() => navigate("/marketplace")}
            >
              BACK TO MARKET
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}