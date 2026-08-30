// CataloguePage.jsx — Home page: browse + search + filter all books.
//
// URL: /
// Public: yes (guests can browse without logging in)
//
// WHAT THIS PAGE DOES:
//   1. Reads ?search= and ?page= from the URL (set by Navbar search + pagination)
//   2. Calls GET /api/books with all active filters
//   3. Renders BookGrid + FilterPanel + Pagination
//   4. "Add to Cart" — if logged in: POST /api/cart/items; if guest: redirect to /login
//
// STATE:
//   books, totalPages, loading, error — from API call
//   filters — category, price range, availability (local state)
//   page — current page index (0-based, from URL)

import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getBooks, getCategories } from '../api/bookApi.js';
import { addItem } from '../api/cartApi.js';
import { useAuth } from '../context/AuthContext.jsx';
import { useCart } from '../context/CartContext.jsx';
import BookGrid       from '../components/catalogue/BookGrid.jsx';
import FilterPanel    from '../components/catalogue/FilterPanel.jsx';
import Pagination     from '../components/catalogue/Pagination.jsx';
import LoadingSpinner from '../components/common/LoadingSpinner.jsx';
import ErrorMessage   from '../components/common/ErrorMessage.jsx';
import EmptyState     from '../components/common/EmptyState.jsx';

const EMPTY_FILTERS = { categoryId: null, minPrice: null, maxPrice: null, available: null };

