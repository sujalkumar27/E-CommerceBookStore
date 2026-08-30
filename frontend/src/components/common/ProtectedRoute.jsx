// ProtectedRoute.jsx — Redirects unauthenticated users to /login.
//
// USAGE:
//   <Route path="/cart" element={<ProtectedRoute><CartPage /></ProtectedRoute>} />
//
// HOW IT WORKS:
//   1. Reads isLoggedIn from AuthContext.
//   2. If false → <Navigate to="/login"> replaces the current URL with /login,
//      and `state={{ from: location }}` records where the user was trying to go
//      so LoginPage can redirect back after a successful login.
//   3. If true  → renders the wrapped children (the actual page).

import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';

export default function ProtectedRoute({ children }) {
  const { isLoggedIn } = useAuth();
  const location = useLocation();

  if (!isLoggedIn) {
    // Pass current location as state so login page can redirect back
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}
