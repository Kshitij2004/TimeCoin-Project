import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './components/ProtectedRoute.js';

// Import your pages from the /pages folder
import Landing from './pages/Landing.js';
import Login from './pages/Login.js';
import Register from './pages/Register.js';
import Dashboard from './pages/dashboard/Dashboard.js';
import Marketplace from './pages/marketplace/Marketplace.js';
import History from './pages/History.js';
import Send from './pages/send/Send.js';

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
      </Routes>
    </Router>
  );
}

export default App;