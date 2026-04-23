import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../services/api.js";
import "./Marketplace.css";

export default function CreateListing() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    price: "",
    category: "Goods",
    imageUrl: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const isAuthenticated = !!localStorage.getItem("token");

  // redirect to login if not authenticated
  useEffect(() => {
    if (!isAuthenticated) {
      navigate("/login", { replace: true });
    }
  }, [isAuthenticated, navigate]);

  // updates form state and clears any previous error
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
      const response = await api.post("/listings", {
        title: trimmedTitle,
        description: trimmedDesc,
        price: priceNum,
        category: formData.category,
        imageUrl: formData.imageUrl.trim() || null,
      });

      const newListingId = response.data?.id;
      if (newListingId) {
        navigate(`/marketplace/${newListingId}`);
      } else {
        navigate("/marketplace");
      }
    } catch (err) {
      const backendMessage = err.response?.data?.message || "Failed to create listing. Please try again.";
      setError(backendMessage);
    } finally {
      setLoading(false);
    }
  };

  if (!isAuthenticated) return null;

  return (
    <div className="marketplace-page">
      <div className="marketplace-card">
        <header className="marketplace-header">
          <h1>Create a New Listing</h1>
          <p>Enter details to sell your goods or services for TimeCoin.</p>
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
              placeholder="e.g., Web Development Consultation"
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
              placeholder="Provide details about your offering..."
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
              placeholder="0.00"
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
              {loading ? "PUBLISHING..." : "CREATE LISTING"}
            </button>
            <button
              type="button"
              className="buy-button"
              style={{ backgroundColor: "rgba(75,75,75,0.5)" }}
              onClick={() => navigate("/marketplace")}
              disabled={loading}
            >
              CANCEL
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}