export default function CataloguePage() {
  const { isLoggedIn } = useAuth();
  const { refreshCartCount } = useCart();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  // Derive search term and page from the URL
  const searchTerm = searchParams.get('search') || '';
  const currentPage = parseInt(searchParams.get('page') || '0', 10);

  const [categories, setCategories] = useState([]);
  const [books,      setBooks]      = useState([]);
  const [totalPages, setTotalPages] = useState(0);
  const [loading,    setLoading]    = useState(false);
  const [error,      setError]      = useState(null);
  const [filters,    setFilters]    = useState(EMPTY_FILTERS);
  const [toast,      setToast]      = useState(null);   // success/error toast message

  // ── Load categories once on mount ──────────────────────────────────────────
  useEffect(() => {
    getCategories()
      .then((res) => setCategories(res.data))
      .catch(() => {}); // categories are non-critical; silently ignore
  }, []);

  // ── Load books whenever search term, filters, or page changes ──────────────
  const loadBooks = useCallback(() => {
    setLoading(true);
    setError(null);

    // Build query params — omit null/empty values
    const params = { page: currentPage, size: 20 };
    if (searchTerm)         params.search     = searchTerm;
    if (filters.categoryId) params.categoryId = filters.categoryId;
    if (filters.minPrice)   params.minPrice   = filters.minPrice;
    if (filters.maxPrice)   params.maxPrice   = filters.maxPrice;
    if (filters.available)  params.available  = true;

    getBooks(params)
      .then((res) => {
        setBooks(res.data.content || []);
        setTotalPages(res.data.totalPages || 0);
      })
      .catch(() => setError('Failed to load books. Please try again.'))
      .finally(() => setLoading(false));
  }, [searchTerm, currentPage, filters]);

  useEffect(() => { loadBooks(); }, [loadBooks]);

  // ── Add to Cart ─────────────────────────────────────────────────────────────
  const handleAddToCart = async (bookId) => {
    if (!isLoggedIn) {
      navigate('/login');
      return;
    }
    try {
      await addItem(bookId, 1);
      await refreshCartCount();
      showToast('✅ Added to cart!');
    } catch (err) {
      showToast('❌ ' + (err.response?.data?.message || 'Could not add to cart.'), true);
    }
  };

  // ── Simple toast notification ───────────────────────────────────────────────
  const showToast = (msg, isError = false) => {
    setToast({ msg, isError });
    setTimeout(() => setToast(null), 3000);
  };

  // ── Pagination — update URL param (triggers loadBooks via useEffect) ────────
  const handlePageChange = (newPage) => {
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', newPage);
      return next;
    });
  };

  // ── Filter change — reset to page 0 ────────────────────────────────────────
  const handleFilterChange = (newFilters) => {
    setFilters(newFilters);
    setSearchParams((prev) => {
      const next = new URLSearchParams(prev);
      next.set('page', '0');
      return next;
    });
  };

  const handleFilterReset = () => {
    setFilters(EMPTY_FILTERS);
    setSearchParams({});
  };

  return (
    <div className="min-h-screen">
      {/* Toast */}
      {toast && (
        <div className={`fixed top-20 right-4 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium transition-all
          ${toast.isError ? 'bg-red-600 text-white' : 'bg-green-600 text-white'}`}>
          {toast.msg}
        </div>
      )}

      {/* ── Hero banner — only on the home page (no search/filter active) ── */}
      {!searchTerm && !filters.categoryId && !filters.available && (
        <div className="bg-gradient-to-r from-slate-900 via-blue-950 to-indigo-900 text-white">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 md:py-14 flex flex-col md:flex-row items-center gap-8">
            <div className="flex-1 min-w-0">
              <p className="text-blue-300 text-xs sm:text-sm font-semibold uppercase tracking-widest mb-2">📚 Welcome to BookStore</p>
              <h1 className="text-3xl sm:text-4xl md:text-5xl font-black leading-tight mb-4">
                Your next great read
                <span className="text-blue-400 block sm:inline"><br className="hidden sm:block" /> is here.</span>
              </h1>
              <p className="text-slate-300 text-sm sm:text-base mb-6 max-w-md">
                Browse 113 hand-picked books across 8 categories. Find your next favourite — from technology to fiction.
              </p>
              <div className="flex flex-wrap gap-2">
                {categories.slice(0, 5).map((cat) => (
                  <button key={cat.id}
                    onClick={() => handleFilterChange({ ...EMPTY_FILTERS, categoryId: cat.id })}
                    className="bg-white/10 hover:bg-blue-600 text-white text-xs font-semibold px-4 py-1.5 rounded-full transition-colors border border-white/20">
                    {cat.name}
                  </button>
                ))}
              </div>
            </div>
            <div className="hidden md:flex gap-4 flex-shrink-0">
              {/* Decorative stacked book spines */}
              {['📗', '📘', '📕', '📙'].map((emoji, i) => (
                <div key={i} className={`text-7xl transform ${i % 2 === 0 ? 'rotate-2' : '-rotate-3'} drop-shadow-2xl`}>
                  {emoji}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Page header — shown when searching or filtering */}
      {(searchTerm || filters.categoryId || filters.available) && (
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900">
            {searchTerm ? `Results for "${searchTerm}"` : 'Filtered Books'}
          </h1>
          {!loading && !error && (
            <p className="text-sm text-gray-500 mt-1">
              Page {currentPage + 1} of {totalPages || 1} · {' '}
              <button onClick={handleFilterReset} className="text-blue-600 hover:underline text-xs">Clear filters</button>
            </p>
          )}
        </div>
      )}

      <div className="flex flex-col lg:flex-row gap-8">
        {/* Filter sidebar */}
        <FilterPanel
          categories={categories}
          filters={filters}
          onChange={handleFilterChange}
          onReset={handleFilterReset}
        />

        {/* Main content area */}
        <div className="flex-1 min-w-0">
          {loading && <LoadingSpinner message="Loading books…" />}
          {!loading && error && <ErrorMessage message={error} onRetry={loadBooks} />}
          {!loading && !error && books.length === 0 && (
            <EmptyState
              icon="🔍"
              title="No books found"
              subtitle="Try adjusting your search or filters."
              action={
                <button
                  onClick={handleFilterReset}
                  className="text-sm text-blue-600 underline"
                >
                  Clear all filters
                </button>
              }
            />
          )}
          {!loading && !error && books.length > 0 && (
            <>
              <BookGrid
                books={books}
                onAddToCart={handleAddToCart}
                isLoggedIn={isLoggedIn}
              />
              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={handlePageChange}
              />
            </>
          )}
        </div>
      </div>
      </div>
    </div>
  );
}
