import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';

/**
 * Acceptance Criteria: "Attach JWT token from localStorage/context"
 * This component now checks localStorage directly to ensure the user 
 * has a valid session before rendering protected pages.
 */
const ProtectedRoute = ({ children }) => {
    const token = localStorage.getItem('token');
    const location = useLocation();

    if (!token) {
        // Redirect unauthenticated users to login, but save the 
        // location they were trying to go to so we can send them back after login.
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    return children;
};

export default ProtectedRoute;