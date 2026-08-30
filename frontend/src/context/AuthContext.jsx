// AuthContext.jsx — Global authentication state shared across the entire app.
//
// WHAT IS A CONTEXT?
//   React Context is a way to share data between components without passing
//   props manually through every layer. Think of it as a "global variable"
//   that any component can read or update.
//
// WHAT THIS CONTEXT PROVIDES:
//   - token       : the raw JWT string (or null if not logged in)
//   - user        : { id, email, giftPointBalance } (or null)
//   - isLoggedIn  : boolean — true if token is present
//   - login(token, user) : call this after a successful login / register
//   - logout()           : clears everything, called from Navbar or on 401
//   - updateUser(fields) : updates user fields in state (e.g. after redeeming points)
//
// WHY LOCALSTORAGE?
//   useState is lost on browser refresh. localStorage persists across refreshes.
//   On mount we read back from localStorage so the user stays logged in.

import { createContext, useContext, useState } from 'react';

// 1. Create the context (undefined default — consumer must be inside Provider)
const AuthContext = createContext(undefined);

// 2. Provider component — wraps the whole app in main.jsx (via App.jsx)
export function AuthProvider({ children }) {
  // Initialise from localStorage so refreshing the page doesn't log the user out
  const [token, setToken] = useState(() => localStorage.getItem('token'));
  const [user,  setUser]  = useState(() => {
    const saved = localStorage.getItem('user');
    try {
      return saved ? JSON.parse(saved) : null;
    } catch {
      return null; // guard against corrupted JSON in localStorage
    }
  });

  /**
   * Call after a successful login or register.
   * Persists token + user to localStorage and updates React state.
   */
  const login = (newToken, newUser) => {
    localStorage.setItem('token', newToken);
    localStorage.setItem('user', JSON.stringify(newUser));
    setToken(newToken);
    setUser(newUser);
  };

  /**
   * Call when the user clicks "Logout" or when a 401 is received.
   * Removes all persisted data.
   */
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
    setUser(null);
  };

  /**
   * Update specific fields on the user object (e.g. giftPointBalance after checkout).
   * Merges the new fields with the existing user object.
   */
  const updateUser = (fields) => {
    setUser((prev) => {
      const updated = { ...prev, ...fields };
      localStorage.setItem('user', JSON.stringify(updated));
      return updated;
    });
  };

  const value = {
    token,
    user,
    login,
    logout,
    updateUser,
    isLoggedIn: !!token, // true if token is a non-empty string
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

// 3. Custom hook — components call useAuth() instead of useContext(AuthContext)
//    This throws a helpful error if someone forgets to wrap with AuthProvider.
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (ctx === undefined) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return ctx;
}
