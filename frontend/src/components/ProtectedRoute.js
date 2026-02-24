import { Navigate } from 'react-router-dom';

// This wrapper checks the 'isAuth' prop before rendering content
const ProtectedRoute = ({ isAuth, children }) => {
  if (!isAuth) {
    // Redirect unauthenticated users to login
    return <Navigate to="/login" replace />;
  }
  return children;
};

export default ProtectedRoute;