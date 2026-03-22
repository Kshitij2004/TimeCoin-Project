import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute';

// Import your pages from the /pages folder
import Landing from './pages/Landing';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/dashboard/Dashboard';
import Marketplace from './pages/marketplace/Marketplace';
import History from './pages/History';
import BlockchainExplorer from './pages/BlockchainExplorer';
import Send from './pages/send/Send';

function App() {
  // Mock authentication state for Sprint 1
  const isAuthenticated = true; // Change to false to test unauthenticated flow


  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected Routes */}
        <Route path="/dashboard" element={
          <ProtectedRoute isAuth={isAuthenticated}><Dashboard /></ProtectedRoute>
        } />
        <Route path="/marketplace" element={
          <ProtectedRoute isAuth={isAuthenticated}><Marketplace /></ProtectedRoute>
        } />
        <Route path="/send" element={
          <ProtectedRoute isAuth={isAuthenticated}><Send /></ProtectedRoute>
        } />
        <Route path="/history" element={
          <ProtectedRoute isAuth={isAuthenticated}><History /></ProtectedRoute>
        } />
        <Route path="/blockchain" element={
          <ProtectedRoute isAuth={isAuthenticated}><BlockchainExplorer /></ProtectedRoute>
        } />
      </Routes>
    </Router>
  );
}

export default App;
