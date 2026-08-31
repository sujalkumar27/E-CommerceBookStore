// LoginPage.jsx — Login form with book-themed background.
//
// UI: Split layout — decorative left panel + form right panel on desktop.
//     Full-width card on mobile.

import { useState, useEffect } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { login as loginApi } from '../api/authApi.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';

// Decorative book quotes shown in the left panel
const QUOTES = [
  { text: "A reader lives a thousand lives before he dies.", author: "George R.R. Martin" },
  { text: "Books are a uniquely portable magic.", author: "Stephen King" },
  { text: "Not all those who wander are lost.", author: "J.R.R. Tolkien" },
];
const QUOTE = QUOTES[Math.floor(Math.random() * QUOTES.length)];

export default function LoginPage() {
  const { login, isLoggedIn } = useAuth();
  const { refreshCartCount }  = useCart();
  const navigate  = useNavigate();
  const location  = useLocation();

  const [email,    setEmail]    = useState('');
  const [password, setPassword] = useState('');
  const [error,    setError]    = useState(null);
  const [loading,  setLoading]  = useState(false);
  const [showPwd,  setShowPwd]  = useState(false);

  // Update browser tab title on mount
  useEffect(() => { document.title = 'Login | BookStore'; }, []);

  if (isLoggedIn) { navigate('/'); return null; }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await loginApi(email, password);
      login(res.data.token, res.data.user);
      await refreshCartCount();
      const from = location.state?.from?.pathname || '/';
      navigate(from, { replace: true });
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 400) {
        setError('Invalid email or password.');
      } else {
        setError('Something went wrong. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* ── Left decorative panel (hidden on mobile) ─────────────────────── */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-slate-800 via-blue-900 to-indigo-900 flex-col justify-between p-12 relative overflow-hidden">
        {/* Background pattern */}
        <div className="absolute inset-0 opacity-10 text-white text-9xl leading-tight select-none pointer-events-none overflow-hidden">
          {Array.from({ length: 60 }).map((_, i) => (
            <span key={i} className="inline-block mr-4">📚</span>
          ))}
        </div>
        {/* Logo */}
        <div className="relative z-10">
          <div className="text-white text-4xl font-black tracking-tight">📚 BookStore</div>
          <p className="text-blue-200 mt-2 text-sm">India's favourite online bookstore</p>
        </div>
        {/* Quote */}
        <div className="relative z-10">
          <blockquote className="text-white text-xl font-light italic leading-relaxed mb-3">
            "{QUOTE.text}"
          </blockquote>
          <p className="text-blue-300 text-sm">— {QUOTE.author}</p>
        </div>
        {/* Stats */}
        <div className="relative z-10 flex gap-8">
          {[['10,000+', 'Books'], ['113', 'Curated titles'], ['8', 'Categories']].map(([num, label]) => (
            <div key={label}>
              <p className="text-white text-2xl font-bold">{num}</p>
              <p className="text-blue-300 text-xs">{label}</p>
            </div>
          ))}
        </div>
      </div>

      {/* ── Right form panel ─────────────────────────────────────────────── */}
      <div className="flex-1 flex items-center justify-center bg-gray-50 px-6 py-12">
        <div className="w-full max-w-md">
          {/* Mobile logo */}
          <div className="lg:hidden text-center mb-8">
            <div className="text-4xl mb-2">📚</div>
            <h2 className="text-xl font-bold text-gray-900">BookStore</h2>
          </div>

          <h1 className="text-3xl font-bold text-gray-900 mb-1">Welcome back</h1>
          <p className="text-gray-500 text-sm mb-8">Sign in to continue reading</p>

          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 text-sm rounded-xl px-4 py-3 mb-6 flex items-center gap-2">
              <span>⚠️</span> {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Email address</label>
              <input
                type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                required autoComplete="email" placeholder="you@example.com"
                className="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-sm
                           focus:border-blue-500 focus:outline-none transition-colors bg-white"
              />
            </div>

            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-1.5">Password</label>
              <div className="relative">
                <input
                  type={showPwd ? 'text' : 'password'} value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required autoComplete="current-password" placeholder="••••••••"
                  className="w-full border-2 border-gray-200 rounded-xl px-4 py-3 text-sm pr-12
                             focus:border-blue-500 focus:outline-none transition-colors bg-white"
                />
                <button type="button" onClick={() => setShowPwd(v => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 text-sm">
                  {showPwd ? '🙈' : '👁️'}
                </button>
              </div>
            </div>

            <button type="submit" disabled={loading}
              className="w-full bg-blue-600 hover:bg-blue-700 active:bg-blue-800 text-white font-bold
                         py-3.5 rounded-xl transition-colors disabled:opacity-50 text-base shadow-sm">
              {loading ? '⏳ Signing in…' : 'Sign In'}
            </button>
          </form>

          <p className="text-center text-sm text-gray-500 mt-8">
            Don't have an account?{' '}
            <Link to="/register" className="text-blue-600 font-bold hover:underline">Create one free →</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
