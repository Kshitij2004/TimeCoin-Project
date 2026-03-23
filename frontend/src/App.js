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
  // 1. Removed the mock isAuthenticated variable. 
  // The ProtectedRoute component now handles this by checking localStorage.

  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* Protected Routes */}
        {/* 2. Removed the isAuth prop from ProtectedRoute calls */}
        <Route path="/dashboard" element={
          <ProtectedRoute><Dashboard /></ProtectedRoute>
        } />
        <Route path="/marketplace" element={
          <ProtectedRoute><Marketplace /></ProtectedRoute>
        } />
        <Route path="/send" element={
          <ProtectedRoute><Send /></ProtectedRoute>
        } />
        <Route path="/history" element={
          <ProtectedRoute><History /></ProtectedRoute>
        } />
      </Routes>
    </Router>
  );
}

export default App;