import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute.js';

// Import pages from the /pages folder
import Landing from './pages/Landing.js';
import Login from './pages/Login.js';
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
        <Route path="/register" element={<Register />} />

        {/* Dashboard handles its own auth internally */}
        <Route path="/dashboard" element={<Dashboard />} />

        {/* Protected Routes */}
        <Route path="/marketplace" element={
          <ProtectedRoute><Marketplace /></ProtectedRoute>
        } />

        <Route path="/marketplace/new" element={
          <ProtectedRoute><CreateListing /></ProtectedRoute>
        } />

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