import React, { useState, useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import api from "../../services/api.js";
import "./Marketplace.css";

export default function EditListing() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    price: "",
    category: "Goods",
    imageUrl: "",
  });
  // Capture the listing's current status so the PUT request preserves it;
  // the Remove button elsewhere is what flips to REMOVED.
  const [originalStatus, setOriginalStatus] = useState("ACTIVE");
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState("");

  const isAuthenticated = !!localStorage.getItem("token");

  useEffect(() => {
    if (!isAuthenticated) {
      navigate("/login", { replace: true });
      return;
    }

    const fetchListing = async () => {
      setFetching(true);
      try {
        const [listingRes, walletRes] = await Promise.all([
          api.get(`/listings/${id}`),
          api.get("/wallet").catch(() => null),
        ]);
        const listing = listingRes.data;

        // Ownership guard: non-owners get sent back. Backend enforces this too,
        // but this avoids rendering an edit form the user can't actually submit.
        if (walletRes && listing.sellerId !== walletRes.data?.userId) {
          navigate(`/marketplace/${id}`, { replace: true });
          return;
        }

        setFormData({
          title: listing.title || "",
          description: listing.description || "",
          price: listing.price != null ? String(listing.price) : "",
          category: listing.category || "Goods",
          imageUrl: listing.imageUrl || "",
        });
        setOriginalStatus(listing.status || "ACTIVE");
      } catch (err) {
        setError(err.response?.data?.message || "Failed to load listing.");
      } finally {
        setFetching(false);
      }
    };

    fetchListing();
  }, [id, isAuthenticated, navigate]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (error) setError("");
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const trimmedTitle = formData.title.trim();
    const trimmedDesc = formData.description.trim();
    const priceNum = parseFloat(formData.price);

    if (!trimmedTitle || !trimmedDesc || isNaN(priceNum)) {
      setError("All fields are required. Please enter a valid price.");
      return;
    }

    if (priceNum <= 0) {
      setError("Price must be greater than 0 TimeCoin.");
      return;
    }

    setLoading(true);
    try {
      await api.put(`/listings/${id}`, {
        title: trimmedTitle,
        description: trimmedDesc,
        price: priceNum,
        category: formData.category,
        imageUrl: formData.imageUrl.trim() || null,
        status: originalStatus,
      });
      navigate(`/marketplace/${id}`);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to update listing. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  if (!isAuthenticated) return null;

  if (fetching) {
    return (
      <div className="marketplace-page">
        <main className="marketplace-main">
          <div className="marketplace-card">
            <p className="loading-text">Loading listing...</p>
          </div>
        </main>
      </div>
    );
  }

  return (
    <div className="marketplace-page">
      <main className="marketplace-main">
        <div className="marketplace-card">
          <header className="marketplace-header">
            <h1>Edit Listing</h1>
            <p>Update the details of your listing.</p>
          </header>

          {error && (
            <div className="status-message status-error" role="alert">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="buy-form" noValidate>
            <div className="form-group">
              <label className="form-label" htmlFor="title">Title</label>
              <input
                id="title"
                name="title"
                type="text"
                className="amount-input"
                value={formData.title}
                onChange={handleChange}
                required
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="description">Description</label>
              <textarea
                id="description"
                name="description"
                className="amount-input create-listing-textarea"
                value={formData.description}
                onChange={handleChange}
                required
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="price">Price (TimeCoin)</label>
              <input
                id="price"
                name="price"
                type="number"
                step="0.01"
                className="amount-input"
                value={formData.price}
                onChange={handleChange}
                required
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="category">Category</label>
              <select
                id="category"
                name="category"
                className="amount-input"
                value={formData.category}
                onChange={handleChange}
                style={{ cursor: "pointer" }}
                disabled={loading}
              >
                <option value="Goods">Goods</option>
                <option value="Services">Services</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="imageUrl">Image URL (optional)</label>
              <input
                id="imageUrl"
                name="imageUrl"
                type="url"
                className="amount-input"
                value={formData.imageUrl}
                onChange={handleChange}
                placeholder="https://example.com/photo.jpg"
                disabled={loading}
              />
              {formData.imageUrl && (
                <img
                  src={formData.imageUrl}
                  alt="Listing preview"
                  className="listing-image-preview"
                  onError={(e) => { e.currentTarget.style.display = "none"; }}
                  onLoad={(e) => { e.currentTarget.style.display = "block"; }}
                />
              )}
            </div>

            <div className="create-listing-actions">
              <button type="submit" className="buy-button" disabled={loading}>
                {loading ? "SAVING..." : "SAVE CHANGES"}
              </button>
              <button
                type="button"
                className="buy-button"
                style={{ backgroundColor: "rgba(75,75,75,0.5)" }}
                onClick={() => navigate(`/marketplace/${id}`)}
                disabled={loading}
              >
                CANCEL
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}
