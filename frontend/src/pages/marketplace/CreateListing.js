import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../../services/api.js'; // Your Axios instance with JWT interceptors
import './Marketplace.css';

export default function CreateListing() {
    const navigate = useNavigate();
    
    // 1. Component State
    // Consolidating all fields into one object makes state updates more scalable.
    const [formData, setFormData] = useState({
        title: '',
        description: '',
        price: '',
        category: 'Goods'
    });
    const [loading, setLoading] = useState(false); // Controls button states during API calls
    const [error, setError] = useState('');      // Stores validation or server error messages

    // 2. Auth Gate Logic
    // Check for the presence of a token to determine if the user is logged in.
    const isAuthenticated = !!localStorage.getItem('token');

    useEffect(() => {
        // If the user isn't logged in, we boot them back to the login page immediately.
        // 'replace: true' prevents them from hitting "back" to return to this restricted page.
        if (!isAuthenticated) {
            navigate("/login", { replace: true });
        }
    }, [isAuthenticated, navigate]);

    /**
     * Universal Input Handler
     * Dynamically updates the formData object based on the input's 'name' attribute.
     */
    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({ ...prev, [name]: value }));
        
        // UX: Clear previous errors as soon as the user starts fixing the input.
        if (error) setError(''); 
    };

    /**
     * Form Submission & Validation
     */
    const handleSubmit = async (e) => {
        e.preventDefault(); // Stop page reload
        
        // A. Client-side Sanitization
        // Trimming whitespace prevents users from submitting " " as a title.
        const trimmedTitle = formData.title.trim();
        const trimmedDesc = formData.description.trim();
        const priceNum = parseFloat(formData.price);

        // B. Basic Validation
        if (!trimmedTitle || !trimmedDesc || isNaN(priceNum)) {
            setError("All fields are required. Please enter a valid price.");
            return;
        }

        // C. Business Logic Validation
        // For a cryptocurrency project, preventing negative or zero-value transactions is critical.
        if (priceNum <= 0) {
            setError("Price must be greater than 0 TimeCoin.");
            return;
        }

        setLoading(true); // Disable buttons to prevent "double-click" duplicate posts
        try {
            // D. The API Call
            // We pass the sanitized data. The 'api' instance handles the Bearer Token automatically.
            const response = await api.post('/listings', {
                title: trimmedTitle,
                description: trimmedDesc,
                price: priceNum,
                category: formData.category
            });

            // E. Dynamic Success Redirect
            // If the server returns a new ID, we take the user to their new listing.
            // Otherwise, we take them back to the general marketplace.
            const newListingId = response.data?.id;
            if (newListingId) {
                navigate(`/marketplace/${newListingId}`);
            } else {
                navigate('/marketplace');
            }
        } catch (err) {
            // F. Error Handling
            // We look for a message from the Spring Boot backend (e.g., "DB Connection Failed")
            // before falling back to a generic error message.
            const backendMessage = err.response?.data?.message || "Failed to create listing. Please try again.";
            setError(backendMessage);
        } finally {
            setLoading(false); // Re-enable the form
        }
    };

    // 3. Security Render Guard
    // If not authenticated, we return null to prevent the form from even appearing for a split second (flicker).
    if (!isAuthenticated) return null;

    return (
        <div className="marketplace-page">
            <div className="marketplace-card">
                <header className="marketplace-header">
                    <h1>Create a New Listing</h1>
                    <p>Enter details to sell your goods or services for TimeCoin.</p>
                </header>

                {/* Conditional Error Alert */}
                {error && (
                    <div className="status-message status-error" role="alert" style={{ marginBottom: '20px' }}>
                        {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="buy-form" noValidate>
                    {/* Title Input */}
                    <div className="form-group" style={{ marginBottom: '20px' }}>
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

                    {/* Description Textarea */}
                    <div className="form-group" style={{ marginBottom: '20px' }}>
                        <label className="form-label" htmlFor="description">Description</label>
                        <textarea
                            id="description"
                            name="description"
                            className="amount-input"
                            style={{ height: '100px', paddingTop: '10px', resize: 'vertical' }}
                            value={formData.description}
                            onChange={handleChange}
                            placeholder="Provide details about your offering..."
                            required
                            disabled={loading}
                        />
                    </div>

                    {/* Price Input (Number with step for decimals) */}
                    <div className="form-group" style={{ marginBottom: '20px' }}>
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

                    {/* Category Dropdown */}
                    <div className="form-group" style={{ marginBottom: '30px' }}>
                        <label className="form-label" htmlFor="category">Category</label>
                        <select 
                            id="category"
                            name="category" 
                            className="amount-input" 
                            value={formData.category} 
                            onChange={handleChange}
                            style={{ cursor: 'pointer' }}
                            disabled={loading}
                        >
                            <option value="Goods">Goods</option>
                            <option value="Services">Services</option>
                        </select>
                    </div>

                    {/* Form Actions (Submit vs Cancel) */}
                    <div className="form-actions" style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                        <button 
                            type="submit" 
                            className="buy-button" 
                            disabled={loading}
                        >
                            {/* Feedback: Change text while the API call is in flight */}
                            {loading ? "PUBLISHING..." : "CREATE LISTING"}
                        </button>
                        
                        <button 
                            type="button" 
                            className="buy-button" 
                            style={{ backgroundColor: '#4a4a4a' }}
                            onClick={() => navigate('/marketplace')} // Standard navigation back
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