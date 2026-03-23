import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../../services/api.js';
import './Marketplace.css';

export default function ListingDetail() {
    const { id } = useParams(); // Grabs the ID from the URL (/marketplace/:id)
    const navigate = useNavigate();
    
    const [listing, setListing] = useState(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [purchaseLoading, setPurchaseLoading] = useState(false);

    // 1. Fetch the specific listing data on mount
    useEffect(() => {
        const fetchListing = async () => {
            setLoading(true);
            try {
                const response = await api.get(`/listings/${id}`);
                setListing(response.data);
            } catch (err) {
                setError(err.response?.data?.message || "Listing not found or failed to load.");
            } finally {
                setLoading(false);
            }
        };

        fetchListing();
    }, [id]);

    // 2. Handle the Purchase logic (Requirement: Confirmation & Balance check)
    const handlePurchase = async () => {
        const confirmPurchase = window.confirm(
            `Confirm purchase of "${listing.title}" for ${listing.price} TimeCoin?`
        );

        if (!confirmPurchase) return;

        setPurchaseLoading(true);
        setError('');

        try {
            // POST request to the purchase endpoint defined in your ticket
            await api.post(`/listings/${id}/purchase`);
            
            // On success, show feedback and redirect back to Marketplace
            alert("Success! You have purchased this item.");
            navigate('/marketplace');
        } catch (err) {
            // Display backend validation (e.g., "Insufficient funds")
            setError(err.response?.data?.message || "Purchase failed. Please check your balance.");
        } finally {
            setPurchaseLoading(false);
        }
    };

    if (loading) return <div className="marketplace-page"><p>Loading details...</p></div>;
    if (error) return <div className="marketplace-page"><p style={{ color: 'red' }}>{error}</p></div>;
    if (!listing) return null;

    return (
        <div className="marketplace-page">
            <div className="marketplace-card">
                <header className="marketplace-header">
                    <h1>{listing.title}</h1>
                    <span className="category-tag">{listing.category}</span>
                </header>

                <div className="detail-content" style={{ marginTop: '20px' }}>
                    <p className="description" style={{ fontSize: '1.1rem', lineHeight: '1.6' }}>
                        {listing.description}
                    </p>
                    
                    <div className="seller-info" style={{ margin: '20px 0', padding: '15px', background: '#f8f9fa', borderRadius: '8px' }}>
                        <p><strong>Seller:</strong> {listing.sellerName || "Anonymous Seller"}</p>
                        <p><strong>Price:</strong> <span style={{ color: '#2ecc71', fontWeight: 'bold' }}>{listing.price} TimeCoin</span></p>
                    </div>

                    <div className="actions" style={{ display: 'flex', gap: '10px', marginTop: '30px' }}>
                        {/* 3. Conditional "Buy" button based on item status */}
                        {!listing.isSold ? (
                            <button 
                                className="buy-button" 
                                onClick={handlePurchase}
                                disabled={purchaseLoading}
                            >
                                {purchaseLoading ? "PROCESSING..." : "BUY NOW"}
                            </button>
                        ) : (
                            <button className="buy-button" style={{ backgroundColor: '#ccc', cursor: 'not-allowed' }} disabled>
                                ITEM SOLD
                            </button>
                        )}
                        
                        <button 
                            className="buy-button" 
                            style={{ backgroundColor: '#6c757d' }} 
                            onClick={() => navigate('/marketplace')}
                        >
                            BACK TO MARKET
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}