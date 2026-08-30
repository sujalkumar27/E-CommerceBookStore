// Navbar.jsx — Top navigation bar rendered on every page.
//
// CONTENTS:
//   Left  : "📚 BookStore" logo — links to /
//   Centre: search bar (navigates to / with ?search= query param)
//   Right : "Cart (n)" badge, Login/Register or user email + Logout
//
// STATE:
//   - cartCount and refreshCartCount come from CartContext
//   - isLoggedIn, user, logout come from AuthContext
//
// SEARCH:
//   Submitting the search form navigates to /?search=<term> using useNavigate.
//   CataloguePage reads the query param from the URL and fires the API call.

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { useCart } from '../../context/CartContext.jsx';

export default function Navbar() {
  const { isLoggedIn, user, logout } = useAuth();
  const { cartCount } = useCart();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');

  // When user submits the search form, navigate to / with a search query param.
  // CataloguePage watches the URL and calls the API automatically.
  const handleSearch = (e) => {
    e.preventDefault();
    const q = searchTerm.trim();
    navigate(q ? `/?search=${encodeURIComponent(q)}` : '/');
  };

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <nav className="bg-white border-b border-gray-200 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center gap-4">

        {/* Logo */}
        <Link
          to="/"
          className="text-xl font-bold text-blue-700 whitespace-nowrap flex-shrink-0"
        >
          📚 BookStore
        </Link>

        {/* Search bar — grows to fill available space */}
        <form onSubmit={handleSearch} className="flex-1 flex items-center max-w-xl">
          <input
            type="search"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search books, authors, publishers…"
            className="w-full border border-gray-300 rounded-l-lg px-4 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-400"
          />
          <button
            type="submit"
            className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 text-sm rounded-r-lg transition-colors"
          >
            Search
          </button>
        </form>

        {/* Right-side actions */}
        <div className="flex items-center gap-3 flex-shrink-0">
          {/* Cart link with item count badge */}
          <Link
            to="/cart"
            className="relative text-sm font-medium text-gray-700 hover:text-blue-700 flex items-center gap-1"
          >
            🛒
            <span>Cart</span>
            {cartCount > 0 && (
              <span className="absolute -top-2 -right-3 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold">
                {cartCount > 99 ? '99+' : cartCount}
              </span>
            )}
          </Link>

          {isLoggedIn ? (
            <>
              {/* Show truncated email, link to order history */}
              <Link
                to="/orders"
                className="text-sm text-gray-600 hover:text-blue-700 hidden sm:block max-w-[140px] truncate"
                title={user?.email}
              >
                {user?.email}
              </Link>
              <Link
                to="/account/addresses"
                className="text-sm text-gray-600 hover:text-blue-700 hidden md:block"
              >
                Addresses
              </Link>
              <button
                onClick={handleLogout}
                className="text-sm font-medium text-red-600 hover:text-red-800 transition-colors"
              >
                Logout
              </button>
            </>
          ) : (
            <>
              <Link
                to="/login"
                className="text-sm font-medium text-gray-700 hover:text-blue-700 transition-colors"
              >
                Login
              </Link>
              <Link
                to="/register"
                className="text-sm font-medium bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition-colors"
              >
                Register
              </Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
}
