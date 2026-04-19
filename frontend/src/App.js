import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute.js';

// Import pages from the /pages folder
import Landing from './pages/Landing.js';
import Login from './pages/Login.js';
import LoginVerify from './pages/LoginVerify.js';
import Register from './pages/Register.js';
import Dashboard from './pages/dashboard/Dashboard.js';
import Marketplace from './pages/marketplace/Marketplace.js';
import History from './pages/History.js';
import Send from './pages/send/Send.js';
import BlockchainExplorer from './pages/BlockchainExplorer.js';

// Marketplace specific pages
import CreateListing from './pages/marketplace/CreateListing.js';
import ListingDetail from './pages/marketplace/ListingDetail.js';

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/login/verify" element={<LoginVerify />} />
        <Route path="/register" element={<Register />} />

        {/* Protected Routes */}
        <Route path="/dashboard" element={
          <ProtectedRoute><Dashboard /></ProtectedRoute>
        } />

        {/* Marketplace Routes */}
        <Route path="/marketplace" element={
          <ProtectedRoute><Marketplace /></ProtectedRoute>
        } />

        {/* Route for creating a new listing */}
        <Route path="/marketplace/new" element={
          <ProtectedRoute><CreateListing /></ProtectedRoute>
        } />

        {/* Dynamic Route for viewing/buying a specific listing (Issue Requirement) */}
        <Route path="/marketplace/:id" element={
          <ProtectedRoute><ListingDetail /></ProtectedRoute>
        } />

        <Route path="/send" element={
          <ProtectedRoute><Send /></ProtectedRoute>
        } />
        <Route path="/history" element={
          <ProtectedRoute><History /></ProtectedRoute>
        } />
        <Route path="/blockchain" element={
          <ProtectedRoute><BlockchainExplorer /></ProtectedRoute>
        } />
      </Routes>
    </Router>
  );
}

export default App;