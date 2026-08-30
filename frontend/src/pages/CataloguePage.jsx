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
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Toast */}
      {toast && (
        <div className={`fixed top-20 right-4 z-50 px-5 py-3 rounded-xl shadow-lg text-sm font-medium transition-all
          ${toast.isError ? 'bg-red-600 text-white' : 'bg-green-600 text-white'}`}>
          {toast.msg}
        </div>
      )}

      {/* Page header */}
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-gray-900">
          {searchTerm ? `Results for "${searchTerm}"` : 'All Books'}
        </h1>
        {!loading && !error && (
          <p className="text-sm text-gray-500 mt-1">
            Page {currentPage + 1} of {totalPages || 1}
          </p>
        )}
      </div>

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
  );
}
