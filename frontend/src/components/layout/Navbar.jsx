// Navbar.jsx — Top navigation bar rendered on every page.
// Redesigned: richer colour, better spacing, user greeting.

import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';
import { useCart } from '../../context/CartContext.jsx';

export default function Navbar() {
  const { isLoggedIn, user, logout } = useAuth();
  const { cartCount } = useCart();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);

  const handleSearch = (e) => {
    e.preventDefault();
    const q = searchTerm.trim();
    navigate(q ? `/?search=${encodeURIComponent(q)}` : '/');
    setMenuOpen(false);
  };

  const handleLogout = () => {
    logout();
    navigate('/');
    setMenuOpen(false);
  };

  // Show first name only from email or name field
  const displayName = user?.name?.split(' ')[0] || user?.email?.split('@')[0] || '';

  return (
    <nav className="bg-slate-900 border-b border-slate-700 sticky top-0 z-50 shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center gap-4">

        {/* Logo */}
        <Link to="/" className="text-xl font-black text-white whitespace-nowrap flex-shrink-0 flex items-center gap-2">
          <span className="text-2xl">📚</span>
          <span className="hidden sm:block">BookStore</span>
        </Link>

        {/* Search bar */}
        <form onSubmit={handleSearch} className="flex-1 flex items-center max-w-2xl">
          <div className="flex w-full bg-slate-800 border border-slate-600 rounded-xl overflow-hidden focus-within:border-blue-400 transition-colors">
            <input
              type="search"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search books, authors, publishers…"
              className="flex-1 bg-transparent text-white placeholder-slate-400 px-4 py-2.5 text-sm focus:outline-none"
            />
            <button type="submit"
              className="bg-blue-600 hover:bg-blue-500 text-white px-5 py-2.5 text-sm font-semibold transition-colors flex-shrink-0">
              Search
            </button>
          </div>
        </form>

        {/* Desktop right-side actions */}
        <div className="hidden md:flex items-center gap-3 flex-shrink-0">
          {/* Cart */}
          <Link to="/cart"
            className="relative flex items-center gap-1.5 text-slate-300 hover:text-white text-sm font-medium transition-colors px-2 py-1">
            <span className="text-lg">🛒</span>
            <span>Cart</span>
            {cartCount > 0 && (
              <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold leading-none">
                {cartCount > 99 ? '99+' : cartCount}
              </span>
            )}
          </Link>

          {isLoggedIn ? (
            <>
              {/* User greeting */}
              <div className="flex items-center gap-1 text-slate-300 text-sm px-1">
                <span>👋</span>
                <span className="font-medium max-w-[100px] truncate">{displayName}</span>
              </div>
              <Link to="/orders" className="text-slate-300 hover:text-white text-sm transition-colors">Orders</Link>
              <Link to="/account/addresses" className="text-slate-300 hover:text-white text-sm transition-colors">Addresses</Link>
              <button onClick={handleLogout}
                className="text-sm font-semibold bg-red-600 hover:bg-red-500 text-white px-3 py-1.5 rounded-lg transition-colors">
                Logout
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-slate-300 hover:text-white text-sm font-medium transition-colors">Login</Link>
              <Link to="/register"
                className="text-sm font-bold bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-xl transition-colors">
                Register
              </Link>
            </>
          )}
        </div>

        {/* Mobile menu button */}
        <button
          onClick={() => setMenuOpen(v => !v)}
          className="md:hidden text-slate-300 hover:text-white text-xl flex-shrink-0 ml-auto">
          {menuOpen ? '✕' : '☰'}
        </button>
      </div>

      {/* Mobile dropdown menu */}
      {menuOpen && (
        <div className="md:hidden bg-slate-800 border-t border-slate-700 px-4 py-3 space-y-2">
          <Link to="/cart" onClick={() => setMenuOpen(false)}
            className="flex items-center gap-2 text-slate-300 hover:text-white py-2 text-sm">
            🛒 Cart {cartCount > 0 && <span className="bg-red-500 text-white text-xs rounded-full px-2 py-0.5">{cartCount}</span>}
          </Link>
          {isLoggedIn ? (
            <>
              <Link to="/orders" onClick={() => setMenuOpen(false)} className="block text-slate-300 hover:text-white py-2 text-sm">📦 Orders</Link>
              <Link to="/account/addresses" onClick={() => setMenuOpen(false)} className="block text-slate-300 hover:text-white py-2 text-sm">🏠 Addresses</Link>
              <button onClick={handleLogout} className="w-full text-left text-red-400 hover:text-red-300 py-2 text-sm">🚪 Logout</button>
            </>
          ) : (
            <>
              <Link to="/login" onClick={() => setMenuOpen(false)} className="block text-slate-300 hover:text-white py-2 text-sm">Login</Link>
              <Link to="/register" onClick={() => setMenuOpen(false)} className="block text-blue-400 hover:text-blue-300 py-2 text-sm font-semibold">Register →</Link>
            </>
          )}
        </div>
      )}
    </nav>
  );
}
